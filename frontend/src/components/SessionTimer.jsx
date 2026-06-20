import { useState, useEffect } from 'react';
import './SessionTimer.css';

/**
 * Displays elapsed session time in MM:SS format.
 * Counts up from 00:00 starting from the matchedAt timestamp.
 */
export default function SessionTimer({ matchedAt }) {
  const [elapsed, setElapsed] = useState(0);

  useEffect(() => {
    if (!matchedAt) return;

    // Calculate initial elapsed time (in case component mounts late)
    setElapsed(Math.floor((Date.now() - matchedAt) / 1000));

    const interval = setInterval(() => {
      setElapsed(Math.floor((Date.now() - matchedAt) / 1000));
    }, 1000);

    return () => clearInterval(interval);
  }, [matchedAt]);

  const minutes = String(Math.floor(elapsed / 60)).padStart(2, '0');
  const seconds = String(elapsed % 60).padStart(2, '0');

  return (
    <span className="session-timer" id="session-timer">
      🕐 {minutes}:{seconds}
    </span>
  );
}
