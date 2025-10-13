package com.alibou.book.DTO;

import lombok.Data;

import java.util.Map;
import java.util.Set;


@Data
public class UpdateProgramDTO {
    private Long programId;
    private String name;
    private Map<String, String> cutoffPoints;
    private Set<CategoryIdDTO> categoryIds;
    private Map<String, String> coreSubjects;
    private Map<String, String> alternativeSubjects;


    // Getters and setters
    public Long getProgramId() { return programId; }
    public void setProgramId(Long programId) { this.programId = programId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Map<String, String> getCutoffPoints() { return cutoffPoints; }
    public void setCutoffPoints(Map<String, String> cutoffPoints) { this.cutoffPoints = cutoffPoints; }
    public Set<CategoryIdDTO> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(Set<CategoryIdDTO> categoryIds) { this.categoryIds = categoryIds; }
}