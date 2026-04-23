package com.alibou.book.Repositories;

import com.alibou.book.DTO.Projections.*;
import com.alibou.book.Entity.PaymentStatuss;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentStatusRepository extends JpaRepository<PaymentStatuss, Long> {
    Optional<PaymentStatuss> findByTransactionId(Long transactionId);

    Optional<PaymentStatuss> findByExternalRef(String externalRef);

    List<PaymentStatuss> findByUser_Id(Long userId);




    // Method to calculate total revenue (sum of all successful payments)
    @Query("SELECT SUM(p.amount) FROM PaymentStatuss p WHERE p.txStatus = 1") // Assuming 1 means successful
    Double calculateTotalRevenue();

    // Optional: Get revenue for a specific user
    @Query("SELECT SUM(p.amount) FROM PaymentStatuss p WHERE p.txStatus = 1 AND p.user.id = :userId")
    Double calculateRevenueByUser(Long userId);

    // Optional: Get revenue within a date range
    @Query("SELECT SUM(p.amount) FROM PaymentStatuss p WHERE p.txStatus = 1 AND p.timestamp BETWEEN :start AND :end")
    Double calculateRevenueBetweenDates(LocalDateTime start, LocalDateTime end);

//    List<PaymentStatus> findByUserIdOrderByPaymentDateDesc(String userId); // Fetch user transactions in descending order








    // Daily payment totals (using DTO instead of Map)
    @Query("""
        SELECT new com.alibou.book.DTO.Projections.DailyPaymentSummary(
            CAST(ps.timestamp AS date), 
            SUM(ps.amount),
            COUNT(ps))
        FROM PaymentStatuss ps
        WHERE ps.timestamp BETWEEN :start AND :end
        AND ps.txStatus = :status
        GROUP BY CAST(ps.timestamp AS date)
        ORDER BY CAST(ps.timestamp AS date)
        """)
    List<DailyPaymentSummary> getDailyPaymentSummary(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") int paymentStatus);

    // Weekly payment totals grouped by status
    @Query("""
        SELECT new com.alibou.book.DTO.Projections.WeeklyPaymentStats(
            FUNCTION('WEEK', ps.timestamp),
            ps.txStatus,
            SUM(ps.amount),
            COUNT(ps))
        FROM PaymentStatuss ps
        WHERE ps.timestamp BETWEEN :start AND :end
        GROUP BY FUNCTION('WEEK', ps.timestamp), ps.txStatus
        ORDER BY FUNCTION('WEEK', ps.timestamp), ps.txStatus
        """)
    List<WeeklyPaymentStats> getWeeklyPaymentStats(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // Monthly revenue by user
//    @Query("""
//        SELECT new com.alibou.book.DTO.Projections.WeeklyRevenueSummary(
//            FUNCTION('WEEK', ps.timestamp),
//            SUM(ps.amount),
//            COUNT(ps.id))
//        FROM PaymentStatuss ps
//        WHERE YEAR(ps.timestamp) = :year
//        AND MONTH(ps.timestamp) = :month
//        AND ps.txStatus = :statusCode
//        GROUP BY FUNCTION('WEEK', ps.timestamp)
//        ORDER BY FUNCTION('WEEK', ps.timestamp)
//        """)
//    List<WeeklyRevenueSummary> findWeeklyRevenueByMonth(
//            @Param("year") int year,
//            @Param("month") int month,
//            @Param("statusCode") int statusCode);





    @Query("SELECT new com.alibou.book.DTO.Projections.WeeklyRevenueSummary(" +
            "FUNCTION('WEEK', ps.timestamp), " +
            "SUM(ps.amount), " +
            "COUNT(ps.id)) " +
            "FROM PaymentStatuss ps " +
            "WHERE ps.timestamp BETWEEN :start AND :end " +
            "AND ps.txStatus = :statusCode " +
            "GROUP BY FUNCTION('WEEK', ps.timestamp) " +
            "ORDER BY FUNCTION('WEEK', ps.timestamp)")
    List<WeeklyRevenueSummary> findWeeklyRevenueByMonth(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("statusCode") int statusCode);







    @Query("""
    SELECT new com.alibou.book.DTO.Projections.DailyPaymentSummary(
        CAST(ps.timestamp AS localdate),
        SUM(ps.amount),
        COUNT(ps.id)
    )
    FROM PaymentStatuss ps
    WHERE ps.timestamp BETWEEN :start AND :end
    AND ps.txStatus = :statusCode
    GROUP BY CAST(ps.timestamp AS localdate)
    ORDER BY CAST(ps.timestamp AS localdate)
    """)
    List<DailyPaymentSummary> getDailyPaymentTotals(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("statusCode") int statusCode);




    @Query("""
    SELECT new com.alibou.book.DTO.Projections.MonthlyRevenueSummary(
        FUNCTION('MONTH', ps.timestamp),
        SUM(ps.amount),
        COUNT(ps.id)
    )
    FROM PaymentStatuss ps
    WHERE FUNCTION('YEAR', ps.timestamp) = :year
      AND ps.txStatus = :statusCode
    GROUP BY FUNCTION('MONTH', ps.timestamp)
    ORDER BY FUNCTION('MONTH', ps.timestamp)
    """)
    List<MonthlyRevenueSummary> getMonthlyRevenue(
            @Param("year") int year,
            @Param("statusCode") int statusCode);








    // Find payments by transaction ID and user
    @Query("""
        SELECT ps
        FROM PaymentStatuss ps
        WHERE ps.transactionId = :transactionId
        AND ps.user.id = :userId
        """)
    Optional<PaymentStatuss> findByTransactionIdAndUser(
            @Param("transactionId") long transactionId,
            @Param("userId") Long userId);
}