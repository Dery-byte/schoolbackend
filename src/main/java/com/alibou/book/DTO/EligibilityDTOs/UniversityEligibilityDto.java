package com.alibou.book.DTO.EligibilityDTOs;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UniversityEligibilityDto {
    private String universityId;
    private String universityName;
    private String universityType;
    private int eligibleProgramsCount;
    private int alternativeProgramsCount;
    private List<ProgramEligibilityDto> programs;
}