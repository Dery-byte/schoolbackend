package com.alibou.book.exception;

public class ResetPasswordTokenAlreadyUsedException extends RuntimeException {
    public ResetPasswordTokenAlreadyUsedException(String message) {
        super(message);
    }
}
