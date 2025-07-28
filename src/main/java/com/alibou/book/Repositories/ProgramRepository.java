package com.alibou.book.Repositories;

import com.alibou.book.Entity.Category;
import com.alibou.book.Entity.Program;
import com.alibou.book.Entity.University;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Long> {
    List<Program> findByUniversity(University university);
    // For Option 1 or 3
    // Correct method for finding programs by category name
    @Query("SELECT p FROM Program p JOIN p.categories c WHERE c.name = :categoryName")
    List<Program> findByCategoryName(@Param("categoryName") String categoryName);

    // Or if you want to find by Category entity
    List<Program> findByCategories(Category category);

    // Or if you want to find by category ID
    List<Program> findByCategories_Id(Long categoryId);

    // For Option 2
//    List<Program> findByCategories_Name(String categoryName);
}