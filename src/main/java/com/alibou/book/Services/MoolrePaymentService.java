package com.alibou.book.Services;

import com.alibou.book.DTO.MoolrePaymentRequest;
import com.alibou.book.DTO.PaymentData;
import com.alibou.book.DTO.PaymentStatusRequest;
import com.alibou.book.Entity.*;
import com.alibou.book.Repositories.ExamCheckRecordRepository;
import com.alibou.book.Repositories.PaymentStatusRepository;
import com.alibou.book.config.MoolreConfig;
import com.alibou.book.email.EmailService;
import com.alibou.book.email.EmailTemplateName;
import com.alibou.book.exception.PaymentProcessingException;
import com.alibou.book.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hibernate.internal.CoreLogging.logger;
import static org.springframework.mail.javamail.MimeMessageHelper.MULTIPART_MODE_MIXED;

@Service
@Slf4j
public class MoolrePaymentService {

    private final MoolreConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PaymentStatusRepository paymentStatusRepository;
    private static final Logger logger = Logger.getLogger(MoolrePaymentService.class.getName());
    private final JavaMailSender mailSender;
    private final ExamCheckRecordRepository examCheckRecordRepository;
    private PaymentData paymentData;
    private final UserDetailsService userDetailsService;
    private final SpringTemplateEngine templateEngine;
    private final MNotifyV2SmsService mNotifyV2SmsService;

    public User user;

    // Thread-safe storage for external references mapped to user IDs (or phone numbers)
    private final Map<String, String> userPaymentReferences = new ConcurrentHashMap<>();

    public MoolrePaymentService(MoolreConfig config, RestTemplate restTemplate, ObjectMapper objectMapper,
                                PaymentStatusRepository paymentStatusRepository, JavaMailSender mailSender,
                                UserDetailsService userDetailsService, EmailService mailService1,
                                SpringTemplateEngine templateEngine, MNotifyV2SmsService mNotifyV2SmsService,
                                ExamCheckRecordRepository examCheckRecordRepository) {
        this.config = config;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.paymentStatusRepository = paymentStatusRepository;
        this.mailSender = mailSender;
        this.userDetailsService = userDetailsService;
        this.templateEngine = templateEngine;
        this.mNotifyV2SmsService = mNotifyV2SmsService;
        this.examCheckRecordRepository = examCheckRecordRepository;
    }

    /**
     * Initiates a payment request and manages ExamCheckRecord creation/reuse.
     * Prevents duplicate external references for the same user's pending payments.
     */
//    @Transactional
//    public MoolrePaymentResponse initiatePayment(Principal principal, MoolrePaymentRequest request, String recordId) {
//        User user = (User) userDetailsService.loadUserByUsername(principal.getName());
//        this.user = user;
//        String externalRef = getOrCreateExternalReference(user, recordId);
//
//        HttpHeaders headers = createHeaders();
//        request.setAccountnumber(config.getAccountNumber());
//        request.setCurrency("GHS");
//        request.setType(1);
//        request.setReference("Optimus");
//
//        if (externalRef == null || externalRef.isEmpty()) {
//            throw new PaymentProcessingException("Failed to generate payment reference");
//        }
//        request.setExternalref(externalRef);
//
//        log.info("Payment initiated for User: {} with External Ref: {}", principal.getName(), externalRef);
//        System.out.println("For the webhook " + externalRef);
//
//        System.out.print("The resquest data for the OTP " + request);
//        // Store externalRef for this user session
//        userPaymentReferences.put(principal.getName(), externalRef);
//
//        HttpEntity<MoolrePaymentRequest> entity = new HttpEntity<>(request, headers);
//
//        try {
//            ResponseEntity<String> response = restTemplate.exchange(
//                    config.getApiUrl(),
//                    HttpMethod.POST,
//                    entity,
//                    String.class
//            );
//
//            log.debug("Raw response: {}", response.getBody());
//            MoolrePaymentResponse paymentResponse = objectMapper.readValue(response.getBody(), MoolrePaymentResponse.class);
//            paymentResponse.setExternalref(externalRef);
//
//            // Handle response messages
//            handlePaymentResponse(paymentResponse);
//
//            return paymentResponse;
//
//        } catch (Exception e) {
//            // Mark the payment as failed if payment initiation fails
//            updateExamCheckRecordStatus(externalRef, PaymentStatus.FAILED);
//            log.error("Payment initiation failed: {}", e.getMessage(), e);
//            throw new PaymentProcessingException("Failed to process payment. Please try again later.", e);
//        }
//    }


@Transactional
public MoolrePaymentResponse initiatePayment(Principal principal, MoolrePaymentRequest request, String recordId) {
//    if (principal == null) {
//        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//    }
//
    User user = (User) userDetailsService.loadUserByUsername(principal.getName());
    this.user = user;
    String externalRef = getOrCreateExternalReference(user, recordId);

    HttpHeaders headers = createHeaders();
    request.setAccountnumber(config.getAccountNumber());
    request.setCurrency("GHS");
    request.setType(1);
    request.setReference("Optimus");

    if (externalRef == null || externalRef.isEmpty()) {
        throw new PaymentProcessingException("Failed to generate payment reference");
    }
    request.setExternalref(externalRef);

    // ✅ Save pending subscription type if provided
    if (request.getSubscriptionType() != null) {
        examCheckRecordRepository.findByExternalRef(externalRef)
                .ifPresent(record -> {
                    record.setPendingSubscriptionType(request.getSubscriptionType().name());
                    record.setLastUpdated(Instant.now());
                    examCheckRecordRepository.save(record);
                });
    }

    log.info("Payment initiated for User: {} with External Ref: {}", principal.getName(), externalRef);
    System.out.println("For the webhook " + externalRef);
    System.out.print("The request data for the OTP " + request);

    // Store externalRef for this user session
    userPaymentReferences.put(principal.getName(), externalRef);

    HttpEntity<MoolrePaymentRequest> entity = new HttpEntity<>(request, headers);

    try {
        ResponseEntity<String> response = restTemplate.exchange(
                config.getApiUrl(),
                HttpMethod.POST,
                entity,
                String.class
        );

        log.debug("Raw response: {}", response.getBody());
        MoolrePaymentResponse paymentResponse =
                objectMapper.readValue(response.getBody(), MoolrePaymentResponse.class);
        paymentResponse.setExternalref(externalRef);

        // Handle response messages
        handlePaymentResponse(paymentResponse);

        return paymentResponse;

    } catch (Exception e) {
        // Mark the payment as failed if payment initiation fails
        updateExamCheckRecordStatus(externalRef, PaymentStatus.FAILED);
        log.error("Payment initiation failed: {}", e.getMessage(), e);
        throw new PaymentProcessingException("Failed to process payment. Please try again later.", e);
    }
}

    /**
     * Gets existing pending ExamCheckRecord or creates a new one.
     * This prevents duplicate external references for the same user.
     */
    private String getOrCreateExternalReference(User user, String recordId) {
        if (recordId != null) {
            Optional<ExamCheckRecord> recordOpt = examCheckRecordRepository.findById(String.valueOf(recordId));
            if (recordOpt.isPresent()) {
                ExamCheckRecord record = recordOpt.get();
                if (record.getPaymentStatus() == PaymentStatus.PENDING &&
                        record.getUser().getId().equals(user.getId())
//                        record.getUserId().equals(String.valueOf(user.getId()))

                ) {

                    // Generate new reference only when updating existing record
                    String externalRef = generateReference();
                    record.setExternalRef(externalRef);
                    record.setLastUpdated(Instant.now());
//                    record.setPendingSubscriptionType(subscriptionType); // ✅ temp storage
                    examCheckRecordRepository.save(record);
                    return externalRef;
                }
            }
        }

        // Generate new reference for new record
        String externalRef = generateReference();
        ExamCheckRecord newRecord = new ExamCheckRecord();
        newRecord.setUser(user);
//        newRecord.setUserId(String.valueOf(user.getId()));
        newRecord.setExternalRef(externalRef);
        newRecord.setPaymentStatus(PaymentStatus.PENDING);
//        newRecord.setPendingSubscriptionType(subscriptionType); // ✅ temp storage
        examCheckRecordRepository.save(newRecord);
        return externalRef;
    }

    /**
     * Creates a new ExamCheckRecord with the provided external reference.
     */
    private ExamCheckRecord createNewExamCheckRecord(User user, String externalRef) {
        ExamCheckRecord record = new ExamCheckRecord();
        record.setUser(user);
//        record.setUserId(String.valueOf(user.getId()));
        record.setExternalRef(externalRef);
        record.setPaymentStatus(PaymentStatus.PENDING);
        record.setCreatedAt(Instant.now());
        record.setLastUpdated(Instant.now());
        return record;
    }

    /**
     * Updates the payment status of an ExamCheckRecord.
     */
    private void updateExamCheckRecordStatus(String externalRef, PaymentStatus status) {
        examCheckRecordRepository.findByExternalRef(externalRef)
                .ifPresent(record -> {
                    record.setPaymentStatus(status);
                    record.setLastUpdated(Instant.now());
                    examCheckRecordRepository.save(record);
                    log.info("Updated ExamCheckRecord with externalRef {} to status {}", externalRef, status);
                });
    }

    /**
     * Handles payment response messages for UI feedback.
     */
    private void handlePaymentResponse(MoolrePaymentResponse paymentResponse) {
        if (paymentResponse.getStatus() == 1) {
            if ("TP14".equals(paymentResponse.getCode())) {
                log.info("Verification required: {}", paymentResponse.getMessage());
                paymentResponse.setUserMessage("A verification code has been sent to your phone. Please complete the verification to proceed.");
            } else {
                log.info("Payment initiated successfully: {}", paymentResponse.getMessage());
                paymentResponse.setUserMessage("Payment initiated successfully. Please wait for confirmation.");
            }
        } else {
            log.warn("Payment initiation failed: {}", paymentResponse.getMessage());
            paymentResponse.setUserMessage("Payment failed: " + paymentResponse.getMessage());
        }
    }

    /**
     * Verifies OTP and retrieves the stored external reference before proceeding.
     */
    public MoolrePaymentResponse verifyOtpAndProceed(Principal principal, MoolrePaymentRequest request) {
        String externalRef = userPaymentReferences.get(principal.getName());

        if (externalRef == null) {
            log.error("No stored External Reference for User: {}", principal.getName());
            throw new PaymentProcessingException("No External Reference found for OTP verification.");
        }

        request.setAccountnumber(config.getAccountNumber());
        request.setCurrency("GHS");
        request.setType(1);
        request.setReference("Optimus");
        request.setExternalref(externalRef);

        log.info("Verifying OTP for User: {} with External Ref: {}", principal.getName(), externalRef);

        HttpHeaders headers = createHeaders();
        HttpEntity<MoolrePaymentRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    config.getApiUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.debug("OTP verification response: {}", response.getBody());
            MoolrePaymentResponse verificationResponse = objectMapper.readValue(response.getBody(), MoolrePaymentResponse.class);

            if (verificationResponse.getStatus() == 1) {
                log.info("OTP verified successfully for User: {}", principal.getName());
                return triggerPayment(request);
            } else {
                log.warn("OTP verification failed for User: {}. Reason: {}", principal.getName(),
                        verificationResponse.getMessage() != null ? verificationResponse.getMessage() : "Unknown reason.");
                // Update status to failed on OTP verification failure
                updateExamCheckRecordStatus(externalRef, PaymentStatus.FAILED);
                throw new PaymentProcessingException("OTP verification failed. " +
                        (verificationResponse.getMessage() != null ? verificationResponse.getMessage() : "Please try again."));
            }
        } catch (Exception e) {
            log.error("OTP verification error: {}", e.getMessage(), e);
            updateExamCheckRecordStatus(externalRef, PaymentStatus.FAILED);
            throw new PaymentProcessingException("Failed to verify OTP. Please try again later.", e);
        }
    }

    /**
     * Triggers payment after OTP verification.
     */
    private MoolrePaymentResponse triggerPayment(MoolrePaymentRequest request) {
        log.info("Triggering payment for External Ref: {}", request.getExternalref());
        HttpHeaders headers = createHeaders();
        HttpEntity<MoolrePaymentRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    config.getApiUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.debug("Payment trigger response: {}", response.getBody());
            MoolrePaymentResponse paymentResponse = objectMapper.readValue(response.getBody(), MoolrePaymentResponse.class);

            if (paymentResponse.getStatus() == 1 && "TR099".equals(paymentResponse.getCode())) {
                log.info("Payment successful for External Ref: {}", request.getExternalref());
                paymentResponse.setMessage(paymentResponse.getMessage() != null ? paymentResponse.getMessage() :
                        "Payment Initiated kindly Complete the Process on your Phone. " + paymentResponse.getData());
            } else {
                log.warn("Payment failed for External Ref: {}. Code: {}, Message: {}", request.getExternalref(),
                        paymentResponse.getCode(), paymentResponse.getMessage());
                updateExamCheckRecordStatus(request.getExternalref(), PaymentStatus.FAILED);
                throw new PaymentProcessingException("Payment failed. " +
                        (paymentResponse.getMessage() != null ? paymentResponse.getMessage() : "Please contact support."));
            }
            return paymentResponse;
        } catch (Exception e) {
            log.error("Error triggering payment: {}", e.getMessage(), e);
            updateExamCheckRecordStatus(request.getExternalref(), PaymentStatus.FAILED);
            throw new PaymentProcessingException("Failed to process payment. Please try again later.", e);
        }
    }

    /**
     * Processes webhook payment status updates.
     * Updates the corresponding ExamCheckRecord's PaymentStatus field.
     */
    @Transactional
    public void processPaymentStatusRequest(PaymentStatusRequest paymentStatusRequest) {
        if (paymentStatusRequest == null || paymentStatusRequest.getData() == null) {
            throw new IllegalArgumentException("Payment status request or data is null");
        }

        PaymentData paymentData = paymentStatusRequest.getData();
        logger.info("Processing payment webhook. Status: " + paymentStatusRequest.getStatus() +
                ", Transaction ID: " + paymentData.getTransactionid());

        // Validate webhook secret
        validateWebhookSecret(paymentData.getSecret());

        // Save webhook data to PaymentStatus table
        PaymentStatuss paymentStatus = mapToPaymentStatus(paymentStatusRequest);
        paymentStatusRepository.save(paymentStatus);

        // Update ExamCheckRecord based on webhook response
        updateExamCheckRecordFromWebhook(paymentData);

        // Send notifications if payment was successful
        if (paymentData.getTxstatus() == 1) {
            sendPaymentSuccessEmail(paymentData);
            sendPaymentSuccessNotification(paymentData);
        }

        logger.info("Payment status processed and saved successfully for transaction: " + paymentData.getTransactionid());
    }

    /**
     * Updates ExamCheckRecord based on webhook payment data.
     */
    private void updateExamCheckRecordFromWebhook(PaymentData paymentData) {
        examCheckRecordRepository.findByExternalRef(paymentData.getExternalref())
                .ifPresentOrElse(
                        record -> {
                            PaymentStatus newStatus = determinePaymentStatus(paymentData.getTxstatus());
                            record.setPaymentStatus(newStatus);
                            record.setLastUpdated(Instant.now());
                            record.setCheckStatus(CheckStatus.IN_PROGRESS);
//                            record.setSubscriptionType(paymentData.getSubscriptionType());
                            record.setSubscriptionType(SubscriptionType.valueOf(record.getPendingSubscriptionType()));
                            record.setPendingSubscriptionType(null); // clear temp
                            System.out.println(paymentData);
                            // Additional fields can be updated here if needed
                            // record.setTransactionId(paymentData.getTransactionid());
                            // record.setAmount(paymentData.getAmount());

                            examCheckRecordRepository.save(record);
                            logger.info("Updated ExamCheckRecord {} to status {} for externalRef {}"
                            );
                        },
                        () -> logger.warning("No ExamCheckRecord found for externalRef: " + paymentData.getExternalref())
                );
    }

    /**
     * Determines PaymentStatus based on transaction status from webhook.
     */
    private PaymentStatus determinePaymentStatus(int txStatus) {
        return txStatus == 1 ? PaymentStatus.PAID : PaymentStatus.FAILED;
    }

    /**
     * Sends SMS notification for successful payment.
     */
    private void sendPaymentSuccessNotification(PaymentData paymentData) {

        if (user == null || user.getUsername() == null) {
            logger.warning("User or email is null. Cannot send email.");
            return;
        }

        if (paymentData == null) {
            logger.warning("PaymentData is null. Cannot send SMS notification.");
            return;
        }

        String phoneNumber = paymentData.getPayer();
        String amount = String.format("%.2f", paymentData.getAmount());
        String transactionId = paymentData.getTransactionid();
        //String username = user.getUsername();
        String lastName = user.getLastname();

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            logger.warning("User phone number is missing. Unable to send SMS.");
            return;
        }

        String message = String.format("Dear %s, your payment of GHS %s was successful. Transaction ID: %s. Thank you!",
                lastName, amount, transactionId);

        try {
            String response = mNotifyV2SmsService.sendSms(Collections.singletonList(phoneNumber), message);
            logger.info("SMS Notification sent to " + phoneNumber + ". Response: " + response);
        } catch (Exception e) {
            logger.severe("Failed to send SMS notification: " + e.getMessage());
        }
    }

    /**
     * Creates HTTP headers for requests.
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-USER", config.getUsername());
        headers.set("X-API-PUBKEY", config.getPublicKey());
        return headers;
    }

    /**
     * Generates a unique external reference.
     */
    private String generateReference() {
        return UUID.randomUUID().toString();
    }

    /**
     * Maps the webhook request to the domain model
     */
    private PaymentStatuss mapToPaymentStatus(PaymentStatusRequest request) {
        PaymentData data = request.getData();
        PaymentStatuss paymentStatus = new PaymentStatuss();
        paymentStatus.setTxStatus(data.getTxstatus());
        paymentStatus.setPayer(data.getPayer());
        paymentStatus.setPayee(data.getAccountnumber());
        paymentStatus.setAmount(data.getAmount());
        paymentStatus.setValue(data.getValue());
        paymentStatus.setTransactionId(parseLongSafely(data.getTransactionid()));
        paymentStatus.setExternalRef(data.getExternalref());
        paymentStatus.setThirdPartyRef(data.getThirdpartyref());
        paymentStatus.setTimestamp(data.getTs());

        Optional<ExamCheckRecord> optionalOrder = examCheckRecordRepository.findByExternalRef(data.getExternalref());
        ExamCheckRecord record = optionalOrder.orElseThrow(() ->
                new IllegalStateException("No order found for externalRef: " + data.getExternalref())
        );

        paymentStatus.setUser(record.getUser());

        return paymentStatus;
    }

    /**
     * Validates the webhook secret
     */
    private void validateWebhookSecret(String secret) {
        // Implement webhook secret validation in production
        // if (secret == null || !webhookSecret.equals(secret)) {
        //     logger.severe("Invalid webhook secret received");
        //     throw new SecurityException("Invalid webhook secret");
        // }
    }

    /**
     * Safely parse a string to long with error handling
     */
    private Long parseLongSafely(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            logger.warning("Failed to parse transaction ID: " + value);
            return null;
        }
    }

    /**
     * Sends payment success email notification.
     */
    private void sendPaymentSuccessEmail(PaymentData paymentData) {
        if (user == null || user.getUsername() == null) {
            logger.warning("User or email is null. Cannot send email.");
            return;
        }

        if (paymentData.getTxstatus() != 1) {
            logger.info("Payment was not successful. No email sent.");
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED,
                    StandardCharsets.UTF_8.name()
            );

            Map<String, Object> properties = new HashMap<>();
            properties.put("username", user.fullName());
            properties.put("amount", paymentData.getAmount());
            properties.put("transactionId", paymentData.getTransactionid());

            Context context = new Context();
            context.setVariables(properties);

            String htmlContent = templateEngine.process(EmailTemplateName.PAYMENT_CONFIRMATION.getName(), context);

            helper.setFrom("optimusinforservice@gmail.com");
            helper.setTo(user.getUsername());
            helper.setSubject("Payment Confirmation - " + paymentData.getTransactionid());
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            logger.info("Payment success email sent to: " + user.getUsername());

        } catch (Exception e) {
            logger.severe("Error sending payment success email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}