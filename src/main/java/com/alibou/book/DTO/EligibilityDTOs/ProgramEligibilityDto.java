package com.alibou.book.DTO.EligibilityDTOs;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProgramEligibilityDto {
    private String programId;
    private String programName;
    private String status; // "ELIGIBLE", "ALTERNATIVE", "NOT_ELIGIBLE"
    private double eligibilityPercentage;
    private String overallDecision;
    private CoreSubjectsResult coreSubjects;
    private List<AlternativeGroupResult> alternativeGroups;
    private List<String> recommendations;
    private String gapAnalysis;
}