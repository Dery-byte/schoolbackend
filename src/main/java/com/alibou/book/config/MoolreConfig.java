package com.alibou.book.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "moolre")
@Data
public class MoolreConfig {
    private String apiUrl = "https://api.moolre.com/open/transact/payment";
    private String username;  // X-API-USER
    private String publicKey; // X-API-PUBKEY
    private String accountNumber;
}