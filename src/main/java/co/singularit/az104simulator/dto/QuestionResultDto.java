package co.singularit.az104simulator.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuestionResultDto {
    private Long questionId;
    private String stem;
    private String explanation;
    private List<Long> correctOptionIds;
    private List<Long> selectedOptionIds;
    private Boolean isCorrect;
    private List<OptionDto> options;
}
