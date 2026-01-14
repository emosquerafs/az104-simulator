package co.singularit.az104simulator.dto;

import co.singularit.az104simulator.domain.ExamMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionStartResponseDto {

    private String sessionId;

    private ExamMode mode;

    private Integer totalQuestions;

    private String locale;

    private String message;
}
