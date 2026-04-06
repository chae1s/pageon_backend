package com.pageon.backend.repository;

import com.pageon.backend.entity.ContentRanking;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RankingRepository extends JpaRepository<ContentRanking, Long> {

    @Query("SELECT DISTINCT r FROM ContentRanking r " +
            "JOIN FETCH r.content c " +
            "JOIN FETCH c.creator w " +
            "WHERE r.rankingHour = :rankingHour ORDER BY r.rankNo, r.content.id")
    List<ContentRanking> findAllHourlyRankings(@Param("rankingHour")LocalDateTime rankingHour, Pageable pageable);

    @Query("SELECT DISTINCT r FROM ContentRanking r " +
            "JOIN FETCH r.content c " +
            "JOIN FETCH c.creator w " +
            "WHERE r.contentType = 'WEBNOVEL' AND r.rankingHour = :rankingHour ORDER By r.rankNo")
    List<ContentRanking> findAllWebnovelRankings(@Param("rankingHour")LocalDateTime rankingHour, Pageable pageable);

    @Query("SELECT DISTINCT r FROM ContentRanking r " +
            "JOIN FETCH r.content c " +
            "JOIN FETCH c.creator w " +
            "WHERE r.contentType = 'WEBTOON' AND r.rankingHour = :rankingHour ORDER By r.rankNo")
    List<ContentRanking> findAllWebtoonRankings(@Param("rankingHour")LocalDateTime rankingHour, Pageable pageable);
}
