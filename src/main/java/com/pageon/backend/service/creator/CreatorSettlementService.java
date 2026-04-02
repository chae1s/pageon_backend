package com.pageon.backend.service.creator;

import com.pageon.backend.common.annotation.ExecutionTimer;
import com.pageon.backend.common.enums.SettlementStatus;
import com.pageon.backend.dto.record.PayoutTarget;
import com.pageon.backend.dto.record.SettlementTarget;
import com.pageon.backend.dto.response.creator.settlement.SettlementSummary;
import com.pageon.backend.dto.response.creator.settlement.SettlementDetail;
import com.pageon.backend.entity.Creator;
import com.pageon.backend.entity.CreatorBankAccount;
import com.pageon.backend.entity.Settlement;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.CreatorRepository;
import com.pageon.backend.repository.creator.CreatorBankAccountRepository;
import com.pageon.backend.repository.creator.CreatorEarningRepository;
import com.pageon.backend.repository.creator.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreatorSettlementService {

    private static final double PLATFORM_FEE_RATE = 0.3;
    private static final double TAX_FEE_RATE = 0.033;

    private final SettlementRepository settlementRepository;
    private final CreatorRepository creatorRepository;
    private final CreatorEarningRepository creatorEarningRepository;
    private final CreatorBankAccountRepository creatorBankAccountRepository;

    @ExecutionTimer
    public void processSettlement(LocalDateTime scheduledAt) {
        LocalDateTime periodStart = scheduledAt.minusMonths(1);
        LocalDateTime periodEnd = scheduledAt.minusDays(1).withHour(23).withMinute(59).withSecond(59);

        List<SettlementTarget> targets = creatorEarningRepository.findSettlementTargets(periodStart, periodEnd);
        List<Settlement> settlements = new ArrayList<>();

        List<Long> creatorIds = targets.stream()
                .map(SettlementTarget::creatorId)
                .toList();

        Map<Long, Creator> creatorMap = creatorRepository.findAllById(creatorIds)
                .stream()
                .collect(Collectors.toMap(Creator::getId, creator -> creator));

        for (SettlementTarget target : targets) {
            try {
                if (settlementRepository.existsSettlement(target.creatorId(), periodStart, periodEnd)) {
                    continue;
                }
                Creator creator = creatorMap.get(target.creatorId());
                if (creator == null) {
                    continue;
                }

                settlements.add(registerSettlement(creator, target.totalPoint(), scheduledAt, periodStart, periodEnd));
            } catch (Exception e) {
                log.error("creatorId: {} 정산 실패 - {}", target.creatorId(), e.getMessage());
            }
        }

        settlementRepository.saveAll(settlements);

    }

    private Settlement registerSettlement(
            Creator creator, Long totalPoint, LocalDateTime scheduledAt,
            LocalDateTime periodStart, LocalDateTime periodEnd
    ) {

        int platformFee = (int) (totalPoint * PLATFORM_FEE_RATE);
        int taxFee = (int) (totalPoint * TAX_FEE_RATE);
        int settledPoint = Math.toIntExact(totalPoint - platformFee - taxFee);

        return Settlement.builder()
                .creator(creator)
                .totalPoint(totalPoint.intValue())
                .settlementStatus(SettlementStatus.PENDING)
                .platformFee(platformFee)
                .taxFee(taxFee)
                .settledPoint(settledPoint)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .scheduledAt(scheduledAt)
                .payoutDate(scheduledAt.withDayOfMonth(10))
                .build();

    }

    public Page<SettlementDetail> getSettlementHistory(Long userId, Pageable pageable) {
        Creator creator = getCreator(userId);
        Page<Settlement> settlements = settlementRepository.findAllByCreatorId(creator.getId(), pageable);

        return settlements.map(SettlementDetail::of);
    }

    public List<SettlementSummary> getRecentSettlements(Long userId) {
        Creator creator = getCreator(userId);

        return settlementRepository.findLatestSettlements(creator.getId());

    }

    private Creator getCreator(Long userId) {
        return creatorRepository.findByUser_Id(userId).orElseThrow(
                () -> new CustomException(ErrorCode.CREATOR_NOT_FOUND)
        );
    }

    @Transactional
    public void payoutSettlement(LocalDateTime payoutDate) {
        List<PayoutTarget> targets = settlementRepository.findPayoutTarget(payoutDate);

        List<Long> creatorIds = targets.stream()
                .map(PayoutTarget::creatorId)
                .toList();

        Map<Long, CreatorBankAccount> accountMap = creatorBankAccountRepository.findAllByCreatorIdInAndIsValidTrue(creatorIds)
                .stream()
                .collect(Collectors.toMap(
                        c -> c.getCreator().getId(),
                        c -> c
                ));

        for (PayoutTarget target : targets) {
            try {

                CreatorBankAccount account = accountMap.get(target.creatorId());
                if (account == null) {
                    target.settlement().fail("유효한 계좌 정보 없음");
                    continue;
                }

                // 정산 금액 지급 메소드 실행

                target.settlement().complete(payoutDate);

            } catch (Exception e) {
                log.error("creatorId: {} 정산 금액 지급 실패 - {}", target.creatorId(), e.getMessage());
            }
        }


    }




}
