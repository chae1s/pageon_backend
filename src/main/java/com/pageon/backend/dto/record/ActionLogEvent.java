package com.pageon.backend.dto.record;

import com.pageon.backend.common.enums.ActionType;
import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.entity.ContentActionLog;

import java.time.LocalDateTime;

public record ActionLogEvent(
        Long userId, Long contentId,
        ContentType contentType, ActionType actionType,
        Integer ratingScore, LocalDateTime actionTime
) {

    public ContentActionLog toEntity() {
        return ContentActionLog.builder()
                .userId(this.userId)
                .contentId(this.contentId)
                .contentType(this.contentType)
                .actionType(this.actionType)
                .ratingScore(this.ratingScore)
                .actionTime(this.actionTime) // 카프카에서 넘어온 시각 그대로 저장
                .build();
    }
}

