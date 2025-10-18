package com.alibou.book.DTO;

import lombok.Data;

import java.util.List;

@Data
public class SubjectRequirementDTO {
    private String requiredGrade;
    private boolean anyOf;
    private List<String> subjects;
}
