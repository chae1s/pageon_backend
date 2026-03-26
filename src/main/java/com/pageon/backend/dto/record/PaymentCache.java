package com.pageon.backend.dto.record;

import com.pageon.backend.common.enums.TransactionStatus;
import com.pageon.backend.common.enums.TransactionType;
import com.pageon.backend.entity.PointTransaction;
import com.pageon.backend.entity.User;
import lombok.Builder;

@Builder
public record PaymentCache(
        Long userId,
        TransactionType transactionType,
        TransactionStatus transactionStatus,
        Integer amount,
        Integer point,
        String description,
        String orderId
) {

    public PointTransaction toEntity(User user) {
        return PointTransaction.builder()
                .user(user)
                .transactionType(this.transactionType())
                .transactionStatus(this.transactionStatus())
                .amount(this.amount())
                .point(this.point())
                .description(this.description())
                .orderId(this.orderId())
                .build();
    }
}