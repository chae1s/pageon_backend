package com.pageon.backend.repository;

import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.entity.EpisodePurchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EpisodePurchaseRepository extends JpaRepository<EpisodePurchase,Long> {
    Optional<EpisodePurchase> findByUser_IdAndContentIdAndEpisodeId(Long userId, Long contentId, Long episodeId);
}
