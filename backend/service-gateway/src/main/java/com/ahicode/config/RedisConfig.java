package com.ahicode.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${gateway.redis.token.host}")
    private String redisHost;
    @Value("${gateway.redis.token.port}")
    private int redisPort;

    @Bean
    public RedisConnectionFactory tokenRedisConnectionFactory() {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }

    @Bean
    public RedisTemplate<String, Object> tokenRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> tokenRedisTemplate = new RedisTemplate<>();
        tokenRedisTemplate.setConnectionFactory(factory);
        tokenRedisTemplate.setKeySerializer(new StringRedisSerializer());
        tokenRedisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return tokenRedisTemplate;
    }
}
