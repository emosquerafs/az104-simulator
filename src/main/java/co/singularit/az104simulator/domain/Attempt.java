package co.singularit.az104simulator.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "attempt")
@Getter
@Setter
public class Attempt {

    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExamMode mode;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "session_id", length = 36)
    private String sessionId;

    @Column(name = "student_id", length = 36)
    private String studentId;

    @Column(name = "score_percentage")
    private Integer scorePercentage;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<AttemptAnswer> answers = new ArrayList<>();

    @Column(name = "current_question_index")
    private Integer currentQuestionIndex = 0;

    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    @PrePersist
    public void prePersist() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }

    public void addAnswer(AttemptAnswer answer) {
        answers.add(answer);
        answer.setAttempt(this);
    }
}
