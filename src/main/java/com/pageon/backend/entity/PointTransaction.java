package com.pageon.backend.entity;

import com.pageon.backend.common.base.BaseTimeEntity;
import com.pageon.backend.common.enums.TransactionStatus;
import com.pageon.backend.common.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@DynamicUpdate
@Table(name = "point_transactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PointTransaction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus transactionStatus;
    // 실제 결제 금액
    private Integer amount;
    // 결제 포인트
    private Integer point;
    private Integer balance;
    private String description;

    @Column(unique = true)
    private String orderId;
    @Column(unique = true)
    private String paymentKey;
    private String paymentMethod;

    private Long domainId;

    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;

    public void completedPayment(LocalDateTime paidAt, Integer balance, String paymentKey, String paymentMethod) {
        transactionStatus = TransactionStatus.COMPLETED;
        this.paidAt = paidAt;
        this.balance = balance;
        this.paymentKey = paymentKey;
        this.paymentMethod = paymentMethod;
    }

    public void failedPayment() {
        transactionStatus = TransactionStatus.FAILED;
    }

    public void cancelPayment(LocalDateTime cancelledAt) {
        this.transactionStatus = TransactionStatus.REFUNDED;
        this.cancelledAt = cancelledAt;
    }

}
