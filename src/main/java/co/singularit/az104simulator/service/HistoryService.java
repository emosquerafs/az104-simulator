package co.singularit.az104simulator.service;

import co.singularit.az104simulator.domain.*;
import co.singularit.az104simulator.dto.AttemptHistoryDto;
import co.singularit.az104simulator.dto.QuestionReviewDto;
import co.singularit.az104simulator.repository.AttemptAnswerRepository;
import co.singularit.az104simulator.repository.AttemptRepository;
import co.singularit.az104simulator.repository.QuestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistoryService {

    private final AttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Get attempt history for a student
     *
     * @param studentId Student identifier
     * @param mode Filter by mode (null for all)
     * @param limit Maximum number of results
     * @return List of attempt history DTOs
     */
    @Transactional(readOnly = true)
    public List<AttemptHistoryDto> getAttemptHistory(String studentId, ExamMode mode, int limit) {
        log.info("Getting attempt history for studentId={}, mode={}, limit={}", studentId, mode, limit);

        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "startedAt"));

        List<Attempt> attempts;
        if (mode != null) {
            attempts = attemptRepository.findByStudentIdAndModeAndIsCompletedTrue(studentId, mode, pageable);
        } else {
            attempts = attemptRepository.findByStudentIdAndIsCompletedTrue(studentId, pageable);
        }

        return attempts.stream()
                .map(this::buildAttemptHistoryDto)
                .collect(Collectors.toList());
    }

    /**
     * Get attempt detail with all questions for review
     *
     * @param attemptId Attempt identifier
     * @param studentId Student identifier for validation
     * @param lang Language for content (en/es)
     * @return Map of position to QuestionReviewDto
     * @throws IllegalArgumentException if attempt not found or doesn't belong to student
     */
    @Transactional(readOnly = true)
    public Map<Integer, QuestionReviewDto> getAttemptDetail(String attemptId, String studentId, String lang) {
        log.info("Getting attempt detail for attemptId={}, studentId={}, lang={}", attemptId, studentId, lang);

        // Validate attempt belongs to student
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));

        if (!attempt.getStudentId().equals(studentId)) {
            log.warn("Attempt {} does not belong to student {}", attemptId, studentId);
            throw new IllegalArgumentException("Attempt not found: " + attemptId);
        }

        if (!attempt.getIsCompleted()) {
            throw new IllegalArgumentException("Attempt is not completed yet");
        }

        // Get all answers ordered by position
        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptOrderByPositionAsc(attempt);

        // Build question review DTOs
        Map<Integer, QuestionReviewDto> reviewMap = new LinkedHashMap<>();
        for (AttemptAnswer answer : answers) {
            Question question = questionRepository.findById(answer.getQuestionId())
                    .orElseThrow(() -> new IllegalArgumentException("Question not found: " + answer.getQuestionId()));

            QuestionReviewDto reviewDto = buildQuestionReviewDto(question, answer, lang);
            reviewMap.put(answer.getPosition() + 1, reviewDto); // 1-indexed for display
        }

        return reviewMap;
    }

    /**
     * Get attempt summary info
     */
    @Transactional(readOnly = true)
    public AttemptHistoryDto getAttemptSummary(String attemptId, String studentId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("Attempt not found: " + attemptId);
        }

        return buildAttemptHistoryDto(attempt);
    }

    /**
     * Build AttemptHistoryDto with calculated stats
     */
    private AttemptHistoryDto buildAttemptHistoryDto(Attempt attempt) {
        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptOrderByPositionAsc(attempt);

        int correctCount = 0;
        int incorrectCount = 0;
        int unansweredCount = 0;
        int markedCount = 0;

        for (AttemptAnswer answer : answers) {
            List<Long> selectedIds = parseSelectedOptionIds(answer.getSelectedOptionIdsJson());

            if (selectedIds == null || selectedIds.isEmpty()) {
                unansweredCount++;
            } else {
                // Check if correct
                Question question = questionRepository.findById(answer.getQuestionId()).orElse(null);
                if (question != null && isAnswerCorrect(question, selectedIds)) {
                    correctCount++;
                } else {
                    incorrectCount++;
                }
            }

            if (Boolean.TRUE.equals(answer.getMarked())) {
                markedCount++;
            }
        }

        int scorePercentage = attempt.getScorePercentage() != null
                ? attempt.getScorePercentage()
                : calculateScorePercentage(correctCount, attempt.getTotalQuestions());

        // Get locale from config
        String locale = "es"; // default
        try {
            if (attempt.getConfigJson() != null) {
                Map<String, Object> config = objectMapper.readValue(
                        attempt.getConfigJson(),
                        new TypeReference<Map<String, Object>>() {}
                );
                locale = (String) config.getOrDefault("locale", "es");
            }
        } catch (Exception e) {
            log.error("Failed to parse config JSON", e);
        }

        return AttemptHistoryDto.builder()
                .id(attempt.getId())
                .mode(attempt.getMode())
                .startedAt(attempt.getStartedAt())
                .completedAt(attempt.getEndedAt())
                .durationSeconds(attempt.getDurationSeconds())
                .totalQuestions(attempt.getTotalQuestions())
                .correctCount(correctCount)
                .incorrectCount(incorrectCount)
                .unansweredCount(unansweredCount)
                .markedCount(markedCount)
                .scorePercentage(scorePercentage)
                .locale(locale)
                .build();
    }

    /**
     * Build QuestionReviewDto for a specific question and answer
     */
    private QuestionReviewDto buildQuestionReviewDto(Question question, AttemptAnswer answer, String lang) {
        List<Long> selectedIds = parseSelectedOptionIds(answer.getSelectedOptionIdsJson());
        List<Long> correctIds = getCorrectOptionIds(question);

        boolean isAnswered = selectedIds != null && !selectedIds.isEmpty();
        boolean isCorrect = isAnswerCorrect(question, selectedIds);

        // Get localized content
        String stem = "es".equals(lang) ? question.getStemEs() : question.getStemEn();
        String explanation = "es".equals(lang) ? question.getExplanationEs() : question.getExplanationEn();

        // Build option review DTOs
        List<QuestionReviewDto.OptionReviewDto> optionDtos = question.getOptions().stream()
                .map(option -> QuestionReviewDto.OptionReviewDto.builder()
                        .id(option.getId())
                        .label(option.getLabel())
                        .text("es".equals(lang) ? option.getTextEs() : option.getTextEn())
                        .isCorrect(option.getIsCorrect())
                        .isSelected(selectedIds != null && selectedIds.contains(option.getId()))
                        .build())
                .collect(Collectors.toList());

        return QuestionReviewDto.builder()
                .questionId(question.getId())
                .position(answer.getPosition() + 1) // 1-indexed
                .domain(question.getDomain())
                .difficulty(question.getDifficulty())
                .qtype(question.getQtype())
                .stem(stem)
                .explanation(explanation)
                .options(optionDtos)
                .selectedOptionIds(selectedIds != null ? selectedIds : new ArrayList<>())
                .correctOptionIds(correctIds)
                .isCorrect(isCorrect)
                .isAnswered(isAnswered)
                .marked(answer.getMarked())
                .build();
    }

    /**
     * Parse selected option IDs from JSON
     */
    private List<Long> parseSelectedOptionIds(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (Exception e) {
            log.error("Failed to parse selected option IDs: {}", json, e);
            return null;
        }
    }

    /**
     * Get correct option IDs for a question
     */
    private List<Long> getCorrectOptionIds(Question question) {
        return question.getOptions().stream()
                .filter(OptionItem::getIsCorrect)
                .map(OptionItem::getId)
                .collect(Collectors.toList());
    }

    /**
     * Check if answer is correct
     * For MULTI questions, all selected options must be correct and all correct options must be selected
     */
    private boolean isAnswerCorrect(Question question, List<Long> selectedIds) {
        if (selectedIds == null || selectedIds.isEmpty()) {
            return false;
        }

        List<Long> correctIds = getCorrectOptionIds(question);

        // For MULTI questions, sets must match exactly
        if (question.getQtype() == QuestionType.MULTI) {
            Set<Long> selectedSet = new HashSet<>(selectedIds);
            Set<Long> correctSet = new HashSet<>(correctIds);
            return selectedSet.equals(correctSet);
        }

        // For SINGLE and YESNO, just check if the selected option is correct
        return selectedIds.size() == 1 && correctIds.contains(selectedIds.get(0));
    }

    /**
     * Calculate score percentage
     */
    private int calculateScorePercentage(int correctCount, int totalQuestions) {
        if (totalQuestions == 0) {
            return 0;
        }
        return Math.round((correctCount * 100.0f) / totalQuestions);
    }
}
