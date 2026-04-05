package com.pageon.backend.repository;

import com.pageon.backend.common.enums.EpisodeStatus;
import com.pageon.backend.dto.response.creator.episode.EpisodeList;
import com.pageon.backend.entity.WebnovelEpisode;
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

public interface WebnovelEpisodeRepository extends JpaRepository<WebnovelEpisode, Long> {

    List<WebnovelEpisode> findByWebnovelIdAndEpisodeStatus(Long id, EpisodeStatus episodeStatus);

    @Query("SELECT w FROM WebnovelEpisode w " +
            "JOIN FETCH w.webnovel wn " +
            "JOIN FETCH wn.creator " +
            "WHERE w.id = :episodeId AND w.episodeStatus = 'PUBLISHED' AND w.deletedAt IS NULL ")
    Optional<WebnovelEpisode> findWithWebnovelById(@Param("episodeId") Long episodeId);


    @Query("""
        SELECT e.id FROM WebnovelEpisode e 
        WHERE e.webnovel.id = :webnovelId
        AND e.episodeNum < :currentEpisodeNum
        AND e.deletedAt IS NULL 
        ORDER BY e.episodeNum DESC 
        LIMIT 1
            
    """)
    Long findPrevEpisodeId(@Param("webnovelId") Long webnovelId, @Param("currentEpisodeNum") int currentEpisodeNum);

    @Query("""
        SELECT e.id FROM WebnovelEpisode e 
        WHERE e.webnovel.id = :webnovelId
        AND e.episodeNum > :currentEpisodeNum
        AND e.deletedAt IS NULL 
        ORDER BY e.episodeNum ASC 
        LIMIT 1
    """)
    Long findNextEpisodeId(@Param("webnovelId") Long webnovelId, @Param("currentEpisodeNum") int currentEpisodeNum);

    @Query("SELECT MAX(e.episodeNum) FROM WebnovelEpisode e " +
            "WHERE e.webnovel.id = :contentId AND e.deletedAt IS NULL ")
    Optional<Integer> findMaxEpisodeNumByContentId(Long contentId);

    @Query("SELECT e.episodeStatus, COUNT(e) FROM WebnovelEpisode e " +
            "WHERE e.webnovel.id = :contentId AND e.deletedAt IS NULL  GROUP BY e.episodeStatus")
    List<Object[]> countGroupByStats(@Param("contentId") Long contentId);



    @Query("SELECT new com.pageon.backend.dto.response.creator.episode.EpisodeList(" +
            "e.id, e.episodeNum, e.episodeTitle, e.averageRating, e.episodeStatus, e.publishedAt, e.createdAt, e.viewCount) " +
            "FROM WebnovelEpisode e " +
            "WHERE e.webnovel.id = :contentId AND e.deletedAt IS NULL")
    Page<EpisodeList> findAllByWebnovel_id(@Param("contentId") Long contentId, Pageable pageable);

    @Query("SELECT new com.pageon.backend.dto.response.creator.episode.EpisodeList(" +
            "e.id, e.episodeNum, e.episodeTitle, e.averageRating, e.episodeStatus, e.publishedAt, e.createdAt, e.viewCount) " +
            "FROM WebnovelEpisode e " +
            "WHERE e.webnovel.id = :contentId AND e.episodeStatus = :episodeStatus AND e.deletedAt IS NULL")
    Page<EpisodeList> findByWebnovel_IdAndEpisodeStatus(Long contentId, @Param("episodeStatus") EpisodeStatus episodeStatus, Pageable pageable);

    @EntityGraph(attributePaths = {"webnovel.creator"})
    Optional<WebnovelEpisode> findByIdAndDeletedAtIsNull(Long episodeId);

    @Query("SELECT we FROM WebnovelEpisode we " +
            "WHERE we.publishedAt = :publishedAt AND we.episodeStatus = 'SCHEDULED' AND we.deletedAt IS NULL ")
    List<WebnovelEpisode> findAllByPublishedAt(LocalDate publishedAt);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE WebnovelEpisode we SET we.deletedAt = :deletedAt 
            WHERE we.webnovel.id = :contentId
            """)
    void bulkUpdateDeletedAt(@Param("contentId") Long contentId, @Param("deletedAt") LocalDateTime deletedAt);
}
