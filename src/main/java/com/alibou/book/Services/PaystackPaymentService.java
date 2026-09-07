package com.alibou.book.Services;

import com.alibou.book.DTO.GuestPaymentInitiateRequest;
import com.alibou.book.DTO.GuestPaymentInitiateResponse;
import com.alibou.book.DTO.PaymentInitiateResult;
import com.alibou.book.Entity.*;
import com.alibou.book.Repositories.ExamCheckRecordRepository;
import com.alibou.book.Repositories.PaymentStatusRepository;
import com.alibou.book.config.PaystackConfig;
import com.alibou.book.email.EmailService;
import com.alibou.book.email.EmailTemplateName;
import com.alibou.book.exception.PaymentProcessingException;
import com.alibou.book.user.User;
import com.alibou.book.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Payment service implementation for Paystack.
 *
 * <p>Paystack flow:
 * <ol>
 *   <li>Frontend calls {@code initiatePayment} → backend POSTs to Paystack initialize API →
 *       returns {@code authorization_url} + {@code access_code}.</li>
 *   <li>Frontend opens the Paystack Inline popup using the access code.</li>
 *   <li>On payment success/failure Paystack fires a webhook to
 *       {@code POST /auth/payments/paystackWebhook}.</li>
 *   <li>{@code processWebhook} verifies the HMAC-SHA512 signature and updates
 *       {@code ExamCheckRecord} + {@code PaymentStatuss}.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaystackPaymentService {

    private final PaystackConfig config;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final ExamCheckRecordRepository examCheckRecordRepository;
    private final PaymentStatusRepository paymentStatusRepository;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final MNotifyV2SmsService mNotifyV2SmsService;
    private final EmailService emailService;

    public String getGatewayName() {
        return "PAYSTACK";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  INITIATE PAYMENT  (authenticated user)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Initiates a Paystack transaction for an authenticated user.
     *
     * @param principal  the logged-in user
     * @param amount     the payment amount in GHS (will be converted to pesewas × 100)
     * @param subscriptionType  the subscription tier being purchased
     * @param recordId   existing ExamCheckRecord ID (optional — creates a new one if null)
     * @param usedDiscountCode  whether a discount code was applied
     * @return unified {@link PaymentInitiateResult} containing the Paystack popup details
     */
    @Transactional
    public PaymentInitiateResult initiatePayment(Principal principal,
                                                 double amount,
                                                 SubscriptionType subscriptionType,
                                                 String recordId,
                                                 boolean usedDiscountCode) {
        User user = (User) userDetailsService.loadUserByUsername(principal.getName());
        String externalRef = getOrCreateExternalReference(user, recordId, subscriptionType, usedDiscountCode);

        String email = user.getUsername(); // username is the email in this system
        long amountInPesewas = Math.round(amount * 100);

        JsonNode responseData = callPaystackInitialize(email, amountInPesewas, externalRef);

        String authUrl   = responseData.path("authorization_url").asText(null);
        String accessCode = responseData.path("access_code").asText(null);

        log.info("Paystack transaction initialized for user={}, ref={}, authUrl={}",
                principal.getName(), externalRef, authUrl);

        return PaymentInitiateResult.builder()
                .gatewayName("PAYSTACK")
                .externalRef(externalRef)
                .requiresOtp(false)
                .authorizationUrl(authUrl)
                .accessCode(accessCode)
                .status(1)
                .message("Paystack authorization URL created")
                .userMessage("Complete your payment securely via Paystack.")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  INITIATE PAYMENT  (guest)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Initiates a Paystack transaction for a guest (unauthenticated) user.
     */
    @Transactional
    public GuestPaymentInitiateResponse initiateGuestPayment(GuestPaymentInitiateRequest req) {
        String sessionId   = UUID.randomUUID().toString();
        String externalRef = UUID.randomUUID().toString();

        ExamCheckRecord record = new ExamCheckRecord();
        record.setSessionId(sessionId);
        record.setExternalRef(externalRef);
        record.setPaymentReference(externalRef);
        record.setTemporary(true);
        record.setPaymentStatus(PaymentStatus.PENDING);
        record.setCandidateName(req.getCandidateName());
        record.setPendingSubscriptionType(req.getSubscriptionType());
        record.setCreatedAt(Instant.now());
        record.setLastUpdated(Instant.now());
        record.setCheckStatus(CheckStatus.NOT_CHECKED);
        record.setCheckLimit(0);
        examCheckRecordRepository.save(record);

        // Use a placeholder email if the guest flow doesn't collect one
        String email = (req.getPayer() != null && req.getPayer().contains("@"))
                ? req.getPayer()
                : "guest+" + sessionId.substring(0, 8) + "@optimus.app";

        long amountInPesewas = Math.round(req.getAmount() * 100);

        try {
            JsonNode responseData = callPaystackInitialize(email, amountInPesewas, externalRef);
            String authUrl    = responseData.path("authorization_url").asText(null);
            String accessCode = responseData.path("access_code").asText(null);

            return GuestPaymentInitiateResponse.builder()
                    .sessionId(sessionId)
                    .externalRef(externalRef)
                    .recordId(record.getId())
                    .status(1)
                    .code("PAYSTACK_INIT")
                    .message(authUrl)          // re-use message field to carry the authUrl
                    .userMessage(accessCode)   // re-use userMessage field to carry accessCode
                    .build();

        } catch (Exception e) {
            record.setPaymentStatus(PaymentStatus.FAILED);
            examCheckRecordRepository.save(record);
            log.error("Paystack guest payment initiation failed: {}", e.getMessage(), e);
            throw new PaymentProcessingException("Failed to initiate guest payment via Paystack. Please try again.", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  VERIFY TRANSACTION  (frontend polling after popup closes)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies a Paystack transaction by reference. Called by the frontend after the
     * Paystack popup reports success, to confirm server-side before updating the UI.
     *
     * @return JSON node from Paystack verify API, or null on failure
     */
    @Transactional
    public JsonNode verifyTransaction(String reference) {
        String url = config.getBaseUrl() + "/transaction/verify/" + reference;
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            if (root.path("status").asBoolean(false)) {
                JsonNode data = root.path("data");
                String txStatus = data.path("status").asText("");
                
                // If Paystack confirms it's successful, update DB immediately.
                // handleChargeSuccess is idempotent so it's safe if the webhook arrives later.
                if ("success".equalsIgnoreCase(txStatus)) {
                    log.info("Paystack verify confirmed success for ref={}. Updating DB...", reference);
                    handleChargeSuccess(data);
                }
                return data;
            }
            log.warn("Paystack verify returned status=false for ref={}", reference);
            return null;
        } catch (Exception e) {
            log.error("Error verifying Paystack transaction ref={}: {}", reference, e.getMessage(), e);
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PROCESS WEBHOOK
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Processes a Paystack webhook event.
     *
     * <p>Validates the HMAC-SHA512 signature, then handles {@code charge.success} events
     * by updating the relevant {@link ExamCheckRecord} and saving a {@link PaymentStatuss}.
     *
     * @param rawPayload   the raw JSON body as a String
     * @param signature    the value of the {@code x-paystack-signature} header
     */
    @Transactional
    public void processWebhook(String rawPayload, String signature) {
        // 1. Verify HMAC signature
        if (!isValidSignature(rawPayload, signature)) {
            log.error("Invalid Paystack webhook signature — request rejected");
            throw new SecurityException("Invalid Paystack webhook signature");
        }

        try {
            JsonNode root  = objectMapper.readTree(rawPayload);
            String   event = root.path("event").asText("");
            JsonNode data  = root.path("data");

            log.info("Paystack webhook received: event={}", event);

            if ("charge.success".equals(event)) {
                handleChargeSuccess(data);
            } else {
                log.info("Ignoring Paystack webhook event: {}", event);
            }
        } catch (SecurityException se) {
            throw se;
        } catch (Exception e) {
            log.error("Error processing Paystack webhook: {}", e.getMessage(), e);
            throw new PaymentProcessingException("Failed to process Paystack webhook", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void handleChargeSuccess(JsonNode data) {
        String reference  = data.path("reference").asText();
        double amountPesewas = data.path("amount").asDouble(0);
        double amountGhs  = amountPesewas / 100.0;
        String email      = data.path("customer").path("email").asText("");
        String txId       = data.path("id").asText("");
        String paidAt     = data.path("paid_at").asText("");

        log.info("Paystack charge.success: ref={}, amount=GHS{}, email={}", reference, amountGhs, email);

        // Update ExamCheckRecord
        examCheckRecordRepository.findByExternalRef(reference).ifPresentOrElse(record -> {
            // Idempotency check: if already PAID, we've already processed this (e.g. via verifyTransaction)
            if (PaymentStatus.PAID.equals(record.getPaymentStatus())) {
                log.info("ExamCheckRecord {} is already PAID. Skipping duplicate processing.", reference);
                return;
            }

            record.setPaymentStatus(PaymentStatus.PAID);
            record.setLastUpdated(Instant.now());
            record.setCheckStatus(CheckStatus.IN_PROGRESS);

            if (record.getPendingSubscriptionType() != null) {
                try {
                    record.setSubscriptionType(SubscriptionType.valueOf(record.getPendingSubscriptionType()));
                } catch (IllegalArgumentException ex) {
                    log.warn("Unknown pending subscription type: {}", record.getPendingSubscriptionType());
                }
                record.setPendingSubscriptionType(null);
            }

            // Clear discount code if it was used
            if (record.isUsedDiscountCode() && record.getUser() != null) {
                User recordUser = record.getUser();
                recordUser.setDiscountCode(null);
                recordUser.setDiscountPackage(null);
                recordUser.setDiscountPrice(null);
                recordUser.setChecksSinceLastDiscount(0);
                userRepository.save(recordUser);
//                logger.info("Discount code used and revoked for user: " + recordUser.getUsername());
            }

            examCheckRecordRepository.save(record);

            // Save to PaymentStatuss table
            savePaymentStatus(record, reference, amountGhs, email, txId, paidAt);

            // Send notifications if there's a real user (not guest)
            if (record.getUser() != null) {
                sendPaymentSuccessEmail(record.getUser(), amountGhs, txId);
                sendPaymentSuccessSms(record.getUser(), amountGhs, txId);
            }

            log.info("ExamCheckRecord {} updated to PAID for Paystack ref={}", record.getId(), reference);

        }, () -> log.warn("No ExamCheckRecord found for Paystack ref={}", reference));
    }

    private void savePaymentStatus(ExamCheckRecord record, String reference,
                                   double amountGhs, String payerEmail,
                                   String paystackTxId, String paidAt) {
        try {
            PaymentStatuss ps = new PaymentStatuss();
            ps.setTxStatus(1);
            ps.setPayer(payerEmail);
            ps.setPayee("Paystack"); // no account number concept in Paystack
            ps.setAmount(amountGhs);
            ps.setValue(amountGhs);

            // Parse Paystack's ISO timestamp (e.g. "2024-01-15T14:30:00.000Z")
            try {
                if (paidAt != null && !paidAt.isBlank()) {
                    Instant instant = Instant.parse(paidAt);
                    ps.setTimestampFromInstant(instant);
                } else {
                    ps.setTimestampFromInstant(Instant.now());
                }
            } catch (Exception te) {
                ps.setTimestampFromInstant(Instant.now());
            }

            // Try to parse Paystack's numeric transaction ID
            try {
                ps.setTransactionId(Long.parseLong(paystackTxId));
            } catch (NumberFormatException nfe) {
                ps.setTransactionId(0L);
            }

            ps.setExternalRef(reference);
            ps.setThirdPartyRef(paystackTxId);

            if (record.getUser() != null) {
                ps.setUser(record.getUser());
            }

            paymentStatusRepository.save(ps);
        } catch (Exception e) {
            log.error("Failed to save PaymentStatuss for Paystack ref={}: {}", reference, e.getMessage(), e);
        }
    }

    /**
     * Calls Paystack's {@code /transaction/initialize} endpoint.
     *
     * @return the {@code data} node from the Paystack response
     */
    private JsonNode callPaystackInitialize(String email, long amountInSmallestUnit, String reference) {
        String url = config.getBaseUrl() + "/transaction/initialize";
        HttpHeaders headers = createHeaders();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("amount", amountInSmallestUnit);
        body.put("reference", reference);
        if (config.getCallbackUrl() != null && !config.getCallbackUrl().isBlank()) {
            body.put("callback_url", config.getCallbackUrl());
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            if (!root.path("status").asBoolean(false)) {
                String msg = root.path("message").asText("Paystack initialization failed");
                throw new PaymentProcessingException(msg);
            }
            return root.path("data");

        } catch (PaymentProcessingException ppe) {
            throw ppe;
        } catch (Exception e) {
            log.error("Error calling Paystack initialize: {}", e.getMessage(), e);
            throw new PaymentProcessingException("Failed to connect to Paystack. Please try again.", e);
        }
    }

    /**
     * Creates the {@link ExamCheckRecord} (or reuses an existing pending one) and returns the externalRef.
     */
    private String getOrCreateExternalReference(User user, String recordId,
                                                SubscriptionType subscriptionType,
                                                boolean usedDiscountCode) {
        if (recordId != null) {
            Optional<ExamCheckRecord> existing = examCheckRecordRepository.findById(recordId);
            if (existing.isPresent()) {
                ExamCheckRecord record = existing.get();
                if (record.getPaymentStatus() == PaymentStatus.PENDING
                        && record.getUser() != null
                        && record.getUser().getId().equals(user.getId())) {
                    String externalRef = UUID.randomUUID().toString();
                    record.setExternalRef(externalRef);
                    record.setLastUpdated(Instant.now());
                    record.setPendingSubscriptionType(subscriptionType != null ? subscriptionType.name() : null);
                    record.setUsedDiscountCode(usedDiscountCode);
                    examCheckRecordRepository.save(record);
                    return externalRef;
                }
            }
        }

        String externalRef = UUID.randomUUID().toString();
        ExamCheckRecord newRecord = new ExamCheckRecord();
        newRecord.setUser(user);
        newRecord.setExternalRef(externalRef);
        newRecord.setPaymentStatus(PaymentStatus.PENDING);
        newRecord.setPendingSubscriptionType(subscriptionType != null ? subscriptionType.name() : null);
        newRecord.setUsedDiscountCode(usedDiscountCode);
        newRecord.setCreatedAt(Instant.now());
        newRecord.setLastUpdated(Instant.now());
        examCheckRecordRepository.save(newRecord);
        return externalRef;
    }

    /** Builds the standard Paystack REST headers. */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getSecretKey());
        return headers;
    }

    /**
     * Validates the HMAC-SHA512 signature on an incoming Paystack webhook.
     * Paystack signs the raw request body with our secret key.
     */
    private boolean isValidSignature(String payload, String signature) {
        if (signature == null || signature.isBlank()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(config.getSecretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexHash = new StringBuilder();
            for (byte b : hash) {
                hexHash.append(String.format("%02x", b));
            }
            return hexHash.toString().equals(signature);
        } catch (Exception e) {
            log.error("HMAC signature verification failed: {}", e.getMessage(), e);
            return false;
        }
    }

    private void sendPaymentSuccessEmail(User user, double amount, String txId) {
        try {
            Map<String, Object> props = new HashMap<>();
            props.put("username", user.getFirstname() + " " + user.getLastname());
            props.put("amount", amount);
            props.put("transactionId", txId);

            emailService.sendEmail(
                    user.getUsername(),
                    EmailTemplateName.PAYMENT_CONFIRMATION,
                    props,
                    "Payment Confirmation - " + txId
            );
        } catch (Exception e) {
            log.error("Failed to send Paystack payment success email: {}", e.getMessage());
        }
    }

    private void sendPaymentSuccessSms(User user, double amount, String txId) {
        try {
            if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) return;
            String message = String.format(
                    "Dear %s, your payment of GHS %.2f was successful via Paystack. Transaction ID: %s. Thank you!",
                    user.getLastname(), amount, txId);
            mNotifyV2SmsService.sendSms(Collections.singletonList(user.getPhoneNumber()), message);
        } catch (Exception e) {
            log.error("Failed to send Paystack SMS notification: {}", e.getMessage());
        }
    }
}
