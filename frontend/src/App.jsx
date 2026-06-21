import { useState, useCallback, useRef } from 'react';
import { useWebSocket } from './hooks/useWebSocket';
import { useWebRTC } from './hooks/useWebRTC';
import { useSoundEffects } from './hooks/useSoundEffects';
import RoleSelect from './components/RoleSelect';
import WaitingScreen from './components/WaitingScreen';
import ChatScreen from './components/ChatScreen';
import FeedbackScreen from './components/FeedbackScreen';
import ConnectionOverlay from './components/ConnectionOverlay';

// Screen states
const SCREENS = {
  ROLE_SELECT: 'ROLE_SELECT',
  WAITING: 'WAITING',
  CHAT: 'CHAT',
  FEEDBACK: 'FEEDBACK',
};

function formatTime() {
  const now = new Date();
  return now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

export default function App() {
  const [screen, setScreen] = useState(SCREENS.ROLE_SELECT);
  const [selectedRole, setSelectedRole] = useState(null);
  const [messages, setMessages] = useState([]);
  const [partnerId, setPartnerId] = useState(null);
  const [sessionId, setSessionId] = useState(null);
  const [matchedAt, setMatchedAt] = useState(null);
  const [sessionDuration, setSessionDuration] = useState(0);

  // Ref to hold the WebRTC signaling handler (avoids circular dependency)
  const webrtcHandlerRef = useRef(null);

  // Sound effects
  const { playMatchSound, playMessageSound } = useSoundEffects();
  const soundsRef = useRef({ playMatchSound, playMessageSound });
  soundsRef.current = { playMatchSound, playMessageSound };

  // Handle incoming server events
  const handleServerEvent = useCallback((event) => {
    switch (event.type) {
      case 'CONNECTED':
        console.log('[App] Connected to server');
        break;

      case 'QUEUED':
        setScreen(SCREENS.WAITING);
        break;

      case 'MATCH_FOUND':
        setPartnerId(event.partnerId);
        setSessionId(event.sessionId);
        setMessages([]);
        setMatchedAt(Date.now());
        setScreen(SCREENS.CHAT);
        soundsRef.current.playMatchSound();
        break;

      case 'RECEIVE_MESSAGE':
        setMessages((prev) => [
          ...prev,
          { type: 'received', text: event.content, time: formatTime() },
        ]);
        soundsRef.current.playMessageSound();
        break;

      case 'SESSION_ENDED':
        transitionToFeedback();
        break;

      case 'PARTNER_LEFT':
        transitionToFeedback();
        break;

      case 'ERROR':
        console.error('[App] Server error:', event.message);
        setMessages((prev) => [
          ...prev,
          { type: 'system', text: `Error: ${event.message}` },
        ]);
        break;

      // WebRTC signaling events — forward to the WebRTC hook
      case 'WEBRTC_OFFER':
      case 'WEBRTC_ANSWER':
      case 'ICE_CANDIDATE':
      case 'PARTNER_VIDEO_READY':
      case 'PARTNER_VIDEO_ENDED':
        if (webrtcHandlerRef.current) {
          webrtcHandlerRef.current(event);
        }
        break;

      case 'QUEUE_LEFT':
        console.log('[App] Left queue');
        resetToRoleSelect();
        break;

      default:
        console.log('[App] Unhandled event:', event.type);
    }
  }, []);

  const { sendEvent, connectionStatus, reconnect } = useWebSocket(handleServerEvent);

  // WebRTC hook — uses sendEvent to relay signaling via WebSocket
  const {
    localStream,
    remoteStream,
    isVideoActive,
    isPartnerVideoActive,
    videoError,
    startVideo,
    stopVideo,
    handleSignalingEvent,
    cleanupConnection,
  } = useWebRTC(sendEvent);

  // Store the signaling handler in the ref so the event callback can access it
  webrtcHandlerRef.current = handleSignalingEvent;

  const transitionToFeedback = () => {
    cleanupConnection();
    // Calculate session duration
    if (matchedAt) {
      setSessionDuration(Math.floor((Date.now() - matchedAt) / 1000));
    }
    setScreen(SCREENS.FEEDBACK);
  };

  const resetToRoleSelect = () => {
    setScreen(SCREENS.ROLE_SELECT);
    setSelectedRole(null);
    setMessages([]);
    setPartnerId(null);
    setSessionId(null);
    setMatchedAt(null);
    setSessionDuration(0);
  };

  // ── Actions ──

  const handleJoinQueue = (role) => {
    setSelectedRole(role);
    sendEvent({ type: 'JOIN_QUEUE', role });
  };

  const handleCancelQueue = () => {
    sendEvent({ type: 'LEAVE_QUEUE' });
  };

  const handleSendMessage = (text) => {
    sendEvent({ type: 'SEND_MESSAGE', content: text });
    setMessages((prev) => [
      ...prev,
      { type: 'sent', text, time: formatTime() },
    ]);
  };

  const handleNextPartner = () => {
    // Clean up video before ending session
    cleanupConnection();
    sendEvent({ type: 'NEXT_USER' });
  };

  // ── Render ──

  return (
    <>
      {/* Connection overlay */}
      <ConnectionOverlay status={connectionStatus} onReconnect={reconnect} />

      {screen === SCREENS.ROLE_SELECT && (
        <RoleSelect onJoinQueue={handleJoinQueue} />
      )}

      {screen === SCREENS.WAITING && (
        <WaitingScreen role={selectedRole} onCancel={handleCancelQueue} />
      )}

      {screen === SCREENS.CHAT && (
        <ChatScreen
          role={selectedRole}
          partnerId={partnerId}
          sessionId={sessionId}
          messages={messages}
          onSendMessage={handleSendMessage}
          onNextPartner={handleNextPartner}
          matchedAt={matchedAt}
          // WebRTC props
          localStream={localStream}
          remoteStream={remoteStream}
          isVideoActive={isVideoActive}
          isPartnerVideoActive={isPartnerVideoActive}
          videoError={videoError}
          onStartVideo={startVideo}
          onStopVideo={stopVideo}
        />
      )}

      {screen === SCREENS.FEEDBACK && (
        <FeedbackScreen
          sessionId={sessionId}
          role={selectedRole}
          duration={sessionDuration}
          onComplete={resetToRoleSelect}
        />
      )}
    </>
  );
}
