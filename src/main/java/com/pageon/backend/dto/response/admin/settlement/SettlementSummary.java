package com.pageon.backend.dto.response.admin.settlement;

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
    private String email;
    private Long creatorId;
    private Integer settledPoint;
    private SettlementStatus settlementStatus;
    private LocalDateTime payoutDate;
    private LocalDateTime settledAt;

    public static SettlementSummary of(Settlement settlement) {
        return SettlementSummary.builder()
                .settlementId(settlement.getId())
                .email(settlement.getCreator().getUser().getEmail())
                .creatorId(settlement.getCreator().getId())
                .settledPoint(settlement.getSettledPoint())
                .settlementStatus(settlement.getSettlementStatus())
                .payoutDate(settlement.getPayoutDate())
                .settledAt(settlement.getSettledAt())
                .build();
    }

}
