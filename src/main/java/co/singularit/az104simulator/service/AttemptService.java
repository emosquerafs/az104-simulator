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

    @Transactional
    public Attempt createAttempt(ExamConfigDto config) {
        Attempt attempt = new Attempt();
        attempt.setMode(config.getMode());
        attempt.setStartedAt(LocalDateTime.now());

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

        List<Question> questions = questionService.getRandomQuestionsWithDistribution(
                domains,
                config.getNumberOfQuestions(),
                distribution
        );

        attempt.setTotalQuestions(questions.size());

        try {
            String configJson = objectMapper.writeValueAsString(config);
            attempt.setConfigJson(configJson);
        } catch (Exception e) {
            log.error("Failed to serialize config", e);
        }

        attempt = attemptRepository.save(attempt);

        // Create answer placeholders
        for (Question question : questions) {
            AttemptAnswer answer = new AttemptAnswer();
            answer.setQuestionId(question.getId());
            answer.setMarked(false);
            attempt.addAnswer(answer);
        }

        attemptRepository.save(attempt);

        log.info("Created attempt {} with {} questions", attempt.getId(), questions.size());
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
        return attempt.getAnswers().stream()
                .map(AttemptAnswer::getQuestionId)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuestionDto getQuestionForAttempt(String attemptId, int index, ExamMode mode) {
        Attempt attempt = getAttempt(attemptId);
        List<AttemptAnswer> answers = attempt.getAnswers();

        if (index < 0 || index >= answers.size()) {
            throw new IllegalArgumentException("Invalid question index: " + index);
        }

        AttemptAnswer answer = answers.get(index);
        Long questionId = answer.getQuestionId();

        Question question = questionService.getRandomQuestions(List.of(Domain.values()), 1000).stream()
                .filter(q -> q.getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));

        boolean includeCorrectAnswers = (mode == ExamMode.PRACTICE);
        QuestionDto dto = questionService.convertToDto(question, includeCorrectAnswers);

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

    @Transactional
    public ResultDto completeAttempt(String attemptId) {
        Attempt attempt = getAttempt(attemptId);
        attempt.setEndedAt(LocalDateTime.now());
        attempt.setIsCompleted(true);

        if (attempt.getStartedAt() != null && attempt.getEndedAt() != null) {
            long seconds = java.time.Duration.between(attempt.getStartedAt(), attempt.getEndedAt()).getSeconds();
            attempt.setDurationSeconds((int) seconds);
        }

        attemptRepository.save(attempt);

        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttempt(attempt);
        return scoringService.calculateResults(attempt, answers);
    }

    @Transactional(readOnly = true)
    public ResultDto getResults(String attemptId) {
        Attempt attempt = getAttempt(attemptId);
        if (!attempt.getIsCompleted()) {
            throw new IllegalStateException("Attempt is not completed yet");
        }

        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttempt(attempt);
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
