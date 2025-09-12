package com.alibou.book.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EnvChecker {

    @Value("${GOOGLE_CLIENT_ID:NOT_FOUND}")
    private String googleClientId;

    @PostConstruct
    public void checkEnv() {
        System.out.println("Loaded GOOGLE_CLIENT_ID = " + googleClientId);
    }
}
