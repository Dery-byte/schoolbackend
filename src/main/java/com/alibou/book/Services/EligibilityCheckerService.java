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

        Map<University, List<Program>> eligibleProgramsMap = new HashMap<>();
        Map<University, List<Program>> alternativeProgramsMap = new HashMap<>();
        Map<Program, List<String>> programExplanations = new HashMap<>();

        Set<String> coreSubjects = Set.of("ENGLISH LANG", "MATHEMATICS(CORE)", "SOCIAL STUDIES", "INTEGRATED SCIENCE");

        Map<String, Integer> gradeScale = Map.ofEntries(
                Map.entry("A1", 100), Map.entry("B2", 90), Map.entry("B3", 80),
                Map.entry("C4", 70), Map.entry("C5", 60), Map.entry("C6", 50),
                Map.entry("D7", 40), Map.entry("E8", 30), Map.entry("F9", 0), Map.entry("*", 0)
        );

        List<Program> allPrograms = programRepository.findAll();
        System.out.println("🎓 Total Programs: " + allPrograms.size());

        for (Program program : allPrograms) {
            University university = program.getUniversity();
            System.out.println("\n➡️ Checking program: " + program.getName() + " at " + university.getName());

            boolean eligible = true;
            int scoreDifference = 0;
            List<String> explanation = new ArrayList<>();

            for (Map.Entry<String, String> requirement : program.getCutoffPoints().entrySet()) {
                String subject = requirement.getKey();
                String requiredGrade = requirement.getValue().trim().toUpperCase();
                String userGrade = subjectGrades.get(subject);

                System.out.printf("   🔎 Subject: %-20s Required: %-3s | User: %-3s%n", subject, requiredGrade, userGrade);

                if (userGrade == null) {
                    explanation.add("Missing subject: " + subject);
                    System.out.println("   ❌ Subject not found in user's grades.");
                    eligible = false;
                    break;
                }

                if (!gradeScale.containsKey(userGrade) || !gradeScale.containsKey(requiredGrade)) {
                    explanation.add("Invalid grade for subject: " + subject);
                    System.out.println("   ❌ Invalid grade (not in scale).");
                    eligible = false;
                    break;
                }

                int userScore = gradeScale.get(userGrade);
                int requiredScore = gradeScale.get(requiredGrade);

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

            // Check core subject rule (auto disqualify for F9 or * in core)
            boolean hasFailedCore = coreSubjects.stream().anyMatch(subject ->
                    subjectGrades.containsKey(subject) &&
                            (subjectGrades.get(subject).equals("F9") || subjectGrades.get(subject).equals("*"))
            );

            if (eligible && !hasFailedCore) {
                System.out.println("✅ Fully eligible for: " + program.getName());
                eligibleProgramsMap.computeIfAbsent(university, u -> new ArrayList<>()).add(program);
            } else if (!hasFailedCore && scoreDifference <= 20) {
                System.out.println("⚠️ Alternative match for: " + program.getName());
                alternativeProgramsMap.computeIfAbsent(university, u -> new ArrayList<>()).add(program);
                programExplanations.put(program, explanation);
            } else {
                System.out.println("🚫 Not eligible for: " + program.getName());
            }
        }

        Set<University> allUniversities = new HashSet<>();
        allUniversities.addAll(eligibleProgramsMap.keySet());
        allUniversities.addAll(alternativeProgramsMap.keySet());

        List<UniversityEligibilityDTO> result = new ArrayList<>();

        for (University university : allUniversities) {
            List<EligibleProgramDTO> eligibleDTOs = eligibleProgramsMap.getOrDefault(university, List.of()).stream()
                    .map(program -> {
                        double percentage = calculateEligibilityPercentage(subjectGrades, coreSubjects, gradeScale);
                        return new EligibleProgramDTO(program.getName(), program.getCutoffPoints(), percentage);
                    }).collect(Collectors.toList());

            List<AlternativeProgramDTO> alternativeDTOs = alternativeProgramsMap.getOrDefault(university, List.of()).stream()
                    .map(program -> {
                        double percentage = calculateEligibilityPercentage(subjectGrades, coreSubjects, gradeScale);
                        return new AlternativeProgramDTO(
                                program.getName(),
                                program.getCutoffPoints(),
                                programExplanations.getOrDefault(program, List.of()),
                                percentage
                        );
                    }).collect(Collectors.toList());

            result.add(new UniversityEligibilityDTO(
                    university.getName(),
                    university.getLocation(),
                    university.getType().name(),
                    eligibleDTOs,
                    alternativeDTOs
            ));
        }

        System.out.println("\n🎯 Eligibility check complete. Universities found: " + result.size());
        return result;
    }

    private double calculateEligibilityPercentage(Map<String, String> subjectGrades, Set<String> coreSubjects, Map<String, Integer> gradeScale) {
        for (String core : coreSubjects) {
            String grade = subjectGrades.getOrDefault(core, "");
            if (grade.equalsIgnoreCase("F9") || grade.equals("*")) {
                return 0.0;
            }
        }

        List<Integer> scores = subjectGrades.values().stream()
                .map(g -> gradeScale.getOrDefault(g.toUpperCase(), 0))
                .sorted(Comparator.reverseOrder())
                .limit(6)
                .collect(Collectors.toList());

        double total = scores.stream().mapToDouble(i -> i).sum();
        return scores.isEmpty() ? 0.0 : Math.round((total / 6.0) * 100.0) / 100.0;
    }



}
