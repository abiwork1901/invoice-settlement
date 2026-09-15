package com.example.settlement.demo;

import com.example.settlement.InvoiceSettlementService;
import com.example.settlement.domain.Invoice;
import com.example.settlement.domain.InvoiceStatus;
import com.example.settlement.domain.ReconciliationStatus;
import com.example.settlement.domain.SettlementResult;
import com.example.settlement.repository.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Profile("demo")
public class SettlementDemoRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SettlementDemoRunner.class);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceSettlementService settlementService;

    public SettlementDemoRunner(InvoiceRepository invoiceRepository, InvoiceSettlementService settlementService) {
        this.invoiceRepository = invoiceRepository;
        this.settlementService = settlementService;
    }

    @Override
    public void run(String... args) {
        invoiceRepository.save(new Invoice(
                "inv20",
                new BigDecimal("1000"),
                BigDecimal.ZERO,
                InvoiceStatus.OPEN,
                ReconciliationStatus.PENDING
        ));

        log.info("Demo invoice inv20 created: total=1000, paid=0");

        SettlementResult first = settlementService.settle("inv20", "payment-1", new BigDecimal("500"));
        log.info("After payment-1 (500): status={}, paid={}", first.status(), first.paidAmount());

        SettlementResult second = settlementService.settle("inv20", "payment-2", new BigDecimal("500"));
        log.info("After payment-2 (500): status={}, paid={}", second.status(), second.paidAmount());

        SettlementResult retry = settlementService.settle("inv20", "payment-1", new BigDecimal("500"));
        log.info("Idempotent retry of payment-1: status={}, paid={}", retry.status(), retry.paidAmount());
    }
}
