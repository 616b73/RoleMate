import { useState, useCallback, useRef } from 'react';
import { useWebSocket } from './hooks/useWebSocket';
import { useWebRTC } from './hooks/useWebRTC';
import RoleSelect from './components/RoleSelect';
import WaitingScreen from './components/WaitingScreen';
import ChatScreen from './components/ChatScreen';

// Screen states
const SCREENS = {
  ROLE_SELECT: 'ROLE_SELECT',
  WAITING: 'WAITING',
  CHAT: 'CHAT',
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

  // Ref to hold the WebRTC signaling handler (avoids circular dependency)
  const webrtcHandlerRef = useRef(null);

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
        setScreen(SCREENS.CHAT);
        break;

      case 'RECEIVE_MESSAGE':
        setMessages((prev) => [
          ...prev,
          { type: 'received', text: event.content, time: formatTime() },
        ]);
        break;

      case 'SESSION_ENDED':
        setMessages((prev) => [
          ...prev,
          { type: 'system', text: 'Session ended. Returning to role selection...' },
        ]);
        setTimeout(() => {
          resetToRoleSelect();
        }, 1500);
        break;

      case 'PARTNER_LEFT':
        setMessages((prev) => [
          ...prev,
          { type: 'system', text: 'Your partner has left. Returning to role selection...' },
        ]);
        setTimeout(() => {
          resetToRoleSelect();
        }, 2000);
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

      default:
        console.log('[App] Unhandled event:', event.type);
    }
  }, []);

  const { sendEvent, connectionStatus } = useWebSocket(handleServerEvent);

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

  const resetToRoleSelect = () => {
    // Clean up WebRTC before resetting
    cleanupConnection();
    setScreen(SCREENS.ROLE_SELECT);
    setSelectedRole(null);
    setMessages([]);
    setPartnerId(null);
    setSessionId(null);
  };

  // ── Actions ──

  const handleJoinQueue = (role) => {
    setSelectedRole(role);
    sendEvent({ type: 'JOIN_QUEUE', role });
  };

  const handleCancelQueue = () => {
    resetToRoleSelect();
    // Reconnect to get a fresh session since there's no LEAVE_QUEUE event
    window.location.reload();
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
      {/* Connection indicator */}
      {connectionStatus !== 'connected' && (
        <div className="connection-bar" id="connection-status">
          {connectionStatus === 'connecting' ? 'Connecting...' : 'Disconnected — reconnecting...'}
        </div>
      )}

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
    </>
  );
}
