package com.ahicode.services;

public interface EncryptionService {
    String encrypt(String data);
    String decrypt(String encryptedData);
}
