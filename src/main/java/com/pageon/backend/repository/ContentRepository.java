package com.pageon.backend.repository;

import com.pageon.backend.common.enums.SeriesStatus;
import com.pageon.backend.entity.Content;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContentRepository extends JpaRepository<Content, Long> {

    Optional<Content> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT c FROM Content c " +
            "JOIN FETCH c.creator " +
            "JOIN c.contentKeywords " +
            "WHERE c.id = :contentId")
    Optional<Content> findByIdWithDetailInfo(Long contentId);

    @Query(value = "SELECT DISTINCT c FROM Content c " +
            "JOIN FETCH c.creator " +
            "JOIN c.contentKeywords k " +
            "WHERE (c.title LIKE %:query% OR c.creator.penName LIKE %:query%) " +
            "AND c.deletedAt IS NULL",
            countQuery = "SELECT COUNT(DISTINCT c.id) FROM Content c " +
                    "JOIN c.contentKeywords k " +
                    "WHERE (c.title LIKE %:query% OR c.creator.penName LIKE %:query%) " +
                    "AND c.deletedAt IS NULL "
    )
    Page<Content> searchByTitleOrPenName(@Param("query") String query, Pageable pageable);


    @Query(value = "SELECT DISTINCT c FROM Content c " +
            "JOIN FETCH c.creator " +
            "WHERE c.status = 'COMPLETED' AND c.totalAverageRating >= 8 AND c.deletedAt IS NULL")
    Page<Content> findTopRatedCompleted(Pageable pageable);

    @EntityGraph(attributePaths = {"creator", "contentKeywords.keyword"})
    Page<Content> findByCreator_IdAndStatusAndDeletedAtIsNull(Long creatorId, SeriesStatus status, Pageable pageable);

    Page<Content> findByCreator_IdAndDeletedAtIsNull(Long creatorId, Pageable pageable);

    @Query(value = "SELECT DISTINCT c FROM Content c " +
            "WHERE c.creator.id = :creatorId AND c.title LIKE %:query%")
    Page<Content> searchByTitle(Long creatorId, String query, Pageable pageable);

    @EntityGraph(attributePaths = {"contentKeywords.keyword"})
    Optional<Content> findByIdAndCreator_IdAndDeletedAtIsNull(Long contentId, Long userId);
}
