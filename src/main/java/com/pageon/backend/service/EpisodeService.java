package com.pageon.backend.service;

import com.pageon.backend.dto.request.EpisodeRatingRequest;
import com.pageon.backend.dto.response.episode.EpisodeCacheResponse;
import com.pageon.backend.dto.response.episode.EpisodePurchaseResponse;
import com.pageon.backend.dto.response.episode.EpisodeSummaryResponse;
import com.pageon.backend.entity.User;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.episode.EpisodePurchaseRepository;
import com.pageon.backend.repository.UserRepository;
import com.pageon.backend.service.provider.EpisodeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeService {
    private final List<EpisodeProvider> providers;
    private final UserRepository userRepository;
    private final IdempotentService idempotentService;
    private final EpisodePurchaseRepository episodePurchaseRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public Slice<EpisodeSummaryResponse> getEpisodeSummaries(Long userId, String contentType, Long contentId, String sort, Pageable pageable) {
        EpisodeProvider provider = getProvider(contentType);
        EpisodeCacheResponse cached = (EpisodeCacheResponse) redisTemplate.opsForValue().get("episodes:summaries:" + contentId);

        Slice<EpisodeSummaryResponse> episodes;

        if (pageable.getPageNumber() == 0 && sort.equals("recent") && cached != null) {
            episodes = new SliceImpl<>(cached.getEpisodes(), pageable, cached.isHasNext());
            log.info("episodes 캐시 조회");
        } else {
            episodes = provider.findEpisodeSummaries(contentId, sort, pageable);
            log.info("episodes DB 조회");
        }

        List<Long> episodeIds = episodes.stream().map(EpisodeSummaryResponse::getEpisodeId).toList();

        if (userId != null && !episodes.isEmpty()) {
            Map<Long, EpisodePurchaseResponse> purchaseMap = getPurchaseMap(userId, episodeIds);

            episodes.forEach(episode -> {
                EpisodePurchaseResponse purchaseResponse = purchaseMap.get(episode.getEpisodeId());
                if (purchaseResponse != null) {
                    episode.setEpisodePurchase(purchaseResponse);
                }
            });
        }

        return episodes;
    }

    private Map<Long, EpisodePurchaseResponse> getPurchaseMap(Long userId, List<Long> episodeIds) {
        List<EpisodePurchaseResponse> episodePurchases = episodePurchaseRepository.findEpisodePurchases(userId, episodeIds);

        return episodePurchases.stream()
                .collect(Collectors.toMap(EpisodePurchaseResponse::getEpisodeId, p -> p));
    }

    @Transactional
    public Object getEpisodeDetail(Long userId, String contentType, Long episodeId) {
        EpisodeProvider provider = getProvider(contentType);

        return provider.findEpisodeDetail(userId, episodeId);
    }

    @Transactional
    public void rateEpisode(Long userId, String contentType, Long episodeId, EpisodeRatingRequest request) throws CustomException {

        String[] key = {userId.toString(), contentType, request.getScore().toString()};
        idempotentService.isValidIdempotent(Arrays.asList(key));

        final Integer score = request.getScore();
        User user = userRepository.getReferenceById(userId);

        EpisodeProvider provider = getProvider(contentType);
        provider.rateEpisode(user, episodeId, score);
    }

    @Transactional
    public void updateEpisodeRating(Long userId, String contentType, Long commentId, EpisodeRatingRequest request) {

        final Integer newScore = request.getScore();

        EpisodeProvider provider = getProvider(contentType);
        provider.updateEpisodeRating(userId, commentId, newScore);

    }


    private EpisodeProvider getProvider(String contentType) {
        return providers.stream()
                .filter(p -> p.supports(contentType))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CONTENT_TYPE));
    }
}
