import { useState, useRef, useEffect } from 'react';
import VideoPanel from './VideoPanel';
import './ChatScreen.css';

export default function ChatScreen({
  role,
  partnerId,
  sessionId,
  messages,
  onSendMessage,
  onNextPartner,
  // WebRTC props
  localStream,
  remoteStream,
  isVideoActive,
  isPartnerVideoActive,
  videoError,
  onStartVideo,
  onStopVideo,
}) {
  const [input, setInput] = useState('');
  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);

  // Auto-scroll to bottom when new messages arrive
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Focus input on mount
  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  const handleSend = (e) => {
    e.preventDefault();
    const trimmed = input.trim();
    if (!trimmed) return;

    onSendMessage(trimmed);
    setInput('');
  };

  const showVideoPanel = isVideoActive || isPartnerVideoActive || videoError;

  return (
    <div className="chat animate-in">
      {/* Header */}
      <div className="chat__header">
        <div className="chat__header-info">
          <span className="chat__role-badge">{role}</span>
          <span className="chat__status">
            <span className="chat__status-dot"></span>
            Connected
          </span>
        </div>
        <div className="chat__header-actions">
          <button
            className={`chat__video-btn ${isVideoActive ? 'chat__video-btn--active' : ''}`}
            onClick={isVideoActive ? onStopVideo : onStartVideo}
            id="video-toggle-btn"
          >
            {isVideoActive ? '🔴 Stop Video' : '📹 Start Video'}
          </button>
          <button
            className="chat__next-btn"
            onClick={onNextPartner}
            id="next-partner-btn"
          >
            Next Partner →
          </button>
        </div>
      </div>

      {/* Video Panel */}
      {showVideoPanel && (
        <VideoPanel
          localStream={localStream}
          remoteStream={remoteStream}
          isPartnerVideoActive={isPartnerVideoActive}
          videoError={videoError}
        />
      )}

      {/* Messages */}
      <div className="chat__messages" id="chat-messages">
        {messages.length === 0 && (
          <div className="chat__empty">
            <p>You're matched! Say hello 👋</p>
          </div>
        )}

        {messages.map((msg, index) => (
          <div
            key={index}
            className={`chat__msg ${msg.type === 'sent' ? 'chat__msg--sent' : ''}${msg.type === 'system' ? 'chat__msg--system' : ''}`}
          >
            {msg.type === 'system' ? (
              <span className="chat__msg-system">{msg.text}</span>
            ) : (
              <>
                <span className="chat__msg-text">{msg.text}</span>
                <span className="chat__msg-time">{msg.time}</span>
              </>
            )}
          </div>
        ))}
        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <form className="chat__input-bar" onSubmit={handleSend}>
        <input
          ref={inputRef}
          type="text"
          className="chat__input"
          placeholder="Type a message..."
          value={input}
          onChange={(e) => setInput(e.target.value)}
          id="chat-input"
          autoComplete="off"
        />
        <button
          type="submit"
          className="chat__send-btn"
          disabled={!input.trim()}
          id="send-btn"
        >
          Send
        </button>
      </form>
    </div>
  );
}
