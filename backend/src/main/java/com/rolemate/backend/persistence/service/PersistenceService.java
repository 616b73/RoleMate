package com.rolemate.backend.persistence.service;

import com.rolemate.backend.matchmaking.model.MatchSession;
import com.rolemate.backend.persistence.entity.SessionRecord;
import com.rolemate.backend.persistence.repository.SessionRecordRepository;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Bridge between the in-memory matchmaking layer and the database persistence layer.
 * All write operations are asynchronous to avoid blocking real-time WebSocket flows.
 *
 * <p>This service translates {@link MatchSession} (in-memory model) into
 * {@link SessionRecord} (DB entity) and delegates to the repository.
 */
@Service
public class PersistenceService {

    private static final Logger log = LoggerFactory.getLogger(PersistenceService.class);

    private final SessionRecordRepository sessionRecordRepository;

    public PersistenceService(SessionRecordRepository sessionRecordRepository) {
        this.sessionRecordRepository = sessionRecordRepository;
    }

    /**
     * Persists a newly created session record asynchronously.
     * Called by {@link com.rolemate.backend.matchmaking.service.SessionService#createSession}.
     */
    @Async
    public void persistSessionCreated(MatchSession session) {
        try {
            SessionRecord record = new SessionRecord(
                    session.getSessionId(),
                    session.getRole(),
                    session.getUserAId(),
                    session.getUserBId(),
                    "TEXT",
                    "ACTIVE",
                    session.getCreatedAt()
            );
            sessionRecordRepository.save(record);
            log.debug("Persisted new session: sessionId={}", session.getSessionId());
        } catch (Exception e) {
            log.error("Failed to persist session creation: sessionId={}, error={}",
                    session.getSessionId(), e.getMessage(), e);
        }
    }

    /**
     * Updates the session record with end time and duration asynchronously.
     * Called by {@link com.rolemate.backend.matchmaking.service.SessionService#endSessionForUser}.
     */
    @Async
    public void persistSessionEnded(MatchSession session) {
        try {
            sessionRecordRepository.findById(session.getSessionId()).ifPresent(record -> {
                record.setStatus("ENDED");
                record.setEndedAt(session.getEndedAt());

                if (session.getCreatedAt() != null && session.getEndedAt() != null) {
                    long seconds = Duration.between(session.getCreatedAt(), session.getEndedAt()).getSeconds();
                    record.setDurationSeconds(seconds);
                }

                sessionRecordRepository.save(record);
                log.debug("Persisted session end: sessionId={}, durationSeconds={}",
                        session.getSessionId(), record.getDurationSeconds());
            });
        } catch (Exception e) {
            log.error("Failed to persist session end: sessionId={}, error={}",
                    session.getSessionId(), e.getMessage(), e);
        }
    }

    /**
     * Returns total number of sessions ever recorded.
     */
    public long getTotalSessionCount() {
        try {
            return sessionRecordRepository.countTotalSessions();
        } catch (Exception e) {
            log.error("Failed to query total session count: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * Returns count of completed (ended) sessions.
     */
    public long getCompletedSessionCount() {
        try {
            return sessionRecordRepository.countCompletedSessions();
        } catch (Exception e) {
            log.error("Failed to query completed session count: {}", e.getMessage());
            return -1;
        }
    }
}
