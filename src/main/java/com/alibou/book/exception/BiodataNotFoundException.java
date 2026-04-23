package com.alibou.book.exception;

public class BiodataNotFoundException extends RuntimeException {
    public BiodataNotFoundException(String message) {
        super(message);
    }
}