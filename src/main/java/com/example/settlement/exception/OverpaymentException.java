package com.example.settlement.exception;

public class OverpaymentException extends RuntimeException {

    public OverpaymentException(String message) {
        super(message);
    }
}
