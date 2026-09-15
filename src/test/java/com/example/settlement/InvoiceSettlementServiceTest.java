package com.example.settlement;

import com.example.settlement.domain.Invoice;
import com.example.settlement.domain.InvoiceStatus;
import com.example.settlement.domain.ReconciliationStatus;
import com.example.settlement.domain.SettlementResult;
import com.example.settlement.exception.ConflictingPaymentException;
import com.example.settlement.exception.InvoiceNotFoundException;
import com.example.settlement.exception.InvoiceNotSettleableException;
import com.example.settlement.exception.OverpaymentException;
import com.example.settlement.exception.PaymentAlreadyUsedException;
import com.example.settlement.repository.InvoiceRepository;
import com.example.settlement.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class InvoiceSettlementServiceTest {

    @Autowired
    private InvoiceSettlementService settlementService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        invoiceRepository.deleteAll();
        invoiceRepository.save(openInvoice("inv20", "1000"));
    }

    @Nested
    class HappyPath {

        @Test
        void partialThenFullPayment() {
            SettlementResult first = settlementService.settle("inv20", "pay-1", bd("500"));
            assertEquals(InvoiceStatus.PARTIALLY_PAID, first.status());
            assertEquals(bd("500"), first.paidAmount());

            SettlementResult second = settlementService.settle("inv20", "pay-2", bd("500"));
            assertEquals(InvoiceStatus.PAID, second.status());
            assertEquals(bd("1000"), second.paidAmount());
        }

        @Test
        void fullPaymentInSingleSettlement() {
            SettlementResult result = settlementService.settle("inv20", "pay-full", bd("1000"));

            assertEquals(InvoiceStatus.PAID, result.status());
            assertEquals(bd("1000"), result.paidAmount());

            Invoice invoice = invoiceRepository.findById("inv20").orElseThrow();
            assertEquals(ReconciliationStatus.MATCHED, invoice.getReconciliationStatus());
            assertNotNull(invoice.getPaidAt());
        }

        @Test
        void partialPaymentKeepsReconciliationPending() {
            settlementService.settle("inv20", "pay-1", bd("500"));

            Invoice invoice = invoiceRepository.findById("inv20").orElseThrow();
            assertEquals(InvoiceStatus.PARTIALLY_PAID, invoice.getStatus());
            assertEquals(ReconciliationStatus.PENDING, invoice.getReconciliationStatus());
        }
    }

    @Nested
    class Idempotency {

        @Test
        void retryWithSameExternalPaymentIdDoesNotDoubleCount() {
            settlementService.settle("inv20", "pay-1", bd("500"));
            SettlementResult retry = settlementService.settle("inv20", "pay-1", bd("500"));

            assertEquals(bd("500"), retry.paidAmount());
            assertEquals(InvoiceStatus.PARTIALLY_PAID, retry.status());
            assertEquals(1, paymentRepository.count());
        }

        @Test
        void retryWithSameIdButDifferentAmountIsRejected() {
            settlementService.settle("inv20", "pay-1", bd("500"));

            assertThrows(ConflictingPaymentException.class, () ->
                    settlementService.settle("inv20", "pay-1", bd("600"))
            );
        }

        @Test
        void paymentAlreadyUsedOnDifferentInvoiceIsRejected() {
            invoiceRepository.save(openInvoice("inv99", "500"));
            settlementService.settle("inv20", "pay-1", bd("500"));

            assertThrows(PaymentAlreadyUsedException.class, () ->
                    settlementService.settle("inv99", "pay-1", bd("500"))
            );
        }
    }

    @Nested
    class ValidationAndGuards {

        @Test
        void rejectsOverpayment() {
            settlementService.settle("inv20", "pay-1", bd("500"));

            assertThrows(OverpaymentException.class, () ->
                    settlementService.settle("inv20", "pay-2", bd("600"))
            );
        }

        @Test
        void rejectsCancelledInvoice() {
            Invoice cancelled = openInvoice("inv-cancelled", "1000");
            cancelled.setStatus(InvoiceStatus.CANCELLED);
            invoiceRepository.save(cancelled);

            assertThrows(InvoiceNotSettleableException.class, () ->
                    settlementService.settle("inv-cancelled", "pay-1", bd("100"))
            );
        }

        @Test
        void rejectsAlreadyPaidInvoice() {
            settlementService.settle("inv20", "pay-1", bd("1000"));

            assertThrows(InvoiceNotSettleableException.class, () ->
                    settlementService.settle("inv20", "pay-2", bd("100"))
            );
        }

        @Test
        void rejectsMissingInvoice() {
            assertThrows(InvoiceNotFoundException.class, () ->
                    settlementService.settle("missing", "pay-1", bd("100"))
            );
        }

        @Test
        void rejectsBlankInvoiceId() {
            assertThrows(IllegalArgumentException.class, () ->
                    settlementService.settle("  ", "pay-1", bd("100"))
            );
        }

        @Test
        void rejectsBlankExternalPaymentId() {
            assertThrows(IllegalArgumentException.class, () ->
                    settlementService.settle("inv20", "", bd("100"))
            );
        }

        @Test
        void rejectsNullAmount() {
            assertThrows(IllegalArgumentException.class, () ->
                    settlementService.settle("inv20", "pay-1", null)
            );
        }

        @Test
        void rejectsZeroAmount() {
            assertThrows(IllegalArgumentException.class, () ->
                    settlementService.settle("inv20", "pay-1", BigDecimal.ZERO)
            );
        }

        @Test
        void rejectsNegativeAmount() {
            assertThrows(IllegalArgumentException.class, () ->
                    settlementService.settle("inv20", "pay-1", bd("-10"))
            );
        }
    }

    private static Invoice openInvoice(String id, String totalAmount) {
        return new Invoice(
                id,
                bd(totalAmount),
                BigDecimal.ZERO,
                InvoiceStatus.OPEN,
                ReconciliationStatus.PENDING
        );
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
