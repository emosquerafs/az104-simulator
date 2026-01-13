package co.singularit.az104simulator.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OptionDto {
    private Long id;
    private String label;
    private String text;
    private Boolean isCorrect;
}
