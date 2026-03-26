package com.pageon.backend.service.provider;

import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.dto.response.EpisodeResponse;
import com.pageon.backend.entity.Content;
import com.pageon.backend.entity.Interest;
import com.pageon.backend.entity.ReadingHistory;
import com.pageon.backend.repository.ContentRepository;
import com.pageon.backend.repository.InterestRepository;
import com.pageon.backend.repository.ReadingHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AllContentProvider implements ContentProvider {
    private final ContentRepository contentRepository;
    private final InterestRepository interestRepository;
    private final ReadingHistoryRepository readingHistoryRepository;

    @Override
    public boolean supports(String contentType) {
        return "all".equals(contentType);
    }

    @Override
    public Optional<? extends Content> findById(Long contentId) {
        return Optional.empty();
    }

    @Override
    public List<EpisodeResponse.Summary> findEpisodes(Long userId, Long contentId) {
        return List.of();
    }

    @Override
    public Page<? extends Content> findByKeyword(String keyword, Pageable pageable) {
        return null;
    }

    @Override
    public Page<? extends Content> findByTitleOrPenName(String query, Pageable pageable) {
        return contentRepository.searchByTitleOrPenName(query, pageable);
    }

    @Override
    public Page<? extends Content> findNewArrivals(LocalDateTime since, Pageable pageable) {
        return null;
    }

    @Override
    public Page<? extends Content> findByStatusCompleted(Pageable pageable) {
        return contentRepository.findTopRatedCompleted(pageable);
    }

    @Override
    public Page<? extends Content> findBySerialDay(SerialDay serialDay, Pageable pageable) {
        return null;
    }

    @Override
    public Page<Interest> findByInterest(Long userId, Pageable pageable) {
        return interestRepository.findAllByUserId(userId, pageable);
    }

    @Override
    public Page<ReadingHistory> findByReadingHistory(Long userId, Pageable pageable) {
        return readingHistoryRepository.findAllReadingHistories(userId, pageable);
    }

}
