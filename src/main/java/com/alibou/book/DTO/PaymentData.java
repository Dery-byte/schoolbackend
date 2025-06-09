package com.alibou.book.DTO;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
//@AllArgsConstructor
//@NoArgsConstructor
//@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentData {
    private int txstatus;
    private String payer;
    private String terminalid;
    private String accountnumber;
    private String name;
    private double amount;
    private double value;
    private String transactionid;
    private String externalref;
    private String thirdpartyref;
    private String secret;
    private String ts;


}