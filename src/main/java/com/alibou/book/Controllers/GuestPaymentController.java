package com.alibou.book.Controllers;

import com.alibou.book.DTO.GuestPaymentInitiateRequest;
import com.alibou.book.DTO.GuestPaymentInitiateResponse;
import com.alibou.book.DTO.MoolrePaymentRequest;
import com.alibou.book.Entity.MoolrePaymentResponse;
import com.alibou.book.Entity.PaymentStatuss;
import com.alibou.book.Repositories.ExamCheckRecordRepository;
import com.alibou.book.Repositories.PaymentStatusRepository;
import com.alibou.book.Services.GuestPaymentService;
import com.alibou.book.Services.PaymentGatewayRouter;
import com.alibou.book.Services.PaystackPaymentService;
import com.alibou.book.config.PaystackConfig;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles payment endpoints for unauthenticated (guest) users.
 *
 * <p>The active gateway (Moolre or Paystack) is read from
 * {@link PaymentGatewayRouter} so the admin can switch at runtime.
 *
 * <ul>
 *   <li>{@code POST /guest/payment/initiate}          – start a payment (gateway-aware)</li>
 *   <li>{@code POST /guest/payment/verify-otp}        – Moolre OTP step (ignored for Paystack)</li>
 *   <li>{@code GET  /guest/payment/status/{ref}}      – query stored payment status by externalRef</li>
 *   <li>{@code GET  /guest/payment/session/{id}}      – query session progress</li>
 * </ul>
 */
@RestController
@RequestMapping("/guest/payment")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class GuestPaymentController {

    private final GuestPaymentService guestPaymentService;
    private final PaystackPaymentService paystackPaymentService;
    private final PaymentGatewayRouter paymentGatewayRouter;
    private final PaymentStatusRepository paymentStatusRepository;
    private final ExamCheckRecordRepository examCheckRecordRepository;
    private final PaystackConfig paystackConfig;

    // ─────────────────────────────────────────────────────────────────────────
    //  INITIATE  — gateway-aware
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Initiates a guest payment using the currently active gateway.
     *
     * <p>For <b>Moolre</b>: returns {@code sessionId}, {@code externalRef}, {@code recordId},
     * and a status code 1 indicating that an OTP will be sent to the guest's phone.
     * The frontend must follow up with {@code POST /verify-otp}.
     *
     * <p>For <b>Paystack</b>: returns {@code sessionId}, {@code externalRef}, {@code recordId},
     * and the Paystack {@code authorizationUrl} (in the {@code message} field) plus
     * {@code accessCode} (in the {@code userMessage} field).
     * The frontend opens the Paystack popup using those values.
     */
    @PostMapping("/initiate")
    public ResponseEntity<GuestPaymentInitiateResponse> initiateGuestPayment(
            @Valid @RequestBody GuestPaymentInitiateRequest request) {

        if (paymentGatewayRouter.isPaystackActive()) {
            return ResponseEntity.ok(paystackPaymentService.initiateGuestPayment(request));
        } else {
            return ResponseEntity.ok(guestPaymentService.initiateGuestPayment(request));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  OTP VERIFY  — Moolre only
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/verify-otp")
    public ResponseEntity<MoolrePaymentResponse> verifyGuestOtp(
            @RequestBody MoolrePaymentRequest request,
            @RequestParam String sessionId) {
        return ResponseEntity.ok(guestPaymentService.verifyGuestOtp(request, sessionId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STATUS QUERIES
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/status/{externalRef}")
    public ResponseEntity<PaymentStatuss> getGuestPaymentStatus(@PathVariable String externalRef) {
        return paymentStatusRepository.findByExternalRef(externalRef)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionProgress(@PathVariable String sessionId) {
        return examCheckRecordRepository.findBySessionId(sessionId)
                .map(record -> {
                    Map<String, Object> res = new LinkedHashMap<>();
                    res.put("sessionId", record.getSessionId());
                    res.put("externalRef", record.getExternalRef());
                    res.put("recordId", record.getId());
                    res.put("paymentStatus", record.getPaymentStatus());
                    res.put("checkStatus", record.getCheckStatus());
                    res.put("candidateName", record.getCandidateName());
                    res.put("biodataCompleted", record.getBiodata() != null);
                    return ResponseEntity.ok(res);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GATEWAY SETTINGS  — no auth required, used by the guest frontend
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the currently active payment gateway name.
     * GET /guest/payment/gateway
     * Response: { "gateway": "MOOLRE" | "PAYSTACK" }
     */
    @GetMapping("/gateway")
    public ResponseEntity<Map<String, String>> getActiveGateway() {
        String gateway = paymentGatewayRouter.isPaystackActive() ? "PAYSTACK" : "MOOLRE";
        return ResponseEntity.ok(Map.of("gateway", gateway));
    }

    /**
     * Returns the Paystack public key so the guest frontend can initialise the Paystack popup.
     * Only the public key is exposed — the secret key stays server-side.
     * GET /guest/payment/paystack-key
     * Response: { "publicKey": "pk_..." }
     */
    @GetMapping("/paystack-key")
    public ResponseEntity<Map<String, String>> getGuestPaystackPublicKey() {
        String pubKey = paystackConfig.getPublicKey() != null ? paystackConfig.getPublicKey() : "";
        return ResponseEntity.ok(Map.of("publicKey", pubKey));
    }

    /**
     * Verifies a Paystack transaction for a guest user after the popup fires onSuccess.
     * No authentication required — the externalRef / reference is the only secret needed.
     * GET /guest/payment/verify/{reference}
     * Response: { "verified": true/false, ... }
     */
    @GetMapping("/verify/{reference}")
    public ResponseEntity<Map<String, Object>> verifyGuestPaystackTransaction(
            @PathVariable String reference) {
        try {
            com.fasterxml.jackson.databind.JsonNode data = paystackPaymentService.verifyTransaction(reference);
            if (data == null) {
                return ResponseEntity.ok(Map.of("verified", false, "message", "Verification pending"));
            }
            String txStatus = data.path("status").asText("");
            boolean verified = "success".equalsIgnoreCase(txStatus);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("verified", verified);
            result.put("status", txStatus);
            result.put("reference", reference);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("verified", false, "message", "Verification failed"));
        }
    }
}
