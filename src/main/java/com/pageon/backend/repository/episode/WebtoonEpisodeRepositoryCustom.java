package com.pageon.backend.repository.episode;

import com.pageon.backend.dto.response.episode.EpisodeSummaryResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;


public interface WebtoonEpisodeRepositoryCustom {

    Slice<EpisodeSummaryResponse> findEpisodeSummaries(Long contentId, String sort, Pageable pageable);

}
