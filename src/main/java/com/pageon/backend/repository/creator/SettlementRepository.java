package com.pageon.backend.repository.creator;

import com.pageon.backend.dto.record.PayoutTarget;
import com.pageon.backend.dto.response.creator.settlement.RevenueDetail;
import com.pageon.backend.dto.response.creator.settlement.SettlementSummary;
import com.pageon.backend.entity.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    @Query("SELECT COUNT(s) > 0 FROM Settlement s " +
           "WHERE s.creator.id = :creatorId AND s.periodStart = :periodStart AND s.periodEnd = :periodEnd")
    boolean existsSettlement(
            @Param("creatorId") Long creatorId, @Param("periodStart") LocalDateTime periodStart, @Param("periodEnd") LocalDateTime periodEnd);

    @Query("SELECT new com.pageon.backend.dto.response.creator.settlement.SettlementSummary(" +
            "s.id, s.settledPoint, s.scheduledAt, s.settlementStatus) " +
            "FROM Settlement s " +
            "WHERE s.creator.id = :creatorId ORDER BY s.scheduledAt DESC LIMIT 3")
    List<SettlementSummary> findLatestSettlements(@Param("creatorId") Long creatorId);

    @Query("SELECT new com.pageon.backend.dto.response.creator.settlement.RevenueDetail(" +
            "s.id, s.totalPoint, s.platformFee, s.taxFee, s.settledPoint, s.payoutDate)" +
            "FROM Settlement s " +
            "WHERE s.creator.id = :creatorId ORDER BY s.scheduledAt DESC LIMIT 1")
    Optional<RevenueDetail> findLatestSettlement(@Param("creatorId")Long creatorId);

    @Query("SELECT s FROM Settlement s " +
            "WHERE s.creator.id = :creatorId " +
            "ORDER BY s.scheduledAt DESC")
    Page<Settlement> findAllByCreatorId(@Param("creatorId") Long creatorId, Pageable pageable);

    @Query("SELECT new com.pageon.backend.dto.record.PayoutTarget(" +
            "s.creator.id, s) FROM Settlement s " +
            "WHERE s.settlementStatus = 'PENDING' " +
            "AND s.payoutDate = :payoutDate")
    List<PayoutTarget> findPayoutTarget(@Param("payoutDate")LocalDateTime payoutDate);
}
