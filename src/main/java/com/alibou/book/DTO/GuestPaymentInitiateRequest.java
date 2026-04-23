package com.alibou.book.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class GuestPaymentInitiateRequest {
    @NotBlank(message = "Payer phone number is required")
    private String payer;

    private Integer channel;

    @Positive(message = "Amount must be greater than zero")
    private double amount;

    @NotBlank(message = "Subscription type is required")
    private String subscriptionType;

    @NotBlank(message = "Candidate name is required")
    private String candidateName;
}
