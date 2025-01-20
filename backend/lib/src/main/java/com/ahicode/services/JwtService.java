package com.ahicode.services;

import com.ahicode.enums.AppRole;
import org.springframework.security.core.Authentication;

import java.util.Date;

public interface JwtService {
    Long extractUserIdFromAccessToken(String token);
    Long extractUserIdFromRefreshToken(String token);
    AppRole extractRoleFromAccessToken(String token);
    AppRole extractRoleFromRefreshToken(String token);
    String extractEmailFromAccessToken(String token);
    String extractEmailFromRefreshToken(String token);
    boolean isAccessTokenValid(String token);
    boolean isRefreshTokenValid(String token);
    boolean isAccessTokenExpired(String token);
    boolean isRefreshTokenExpired(String token);
    Date extractRefreshTokenExpirationTime(String token);
    Authentication authenticatedAccessValidation(String token);
    String generateAccessToken(Long id, String email, AppRole role);
    String generateRefreshToken(Long id, String email, AppRole role);
}
