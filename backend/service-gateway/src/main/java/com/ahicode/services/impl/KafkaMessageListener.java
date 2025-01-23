package com.ahicode.services.impl;

import com.ahicode.services.TokenProcessingService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaMessageListener {

    private final TokenProcessingService tokenProcessingService;

    @Autowired
    public KafkaMessageListener(TokenProcessingService tokenProcessingService) {
        this.tokenProcessingService = tokenProcessingService;
    }

    @KafkaListener(topics = "blacklist_tokens_topic", groupId = "consumer_group")
    public void listen(String token) {
        System.out.println(token);
        tokenProcessingService.processToken(token);
    }
}