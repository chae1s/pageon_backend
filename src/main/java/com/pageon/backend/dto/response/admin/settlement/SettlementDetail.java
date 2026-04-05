package com.pageon.backend.dto.response.admin.settlement;

import com.pageon.backend.entity.Settlement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementDetail {

    private Integer totalPoint;
    private Integer platformFee;
    private Integer taxFee;
    private Integer settledPoint;
    private String failedReason;
    private boolean isResolved;

    public static SettlementDetail of(Settlement settlement, boolean isResolved) {


        return SettlementDetail.builder()
                .totalPoint(settlement.getTotalPoint())
                .platformFee(settlement.getPlatformFee())
                .taxFee(settlement.getTaxFee())
                .settledPoint(settlement.getSettledPoint())
                .failedReason(settlement.getFailedReason())
                .isResolved(isResolved)
                .build();

    }
}
