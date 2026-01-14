package co.singularit.az104simulator.service;

import co.singularit.az104simulator.domain.*;
import co.singularit.az104simulator.dto.AnswerSubmissionDto;
import co.singularit.az104simulator.dto.ExamConfigDto;
import co.singularit.az104simulator.dto.QuestionDto;
import co.singularit.az104simulator.dto.ResultDto;
import co.singularit.az104simulator.repository.AttemptAnswerRepository;
import co.singularit.az104simulator.repository.AttemptRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttemptService {

    private final AttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final QuestionService questionService;
    private final ScoringService scoringService;
    private final ObjectMapper objectMapper;
    private final co.singularit.az104simulator.repository.QuestionRepository questionRepository;
    private final ExamSessionService examSessionService;

    @Transactional
    public Attempt createAttempt(ExamConfigDto config, String studentId) {
        Attempt attempt = new Attempt();
        attempt.setMode(config.getMode());
        attempt.setStartedAt(LocalDateTime.now());
        attempt.setStudentId(studentId);

        List<Domain> domains = config.getSelectedDomains();
        if (domains == null || domains.isEmpty()) {
            domains = Arrays.asList(Domain.values());
        }

        // Build domain distribution
        Map<Domain, Integer> distribution = new HashMap<>();
        distribution.put(Domain.IDENTITY_GOVERNANCE, config.getIdentityPercentage());
        distribution.put(Domain.STORAGE, config.getStoragePercentage());
        distribution.put(Domain.COMPUTE, config.getComputePercentage());
        distribution.put(Domain.NETWORKING, config.getNetworkingPercentage());
        distribution.put(Domain.MONITOR_MAINTAIN, config.getMonitorPercentage());

        // Create ExamSession to guarantee unique questions
        String locale = LocaleContextHolder.getLocale().getLanguage();
        if (locale == null || locale.isEmpty()) {
            locale = "es";
        }
        String sessionId = examSessionService.startSession(
            config.getMode(),
            config.getNumberOfQuestions(),
            locale,
            domains,
            distribution
        );

        // Get question IDs from the session (guaranteed unique)
        List<Long> questionIds = examSessionService.getSessionQuestionIds(sessionId);

        attempt.setTotalQuestions(questionIds.size());
        attempt.setSessionId(sessionId);

        try {
            String configJson = objectMapper.writeValueAsString(config);
            attempt.setConfigJson(configJson);
        } catch (Exception e) {
            log.error("Failed to serialize config", e);
        }

        attempt = attemptRepository.save(attempt);

        // Create answer placeholders with stable position field using session questions
        int position = 0;
        for (Long questionId : questionIds) {
            AttemptAnswer answer = new AttemptAnswer();
            answer.setQuestionId(questionId);
            answer.setPosition(position++);
            answer.setMarked(false);
            attempt.addAnswer(answer);
        }

        attemptRepository.save(attempt);

        log.info("Created attempt {} with session {} and {} unique questions (positions 0-{})",
                 attempt.getId(), sessionId, questionIds.size(), questionIds.size() - 1);
        return attempt;
    }

    @Transactional(readOnly = true)
    public Attempt getAttempt(String attemptId) {
        return attemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Attempt not found: " + attemptId));
    }

    @Transactional(readOnly = true)
    public List<Long> getQuestionIds(String attemptId) {
        Attempt attempt = getAttempt(attemptId);
        // Use ordered retrieval to guarantee stable order
        return attemptAnswerRepository.findByAttemptOrderByPositionAsc(attempt).stream()
                .map(AttemptAnswer::getQuestionId)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuestionDto getQuestionForAttempt(String attemptId, int index, ExamMode mode) {
        return getQuestionForAttempt(attemptId, index, mode, "es");
    }

    @Transactional(readOnly = true)
    public QuestionDto getQuestionForAttempt(String attemptId, int index, ExamMode mode, String lang) {
        Attempt attempt = getAttempt(attemptId);
        // Use ordered retrieval to guarantee stable index -> question mapping
        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptOrderByPositionAsc(attempt);

        if (index < 0 || index >= answers.size()) {
            throw new IllegalArgumentException("Invalid question index: " + index);
        }

        AttemptAnswer answer = answers.get(index);
        Long questionId = answer.getQuestionId();

        // Direct lookup by ID - much more efficient than filtering 1000 random questions
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));

        boolean includeCorrectAnswers = (mode == ExamMode.PRACTICE);
        QuestionDto dto = questionService.convertToDto(question, includeCorrectAnswers, lang);

        // Add user's previous selection
        if (answer.getSelectedOptionIdsJson() != null && !answer.getSelectedOptionIdsJson().isEmpty()) {
            try {
                List<Long> selectedIds = objectMapper.readValue(
                        answer.getSelectedOptionIdsJson(),
                        new TypeReference<List<Long>>() {}
                );
                dto.setSelectedOptionIds(selectedIds);
                dto.setAnswered(true);
            } catch (Exception e) {
                log.error("Failed to parse selected options", e);
                dto.setSelectedOptionIds(new ArrayList<>());
                dto.setAnswered(false);
            }
        } else {
            dto.setSelectedOptionIds(new ArrayList<>());
            dto.setAnswered(false);
        }

        dto.setMarked(answer.getMarked());

        return dto;
    }

    @Transactional
    public void submitAnswer(String attemptId, AnswerSubmissionDto submission) {
        Attempt attempt = getAttempt(attemptId);

        AttemptAnswer answer = attemptAnswerRepository
                .findByAttemptAndQuestionId(attempt, submission.getQuestionId())
                .orElseThrow(() -> new IllegalArgumentException("Answer not found for question: " + submission.getQuestionId()));

        try {
            if (submission.getSelectedOptionIds() != null && !submission.getSelectedOptionIds().isEmpty()) {
                String json = objectMapper.writeValueAsString(submission.getSelectedOptionIds());
                answer.setSelectedOptionIdsJson(json);
                answer.setAnsweredAt(LocalDateTime.now());
            } else {
                answer.setSelectedOptionIdsJson(null);
                answer.setAnsweredAt(null);
            }
        } catch (Exception e) {
            log.error("Failed to serialize selected options", e);
        }

        if (submission.getMarked() != null) {
            answer.setMarked(submission.getMarked());
        }

        attemptAnswerRepository.save(answer);
    }

    @Transactional
    public void updateCurrentIndex(String attemptId, int index) {
        Attempt attempt = getAttempt(attemptId);
        attempt.setCurrentQuestionIndex(index);
        attemptRepository.save(attempt);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAttemptStatus(String attemptId) {
        Attempt attempt = getAttempt(attemptId);
        long answeredCount = attemptAnswerRepository.countByAttemptAndSelectedOptionIdsJsonIsNotNull(attempt);
        long markedCount = attemptAnswerRepository.countByAttemptAndMarkedTrue(attempt);

        Map<String, Object> status = new HashMap<>();
        status.put("totalQuestions", attempt.getTotalQuestions());
        status.put("answeredCount", answeredCount);
        status.put("markedCount", markedCount);
        status.put("unansweredCount", attempt.getTotalQuestions() - answeredCount);
        status.put("currentIndex", attempt.getCurrentQuestionIndex());

        return status;
    }

    @Transactional(readOnly = true)
    public List<String> getQuestionStates(String attemptId) {
        Attempt attempt = getAttempt(attemptId);
        // Use ordered retrieval to guarantee states match question positions
        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptOrderByPositionAsc(attempt);

        return answers.stream()
                .map(answer -> {
                    boolean isAnswered = answer.getSelectedOptionIdsJson() != null && !answer.getSelectedOptionIdsJson().isEmpty();
                    boolean isMarked = answer.getMarked() != null && answer.getMarked();

                    if (isAnswered && isMarked) {
                        return "q-answered q-marked";
                    } else if (isAnswered) {
                        return "q-answered";
                    } else if (isMarked) {
                        return "q-marked";
                    } else {
                        return "q-unanswered";
                    }
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ResultDto completeAttempt(String attemptId) {
        Attempt attempt = getAttempt(attemptId);
        attempt.setEndedAt(LocalDateTime.now());
        attempt.setIsCompleted(true);

        if (attempt.getStartedAt() != null && attempt.getEndedAt() != null) {
            long seconds = java.time.Duration.between(attempt.getStartedAt(), attempt.getEndedAt()).getSeconds();
            attempt.setDurationSeconds((int) seconds);
        }

        // Use ordered retrieval for consistent results
        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptOrderByPositionAsc(attempt);
        ResultDto results = scoringService.calculateResults(attempt, answers);

        // Calculate and store score percentage
        int scorePercentage = Math.round((results.getCorrectAnswers() * 100.0f) / attempt.getTotalQuestions());
        attempt.setScorePercentage(scorePercentage);

        attemptRepository.save(attempt);

        return results;
    }

    @Transactional(readOnly = true)
    public ResultDto getResults(String attemptId) {
        Attempt attempt = getAttempt(attemptId);
        if (!attempt.getIsCompleted()) {
            throw new IllegalStateException("Attempt is not completed yet");
        }

        // Use ordered retrieval for consistent results
        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptOrderByPositionAsc(attempt);
        return scoringService.calculateResults(attempt, answers);
    }

    @Transactional(readOnly = true)
    public ExamConfigDto getAttemptConfig(String attemptId) {
        Attempt attempt = getAttempt(attemptId);
        if (attempt.getConfigJson() == null) {
            ExamConfigDto defaultConfig = new ExamConfigDto();
            defaultConfig.setMode(attempt.getMode());
            defaultConfig.setNumberOfQuestions(attempt.getTotalQuestions());
            return defaultConfig;
        }

        try {
            return objectMapper.readValue(attempt.getConfigJson(), ExamConfigDto.class);
        } catch (Exception e) {
            log.error("Failed to parse config JSON", e);
            ExamConfigDto defaultConfig = new ExamConfigDto();
            defaultConfig.setMode(attempt.getMode());
            defaultConfig.setNumberOfQuestions(attempt.getTotalQuestions());
            return defaultConfig;
        }
    }
}
