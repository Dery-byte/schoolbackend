package com.alibou.book.DTO.EligibilityDTOs;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GroupEvaluationResult {
    private String groupType; // "anyOf" or "allOf"
    private List<String> groupSubjects;
    private String requiredGrade;
    private boolean groupMet;
    private List<SubjectComparison> subjectComparisons;
    private String summaryMessage;
}