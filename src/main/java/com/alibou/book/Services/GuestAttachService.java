package com.alibou.book.Services;

import com.alibou.book.Entity.EligibilityRecord;
import com.alibou.book.Entity.ExamCheckRecord;
import com.alibou.book.Repositories.EligibilityRecordRepository;
import com.alibou.book.Repositories.ExamCheckRecordRepository;
import com.alibou.book.Repositories.PaymentStatusRepository;
import com.alibou.book.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestAttachService {

    private final ExamCheckRecordRepository examCheckRecordRepository;
    private final EligibilityRecordRepository eligibilityRecordRepository;
    private final PaymentStatusRepository paymentStatusRepository;
    private final UserDetailsService userDetailsService;

    @Transactional
    public EligibilityRecord attachToUser(String sessionId, Principal principal) {
        User user = (User) userDetailsService.loadUserByUsername(principal.getName());

        ExamCheckRecord examRecord = examCheckRecordRepository
                .findBySessionIdAndTemporary(sessionId, true)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No temporary exam record found for sessionId: " + sessionId));

        EligibilityRecord eligRecord = eligibilityRecordRepository
                .findBySessionIdAndTemporary(sessionId, true)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No temporary eligibility record found for sessionId: " + sessionId));

        examRecord.setUser(user);
        examRecord.setTemporary(false);
        examCheckRecordRepository.save(examRecord);

        eligRecord.setUserId(String.valueOf(user.getId()));
        eligRecord.setTemporary(false);
        eligibilityRecordRepository.save(eligRecord);

        if (examRecord.getPaymentReference() != null) {
            paymentStatusRepository.findByExternalRef(examRecord.getPaymentReference())
                    .ifPresent(paymentStatus -> {
                        paymentStatus.setUser(user);
                        paymentStatusRepository.save(paymentStatus);
                    });
        }

        log.info("Attached guest session {} to user {}", sessionId, user.getId());
        return eligRecord;
    }
}
