package com.ahicode.services.impl;

import com.ahicode.services.TokenSetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenSetServiceImpl implements TokenSetService {
    @Autowired
    private RedisTemplate<String, Object> tokenRedisTemplate;

    @Autowired
    public TokenSetServiceImpl(@Qualifier("tokenRedisTemplate") RedisTemplate<String, Object> tokenRedisTemplate) {
        this.tokenRedisTemplate = tokenRedisTemplate;
    }

    public void saveToken(String token, long ttlInUnits, TimeUnit unit) {
        tokenRedisTemplate.opsForValue().set(token, "stored", ttlInUnits, unit);
    }

    public boolean isTokenInBlackList(String token) {
        return Boolean.TRUE.equals(tokenRedisTemplate.hasKey(token));
    }
}
