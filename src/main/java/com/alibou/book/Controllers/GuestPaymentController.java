package com.alibou.book.Controllers;

import com.alibou.book.DTO.GuestPaymentInitiateRequest;
import com.alibou.book.DTO.GuestPaymentInitiateResponse;
import com.alibou.book.DTO.MoolrePaymentRequest;
import com.alibou.book.Entity.MoolrePaymentResponse;
import com.alibou.book.Entity.PaymentStatuss;
import com.alibou.book.Repositories.ExamCheckRecordRepository;
import com.alibou.book.Repositories.PaymentStatusRepository;
import com.alibou.book.Services.GuestPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/guest/payment")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class GuestPaymentController {

    private final GuestPaymentService guestPaymentService;
    private final PaymentStatusRepository paymentStatusRepository;
    private final ExamCheckRecordRepository examCheckRecordRepository;

    @PostMapping("/initiate")
    public ResponseEntity<GuestPaymentInitiateResponse> initiateGuestPayment(
            @Valid @RequestBody GuestPaymentInitiateRequest request) {
        return ResponseEntity.ok(guestPaymentService.initiateGuestPayment(request));
    }















    @PostMapping("/verify-otp")
    public ResponseEntity<MoolrePaymentResponse> verifyGuestOtp(
            @RequestBody MoolrePaymentRequest request,
            @RequestParam String sessionId) {
        return ResponseEntity.ok(guestPaymentService.verifyGuestOtp(request, sessionId));
    }

















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
