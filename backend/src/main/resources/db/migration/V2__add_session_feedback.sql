-- RoleMate Phase 4: Session feedback
-- Stores user feedback (thumbs up/down) after each session ends.

CREATE TABLE IF NOT EXISTS session_feedback (
    id              SERIAL       PRIMARY KEY,
    session_id      VARCHAR(36)  NOT NULL REFERENCES match_sessions(id),
    user_id         VARCHAR(100) NOT NULL,
    rating          VARCHAR(10)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_feedback_session ON session_feedback(session_id);
