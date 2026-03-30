package com.pageon.backend.dto.response.creator.episode;

import com.pageon.backend.common.enums.EpisodeStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeList {
    private Long episodeId;
    private Integer episodeNum;
    private String episodeTitle;
    private Double averageRating;
    private EpisodeStatus episodeStatus;
    private LocalDate publishedAt;
    private LocalDateTime createdAt;

}
