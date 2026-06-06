package com.rolemate.backend.websocket;

import com.rolemate.backend.matchmaking.service.MatchmakingService;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket handler for the /ws/matchmaking endpoint.
 * Delegates all business logic to {@link MatchmakingService} and handles
 * transport-level concerns (connection lifecycle, error logging, malformed payload recovery).
 */
@Component
public class RoleMateWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(RoleMateWebSocketHandler.class);

    private final MatchmakingService matchmakingService;

    public RoleMateWebSocketHandler(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established: sessionId={}, remoteAddress={}",
                session.getId(), session.getRemoteAddress());
        matchmakingService.registerConnection(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            matchmakingService.handleEvent(session, message.getPayload());
        } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
            log.warn("Malformed JSON from sessionId={}: {}", session.getId(), e.getMessage());
            sendErrorEvent(session, "Invalid JSON payload: " + e.getOriginalMessage());
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.warn("JSON parse error from sessionId={}: {}", session.getId(), e.getMessage());
            sendErrorEvent(session, "Malformed JSON: " + e.getOriginalMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Transport error for sessionId={}: {}", session.getId(), exception.getMessage(), exception);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        log.info("WebSocket connection closed: sessionId={}, status={}", session.getId(), status);
        matchmakingService.handleDisconnect(session);
    }

    /**
     * Sends a raw ERROR event to the client for malformed payloads that couldn't
     * be parsed by the matchmaking service.
     */
    private void sendErrorEvent(WebSocketSession session, String errorMessage) {
        if (!session.isOpen()) {
            return;
        }
        try {
            String json = String.format("{\"type\":\"ERROR\",\"message\":\"%s\"}",
                    errorMessage.replace("\"", "\\\""));
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("Failed to send error event to sessionId={}: {}", session.getId(), e.getMessage());
        }
    }
}
