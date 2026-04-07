package com.pageon.backend.dto.record;

import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.common.enums.NotificationType;

import java.time.LocalDateTime;

public record EpisodeNotificationEvent(
        Long userId, NotificationType notificationType,
        ContentType contentType, Long contentId,
        String contentTitle, LocalDateTime createdAt
) {

}
