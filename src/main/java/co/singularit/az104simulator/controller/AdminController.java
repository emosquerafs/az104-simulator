package co.singularit.az104simulator.controller;

import co.singularit.az104simulator.domain.OptionItem;
import co.singularit.az104simulator.domain.Question;
import co.singularit.az104simulator.repository.QuestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    @GetMapping("/export")
    public ResponseEntity<String> exportQuestions() {
        try {
            List<Question> questions = questionRepository.findAll();
            List<Map<String, Object>> exportData = new ArrayList<>();

            for (Question q : questions) {
                Map<String, Object> questionData = new HashMap<>();
                questionData.put("domain", q.getDomain().name());
                questionData.put("difficulty", q.getDifficulty().name());
                questionData.put("qtype", q.getQtype().name());
                questionData.put("stem", q.getStem());
                questionData.put("explanation", q.getExplanation());

                // Parse tags
                if (q.getTagsJson() != null) {
                    List<String> tags = objectMapper.readValue(q.getTagsJson(), new TypeReference<List<String>>() {});
                    questionData.put("tags", tags);
                } else {
                    questionData.put("tags", new ArrayList<>());
                }

                // Convert options
                List<Map<String, Object>> optionsData = new ArrayList<>();
                for (OptionItem opt : q.getOptions()) {
                    Map<String, Object> optionData = new HashMap<>();
                    optionData.put("label", opt.getLabel());
                    optionData.put("text", opt.getText());
                    optionData.put("isCorrect", opt.getIsCorrect());
                    optionsData.add(optionData);
                }
                questionData.put("options", optionsData);

                exportData.add(questionData);
            }

            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportData);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=questions.json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json);

        } catch (Exception e) {
            log.error("Error exporting questions", e);
            return ResponseEntity.internalServerError().body("{\"error\": \"Failed to export questions\"}");
        }
    }

    @PostMapping("/import")
    @Transactional
    public ResponseEntity<Map<String, Object>> importQuestions(@RequestParam("file") MultipartFile file) {
        try {
            // Delete existing questions
            questionRepository.deleteAll();

            // Parse JSON
            List<Map<String, Object>> questionsData = objectMapper.readValue(
                    file.getInputStream(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            // Import questions
            for (Map<String, Object> questionData : questionsData) {
                Question question = new Question();
                question.setDomain(co.singularit.az104simulator.domain.Domain.valueOf((String) questionData.get("domain")));
                question.setDifficulty(co.singularit.az104simulator.domain.Difficulty.valueOf((String) questionData.get("difficulty")));
                question.setQtype(co.singularit.az104simulator.domain.QuestionType.valueOf((String) questionData.get("qtype")));
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
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Imported " + questionsData.size() + " questions",
                    "count", questionsData.size()
            ));

        } catch (Exception e) {
            log.error("Error importing questions", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
