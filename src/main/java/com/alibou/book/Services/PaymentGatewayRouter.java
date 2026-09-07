package com.alibou.book.Services;

import com.alibou.book.Services.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Reads the {@code ACTIVE_PAYMENT_GATEWAY} system setting at call-time and returns
 * the corresponding service. This allows the admin to switch gateways at runtime
 * without a redeployment.
 *
 * <p>Default gateway is {@code MOOLRE} to preserve existing behaviour.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayRouter {

    private final SystemSettingService systemSettingService;
    private final MoolrePaymentService moolrePaymentService;
    private final PaystackPaymentService paystackPaymentService;

    public static final String SETTING_KEY = "ACTIVE_PAYMENT_GATEWAY";
    public static final String GATEWAY_MOOLRE   = "MOOLRE";
    public static final String GATEWAY_PAYSTACK = "PAYSTACK";

    /**
     * Returns the name of the currently active gateway.
     *
     * @return "MOOLRE" or "PAYSTACK"
     */
    public String getActiveGatewayName() {
        return systemSettingService.getSetting(SETTING_KEY, GATEWAY_MOOLRE).toUpperCase();
    }

    /**
     * Returns {@code true} when Paystack is the active gateway.
     */
    public boolean isPaystackActive() {
        return GATEWAY_PAYSTACK.equalsIgnoreCase(getActiveGatewayName());
    }

    /**
     * Returns {@code true} when Moolre is the active gateway.
     */
    public boolean isMoolreActive() {
        return GATEWAY_MOOLRE.equalsIgnoreCase(getActiveGatewayName());
    }

    public MoolrePaymentService getMoolre() {
        return moolrePaymentService;
    }

    public PaystackPaymentService getPaystack() {
        return paystackPaymentService;
    }
}
