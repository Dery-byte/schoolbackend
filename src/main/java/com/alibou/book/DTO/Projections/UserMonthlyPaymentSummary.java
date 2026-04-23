package com.alibou.book.DTO.Projections;

public record UserMonthlyPaymentSummary(
        int month,
        Long userId,
        double totalAmount) {}