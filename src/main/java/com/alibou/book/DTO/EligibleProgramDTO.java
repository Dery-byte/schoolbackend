package com.alibou.book.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
public class EligibleProgramDTO {
    private String programName;
    private Map<String, String> cutoffPoints;

    public EligibleProgramDTO(String programName, Map<String, String> cutoffPoints) {
        this.programName = programName;
        this.cutoffPoints = cutoffPoints;
    }

    // Getters and Setters
}
