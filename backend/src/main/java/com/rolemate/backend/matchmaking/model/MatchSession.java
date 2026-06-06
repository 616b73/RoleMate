package com.rolemate.backend.matchmaking.model;

import java.time.Instant;

/**
 * Represents an active match session between two users.
 * Tracks both users, the matched role, lifecycle timestamps, and session status.
 */
public class MatchSession {

    /** Lifecycle status of a match session. */
    public enum Status {
        ACTIVE,
        ENDED
    }

    private final String sessionId;
    private final String role;
    private final String userAId;
    private final String userBId;
    private final Instant createdAt;
    private Status status;
    private Instant endedAt;

    public MatchSession(String sessionId, String role, String userAId, String userBId, Instant createdAt) {
        this.sessionId = sessionId;
        this.role = role;
        this.userAId = userAId;
        this.userBId = userBId;
        this.createdAt = createdAt;
        this.status = Status.ACTIVE;
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

    public Status getStatus() {
        return status;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    /**
     * Marks this session as ended and records the end timestamp.
     */
    public void end() {
        this.status = Status.ENDED;
        this.endedAt = Instant.now();
    }

    /**
     * Returns the partner's user ID given one user's ID.
     *
     * @param userId the known user's ID
     * @return the other user's ID in this session
     */
    public String getPartnerId(String userId) {
        if (userAId.equals(userId)) {
            return userBId;
        }
        return userAId;
    }
}
