package com.rolemate.backend.persistence.entity;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Spring Data JDBC entity mapped to the {@code match_sessions} table.
 * Stores session metadata only — chat messages are ephemeral and not persisted.
 */
@Table("match_sessions")
public class SessionRecord {

    @Id
    private String id;

    private String role;

    @Column("user_a_id")
    private String userAId;

    @Column("user_b_id")
    private String userBId;

    @Column("session_type")
    private String sessionType;

    private String status;

    @Column("created_at")
    private Instant createdAt;

    @Column("ended_at")
    private Instant endedAt;

    @Column("duration_seconds")
    private Long durationSeconds;

    /** Required by Spring Data JDBC. */
    public SessionRecord() {
    }

    public SessionRecord(String id, String role, String userAId, String userBId,
                         String sessionType, String status, Instant createdAt) {
        this.id = id;
        this.role = role;
        this.userAId = userAId;
        this.userBId = userBId;
        this.sessionType = sessionType;
        this.status = status;
        this.createdAt = createdAt;
    }

    // ── Getters and Setters ──

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUserAId() {
        return userAId;
    }

    public void setUserAId(String userAId) {
        this.userAId = userAId;
    }

    public String getUserBId() {
        return userBId;
    }

    public void setUserBId(String userBId) {
        this.userBId = userBId;
    }

    public String getSessionType() {
        return sessionType;
    }

    public void setSessionType(String sessionType) {
        this.sessionType = sessionType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
