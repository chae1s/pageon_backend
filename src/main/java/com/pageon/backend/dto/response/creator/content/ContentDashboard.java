package com.pageon.backend.dto.response.creator.content;

import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.common.enums.SeriesStatus;
import com.pageon.backend.common.enums.WorkStatus;
import com.pageon.backend.dto.response.creator.episode.EpisodeStats;
import com.pageon.backend.entity.Content;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentDashboard {

    private Long contentId;
    private String contentTitle;
    private String cover;
    private String description;
    private String contentType;
    private SerialDay serialDay;
    private SeriesStatus seriesStatus;
    private List<String> keywords;
    private Long viewCount;
    private Long interestCount;
    private WorkStatus workStatus;

    private LocalDateTime episodeUpdatedAt;
    private EpisodeStats episodeStats;

    public static ContentDashboard of(Content content, EpisodeStats stats) {
        List<String> keywords = content.getContentKeywords().stream()
                .map(ck -> ck.getKeyword().getName())
                .toList();

        return ContentDashboard.builder()
                .contentId(content.getId())
                .contentTitle(content.getTitle())
                .cover(content.getCover())
                .description(content.getDescription())
                .contentType(content.getDtype())
                .serialDay(content.getSerialDay())
                .seriesStatus(content.getStatus())
                .keywords(keywords)
                .viewCount(content.getViewCount())
                .interestCount(content.getInterestCount())
                .workStatus(content.getWorkStatus())
                .episodeUpdatedAt(content.getEpisodeUpdatedAt())
                .episodeStats(stats)
                .build();

    }
}
