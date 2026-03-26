package com.pageon.backend.service;

import com.pageon.backend.common.enums.SerialDay;
import com.pageon.backend.common.utils.PageableUtil;
import com.pageon.backend.dto.response.ContentResponse;
import com.pageon.backend.entity.Content;
import com.pageon.backend.entity.ReadingHistory;
import com.pageon.backend.entity.User;
import com.pageon.backend.exception.CustomException;
import com.pageon.backend.exception.ErrorCode;
import com.pageon.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadingHistoryService {

    private final ReadingHistoryRepository readingHistoryRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;

    @Transactional
    public void checkReadingHistory(Long userId, Long contentId, Long episodeId) {
        User user = userRepository.getReferenceById(userId);
        Content content = contentRepository.findByIdAndDeletedAtIsNull(contentId).orElseThrow(
                () -> new CustomException(ErrorCode.CONTENT_NOT_FOUND)
        );

        log.info("Checking reading history for user {} and content {}", userId, content);

        ReadingHistory readingHistory = readingHistoryRepository.findByUser_IdAndContent_Id(user.getId(), contentId).orElse(null);
        log.info("readingHistory: {}", readingHistory);

        if (readingHistory == null) {
            log.info("ReadingHistory not found");
            createReadingHistory(user, content, episodeId);
        } else {
            log.info("ReadingHistory found");
            updateReadingHistory(readingHistory, episodeId);
        }

    }

    private void createReadingHistory(User user, Content content, Long episodeId) {
        ReadingHistory readingHistory = ReadingHistory.builder()
                .user(user)
                .content(content)
                .episodeId(episodeId)
                .lastReadAt(LocalDateTime.now())
                .build();

        readingHistoryRepository.save(readingHistory);
    }

    private void updateReadingHistory(ReadingHistory readingHistory, Long episodeId) {
        readingHistory.updateEpisodeId(episodeId);
    }

}
