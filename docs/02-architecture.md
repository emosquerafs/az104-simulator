# Architecture

This document explains the high-level architecture of the AZ-104 Simulator, how components interact, and key design decisions.

## High-Level Components

The simulator follows a classic layered Spring Boot architecture:

```
┌─────────────────────────────────────────┐
│         Web UI (Thymeleaf)              │  ← User interacts here
├─────────────────────────────────────────┤
│  Controllers (HomeController, etc.)     │  ← Handle HTTP requests
├─────────────────────────────────────────┤
│  Services (AttemptService, etc.)        │  ← Business logic layer
├─────────────────────────────────────────┤
│  Repositories (JPA/Hibernate)           │  ← Data access layer
├─────────────────────────────────────────┤
│      Database (H2)                      │  ← Persistent storage
└─────────────────────────────────────────┘
```

See [Component Diagram](./diagrams.md#component-diagram) for the detailed view.

## Core Components

### 1. Controllers

**Location**: `src/main/java/.../controller/`

| Controller | Purpose |
|-----------|---------|
| `HomeController` | Landing page, mode selection, configuration |
| `ExamController` | Main exam/practice flow (questions, answers, review, results) |
| `ExamSessionController` | REST API for session-based exam management |
| `AdminController` | Import/export questions (JSON) |

### 2. Services

**Location**: `src/main/java/.../service/`

| Service | Responsibility |
|---------|---------------|
| `AttemptService` | Create attempts, manage answers, calculate progress |
| `ExamSessionService` | Session lifecycle, question assignment (no duplicates) |
| `QuestionService` | Fetch questions, apply filters, localization |
| `ScoringService` | Score attempts, domain breakdowns, result DTOs |
| `StudentIdentityService` | Track users via cookies (no auth, just analytics) |

### 3. Repositories

**Location**: `src/main/java/.../repository/`

Standard Spring Data JPA repositories:

- `QuestionRepository`: Query questions by domain, difficulty, etc.
- `OptionItemRepository`: Manage answer options
- `AttemptRepository`: CRUD for attempts
- `AttemptAnswerRepository`: Store user answers with position ordering
- `ExamSessionRepository`: Track exam sessions
- `ExamSessionQuestionRepository`: Enforce unique questions per session

### 4. Domain Entities

**Location**: `src/main/java/.../domain/`

| Entity | Description |
|--------|-------------|
| `Question` | Exam question with bilingual stem/explanation |
| `OptionItem` | Answer choice with bilingual text |
| `Attempt` | User's exam or practice session |
| `AttemptAnswer` | User's answer to a specific question (with position) |
| `ExamSession` | Session tracking to prevent question duplication |
| `ExamSessionQuestion` | Junction table linking sessions to questions |

### 5. DTOs

**Location**: `src/main/java/.../dto/`

Data Transfer Objects for clean API contracts:

- `ExamConfigDto`: User's exam configuration (mode, questions, domains, time)
- `QuestionDto`: Question with localized content
- `AnswerDto`: User's answer submission
- `ResultDto`: Scored exam results with domain breakdowns
- `QuestionResultDto`: Individual question result with correct/incorrect flag

## Localization (i18n)

The simulator supports **bilingual content** (English and Spanish):

### UI Localization

- **Files**: `messages_en.properties`, `messages_es.properties`
- **Mechanism**: Spring `MessageSource` + `LocaleContextHolder`
- **Switching**: User selects language from dropdown → sets cookie → renders in chosen locale

### Question Content Localization

Questions and options have **dual columns** in the database:

- `stem_en` / `stem_es`
- `explanation_en` / `explanation_es`
- `text_en` / `text_es` (for options)

The `QuestionService.toDto()` method selects the appropriate language column based on the current locale.

**Migration History**:
- V1: Original single-language columns (`stem`, `explanation`, `text`)
- V3: Added bilingual columns (`_en`, `_es`)
- Current: Legacy columns kept for backward compatibility but new code uses `_en`/`_es`

## Question Bank Management

### Storage

Questions are stored in the **database** (not JSON at runtime). The question bank lifecycle:

1. **JSON Import**: Admin uploads `questions.json` via `/admin/import`
2. **Parsing**: `AdminController` parses JSON and creates `Question` + `OptionItem` entities
3. **Persistence**: Entities saved via JPA
4. **Export**: Admin can export DB questions back to JSON via `/admin/export`

### Question Structure

Each question has:

- **Domain**: `COMPUTE`, `NETWORKING`, `STORAGE`, `MONITORING`, `IDENTITY`, `GOVERNANCE`
- **Difficulty**: `EASY`, `MEDIUM`, `HARD`
- **Type**: `SINGLE` (one correct answer), `MULTI` (multiple correct), `YESNO` (True/False)
- **Stem**: The question text (bilingual)
- **Options**: List of answer choices (bilingual)
- **Explanation**: Why the correct answer is right (bilingual)
- **Tags**: JSON array of keywords (e.g., `["VMs", "Scale Sets"]`)

## No Duplicate Questions Guarantee

The system ensures **zero question duplication within a session** using database constraints:

1. `ExamSession` created with fixed `total_questions`
2. `ExamSessionService` randomly selects N questions
3. `ExamSessionQuestion` records each question with:
   - `UNIQUE (session_id, question_id)` ← prevents same question twice
   - `UNIQUE (session_id, position)` ← prevents position conflicts
4. `AttemptAnswer` also enforces:
   - `UNIQUE (attempt_id, question_id)` ← no duplicates in attempt
   - `UNIQUE (attempt_id, position)` ← stable ordering

This architecture prevents the "question repetition bug" that plagued earlier versions.

## Session vs Attempt

Confusing? Here's the distinction:

- **ExamSession**: Abstract container for a fixed set of questions (no duplicates)
- **Attempt**: User's interaction with that session (answers, progress, score)

One session can theoretically support multiple attempts (though current UX creates fresh sessions).

## Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 |
| Framework | Spring Boot | 3.4.1 |
| Persistence | Spring Data JPA | (via Boot) |
| Database | H2 | (embedded) |
| Migrations | Flyway | (via Boot) |
| Template Engine | Thymeleaf | (via Boot) |
| Build Tool | Gradle | 8.x |
| Frontend | Vanilla JS + CSS | N/A |

## Design Decisions

### Why H2?

- **Simplicity**: No external DB setup required
- **Portability**: Run anywhere with Java
- **Flexibility**: File mode for persistence or in-memory for testing
- **Perfect for Learning**: Easy to inspect via H2 Console

### Why No Authentication?

- **Focus on Learning**: Not a production app, no sensitive data
- **Student Tracking**: Uses cookie-based `student_id` for history (optional)
- **Easy Deployment**: No user management overhead

### Why Server-Side Rendering?

- **Simplicity**: No React/Vue/Angular complexity
- **SEO-Friendly**: Not relevant here, but good practice
- **Fast Initial Load**: No JS framework download
- **Progressive Enhancement**: Works without JavaScript (mostly)

### Why Flyway?

- **Version Control for DB**: Schema changes tracked like code
- **Reproducibility**: Same schema everywhere (dev, test, prod)
- **Team Collaboration**: No manual SQL scripts

## Diagram Reference

For visual learners, check out:

- [Component Diagram](./diagrams.md#component-diagram)
- [Sequence Diagrams](./diagrams.md#sequence-diagrams)
- [Database ER Diagram](./diagrams.md#er-diagram)

---

**Fun Fact**: The original version had questions repeating in sessions. We fixed it by introducing `ExamSession` and unique constraints. Now it's mathematically impossible to see the same question twice in one exam. Database constraints for the win! 🎉

