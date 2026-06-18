package com.rolemate.backend.matchmaking.model;

public enum ClientEventType {
    // Matchmaking
    JOIN_QUEUE,
    LEAVE_QUEUE,
    SEND_MESSAGE,
    NEXT_USER,

    // WebRTC signaling
    WEBRTC_OFFER,
    WEBRTC_ANSWER,
    ICE_CANDIDATE,
    VIDEO_READY,
    VIDEO_ENDED
}
