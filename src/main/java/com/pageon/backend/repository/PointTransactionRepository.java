package com.pageon.backend.repository;

import com.pageon.backend.common.enums.TransactionStatus;
import com.pageon.backend.common.enums.TransactionType;
import com.pageon.backend.entity.PointTransaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Integer> {

    @Query("SELECT p FROM PointTransaction p " +
            "WHERE p.user.id = :userId " +
            "AND p.transactionType = :transactionType " +
            "AND (p.transactionStatus = 'COMPLETED' OR p.transactionStatus = 'REFUNDED')")
    Page<PointTransaction> findAllByTransactionStatus(Long userId, TransactionType transactionType, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PointTransaction p " +
            "WHERE p.user.id = :userId " +
            "AND p.orderId = :orderId")
    Optional<PointTransaction> findByUserAndOrderIdWithLock(Long userId, String orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PointTransaction p " +
            "WHERE p.id = :transactionId " +
            "AND p.user.id = :userId")
    Optional<PointTransaction> findByIdAndUserWithLock(Long transactionId, Long userId);
}
