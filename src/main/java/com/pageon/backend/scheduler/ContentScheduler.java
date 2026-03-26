package com.pageon.backend.scheduler;

import com.pageon.backend.common.annotation.ExecutionTimer;
import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.common.utils.PageableUtil;
import com.pageon.backend.service.ContentCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ContentScheduler {

    private final ContentCacheService contentCacheService;

    @ExecutionTimer
    @Scheduled(cron = "0 50 23 * * 0")
    public void updateDailyContents() {
        Pageable pageable = PageableUtil.redisPageable(18, "viewCount");

        for (SerialDay serialDay : SerialDay.values()) {
            contentCacheService.refreshDailyWebnovels(pageable, serialDay);
            contentCacheService.refreshDailyWebtoons(pageable, serialDay);
        }

    }

    @ExecutionTimer
    @Scheduled(cron = "0 50 23 * * 0")
    public void updateMasterpieceContents() {

        Pageable pageable = PageableUtil.redisPageable(6, "viewCount");

        contentCacheService.refreshCompletedAll(pageable);
        contentCacheService.refreshCompletedWebnovels(pageable);
        contentCacheService.refreshCompletedWebtoons(pageable);

    }

    @ExecutionTimer
    @Scheduled(cron = "0 50 23 * * 0")
    public void updateKeywordContents() {

        Pageable pageable = PageableUtil.redisPageable(6, "viewCount");

        contentCacheService.refreshKeywordWebnovels(pageable);
        contentCacheService.refreshKeywordWebtoons(pageable);

    }

    @ExecutionTimer
    @Scheduled(cron = "0 50 23 * * *")
    public void updateRecentContents() {

        Pageable pageable = PageableUtil.redisPageable(6, "createdAt");

        LocalDate baseline = LocalDate.now().plusDays(1);

        contentCacheService.refreshNewWebnovels(pageable, baseline);
        contentCacheService.refreshNewWebtoons(pageable, baseline);
    }


}
