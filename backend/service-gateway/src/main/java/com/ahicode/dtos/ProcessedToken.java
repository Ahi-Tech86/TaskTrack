package com.ahicode.dtos;

public class ProcessedToken {
    private final String token;
    private final long ttl;

    public ProcessedToken(String token, long ttl) {
        this.token = token;
        this.ttl = ttl;
    }

    public String getToken() {
        return token;
    }

    public long getTtl() {
        return ttl;
    }
}
