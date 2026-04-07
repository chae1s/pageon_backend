package com.pageon.backend.service;

import com.pageon.backend.common.enums.ActionType;
import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.dto.record.ActionLogEvent;
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
    private final ConsumerFactory<String, Object> consumerFactory;

    @Value("${topic.name.log}")
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
        Consumer<String, Object> consumer = consumerFactory.createConsumer("log-group", null);
        TopicPartition partition = new TopicPartition(topicName, 0);
        consumer.assign(List.of(partition));

        LocalDateTime endTime = LocalDateTime.now().minusHours(1)
                .withMinute(59).withSecond(59).withNano(999999999);

        List<ContentActionLog> result = new ArrayList<>();
        long lastOffsetToCommit = -1;
        boolean keepPolling = true;

        while (keepPolling) {
            ConsumerRecords<String, Object> records = consumer.poll(Duration.ofSeconds(1));
            if (records.isEmpty()) break;

            for (ConsumerRecord<String, Object> record : records) {
                if (record.value() instanceof ActionLogEvent event) {

                    if (event.actionTime().isBefore(endTime)) {
                        result.add(event.toEntity());
                        lastOffsetToCommit = record.offset();
                    } else {

                        keepPolling = false;
                        break;
                    }
                }
            }
        }

        if (!result.isEmpty()) {
            actionLogRepository.saveAll(result);

            if (lastOffsetToCommit >= 0) {
                Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
                offsets.put(partition, new OffsetAndMetadata(lastOffsetToCommit + 1));
                consumer.commitSync(offsets);
            }
        }

        consumer.close();

    }


}

