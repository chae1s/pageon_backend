package com.pageon.backend.repository.episode;

import com.pageon.backend.dto.response.episode.EpisodeSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WebnovelEpisodeRepositoryCustom {

    Page<EpisodeSummaryResponse> findEpisodeSummaries(Long contentId, String sort, Pageable pageable);
}
