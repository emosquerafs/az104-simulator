-- Add student_id to attempt table for user tracking
ALTER TABLE attempt ADD COLUMN student_id VARCHAR(36);

-- Add score to attempt table for precomputed scores
ALTER TABLE attempt ADD COLUMN score_percentage INTEGER;

-- Create indexes for efficient queries
CREATE INDEX idx_attempt_student_created ON attempt(student_id, started_at DESC);
CREATE INDEX idx_attempt_mode ON attempt(mode);
CREATE INDEX idx_attempt_answer_attempt ON attempt_answer(attempt_id);
CREATE INDEX idx_exam_session_question_session_position ON exam_session_question(session_id, position);
