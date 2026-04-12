package com.pageon.backend.service;

import com.pageon.backend.common.annotation.ExecutionTimer;
import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.common.utils.PageableUtil;
import com.pageon.backend.dto.response.ContentResponse;
import com.pageon.backend.dto.response.EpisodeResponse;
import com.pageon.backend.dto.response.PageResponse;
import com.pageon.backend.dto.response.content.ContentDetailResponse;
import com.pageon.backend.entity.*;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.*;
import com.pageon.backend.repository.content.ContentRepository;
import com.pageon.backend.security.PrincipalUser;
import com.pageon.backend.service.handler.EpisodeActionHandler;
import com.pageon.backend.service.provider.ContentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {
    private final List<ContentProvider> providers;
    private final InterestRepository interestRepository;
    private final ContentRepository contentRepository;
    private final KeywordRepository keywordRepository;
    private final UserRepository userRepository;
    private final ReadingHistoryRepository readingHistoryRepository;
    private final EpisodeActionHandler actionHandler;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional(readOnly = true)
    public ContentResponse.Detail getContentDetail(PrincipalUser principalUser, String contentType, Long contentId) {

        log.info("Fetching {} details for content ID: {}", contentType, contentId);
        ContentProvider provider = getProvider(contentType);

        Content content = provider.findById(contentId).orElseThrow(
                () -> new CustomException(ErrorCode.CONTENT_NOT_FOUND)
        );

        Long userId = (principalUser != null) ? principalUser.getId() : null;
        List<EpisodeResponse.Summary> episodes = provider.findEpisodes(userId, contentId);

        Boolean isInterested = (userId != null) && interestRepository.existsByUser_IdAndContentId(userId, contentId);

        log.info("Successfully retrieved {}: {} (ID: {})", contentType, content.getTitle(), contentId);
        return ContentResponse.Detail.fromEntity(content, episodes, isInterested);
    }

    public ContentDetailResponse getContentDetail(Long userId, Long contentId) {

        CompletableFuture<ContentDetailResponse> contentFuture = CompletableFuture.supplyAsync(() -> {
            ContentDetailResponse content = (ContentDetailResponse) redisTemplate.opsForValue().get("contents:detail:" + contentId);

            if (content == null) {
                log.info("content db 조회");
                return contentRepository.findContentDetail(contentId).orElseThrow(
                        () -> new CustomException(ErrorCode.CONTENT_NOT_FOUND)
                );
            }
            log.info("content 캐시 조회");
            return content;
        });

        if (userId == null) {
            return contentFuture.join();
        }

        CompletableFuture<Boolean> interestFuture = CompletableFuture.supplyAsync(
                () -> interestRepository.existsByUser_IdAndContentId(userId, contentId)
        );

        return contentFuture.thenCombine(interestFuture, (content, isInterested) -> {
            content.setIsInterested(isInterested);
            return content;
        }).join();
    }

    @Transactional(readOnly = true)
    public Page<ContentResponse.Search> searchContentsByKeyword(String contentType, String keyword, Pageable pageable, String sort) {
        log.info("Searching for {} with keyword: '{}'", contentType, keyword);
        if (keyword.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_KEYWORD);
        }

        ContentProvider provider = getProvider(contentType);
        Pageable searchPageable = PageableUtil.searchPageable(pageable, sort);
        Page<? extends Content> contents = provider.findByKeyword(keyword, searchPageable);

        log.info("Search completed. Found {} {} for keyword: '{}'", contents.getTotalElements(), contentType, keyword);
        return contents.map(ContentResponse.Search::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ContentResponse.Search> searchContentsByTitleOrAuthor(String contentType, String query, Pageable pageable, String sort) {
        log.info("Searching for {} with title or creator: '{}'", contentType, query);
        if (query.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_SEARCH_QUERY);
        }
        Pageable searchPageable = PageableUtil.searchPageable(pageable, sort);
        ContentProvider provider = getProvider(contentType);

        Page<? extends Content> contents = provider.findByTitleOrPenName(query, searchPageable);

        log.info("Search completed. Found {} {} for title or creator: '{}'", contents.getTotalElements(), contentType, query);

        return contents.map(ContentResponse.Search::fromEntity);
    }


    @ExecutionTimer
    @Transactional(readOnly = true)
    @Cacheable(value = "contents:new", key = "#contentType + ':' + #date")
    public List<ContentResponse.Simple> getNewArrivalList(String contentType, LocalDate date) {

        log.info("Cache miss for new contents. Fetching the standard 6 {} from DB.", contentType);
        Pageable pageable = PageableUtil.redisPageable(6, "createdAt");
        ContentProvider provider = getProvider(contentType);

        LocalDateTime since = date.minusDays(180).atStartOfDay();
        Page<? extends Content> contents = provider.findNewArrivals(since, pageable);

        log.info("Successfully retrieved all 6 {} for new from DB.", contentType);
        return contents.stream().map(ContentResponse.Simple::fromEntity).collect(Collectors.toList());
    }

    @ExecutionTimer
    @Transactional(readOnly = true)
    public Page<ContentResponse.Simple> getNewArrivalPage(String contentType, Pageable pageable) {

        log.info("Fetching the 'See More' list of latest {} (Page: {})",
                contentType, pageable.getPageNumber() + 1);
        ContentProvider provider = getProvider(contentType);
        LocalDateTime since = LocalDate.now().minusDays(180).atStartOfDay();

        Pageable moreContentPageable = PageableUtil.moreContentPageable(pageable, "createdAt");
        Page<? extends Content> contents = provider.findNewArrivals(since, moreContentPageable);

        log.info("Successfully retrieved {} latest {} for page {}",
                contents.getNumberOfElements(), contentType, pageable.getPageNumber());

        return contents.map(ContentResponse.Simple::fromEntity);
    }

    @ExecutionTimer
    @Transactional(readOnly = true)
    @Cacheable(value = "contents:daily", key = "#contentType + ':' + #serialDay")
    public List<ContentResponse.Simple> getDailyScheduleList(String contentType, String serialDay) {

        log.info("Cache miss for {} contents. Fetching the standard 18 {} from DB.", serialDay, contentType);
        SerialDay.from(serialDay);
        ContentProvider provider = getProvider(contentType);
        Pageable pageable = PageableUtil.redisPageable(18, "viewCount");

        Page<? extends Content> contents = provider.findBySerialDay(SerialDay.valueOf(serialDay), pageable);

        log.info("Successfully retrieved all 18 {} for {} from DB.", contentType, serialDay);

        return contents.stream()
                .map(ContentResponse.Simple::fromEntity)
                .collect(Collectors.toList());
    }

    @ExecutionTimer
    @Transactional(readOnly = true)
    @Cacheable(value = "contents:completed", key = "#contentType")
    public List<ContentResponse.Simple> getBestCompletedList(String contentType) {

        log.info("Cache miss for completed contents. Fetching the standard 6 {} from DB.", contentType);
        Pageable pageable = PageableUtil.redisPageable(6, "viewCount");

        ContentProvider provider = getProvider(contentType);
        Page<? extends Content> contents = provider.findByStatusCompleted(pageable);

        log.info("Successfully retrieved all 6 {} for completed from DB.", contentType);
        return contents.stream()
                .map(ContentResponse.Simple::fromEntity)
                .collect(Collectors.toList());
    }

    @ExecutionTimer
    @Transactional(readOnly = true)
    public Page<ContentResponse.Simple> getBestCompletedPage(String contentType, Pageable pageable) {

        log.info("Fetching the 'See More' list of completed {} (Page: {})",
                contentType, pageable.getPageNumber() + 1);
        Pageable moreContentPageable = PageableUtil.moreContentPageable(pageable, "viewCount");
        ContentProvider provider = getProvider(contentType);

        Page<? extends Content> contents = provider.findByStatusCompleted(moreContentPageable);

        log.info("Successfully retrieved {} recommended completed {} for page {}",
                contents.getNumberOfElements(), contentType, pageable.getPageNumber());
        return contents.map(ContentResponse.Simple::fromEntity);
    }

    @ExecutionTimer
    @Transactional(readOnly = true)
    @Cacheable(value = "contents:keyword", key = "#contentType")
    public ContentResponse.KeywordContent getFeaturedKeywordContentsList(String contentType) {

        log.info("Cache miss for keyword contents. Fetching the standard 6 {} from DB.", contentType);
        LocalDate currentDate = LocalDate.now();
        Pageable pageable = PageableUtil.redisPageable(6, "viewCount");

        Keyword keyword = keywordRepository.findValidKeyword(currentDate).orElseThrow(
                () -> new CustomException(ErrorCode.INVALID_KEYWORD)
        );

        ContentProvider provider = getProvider(contentType);
        Page<? extends Content> contents = provider.findByKeyword(keyword.getName(), pageable);
        Page<ContentResponse.Simple> contentsList = contents.map(ContentResponse.Simple::fromEntity);

        log.info("Successfully retrieved all 6 {} for keyword from DB.", contentType);

        return ContentResponse.KeywordContent.fromEntity(
                keyword.getName(), new PageResponse<>(contentsList)
        );

    }

    @ExecutionTimer
    @Transactional(readOnly = true)
    public ContentResponse.KeywordContent getFeaturedKeywordContentsPage(String contentType, Pageable pageable) {

        log.info("Fetching the 'See More' list of keyword {} (Page: {})",
                contentType, pageable.getPageNumber() + 1);
        LocalDate currentDate = LocalDate.now();

        Keyword keyword = keywordRepository.findValidKeyword(currentDate).orElseThrow(
                () -> new CustomException(ErrorCode.INVALID_KEYWORD)
        );

        Pageable moreContentPageable = PageableUtil.moreContentPageable(pageable, "viewCount");

        ContentProvider provider = getProvider(contentType);
        Page<? extends Content> contents = provider.findByKeyword(keyword.getName(), moreContentPageable);

        Page<ContentResponse.Simple> contentsList = contents.map(ContentResponse.Simple::fromEntity);

        log.info("Successfully retrieved {} recommended keyword {} for page {}",
                contents.getNumberOfElements(), contentType, pageable.getPageNumber());

        return ContentResponse.KeywordContent.fromEntity(
                keyword.getName(),
                new PageResponse<>(contentsList)
        );

    }

    @ExecutionTimer
    @Transactional(readOnly = true)
    @Cacheable(value = "contents:hourly", key = "#contentType + ':' + #timeSuffix")
    public List<ContentResponse.Simple> getHourlyRankingList(String contentType, String timeSuffix, LocalDateTime rankingHour) {
        ContentProvider provider = getProvider(contentType);

        List<ContentRanking> rankings = provider.findAllHourlyRankings(rankingHour);

        return rankings.stream()
                .map(ranking -> ContentResponse.Simple.fromEntity(ranking.getContent()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void toggleInterest(Long userId, Long contentId) {
        log.info("Toggling interest status for User ID: {} and Content ID: {}", userId, contentId);
        Optional<Interest> existingInterest = interestRepository.findByUser_IdAndContentId(userId, contentId);

        Content content = contentRepository.findByIdAndDeletedAtIsNull(contentId).orElseThrow(
                () -> new CustomException(ErrorCode.CONTENT_NOT_FOUND)
        );

        if (existingInterest.isPresent()) {
            interestRepository.delete(existingInterest.get());
            content.updateInterestCount(-1);
            log.info("Successfully REMOVED interest for User: {} on Content: {}", userId, contentId);
        } else {
            User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                    () -> new CustomException(ErrorCode.USER_NOT_FOUND)
            );

            Interest interest = Interest.builder()
                    .user(user)
                    .content(content)
                    .build();

            interestRepository.save(interest);
            content.updateInterestCount(1);
            actionHandler.handleInterestContent(userId, contentId, ContentType.valueOf(content.getDtype()));

            log.info("Successfully ADDED interest for User: {} on Content: {}", userId, contentId);
        }
    }

    @Transactional(readOnly = true)
    public Page<ContentResponse.InterestContent> getInterestContents(Long userId, String contentType, Pageable pageable, String sort) {
        Pageable interestPageable = PageableUtil.interestPageable(pageable, sort);
        ContentProvider provider = getProvider(contentType);

        Page<Interest> interests = provider.findByInterest(userId, interestPageable);

        return interests.map(interest -> ContentResponse.InterestContent.fromEntity(interest.getContent()));
    }

    @Transactional(readOnly = true)
    public Page<ContentResponse.RecentRead> getReadingHistory(Long userId, String contentType, String sort, Pageable pageable) {
        Pageable readingHistoryPageable = PageableUtil.readingHistoryPageable(pageable, sort);
        ContentProvider provider = getProvider(contentType);

        Page<ReadingHistory> histories = provider.findByReadingHistory(userId, readingHistoryPageable);

        return histories.map(ContentResponse.RecentRead::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<ContentResponse.Simple> getTodayReadingHistory(Long userId) {
        SerialDay today = SerialDay.valueOf(LocalDate.now().getDayOfWeek().name());

        List<ReadingHistory> histories = readingHistoryRepository.findWithContentByUserIdAndSerialDay(userId, today);

        return histories.stream()
                .map(h -> ContentResponse.Simple.fromEntity(h.getContent()))
                .toList();
    }

    private ContentProvider getProvider(String contentType) {
        return providers.stream()
                .filter(p -> p.supports(contentType))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CONTENT_TYPE));
    }

}
