package com.alibou.book.Services;

import com.alibou.book.DTO.AlternativeProgramDTO;
import com.alibou.book.DTO.EligibleProgramDTO;
import com.alibou.book.DTO.UniversityEligibilityDTO;
import com.alibou.book.Entity.Program;
import com.alibou.book.Entity.University;
import com.alibou.book.Entity.WaecResult;
import com.alibou.book.Repositories.ProgramRepository;
import com.alibou.book.Repositories.WaecResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EligibilityCheckerService {

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private WaecResultRepository waecResultRepository;

//    private final Map<String, Integer> gradeScale = Map.of(
//            "A1", 1, "B2", 2, "B3", 3, "C4", 4, "C5", 5,
//            "C6", 6, "D7", 7, "E8", 8, "F9", 9
//    );
public List<UniversityEligibilityDTO> checkEligibility(String indexNumber, String typeFilter) {
    List<WaecResult> results = waecResultRepository.findByIndexNumber(indexNumber);
    if (results.isEmpty()) throw new RuntimeException("No WAEC result found");

    WaecResult result = results.stream()
            .max(Comparator.comparing(WaecResult::getCreatedAt))
            .orElseThrow();

    Map<String, String> grades = result.getSubjectsGrades();

    Map<String, Integer> gradeScale = Map.ofEntries(
            Map.entry("A1", 1), Map.entry("B2", 2), Map.entry("B3", 3),
            Map.entry("C4", 4), Map.entry("C5", 5), Map.entry("C6", 6),
            Map.entry("D7", 7), Map.entry("E8", 8), Map.entry("F9", 9)
    );

    Map<University, List<Program>> eligibleProgramsMap = new HashMap<>();
    Map<University, List<Program>> alternativeProgramsMap = new HashMap<>();
    Map<Program, List<String>> programExplanations = new HashMap<>();

    System.out.println("\n==============================");
    System.out.println("📌 Checking eligibility for index number: " + indexNumber);
    System.out.println("📘 Type filter: " + (typeFilter != null ? typeFilter : "NONE"));
    System.out.println("📋 User's WAEC Grades: " + grades);
    System.out.println("==============================");

    for (Program program : programRepository.findAll()) {
        boolean eligible = true;
        int scoreDifference = 0;
        List<String> explanation = new ArrayList<>();

        University university = program.getUniversity();

        System.out.println("\n➡️ Program: " + program.getName() + " | University: " + university.getName());

        if (typeFilter != null && !university.getType().name().equalsIgnoreCase(typeFilter.trim())) {
            System.out.println("⏭️ Skipped (type mismatch): " + university.getType());
            continue;
        }

        System.out.println("🎯 Required Subjects & Grades: " + program.getCutoffPoints());

        for (Map.Entry<String, String> requirement : program.getCutoffPoints().entrySet()) {
            String subject = requirement.getKey();
            String requiredGrade = requirement.getValue().trim().toUpperCase();
            String userGrade = grades.get(subject);

            if (userGrade != null) userGrade = userGrade.trim().toUpperCase();

            System.out.printf("  - 📚 Subject: %-15s Required: %-3s | User: %-3s%n",
                    subject, requiredGrade, userGrade);

            if (userGrade == null) {
                explanation.add("Subject: " + subject + " — Missing from WAEC result.");
                System.out.println("    ❌ Missing subject: " + subject);
                eligible = false;
                break;
            }

            if (!gradeScale.containsKey(userGrade) || !gradeScale.containsKey(requiredGrade)) {
                explanation.add("Subject: " + subject + " — Invalid grade mapping.");
                System.out.println("    ❌ Invalid grade (not in scale)");
                eligible = false;
                break;
            }

            int userScore = gradeScale.get(userGrade);
            int requiredScore = gradeScale.get(requiredGrade);

            if (userScore > requiredScore) {
                int diff = userScore - requiredScore;
                scoreDifference += diff;
                String techExplanation = String.format(
                        "Subject: %s — Required: %s (%d), User: %s (%d), Δ = +%d",
                        subject, requiredGrade, requiredScore, userGrade, userScore, diff
                );
                explanation.add(techExplanation);
                System.out.println("    ❌ " + techExplanation);
                eligible = false;
            } else {
                System.out.println("    ✅ Passed");
            }
        }

        if (eligible) {
            System.out.println("✅✅ Eligible for: " + program.getName());
            eligibleProgramsMap.computeIfAbsent(university, u -> new ArrayList<>()).add(program);
        } else if (scoreDifference > 0 && scoreDifference <= 2) {
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

    List<UniversityEligibilityDTO> response = new ArrayList<>();

    for (University university : allUniversities) {
        List<Program> eligible = eligibleProgramsMap.getOrDefault(university, List.of());
        List<Program> alternative = alternativeProgramsMap.getOrDefault(university, List.of());

        List<EligibleProgramDTO> eligibleDTOs = eligible.stream()
                .map(p -> new EligibleProgramDTO(p.getName(), p.getCutoffPoints()))
                .collect(Collectors.toList());

        List<AlternativeProgramDTO> alternativeDTOs = alternative.stream()
                .map(p -> new AlternativeProgramDTO(
                        p.getName(),
                        p.getCutoffPoints(),
                        programExplanations.getOrDefault(p, List.of())
                )).collect(Collectors.toList());

        response.add(new UniversityEligibilityDTO(
                university.getName(),
                university.getLocation(),
                university.getType().name(),
                eligibleDTOs,
                alternativeDTOs
        ));
    }

    System.out.println("\n🎉 Final Result: " + response.size() + " university(ies) with eligible or alternative programs found.");
    return response;
}

}
