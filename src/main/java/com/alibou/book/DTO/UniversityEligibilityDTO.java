package com.alibou.book.DTO;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.List;
@Setter
@Getter

public class UniversityEligibilityDTO {
    private String universityName;
    private String location;
    private String type; // PUBLIC or PRIVATE
    private List<EligibleProgramDTO> eligiblePrograms;
//    private List<EligibleProgramDTO> alternativePrograms;
    private List<AlternativeProgramDTO> alternativePrograms;




    public UniversityEligibilityDTO(String universityName,  String location, String type, List<EligibleProgramDTO> eligiblePrograms,List<AlternativeProgramDTO> alternativePrograms) {
        this.universityName = universityName;
        this.location = location;
        this.type = type;
        this.eligiblePrograms = eligiblePrograms;
        this.alternativePrograms = alternativePrograms;

    }

    // Getters and Setters
    @Override
    public String toString() {
        return "UniversityEligibilityDTO{" +
                "universityName='" + universityName + '\'' +
                ", location='" + location + '\'' +
                ", type='" + type + '\'' +
                ", eligiblePrograms=" + eligiblePrograms +
                ", alternativePrograms=" + alternativePrograms +
                '}';
    }
}
