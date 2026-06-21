import { useState, useEffect } from 'react';
import './ConnectionOverlay.css';

/**
 * Full-screen overlay shown when the WebSocket connection is lost.
 * Shows auto-reconnect countdown and a manual reconnect button.
 */
export default function ConnectionOverlay({ status, onReconnect }) {
  const [countdown, setCountdown] = useState(3);

  // Reset countdown when status changes
  useEffect(() => {
    if (status !== 'disconnected') return;

    setCountdown(3);
    const interval = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [status]);

  if (status === 'connected') return null;

  return (
    <div className="connection-overlay" id="connection-overlay">
      <div className="connection-overlay__card">
        <div className="connection-overlay__icon">
          {status === 'connecting' ? '🔄' : '⚡'}
        </div>

        <h2 className="connection-overlay__title">
          {status === 'connecting' ? 'Connecting...' : 'Connection Lost'}
        </h2>

        <p className="connection-overlay__text">
          {status === 'connecting'
            ? 'Establishing connection to the server...'
            : countdown > 0
              ? `Reconnecting in ${countdown}s...`
              : 'Attempting to reconnect...'}
        </p>

        {status === 'disconnected' && (
          <button
            className="connection-overlay__btn"
            onClick={onReconnect}
            id="reconnect-btn"
          >
            Reconnect Now
          </button>
        )}
      </div>
    </div>
  );
}
