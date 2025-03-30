package com.alibou.book.Services;

import com.alibou.book.DTO.AlternativeProgramDTO;
import com.alibou.book.DTO.CandidateSearchRequest;
import com.alibou.book.DTO.EligibleProgramDTO;
import com.alibou.book.DTO.UniversityEligibilityDTO;
import com.alibou.book.Entity.Program;
import com.alibou.book.Entity.University;
import com.alibou.book.Entity.WaecCandidateEntity;
import com.alibou.book.Entity.WaecResultDetailEntity;
import com.alibou.book.Repositories.ProgramRepository;
import com.alibou.book.Repositories.WaecCandidateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EligibilityCheckerService {

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private WaecCandidateRepository waecCandidateRepository;

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
