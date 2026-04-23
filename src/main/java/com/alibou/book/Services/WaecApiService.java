package com.alibou.book.Services;

import com.alibou.book.DTO.*;
import com.alibou.book.Entity.*;
import com.alibou.book.Repositories.*;
import com.alibou.book.exception.EligibilityException;
import com.alibou.book.utillities.EligibilityUtils;
//import com.alibou.book.utillities.ProgramEvaluator;
import com.alibou.book.utillities.SubjectNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.apache.commons.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class WaecApiService {

    private static final Logger log = LoggerFactory.getLogger(WaecApiService.class);
    private final RestTemplate waecApiRestTemplate;
    private final ObjectMapper objectMapper;
    private final WaecCandidateRepository waecCandidateRepository;
    private final ProgramRepository programRepository;

    private final ExamCheckRecordRepository examCheckRecordRepository;

    private final EligibilityRecordRepository eligibilityRecordRepository;

    private final ProgramRecommendationService aiRecommendationService;
    private final CategoryRepository categoryRepository;
//    private final ProgramEvaluator programEvaluator;
    private final SubjectNormalizer subjectNormalizer;



    @Value("${waec.api.url}")
    private String apiUrl;

    public WaecApiService(
            RestTemplate waecApiRestTemplate,
            ObjectMapper objectMapper,
            WaecCandidateRepository waecCandidateRepository,
            ProgramRepository programRepository, ExamCheckRecordRepository examCheckRecordRepository, EligibilityRecordRepository eligibilityRecordRepository, ProgramRecommendationService aiRecommendationService, CategoryRepository categoryRepository, SubjectNormalizer subjectNormalizer) {
        this.waecApiRestTemplate = waecApiRestTemplate;
        this.objectMapper = objectMapper;
        this.waecCandidateRepository = waecCandidateRepository;
        this.programRepository = programRepository;
        this.examCheckRecordRepository = examCheckRecordRepository;
        this.eligibilityRecordRepository = eligibilityRecordRepository;
        this.aiRecommendationService = aiRecommendationService;
        this.categoryRepository = categoryRepository;
        this.subjectNormalizer = subjectNormalizer;
    }


    // VERIFY RETURNS THE DATA FROM THE DATABASE IF EXIST AND FETCH FROM THE WAEC API OTHERWISE


    @Transactional  // Add this annotation
    public ResponseEntity<?> verifyResult(WaecResultsRequest request, String recordId) {
//        String externalRef = request.getReqref();

        System.out.println(recordId);

        Optional<ExamCheckRecord> checkRecord = examCheckRecordRepository.findById(recordId);

        Optional<WaecCandidateEntity> existing = waecCandidateRepository.findFirstByCindexAndExamyearAndExamtype(
                request.getCindex(), request.getExamyear(), Long.valueOf((request.getExamtype()))
        );

        if (existing.isPresent()) {
            System.out.println("Results found in database ");
            WaecCandidateEntity candidateEntity = existing.get();

            // UDATE CHECK LIMIT
            checkRecord.ifPresent(record -> {
                record.setCheckLimit(record.getCheckLimit() + 1); // Increment
                record.setLastUpdated(Instant.now());
                record.setCandidateName(candidateEntity.getCname());
//                record.setWaecCandidateEntity(candidateEntity);
                System.out.println(candidateEntity);
                //record.setCheckStatus("completed");
                examCheckRecordRepository.save(record);
            });


            WaecCandidateDTO responseDTO = mapToDTO(candidateEntity);
            return ResponseEntity.ok(responseDTO);
        }


        String reqRef = UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        request.setReqref(reqRef);

        try {
            System.out.println("Request Body (JSON): " + objectMapper.writeValueAsString(request));
            HttpEntity<WaecResultsRequest> entity = new HttpEntity<>(request);
            ResponseEntity<String> response =
                    waecApiRestTemplate.postForEntity(apiUrl, entity, String.class);
            System.out.println("WAEC API Response: " + response.getStatusCode());
            System.out.println("Body: " + response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                WaecResponse waecResponse = objectMapper.readValue(response.getBody(), WaecResponse.class);
                System.out.println(waecResponse);

                Candidate c = waecResponse.getCandidate();
                WaecCandidateEntity candidateEntity = new WaecCandidateEntity();
                candidateEntity.setCindex(c.getCindex());
                candidateEntity.setCname(c.getCname());
                candidateEntity.setDob(c.getDob());
                candidateEntity.setGender(c.getGender());

                // Mapping of exam type string to integer (or Long)
                Map<String, Long> examTypeMap = Map.of(
                        "Wassce School Candidate", 1L,
                        "Wassce Private Candidate", 2L,
                        "Nov/Dec", 3L,
                        "BECE", 4L
                );

                // Get the mapped value or fallback to 0L if not found
                Long examTypeCode = examTypeMap.getOrDefault(c.getExamtype(), 0L);
                candidateEntity.setExamtype(Long.valueOf(String.valueOf(examTypeCode)));

//                candidateEntity.setExamtype(Long.valueOf(c.getExamtype()));
//                candidateEntity.setExamtype(c.getExamtype());
                candidateEntity.setExamyear(String.valueOf(Long.valueOf(request.getExamyear())));

                List<WaecResultDetailEntity> resultEntities = waecResponse.getResultdetails().stream().map(result -> {
                    WaecResultDetailEntity r = new WaecResultDetailEntity();
                    r.setSubjectcode(result.getSubjectcode());
                    r.setSubject(result.getSubject());
                    r.setGrade(result.getGrade());
                    r.setInterpretation(result.getInterpretation());
                    r.setCandidate(candidateEntity);
                    return r;
                }).toList();

                candidateEntity.setResultDetails(resultEntities);
                waecCandidateRepository.save(candidateEntity);
//                checkEligibility(candidateEntity, null);
//                List<UniversityEligibilityDTO> eligibility = checkEligibility(candidateEntity, null);


                System.out.println("This is the Saved Candidate : " + candidateEntity);
                System.out.println("✅ WAEC result saved successfully.");

                // Return the candidate entity (from the API) as the response
//                WaecCandidateDTO responseDTO = mapToDTO(candidateEntity);
//                return ResponseEntity.ok(responseDTO);

                // UDATE CHECK LIMIT
                checkRecord.ifPresent(record -> {
                    record.setCheckLimit(record.getCheckLimit() + 1); // Increment
                    record.setLastUpdated(Instant.now());
                    record.setCandidateName(candidateEntity.getCname());
//                    record.setWaecCandidateEntity(candidateEntity);

                    System.out.println(candidateEntity);
                    System.out.println(record.getCheckLimit());
                    System.out.println("What is  happeneing here");
                    examCheckRecordRepository.save(record);

                });
                return ResponseEntity.ok(candidateEntity);
            }


            // API call failed (don't increment checkLimit)
            checkRecord.ifPresent(record -> {
                //record.setCheckStatus("failed");
                examCheckRecordRepository.save(record);
            });

            return response;

        } catch (HttpStatusCodeException e) {
            System.out.println("Exception when calling WAEC API: " + e.getStatusCode());
            System.out.println("Response body: " + e.getResponseBodyAsString());
            return new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());

        } catch (Exception e) {
            System.out.println("General Exception: " + e.getMessage());
            return new ResponseEntity<>("Something went wrong",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    public WaecCandidateDTO mapToDTO(WaecCandidateEntity entity) {
        WaecCandidateDTO dto = new WaecCandidateDTO();
        dto.setCindex(entity.getCindex());
        dto.setCname(entity.getCname());
        dto.setDob(entity.getDob());
        dto.setGender(String.valueOf(entity.getGender()));
        dto.setExamtype(String.valueOf(Long.valueOf((entity.getExamtype()))));
        dto.setExamyear(Integer.valueOf(entity.getExamyear()));

        List<WaecResultDetailDTO> resultDTOs = entity.getResultDetails().stream().map(result -> {
            WaecResultDetailDTO r = new WaecResultDetailDTO();
            r.setSubjectcode(result.getSubjectcode());
            r.setSubject(result.getSubject());
            r.setGrade(result.getGrade());
            r.setInterpretation(result.getInterpretation());
            return r;
        }).toList();

        dto.setResultDetails(resultDTOs);
        return dto;
    }



    @Transactional
    public EligibilityRecord checkEligibility(
            WaecCandidateEntity candidate,
            String universityType,
            String userId,
            String checkExamRecordId,
            List<Long> userSelectedCategoryIds) {

        validateInput(candidate, userId, checkExamRecordId, userSelectedCategoryIds);
        log.info("🔍 Checking eligibility for candidate: {}", candidate.getCname());

        // ✅ Normalize candidate's subjects and grades
        Map<String, String> subjectGrades = candidate.getResultDetails().stream()
                .collect(Collectors.toMap(
                        r -> subjectNormalizer.normalize(r.getSubject()).toUpperCase(),
                        r -> r.getGrade().trim().toUpperCase(),
                        (g1, g2) -> EligibilityUtils.getGradeScore(g1) >= EligibilityUtils.getGradeScore(g2) ? g1 : g2
                ));

        System.out.println("Normalized Candidate Subjects: " + subjectGrades);

        // ✅ Fetch programs from categories
        List<Category> categories = categoryRepository.findAllById(userSelectedCategoryIds);
        if (categories.isEmpty()) throw new EligibilityException("No valid categories found");

        Set<Program> allPrograms = categories.stream()
                .flatMap(c -> programRepository.findByCategories_Id(c.getId()).stream())
                .collect(Collectors.toSet());

        Map<University, List<Program>> eligiblePrograms = new HashMap<>();
        Map<University, List<Program>> alternativePrograms = new HashMap<>();
        Map<Program, List<String>> explanations = new HashMap<>();
        Map<Program, Double> percentages = new HashMap<>();

        // ✅ Evaluate each program
        for (Program program : allPrograms) {
            double totalScore = 0.0;
            int totalSubjectsConsidered = 0;
            List<String> notes = new ArrayList<>();

            Map<String, String> programCores = program.getCoreSubjects() != null ? program.getCoreSubjects() : Map.of();
            List<SubjectRequirement> altGroups = program.getAlternativeGroups() != null ? program.getAlternativeGroups() : List.of();

            // ✅ ---- CORE SUBJECTS ----
            for (Map.Entry<String, String> core : programCores.entrySet()) {
                String subjNorm = subjectNormalizer.normalize(core.getKey()).toUpperCase();
                String requiredGrade = core.getValue();
                String candidateGrade = subjectGrades.get(subjNorm);

                int candidateScore = EligibilityUtils.getGradeScore(candidateGrade);
                totalScore += candidateScore;
                totalSubjectsConsidered++;

                if (candidateGrade == null) {
                    notes.add("❌ Missing core subject: " + subjNorm);
                } else if (!EligibilityUtils.meetsRequirement(candidateGrade, requiredGrade)) {
                    notes.add("⚠️ Core below required: " + subjNorm + " (" + candidateGrade + ")");
                } else {
                    notes.add("✅ Core ok: " + subjNorm + " (" + candidateGrade + ")");
                }
            }

            // ✅ ---- ALTERNATIVE GROUPS ----
            for (SubjectRequirement group : altGroups) {
                List<String> groupSubjects = group.getSubjects() != null ? group.getSubjects() : List.of();
                boolean anyOf = group.isAnyOf();
                String requiredGrade = group.getRequiredGrade();

                if (groupSubjects.isEmpty()) {
                    notes.add("⚠️ Skipped empty alternative group (no subjects listed)");
                    continue;
                }

                // Get candidate's subjects that match this group
                List<String> candidateMatchingSubjects = groupSubjects.stream()
                        .filter(subj -> subjectGrades.containsKey(subjectNormalizer.normalize(subj).toUpperCase()))
                        .toList();

                if (candidateMatchingSubjects.isEmpty()) {
                    notes.add("❌ No matching subjects found in group: " + groupSubjects);
                    continue;
                }

                if (anyOf) {
                    // ✅ At least one subject must meet requirement
                    boolean groupMet = false;
                    String bestSubject = null, bestGrade = null;
                    int bestScore = -1;

                    for (String subj : candidateMatchingSubjects) {
                        String subjNorm = subjectNormalizer.normalize(subj).toUpperCase();
                        String candidateGrade = subjectGrades.get(subjNorm);
                        int score = EligibilityUtils.getGradeScore(candidateGrade);

                        if (score > bestScore) {
                            bestScore = score;
                            bestSubject = subjNorm;
                            bestGrade = candidateGrade;
                        }

                        if (EligibilityUtils.meetsRequirement(candidateGrade, requiredGrade)) {
                            groupMet = true;
                        }
                    }

                    totalScore += bestScore;
                    totalSubjectsConsidered++;

                    if (groupMet) {
                        notes.add("✅ Alternative (anyOf) met: " + bestSubject + " (" + bestGrade + ")");
                    } else {
                        notes.add("⚠️ Alternative (anyOf) below required: " + bestSubject + " (" + bestGrade + ")");
                    }

                } else {
                    // ✅ All subjects in group must meet requirement
                    boolean allPresent = true;
                    boolean allMeetRequirement = true;

                    for (String subj : groupSubjects) {
                        String subjNorm = subjectNormalizer.normalize(subj).toUpperCase();
                        String candidateGrade = subjectGrades.get(subjNorm);

                        if (candidateGrade == null) {
                            allPresent = false;
                            allMeetRequirement = false;
                            notes.add("❌ Missing alternative (allOf) subject: " + subjNorm);
                            continue;
                        }

                        int score = EligibilityUtils.getGradeScore(candidateGrade);
                        totalScore += score;
                        totalSubjectsConsidered++;

                        if (!EligibilityUtils.meetsRequirement(candidateGrade, requiredGrade)) {
                            allMeetRequirement = false;
                            notes.add("⚠️ Alternative (allOf) below required: " + subjNorm + " (" + candidateGrade + ")");
                        } else {
                            notes.add("✅ Alternative (allOf) ok: " + subjNorm + " (" + candidateGrade + ")");
                        }
                    }

                    if (allPresent && allMeetRequirement) {
                        notes.add("✅ All subjects in group met requirement: " + groupSubjects);
                    } else if (allPresent) {
                        notes.add("⚠️ Some subjects in group did not meet requirement: " + groupSubjects);
                    }
                }
            }

            // ✅ ---- CHECK IF ALL SUBJECT REQUIREMENTS (CORE + ALTERNATIVE) ARE MET ----
            boolean allCoresMeet = programCores.entrySet().stream().allMatch(core -> {
                String subjNorm = subjectNormalizer.normalize(core.getKey()).toUpperCase();
                String candidateGrade = subjectGrades.get(subjNorm);
                return candidateGrade != null && EligibilityUtils.meetsRequirement(candidateGrade, core.getValue());
            });

            boolean allAlternativeGroupsMeet = altGroups.stream().allMatch(group -> {
                List<String> groupSubjects = group.getSubjects() != null ? group.getSubjects() : List.of();
                if (groupSubjects.isEmpty()) return true;

                boolean anyOf = group.isAnyOf();
                String requiredGrade = group.getRequiredGrade();

                List<String> normalizedSubjects = groupSubjects.stream()
                        .map(s -> subjectNormalizer.normalize(s).toUpperCase())
                        .toList();

                List<String> candidateMatching = normalizedSubjects.stream()
                        .filter(subjectGrades::containsKey)
                        .toList();

                if (candidateMatching.isEmpty()) return false;

                if (anyOf) {
                    return candidateMatching.stream().anyMatch(subj ->
                            EligibilityUtils.meetsRequirement(subjectGrades.get(subj), requiredGrade)
                    );
                } else {
                    return candidateMatching.stream().allMatch(subj ->
                            EligibilityUtils.meetsRequirement(subjectGrades.get(subj), requiredGrade)
                    );
                }
            });

            if (allCoresMeet && allAlternativeGroupsMeet) {
                notes.add("🎯 All subject requirements (core + alternative) fully met!");
            }

            // ✅ ---- FINAL SCORE + DECISION ----
            double percentage = EligibilityUtils.calculateEligibilityPercentage(totalScore, totalSubjectsConsidered);
            percentages.put(program, percentage);

            University uni = program.getUniversity();

            if (allCoresMeet && EligibilityUtils.isEligible(percentage)) {
                eligiblePrograms.computeIfAbsent(uni, u -> new ArrayList<>()).add(program);
                notes.add("✅ Eligible (" + String.format("%.2f", percentage) + "%)");
            } else if (EligibilityUtils.isAlternative(percentage)) {
                alternativePrograms.computeIfAbsent(uni, u -> new ArrayList<>()).add(program);
                notes.add("⚠️ Alternative (" + String.format("%.2f", percentage) + "%)");
            } else {
                notes.add("❌ Not eligible (" + String.format("%.2f", percentage) + "%)");
            }

            explanations.put(program, notes);
        }

        // ✅ --- Filter universities if specified ---
        Set<University> universities = Stream.concat(
                eligiblePrograms.keySet().stream(),
                alternativePrograms.keySet().stream()
        ).collect(Collectors.toSet());

        if (StringUtils.hasText(universityType)) {
            universities = universities.stream()
                    .filter(u -> u.getType().name().equalsIgnoreCase(universityType))
                    .collect(Collectors.toSet());
        }

        // ✅ --- Persist eligibility results ---
        ExamCheckRecord recordToSet = examCheckRecordRepository.findById(checkExamRecordId)
                .orElseThrow(() -> new EntityNotFoundException("ExamCheckRecord not found: " + checkExamRecordId));

        EligibilityRecord record = new EligibilityRecord();
        record.setId(UUID.randomUUID().toString());
        record.setUserId(userId);
        record.setExamCheckRecord(recordToSet);
        record.setCreatedAt(LocalDateTime.now(ZoneId.of("Africa/Accra")));
        record.setSelectedCategories(categories.stream().map(Category::getName).toList());

        List<UniversityEligibility> universityEligibilities = universities.stream()
                .map(uni -> buildUniversityEligibility(uni, eligiblePrograms, alternativePrograms, percentages, explanations, record))
                .toList();

        record.setUniversities(universityEligibilities);
        recordToSet.setCheckStatus(CheckStatus.CHECKED);
        examCheckRecordRepository.save(recordToSet);

        return eligibilityRecordRepository.save(record);
    }




//
//    @Transactional
//    public EligibilityRecord checkEligibility(
//            WaecCandidateEntity candidate,
//            String universityType,
//            String userId,
//            String checkExamRecordId,
//            List<Long> userSelectedCategoryIds) {
//
//        validateInput(candidate, userId, checkExamRecordId, userSelectedCategoryIds);
//        log.info("🔍 Checking eligibility for candidate: {}", candidate.getCname());
//
//        // ✅ Normalize candidate's subjects and grades
//        Map<String, String> subjectGrades = candidate.getResultDetails().stream()
//                .collect(Collectors.toMap(
//                        r -> subjectNormalizer.normalize(r.getSubject()).toUpperCase(),
//                        r -> r.getGrade().trim().toUpperCase(),
//                        (g1, g2) -> EligibilityUtils.getGradeScore(g1) >= EligibilityUtils.getGradeScore(g2) ? g1 : g2
//                ));
//
//        System.out.println("Normalized Candidate Subjects: " + subjectGrades);
//
//        // ✅ Fetch programs from categories
//        List<Category> categories = categoryRepository.findAllById(userSelectedCategoryIds);
//        if (categories.isEmpty()) throw new EligibilityException("No valid categories found");
//
//        Set<Program> allPrograms = categories.stream()
//                .flatMap(c -> programRepository.findByCategories_Id(c.getId()).stream())
//                .collect(Collectors.toSet());
//
//        Map<University, List<Program>> eligiblePrograms = new HashMap<>();
//        Map<University, List<Program>> alternativePrograms = new HashMap<>();
//        Map<Program, List<String>> explanations = new HashMap<>();
//        Map<Program, Double> percentages = new HashMap<>();
//
//        // ✅ Evaluate each program
//        for (Program program : allPrograms) {
//            double totalScore = 0.0;
//            int totalSubjectsConsidered = 0;
//            List<String> notes = new ArrayList<>();
//
//            Map<String, String> programCores = program.getCoreSubjects() != null ? program.getCoreSubjects() : Map.of();
//            List<SubjectRequirement> altGroups = program.getAlternativeGroups() != null ? program.getAlternativeGroups() : List.of();
//
//            // ✅ ---- CORE SUBJECTS ----
//            for (Map.Entry<String, String> core : programCores.entrySet()) {
//                String subjNorm = subjectNormalizer.normalize(core.getKey()).toUpperCase();
//                String requiredGrade = core.getValue();
//                String candidateGrade = subjectGrades.get(subjNorm);
//
//                int candidateScore = EligibilityUtils.getGradeScore(candidateGrade);
//                totalScore += candidateScore;
//                totalSubjectsConsidered++;
//
//                if (candidateGrade == null) {
//                    notes.add("❌ Missing core subject: " + subjNorm);
//                } else if (!EligibilityUtils.meetsRequirement(candidateGrade, requiredGrade)) {
//                    notes.add("⚠️ Core below required: " + subjNorm + " (" + candidateGrade + ")");
//                } else {
//                    notes.add("✅ Core ok: " + subjNorm + " (" + candidateGrade + ")");
//                }
//            }
//
//            // ✅ ---- ALTERNATIVE GROUPS ----
//            for (SubjectRequirement group : altGroups) {
//                List<String> groupSubjects = group.getSubjects() != null ? group.getSubjects() : List.of();
//                boolean anyOf = group.isAnyOf();
//                String requiredGrade = group.getRequiredGrade();
//
//                if (groupSubjects.isEmpty()) {
//                    notes.add("⚠️ Skipped empty alternative group (no subjects listed)");
//                    continue;
//                }
//
//                // Get candidate's subjects that match this group
//                List<String> candidateMatchingSubjects = groupSubjects.stream()
//                        .filter(subj -> subjectGrades.containsKey(subjectNormalizer.normalize(subj).toUpperCase()))
//                        .toList();
//
//                if (candidateMatchingSubjects.isEmpty()) {
//                    notes.add("❌ No matching subjects found in group: " + groupSubjects);
//                    continue;
//                }
//
//                if (anyOf) {
//                    // ✅ At least one subject must meet requirement
//                    boolean groupMet = false;
//                    String bestSubject = null, bestGrade = null;
//                    int bestScore = -1;
//
//                    for (String subj : candidateMatchingSubjects) {
//                        String subjNorm = subjectNormalizer.normalize(subj).toUpperCase();
//                        String candidateGrade = subjectGrades.get(subjNorm);
//                        int score = EligibilityUtils.getGradeScore(candidateGrade);
//
//                        if (score > bestScore) {
//                            bestScore = score;
//                            bestSubject = subjNorm;
//                            bestGrade = candidateGrade;
//                        }
//                        if (EligibilityUtils.meetsRequirement(candidateGrade, requiredGrade)) {
//                            groupMet = true;
//                        }
//                    }
//
//                    totalScore += bestScore;
//                    totalSubjectsConsidered++;
//
//                    if (groupMet) {
//                        notes.add("✅ Alternative (anyOf) met: " + bestSubject + " (" + bestGrade + ")");
//                    } else {
//                        notes.add("⚠️ Alternative (anyOf) below required: " + bestSubject + " (" + bestGrade + ")");
//                    }
//
//                } else {
//                    // ✅ All subjects in group must meet requirement
//                    boolean allPresent = true;
//                    boolean allMeetRequirement = true;
//
//                    for (String subj : groupSubjects) {
//                        String subjNorm = subjectNormalizer.normalize(subj).toUpperCase();
//                        String candidateGrade = subjectGrades.get(subjNorm);
//
//                        if (candidateGrade == null) {
//                            allPresent = false;
//                            allMeetRequirement = false;
//                            notes.add("❌ Missing alternative (allOf) subject: " + subjNorm);
//                            continue;
//                        }
//
//                        int score = EligibilityUtils.getGradeScore(candidateGrade);
//                        totalScore += score;
//                        totalSubjectsConsidered++;
//
//                        if (!EligibilityUtils.meetsRequirement(candidateGrade, requiredGrade)) {
//                            allMeetRequirement = false;
//                            notes.add("⚠️ Alternative (allOf) below required: " + subjNorm + " (" + candidateGrade + ")");
//                        } else {
//                            notes.add("✅ Alternative (allOf) ok: " + subjNorm + " (" + candidateGrade + ")");
//                        }
//                    }
//
//                    if (allPresent && allMeetRequirement) {
//                        notes.add("✅ All subjects in group met requirement: " + groupSubjects);
//                    } else if (allPresent) {
//                        notes.add("⚠️ Some subjects in group did not meet requirement: " + groupSubjects);
//                    }
//                }
//            }
//
//            // ✅ ---- FINAL SCORE + DECISION ----
//            double percentage = EligibilityUtils.calculateEligibilityPercentage(totalScore, totalSubjectsConsidered);
//            percentages.put(program, percentage);
//
//            boolean allCoresMeet = programCores.entrySet().stream().allMatch(core -> {
//                String subjNorm = subjectNormalizer.normalize(core.getKey()).toUpperCase();
//                String candidateGrade = subjectGrades.get(subjNorm);
//                return candidateGrade != null && EligibilityUtils.meetsRequirement(candidateGrade, core.getValue());
//            });
//
//            University uni = program.getUniversity();
//
//            if (allCoresMeet && EligibilityUtils.isEligible(percentage)) {
//                eligiblePrograms.computeIfAbsent(uni, u -> new ArrayList<>()).add(program);
//                notes.add("✅ Eligible (" + String.format("%.2f", percentage) + "%)");
//            } else if (EligibilityUtils.isAlternative(percentage)) {
//                alternativePrograms.computeIfAbsent(uni, u -> new ArrayList<>()).add(program);
//                notes.add("⚠️ Alternative (" + String.format("%.2f", percentage) + "%)");
//            } else {
//                notes.add("❌ Not eligible (" + String.format("%.2f", percentage) + "%)");
//            }
//
//            explanations.put(program, notes);
//        }
//
//        // ✅ --- Filter universities if specified ---
//        Set<University> universities = Stream.concat(
//                eligiblePrograms.keySet().stream(),
//                alternativePrograms.keySet().stream()
//        ).collect(Collectors.toSet());
//
//        if (StringUtils.hasText(universityType)) {
//            universities = universities.stream()
//                    .filter(u -> u.getType().name().equalsIgnoreCase(universityType))
//                    .collect(Collectors.toSet());
//        }
//
//        // ✅ --- Persist eligibility results ---
//        ExamCheckRecord recordToSet = examCheckRecordRepository.findById(checkExamRecordId)
//                .orElseThrow(() -> new EntityNotFoundException("ExamCheckRecord not found: " + checkExamRecordId));
//
//        EligibilityRecord record = new EligibilityRecord();
//        record.setId(UUID.randomUUID().toString());
//        record.setUserId(userId);
//        record.setExamCheckRecord(recordToSet);
//        record.setCreatedAt(LocalDateTime.now(ZoneId.of("Africa/Accra")));
//        record.setSelectedCategories(categories.stream().map(Category::getName).toList());
//
//        List<UniversityEligibility> universityEligibilities = universities.stream()
//                .map(uni -> buildUniversityEligibility(uni, eligiblePrograms, alternativePrograms, percentages, explanations, record))
//                .toList();
//
//        record.setUniversities(universityEligibilities);
//        recordToSet.setCheckStatus(CheckStatus.CHECKED);
//        examCheckRecordRepository.save(recordToSet);
//
//        return eligibilityRecordRepository.save(record);
//    }
//
//
//



    private void validateInput(WaecCandidateEntity candidate, String userId, String checkExamRecordId, List<Long> categoryIds) {
        if (candidate == null || candidate.getResultDetails() == null || candidate.getResultDetails().isEmpty())
            throw new EligibilityException("Candidate result details are required");
        if (!StringUtils.hasText(userId))
            throw new EligibilityException("User ID is required");
        if (!StringUtils.hasText(checkExamRecordId))
            throw new EligibilityException("CheckExamRecord ID is required");
        if (categoryIds == null || categoryIds.isEmpty())
            throw new EligibilityException("At least one category must be selected");
    }

    private UniversityEligibility buildUniversityEligibility(
            University uni,
            Map<University, List<Program>> eligible,
            Map<University, List<Program>> alternative,
            Map<Program, Double> percentages,
            Map<Program, List<String>> explanations,
            EligibilityRecord record) {

        UniversityEligibility ue = new UniversityEligibility();
        ue.setUniversityName(uni.getName());
        ue.setType(uni.getType().name());
        ue.setLocation(uni.getLocation());
        ue.setEligibilityRecord(record);

        List<EligibleProgram> eligibleList = eligible.getOrDefault(uni, List.of()).stream()
                .map(p -> toEligibleProgram(p, percentages, ue))
                .toList();

        List<AlternativeProgram> altList = alternative.getOrDefault(uni, List.of()).stream()
                .map(p -> toAlternativeProgram(p, percentages, explanations, ue))
                .toList();

        ue.setEligiblePrograms(eligibleList);
        ue.setAlternativePrograms(altList);
        return ue;
    }

    private EligibleProgram toEligibleProgram(Program p, Map<Program, Double> perc, UniversityEligibility ue) {
        EligibleProgram ep = new EligibleProgram();
        ep.setName(p.getName());
//        ep.setCutoffPoints(p.getCutoffPoints());
        ep.setPercentage(perc.getOrDefault(p, 0.0));
        ep.setCategories(p.getCategories().stream().map(Category::getName).toList());
        ep.setUniversityEligibility(ue);
        return ep;
    }

    private AlternativeProgram toAlternativeProgram(Program p, Map<Program, Double> perc, Map<Program, List<String>> expl, UniversityEligibility ue) {
        AlternativeProgram ap = new AlternativeProgram();
        ap.setName(p.getName());
//        ap.setCutoffPoints(p.getCutoffPoints());
        ap.setPercentage(perc.getOrDefault(p, 0.0));
        ap.setExplanations(expl.getOrDefault(p, List.of()));
        ap.setCategories(p.getCategories().stream().map(Category::getName).toList());
        ap.setUniversityEligibility(ue);
        return ap;
    }
}






