package co.singularit.az104simulator.dto;

import co.singularit.az104simulator.domain.Difficulty;
import co.singularit.az104simulator.domain.Domain;
import co.singularit.az104simulator.domain.QuestionType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuestionReviewDto {
    private Long questionId;
    private Integer position;
    private Domain domain;
    private Difficulty difficulty;
    private QuestionType qtype;
    private String stem;
    private String explanation;
    private List<OptionReviewDto> options;
    private List<Long> selectedOptionIds;
    private List<Long> correctOptionIds;
    private Boolean isCorrect;
    private Boolean isAnswered;
    private Boolean marked;

    @Data
    @Builder
    public static class OptionReviewDto {
        private Long id;
        private String label;
        private String text;
        private Boolean isCorrect;
        private Boolean isSelected;
    }
}
