-- Create exam_session_question table to track questions assigned to each session
-- This table ensures NO duplicate questions within a session via UNIQUE constraints

CREATE TABLE exam_session_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL,
    question_id BIGINT NOT NULL,
    position INT NOT NULL,
    served_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_session FOREIGN KEY (session_id) REFERENCES exam_session(id) ON DELETE CASCADE,
    CONSTRAINT fk_question FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE CASCADE,
    CONSTRAINT unique_session_question UNIQUE (session_id, question_id),
    CONSTRAINT unique_session_position UNIQUE (session_id, position)
);

-- Index for quick lookup by session and position (primary navigation pattern)
CREATE INDEX idx_session_position ON exam_session_question(session_id, position);

-- Index for quick lookup by session (for getting all questions in a session)
CREATE INDEX idx_session_id ON exam_session_question(session_id);
