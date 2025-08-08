package com.alibou.book.DTO.Projections;

import java.time.LocalDate;

public record DailyPaymentSummary(
        LocalDate date,
        double totalAmount,
        long transactionCount) {}