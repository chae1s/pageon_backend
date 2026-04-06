package com.pageon.backend.service;

import com.pageon.backend.common.enums.ActionType;
import com.pageon.backend.common.enums.ContentType;
import com.pageon.backend.dto.payload.ActionLogEvent;
import com.pageon.backend.entity.ContentActionLog;
import com.pageon.backend.repository.ActionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActionLogService {

    private final ActionLogRepository actionLogRepository;
    private final ConsumerFactory<String, ActionLogEvent> consumerFactory;

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

    public void processLogsByTimeRange(LocalDateTime start, LocalDateTime end) {

        try (Consumer<String, ActionLogEvent> consumer = consumerFactory.createConsumer()) {

            TopicPartition partition = new TopicPartition("pageon-log-topic", 0);
            consumer.assign(Collections.singletonList(partition));

            long startTimestamp = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endTimestamp = end.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            Map<TopicPartition, Long> timestampsToSearch = Map.of(partition, startTimestamp);
            Map<TopicPartition, OffsetAndTimestamp> foundOffsets = consumer.offsetsForTimes(timestampsToSearch);

            OffsetAndTimestamp startOffset = foundOffsets.get(partition);
            if (startOffset == null) {
                log.info("{} 시점 이후의 데이터가 카프카에 존재하지 않습니다.", start);
                return;
            }

            // 3. 해당 시점으로 이동
            consumer.seek(partition, startOffset.offset());

            // 4. 데이터 수집
            List<ContentActionLog> actionLogs = new ArrayList<>();
            boolean keepRunning = true;

            while (keepRunning) {

                ConsumerRecords<String, ActionLogEvent> records = consumer.poll(Duration.ofSeconds(2));

                if (records.isEmpty()) {
                    log.info("더 이상 읽을 데이터가 없어 수집을 종료합니다.");
                    break;
                }

                for (ConsumerRecord<String, ActionLogEvent> record : records) {
                    // 종료 시간 체크
                    if (record.timestamp() > endTimestamp) {
                        keepRunning = false;
                        break;
                    }

                    actionLogs.add(
                            record.value().toEntity()
                    );

                }
            }

            if (!actionLogs.isEmpty()) {
                return;
            }

            actionLogRepository.saveAll(actionLogs);

        } catch (Exception e) {
            log.error("로그 배치 처리 중 에러 발생", e);
        }
    }


}
