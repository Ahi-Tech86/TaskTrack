package com.ahicode.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageProducerServiceTest implements MessageProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = "blacklist_tokens_topic";

    @Autowired
    public MessageProducerServiceTest(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void sendMessage(List<String> message) {
        for (String token : message) {
            kafkaTemplate.send(TOPIC, token);
        }
    }
}
