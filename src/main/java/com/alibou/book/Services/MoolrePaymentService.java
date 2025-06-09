package com.alibou.book.Services;

import com.alibou.book.DTO.MoolrePaymentRequest;
import com.alibou.book.DTO.PaymentData;
import com.alibou.book.DTO.PaymentStatusRequest;
import com.alibou.book.Entity.MoolrePaymentResponse;
import com.alibou.book.Entity.Payment;
import com.alibou.book.Entity.PaymentStatuss;
import com.alibou.book.Repositories.PaymentRepository;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.mail.javamail.MimeMessageHelper.MULTIPART_MODE_MIXED;

@Service
@Slf4j
public class MoolrePaymentService {



    private final MoolreConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
   private final PaymentStatusRepository paymentStatusRepository;
    //private final SimpMessagingTemplate messagingTemplate;
    private static final Logger logger = Logger.getLogger(MoolrePaymentService.class.getName());
    //private final MailService mailService;
   // private final EmailService mailService;
  //  private final MailServiceImpl mailServiceImpl;
  private final JavaMailSender mailSender;


    private  PaymentData paymentData;
    private final PaymentRepository paymentRepository;
    private final UserDetailsService userDetailsService;
    private final SpringTemplateEngine templateEngine;
    private final MNotifyV2SmsService mNotifyV2SmsService;

    //private final SmsService smsService;



//    @Value("${moolre.webhook.secret}") // Load the secret from application.properties
//    private String webhookSecret;

    public User user;


    // Thread-safe storage for external references mapped to user IDs (or phone numbers)
    private final Map<String, String> userPaymentReferences = new ConcurrentHashMap<>();
    public MoolrePaymentService(MoolreConfig config, RestTemplate restTemplate, ObjectMapper objectMapper, PaymentStatusRepository paymentStatusRepository, JavaMailSender mailSender, PaymentRepository paymentRepository, UserDetailsService userDetailsService, EmailService mailService1, SpringTemplateEngine templateEngine, MNotifyV2SmsService mNotifyV2SmsService) {
        this.config = config;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.paymentStatusRepository = paymentStatusRepository;
        this.mailSender = mailSender;
        this.paymentRepository = paymentRepository;
        this.userDetailsService = userDetailsService;
        this.templateEngine = templateEngine;
        this.mNotifyV2SmsService = mNotifyV2SmsService;
    }

    /**
     * Initiates a payment request and stores the external reference in the map.
     */
//
//    public MoolrePaymentResponse initiatePayment(Principal principal, MoolrePaymentRequest request) {
//        User user = (User) userDetailsService.loadUserByUsername(principal.getName());
//
//        HttpHeaders headers = createHeaders();
//        request.setAccountnumber(config.getAccountNumber());
//        request.setCurrency("GHS");
//        request.setType(1);
//
//        // Generate unique external reference
//        String externalRef = generateReference();
//        request.setExternalref(externalRef);
//
//        System.out.println("For the webhook " + generateReference());
////        Save the externalRef in the Payment Entity
//        Payment payment= new Payment();
//        payment.setExternalRef(externalRef);
//        payment.setUser(user);
//        System.out.println(" externalRef Save: " + externalRef);
//        paymentRepository.save(payment);
//
//
//        // Store externalRef for this user
//        userPaymentReferences.put(principal.getName(), externalRef);
//        log.info("Payment initiated for User: {} with External Ref: {}", principal.getName(), externalRef);
//
//
//
//
//        HttpEntity<MoolrePaymentRequest> entity = new HttpEntity<>(request, headers);
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
//            // Log and return appropriate messages
//            if (paymentResponse.getStatus() == 1) {
//                if ("TP14".equals(paymentResponse.getCode())) {
//                    log.info("Verification required: {}", paymentResponse.getMessage());
//                    paymentResponse.setUserMessage("A verification code has been sent to your phone. Please complete the verification to proceed.");
//                } else {
//                    log.info("Payment initiated successfully: {}", paymentResponse.getMessage());
//                    paymentResponse.setUserMessage("Payment initiated successfully. Please wait for confirmation.");
//                }
//            } else {
//                log.warn("Payment initiation failed: {}", paymentResponse.getMessage());
//                paymentResponse.setUserMessage("Payment failed: " + paymentResponse.getMessage());
//            }
//
//            return paymentResponse;
//        } catch (Exception e) {
//            log.error("Payment initiation failed: {}", e.getMessage(), e);
//            throw new PaymentProcessingException("Failed to process payment. Please try again later.", e);
//        }
//
//
//
//    }




    public MoolrePaymentResponse initiatePayment(Principal principal, MoolrePaymentRequest request) {
        User user = (User) userDetailsService.loadUserByUsername(principal.getName());

        this.user = (User) userDetailsService.loadUserByUsername(principal.getName());
        HttpHeaders headers = createHeaders();
        request.setAccountnumber(config.getAccountNumber());
        request.setCurrency("GHS");
        request.setType(1);

        // Generate unique external reference
        String externalRef = generateReference();
        request.setExternalref(externalRef);

        System.out.println("For the webhook " + externalRef);

        // Save the externalRef in the Payment Entity
        Payment payment = new Payment();
        payment.setExternalRef(externalRef);
        payment.setUser(user);
        System.out.println("ExternalRef Saved: " + externalRef);
        paymentRepository.save(payment);

        // Store externalRef for this user
        userPaymentReferences.put(principal.getName(), externalRef);
        log.info("Payment initiated for User: {} with External Ref: {}", principal.getName(), externalRef);

        HttpEntity<MoolrePaymentRequest> entity = new HttpEntity<>(request, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    config.getApiUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.debug("Raw response: {}", response.getBody());
            MoolrePaymentResponse paymentResponse = objectMapper.readValue(response.getBody(), MoolrePaymentResponse.class);
            paymentResponse.setExternalref(externalRef); // Ensure externalRef is included in the response
            // Log and return appropriate messages
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
            return paymentResponse;
        } catch (Exception e) {
            log.error("Payment initiation failed: {}", e.getMessage(), e);
            throw new PaymentProcessingException("Failed to process payment. Please try again later.", e);
        }
    }








    /**
     * Verifies OTP and retrieves the stored external reference before proceeding.
     */

    public MoolrePaymentResponse verifyOtpAndProceed(Principal principal, MoolrePaymentRequest request) {
        String externalRef = userPaymentReferences.get(principal.getName());
        request.setAccountnumber(config.getAccountNumber());
        request.setCurrency("GHS");
        request.setType(1);
        request.setReference("Optimus");

        System.out.println("This is the externalRef " + externalRef);

        if (externalRef == null) {
            log.error("No stored External Reference for User: {}", principal.getName());
            throw new PaymentProcessingException("No External Reference found for OTP verification.");
        }

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
                throw new PaymentProcessingException("OTP verification failed. " +
                        (verificationResponse.getMessage() != null ? verificationResponse.getMessage() : "Please try again."));
            }
        } catch (Exception e) {
            log.error("OTP verification error: {}", e.getMessage(), e);
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

            // Handle response message for UI
            if (paymentResponse.getStatus() == 1 && "TR099".equals(paymentResponse.getCode())) {
                log.info("Payment successful for External Ref: {}", request.getExternalref());
                paymentResponse.setMessage(paymentResponse.getMessage() != null ? paymentResponse.getMessage() :
                        "Payment Initiated kindly Complete the Process on your Phone. " + paymentResponse.getData());
            } else {
                log.warn("Payment failed for External Ref: {}. Code: {}, Message: {}", request.getExternalref(),
                        paymentResponse.getCode(), paymentResponse.getMessage());
                throw new PaymentProcessingException("Payment failed. " +
                        (paymentResponse.getMessage() != null ? paymentResponse.getMessage() : "Please contact support."));
            }
            return paymentResponse;
        } catch (Exception e) {
            log.error("Error triggering payment: {}", e.getMessage(), e);
            throw new PaymentProcessingException("Failed to process payment. Please try again later.", e);
        }
    }





    /**
     * Sends SMS notification for successful payment.
     */
    private void sendPaymentSuccessNotification(PaymentData paymentData) {
        if (paymentData == null) {
            logger.warning("PaymentData is null. Cannot send SMS notification.");
            return;
        }

        String phoneNumber = paymentData.getPayer(); // Get recipient phone number
        String amount = String.format("%.2f", paymentData.getAmount());
        String transactionId = paymentData.getTransactionid();

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            logger.warning("User phone number is missing. Unable to send SMS.");
            return;
        }
        String senderId = "Optimus";
        String message = String.format("Dear Customer, your payment of GHS %s was successful. Transaction ID: %s. Thank you!",
                amount, transactionId);
        try {
            // Call SmsService to send SMS
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
    private String generateReference () {
        return UUID.randomUUID().toString();
    }































    @Transactional
    public void processPaymentStatusRequest(PaymentStatusRequest paymentStatusRequest) {
        if (paymentStatusRequest == null || paymentStatusRequest.getData() == null) {
            throw new IllegalArgumentException("Payment status request or data is null");
        }
//        User user = (User) userDetailsService.loadUserByUsername(principal.getName());
        PaymentData paymentData = paymentStatusRequest.getData();
        logger.info("Processing payment webhook. Status: " + paymentStatusRequest.getStatus() +
                ", Transaction ID: " + paymentData.getTransactionid());
        // Validate webhook secret (commented out for now but should be implemented)
        validateWebhookSecret(paymentData.getSecret());


        // Map webhook data to domain model
        PaymentStatuss paymentStatus = mapToPaymentStatus(paymentStatusRequest);
        // Save to database
        paymentStatusRepository.save(paymentStatus);
        sendPaymentSuccessEmail(paymentData); // SEND EMAIL NOTIFICATION
        sendPaymentSuccessNotification(paymentData); // SEND SMS NOTIFICATION

        // Notify UI through WebSockets
//        messagingTemplate.convertAndSend("/topic/paymentStatus", paymentStatus);
//        logger.info("Payment status processed and saved successfully for transaction: " +
//                paymentData.getTransactionid());
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

        // Set additional information from the request header
//        paymentStatus.setStatusCode(request.getCode());
//        paymentStatus.setStatusMessage(request.getMessage());

        return paymentStatus;
    }

    /**
     * Validates the webhook secret
     */
    private void validateWebhookSecret(String secret) {
        // Uncomment this in production
        /*
        if (secret == null || !webhookSecret.equals(secret)) {
            logger.severe("Invalid webhook secret received");
            throw new SecurityException("Invalid webhook secret");
        }
        */
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

            // Prepare variables for the template
            Map<String, Object> properties = new HashMap<>();
            properties.put("username", user.fullName());  // or user.getFullName() if that's the method
            properties.put("amount", paymentData.getAmount());
            properties.put("transactionId", paymentData.getTransactionid());

            // Inject variables into the Thymeleaf template
            Context context = new Context();
            context.setVariables(properties);

            // Use enum for template name
            String htmlContent = templateEngine.process(EmailTemplateName.PAYMENT_CONFIRMATION.getName(), context);

            // Set email metadata
            helper.setFrom("optimusinforservice@gmail.com");
            helper.setTo(user.getUsername());  // Consider renaming this field to getEmail() for clarity
            helper.setSubject("Payment Confirmation - " + paymentData.getTransactionid());
            helper.setText(htmlContent, true);

            // Send the email
            mailSender.send(mimeMessage);
            logger.info("Payment success email sent to: " + user.getUsername());

        } catch (Exception e) {
            logger.severe("Error sending payment success email: " + e.getMessage());
            e.printStackTrace();
        }
    }





}

