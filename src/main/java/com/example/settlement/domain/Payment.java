package com.example.settlement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String externalPaymentId;

    @Column(nullable = false)
    private String invoiceId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    protected Payment() {
    }

    public Payment(String externalPaymentId, String invoiceId, BigDecimal amount, LocalDateTime receivedAt) {
        this.externalPaymentId = externalPaymentId;
        this.invoiceId = invoiceId;
        this.amount = amount;
        this.receivedAt = receivedAt;
    }

    public Long getId() {
        return id;
    }

    public String getExternalPaymentId() {
        return externalPaymentId;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }
}
