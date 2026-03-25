package com.rolemate.backend.websocket;

import com.rolemate.backend.matchmaking.service.MatchmakingService;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class RoleMateWebSocketHandler extends TextWebSocketHandler {

    private final MatchmakingService matchmakingService;

    public RoleMateWebSocketHandler(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        matchmakingService.registerConnection(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        matchmakingService.handleEvent(session, message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        matchmakingService.handleDisconnect(session);
    }
}
