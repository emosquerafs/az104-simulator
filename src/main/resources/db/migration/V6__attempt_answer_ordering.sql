-- Add position field to guarantee stable question order in attempts
-- This prevents the "question repetition" bug where the same index would show different questions

-- Step 1: Add position column (nullable first to allow existing data)
ALTER TABLE attempt_answer ADD COLUMN position INT;

-- Step 2: Populate position for existing records based on id order (stable fallback)
-- This ensures existing attempts get a deterministic order
UPDATE attempt_answer aa
SET position = (
    SELECT COUNT(*) - 1
    FROM attempt_answer aa2
    WHERE aa2.attempt_id = aa.attempt_id
      AND aa2.id <= aa.id
);

-- Step 3: Make position NOT NULL now that all records have values
ALTER TABLE attempt_answer ALTER COLUMN position SET NOT NULL;

-- Step 4: Add unique constraints to prevent duplicates
-- Constraint 1: No duplicate (attempt_id, position) - ensures no two questions share same position
CREATE UNIQUE INDEX ux_attempt_answer_attempt_position ON attempt_answer(attempt_id, position);

-- Constraint 2: No duplicate (attempt_id, question_id) - ensures no question appears twice in same attempt
CREATE UNIQUE INDEX ux_attempt_answer_attempt_question ON attempt_answer(attempt_id, question_id);

-- Step 5: Add index for efficient ordered retrieval
CREATE INDEX idx_attempt_answer_attempt_position ON attempt_answer(attempt_id, position);
