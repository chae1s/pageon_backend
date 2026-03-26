package com.pageon.backend.repository;

import com.pageon.backend.dto.response.ActionCountResponse;
import com.pageon.backend.entity.ContentActionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ActionLogRepository extends JpaRepository<ContentActionLog, Long> {

    @Query("SELECT new com.pageon.backend.dto.response.ActionCountResponse(" +
            "c.contentId, c.contentType, c.actionType, " +
            "SUM(CASE " +
            "WHEN c.actionType = 'RATING' " +
            "THEN c.ratingScore " +
            "ELSE 1 " +
            "END)" +
            ") " +
            "FROM ContentActionLog c " +
            "WHERE c.createdAt >= :startTime AND c.createdAt < :endTime GROUP BY c.contentId, c.actionType, c.contentType")
    List<ActionCountResponse> countActionsByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
