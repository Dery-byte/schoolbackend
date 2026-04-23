package com.alibou.book.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AttachTempReportRequest {
    @NotBlank(message = "Session ID is required")
    private String sessionId;
}
