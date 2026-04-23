package com.alibou.book.Repositories;

import com.alibou.book.Entity.WaecResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WaecResultRepository extends JpaRepository<WaecResult, Long> {
    List<WaecResult> findByIndexNumber(String indexNumber);
}