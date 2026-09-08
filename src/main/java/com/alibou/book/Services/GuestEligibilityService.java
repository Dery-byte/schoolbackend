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
        log.info("🚀 START: Guest eligibility check | sessionId={} | recordId={}", 
                request.getSessionId(), request.getCheckRecordId());

        try {
            ExamCheckRecord record = examCheckRecordRepository.findById(request.getCheckRecordId())
                    .orElseThrow(() -> {
                        log.error("❌ ExamCheckRecord not found: {}", request.getCheckRecordId());
                        return new EntityNotFoundException("ExamCheckRecord not found: " + request.getCheckRecordId());
                    });

            if (!request.getSessionId().equals(record.getSessionId())) {
                log.warn("⚠️ Session mismatch | requestSession={} | recordSession={}", 
                        request.getSessionId(), record.getSessionId());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid session for this record.");
            }

            if (record.getPaymentStatus() != PaymentStatus.PAID) {
                log.warn("⚠️ Payment not verified for record: {}", request.getCheckRecordId());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Payment not verified. Please complete payment before running eligibility check.");
            }

            WaecCandidateEntity candidate = buildCandidate(request, record);

            String guestUserId = "GUEST_" + request.getSessionId();
            log.debug("🏃 Calling eligibilityService.checkEligibilityWithDetails for guest...");
            EligibilityApiResponse response = eligibilityService.checkEligibilityWithDetails(
                    candidate,
                    request.getUniversityType(),
                    guestUserId,
                    request.getCheckRecordId(),
                    request.getCategoryIds()
            );

            // Tag the resulting EligibilityRecord as temporary (single UPDATE — no load+save round-trip)
            if (response != null && response.getRecordId() != null) {
                log.debug("🔄 Tagging EligibilityRecord as temporary | recordId={}", response.getRecordId());
                eligibilityRecordRepository.tagAsTemporary(
                        response.getRecordId(),
                        request.getSessionId(),
                        record.getPaymentReference()
                );
                log.debug("✅ Tagged as temporary successfully");
            }

            log.info("✅ END: Guest eligibility check successful");
            return response;
        } catch (Exception e) {
            log.error("💥 ERROR in checkEligibility for guest: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public EligibilityRecord saveTempRecord(GuestSaveTempRequest request) {
        log.info("💾 START: Saving temporary record | eligibilityRecordId={} | sessionId={}", 
                request.getEligibilityRecordId(), request.getSessionId());

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

    @Transactional(readOnly = true)
    public java.util.Optional<EligibilityApiResponse> getEligibilityBySessionId(String sessionId) {
        return eligibilityRecordRepository.findBySessionId(sessionId)
                .map(record -> {
                    // Convert universities
                    List<com.alibou.book.DTO.EligibilityDTOs.UniversityEligibilityDto> uniDtos = null;
                    if (record.getUniversities() != null) {
                        uniDtos = record.getUniversities().stream().map(uni -> {
                            
                            List<com.alibou.book.DTO.EligibilityDTOs.ProgramEligibilityDto> progDtos = new java.util.ArrayList<>();
                            if (uni.getEligiblePrograms() != null) {
                                progDtos.addAll(uni.getEligiblePrograms().stream().map(prog -> 
                                    com.alibou.book.DTO.EligibilityDTOs.ProgramEligibilityDto.builder()
                                        .programName(prog.getName())
                                        .status("ELIGIBLE")
                                        .eligibilityPercentage(prog.getPercentage())
                                        .build()
                                ).collect(Collectors.toList()));
                            }
                            if (uni.getAlternativePrograms() != null) {
                                progDtos.addAll(uni.getAlternativePrograms().stream().map(prog -> 
                                    com.alibou.book.DTO.EligibilityDTOs.ProgramEligibilityDto.builder()
                                        .programName(prog.getName())
                                        .status("ALTERNATIVE")
                                        .eligibilityPercentage(prog.getPercentage())
                                        .build()
                                ).collect(Collectors.toList()));
                            }
                            
                            return com.alibou.book.DTO.EligibilityDTOs.UniversityEligibilityDto.builder()
                                .universityName(uni.getUniversityName())
                                .universityType(uni.getType())
                                .programs(progDtos)
                                .build();
                        }).collect(Collectors.toList());
                    }

                    // Build summary
                    long totalUnis = uniDtos != null ? uniDtos.size() : 0;
                    long totalEligible = uniDtos != null ? uniDtos.stream()
                            .flatMap(u -> u.getPrograms() != null ? u.getPrograms().stream() : java.util.stream.Stream.empty())
                            .filter(p -> "ELIGIBLE".equals(p.getStatus()))
                            .count() : 0;
                    long totalAlternative = uniDtos != null ? uniDtos.stream()
                            .flatMap(u -> u.getPrograms() != null ? u.getPrograms().stream() : java.util.stream.Stream.empty())
                            .filter(p -> "ALTERNATIVE".equals(p.getStatus()))
                            .count() : 0;
                            
                    com.alibou.book.DTO.EligibilityDTOs.EligibilitySummary summary = com.alibou.book.DTO.EligibilityDTOs.EligibilitySummary.builder()
                        .totalUniversities((int) totalUnis)
                        .totalEligiblePrograms((int) totalEligible)
                        .totalAlternativePrograms((int) totalAlternative)
                        .build();
                    
                    return EligibilityApiResponse.builder()
                        .recordId(record.getId())
                        .universities(uniDtos)
                        .summary(summary)
                        .build();
                });
    }
}
