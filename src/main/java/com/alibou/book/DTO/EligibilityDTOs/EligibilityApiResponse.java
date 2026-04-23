package com.alibou.book.DTO.EligibilityDTOs;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class EligibilityApiResponse {
    private String recordId;
    private String userId;
    private String candidateName;
    private LocalDateTime checkedAt;
    private List<String> selectedCategories;
    private Map<String, String> candidateGrades;
    private List<UniversityEligibilityDto> universities;
    private EligibilitySummary summary;
}