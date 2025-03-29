package com.alibou.book.Repositories;

import com.alibou.book.Entity.WaecCandidateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WaecCandidateRepository extends JpaRepository<WaecCandidateEntity, Long> {

    Optional<WaecCandidateEntity> findByCindexAndExamyearAndExamtype(String cindex, String examyear, String examtype);
}
