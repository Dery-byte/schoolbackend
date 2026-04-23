package com.alibou.book.Services;

import com.alibou.book.DTO.GuestPaymentInitiateRequest;
import com.alibou.book.DTO.GuestPaymentInitiateResponse;
import com.alibou.book.DTO.MoolrePaymentRequest;
import com.alibou.book.Entity.*;
import com.alibou.book.Repositories.ExamCheckRecordRepository;
import com.alibou.book.config.MoolreConfig;
import com.alibou.book.exception.PaymentProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestPaymentService {

    private final MoolreConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ExamCheckRecordRepository examCheckRecordRepository;

    // Maps sessionId -> externalRef for OTP verification
    private final Map<String, String> sessionPaymentReferences = new ConcurrentHashMap<>();

    @Transactional
    public GuestPaymentInitiateResponse initiateGuestPayment(GuestPaymentInitiateRequest initiateRequest) {
        String sessionId = UUID.randomUUID().toString();
        String externalRef = UUID.randomUUID().toString();

        ExamCheckRecord record = new ExamCheckRecord();
        record.setSessionId(sessionId);
        record.setExternalRef(externalRef);
        record.setPaymentReference(externalRef);
        record.setTemporary(true);
        record.setPaymentStatus(PaymentStatus.PENDING);
        record.setCandidateName(initiateRequest.getCandidateName());
        record.setPendingSubscriptionType(initiateRequest.getSubscriptionType());
        record.setCreatedAt(Instant.now());
        record.setLastUpdated(Instant.now());
        record.setCheckStatus(CheckStatus.NOT_CHECKED);
        record.setCheckLimit(0);
        examCheckRecordRepository.save(record);

        sessionPaymentReferences.put(sessionId, externalRef);

        MoolrePaymentRequest moolreRequest = new MoolrePaymentRequest();
        moolreRequest.setAccountnumber(config.getAccountNumber());
        moolreRequest.setCurrency("GHS");
        moolreRequest.setType(1);
        moolreRequest.setReference("Optimus");
        moolreRequest.setExternalref(externalRef);
        moolreRequest.setPayer(initiateRequest.getPayer());
        moolreRequest.setChannel(initiateRequest.getChannel());
        moolreRequest.setAmount(initiateRequest.getAmount());

        HttpHeaders headers = createHeaders();
        HttpEntity<MoolrePaymentRequest> entity = new HttpEntity<>(moolreRequest, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    config.getApiUrl(), HttpMethod.POST, entity, String.class);

            MoolrePaymentResponse moolreResponse = objectMapper.readValue(response.getBody(), MoolrePaymentResponse.class);

            return GuestPaymentInitiateResponse.builder()
                    .sessionId(sessionId)
                    .externalRef(externalRef)
                    .recordId(record.getId())
                    .status(moolreResponse.getStatus() != null ? moolreResponse.getStatus() : 0)
                    .code(moolreResponse.getCode())
                    .message(moolreResponse.getMessage())
                    .userMessage(moolreResponse.getStatus() != null && moolreResponse.getStatus() == 1
                            ? "A verification code has been sent to your phone."
                            : "Payment initiation failed: " + moolreResponse.getMessage())
                    .build();

        } catch (Exception e) {
            record.setPaymentStatus(PaymentStatus.FAILED);
            examCheckRecordRepository.save(record);
            log.error("Guest payment initiation failed: {}", e.getMessage(), e);
            throw new PaymentProcessingException("Failed to initiate guest payment. Please try again.", e);
        }
    }











    public MoolrePaymentResponse verifyGuestOtp(MoolrePaymentRequest request, String sessionId) {
        String externalRef = sessionPaymentReferences.get(sessionId);
        if (externalRef == null) {
            throw new PaymentProcessingException("No payment session found. Please restart the payment process.");
        }

        request.setAccountnumber(config.getAccountNumber());
        request.setCurrency("GHS");
        request.setType(1);
        request.setReference("Optimus");
        request.setExternalref(externalRef);
        HttpHeaders headers = createHeaders();
        HttpEntity<MoolrePaymentRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    config.getApiUrl(), HttpMethod.POST, entity, String.class);

            MoolrePaymentResponse verificationResponse = objectMapper.readValue(response.getBody(), MoolrePaymentResponse.class);

            if (verificationResponse.getStatus() != null && verificationResponse.getStatus() == 1) {
                return triggerGuestPayment(request);
            } else {
                updateExamCheckRecordStatus(externalRef, PaymentStatus.FAILED);
                throw new PaymentProcessingException("OTP verification failed: " +
                        (verificationResponse.getMessage() != null ? verificationResponse.getMessage() : "Please try again."));
            }
        } catch (PaymentProcessingException e) {
            throw e;
        } catch (Exception e) {
            updateExamCheckRecordStatus(externalRef, PaymentStatus.FAILED);
            log.error("Guest OTP verification error: {}", e.getMessage(), e);
            throw new PaymentProcessingException("Failed to verify OTP. Please try again.", e);
        }
    }

    private MoolrePaymentResponse triggerGuestPayment(MoolrePaymentRequest request) {
        HttpHeaders headers = createHeaders();
        HttpEntity<MoolrePaymentRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    config.getApiUrl(), HttpMethod.POST, entity, String.class);

            MoolrePaymentResponse paymentResponse = objectMapper.readValue(response.getBody(), MoolrePaymentResponse.class);

            if (paymentResponse.getStatus() != null && paymentResponse.getStatus() == 1
                    && "TR099".equals(paymentResponse.getCode())) {
                log.info("Guest payment triggered for externalRef: {}", request.getExternalref());
            } else {
                updateExamCheckRecordStatus(request.getExternalref(), PaymentStatus.FAILED);
                throw new PaymentProcessingException("Payment failed: " +
                        (paymentResponse.getMessage() != null ? paymentResponse.getMessage() : "Please try again."));
            }
            return paymentResponse;
        } catch (PaymentProcessingException e) {
            throw e;
        } catch (Exception e) {
            updateExamCheckRecordStatus(request.getExternalref(), PaymentStatus.FAILED);
            log.error("Error triggering guest payment: {}", e.getMessage(), e);
            throw new PaymentProcessingException("Failed to process payment.", e);
        }
    }

    private void updateExamCheckRecordStatus(String externalRef, PaymentStatus status) {
        examCheckRecordRepository.findByExternalRef(externalRef).ifPresent(record -> {
            record.setPaymentStatus(status);
            record.setLastUpdated(Instant.now());
            examCheckRecordRepository.save(record);
        });
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-USER", config.getUsername());
        headers.set("X-API-PUBKEY", config.getPublicKey());
        return headers;
    }
}
