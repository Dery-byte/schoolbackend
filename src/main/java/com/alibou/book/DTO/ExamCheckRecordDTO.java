package com.alibou.book.DTO;

import com.alibou.book.Entity.ExamCheckRecord;
import com.alibou.book.Entity.PaymentStatus;
import com.alibou.book.Entity.SubscriptionType;
import com.alibou.book.Entity.WaecCandidateEntity;

import java.time.Instant;

public record ExamCheckRecordDTO(
        String id,
        Integer userId,
        String candidateName,
        PaymentStatus paymentStatus,
        com.alibou.book.Entity.CheckStatus checkStatus,
        Instant createdAt,
        Instant lastUpdated,
        int checkLimit,
        String externalRef,
        WaecCandidateEntity waecCandidateEntity,  // or create a DTO for this too
        SubscriptionType subscriptionType
) {
    public static ExamCheckRecordDTO fromEntity(ExamCheckRecord record) {
        return new ExamCheckRecordDTO(
                record.getId(),
                record.getUser().getId(),
//                record.getUserId(),
                record.getCandidateName(),
                record.getPaymentStatus(),
                record.getCheckStatus(),
                record.getCreatedAt(),
                record.getLastUpdated(),
                record.getCheckLimit(),
                record.getExternalRef(),
                record.getWaecCandidateEntity(),
                record.getSubscriptionType()
        );
    }
}