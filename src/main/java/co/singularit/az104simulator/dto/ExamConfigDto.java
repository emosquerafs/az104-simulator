package co.singularit.az104simulator.dto;

import co.singularit.az104simulator.domain.Domain;
import co.singularit.az104simulator.domain.ExamMode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ExamConfigDto {
    private ExamMode mode;
    private Integer numberOfQuestions = 50;
    private Integer timeLimitMinutes = 100;
    private List<Domain> selectedDomains = new ArrayList<>();
    private Boolean showExplanationsImmediately = false;

    // Distribution percentages (optional, defaults will be used if not set)
    private Integer identityPercentage = 23;
    private Integer storagePercentage = 18;
    private Integer computePercentage = 23;
    private Integer networkingPercentage = 18;
    private Integer monitorPercentage = 18;
}
