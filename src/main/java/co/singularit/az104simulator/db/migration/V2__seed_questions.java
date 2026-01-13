package co.singularit.az104simulator.db.migration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

public class V2__seed_questions extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        // Load questions from JSON
        ClassPathResource resource = new ClassPathResource("seed/questions.json");
        InputStream inputStream = resource.getInputStream();

        List<Map<String, Object>> questions = objectMapper.readValue(
                inputStream,
                new TypeReference<List<Map<String, Object>>>() {}
        );

        // Insert questions and options
        String insertQuestionSql = "INSERT INTO question (domain, difficulty, qtype, stem, explanation, tags_json) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        String insertOptionSql = "INSERT INTO option_item (question_id, label, text, is_correct) " +
                "VALUES (?, ?, ?, ?)";

        for (Map<String, Object> questionData : questions) {
            // Insert question
            try (PreparedStatement questionStmt = context.getConnection().prepareStatement(
                    insertQuestionSql, Statement.RETURN_GENERATED_KEYS)) {

                questionStmt.setString(1, (String) questionData.get("domain"));
                questionStmt.setString(2, (String) questionData.get("difficulty"));
                questionStmt.setString(3, (String) questionData.get("qtype"));
                questionStmt.setString(4, (String) questionData.get("stem"));
                questionStmt.setString(5, (String) questionData.get("explanation"));

                // Serialize tags to JSON
                @SuppressWarnings("unchecked")
                List<String> tags = (List<String>) questionData.get("tags");
                String tagsJson = objectMapper.writeValueAsString(tags);
                questionStmt.setString(6, tagsJson);

                questionStmt.executeUpdate();

                // Get generated question ID
                ResultSet generatedKeys = questionStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    long questionId = generatedKeys.getLong(1);

                    // Insert options
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> options = (List<Map<String, Object>>) questionData.get("options");

                    for (Map<String, Object> option : options) {
                        try (PreparedStatement optionStmt = context.getConnection().prepareStatement(insertOptionSql)) {
                            optionStmt.setLong(1, questionId);
                            optionStmt.setString(2, (String) option.get("label"));
                            optionStmt.setString(3, (String) option.get("text"));
                            optionStmt.setBoolean(4, (Boolean) option.get("isCorrect"));
                            optionStmt.executeUpdate();
                        }
                    }
                }
            }
        }

        System.out.println("Successfully seeded " + questions.size() + " questions");
    }
}
