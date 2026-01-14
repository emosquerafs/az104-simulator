package co.singularit.az104simulator.integration;

import co.singularit.az104simulator.domain.*;
import co.singularit.az104simulator.dto.ExamConfigDto;
import co.singularit.az104simulator.dto.QuestionDto;
import co.singularit.az104simulator.repository.AttemptAnswerRepository;
import co.singularit.az104simulator.repository.AttemptRepository;
import co.singularit.az104simulator.repository.QuestionRepository;
import co.singularit.az104simulator.service.AttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for Attempt functionality - CRITICAL for preventing question repetition bug.
 * These tests verify database constraints and stable question ordering.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AttemptIntegrationTest {

    @Autowired
    private AttemptService attemptService;

    @Autowired
    private AttemptRepository attemptRepository;

    @Autowired
    private AttemptAnswerRepository attemptAnswerRepository;

    @Autowired
    private QuestionRepository questionRepository;

    private List<Question> testQuestions;

    @BeforeEach
    void setUp() {
        // Clean up
        attemptAnswerRepository.deleteAll();
        attemptRepository.deleteAll();

        // Create 100 test questions if they don't exist
        long existingCount = questionRepository.count();
        if (existingCount < 100) {
            testQuestions = IntStream.range(1, 101)
                .mapToObj(i -> {
                    Question q = new Question();
                    q.setDomain(Domain.values()[i % Domain.values().length]);
                    q.setDifficulty(Difficulty.MEDIUM);
                    q.setQtype(QuestionType.SINGLE);
                    q.setStemEn("Test question " + i);
                    q.setStemEs("Pregunta de prueba " + i);
                    q.setExplanationEn("Explanation " + i);
                    q.setExplanationEs("Explicación " + i);
                    q.setTagsJson("[\"test\"]");

                    // Add options
                    OptionItem correctOption = new OptionItem();
                    correctOption.setLabel("A");
                    correctOption.setTextEn("Correct answer");
                    correctOption.setTextEs("Respuesta correcta");
                    correctOption.setIsCorrect(true);
                    correctOption.setQuestion(q);

                    OptionItem wrongOption = new OptionItem();
                    wrongOption.setLabel("B");
                    wrongOption.setTextEn("Wrong answer");
                    wrongOption.setTextEs("Respuesta incorrecta");
                    wrongOption.setIsCorrect(false);
                    wrongOption.setQuestion(q);

                    q.setOptions(List.of(correctOption, wrongOption));
                    return q;
                })
                .collect(Collectors.toList());

            testQuestions = questionRepository.saveAll(testQuestions);
        } else {
            testQuestions = questionRepository.findAll();
        }
    }

    @Test
    void createAttempt_With50Questions_ShouldNotContainDuplicates() {
        // Arrange
        int requestedQuestions = 50;
        ExamConfigDto config = createExamConfig(ExamMode.EXAM, requestedQuestions);

        // Act
        Attempt attempt = attemptService.createAttempt(config);

        // Assert
        assertThat(attempt).isNotNull();
        assertThat(attempt.getId()).isNotNull();
        assertThat(attempt.getTotalQuestions()).isEqualTo(requestedQuestions);

        // Verify in database - fetch from repository to ensure persistence
        Attempt savedAttempt = attemptRepository.findById(attempt.getId()).orElseThrow();
        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptOrderByPositionAsc(savedAttempt);

        assertThat(answers).hasSize(requestedQuestions);

        // CRITICAL: Verify all question IDs are unique - no duplicates allowed
        Set<Long> questionIds = answers.stream()
            .map(AttemptAnswer::getQuestionId)
            .collect(Collectors.toSet());
        assertThat(questionIds)
            .as("Attempt must have %d unique questions - no duplicates", requestedQuestions)
            .hasSize(requestedQuestions);

        // CRITICAL: Verify all positions are unique and sequential
        Set<Integer> positions = answers.stream()
            .map(AttemptAnswer::getPosition)
            .collect(Collectors.toSet());
        assertThat(positions)
            .as("All positions must be unique")
            .hasSize(requestedQuestions);

        List<Integer> sortedPositions = new ArrayList<>(positions);
        Collections.sort(sortedPositions);
        for (int i = 0; i < requestedQuestions; i++) {
            assertThat(sortedPositions.get(i))
                .as("Position should be sequential from 0 to %d", requestedQuestions - 1)
                .isEqualTo(i);
        }
    }

    @Test
    void getQuestionForAttempt_Called20Times_ShouldReturnSameQuestionForSameIndex() {
        // Arrange
        int requestedQuestions = 50;
        int testIndex = 0;
        int numberOfCalls = 20;

        ExamConfigDto config = createExamConfig(ExamMode.EXAM, requestedQuestions);
        Attempt attempt = attemptService.createAttempt(config);

        // Act - Call the same method 20 times with the same index
        Set<Long> returnedQuestionIds = new HashSet<>();
        Set<String> returnedStems = new HashSet<>();

        for (int call = 0; call < numberOfCalls; call++) {
            QuestionDto question = attemptService.getQuestionForAttempt(
                attempt.getId(),
                testIndex,
                ExamMode.EXAM,
                "es"
            );
            returnedQuestionIds.add(question.getId());
            returnedStems.add(question.getStem());
        }

        // Assert - CRITICAL: All 20 calls should return the EXACT SAME question
        assertThat(returnedQuestionIds)
            .as("Calling getQuestionForAttempt 20 times with index=%d must ALWAYS return the same question ID", testIndex)
            .hasSize(1);

        assertThat(returnedStems)
            .as("Calling getQuestionForAttempt 20 times with index=%d must ALWAYS return the same question content", testIndex)
            .hasSize(1);
    }

    @Test
    void getQuestionForAttempt_DifferentIndexes_ShouldReturnDifferentQuestions() {
        // Arrange
        int requestedQuestions = 10;
        ExamConfigDto config = createExamConfig(ExamMode.PRACTICE, requestedQuestions);
        Attempt attempt = attemptService.createAttempt(config);

        // Act - Get questions at different indexes
        Map<Integer, Long> indexToQuestionId = new HashMap<>();
        for (int index = 0; index < requestedQuestions; index++) {
            QuestionDto question = attemptService.getQuestionForAttempt(
                attempt.getId(),
                index,
                ExamMode.PRACTICE,
                "en"
            );
            indexToQuestionId.put(index, question.getId());
        }

        // Assert - All indexes should map to different questions
        Set<Long> uniqueQuestionIds = new HashSet<>(indexToQuestionId.values());
        assertThat(uniqueQuestionIds)
            .as("Each index should point to a different question")
            .hasSize(requestedQuestions);
    }

    @Test
    void databaseConstraint_ShouldPreventDuplicateQuestionInAttempt() {
        // Arrange
        Attempt attempt = new Attempt();
        attempt.setMode(ExamMode.EXAM);
        attempt.setTotalQuestions(2);
        attempt = attemptRepository.saveAndFlush(attempt);

        Long duplicateQuestionId = testQuestions.get(0).getId();

        // Add question at position 0
        AttemptAnswer answer1 = new AttemptAnswer();
        answer1.setAttempt(attempt);
        answer1.setQuestionId(duplicateQuestionId);
        answer1.setPosition(0);
        answer1.setMarked(false);
        attemptAnswerRepository.saveAndFlush(answer1);

        // Act & Assert - Try to add same question at position 1
        AttemptAnswer answer2 = new AttemptAnswer();
        answer2.setAttempt(attempt);
        answer2.setQuestionId(duplicateQuestionId); // DUPLICATE question_id
        answer2.setPosition(1);
        answer2.setMarked(false);

        assertThatThrownBy(() -> {
            attemptAnswerRepository.saveAndFlush(answer2);
        })
            .isInstanceOf(DataIntegrityViolationException.class)
            .as("Database must prevent duplicate (attempt_id, question_id)");
    }

    @Test
    void databaseConstraint_ShouldPreventDuplicatePositionInAttempt() {
        // Arrange
        Attempt attempt = new Attempt();
        attempt.setMode(ExamMode.PRACTICE);
        attempt.setTotalQuestions(2);
        attempt = attemptRepository.saveAndFlush(attempt);

        // Add question1 at position 0
        AttemptAnswer answer1 = new AttemptAnswer();
        answer1.setAttempt(attempt);
        answer1.setQuestionId(testQuestions.get(0).getId());
        answer1.setPosition(0);
        answer1.setMarked(false);
        attemptAnswerRepository.saveAndFlush(answer1);

        // Act & Assert - Try to add question2 at same position 0
        AttemptAnswer answer2 = new AttemptAnswer();
        answer2.setAttempt(attempt);
        answer2.setQuestionId(testQuestions.get(1).getId()); // Different question
        answer2.setPosition(0); // DUPLICATE position
        answer2.setMarked(false);

        assertThatThrownBy(() -> {
            attemptAnswerRepository.saveAndFlush(answer2);
        })
            .isInstanceOf(DataIntegrityViolationException.class)
            .as("Database must prevent duplicate (attempt_id, position)");
    }

    @Test
    void getQuestionIds_ShouldReturnQuestionsInStablePositionOrder() {
        // Arrange
        ExamConfigDto config = createExamConfig(ExamMode.EXAM, 10);
        Attempt attempt = attemptService.createAttempt(config);

        // Act - Call multiple times
        List<Long> firstCall = attemptService.getQuestionIds(attempt.getId());
        List<Long> secondCall = attemptService.getQuestionIds(attempt.getId());
        List<Long> thirdCall = attemptService.getQuestionIds(attempt.getId());

        // Assert - Order must be identical across calls
        assertThat(secondCall).containsExactlyElementsOf(firstCall);
        assertThat(thirdCall).containsExactlyElementsOf(firstCall);

        // All IDs should be unique
        Set<Long> uniqueIds = new HashSet<>(firstCall);
        assertThat(uniqueIds).hasSize(firstCall.size());
    }

    @Test
    void getQuestionStates_ShouldReturnStatesInPositionOrder() {
        // Arrange
        ExamConfigDto config = createExamConfig(ExamMode.PRACTICE, 5);
        Attempt attempt = attemptService.createAttempt(config);

        // Act - Call multiple times
        List<String> firstCall = attemptService.getQuestionStates(attempt.getId());
        List<String> secondCall = attemptService.getQuestionStates(attempt.getId());

        // Assert
        assertThat(firstCall).hasSize(5);
        assertThat(secondCall).containsExactlyElementsOf(firstCall);

        // All should be unanswered initially
        firstCall.forEach(state ->
            assertThat(state).isEqualTo("q-unanswered")
        );
    }

    @Test
    void attemptCreation_InPracticeMode_ShouldWorkCorrectly() {
        // Arrange
        int requestedQuestions = 25;
        ExamConfigDto config = createExamConfig(ExamMode.PRACTICE, requestedQuestions);

        // Act
        Attempt attempt = attemptService.createAttempt(config);

        // Assert
        assertThat(attempt.getMode()).isEqualTo(ExamMode.PRACTICE);
        assertThat(attempt.getTotalQuestions()).isEqualTo(requestedQuestions);

        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptOrderByPositionAsc(attempt);
        assertThat(answers).hasSize(requestedQuestions);

        // Verify uniqueness
        Set<Long> questionIds = answers.stream()
            .map(AttemptAnswer::getQuestionId)
            .collect(Collectors.toSet());
        assertThat(questionIds).hasSize(requestedQuestions);
    }

    @Test
    void completeAttempt_ShouldCalculateResultsWithStableOrder() {
        // Arrange
        int requestedQuestions = 10;
        ExamConfigDto config = createExamConfig(ExamMode.EXAM, requestedQuestions);
        Attempt attempt = attemptService.createAttempt(config);

        // Act
        var results = attemptService.completeAttempt(attempt.getId());

        // Assert
        assertThat(results).isNotNull();

        Attempt completedAttempt = attemptRepository.findById(attempt.getId()).orElseThrow();
        assertThat(completedAttempt.getIsCompleted()).isTrue();
        assertThat(completedAttempt.getEndedAt()).isNotNull();
    }

    private ExamConfigDto createExamConfig(ExamMode mode, int numberOfQuestions) {
        ExamConfigDto config = new ExamConfigDto();
        config.setMode(mode);
        config.setNumberOfQuestions(numberOfQuestions);
        config.setSelectedDomains(List.of(Domain.values()));
        config.setIdentityPercentage(20);
        config.setStoragePercentage(20);
        config.setComputePercentage(20);
        config.setNetworkingPercentage(20);
        config.setMonitorPercentage(20);
        return config;
    }
}
