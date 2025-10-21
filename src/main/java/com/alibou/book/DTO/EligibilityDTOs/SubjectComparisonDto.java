package com.alibou.book.DTO.EligibilityDTOs;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubjectComparisonDto {
    private String subjectName;
    private String requiredGrade;
    private String candidateGrade;
    private boolean meetRequirement;
    private String status; // "EXCELLENT", "PASS", "MARGINAL", "FAIL", "MISSING"
    private String message;
}