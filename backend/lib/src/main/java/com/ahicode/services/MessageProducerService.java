package com.ahicode.services;

import java.util.List;

public interface MessageProducerService {
    void sendMessage(List<String> message);
}
