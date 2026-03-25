package com.rolemate.backend.matchmaking.model;

public class ServerEvent {

    private ServerEventType type;
    private String sessionId;
    private String partnerId;
    private String role;
    private String content;
    private String message;

    public ServerEvent() {
    }

    public ServerEvent(ServerEventType type, String message) {
        this.type = type;
        this.message = message;
    }

    public ServerEventType getType() {
        return type;
    }

    public void setType(ServerEventType type) {
        this.type = type;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
