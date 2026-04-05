package com.pageon.backend.service.admin;

import com.pageon.backend.common.enums.SettlementStatus;
import com.pageon.backend.dto.response.admin.settlement.SettlementDetail;
import com.pageon.backend.dto.response.admin.settlement.SettlementSummary;
import com.pageon.backend.entity.CreatorBankAccount;
import com.pageon.backend.entity.Settlement;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.creator.SettlementRepository;
import com.pageon.backend.service.IdempotentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class AdminSettlementService {

    private final SettlementRepository settlementRepository;
    private final IdempotentService idempotentService;

    public Page<SettlementSummary> getSettlementStatusByStatus(Integer month, Pageable pageable, String status) {

        LocalDateTime choiceMonth = LocalDateTime.now()
                .withMonth(month).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        if (status.equals("ALL")) {
            Page<Settlement> settlements = settlementRepository.findAllStatusByMonth(choiceMonth, pageable);

            return settlements.map(SettlementSummary::of);
        }

        Page<Settlement> settlements = settlementRepository.findSettlementByStatusAndScheduledAt(SettlementStatus.valueOf(status), choiceMonth, pageable);

        return settlements.map(SettlementSummary::of);

    }

    public SettlementDetail getSettlementDetail(Long settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId).orElseThrow(
                () -> new CustomException(ErrorCode.SETTLEMENT_NOT_FOUND)
        );

        return SettlementDetail.of(settlement, checkAccountIsValid(settlement));
    }

    @Transactional
    public void retryPayout(Long settlementId, Long creatorId) {

        Settlement settlement = settlementRepository.findPayoutTarget(settlementId).orElseThrow(
                () -> new CustomException(ErrorCode.SETTLEMENT_NOT_FOUND)
        );

        String[] key = {settlementId.toString(), creatorId.toString(), settlement.getScheduledAt().toString(), settlement.getSettledPoint().toString()};
        idempotentService.isValidIdempotent(Arrays.asList(key));


        if (!creatorId.equals(settlement.getCreator().getId())) {
            throw new CustomException(ErrorCode.INVALID_SETTLEMENT_OWNER);
        }

        if (!checkAccountIsValid(settlement)) {
            throw new CustomException(ErrorCode.INVALID_BANK_ACCOUNT);
        }

        settlement.complete(LocalDateTime.now());
    }

    private boolean checkAccountIsValid(Settlement settlement) {
        return settlement.getCreator().getCreatorBankAccounts().stream()
                .anyMatch(CreatorBankAccount::getIsValid);
    }
}
