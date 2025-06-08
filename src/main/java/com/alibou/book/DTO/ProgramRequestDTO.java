package com.alibou.book.DTO;

import com.alibou.book.Entity.Program;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.List;

//@AllArgsConstructor
//@NoArgsConstructor
//@RequiredArgsConstructor
public class ProgramRequestDTO {
    @NotNull(message = "Select University")

    private Long universityId;
    private List<Program> programs;

    // Getters and Setters
    public Long getUniversityId() {
        return universityId;
    }

    public void setUniversityId(Long universityId) {
        this.universityId = universityId;
    }

    public List<Program> getPrograms() {
        return programs;
    }

    public void setPrograms(List<Program> programs) {
        this.programs = programs;
    }
}
