package com.pageon.backend.service.kafka;

import com.pageon.backend.dto.record.ActionLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class ActionLogProducer {

    @Value("${topic.name.log}")
    private String topicName;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendMessage(ActionLogEvent event) {
        kafkaTemplate.send(topicName, event);
    }
}
