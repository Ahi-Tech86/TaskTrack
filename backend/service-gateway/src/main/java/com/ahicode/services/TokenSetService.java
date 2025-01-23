package com.ahicode.services;

import java.util.concurrent.TimeUnit;

public interface TokenSetService {
    void saveToken(String token, long ttlInUnits, TimeUnit unit);
    boolean isTokenInBlackList(String token);
}
