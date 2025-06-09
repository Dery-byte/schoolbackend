package com.alibou.book.Entity;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)  // Ignore unknown fields
public class MoolrePaymentResponse {
    private Integer status;    // 1-Success, 0-Failure
    private String code;       // Response code (e.g., TP14 for OTP required)
    private String message;    // Response message
    private Object go;         // Not used
    private String data;       // Request ID
    private String externalref; // Unique transaction ID
    private String userMessage; // Field to store user-friendly message





    @JsonAnySetter
    public void handleUnknown(String key, Object value) {
        // Handle any unknown fields

        System.out.println("Unknown field in response: {} = {}\", key, value");
    }
}