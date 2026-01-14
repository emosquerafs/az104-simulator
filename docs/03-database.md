# Database Schema

This document describes the database structure, relationships, and key constraints that make the simulator work.

## Overview

The database uses **Flyway migrations** for version control. Each migration file (`V1__`, `V2__`, etc.) represents a schema change applied in order.

**Migration Files**: `src/main/resources/db/migration/`

| Migration | Purpose |
|-----------|---------|
| `V1__schema.sql` | Initial schema (questions, options, attempts) |
| `V3__i18n_questions.sql` | Add bilingual columns for questions/options |
| `V4__exam_session.sql` | Create exam session table |
| `V5__exam_session_question.sql` | Junction table with uniqueness constraints |
| `V6__attempt_answer_ordering.sql` | Add position field to prevent question repetition bug |
| `V7__add_session_id_to_attempt.sql` | Link attempts to sessions |
| `V8__add_student_id_and_indexes.sql` | Student tracking and performance indexes |

## Entity Relationship Diagram

See the [ER Diagram](./diagrams.md#er-diagram) for a visual representation.

## Tables

### `question`

Stores exam questions with bilingual content.

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT (PK) | Auto-increment primary key |
| `domain` | VARCHAR(50) | Azure domain (`COMPUTE`, `NETWORKING`, etc.) |
| `difficulty` | VARCHAR(20) | `EASY`, `MEDIUM`, `HARD` |
| `qtype` | VARCHAR(20) | `SINGLE`, `MULTI`, `YESNO` |
| `stem` | TEXT | Legacy question text (deprecated) |
| `explanation` | TEXT | Legacy explanation (deprecated) |
| `stem_es` | TEXT | Spanish question text |
| `stem_en` | TEXT | English question text |
| `explanation_es` | TEXT | Spanish explanation |
| `explanation_en` | TEXT | English explanation |
| `tags_json` | TEXT | JSON array of tags (e.g., `["VMs", "Storage"]`) |

**Indexes**:
- `idx_question_domain` on `domain`
- `idx_question_difficulty` on `difficulty`

**Relationships**:
- One-to-many with `option_item`

---

### `option_item`

Stores answer choices for questions.

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT (PK) | Auto-increment primary key |
| `question_id` | BIGINT (FK) | References `question(id)` |
| `label` | VARCHAR(10) | Option label (`A`, `B`, `C`, etc.) |
| `text` | TEXT | Legacy option text (deprecated) |
| `text_es` | TEXT | Spanish option text |
| `text_en` | TEXT | English option text |
| `is_correct` | BOOLEAN | Whether this option is correct |

**Indexes**:
- `idx_option_question_id` on `question_id`

**Constraints**:
- Foreign key to `question(id)` with `ON DELETE CASCADE`

**Relationships**:
- Many-to-one with `question`

---

### `attempt`

Represents a user's exam or practice session.

| Column | Type | Description |
|--------|------|-------------|
| `id` | VARCHAR(36) (PK) | UUID identifier |
| `mode` | VARCHAR(20) | `EXAM` or `PRACTICE` |
| `started_at` | TIMESTAMP | When attempt was created |
| `ended_at` | TIMESTAMP | When attempt was completed (nullable) |
| `duration_seconds` | INT | Total time taken in seconds |
| `total_questions` | INT | Number of questions in this attempt |
| `config_json` | TEXT | JSON of user's config (domains, time limit, etc.) |
| `current_question_index` | INT | Current position in question list |
| `is_completed` | BOOLEAN | Whether attempt has been submitted |
| `session_id` | VARCHAR(36) | References `exam_session(id)` (nullable) |
| `student_id` | VARCHAR(36) | Cookie-based student identifier (nullable) |
| `score_percentage` | INT | Precomputed score percentage |

**Indexes**:
- `idx_attempt_completed` on `is_completed`
- `idx_attempt_session_id` on `session_id`
- `idx_attempt_student_created` on `(student_id, started_at DESC)`
- `idx_attempt_mode` on `mode`

**Relationships**:
- One-to-many with `attempt_answer`
- Many-to-one with `exam_session` (optional)

---

### `attempt_answer`

Stores user's answers to individual questions within an attempt.

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT (PK) | Auto-increment primary key |
| `attempt_id` | VARCHAR(36) (FK) | References `attempt(id)` |
| `question_id` | BIGINT | Question ID (not FK to allow historical data) |
| `selected_option_ids_json` | TEXT | JSON array of selected option IDs |
| `marked` | BOOLEAN | Whether user marked this for review |
| `answered_at` | TIMESTAMP | When answer was submitted (nullable) |
| `position` | INT | Position in attempt (0-based index) |

**Indexes**:
- `idx_attempt_answer_attempt_id` on `attempt_id`
- `idx_attempt_answer_question_id` on `question_id`
- `idx_attempt_answer_attempt_position` on `(attempt_id, position)`
- `idx_attempt_answer_attempt` on `attempt_id`

**Unique Constraints**:
- `ux_attempt_answer_attempt_position` on `(attempt_id, position)` ← **Prevents position conflicts**
- `ux_attempt_answer_attempt_question` on `(attempt_id, question_id)` ← **Prevents question duplication**

**Constraints**:
- Foreign key to `attempt(id)` with `ON DELETE CASCADE`

**Relationships**:
- Many-to-one with `attempt`

---

### `exam_session`

Tracks exam sessions to ensure unique question sets.

| Column | Type | Description |
|--------|------|-------------|
| `id` | VARCHAR(36) (PK) | UUID identifier |
| `mode` | VARCHAR(20) | `EXAM` or `PRACTICE` |
| `total_questions` | INT | Number of questions in this session |
| `locale` | VARCHAR(5) | Language (`en`, `es`) |
| `seed` | INT | Random seed for reproducibility (nullable) |
| `created_at` | TIMESTAMP | Session creation time |
| `completed_at` | TIMESTAMP | When session was finished (nullable) |

**Indexes**:
- `idx_exam_session_created` on `created_at`
- `idx_exam_session_mode` on `mode`

**Constraints**:
- CHECK constraint: `mode IN ('EXAM', 'PRACTICE')`

**Relationships**:
- One-to-many with `exam_session_question`
- One-to-many with `attempt` (via `session_id`)

---

### `exam_session_question`

Junction table linking sessions to questions with guaranteed uniqueness.

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT (PK) | Auto-increment primary key |
| `session_id` | VARCHAR(36) (FK) | References `exam_session(id)` |
| `question_id` | BIGINT (FK) | References `question(id)` |
| `position` | INT | Position in session (0-based index) |
| `served_at` | TIMESTAMP | When question was added to session |

**Indexes**:
- `idx_session_position` on `(session_id, position)`
- `idx_session_id` on `session_id`
- `idx_exam_session_question_session_position` on `(session_id, position)`

**Unique Constraints**:
- `unique_session_question` on `(session_id, question_id)` ← **No duplicate questions**
- `unique_session_position` on `(session_id, position)` ← **No position conflicts**

**Constraints**:
- Foreign key to `exam_session(id)` with `ON DELETE CASCADE`
- Foreign key to `question(id)` with `ON DELETE CASCADE`

**Relationships**:
- Many-to-one with `exam_session`
- Many-to-one with `question`

---

## How No-Duplication Works

The magic happens in `exam_session_question`:

```sql
CONSTRAINT unique_session_question UNIQUE (session_id, question_id)
```

This constraint **guarantees** at the database level that:

1. You cannot insert the same `question_id` twice for a given `session_id`
2. If you try, the database throws a constraint violation error
3. The application catches this and handles it gracefully (or prevents it upstream)

Similarly, `attempt_answer` enforces:

```sql
CREATE UNIQUE INDEX ux_attempt_answer_attempt_question ON attempt_answer(attempt_id, question_id);
```

This means even if session logic fails, the attempt layer prevents duplicates.

## Question Selection Logic

When starting a session, `ExamSessionService` does:

1. Query available questions matching user's filters (domains, difficulty)
2. Shuffle them randomly
3. Take first N questions
4. Insert into `exam_session_question` with position 0, 1, 2, ..., N-1
5. Database rejects any duplicates via unique constraint

## Position-Based Ordering

Earlier versions had a bug where question order was unstable, causing:

- Question at index 3 shows different content on refresh
- "I already answered this question!" complaints

**Fix**: V6 migration added `position` column with unique constraint:

```sql
CREATE UNIQUE INDEX ux_attempt_answer_attempt_position ON attempt_answer(attempt_id, position);
```

Now, queries use:

```java
findByAttemptOrderByPositionAsc(attempt)
```

This guarantees stable, reproducible question order.

## Data Integrity Notes

### Cascading Deletes

- Deleting a `question` → deletes all `option_item` rows
- Deleting an `attempt` → deletes all `attempt_answer` rows
- Deleting an `exam_session` → deletes all `exam_session_question` rows

### Orphaned Data

`attempt_answer.question_id` is **not a foreign key** (just a BIGINT). This allows:

- Keeping historical data even if questions are deleted
- Scoring old attempts with questions that no longer exist

Trade-off: No referential integrity for historical data (acceptable for this use case).

## Schema Diagram

See the [ER Diagram](./diagrams.md#er-diagram) for a visual representation showing:

- All tables
- Primary keys
- Foreign keys
- Unique constraints
- Cardinality (one-to-many, many-to-one)

---

**Protip**: Use the H2 Console to explore the schema interactively. See [Local Dev](./05-local-dev.md#h2-console) for access instructions.

