package com.alibou.book.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class EligibilityCheckRequest {

    @NotNull(message = "Result details are required")
    @NotEmpty(message = "At least one subject result is required")
    @Valid
    private List<ResultDetailDto> resultDetails;

    @NotNull(message = "Category IDs are required")
    @NotEmpty(message = "At least one category must be selected")
    private List<Long> categoryIds;

    @NotNull(message = "Check record ID is required")
    @JsonProperty("checkRecordId")
    private String checkRecordId;

    private String universityType; // Optional: "PUBLIC", "PRIVATE", etc.
}


