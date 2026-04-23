package com.alibou.book.Controllers;
import com.alibou.book.DTO.CategoryRequest;
import com.alibou.book.DTO.UpdateCategoryDTO;
import com.alibou.book.Entity.Category;
import com.alibou.book.Entity.University;
import com.alibou.book.Repositories.CategoryRepository;
import com.alibou.book.Services.CategoryService;
import com.alibou.book.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/auth/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;
    private final CategoryRepository categoryRepository;

    @PostMapping("/add")
    public ResponseEntity<Category> createCategory(@RequestBody CategoryRequest request) {
        Category category = categoryService.createCategory(request);
        return ResponseEntity.created(URI.create("/api/v1/categories/" + category.getId()))
                .body(category);
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/getCategoryById/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

//    @PutMapping("/upateCategoryByid/{id}")
//    public ResponseEntity<Category> updateCategory(
//            @PathVariable Long id,
//            @RequestBody CategoryRequest request
//    ) {
//        categoryService.updateCategory(id, request);
//        return ResponseEntity.noContent().build();
//    }

    @DeleteMapping("/deleteCategoryById/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }



//    @PutMapping("/upateCategorys")
//    public ResponseEntity<Category> updateCategory(@RequestBody CategoryRequest request
//    ) {
//        categoryService.updateCategory(id, request);
//        return ResponseEntity.noContent().build();
//    }



    @PutMapping("/upateCategory")
    @Transactional
    public ResponseEntity<Category> updateUniversity(
            @Valid @RequestBody UpdateCategoryDTO updateDTO) {
        Category category = categoryRepository.findById(updateDTO.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + updateDTO.getId()));

        // Update fields
        category.setName(updateDTO.getName());
        category.setDescription(updateDTO.getDescription());

        Category updatedCategory = categoryRepository.save(category);
        return ResponseEntity.ok(updatedCategory);
    }
}