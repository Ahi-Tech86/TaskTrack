package com.ahicode.config;

import com.ahicode.enums.AppRole;
import com.ahicode.services.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAuthenticationProvider {

    private final JwtService service;

    public Long extractUserId(String token) {
        return service.extractUserIdFromRefreshToken(token);
    }

    public AppRole extractRole(String token) {
        return service.extractRoleFromRefreshToken(token);
    }

    public String extractEmail(String token) {
        return service.extractEmailFromRefreshToken(token);
    }

    public boolean isAccessTokenValid(String token) {
        return service.isAccessTokenValid(token);
    }

    public boolean isRefreshTokenValid(String token) {
        return service.isRefreshTokenValid(token);
    }

    public boolean isAccessTokenExpired(String token) {
        return service.isAccessTokenExpired(token);
    }

    public boolean isRefreshTokenExpired(String token) {
        return service.isRefreshTokenExpired(token);
    }

    public Authentication authenticatedAccessValidation(String token) {
        return service.authenticatedAccessValidation(token);
    }

    public String generateAccessToken(Long id, String email, AppRole role) {
        return service.generateAccessToken(id, email, role);
    }
}
