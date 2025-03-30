package com.alibou.book.Services;

import com.alibou.book.DTO.AlternativeProgramDTO;
import com.alibou.book.DTO.Candidate;
import com.alibou.book.DTO.CandidateSearchRequest;
import com.alibou.book.DTO.EligibleProgramDTO;
import com.alibou.book.DTO.UniversityEligibilityDTO;
import com.alibou.book.DTO.WaecResponse;
import com.alibou.book.DTO.WaecResultsRequest;
import com.alibou.book.Entity.Program;
import com.alibou.book.Entity.University;
import com.alibou.book.Entity.WaecCandidateEntity;
import com.alibou.book.Entity.WaecResultDetailEntity;
import com.alibou.book.Repositories.ProgramRepository;
import com.alibou.book.Repositories.WaecCandidateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;
@Service
public class WaecApiService {

    private final RestTemplate waecApiRestTemplate;
    private final ObjectMapper objectMapper;
    private final WaecCandidateRepository waecCandidateRepository;
    private final ProgramRepository programRepository;

    @Value("${waec.api.url}")
    private String apiUrl;

    public WaecApiService(
            RestTemplate waecApiRestTemplate,
            ObjectMapper objectMapper,
            WaecCandidateRepository waecCandidateRepository,
            ProgramRepository programRepository) {
        this.waecApiRestTemplate = waecApiRestTemplate;
        this.objectMapper = objectMapper;
        this.waecCandidateRepository = waecCandidateRepository;
        this.programRepository = programRepository;
    }

    public ResponseEntity<?> verifyResult(WaecResultsRequest request) {
        Optional<WaecCandidateEntity> existing = waecCandidateRepository.findByCindexAndExamyearAndExamtype(
                request.getCindex(), request.getExamyear(), String.valueOf(request.getExamtype())
        );
        if (existing.isPresent()) {
            System.out.println("✅ Result found in database. Skipping WAEC API call.");
            return ResponseEntity.ok(existing.get());
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

                Candidate c = waecResponse.getCandidate();
                WaecCandidateEntity candidateEntity = new WaecCandidateEntity();
                candidateEntity.setCindex(c.getCindex());
                candidateEntity.setCname(c.getCname());
                candidateEntity.setDob(c.getDob());
                candidateEntity.setGender(c.getGender());
                candidateEntity.setExamtype(c.getExamtype());
                candidateEntity.setExamyear(request.getExamyear());

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
                System.out.println("✅ WAEC result saved successfully.");
            }

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

    public ResponseEntity<WaecCandidateEntity> getCandidateWithResultsFromDb(@RequestBody CandidateSearchRequest request) {
        return waecCandidateRepository.findByCindexAndExamyearAndExamtype(
                request.getCindex(), request.getExamyear(), request.getExamtype()
        ).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    private final Map<String, Integer> gradeScale = Map.ofEntries(
            Map.entry("A1", 1), Map.entry("B2", 2), Map.entry("B3", 3),
            Map.entry("C4", 4), Map.entry("C5", 5), Map.entry("C6", 6),
            Map.entry("D7", 7), Map.entry("E8", 8), Map.entry("F9", 9)
    );








    private final Set<String> coreSubjects = Set.of(
            "ENGLISH LANG", "MATHEMATICS(CORE)", "SOCIAL STUDIES", "INTEGRATED SCIENCE"
    );

    public List<UniversityEligibilityDTO> checkEligibility(WaecCandidateEntity candidate) {
        System.out.println("\n🔍 Checking eligibility for: " + candidate.getCname() + " (Index: " + candidate.getCindex() + ")");

        Map<String, String> subjectGrades = candidate.getResultDetails().stream()
                .collect(Collectors.toMap(
                        WaecResultDetailEntity::getSubject,
                        r -> r.getGrade().trim().toUpperCase()
                ));

        System.out.println("📘 Extracted Grades: " + subjectGrades);

        Set<String> coreSubjects = Set.of("ENGLISH LANG", "MATHEMATICS(CORE)", "SOCIAL STUDIES", "INTEGRATED SCIENCE");

        Map<String, Integer> gradeScale = Map.ofEntries(
                Map.entry("A1", 100), Map.entry("B2", 90), Map.entry("B3", 80),
                Map.entry("C4", 70), Map.entry("C5", 60), Map.entry("C6", 50),
                Map.entry("D7", 40), Map.entry("E8", 30), Map.entry("F9", 0), Map.entry("*", 0)
        );

        Map<University, List<Program>> eligibleProgramsMap = new HashMap<>();
        Map<University, List<Program>> alternativeProgramsMap = new HashMap<>();
        Map<Program, List<String>> programExplanations = new HashMap<>();
        Map<Program, Double> percentageMap = new HashMap<>();

        for (Program program : programRepository.findAll()) {
            University university = program.getUniversity();
            System.out.println("\n➡️ Checking program: " + program.getName() + " at " + university.getName());

            boolean eligible = true;
            int scoreDifference = 0;
            boolean failedCore = false;
            List<Integer> scores = new ArrayList<>();
            List<String> explanation = new ArrayList<>();

            for (Map.Entry<String, String> requirement : program.getCutoffPoints().entrySet()) {
                String subject = requirement.getKey();
                String requiredGrade = requirement.getValue().trim().toUpperCase();
                String userGrade = subjectGrades.get(subject);

                System.out.printf("   🔎 Subject: %-20s Required: %-3s | User: %-3s%n", subject, requiredGrade, userGrade);

                if (userGrade == null || !gradeScale.containsKey(userGrade) || !gradeScale.containsKey(requiredGrade)) {
                    explanation.add("Invalid or missing grade for subject: " + subject);
                    System.out.println("   ❌ Invalid or missing grade");
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
                    explanation.add(String.format("Subject: %s - Required: %s (%d), Got: %s (%d), Diff: -%d",
                            subject, requiredGrade, requiredScore, userGrade, userScore, diff));
                    System.out.println("   ❌ Score too low. Diff: -" + diff);
                    eligible = false;
                } else {
                    System.out.println("   ✅ Passed");
                }
            }

            double percentage = (failedCore || scores.isEmpty())
                    ? 0.0
                    : Math.round(scores.stream().mapToInt(i -> i).average().orElse(0.0) * 100.0) / 100.0;
            percentageMap.put(program, percentage);

            if (eligible && !failedCore) {
                System.out.println("✅ Fully eligible for: " + program.getName() + " (" + percentage + "%)");
                eligibleProgramsMap.computeIfAbsent(university, u -> new ArrayList<>()).add(program);
            } else if (!failedCore && scoreDifference <= 20) {
                System.out.println("⚠️ Alternative match for: " + program.getName() + " (" + percentage + "%)");
                alternativeProgramsMap.computeIfAbsent(university, u -> new ArrayList<>()).add(program);
                programExplanations.put(program, explanation);
            } else {
                System.out.println("🚫 Not eligible for: " + program.getName());
            }
        }

        Set<University> allUniversities = new HashSet<>();
        allUniversities.addAll(eligibleProgramsMap.keySet());
        allUniversities.addAll(alternativeProgramsMap.keySet());

        List<UniversityEligibilityDTO> response = new ArrayList<>();

        for (University university : allUniversities) {
            List<EligibleProgramDTO> eligibleDTOs = eligibleProgramsMap.getOrDefault(university, List.of()).stream()
                    .map(p -> new EligibleProgramDTO(p.getName(), p.getCutoffPoints(), percentageMap.getOrDefault(p, 0.0)))
                    .collect(Collectors.toList());

            List<AlternativeProgramDTO> alternativeDTOs = alternativeProgramsMap.getOrDefault(university, List.of()).stream()
                    .map(p -> new AlternativeProgramDTO(
                            p.getName(),
                            p.getCutoffPoints(),
                            programExplanations.getOrDefault(p, List.of()),
                            percentageMap.getOrDefault(p, 0.0)
                    ))
                    .collect(Collectors.toList());

            response.add(new UniversityEligibilityDTO(
                    university.getName(),
                    university.getLocation(),
                    university.getType().name(),
                    eligibleDTOs,
                    alternativeDTOs
            ));
        }

        System.out.println("\n🎯 Eligibility check complete. Universities found: " + response.size());
        return response;
    }




}
