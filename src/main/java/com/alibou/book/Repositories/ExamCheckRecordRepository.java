package com.alibou.book.Repositories;


import com.alibou.book.Entity.ExamCheckRecord;
import com.alibou.book.Entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamCheckRecordRepository extends JpaRepository<ExamCheckRecord, String> {
    List<ExamCheckRecord> findAllByUserId(String userId);
    Optional<ExamCheckRecord> findByUserIdAndPaymentStatus(String userId, PaymentStatus status);

    Optional<ExamCheckRecord> findByExternalRef(String externalRef);

    // Combined with status
    Optional<ExamCheckRecord> findByUserIdAndPaymentStatusAndExternalRef(
            String userId, PaymentStatus status, String externalRef);
}
