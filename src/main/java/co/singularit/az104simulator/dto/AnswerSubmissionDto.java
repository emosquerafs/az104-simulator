package co.singularit.az104simulator.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AnswerSubmissionDto {
    private Long questionId;
    private List<Long> selectedOptionIds;
    private Boolean marked;
}
