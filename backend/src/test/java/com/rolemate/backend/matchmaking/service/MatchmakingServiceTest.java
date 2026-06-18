package com.rolemate.backend.matchmaking.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rolemate.backend.config.JacksonConfig;
import com.rolemate.backend.persistence.service.PersistenceService;
import com.rolemate.backend.signaling.service.SignalingService;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class MatchmakingServiceTest {

    private ObjectMapper objectMapper;
    private SessionService sessionService;
    private MatchmakingService matchmakingService;

    @BeforeEach
    void setUp() {
        objectMapper = new JacksonConfig().objectMapper();
        PersistenceService persistenceService = mock(PersistenceService.class);
        sessionService = new SessionService(persistenceService);
        SignalingService signalingService = new SignalingService(sessionService, objectMapper);
        matchmakingService = new MatchmakingService(objectMapper, sessionService, signalingService);
    }

    private WebSocketSession createMockSession(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    @Test
    void registerConnection_sendsConnectedEvent() throws IOException {
        WebSocketSession session = createMockSession("user1");
        matchmakingService.registerConnection(session);
        verify(session).sendMessage(argThat(msg ->
                ((TextMessage) msg).getPayload().contains("\"type\":\"CONNECTED\"")));
    }

    @Test
    void joinQueue_validRole_sendsQueuedEvent() throws IOException {
        WebSocketSession session = createMockSession("user1");
        matchmakingService.registerConnection(session);
        matchmakingService.handleEvent(session, "{\"type\":\"JOIN_QUEUE\",\"role\":\"Backend Engineering\"}");
        verify(session).sendMessage(argThat(msg ->
                ((TextMessage) msg).getPayload().contains("\"type\":\"QUEUED\"")));
    }

    @Test
    void joinQueue_invalidRole_sendsError() throws IOException {
        WebSocketSession session = createMockSession("user1");
        matchmakingService.registerConnection(session);
        matchmakingService.handleEvent(session, "{\"type\":\"JOIN_QUEUE\",\"role\":\"Unknown Role\"}");
        verify(session).sendMessage(argThat(msg ->
                ((TextMessage) msg).getPayload().contains("\"type\":\"ERROR\"")));
    }

    @Test
    void twoUsers_sameRole_getMatched() throws IOException {
        WebSocketSession s1 = createMockSession("u1");
        WebSocketSession s2 = createMockSession("u2");
        matchmakingService.registerConnection(s1);
        matchmakingService.registerConnection(s2);
        matchmakingService.handleEvent(s1, "{\"type\":\"JOIN_QUEUE\",\"role\":\"Backend Engineering\"}");
        matchmakingService.handleEvent(s2, "{\"type\":\"JOIN_QUEUE\",\"role\":\"Backend Engineering\"}");
        verify(s1).sendMessage(argThat(msg ->
                ((TextMessage) msg).getPayload().contains("\"type\":\"MATCH_FOUND\"")));
        verify(s2).sendMessage(argThat(msg ->
                ((TextMessage) msg).getPayload().contains("\"type\":\"MATCH_FOUND\"")));
    }

    @Test
    void twoUsers_differentRoles_noMatch() throws IOException {
        WebSocketSession s1 = createMockSession("u1");
        WebSocketSession s2 = createMockSession("u2");
        matchmakingService.registerConnection(s1);
        matchmakingService.registerConnection(s2);
        matchmakingService.handleEvent(s1, "{\"type\":\"JOIN_QUEUE\",\"role\":\"Backend Engineering\"}");
        matchmakingService.handleEvent(s2, "{\"type\":\"JOIN_QUEUE\",\"role\":\"Frontend Engineering\"}");
        verify(s1, never()).sendMessage(argThat(msg ->
                ((TextMessage) msg).getPayload().contains("\"type\":\"MATCH_FOUND\"")));
    }

    @Test
    void sendMessage_whenMatched_partnerReceivesIt() throws IOException {
        WebSocketSession s1 = createMockSession("u1");
        WebSocketSession s2 = createMockSession("u2");
        matchmakingService.registerConnection(s1);
        matchmakingService.registerConnection(s2);
        matchmakingService.handleEvent(s1, "{\"type\":\"JOIN_QUEUE\",\"role\":\"Data Science\"}");
        matchmakingService.handleEvent(s2, "{\"type\":\"JOIN_QUEUE\",\"role\":\"Data Science\"}");
        matchmakingService.handleEvent(s1, "{\"type\":\"SEND_MESSAGE\",\"content\":\"Hello!\"}");
        verify(s2).sendMessage(argThat(msg ->
                ((TextMessage) msg).getPayload().contains("\"type\":\"RECEIVE_MESSAGE\"")));
    }

    @Test
    void sendMessage_whenNotMatched_receivesError() throws IOException {
        WebSocketSession s = createMockSession("u1");
        matchmakingService.registerConnection(s);
        matchmakingService.handleEvent(s, "{\"type\":\"SEND_MESSAGE\",\"content\":\"Hello!\"}");
        verify(s).sendMessage(argThat(msg ->
                ((TextMessage) msg).getPayload().contains("\"type\":\"ERROR\"")));
    }

    @Test
    void nextUser_endsSession_andRequeues() throws IOException {
        WebSocketSession s1 = createMockSession("u1");
        WebSocketSession s2 = createMockSession("u2");
        matchmakingService.registerConnection(s1);
        matchmakingService.registerConnection(s2);
        matchmakingService.handleEvent(s1, "{\"type\":\"JOIN_QUEUE\",\"role\":\"Backend Engineering\"}");
        matchmakingService.handleEvent(s2, "{\"type\":\"JOIN_QUEUE\",\"role\":\"Backend Engineering\"}");
        matchmakingService.handleEvent(s1, "{\"type\":\"NEXT_USER\"}");
        verify(s1).sendMessage(argThat(msg ->
                ((TextMessage) msg).getPayload().contains("\"type\":\"SESSION_ENDED\"")));
        verify(s2).sendMessage(argThat(msg ->
                ((TextMessage) msg).getPayload().contains("\"type\":\"PARTNER_LEFT\"")));
    }

    @Test
    void disconnect_whenMatched_partnerReceivesPartnerLeft() throws IOException {
        WebSocketSession s1 = createMockSession("u1");
        WebSocketSession s2 = createMockSession("u2");
        matchmakingService.registerConnection(s1);
        matchmakingService.registerConnection(s2);
        matchmakingService.handleEvent(s1, "{\"type\":\"JOIN_QUEUE\",\"role\":\"UI/UX Design\"}");
        matchmakingService.handleEvent(s2, "{\"type\":\"JOIN_QUEUE\",\"role\":\"UI/UX Design\"}");
        matchmakingService.handleDisconnect(s1);
        verify(s2).sendMessage(argThat(msg ->
                ((TextMessage) msg).getPayload().contains("\"type\":\"PARTNER_LEFT\"")));
    }

    @Test
    void queueStatus_returnsAllRoles() {
        Map<String, Integer> status = matchmakingService.getQueueStatus();
        assertEquals(6, status.size());
        assertTrue(status.containsKey("Backend Engineering"));
    }

    @Test
    void connectedUserCount_reflectsRegistrations() throws IOException {
        assertEquals(0, matchmakingService.getConnectedUserCount());
        matchmakingService.registerConnection(createMockSession("u1"));
        assertEquals(1, matchmakingService.getConnectedUserCount());
    }

    @Test
    void leaveQueue_removesFromQueue_sendsQueueLeft() throws IOException {
        WebSocketSession s1 = createMockSession("u1");
        WebSocketSession s2 = createMockSession("u2");
        matchmakingService.registerConnection(s1);
        matchmakingService.registerConnection(s2);

        // u1 joins queue
        matchmakingService.handleEvent(s1, "{\"type\":\"JOIN_QUEUE\",\"role\":\"Backend Engineering\"}");
        verify(s1).sendMessage(argThat(msg ->
                ((TextMessage) msg).getPayload().contains("\"type\":\"QUEUED\"")));

        // u1 leaves queue
        matchmakingService.handleEvent(s1, "{\"type\":\"LEAVE_QUEUE\"}");
        verify(s1).sendMessage(argThat(msg ->
                ((TextMessage) msg).getPayload().contains("\"type\":\"QUEUE_LEFT\"")));

        // u2 joins same role — should NOT match (u1 already left)
        matchmakingService.handleEvent(s2, "{\"type\":\"JOIN_QUEUE\",\"role\":\"Backend Engineering\"}");
        verify(s1, never()).sendMessage(argThat(msg ->
                ((TextMessage) msg).getPayload().contains("\"type\":\"MATCH_FOUND\"")));
        verify(s2, never()).sendMessage(argThat(msg ->
                ((TextMessage) msg).getPayload().contains("\"type\":\"MATCH_FOUND\"")));

        // u1 is still connected (no disconnect)
        assertEquals(2, matchmakingService.getConnectedUserCount());
    }
}
