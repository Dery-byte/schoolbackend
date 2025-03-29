package com.alibou.book.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
public class EligibleProgramDTO {
    private String programName;
    private Map<String, String> cutoffPoints;
    private double percentageEligibility;


    public EligibleProgramDTO(String programName, Map<String, String> cutoffPoints, double percentageEligibility) {
        this.programName = programName;
        this.cutoffPoints = cutoffPoints;
        this.percentageEligibility = percentageEligibility;
    }

    // Getters and Setters
}
