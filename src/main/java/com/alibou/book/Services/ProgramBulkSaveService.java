package com.alibou.book.Services;

import com.alibou.book.DTO.ProgramRequestDTO;
import com.alibou.book.Entity.Category;
import com.alibou.book.Entity.Program;
import com.alibou.book.Entity.SubjectRequirement;
import com.alibou.book.Entity.University;
import com.alibou.book.Repositories.CategoryRepository;
import com.alibou.book.Repositories.ProgramRepository;
import com.alibou.book.Repositories.UniversityRepository;
import com.alibou.book.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProgramBulkSaveService {

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UniversityRepository universityRepository;

    /**
     * Builds and saves one program entirely inside its own REQUIRES_NEW transaction.
     *
     * Accepting universityId (not the entity) and the raw DTO avoids passing
     * detached JPA entities across session boundaries, which caused alternativeSubjects
     * to be silently dropped when Hibernate initialised its PersistentMap wrappers
     * in the new session.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Program saveSingleProgram(Long universityId,
                                     ProgramRequestDTO.ProgramWithCategoriesDTO dto) {

        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "University not found with id: " + universityId));

        Program program = new Program();
        program.setName(dto.getName().trim());
        program.setUniversity(university);

        // ── Core subjects ────────────────────────────────────────────────────
        program.setCoreSubjects(normalizeKeys(dto.getCoreSubjects()));

        // ── Alternative subjects ─────────────────────────────────────────────
        program.setAlternativeSubjects(normalizeKeys(dto.getAlternativeSubjects()));

        // ── Alternative groups (AND/OR logic) ───────────────────────────────
        if (dto.getAlternativeGroups() != null && !dto.getAlternativeGroups().isEmpty()) {
            List<SubjectRequirement> groups = dto.getAlternativeGroups().stream()
                    .map(g -> {
                        SubjectRequirement req = new SubjectRequirement();
                        req.setRequiredGrade(g.getRequiredGrade());
                        req.setAnyOf(g.isAnyOf());
                        req.setSubjects(g.getSubjects() != null ? g.getSubjects() : List.of());
                        return req;
                    }).toList();
            program.setAlternativeGroups(groups);
        }

        // ── Categories ───────────────────────────────────────────────────────
        if (dto.getCategoryIds() != null && !dto.getCategoryIds().isEmpty()) {
            Set<Long> categoryIds = dto.getCategoryIds().stream()
                    .map(c -> c.getId())
                    .collect(Collectors.toSet());
            List<Category> cats = categoryRepository.findAllById(categoryIds);
            program.setCategories(new HashSet<>(cats));
        }

        return programRepository.save(program);
    }

    private Map<String, String> normalizeKeys(Map<String, String> subjects) {
        if (subjects == null) return new HashMap<>();
        Map<String, String> out = new LinkedHashMap<>();
        subjects.forEach((k, v) -> out.put(k.trim().toUpperCase(), v));
        return out;
    }
}
