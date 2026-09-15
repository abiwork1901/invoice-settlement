package com.example.settlement;

import com.example.settlement.domain.Invoice;
import com.example.settlement.domain.InvoiceStatus;
import com.example.settlement.domain.Payment;
import com.example.settlement.domain.ReconciliationStatus;
import com.example.settlement.domain.SettlementResult;
import com.example.settlement.exception.ConflictingPaymentException;
import com.example.settlement.exception.InvoiceNotFoundException;
import com.example.settlement.exception.InvoiceNotSettleableException;
import com.example.settlement.exception.OverpaymentException;
import com.example.settlement.exception.PaymentAlreadyUsedException;
import com.example.settlement.repository.InvoiceRepository;
import com.example.settlement.repository.PaymentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class InvoiceSettlementService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final Clock clock;

    public InvoiceSettlementService(
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            Clock clock
    ) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.clock = clock;
    }

    @Transactional
    public SettlementResult settle(String invoiceId, String externalPaymentId, BigDecimal amount) {
        validateInputs(invoiceId, externalPaymentId, amount);

        Optional<Payment> existing = paymentRepository.findByExternalPaymentId(externalPaymentId);
        if (existing.isPresent()) {
            return handleExistingPayment(existing.get(), invoiceId, amount);
        }

        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        assertSettleable(invoice);

        BigDecimal remaining = invoice.getTotalAmount().subtract(invoice.getPaidAmount());
        if (amount.compareTo(remaining) > 0) {
            throw new OverpaymentException(
                    "Payment %s amount %s exceeds remaining %s for invoice %s"
                            .formatted(externalPaymentId, amount, remaining, invoiceId)
            );
        }

        BigDecimal newPaidAmount = invoice.getPaidAmount().add(amount);

        try {
            paymentRepository.save(new Payment(
                    externalPaymentId,
                    invoiceId,
                    amount,
                    LocalDateTime.now(clock)
            ));
        } catch (DataIntegrityViolationException ex) {
            Payment concurrent = paymentRepository.findByExternalPaymentId(externalPaymentId)
                    .orElseThrow(() -> ex);
            return handleExistingPayment(concurrent, invoiceId, amount);
        }

        applySettlement(invoice, newPaidAmount);
        invoiceRepository.save(invoice);

        return toResult(invoice);
    }

    private SettlementResult handleExistingPayment(Payment payment, String invoiceId, BigDecimal amount) {
        if (!payment.getInvoiceId().equals(invoiceId)) {
            throw new PaymentAlreadyUsedException(
                    "Payment %s already applied to invoice %s"
                            .formatted(payment.getExternalPaymentId(), payment.getInvoiceId())
            );
        }
        if (payment.getAmount().compareTo(amount) != 0) {
            throw new ConflictingPaymentException(
                    "Payment %s already recorded with amount %s, got %s"
                            .formatted(payment.getExternalPaymentId(), payment.getAmount(), amount)
            );
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
        return toResult(invoice);
    }

    private void applySettlement(Invoice invoice, BigDecimal newPaidAmount) {
        invoice.setPaidAmount(newPaidAmount);

        int cmp = newPaidAmount.compareTo(invoice.getTotalAmount());
        if (cmp == 0) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(LocalDateTime.now(clock));
            invoice.setReconciliationStatus(ReconciliationStatus.MATCHED);
        } else if (cmp < 0) {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
            invoice.setReconciliationStatus(ReconciliationStatus.PENDING);
        }
    }

    private void assertSettleable(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new InvoiceNotSettleableException("Invoice " + invoice.getId() + " is cancelled");
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new InvoiceNotSettleableException("Invoice " + invoice.getId() + " is already paid");
        }
    }

    private void validateInputs(String invoiceId, String externalPaymentId, BigDecimal amount) {
        if (invoiceId == null || invoiceId.isBlank()) {
            throw new IllegalArgumentException("invoiceId is required");
        }
        if (externalPaymentId == null || externalPaymentId.isBlank()) {
            throw new IllegalArgumentException("externalPaymentId is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }

    private SettlementResult toResult(Invoice invoice) {
        return new SettlementResult(invoice.getId(), invoice.getStatus(), invoice.getPaidAmount());
    }
}
