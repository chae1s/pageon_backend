package com.pageon.backend.service.cache;

import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.dto.mapping.EpisodeSummaryMapping;
import com.pageon.backend.dto.response.ContentResponse;
import com.pageon.backend.dto.response.PageResponse;
import com.pageon.backend.dto.response.content.ContentDetailResponse;
import com.pageon.backend.dto.response.episode.EpisodeCacheResponse;
import com.pageon.backend.dto.response.episode.EpisodeSummaryResponse;
import com.pageon.backend.entity.*;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.content.ContentRepository;
import com.pageon.backend.repository.KeywordRepository;
import com.pageon.backend.service.provider.ContentProvider;
import com.pageon.backend.service.provider.EpisodeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.serializer.RedisSerializer;
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
    private final RedisTemplate<String, Object> redisTemplate;
    private final List<EpisodeProvider> episodeProviders;

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
        SerialDay tomorrowSerialDay = getTomorrowSerialDay();

        List<ContentDetailResponse> contents = contentRepository.findContentDetails(tomorrowSerialDay);

        RedisSerializer<String> keySerializer = (RedisSerializer<String>) redisTemplate.getKeySerializer();
        RedisSerializer<Object> valueSerializer = (RedisSerializer<Object>) redisTemplate.getValueSerializer();

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            contents.forEach(content -> {
                byte[] keyBytes = keySerializer.serialize("contents:detail:" + content.getContentId());
                byte[] valueBytes = valueSerializer.serialize(content);
                if (keyBytes == null || valueBytes == null) return;
                connection.stringCommands().set(
                        keyBytes, valueBytes,
                        Expiration.seconds(Duration.ofHours(24).toSeconds()),
                        RedisStringCommands.SetOption.upsert()
                );
            });
            return null;
        });

        Map<ContentType, List<Long>> groupedIds = contents.stream()
                .collect(Collectors.groupingBy(
                        ContentDetailResponse::getContentType,
                        Collectors.mapping(ContentDetailResponse::getContentId, Collectors.toList())
                ));

        partitionContentIds(groupedIds.getOrDefault(ContentType.WEBNOVEL, List.of()), "webnovels");
        partitionContentIds(groupedIds.getOrDefault(ContentType.WEBTOON, List.of()), "webtoons");

    }

    private SerialDay getTomorrowSerialDay() {
        return SerialDay.valueOf(
                LocalDate.now().plusDays(1).getDayOfWeek().name()
        );
    }

    private void partitionContentIds(List<Long> contentIds, String contentType) {
        int batchSize = 500;

        for (int i = 0; i < contentIds.size(); i += batchSize) {
            List<Long> batchIds = contentIds.subList(i, Math.min(i + batchSize, contentIds.size()));
            processAndCacheEpisode(batchIds, contentType);
        }
    }

    private void processAndCacheEpisode(List<Long> contentIds, String contentType) {
        if (contentIds.isEmpty()) {
            return;
        }
        EpisodeProvider provider = getEpisodeProvider(contentType);

        List<EpisodeSummaryMapping> mappings = provider.findTop20EpisodesByContentIds(contentIds);

        Map<Long, List<EpisodeSummaryResponse>> episodeMap = mappings.stream()
                .collect(Collectors.groupingBy(
                        EpisodeSummaryMapping::getContentId,
                        Collectors.mapping(
                                EpisodeSummaryResponse::of,
                                Collectors.toList()
                        )
                ));

        RedisSerializer<String> keySerializer = (RedisSerializer<String>) redisTemplate.getKeySerializer();
        RedisSerializer<Object> valueSerializer = (RedisSerializer<Object>) redisTemplate.getValueSerializer();

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            episodeMap.forEach((contentId, episodes) -> {
                String redisKey = "episodes:summaries:" + contentId;
                boolean hasNext = episodes.size() > 20;
                if (hasNext) {
                    episodes.remove(20);
                }

                EpisodeCacheResponse cacheResponse = new EpisodeCacheResponse(episodes, hasNext);

                byte[] keyBytes = keySerializer.serialize(redisKey);
                byte[] valueBytes = valueSerializer.serialize(cacheResponse);

                if (keyBytes == null || valueBytes == null) return;

                connection.stringCommands().set(
                        keyBytes,
                        valueBytes,
                        Expiration.seconds(Duration.ofHours(24).toSeconds()),
                        RedisStringCommands.SetOption.upsert()
                );
            });

            return null;
        });

    }


    private ContentProvider getProvider(String contentType) {
        return providers.stream()
                .filter(p -> p.supports(contentType))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CONTENT_TYPE));
    }

    private EpisodeProvider getEpisodeProvider(String contentType) {
        return episodeProviders.stream()
                .filter(p -> p.supports(contentType))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CONTENT_TYPE));
    }
}
