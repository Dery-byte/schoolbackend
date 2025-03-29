package com.alibou.book.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;


@Setter
@Getter
public class AlternativeProgramDTO {
    private String programName;
    private Map<String, String> cutoffPoints;
    private List<String> explanation; // Technical grade deficit notes
    private double percentageEligibility;


    public AlternativeProgramDTO(String programName, Map<String, String> cutoffPoints, List<String> explanation, double percentageEligibility) {
        this.programName = programName;
        this.cutoffPoints = cutoffPoints;
        this.explanation = explanation;
        this.percentageEligibility = percentageEligibility;
    }

    // Getters and setters...
}
