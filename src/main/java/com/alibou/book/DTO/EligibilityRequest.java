package com.alibou.book.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class EligibilityRequest {
    @Valid
    @NotEmpty(message = "Result details cannot be empty")
    private List<ResultDetail> resultDetails;

    private String universityType;
    private String checkRecordId;


    @NotNull(message = "At least one category must be selected")
    @Size(min = 1, message = "At least one category must be selected")
    private List<Long> categoryIds;


}