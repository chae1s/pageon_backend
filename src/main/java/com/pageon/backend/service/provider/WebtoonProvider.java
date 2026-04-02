package com.pageon.backend.service.provider;

import com.pageon.backend.common.enums.EpisodeStatus;
import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.dto.response.EpisodeResponse;
import com.pageon.backend.entity.*;
import com.pageon.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebtoonProvider implements ContentProvider {
    private final WebtoonRepository webtoonRepository;
    private final InterestRepository interestRepository;
    private final WebtoonEpisodeRepository webtoonEpisodeRepository;
    private final EpisodePurchaseRepository episodePurchaseRepository;
    private final ReadingHistoryRepository readingHistoryRepository;

    @Override
    public boolean supports(String contentType) {
        return "webtoons".equals(contentType);
    }

    @Override
    public Optional<? extends Content> findById(Long contentId) {
        return webtoonRepository.findWithCreatorById(contentId);
    }

    @Override
    public List<EpisodeResponse.Summary> findEpisodes(Long userId, Long contentId) {

        log.info("Fetching episode list for content: {}. (User: {})", contentId, userId);
        List<WebtoonEpisode> episodes = webtoonEpisodeRepository.findByWebtoonIdAndEpisodeStatus(contentId, EpisodeStatus.PUBLISHED);
        if (userId == null) {
            return episodes.stream()
                    .map(e -> EpisodeResponse.Summary.fromEntity(e, null)).toList();
        } else {
            return episodes.stream().map(e -> {
                EpisodePurchase episodePurchase = episodePurchaseRepository.findByUser_IdAndContent_IdAndEpisodeId(userId, contentId, e.getId()).orElse(null);
                return EpisodeResponse.Summary.fromEntity(
                        e,
                        (episodePurchase != null) ? EpisodeResponse.Purchase.fromEntity(episodePurchase) : null
                );
            }).toList();
        }
    }

    @Override
    public Page<? extends Content> findByKeyword(String keyword, Pageable pageable) {
        return webtoonRepository.findAllByKeyword(keyword, pageable);
    }

    @Override
    public Page<? extends Content> findByTitleOrPenName(String query, Pageable pageable) {
        return webtoonRepository.searchByTitleOrPenName(query, pageable);
    }

    @Override
    public Page<? extends Content> findNewArrivals(LocalDateTime since, Pageable pageable) {
        return webtoonRepository.findAllNewArrivals(since, pageable);
    }

    @Override
    public Page<? extends Content> findByStatusCompleted(Pageable pageable) {
        return webtoonRepository.findTopRatedCompleted(pageable);
    }

    @Override
    public Page<? extends Content> findBySerialDay(SerialDay serialDay, Pageable pageable) {
        return webtoonRepository.findOngoingBySerialDay(serialDay, pageable);
    }

    @Override
    public Page<Interest> findByInterest(Long userId, Pageable pageable) {
        return interestRepository.findWebtoonsByUserId(userId, pageable);
    }

    @Override
    public Page<ReadingHistory> findByReadingHistory(Long userId, Pageable pageable) {
        return readingHistoryRepository.findWebtoonReadingHistories(userId, pageable);
    }

}
