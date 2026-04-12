package com.pageon.backend.repository.episode;

import com.pageon.backend.dto.response.episode.EpisodePurchaseResponse;

import java.util.List;

public interface EpisodePurchaseRepositoryCustom {

    List<EpisodePurchaseResponse> findEpisodePurchases(Long userId, List<Long> episodeIds);
}
