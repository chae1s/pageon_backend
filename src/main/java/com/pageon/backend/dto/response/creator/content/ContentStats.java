package com.pageon.backend.dto.response.creator.content;

import com.pageon.backend.common.enums.SeriesStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentStats {

    private int ongoingCount;
    private int restCount;
    private int completedCount;
    private int deletionCount;


    private Long totalViewCount;
    private Double averageRating;
    private Long totalInterestCount;

    // 작품 목록 (탭)
    private List<ContentTab> contents;

    public static ContentStats of(Map<SeriesStatus, Long> statusMap, int deletionCount, ContentPerformanceStats stats, List<ContentTab> contentTabs) {
        return ContentStats.builder()
                .ongoingCount(statusMap.getOrDefault(SeriesStatus.ONGOING, 0L).intValue())
                .restCount(statusMap.getOrDefault(SeriesStatus.REST, 0L).intValue())
                .completedCount(statusMap.getOrDefault(SeriesStatus.COMPLETED, 0L).intValue())
                .deletionCount(deletionCount)
                .totalViewCount(stats.getTotalViewCount())
                .averageRating(stats.getAverageRating())
                .totalInterestCount(stats.getTotalInterestCount())
                .contents(contentTabs)
                .build();
    }
}
