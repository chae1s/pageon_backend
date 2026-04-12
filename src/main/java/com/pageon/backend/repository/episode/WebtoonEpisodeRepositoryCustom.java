package com.pageon.backend.repository.episode;

import com.pageon.backend.dto.response.episode.EpisodeSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface WebtoonEpisodeRepositoryCustom {

    Page<EpisodeSummaryResponse> findEpisodeSummaries(Long contentId, String sort, Pageable pageable);

}
