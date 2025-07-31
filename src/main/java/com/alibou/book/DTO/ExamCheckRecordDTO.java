package com.alibou.book.DTO;

import com.alibou.book.Entity.ExamCheckRecord;
import com.alibou.book.Entity.PaymentStatus;
import com.alibou.book.Entity.WaecCandidateEntity;

import java.time.Instant;

public record ExamCheckRecordDTO(
        String id,
        String userId,
        String candidateName,
        PaymentStatus paymentStatus,
        String checkStatus,
        Instant createdAt,
        Instant lastUpdated,
        int checkLimit,
        String externalRef,
        WaecCandidateEntity waecCandidateEntity  // or create a DTO for this too
) {
    public static ExamCheckRecordDTO fromEntity(ExamCheckRecord record) {
        return new ExamCheckRecordDTO(
                record.getId(),
                record.getUserId(),
                record.getCandidateName(),
                record.getPaymentStatus(),
                record.getCheckStatus(),
                record.getCreatedAt(),
                record.getLastUpdated(),
                record.getCheckLimit(),
                record.getExternalRef(),
                record.getWaecCandidateEntity()
        );
    }
}