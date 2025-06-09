package com.alibou.book.Repositories;

import com.alibou.book.Entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByExternalRef(String externalRef);
}
