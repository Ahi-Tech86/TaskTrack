package com.ahicode.services;

public interface EmailService {
    void sendConfirmationCode(String to, String confirmationCode);
}
