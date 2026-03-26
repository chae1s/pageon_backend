package com.pageon.backend.repository;

import com.pageon.backend.entity.WebnovelEpisode;
import com.pageon.backend.entity.WebtoonEpisode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WebtoonEpisodeRepository extends JpaRepository<WebtoonEpisode, Long> {

    List<WebtoonEpisode> findByWebtoonId(Long id);
    Optional<WebtoonEpisode> findById(Long id);

    @Query("SELECT w FROM WebtoonEpisode w JOIN FETCH w.webtoon WHERE w.id = :episodeId")
    Optional<WebtoonEpisode> findWithWebtoonById(@Param("episodeId") Long episodeId);

    @Query("""
        SELECT e.id FROM WebtoonEpisode e 
        WHERE e.webtoon.id = :webtoonId
        AND e.episodeNum < :currentEpisodeNum
        ORDER BY e.episodeNum DESC 
        LIMIT 1
            
    """)
    Long findPrevEpisodeId(@Param("webtoonId") Long webtoonId, @Param("currentEpisodeNum") int currentEpisodeNum);

    @Query("""
        SELECT e.id FROM WebtoonEpisode e 
        WHERE e.webtoon.id = :webtoonId
        AND e.episodeNum > :currentEpisodeNum
        ORDER BY e.episodeNum ASC 
        LIMIT 1
    """)
    Long findNextEpisodeId(@Param("webtoonId") Long webtoonId, @Param("currentEpisodeNum") int currentEpisodeNum);
}
