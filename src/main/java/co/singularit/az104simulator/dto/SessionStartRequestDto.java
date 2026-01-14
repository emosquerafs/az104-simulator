package co.singularit.az104simulator.dto;

import co.singularit.az104simulator.domain.Domain;
import co.singularit.az104simulator.domain.ExamMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionStartRequestDto {

    private ExamMode mode;

    private Integer totalQuestions;

    private String locale;

    private List<Domain> selectedDomains;

    // Domain distribution percentages (optional)
    private Integer identityPercentage;
    private Integer storagePercentage;
    private Integer computePercentage;
    private Integer networkingPercentage;
    private Integer monitorPercentage;

    /**
     * Convert percentage fields to domain distribution map
     */
    public Map<Domain, Integer> getDomainPercentages() {
        if (identityPercentage == null && storagePercentage == null &&
            computePercentage == null && networkingPercentage == null &&
            monitorPercentage == null) {
            return null;
        }

        return Map.of(
            Domain.IDENTITY_GOVERNANCE, identityPercentage != null ? identityPercentage : 0,
            Domain.STORAGE, storagePercentage != null ? storagePercentage : 0,
            Domain.COMPUTE, computePercentage != null ? computePercentage : 0,
            Domain.NETWORKING, networkingPercentage != null ? networkingPercentage : 0,
            Domain.MONITOR_MAINTAIN, monitorPercentage != null ? monitorPercentage : 0
        );
    }
}
