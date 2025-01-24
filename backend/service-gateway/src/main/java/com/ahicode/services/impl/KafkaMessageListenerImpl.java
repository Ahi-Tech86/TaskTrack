package com.ahicode.services.impl;

import com.ahicode.services.KafkaMessageListener;
import com.ahicode.services.TokenProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaMessageListenerImpl implements KafkaMessageListener {

    private final TokenProcessingService tokenProcessingService;

    @Autowired
    public KafkaMessageListenerImpl(TokenProcessingService tokenProcessingService) {
        this.tokenProcessingService = tokenProcessingService;
    }

    @Override
    @KafkaListener(topics = "blacklist_tokens_topic", groupId = "consumer_group")
    public void listen(String token) {
        tokenProcessingService.processToken(token);
    }
}