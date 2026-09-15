package com.example.settlement.exception;

public class InvoiceNotFoundException extends RuntimeException {

    public InvoiceNotFoundException(String invoiceId) {
        super("Invoice not found: " + invoiceId);
    }
}
