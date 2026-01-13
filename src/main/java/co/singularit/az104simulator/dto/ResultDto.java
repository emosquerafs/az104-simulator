package co.singularit.az104simulator.dto;

import co.singularit.az104simulator.domain.Domain;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ResultDto {
    private String attemptId;
    private Double score;
    private Integer correctAnswers;
    private Integer incorrectAnswers;
    private Integer totalQuestions;
    private Integer durationSeconds;
    private Double averageTimePerQuestion;
    private Map<Domain, DomainBreakdown> domainBreakdowns = new HashMap<>();
    private List<QuestionResultDto> questionResults = new ArrayList<>();

    @Getter
    @Setter
    public static class DomainBreakdown {
        private Domain domain;
        private Integer correct;
        private Integer total;
        private Double percentage;
    }
}
