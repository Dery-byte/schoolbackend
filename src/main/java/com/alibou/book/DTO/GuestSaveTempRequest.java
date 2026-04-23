package com.alibou.book.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GuestSaveTempRequest {
    @NotBlank(message = "Session ID is required")
    private String sessionId;

    @NotBlank(message = "Eligibility record ID is required")
    private String eligibilityRecordId;
}
