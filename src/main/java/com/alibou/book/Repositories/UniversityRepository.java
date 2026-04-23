package com.alibou.book.Repositories;

import com.alibou.book.Entity.University;
import com.alibou.book.Entity.UniversityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UniversityRepository extends JpaRepository<University, Long> {
    Optional<University> findByName(String name);
    List<University> findByType(UniversityType type); // Filter by type
}