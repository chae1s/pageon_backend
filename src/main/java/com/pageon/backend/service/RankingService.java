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
import com.pageon.backend.service.provider.ContentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final ContentRepository contentRepository;
    private final RankingRepository rankingRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Transactional
    @ExecutionTimer
    @Scheduled(cron = "0 0 * * * *")
    public void updateHourlyRanking() {

        LocalDateTime rankingHour = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);

        String timeSuffix = rankingHour.format((DateTimeFormatter.ofPattern("yyyyMMddHH")));

        String webnovelKey = "ranking:hourly:" + timeSuffix + ":WEBNOVEL";
        String webtoonKey = "ranking:hourly:" + timeSuffix + ":WEBTOON";

        Set<ZSetOperations.TypedTuple<String>> webnovelScores =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(webnovelKey, 0, 19);

        Set<ZSetOperations.TypedTuple<String>> webtoonScores =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(webtoonKey, 0, 19);


        saveHourlyRanking(webnovelScores, rankingHour, ContentType.WEBNOVEL);
        saveHourlyRanking(webtoonScores, rankingHour, ContentType.WEBTOON);


    }

    private void saveHourlyRanking(Set<ZSetOperations.TypedTuple<String>> scores, LocalDateTime rankingHour, ContentType contentType) {
        if (scores == null || scores.isEmpty()) {
            return;
        }

        Map<Long, Integer> scoreMap = scores.stream()
                .filter(tuple -> tuple.getValue() != null)
                .filter(tuple -> tuple.getScore() != null)
                .collect(Collectors.toMap(
                        tuple -> Long.valueOf(tuple.getValue()),
                        tuple -> tuple.getScore().intValue(),
                        (existing, replacement) -> existing
                ));

        Set<Long> contentIds = scoreMap.keySet();
        List<Content> contents = contentRepository.findAllByIdIn(contentIds);

        List<ContentRanking> rankings = new ArrayList<>();
        for (int i = 0; i < contents.size(); i++) {
            rankings.add(
                    ContentRanking.builder()
                            .content(contents.get(i))
                            .contentType(contentType)
                            .rankNo(i + 1)
                            .rankingHour(rankingHour)
                            .totalScore(Long.valueOf(scoreMap.get(contents.get(i).getId())))
                            .build()
            );

        }

        rankingRepository.saveAll(rankings);

        String type = (contentType == ContentType.WEBNOVEL) ? "webnovels" : "webtoons";

        getHourlyRankingList(contents, type, rankingHour);
    }


    @CachePut(value = "contents:hourly", key = "#contentType + ':' + #rankingHour")
    public List<ContentResponse.Simple> getHourlyRankingList(List<Content> contents, String contentType, LocalDateTime rankingHour) {

        return contents.stream()
                .limit(9)
                .map(ContentResponse.Simple::fromEntity)
                .collect(Collectors.toList());
    }

}
