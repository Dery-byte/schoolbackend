package com.alibou.book.Repositories;


import com.alibou.book.Entity.ExamCheckRecord;
import com.alibou.book.Entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamCheckRecordRepository extends JpaRepository<ExamCheckRecord, String> {
    List<ExamCheckRecord> findAllByUserId(Integer userId);
    Optional<ExamCheckRecord> findByUserIdAndPaymentStatus(Integer userId, PaymentStatus status);

    Optional<ExamCheckRecord> findByExternalRef(String externalRef);

    // Combined with status
    Optional<ExamCheckRecord> findByUserIdAndPaymentStatusAndExternalRef(
            Integer userId, PaymentStatus status, String externalRef);


    Optional<ExamCheckRecord> findFirstByUserIdAndPaymentStatusOrderByCreatedAtDesc(Integer userId, PaymentStatus paymentStatus);

    // Count all records
    long count();

    // Count records by payment status
    long countByPaymentStatus(PaymentStatus paymentStatus);

    long countByPaymentStatusAndCreatedAtBetween(PaymentStatus paymentStatus, Instant startDate, Instant endDate);

    long countByCreatedAtBetween(Instant startDate, Instant endDate);




    @Query("SELECT MONTH(e.createdAt) AS month, COUNT(e.id) AS totalRecords " +
            "FROM ExamCheckRecord e " +
            "WHERE YEAR(e.createdAt) = :year " +
            "AND e.paymentStatus = com.alibou.book.Entity.PaymentStatus.PAID " +
            "GROUP BY MONTH(e.createdAt) " +
            "ORDER BY month")
    List<Object[]> getMonthlyExamCheckRecords(@Param("year") int year);





}



