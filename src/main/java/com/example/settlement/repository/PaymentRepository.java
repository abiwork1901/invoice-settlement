package com.example.settlement.repository;

import com.example.settlement.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByExternalPaymentId(String externalPaymentId);
}
