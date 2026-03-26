package com.pageon.backend.service.handler;

import com.pageon.backend.common.enums.ActionType;
import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.entity.Content;
import com.pageon.backend.entity.base.EpisodeBase;
import com.pageon.backend.entity.base.EpisodeRatingBase;
import com.pageon.backend.service.ActionLogService;
import com.pageon.backend.service.ReadingHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EpisodeActionHandler {
    private final ReadingHistoryService readingHistoryService;
    private final ActionLogService actionLogService;

    @Transactional
    public void handleViewEffects(Long userId, Content content, Long episodeId, ContentType contentType) {

        readingHistoryService.checkReadingHistory(userId, content.getId(), episodeId);

        actionLogService.createActionLog(userId, content.getId(), contentType, ActionType.VIEW, 0);

        content.updateViewCount();
    }

    @Transactional
    public void handleSaveRate(Long userId, Content content, ContentType contentType, EpisodeBase episode, int score) {

        episode.addRating(score);
        content.addRating(score);

        actionLogService.createActionLog(userId, content.getId(), contentType, ActionType.RATING, score);

    }

    @Transactional
    public void handleUpdateRate(Content content, EpisodeBase episode, EpisodeRatingBase rating, int newScore, int oldScore) {

        rating.updateRating(newScore);

        episode.updateRating(oldScore, newScore);
        content.updateRating(oldScore, newScore);

    }

    @Transactional
    public void handleSaveComment(Long userId, Content content, ContentType contentType) {

        actionLogService.createActionLog(userId, content.getId(), contentType, ActionType.COMMENT, 0);
    }


}
