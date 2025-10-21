package com.alibou.book.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResultDetailDto {
    @NotNull(message = "Subject is required")
    private String subject;

    @NotNull(message = "Grade is required")
    private String grade;
    private String interpretation;
    private String subjectcode;
}