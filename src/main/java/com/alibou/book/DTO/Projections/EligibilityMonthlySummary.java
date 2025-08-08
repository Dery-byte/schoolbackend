package com.alibou.book.DTO.Projections;

import lombok.Data;

@Data
public class EligibilityMonthlySummary {
    private int month;
    private Long recordCount;
    private Long universityCount;

    public EligibilityMonthlySummary(int month, Long recordCount, Long universityCount) {
        this.month = month;
        this.recordCount = recordCount;
        this.universityCount = universityCount;
    }
}