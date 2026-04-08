package com.pageon.backend.dto.record;

import com.pageon.backend.common.enums.NotificationType;

public record SettlementNotificationEvent(
        Long userId, Long settlementId,
        NotificationType notificationType, Integer month
) {
}
