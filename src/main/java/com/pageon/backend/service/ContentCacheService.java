package com.pageon.backend.service;

import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.dto.response.ContentResponse;
import com.pageon.backend.dto.response.PageResponse;
import com.pageon.backend.entity.Content;
import com.pageon.backend.entity.Keyword;
import com.pageon.backend.entity.Webnovel;
import com.pageon.backend.entity.Webtoon;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.ContentRepository;
import com.pageon.backend.repository.KeywordRepository;
import com.pageon.backend.repository.WebnovelRepository;
import com.pageon.backend.repository.WebtoonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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

    @CachePut(value = "contents:daily", key = "'webnovels:' + #serialDay")
    public List<ContentResponse.Simple> refreshDailyWebnovels(Pageable pageable, SerialDay serialDay) {

        log.info("Starting Redis cache WARM-UP webnovels for day: {} (Fetching 18 items)", serialDay);
        Page<Webnovel> webnovels = webnovelRepository.findOngoingBySerialDay(serialDay, pageable);

        log.info("Successfully loaded 18 webnovels for {}. Ready to update cache.", serialDay);
        return webnovels.getContent().stream()
                .map(ContentResponse.Simple::fromEntity)
                .collect(Collectors.toList());

    }

    @CachePut(value = "contents:daily", key = "'webtoons:' + #serialDay")
    public List<ContentResponse.Simple> refreshDailyWebtoons(Pageable pageable, SerialDay serialDay) {

        log.info("Starting Redis cache WARM-UP webtoons for day: {} (Fetching 18 items)", serialDay);
        Page<Webtoon> webtoons = webtoonRepository.findOngoingBySerialDay(serialDay, pageable);

        log.info("Successfully loaded 18 webtoons for {}. Ready to update cache.", serialDay);
        return webtoons.getContent().stream()
                .map(ContentResponse.Simple::fromEntity)
                .collect(Collectors.toList());

    }

    @CachePut(value = "contents:completed", key = "'all'")
    public List<ContentResponse.Simple> refreshCompletedAll(Pageable pageable) {

        log.info("Redis cache WARM-UP starting for completed all (Target: 6 items)");
        Page<Content> contents = contentRepository.findTopRatedCompleted(pageable);

        log.info("Successfully prepared 6 completed all for cache update.");

        return contents.stream()
                .map(ContentResponse.Simple::fromEntity)
                .collect(Collectors.toList());
    }

    @CachePut(value = "contents:completed", key = "'webnovels'")
    public List<ContentResponse.Simple> refreshCompletedWebnovels(Pageable pageable) {

        log.info("Redis cache WARM-UP starting for completed webnovels (Target: 6 items)");
        Page<Webnovel> webnovels = webnovelRepository.findTopRatedCompleted(pageable);

        log.info("Successfully prepared 6 completed webnovels for cache update.");
        return webnovels.stream()
                .map(ContentResponse.Simple::fromEntity)
                .collect(Collectors.toList());
    }

    @CachePut(value = "contents:completed", key = "'webtoons'")
    public List<ContentResponse.Simple> refreshCompletedWebtoons(Pageable pageable) {

        log.info("Redis cache WARM-UP starting for completed webtoons (Target: 6 items)");
        Page<Webtoon> webtoons = webtoonRepository.findTopRatedCompleted(pageable);

        log.info("Successfully prepared 6 completed webtoons for cache update.");
        return webtoons.stream()
                .map(ContentResponse.Simple::fromEntity)
                .collect(Collectors.toList());
    }

    @CachePut(value = "contents:keyword", key = "'webnovels'")
    public ContentResponse.KeywordContent refreshKeywordWebnovels(Pageable pageable) {

        log.info("Redis cache WARM-UP starting for keyword webnovels (Target: 6 items)");
        LocalDate date = LocalDate.now();
        Keyword keyword = keywordRepository.findValidKeyword(date).orElseThrow(
                () -> new CustomException(ErrorCode.INVALID_KEYWORD)
        );

        Page<Webnovel> webnovels = webnovelRepository.findAllByKeyword(keyword.getName(), pageable);
        Page<ContentResponse.Simple> contents = webnovels.map(ContentResponse.Simple::fromEntity);

        log.info("Successfully prepared 6 keyword webnovels for cache update.");
        return ContentResponse.KeywordContent.fromEntity(
                keyword.getName(), new PageResponse<>(contents)
        );

    }

    @CachePut(value = "contents:keyword", key = "'webtoons'")
    public ContentResponse.KeywordContent refreshKeywordWebtoons(Pageable pageable) {

        log.info("Redis cache WARM-UP starting for keyword webtoons (Target: 6 items)");
        LocalDate date = LocalDate.now();
        Keyword keyword = keywordRepository.findValidKeyword(date).orElseThrow(
                () -> new CustomException(ErrorCode.INVALID_KEYWORD)
        );

        Page<Webtoon> webtoons = webtoonRepository.findAllByKeyword(keyword.getName(), pageable);
        Page<ContentResponse.Simple> contents = webtoons.map(ContentResponse.Simple::fromEntity);

        log.info("Successfully prepared 6 keyword webtoons for cache update.");
        return ContentResponse.KeywordContent.fromEntity(
                keyword.getName(), new PageResponse<>(contents)
        );
    }

    @CachePut(value = "contents:new", key = "'webnovels:' + #date")
    public List<ContentResponse.Simple> refreshNewWebnovels(Pageable pageable, LocalDate date) {

        log.info("Redis cache WARM-UP starting for new webnovels (Target: 6 items)");
        LocalDateTime since = date.minusDays(180).atStartOfDay();

        Page<Webnovel> webnovels = webnovelRepository.findAllNewArrivals(since, pageable);

        log.info("Successfully prepared 6 new webnovels for cache update.");
        return webnovels.stream()
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
}
