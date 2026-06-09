package com.rolemate.backend.matchmaking.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;

/**
 * Represents an inbound event sent by a WebSocket client.
 * Supports matchmaking events (JOIN_QUEUE, SEND_MESSAGE, NEXT_USER)
 * and WebRTC signaling events (WEBRTC_OFFER, WEBRTC_ANSWER, ICE_CANDIDATE, etc).
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class ClientEvent {

    @NotNull(message = "Event type is required")
    private ClientEventType type;

    // Matchmaking fields
    private String role;
    private String content;

    // WebRTC signaling fields
    private String sdp;
    private String candidate;
    private String sdpMid;
    private Integer sdpMLineIndex;

    public ClientEventType getType() {
        return type;
    }

    public void setType(ClientEventType type) {
        this.type = type;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSdp() {
        return sdp;
    }

    public void setSdp(String sdp) {
        this.sdp = sdp;
    }

    public String getCandidate() {
        return candidate;
    }

    public void setCandidate(String candidate) {
        this.candidate = candidate;
    }

    public String getSdpMid() {
        return sdpMid;
    }

    public void setSdpMid(String sdpMid) {
        this.sdpMid = sdpMid;
    }

    public Integer getSdpMLineIndex() {
        return sdpMLineIndex;
    }

    public void setSdpMLineIndex(Integer sdpMLineIndex) {
        this.sdpMLineIndex = sdpMLineIndex;
    }
}
