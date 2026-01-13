package co.singularit.az104simulator.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "option_item")
@Getter
@Setter
public class OptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore
    private Question question;

    @Column(nullable = false, length = 10)
    private String label;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "text_es", columnDefinition = "TEXT")
    private String textEs;

    @Column(name = "text_en", columnDefinition = "TEXT")
    private String textEn;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;
}
