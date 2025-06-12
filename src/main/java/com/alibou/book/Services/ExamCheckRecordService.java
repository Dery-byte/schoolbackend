package com.alibou.book.Services;


import com.alibou.book.Entity.ExamCheckRecord;
import com.alibou.book.Entity.PaymentStatus;
import com.alibou.book.Entity.WaecCandidateEntity;
import com.alibou.book.Repositories.ExamCheckRecordRepository;
import com.alibou.book.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExamCheckRecordService {

    private final ExamCheckRecordRepository examCheckRecordRepository;
    private final UserDetailsService userDetailsService;


    // Create a new record
    public ExamCheckRecord startCheck(ExamCheckRecord record, Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("User must be authenticated to add a product.");
        }
        // Load user and validate

        String externalRef = generateReference();
//        request.setExternalref(externalRef);
        User user = (User) userDetailsService.loadUserByUsername(principal.getName());
        Long UserId = Long.valueOf(user.getId());
        record.setUserId(UserId.toString());
        record.setCreatedAt(Instant.now());
        record.setLastUpdated(Instant.now());
        record.setExternalRef(externalRef);
        record.setPaymentStatus(PaymentStatus.PENDING);
        return examCheckRecordRepository.save(record);
    }

    private String generateReference () {
        return UUID.randomUUID().toString();
    }

    public ExamCheckRecord updatePaymentStatus(String id, String paymentStatus) {
        ExamCheckRecord record = examCheckRecordRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Record not found"));
        record.setPaymentStatus(PaymentStatus.PAID);
        record.setLastUpdated(Instant.now());
        return examCheckRecordRepository.save(record);
    }

    public ExamCheckRecord updateCandidate(String id, WaecCandidateEntity candidate) {
        ExamCheckRecord record = examCheckRecordRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Record not found"));
        record.setWaecCandidateEntity(candidate);
        record.setLastUpdated(Instant.now());
        return examCheckRecordRepository.save(record);
    }





























    // Get all records by user ID
    public List<ExamCheckRecord> getAllByUserId(String userId) {
        return examCheckRecordRepository.findAllByUserId(userId);
    }

    // Get a specific record by ID
    public Optional<ExamCheckRecord> getById(String id) {
        return examCheckRecordRepository.findById(id);
    }

    // Update a record
    public ExamCheckRecord update(ExamCheckRecord updatedRecord) {
        updatedRecord.setLastUpdated(Instant.now());
        return examCheckRecordRepository.save(updatedRecord);
    }

    // Delete a record
    public void delete(String id) {
        examCheckRecordRepository.deleteById(id);
    }
}
