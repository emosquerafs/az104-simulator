package co.singularit.az104simulator.service;

import co.singularit.az104simulator.domain.*;
import co.singularit.az104simulator.dto.ExamConfigDto;
import co.singularit.az104simulator.dto.QuestionDto;
import co.singularit.az104simulator.repository.AttemptAnswerRepository;
import co.singularit.az104simulator.repository.AttemptRepository;
import co.singularit.az104simulator.repository.QuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for AttemptService focusing on question uniqueness and order stability.
 * This prevents the "question repetition" bug where the same index would show different questions.
 */
@ExtendWith(MockitoExtension.class)
class AttemptServiceTest {

    @Mock
    private AttemptRepository attemptRepository;

    @Mock
    private AttemptAnswerRepository attemptAnswerRepository;

    @Mock
    private QuestionService questionService;

    @Mock
    private ScoringService scoringService;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AttemptService attemptService;

    private List<Question> mockQuestions;

    @BeforeEach
    void setUp() {
        // Create 100 mock questions
        mockQuestions = IntStream.range(1, 101)
            .mapToObj(i -> {
                Question q = new Question();
                q.setId((long) i);
                q.setDomain(Domain.COMPUTE);
                q.setDifficulty(Difficulty.MEDIUM);
                q.setQtype(QuestionType.SINGLE);
                q.setStemEn("Question " + i);
                q.setStemEs("Pregunta " + i);
                q.setExplanationEn("Explanation " + i);
                q.setExplanationEs("Explicación " + i);
                q.setOptions(new ArrayList<>());
                return q;
            })
            .collect(Collectors.toList());
    }

    @Test
    void createAttempt_ShouldNotContainDuplicateQuestionIds() {
        // Arrange
        int requestedQuestions = 50;
        List<Question> selectedQuestions = mockQuestions.subList(0, requestedQuestions);

        ExamConfigDto config = new ExamConfigDto();
        config.setMode(ExamMode.EXAM);
        config.setNumberOfQuestions(requestedQuestions);
        config.setSelectedDomains(List.of(Domain.COMPUTE));
        config.setIdentityPercentage(20);
        config.setStoragePercentage(20);
        config.setComputePercentage(20);
        config.setNetworkingPercentage(20);
        config.setMonitorPercentage(20);

        when(questionService.getRandomQuestionsWithDistribution(any(), anyInt(), any()))
            .thenReturn(selectedQuestions);

        when(attemptRepository.save(any(Attempt.class)))
            .thenAnswer(invocation -> {
                Attempt attempt = invocation.getArgument(0);
                if (attempt.getId() == null) {
                    attempt.setId(UUID.randomUUID().toString());
                }
                return attempt;
            });

        // Act
        Attempt attempt = attemptService.createAttempt(config);

        // Assert
        assertThat(attempt).isNotNull();
        assertThat(attempt.getAnswers()).hasSize(requestedQuestions);

        // Verify all question IDs are unique
        Set<Long> questionIds = attempt.getAnswers().stream()
            .map(AttemptAnswer::getQuestionId)
            .collect(Collectors.toSet());
        assertThat(questionIds)
            .as("All question IDs should be unique - no duplicates allowed")
            .hasSize(requestedQuestions);

        // Verify positions are sequential from 0 to N-1
        List<Integer> positions = attempt.getAnswers().stream()
            .map(AttemptAnswer::getPosition)
            .sorted()
            .collect(Collectors.toList());
        for (int i = 0; i < positions.size(); i++) {
            assertThat(positions.get(i))
                .as("Position should be sequential starting from 0")
                .isEqualTo(i);
        }

        // Verify no duplicate positions
        Set<Integer> uniquePositions = new HashSet<>(positions);
        assertThat(uniquePositions)
            .as("All positions should be unique")
            .hasSize(requestedQuestions);
    }

    @Test
    void getQuestionForAttempt_ShouldReturnSameQuestionForSameIndex_Over20Calls() {
        // Arrange
        String attemptId = UUID.randomUUID().toString();
        int testIndex = 0;
        int numberOfCalls = 20;

        Attempt attempt = new Attempt();
        attempt.setId(attemptId);
        attempt.setMode(ExamMode.EXAM);

        // Create mock answers with stable positions
        List<AttemptAnswer> answers = IntStream.range(0, 50)
            .mapToObj(i -> {
                AttemptAnswer answer = new AttemptAnswer();
                answer.setQuestionId((long) (i + 1));
                answer.setPosition(i);
                answer.setAttempt(attempt);
                answer.setMarked(false);
                return answer;
            })
            .collect(Collectors.toList());

        Question expectedQuestion = mockQuestions.get(testIndex);
        expectedQuestion.setId(answers.get(testIndex).getQuestionId());

        QuestionDto expectedDto = new QuestionDto();
        expectedDto.setId(expectedQuestion.getId());
        expectedDto.setStem("Question " + expectedQuestion.getId());

        when(attemptRepository.findById(attemptId))
            .thenReturn(Optional.of(attempt));

        // CRITICAL: Repository must return answers in the same order every time
        when(attemptAnswerRepository.findByAttemptOrderByPositionAsc(attempt))
            .thenReturn(answers);

        when(questionRepository.findById(answers.get(testIndex).getQuestionId()))
            .thenReturn(Optional.of(expectedQuestion));

        when(questionService.convertToDto(expectedQuestion, false, "es"))
            .thenReturn(expectedDto);

        // Act - Call the same method 20 times
        Set<Long> returnedQuestionIds = new HashSet<>();
        for (int call = 0; call < numberOfCalls; call++) {
            QuestionDto result = attemptService.getQuestionForAttempt(attemptId, testIndex, ExamMode.EXAM, "es");
            returnedQuestionIds.add(result.getId());
        }

        // Assert
        assertThat(returnedQuestionIds)
            .as("Calling getQuestionForAttempt 20 times with the same index should ALWAYS return the same question ID")
            .hasSize(1)
            .containsExactly(expectedQuestion.getId());

        // Verify the repository method was called 20 times and returned stable results
        verify(attemptAnswerRepository, times(numberOfCalls))
            .findByAttemptOrderByPositionAsc(attempt);

        // Verify we used questionRepository.findById (efficient) not questionService.getRandomQuestions (inefficient)
        verify(questionRepository, times(numberOfCalls))
            .findById(answers.get(testIndex).getQuestionId());
        verify(questionService, never())
            .getRandomQuestions(any(), anyInt());
    }

    @Test
    void createAttempt_ShouldThrowException_WhenDuplicateQuestionsDetected() {
        // Arrange - Simulate a bug where questionService returns duplicates
        List<Question> duplicateQuestions = new ArrayList<>();
        duplicateQuestions.add(mockQuestions.get(0)); // ID 1
        duplicateQuestions.add(mockQuestions.get(1)); // ID 2
        duplicateQuestions.add(mockQuestions.get(0)); // ID 1 again (duplicate!)

        ExamConfigDto config = new ExamConfigDto();
        config.setMode(ExamMode.PRACTICE);
        config.setNumberOfQuestions(3);
        config.setSelectedDomains(List.of(Domain.COMPUTE));
        config.setIdentityPercentage(20);
        config.setStoragePercentage(20);
        config.setComputePercentage(20);
        config.setNetworkingPercentage(20);
        config.setMonitorPercentage(20);

        when(questionService.getRandomQuestionsWithDistribution(any(), anyInt(), any()))
            .thenReturn(duplicateQuestions);

        when(attemptRepository.save(any(Attempt.class)))
            .thenAnswer(invocation -> {
                Attempt attempt = invocation.getArgument(0);
                if (attempt.getId() == null) {
                    attempt.setId(UUID.randomUUID().toString());
                }
                return attempt;
            });

        // Act & Assert
        assertThatThrownBy(() -> attemptService.createAttempt(config))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Duplicate questions detected");

        // Verify the attempt was not fully saved due to validation failure
        verify(attemptRepository, atMost(1)).save(any(Attempt.class));
    }

    @Test
    void getQuestionIds_ShouldReturnQuestionsInStableOrder() {
        // Arrange
        String attemptId = UUID.randomUUID().toString();
        Attempt attempt = new Attempt();
        attempt.setId(attemptId);

        List<AttemptAnswer> answers = IntStream.range(0, 10)
            .mapToObj(i -> {
                AttemptAnswer answer = new AttemptAnswer();
                answer.setQuestionId((long) (10 - i)); // Reverse order IDs
                answer.setPosition(i); // But sequential positions
                answer.setAttempt(attempt);
                return answer;
            })
            .collect(Collectors.toList());

        when(attemptRepository.findById(attemptId))
            .thenReturn(Optional.of(attempt));
        when(attemptAnswerRepository.findByAttemptOrderByPositionAsc(attempt))
            .thenReturn(answers);

        // Act
        List<Long> result = attemptService.getQuestionIds(attemptId);

        // Assert - Should follow position order, not question ID order
        assertThat(result).containsExactly(10L, 9L, 8L, 7L, 6L, 5L, 4L, 3L, 2L, 1L);
    }

    @Test
    void getQuestionStates_ShouldReturnStatesInSameOrderAsPositions() {
        // Arrange
        String attemptId = UUID.randomUUID().toString();
        Attempt attempt = new Attempt();
        attempt.setId(attemptId);

        List<AttemptAnswer> answers = IntStream.range(0, 5)
            .mapToObj(i -> {
                AttemptAnswer answer = new AttemptAnswer();
                answer.setQuestionId((long) (i + 1));
                answer.setPosition(i);
                answer.setAttempt(attempt);
                answer.setMarked(i % 2 == 0); // Mark even positions
                answer.setSelectedOptionIdsJson(i < 3 ? "[1]" : null); // Answer first 3
                return answer;
            })
            .collect(Collectors.toList());

        when(attemptRepository.findById(attemptId))
            .thenReturn(Optional.of(attempt));
        when(attemptAnswerRepository.findByAttemptOrderByPositionAsc(attempt))
            .thenReturn(answers);

        // Act
        List<String> states = attemptService.getQuestionStates(attemptId);

        // Assert
        assertThat(states).hasSize(5);
        assertThat(states.get(0)).isEqualTo("q-answered q-marked"); // pos 0: answered + marked
        assertThat(states.get(1)).isEqualTo("q-answered");          // pos 1: answered only
        assertThat(states.get(2)).isEqualTo("q-answered q-marked"); // pos 2: answered + marked
        assertThat(states.get(3)).isEqualTo("q-unanswered");        // pos 3: unanswered only
        assertThat(states.get(4)).isEqualTo("q-marked");            // pos 4: marked only
    }
}
