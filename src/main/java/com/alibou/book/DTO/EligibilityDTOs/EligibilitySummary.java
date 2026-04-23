package com.alibou.book.DTO.EligibilityDTOs;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EligibilitySummary {
    private int totalUniversities;
    private int totalEligiblePrograms;
    private int totalAlternativePrograms;
    private int totalIneligiblePrograms;
    private List<String> topRecommendations;
}