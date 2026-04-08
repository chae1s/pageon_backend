package com.pageon.backend.service.kafka;

import com.pageon.backend.dto.record.EpisodeNotificationEvent;
import com.pageon.backend.dto.record.SettlementNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventProducer {
    @Value("${topic.name.notification.episode}")
    private String episodeTopicName;
    @Value("${topic.name.notification.settlement}")
    private String settlementTopicName;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendEpisodeMessage(EpisodeNotificationEvent event) {
        kafkaTemplate.send(episodeTopicName, event);
    }

    public void settlementMessage(SettlementNotificationEvent event) {
        kafkaTemplate.send(settlementTopicName, event);
    }
}
