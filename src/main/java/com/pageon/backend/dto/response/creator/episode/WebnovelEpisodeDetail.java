package com.pageon.backend.dto.response.creator.episode;

import com.pageon.backend.common.enums.EpisodeStatus;
import com.pageon.backend.entity.WebnovelEpisode;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebnovelEpisodeDetail {
    private String contentTitle;
    private Long episodeId;
    private Integer episodeNum;
    private String episodeTitle;
    private LocalDate publishedAt;
    private EpisodeStatus episodeStatus;
    private String content;
    private LocalDateTime createdAt;

    public static WebnovelEpisodeDetail of(WebnovelEpisode episode) {
        return WebnovelEpisodeDetail.builder()
                .contentTitle(episode.getWebnovel().getTitle())
                .episodeNum(episode.getEpisodeNum())
                .episodeId(episode.getId())
                .episodeTitle(episode.getEpisodeTitle())
                .publishedAt(episode.getPublishedAt())
                .episodeStatus(episode.getEpisodeStatus())
                .content(episode.getContent())
                .createdAt(episode.getCreatedAt())
                .build();
    }
}
