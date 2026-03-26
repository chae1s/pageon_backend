package com.pageon.backend.dto.response;

import com.pageon.backend.common.enums.TransactionStatus;
import com.pageon.backend.entity.PointTransaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointTransactionResponse {
    private Long id;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private String description;
    private Integer amount;
    private Integer point;
    private Integer balance;
    private TransactionStatus transactionStatus;
    private String paymentMethod;

    public static PointTransactionResponse fromEntity(PointTransaction pointTransaction) {
        return PointTransactionResponse.builder()
                .id(pointTransaction.getId())
                .paidAt(pointTransaction.getPaidAt())
                .description(pointTransaction.getDescription())
                .amount(pointTransaction.getAmount())
                .point(pointTransaction.getPoint())
                .balance(pointTransaction.getBalance())
                .transactionStatus(pointTransaction.getTransactionStatus())
                .paymentMethod(pointTransaction.getPaymentMethod())
                .cancelledAt(pointTransaction.getCancelledAt())
                .build();
    }
}
