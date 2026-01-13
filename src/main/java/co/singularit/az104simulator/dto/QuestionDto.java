package co.singularit.az104simulator.dto;

import co.singularit.az104simulator.domain.Difficulty;
import co.singularit.az104simulator.domain.Domain;
import co.singularit.az104simulator.domain.QuestionType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuestionDto {
    private Long id;
    private Domain domain;
    private Difficulty difficulty;
    private QuestionType qtype;
    private String stem;
    private String explanation;
    private List<OptionDto> options;
    private List<String> tags;
    private List<Long> selectedOptionIds;
    private Boolean marked;
    private Boolean answered;
}
