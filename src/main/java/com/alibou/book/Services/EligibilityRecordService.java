package com.alibou.book.Services;

import com.alibou.book.DTO.EligibilityMonthlySummary;
import com.alibou.book.Entity.EligibilityRecord;
import com.alibou.book.Repositories.EligibilityRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EligibilityRecordService {

    private final EligibilityRecordRepository repository;
    private final UserDetailsService userDetailsService;
    private final EligibilityRecordRepository eligibilityRecordRepository;

    public List<EligibilityRecord> getAllRecords() {
        return repository.findAll();
    }

    public List<EligibilityRecord> getRecordsByUser(String userId) {
        return repository.findByUserId(userId);  // uses eager fetch
    }








    // Get total count of all eligibility records
    public long getTotalEligibilityRecords() {
        return repository.count();
    }

    // Get count for a specific user
    public long getEligibilityRecordsCountByUser(String userId) {
        return repository.countByUserId(userId);
    }

    // Get all records for a user
    public List<EligibilityRecord> getEligibilityRecordsByUser(String userId) {
        return repository.findByUserId(userId);
    }

    // Get all records with pagination
    public List<EligibilityRecord> getAllEligibilityRecords() {
        return repository.findAllOrderByCreatedAtDesc();
    }

    // Get a single record by ID
    public EligibilityRecord getEligibilityRecordById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Eligibility record not found"));
    }

    // Save a new record
    public EligibilityRecord createEligibilityRecord(EligibilityRecord record) {
        return repository.save(record);
    }

    // Delete a record
    public void deleteEligibilityRecord(String id) {
        repository.deleteById(id);
    }




    public List<EligibilityMonthlySummary> getMonthlyStats(int year) {
        return eligibilityRecordRepository.getMonthlyStats(year);
    }




}
