package com.ahicode.services;

import com.ahicode.dtos.ProcessedToken;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

public class TokenProcessor {
    public static ProcessedToken parse(String token) {
        try {
            String[] tokenParts = token.split("\\.");
            if (tokenParts.length != 3) {
                throw new RuntimeException("Invalid JWT format");
            }

            String tokenPayload = tokenParts[1];
            byte[] decodedBytes = Base64.getUrlDecoder().decode(tokenPayload);
            String jsonToken = new String(decodedBytes, StandardCharsets.UTF_8);

            JsonObject jsonObject = JsonParser.parseString(jsonToken).getAsJsonObject();
            long exp = jsonObject.get("exp").getAsLong();

            Instant expTime = Instant.ofEpochSecond(exp);
            Instant now = Instant.now();
            long minutesUntilExpiration = Duration.between(now, expTime).toMinutes();

            return new ProcessedToken(token, minutesUntilExpiration);

        } catch (Exception e) {
            throw new RuntimeException("Token parsing failed");
        }
    }
}
