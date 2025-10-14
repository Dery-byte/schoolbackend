package com.alibou.book.utillities;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Set;

@Component
public class EligibilityUtils {

     public static final Map<String, Integer> GRADE_SCALE = Map.ofEntries(
            Map.entry("A1", 100),
            Map.entry("B2", 90),
            Map.entry("B3", 80),
            Map.entry("C4", 70),
            Map.entry("C5", 60),
            Map.entry("C6", 50),
            Map.entry("D7", 40),
            Map.entry("E8", 30),
            Map.entry("F9", 0)
    );

    public static int getGradeScore(String grade) {
        if (grade == null) return 0;
        return GRADE_SCALE.getOrDefault(grade.trim().toUpperCase(), 0);
    }

    /**
     * Returns the absolute candidate score for that subject (0..100).
     * NOTE: This returns the candidate's grade numeric value (A1->100 ... F9->0).
     */
    public static int compareGrades(String candidateGrade, String requiredGrade) {
        // candidate's performance (absolute)
        return getGradeScore(candidateGrade);
    }

    /**
     * True if candidate meets or exceeds required grade.
     */
    public static boolean meetsRequirement(String candidateGrade, String requiredGrade) {
        return getGradeScore(candidateGrade) >= getGradeScore(requiredGrade);
    }

    public static double calculateEligibilityPercentage(double totalScore, int totalSubjects) {
        if (totalSubjects <= 0) return 0.0;
        // totalScore is sum of absolute subject scores (0..100 each).
        // average score across subjects is the percentage (0..100).
        return (totalScore / (totalSubjects * 100.0)) * 100.0;
    }

    public static boolean isEligible(double percentage) {
        return percentage >= 60.0;
    }

    public static boolean isAlternative(double percentage) {
        return percentage >= 40.0 && percentage < 60.0;
    }

    public static String getEligibilityStatus(double percentage) {
        if (isEligible(percentage)) return "Eligible";
        if (isAlternative(percentage)) return "Alternative";
        return "Not Eligible";
    }
}
