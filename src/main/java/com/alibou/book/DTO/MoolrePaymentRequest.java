package com.alibou.book.DTO;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MoolrePaymentRequest {
    private Integer type = 1; // Fixed value for payment
    private Integer channel;   // 7-AT, 13-MTN, 6-Vodafone
    private String currency;   // GHS or NGN
    private String payer;      // Customer's mobile money number
    private Double amount;     // Amount with 2 decimal places
    private String externalref; // Unique transaction ID
    private String otpcode;    // Optional OTP code
    private String reference;  // Payment reference message
    private String accountnumber; // Your Moolre account number
}