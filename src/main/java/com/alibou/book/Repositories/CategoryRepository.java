package com.alibou.book.Repositories;

import com.alibou.book.Entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    boolean existsByName(String name);

    @Query("SELECT c.name FROM Category c WHERE c.id IN :ids")
    List<String> findNamesByIds(@Param("ids") List<Long> ids);
}