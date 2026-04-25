package com.alibou.book.Services;

import com.alibou.book.DTO.EligibilityMonthlySummary;
import com.alibou.book.Entity.EligibilityRecord;
import com.alibou.book.Repositories.EligibilityRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EligibilityRecordService {

    private final EligibilityRecordRepository repository;

    public List<EligibilityRecord> getAllRecords() {
        return repository.findAll();
    }

    public List<EligibilityRecord> getRecordsByUser(String userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long getTotalEligibilityRecords() {
        return repository.count();
    }

    public long getEligibilityRecordsCountByUser(String userId) {
        return repository.countByUserId(userId);
    }

    public List<EligibilityRecord> getEligibilityRecordsByUser(String userId) {
        return repository.findByUserId(userId);
    }

    public List<EligibilityRecord> getAllEligibilityRecords() {
        return repository.findAllOrderByCreatedAtDesc();
    }

    public EligibilityRecord getEligibilityRecordById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Eligibility record not found: " + id));
    }

    public EligibilityRecord createEligibilityRecord(EligibilityRecord record) {
        return repository.save(record);
    }

    public void deleteEligibilityRecord(String id) {
        repository.deleteById(id);
    }

    public List<EligibilityMonthlySummary> getMonthlyStats(int year) {
        return repository.getMonthlyStats(year);
    }
}
