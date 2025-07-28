package com.alibou.book.Services;

import com.alibou.book.DTO.UpdateProgramDTO;
import com.alibou.book.Entity.Category;
import com.alibou.book.Entity.Program;
import com.alibou.book.Entity.University;
import com.alibou.book.Repositories.CategoryRepository;
import com.alibou.book.Repositories.ProgramRepository;
import com.alibou.book.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import com.alibou.book.DTO.CategoryIdDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProgramService {

    private final ProgramRepository programRepository;
    private final CategoryRepository categoryRepository;

    public ProgramService(ProgramRepository programRepository, CategoryRepository categoryRepository) {
        this.programRepository = programRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<Program> getProgramsByCategoryName(String categoryName) {
        return programRepository.findByCategoryName(categoryName);
    }

    // Or using the entity version
    public List<Program> getProgramsByCategory(Category category) {
        return programRepository.findByCategories(category);
    }

    // Or using ID version
    public List<Program> getProgramsByCategoryId(Long categoryId) {
        return programRepository.findByCategories_Id(categoryId);
    }





    @Transactional
    public void deleteProgram(Long programId) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));
        // Clear both sides of the relationship
        new ArrayList<>(program.getCategories()).forEach(category ->
                category.getPrograms().remove(program));
        programRepository.delete(program);
    }






    @Transactional
    public Program updateProgram(UpdateProgramDTO updateDTO) {
        Program program = programRepository.findById(updateDTO.getProgramId())
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));
        program.setName(updateDTO.getName());
        program.setCutoffPoints(updateDTO.getCutoffPoints());
        if (updateDTO.getCategoryIds() != null) {
            Set<Category> categories = categoryRepository.findAllById(
                    updateDTO.getCategoryIds().stream()
                            .map(CategoryIdDTO::getId)
                            .collect(Collectors.toSet())
            ).stream().collect(Collectors.toSet());
            program.setCategories(categories);
        }
        return programRepository.save(program);
    }






    public Program getProgramById(Long id) {
        return programRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Program not found"));
    }
}
