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
    private double admissionProbability; // New field

    // Updated constructor to include admissionProbability
    public EligibleProgramDTO(String programName, Map<String, String> cutoffPoints, double percentageEligibility, double admissionProbability) {
        this.programName = programName;
        this.cutoffPoints = cutoffPoints;
        this.percentageEligibility = percentageEligibility;
        this.admissionProbability = admissionProbability; // Initialize new field
    }

    // Getters and Setters
}
