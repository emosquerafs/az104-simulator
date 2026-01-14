# Diagrams

This document contains all Mermaid diagrams for the AZ-104 Simulator. These visualizations help understand the architecture, data model, and user flows.

## Table of Contents

1. [Component Diagram](#component-diagram)
2. [ER Diagram (Database Schema)](#er-diagram)
3. [Sequence Diagrams](#sequence-diagrams)
   - [Practice Mode](#practice-mode-sequence)
   - [Exam Mode](#exam-mode-sequence)
   - [Attempt History](#attempt-history-sequence)
   - [Language Switch](#language-switch-sequence)
4. [State Machine](#attempt-state-machine)

---

## Component Diagram

High-level architecture showing how major components interact.

```mermaid
graph TB
    subgraph "Web Browser"
        UI[HTML/JS/CSS<br/>Thymeleaf Templates]
    end
    
    subgraph "Spring Boot Application"
        subgraph "Controllers"
            HC[HomeController]
            EC[ExamController]
            ESC[ExamSessionController]
            AC[AdminController]
        end
        
        subgraph "Services"
            AS[AttemptService]
            QS[QuestionService]
            ESS[ExamSessionService]
            SS[ScoringService]
            SIS[StudentIdentityService]
        end
        
        subgraph "Repositories"
            QR[QuestionRepository]
            AR[AttemptRepository]
            AAR[AttemptAnswerRepository]
            ESR[ExamSessionRepository]
            ESQR[ExamSessionQuestionRepository]
        end
    end
    
    subgraph "Database"
        DB[(H2 Database<br/>question, option_item,<br/>attempt, attempt_answer,<br/>exam_session, exam_session_question)]
    end
    
    subgraph "Localization"
        MSG[messages_en.properties<br/>messages_es.properties]
    end
    
    UI --> HC
    UI --> EC
    UI --> ESC
    UI --> AC
    
    HC --> QS
    EC --> AS
    EC --> QS
    ESC --> ESS
    AC --> QR
    
    AS --> AR
    AS --> AAR
    AS --> SS
    AS --> SIS
    QS --> QR
    ESS --> ESR
    ESS --> ESQR
    ESS --> QR
    SS --> QR
    
    AR --> DB
    AAR --> DB
    QR --> DB
    ESR --> DB
    ESQR --> DB
    
    HC -.uses.-> MSG
    EC -.uses.-> MSG
    QS -.uses.-> MSG
    
    style UI fill:#e1f5ff
    style DB fill:#fff4e1
    style MSG fill:#ffe1f5
```

---

## ER Diagram

Database schema showing all tables, relationships, and key constraints.

```mermaid
erDiagram
    QUESTION ||--o{ OPTION_ITEM : "has"
    QUESTION ||--o{ EXAM_SESSION_QUESTION : "included in"
    EXAM_SESSION ||--o{ EXAM_SESSION_QUESTION : "contains"
    EXAM_SESSION ||--o{ ATTEMPT : "generates"
    ATTEMPT ||--o{ ATTEMPT_ANSWER : "has"
    
    QUESTION {
        bigint id PK
        varchar domain
        varchar difficulty
        varchar qtype
        text stem "deprecated"
        text stem_es
        text stem_en
        text explanation "deprecated"
        text explanation_es
        text explanation_en
        text tags_json
    }
    
    OPTION_ITEM {
        bigint id PK
        bigint question_id FK
        varchar label
        text text "deprecated"
        text text_es
        text text_en
        boolean is_correct
    }
    
    EXAM_SESSION {
        varchar id PK "UUID"
        varchar mode
        int total_questions
        varchar locale
        int seed
        timestamp created_at
        timestamp completed_at
    }
    
    EXAM_SESSION_QUESTION {
        bigint id PK
        varchar session_id FK
        bigint question_id FK
        int position
        timestamp served_at
    }
    
    ATTEMPT {
        varchar id PK "UUID"
        varchar mode
        timestamp started_at
        timestamp ended_at
        int duration_seconds
        int total_questions
        text config_json
        int current_question_index
        boolean is_completed
        varchar session_id FK
        varchar student_id
        int score_percentage
    }
    
    ATTEMPT_ANSWER {
        bigint id PK
        varchar attempt_id FK
        bigint question_id
        text selected_option_ids_json
        boolean marked
        timestamp answered_at
        int position
    }
```

**Key Constraints**:

- `UNIQUE (session_id, question_id)` on `EXAM_SESSION_QUESTION` → **No duplicate questions per session**
- `UNIQUE (session_id, position)` on `EXAM_SESSION_QUESTION` → **No position conflicts**
- `UNIQUE (attempt_id, question_id)` on `ATTEMPT_ANSWER` → **No duplicate questions per attempt**
- `UNIQUE (attempt_id, position)` on `ATTEMPT_ANSWER` → **Stable question ordering**

---

## Sequence Diagrams

### Practice Mode Sequence

User flow for practice mode with immediate feedback.

```mermaid
sequenceDiagram
    actor User
    participant Browser
    participant HomeController
    participant ExamController
    participant AttemptService
    participant ExamSessionService
    participant QuestionService
    participant Database
    
    User->>Browser: Click "Start Practice"
    Browser->>HomeController: GET /config?mode=PRACTICE
    HomeController-->>Browser: Show config page
    
    User->>Browser: Configure (20 questions, Compute)
    Browser->>ExamController: POST /attempt/start
    
    ExamController->>ExamSessionService: startSession(PRACTICE, 20, "en", [COMPUTE])
    ExamSessionService->>Database: Create EXAM_SESSION
    ExamSessionService->>Database: Select 20 random questions
    ExamSessionService->>Database: Insert EXAM_SESSION_QUESTION (with unique constraints)
    ExamSessionService-->>ExamController: sessionId
    
    ExamController->>AttemptService: createAttempt(PRACTICE, 20, sessionId)
    AttemptService->>Database: Create ATTEMPT
    AttemptService->>Database: Create ATTEMPT_ANSWER for each question
    AttemptService-->>ExamController: attemptId
    
    ExamController-->>Browser: Redirect to /attempt/{attemptId}/question/0
    
    loop For each question
        Browser->>ExamController: GET /attempt/{attemptId}/question/{index}
        ExamController->>AttemptService: getQuestionForAttempt(attemptId, index, PRACTICE)
        AttemptService->>QuestionService: toDto(questionId, includeCorrectAnswers=false)
        QuestionService->>Database: SELECT question, options
        QuestionService-->>AttemptService: QuestionDto (localized)
        AttemptService-->>ExamController: QuestionDto with user's previous answer
        ExamController-->>Browser: Render exam.html
        
        User->>Browser: Select answer, click "Next"
        Browser->>ExamController: POST /attempt/{attemptId}/answer
        ExamController->>AttemptService: saveAnswer(attemptId, questionId, selectedOptions)
        AttemptService->>Database: UPDATE ATTEMPT_ANSWER
        AttemptService-->>ExamController: Success
        
        alt Practice Mode (immediate feedback)
            ExamController->>QuestionService: toDto(questionId, includeCorrectAnswers=true)
            QuestionService-->>ExamController: QuestionDto with correct answers
            ExamController-->>Browser: Show feedback (✓ or ✗) + explanation
        end
    end
    
    User->>Browser: Click "Review All"
    Browser->>ExamController: GET /attempt/{attemptId}/review
    ExamController->>AttemptService: getAttemptStatus(attemptId)
    AttemptService->>Database: Count answered/unanswered/marked
    AttemptService-->>ExamController: Status summary
    ExamController-->>Browser: Render review.html
    
    User->>Browser: Click "Submit"
    Browser->>ExamController: POST /attempt/{attemptId}/submit
    ExamController->>AttemptService: completeAttempt(attemptId)
    AttemptService->>AttemptService: calculateResults()
    AttemptService->>Database: UPDATE ATTEMPT (is_completed=true, score_percentage)
    AttemptService-->>ExamController: Success
    
    ExamController-->>Browser: Redirect to /attempt/{attemptId}/results
    Browser->>ExamController: GET /attempt/{attemptId}/results
    ExamController->>AttemptService: getResults(attemptId)
    AttemptService-->>ExamController: ResultDto (score, domain breakdown)
    ExamController-->>Browser: Render results.html
```

---

### Exam Mode Sequence

User flow for exam mode with timer and no immediate feedback.

```mermaid
sequenceDiagram
    actor User
    participant Browser
    participant ExamController
    participant AttemptService
    participant ScoringService
    participant Database
    
    User->>Browser: Click "Start Exam"
    Note over Browser: (Config step similar to Practice)
    
    Browser->>ExamController: POST /attempt/start (mode=EXAM)
    ExamController->>AttemptService: createAttempt(EXAM, 50, sessionId)
    AttemptService->>Database: Create ATTEMPT with mode=EXAM
    AttemptService-->>ExamController: attemptId
    
    ExamController-->>Browser: Redirect to /attempt/{attemptId}/question/0
    Browser->>Browser: Start countdown timer (90 minutes)
    
    loop Answer questions (no feedback)
        Browser->>ExamController: GET /attempt/{attemptId}/question/{index}
        ExamController->>AttemptService: getQuestionForAttempt(attemptId, index, EXAM)
        Note over AttemptService: includeCorrectAnswers=false for EXAM mode
        AttemptService-->>ExamController: QuestionDto (no correct answer info)
        ExamController-->>Browser: Render question
        
        User->>Browser: Select answer
        Browser->>ExamController: POST /attempt/{attemptId}/answer
        ExamController->>AttemptService: saveAnswer(attemptId, questionId, selectedOptions)
        AttemptService->>Database: UPDATE ATTEMPT_ANSWER
        Note over Browser: No feedback shown
        ExamController-->>Browser: Navigate to next question
    end
    
    alt User marks questions
        User->>Browser: Click "Mark for Review"
        Browser->>ExamController: POST /attempt/{attemptId}/question/{index}/mark
        ExamController->>AttemptService: markForReview(attemptId, questionId, true)
        AttemptService->>Database: UPDATE ATTEMPT_ANSWER (marked=true)
        ExamController-->>Browser: Show flag icon
    end
    
    User->>Browser: Click "Review All"
    Browser->>ExamController: GET /attempt/{attemptId}/review
    ExamController->>AttemptService: getAttemptStatus(attemptId)
    AttemptService->>Database: Query attempt_answer for counts
    AttemptService-->>ExamController: answered: 45, unanswered: 5, marked: 3
    ExamController-->>Browser: Render review page with summary
    
    alt Time expires
        Browser->>Browser: Timer reaches 00:00
        Browser->>ExamController: POST /attempt/{attemptId}/submit (auto-submit)
    else User clicks Submit
        User->>Browser: Click "Submit Exam"
        Browser->>Browser: Show confirmation dialog
        User->>Browser: Confirm
        Browser->>ExamController: POST /attempt/{attemptId}/submit
    end
    
    ExamController->>AttemptService: completeAttempt(attemptId)
    AttemptService->>Database: SELECT all attempt_answers
    AttemptService->>ScoringService: calculateResults(attempt, answers)
    
    loop For each answer
        ScoringService->>Database: SELECT question, correct options
        ScoringService->>ScoringService: Compare selected vs correct
        ScoringService->>ScoringService: Update score counters
    end
    
    ScoringService-->>AttemptService: ResultDto (score, breakdown)
    AttemptService->>Database: UPDATE ATTEMPT (score_percentage, is_completed, ended_at)
    AttemptService-->>ExamController: ResultDto
    
    ExamController-->>Browser: Redirect to /attempt/{attemptId}/results
    Browser->>ExamController: GET /attempt/{attemptId}/results
    ExamController-->>Browser: Render results (score, pass/fail, breakdown)
```

---

### Attempt History Sequence

User reviewing past attempts.

```mermaid
sequenceDiagram
    actor User
    participant Browser
    participant HistoryController
    participant AttemptService
    participant QuestionService
    participant Database
    
    User->>Browser: Click "My Attempts"
    Browser->>HistoryController: GET /history
    HistoryController->>AttemptService: getAttemptsByStudent(studentId)
    AttemptService->>Database: SELECT * FROM attempt WHERE student_id=? ORDER BY started_at DESC
    Database-->>AttemptService: List of attempts
    AttemptService-->>HistoryController: List<Attempt>
    HistoryController-->>Browser: Render history.html
    
    User->>Browser: Click on specific attempt
    Browser->>HistoryController: GET /history/{attemptId}
    HistoryController->>AttemptService: getResults(attemptId)
    AttemptService->>Database: SELECT attempt, attempt_answers
    AttemptService->>AttemptService: Calculate results
    AttemptService-->>HistoryController: ResultDto
    HistoryController-->>Browser: Render attempt-detail.html
    
    User->>Browser: Click "Question 5"
    Browser->>HistoryController: GET /history/{attemptId}/question/4
    HistoryController->>AttemptService: getQuestionForAttempt(attemptId, 4)
    AttemptService->>Database: SELECT attempt_answer WHERE position=4
    AttemptService->>QuestionService: toDto(questionId, includeCorrectAnswers=true)
    QuestionService->>Database: SELECT question, options
    QuestionService-->>AttemptService: QuestionDto (with correct answers)
    AttemptService-->>HistoryController: QuestionDto + user's answer
    HistoryController-->>Browser: Render question with review mode
    
    Note over Browser: Show:<br/>- User's selected answer (highlighted)<br/>- Correct answer (green)<br/>- ✓ or ✗ indicator<br/>- Explanation
    
    User->>Browser: Click "Next Question"
    Browser->>HistoryController: GET /history/{attemptId}/question/5
    Note over Browser,Database: Repeat above steps
```

---

### Language Switch Sequence

User changing interface language.

```mermaid
sequenceDiagram
    actor User
    participant Browser
    participant LocaleController
    participant LocaleInterceptor
    participant QuestionService
    participant MessageSource
    
    User->>Browser: Select "🇬🇧 English" from dropdown
    Browser->>LocaleController: GET /locale/change?lang=en&redirect=/config
    
    LocaleController->>LocaleController: Parse locale "en"
    LocaleController->>LocaleController: Set cookie lang=en (max-age 1 year)
    LocaleController->>LocaleController: LocaleContextHolder.setLocale(Locale.EN)
    LocaleController-->>Browser: Set-Cookie header with lang=en
    LocaleController-->>Browser: Redirect to /config
    
    Browser->>Browser: Store cookie
    Browser->>LocaleController: GET /config
    Note over Browser: Cookie sent with request
    
    LocaleInterceptor->>LocaleInterceptor: preHandle(request)
    LocaleInterceptor->>LocaleInterceptor: Read cookie lang value
    LocaleInterceptor->>LocaleInterceptor: Set locale to EN
    
    LocaleController->>MessageSource: getMessage for config.title in EN
    MessageSource->>MessageSource: Load messages_en.properties
    MessageSource-->>LocaleController: Return "Configure Your"
    
    alt Rendering question content
        LocaleController->>QuestionService: toDto with lang EN
        QuestionService->>QuestionService: Select stem_en, explanation_en, text_en
        QuestionService-->>LocaleController: QuestionDto (English content)
    end
    
    LocaleController-->>Browser: Render page in English
    
    Note over Browser: All UI labels from messages_en.properties<br/>All question content from EN columns
```

---

## Attempt State Machine

Lifecycle of an `Attempt` entity.

```mermaid
stateDiagram-v2
    [*] --> Created: createAttempt()
    
    Created --> InProgress: User answers first question
    
    InProgress --> InProgress: saveAnswer()
    InProgress --> InProgress: markForReview()
    InProgress --> InProgress: Navigate questions
    
    InProgress --> Completed: completeAttempt()
    
    Completed --> [*]
    
    note right of Created
        is_completed = false
        ended_at = null
        score_percentage = null
    end note
    
    note right of InProgress
        is_completed = false
        ended_at = null
        current_question_index updates
        Answers saved to attempt_answer
    end note
    
    note right of Completed
        is_completed = true
        ended_at = <timestamp>
        score_percentage = <calculated>
        Read-only from this point
    end note
```

**State Details**:

| State | Properties | Allowed Actions |
|-------|-----------|----------------|
| **Created** | `is_completed=false`, `ended_at=null` | Start answering, abandon |
| **InProgress** | `is_completed=false`, answers accumulating | Answer, navigate, mark, submit |
| **Completed** | `is_completed=true`, `ended_at` set, score calculated | Review only (read-only) |

**State Transitions**:

- **Created → InProgress**: Automatic on first answer
- **InProgress → InProgress**: Each answer/navigation
- **InProgress → Completed**: User submits or timer expires (Exam mode)
- **Completed → [no transition]**: Terminal state

---

## Usage Notes

### Viewing Mermaid Diagrams

These diagrams render automatically on:
- **GitHub**: In Markdown preview
- **GitLab**: In Markdown preview
- **VS Code**: With Mermaid extension
- **IntelliJ**: With Mermaid plugin

Or use online viewer: https://mermaid.live/

### Exporting Diagrams

**To PNG/SVG**:
1. Copy diagram code
2. Paste into https://mermaid.live/
3. Click "Export" → PNG or SVG

**To PDF** (for documentation):
1. Use Mermaid CLI:
   ```bash
   npm install -g @mermaid-js/mermaid-cli
   mmdc -i diagrams.md -o diagrams.pdf
   ```

---

**Tip**: These diagrams are living documentation. If the architecture changes, update the diagrams first, then the code. Visual consistency helps onboarding. 📊

