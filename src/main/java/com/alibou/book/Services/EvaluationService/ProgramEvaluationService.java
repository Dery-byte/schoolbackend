package com.alibou.book.Services.EvaluationService;

// ============================================
// 3. EXTRACT PROGRAM EVALUATION SERVICE
// ============================================

import com.alibou.book.DTO.EligibilityDTOs.GroupEvaluationResult;
import com.alibou.book.DTO.EligibilityDTOs.ProgramEvaluationResult;
import com.alibou.book.DTO.EligibilityDTOs.SubjectComparison;
import com.alibou.book.Entity.Program;
import com.alibou.book.Entity.SubjectRequirement;
import com.alibou.book.utillities.EligibilityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgramEvaluationService {

    @Autowired
    private final SubjectEvaluationService subjectEvaluationService;

    /**
     * Evaluates a single program against candidate's results
     */
    public ProgramEvaluationResult evaluateProgram(
            Program program,
            Map<String, String> candidateGrades) {

        Map<String, String> programCores = program.getCoreSubjects() != null
                ? program.getCoreSubjects()
                : Map.of();
        List<SubjectRequirement> altGroups = program.getAlternativeGroups() != null
                ? program.getAlternativeGroups()
                : List.of();

        // Evaluate core subjects
        List<SubjectComparison> coreResults = subjectEvaluationService.evaluateCoreSubjects(
                programCores, candidateGrades);

        // Evaluate alternative groups
        List<GroupEvaluationResult> groupResults = altGroups.stream()
                .map(group -> subjectEvaluationService.evaluateAlternativeGroup(group, candidateGrades))
                .collect(Collectors.toList());

        // Calculate scores
        int totalScore = calculateTotalScore(coreResults, groupResults);
        int totalSubjects = calculateTotalSubjects(coreResults, groupResults);
        double percentage = EligibilityUtils.calculateEligibilityPercentage(totalScore, totalSubjects);

        // Determine eligibility
        boolean allCoresMet = coreResults.stream().allMatch(SubjectComparison::isMeetRequirement);
        boolean allGroupsMet = groupResults.stream().allMatch(GroupEvaluationResult::isGroupMet);
        boolean isEligible = allCoresMet && EligibilityUtils.isEligible(percentage);
        boolean isAlternative = !isEligible && EligibilityUtils.isAlternative(percentage);

        String decision = determineDecision(isEligible, isAlternative, allCoresMet, allGroupsMet, percentage);
        List<String> recommendations = generateRecommendations(coreResults, groupResults, allCoresMet);

        return ProgramEvaluationResult.builder()
                .program(program)
                .coreSubjectResults(coreResults)
                .alternativeGroupResults(groupResults)
                .eligibilityPercentage(percentage)
                .totalScore(totalScore)
                .totalSubjectsConsidered(totalSubjects)
                .isEligible(isEligible)
                .isAlternative(isAlternative)
                .overallDecision(decision)
                .recommendations(recommendations)
                .build();
    }

    private int calculateTotalScore(
            List<SubjectComparison> coreResults,
            List<GroupEvaluationResult> groupResults) {

        int coreScore = coreResults.stream()
                .mapToInt(SubjectComparison::getCandidateScore)
                .sum();

        int groupScore = groupResults.stream()
                .flatMap(g -> g.getSubjectComparisons().stream())
                .mapToInt(SubjectComparison::getCandidateScore)
                .sum();

        return coreScore + groupScore;
    }

    private int calculateTotalSubjects(
            List<SubjectComparison> coreResults,
            List<GroupEvaluationResult> groupResults) {

        int coreCount = coreResults.size();
        int groupCount = (int) groupResults.stream()
                .flatMap(g -> g.getSubjectComparisons().stream())
                .count();

        return coreCount + groupCount;
    }

    private String determineDecision(
            boolean isEligible,
            boolean isAlternative,
            boolean allCoresMet,
            boolean allGroupsMet,
            double percentage) {

        if (isEligible) {
            return String.format("✅ ELIGIBLE - %.2f%% (All requirements met)", percentage);
        } else if (isAlternative) {
            return String.format("⚠️ ALTERNATIVE - %.2f%% (Consider as backup option)", percentage);
        } else if (!allCoresMet) {
            return String.format("❌ NOT ELIGIBLE - %.2f%% (Core subjects not met)", percentage);
        } else if (!allGroupsMet) {
            return String.format("❌ NOT ELIGIBLE - %.2f%% (Alternative requirements not met)", percentage);
        } else {
            return String.format("❌ NOT ELIGIBLE - %.2f%%", percentage);
        }
    }

    private List<String> generateRecommendations(
            List<SubjectComparison> coreResults,
            List<GroupEvaluationResult> groupResults,
            boolean allCoresMet) {

        List<String> recommendations = new ArrayList<>();

        // Check for missing core subjects
        List<SubjectComparison> missingCores = coreResults.stream()
                .filter(c -> c.getCandidateGrade().equals("N/A"))
                .collect(Collectors.toList());

        if (!missingCores.isEmpty()) {
            recommendations.add("📋 Missing core subjects: " +
                    missingCores.stream()
                            .map(SubjectComparison::getSubjectName)
                            .collect(Collectors.joining(", ")));
        }

        // Check for marginal subjects
        List<SubjectComparison> marginalSubjects = coreResults.stream()
                .filter(c -> c.getStatus() == SubjectComparison.ComparisonStatus.MARGINAL)
                .collect(Collectors.toList());

        if (!marginalSubjects.isEmpty()) {
            recommendations.add("💡 You're close in: " +
                    marginalSubjects.stream()
                            .map(s -> s.getSubjectName() + " (need " + s.getRequiredGrade() + ", have " + s.getCandidateGrade() + ")")
                            .collect(Collectors.joining(", ")));
        }

        // Highlight strengths
        List<SubjectComparison> excellentSubjects = coreResults.stream()
                .filter(c -> c.getStatus() == SubjectComparison.ComparisonStatus.EXCELLENT)
                .collect(Collectors.toList());

        if (!excellentSubjects.isEmpty()) {
            recommendations.add("⭐ Strong performance in: " +
                    excellentSubjects.stream()
                            .map(SubjectComparison::getSubjectName)
                            .collect(Collectors.joining(", ")));
        }

        // Alternative group suggestions
        groupResults.stream()
                .filter(g -> !g.isGroupMet() && !g.getSubjectComparisons().isEmpty())
                .forEach(g -> {
                    if (g.getGroupType().equals("anyOf")) {
                        recommendations.add("📚 For alternative requirement, consider improving any of: " +
                                String.join(", ", g.getGroupSubjects()));
                    }
                });

        return recommendations;
    }
}