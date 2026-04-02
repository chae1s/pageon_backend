package com.pageon.backend.dto.response.creator.settlement;

import com.pageon.backend.common.enums.SettlementStatus;
import com.pageon.backend.entity.Settlement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementDetail {
    private Long settlementId;
    private Integer totalPoint;
    private Integer platformFee;
    private Integer taxFee;
    private Integer settledPoint;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private LocalDateTime scheduledAt;
    private LocalDateTime settledAt;
    private LocalDateTime payoutDate;
    private SettlementStatus settlementStatus;

    public static SettlementDetail of(Settlement settlement) {
        return SettlementDetail.builder()
                .settlementId(settlement.getId())
                .totalPoint(settlement.getTotalPoint())
                .platformFee(settlement.getPlatformFee())
                .taxFee(settlement.getTaxFee())
                .settledPoint(settlement.getSettledPoint())
                .periodStart(settlement.getPeriodStart())
                .periodEnd(settlement.getPeriodEnd())
                .scheduledAt(settlement.getScheduledAt())
                .settledAt(settlement.getSettledAt())
                .payoutDate(settlement.getPayoutDate())
                .settlementStatus(settlement.getSettlementStatus())
                .build();
    }
}
