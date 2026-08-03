package com.pageon.backend.dto.record;

import com.pageon.backend.common.enums.TransactionStatus;
import com.pageon.backend.common.enums.TransactionType;
import com.pageon.backend.entity.PointTransaction;
import com.pageon.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor // Jackson 역직렬화를 위한 필수 기본 생성자
@AllArgsConstructor // @Builder 사용을 위한 모든 필드 생성자
public class PaymentCache {

    private Long userId;
    private TransactionType transactionType;
    private TransactionStatus transactionStatus;
    private Integer amount;
    private Integer point;
    private String description;
    private String orderId;

    public PointTransaction toEntity(User user) {
        return PointTransaction.builder()
                .user(user)
                .transactionType(this.transactionType)
                .transactionStatus(this.transactionStatus)
                .amount(this.amount)
                .point(this.point)
                .description(this.description)
                .orderId(this.orderId)
                .build();
    }
}