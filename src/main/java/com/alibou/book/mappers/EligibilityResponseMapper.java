package com.alibou.book.mappers;

import com.alibou.book.DTO.EligibilityDTOs.*;
import com.alibou.book.Entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class EligibilityResponseMapper {

    /**
     * Maps database entity to API response DTO
     */
    public EligibilityApiResponse toApiResponse(
            EligibilityRecord record,
            WaecCandidateEntity candidate,
            Map<String, String> candidateGrades,
            List<ProgramEvaluationResult> allEvaluations) {

        List<UniversityEligibilityDto> universityDtos = record.getUniversities().stream()
                .map(uni -> mapUniversityEligibility(uni, allEvaluations))
                .collect(Collectors.toList());

        EligibilitySummary summary = buildSummary(universityDtos, allEvaluations);

        return EligibilityApiResponse.builder()
                .recordId(record.getId())
                .userId(record.getUserId())
                .candidateName(candidate.getCname())
                .checkedAt(record.getCreatedAt())
                .selectedCategories(record.getSelectedCategories())
                .candidateGrades(candidateGrades)
                .universities(universityDtos)
                .summary(summary)
                .build();
    }

    private UniversityEligibilityDto mapUniversityEligibility(
            UniversityEligibility uniElig,
            List<ProgramEvaluationResult> allEvaluations) {

        // Combine both eligible and alternative programs
        List<ProgramEligibilityDto> programDtos = new ArrayList<>();

        // Map eligible programs
        if (uniElig.getEligiblePrograms() != null) {
            uniElig.getEligiblePrograms().forEach(eligibleProg ->
                    programDtos.add(mapEligibleProgram(eligibleProg, allEvaluations)));
        }

        // Map alternative programs
        if (uniElig.getAlternativePrograms() != null) {
            uniElig.getAlternativePrograms().forEach(altProg ->
                    programDtos.add(mapAlternativeProgram(altProg, allEvaluations)));
        }

        long eligibleCount = uniElig.getEligiblePrograms() != null
                ? uniElig.getEligiblePrograms().size()
                : 0;

        long alternativeCount = uniElig.getAlternativePrograms() != null
                ? uniElig.getAlternativePrograms().size()
                : 0;

        return UniversityEligibilityDto.builder()
                .universityId(uniElig.getId())
                .universityName(uniElig.getUniversityName())
                .universityType(uniElig.getType())
                .eligibleProgramsCount((int) eligibleCount)
                .alternativeProgramsCount((int) alternativeCount)
                .programs(programDtos)
                .build();
    }

    private ProgramEligibilityDto mapEligibleProgram(
            EligibleProgram eligibleProg,
            List<ProgramEvaluationResult> allEvaluations) {

        // Find the corresponding evaluation result by program name
        ProgramEvaluationResult evalResult = allEvaluations.stream()
                .filter(e -> e.getProgram().getName().equals(eligibleProg.getName()))
                .findFirst()
                .orElse(null);

        if (evalResult == null) {
            // Fallback: Create basic DTO without detailed evaluation
            return ProgramEligibilityDto.builder()
                    .programId(eligibleProg.getId())
                    .programName(eligibleProg.getName())
                    .status("ELIGIBLE")
                    .eligibilityPercentage(eligibleProg.getPercentage())
                    .overallDecision(buildBasicDecision(eligibleProg))
                    .coreSubjects(buildCoreFromCutoff(eligibleProg.getCutoffPoints()))
                    .alternativeGroups(new ArrayList<>())
//                    .recommendations(extractRecommendations(eligibleProg.getAiRecommendation()))
                    .build();
        }

        // Map with full evaluation details
        CoreSubjectsResult coreResult = mapCoreSubjects(evalResult.getCoreSubjectResults());

        List<AlternativeGroupResult> groupResults = evalResult.getAlternativeGroupResults().stream()
                .map(this::mapAlternativeGroup)
                .collect(Collectors.toList());

        return ProgramEligibilityDto.builder()
                .programId(eligibleProg.getId())
                .programName(eligibleProg.getName())
                .status("ELIGIBLE")
                .eligibilityPercentage(eligibleProg.getPercentage())
                .overallDecision(evalResult.getOverallDecision())
                .coreSubjects(coreResult)
                .alternativeGroups(groupResults)
                .recommendations(evalResult.getRecommendations())
                .build();
    }

    private ProgramEligibilityDto mapAlternativeProgram(
            AlternativeProgram altProg,
            List<ProgramEvaluationResult> allEvaluations) {

        // Find the corresponding evaluation result by program name
        ProgramEvaluationResult evalResult = allEvaluations.stream()
                .filter(e -> e.getProgram().getName().equals(altProg.getName()))
                .findFirst()
                .orElse(null);

        if (evalResult == null) {
            // Fallback: Create basic DTO without detailed evaluation
            return ProgramEligibilityDto.builder()
                    .programId(altProg.getId())
                    .programName(altProg.getName())
                    .status("ALTERNATIVE")
                    .eligibilityPercentage(altProg.getPercentage())
                    .overallDecision(buildBasicDecision(altProg))
                    .coreSubjects(buildCoreFromCutoff(altProg.getCutoffPoints()))
                    .alternativeGroups(new ArrayList<>())
//                    .recommendations(extractRecommendationsWithGap(
//                            altProg.getAiRecommendation(),
//                            altProg.getGapAnalysis()))
                    .build();
        }

        // Map with full evaluation details
        CoreSubjectsResult coreResult = mapCoreSubjects(evalResult.getCoreSubjectResults());
        List<AlternativeGroupResult> groupResults = evalResult.getAlternativeGroupResults().stream()
                .map(this::mapAlternativeGroup)
                .collect(Collectors.toList());
        return ProgramEligibilityDto.builder()
                .programId(altProg.getId())
                .programName(altProg.getName())
                .status("ALTERNATIVE")
                .eligibilityPercentage(altProg.getPercentage())
                .overallDecision(evalResult.getOverallDecision())
                .coreSubjects(coreResult)
                .alternativeGroups(groupResults)
                .recommendations(evalResult.getRecommendations())
                .build();
    }

    private CoreSubjectsResult mapCoreSubjects(List<SubjectComparison> coreComparisons) {
        List<SubjectComparisonDto> subjectDtos = coreComparisons.stream()
                .map(this::mapSubjectComparison)
                .collect(Collectors.toList());

        long passedCount = subjectDtos.stream()
                .filter(SubjectComparisonDto::isMeetRequirement)
                .count();

        return CoreSubjectsResult.builder()
                .allCoreMet(passedCount == subjectDtos.size())
                .totalCoreSubjects(subjectDtos.size())
                .coreSubjectsPassed((int) passedCount)
                .subjects(subjectDtos)
                .build();
    }

    private AlternativeGroupResult mapAlternativeGroup(GroupEvaluationResult groupEval) {
        List<SubjectComparisonDto> subjectDtos = groupEval.getSubjectComparisons().stream()
                .map(this::mapSubjectComparison)
                .collect(Collectors.toList());

        return AlternativeGroupResult.builder()
                .groupType(groupEval.getGroupType())
                .groupMet(groupEval.isGroupMet())
                .requiredGrade(groupEval.getRequiredGrade())
                .groupSubjects(groupEval.getGroupSubjects())
                .summary(groupEval.getSummaryMessage())
                .subjectDetails(subjectDtos)
                .build();
    }

    private SubjectComparisonDto mapSubjectComparison(SubjectComparison comparison) {
        return SubjectComparisonDto.builder()
                .subjectName(comparison.getSubjectName())
                .requiredGrade(comparison.getRequiredGrade())
                .candidateGrade(comparison.getCandidateGrade())
                .meetRequirement(comparison.isMeetRequirement())
                .status(comparison.getStatus().name())
                .message(comparison.getMessage())
                .build();
    }

    private EligibilitySummary buildSummary(
            List<UniversityEligibilityDto> universities,
            List<ProgramEvaluationResult> allEvaluations) {

        int totalEligible = universities.stream()
                .mapToInt(UniversityEligibilityDto::getEligibleProgramsCount)
                .sum();

        int totalAlternative = universities.stream()
                .mapToInt(UniversityEligibilityDto::getAlternativeProgramsCount)
                .sum();

        int totalIneligible = (int) allEvaluations.stream()
                .filter(e -> !e.isEligible() && !e.isAlternative())
                .count();

        // Aggregate top recommendations from all programs
        List<String> topRecommendations = allEvaluations.stream()
                .filter(ProgramEvaluationResult::isEligible)
                .flatMap(e -> e.getRecommendations().stream())
                .distinct()
                .limit(5)
                .collect(Collectors.toList());

        return EligibilitySummary.builder()
                .totalUniversities(universities.size())
                .totalEligiblePrograms(totalEligible)
                .totalAlternativePrograms(totalAlternative)
                .totalIneligiblePrograms(totalIneligible)
                .topRecommendations(topRecommendations)
                .build();
    }

    // ============================================
    // HELPER METHODS FOR FALLBACK SCENARIOS
    // ============================================

    /**
     * Builds a basic decision message when evaluation result is not available
     */
    private String buildBasicDecision(EligibleProgram program) {
        return String.format("✅ ELIGIBLE - %.2f%% (All requirements met)",
                program.getPercentage());
    }

    private String buildBasicDecision(AlternativeProgram program) {
        return String.format("⚠️ ALTERNATIVE - %.2f%% (Consider as backup option)",
                program.getPercentage());
    }

    /**
     * Builds core subjects result from cutoff points when evaluation not available
     */
    private CoreSubjectsResult buildCoreFromCutoff(Map<String, String> cutoffPoints) {
        if (cutoffPoints == null || cutoffPoints.isEmpty()) {
            return CoreSubjectsResult.builder()
                    .allCoreMet(true)
                    .totalCoreSubjects(0)
                    .coreSubjectsPassed(0)
                    .subjects(new ArrayList<>())
                    .build();
        }

        List<SubjectComparisonDto> subjects = cutoffPoints.entrySet().stream()
                .map(entry -> SubjectComparisonDto.builder()
                        .subjectName(entry.getKey())
                        .requiredGrade(entry.getValue())
                        .candidateGrade("N/A")
                        .meetRequirement(false)
                        .status("UNKNOWN")
                        .message(String.format("Required: %s", entry.getValue()))
                        .build())
                .collect(Collectors.toList());

        return CoreSubjectsResult.builder()
                .allCoreMet(false)
                .totalCoreSubjects(subjects.size())
                .coreSubjectsPassed(0)
                .subjects(subjects)
                .build();
    }

    /**
     * Extracts recommendations from AI recommendation text
     */
//    private List<String> extractRecommendations(AIRecommendation aiRec) {
//        if (aiRec == null || aiRec.getRecommendationText() == null) {
//            return new ArrayList<>();
//        }
//
//        // Extract lines that start with recommendation indicators
//        return aiRec.getRecommendationText().lines()
//                .filter(line -> line.trim().startsWith("💡") ||
//                        line.trim().startsWith("⭐") ||
//                        line.trim().startsWith("📋"))
//                .map(String::trim)
//                .collect(Collectors.toList());
//    }





    /**
     * Extracts recommendations including gap analysis
     */
//    private List<String> extractRecommendationsWithGap(
//            AIRecommendation aiRec,
//            String gapAnalysis) {
//
//        List<String> recommendations = extractRecommendations(aiRec);
//
//        // Add gap analysis summary if available
//        if (gapAnalysis != null && !gapAnalysis.isEmpty()) {
//            recommendations.add("📊 " + gapAnalysis.lines()
//                    .filter(line -> line.trim().startsWith("•"))
//                    .findFirst()
//                    .orElse("Review gap analysis for improvement areas"));
//        }
//
//        return recommendations;
//    }
}