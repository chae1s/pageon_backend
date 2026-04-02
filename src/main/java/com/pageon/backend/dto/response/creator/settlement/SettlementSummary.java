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
public class SettlementSummary {

    private Long settlementId;
    private Integer settledPoint;
    private LocalDateTime scheduledAt;
    private SettlementStatus settlementStatus;

    public static SettlementSummary of(Settlement settlement) {
        return SettlementSummary.builder()
                .settlementId(settlement.getId())
                .settledPoint(settlement.getSettledPoint())
                .scheduledAt(settlement.getScheduledAt())
                .settlementStatus(settlement.getSettlementStatus())
                .build();
    }
}
