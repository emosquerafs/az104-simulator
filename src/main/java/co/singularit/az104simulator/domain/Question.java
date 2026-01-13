package co.singularit.az104simulator.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "question")
@Getter
@Setter
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Domain domain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "qtype")
    private QuestionType qtype;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String stem;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "stem_es", columnDefinition = "TEXT")
    private String stemEs;

    @Column(name = "stem_en", columnDefinition = "TEXT")
    private String stemEn;

    @Column(name = "explanation_es", columnDefinition = "TEXT")
    private String explanationEs;

    @Column(name = "explanation_en", columnDefinition = "TEXT")
    private String explanationEn;

    @Column(name = "tags_json", columnDefinition = "TEXT")
    private String tagsJson;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OptionItem> options = new ArrayList<>();

    public void addOption(OptionItem option) {
        options.add(option);
        option.setQuestion(this);
    }
}
