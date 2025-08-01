package com.alibou.book.Services;

import com.alibou.book.DTO.*;
import com.alibou.book.Entity.*;
import com.alibou.book.Repositories.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
@Service
public class WaecApiService {

    private final RestTemplate waecApiRestTemplate;
    private final ObjectMapper objectMapper;
    private final WaecCandidateRepository waecCandidateRepository;
    private final ProgramRepository programRepository;

    private final ExamCheckRecordRepository examCheckRecordRepository;

    private final EligibilityRecordRepository eligibilityRecordRepository;

    private final ProgramRecommendationService aiRecommendationService;
    private final CategoryRepository categoryRepository;


    @Value("${waec.api.url}")
    private String apiUrl;

    public WaecApiService(
            RestTemplate waecApiRestTemplate,
            ObjectMapper objectMapper,
            WaecCandidateRepository waecCandidateRepository,
            ProgramRepository programRepository, ExamCheckRecordRepository examCheckRecordRepository, EligibilityRecordRepository eligibilityRecordRepository, ProgramRecommendationService aiRecommendationService, CategoryRepository categoryRepository) {
        this.waecApiRestTemplate = waecApiRestTemplate;
        this.objectMapper = objectMapper;
        this.waecCandidateRepository = waecCandidateRepository;
        this.programRepository = programRepository;
        this.examCheckRecordRepository = examCheckRecordRepository;
        this.eligibilityRecordRepository = eligibilityRecordRepository;
        this.aiRecommendationService = aiRecommendationService;
        this.categoryRepository = categoryRepository;
    }


    // VERIFY RETURNS THE DATA FROM THE DATABASE IF EXIST AND FETCH FROM THE WAEC API OTHERWISE


    @Transactional  // Add this annotation
    public ResponseEntity<?> verifyResult(WaecResultsRequest request, String recordId) {
//        String externalRef = request.getReqref();

        System.out.println(recordId);

        // First, find or create an ExamCheckRecord (but don't increment yet)
        //ExamCheckRecord checkRecord = examCheckRecordRepository.findByUserId(request.getUserId());
        //Optional<ExamCheckRecord> checkRecord  = examCheckRecordRepository.findByExternalRef(recordId);
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


//    public List<UniversityEligibilityDTO> checkEligibility(WaecCandidateEntity candidate, String universityType) {
//        System.out.println("\n🔍 Checking eligibility for: " + candidate.getCname() + " (Index: " + candidate.getCindex() + ")");
//
//        Map<String, String> subjectGrades = candidate.getResultDetails().stream()
//                .collect(Collectors.toMap(
//                        WaecResultDetailEntity::getSubject,
//                        r -> r.getGrade().trim().toUpperCase()
//                ));
//
//        System.out.println("📘 Extracted Grades: " + subjectGrades);
//
//        Set<String> coreSubjects = Set.of("ENGLISH LANG", "MATHEMATICS(CORE)", "SOCIAL STUDIES", "INTEGRATED SCIENCE");
//
//        Map<String, Integer> gradeScale = Map.ofEntries(
//                Map.entry("A1", 100), Map.entry("B2", 90), Map.entry("B3", 80),
//                Map.entry("C4", 70), Map.entry("C5", 60), Map.entry("C6", 50),
//                Map.entry("D7", 40), Map.entry("E8", 30), Map.entry("F9", 0), Map.entry("*", 0)
//        );
//
//        Map<University, List<Program>> eligibleProgramsMap = new HashMap<>();
//        Map<University, List<Program>> alternativeProgramsMap = new HashMap<>();
//        Map<Program, List<String>> programExplanations = new HashMap<>();
//        Map<Program, Double> percentageMap = new HashMap<>();
//
//        for (Program program : programRepository.findAll()) {
//            University university = program.getUniversity();
//            System.out.println("\n➡️ Checking program: " + program.getName() + " at " + university.getName());
//
//            boolean eligible = true;
//            int scoreDifference = 0;
//            boolean failedCore = false;
//            List<Integer> scores = new ArrayList<>();
//            List<String> explanation = new ArrayList<>();
//
//            for (Map.Entry<String, String> requirement : program.getCutoffPoints().entrySet()) {
//                String subject = requirement.getKey();
//                String requiredGrade = requirement.getValue().trim().toUpperCase();
//                String userGrade = subjectGrades.get(subject);
//
//                System.out.printf("   🔎 Subject: %-20s Required: %-3s | User: %-3s%n", subject, requiredGrade, userGrade);
//
//                if (userGrade == null || !gradeScale.containsKey(userGrade) || !gradeScale.containsKey(requiredGrade)) {
//                    explanation.add("Invalid or missing grade for subject: " + subject);
//                    System.out.println("   ❌ Invalid or missing grade");
//                    eligible = false;
//                    break;
//                }
//
//                int userScore = gradeScale.get(userGrade);
//                int requiredScore = gradeScale.get(requiredGrade);
//
//                if (coreSubjects.contains(subject) && (userGrade.equals("F9") || userGrade.equals("*"))) {
//                    failedCore = true;
//                }
//
//                scores.add(userScore);
//
//                if (userScore < requiredScore) {
//                    int diff = requiredScore - userScore;
//                    scoreDifference += diff;
//                    explanation.add(String.format("Subject: %s - Required: %s (%d), Got: %s (%d), Diff: -%d",
//                            subject, requiredGrade, requiredScore, userGrade, userScore, diff));
//                    System.out.println("   ❌ Score too low. Diff: -" + diff);
//                    eligible = false;
//                } else {
//                    System.out.println("   ✅ Passed");
//                }
//            }
//
//            double percentage = (failedCore || scores.isEmpty())
//                    ? 0.0
//                    : Math.round(scores.stream().mapToInt(i -> i).average().orElse(0.0) * 100.0) / 100.0;
//            percentageMap.put(program, percentage);
//
//            if (eligible && !failedCore) {
//                System.out.println("✅ Fully eligible for: " + program.getName() + " (" + percentage + "%)");
//                eligibleProgramsMap.computeIfAbsent(university, u -> new ArrayList<>()).add(program);
//            } else if (!failedCore && scoreDifference <= 20) {
//                System.out.println("⚠️ Alternative match for: " + program.getName() + " (" + percentage + "%)");
//                alternativeProgramsMap.computeIfAbsent(university, u -> new ArrayList<>()).add(program);
//                programExplanations.put(program, explanation);
//            } else {
//                System.out.println("🚫 Not eligible for: " + program.getName());
//            }
//        }
//
//        Set<University> allUniversities = new HashSet<>();
//        allUniversities.addAll(eligibleProgramsMap.keySet());
//        allUniversities.addAll(alternativeProgramsMap.keySet());
//
//        // ✅ Filter by university type if it's provided
//        if (universityType != null && !universityType.isBlank()) {
//            String typeFilter = universityType.trim().toUpperCase();
//            System.out.println("🔎 Filtering universities by type: " + typeFilter);
//
//            allUniversities = allUniversities.stream()
//                    .filter(u -> u.getType().name().equalsIgnoreCase(typeFilter))
//                    .collect(Collectors.toSet());
//        }
//
//        List<UniversityEligibilityDTO> response = new ArrayList<>();
//
//        for (University university : allUniversities) {
//            List<EligibleProgramDTO> eligibleDTOs = eligibleProgramsMap.getOrDefault(university, List.of()).stream()
//                    .map(p -> new EligibleProgramDTO(p.getName(), p.getCutoffPoints(), percentageMap.getOrDefault(p, 0.0)))
//                    .collect(Collectors.toList());
//
//            List<AlternativeProgramDTO> alternativeDTOs = alternativeProgramsMap.getOrDefault(university, List.of()).stream()
//                    .map(p -> new AlternativeProgramDTO(
//                            p.getName(),
//                            p.getCutoffPoints(),
//                            programExplanations.getOrDefault(p, List.of()),
//                            percentageMap.getOrDefault(p, 0.0)
//                    ))
//                    .collect(Collectors.toList());
//
//            response.add(new UniversityEligibilityDTO(
//                    university.getName(),
//                    university.getLocation(),
//                    university.getType().name(),
//                    eligibleDTOs,
//                    alternativeDTOs
//            ));
//        }
//
//        System.out.println("\n🎯 Eligibility check complete. Universities found: " + response.size());
//        return response;
//    }


//    public List<UniversityEligibilityDTO> checkEligibility(WaecCandidateEntity candidate, String universityType) {
//        System.out.println("\n🔍 Checking eligibility for: " + candidate.getCname() + " (Index: " + candidate.getCindex() + ")");
//
//        Map<String, String> subjectGrades = candidate.getResultDetails().stream()
//                .collect(Collectors.toMap(
//                        WaecResultDetailEntity::getSubject,
//                        r -> r.getGrade().trim().toUpperCase()
//                ));
//
//        System.out.println("📘 Extracted Grades: " + subjectGrades);
//
//        Set<String> coreSubjects = Set.of("ENGLISH LANG", "MATHEMATICS(CORE)", "SOCIAL STUDIES", "INTEGRATED SCIENCE");
//
//        Map<String, Integer> gradeScale = Map.ofEntries(
//                Map.entry("A1", 100), Map.entry("B2", 90), Map.entry("B3", 80),
//                Map.entry("C4", 70), Map.entry("C5", 60), Map.entry("C6", 50),
//                Map.entry("D7", 40), Map.entry("E8", 30), Map.entry("F9", 0), Map.entry("*", 0)
//        );
//
//        Map<University, List<Program>> eligibleProgramsMap = new HashMap<>();
//        Map<University, List<Program>> alternativeProgramsMap = new HashMap<>();
//        Map<Program, List<String>> programExplanations = new HashMap<>();
//        Map<Program, Double> percentageMap = new HashMap<>();
//
//        // Helper method to estimate admission probability based on percentage
//
//        for (Program program : programRepository.findAll()) {
//            University university = program.getUniversity();
//            System.out.println("\n➡️ Checking program: " + program.getName() + " at " + university.getName());
//
//            boolean eligible = true;
//            int scoreDifference = 0;
//            boolean failedCore = false;
//            List<Integer> scores = new ArrayList<>();
//            List<String> explanation = new ArrayList<>();
//
//            for (Map.Entry<String, String> requirement : program.getCutoffPoints().entrySet()) {
//                String subject = requirement.getKey();
//                String requiredGrade = requirement.getValue().trim().toUpperCase();
//                String userGrade = subjectGrades.get(subject);
//
//                System.out.printf("   🔎 Subject: %-20s Required: %-3s | User: %-3s%n", subject, requiredGrade, userGrade);
//
//                if (userGrade == null || !gradeScale.containsKey(userGrade) || !gradeScale.containsKey(requiredGrade)) {
//                    explanation.add("Invalid or missing grade for subject: " + subject);
//                    System.out.println("   ❌ Invalid or missing grade");
//                    eligible = false;
//                    break;
//                }
//
//                int userScore = gradeScale.get(userGrade);
//                int requiredScore = gradeScale.get(requiredGrade);
//
//                if (coreSubjects.contains(subject) && (userGrade.equals("F9") || userGrade.equals("*"))) {
//                    failedCore = true;
//                }
//
//                scores.add(userScore);
//
//                if (userScore < requiredScore) {
//                    int diff = requiredScore - userScore;
//                    scoreDifference += diff;
//                    explanation.add(String.format("Subject: %s - Required: %s (%d), Got: %s (%d), Diff: -%d",
//                            subject, requiredGrade, requiredScore, userGrade, userScore, diff));
//                    System.out.println("   ❌ Score too low. Diff: -" + diff);
//                    eligible = false;
//                } else {
//                    System.out.println("   ✅ Passed");
//                }
//            }
//
//            double percentage = (failedCore || scores.isEmpty())
//                    ? 0.0
//                    : Math.round(scores.stream().mapToInt(i -> i).average().orElse(0.0) * 100.0) / 100.0;
//
//            double probability = estimateAdmissionProbability(percentage);
//            percentageMap.put(program, percentage);
//
//            if (eligible && !failedCore) {
//                System.out.printf("✅ Fully eligible for: %s (%.2f%%) | Estimated Admission Probability: %.0f%%%n",
//                        program.getName(), percentage, probability * 100);
//                eligibleProgramsMap.computeIfAbsent(university, u -> new ArrayList<>()).add(program);
//            } else if (!failedCore && scoreDifference <= 20) {
//                System.out.printf("⚠️ Alternative match for: %s (%.2f%%) | Estimated Admission Probability: %.0f%%%n",
//                        program.getName(), percentage, probability * 100);
//                alternativeProgramsMap.computeIfAbsent(university, u -> new ArrayList<>()).add(program);
//                programExplanations.put(program, explanation);
//            } else {
//                System.out.println("🚫 Not eligible for: " + program.getName());
//            }
//        }
//
//        Set<University> allUniversities = new HashSet<>();
//        allUniversities.addAll(eligibleProgramsMap.keySet());
//        allUniversities.addAll(alternativeProgramsMap.keySet());
//
//        // ✅ Filter by university type if it's provided
//        if (universityType != null && !universityType.isBlank()) {
//            String typeFilter = universityType.trim().toUpperCase();
//            System.out.println("🔎 Filtering universities by type: " + typeFilter);
//
//            allUniversities = allUniversities.stream()
//                    .filter(u -> u.getType().name().equalsIgnoreCase(typeFilter))
//                    .collect(Collectors.toSet());
//        }
//
//        List<UniversityEligibilityDTO> response = new ArrayList<>();
//
//        for (University university : allUniversities) {
//            List<EligibleProgramDTO> eligibleDTOs = eligibleProgramsMap.getOrDefault(university, List.of()).stream()
//                    .map(p -> {
//                        double percent = percentageMap.getOrDefault(p, 0.0);
//                        double prob = estimateAdmissionProbability(percent);
//                        return new EligibleProgramDTO(p.getName(), p.getCutoffPoints(), percent, prob);
//                    })
//                    .collect(Collectors.toList());
//
//            List<AlternativeProgramDTO> alternativeDTOs = alternativeProgramsMap.getOrDefault(university, List.of()).stream()
//                    .map(p -> {
//                        double percent = percentageMap.getOrDefault(p, 0.0);
//                        double prob = estimateAdmissionProbability(percent);
//                        return new AlternativeProgramDTO(
//                                p.getName(),
//                                p.getCutoffPoints(),
//                                programExplanations.getOrDefault(p, List.of()),
//                                percent,
//                                prob
//                        );
//                    })
//                    .collect(Collectors.toList());
//
//            response.add(new UniversityEligibilityDTO(
//                    university.getName(),
//                    university.getLocation(),
//                    university.getType().name(),
//                    eligibleDTOs,
//                    alternativeDTOs
//            ));
//        }
//
//        System.out.println("\n🎯 Eligibility check complete. Universities found: " + response.size());
//        return response;
//    }

//    public EligibilityRecord checkEligibility(
//            WaecCandidateEntity candidate,
//            String universityType,
//            String userId
//    ) {
////        ExamCheckRecord examCheckRecord = examCheckRecordRepository.findById(recordId)
////                .orElseThrow(() -> new RuntimeException("Record not found: " + recordId));
//
//        System.out.println("\n🔍 Checking eligibility for: " + candidate.getCname() + " (Index: " + candidate.getCindex() + ")");
//
//
//
//
//        // Normalize function (can be moved to a utility class)
//        Function<String, String> normalizeSubject = subject -> {
//            Map<String, String> aliases = Map.of(
//                    "ENGLISH LANG", "ENGLISH LANGUAGE",
//                    "MATHS", "MATHEMATICS(CORE)",
//                    "MATHEMATICS", "MATHEMATICS(CORE)",
//                    "SOCIAL STUDY", "SOCIAL STUDIES",
//                    "INTEGRATED SCI", "INTEGRATED SCIENCE"
//            );
//            return aliases.getOrDefault(subject.trim().toUpperCase(), subject.trim().toUpperCase());
//        };
//
//// Canonical core subjects
//        Set<String> coreSubjects = Set.of(
//                "ENGLISH LANGUAGE",
//                "MATHEMATICS(CORE)",
//                "SOCIAL STUDIES",
//                "INTEGRATED SCIENCE"
//        );
//
//// Grade scale mapping
//        Map<String, Integer> gradeScale = Map.ofEntries(
//                Map.entry("A1", 100), Map.entry("B2", 90), Map.entry("B3", 80),
//                Map.entry("C4", 70), Map.entry("C5", 60), Map.entry("C6", 50),
//                Map.entry("D7", 40), Map.entry("E8", 30), Map.entry("F9", 0), Map.entry("*", 0)
//        );
//        Map<String, String> subjectGrades = candidate.getResultDetails().stream()
//                .collect(Collectors.toMap(
//                        result -> normalizeSubject.apply(result.getSubject()),
//                        result -> result.getGrade().trim().toUpperCase(),
//                        (grade1, grade2) -> {
//                            // In case of duplicate subjects, choose higher grade
//                            int g1 = gradeScale.getOrDefault(grade1, 0);
//                            int g2 = gradeScale.getOrDefault(grade2, 0);
//                            return g1 >= g2 ? grade1 : grade2;
//                        }
//                ));
//
//        Map<University, List<Program>> eligibleProgramsMap = new HashMap<>();
//        Map<University, List<Program>> alternativeProgramsMap = new HashMap<>();
//        Map<Program, List<String>> programExplanations = new HashMap<>();
//        Map<Program, Double> percentageMap = new HashMap<>();
//
//        for (Program program : programRepository.findAll()) {
//            University university = program.getUniversity();
//
//            boolean eligible = true;
//            int scoreDifference = 0;
//            boolean failedCore = false;
//            List<Integer> scores = new ArrayList<>();
//            List<String> explanation = new ArrayList<>();
//
//            for (Map.Entry<String, String> requirement : program.getCutoffPoints().entrySet()) {
//                String subject = requirement.getKey();
//                String requiredGrade = requirement.getValue().trim().toUpperCase();
//                String userGrade = subjectGrades.get(subject);
//
//                if (userGrade == null || !gradeScale.containsKey(userGrade) || !gradeScale.containsKey(requiredGrade)) {
//                    explanation.add("Invalid or missing grade for subject: " + subject);
//                    eligible = false;
//                    break;
//                }
//
//                int userScore = gradeScale.get(userGrade);
//                int requiredScore = gradeScale.get(requiredGrade);
//
//                if (coreSubjects.contains(subject) && (userGrade.equals("F9") || userGrade.equals("*"))) {
//                    failedCore = true;
//                }
//
//                scores.add(userScore);
//
//                if (userScore < requiredScore) {
//                    int diff = requiredScore - userScore;
//                    scoreDifference += diff;
//                    explanation.add(String.format("Subject: %s - Required: %s (%d), Got: %s (%d), Diff: -%d",
//                            subject, requiredGrade, requiredScore, userGrade, userScore, diff));
//                    eligible = false;
//                }
//            }
//
//            double percentage = (failedCore || scores.isEmpty())
//                    ? 0.0
//                    : Math.round(scores.stream().mapToInt(i -> i).average().orElse(0.0) * 100.0) / 100.0;
//
//            double probability = estimateAdmissionProbability(percentage);
//            percentageMap.put(program, percentage);
//
//            if (eligible && !failedCore) {
//                eligibleProgramsMap.computeIfAbsent(university, u -> new ArrayList<>()).add(program);
//            } else if (!failedCore && scoreDifference <= 20) {
//                alternativeProgramsMap.computeIfAbsent(university, u -> new ArrayList<>()).add(program);
//                programExplanations.put(program, explanation);
//            }
//        }
//
//        Set<University> allUniversities = new HashSet<>();
//        allUniversities.addAll(eligibleProgramsMap.keySet());
//        allUniversities.addAll(alternativeProgramsMap.keySet());
//
//        if (universityType != null && !universityType.isBlank()) {
//            String typeFilter = universityType.trim().toUpperCase();
//            allUniversities = allUniversities.stream()
//                    .filter(u -> u.getType().name().equalsIgnoreCase(typeFilter))
//                    .collect(Collectors.toSet());
//        }
//
//        // ✅ Create the EligibilityRecord early
//        EligibilityRecord record = new EligibilityRecord();
//        record.setId(UUID.randomUUID().toString());
//        record.setUserId(userId);
//        record.setCreatedAt(LocalDateTime.ofInstant(Instant.now(), ZoneId.of("Africa/Accra")));
//      //  record.setExamCheckRecord(examCheckRecord);
//
//        List<UniversityEligibility> universityEntities = new ArrayList<>();
//
//        for (University university : allUniversities) {
//            UniversityEligibility entity = new UniversityEligibility();
//            entity.setUniversityName(university.getName());
//            entity.setLocation(university.getLocation());
//            entity.setType(university.getType().name());
//
//            // 🔁 Link back to parent EligibilityRecord
//            entity.setEligibilityRecord(record);
//
//            List<EligibleProgram> eligiblePrograms = eligibleProgramsMap.getOrDefault(university, List.of()).stream()
//                    .map(p -> {
//                        double percent = percentageMap.getOrDefault(p, 0.0);
//
//                        EligibleProgram ep = new EligibleProgram();
//                        ep.setName(p.getName());
//                        ep.setCutoffPoints(p.getCutoffPoints());
//                        ep.setPercentage(percent);
//                        ep.setUniversityEligibility(entity); // back-link
//                        return ep;
//                    }).collect(Collectors.toList());
//
//            List<AlternativeProgram> alternativePrograms = alternativeProgramsMap.getOrDefault(university, List.of()).stream()
//                    .map(p -> {
//                        double percent = percentageMap.getOrDefault(p, 0.0);
//
//                        AlternativeProgram ap = new AlternativeProgram();
//                        ap.setName(p.getName());
//                        ap.setCutoffPoints(p.getCutoffPoints());
//                        ap.setExplanations(programExplanations.getOrDefault(p, List.of()));
//                        ap.setPercentage(percent);
//                        ap.setUniversityEligibility(entity); // back-link
//                        return ap;
//                    }).collect(Collectors.toList());
//
//            entity.setEligiblePrograms(eligiblePrograms);
//            entity.setAlternativePrograms(alternativePrograms);
//
//            universityEntities.add(entity);
//        }
//
//        // 🔁 Finally link universities to the record
//        record.setUniversities(universityEntities);
//
//        return eligibilityRecordRepository.save(record);
//    }
//
//


    private double estimateAdmissionProbability(double percentage) {
        if (percentage >= 90) return 0.95;
        if (percentage >= 80) return 0.85;
        if (percentage >= 70) return 0.70;
        if (percentage >= 60) return 0.50;
        if (percentage >= 50) return 0.30;
        return 0.10;
    }


//
//
//        public EligibilityRecord checkEligibility(
//                WaecCandidateEntity candidate,
//                String universityType,
//                String userId) {
//
//            System.out.println("\n🔍 Checking eligibility for: " + candidate.getCname() +
//                    " (Index: " + candidate.getCindex() + ")");
//
//            // Normalize subject names
//            Function<String, String> normalizeSubject = subject -> {
//                Map<String, String> aliases = Map.of(
//                        "ENGLISH LANG", "ENGLISH LANGUAGE",
//                        "MATHS", "MATHEMATICS(CORE)",
//                        "MATHEMATICS", "MATHEMATICS(CORE)",
//                        "SOCIAL STUDY", "SOCIAL STUDIES",
//                        "INTEGRATED SCI", "INTEGRATED SCIENCE"
//                );
//                return aliases.getOrDefault(subject.trim().toUpperCase(), subject.trim().toUpperCase());
//            };
//
//            // Core subjects and grade scale
//            Set<String> coreSubjects = Set.of(
//                    "ENGLISH LANGUAGE",
//                    "MATHEMATICS(CORE)",
//                    "SOCIAL STUDIES",
//                    "INTEGRATED SCIENCE"
//            );
//
//            Map<String, Integer> gradeScale = Map.ofEntries(
//                    Map.entry("A1", 100), Map.entry("B2", 90), Map.entry("B3", 80),
//                    Map.entry("C4", 70), Map.entry("C5", 60), Map.entry("C6", 50),
//                    Map.entry("D7", 40), Map.entry("E8", 30), Map.entry("F9", 0),
//                    Map.entry("*", 0)
//            );
//
//            // Process candidate's grades
//            Map<String, String> subjectGrades = candidate.getResultDetails().stream()
//                    .collect(Collectors.toMap(
//                            result -> normalizeSubject.apply(result.getSubject()),
//                            result -> result.getGrade().trim().toUpperCase(),
//                            (grade1, grade2) -> {
//                                int g1 = gradeScale.getOrDefault(grade1, 0);
//                                int g2 = gradeScale.getOrDefault(grade2, 0);
//                                return g1 >= g2 ? grade1 : grade2;
//                            }
//                    ));
//
//            // Initialize data structures
//            Map<University, List<Program>> eligibleProgramsMap = new HashMap<>();
//            Map<University, List<Program>> alternativeProgramsMap = new HashMap<>();
//            Map<Program, List<String>> programExplanations = new HashMap<>();
//            Map<Program, Double> percentageMap = new HashMap<>();
//
//            // Evaluate all programs
//            for (Program program : programRepository.findAll()) {
//                University university = program.getUniversity();
//                boolean eligible = true;
//                int scoreDifference = 0;
//                boolean failedCore = false;
//                List<Integer> scores = new ArrayList<>();
//                List<String> explanation = new ArrayList<>();
//
//                for (Map.Entry<String, String> requirement : program.getCutoffPoints().entrySet()) {
//                    String subject = requirement.getKey();
//                    String requiredGrade = requirement.getValue().trim().toUpperCase();
//                    String userGrade = subjectGrades.get(subject);
//
//                    if (userGrade == null || !gradeScale.containsKey(userGrade)){
//                        explanation.add("Missing grade for: " + subject);
//                        eligible = false;
//                        break;
//                    }
//
//                    int userScore = gradeScale.get(userGrade);
//                    int requiredScore = gradeScale.get(requiredGrade);
//
//                    if (coreSubjects.contains(subject) && (userGrade.equals("F9") || userGrade.equals("*"))) {
//                        failedCore = true;
//                    }
//
//                    scores.add(userScore);
//
//                    if (userScore < requiredScore) {
//                        int diff = requiredScore - userScore;
//                        scoreDifference += diff;
//                        explanation.add(String.format("%s: Required %s (%d), Got %s (%d)",
//                                subject, requiredGrade, requiredScore, userGrade, userScore));
//                        eligible = false;
//                    }
//                }
//
//                double percentage = (failedCore || scores.isEmpty()) ? 0.0 :
//                        Math.round(scores.stream().mapToInt(i -> i).average().orElse(0.0) * 100.0) / 100.0;
//
//                percentageMap.put(program, percentage);
//
//                if (eligible && !failedCore) {
//                    eligibleProgramsMap.computeIfAbsent(university, u -> new ArrayList<>()).add(program);
//                } else if (!failedCore && scoreDifference <= 20) {
//                    alternativeProgramsMap.computeIfAbsent(university, u -> new ArrayList<>()).add(program);
//                    programExplanations.put(program, explanation);
//                }
//            }
//
//            // Generate AI recommendations
//            Map<Program, AIRecommendation> aiRecommendations = aiRecommendationService
//                    .generateRecommendations(eligibleProgramsMap, alternativeProgramsMap, candidate);
//
//            // Filter universities by type
//            Set<University> allUniversities = new HashSet<>();
//            allUniversities.addAll(eligibleProgramsMap.keySet());
//            allUniversities.addAll(alternativeProgramsMap.keySet());
//
//            if (universityType != null && !universityType.isBlank()) {
//                allUniversities = allUniversities.stream()
//                        .filter(u -> u.getType().name().equalsIgnoreCase(universityType.trim()))
//                        .collect(Collectors.toSet());
//            }
//
//            // Create eligibility record
//            EligibilityRecord record = new EligibilityRecord();
//            record.setId(UUID.randomUUID().toString());
//            record.setUserId(userId);
//            record.setCreatedAt(LocalDateTime.now(ZoneId.of("Africa/Accra")));
//
//            // Process universities and programs
//            List<UniversityEligibility> universityEntities = allUniversities.stream()
//                    .map(university -> createUniversityEligibility(
//                            university,
//                            eligibleProgramsMap.getOrDefault(university, List.of()),
//                            alternativeProgramsMap.getOrDefault(university, List.of()),
//                            programExplanations,
//                            percentageMap,
//                            aiRecommendations,
//                            record))
//                    .collect(Collectors.toList());
//
//            record.setUniversities(universityEntities);
//            return eligibilityRecordRepository.save(record);
//        }
//
//        private UniversityEligibility createUniversityEligibility(
//                University university,
//                List<Program> eligiblePrograms,
//                List<Program> alternativePrograms,
//                Map<Program, List<String>> programExplanations,
//                Map<Program, Double> percentageMap,
//                Map<Program, AIRecommendation> aiRecommendations,
//                EligibilityRecord record) {
//
//            UniversityEligibility entity = new UniversityEligibility();
//            entity.setUniversityName(university.getName());
//            entity.setLocation(university.getLocation());
//            entity.setType(university.getType().name());
//            entity.setEligibilityRecord(record);
//
//            // Process eligible programs
//            List<EligibleProgram> eligibleProgramEntities = eligiblePrograms.stream()
//                    .map(program -> {
//                        EligibleProgram ep = new EligibleProgram();
//                        ep.setName(program.getName());
//                        ep.setCutoffPoints(program.getCutoffPoints());
//                        ep.setPercentage(percentageMap.get(program));
//                        ep.setAiRecommendation(aiRecommendations.get(program));
//                        ep.setUniversityEligibility(entity);
//                        return ep;
//                    })
//                    .collect(Collectors.toList());
//
//            // Process alternative programs
//            List<AlternativeProgram> alternativeProgramEntities = alternativePrograms.stream()
//                    .map(program -> {
//                        AlternativeProgram ap = new AlternativeProgram();
//                        ap.setName(program.getName());
//                        ap.setCutoffPoints(program.getCutoffPoints());
//                        ap.setPercentage(percentageMap.get(program));
//                        ap.setExplanations(programExplanations.get(program));
//                        ap.setAiRecommendation(aiRecommendations.get(program));
//                        ap.setUniversityEligibility(entity);
//                        return ap;
//                    })
//                    .collect(Collectors.toList());
//
//            entity.setEligiblePrograms(eligibleProgramEntities);
//            entity.setAlternativePrograms(alternativeProgramEntities);
//
//            return entity;
//        }


//
//
//    public EligibilityRecord checkEligibility(
//            WaecCandidateEntity candidate,
//            String universityType,
//            String userId,
//            List<Long> userSelectedCategoryIds) {
//
//        // Validate input
//        if (candidate == null || candidate.getResultDetails() == null || candidate.getResultDetails().isEmpty()) {
//            throw new IllegalArgumentException("Candidate result details are required");
//        }
//        if (userSelectedCategoryIds == null || userSelectedCategoryIds.isEmpty()) {
//            throw new IllegalArgumentException("At least one category must be selected");
//        }
//
//        System.out.println("\n🔍 Checking eligibility for candidate");
//
//        // Normalize subject names
//        Function<String, String> normalizeSubject = subject -> {
//            Map<String, String> aliases = Map.of(
//                    "ENGLISH LANG", "ENGLISH LANGUAGE",
//                    "MATHS", "MATHEMATICS(CORE)",
//                    "MATHEMATICS", "MATHEMATICS(CORE)",
//                    "SOCIAL STUDY", "SOCIAL STUDIES",
//                    "INTEGRATED SCI", "INTEGRATED SCIENCE"
//            );
//            return aliases.getOrDefault(subject.trim().toUpperCase(), subject.trim().toUpperCase());
//        };
//
//        // Core subjects and grade scale
//        Set<String> coreSubjects = Set.of(
//                "ENGLISH LANGUAGE",
//                "MATHEMATICS(CORE)",
//                "SOCIAL STUDIES",
//                "INTEGRATED SCIENCE"
//        );
//
//        Map<String, Integer> gradeScale = Map.ofEntries(
//                Map.entry("A1", 100), Map.entry("B2", 90), Map.entry("B3", 80),
//                Map.entry("C4", 70), Map.entry("C5", 60), Map.entry("C6", 50),
//                Map.entry("D7", 40), Map.entry("E8", 30), Map.entry("F9", 0),
//                Map.entry("*", 0)
//        );
//
//        // Process candidate's grades
//        Map<String, String> subjectGrades = candidate.getResultDetails().stream()
//                .collect(Collectors.toMap(
//                        result -> normalizeSubject.apply(result.getSubject()),
//                        result -> result.getGrade().trim().toUpperCase(),
//                        (grade1, grade2) -> {
//                            int g1 = gradeScale.getOrDefault(grade1, 0);
//                            int g2 = gradeScale.getOrDefault(grade2, 0);
//                            return g1 >= g2 ? grade1 : grade2;
//                        }
//                ));
//
//        // Get selected categories with programs
//        List<Category> selectedCategories = categoryRepository.findAllById(userSelectedCategoryIds);
//        if (selectedCategories.isEmpty()) {
//            throw new IllegalArgumentException("No valid categories found for the provided IDs");
//        }
//
//        // Get programs in selected categories
//        Set<Program> allProgramsInCategories = selectedCategories.stream()
//                .flatMap(category -> programRepository.findByCategories_Id(category.getId()).stream())
//                .collect(Collectors.toSet());
//
//        // Initialize data structures
//        Map<University, List<Program>> eligibleProgramsMap = new HashMap<>();
//        Map<University, List<Program>> alternativeProgramsMap = new HashMap<>();
//        Map<Program, List<String>> programExplanations = new HashMap<>();
//        Map<Program, Double> percentageMap = new HashMap<>();
//        Map<Program, Set<String>> programCategoriesMap = new HashMap<>();
//
//        // Build program to categories mapping
//        for (Program program : allProgramsInCategories) {
//            Set<String> categoryNames = program.getCategories().stream()
//                    .map(Category::getName)
//                    .collect(Collectors.toSet());
//            programCategoriesMap.put(program, categoryNames);
//        }
//
//        // Evaluate all programs in selected categories
//        for (Program program : allProgramsInCategories) {
//            University university = program.getUniversity();
//            boolean eligible = true;
//            int scoreDifference = 0;
//            boolean failedCore = false;
//            List<Integer> scores = new ArrayList<>();
//            List<String> explanation = new ArrayList<>();
//
//            for (Map.Entry<String, String> requirement : program.getCutoffPoints().entrySet()) {
//                String subject = requirement.getKey();
//                String requiredGrade = requirement.getValue().trim().toUpperCase();
//                String userGrade = subjectGrades.get(subject);
//
//                if (userGrade == null || !gradeScale.containsKey(userGrade)) {
//                    explanation.add("Missing grade for: " + subject);
//                    eligible = false;
//                    break;
//                }
//
//                int userScore = gradeScale.get(userGrade);
//                int requiredScore = gradeScale.get(requiredGrade);
//
//                if (coreSubjects.contains(subject) && (userGrade.equals("F9") || userGrade.equals("*"))) {
//                    failedCore = true;
//                }
//
//                scores.add(userScore);
//
//                if (userScore < requiredScore) {
//                    int diff = requiredScore - userScore;
//                    scoreDifference += diff;
//                    explanation.add(String.format("%s: Required %s (%d), Got %s (%d)",
//                            subject, requiredGrade, requiredScore, userGrade, userScore));
//                    eligible = false;
//                }
//            }
//
//            double percentage = (failedCore || scores.isEmpty()) ? 0.0 :
//                    Math.round(scores.stream().mapToInt(i -> i).average().orElse(0.0) * 100.0) / 100.0;
//
//            percentageMap.put(program, percentage);
//
//            if (eligible && !failedCore) {
//                eligibleProgramsMap.computeIfAbsent(university, u -> new ArrayList<>()).add(program);
//            } else if (!failedCore && scoreDifference <= 20) {
//                alternativeProgramsMap.computeIfAbsent(university, u -> new ArrayList<>()).add(program);
//                programExplanations.put(program, explanation);
//            }
//        }
//
//        // Filter universities by type
//        Set<University> filteredUniversities = new HashSet<>();
//        filteredUniversities.addAll(eligibleProgramsMap.keySet());
//        filteredUniversities.addAll(alternativeProgramsMap.keySet());
//
//        if (universityType != null && !universityType.isBlank()) {
//            filteredUniversities = filteredUniversities.stream()
//                    .filter(u -> u.getType().name().equalsIgnoreCase(universityType.trim()))
//                    .collect(Collectors.toSet());
//        }
//
//        // Create eligibility record with categories
//        EligibilityRecord record = new EligibilityRecord();
//        record.setId(UUID.randomUUID().toString());
//        record.setUserId(userId);
//        record.setCreatedAt(LocalDateTime.now(ZoneId.of("Africa/Accra")));
////        record.setSelectedCategories(selectedCategories.stream()
////                .map(Category::getName)
////                .collect(Collectors.toList()));
//
//        // Process universities and programs
//        List<UniversityEligibility> universityEntities = new ArrayList<>();
//
//        for (University university : filteredUniversities) {
//            List<Program> universityPrograms = allProgramsInCategories.stream()
//                    .filter(p -> p.getUniversity().equals(university))
//                    .collect(Collectors.toList());
//
//            if (universityPrograms.isEmpty()) {
//                continue;
//            }
//
//            UniversityEligibility universityEligibility = new UniversityEligibility();
//            universityEligibility.setUniversityName(university.getName());
//            universityEligibility.setLocation(university.getLocation());
//            universityEligibility.setType(university.getType().name());
//            universityEligibility.setEligibilityRecord(record);
//
//            // Process eligible programs with categories
//            List<EligibleProgram> eligiblePrograms = eligibleProgramsMap.getOrDefault(university, List.of()).stream()
//                    .map(program -> {
//                        EligibleProgram ep = new EligibleProgram();
//                        ep.setName(program.getName());
//                        ep.setCutoffPoints(program.getCutoffPoints());
//                        ep.setPercentage(percentageMap.get(program));
//                        ep.setCategories(new ArrayList<>(programCategoriesMap.get(program)));
//                        ep.setUniversityEligibility(universityEligibility);
//                        return ep;
//                    })
//                    .collect(Collectors.toList());
//
//            // Process alternative programs with categories
//            List<AlternativeProgram> alternativePrograms = alternativeProgramsMap.getOrDefault(university, List.of()).stream()
//                    .map(program -> {
//                        AlternativeProgram ap = new AlternativeProgram();
//                        ap.setName(program.getName());
//                        ap.setCutoffPoints(program.getCutoffPoints());
//                        ap.setPercentage(percentageMap.get(program));
//                        ap.setExplanations(programExplanations.get(program));
//                        ap.setCategories(new ArrayList<>(programCategoriesMap.get(program)));
//                        ap.setUniversityEligibility(universityEligibility);
//                        return ap;
//                    })
//                    .collect(Collectors.toList());
//
//            universityEligibility.setEligiblePrograms(eligiblePrograms);
//            universityEligibility.setAlternativePrograms(alternativePrograms);
//            universityEntities.add(universityEligibility);
//        }
//
//        record.setUniversities(universityEntities);
//        return eligibilityRecordRepository.save(record);
//    }
//
//
//
//


    public EligibilityRecord checkEligibility(
            WaecCandidateEntity candidate,
            String universityType,
            String userId,
            List<Long> userSelectedCategoryIds) {

        // Validate input
        if (candidate == null || candidate.getResultDetails() == null || candidate.getResultDetails().isEmpty()) {
            throw new IllegalArgumentException("Candidate result details are required");
        }
        if (userSelectedCategoryIds == null || userSelectedCategoryIds.isEmpty()) {
            throw new IllegalArgumentException("At least one category must be selected");
        }

        System.out.println("\n🔍 Checking eligibility for candidate");

        // Normalize subject names
        Function<String, String> normalizeSubject = subject -> {
            Map<String, String> aliases = Map.of(
                    "ENGLISH LANG", "ENGLISH LANGUAGE",
                    "MATHS", "MATHEMATICS(CORE)",
                    "MATHEMATICS", "MATHEMATICS(CORE)",
                    "SOCIAL STUDY", "SOCIAL STUDIES",
                    "INTEGRATED SCI", "INTEGRATED SCIENCE"
            );
            return aliases.getOrDefault(subject.trim().toUpperCase(), subject.trim().toUpperCase());
        };

        // Core subjects and grade scale
        Set<String> coreSubjects = Set.of(
                "ENGLISH LANGUAGE",
                "MATHEMATICS(CORE)",
                "SOCIAL STUDIES",
                "INTEGRATED SCIENCE"
        );

        Map<String, Integer> gradeScale = Map.ofEntries(
                Map.entry("A1", 100), Map.entry("B2", 90), Map.entry("B3", 80),
                Map.entry("C4", 70), Map.entry("C5", 60), Map.entry("C6", 50),
                Map.entry("D7", 40), Map.entry("E8", 30), Map.entry("F9", 0),
                Map.entry("*", 0)
        );

        // Process candidate's grades
        Map<String, String> subjectGrades = candidate.getResultDetails().stream()
                .collect(Collectors.toMap(
                        result -> normalizeSubject.apply(result.getSubject()),
                        result -> result.getGrade().trim().toUpperCase(),
                        (grade1, grade2) -> {
                            int g1 = gradeScale.getOrDefault(grade1, 0);
                            int g2 = gradeScale.getOrDefault(grade2, 0);
                            return g1 >= g2 ? grade1 : grade2;
                        }
                ));

        // Get selected categories with programs
        List<Category> selectedCategories = categoryRepository.findAllById(userSelectedCategoryIds);
        if (selectedCategories.isEmpty()) {
            throw new IllegalArgumentException("No valid categories found for the provided IDs");
        }

        // Get programs in selected categories
        Set<Program> allProgramsInCategories = selectedCategories.stream()
                .flatMap(category -> programRepository.findByCategories_Id(category.getId()).stream())
                .collect(Collectors.toSet());

        // Initialize data structures
        Map<University, List<Program>> eligibleProgramsMap = new HashMap<>();
        Map<University, List<Program>> alternativeProgramsMap = new HashMap<>();
        Map<Program, List<String>> programExplanations = new HashMap<>();
        Map<Program, Double> percentageMap = new HashMap<>();
        Map<Program, Set<String>> programCategoriesMap = new HashMap<>();

        // Build program to categories mapping
        for (Program program : allProgramsInCategories) {
            Set<String> categoryNames = program.getCategories().stream()
                    .map(Category::getName)
                    .collect(Collectors.toSet());
            programCategoriesMap.put(program, categoryNames);
        }

        // Evaluate all programs in selected categories
        for (Program program : allProgramsInCategories) {
            University university = program.getUniversity();
            boolean eligible = true;
            int scoreDifference = 0;
            boolean failedCore = false;
            List<Integer> scores = new ArrayList<>();
            List<String> explanation = new ArrayList<>();

            for (Map.Entry<String, String> requirement : program.getCutoffPoints().entrySet()) {
                String subject = requirement.getKey();
                String requiredGrade = requirement.getValue().trim().toUpperCase();
                String userGrade = subjectGrades.get(subject);

                if (userGrade == null || !gradeScale.containsKey(userGrade)) {
                    explanation.add("Missing grade for: " + subject);
                    eligible = false;
                    break;
                }

                int userScore = gradeScale.get(userGrade);
                int requiredScore = gradeScale.get(requiredGrade);

                if (coreSubjects.contains(subject) && (userGrade.equals("F9") || userGrade.equals("*"))) {
                    failedCore = true;
                }

                scores.add(userScore);

                if (userScore < requiredScore) {
                    int diff = requiredScore - userScore;
                    scoreDifference += diff;
                    explanation.add(String.format("%s: Required %s (%d), Got %s (%d)",
                            subject, requiredGrade, requiredScore, userGrade, userScore));
                    eligible = false;
                }
            }

            double percentage = (failedCore || scores.isEmpty()) ? 0.0 :
                    Math.round(scores.stream().mapToInt(i -> i).average().orElse(0.0) * 100.0) / 100.0;

            percentageMap.put(program, percentage);

            if (eligible && !failedCore) {
                eligibleProgramsMap.computeIfAbsent(university, u -> new ArrayList<>()).add(program);
            } else if (!failedCore && scoreDifference <= 20) {
                alternativeProgramsMap.computeIfAbsent(university, u -> new ArrayList<>()).add(program);
                programExplanations.put(program, explanation);
            }
        }

        // Generate AI recommendations for all programs
        Map<Program, AIRecommendation> aiRecommendations = aiRecommendationService
                .generateRecommendations(eligibleProgramsMap, alternativeProgramsMap, candidate);

        // Filter universities by type
        Set<University> filteredUniversities = new HashSet<>();
        filteredUniversities.addAll(eligibleProgramsMap.keySet());
        filteredUniversities.addAll(alternativeProgramsMap.keySet());

        if (universityType != null && !universityType.isBlank()) {
            filteredUniversities = filteredUniversities.stream()
                    .filter(u -> u.getType().name().equalsIgnoreCase(universityType.trim()))
                    .collect(Collectors.toSet());
        }

        // Create eligibility record with categories
        EligibilityRecord record = new EligibilityRecord();
        record.setId(UUID.randomUUID().toString());
        record.setUserId(userId);
        record.setCreatedAt(LocalDateTime.now(ZoneId.of("Africa/Accra")));
        record.setSelectedCategories(selectedCategories.stream()
                .map(Category::getName)
                .collect(Collectors.toList()));

        // Process universities and programs
        List<UniversityEligibility> universityEntities = new ArrayList<>();

        for (University university : filteredUniversities) {
            List<Program> universityPrograms = allProgramsInCategories.stream()
                    .filter(p -> p.getUniversity().equals(university))
                    .collect(Collectors.toList());

            if (universityPrograms.isEmpty()) {
                continue;
            }

            UniversityEligibility universityEligibility = new UniversityEligibility();
            universityEligibility.setUniversityName(university.getName());
            universityEligibility.setLocation(university.getLocation());
            universityEligibility.setType(university.getType().name());
            universityEligibility.setEligibilityRecord(record);

            // Process eligible programs with categories and AI recommendations
            List<EligibleProgram> eligiblePrograms = eligibleProgramsMap.getOrDefault(university, List.of()).stream()
                    .map(program -> {
                        EligibleProgram ep = new EligibleProgram();
                        ep.setName(program.getName());
                        ep.setCutoffPoints(program.getCutoffPoints());
                        ep.setPercentage(percentageMap.get(program));
                        ep.setCategories(new ArrayList<>(programCategoriesMap.get(program)));
//                        ep.setAiRecommendation(aiRecommendations.get(program));
                        ep.setUniversityEligibility(universityEligibility);
                        return ep;
                    })
                    .collect(Collectors.toList());

            // Process alternative programs with categories and AI recommendations
            List<AlternativeProgram> alternativePrograms = alternativeProgramsMap.getOrDefault(university, List.of()).stream()
                    .map(program -> {
                        AlternativeProgram ap = new AlternativeProgram();
                        ap.setName(program.getName());
                        ap.setCutoffPoints(program.getCutoffPoints());
                        ap.setPercentage(percentageMap.get(program));
                        ap.setExplanations(programExplanations.get(program));
                        ap.setCategories(new ArrayList<>(programCategoriesMap.get(program)));
                        ap.setAiRecommendation(aiRecommendations.get(program));
                        ap.setUniversityEligibility(universityEligibility);
                        return ap;
                    })
                    .collect(Collectors.toList());

            universityEligibility.setEligiblePrograms(eligiblePrograms);
            universityEligibility.setAlternativePrograms(alternativePrograms);
            universityEntities.add(universityEligibility);
        }

        record.setUniversities(universityEntities);
        return eligibilityRecordRepository.save(record);
    }








    public EligibilityRecord checkEligibilityAI(
            WaecCandidateEntity candidate,
            String universityType,
            String userId,
            List<Long> userSelectedCategoryIds) {

        // Validate input
        if (candidate == null || candidate.getResultDetails() == null || candidate.getResultDetails().isEmpty()) {
            throw new IllegalArgumentException("Candidate result details are required");
        }
        if (userSelectedCategoryIds == null || userSelectedCategoryIds.isEmpty()) {
            throw new IllegalArgumentException("At least one category must be selected");
        }

        // Normalize subject names
        Function<String, String> normalizeSubject = subject -> {
            Map<String, String> aliases = Map.of(
                    "ENGLISH LANG", "ENGLISH LANGUAGE",
                    "MATHS", "MATHEMATICS(CORE)",
                    "MATHEMATICS", "MATHEMATICS(CORE)",
                    "SOCIAL STUDY", "SOCIAL STUDIES",
                    "INTEGRATED SCI", "INTEGRATED SCIENCE"
            );
            return aliases.getOrDefault(subject.trim().toUpperCase(), subject.trim().toUpperCase());
        };

        // Core subjects and grade scale
        Set<String> coreSubjects = Set.of(
                "ENGLISH LANGUAGE",
                "MATHEMATICS(CORE)",
                "SOCIAL STUDIES",
                "INTEGRATED SCIENCE"
        );

        Map<String, Integer> gradeScale = Map.ofEntries(
                Map.entry("A1", 100), Map.entry("B2", 90), Map.entry("B3", 80),
                Map.entry("C4", 70), Map.entry("C5", 60), Map.entry("C6", 50),
                Map.entry("D7", 40), Map.entry("E8", 30), Map.entry("F9", 0),
                Map.entry("*", 0)
        );

        // Process candidate's grades
        Map<String, String> subjectGrades = candidate.getResultDetails().stream()
                .collect(Collectors.toMap(
                        result -> normalizeSubject.apply(result.getSubject()),
                        result -> result.getGrade().trim().toUpperCase(),
                        (grade1, grade2) -> {
                            int g1 = gradeScale.getOrDefault(grade1, 0);
                            int g2 = gradeScale.getOrDefault(grade2, 0);
                            return g1 >= g2 ? grade1 : grade2;
                        }
                ));

        // Get selected categories with programs
        List<Category> selectedCategories = categoryRepository.findAllById(userSelectedCategoryIds);
        if (selectedCategories.isEmpty()) {
            throw new IllegalArgumentException("No valid categories found for the provided IDs");
        }

        // Get programs in selected categories
        Set<Program> allProgramsInCategories = selectedCategories.stream()
                .flatMap(category -> programRepository.findByCategories_Id(category.getId()).stream())
                .collect(Collectors.toSet());

        // Initialize data structures
        Map<University, List<Program>> eligibleProgramsMap = new HashMap<>();
        Map<University, List<Program>> alternativeProgramsMap = new HashMap<>();
        Map<Program, List<String>> programExplanations = new HashMap<>();
        Map<Program, Double> percentageMap = new HashMap<>();
        Map<Program, Set<String>> programCategoriesMap = new HashMap<>();

        // Build program to categories mapping
        for (Program program : allProgramsInCategories) {
            Set<String> categoryNames = program.getCategories().stream()
                    .map(Category::getName)
                    .collect(Collectors.toSet());
            programCategoriesMap.put(program, categoryNames);
        }

        // Evaluate all programs in selected categories
        for (Program program : allProgramsInCategories) {
            University university = program.getUniversity();
            boolean eligible = true;
            int scoreDifference = 0;
            boolean failedCore = false;
            List<Integer> scores = new ArrayList<>();
            List<String> explanation = new ArrayList<>();

            for (Map.Entry<String, String> requirement : program.getCutoffPoints().entrySet()) {
                String subject = requirement.getKey();
                String requiredGrade = requirement.getValue().trim().toUpperCase();
                String userGrade = subjectGrades.get(subject);

                if (userGrade == null || !gradeScale.containsKey(userGrade)) {
                    explanation.add("Missing grade for: " + subject);
                    eligible = false;
                    break;
                }

                int userScore = gradeScale.get(userGrade);
                int requiredScore = gradeScale.get(requiredGrade);

                if (coreSubjects.contains(subject) && (userGrade.equals("F9") || userGrade.equals("*"))) {
                    failedCore = true;
                }

                scores.add(userScore);

                if (userScore < requiredScore) {
                    int diff = requiredScore - userScore;
                    scoreDifference += diff;
                    explanation.add(String.format("%s: Required %s (%d), Got %s (%d)",
                            subject, requiredGrade, requiredScore, userGrade, userScore));
                    eligible = false;
                }
            }

            double percentage = (failedCore || scores.isEmpty()) ? 0.0 :
                    Math.round(scores.stream().mapToInt(i -> i).average().orElse(0.0) * 100.0) / 100.0;

            percentageMap.put(program, percentage);

            if (eligible && !failedCore) {
                eligibleProgramsMap.computeIfAbsent(university, u -> new ArrayList<>()).add(program);
            } else if (!failedCore && scoreDifference <= 20) {
                alternativeProgramsMap.computeIfAbsent(university, u -> new ArrayList<>()).add(program);
                programExplanations.put(program, explanation);
            }
        }

        // Safely generate AI recommendations (won't break if service is unavailable)
        Map<Program, AIRecommendation> aiRecommendations = Collections.emptyMap();
        try {
            aiRecommendations = aiRecommendationService != null ?
                    aiRecommendationService.generateRecommendations(eligibleProgramsMap, alternativeProgramsMap, candidate) :
                    Collections.emptyMap();
        } catch (Exception e) {
            System.err.println("AI recommendation service failed: " + e.getMessage());
            aiRecommendations = Collections.emptyMap();
        }

        // Filter universities by type
        Set<University> filteredUniversities = new HashSet<>();
        filteredUniversities.addAll(eligibleProgramsMap.keySet());
        filteredUniversities.addAll(alternativeProgramsMap.keySet());

        if (universityType != null && !universityType.isBlank()) {
            filteredUniversities = filteredUniversities.stream()
                    .filter(u -> u.getType().name().equalsIgnoreCase(universityType.trim()))
                    .collect(Collectors.toSet());
        }

        // Create eligibility record with categories
        EligibilityRecord record = new EligibilityRecord();
        record.setId(UUID.randomUUID().toString());
        record.setUserId(userId);
        record.setCreatedAt(LocalDateTime.now(ZoneId.of("Africa/Accra")));
        record.setSelectedCategories(selectedCategories.stream()
                .map(Category::getName)
                .collect(Collectors.toList()));

        // Process universities and programs
        List<UniversityEligibility> universityEntities = new ArrayList<>();

        for (University university : filteredUniversities) {
            List<Program> universityPrograms = allProgramsInCategories.stream()
                    .filter(p -> p.getUniversity().equals(university))
                    .collect(Collectors.toList());

            if (universityPrograms.isEmpty()) {
                continue;
            }

            UniversityEligibility universityEligibility = new UniversityEligibility();
            universityEligibility.setUniversityName(university.getName());
            universityEligibility.setLocation(university.getLocation());
            universityEligibility.setType(university.getType().name());
            universityEligibility.setEligibilityRecord(record);

            // Process eligible programs with categories
            Map<Program, AIRecommendation> finalAiRecommendations1 = aiRecommendations;
            List<EligibleProgram> eligiblePrograms = eligibleProgramsMap.getOrDefault(university, List.of()).stream()
                    .map(program -> {
                        EligibleProgram ep = new EligibleProgram();
                        ep.setName(program.getName());
                        ep.setCutoffPoints(program.getCutoffPoints());
                        ep.setPercentage(percentageMap.get(program));
                        ep.setCategories(new ArrayList<>(programCategoriesMap.get(program)));

                        // Safe AI recommendation access
                        if (finalAiRecommendations1 != null) {
                            ep.setAiRecommendation(finalAiRecommendations1.get(program));
                        }

                        ep.setUniversityEligibility(universityEligibility);
                        return ep;
                    })
                    .collect(Collectors.toList());

            // Process alternative programs with categories
            Map<Program, AIRecommendation> finalAiRecommendations = aiRecommendations;
            List<AlternativeProgram> alternativePrograms = alternativeProgramsMap.getOrDefault(university, List.of()).stream()
                    .map(program -> {
                        AlternativeProgram ap = new AlternativeProgram();
                        ap.setName(program.getName());
                        ap.setCutoffPoints(program.getCutoffPoints());
                        ap.setPercentage(percentageMap.get(program));
                        ap.setExplanations(programExplanations.get(program));
                        ap.setCategories(new ArrayList<>(programCategoriesMap.get(program)));

                        // Safe AI recommendation access
                        if (finalAiRecommendations != null) {
                            ap.setAiRecommendation(finalAiRecommendations.get(program));
                        }

                        ap.setUniversityEligibility(universityEligibility);
                        return ap;
                    })
                    .collect(Collectors.toList());

            universityEligibility.setEligiblePrograms(eligiblePrograms);
            universityEligibility.setAlternativePrograms(alternativePrograms);
            universityEntities.add(universityEligibility);
        }

        record.setUniversities(universityEntities);
        return eligibilityRecordRepository.save(record);
    }
















    }

