package com.pageon.backend.repository.creator;

import com.pageon.backend.dto.record.SettlementTarget;
import com.pageon.backend.dto.response.creator.settlement.DailyRevenue;
import com.pageon.backend.entity.CreatorEarning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CreatorEarningRepository extends JpaRepository<CreatorEarning, Long> {

    @Query("""
    SELECT new com.pageon.backend.dto.record.SettlementTarget(
        ce.creator.id, SUM(ce.point)
    )
    FROM CreatorEarning ce
    WHERE ce.earningStatus = 'EARNED'
      AND ce.createdAt BETWEEN :startDate AND :endDate
    GROUP BY ce.creator.id
    HAVING SUM(ce.point) > 0
    """)
    List<SettlementTarget> findSettlementTargets(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Modifying(clearAutomatically = true)
    @Query("""
    UPDATE CreatorEarning ce SET ce.earningStatus = 'SETTLED' 
    WHERE ce.creator.id IN :creatorIds 
        AND ce.earningStatus = 'EARNED' 
        AND ce.createdAt BETWEEN :startDate AND :endDate
    """)
    void bulkUpdateStatusToSettled(@Param("creatorIds")List<Long> creatorIds,
                                   @Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(ce.point), 0) FROM CreatorEarning ce " +
            "WHERE ce.creator.id = :creatorId " +
            "AND ce.earningStatus = 'EARNED' " +
            "AND ce.createdAt >= :startDate")
    Long sumMonthlyEarnings(@Param("creatorId") Long creatorId, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT COALESCE(SUM(ce.point), 0) " +
            "FROM CreatorEarning ce " +
            "WHERE ce.creator.id = :creatorId " +
            "AND ce.earningStatus != 'CANCELED'")
    Long sumTotalEarnings(@Param("creatorId") Long creatorId);

    @Query("SELECT ce.content.id, ce.content.title, SUM(ce.point) as totalPoint " +
            "FROM CreatorEarning ce " +
            "WHERE ce.creator.id = :creatorId " +
            "AND ce.earningStatus != 'CANCELED' " +
            "GROUP BY ce.content.id, ce.content.title " +
            "ORDER BY totalPoint DESC limit 4")
    List<Object[]> findRevenueByContents(@Param("creatorId") Long creatorId);

    @Query("SELECT new com.pageon.backend.dto.response.creator.settlement.DailyRevenue(" +
            "CAST(ce.createdAt AS LOCALDATE), COALESCE(SUM(ce.point), 0) ) FROM CreatorEarning ce " +
            "WHERE ce.creator.id = :creatorId " +
            "AND ce.createdAt >= :startDate " +
            "GROUP BY CAST(ce.createdAt AS LOCALDATE) " +
            "ORDER BY CAST(ce.createdAt AS LOCALDATE) ASC")
    List<DailyRevenue> findDailyRevenue(@Param("creatorId") Long creatorId, @Param("startDate")LocalDateTime startDate);
}
