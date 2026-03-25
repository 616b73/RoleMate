package com.rolemate.backend.matchmaking.model;

import org.springframework.web.socket.WebSocketSession;

public class UserConnection {

    private final String userId;
    private final WebSocketSession socketSession;
    private String role;

    public UserConnection(String userId, WebSocketSession socketSession) {
        this.userId = userId;
        this.socketSession = socketSession;
    }

    public String getUserId() {
        return userId;
    }

    public WebSocketSession getSocketSession() {
        return socketSession;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
