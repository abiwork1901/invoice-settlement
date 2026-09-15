package com.example.settlement.exception;

public class InvoiceNotSettleableException extends RuntimeException {

    public InvoiceNotSettleableException(String message) {
        super(message);
    }
}
