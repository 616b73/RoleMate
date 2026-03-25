package com.rolemate.backend.matchmaking.model;

public enum ServerEventType {
    CONNECTED,
    QUEUED,
    MATCH_FOUND,
    RECEIVE_MESSAGE,
    SESSION_ENDED,
    PARTNER_LEFT,
    ERROR
}
