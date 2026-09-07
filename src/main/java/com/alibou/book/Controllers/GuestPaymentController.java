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
                    return ResponseEntity.ok(res);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
