package com.pageon.backend.dto.response.creator.episode;

import com.pageon.backend.dto.response.PageResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeDashboard {
    private String contentTitle;
    private String contentType;
    private EpisodeStats episodeStats;
    private PageResponse<EpisodeList> episodes;

}
