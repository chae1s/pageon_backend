package com.pageon.backend.service;

import com.pageon.backend.common.enums.ActionType;
import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.dto.payload.ActionLogEvent;
import com.pageon.backend.entity.ContentActionLog;
import com.pageon.backend.repository.ActionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionLogService {

    private final ActionLogRepository actionLogRepository;
    private final ConsumerFactory<String, ActionLogEvent> consumerFactory;

    @Value("${topic.name}")
    private String topicName;

    @Transactional
    public void createActionLog(Long userId, Long contentId, ContentType contentType, ActionType actionType, Integer ratingScore) {

        ContentActionLog actionLog = ContentActionLog.builder()
                .contentId(contentId)
                .userId(userId)
                .contentType(contentType)
                .actionType(actionType)
                .ratingScore(ratingScore)
                .build();

        actionLogRepository.save(actionLog);
    }

    public void consumeActionLog() {
        Consumer<String, ActionLogEvent> consumer = consumerFactory.createConsumer("pageon-log-consumer", null);
        TopicPartition partition = new TopicPartition(topicName, 0);
        consumer.assign(List.of(partition));

        LocalDateTime endTime = LocalDateTime.now().minusHours(1)
                .withMinute(59).withSecond(59).withNano(999999999);

        List<ContentActionLog> result = new ArrayList<>();
        long lastCommitOffset = -1;

        ConsumerRecords<String, ActionLogEvent> records = consumer.poll(Duration.ofSeconds(3));
        for (ConsumerRecord<String, ActionLogEvent> record : records) {
            ActionLogEvent event = record.value();

            if (event.getActionTime().isBefore(endTime)) {
                result.add(event.toEntity());
                lastCommitOffset = record.offset();
            }
        }

        if (lastCommitOffset >= 0) {
            Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
            offsets.put(partition, new OffsetAndMetadata(lastCommitOffset + 1));
            consumer.commitSync(offsets);
        }

        actionLogRepository.saveAll(result);

        consumer.close();

    }


}

