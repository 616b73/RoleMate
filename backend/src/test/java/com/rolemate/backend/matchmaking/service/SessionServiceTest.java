package com.rolemate.backend.matchmaking.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.rolemate.backend.matchmaking.model.MatchSession;
import com.rolemate.backend.matchmaking.model.UserConnection;
import com.rolemate.backend.persistence.service.PersistenceService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

/**
 * Unit tests for {@link SessionService}.
 */
class SessionServiceTest {

    private SessionService sessionService;
    private PersistenceService persistenceService;

    @BeforeEach
    void setUp() {
        persistenceService = mock(PersistenceService.class);
        sessionService = new SessionService(persistenceService);
    }

    private UserConnection createMockUser(String id) {
        WebSocketSession ws = mock(WebSocketSession.class);
        when(ws.getId()).thenReturn(id);
        when(ws.isOpen()).thenReturn(true);
        return new UserConnection(id, ws);
    }

    @Test
    void createSession_storesSessionAndReturnsIt() {
        UserConnection userA = createMockUser("a1");
        UserConnection userB = createMockUser("b1");

        MatchSession session = sessionService.createSession(userA, userB, "Backend Engineering");

        assertNotNull(session);
        assertNotNull(session.getSessionId());
        assertEquals("Backend Engineering", session.getRole());
        assertEquals("a1", session.getUserAId());
        assertEquals("b1", session.getUserBId());
        assertEquals(MatchSession.Status.ACTIVE, session.getStatus());
        verify(persistenceService).persistSessionCreated(session);
    }

    @Test
    void getSessionForUser_returnsSession_whenActive() {
        UserConnection userA = createMockUser("a1");
        UserConnection userB = createMockUser("b1");
        sessionService.createSession(userA, userB, "Data Science");

        Optional<MatchSession> sessionA = sessionService.getSessionForUser("a1");
        Optional<MatchSession> sessionB = sessionService.getSessionForUser("b1");

        assertTrue(sessionA.isPresent());
        assertTrue(sessionB.isPresent());
        assertEquals(sessionA.get().getSessionId(), sessionB.get().getSessionId());
    }

    @Test
    void getSessionForUser_returnsEmpty_whenNoSession() {
        assertTrue(sessionService.getSessionForUser("unknown").isEmpty());
    }

    @Test
    void isInSession_returnsTrue_whenActive() {
        UserConnection userA = createMockUser("a1");
        UserConnection userB = createMockUser("b1");
        sessionService.createSession(userA, userB, "DevOps");

        assertTrue(sessionService.isInSession("a1"));
        assertTrue(sessionService.isInSession("b1"));
    }

    @Test
    void isInSession_returnsFalse_whenNoSession() {
        assertFalse(sessionService.isInSession("unknown"));
    }

    @Test
    void endSessionForUser_removesSession_andMarksEnded() {
        UserConnection userA = createMockUser("a1");
        UserConnection userB = createMockUser("b1");
        sessionService.createSession(userA, userB, "Frontend Engineering");

        Optional<MatchSession> ended = sessionService.endSessionForUser("a1");

        assertTrue(ended.isPresent());
        assertEquals(MatchSession.Status.ENDED, ended.get().getStatus());
        assertNotNull(ended.get().getEndedAt());
        assertFalse(sessionService.isInSession("a1"));
        assertFalse(sessionService.isInSession("b1"));
        verify(persistenceService).persistSessionEnded(ended.get());
    }

    @Test
    void endSessionForUser_returnsEmpty_whenNoSession() {
        assertTrue(sessionService.endSessionForUser("unknown").isEmpty());
    }

    @Test
    void endSessionForUser_twice_secondCallReturnsEmpty() {
        UserConnection userA = createMockUser("a1");
        UserConnection userB = createMockUser("b1");
        sessionService.createSession(userA, userB, "Data Science");

        assertTrue(sessionService.endSessionForUser("a1").isPresent());
        assertTrue(sessionService.endSessionForUser("a1").isEmpty());
    }

    @Test
    void getActiveSessionCount_reflectsState() {
        assertEquals(0, sessionService.getActiveSessionCount());

        UserConnection userA = createMockUser("a1");
        UserConnection userB = createMockUser("b1");
        sessionService.createSession(userA, userB, "DevOps");
        assertEquals(1, sessionService.getActiveSessionCount());

        UserConnection userC = createMockUser("c1");
        UserConnection userD = createMockUser("d1");
        sessionService.createSession(userC, userD, "Data Science");
        assertEquals(2, sessionService.getActiveSessionCount());

        sessionService.endSessionForUser("a1");
        assertEquals(1, sessionService.getActiveSessionCount());
    }

    @Test
    void createSession_partnerLookup_isCorrect() {
        UserConnection userA = createMockUser("a1");
        UserConnection userB = createMockUser("b1");
        MatchSession session = sessionService.createSession(userA, userB, "Full Stack Development");

        assertEquals("b1", session.getPartnerId("a1"));
        assertEquals("a1", session.getPartnerId("b1"));
    }
}
