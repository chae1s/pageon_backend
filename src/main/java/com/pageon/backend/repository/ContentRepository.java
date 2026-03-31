package com.pageon.backend.repository;

import com.pageon.backend.common.enums.SeriesStatus;
import com.pageon.backend.dto.response.creator.content.ContentPerformanceStats;
import com.pageon.backend.dto.response.creator.content.ContentSimple;
import com.pageon.backend.dto.response.creator.content.ContentTab;
import com.pageon.backend.entity.Content;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Objects;
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

    @Query(value = "SELECT DISTINCT c FROM Content c " +
            "WHERE c.creator.id = :creatorId AND c.deletedAt IS NULL AND c.workStatus = 'PUBLISHED'")
    Page<Content> findByPublishedContentByCreatorId(Long creatorId, Pageable pageable);

    @Query(value = "SELECT DISTINCT c FROM Content c " +
            "WHERE c.creator.id = :creatorId AND c.title LIKE %:query% AND c.deletedAt IS NULL AND c.workStatus = 'PUBLISHED'")
    Page<Content> searchByTitle(Long creatorId, String query, Pageable pageable);

    @EntityGraph(attributePaths = {"contentKeywords.keyword"})
    Optional<Content> findByIdAndCreator_IdAndDeletedAtIsNull(Long contentId, Long userId);

    List<Content> creatorId(Long creatorId);

    @Query("SELECT new com.pageon.backend.dto.response.creator.content.ContentSimple(" +
            "c.id, c.title, c.dtype, c.serialDay, null ) " +
            "FROM Content c " +
            "WHERE c.id = :contentId AND c.creator.id = :creatorId")
    Optional<ContentSimple> findSimpleDtoByContentIdAndCreatorId(Long contentId, Long creatorId);

    @Query("SELECT c.status, COUNT(c) FROM Content c " +
            "WHERE c.creator.id = :creatorId GROUP BY c.status")
    List<Object[]> countGroupByStatus(@Param("creatorId")Long creatorId);


    @Query("SELECT new com.pageon.backend.dto.response.creator.content.ContentPerformanceStats(" +
            "SUM(c.viewCount), AVG(c.totalAverageRating), SUM(c.interestCount)) FROM Content c " +
            "WHERE c.creator.id = :creatorId AND c.deletedAt IS NULL")
    ContentPerformanceStats findTotalStatsByCreatorId(@Param("creatorId") Long creatorId);

    @Query("SELECT new com.pageon.backend.dto.response.creator.content.ContentTab(" +
            "c.id, c.title, c.cover, c.status, c.workStatus, c.dtype) " +
            "FROM Content c " +
            "WHERE c.creator.id = :creatorId AND c.status = 'ONGOING' AND c.workStatus != 'DELETED' AND c.workStatus != 'DELETING'")
    List<ContentTab> findAllByStatusOngoing(@Param("creatorId") Long creatorId);
}
