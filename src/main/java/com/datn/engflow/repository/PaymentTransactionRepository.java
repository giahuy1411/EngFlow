package com.datn.engflow.repository;

import com.datn.engflow.model.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
    List<PaymentTransaction> findTop10ByUserIdAndStatusOrderByIdDesc(Long userId, String status);

    /**
     * Pending orders created at or after {@code since}, newest first, capped
     * to {@code max} rows. Used by the background SePay polling fallback so it
     * only scans recent orders instead of every PENDING row ever created.
     */
    @Query("SELECT t FROM PaymentTransaction t WHERE t.status = 'PENDING' AND t.createdAt >= :since ORDER BY t.id DESC")
    List<PaymentTransaction> findRecentPending(@Param("since") LocalDateTime since, @Param("max") int max);
}
