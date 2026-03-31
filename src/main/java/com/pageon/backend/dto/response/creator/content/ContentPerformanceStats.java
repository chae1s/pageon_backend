package com.pageon.backend.dto.response.creator.content;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ContentPerformanceStats {
    private Long totalViewCount;
    private Double averageRating;
    private Long totalInterestCount;

    public ContentPerformanceStats(Long totalViewCount, Double averageRating, Long totalInterestCount) {
        this.totalViewCount = totalViewCount != null ? totalViewCount : 0L;
        this.averageRating = averageRating != null ? averageRating : 0.0;
        this.totalInterestCount = totalInterestCount != null ? totalInterestCount : 0L;
    }
}
