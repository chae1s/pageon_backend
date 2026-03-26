package com.pageon.backend.repository;

import com.pageon.backend.entity.WebnovelEpisode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WebnovelEpisodeRepository extends JpaRepository<WebnovelEpisode, Long> {

    List<WebnovelEpisode> findByWebnovelId(Long id);
    Optional<WebnovelEpisode> findById(Long id);

    @Query("SELECT w FROM WebnovelEpisode w JOIN FETCH w.webnovel WHERE w.id = :episodeId")
    Optional<WebnovelEpisode> findWithWebnovelById(@Param("episodeId") Long episodeId);


    @Query("""
        SELECT e.id FROM WebnovelEpisode e 
        WHERE e.webnovel.id = :webnovelId
        AND e.episodeNum < :currentEpisodeNum
        ORDER BY e.episodeNum DESC 
        LIMIT 1
            
    """)
    Long findPrevEpisodeId(@Param("webnovelId") Long webnovelId, @Param("currentEpisodeNum") int currentEpisodeNum);

    @Query("""
        SELECT e.id FROM WebnovelEpisode e 
        WHERE e.webnovel.id = :webnovelId
        AND e.episodeNum > :currentEpisodeNum
        ORDER BY e.episodeNum ASC 
        LIMIT 1
    """)
    Long findNextEpisodeId(@Param("webnovelId") Long webnovelId, @Param("currentEpisodeNum") int currentEpisodeNum);
}
