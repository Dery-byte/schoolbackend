package com.alibou.book.DTO.EligibilityDTOs;

import com.alibou.book.Entity.Program;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProgramEvaluationResult {
    private Program program;
    private List<SubjectComparison> coreSubjectResults;
    private List<GroupEvaluationResult> alternativeGroupResults;
    private double eligibilityPercentage;
    private int totalScore;
    private int totalSubjectsConsidered;
    private boolean isEligible;
    private boolean isAlternative;
    private String overallDecision;
    private List<String> recommendations;
}