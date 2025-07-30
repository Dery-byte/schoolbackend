package com.alibou.book.Repositories;

import com.alibou.book.Entity.Biodata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BiodataRepository extends JpaRepository<Biodata, Integer> {
    // You can add custom query methods here if needed
    Optional<Biodata> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<Biodata> findByRecordId(String recordId);  // Add this method

}