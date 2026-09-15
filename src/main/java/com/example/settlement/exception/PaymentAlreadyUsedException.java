package com.example.settlement.exception;

public class PaymentAlreadyUsedException extends RuntimeException {

    public PaymentAlreadyUsedException(String message) {
        super(message);
    }
}
