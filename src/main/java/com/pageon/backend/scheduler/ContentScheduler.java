package com.pageon.backend.scheduler;

import com.pageon.backend.common.annotation.ExecutionTimer;
import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.common.utils.PageableUtil;
import com.pageon.backend.service.ContentCacheService;
import com.pageon.backend.service.CreatorEpisodeService;
import com.pageon.backend.service.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ContentScheduler {

    private final ContentCacheService contentCacheService;
    private final CreatorEpisodeService creatorEpisodeService;
    private final RankingService rankingService;

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

    @ExecutionTimer
    @Scheduled(cron = "0 50 23 * * *")
    public void runDailyPublishing() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        creatorEpisodeService.publishScheduledWebnovelEpisodes(tomorrow);
        creatorEpisodeService.publishScheduledWebtoonEpisodes(tomorrow);

    }

    @ExecutionTimer
    @Scheduled(cron = "0 0 * * * *")
    public void runHourlyRanking() {
        rankingService.updateHourlyRanking();
    }


}
