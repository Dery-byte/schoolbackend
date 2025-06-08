package com.alibou.book.Repositories;


import com.alibou.book.Entity.ExamCheckRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamCheckRecordRepository extends JpaRepository<ExamCheckRecord, String> {
    List<ExamCheckRecord> findAllByUserId(String userId);
}
