package com.alibou.book.Controllers;

import com.alibou.book.DTO.MoolrePaymentRequest;
import com.alibou.book.DTO.PaymentStatusRequest;
import com.alibou.book.Entity.MoolrePaymentResponse;
import com.alibou.book.Entity.PaymentStatuss;
import com.alibou.book.Repositories.PaymentStatusRepository;
import com.alibou.book.Services.MoolrePaymentService;
import com.alibou.book.exception.PaymentProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import lombok.extern.slf4j.Slf4j;



@Slf4j
@RestController
@RequestMapping("/auth/payments")
@RequiredArgsConstructor
public class PaymentController {

private final MoolrePaymentService moolrePaymentService;
    private static final Logger logger = Logger.getLogger(MoolrePaymentService.class.getName());
    private final PaymentStatusRepository paymentStatusRepository;


//    @PostMapping("/initiate")
//    public ResponseEntity<MoolrePaymentResponse> initiatePayment(
//            Principal principal,
//            @RequestBody MoolrePaymentRequest request
//    ) {
//        MoolrePaymentResponse response = moolrePaymentService.initiatePayment(principal, request);
//        return ResponseEntity.ok(response);
//    }

    @PostMapping("/initiate")
    public ResponseEntity<MoolrePaymentResponse> initiatePayment(
            Principal principal,
            @RequestBody MoolrePaymentRequest request,
            @RequestParam(required = true) String recordId  // Optional parameter
    ) {
        MoolrePaymentResponse response = moolrePaymentService.initiatePayment(principal, request, String.valueOf(recordId));
        return ResponseEntity.ok(response);
    }







    @PostMapping("/verify-otp")
    public ResponseEntity<MoolrePaymentResponse> verifyOtpAndProceed(
            @RequestBody MoolrePaymentRequest request, Principal principal) {
        String username = principal.getName(); // Get logged-in username
        log.info("Verifying OTP for User: {}", username);
        try {
            log.debug("OTP verification request: {}", new ObjectMapper().writeValueAsString(request));
        } catch (Exception e) {
            log.warn("Failed to log OTP verification request body", e);
        }
        try {
            MoolrePaymentResponse response = moolrePaymentService.verifyOtpAndProceed(principal, request);
            if (response.getStatus() == 1) {
                log.info("OTP verified successfully for User: {}. Payment proceeding.", username);
            } else {
                log.warn("OTP verification failed for User: {}. Code: {}", username, response.getCode());
            }
            return ResponseEntity.ok(response);
        } catch (PaymentProcessingException e) {
            return buildErrorResponse("OTP verification failed", e, username);
        } catch (Exception e) {
            return buildErrorResponse("An unexpected error occurred while verifying OTP", e, username);
        }
    }



    private ResponseEntity<MoolrePaymentResponse> buildErrorResponse(String message, Exception e, String username) {
        log.error("{} for User: {}", message, username, e);
        MoolrePaymentResponse errorResponse = MoolrePaymentResponse.builder()
                .status(0)
                .code("ERROR")
                .message(message + ": " + e.getMessage())
                .go(null)
                .data(null)
                .build();
        return ResponseEntity.badRequest().body(errorResponse);
    }
















    //    WEBHOOK CONTROLLER

    @PostMapping("/statusWebhook")
    //@CrossOrigin(origins = "https://optimus-4fb5d.web.app")
    public ResponseEntity<Map<String, String>> handlePaymentStatus(
            @RequestBody(required = false) PaymentStatusRequest request, Principal principal) {

        logger.info("Received payment webhook: " + (request != null ? "Valid request" : "Null request"));

        // Validate request
        if (request == null) {
            logger.warning("Webhook received with null request body");
            return ResponseEntity.badRequest().body(
                    Map.of("status", "error", "message", "Invalid request: Request body is missing"));
        }

        if (request.getData() == null) {
            logger.warning("Webhook received with null data object");
            return ResponseEntity.badRequest().body(
                    Map.of("status", "error", "message", "Invalid request: Missing data object"));
        }

        try {
            // Process the payment status
            moolrePaymentService.processPaymentStatusRequest(request);
            // Prepare success response
            Map<String, String> response = new HashMap<>();
            response.put("status", String.valueOf(request.getStatus()));
            response.put("message", request.getMessage());
            response.put("transactionId", request.getData().getTransactionid());
            response.put("received", "true");

            logger.info("Successfully processed payment webhook for transaction: " +
                    request.getData().getTransactionid());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Log the error
            logger.severe("Error processing payment webhook: " + e.getMessage());

            // Return error response
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to process payment notification: " + e.getMessage()
            ));
        }
    }



    @GetMapping("/payment-status/{externalRef}")
    public ResponseEntity<PaymentStatuss> getStatus(@PathVariable String externalRef) {
        Optional<PaymentStatuss> status = paymentStatusRepository.findByExternalRef(externalRef);
        return status.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }











}


