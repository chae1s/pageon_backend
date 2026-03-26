package com.pageon.backend.controller;

import com.pageon.backend.service.kafka.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
public class KafkaProducerController {
    private final KafkaProducerService kafkaProducerService;

    @PostMapping
    public ResponseEntity<Void> sendMessage(@RequestParam String message) {
        kafkaProducerService.sendMessageToKafka(message);

        return ResponseEntity.ok().build();
    }
}
