package com.alibou.book.exception;

public class PaymentProcessingException extends RuntimeException {
    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
    public PaymentProcessingException(String message) {
        super(message);
    }

}