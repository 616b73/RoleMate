package com.rolemate.backend.signaling.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rolemate.backend.matchmaking.model.ClientEvent;
import com.rolemate.backend.matchmaking.model.MatchSession;
import com.rolemate.backend.matchmaking.model.ServerEvent;
import com.rolemate.backend.matchmaking.model.ServerEventType;
import com.rolemate.backend.matchmaking.model.UserConnection;
import com.rolemate.backend.matchmaking.service.SessionService;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * Handles WebRTC signaling by relaying SDP offers, SDP answers, and ICE candidates
 * between matched partners via the existing WebSocket connection.
 *
 * <p>This service does NOT handle media streams — those flow peer-to-peer
 * between browsers after the signaling handshake completes.
 */
@Service
public class SignalingService {

    private static final Logger log = LoggerFactory.getLogger(SignalingService.class);

    private final SessionService sessionService;
    private final ObjectMapper objectMapper;

    public SignalingService(SessionService sessionService, ObjectMapper objectMapper) {
        this.sessionService = sessionService;
        this.objectMapper = objectMapper;
    }

    /**
     * Relays a WebRTC offer from the sender to their matched partner.
     */
    public void relayOffer(WebSocketSession senderSession, ClientEvent event,
                           Map<String, UserConnection> connectedUsers) throws IOException {
        relaySignalingEvent(senderSession, connectedUsers, ServerEventType.WEBRTC_OFFER, event);
    }

    /**
     * Relays a WebRTC answer from the sender to their matched partner.
     */
    public void relayAnswer(WebSocketSession senderSession, ClientEvent event,
                            Map<String, UserConnection> connectedUsers) throws IOException {
        relaySignalingEvent(senderSession, connectedUsers, ServerEventType.WEBRTC_ANSWER, event);
    }

    /**
     * Relays an ICE candidate from the sender to their matched partner.
     */
    public void relayIceCandidate(WebSocketSession senderSession, ClientEvent event,
                                  Map<String, UserConnection> connectedUsers) throws IOException {
        relaySignalingEvent(senderSession, connectedUsers, ServerEventType.ICE_CANDIDATE, event);
    }

    /**
     * Notifies the partner that the sender's video is ready.
     */
    public void notifyVideoReady(WebSocketSession senderSession,
                                 Map<String, UserConnection> connectedUsers) throws IOException {
        relaySimpleEvent(senderSession, connectedUsers, ServerEventType.PARTNER_VIDEO_READY);
    }

    /**
     * Notifies the partner that the sender has ended their video.
     */
    public void notifyVideoEnded(WebSocketSession senderSession,
                                 Map<String, UserConnection> connectedUsers) throws IOException {
        relaySimpleEvent(senderSession, connectedUsers, ServerEventType.PARTNER_VIDEO_ENDED);
    }

    // ──────────────────────────────────────────────────────────────

    private void relaySignalingEvent(WebSocketSession senderSession,
                                     Map<String, UserConnection> connectedUsers,
                                     ServerEventType eventType,
                                     ClientEvent clientEvent) throws IOException {
        String senderId = senderSession.getId();
        Optional<MatchSession> sessionOpt = sessionService.getSessionForUser(senderId);

        if (sessionOpt.isEmpty()) {
            sendError(senderSession, "You are not currently matched — cannot relay " + eventType);
            return;
        }

        MatchSession matchSession = sessionOpt.get();
        String partnerId = matchSession.getPartnerId(senderId);
        UserConnection partner = connectedUsers.get(partnerId);

        if (partner == null || !partner.getSocketSession().isOpen()) {
            sendError(senderSession, "Partner is no longer connected");
            return;
        }

        ServerEvent relayEvent = new ServerEvent(eventType, null);
        relayEvent.setSessionId(matchSession.getSessionId());
        relayEvent.setPartnerId(senderId);
        relayEvent.setSdp(clientEvent.getSdp());
        relayEvent.setCandidate(clientEvent.getCandidate());
        relayEvent.setSdpMid(clientEvent.getSdpMid());
        relayEvent.setSdpMLineIndex(clientEvent.getSdpMLineIndex());

        sendEvent(partner.getSocketSession(), relayEvent);
        log.debug("Relayed {} from {} to {}", eventType, senderId, partnerId);
    }

    private void relaySimpleEvent(WebSocketSession senderSession,
                                  Map<String, UserConnection> connectedUsers,
                                  ServerEventType eventType) throws IOException {
        String senderId = senderSession.getId();
        Optional<MatchSession> sessionOpt = sessionService.getSessionForUser(senderId);

        if (sessionOpt.isEmpty()) {
            sendError(senderSession, "You are not currently matched");
            return;
        }

        MatchSession matchSession = sessionOpt.get();
        String partnerId = matchSession.getPartnerId(senderId);
        UserConnection partner = connectedUsers.get(partnerId);

        if (partner == null || !partner.getSocketSession().isOpen()) {
            sendError(senderSession, "Partner is no longer connected");
            return;
        }

        ServerEvent event = new ServerEvent(eventType, null);
        event.setSessionId(matchSession.getSessionId());
        event.setPartnerId(senderId);
        sendEvent(partner.getSocketSession(), event);
        log.debug("Relayed {} from {} to {}", eventType, senderId, partnerId);
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        sendEvent(session, new ServerEvent(ServerEventType.ERROR, message));
    }

    private void sendEvent(WebSocketSession session, ServerEvent event) throws IOException {
        if (!session.isOpen()) return;
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
    }
}
