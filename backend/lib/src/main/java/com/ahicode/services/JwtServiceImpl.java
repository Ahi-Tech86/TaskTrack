package com.ahicode.services;

import com.ahicode.enums.AppRole;
import com.ahicode.exceptions.AppException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class JwtServiceImpl implements JwtService {

    @Value("${application.security.jwt.access-token.secret-key}")
    private String accessTokenSecretKey;
    @Value("${application.security.jwt.access-token.expiration}")
    private Long accessTokenExpirationTime;
    @Value("${application.security.jwt.refresh-token.secret-key}")
    private String refreshTokenSecretKey;
    @Value("${application.security.jwt.refresh-token.expiration}")
    private Long refreshTokenExpirationTime;

    private Key accessSignKey;
    private Key refreshSignKey;

    @PostConstruct
    private void init() {
        accessSignKey = getAccessSignKey();
        refreshSignKey = getRefreshSignKey();
    }

    @Override
    public Long extractUserIdFromAccessToken(String token) {
        Claims claims = extractAllClaims(token, accessSignKey);

        return claims.get("id", Long.class);
    }

    @Override
    public Long extractUserIdFromRefreshToken(String token) {
        Claims claims = extractAllClaims(token, refreshSignKey);

        return claims.get("id", Long.class);
    }

    @Override
    public AppRole extractRoleFromAccessToken(String token) {
        Claims claims = extractAllClaims(token, accessSignKey);
        String role = claims.get("role", String.class);

        return AppRole.valueOf(role);
    }

    @Override
    public AppRole extractRoleFromRefreshToken(String token) {
        Claims claims = extractAllClaims(token, refreshSignKey);
        String role = claims.get("role", String.class);

        return AppRole.valueOf(role);
    }

    @Override
    public String extractEmailFromAccessToken(String token) {
        Claims claims = extractAllClaims(token, accessSignKey);

        return claims.getSubject();
    }

    @Override
    public String extractEmailFromRefreshToken(String token) {
        Claims claims = extractAllClaims(token, refreshSignKey);

        return claims.getSubject();
    }

    @Override
    public boolean isAccessTokenValid(String token) {
        return validateToken(token, accessSignKey);
    }

    @Override
    public boolean isRefreshTokenValid(String token) {
        return validateToken(token, refreshSignKey);
    }

    @Override
    public boolean isAccessTokenExpired(String token) {
        return expirationValidate(token, accessSignKey);
    }

    @Override
    public boolean isRefreshTokenExpired(String token) {
        return expirationValidate(token, refreshSignKey);
    }

    @Override
    public Date extractRefreshTokenExpirationTime(String token) {
        return extractTokenExpiration(token, refreshSignKey);
    }

    @Override
    public Authentication authenticatedAccessValidation(String token) {
        return authValidateToken(token, accessSignKey);
    }

    @Override
    public String generateAccessToken(Long id, String email, AppRole role) {
        return generateToken(email, id, role, accessSignKey, accessTokenExpirationTime);
    }

    @Override
    public String generateRefreshToken(Long id, String email, AppRole role) {
        return generateToken(email, id, role, refreshSignKey, refreshTokenExpirationTime);
    }

    private Key getAccessSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(accessTokenSecretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Key getRefreshSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(refreshTokenSecretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private boolean validateToken(String token, Key signKey) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(signKey)
                    .build()
                    .parseClaimsJws(token);

            return true;
        } catch (RuntimeException exception) {
            log.error("Attempt to validate token with wrong signed token {} with exception: {}", token, exception.getMessage());
            return false;
        }
    }

    private Claims extractAllClaims(String token, Key signKey) {
        return Jwts.parserBuilder()
                .setSigningKey(signKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean expirationValidate(String token, Key signKey) {
        Date expirationDate = extractTokenExpiration(token, signKey);

        return expirationDate.before(new Date());
    }

    private Date extractTokenExpiration(String token, Key signKey) {
        Claims claims = extractAllClaims(token, signKey);

        return claims.getExpiration();
    }

    private Authentication authValidateToken(String token, Key signKey) {
        Claims claims = extractAllClaims(token, signKey);

        String email = claims.getSubject();

        return new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
    }

    private String generateToken(String email, Long userId, AppRole role, Key signKey, Long expirationTime) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", userId);
        claims.put("role", role.toString());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(SignatureAlgorithm.HS256, signKey)
                .compact();
    }
}
