package com.rolemate.backend.matchmaking.model;

import java.time.Instant;

public class MatchSession {

    private final String sessionId;
    private final String role;
    private final String userAId;
    private final String userBId;
    private final Instant createdAt;

    public MatchSession(String sessionId, String role, String userAId, String userBId, Instant createdAt) {
        this.sessionId = sessionId;
        this.role = role;
        this.userAId = userAId;
        this.userBId = userBId;
        this.createdAt = createdAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getRole() {
        return role;
    }

    public String getUserAId() {
        return userAId;
    }

    public String getUserBId() {
        return userBId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getPartnerId(String userId) {
        if (userAId.equals(userId)) {
            return userBId;
        }
        return userAId;
    }
}
