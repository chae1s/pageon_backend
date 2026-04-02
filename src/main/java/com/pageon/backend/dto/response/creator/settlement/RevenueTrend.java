package com.pageon.backend.dto.response.creator.settlement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueTrend {
    private Long maxRevenue;
    private List<DailyRevenue> dailyRevenues;

    public static RevenueTrend of(List<DailyRevenue> dailyRevenues) {
        Long maxRevenue = dailyRevenues.stream()
                .mapToLong(DailyRevenue::getRevenue)
                .max()
                .orElse(0L);

        return RevenueTrend.builder()
                .maxRevenue(maxRevenue)
                .dailyRevenues(dailyRevenues)
                .build();
    }
}
