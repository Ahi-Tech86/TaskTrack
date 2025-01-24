package com.ahicode.services;

public interface KafkaMessageListener {
    void listen(String message);
}
