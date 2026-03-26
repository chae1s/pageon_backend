package com.pageon.backend.service;

import com.pageon.backend.common.annotation.ExecutionTimer;
import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.dto.response.ActionCountResponse;
import com.pageon.backend.dto.response.ContentResponse;
import com.pageon.backend.entity.Content;
import com.pageon.backend.entity.ContentActionLog;
import com.pageon.backend.entity.ContentRanking;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.ActionLogRepository;
import com.pageon.backend.repository.ContentRepository;
import com.pageon.backend.repository.RankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final ActionLogRepository actionLogRepository;
    private final ContentRepository contentRepository;
    private final RankingRepository rankingRepository;

    @Transactional
    @ExecutionTimer
    @Scheduled(cron = "0 0 * * * *")
    public void updateHourlyRanking() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime rankingHour = now.withMinute(0).withSecond(0).withNano(0);
        LocalDateTime startTime = now.minusHours(1);

        List<ActionCountResponse> actionCounts = actionLogRepository.countActionsByTimeRange(startTime, rankingHour);

        Map<ContentType, List<ActionCountResponse>> actionCountsMap = actionCounts.stream()
                .collect(Collectors.groupingBy(ActionCountResponse::getContentType));

        List<ContentRanking> rankings = new ArrayList<>();

        rankings.addAll(createRankingList(actionCountsMap.get(ContentType.WEBNOVEL), ContentType.WEBNOVEL, rankingHour));
        rankings.addAll(createRankingList(actionCountsMap.get(ContentType.WEBTOON), ContentType.WEBTOON, rankingHour));

        rankingRepository.saveAll(rankings);

        stopWatch.stop();
        log.info("실시간 랭킹 콘텐츠 저장 소요 시간: {}ms", stopWatch.getTotalTimeMillis());

    }

    private List<ContentRanking> createRankingList(List<ActionCountResponse> actionCounts, ContentType contentType, LocalDateTime rankingHour) {
        Map<Long, Long> scoreMap = new HashMap<>();
        for (ActionCountResponse actionCountResponse : actionCounts) {
            Long points = actionCountResponse.getTotalCount() * actionCountResponse.getActionType().getScore();
            scoreMap.merge(actionCountResponse.getContentId(), points, Long::sum);
        }

        List<Long> topIds = scoreMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(9)
                .map(Map.Entry::getKey).toList();

        List<Content> contents = contentRepository.findAllById(topIds);
        Map<Long, Content> contentMap = contents.stream()
                .collect(Collectors.toMap(Content::getId, Function.identity()));

        List<ContentRanking> contentRankings = new ArrayList<>();
        for (int i = 0; i < topIds.size(); i++) {
            Long contentId = topIds.get(i);
            Content content = contentMap.get(contentId);

            if (content != null) {
                contentRankings.add(ContentRanking.builder()
                        .content(content)
                        .contentType(contentType)
                        .rankNo(i + 1)
                        .totalScore(scoreMap.get(contentId))
                        .rankingHour(rankingHour)
                        .build());
            }
        }

        return contentRankings;
    }

    @Transactional(readOnly = true)
    public List<ContentResponse.Simple> getHourlyRankingContents(String contentType) {

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        LocalDateTime rankingHour = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        List<ContentRanking> contentRankings;
        switch (contentType) {
            case "all" -> contentRankings = rankingRepository.findAllRankings(rankingHour);
            case "webnovels" -> contentRankings = rankingRepository.findWebnovelRankings(rankingHour);
            case "webtoons" -> contentRankings = rankingRepository.findWebtoonRankings(rankingHour);
            default -> throw new CustomException(ErrorCode.INVALID_CONTENT_TYPE);
        }

        stopWatch.stop();
        log.info("실시간 랭킹 콘텐츠 조회 소요 시간: {}ms", stopWatch.getTotalTimeMillis());
        return contentRankings.stream().map(contentRanking -> ContentResponse.Simple.fromEntity(contentRanking.getContent())).toList();
    }
}
