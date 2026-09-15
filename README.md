# Invoice Settlement

A **Spring Boot 3** + **Maven** Java project that implements production-safe invoice settlement.

It fixes the common review issues from the original PR: race conditions, missing idempotency, overpayments, and incorrect status transitions.

## Tech stack

| Tool | Version |
|------|---------|
| Java | 21 |
| Spring Boot | 3.3.4 |
| Spring Data JPA | (via Spring Boot) |
| H2 (in-memory DB for local dev/tests) | runtime |
| Maven | 3.9+ |
| JUnit 5 | (via spring-boot-starter-test) |

## Prerequisites

```bash
java -version   # should show 21+
mvn -version    # should show Maven 3.9+
```

## Quick start

```bash
cd ~/play/invoice-settlement

# 1. Compile
mvn clean compile

# 2. Run unit/integration tests
mvn test

# 3. Run the Spring Boot app (demo profile prints sample settlements)
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

## How to run

### Run tests (recommended first step)

Runs all JUnit tests against an in-memory H2 database:

```bash
mvn test
```

Expected output ends with:

```
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Run the Spring Boot application

Starts the Spring context. With the `demo` profile, a sample invoice is created and settled on startup:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

You should see log lines like:

```
Demo invoice inv20 created: total=1000, paid=0
After payment-1 (500): status=PARTIALLY_PAID, paid=500
After payment-2 (500): status=PAID, paid=1000
Idempotent retry of payment-1: status=PAID, paid=1000
```

Run without the demo profile (context only, no sample data):

```bash
mvn spring-boot:run
```

### Build a runnable JAR

```bash
mvn clean package
java -jar target/invoice-settlement-1.0.0-SNAPSHOT.jar --spring.profiles.active=demo
```

### Compile only (no tests)

```bash
mvn clean compile
```

## Project layout

```
src/main/java/com/example/settlement/
├── InvoiceSettlementApplication.java   # Spring Boot entry point
├── InvoiceSettlementService.java       # Core settlement logic
├── config/ClockConfig.java
├── demo/SettlementDemoRunner.java      # Demo runner (profile=demo)
├── domain/
│   ├── Invoice.java
│   ├── Payment.java
│   ├── InvoiceStatus.java
│   ├── ReconciliationStatus.java
│   └── SettlementResult.java
├── repository/
│   ├── InvoiceRepository.java
│   └── PaymentRepository.java
└── exception/
    ├── InvoiceNotFoundException.java
    ├── InvoiceNotSettleableException.java
    ├── OverpaymentException.java
    ├── PaymentAlreadyUsedException.java
    └── ConflictingPaymentException.java

src/test/java/com/example/settlement/
└── InvoiceSettlementServiceTest.java   # Unit/integration tests
```

## What the service does

1. **Idempotency** — same `externalPaymentId` is safe to retry; no double-counting
2. **Concurrency safety** — pessimistic write lock on invoice row (`SELECT FOR UPDATE`)
3. **Validation** — rejects invalid input, cancelled invoices, and overpayments
4. **Status transitions** — `PARTIALLY_PAID` → `PAID` with correct reconciliation state

## Test coverage

`InvoiceSettlementServiceTest` covers:

| Category | Tests |
|----------|-------|
| Happy path | partial + full payment, single full payment, reconciliation status |
| Idempotency | retry same payment, conflicting amount retry, payment reused on another invoice |
| Guards | overpayment, cancelled invoice, already paid, missing invoice |
| Input validation | blank ids, null/zero/negative amount |

## Example settlement flow

| Step | Invoice state | Payment | Result |
|------|---------------|---------|--------|
| 1 | total=1000, paid=0 | 500 | paid=500, `PARTIALLY_PAID`, `PENDING` |
| 2 | total=1000, paid=500 | 500 | paid=1000, `PAID`, `MATCHED` |
| Retry | same `externalPaymentId` | 500 | Idempotent success, no double-count |
| Invalid | total=1000, paid=500 | 600 | `OverpaymentException` (remaining=500) |

## Database note (production)

For multi-node deployments, enforce uniqueness at the DB level:

```sql
ALTER TABLE payments
  ADD CONSTRAINT uq_payments_external_payment_id UNIQUE (external_payment_id);
```

## License

Internal / example project.
