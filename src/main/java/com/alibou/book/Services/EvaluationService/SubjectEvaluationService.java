package com.alibou.book.Services.EvaluationService;

import com.alibou.book.DTO.EligibilityDTOs.GroupEvaluationResult;
import com.alibou.book.DTO.EligibilityDTOs.SubjectComparison;
import com.alibou.book.Entity.SubjectRequirement;
import com.alibou.book.utillities.EligibilityUtils;
import com.alibou.book.utillities.SubjectNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectEvaluationService {

    private final SubjectNormalizer subjectNormalizer;

    /**
     * Evaluates a single subject comparison
     */
    public SubjectComparison evaluateSubject(
            String subjectName,
            String requiredGrade,
            String candidateGrade) {

        String normalizedSubject = subjectNormalizer.normalize(subjectName).toUpperCase();
        int requiredScore = EligibilityUtils.getGradeScore(requiredGrade);
        int candidateScore = EligibilityUtils.getGradeScore(candidateGrade);
        boolean meets = EligibilityUtils.meetsRequirement(candidateGrade, requiredGrade);

        SubjectComparison.ComparisonStatus status = determineStatus(candidateScore, requiredScore, meets);
        String message = buildComparisonMessage(normalizedSubject, requiredGrade, candidateGrade, status);

        return SubjectComparison.builder()
                .subjectName(normalizedSubject)
                .requiredGrade(requiredGrade)
                .candidateGrade(candidateGrade != null ? candidateGrade : "N/A")
                .requiredScore(requiredScore)
                .candidateScore(candidateScore)
                .meetRequirement(meets)
                .status(status)
                .message(message)
                .build();
    }

    /**
     * Evaluates all core subjects
     */
    public List<SubjectComparison> evaluateCoreSubjects(
            Map<String, String> programCores,
            Map<String, String> candidateGrades) {

        return programCores.entrySet().stream()
                .map(entry -> {
                    String subjNorm = subjectNormalizer.normalize(entry.getKey()).toUpperCase();
                    String candidateGrade = candidateGrades.get(subjNorm);
                    return evaluateSubject(entry.getKey(), entry.getValue(), candidateGrade);
                })
                .collect(Collectors.toList());
    }

    /**
     * Evaluates an alternative group (anyOf or allOf)
     */
    public GroupEvaluationResult evaluateAlternativeGroup(
            SubjectRequirement group,
            Map<String, String> candidateGrades) {

        List<String> groupSubjects = group.getSubjects() != null ? group.getSubjects() : List.of();
        if (groupSubjects.isEmpty()) {
            return GroupEvaluationResult.builder()
                    .groupType(group.isAnyOf() ? "anyOf" : "allOf")
                    .groupSubjects(List.of())
                    .groupMet(true)
                    .subjectComparisons(List.of())
                    .summaryMessage("⚠️ Empty group - skipped")
                    .build();
        }

        String requiredGrade = group.getRequiredGrade();
        boolean anyOf = group.isAnyOf();

        // Evaluate each subject in the group
        List<SubjectComparison> comparisons = groupSubjects.stream()
                .filter(subj -> {
                    String subjNorm = subjectNormalizer.normalize(subj).toUpperCase();
                    return candidateGrades.containsKey(subjNorm);
                })
                .map(subj -> {
                    String subjNorm = subjectNormalizer.normalize(subj).toUpperCase();
                    String candidateGrade = candidateGrades.get(subjNorm);
                    return evaluateSubject(subj, requiredGrade, candidateGrade);
                })
                .collect(Collectors.toList());

        boolean groupMet = anyOf
                ? comparisons.stream().anyMatch(SubjectComparison::isMeetRequirement)
                : comparisons.stream().allMatch(SubjectComparison::isMeetRequirement);

        String summaryMessage = buildGroupSummary(anyOf, groupMet, comparisons, groupSubjects);

        return GroupEvaluationResult.builder()
                .groupType(anyOf ? "anyOf" : "allOf")
                .groupSubjects(groupSubjects)
                .requiredGrade(requiredGrade)
                .groupMet(groupMet)
                .subjectComparisons(comparisons)
                .summaryMessage(summaryMessage)
                .build();
    }

    private SubjectComparison.ComparisonStatus determineStatus(
            int candidateScore,
            int requiredScore,
            boolean meets) {

        if (candidateScore < 0) return SubjectComparison.ComparisonStatus.FAIL;

        int difference = candidateScore - requiredScore;

        if (meets) {
            return difference >= 2
                    ? SubjectComparison.ComparisonStatus.EXCELLENT
                    : SubjectComparison.ComparisonStatus.PASS;
        } else {
            return difference >= -1
                    ? SubjectComparison.ComparisonStatus.MARGINAL
                    : SubjectComparison.ComparisonStatus.FAIL;
        }
    }

    private String buildComparisonMessage(
            String subject,
            String required,
            String candidate,
            SubjectComparison.ComparisonStatus status) {

        if (candidate == null || candidate.equals("N/A")) {
            return String.format("❌ %s: Missing (Required: %s)", subject, required);
        }

        switch (status) {
            case EXCELLENT:
                return String.format("✅ %s: %s (Required: %s) - Excellent!", subject, candidate, required);
            case PASS:
                return String.format("✅ %s: %s (Required: %s) - Pass", subject, candidate, required);
            case MARGINAL:
                return String.format("⚠️ %s: %s (Required: %s) - Close but below", subject, candidate, required);
            case FAIL:
                return String.format("❌ %s: %s (Required: %s) - Does not meet requirement", subject, candidate, required);
            default:
                return String.format("⚠️ %s: %s vs %s", subject, candidate, required);
        }
    }

    private String buildGroupSummary(
            boolean anyOf,
            boolean groupMet,
            List<SubjectComparison> comparisons,
            List<String> allGroupSubjects) {

        if (comparisons.isEmpty()) {
            return String.format("❌ No matching subjects found in group: %s", allGroupSubjects);
        }

        long metCount = comparisons.stream().filter(SubjectComparison::isMeetRequirement).count();

        if (anyOf) {
            return groupMet
                    ? String.format("✅ Alternative (anyOf) requirement met - %d of %d subjects passed",
                    metCount, comparisons.size())
                    : String.format("❌ Alternative (anyOf) not met - none of %d subjects passed",
                    comparisons.size());
        } else {
            return groupMet
                    ? String.format("✅ Alternative (allOf) requirement met - all %d subjects passed",
                    comparisons.size())
                    : String.format("❌ Alternative (allOf) not met - only %d of %d subjects passed",
                    metCount, comparisons.size());
        }
    }
}