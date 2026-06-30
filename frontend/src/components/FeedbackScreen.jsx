import { useState } from 'react';
import './FeedbackScreen.css';

const API_BASE = '';

/**
 * Post-session feedback screen.
 * Shows after a session ends — user can rate 👍 or 👎 before returning to role selection.
 */
export default function FeedbackScreen({ sessionId, role, duration, onComplete }) {
  const [submitted, setSubmitted] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const formatDuration = (seconds) => {
    if (!seconds || seconds <= 0) return '0:00';
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${String(s).padStart(2, '0')}`;
  };

  const handleRate = async (rating) => {
    if (submitting) return;
    setSubmitting(true);

    try {
      await fetch(`${API_BASE}/api/feedback`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sessionId, rating }),
      });
    } catch (err) {
      console.warn('[Feedback] Failed to submit:', err);
    }

    setSubmitted(true);
    setTimeout(() => onComplete(), 1200);
  };

  const handleSkip = () => {
    onComplete();
  };

  return (
    <div className="feedback animate-in">
      <div className="feedback__card">
        {!submitted ? (
          <>
            <h2 className="feedback__title">Session Complete</h2>

            <div className="feedback__meta">
              <span className="feedback__role-badge">{role}</span>
              {duration > 0 && (
                <span className="feedback__duration">
                  🕐 {formatDuration(duration)}
                </span>
              )}
            </div>

            <p className="feedback__prompt">How was your session?</p>

            <div className="feedback__actions">
              <button
                className="feedback__btn feedback__btn--good"
                onClick={() => handleRate('GOOD')}
                disabled={submitting}
                id="feedback-good-btn"
              >
                <span className="feedback__btn-emoji">👍</span>
                <span>Good</span>
              </button>
              <button
                className="feedback__btn feedback__btn--bad"
                onClick={() => handleRate('BAD')}
                disabled={submitting}
                id="feedback-bad-btn"
              >
                <span className="feedback__btn-emoji">👎</span>
                <span>Bad</span>
              </button>
            </div>

            <button
              className="feedback__skip"
              onClick={handleSkip}
              disabled={submitting}
              id="feedback-skip-btn"
            >
              Skip
            </button>
          </>
        ) : (
          <div className="feedback__thanks">
            <span className="feedback__thanks-icon">✅</span>
            <p>Thanks for your feedback!</p>
          </div>
        )}
      </div>
    </div>
  );
}
