package com.pageon.backend.dto.response.creator.settlement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueDetail {

    private Long settlementId;
    private Integer totalPoint;
    private Integer platformFee;
    private Integer taxFee;
    private Integer settledPoint;
    private LocalDateTime payoutDate;

}
