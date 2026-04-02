package com.pageon.backend.repository;

import com.pageon.backend.common.enums.EpisodeStatus;
import com.pageon.backend.dto.response.creator.episode.EpisodeList;
import com.pageon.backend.entity.WebnovelEpisode;
import com.pageon.backend.entity.WebtoonEpisode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WebtoonEpisodeRepository extends JpaRepository<WebtoonEpisode, Long> {

    List<WebtoonEpisode> findByWebtoonId(Long id);

    @Query("SELECT w FROM WebtoonEpisode w " +
            "JOIN FETCH w.webtoon wt " +
            "JOIN FETCH wt.creator " +
            "LEFT JOIN FETCH w.images " +
            "WHERE w.id = :episodeId")
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

    @Query("SELECT MAX(e.episodeNum) FROM WebtoonEpisode e " +
            "WHERE e.webtoon.id = :contentId")
    Optional<Integer> findMaxEpisodeNumByContentId(Long contentId);

    @Query("SELECT e.episodeStatus, COUNT(e) FROM WebtoonEpisode e " +
            "WHERE e.webtoon.id = :contentId GROUP BY e.episodeStatus")
    List<Object[]> countGroupByStats(@Param("contentId") Long contentId);

    @Query("SELECT new com.pageon.backend.dto.response.creator.episode.EpisodeList(" +
            "e.id, e.episodeNum, e.episodeTitle, e.averageRating, e.episodeStatus, e.publishedAt, e.createdAt) " +
            "FROM WebtoonEpisode e " +
            "WHERE e.webtoon.id = :contentId AND e.deletedAt IS NULL")
    Page<EpisodeList> findAllByWebtoon_id(@Param("contentId") Long contentId, Pageable pageable);

    @Query("SELECT new com.pageon.backend.dto.response.creator.episode.EpisodeList(" +
            "e.id, e.episodeNum, e.episodeTitle, e.averageRating, e.episodeStatus, e.publishedAt, e.createdAt) " +
            "FROM WebtoonEpisode e " +
            "WHERE e.webtoon.id = :contentId AND e.episodeStatus = :episodeStatus AND e.deletedAt IS NULL")
    Page<EpisodeList> findByWebtoon_IdAndEpisodeStatus(@Param("contentId") Long contentId, @Param("episodeStatus") EpisodeStatus episodeStatus, Pageable pageable);

    @EntityGraph(attributePaths = {"webtoon", "images"})
    Optional<WebtoonEpisode> findById(Long episodeId);
}
