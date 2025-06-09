package com.alibou.book.Repositories;

import com.alibou.book.Entity.PaymentStatuss;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentStatusRepository extends JpaRepository<PaymentStatuss, Long> {
    Optional<PaymentStatuss> findByTransactionId(Long transactionId);

    Optional<PaymentStatuss> findByExternalRef(String externalRef);

//    List<PaymentStatus> findByUserIdOrderByPaymentDateDesc(String userId); // Fetch user transactions in descending order

}