-- Add session_id column to attempt table to link with exam_session
ALTER TABLE attempt ADD COLUMN session_id VARCHAR(36);

-- Add index for faster lookups
CREATE INDEX idx_attempt_session_id ON attempt(session_id);
