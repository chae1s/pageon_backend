package com.pageon.backend.service;

import com.pageon.backend.entity.PointTransaction;
import com.pageon.backend.repository.PointTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentFailureLogger {

    private final PointTransactionRepository pointTransactionRepository;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(PointTransaction transaction) {
        transaction.failedPayment();
        pointTransactionRepository.save(transaction);
    }
}
