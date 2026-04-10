package com.pageon.backend.repository.episode;

import com.pageon.backend.common.enums.EpisodeStatus;
import com.pageon.backend.dto.response.creator.episode.EpisodeList;
import com.pageon.backend.entity.WebtoonEpisode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WebtoonEpisodeRepository extends JpaRepository<WebtoonEpisode, Long>, WebtoonEpisodeRepositoryCustom {


    List<WebtoonEpisode> findByWebtoonIdAndEpisodeStatus(Long id, EpisodeStatus episodeStatus);

    @Query("SELECT w FROM WebtoonEpisode w " +
            "JOIN FETCH w.webtoon wt " +
            "JOIN FETCH wt.creator " +
            "LEFT JOIN FETCH w.images " +
            "WHERE w.id = :episodeId AND w.episodeStatus = 'PUBLISHED' AND w.deletedAt IS NULL ")
    Optional<WebtoonEpisode> findWithWebtoonById(@Param("episodeId") Long episodeId);

    @Query("""
        SELECT e.id FROM WebtoonEpisode e 
        WHERE e.webtoon.id = :webtoonId
        AND e.episodeNum < :currentEpisodeNum
        AND e.deletedAt IS NULL 
        ORDER BY e.episodeNum DESC 
        LIMIT 1
            
    """)
    Long findPrevEpisodeId(@Param("webtoonId") Long webtoonId, @Param("currentEpisodeNum") int currentEpisodeNum);

    @Query("""
        SELECT e.id FROM WebtoonEpisode e 
        WHERE e.webtoon.id = :webtoonId
        AND e.episodeNum > :currentEpisodeNum 
        AND e.deletedAt IS NULL 
        ORDER BY e.episodeNum ASC 
        LIMIT 1
    """)
    Long findNextEpisodeId(@Param("webtoonId") Long webtoonId, @Param("currentEpisodeNum") int currentEpisodeNum);

    @Query("SELECT MAX(e.episodeNum) FROM WebtoonEpisode e " +
            "WHERE e.webtoon.id = :contentId AND e.deletedAt IS NULL")
    Optional<Integer> findMaxEpisodeNumByContentId(Long contentId);

    @Query("SELECT e.episodeStatus, COUNT(e) FROM WebtoonEpisode e " +
            "WHERE e.webtoon.id = :contentId AND e.deletedAt IS NULL GROUP BY e.episodeStatus")
    List<Object[]> countGroupByStats(@Param("contentId") Long contentId);

    @Query("SELECT new com.pageon.backend.dto.response.creator.episode.EpisodeList(" +
            "e.id, e.episodeNum, e.episodeTitle, e.averageRating, e.episodeStatus, e.publishedAt, e.createdAt, e.viewCount) " +
            "FROM WebtoonEpisode e " +
            "WHERE e.webtoon.id = :contentId AND e.deletedAt IS NULL")
    Page<EpisodeList> findAllByWebtoon_id(@Param("contentId") Long contentId, Pageable pageable);

    @Query("SELECT new com.pageon.backend.dto.response.creator.episode.EpisodeList(" +
            "e.id, e.episodeNum, e.episodeTitle, e.averageRating, e.episodeStatus, e.publishedAt, e.createdAt, e.viewCount) " +
            "FROM WebtoonEpisode e " +
            "WHERE e.webtoon.id = :contentId AND e.episodeStatus = :episodeStatus AND e.deletedAt IS NULL")
    Page<EpisodeList> findByWebtoon_IdAndEpisodeStatus(@Param("contentId") Long contentId, @Param("episodeStatus") EpisodeStatus episodeStatus, Pageable pageable);

    @EntityGraph(attributePaths = {"webtoon.creator", "images"})
    Optional<WebtoonEpisode> findByIdAndDeletedAtIsNull(Long episodeId);

    @Query("SELECT we FROM WebtoonEpisode we " +
            "JOIN FETCH we.webtoon w " +
            "JOIN FETCH w.creator c " +
            "JOIN FETCH c.user " +
            "WHERE we.publishedAt = :publishedAt AND we.episodeStatus = 'SCHEDULED' AND we.deletedAt IS NULL")
    List<WebtoonEpisode> findAllByPublishedAt(LocalDate publishedAt);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE WebtoonEpisode we SET we.deletedAt = :deletedAt 
            WHERE we.webtoon.id = :contentId
            """)
    void bulkUpdateDeletedAt(@Param("contentId") Long contentId, @Param("deletedAt") LocalDateTime deletedAt);
}
