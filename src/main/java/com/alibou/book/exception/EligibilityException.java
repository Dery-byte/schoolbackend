package com.alibou.book.exception;

// ✅ EligibilityException.java
public class EligibilityException extends RuntimeException {
    public EligibilityException(String message) {
        super(message);
    }
}
