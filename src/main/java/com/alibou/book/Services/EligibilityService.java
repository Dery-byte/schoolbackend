package com.alibou.book.Services;

import com.alibou.book.DTO.EligibilityDTOs.EligibilityApiResponse;
import com.alibou.book.DTO.EligibilityDTOs.ProgramEvaluationResult;
import com.alibou.book.Entity.*;
import com.alibou.book.Repositories.CategoryRepository;
import com.alibou.book.Repositories.EligibilityRecordRepository;
import com.alibou.book.Repositories.ExamCheckRecordRepository;
import com.alibou.book.Repositories.ProgramRepository;
import com.alibou.book.Services.EvaluationService.ProgramEvaluationService;
import com.alibou.book.Services.EvaluationService.SubjectEvaluationService;
import com.alibou.book.exception.EligibilityException;
import com.alibou.book.mappers.EligibilityResponseMapper;
import com.alibou.book.utillities.EligibilityUtils;
import com.alibou.book.utillities.SubjectNormalizer;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class EligibilityService {

    private final SubjectNormalizer subjectNormalizer;
    private final CategoryRepository categoryRepository;
    private final ProgramRepository programRepository;
    private final ExamCheckRecordRepository examCheckRecordRepository;
    private final EligibilityRecordRepository eligibilityRecordRepository;
    private final SubjectEvaluationService subjectEvaluationService;
    private final ProgramEvaluationService programEvaluationService;
    private final EligibilityResponseMapper responseMapper;

    /**
     * Main method that returns API response with detailed subject comparisons
     */
    @Transactional
    public EligibilityApiResponse checkEligibilityWithDetails(
            WaecCandidateEntity candidate,
            String universityType,
            String userId,
            String checkExamRecordId,
            List<Long> userSelectedCategoryIds) {

        // 1. Validate input
        validateInput(candidate, userId, checkExamRecordId, userSelectedCategoryIds);
        log.info("🔍 Checking eligibility for candidate: {}", candidate.getCname());

        // 2. Normalize candidate grades
        Map<String, String> candidateGrades = normalizeCandidateGrades(candidate);
        log.debug("Normalized candidate subjects: {}", candidateGrades);

        // 3. Fetch programs from categories
        Set<Program> programs = fetchProgramsFromCategories(userSelectedCategoryIds);

        // 4. Evaluate all programs
        List<ProgramEvaluationResult> evaluationResults = programs.stream()
                .map(program -> programEvaluationService.evaluateProgram(program, candidateGrades))
                .collect(Collectors.toList());

        // 5. Categorize programs by eligibility
        Map<University, List<ProgramEvaluationResult>> eligiblePrograms = categorizePrograms(
                evaluationResults, ProgramEvaluationResult::isEligible);

        Map<University, List<ProgramEvaluationResult>> alternativePrograms = categorizePrograms(
                evaluationResults, result -> !result.isEligible() && result.isAlternative());

        // 6. Filter by university type if specified
        Set<University> universities = filterUniversitiesByType(
                eligiblePrograms, alternativePrograms, universityType);

        // 7. Persist results
        EligibilityRecord record = persistEligibilityResults(
                userId, checkExamRecordId, userSelectedCategoryIds,
                universities, eligiblePrograms, alternativePrograms, evaluationResults);

        log.info("✅ Eligibility check completed for candidate: {}", candidate.getCname());

        // 8. Map to API response DTO
        return responseMapper.toApiResponse(record, candidate, candidateGrades, evaluationResults);
    }

    /**
     * Legacy method that returns database entity (for backward compatibility)
     */
    @Transactional
    public EligibilityRecord checkEligibility(
            WaecCandidateEntity candidate,
            String universityType,
            String userId,
            String checkExamRecordId,
            List<Long> userSelectedCategoryIds) {

        EligibilityApiResponse apiResponse = checkEligibilityWithDetails(
                candidate, universityType, userId, checkExamRecordId, userSelectedCategoryIds);

        // Return just the entity (for services that need it)
        return eligibilityRecordRepository.findById(apiResponse.getRecordId())
                .orElseThrow(() -> new EntityNotFoundException("Record not found"));
    }

    private Map<String, String> normalizeCandidateGrades(WaecCandidateEntity candidate) {
        return candidate.getResultDetails().stream()
                .collect(Collectors.toMap(
                        r -> subjectNormalizer.normalize(r.getSubject()).toUpperCase(),
                        r -> r.getGrade().trim().toUpperCase(),
                        (g1, g2) -> EligibilityUtils.getGradeScore(g1) >= EligibilityUtils.getGradeScore(g2) ? g1 : g2
                ));
    }

    private Set<Program> fetchProgramsFromCategories(List<Long> categoryIds) {
        List<Category> categories = categoryRepository.findAllById(categoryIds);
        if (categories.isEmpty()) {
            throw new EligibilityException("No valid categories found");
        }

        return categories.stream()
                .flatMap(c -> programRepository.findByCategories_Id(c.getId()).stream())
                .collect(Collectors.toSet());
    }

    private Map<University, List<ProgramEvaluationResult>> categorizePrograms(
            List<ProgramEvaluationResult> results,
            Predicate<ProgramEvaluationResult> filter) {

        return results.stream()
                .filter(filter)
                .collect(Collectors.groupingBy(
                        result -> result.getProgram().getUniversity(),
                        Collectors.toList()
                ));
    }

    private Set<University> filterUniversitiesByType(
            Map<University, List<ProgramEvaluationResult>> eligible,
            Map<University, List<ProgramEvaluationResult>> alternative,
            String universityType) {

        Set<University> universities = Stream.concat(
                eligible.keySet().stream(),
                alternative.keySet().stream()
        ).collect(Collectors.toSet());

        if (StringUtils.hasText(universityType)) {
            universities = universities.stream()
                    .filter(u -> u.getType().name().equalsIgnoreCase(universityType))
                    .collect(Collectors.toSet());
        }

        return universities;
    }

    private EligibilityRecord persistEligibilityResults(
            String userId,
            String checkExamRecordId,
            List<Long> categoryIds,
            Set<University> universities,
            Map<University, List<ProgramEvaluationResult>> eligiblePrograms,
            Map<University, List<ProgramEvaluationResult>> alternativePrograms,
            List<ProgramEvaluationResult> allResults) {

        ExamCheckRecord examRecord = examCheckRecordRepository.findById(checkExamRecordId)
                .orElseThrow(() -> new EntityNotFoundException("ExamCheckRecord not found: " + checkExamRecordId));

        List<Category> categories = categoryRepository.findAllById(categoryIds);

        EligibilityRecord record = new EligibilityRecord();
        record.setId(UUID.randomUUID().toString());
        record.setUserId(userId);
        record.setExamCheckRecord(examRecord);
        record.setCreatedAt(LocalDateTime.now(ZoneId.of("Africa/Accra")));
        record.setSelectedCategories(categories.stream().map(Category::getName).toList());

        List<UniversityEligibility> universityEligibilities = universities.stream()
                .map(uni -> buildUniversityEligibility(
                        uni, eligiblePrograms, alternativePrograms, allResults, record, userId))
                .collect(Collectors.toList());

        record.setUniversities(universityEligibilities);
        examRecord.setCheckStatus(CheckStatus.CHECKED);
        examCheckRecordRepository.save(examRecord);

        return eligibilityRecordRepository.save(record);
    }

    private UniversityEligibility buildUniversityEligibility(
            University university,
            Map<University, List<ProgramEvaluationResult>> eligiblePrograms,
            Map<University, List<ProgramEvaluationResult>> alternativePrograms,
            List<ProgramEvaluationResult> allResults,
            EligibilityRecord record,
            String userId) {

        UniversityEligibility uniElig = new UniversityEligibility();
        uniElig.setId(UUID.randomUUID().toString());
        uniElig.setUserId(userId);
        uniElig.setUniversityName(university.getName());
        uniElig.setLocation(university.getLocation());
        uniElig.setType(university.getType().name());
        uniElig.setEligibilityRecord(record);

        // Build eligible programs list
        List<EligibleProgram> eligibleProgramsList = eligiblePrograms
                .getOrDefault(university, List.of())
                .stream()
                .map(result -> createEligibleProgram(result, uniElig))
                .collect(Collectors.toList());

        // Build alternative programs list
        List<AlternativeProgram> alternativeProgramsList = alternativePrograms
                .getOrDefault(university, List.of())
                .stream()
                .map(result -> createAlternativeProgram(result, uniElig))
                .collect(Collectors.toList());

        uniElig.setEligiblePrograms(eligibleProgramsList);
        uniElig.setAlternativePrograms(alternativeProgramsList);

        return uniElig;
    }

    private EligibleProgram createEligibleProgram(
            ProgramEvaluationResult result,
            UniversityEligibility uniElig) {

        // Build cutoff points from core subjects
        Map<String, String> cutoffPoints = new HashMap<>();
        result.getCoreSubjectResults().forEach(core ->
                cutoffPoints.put(core.getSubjectName(), core.getRequiredGrade()));

        // Extract category names from program
        List<String> categoryNames = result.getProgram().getCategories() != null
                ? result.getProgram().getCategories().stream()
                .map(Category::getName)
                .collect(Collectors.toList())
                : new ArrayList<>();

        // Build AI recommendation with detailed explanation
        AIRecommendation aiRec = buildAIRecommendation(result);

        return EligibleProgram.builder()
                .id(UUID.randomUUID().toString())
                .name(result.getProgram().getName())
                .cutoffPoints(cutoffPoints)
                .percentage(result.getEligibilityPercentage())
                .admissionProbability(calculateAdmissionProbability(result))
                .aiRecommendation(aiRec)
                .universityEligibility(uniElig)
                .categories(categoryNames)
                .build();
    }

    private AlternativeProgram createAlternativeProgram(
            ProgramEvaluationResult result,
            UniversityEligibility uniElig) {

        // Build cutoff points
        Map<String, String> cutoffPoints = new HashMap<>();
        result.getCoreSubjectResults().forEach(core ->
                cutoffPoints.put(core.getSubjectName(), core.getRequiredGrade()));

        // Extract categories
        List<String> categoryNames = result.getProgram().getCategories() != null
                ? result.getProgram().getCategories().stream()
                .map(Category::getName)
                .collect(Collectors.toList())
                : new ArrayList<>();

        // Build AI recommendation
        AIRecommendation aiRec = buildAIRecommendation(result);

        return AlternativeProgram.builder()
                .id(UUID.randomUUID().toString())
                .name(result.getProgram().getName())
                .cutoffPoints(cutoffPoints)
                .percentage(result.getEligibilityPercentage())
//                .gapAnalysis(buildGapAnalysis(result))
                .aiRecommendation(aiRec)
                .universityEligibility(uniElig)
                .categories(categoryNames)
                .build();
    }

    private AIRecommendation buildAIRecommendation(ProgramEvaluationResult result) {
        // Build detailed explanation with subject comparisons
        StringBuilder explanation = new StringBuilder();
        explanation.append(result.getOverallDecision()).append("\n\n");

        explanation.append("📚 CORE SUBJECTS ANALYSIS:\n");
        result.getCoreSubjectResults().forEach(core ->
                explanation.append("  ").append(core.getMessage()).append("\n"));

        if (!result.getAlternativeGroupResults().isEmpty()) {
            explanation.append("\n🔄 ALTERNATIVE REQUIREMENTS:\n");
            result.getAlternativeGroupResults().forEach(group -> {
                explanation.append("  ").append(group.getSummaryMessage()).append("\n");
                group.getSubjectComparisons().forEach(subj ->
                        explanation.append("    ").append(subj.getMessage()).append("\n"));
            });
        }

        if (!result.getRecommendations().isEmpty()) {
            explanation.append("\n💡 RECOMMENDATIONS:\n");
            result.getRecommendations().forEach(rec ->
                    explanation.append("  ").append(rec).append("\n"));
        }

        AIRecommendation aiRec = new AIRecommendation();
//        aiRec.setId(UUID.randomUUID().toString());
        aiRec.setRecommendationText(explanation.toString().trim());
        aiRec.setConfidenceScore(calculateConfidenceScore(result));

        return aiRec;
    }

    private String buildGapAnalysis(ProgramEvaluationResult result) {
        StringBuilder gap = new StringBuilder();
        gap.append("Gap Analysis:\n\n");

        // Missing or below-requirement subjects
        List<String> gaps = new ArrayList<>();

        result.getCoreSubjectResults().stream()
                .filter(c -> !c.isMeetRequirement())
                .forEach(c -> gaps.add(String.format(
                        "%s: Need %s, Have %s (Gap: %d points)",
                        c.getSubjectName(),
                        c.getRequiredGrade(),
                        c.getCandidateGrade(),
                        c.getRequiredScore() - c.getCandidateScore()
                )));

        if (gaps.isEmpty()) {
            gap.append("All core requirements met, but overall score needs improvement.");
        } else {
            gaps.forEach(g -> gap.append("• ").append(g).append("\n"));
        }

        return gap.toString();
    }

    private double calculateAdmissionProbability(ProgramEvaluationResult result) {
        // Simple probability calculation based on percentage
        double percentage = result.getEligibilityPercentage();

        if (percentage >= 90) return 0.95;
        if (percentage >= 80) return 0.85;
        if (percentage >= 75) return 0.75;
        if (percentage >= 70) return 0.65;
        return 0.50;
    }

    private double calculateConfidenceScore(ProgramEvaluationResult result) {
        // Confidence based on how well requirements are met
        long totalSubjects = result.getCoreSubjectResults().size() +
                result.getAlternativeGroupResults().stream()
                        .mapToLong(g -> g.getSubjectComparisons().size())
                        .sum();

        long metSubjects = result.getCoreSubjectResults().stream()
                .filter(c -> c.isMeetRequirement())
                .count() +
                result.getAlternativeGroupResults().stream()
                        .filter(g -> g.isGroupMet())
                        .count();

        return totalSubjects > 0 ? (metSubjects * 100.0) / totalSubjects : 0.0;
    }

    private void validateInput(
            WaecCandidateEntity candidate,
            String userId,
            String checkExamRecordId,
            List<Long> categoryIds) {

        if (candidate == null) {
            throw new IllegalArgumentException("Candidate data cannot be null");
        }
        if (candidate.getResultDetails() == null || candidate.getResultDetails().isEmpty()) {
            throw new IllegalArgumentException("Result details cannot be empty. Please provide at least one subject with grade.");
        }
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (!StringUtils.hasText(checkExamRecordId)) {
            throw new IllegalArgumentException("Check exam record ID is required");
        }
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new IllegalArgumentException("At least one category must be selected");
        }
    }








    // Get a single record by ID
    public EligibilityRecord getEligibilityRecordById(String id) {
        return eligibilityRecordRepository.findById(id) .orElseThrow(() -> new RuntimeException("Eligibility record not found")); }
}