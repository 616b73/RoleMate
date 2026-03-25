package com.rolemate.backend.matchmaking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rolemate.backend.matchmaking.model.ClientEvent;
import com.rolemate.backend.matchmaking.model.ClientEventType;
import com.rolemate.backend.matchmaking.model.MatchSession;
import com.rolemate.backend.matchmaking.model.ServerEvent;
import com.rolemate.backend.matchmaking.model.ServerEventType;
import com.rolemate.backend.matchmaking.model.UserConnection;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Service
public class MatchmakingService {

    private final ObjectMapper objectMapper;
    private final Map<String, UserConnection> connectedUsers = new ConcurrentHashMap<>();
    private final Map<String, Queue<UserConnection>> waitingQueues = new ConcurrentHashMap<>();
    private final Map<String, MatchSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<String, String> sessionIdByUserId = new ConcurrentHashMap<>();

    public MatchmakingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void registerConnection(WebSocketSession webSocketSession) throws IOException {
        UserConnection connection = new UserConnection(webSocketSession.getId(), webSocketSession);
        connectedUsers.put(connection.getUserId(), connection);

        ServerEvent event = new ServerEvent(ServerEventType.CONNECTED, "Connected to RoleMate matchmaking");
        event.setPartnerId(connection.getUserId());
        sendEvent(webSocketSession, event);
    }

    public void handleEvent(WebSocketSession webSocketSession, String payload) throws IOException {
        ClientEvent event = objectMapper.readValue(payload, ClientEvent.class);

        if (event.getType() == null) {
            sendError(webSocketSession, "Event type is required");
            return;
        }

        switch (event.getType()) {
            case JOIN_QUEUE -> joinQueue(webSocketSession, event);
            case SEND_MESSAGE -> sendChatMessage(webSocketSession, event);
            case NEXT_USER -> handleNextUser(webSocketSession);
            default -> sendError(webSocketSession, "Unsupported event type: " + event.getType());
        }
    }

    public synchronized void handleDisconnect(WebSocketSession webSocketSession) throws IOException {
        UserConnection disconnectedUser = connectedUsers.remove(webSocketSession.getId());
        if (disconnectedUser == null) {
            return;
        }

        removeFromWaitingQueues(disconnectedUser.getUserId());
        endSessionForUser(disconnectedUser.getUserId(), true);
    }

    private synchronized void joinQueue(WebSocketSession webSocketSession, ClientEvent event) throws IOException {
        UserConnection connection = connectedUsers.get(webSocketSession.getId());
        if (connection == null) {
            sendError(webSocketSession, "Connection not registered");
            return;
        }

        String normalizedRole = normalizeRole(event.getRole());
        if (normalizedRole == null) {
            sendError(webSocketSession, "Role is required for JOIN_QUEUE");
            return;
        }

        removeFromWaitingQueues(connection.getUserId());
        endSessionForUser(connection.getUserId(), true);

        connection.setRole(normalizedRole);
        waitingQueues.computeIfAbsent(normalizedRole, ignored -> new ConcurrentLinkedQueue<>()).offer(connection);

        ServerEvent queuedEvent = new ServerEvent(ServerEventType.QUEUED, "Added to queue");
        queuedEvent.setRole(normalizedRole);
        sendEvent(webSocketSession, queuedEvent);

        attemptMatch(normalizedRole);
    }

    private synchronized void attemptMatch(String role) throws IOException {
        Queue<UserConnection> queue = waitingQueues.get(role);
        if (queue == null) {
            return;
        }

        UserConnection first = pollNextAvailable(queue);
        UserConnection second = pollNextAvailable(queue);

        if (first == null || second == null) {
            if (first != null) {
                queue.offer(first);
            }
            return;
        }

        String matchId = UUID.randomUUID().toString();
        MatchSession matchSession = new MatchSession(matchId, role, first.getUserId(), second.getUserId(), Instant.now());

        sessionsById.put(matchId, matchSession);
        sessionIdByUserId.put(first.getUserId(), matchId);
        sessionIdByUserId.put(second.getUserId(), matchId);

        sendMatchFound(first, second, matchSession);
        sendMatchFound(second, first, matchSession);
    }

    private UserConnection pollNextAvailable(Queue<UserConnection> queue) {
        while (true) {
            UserConnection candidate = queue.poll();
            if (candidate == null) {
                return null;
            }

            if (candidate.getSocketSession().isOpen() && !sessionIdByUserId.containsKey(candidate.getUserId())) {
                return candidate;
            }
        }
    }

    private void sendMatchFound(UserConnection recipient, UserConnection partner, MatchSession matchSession) throws IOException {
        ServerEvent event = new ServerEvent(ServerEventType.MATCH_FOUND, "Match found");
        event.setSessionId(matchSession.getSessionId());
        event.setRole(matchSession.getRole());
        event.setPartnerId(partner.getUserId());
        sendEvent(recipient.getSocketSession(), event);
    }

    private synchronized void sendChatMessage(WebSocketSession webSocketSession, ClientEvent event) throws IOException {
        String content = sanitizeContent(event.getContent());
        if (content == null) {
            sendError(webSocketSession, "Message content is required");
            return;
        }

        String currentSessionId = sessionIdByUserId.get(webSocketSession.getId());
        if (currentSessionId == null) {
            sendError(webSocketSession, "You are not currently matched");
            return;
        }

        MatchSession matchSession = sessionsById.get(currentSessionId);
        if (matchSession == null) {
            sendError(webSocketSession, "Session not found");
            return;
        }

        String partnerId = matchSession.getPartnerId(webSocketSession.getId());
        UserConnection partner = connectedUsers.get(partnerId);
        if (partner == null || !partner.getSocketSession().isOpen()) {
            sendError(webSocketSession, "Partner is no longer connected");
            endSessionForUser(webSocketSession.getId(), true);
            return;
        }

        ServerEvent chatEvent = new ServerEvent(ServerEventType.RECEIVE_MESSAGE, null);
        chatEvent.setSessionId(matchSession.getSessionId());
        chatEvent.setPartnerId(webSocketSession.getId());
        chatEvent.setContent(content);
        sendEvent(partner.getSocketSession(), chatEvent);
    }

    private synchronized void handleNextUser(WebSocketSession webSocketSession) throws IOException {
        UserConnection connection = connectedUsers.get(webSocketSession.getId());
        if (connection == null) {
            sendError(webSocketSession, "Connection not registered");
            return;
        }

        String role = connection.getRole();
        endSessionForUser(connection.getUserId(), true);

        if (role != null) {
            waitingQueues.computeIfAbsent(role, ignored -> new ConcurrentLinkedQueue<>()).offer(connection);
            ServerEvent queuedEvent = new ServerEvent(ServerEventType.QUEUED, "Looking for next partner");
            queuedEvent.setRole(role);
            sendEvent(webSocketSession, queuedEvent);
            attemptMatch(role);
        }
    }

    private synchronized void endSessionForUser(String userId, boolean notifyPartner) throws IOException {
        String currentSessionId = sessionIdByUserId.remove(userId);
        if (currentSessionId == null) {
            return;
        }

        MatchSession matchSession = sessionsById.remove(currentSessionId);
        if (matchSession == null) {
            return;
        }

        String partnerId = matchSession.getPartnerId(userId);
        sessionIdByUserId.remove(partnerId);

        UserConnection currentUser = connectedUsers.get(userId);
        if (currentUser != null && currentUser.getSocketSession().isOpen()) {
            ServerEvent event = new ServerEvent(ServerEventType.SESSION_ENDED, "Session ended");
            event.setSessionId(currentSessionId);
            sendEvent(currentUser.getSocketSession(), event);
        }

        if (notifyPartner) {
            UserConnection partner = connectedUsers.get(partnerId);
            if (partner != null && partner.getSocketSession().isOpen()) {
                ServerEvent event = new ServerEvent(ServerEventType.PARTNER_LEFT, "Partner left the session");
                event.setSessionId(currentSessionId);
                sendEvent(partner.getSocketSession(), event);
            }
        }
    }

    private void removeFromWaitingQueues(String userId) {
        for (Queue<UserConnection> queue : waitingQueues.values()) {
            queue.removeIf(connection -> Objects.equals(connection.getUserId(), userId));
        }
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        sendEvent(session, new ServerEvent(ServerEventType.ERROR, message));
    }

    private void sendEvent(WebSocketSession session, ServerEvent event) throws IOException {
        if (!session.isOpen()) {
            return;
        }

        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException exception) {
            throw new IOException("Failed to serialize websocket event", exception);
        }
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        return role.trim();
    }

    private String sanitizeContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        return content.trim();
    }
}
