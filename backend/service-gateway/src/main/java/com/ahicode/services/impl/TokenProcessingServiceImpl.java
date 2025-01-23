package com.ahicode.services.impl;

import com.ahicode.dtos.ProcessedToken;
import com.ahicode.services.TokenProcessingService;
import com.ahicode.services.TokenProcessor;
import com.ahicode.services.TokenSetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class TokenProcessingServiceImpl implements TokenProcessingService {

    private final TokenSetService tokenSetService;
    private static final Logger logger = Logger.getLogger(TokenProcessingServiceImpl.class.getName());

    @Autowired
    public TokenProcessingServiceImpl(TokenSetService tokenSetService) {
        this.tokenSetService = tokenSetService;
    }

    @Override
    public void processToken(String token) {
        ProcessedToken processedToken = TokenProcessor.parse(token);

        long ttl = processedToken.getTtl();

        tokenSetService.saveToken(token, ttl, TimeUnit.MINUTES);
        logger.log(Level.INFO, "The token was successfully save to blacklist");
    }
}
