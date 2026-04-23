package com.alibou.book.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GuestPaymentInitiateResponse {
    private String sessionId;
    private String externalRef;
    private String recordId;
    private int status;
    private String code;
    private String message;
    private String userMessage;
}
