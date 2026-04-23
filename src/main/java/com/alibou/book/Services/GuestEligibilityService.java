package com.alibou.book.Services;

import com.alibou.book.DTO.EligibilityDTOs.EligibilityApiResponse;
import com.alibou.book.DTO.GuestEligibilityCheckRequest;
import com.alibou.book.DTO.GuestSaveTempRequest;
import com.alibou.book.Entity.*;
import com.alibou.book.Repositories.EligibilityRecordRepository;
import com.alibou.book.Repositories.ExamCheckRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestEligibilityService {

    private final EligibilityService eligibilityService;
    private final ExamCheckRecordRepository examCheckRecordRepository;
    private final EligibilityRecordRepository eligibilityRecordRepository;

    @Transactional
    public EligibilityApiResponse checkEligibility(GuestEligibilityCheckRequest request) {
        ExamCheckRecord record = examCheckRecordRepository.findById(request.getCheckRecordId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "ExamCheckRecord not found: " + request.getCheckRecordId()));

        if (!request.getSessionId().equals(record.getSessionId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid session for this record.");
        }

        if (record.getPaymentStatus() != PaymentStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Payment not verified. Please complete payment before running eligibility check.");
        }

        WaecCandidateEntity candidate = buildCandidate(request, record);

        String guestUserId = "GUEST_" + request.getSessionId();
        EligibilityApiResponse response = eligibilityService.checkEligibilityWithDetails(
                candidate,
                request.getUniversityType(),
                guestUserId,
                request.getCheckRecordId(),
                request.getCategoryIds()
        );

        // Tag the resulting EligibilityRecord as temporary
        if (response != null && response.getRecordId() != null) {
            eligibilityRecordRepository.findById(response.getRecordId()).ifPresent(eligRecord -> {
                eligRecord.setSessionId(request.getSessionId());
                eligRecord.setTemporary(true);
                eligRecord.setPaymentReference(record.getPaymentReference());
                eligibilityRecordRepository.save(eligRecord);
            });
        }

        return response;
    }

    @Transactional
    public EligibilityRecord saveTempRecord(GuestSaveTempRequest request) {
        EligibilityRecord eligRecord = eligibilityRecordRepository.findById(request.getEligibilityRecordId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "EligibilityRecord not found: " + request.getEligibilityRecordId()));

        if (!request.getSessionId().equals(eligRecord.getSessionId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid session for this record.");
        }

        eligRecord.setTemporary(true);
        return eligibilityRecordRepository.save(eligRecord);
    }

    private WaecCandidateEntity buildCandidate(GuestEligibilityCheckRequest request, ExamCheckRecord record) {
        WaecCandidateEntity candidate = new WaecCandidateEntity();
        candidate.setCname(record.getCandidateName() != null ? record.getCandidateName() : "Guest Candidate");

        List<WaecResultDetailEntity> resultDetails = request.getResultDetails().stream()
                .map(dto -> {
                    WaecResultDetailEntity detail = new WaecResultDetailEntity();
                    detail.setSubject(dto.getSubject());
                    detail.setGrade(dto.getGrade());
                    detail.setInterpretation(dto.getInterpretation());
                    detail.setSubjectcode(dto.getSubjectcode());
                    detail.setCandidate(candidate);
                    return detail;
                })
                .collect(Collectors.toList());

        candidate.setResultDetails(resultDetails);
        return candidate;
    }
}
