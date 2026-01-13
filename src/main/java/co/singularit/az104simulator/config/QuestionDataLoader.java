package co.singularit.az104simulator.config;

import co.singularit.az104simulator.domain.Difficulty;
import co.singularit.az104simulator.domain.Domain;
import co.singularit.az104simulator.domain.OptionItem;
import co.singularit.az104simulator.domain.Question;
import co.singularit.az104simulator.domain.QuestionType;
import co.singularit.az104simulator.repository.QuestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuestionDataLoader implements CommandLineRunner {

    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Check if questions already loaded
        if (questionRepository.count() > 0) {
            log.info("Questions already loaded, skipping seed");
            return;
        }

        log.info("Loading questions from JSON...");

        // Load questions from JSON
        ClassPathResource resource = new ClassPathResource("seed/questions.json");
        InputStream inputStream = resource.getInputStream();

        List<Map<String, Object>> questionsData = objectMapper.readValue(
                inputStream,
                new TypeReference<List<Map<String, Object>>>() {}
        );

        int count = 0;
        for (Map<String, Object> questionData : questionsData) {
            Question question = new Question();
            question.setDomain(Domain.valueOf((String) questionData.get("domain")));
            question.setDifficulty(Difficulty.valueOf((String) questionData.get("difficulty")));
            question.setQtype(QuestionType.valueOf((String) questionData.get("qtype")));
            question.setStem((String) questionData.get("stem"));
            question.setExplanation((String) questionData.get("explanation"));

            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) questionData.get("tags");
            question.setTagsJson(objectMapper.writeValueAsString(tags));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> optionsData = (List<Map<String, Object>>) questionData.get("options");

            for (Map<String, Object> optionData : optionsData) {
                OptionItem option = new OptionItem();
                option.setLabel((String) optionData.get("label"));
                option.setText((String) optionData.get("text"));
                option.setIsCorrect((Boolean) optionData.get("isCorrect"));
                question.addOption(option);
            }

            questionRepository.save(question);
            count++;
        }

        log.info("Successfully loaded {} questions from JSON", count);
    }
}
