package co.singularit.az104simulator.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "exam_session_question",
    uniqueConstraints = {
        @UniqueConstraint(name = "unique_session_question", columnNames = {"session_id", "question_id"}),
        @UniqueConstraint(name = "unique_session_position", columnNames = {"session_id", "position"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamSessionQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ExamSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "served_at", nullable = false)
    private LocalDateTime servedAt;

    @PrePersist
    protected void onCreate() {
        if (servedAt == null) {
            servedAt = LocalDateTime.now();
        }
    }
}
