package co.singularit.az104simulator.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "attempt_answer")
@Getter
@Setter
public class AttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    @JsonIgnore
    private Attempt attempt;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "selected_option_ids_json", columnDefinition = "TEXT")
    private String selectedOptionIdsJson;

    @Column(nullable = false)
    private Boolean marked = false;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;
}
