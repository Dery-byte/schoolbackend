package com.alibou.book.DTO;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.internal.IgnoreForbiddenApisErrors;

import java.util.List;
import java.util.Map;


@Setter
@Getter
public class AlternativeProgramDTO {
    private String programName;
    private Map<String, String> cutoffPoints;
    private List<String> explanation; // Technical grade deficit notes

    public AlternativeProgramDTO(String programName, Map<String, String> cutoffPoints, List<String> explanation) {
        this.programName = programName;
        this.cutoffPoints = cutoffPoints;
        this.explanation = explanation;
    }

    // Getters and setters...
}
