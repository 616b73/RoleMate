package com.rolemate.backend.websocket;

import java.util.concurrent.BlockingQueue;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Simple WebSocket handler that collects received text messages into a blocking queue
 * for test assertions.
 */
class MessageCollector extends TextWebSocketHandler {

    private final BlockingQueue<String> messages;

    MessageCollector(BlockingQueue<String> messages) {
        this.messages = messages;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        messages.add(message.getPayload());
    }
}
