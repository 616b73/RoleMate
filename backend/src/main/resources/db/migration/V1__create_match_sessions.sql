-- RoleMate Phase 2: Session metadata persistence
-- This table stores completed and active match session records.
-- Chat messages are NOT persisted (ephemeral by design).

CREATE TABLE IF NOT EXISTS match_sessions (
    id                  VARCHAR(36)  PRIMARY KEY,
    role                VARCHAR(50)  NOT NULL,
    user_a_id           VARCHAR(100) NOT NULL,
    user_b_id           VARCHAR(100) NOT NULL,
    session_type        VARCHAR(20)  NOT NULL DEFAULT 'TEXT',
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMP    NOT NULL,
    ended_at            TIMESTAMP,
    duration_seconds    BIGINT
);

-- Indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_sessions_role ON match_sessions(role);
CREATE INDEX IF NOT EXISTS idx_sessions_status ON match_sessions(status);
CREATE INDEX IF NOT EXISTS idx_sessions_created_at ON match_sessions(created_at);
