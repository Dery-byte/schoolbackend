package com.alibou.book.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * Represents a logical subject requirement group.
 * Example:
 *  - ["Biology", "Chemistry"] with anyOf=false → both required
 *  - ["Physics", "Mathematics (Elective)"] with anyOf=true → either one acceptable
 */
@Data
@Embeddable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubjectRequirement {

    @Lob
    @Column(columnDefinition = "TEXT")
    @JsonIgnore // hide raw JSON from frontend
    private String subjectsJson;

    private String requiredGrade;
    private boolean anyOf;

    @Transient
    private List<String> subjects;

    /**
     * ✅ Lazy load JSON → List<String> on demand (always works even in ElementCollection)
     */
    public List<String> getSubjects() {
        if (subjects != null) return subjects; // already loaded
        try {
            if (subjectsJson != null && !subjectsJson.isBlank()) {
                ObjectMapper mapper = new ObjectMapper();
                subjects = mapper.readValue(subjectsJson, new TypeReference<>() {});
            } else {
                subjects = List.of();
            }
        } catch (Exception e) {
            subjects = Collections.emptyList();
        }
        return subjects;
    }

    /**
     * ✅ Automatically serialize List<String> → JSON string when saving
     */
    public void setSubjects(List<String> subjects) {
        try {
            this.subjects = subjects;
            ObjectMapper mapper = new ObjectMapper();
            this.subjectsJson = mapper.writeValueAsString(subjects);
        } catch (Exception e) {
            this.subjectsJson = "[]";
        }
    }
}
