package com.pageon.backend.service;

import com.pageon.backend.common.annotation.ExecutionTimer;
import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.dto.response.ContentResponse;
import com.pageon.backend.dto.response.PageResponse;
import com.pageon.backend.entity.*;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.ContentRepository;
import com.pageon.backend.repository.KeywordRepository;
import com.pageon.backend.repository.WebnovelRepository;
import com.pageon.backend.repository.WebtoonRepository;
import com.pageon.backend.service.provider.ContentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentCacheService {

    private final ContentRepository contentRepository;
    private final WebnovelRepository webnovelRepository;
    private final WebtoonRepository webtoonRepository;
    private final KeywordRepository keywordRepository;
    private final List<ContentProvider> providers;

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

    @CachePut(value = "contents:new", key = "'webntoons:' + #date")
    public List<ContentResponse.Simple> refreshNewWebtoons(Pageable pageable, LocalDate date) {

        log.info("Redis cache WARM-UP starting for new webtoons (Target: 6 items)");
        LocalDateTime since = date.minusDays(180).atStartOfDay();

        Page<Webtoon> webtoons = webtoonRepository.findAllNewArrivals(since, pageable);

        log.info("Successfully prepared 6 new webtoons for cache update.");
        return webtoons.stream()
                .map(ContentResponse.Simple::fromEntity)
                .collect(Collectors.toList());
    }

    private ContentProvider getProvider(String contentType) {
        return providers.stream()
                .filter(p -> p.supports(contentType))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CONTENT_TYPE));
    }
}
