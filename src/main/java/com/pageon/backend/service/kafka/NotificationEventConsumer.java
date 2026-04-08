package com.pageon.backend.service.kafka;

import com.pageon.backend.dto.record.EpisodeNotificationEvent;
import com.pageon.backend.dto.record.SettlementNotificationEvent;
import com.pageon.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "ep-notification-topic", groupId = "ep-notification-group")
    public void consume(EpisodeNotificationEvent event) {
        try {
            log.info("Notification event received: {}", event);
            notificationService.updateEpisode(event);
        } catch (Exception e) {
            log.error("Error consuming notification event", e);
        }
    }

    @KafkaListener(topics = "settlement-notification-topic", groupId = "settlement-notification-group")
    public void consume(SettlementNotificationEvent event) {
        try {
            log.info("Notification event received: {}", event);
            notificationService.payoutSettlement(event);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
