package com.rolemate.backend.matchmaking.model;

public enum ServerEventType {
    // Matchmaking
    CONNECTED,
    QUEUED,
    QUEUE_LEFT,
    MATCH_FOUND,
    RECEIVE_MESSAGE,
    SESSION_ENDED,
    PARTNER_LEFT,
    ERROR,

    // WebRTC signaling (relayed to partner)
    WEBRTC_OFFER,
    WEBRTC_ANSWER,
    ICE_CANDIDATE,
    PARTNER_VIDEO_READY,
    PARTNER_VIDEO_ENDED
}
