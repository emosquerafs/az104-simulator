-- Create exam_session table to track unique sessions
-- Each session represents a single exam or practice attempt with a fixed set of questions

CREATE TABLE exam_session (
    id VARCHAR(36) PRIMARY KEY,
    mode VARCHAR(20) NOT NULL CHECK (mode IN ('EXAM', 'PRACTICE')),
    total_questions INT NOT NULL,
    locale VARCHAR(5) NOT NULL,
    seed INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL
);

-- Index for quick lookup of active sessions
CREATE INDEX idx_exam_session_created ON exam_session(created_at);

-- Index for filtering by mode
CREATE INDEX idx_exam_session_mode ON exam_session(mode);
