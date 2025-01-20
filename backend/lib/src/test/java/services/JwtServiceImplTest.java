package services;

import com.ahicode.enums.AppRole;
import com.ahicode.services.JwtServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class JwtServiceImplTest {
    @InjectMocks
    private JwtServiceImpl jwtService;

    private Key mockAccessSignKey;
    private Key mockRefreshSignKey;
    private final Long mockAccessTokenExpirationTime = 3600000L;
    private final Long mockRefreshTokenExpirationTime = 7200000L;

    @BeforeEach
    void setup() {
        String mockAccessTokenSecretKey = "mockAccessSecretKey1234567890ABCDEF";
        String mockRefreshTokenSecretKey = "mockRefresSecretKey1234567890ABCDEF";
        mockAccessSignKey = Keys.hmacShaKeyFor(mockAccessTokenSecretKey.getBytes());
        mockRefreshSignKey = Keys.hmacShaKeyFor(mockRefreshTokenSecretKey.getBytes());

        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationTime", mockAccessTokenExpirationTime);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpirationTime", mockRefreshTokenExpirationTime);

        ReflectionTestUtils.setField(jwtService, "accessSignKey", mockAccessSignKey);
        ReflectionTestUtils.setField(jwtService, "refreshSignKey", mockRefreshSignKey);
    }

    @Test
    void shouldExtractUserIdFromAccessToken() {
        Long expectedUserId = 1L;
        String token = buildToken(
                mockAccessSignKey, Map.of("id", expectedUserId),
                "mock@example.com", mockAccessTokenExpirationTime
        );
        Long actualUserId = jwtService.extractUserIdFromAccessToken(token);
        assertExtractedValue(expectedUserId, actualUserId);
    }

    @Test
    void shouldExtractUserIdFromRefreshToken() {
        Long expectedUserId = 1L;
        String token = buildToken(
                mockRefreshSignKey, Map.of("id", expectedUserId),
                "mock@example.com", mockRefreshTokenExpirationTime
        );
        Long actualUserId = jwtService.extractUserIdFromRefreshToken(token);
        assertExtractedValue(expectedUserId, actualUserId);
    }

    @Test
    void shouldExtractRoleFromAccessToken() {
        AppRole expectedUserRole = AppRole.USER;
        String token = buildToken(
                mockAccessSignKey, Map.of("role", expectedUserRole),
                "mock@example.com", mockAccessTokenExpirationTime
        );
        AppRole actualUserRole = jwtService.extractRoleFromAccessToken(token);
        assertExtractedValue(expectedUserRole, actualUserRole);
    }

    @Test
    void shouldExtractRoleFromRefreshToken() {
        AppRole expectedUserRole = AppRole.USER;
        String token = buildToken(
                mockRefreshSignKey, Map.of("role", expectedUserRole),
                "mock@example.com", mockRefreshTokenExpirationTime
        );
        AppRole actualUserRole = jwtService.extractRoleFromRefreshToken(token);
        assertExtractedValue(expectedUserRole, actualUserRole);
    }

    @Test
    void shouldExtractUserEmailFromAccessToken() {
        String expectedUserEmail = "mock@example.com";
        String token = buildToken(mockAccessSignKey, Map.of(), expectedUserEmail, mockAccessTokenExpirationTime);
        String actualUserEmail = jwtService.extractEmailFromAccessToken(token);
        assertExtractedValue(expectedUserEmail, actualUserEmail);
    }

    @Test
    void shouldExtractUserEmailFromRefreshToken() {
        String expectedUserEmail = "mock@example.com";
        String token = buildToken(mockRefreshSignKey, Map.of(), expectedUserEmail, mockRefreshTokenExpirationTime);
        String actualUserEmail = jwtService.extractEmailFromRefreshToken(token);
        assertExtractedValue(expectedUserEmail, actualUserEmail);
    }

    @Test
    void shouldValidateAccessToken() {
        String expectedUserEmail = "mock@example.com";
        String token = buildToken(mockAccessSignKey, Map.of(), expectedUserEmail, mockAccessTokenExpirationTime);
        assertNotNull(token);
        assertTrue(jwtService.isAccessTokenValid(token));
    }

    @Test
    void shouldValidateRefreshToken() {
        String expectedUserEmail = "mock@example.com";
        String token = buildToken(mockRefreshSignKey, Map.of(), expectedUserEmail, mockRefreshTokenExpirationTime);
        assertNotNull(token);
        assertTrue(jwtService.isRefreshTokenValid(token));
    }

    @Test
    void shouldValidateAccessTokenExpiration() {
        String userEmail = "mock@example.com";
        String token = buildToken(
                mockAccessSignKey, Map.of(), userEmail,
                (Long) ReflectionTestUtils.getField(jwtService, "accessTokenExpirationTime")
        );
        assertNotNull(token);
        assertFalse(jwtService.isAccessTokenExpired(token));
    }

    @Test
    void shouldValidateRefreshTokenExpiration() {
        String userEmail = "mock@example.com";
        String token = buildToken(
                mockRefreshSignKey, Map.of(), userEmail,
                (Long) ReflectionTestUtils.getField(jwtService, "refreshTokenExpirationTime")
        );
        assertNotNull(token);
        assertFalse(jwtService.isRefreshTokenExpired(token));
    }

    @Test
    void shouldExtractExpirationTimeFromRefreshToken() {
        String userEmail = "mock@example.com";
        long currentTimeMillis = System.currentTimeMillis();
        Long refreshTokenExpirationTime = (Long) ReflectionTestUtils.getField(
                jwtService, "refreshTokenExpirationTime"
        );
        Date expectedExpirationTime = new Date(currentTimeMillis + refreshTokenExpirationTime);

        String token = buildToken(mockRefreshSignKey, Map.of(), userEmail, refreshTokenExpirationTime);

        Date actualExpirationTime = jwtService.extractRefreshTokenExpirationTime(token);
        assertNotNull(actualExpirationTime);
        assertEquals(expectedExpirationTime.toString(), actualExpirationTime.toString());
    }

    @Test
    void shouldAuthenticateAccessToken() {
        String userEmail = "mock@example.com";
        String token = buildToken(mockAccessSignKey, Map.of(), userEmail, mockAccessTokenExpirationTime);
        Authentication authentication = jwtService.authenticatedAccessValidation(token);
        assertNotNull(authentication);
        assertEquals(userEmail, authentication.getName());
        assertTrue(authentication.isAuthenticated());
        assertTrue(authentication.getAuthorities().isEmpty());
    }

    @Test
    void shouldGenerateAccessToken() {
        Long expectedUserId = 1L;
        AppRole expectedUserRole = AppRole.USER;
        String expectedUserEmail = "mock@example.com";

        String token = jwtService.generateAccessToken(expectedUserId, expectedUserEmail, expectedUserRole);

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(mockAccessSignKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals(expectedUserEmail, claims.getSubject());
        assertEquals(expectedUserId, Long.valueOf(claims.get("id", Integer.class)));
        assertEquals(expectedUserRole, AppRole.valueOf(claims.get("role").toString()));

        long currentTime = System.currentTimeMillis();
        long expirationTime = claims.getExpiration().getTime();
        assertTrue(expirationTime > currentTime);
    }

    @Test
    void shouldGenerateRefreshToken() {
        Long expectedUserId = 1L;
        AppRole expectedUserRole = AppRole.USER;
        String expectedUserEmail = "mock@example.com";

        String token = jwtService.generateRefreshToken(expectedUserId, expectedUserEmail, expectedUserRole);

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(mockRefreshSignKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals(expectedUserEmail, claims.getSubject());
        assertEquals(expectedUserId, Long.valueOf(claims.get("id", Integer.class)));
        assertEquals(expectedUserRole, AppRole.valueOf(claims.get("role").toString()));

        long currentTime = System.currentTimeMillis();
        long expirationTime = claims.getExpiration().getTime();
        assertTrue(expirationTime > currentTime);
    }

    private <T> void assertExtractedValue(T expectedValue, T actualValue) {
        assertNotNull(actualValue);
        assertEquals(expectedValue, actualValue);
    }

    private String buildToken(Key signKey, Map<String, Object> claims, String subject, Long expiration) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(signKey)
                .compact();
    }
}
