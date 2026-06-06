package com.rolemate.backend.matchmaking.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;

/**
 * Represents an inbound event sent by a WebSocket client.
 * Unknown JSON properties are rejected to enforce a clean protocol.
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class ClientEvent {

    @NotNull(message = "Event type is required")
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
