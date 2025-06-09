package com.alibou.book.Controllers;

import com.alibou.book.DTO.MoolrePaymentRequest;
import com.alibou.book.Entity.MoolrePaymentResponse;
import com.alibou.book.Services.MoolrePaymentService;
import com.alibou.book.exception.PaymentProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/auth/payments")
@RequiredArgsConstructor
public class PaymentController {

private final MoolrePaymentService moolrePaymentService;

    @PostMapping("/initiate")
    public ResponseEntity<MoolrePaymentResponse> initiatePayment(
            Principal principal,
            @RequestBody MoolrePaymentRequest request
    ) {
        MoolrePaymentResponse response = moolrePaymentService.initiatePayment(principal, request);
        return ResponseEntity.ok(response);
    }







    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtpAndProceed(@AuthenticationPrincipal Principal principal,
                                                 @RequestBody MoolrePaymentRequest request) {
        try {
            MoolrePaymentResponse response = moolrePaymentService.verifyOtpAndProceed(principal, request);
            return ResponseEntity.ok(response);
        } catch (PaymentProcessingException ex) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }
}






//13 for MTN,
//
//        7 for AirtelTigo,
//        6 for Vodafone