package com.alibou.book.utillities;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

// ✅ EligibilityUtils.java
@Component
public class EligibilityUtils {

    public static final Set<String> CORE_SUBJECTS = Set.of(
            "ENGLISH LANGUAGE", "MATHEMATICS(CORE)", "SOCIAL STUDIES", "INTEGRATED SCIENCE"
    );

    public static final Map<String, Integer> GRADE_SCALE = Map.ofEntries(
            Map.entry("A1", 100), Map.entry("B2", 90), Map.entry("B3", 80),
            Map.entry("C4", 70), Map.entry("C5", 60), Map.entry("C6", 50),
            Map.entry("D7", 40), Map.entry("E8", 30), Map.entry("F9", 0)
    );

    public static int getGradeScore(String grade) {
        return GRADE_SCALE.getOrDefault(grade.toUpperCase().trim(), 0);
    }
}
