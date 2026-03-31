package com.pageon.backend.dto.response.creator.episode;

import com.pageon.backend.common.enums.EpisodeStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
public class EpisodeStats {
    private Integer totalEpisodeCount;
    private Integer publishedEpisodeCount;
    private Integer draftEpisodeCount;
    private Integer scheduledEpisodeCount;

    public EpisodeStats(Integer totalCount, Map<EpisodeStatus, Long> statusMap) {
        this.totalEpisodeCount = totalCount;
        this.publishedEpisodeCount = statusMap.get(EpisodeStatus.PUBLISHED) != null ? statusMap.get(EpisodeStatus.PUBLISHED).intValue() : 0;
        this.draftEpisodeCount = statusMap.get(EpisodeStatus.DRAFT) != null ? statusMap.get(EpisodeStatus.DRAFT).intValue() : 0;
        this.scheduledEpisodeCount = statusMap.get(EpisodeStatus.SCHEDULED) != null ? statusMap.get(EpisodeStatus.SCHEDULED).intValue() : 0;
    }
}
