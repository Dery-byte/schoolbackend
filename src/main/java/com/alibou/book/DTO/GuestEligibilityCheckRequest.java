package com.alibou.book.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class GuestEligibilityCheckRequest {
    @NotBlank(message = "Session ID is required")
    private String sessionId;

    @NotBlank(message = "Check record ID is required")
    private String checkRecordId;

    @NotNull(message = "Result details are required")
    @NotEmpty(message = "At least one subject result is required")
    @Valid
    private List<ResultDetailDto> resultDetails;

    @NotNull(message = "Category IDs are required")
    @NotEmpty(message = "At least one category must be selected")
    private List<Long> categoryIds;

    private String universityType;
}
