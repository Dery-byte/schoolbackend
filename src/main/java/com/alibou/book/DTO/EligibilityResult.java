package com.alibou.book.DTO;

import com.alibou.book.Entity.Program;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

// ✅ EligibilityResult.java
@Data
@AllArgsConstructor
public class EligibilityResult {
    private Program program;
    private boolean eligible;
    private boolean failedCore;
    private double percentage;
    private int scoreDifference;
    private List<String> explanations;
}
