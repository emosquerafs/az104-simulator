package co.singularit.az104simulator.service;

import co.singularit.az104simulator.domain.*;
import co.singularit.az104simulator.dto.QuestionDto;
import co.singularit.az104simulator.repository.ExamSessionQuestionRepository;
import co.singularit.az104simulator.repository.ExamSessionRepository;
import co.singularit.az104simulator.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExamSessionService {

    private final ExamSessionRepository examSessionRepository;
    private final ExamSessionQuestionRepository examSessionQuestionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionService questionService;

    /**
     * Start a new exam session with guaranteed unique questions
     *
     * @param mode EXAM or PRACTICE
     * @param totalQuestions Number of questions to include
     * @param locale Language preference (en, es)
     * @param domains List of domains to select from
     * @param domainPercentages Optional distribution percentages by domain
     * @return Session ID (UUID)
     * @throws IllegalArgumentException if not enough unique questions available
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public String startSession(
        ExamMode mode,
        Integer totalQuestions,
        String locale,
        List<Domain> domains,
        Map<Domain, Integer> domainPercentages
    ) {
        log.info("Starting new {} session with {} questions, locale: {}, domains: {}",
                 mode, totalQuestions, locale, domains);

        // Generate unique session ID
        String sessionId = UUID.randomUUID().toString();

        // Create session entity
        ExamSession session = ExamSession.builder()
            .id(sessionId)
            .mode(mode)
            .totalQuestions(totalQuestions)
            .locale(locale)
            .seed(new Random().nextInt(Integer.MAX_VALUE))
            .createdAt(LocalDateTime.now())
            .build();

        // Select unique questions
        List<Question> selectedQuestions = selectUniqueQuestions(
            domains,
            totalQuestions,
            domainPercentages
        );

        // Verify we have enough questions
        if (selectedQuestions.size() < totalQuestions) {
            String errorMsg = String.format(
                "Not enough questions in the bank to create a unique session. " +
                "Requested: %d, Available: %d",
                totalQuestions, selectedQuestions.size()
            );
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // Save session first
        examSessionRepository.save(session);

        // Assign questions to session with positions (1-indexed)
        List<ExamSessionQuestion> sessionQuestions = new ArrayList<>();
        for (int i = 0; i < selectedQuestions.size(); i++) {
            Question question = selectedQuestions.get(i);
            ExamSessionQuestion sessionQuestion = ExamSessionQuestion.builder()
                .session(session)
                .question(question)
                .position(i + 1) // 1-indexed positions
                .servedAt(LocalDateTime.now())
                .build();
            sessionQuestions.add(sessionQuestion);
        }

        // Batch insert all session questions
        try {
            examSessionQuestionRepository.saveAll(sessionQuestions);
            log.info("Successfully created session {} with {} unique questions",
                     sessionId, sessionQuestions.size());
        } catch (Exception e) {
            log.error("Failed to save session questions for session {}: {}", sessionId, e.getMessage());
            throw new RuntimeException("Failed to create session due to database constraint violation", e);
        }

        return sessionId;
    }

    /**
     * Select unique questions based on domain distribution
     * Uses the existing QuestionService logic with distribution support
     */
    private List<Question> selectUniqueQuestions(
        List<Domain> domains,
        int totalCount,
        Map<Domain, Integer> domainPercentages
    ) {
        if (domainPercentages != null && !domainPercentages.isEmpty()) {
            // Use distribution-based selection
            return questionService.getRandomQuestionsWithDistribution(
                domains,
                totalCount,
                domainPercentages
            );
        } else {
            // Simple random selection from specified domains
            return questionService.getRandomQuestions(domains, totalCount);
        }
    }

    /**
     * Get a question by position for a specific session
     *
     * @param sessionId The session identifier
     * @param position The question position (1-indexed)
     * @param includeCorrectAnswers Whether to include correct answers (for practice mode)
     * @param lang Language preference
     * @return QuestionDto or null if not found
     */
    @Transactional(readOnly = true)
    public QuestionDto getQuestionByPosition(
        String sessionId,
        Integer position,
        boolean includeCorrectAnswers,
        String lang
    ) {
        log.debug("Fetching question at position {} for session {}", position, sessionId);

        Optional<ExamSessionQuestion> sessionQuestionOpt =
            examSessionQuestionRepository.findBySessionIdAndPosition(sessionId, position);

        if (sessionQuestionOpt.isEmpty()) {
            log.warn("No question found at position {} for session {}", position, sessionId);
            return null;
        }

        ExamSessionQuestion sessionQuestion = sessionQuestionOpt.get();
        Question question = sessionQuestion.getQuestion();

        return questionService.convertToDto(question, includeCorrectAnswers, lang);
    }

    /**
     * Get all question IDs for a session in order
     *
     * @param sessionId The session identifier
     * @return Ordered list of question IDs
     */
    @Transactional(readOnly = true)
    public List<Long> getSessionQuestionIds(String sessionId) {
        return examSessionQuestionRepository.findBySessionIdOrderByPosition(sessionId)
            .stream()
            .map(sq -> sq.getQuestion().getId())
            .collect(Collectors.toList());
    }

    /**
     * Get session summary for review page
     *
     * @param sessionId The session identifier
     * @return Map of position -> QuestionDto for all questions in session
     */
    @Transactional(readOnly = true)
    public Map<Integer, QuestionDto> getSessionSummary(String sessionId, String lang) {
        log.info("Generating summary for session {}", sessionId);

        List<ExamSessionQuestion> sessionQuestions =
            examSessionQuestionRepository.findBySessionIdWithQuestionsOrderByPosition(sessionId);

        Map<Integer, QuestionDto> summary = new LinkedHashMap<>();
        for (ExamSessionQuestion sq : sessionQuestions) {
            QuestionDto dto = questionService.convertToDto(
                sq.getQuestion(),
                false, // Never include correct answers in summary
                lang
            );
            summary.put(sq.getPosition(), dto);
        }

        return summary;
    }

    /**
     * Complete a session
     *
     * @param sessionId The session identifier
     */
    @Transactional
    public void completeSession(String sessionId) {
        log.info("Completing session {}", sessionId);

        ExamSession session = examSessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        session.setCompletedAt(LocalDateTime.now());
        examSessionRepository.save(session);
    }

    /**
     * Get session details
     *
     * @param sessionId The session identifier
     * @return ExamSession entity
     */
    @Transactional(readOnly = true)
    public ExamSession getSession(String sessionId) {
        return examSessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
    }

    /**
     * Check if a session exists and is active
     *
     * @param sessionId The session identifier
     * @return true if session exists and is not completed
     */
    @Transactional(readOnly = true)
    public boolean isSessionActive(String sessionId) {
        return examSessionRepository.existsActiveSession(sessionId);
    }

    /**
     * Validate that all questions in a session are unique (for testing/debugging)
     *
     * @param sessionId The session identifier
     * @return true if all questions are unique, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean validateSessionUniqueness(String sessionId) {
        List<Long> questionIds = examSessionQuestionRepository.findQuestionIdsBySessionId(sessionId);
        Set<Long> uniqueIds = new HashSet<>(questionIds);

        boolean isUnique = questionIds.size() == uniqueIds.size();

        if (!isUnique) {
            log.error("Session {} has duplicate questions! Total: {}, Unique: {}",
                     sessionId, questionIds.size(), uniqueIds.size());
        }

        return isUnique;
    }

    /**
     * Get total number of questions in a session
     *
     * @param sessionId The session identifier
     * @return Count of questions
     */
    @Transactional(readOnly = true)
    public long getSessionQuestionCount(String sessionId) {
        return examSessionQuestionRepository.countBySessionId(sessionId);
    }
}
