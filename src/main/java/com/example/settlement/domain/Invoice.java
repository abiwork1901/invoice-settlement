package com.example.settlement.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    private String id;

    private BigDecimal totalAmount;
    private BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    @Enumerated(EnumType.STRING)
    private ReconciliationStatus reconciliationStatus;

    private LocalDateTime paidAt;

    @Version
    private Long version;

    protected Invoice() {
    }

    public Invoice(
            String id,
            BigDecimal totalAmount,
            BigDecimal paidAmount,
            InvoiceStatus status,
            ReconciliationStatus reconciliationStatus
    ) {
        this.id = id;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.status = status;
        this.reconciliationStatus = reconciliationStatus;
    }

    public String getId() {
        return id;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public ReconciliationStatus getReconciliationStatus() {
        return reconciliationStatus;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public void setReconciliationStatus(ReconciliationStatus reconciliationStatus) {
        this.reconciliationStatus = reconciliationStatus;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}
