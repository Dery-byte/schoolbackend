package com.alibou.book.DTO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentStatusResponseDTO {
    private Long id;
    private int txStatus;
    private String payer;
    private String payee;
    private double amount;
    private double value;
    private long transactionId;
    private String externalRef;
    private String thirdPartyRef;
    private String secret;
    private LocalDateTime timestamp;

    private UserSummaryDTOs customer;
    // private UserSummaryDTO farmer;
}
