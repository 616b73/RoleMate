import { useRef, useState, useCallback, useEffect } from 'react';

const ICE_SERVERS = {
  iceServers: [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' },
  ],
};

/**
 * Custom hook managing the WebRTC peer connection lifecycle.
 *
 * Responsibilities:
 * - getUserMedia for camera/mic
 * - RTCPeerConnection setup and teardown
 * - SDP offer/answer creation and exchange
 * - ICE candidate exchange
 * - Provides local/remote streams and state to the UI
 *
 * @param {function} sendEvent - sends a WebSocket event to the backend
 * @returns {object} WebRTC state and control functions
 */
export function useWebRTC(sendEvent) {
  const peerConnectionRef = useRef(null);
  const localStreamRef = useRef(null);
  const pendingCandidatesRef = useRef([]);

  const [localStream, setLocalStream] = useState(null);
  const [remoteStream, setRemoteStream] = useState(null);
  const [isVideoActive, setIsVideoActive] = useState(false);
  const [isPartnerVideoActive, setIsPartnerVideoActive] = useState(false);
  const [videoError, setVideoError] = useState(null);

  /**
   * Creates a new RTCPeerConnection with event handlers wired up.
   */
  const createPeerConnection = useCallback(() => {
    if (peerConnectionRef.current) {
      peerConnectionRef.current.close();
    }

    const pc = new RTCPeerConnection(ICE_SERVERS);

    // Send ICE candidates to the partner via WebSocket
    pc.onicecandidate = (event) => {
      if (event.candidate) {
        sendEvent({
          type: 'ICE_CANDIDATE',
          candidate: event.candidate.candidate,
          sdpMid: event.candidate.sdpMid,
          sdpMLineIndex: event.candidate.sdpMLineIndex,
        });
      }
    };

    pc.oniceconnectionstatechange = () => {
      console.log('[WebRTC] ICE connection state:', pc.iceConnectionState);
      if (pc.iceConnectionState === 'failed' || pc.iceConnectionState === 'disconnected') {
        console.warn('[WebRTC] ICE connection failed or disconnected');
      }
    };

    // Receive remote tracks
    pc.ontrack = (event) => {
      console.log('[WebRTC] Remote track received:', event.track.kind);
      const [stream] = event.streams;
      if (stream) {
        setRemoteStream(stream);
        setIsPartnerVideoActive(true);
      }
    };

    peerConnectionRef.current = pc;
    return pc;
  }, [sendEvent]);

  /**
   * Acquires the local media stream (camera + mic).
   */
  const acquireLocalStream = useCallback(async () => {
    try {
      setVideoError(null);
      const stream = await navigator.mediaDevices.getUserMedia({
        video: true,
        audio: true,
      });
      localStreamRef.current = stream;
      setLocalStream(stream);
      return stream;
    } catch (err) {
      console.error('[WebRTC] Failed to get user media:', err);
      if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
        setVideoError('Camera permission denied. Please allow camera access and try again.');
      } else if (err.name === 'NotFoundError') {
        setVideoError('No camera or microphone found on this device.');
      } else {
        setVideoError(`Failed to access camera: ${err.message}`);
      }
      return null;
    }
  }, []);

  /**
   * Starts a video call — called when the user clicks "Start Video".
   * This side creates the SDP offer (is the "caller").
   */
  const startVideo = useCallback(async () => {
    const stream = await acquireLocalStream();
    if (!stream) return;

    const pc = createPeerConnection();

    // Add local tracks to peer connection
    stream.getTracks().forEach((track) => {
      pc.addTrack(track, stream);
    });

    // Create and send offer
    try {
      const offer = await pc.createOffer();
      await pc.setLocalDescription(offer);

      sendEvent({
        type: 'WEBRTC_OFFER',
        sdp: offer.sdp,
      });

      setIsVideoActive(true);
      sendEvent({ type: 'VIDEO_READY' });
      console.log('[WebRTC] Offer sent');
    } catch (err) {
      console.error('[WebRTC] Failed to create offer:', err);
      setVideoError('Failed to start video call.');
      cleanupConnection();
    }
  }, [acquireLocalStream, createPeerConnection, sendEvent]);

  /**
   * Stops the video call — called when the user clicks "Stop Video".
   */
  const stopVideo = useCallback(() => {
    cleanupConnection();
    sendEvent({ type: 'VIDEO_ENDED' });
  }, [sendEvent]);

  /**
   * Handles an incoming WebRTC signaling event from the partner.
   * Called by App.jsx when it receives WEBRTC_OFFER, WEBRTC_ANSWER, or ICE_CANDIDATE.
   */
  const handleSignalingEvent = useCallback(async (event) => {
    switch (event.type) {
      case 'WEBRTC_OFFER':
        await handleOffer(event);
        break;
      case 'WEBRTC_ANSWER':
        await handleAnswer(event);
        break;
      case 'ICE_CANDIDATE':
        await handleIceCandidate(event);
        break;
      case 'PARTNER_VIDEO_READY':
        setIsPartnerVideoActive(true);
        break;
      case 'PARTNER_VIDEO_ENDED':
        handlePartnerVideoEnded();
        break;
      default:
        break;
    }
  }, []);

  /**
   * Handles an incoming SDP offer — this side is the "callee".
   * Acquires local media, sets the remote description, and sends an answer.
   */
  const handleOffer = async (event) => {
    console.log('[WebRTC] Received offer');

    const stream = await acquireLocalStream();
    if (!stream) return;

    const pc = createPeerConnection();

    // Add local tracks
    stream.getTracks().forEach((track) => {
      pc.addTrack(track, stream);
    });

    try {
      await pc.setRemoteDescription(new RTCSessionDescription({
        type: 'offer',
        sdp: event.sdp,
      }));

      // Apply any ICE candidates that arrived before the offer
      for (const candidate of pendingCandidatesRef.current) {
        await pc.addIceCandidate(candidate);
      }
      pendingCandidatesRef.current = [];

      const answer = await pc.createAnswer();
      await pc.setLocalDescription(answer);

      sendEvent({
        type: 'WEBRTC_ANSWER',
        sdp: answer.sdp,
      });

      setIsVideoActive(true);
      sendEvent({ type: 'VIDEO_READY' });
      console.log('[WebRTC] Answer sent');
    } catch (err) {
      console.error('[WebRTC] Failed to handle offer:', err);
      setVideoError('Failed to connect video call.');
      cleanupConnection();
    }
  };

  /**
   * Handles an incoming SDP answer.
   */
  const handleAnswer = async (event) => {
    console.log('[WebRTC] Received answer');
    const pc = peerConnectionRef.current;
    if (!pc) return;

    try {
      await pc.setRemoteDescription(new RTCSessionDescription({
        type: 'answer',
        sdp: event.sdp,
      }));

      // Apply any buffered ICE candidates
      for (const candidate of pendingCandidatesRef.current) {
        await pc.addIceCandidate(candidate);
      }
      pendingCandidatesRef.current = [];
    } catch (err) {
      console.error('[WebRTC] Failed to set remote description:', err);
    }
  };

  /**
   * Handles an incoming ICE candidate.
   * Buffers candidates if the remote description hasn't been set yet.
   */
  const handleIceCandidate = async (event) => {
    const candidate = new RTCIceCandidate({
      candidate: event.candidate,
      sdpMid: event.sdpMid,
      sdpMLineIndex: event.sdpMLineIndex,
    });

    const pc = peerConnectionRef.current;
    if (pc && pc.remoteDescription) {
      try {
        await pc.addIceCandidate(candidate);
      } catch (err) {
        console.error('[WebRTC] Failed to add ICE candidate:', err);
      }
    } else {
      // Buffer until remote description is set
      pendingCandidatesRef.current.push(candidate);
    }
  };

  /**
   * Handles partner stopping their video.
   */
  const handlePartnerVideoEnded = () => {
    setIsPartnerVideoActive(false);
    setRemoteStream(null);
  };

  /**
   * Cleans up all WebRTC resources.
   */
  const cleanupConnection = useCallback(() => {
    // Stop local media tracks
    if (localStreamRef.current) {
      localStreamRef.current.getTracks().forEach((track) => track.stop());
      localStreamRef.current = null;
    }

    // Close peer connection
    if (peerConnectionRef.current) {
      peerConnectionRef.current.close();
      peerConnectionRef.current = null;
    }

    pendingCandidatesRef.current = [];
    setLocalStream(null);
    setRemoteStream(null);
    setIsVideoActive(false);
    setIsPartnerVideoActive(false);
    setVideoError(null);
  }, []);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (localStreamRef.current) {
        localStreamRef.current.getTracks().forEach((track) => track.stop());
      }
      if (peerConnectionRef.current) {
        peerConnectionRef.current.close();
      }
    };
  }, []);

  return {
    localStream,
    remoteStream,
    isVideoActive,
    isPartnerVideoActive,
    videoError,
    startVideo,
    stopVideo,
    handleSignalingEvent,
    cleanupConnection,
  };
}
