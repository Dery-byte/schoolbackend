package com.alibou.book.DTO;

import java.util.List;

public class UniversityEligibilityGroupDTO {
    private String universityName;
    private String location;
    private String type;
    private List<EligibleProgramDTO> eligiblePrograms;
    private List<EligibleProgramDTO> alternativePrograms;

    public UniversityEligibilityGroupDTO(String universityName, String location, String type,
                                         List<EligibleProgramDTO> eligiblePrograms,
                                         List<EligibleProgramDTO> alternativePrograms) {
        this.universityName = universityName;
        this.location = location;
        this.type = type;
        this.eligiblePrograms = eligiblePrograms;
        this.alternativePrograms = alternativePrograms;
    }

    // Getters and setters
}
