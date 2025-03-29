package com.alibou.book.Repositories;

import com.alibou.book.Entity.Program;
import com.alibou.book.Entity.University;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Long> {
    List<Program> findByUniversity(University university);
}