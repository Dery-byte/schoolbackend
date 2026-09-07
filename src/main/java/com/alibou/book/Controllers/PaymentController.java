package com.alibou.book.Controllers;

import com.alibou.book.DTO.MoolrePaymentRequest;
import com.alibou.book.DTO.PaymentInitiateResult;
import com.alibou.book.DTO.PaymentStatusRequest;
import com.alibou.book.Entity.MoolrePaymentResponse;
import com.alibou.book.Entity.PaymentStatuss;
import com.alibou.book.Entity.SubscriptionType;
import com.alibou.book.Repositories.PaymentStatusRepository;
import com.alibou.book.Services.MoolrePaymentService;
import com.alibou.book.Services.PaymentGatewayRouter;
import com.alibou.book.Services.PaystackPaymentService;
import com.alibou.book.exception.PaymentProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Handles all payment-related HTTP requests for authenticated users.
 *
 * <p>The active gateway (Moolre or Paystack) is determined at runtime by the
 * {@link PaymentGatewayRouter}, which reads the {@code ACTIVE_PAYMENT_GATEWAY}
 * system setting set by the admin.
 *
 * <ul>
 *   <li>{@code POST /initiate}        – initiates a payment (returns gateway-specific result)</li>
 *   <li>{@code POST /verify-otp}      – Moolre OTP verification (ignored for Paystack)</li>
 *   <li>{@code POST /statusWebhook}   – Moolre payment webhook</li>
 *   <li>{@code POST /paystackWebhook} – Paystack payment webhook</li>
 *   <li>{@code GET  /paystack/verify/{reference}} – frontend verifies a Paystack transaction</li>
 *   <li>{@code GET  /payment-status/{externalRef}} – query stored payment status</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/auth/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final MoolrePaymentService moolrePaymentService;
    private final PaystackPaymentService paystackPaymentService;
    private final PaymentGatewayRouter paymentGatewayRouter;
    private final PaymentStatusRepository paymentStatusRepository;
    private final ObjectMapper objectMapper;

    private static final Logger logger = Logger.getLogger(PaymentController.class.getName());

    // ─────────────────────────────────────────────────────────────────────────
    //  INITIATE  — delegates to the active gateway
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Initiates a payment for the logged-in user.
     *
     * <p><b>Moolre response</b> has {@code requiresOtp=true} and no {@code authorizationUrl}.
     * The frontend must show the OTP modal and call {@code /verify-otp} next.
     *
     * <p><b>Paystack response</b> has {@code requiresOtp=false} and an {@code authorizationUrl}
     * (plus {@code accessCode}). The frontend opens the Paystack popup inline.
     */
    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiateResult> initiatePayment(
            Principal principal,
            @RequestBody MoolrePaymentRequest request,
            @RequestParam(required = true) String recordId) {

        if (paymentGatewayRouter.isPaystackActive()) {
            // ── Paystack flow ──────────────────────────────────────────────
            SubscriptionType subType = request.getSubscriptionType();
            double amount = request.getAmount() != null ? request.getAmount() : 0;

            PaymentInitiateResult result = paystackPaymentService.initiatePayment(
                    principal, amount, subType, recordId, request.isUsedDiscountCode());
            return ResponseEntity.ok(result);

        } else {
            // ── Moolre flow (existing behaviour) ──────────────────────────
            try {
                MoolrePaymentResponse moolreResp =
                        moolrePaymentService.initiatePayment(principal, request, recordId);

                PaymentInitiateResult result = PaymentInitiateResult.builder()
                        .gatewayName("MOOLRE")
                        .externalRef(moolreResp.getExternalref())
                        .requiresOtp(true)
                        .status(moolreResp.getStatus())
                        .code(moolreResp.getCode())
                        .message(moolreResp.getMessage())
                        .userMessage(moolreResp.getUserMessage())
                        .moolreRequestId(moolreResp.getData())
                        .build();
                return ResponseEntity.ok(result);

            } catch (PaymentProcessingException e) {
                PaymentInitiateResult err = PaymentInitiateResult.builder()
                        .gatewayName("MOOLRE")
                        .status(0)
                        .code("ERROR")
                        .message(e.getMessage())
                        .userMessage("Payment initiation failed: " + e.getMessage())
                        .requiresOtp(false)
                        .build();
                return ResponseEntity.badRequest().body(err);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  OTP VERIFY  — Moolre only
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/verify-otp")
    public ResponseEntity<MoolrePaymentResponse> verifyOtpAndProceed(
            @RequestBody MoolrePaymentRequest request, Principal principal) {
        String username = principal.getName();
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
            return buildMoolreErrorResponse("OTP verification failed", e, username);
        } catch (Exception e) {
            return buildMoolreErrorResponse("An unexpected error occurred while verifying OTP", e, username);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MOOLRE WEBHOOK
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/statusWebhook")
    public ResponseEntity<Map<String, String>> handleMoolrePaymentStatus(
            @RequestBody(required = false) PaymentStatusRequest request, Principal principal) {

        logger.info("Received Moolre payment webhook: " + (request != null ? "Valid request" : "Null request"));

        if (request == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("status", "error", "message", "Invalid request: Request body is missing"));
        }
        if (request.getData() == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("status", "error", "message", "Invalid request: Missing data object"));
        }

        try {
            moolrePaymentService.processPaymentStatusRequest(request);
            Map<String, String> response = new HashMap<>();
            response.put("status", String.valueOf(request.getStatus()));
            response.put("message", request.getMessage());
            response.put("transactionId", request.getData().getTransactionid());
            response.put("received", "true");
            logger.info("Successfully processed Moolre webhook for transaction: " +
                    request.getData().getTransactionid());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.severe("Error processing Moolre payment webhook: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to process payment notification: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PAYSTACK WEBHOOK
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Receives Paystack webhook events (e.g. {@code charge.success}).
     *
     * <p>Paystack signs the raw body with HMAC-SHA512 using your secret key and puts
     * the hex digest in the {@code x-paystack-signature} header.  This endpoint
     * must be publicly accessible (no JWT required).
     */
    @PostMapping("/paystackWebhook")
    public ResponseEntity<Map<String, String>> handlePaystackWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "x-paystack-signature", required = false) String signature) {

        log.info("Received Paystack webhook event");

        try {
            paystackPaymentService.processWebhook(rawPayload, signature);
            return ResponseEntity.ok(Map.of("status", "received"));
        } catch (SecurityException se) {
            log.warn("Paystack webhook rejected — invalid signature");
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Invalid signature"));
        } catch (Exception e) {
            log.error("Error processing Paystack webhook: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PAYSTACK TRANSACTION VERIFY  — frontend calls this after popup closes
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies a Paystack transaction server-side by reference.
     * The frontend calls this after the Paystack popup fires {@code onSuccess}
     * to get a server-side confirmation before showing the success screen.
     */
    @GetMapping("/paystack/verify/{reference}")
    public ResponseEntity<Map<String, Object>> verifyPaystackTransaction(@PathVariable String reference) {
        try {
            JsonNode data = paystackPaymentService.verifyTransaction(reference);
            if (data == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("status", "error", "message", "Transaction could not be verified"));
            }
            String txStatus = data.path("status").asText("");
            Map<String, Object> result = new HashMap<>();
            result.put("status",        txStatus);
            result.put("reference",     reference);
            result.put("amount",        data.path("amount").asDouble(0) / 100.0);
            result.put("verified",      "success".equalsIgnoreCase(txStatus));
            // ── Fields shown on the success card ──────────────────────────
            result.put("transactionId", data.path("id").asText(""));          // Paystack numeric txn ID
            result.put("timestamp",     data.path("paid_at").asText(""));     // ISO-8601 e.g. 2026-09-07T13:00:00.000Z
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error verifying Paystack transaction {}: {}", reference, e.getMessage());
            return ResponseEntity.internalServerError().body(
                    Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PAYMENT STATUS QUERY
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/payment-status/{externalRef}")
    public ResponseEntity<PaymentStatuss> getStatus(@PathVariable String externalRef) {
        Optional<PaymentStatuss> status = paymentStatusRepository.findByExternalRef(externalRef);
        return status.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private ResponseEntity<MoolrePaymentResponse> buildMoolreErrorResponse(
            String message, Exception e, String username) {
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
}
