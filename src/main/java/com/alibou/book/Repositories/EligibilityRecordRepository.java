package com.alibou.book.Repositories;

import com.alibou.book.DTO.EligibilityMonthlySummary;
import com.alibou.book.Entity.EligibilityRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EligibilityRecordRepository extends JpaRepository<EligibilityRecord, String> {
    List<EligibilityRecord> findByUserId(String userId);


    // Count all eligibility records
    long count();

    // Count records by user ID
    long countByUserId(String userId);


    // Find records with pagination
    @Query("SELECT e FROM EligibilityRecord e ORDER BY e.createdAt DESC")
    List<EligibilityRecord> findAllOrderByCreatedAtDesc();


//    @Query(value = """
//        SELECT
//            MONTH(er.created_at) as month,
//            COUNT(DISTINCT er.id) as recordCount,
//            COUNT(DISTINCT u.id) as universityCount
//        FROM eligibility_record er
//        LEFT JOIN university_eligibility u ON u.eligibility_record_id = er.id
//        WHERE YEAR(er.created_at) = ?1
//        GROUP BY MONTH(er.created_at)
//        ORDER BY MONTH(er.created_at)
//        """, nativeQuery = true)
//    List<EligibilityMonthlySummary> getMonthlyStats(int year);



        @Query("""
        SELECT NEW com.alibou.book.DTO.EligibilityMonthlySummary(
            MONTH(er.createdAt),
            COUNT(DISTINCT er.id),
            COUNT(DISTINCT u.id))
        FROM EligibilityRecord er
        LEFT JOIN er.universities u
        WHERE YEAR(er.createdAt) = :year
        GROUP BY MONTH(er.createdAt)
        ORDER BY MONTH(er.createdAt)
        """)
        List<EligibilityMonthlySummary> getMonthlyStats(@Param("year") int year);
    }





