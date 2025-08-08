package com.alibou.book.DTO.Projections;

// WeeklyPaymentStats.java
public record WeeklyPaymentStats(
        int weekNumber,
        int status,
        double totalAmount,
        long transactionCount) {}