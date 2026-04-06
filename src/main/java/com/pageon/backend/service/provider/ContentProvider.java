package com.pageon.backend.service.provider;

import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.dto.response.EpisodeResponse;
import com.pageon.backend.entity.Content;
import com.pageon.backend.entity.ContentRanking;
import com.pageon.backend.entity.Interest;
import com.pageon.backend.entity.ReadingHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ContentProvider {
    boolean supports(String contentType);

    Optional<? extends Content> findById(Long contentId);
    List<EpisodeResponse.Summary> findEpisodes(Long userId, Long contentId);

    // 검색 및 목록 조회
    Page<? extends Content> findByKeyword(String keyword, Pageable pageable);
    Page<? extends Content> findByTitleOrPenName(String query, Pageable pageable);
    Page<? extends Content> findNewArrivals(LocalDateTime since, Pageable pageable);

    Page<? extends Content> findByStatusCompleted(Pageable pageable);
    Page<? extends Content> findBySerialDay(SerialDay serialDay, Pageable pageable);

    Page<Interest> findByInterest(Long userId, Pageable pageable);
    Page<ReadingHistory> findByReadingHistory(Long userId, Pageable pageable);

    List<ContentRanking> findAllHourlyRankings(LocalDateTime rankingHour);


}
