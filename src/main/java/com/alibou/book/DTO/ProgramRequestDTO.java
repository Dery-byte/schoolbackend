//package com.alibou.book.DTO;
//
//import com.alibou.book.Entity.Program;
//import jakarta.validation.constraints.NotNull;
//import lombok.AllArgsConstructor;
//import lombok.NoArgsConstructor;
//import lombok.RequiredArgsConstructor;
//
//import java.util.List;
//import java.util.Set;
//
////@AllArgsConstructor
////@NoArgsConstructor
////@RequiredArgsConstructor
//public class ProgramRequestDTO {
//    @NotNull(message = "Select University")
//
//    private Long universityId;
//    private List<Program> programs;
//    private Set<Long> categoryIds;  // Added for categories
//
//    public ProgramRequestDTO(Set<Long> categoryIds) {
//        this.categoryIds = categoryIds;
//    }
//
//
//    // Getters and Setters
//    public Long getUniversityId() {
//        return universityId;
//    }
//
//    public void setUniversityId(Long universityId) {
//        this.universityId = universityId;
//    }
//
//    public List<Program> getPrograms() {
//        return programs;
//    }
//
//    public void setPrograms(List<Program> programs) {
//        this.programs = programs;
//    }
//}

package com.alibou.book.DTO;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public class ProgramRequestDTO {
    @NotNull(message = "Select University")
    private Long universityId;
    private List<ProgramWithCategoriesDTO> programs;

    // Getters and Setters
    public Long getUniversityId() {
        return universityId;
    }

    public void setUniversityId(Long universityId) {
        this.universityId = universityId;
    }

    public List<ProgramWithCategoriesDTO> getPrograms() {
        return programs;
    }

    public void setPrograms(List<ProgramWithCategoriesDTO> programs) {
        this.programs = programs;
    }

    public static class ProgramWithCategoriesDTO {
        private String name;
        private Map<String, String> cutoffPoints;
        private List<CategoryIdDTO> categoryIds;

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, String> getCutoffPoints() {
            return cutoffPoints;
        }

        public void setCutoffPoints(Map<String, String> cutoffPoints) {
            this.cutoffPoints = cutoffPoints;
        }

        public List<CategoryIdDTO> getCategoryIds() {
            return categoryIds;
        }

        public void setCategoryIds(List<CategoryIdDTO> categoryIds) {
            this.categoryIds = categoryIds;
        }
    }
}