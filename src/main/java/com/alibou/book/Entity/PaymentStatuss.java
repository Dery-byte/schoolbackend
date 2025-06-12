package com.alibou.book.Entity;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data

//@AllArgsConstructor
//@NoArgsConstructor
//@RequiredArgsConstructor
@Entity
@Table(name = "payment_status")
public class PaymentStatuss {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int txStatus;
    private String payer;
    private String payee;
    private double amount;
    private double value;
    private long transactionId;
    private String externalRef;
    private String thirdPartyRef;
    private String secret;  // Store the received secret
    private LocalDateTime timestamp;

    public void setTimestamp(String ts) {
        this.timestamp = LocalDateTime.parse(ts, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}