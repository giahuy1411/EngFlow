package com.datn.engflow.repository;

import com.datn.engflow.model.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
/**
 * interface PaymentTransactionRepository.
 */
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByTransactionId(String transactionId);
    Optional<PaymentTransaction> findByOrderCode(String orderCode);
    Optional<PaymentTransaction> findFirstByOrderCodeAndStatusOrderByIdDesc(String orderCode, String status);
    boolean existsByOrderCodeAndStatus(String orderCode, String status);
    List<PaymentTransaction> findTop5ByUserIdAndStatusOrderByIdDesc(Long userId, String status);
}
