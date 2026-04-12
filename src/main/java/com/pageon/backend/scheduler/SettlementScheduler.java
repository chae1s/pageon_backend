package com.pageon.backend.scheduler;

import com.pageon.backend.common.annotation.ExecutionTimer;
import com.pageon.backend.service.creator.CreatorSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SettlementScheduler {

    private final CreatorSettlementService creatorSettlementService;

    @Scheduled(cron = "0 0 2 1 * *", zone = "Asia/Seoul")
    public void runSettlement() {
        LocalDateTime scheduledAt = LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        LocalDateTime testTime = LocalDateTime.now()
                .withMonth(2)
                .withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        creatorSettlementService.processSettlement(testTime);
    }

    @ExecutionTimer
    @Scheduled(cron = "0 0 2 10 * *", zone = "Asia/Seoul")
    public void runPayout() {
        LocalDateTime payoutDate = LocalDateTime.now()
                .withDayOfMonth(10)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        LocalDateTime testTime = LocalDateTime.now()
                .withMonth(2)
                .withDayOfMonth(10)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);


        creatorSettlementService.payoutSettlement(testTime);
    }

}
