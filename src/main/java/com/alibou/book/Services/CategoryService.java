package com.alibou.book.Services;

import com.alibou.book.DTO.CategoryRequest;
import com.alibou.book.Entity.Category;
import com.alibou.book.Repositories.CategoryRepository;
import com.alibou.book.exception.DuplicateResourceException;
import com.alibou.book.exception.RequestValidationException;
import com.alibou.book.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;



    public Category createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category with name [%s] already exists".formatted(request.getName()));
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return categoryRepository.save(category);
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category with id [%s] not found".formatted(id)));
    }

    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category with id [%s] not found".formatted(id));
        }
        categoryRepository.deleteById(id);
    }

    public void updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category with id [%s] not found".formatted(id)));

        if (category.getName().equals(request.getName())) {
            throw new RequestValidationException("No changes detected");
        }

        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category name already taken");
        }

        category.setName(request.getName());
        categoryRepository.save(category);
    }





}