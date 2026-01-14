package co.singularit.az104simulator.dto;

import co.singularit.az104simulator.domain.ExamMode;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AttemptHistoryDto {
    private String id;
    private ExamMode mode;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer durationSeconds;
    private Integer totalQuestions;
    private Integer correctCount;
    private Integer incorrectCount;
    private Integer unansweredCount;
    private Integer markedCount;
    private Integer scorePercentage;
    private String locale;

    public String getFormattedDuration() {
        if (durationSeconds == null) {
            return "--";
        }
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
