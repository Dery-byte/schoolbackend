package com.alibou.book.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified payment initiation result returned by both Moolre and Paystack gateways.
 * The frontend uses {@code gatewayName} to decide which UI flow to render:
 *  - MOOLRE  → show the OTP modal
 *  - PAYSTACK → open the Paystack popup using {@code authorizationUrl} / {@code accessCode}
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentInitiateResult {

    /** Gateway that processed this initiation: "MOOLRE" or "PAYSTACK" */
    private String gatewayName;

    /** Our internal transaction reference stored in ExamCheckRecord.externalRef */
    private String externalRef;

    // ── Common ───────────────────────────────────────────────
    private Integer status;     // 1 = success / proceed, 0 = failure
    private String  code;       // gateway-specific code (e.g. TP14 for Moolre OTP)
    private String  message;    // raw gateway message
    private String  userMessage; // human-friendly message shown in the UI

    // ── Moolre-specific ──────────────────────────────────────
    /**
     * True when the gateway requires a follow-up OTP verification call.
     * Always {@code false} for Paystack.
     */
    private boolean requiresOtp;

    /** Moolre request ID (data field from Moolre response). Null for Paystack. */
    private String moolreRequestId;

    // ── Paystack-specific ────────────────────────────────────
    /**
     * Paystack hosted checkout URL. The frontend opens this in the Paystack popup.
     * Null when gateway is Moolre.
     */
    private String authorizationUrl;

    /**
     * Paystack access code. Used by the Paystack inline JS popup as an alternative
     * to the full authorizationUrl.
     */
    private String accessCode;
}
