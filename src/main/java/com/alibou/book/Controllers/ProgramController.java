package com.alibou.book.Controllers;

import com.alibou.book.DTO.DeleteProgramRequest;
import com.alibou.book.DTO.ProgramRequestDTO;
import com.alibou.book.DTO.UpdateProgramDTO;
import com.alibou.book.Entity.Category;
import com.alibou.book.Entity.Program;
import com.alibou.book.Entity.University;
import com.alibou.book.Repositories.CategoryRepository;
import com.alibou.book.Repositories.ProgramRepository;
import com.alibou.book.Services.ProgramService;
import com.alibou.book.Services.UniversityService;
import com.alibou.book.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import com.alibou.book.DTO.CategoryIdDTO;


@RestController
@RequestMapping("/auth/programs")
public class ProgramController {


    @Autowired
    private UniversityService universityService;
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private  ProgramService programService;

    @Autowired
    private ProgramRepository programRepository;





//
//    @PostMapping("/{universityId}/add")
//    public ResponseEntity<List<Program>> addProgramToUniversity(
//            @PathVariable Long universityId,
//            @RequestBody List<Program> programs) {
//        University university = universityService.getUniversityById(universityId);
//        for (Program program : programs) {
//            program.setUniversity(university);
//        }
//        List<Program> savedPrograms = programRepository.saveAll(programs);
//        return ResponseEntity.ok(savedPrograms);
//    }



//    @PostMapping("/addProgram")
//    public ResponseEntity<List<Program>> addProgramToUniversity(
//            @RequestBody ProgramRequestDTO requestDTO) {
//
//        University university = universityService.getUniversityById(requestDTO.getUniversityId());
//        List<Program> programs = requestDTO.getPrograms();
//
//        for (Program program : programs) {
//            program.setUniversity(university);
//        }
//
//        List<Program> savedPrograms = programRepository.saveAll(programs);
//        return ResponseEntity.ok(savedPrograms);
//    }




    @PostMapping("/addProgram")
    @Transactional
    public ResponseEntity<List<Program>> addProgramToUniversity(@RequestBody ProgramRequestDTO requestDTO) {
        University university = universityService.getUniversityById(requestDTO.getUniversityId());
        List<Program> savedPrograms = new ArrayList<>();
        for (ProgramRequestDTO.ProgramWithCategoriesDTO programDTO : requestDTO.getPrograms()) {
            Program program = new Program();
            program.setName(programDTO.getName());
            program.setUniversity(university);
            // ✅ Set core and alternative subjects with grades
            program.setCoreSubjects(programDTO.getCoreSubjects());
            program.setAlternativeSubjects(programDTO.getAlternativeSubjects());
            // ✅ Optional: cutoff points if applicable
            program.setCutoffPoints(programDTO.getCutoffPoints());
            // ✅ Handle categories for each program
            if (programDTO.getCategoryIds() != null && !programDTO.getCategoryIds().isEmpty()) {
                Set<Long> categoryIds = programDTO.getCategoryIds().stream()
                        .map(CategoryIdDTO::getId)
                        .collect(Collectors.toSet());
                List<Category> categoryList = categoryRepository.findAllById(categoryIds);
                Set<Category> categories = new HashSet<>(categoryList);
                program.setCategories(categories);
            }
            savedPrograms.add(programRepository.save(program));
        }
        return ResponseEntity.ok(savedPrograms);
    }

//    public ResponseEntity<List<Program>> addProgramToUniversity(
//            @RequestBody ProgramRequestDTO requestDTO) {
//
//        University university = universityService.getUniversityById(requestDTO.getUniversityId());
//        List<Program> savedPrograms = new ArrayList<>();
//
//        for (ProgramRequestDTO.ProgramWithCategoriesDTO programDTO : requestDTO.getPrograms()) {
//            Program program = new Program();
//            program.setName(programDTO.getName());
//            program.setCutoffPoints(programDTO.getCutoffPoints());
//            program.setUniversity(university);
//
//            // Handle categories for each program
//            if (programDTO.getCategoryIds() != null && !programDTO.getCategoryIds().isEmpty()) {
//                Set<Long> categoryIds = programDTO.getCategoryIds().stream()
//                        .map(CategoryIdDTO::getId)
//                        .collect(Collectors.toSet());
//                // Convert the List from findAllById to a Set
//                List<Category> categoryList = categoryRepository.findAllById(categoryIds);
//                Set<Category> categories = new HashSet<>(categoryList);
//                program.setCategories(categories);
//            }
//
//            savedPrograms.add(programRepository.save(program));
//        }
//
//        return ResponseEntity.ok(savedPrograms);
//    }









    @GetMapping("/university/{universityId}")
    public ResponseEntity<List<Program>> getProgramsByUniversity(@PathVariable Long universityId) {
        University university = universityService.getUniversityById(universityId);
        return ResponseEntity.ok(programRepository.findByUniversity(university));
    }



    @GetMapping("/by-category-name/{categoryName}")
    public ResponseEntity<List<Program>> getProgramsByCategoryName(
            @PathVariable String categoryName
    ) {
        return ResponseEntity.ok(programService.getProgramsByCategoryName(categoryName));
    }

    @GetMapping("/by-category/{categoryId}")
    public ResponseEntity<List<Program>> getProgramsByCategoryId(
            @PathVariable Long categoryId
    ) {
        return ResponseEntity.ok(programService.getProgramsByCategoryId(categoryId));
    }


    @DeleteMapping("/deleteById")
    public ResponseEntity<?> deleteProgram(@RequestBody DeleteProgramRequest request) {
        try {
            programService.deleteProgram(request.getProgramId());
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.notFound().build();
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body("Error deleting program: " + ex.getMessage());
        }
    }





    @PutMapping("/updateProgram")
    @Transactional
    public ResponseEntity<Program> updateProgram(@Valid @RequestBody UpdateProgramDTO updateDTO) {
        Program program = programRepository.findById(updateDTO.getProgramId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Program not found with id: " + updateDTO.getProgramId()));
        // Update basic and map-based fields
        Optional.ofNullable(updateDTO.getName()).ifPresent(program::setName);
        Optional.ofNullable(updateDTO.getCutoffPoints()).ifPresent(program::setCutoffPoints);
        Optional.ofNullable(updateDTO.getCoreSubjects()).ifPresent(program::setCoreSubjects);
        Optional.ofNullable(updateDTO.getAlternativeSubjects()).ifPresent(program::setAlternativeSubjects);
        // Handle categories
        updateCategories(program, updateDTO.getCategoryIds());
        Program updatedProgram = programRepository.save(program);
        return ResponseEntity.ok(updatedProgram);
    }

    private void updateCategories(Program program, Collection<CategoryIdDTO> categoryIds) {
        if (categoryIds != null && !categoryIds.isEmpty()) {
            Set<Long> ids = categoryIds.stream()
                    .map(CategoryIdDTO::getId)
                    .collect(Collectors.toSet());
            List<Category> categories = categoryRepository.findAllById(ids);
            program.setCategories(new HashSet<>(categories));
        }
    }


//    public ResponseEntity<Program> updateProgram(
//            @Valid @RequestBody UpdateProgramDTO updateDTO) {
//        Program program = programRepository.findById(updateDTO.getProgramId())
//                .orElseThrow(() -> new ResourceNotFoundException(
//                        "Program not found with id: " + updateDTO.getProgramId()));
//        // Update basic fields
//        if (updateDTO.getName() != null) {
//            program.setName(updateDTO.getName());
//        }
//        if (updateDTO.getCutoffPoints() != null) {
//            program.setCutoffPoints(updateDTO.getCutoffPoints());
//        }
//        // Handle categories update
//        if (updateDTO.getCategoryIds() != null) {
//            Set<Long> categoryIds = updateDTO.getCategoryIds().stream()
//                    .map(CategoryIdDTO::getId)
//                    .collect(Collectors.toSet());
//            List<Category> categoryList = categoryRepository.findAllById(categoryIds);
//            program.setCategories(new HashSet<>(categoryList));
//        }
//        Program updatedProgram = programRepository.save(program);
//        return ResponseEntity.ok(updatedProgram);
//    }



    @GetMapping("/getProgramById/{id}")
    public ResponseEntity<Program> getUniversityById(@PathVariable Long id) {
        return ResponseEntity.ok(programService.getProgramById(id));
    }


}
