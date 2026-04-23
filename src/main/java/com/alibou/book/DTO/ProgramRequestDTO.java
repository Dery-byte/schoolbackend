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
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
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
        private List<SubjectRequirementDTO> alternativeGroups; // ✅ new field

        private Map<String, String> coreSubjects;        // ✅ e.g. {"Mathematics":"A1","English":"B2"}
        private Map<String, String> alternativeSubjects;
        private String name;
        private Map<String, String> cutoffPoints;
        private List<CategoryIdDTO> categoryIds;


        // Getters and Setters


        public Map<String, String> getCoreSubjects() {
            return coreSubjects;
        }

        public void setCoreSubjects(Map<String, String> coreSubjects) {
            this.coreSubjects = coreSubjects;
        }

        public Map<String, String> getAlternativeSubjects() {
            return alternativeSubjects;
        }

        public void setAlternativeSubjects(Map<String, String> alternativeSubjects) {
            this.alternativeSubjects = alternativeSubjects;
        }

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

        public List<SubjectRequirementDTO> getAlternativeGroups() {
            return alternativeGroups;
        }

        public void setAlternativeGroups(List<SubjectRequirementDTO> alternativeGroups) {
            this.alternativeGroups = alternativeGroups;
        }


    }
    @Data
    public static class SubjectRequirementDTO {
        private List<String> subjects;      // e.g. ["Physics", "Maths (Elective)"]
        private String requiredGrade;       // e.g. "C6"
        private boolean anyOf;              // true → OR condition
    }
}