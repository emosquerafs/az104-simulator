package co.singularit.az104simulator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionSummaryDto {

    private String sessionId;

    private Integer totalQuestions;

    /**
     * Map of position (1-indexed) to question details
     * This ensures stable ordering and no duplicates
     */
    private Map<Integer, QuestionSummaryItem> questions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionSummaryItem {
        private Long questionId;
        private Boolean answered;
        private Boolean markedForReview;
        private Boolean isCorrect; // Only populated in PRACTICE mode
    }
}
