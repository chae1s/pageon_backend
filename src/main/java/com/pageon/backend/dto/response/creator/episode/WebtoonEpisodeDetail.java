package com.pageon.backend.dto.response.creator.episode;

import com.pageon.backend.common.enums.EpisodeStatus;
import com.pageon.backend.dto.response.episode.EpisodeImage;
import com.pageon.backend.entity.WebtoonEpisode;
import com.pageon.backend.entity.WebtoonImage;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebtoonEpisodeDetail {
    private String contentTitle;
    private Long episodeId;
    private Integer episodeNum;
    private String episodeTitle;
    private LocalDate publishedAt;
    private EpisodeStatus episodeStatus;
    private List<EpisodeImage> episodeImages;
    private LocalDateTime createdAt;

    public static WebtoonEpisodeDetail of(WebtoonEpisode episode, List<EpisodeImage> images) {

        return WebtoonEpisodeDetail.builder()
                .contentTitle(episode.getWebtoon().getTitle())
                .episodeId(episode.getId())
                .episodeNum(episode.getEpisodeNum())
                .episodeTitle(episode.getEpisodeTitle())
                .publishedAt(episode.getPublishedAt())
                .episodeStatus(episode.getEpisodeStatus())
                .episodeImages(images)
                .createdAt(episode.getCreatedAt())
                .build();
    }

}
