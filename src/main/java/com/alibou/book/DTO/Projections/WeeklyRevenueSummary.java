package com.alibou.book.DTO.Projections;

import lombok.*;

import java.math.BigDecimal;

@Data
@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
public class WeeklyRevenueSummary {
    private Integer weekNumber;
    private BigDecimal totalAmount;
    private Long transactionCount;
    // Explicit constructor for Hibernate
    public WeeklyRevenueSummary(Integer weekNumber, Double amount, Long count) {
        this.weekNumber = weekNumber;
        this.totalAmount = BigDecimal.valueOf(amount);
        this.transactionCount = count;
    }
}