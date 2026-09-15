package com.example.settlement.domain;

import java.math.BigDecimal;

public record SettlementResult(
        String invoiceId,
        InvoiceStatus status,
        BigDecimal paidAmount
) {}
