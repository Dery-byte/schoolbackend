package com.alibou.book.Repositories;

import com.alibou.book.Entity.EligibilityRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EligibilityRecordRepository extends JpaRepository<EligibilityRecord, String> {
    List<EligibilityRecord> findByUserId(String userId);

    //@EntityGraph(attributePaths = {"universities"})
    List<EligibilityRecord> findAllByUserId(String userId);


}
