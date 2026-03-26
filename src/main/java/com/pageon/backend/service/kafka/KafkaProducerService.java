package com.pageon.backend.service.kafka;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    @Value("${topic.name}")
    private String topicName;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendMessageToKafka(String message) {
        System.out.printf("Producer Message: %s%n", message);
        this.kafkaTemplate.send(this.topicName, message);
    }

}
