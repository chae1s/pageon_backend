package com.pageon.backend.repository.episode;

import com.pageon.backend.entity.EpisodePurchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EpisodePurchaseRepository extends JpaRepository<EpisodePurchase,Long>, EpisodePurchaseRepositoryCustom {
    Optional<EpisodePurchase> findByUser_IdAndContent_IdAndEpisodeId(Long userId, Long contentId, Long episodeId);
    List<EpisodePurchase> findByUser_IdAndContent_Id(Long userId, Long contentId);
}
