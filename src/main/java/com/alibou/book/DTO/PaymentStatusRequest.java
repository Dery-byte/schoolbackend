package com.alibou.book.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentStatusRequest {
    private int status;
    private String code;
    private String message;

    @JsonProperty("data") // Ensure Spring maps this correctly
    private PaymentData data;  // This maps to the "data" field in the JSON

}