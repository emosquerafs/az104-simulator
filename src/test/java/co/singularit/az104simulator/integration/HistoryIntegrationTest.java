package co.singularit.az104simulator.integration;

import co.singularit.az104simulator.domain.*;
import co.singularit.az104simulator.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class HistoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AttemptRepository attemptRepository;

    @Autowired
    private AttemptAnswerRepository attemptAnswerRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ExamSessionRepository examSessionRepository;

    @Autowired
    private ExamSessionQuestionRepository examSessionQuestionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String studentId;
    private String attemptId;
    private List<Question> testQuestions;

    @BeforeEach
    public void setup() {
        studentId = UUID.randomUUID().toString();
        testQuestions = createTestQuestions();
    }

    @Test
    public void testGetHistory_returnsAttempts() throws Exception {
        // Create a completed attempt
        Attempt attempt = createCompletedAttempt();

        // Call /history with student cookie
        mockMvc.perform(get("/history")
                        .cookie(new Cookie("studentId", studentId)))
                .andExpect(status().isOk())
                .andExpect(view().name("history"))
                .andExpect(model().attributeExists("attempts"))
                .andExpect(model().attribute("attempts", hasSize(1)));
    }

    @Test
    public void testGetHistory_filtersByMode() throws Exception {
        // Create EXAM and PRACTICE attempts
        createCompletedAttempt(ExamMode.EXAM);
        createCompletedAttempt(ExamMode.PRACTICE);

        // Filter by EXAM
        mockMvc.perform(get("/history?mode=EXAM")
                        .cookie(new Cookie("studentId", studentId)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("attempts", hasSize(1)))
                .andExpect(model().attribute("selectedMode", "EXAM"));
    }

    @Test
    public void testGetAttemptDetail_returnsQuestions() throws Exception {
        // Create completed attempt with answers
        Attempt attempt = createCompletedAttempt();

        // Call /history/{attemptId}
        mockMvc.perform(get("/history/" + attempt.getId())
                        .cookie(new Cookie("studentId", studentId)))
                .andExpect(status().isOk())
                .andExpect(view().name("attempt-detail"))
                .andExpect(model().attributeExists("summary"))
                .andExpect(model().attributeExists("questions"))
                .andExpect(model().attribute("questions", aMapWithSize(testQuestions.size())));
    }

    @Test
    public void testGetAttemptDetail_calculatesCorrectness() throws Exception {
        // Create attempt with correct and incorrect answers
        Attempt attempt = createCompletedAttempt();

        mockMvc.perform(get("/history/" + attempt.getId())
                        .cookie(new Cookie("studentId", studentId)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("summary"));
    }

    @Test
    public void testGetAttemptDetail_invalidStudentId_redirects() throws Exception {
        // Create attempt for one student
        Attempt attempt = createCompletedAttempt();

        // Try to access with different student ID
        String otherStudentId = UUID.randomUUID().toString();

        mockMvc.perform(get("/history/" + attempt.getId())
                        .cookie(new Cookie("studentId", otherStudentId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/history"));
    }

    @Test
    public void testGetAttemptDetail_i18n_spanish() throws Exception {
        Attempt attempt = createCompletedAttempt();

        mockMvc.perform(get("/history/" + attempt.getId() + "?lang=es")
                        .cookie(new Cookie("studentId", studentId)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentLang", "es"));
    }

    @Test
    public void testGetAttemptDetail_i18n_english() throws Exception {
        Attempt attempt = createCompletedAttempt();

        mockMvc.perform(get("/history/" + attempt.getId() + "?lang=en")
                        .cookie(new Cookie("studentId", studentId)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentLang", "en"));
    }

    @Test
    public void testGetHistory_emptyState() throws Exception {
        // No attempts created
        mockMvc.perform(get("/history")
                        .cookie(new Cookie("studentId", UUID.randomUUID().toString())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("attempts", hasSize(0)));
    }

    // Helper methods

    private Attempt createCompletedAttempt() {
        return createCompletedAttempt(ExamMode.PRACTICE);
    }

    private Attempt createCompletedAttempt(ExamMode mode) {
        // Create exam session
        ExamSession session = ExamSession.builder()
                .id(UUID.randomUUID().toString())
                .mode(mode)
                .totalQuestions(testQuestions.size())
                .locale("es")
                .createdAt(LocalDateTime.now())
                .build();
        examSessionRepository.save(session);

        // Create session questions
        for (int i = 0; i < testQuestions.size(); i++) {
            ExamSessionQuestion sq = ExamSessionQuestion.builder()
                    .session(session)
                    .question(testQuestions.get(i))
                    .position(i + 1)
                    .servedAt(LocalDateTime.now())
                    .build();
            examSessionQuestionRepository.save(sq);
        }

        // Create attempt
        Attempt attempt = new Attempt();
        attempt.setId(UUID.randomUUID().toString());
        attempt.setMode(mode);
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(30));
        attempt.setEndedAt(LocalDateTime.now());
        attempt.setDurationSeconds(1800); // 30 minutes
        attempt.setTotalQuestions(testQuestions.size());
        attempt.setStudentId(studentId);
        attempt.setSessionId(session.getId());
        attempt.setIsCompleted(true);
        attempt.setScorePercentage(70);
        attemptRepository.save(attempt);

        // Create answers
        for (int i = 0; i < testQuestions.size(); i++) {
            Question q = testQuestions.get(i);
            AttemptAnswer answer = new AttemptAnswer();
            answer.setAttempt(attempt);
            answer.setQuestionId(q.getId());
            answer.setPosition(i);

            // First question correct, second incorrect, third unanswered
            if (i == 0) {
                // Correct answer
                Long correctId = q.getOptions().stream()
                        .filter(OptionItem::getIsCorrect)
                        .findFirst()
                        .map(OptionItem::getId)
                        .orElse(null);
                if (correctId != null) {
                    try {
                        answer.setSelectedOptionIdsJson(objectMapper.writeValueAsString(List.of(correctId)));
                    } catch (Exception e) {
                        // ignore
                    }
                }
            } else if (i == 1) {
                // Incorrect answer
                Long incorrectId = q.getOptions().stream()
                        .filter(opt -> !opt.getIsCorrect())
                        .findFirst()
                        .map(OptionItem::getId)
                        .orElse(null);
                if (incorrectId != null) {
                    try {
                        answer.setSelectedOptionIdsJson(objectMapper.writeValueAsString(List.of(incorrectId)));
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
            // i == 2 remains unanswered (null)

            answer.setMarked(i == 1); // Mark second question
            attempt.addAnswer(answer);
        }

        attemptRepository.save(attempt);
        return attempt;
    }

    private List<Question> createTestQuestions() {
        List<Question> questions = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            Question q = new Question();
            q.setDomain(Domain.COMPUTE);
            q.setDifficulty(Difficulty.MEDIUM);
            q.setQtype(QuestionType.SINGLE);
            q.setStemEn("Test question " + i + " in English");
            q.setStemEs("Pregunta de prueba " + i + " en español");
            q.setExplanationEn("Explanation " + i + " in English");
            q.setExplanationEs("Explicación " + i + " en español");

            // Create options
            for (int j = 0; j < 4; j++) {
                OptionItem option = new OptionItem();
                option.setLabel(String.valueOf((char) ('A' + j)));
                option.setTextEn("Option " + j + " English");
                option.setTextEs("Opción " + j + " Español");
                option.setIsCorrect(j == 0); // First option is correct
                q.addOption(option);
            }

            questionRepository.save(q);
            questions.add(q);
        }

        return questions;
    }
}
