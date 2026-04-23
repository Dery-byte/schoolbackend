package com.alibou.book.DTO;

public class EligibilityMonthlySummary {
    private int month;
    private long recordCount;
    private long universityCount;

    // Constructor
    public EligibilityMonthlySummary(int month, long recordCount, long universityCount) {
        this.month = month;
        this.recordCount = recordCount;
        this.universityCount = universityCount;
    }

    // Getters
    public int getMonth() { return month; }
    public long getRecordCount() { return recordCount; }
    public long getUniversityCount() { return universityCount; }
}