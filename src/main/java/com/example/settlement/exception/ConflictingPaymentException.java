package com.example.settlement.exception;

public class ConflictingPaymentException extends RuntimeException {

    public ConflictingPaymentException(String message) {
        super(message);
    }
}
