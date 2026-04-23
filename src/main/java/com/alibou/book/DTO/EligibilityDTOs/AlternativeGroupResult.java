package com.alibou.book.DTO.EligibilityDTOs;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AlternativeGroupResult {
    private String groupType; // "anyOf" or "allOf"
    private boolean groupMet;
    private String requiredGrade;
    private List<String> groupSubjects;
    private String summary;
    private List<SubjectComparisonDto> subjectDetails;
}