package co.singularit.az104simulator.service;

import co.singularit.az104simulator.domain.*;
import co.singularit.az104simulator.dto.QuestionDto;
import co.singularit.az104simulator.repository.ExamSessionQuestionRepository;
import co.singularit.az104simulator.repository.ExamSessionRepository;
import co.singularit.az104simulator.repository.QuestionRepository;
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

@ExtendWith(MockitoExtension.class)
class ExamSessionServiceTest {

    @Mock
    private ExamSessionRepository examSessionRepository;

    @Mock
    private ExamSessionQuestionRepository examSessionQuestionRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionService questionService;

    @InjectMocks
    private ExamSessionService examSessionService;

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
    void startSession_ShouldCreateSessionWithUniqueQuestions() {
        // Arrange
        int requestedQuestions = 50;
        List<Domain> domains = List.of(Domain.COMPUTE);
        List<Question> selectedQuestions = mockQuestions.subList(0, requestedQuestions);

        when(questionService.getRandomQuestions(domains, requestedQuestions))
            .thenReturn(selectedQuestions);
        when(examSessionRepository.save(any(ExamSession.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(examSessionQuestionRepository.saveAll(anyList()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String sessionId = examSessionService.startSession(
            ExamMode.EXAM,
            requestedQuestions,
            "en",
            domains,
            null
        );

        // Assert
        assertThat(sessionId).isNotNull();
        assertThat(UUID.fromString(sessionId)).isNotNull(); // Valid UUID

        verify(examSessionRepository, times(1)).save(argThat(session ->
            session.getMode() == ExamMode.EXAM &&
            session.getTotalQuestions() == requestedQuestions &&
            session.getLocale().equals("en")
        ));

        verify(examSessionQuestionRepository, times(1)).saveAll(argThat(questions -> {
            List<ExamSessionQuestion> list = (List<ExamSessionQuestion>) questions;

            // Verify count
            if (list.size() != requestedQuestions) return false;

            // Verify all question IDs are unique
            Set<Long> questionIds = list.stream()
                .map(sq -> sq.getQuestion().getId())
                .collect(Collectors.toSet());
            if (questionIds.size() != requestedQuestions) return false;

            // Verify positions are sequential from 1 to N
            List<Integer> positions = list.stream()
                .map(ExamSessionQuestion::getPosition)
                .sorted()
                .collect(Collectors.toList());
            for (int i = 0; i < positions.size(); i++) {
                if (positions.get(i) != i + 1) return false;
            }

            return true;
        }));
    }

    @Test
    void startSession_ShouldThrowException_WhenNotEnoughQuestions() {
        // Arrange
        int requestedQuestions = 50;
        List<Domain> domains = List.of(Domain.COMPUTE);
        List<Question> insufficientQuestions = mockQuestions.subList(0, 30); // Only 30 available

        when(questionService.getRandomQuestions(domains, requestedQuestions))
            .thenReturn(insufficientQuestions);

        // Act & Assert
        assertThatThrownBy(() -> examSessionService.startSession(
            ExamMode.EXAM,
            requestedQuestions,
            "en",
            domains,
            null
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Not enough questions in the bank")
            .hasMessageContaining("Requested: 50")
            .hasMessageContaining("Available: 30");

        // Verify no session or questions were saved
        verify(examSessionRepository, never()).save(any(ExamSession.class));
        verify(examSessionQuestionRepository, never()).saveAll(anyList());
    }

    @Test
    void startSession_ShouldUseDistribution_WhenPercentagesProvided() {
        // Arrange
        int requestedQuestions = 50;
        List<Domain> domains = List.of(Domain.COMPUTE, Domain.STORAGE);
        Map<Domain, Integer> percentages = Map.of(
            Domain.COMPUTE, 60,
            Domain.STORAGE, 40
        );

        List<Question> selectedQuestions = mockQuestions.subList(0, requestedQuestions);

        when(questionService.getRandomQuestionsWithDistribution(domains, requestedQuestions, percentages))
            .thenReturn(selectedQuestions);
        when(examSessionRepository.save(any(ExamSession.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(examSessionQuestionRepository.saveAll(anyList()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String sessionId = examSessionService.startSession(
            ExamMode.PRACTICE,
            requestedQuestions,
            "es",
            domains,
            percentages
        );

        // Assert
        assertThat(sessionId).isNotNull();
        verify(questionService, times(1))
            .getRandomQuestionsWithDistribution(domains, requestedQuestions, percentages);
        verify(questionService, never())
            .getRandomQuestions(any(), anyInt());
    }

    @Test
    void getQuestionByPosition_ShouldReturnQuestion_WhenPositionExists() {
        // Arrange
        String sessionId = UUID.randomUUID().toString();
        int position = 5;
        Question question = mockQuestions.get(0);

        ExamSession session = new ExamSession();
        session.setId(sessionId);

        ExamSessionQuestion sessionQuestion = new ExamSessionQuestion();
        sessionQuestion.setQuestion(question);
        sessionQuestion.setPosition(position);
        sessionQuestion.setSession(session);

        QuestionDto expectedDto = new QuestionDto();
        expectedDto.setId(question.getId());

        when(examSessionQuestionRepository.findBySessionIdAndPosition(sessionId, position))
            .thenReturn(Optional.of(sessionQuestion));
        when(questionService.convertToDto(question, false, "en"))
            .thenReturn(expectedDto);

        // Act
        QuestionDto result = examSessionService.getQuestionByPosition(sessionId, position, false, "en");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(question.getId());
        verify(examSessionQuestionRepository, times(1))
            .findBySessionIdAndPosition(sessionId, position);
    }

    @Test
    void getQuestionByPosition_ShouldReturnNull_WhenPositionNotExists() {
        // Arrange
        String sessionId = UUID.randomUUID().toString();
        int position = 999;

        when(examSessionQuestionRepository.findBySessionIdAndPosition(sessionId, position))
            .thenReturn(Optional.empty());

        // Act
        QuestionDto result = examSessionService.getQuestionByPosition(sessionId, position, false, "en");

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void getSessionQuestionIds_ShouldReturnOrderedIds() {
        // Arrange
        String sessionId = UUID.randomUUID().toString();
        ExamSession session = new ExamSession();
        session.setId(sessionId);

        List<ExamSessionQuestion> sessionQuestions = IntStream.range(0, 10)
            .mapToObj(i -> {
                ExamSessionQuestion sq = new ExamSessionQuestion();
                sq.setSession(session);
                sq.setQuestion(mockQuestions.get(i));
                sq.setPosition(i + 1);
                return sq;
            })
            .collect(Collectors.toList());

        when(examSessionQuestionRepository.findBySessionIdOrderByPosition(sessionId))
            .thenReturn(sessionQuestions);

        // Act
        List<Long> result = examSessionService.getSessionQuestionIds(sessionId);

        // Assert
        assertThat(result).hasSize(10);
        assertThat(result).containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
    }

    @Test
    void validateSessionUniqueness_ShouldReturnTrue_WhenAllQuestionsUnique() {
        // Arrange
        String sessionId = UUID.randomUUID().toString();
        List<Long> uniqueIds = List.of(1L, 2L, 3L, 4L, 5L);

        when(examSessionQuestionRepository.findQuestionIdsBySessionId(sessionId))
            .thenReturn(uniqueIds);

        // Act
        boolean result = examSessionService.validateSessionUniqueness(sessionId);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void validateSessionUniqueness_ShouldReturnFalse_WhenDuplicatesExist() {
        // Arrange
        String sessionId = UUID.randomUUID().toString();
        List<Long> duplicateIds = List.of(1L, 2L, 3L, 2L, 5L); // 2 appears twice

        when(examSessionQuestionRepository.findQuestionIdsBySessionId(sessionId))
            .thenReturn(duplicateIds);

        // Act
        boolean result = examSessionService.validateSessionUniqueness(sessionId);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void isSessionActive_ShouldReturnTrue_WhenSessionExistsAndNotCompleted() {
        // Arrange
        String sessionId = UUID.randomUUID().toString();
        when(examSessionRepository.existsActiveSession(sessionId)).thenReturn(true);

        // Act
        boolean result = examSessionService.isSessionActive(sessionId);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void completeSession_ShouldUpdateCompletedAt() {
        // Arrange
        String sessionId = UUID.randomUUID().toString();
        ExamSession session = new ExamSession();
        session.setId(sessionId);
        session.setCompletedAt(null);

        when(examSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(examSessionRepository.save(any(ExamSession.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        examSessionService.completeSession(sessionId);

        // Assert
        verify(examSessionRepository, times(1)).save(argThat(s ->
            s.getId().equals(sessionId) && s.getCompletedAt() != null
        ));
    }

    @Test
    void getSession_ShouldThrowException_WhenSessionNotFound() {
        // Arrange
        String sessionId = UUID.randomUUID().toString();
        when(examSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> examSessionService.getSession(sessionId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Session not found");
    }
}
