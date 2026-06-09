package com.rolemate.backend.matchmaking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rolemate.backend.matchmaking.model.ClientEvent;
import com.rolemate.backend.matchmaking.model.ClientEventType;
import com.rolemate.backend.matchmaking.model.MatchSession;
import com.rolemate.backend.matchmaking.model.RoleCategory;
import com.rolemate.backend.matchmaking.model.ServerEvent;
import com.rolemate.backend.matchmaking.model.ServerEventType;
import com.rolemate.backend.matchmaking.model.UserConnection;
import com.rolemate.backend.signaling.service.SignalingService;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Core matchmaking service responsible for:
 * <ul>
 *   <li>Managing WebSocket connections</li>
 *   <li>Role-based queue management</li>
 *   <li>Automatic partner matching</li>
 *   <li>Chat message relay between matched users</li>
 *   <li>Next-partner and disconnect flows</li>
 * </ul>
 *
 * <p>Session lifecycle is delegated to {@link SessionService}.
 * Uses per-role {@link ReentrantLock} for fine-grained concurrency control.
 */
@Service
public class MatchmakingService {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingService.class);

    private final ObjectMapper objectMapper;
    private final SessionService sessionService;
    private final SignalingService signalingService;

    /** All currently connected users, keyed by WebSocket session ID. */
    private final Map<String, UserConnection> connectedUsers = new ConcurrentHashMap<>();

    /** Waiting queues grouped by normalized role name. */
    private final Map<String, Queue<UserConnection>> waitingQueues = new ConcurrentHashMap<>();

    /** Per-role locks for fine-grained concurrency on queue operations. */
    private final Map<String, ReentrantLock> roleLocks = new ConcurrentHashMap<>();

    /** Global lock for cross-role operations (disconnect cleanup). */
    private final ReentrantLock globalLock = new ReentrantLock();

    public MatchmakingService(ObjectMapper objectMapper, SessionService sessionService,
                              SignalingService signalingService) {
        this.objectMapper = objectMapper;
        this.sessionService = sessionService;
        this.signalingService = signalingService;
    }

    /**
     * Registers a new WebSocket connection and sends a CONNECTED event to the client.
     */
    public void registerConnection(WebSocketSession webSocketSession) throws IOException {
        UserConnection connection = new UserConnection(webSocketSession.getId(), webSocketSession);
        connectedUsers.put(connection.getUserId(), connection);

        log.info("Connection registered: userId={}", connection.getUserId());

        ServerEvent event = new ServerEvent(ServerEventType.CONNECTED, "Connected to RoleMate matchmaking");
        event.setPartnerId(connection.getUserId());
        sendEvent(webSocketSession, event);
    }

    /**
     * Routes an inbound client event to the appropriate handler.
     */
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
            // WebRTC signaling — relay to matched partner
            case WEBRTC_OFFER -> signalingService.relayOffer(webSocketSession, event, connectedUsers);
            case WEBRTC_ANSWER -> signalingService.relayAnswer(webSocketSession, event, connectedUsers);
            case ICE_CANDIDATE -> signalingService.relayIceCandidate(webSocketSession, event, connectedUsers);
            case VIDEO_READY -> signalingService.notifyVideoReady(webSocketSession, connectedUsers);
            case VIDEO_ENDED -> signalingService.notifyVideoEnded(webSocketSession, connectedUsers);
            default -> sendError(webSocketSession, "Unsupported event type: " + event.getType());
        }
    }

    /**
     * Handles a user disconnection: removes from queues, ends active session,
     * and notifies the partner.
     */
    public void handleDisconnect(WebSocketSession webSocketSession) throws IOException {
        globalLock.lock();
        try {
            UserConnection disconnectedUser = connectedUsers.remove(webSocketSession.getId());
            if (disconnectedUser == null) {
                return;
            }

            log.info("User disconnected: userId={}", disconnectedUser.getUserId());
            removeFromWaitingQueues(disconnectedUser.getUserId());
            endSessionAndNotifyPartner(disconnectedUser.getUserId());
        } finally {
            globalLock.unlock();
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Queue & Matching
    // ──────────────────────────────────────────────────────────────

    private void joinQueue(WebSocketSession webSocketSession, ClientEvent event) throws IOException {
        UserConnection connection = connectedUsers.get(webSocketSession.getId());
        if (connection == null) {
            sendError(webSocketSession, "Connection not registered");
            return;
        }

        // Validate role against known categories
        RoleCategory roleCategory = RoleCategory.fromDisplayName(event.getRole());
        if (roleCategory == null) {
            String supportedRoles = Arrays.stream(RoleCategory.values())
                    .map(RoleCategory::getDisplayName)
                    .collect(Collectors.joining(", "));
            sendError(webSocketSession, "Unsupported role. Supported roles: " + supportedRoles);
            return;
        }

        String normalizedRole = roleCategory.getDisplayName();
        ReentrantLock roleLock = roleLocks.computeIfAbsent(normalizedRole, k -> new ReentrantLock());

        roleLock.lock();
        try {
            // Clean up any existing state before joining
            removeFromWaitingQueues(connection.getUserId());
            endSessionAndNotifyPartner(connection.getUserId());

            connection.setRole(normalizedRole);
            waitingQueues.computeIfAbsent(normalizedRole, ignored -> new ConcurrentLinkedQueue<>()).offer(connection);

            log.info("User joined queue: userId={}, role={}", connection.getUserId(), normalizedRole);

            ServerEvent queuedEvent = new ServerEvent(ServerEventType.QUEUED, "Added to queue");
            queuedEvent.setRole(normalizedRole);
            sendEvent(webSocketSession, queuedEvent);

            attemptMatch(normalizedRole);
        } finally {
            roleLock.unlock();
        }
    }

    private void attemptMatch(String role) throws IOException {
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

        MatchSession matchSession = sessionService.createSession(first, second, role);

        log.info("Match found: sessionId={}, userA={}, userB={}, role={}",
                matchSession.getSessionId(), first.getUserId(), second.getUserId(), role);

        sendMatchFound(first, second, matchSession);
        sendMatchFound(second, first, matchSession);
    }

    private UserConnection pollNextAvailable(Queue<UserConnection> queue) {
        while (true) {
            UserConnection candidate = queue.poll();
            if (candidate == null) {
                return null;
            }

            if (candidate.getSocketSession().isOpen() && !sessionService.isInSession(candidate.getUserId())) {
                return candidate;
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Chat
    // ──────────────────────────────────────────────────────────────

    private void sendChatMessage(WebSocketSession webSocketSession, ClientEvent event) throws IOException {
        String content = sanitizeContent(event.getContent());
        if (content == null) {
            sendError(webSocketSession, "Message content is required");
            return;
        }

        Optional<MatchSession> sessionOpt = sessionService.getSessionForUser(webSocketSession.getId());
        if (sessionOpt.isEmpty()) {
            sendError(webSocketSession, "You are not currently matched");
            return;
        }

        MatchSession matchSession = sessionOpt.get();
        String partnerId = matchSession.getPartnerId(webSocketSession.getId());
        UserConnection partner = connectedUsers.get(partnerId);

        if (partner == null || !partner.getSocketSession().isOpen()) {
            sendError(webSocketSession, "Partner is no longer connected");
            endSessionAndNotifyPartner(webSocketSession.getId());
            return;
        }

        ServerEvent chatEvent = new ServerEvent(ServerEventType.RECEIVE_MESSAGE, null);
        chatEvent.setSessionId(matchSession.getSessionId());
        chatEvent.setPartnerId(webSocketSession.getId());
        chatEvent.setContent(content);
        sendEvent(partner.getSocketSession(), chatEvent);
    }

    // ──────────────────────────────────────────────────────────────
    // Next Partner
    // ──────────────────────────────────────────────────────────────

    private void handleNextUser(WebSocketSession webSocketSession) throws IOException {
        UserConnection connection = connectedUsers.get(webSocketSession.getId());
        if (connection == null) {
            sendError(webSocketSession, "Connection not registered");
            return;
        }

        String role = connection.getRole();

        log.info("User requested next partner: userId={}, role={}", connection.getUserId(), role);

        endSessionAndNotifyPartner(connection.getUserId());

        if (role != null) {
            ReentrantLock roleLock = roleLocks.computeIfAbsent(role, k -> new ReentrantLock());
            roleLock.lock();
            try {
                waitingQueues.computeIfAbsent(role, ignored -> new ConcurrentLinkedQueue<>()).offer(connection);

                ServerEvent queuedEvent = new ServerEvent(ServerEventType.QUEUED, "Looking for next partner");
                queuedEvent.setRole(role);
                sendEvent(webSocketSession, queuedEvent);

                attemptMatch(role);
            } finally {
                roleLock.unlock();
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Session Lifecycle Helpers
    // ──────────────────────────────────────────────────────────────

    /**
     * Ends any active session for the user and notifies both parties.
     */
    private void endSessionAndNotifyPartner(String userId) throws IOException {
        Optional<MatchSession> sessionOpt = sessionService.endSessionForUser(userId);
        if (sessionOpt.isEmpty()) {
            return;
        }

        MatchSession session = sessionOpt.get();

        // Notify the user who initiated the end
        UserConnection currentUser = connectedUsers.get(userId);
        if (currentUser != null && currentUser.getSocketSession().isOpen()) {
            ServerEvent endedEvent = new ServerEvent(ServerEventType.SESSION_ENDED, "Session ended");
            endedEvent.setSessionId(session.getSessionId());
            sendEvent(currentUser.getSocketSession(), endedEvent);
        }

        // Notify the partner
        String partnerId = session.getPartnerId(userId);
        UserConnection partner = connectedUsers.get(partnerId);
        if (partner != null && partner.getSocketSession().isOpen()) {
            ServerEvent partnerEvent = new ServerEvent(ServerEventType.PARTNER_LEFT, "Partner left the session");
            partnerEvent.setSessionId(session.getSessionId());
            sendEvent(partner.getSocketSession(), partnerEvent);
        }
    }

    private void removeFromWaitingQueues(String userId) {
        for (Queue<UserConnection> queue : waitingQueues.values()) {
            queue.removeIf(connection -> Objects.equals(connection.getUserId(), userId));
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Monitoring / Status
    // ──────────────────────────────────────────────────────────────

    /**
     * Returns a snapshot of current queue sizes per role.
     */
    public Map<String, Integer> getQueueStatus() {
        Map<String, Integer> status = new LinkedHashMap<>();
        for (RoleCategory role : RoleCategory.values()) {
            Queue<UserConnection> queue = waitingQueues.get(role.getDisplayName());
            status.put(role.getDisplayName(), queue != null ? queue.size() : 0);
        }
        return status;
    }

    /**
     * Returns the number of currently connected users.
     */
    public int getConnectedUserCount() {
        return connectedUsers.size();
    }

    /**
     * Returns the number of active match sessions.
     */
    public int getActiveSessionCount() {
        return sessionService.getActiveSessionCount();
    }

    // ──────────────────────────────────────────────────────────────
    // Event Sending Utilities
    // ──────────────────────────────────────────────────────────────

    private void sendMatchFound(UserConnection recipient, UserConnection partner, MatchSession matchSession) throws IOException {
        ServerEvent event = new ServerEvent(ServerEventType.MATCH_FOUND, "Match found");
        event.setSessionId(matchSession.getSessionId());
        event.setRole(matchSession.getRole());
        event.setPartnerId(partner.getUserId());
        sendEvent(recipient.getSocketSession(), event);
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

    private String sanitizeContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        return content.trim();
    }
}
