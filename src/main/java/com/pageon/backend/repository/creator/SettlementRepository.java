package com.pageon.backend.repository.creator;

import com.pageon.backend.common.enums.SettlementStatus;
import com.pageon.backend.dto.record.PayoutTarget;
import com.pageon.backend.dto.response.creator.settlement.RevenueDetail;
import com.pageon.backend.dto.response.creator.settlement.SettlementSummary;
import com.pageon.backend.entity.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;

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
    List<PayoutTarget> findAllPayoutTargets(@Param("payoutDate")LocalDateTime payoutDate);

    @Query("SELECT s FROM Settlement s " +
            "JOIN FETCH s.creator c " +
            "JOIN FETCH c.creatorBankAccounts " +
            "WHERE s.id = :settlementId")
    Optional<Settlement> findPayoutTarget(@Param("settlementId")Long settlementId);

    @Query("SELECT s FROM Settlement s " +
            "JOIN FETCH s.creator c " +
            "JOIN FETCH c.user u " +
            "WHERE s.scheduledAt = :scheduledAt")
    Page<Settlement> findAllStatusByMonth(LocalDateTime scheduledAt, Pageable pageable);


    @Query("SELECT s FROM Settlement s " +
            "JOIN FETCH s.creator c " +
            "JOIN FETCH c.user u " +
            "WHERE s.settlementStatus = :settlementStatus " +
            "AND s.scheduledAt = :scheduledAt")
    Page<Settlement> findSettlementByStatusAndScheduledAt(SettlementStatus settlementStatus, LocalDateTime scheduledAt, Pageable pageable);

    @EntityGraph(attributePaths = {"creator.creatorBankAccounts"})
    Optional<Settlement> findById(Long settledMon);
}
