package com.pageon.backend.scheduler;

import com.pageon.backend.common.annotation.ExecutionTimer;
import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.common.utils.PageableUtil;
import com.pageon.backend.service.ActionLogService;
import com.pageon.backend.service.cache.ContentCacheService;
import com.pageon.backend.service.RankingService;
import com.pageon.backend.service.creator.EpisodePublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ContentScheduler {

    private final ContentCacheService contentCacheService;
    private final EpisodePublishService episodePublishService;
    private final RankingService rankingService;
    private final ActionLogService actionLogService;

    @ExecutionTimer
    @Scheduled(cron = "0 50 23 * * 0")

    public void updateDailyContents() {
        Pageable pageable = PageableUtil.redisPageable(18, "viewCount");

        for (SerialDay serialDay : SerialDay.values()) {
            contentCacheService.refreshDailyContents("webnovels", pageable, serialDay);
            contentCacheService.refreshDailyContents("webtoons", pageable, serialDay);
        }

    }

    @ExecutionTimer
    @Scheduled(cron = "0 50 23 * * 0")
    public void updateMasterpieceContents() {

        Pageable pageable = PageableUtil.redisPageable(6, "viewCount");

        contentCacheService.refreshCompletedContents("all", pageable);
        contentCacheService.refreshCompletedContents("webnovels", pageable);
        contentCacheService.refreshCompletedContents("webtoons", pageable);

    }

    @ExecutionTimer
    @Scheduled(cron = "0 50 23 * * 0")
    public void updateKeywordContents() {

        Pageable pageable = PageableUtil.redisPageable(6, "viewCount");

        contentCacheService.refreshKeywordContents("webnovels", pageable);
        contentCacheService.refreshKeywordContents("webtoons", pageable);

    }

    @ExecutionTimer
    @Scheduled(cron = "0 50 23 * * *")
    public void updateRecentContents() {

        Pageable pageable = PageableUtil.redisPageable(6, "createdAt");

        LocalDate baseline = LocalDate.now().plusDays(1);

        contentCacheService.refreshNewContents("webnovels", pageable, baseline);
        contentCacheService.refreshNewContents("webtoons", pageable, baseline);

    }

    @ExecutionTimer
    @Scheduled(cron = "0 50 23 * * *")
    public void updateContentDetail() {
        contentCacheService.warmUpContentDetailBySerialDay();
    }

    @ExecutionTimer
    @Scheduled(cron = "0 50 23 * * *")
    public void runDailyPublishing() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        episodePublishService.publishScheduledWebnovelEpisodes(tomorrow);
        episodePublishService.publishScheduledWebtoonEpisodes(tomorrow);

    }

    @ExecutionTimer
    @Scheduled(cron = "0 0 * * * *")
    public void runHourlyRanking() {
        rankingService.updateHourlyRanking();
        actionLogService.consumeActionLog();
    }


}
