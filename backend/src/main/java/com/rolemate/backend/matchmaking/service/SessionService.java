package com.rolemate.backend.matchmaking.service;

import com.rolemate.backend.matchmaking.model.MatchSession;
import com.rolemate.backend.matchmaking.model.UserConnection;
import com.rolemate.backend.persistence.service.PersistenceService;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Manages the lifecycle of match sessions.
 * Owns the in-memory session storage maps and provides clean CRUD operations
 * for creating, querying, and ending sessions.
 *
 * <p>Session metadata is persisted asynchronously via {@link PersistenceService}
 * for historical tracking and analytics.
 */
@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final PersistenceService persistenceService;

    /** Active sessions indexed by session ID. */
    private final Map<String, MatchSession> sessionsById = new ConcurrentHashMap<>();

    /** Reverse lookup: user ID → session ID for quick session resolution. */
    private final Map<String, String> sessionIdByUserId = new ConcurrentHashMap<>();

    public SessionService(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    /**
     * Creates a new active session pairing two users under the given role.
     *
     * @param userA the first matched user
     * @param userB the second matched user
     * @param role  the role category they were matched on
     * @return the newly created session
     */
    public MatchSession createSession(UserConnection userA, UserConnection userB, String role) {
        String sessionId = UUID.randomUUID().toString();
        MatchSession session = new MatchSession(sessionId, role, userA.getUserId(), userB.getUserId(), Instant.now());

        sessionsById.put(sessionId, session);
        sessionIdByUserId.put(userA.getUserId(), sessionId);
        sessionIdByUserId.put(userB.getUserId(), sessionId);

        log.info("Session created: sessionId={}, role={}, userA={}, userB={}",
                sessionId, role, userA.getUserId(), userB.getUserId());

        // Persist asynchronously — does not block the matchmaking flow
        persistenceService.persistSessionCreated(session);

        return session;
    }

    /**
     * Ends the session that the given user belongs to.
     * Removes both users from the session lookup and marks the session as ended.
     *
     * @param userId the user requesting session termination
     * @return the ended session, or empty if the user had no active session
     */
    public Optional<MatchSession> endSessionForUser(String userId) {
        String sessionId = sessionIdByUserId.remove(userId);
        if (sessionId == null) {
            return Optional.empty();
        }

        MatchSession session = sessionsById.remove(sessionId);
        if (session == null) {
            return Optional.empty();
        }

        // Remove the partner's mapping as well
        String partnerId = session.getPartnerId(userId);
        sessionIdByUserId.remove(partnerId);

        session.end();
        log.info("Session ended: sessionId={}, initiator={}, partner={}", sessionId, userId, partnerId);

        // Persist end-of-session metadata asynchronously
        persistenceService.persistSessionEnded(session);

        return Optional.of(session);
    }

    /**
     * Retrieves the active session for the given user.
     *
     * @param userId the user ID to look up
     * @return the active session, or empty if the user is not in a session
     */
    public Optional<MatchSession> getSessionForUser(String userId) {
        String sessionId = sessionIdByUserId.get(userId);
        if (sessionId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessionsById.get(sessionId));
    }

    /**
     * Checks whether a user is currently in an active session.
     *
     * @param userId the user ID to check
     * @return true if the user has an active session
     */
    public boolean isInSession(String userId) {
        return sessionIdByUserId.containsKey(userId);
    }

    /**
     * Returns the count of currently active sessions.
     */
    public int getActiveSessionCount() {
        return sessionsById.size();
    }
}
