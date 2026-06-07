package com.rolemate.backend.persistence.repository;

import com.rolemate.backend.persistence.entity.SessionRecord;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JDBC repository for {@link SessionRecord}.
 * Provides basic CRUD plus custom queries for analytics.
 */
@Repository
public interface SessionRecordRepository extends CrudRepository<SessionRecord, String> {

    /**
     * Counts total sessions ever created (all statuses).
     */
    @Query("SELECT COUNT(*) FROM match_sessions")
    long countTotalSessions();

    /**
     * Counts sessions by role.
     */
    @Query("SELECT COUNT(*) FROM match_sessions WHERE role = :role")
    long countByRole(String role);

    /**
     * Counts sessions that ended (completed lifecycle).
     */
    @Query("SELECT COUNT(*) FROM match_sessions WHERE status = 'ENDED'")
    long countCompletedSessions();
}
