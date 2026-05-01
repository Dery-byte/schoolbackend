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
    private final PackageConfigurationService packageConfigurationService;

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

        log.info("🔍 START: checkEligibilityWithDetails | userId={} | recordId={}", userId, checkExamRecordId);

        try {
            // 1. Validate input
            validateInput(candidate, userId, checkExamRecordId, userSelectedCategoryIds);
            log.debug("✅ Step 1: Input validated");

            // 2. Normalize candidate grades
            Map<String, String> candidateGrades = normalizeCandidateGrades(candidate);
            log.debug("✅ Step 2: Normalized candidate subjects: {}", candidateGrades);

            // 3. Fetch programs from categories (single batch query) + resolve category names
            log.debug("🔄 Step 3: Fetching programs for categories: {}", userSelectedCategoryIds);
            Set<Program> programs = fetchProgramsFromCategories(userSelectedCategoryIds);
            List<String> categoryNames = categoryRepository.findNamesByIds(userSelectedCategoryIds);
            log.info("📊 Step 3 Complete: Fetched {} programs from {} categories", programs.size(), categoryNames.size());

            // 4. Evaluate all programs
            log.debug("🔄 Step 4: Evaluating programs...");
            List<ProgramEvaluationResult> evaluationResults = programs.stream()
                    .map(program -> programEvaluationService.evaluateProgram(program, candidateGrades))
                    .collect(Collectors.toList());
            log.info("📊 Step 4 Complete: Evaluated {} programs", evaluationResults.size());

            // 5. Categorize programs by eligibility
            log.debug("🔄 Step 5: Categorizing results...");
            Map<University, List<ProgramEvaluationResult>> eligiblePrograms = categorizePrograms(
                    evaluationResults, ProgramEvaluationResult::isEligible);

            Map<University, List<ProgramEvaluationResult>> alternativePrograms = categorizePrograms(
                    evaluationResults, result -> !result.isEligible() && result.isAlternative());
            log.info("📊 Step 5 Complete: {} universities with eligible programs, {} with alternative", 
                    eligiblePrograms.size(), alternativePrograms.size());

            // 6. Filter and apply Quotas/Visibility from Package Configuration
            log.debug("🔄 Step 6: Applying Package Configuration logic...");
            ExamCheckRecord examRecord = examCheckRecordRepository.findById(checkExamRecordId)
                    .orElseThrow(() -> new EntityNotFoundException("ExamCheckRecord not found: " + checkExamRecordId));
            
            PackageConfiguration packageConfig = packageConfigurationService.getConfigurationBySubscriptionType(
                    examRecord.getSubscriptionType() != null ? examRecord.getSubscriptionType() : SubscriptionType.BASIC);
            
            log.info("📦 Using Package Config for {}: visibility={}, privateSlots={}, publicSlots={}", 
                    packageConfig.getSubscriptionType(), packageConfig.getVisibility(), 
                    packageConfig.getPrivateSchoolSlots(), packageConfig.getPublicSchoolSlots());

            Set<University> universities = applyPackageConstraints(
                    eligiblePrograms, alternativePrograms, universityType, packageConfig);
            
            log.info("📊 Step 6 Complete: Final university count after constraints: {}", universities.size());

            // 7. Persist results
            log.debug("🔄 Step 7: Persisting eligibility results...");
            EligibilityRecord record = persistEligibilityResults(
                    userId, checkExamRecordId, categoryNames,
                    universities, eligiblePrograms, alternativePrograms, evaluationResults, packageConfig);
            log.info("📊 Step 7 Complete: EligibilityRecord saved | id={}", record.getId());

            // 8. Map to API response DTO
            log.debug("🔄 Step 8: Mapping to API response...");
            EligibilityApiResponse response = responseMapper.toApiResponse(record, candidate, candidateGrades, evaluationResults);
            log.info("✅ END: checkEligibilityWithDetails successful for candidate: {}", candidate.getCname());

            return response;
        } catch (EligibilityException e) {
            log.warn("⚠️ Eligibility business exception: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("💥 CRITICAL ERROR in checkEligibilityWithDetails: {}", e.getMessage(), e);
            throw e;
        }
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
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new EligibilityException("No valid categories found");
        }
        Set<Program> programs = new HashSet<>(programRepository.findDistinctByCategoryIds(categoryIds));
        if (programs.isEmpty()) {
            throw new EligibilityException("No programs found for selected categories");
        }
        return programs;
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

    private Set<University> applyPackageConstraints(
            Map<University, List<ProgramEvaluationResult>> eligible,
            Map<University, List<ProgramEvaluationResult>> alternative,
            String requestedType,
            PackageConfiguration config) {

        // 1. Initial list of all universities with any results
        Set<University> allUnis = Stream.concat(
                eligible.keySet().stream(),
                alternative.keySet().stream()
        ).collect(Collectors.toSet());

        // 2. Apply Visibility Constraint
        log.debug("Applying visibility constraint: {}", config.getVisibility());
        Stream<University> filteredStream = allUnis.stream();
        switch (config.getVisibility()) {
            case PRIVATE_ONLY:
                filteredStream = filteredStream.filter(u -> u.getType() == UniversityType.PRIVATE);
                break;
            case PUBLIC_ONLY:
                filteredStream = filteredStream.filter(u -> u.getType() == UniversityType.PUBLIC);
                break;
            case BOTH:
            default:
                // No extra filtering
                break;
        }

        // 3. Apply requested type filter (if any)
        if (StringUtils.hasText(requestedType)) {
            filteredStream = filteredStream.filter(u -> u.getType().name().equalsIgnoreCase(requestedType));
        }

        List<University> filteredUnis = filteredStream.collect(Collectors.toList());

        // 4. Apply Quotas (Slots)
        // We separate them to apply slots independently
        List<University> privateUnis = filteredUnis.stream()
                .filter(u -> u.getType() == UniversityType.PRIVATE)
                .limit(config.getPrivateSchoolSlots() != null ? config.getPrivateSchoolSlots() : Integer.MAX_VALUE)
                .collect(Collectors.toList());

        List<University> publicUnis = filteredUnis.stream()
                .filter(u -> u.getType() == UniversityType.PUBLIC)
                .limit(config.getPublicSchoolSlots() != null ? config.getPublicSchoolSlots() : Integer.MAX_VALUE)
                .collect(Collectors.toList());

        Set<University> finalUnis = new HashSet<>();
        finalUnis.addAll(privateUnis);
        finalUnis.addAll(publicUnis);

        return finalUnis;
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
        List<String> categoryNames,
        Set<University> universities,
        Map<University, List<ProgramEvaluationResult>> eligiblePrograms,
        Map<University, List<ProgramEvaluationResult>> alternativePrograms,
        List<ProgramEvaluationResult> allResults,
        PackageConfiguration packageConfig) {

    log.info("📥 START: persistEligibilityResults | userId={} | recordId={}", userId, checkExamRecordId);

    try {
        ExamCheckRecord examRecord = examCheckRecordRepository.findById(checkExamRecordId)
                .orElseThrow(() -> {
                    log.error("❌ ExamCheckRecord not found for id={}", checkExamRecordId);
                    return new EntityNotFoundException("ExamCheckRecord not found: " + checkExamRecordId);
                });

        log.debug("📋 ExamCheckRecord fetched | id={} | currentStatus={}", examRecord.getId(), examRecord.getCheckStatus());

        // Guard against duplicate processing
        if (CheckStatus.CHECKED.equals(examRecord.getCheckStatus())) {
            log.warn("⚠️ Duplicate request detected — examRecord {} is already CHECKED.", checkExamRecordId);
            return eligibilityRecordRepository.findByExamCheckRecord(examRecord)
                    .orElseThrow(() -> {
                        log.error("❌ ExamRecord {} is CHECKED but no EligibilityRecord found — data inconsistency!", checkExamRecordId);
                        return new EntityNotFoundException("Existing eligibility record not found for examRecord: " + checkExamRecordId);
                    });
        }

        EligibilityRecord record = new EligibilityRecord();
        record.setId(UUID.randomUUID().toString());
        record.setUserId(userId);
        record.setExamCheckRecord(examRecord);
        record.setCreatedAt(LocalDateTime.now(ZoneId.of("Africa/Accra")));
        record.setSelectedCategories(categoryNames);

        log.debug("🆕 EligibilityRecord initialized | id={}", record.getId());

        try {
            log.debug("🔄 Updating ExamCheckRecord status to CHECKED...");
            examCheckRecordRepository.updateCheckStatus(checkExamRecordId, CheckStatus.CHECKED);
            log.debug("✅ ExamCheckRecord status updated");
        } catch (Exception e) {
            log.error("❌ Failed to update CheckStatus for examRecordId={}: {}", checkExamRecordId, e.getMessage());
            throw e;
        }

        // Pre-calculate categories for each program to avoid N+1 queries during mapping
        log.debug("🔄 Pre-calculating program categories...");
        Map<Long, List<String>> programCategoryNamesMap = allResults.stream()
                .map(ProgramEvaluationResult::getProgram)
                .distinct()
                .collect(Collectors.toMap(
                        Program::getId,
                        p -> {
                            try {
                                return p.getCategories().stream().map(Category::getName).collect(Collectors.toList());
                            } catch (Exception e) {
                                log.error("❌ Error fetching categories for program {}: {}", p.getName(), e.getMessage());
                                return new ArrayList<>();
                            }
                        }
                ));
        log.debug("✅ Program categories pre-calculated for {} programs", programCategoryNamesMap.size());

        log.debug("🔄 Building UniversityEligibility list...");
        List<UniversityEligibility> universityEligibilities = universities.stream()
                .map(uni -> {
                    log.debug("🏫 Processing university: {}", uni.getName());
                    return buildUniversityEligibility(
                            uni, eligiblePrograms, alternativePrograms, allResults, record, userId, programCategoryNamesMap, packageConfig);
                })
                .collect(Collectors.toList());

        record.setUniversities(universityEligibilities);
        log.debug("✅ Built {} UniversityEligibility objects", universityEligibilities.size());

        try {
            log.debug("🔄 SAVING EligibilityRecord to database (this may take time)...");
            EligibilityRecord saved = eligibilityRecordRepository.save(record);
            log.info("💾 END: persistEligibilityResults successful | id={}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("❌ DATABASE SAVE FAILED for EligibilityRecord: {}", e.getMessage(), e);
            throw e;
        }
    } catch (Exception e) {
        log.error("💥 CRITICAL ERROR in persistEligibilityResults: {}", e.getMessage(), e);
        throw e;
    }
}
    private UniversityEligibility buildUniversityEligibility(
            University university,
            Map<University, List<ProgramEvaluationResult>> eligiblePrograms,
            Map<University, List<ProgramEvaluationResult>> alternativePrograms,
            List<ProgramEvaluationResult> allResults,
            EligibilityRecord record,
            String userId,
            Map<Long, List<String>> programCategoryNamesMap,
            PackageConfiguration config) {

        UniversityEligibility uniElig = new UniversityEligibility();
        uniElig.setId(UUID.randomUUID().toString());
        uniElig.setUserId(userId);
        uniElig.setUniversityName(university.getName());
        uniElig.setLocation(university.getLocation());
        uniElig.setType(university.getType().name());
        uniElig.setEligibilityRecord(record);

        // Determine program quota for this university type
        int programLimit = (university.getType() == UniversityType.PRIVATE) 
                ? (config.getProgramsPerPrivateUniversity() != null ? config.getProgramsPerPrivateUniversity() : Integer.MAX_VALUE)
                : (config.getProgramsPerPublicUniversity() != null ? config.getProgramsPerPublicUniversity() : Integer.MAX_VALUE);

        // Build eligible programs list with quota
        List<EligibleProgram> eligibleProgramsList = eligiblePrograms
                .getOrDefault(university, List.of())
                .stream()
                .limit(programLimit)
                .map(result -> createEligibleProgram(result, uniElig, programCategoryNamesMap))
                .collect(Collectors.toList());

        // Build alternative programs list with quota
        List<AlternativeProgram> alternativeProgramsList = alternativePrograms
                .getOrDefault(university, List.of())
                .stream()
                .limit(Math.max(0, programLimit - eligibleProgramsList.size())) 
                .map(result -> createAlternativeProgram(result, uniElig, programCategoryNamesMap))
                .collect(Collectors.toList());

        uniElig.setEligiblePrograms(eligibleProgramsList);
        uniElig.setAlternativePrograms(alternativeProgramsList);

        return uniElig;
    }

    private EligibleProgram createEligibleProgram(
            ProgramEvaluationResult result,
            UniversityEligibility uniElig,
            Map<Long, List<String>> programCategoryNamesMap) {

        // Build cutoff points from core subjects
        Map<String, String> cutoffPoints = new HashMap<>();
        result.getCoreSubjectResults().forEach(core ->
                cutoffPoints.put(core.getSubjectName(), core.getRequiredGrade()));

        // Extract category names from pre-calculated map
        List<String> categoryNames = programCategoryNamesMap.getOrDefault(result.getProgram().getId(), new ArrayList<>());

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
            UniversityEligibility uniElig,
            Map<Long, List<String>> programCategoryNamesMap) {

        // Build cutoff points
        Map<String, String> cutoffPoints = new HashMap<>();
        result.getCoreSubjectResults().forEach(core ->
                cutoffPoints.put(core.getSubjectName(), core.getRequiredGrade()));

        // Extract categories from pre-calculated map
        List<String> categoryNames = programCategoryNamesMap.getOrDefault(result.getProgram().getId(), new ArrayList<>());

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

        explanation.append("CORE SUBJECTS ANALYSIS:\n");
        result.getCoreSubjectResults().forEach(core ->
                explanation.append("  ").append(core.getMessage()).append("\n"));

        if (!result.getAlternativeGroupResults().isEmpty()) {
            explanation.append("\nALTERNATIVE REQUIREMENTS:\n");
            result.getAlternativeGroupResults().forEach(group -> {
                explanation.append("  ").append(group.getSummaryMessage()).append("\n");
                group.getSubjectComparisons().forEach(subj ->
                        explanation.append("    ").append(subj.getMessage()).append("\n"));
            });
        }

        if (!result.getRecommendations().isEmpty()) {
            explanation.append("\nRECOMMENDATIONS:\n");
            result.getRecommendations().forEach(rec ->
                    explanation.append("  ").append(rec).append("\n"));
        }

        AIRecommendation aiRec = new AIRecommendation();
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