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
public class RevenueDashboard {

    private Integer totalPoint;
    private Integer settledPoint;
    private LocalDateTime payoutDate;

}
