package com.alibou.book.DTO.EligibilityDTOs;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubjectComparison {
    private String subjectName;
    private String requiredGrade;
    private String candidateGrade;
    private int requiredScore;
    private int candidateScore;
    private boolean meetRequirement;
    private ComparisonStatus status;
    private String message;

    public enum ComparisonStatus {
        EXCELLENT, // Exceeds requirement significantly
        PASS,      // Meets requirement
        MARGINAL,  // Close but below
        FAIL       // Significantly below
    }
}