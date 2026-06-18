import { useRef, useCallback, useEffect, useState } from 'react';

const WS_URL = 'ws://localhost:8080/ws/matchmaking';
const RECONNECT_DELAY = 3000;

/**
 * Custom hook managing the WebSocket connection to the RoleMate backend.
 * Handles connection, reconnection, event parsing, and provides a send function.
 *
 * @param {function} onEvent - callback invoked with each parsed server event
 * @returns {{ sendEvent, connectionStatus }}
 */
export function useWebSocket(onEvent) {
  const wsRef = useRef(null);
  const reconnectTimer = useRef(null);
  const onEventRef = useRef(onEvent);
  const [connectionStatus, setConnectionStatus] = useState('disconnected');

  // Keep the callback ref current without re-triggering the effect
  useEffect(() => {
    onEventRef.current = onEvent;
  }, [onEvent]);

  const connect = useCallback(() => {
    // Clean up any existing connection
    if (wsRef.current) {
      wsRef.current.close();
    }

    setConnectionStatus('connecting');
    const ws = new WebSocket(WS_URL);

    ws.onopen = () => {
      console.log('[WS] Connected');
      setConnectionStatus('connected');
    };

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        onEventRef.current(data);
      } catch (err) {
        console.error('[WS] Failed to parse message:', err);
      }
    };

    ws.onclose = (event) => {
      console.log('[WS] Disconnected:', event.code, event.reason);
      setConnectionStatus('disconnected');
      wsRef.current = null;

      // Auto-reconnect unless it was a clean close
      if (event.code !== 1000) {
        reconnectTimer.current = setTimeout(() => {
          console.log('[WS] Attempting reconnect...');
          connect();
        }, RECONNECT_DELAY);
      }
    };

    ws.onerror = (error) => {
      console.error('[WS] Error:', error);
    };

    wsRef.current = ws;
  }, []);

  // Connect on mount, cleanup on unmount
  useEffect(() => {
    connect();

    return () => {
      clearTimeout(reconnectTimer.current);
      if (wsRef.current) {
        wsRef.current.close(1000, 'Component unmounted');
      }
    };
  }, [connect]);

  const sendEvent = useCallback((event) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(event));
    } else {
      console.warn('[WS] Cannot send — not connected');
    }
  }, []);

  const reconnect = useCallback(() => {
    clearTimeout(reconnectTimer.current);
    connect();
  }, [connect]);

  return { sendEvent, connectionStatus, reconnect };
}
