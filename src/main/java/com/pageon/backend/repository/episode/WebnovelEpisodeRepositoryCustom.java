package com.pageon.backend.repository.episode;

import com.pageon.backend.dto.response.episode.EpisodeSummaryResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.Map;

public interface WebnovelEpisodeRepositoryCustom {

    Slice<EpisodeSummaryResponse> findEpisodeSummaries(Long contentId, String sort, Pageable pageable);

}
