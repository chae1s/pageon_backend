package com.pageon.backend.service.cache;

import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.dto.response.ContentResponse;
import com.pageon.backend.dto.response.PageResponse;
import com.pageon.backend.dto.response.content.ContentDetailResponse;
import com.pageon.backend.dto.response.episode.EpisodeSummaryResponse;
import com.pageon.backend.entity.*;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.content.ContentRepository;
import com.pageon.backend.repository.KeywordRepository;
import com.pageon.backend.repository.WebnovelRepository;
import com.pageon.backend.repository.WebtoonRepository;
import com.pageon.backend.repository.episode.WebnovelEpisodeRepository;
import com.pageon.backend.repository.episode.WebtoonEpisodeRepository;
import com.pageon.backend.service.provider.ContentProvider;
import com.pageon.backend.service.provider.EpisodeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentCacheService {

    private final ContentRepository contentRepository;
    private final KeywordRepository keywordRepository;
    private final List<ContentProvider> providers;
    private final WebnovelEpisodeRepository webnovelEpisodeRepository;
    private final WebtoonEpisodeRepository webtoonEpisodeRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @CachePut(value = "contents:daily", key = "#contentType + ':' + #serialDay")
    public List<ContentResponse.Simple> refreshDailyContents(String contentType, Pageable pageable, SerialDay serialDay) {
        log.info("Starting Redis cache WARM-UP {} for day: {} (Fetching 18 items)", contentType, serialDay);
        ContentProvider provider = getProvider(contentType);
        Page<? extends Content> contents = provider.findBySerialDay(serialDay, pageable);

        log.info("Successfully loaded 18 {} for {}. Ready to update cache.", contentType, serialDay);
        return contents.getContent().stream()
                .map(ContentResponse.Simple::fromEntity)
                .collect(Collectors.toList());

    }

    @CachePut(value = "contents:completed", key = "#contentType")
    public List<ContentResponse.Simple> refreshCompletedContents(String contentType, Pageable pageable) {
        log.info("Redis cache WARM-UP starting for completed {} (Target: 6 items)", contentType);
        ContentProvider provider = getProvider(contentType);
        Page<? extends Content> contents = provider.findByStatusCompleted(pageable);

        log.info("Successfully prepared 6 completed {} for cache update.", contentType);

        return contents.stream()
                .map(ContentResponse.Simple::fromEntity)
                .collect(Collectors.toList());
    }

    @CachePut(value = "contents:keyword", key = "#contentType")
    public ContentResponse.KeywordContent refreshKeywordContents(String contentType, Pageable pageable) {
        log.info("Redis cache WARM-UP starting for keyword {}} (Target: 6 items)", contentType);
        LocalDate date = LocalDate.now();
        Keyword keyword = keywordRepository.findValidKeyword(date).orElseThrow(
                () -> new CustomException(ErrorCode.INVALID_KEYWORD)
        );

        ContentProvider provider = getProvider(contentType);
        Page<? extends Content> contents = provider.findByKeyword(keyword.getName(), pageable);
        Page<ContentResponse.Simple> simpleContents = contents.map(ContentResponse.Simple::fromEntity);

        log.info("Successfully prepared 6 keyword {} for cache update.", contentType);
        return ContentResponse.KeywordContent.fromEntity(
                keyword.getName(), new PageResponse<>(simpleContents)
        );

    }


    @CachePut(value = "contents:new", key = "#contentType + ':' + #date")
    public List<ContentResponse.Simple> refreshNewContents(String contentType, Pageable pageable, LocalDate date) {

        log.info("Redis cache WARM-UP starting for new {} (Target: 6 items)", contentType);
        LocalDateTime since = date.minusDays(180).atStartOfDay();
        ContentProvider provider = getProvider(contentType);
        Page<? extends Content> contents = provider.findNewArrivals(since, pageable);

        log.info("Successfully prepared 6 new {} for cache update.", contentType);
        return contents.stream()
                .map(ContentResponse.Simple::fromEntity)
                .collect(Collectors.toList());
    }


    public void warmUpContentDetailBySerialDay() {
        String today = LocalDate.now().plusDays(1).getDayOfWeek().name();
        SerialDay serialDay = SerialDay.valueOf(today);

        List<ContentDetailResponse> contents = contentRepository.findContentDetails(serialDay);

        contents.forEach(content ->
                redisTemplate.opsForValue().set(
                    "contents:detail:" + content.getContentId(),
                    content,
                    Duration.ofHours(24)
                )
        );

        List<Long> webnovelIds = contents.stream()
                .filter(c -> c.getContentType().equals(ContentType.WEBNOVEL))
                .map(ContentDetailResponse::getContentId)
                .toList();

        List<Long> webtoonIds = contents.stream()
                .filter(c -> c.getContentType().equals(ContentType.WEBTOON))
                .map(ContentDetailResponse::getContentId)
                .toList();

        Map<Long, List<EpisodeSummaryResponse>> webnovelEpisodeMap = webnovelEpisodeRepository.findEpisodeSummariesByContentIds(webnovelIds);
        Map<Long, List<EpisodeSummaryResponse>> webtoonEpisodeMap = webtoonEpisodeRepository.findEpisodeSummariesByContentIds(webtoonIds);

        webnovelEpisodeMap.forEach((contentId, episodes) ->
                redisTemplate.opsForValue().set(
                        "episodes:summaries:" + contentId,
                        episodes,
                        Duration.ofHours(24)
                )
        );

        webtoonEpisodeMap.forEach((contentId, episodes) ->
                redisTemplate.opsForValue().set(
                        "episodes:summaries:" + contentId,
                        episodes,
                        Duration.ofHours(24)
                )
        );

    }

    private ContentProvider getProvider(String contentType) {
        return providers.stream()
                .filter(p -> p.supports(contentType))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CONTENT_TYPE));
    }
}
