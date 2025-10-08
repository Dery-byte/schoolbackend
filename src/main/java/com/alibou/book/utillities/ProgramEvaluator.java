package com.alibou.book.utillities;

import com.alibou.book.DTO.EligibilityResult;
import com.alibou.book.Entity.Program;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// ✅ ProgramEvaluator.java
@Component
@RequiredArgsConstructor
public class ProgramEvaluator {

    private final SubjectNormalizer subjectNormalizer;

    public EligibilityResult evaluate(
            Program program,
            Map<String, String> candidateGrades,
            Map<String, Integer> gradeScale,
            Set<String> coreSubjects) {

        boolean eligible = true;
        boolean failedCore = false;
        int scoreDifference = 0;
        List<Integer> scores = new ArrayList<>();
        List<String> explanations = new ArrayList<>();

        for (Map.Entry<String, String> requirement : program.getCutoffPoints().entrySet()) {
            String subject = subjectNormalizer.normalize(requirement.getKey());
            String requiredGrade = requirement.getValue().trim().toUpperCase();
            String userGrade = candidateGrades.get(subject);

            System.out.println("Normalized School Subject: " + subject);

            if (userGrade == null) {
                explanations.add("Missing grade for subject: " + subject);
                eligible = false;
                continue;
            }

            int userScore = gradeScale.getOrDefault(userGrade, 0);
            int requiredScore = gradeScale.getOrDefault(requiredGrade, 0);

            if (coreSubjects.contains(subject) && userGrade.equals("F9")) {
                failedCore = true;
            }

            scores.add(userScore);

            if (userScore < requiredScore) {
                eligible = false;
                int diff = requiredScore - userScore;
                scoreDifference += diff;
                explanations.add(String.format("%s: Required %s (%d), Got %s (%d)",
                        subject, requiredGrade, requiredScore, userGrade, userScore));
            }
        }

        double percentage = (scores.isEmpty() || failedCore)
                ? 0.0
                : scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);

        return new EligibilityResult(program, eligible, failedCore, percentage, scoreDifference, explanations);
    }
}
