package com.rolemate.backend.persistence.entity;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Spring Data JDBC entity mapped to the {@code session_feedback} table.
 * Stores per-user feedback (GOOD/BAD) after a session ends.
 */
@Table("session_feedback")
public class FeedbackRecord {

    @Id
    private Long id;

    @Column("session_id")
    private String sessionId;

    @Column("user_id")
    private String userId;

    private String rating;

    @Column("created_at")
    private Instant createdAt;

    public FeedbackRecord() {
    }

    public FeedbackRecord(String sessionId, String userId, String rating) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.rating = rating;
        this.createdAt = Instant.now();
    }

    // ── Getters and Setters ──

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
