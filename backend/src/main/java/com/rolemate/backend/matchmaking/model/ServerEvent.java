package com.rolemate.backend.matchmaking.model;

/**
 * Represents an outbound event sent from the server to a WebSocket client.
 * Supports matchmaking events and WebRTC signaling relay events.
 */
public class ServerEvent {

    private ServerEventType type;
    private String sessionId;
    private String partnerId;
    private String role;
    private String content;
    private String message;

    // WebRTC signaling fields
    private String sdp;
    private String candidate;
    private String sdpMid;
    private Integer sdpMLineIndex;

    public ServerEvent() {
    }

    public ServerEvent(ServerEventType type, String message) {
        this.type = type;
        this.message = message;
    }

    public ServerEventType getType() {
        return type;
    }

    public void setType(ServerEventType type) {
        this.type = type;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
