package com.pageon.backend.repository;

import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.entity.Creator;
import com.pageon.backend.entity.Webnovel;
import com.pageon.backend.entity.Webtoon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WebtoonRepository extends JpaRepository<Webtoon, Long> {

    @Query("SELECT w FROM Webtoon w " +
            "JOIN FETCH w.creator " +
            "WHERE w.id = :webtoonId")
    Optional<Webtoon> findWithCreatorById(@Param("webtoonId") Long webtoonId);

    List<Webtoon> findByCreator(Creator creator);

    List<Webtoon> findByDeletedAtIsNull();

    @Query("SELECT w FROM Webtoon w " +
            "JOIN FETCH w.creator c " +
            "WHERE w.serialDay = :serialDay AND w.status = 'ONGOING' " +
            "AND w.deletedAt IS NULL")
    Page<Webtoon> findOngoingBySerialDay(SerialDay serialDay, Pageable pageable);

    @Query(value = "SELECT DISTINCT w FROM Webtoon w " +
            "JOIN FETCH w.creator c " +
            "JOIN w.contentKeywords k " +
            "WHERE k.keyword.name = :keyword",
            countQuery = "SELECT DISTINCT COUNT(w.id) FROM Webtoon w " +
                    "JOIN w.contentKeywords k " +
                    "WHERE k.keyword.name = :keyword"
    )
    Page<Webtoon> findAllByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query(value = "SELECT DISTINCT c FROM Webtoon c " +
            "JOIN FETCH c.creator " +
            "JOIN c.contentKeywords k " +
            "WHERE (c.title LIKE %:query% OR c.creator.penName LIKE %:query%) " +
            "AND c.deletedAt IS NULL",
            countQuery = "SELECT COUNT(DISTINCT c.id) FROM Webtoon c " +
                    "JOIN c.contentKeywords k " +
                    "WHERE (c.title LIKE %:query% OR c.creator.penName LIKE %:query%) " +
                    "AND c.deletedAt IS NULL "
    )
    Page<Webtoon> searchByTitleOrPenName(@Param("query") String query, Pageable pageable);

    // 최근 신작 조회(신작 등록 후 2주가 지나지 않은 콘텐츠만 리턴)
    @Query(value = "SELECT DISTINCT w FROM Webtoon w " +
            "JOIN FETCH w.creator " +
            "WHERE w.createdAt >= :since AND w.deletedAt IS NULL")
    Page<Webtoon> findAllNewArrivals(LocalDateTime since, Pageable pageable);

    // 정주행 작품 추천 (완결 작품 중 조회수 높은 작품 리스트 출력) -> WHERE 절의 평점 점수는 나중에 수정
    @Query(value = "SELECT DISTINCT w FROM Webtoon w " +
            "JOIN FETCH w.creator " +
            "WHERE w.status = 'COMPLETED' AND w.totalAverageRating >= 8 AND w.deletedAt IS NULL")
    Page<Webtoon> findTopRatedCompleted(Pageable pageable);
}
