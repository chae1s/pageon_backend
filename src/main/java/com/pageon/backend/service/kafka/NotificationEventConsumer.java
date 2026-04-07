package com.pageon.backend.service.kafka;

import com.pageon.backend.dto.record.EpisodeNotificationEvent;
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

    @KafkaListener(topics = "notification-topic", groupId = "notification-group")
    public void consume(EpisodeNotificationEvent event) {
        try {
            log.info("Notification event received: {}", event);
            notificationService.updateEpisode(event);
        } catch (Exception e) {
            log.error("Error consuming notification event", e);
        }
    }
}
