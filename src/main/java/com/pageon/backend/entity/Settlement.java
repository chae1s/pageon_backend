package com.pageon.backend.entity;


import com.pageon.backend.common.base.BaseTimeEntity;
import com.pageon.backend.common.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@DynamicUpdate
@Table(name = "settlements")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Settlement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private Creator creator;


    private Integer totalPoint;
    private Integer platformFee;
    private Integer taxFee;
    private Integer settledPoint;


    @Enumerated(EnumType.STRING)
    private SettlementStatus settlementStatus;
    private String failedReason;

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    private LocalDateTime scheduledAt;
    private LocalDateTime payoutDate;
    private LocalDateTime settledAt;

    public void complete(LocalDateTime payoutDate) {
        this.settlementStatus = SettlementStatus.DONE;
        this.settledAt = payoutDate;
    }

    public void fail(String reason) {
        this.settlementStatus = SettlementStatus.FAILED;
        this.failedReason = reason;
    }



}
