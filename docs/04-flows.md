# User Flows

This document describes the major user workflows in the AZ-104 Simulator with detailed sequence diagrams.

All diagrams are in [diagrams.md](./diagrams.md) for easy reference.

---

## 1. Practice Mode Flow

**Goal**: Learn at your own pace with immediate feedback.

### User Journey

1. User lands on home page
2. Clicks **"Start Practice"**
3. Configures session:
   - Number of questions (e.g., 20)
   - Domains (e.g., Compute + Networking)
   - Show explanations immediately: **Yes**
4. System creates `Attempt` and `ExamSession`
5. User sees **Question 1**
6. User selects answer and clicks **"Next"**
7. System shows:
   - ✅ Correct or ❌ Incorrect
   - Explanation
   - Correct answer(s) highlighted
8. User clicks **"Next"** → Question 2
9. Repeat until all questions answered
10. User clicks **"Review All"**
11. System shows summary with score and domain breakdown
12. User can review individual questions

### Key Characteristics

- **No time limit**: User can take as long as needed
- **Instant feedback**: Correct answer shown immediately
- **Change answers**: User can go back and modify answers
- **Explanations visible**: Learn why answers are right/wrong

### Sequence Diagram

See [Practice Mode Sequence](./diagrams.md#practice-mode-sequence).

### Controllers & Services Involved

```
HomeController.showConfig()
  ↓
ExamController.startAttempt()
  ↓
AttemptService.createAttempt() + ExamSessionService.startSession()
  ↓
ExamController.showQuestion()
  ↓
ExamController.submitAnswer()
  ↓
AttemptService.saveAnswer() + QuestionService.getQuestion()
  ↓
ExamController.reviewAttempt()
  ↓
ExamController.submitAttempt()
  ↓
AttemptService.completeAttempt() + ScoringService.calculateResults()
  ↓
ExamController.showResults()
```

---

## 2. Exam Mode Flow

**Goal**: Simulate real exam conditions with time pressure.

### User Journey

1. User lands on home page
2. Clicks **"Start Exam"**
3. Configures session:
   - Number of questions (e.g., 50)
   - Time limit (e.g., 90 minutes)
   - Domains (or "All")
   - Show explanations: **No** (disabled)
4. System creates `Attempt` with `mode=EXAM`
5. Timer starts on first question
6. User answers questions in order (or jumps around)
7. User can **mark for review** (flag icon)
8. **No feedback** during exam (no correct/incorrect shown)
9. User clicks **"Review All"** to see summary:
   - Answered: 45/50
   - Marked: 5
   - Unanswered: 5
10. User navigates to unanswered questions
11. User clicks **"Submit Exam"**
12. Confirmation dialog: "Are you sure?"
13. System calculates score
14. Results page shows:
    - Overall score (e.g., 76%)
    - Domain breakdown
    - Pass/Fail indicator
15. User can review question-by-question with correct answers

### Key Characteristics

- **Timed**: Countdown timer visible
- **No feedback during exam**: Answers not revealed until submission
- **Mark for review**: Flag questions to revisit
- **Review screen**: Navigate all questions before final submit
- **Final submission**: One-way action, locks the attempt

### Sequence Diagram

See [Exam Mode Sequence](./diagrams.md#exam-mode-sequence).

### Controllers & Services Involved

```
HomeController.showConfig()
  ↓
ExamController.startAttempt() [mode=EXAM]
  ↓
AttemptService.createAttempt()
  ↓
ExamController.showQuestion() [showCorrectAnswer=false]
  ↓
ExamController.submitAnswer() [no feedback]
  ↓
ExamController.markForReview()
  ↓
ExamController.reviewAttempt() [summary page]
  ↓
ExamController.submitAttempt() [locks attempt]
  ↓
ScoringService.calculateResults()
  ↓
ExamController.showResults()
```

---

## 3. Attempt History & Review Flow

**Goal**: Review past exams and track progress.

### User Journey

1. User clicks **"My Attempts"** from home page
2. System displays list of all attempts:
   - Date/Time
   - Mode (Practice/Exam)
   - Score (if completed)
   - Status (In Progress / Completed)
3. User clicks on an attempt
4. System loads **Attempt Detail** page:
   - Overall score
   - Domain breakdown
   - Time taken
   - List of all questions
5. User clicks on **"Question 3"**
6. System shows:
   - Question text
   - All options
   - User's selected answer (highlighted)
   - Correct answer (highlighted in green)
   - Explanation
   - ✅ Correct or ❌ Incorrect badge
7. User navigates next/previous questions
8. User clicks **"Back to History"**

### Key Characteristics

- **Read-only**: Cannot change answers in completed attempts
- **Full transparency**: See what you got wrong and why
- **Domain analytics**: Identify weak areas
- **Historical tracking**: All attempts preserved

### Sequence Diagram

See [Attempt History Sequence](./diagrams.md#attempt-history-sequence).

### Controllers & Services Involved

```
HistoryController.listAttempts()
  ↓
AttemptService.getAttemptsByStudent()
  ↓
HistoryController.viewAttempt()
  ↓
AttemptService.getResults()
  ↓
HistoryController.viewQuestion()
  ↓
AttemptService.getQuestionForAttempt() + QuestionService.toDto()
```

---

## 4. Language Selection Flow

**Goal**: Switch between English and Spanish for UI and content.

### User Journey

1. User sees language dropdown in header
2. Current language shown (e.g., "🇪🇸 Español")
3. User clicks dropdown
4. Selects "🇬🇧 English"
5. System:
   - Sets `LocaleContextHolder` to `en`
   - Sets cookie `lang=en`
   - Redirects to same page
6. Page reloads with:
   - UI labels in English
   - Question text in English
   - Explanation in English
   - Option text in English

### How It Works

**UI Localization**:
- Thymeleaf uses `#{message.key}` syntax
- Spring resolves keys from `messages_en.properties` or `messages_es.properties`
- Based on `LocaleContextHolder.getLocale()`

**Content Localization**:
- `QuestionService.toDto()` checks current locale
- Selects `stem_en` vs `stem_es` from database
- Returns localized DTO to controller
- Thymeleaf renders localized content

### Persistence

Language preference stored in:
- **Cookie**: `lang=en` or `lang=es` (persistent across sessions)
- **Session**: `LocaleContextHolder` (current request)

### Sequence Diagram

See [Language Switch Sequence](./diagrams.md#language-switch-sequence).

### Controllers & Services Involved

```
LocaleController.changeLanguage()
  ↓
LocaleContextHolder.setLocale(locale)
  ↓
CookieUtils.setCookie("lang", locale)
  ↓
return "redirect:" + referer
  ↓
[Page reloads]
  ↓
LocaleInterceptor.preHandle() [reads cookie]
  ↓
QuestionService.toDto(questionId, lang) [returns localized content]
```

---

## 5. Attempt Lifecycle (State Machine)

An `Attempt` goes through these states:

```
[Created] → [In Progress] → [Completed]
     ↓              ↓              ↓
 is_completed=false  ...    is_completed=true
 ended_at=null             ended_at=<timestamp>
                           score_percentage=<value>
```

### State Transitions

| Transition | Trigger | Service Method |
|-----------|---------|---------------|
| **Created → In Progress** | User starts answering | `createAttempt()` |
| **In Progress → In Progress** | User answers question | `saveAnswer()` |
| **In Progress → Completed** | User submits exam | `completeAttempt()` |

### State-Specific Behavior

**Created / In Progress**:
- Can navigate questions
- Can change answers
- Can mark for review
- Timer running (if Exam mode)

**Completed**:
- Read-only
- Score calculated
- Results visible
- Cannot modify answers

### Diagram

See [Attempt State Machine](./diagrams.md#attempt-state-machine).

---

## 6. Question Navigation

Users can navigate questions in multiple ways:

### Linear Navigation

- **Next button**: `index + 1`
- **Previous button**: `index - 1`
- Sequential flow through all questions

### Direct Navigation

- **Question palette**: Click "Question 5" → jump to index 4
- **Review screen**: Click any question from summary list

### Smart Navigation

- **Go to unanswered**: Jump to first unanswered question
- **Go to marked**: Jump to first marked question

### URL Pattern

```
GET /attempt/{attemptId}/question/{index}
```

Example: `/attempt/abc-123/question/0` (first question)

The `index` is **position-based** (not question ID), ensuring stable navigation.

---

## 7. Scoring Flow

When user submits an exam:

1. **Calculate Correct Answers**:
   - For each `AttemptAnswer`, compare `selected_option_ids_json` with correct options
   - Single choice: Must match exactly
   - Multiple choice: Must match all correct options (no partial credit)

2. **Domain Breakdown**:
   - Group questions by domain
   - Calculate correct/total per domain
   - Example: Compute: 8/10, Networking: 7/10

3. **Overall Score**:
   - `score = (correct / total) * 100`
   - Round to integer percentage

4. **Pass/Fail**:
   - Pass threshold: 70% (700/1000 in real AZ-104)
   - Display badge: ✅ Passed or ❌ Failed

5. **Persist Results**:
   - `attempt.score_percentage = score`
   - `attempt.is_completed = true`
   - `attempt.ended_at = now()`

### Service Method

```java
ResultDto calculateResults(Attempt attempt, List<AttemptAnswer> answers)
```

Returns:
- `correctAnswers`: int
- `totalQuestions`: int
- `scorePercentage`: int
- `domainBreakdowns`: Map<Domain, DomainBreakdown>
- `questionResults`: List<QuestionResultDto>

---

## Edge Cases Handled

### 1. Incomplete Attempts

**Scenario**: User closes browser mid-exam.

**Handling**:
- Attempt remains `is_completed=false`
- User can resume from **"My Attempts"** → "Continue"
- Progress preserved (answered questions still saved)

### 2. Timeout (Exam Mode)

**Scenario**: Timer reaches 00:00.

**Handling**:
- JavaScript auto-submits attempt
- POST to `/attempt/{id}/submit`
- Calculates score with current answers

### 3. Question Deleted

**Scenario**: Admin deletes a question that's in an old attempt.

**Handling**:
- `attempt_answer.question_id` is not FK (allows orphaned data)
- Results page shows "Question Unavailable" for that question
- Score still calculated with remaining questions

### 4. Duplicate Question Prevention

**Scenario**: Bug tries to add same question twice.

**Handling**:
- Database throws constraint violation
- `exam_session_question` UNIQUE constraint blocks it
- Application logs error and skips duplicate

---

## Summary

The simulator supports four main flows:

1. **Practice Mode**: Learn with instant feedback
2. **Exam Mode**: Test yourself under time pressure
3. **History Review**: Analyze past performance
4. **Language Switch**: Toggle between EN/ES

Each flow is backed by clear service boundaries, stable database constraints, and predictable state transitions.

For visual representations, see [Sequence Diagrams](./diagrams.md#sequence-diagrams).

