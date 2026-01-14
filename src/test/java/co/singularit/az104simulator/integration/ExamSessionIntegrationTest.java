package co.singularit.az104simulator.integration;

import co.singularit.az104simulator.domain.*;
import co.singularit.az104simulator.repository.ExamSessionQuestionRepository;
import co.singularit.az104simulator.repository.ExamSessionRepository;
import co.singularit.az104simulator.repository.QuestionRepository;
import co.singularit.az104simulator.service.ExamSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for ExamSession functionality
 * Tests database constraints and transaction handling
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExamSessionIntegrationTest {

    @Autowired
    private ExamSessionService examSessionService;

    @Autowired
    private ExamSessionRepository examSessionRepository;

    @Autowired
    private ExamSessionQuestionRepository examSessionQuestionRepository;

    @Autowired
    private QuestionRepository questionRepository;

    private List<Question> testQuestions;

    @BeforeEach
    void setUp() {
        // Clean up
        examSessionQuestionRepository.deleteAll();
        examSessionRepository.deleteAll();

        // Create 100 test questions if they don't exist
        long existingCount = questionRepository.count();
        if (existingCount < 100) {
            testQuestions = IntStream.range(1, 101)
                .mapToObj(i -> {
                    Question q = new Question();
                    q.setDomain(Domain.COMPUTE);
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
    void startSession_ShouldCreateSessionWithUniqueQuestions() {
        // Arrange
        int requestedQuestions = 50;
        List<Domain> domains = List.of(Domain.COMPUTE);

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

        ExamSession session = examSessionRepository.findById(sessionId).orElseThrow();
        assertThat(session.getMode()).isEqualTo(ExamMode.EXAM);
        assertThat(session.getTotalQuestions()).isEqualTo(requestedQuestions);
        assertThat(session.getLocale()).isEqualTo("en");
        assertThat(session.getCreatedAt()).isNotNull();
        assertThat(session.getCompletedAt()).isNull();

        List<ExamSessionQuestion> sessionQuestions =
            examSessionQuestionRepository.findBySessionIdOrderByPosition(sessionId);

        assertThat(sessionQuestions).hasSize(requestedQuestions);

        // Verify all question IDs are unique
        Set<Long> questionIds = sessionQuestions.stream()
            .map(sq -> sq.getQuestion().getId())
            .collect(Collectors.toSet());
        assertThat(questionIds).hasSize(requestedQuestions);

        // Verify positions are sequential from 1 to N
        List<Integer> positions = sessionQuestions.stream()
            .map(ExamSessionQuestion::getPosition)
            .sorted()
            .collect(Collectors.toList());
        for (int i = 0; i < positions.size(); i++) {
            assertThat(positions.get(i)).isEqualTo(i + 1);
        }
    }

    @Test
    void databaseConstraint_ShouldPreventDuplicateQuestionInSession() {
        // Arrange
        String sessionId = UUID.randomUUID().toString();
        Question question = testQuestions.get(0);

        ExamSession session = ExamSession.builder()
            .id(sessionId)
            .mode(ExamMode.EXAM)
            .totalQuestions(50)
            .locale("en")
            .createdAt(LocalDateTime.now())
            .build();
        examSessionRepository.saveAndFlush(session);

        // Add same question at position 1
        ExamSessionQuestion sq1 = ExamSessionQuestion.builder()
            .session(session)
            .question(question)
            .position(1)
            .servedAt(LocalDateTime.now())
            .build();
        examSessionQuestionRepository.saveAndFlush(sq1);

        // Act & Assert - Try to add same question at position 2
        ExamSessionQuestion sq2 = ExamSessionQuestion.builder()
            .session(session)
            .question(question)
            .position(2)
            .servedAt(LocalDateTime.now())
            .build();

        assertThatThrownBy(() -> {
            examSessionQuestionRepository.saveAndFlush(sq2);
        })
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("UNIQUE_SESSION_QUESTION");
    }

    @Test
    void databaseConstraint_ShouldPreventDuplicatePositionInSession() {
        // Arrange
        String sessionId = UUID.randomUUID().toString();
        Question question1 = testQuestions.get(0);
        Question question2 = testQuestions.get(1);

        ExamSession session = ExamSession.builder()
            .id(sessionId)
            .mode(ExamMode.EXAM)
            .totalQuestions(50)
            .locale("en")
            .createdAt(LocalDateTime.now())
            .build();
        examSessionRepository.saveAndFlush(session);

        // Add question1 at position 1
        ExamSessionQuestion sq1 = ExamSessionQuestion.builder()
            .session(session)
            .question(question1)
            .position(1)
            .servedAt(LocalDateTime.now())
            .build();
        examSessionQuestionRepository.saveAndFlush(sq1);

        // Act & Assert - Try to add question2 at same position 1
        ExamSessionQuestion sq2 = ExamSessionQuestion.builder()
            .session(session)
            .question(question2)
            .position(1)
            .servedAt(LocalDateTime.now())
            .build();

        assertThatThrownBy(() -> {
            examSessionQuestionRepository.saveAndFlush(sq2);
        })
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("UNIQUE_SESSION_POSITION");
    }

    @Test
    void startSession_ShouldAllowSameQuestionInDifferentSessions() {
        // Arrange
        int requestedQuestions = 10;
        List<Domain> domains = List.of(Domain.COMPUTE);

        // Act - Create two sessions
        String sessionId1 = examSessionService.startSession(
            ExamMode.EXAM,
            requestedQuestions,
            "en",
            domains,
            null
        );

        String sessionId2 = examSessionService.startSession(
            ExamMode.PRACTICE,
            requestedQuestions,
            "es",
            domains,
            null
        );

        // Assert - Both sessions created successfully
        assertThat(sessionId1).isNotNull();
        assertThat(sessionId2).isNotNull();
        assertThat(sessionId1).isNotEqualTo(sessionId2);

        List<Long> questionIds1 = examSessionService.getSessionQuestionIds(sessionId1);
        List<Long> questionIds2 = examSessionService.getSessionQuestionIds(sessionId2);

        assertThat(questionIds1).hasSize(requestedQuestions);
        assertThat(questionIds2).hasSize(requestedQuestions);

        // Questions may overlap between sessions (this is allowed)
        // Just verify each session has unique questions within itself
        Set<Long> uniqueIds1 = new HashSet<>(questionIds1);
        Set<Long> uniqueIds2 = new HashSet<>(questionIds2);

        assertThat(uniqueIds1).hasSize(requestedQuestions);
        assertThat(uniqueIds2).hasSize(requestedQuestions);
    }

    @Test
    void getQuestionByPosition_ShouldReturnCorrectQuestion() {
        // Arrange
        int requestedQuestions = 10;
        List<Domain> domains = List.of(Domain.COMPUTE);

        String sessionId = examSessionService.startSession(
            ExamMode.PRACTICE,
            requestedQuestions,
            "en",
            domains,
            null
        );

        // Act - Get question at position 5
        var question = examSessionService.getQuestionByPosition(sessionId, 5, true, "en");

        // Assert
        assertThat(question).isNotNull();
        assertThat(question.getId()).isNotNull();
        assertThat(question.getStem()).isNotBlank();
    }

    @Test
    void validateSessionUniqueness_ShouldReturnTrue_ForValidSession() {
        // Arrange
        int requestedQuestions = 50;
        List<Domain> domains = List.of(Domain.COMPUTE);

        String sessionId = examSessionService.startSession(
            ExamMode.EXAM,
            requestedQuestions,
            "en",
            domains,
            null
        );

        // Act
        boolean isUnique = examSessionService.validateSessionUniqueness(sessionId);

        // Assert
        assertThat(isUnique).isTrue();
    }

    @Test
    void completeSession_ShouldUpdateCompletedTimestamp() {
        // Arrange
        int requestedQuestions = 10;
        List<Domain> domains = List.of(Domain.COMPUTE);

        String sessionId = examSessionService.startSession(
            ExamMode.EXAM,
            requestedQuestions,
            "en",
            domains,
            null
        );

        ExamSession sessionBefore = examSessionRepository.findById(sessionId).orElseThrow();
        assertThat(sessionBefore.getCompletedAt()).isNull();

        // Act
        examSessionService.completeSession(sessionId);

        // Assert
        ExamSession sessionAfter = examSessionRepository.findById(sessionId).orElseThrow();
        assertThat(sessionAfter.getCompletedAt()).isNotNull();
        assertThat(sessionAfter.getCompletedAt()).isAfter(sessionAfter.getCreatedAt());
    }

    // Note: CASCADE DELETE is configured at the database level in V5__exam_session_question.sql
    // The ON DELETE CASCADE foreign key ensures that when a session is deleted,
    // all associated session questions are automatically deleted by the database

    @Test
    void getSessionSummary_ShouldReturnAllQuestionsInOrder() {
        // Arrange
        int requestedQuestions = 20;
        List<Domain> domains = List.of(Domain.COMPUTE);

        String sessionId = examSessionService.startSession(
            ExamMode.EXAM,
            requestedQuestions,
            "en",
            domains,
            null
        );

        // Act
        Map<Integer, ?> summary = examSessionService.getSessionSummary(sessionId, "en");

        // Assert
        assertThat(summary).hasSize(requestedQuestions);
        assertThat(summary.keySet()).containsExactlyElementsOf(
            IntStream.rangeClosed(1, requestedQuestions).boxed().collect(Collectors.toList())
        );
    }
}
