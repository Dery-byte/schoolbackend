package com.alibou.book.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Paystack gateway configuration, bound from application-*.yml under the {@code paystack} prefix.
 */
@Configuration
@ConfigurationProperties(prefix = "paystack")
@Data
public class PaystackConfig {

    /** Paystack secret key — used for server-side API calls and webhook HMAC verification. */
    private String secretKey;

    /** Paystack public key — exposed to the Angular frontend to initialise the popup. */
    private String publicKey;

    /** Base URL for the Paystack REST API. */
    private String baseUrl = "https://api.paystack.co";

    /**
     * Callback URL that Paystack redirects to after payment (used in non-popup redirect flow).
     * Not strictly needed when using the Paystack inline popup, but good practice to set.
     */
    private String callbackUrl;
}
