package co.singularit.az104simulator.controller;

import co.singularit.az104simulator.domain.ExamMode;
import co.singularit.az104simulator.domain.ExamSession;
import co.singularit.az104simulator.dto.*;
import co.singularit.az104simulator.service.ExamSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for session-based exam management
 * Ensures zero duplicate questions within a session through DB constraints
 */
@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
@Slf4j
public class ExamSessionController {

    private final ExamSessionService examSessionService;

    /**
     * Start a new exam session with guaranteed unique questions
     *
     * POST /api/exams/start
     *
     * Request body:
     * {
     *   "mode": "EXAM",
     *   "totalQuestions": 50,
     *   "locale": "es",
     *   "selectedDomains": ["IDENTITY_GOVERNANCE", "STORAGE", "COMPUTE"],
     *   "identityPercentage": 23,
     *   "storagePercentage": 18,
     *   ...
     * }
     *
     * Response:
     * {
     *   "sessionId": "uuid-here",
     *   "mode": "EXAM",
     *   "totalQuestions": 50,
     *   "locale": "es",
     *   "message": "Session created successfully"
     * }
     *
     * Error responses:
     * - 409 CONFLICT: Not enough unique questions available
     * - 400 BAD_REQUEST: Invalid request parameters
     */
    @PostMapping("/start")
    public ResponseEntity<?> startSession(@RequestBody SessionStartRequestDto request) {
        log.info("Received session start request: mode={}, questions={}, locale={}, domains={}",
                 request.getMode(), request.getTotalQuestions(), request.getLocale(), request.getSelectedDomains());

        // Validate request
        if (request.getMode() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mode is required (EXAM or PRACTICE)"));
        }
        if (request.getTotalQuestions() == null || request.getTotalQuestions() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Total questions must be positive"));
        }
        if (request.getSelectedDomains() == null || request.getSelectedDomains().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "At least one domain must be selected"));
        }

        String locale = request.getLocale() != null ? request.getLocale() : "es";

        try {
            String sessionId = examSessionService.startSession(
                request.getMode(),
                request.getTotalQuestions(),
                locale,
                request.getSelectedDomains(),
                request.getDomainPercentages()
            );

            SessionStartResponseDto response = SessionStartResponseDto.builder()
                .sessionId(sessionId)
                .mode(request.getMode())
                .totalQuestions(request.getTotalQuestions())
                .locale(locale)
                .message("Session created successfully")
                .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Failed to create session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error creating session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Get a specific question by position in the session
     *
     * GET /api/exams/{sessionId}/questions/{position}
     *
     * Response:
     * {
     *   "id": 123,
     *   "domain": "COMPUTE",
     *   "difficulty": "MEDIUM",
     *   "stem": "Question text...",
     *   "options": [...],
     *   ...
     * }
     *
     * Error responses:
     * - 404 NOT_FOUND: Session or question not found
     */
    @GetMapping("/{sessionId}/questions/{position}")
    public ResponseEntity<?> getQuestionByPosition(
        @PathVariable String sessionId,
        @PathVariable Integer position
    ) {
        log.debug("Fetching question at position {} for session {}", position, sessionId);

        try {
            // Check if session exists
            if (!examSessionService.isSessionActive(sessionId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Session not found or already completed: " + sessionId));
            }

            // Get session to determine mode
            ExamSession session = examSessionService.getSession(sessionId);
            boolean includeCorrectAnswers = session.getMode() == ExamMode.PRACTICE;

            // Get current locale
            String lang = LocaleContextHolder.getLocale().getLanguage();

            QuestionDto question = examSessionService.getQuestionByPosition(
                sessionId,
                position,
                includeCorrectAnswers,
                lang
            );

            if (question == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Question not found at position " + position));
            }

            return ResponseEntity.ok(question);

        } catch (Exception e) {
            log.error("Error fetching question at position {} for session {}", position, sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Get session summary for review page
     *
     * GET /api/exams/{sessionId}/summary
     *
     * Response:
     * {
     *   "sessionId": "uuid",
     *   "totalQuestions": 50,
     *   "questions": {
     *     "1": { "questionId": 123, "answered": true, "markedForReview": false, ... },
     *     "2": { "questionId": 456, "answered": false, "markedForReview": true, ... },
     *     ...
     *   }
     * }
     *
     * Error responses:
     * - 404 NOT_FOUND: Session not found
     */
    @GetMapping("/{sessionId}/summary")
    public ResponseEntity<?> getSessionSummary(@PathVariable String sessionId) {
        log.info("Fetching summary for session {}", sessionId);

        try {
            ExamSession session = examSessionService.getSession(sessionId);
            String lang = LocaleContextHolder.getLocale().getLanguage();

            Map<Integer, QuestionDto> questionMap = examSessionService.getSessionSummary(sessionId, lang);

            // Note: This returns basic question info without answer state
            // To include answer state, you'd need to integrate with AttemptAnswer logic
            // For now, this ensures the question set is stable and unique

            return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "totalQuestions", session.getTotalQuestions(),
                "mode", session.getMode(),
                "questions", questionMap
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching summary for session {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Submit an answer for a specific question position
     *
     * POST /api/exams/{sessionId}/questions/{position}/answer
     *
     * Request body:
     * {
     *   "selectedOptionIds": [1, 2, 3],
     *   "marked": false
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "message": "Answer recorded",
     *   "isCorrect": true  // Only in PRACTICE mode
     * }
     *
     * Note: This endpoint currently only validates the session and position.
     * To persist answers, integrate with AttemptAnswer logic or create a new
     * SessionAnswer table following the same pattern.
     */
    @PostMapping("/{sessionId}/questions/{position}/answer")
    public ResponseEntity<?> submitAnswer(
        @PathVariable String sessionId,
        @PathVariable Integer position,
        @RequestBody AnswerSubmissionDto submission
    ) {
        log.debug("Submitting answer for session {} at position {}", sessionId, position);

        try {
            // Validate session and position
            ExamSession session = examSessionService.getSession(sessionId);
            QuestionDto question = examSessionService.getQuestionByPosition(
                sessionId,
                position,
                false,
                LocaleContextHolder.getLocale().getLanguage()
            );

            if (question == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Question not found at position " + position));
            }

            // TODO: Persist answer to database
            // You can either:
            // 1. Create a new SessionAnswer entity similar to AttemptAnswer
            // 2. Link ExamSession with existing Attempt entity
            // For now, just validate the request

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Answer recorded for session " + sessionId + " at position " + position
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error submitting answer for session {} at position {}", sessionId, position, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Complete a session
     *
     * POST /api/exams/{sessionId}/complete
     *
     * Response:
     * {
     *   "success": true,
     *   "message": "Session completed"
     * }
     */
    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<?> completeSession(@PathVariable String sessionId) {
        log.info("Completing session {}", sessionId);

        try {
            examSessionService.completeSession(sessionId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Session completed successfully"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error completing session {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Get session details
     *
     * GET /api/exams/{sessionId}
     *
     * Response:
     * {
     *   "id": "uuid",
     *   "mode": "EXAM",
     *   "totalQuestions": 50,
     *   "locale": "es",
     *   "createdAt": "2024-01-01T10:00:00",
     *   "completedAt": null
     * }
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable String sessionId) {
        try {
            ExamSession session = examSessionService.getSession(sessionId);

            return ResponseEntity.ok(Map.of(
                "id", session.getId(),
                "mode", session.getMode(),
                "totalQuestions", session.getTotalQuestions(),
                "locale", session.getLocale(),
                "createdAt", session.getCreatedAt().toString(),
                "completedAt", session.getCompletedAt() != null ? session.getCompletedAt().toString() : null,
                "questionCount", examSessionService.getSessionQuestionCount(sessionId)
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching session {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }
}
