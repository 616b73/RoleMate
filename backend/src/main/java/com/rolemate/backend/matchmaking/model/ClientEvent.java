package com.rolemate.backend.matchmaking.model;

public class ClientEvent {

    private ClientEventType type;
    private String role;
    private String content;

    public ClientEventType getType() {
        return type;
    }

    public void setType(ClientEventType type) {
        this.type = type;
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
}
