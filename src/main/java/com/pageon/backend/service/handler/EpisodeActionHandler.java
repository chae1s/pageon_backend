package com.pageon.backend.service.handler;

import com.pageon.backend.common.enums.ActionType;
import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.dto.payload.ActionLogEvent;
import com.pageon.backend.entity.Content;
import com.pageon.backend.entity.base.EpisodeBase;
import com.pageon.backend.entity.base.EpisodeRatingBase;
import com.pageon.backend.service.ReadingHistoryService;
import com.pageon.backend.service.kafka.ActionLogProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class EpisodeActionHandler {
    private final ReadingHistoryService readingHistoryService;
    private final ActionLogProducer actionLogProducer;
    private final StringRedisTemplate stringRedisTemplate;

    @Transactional
    public void handleViewEffects(Long userId, Content content, EpisodeBase episode, ContentType contentType) {

        readingHistoryService.checkReadingHistory(userId, content.getId(), episode.getId());

        actionLogProducer.sendMessage(getActionLogEvent(userId, content.getId(), contentType, ActionType.VIEW, 0));

        content.updateViewCount();
        episode.updateViewCount();
        addScoreInRedis(contentType, content.getId(), ActionType.VIEW.getScore());
    }

    @Transactional
    public void handleSaveRate(Long userId, Content content, ContentType contentType, EpisodeBase episode, int score) {

        episode.addRating(score);
        content.addRating(score);

        actionLogProducer.sendMessage(getActionLogEvent(userId, content.getId(), contentType, ActionType.RATING, score));
        addScoreInRedis(contentType, content.getId(), score);

    }

    @Transactional
    public void handleUpdateRate(Content content, EpisodeBase episode, EpisodeRatingBase rating, int newScore, int oldScore) {

        rating.updateRating(newScore);

        episode.updateRating(oldScore, newScore);
        content.updateRating(oldScore, newScore);

    }

    @Transactional
    public void handleSaveComment(Long userId, Content content, ContentType contentType) {

        actionLogProducer.sendMessage(getActionLogEvent(userId, content.getId(), contentType, ActionType.COMMENT, 0));
        addScoreInRedis(contentType, content.getId(), ActionType.COMMENT.getScore());
    }

    @Transactional
    public void handlePurchaseEpisode(Long userId, Long contentId, ContentType contentType, ActionType actionType) {

        actionLogProducer.sendMessage(getActionLogEvent(userId, contentId, contentType, actionType, 0));
        addScoreInRedis(contentType, contentId, actionType.getScore());
    }

    @Transactional
    public void handleInterestContent(Long userId, Long contentId, ContentType contentType) {
        actionLogProducer.sendMessage(getActionLogEvent(userId, contentId, contentType, ActionType.INTEREST, 0));
        addScoreInRedis(contentType, contentId, ActionType.INTEREST.getScore());
    }

    private ActionLogEvent getActionLogEvent(Long userId, Long contentId, ContentType contentType, ActionType actionType, int ratingScore) {
        return ActionLogEvent.builder()
                .userId(userId)
                .contentId(contentId)
                .contentType(contentType)
                .actionType(actionType)
                .ratingScore(ratingScore)
                .build();

    }

    private void addScoreInRedis(ContentType contentType, Long contentId, int score) {
        LocalDateTime rankingHour = LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0);

        String timeSuffix = rankingHour.format((DateTimeFormatter.ofPattern("yyyyMMddHH")));

        String key = "ranking:hourly:" + timeSuffix + ":" + contentType;
        stringRedisTemplate.opsForZSet().incrementScore(key, String.valueOf(contentId), score);

        stringRedisTemplate.expire(key, 3, TimeUnit.HOURS);
    }


}
