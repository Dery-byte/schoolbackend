package com.alibou.book.DTO.EligibilityDTOs;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CoreSubjectsResult {
    private boolean allCoreMet;
    private int totalCoreSubjects;
    private int coreSubjectsPassed;
    private List<SubjectComparisonDto> subjects;
}