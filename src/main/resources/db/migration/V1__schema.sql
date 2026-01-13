-- Question table
CREATE TABLE question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    domain VARCHAR(50) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    qtype VARCHAR(20) NOT NULL,
    stem TEXT NOT NULL,
    explanation TEXT NOT NULL,
    tags_json TEXT
);

-- Option item table
CREATE TABLE option_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    label VARCHAR(10) NOT NULL,
    text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL,
    FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE CASCADE
);

-- Attempt table
CREATE TABLE attempt (
    id VARCHAR(36) PRIMARY KEY,
    mode VARCHAR(20) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    duration_seconds INT,
    total_questions INT NOT NULL,
    config_json TEXT,
    current_question_index INT DEFAULT 0,
    is_completed BOOLEAN DEFAULT FALSE
);

-- Attempt answer table
CREATE TABLE attempt_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempt_id VARCHAR(36) NOT NULL,
    question_id BIGINT NOT NULL,
    selected_option_ids_json TEXT,
    marked BOOLEAN NOT NULL DEFAULT FALSE,
    answered_at TIMESTAMP,
    FOREIGN KEY (attempt_id) REFERENCES attempt(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_question_domain ON question(domain);
CREATE INDEX idx_question_difficulty ON question(difficulty);
CREATE INDEX idx_option_question_id ON option_item(question_id);
CREATE INDEX idx_attempt_answer_attempt_id ON attempt_answer(attempt_id);
CREATE INDEX idx_attempt_answer_question_id ON attempt_answer(question_id);
CREATE INDEX idx_attempt_completed ON attempt(is_completed);
