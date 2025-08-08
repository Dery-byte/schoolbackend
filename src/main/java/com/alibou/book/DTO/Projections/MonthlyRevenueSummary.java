package com.alibou.book.DTO.Projections;

import lombok.Data;

@Data
public class MonthlyRevenueSummary {
    private int month;
    private Double totalAmount;
    private Long transactionCount; // Added to match your query

    public MonthlyRevenueSummary(int month, Double totalAmount, Long transactionCount) {
        this.month = month;
        this.totalAmount = totalAmount;
        this.transactionCount = transactionCount;
    }
}