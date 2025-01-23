package com.ahicode.config;

import com.ahicode.enums.AppRole;
import com.ahicode.exceptions.AppException;
import com.ahicode.services.MessageProducerService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final int tokenMaxAge = 3600;
    private final UserAuthenticationProvider provider;
    private final MessageProducerService messageService;

    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/confirmRegister",
            "/api/v1/auth/admin/confirmRegister",
            "/webjars",
            "/favicon.ico",
            "/v3/api-docs",
            "/swagger-resources",
            "/swagger-ui/index.css",
            "/swagger-ui/index.html",
            "/swagger-ui/swagger-ui.css",
            "/v3/api-docs/swagger-config",
            "/swagger-ui/favicon-32x32.png",
            "/swagger-ui/swagger-ui-bundle.js",
            "/swagger-ui/swagger-initializer.js",
            "/swagger-ui/swagger-ui-standalone-preset.js"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // getting request path
        String path = request.getRequestURI();

        if (ALLOWED_PATHS.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // extract accessToken from headers or cookies
        String accessToken = extractAccessToken(request);
        // extract refreshToken from cookies
        String refreshToken = extractTokenFromCookies(request, "refreshToken");

        try {
            handleTokens(request, response, accessToken, refreshToken);
            filterChain.doFilter(request, response);
        } catch (AppException exception) {
            SecurityContextHolder.clearContext();
            response.sendError(exception.getHttpStatus().value(), exception.getMessage());
        }
    }

    private void handleTokens(
            HttpServletRequest request,
            HttpServletResponse response,
            String accessToken,
            String refreshToken
    ) {
        if (isTokenInvalid(refreshToken)) {
            if (refreshToken != null) {
                messageService.sendMessage(List.of(refreshToken));
                log.info("An invalid refresh token was sent to the kafka topic");
            }

            log.error("An attempt was made to gain access with an invalid refresh token or missing refresh token");
            throw new AppException("The refresh token was lost or is not valid", HttpStatus.UNAUTHORIZED);
        }

        if (accessToken == null || provider.isAccessTokenExpired(accessToken)) {
            if (provider.isRefreshTokenExpired(refreshToken)) {
                log.error("An attempt was made to gain access with an expired refresh token");
                throw new AppException("The refresh token is expired, please authorize again", HttpStatus.UNAUTHORIZED);
            }

            handleRefreshToken(response, refreshToken);
        } else {
            if (!provider.isAccessTokenValid(accessToken)) {
                log.error("An attempt was made to gain access with an invalid access token");
                throw new AppException("The access token is not valid", HttpStatus.UNAUTHORIZED);
            }
            authenticateUser(accessToken);
        }
    }

    private void handleRefreshToken(
            HttpServletResponse response,
            String token
    ) {
        Long userId = provider.extractUserId(token);
        AppRole role = provider.extractRole(token);
        String email = provider.extractEmail(token);

        String newAccessToken = provider.generateAccessToken(userId, email, role);
        log.info("Generated new access token for a user with email {}", email);

        updateTokenCookie(response, newAccessToken);
        authenticateUser(newAccessToken);
    }

    private boolean isTokenInvalid(String token) {
        return token == null || !provider.isRefreshTokenValid(token);
    }

    private void authenticateUser(String token) {
        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(provider.authenticatedAccessValidation(token));
            log.info("Successful authentication occurred");
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
            log.error("An error occurred during user authentication");
            throw new AppException("Authentication failed", HttpStatus.UNAUTHORIZED);
        }
    }

    private String extractAccessToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null) {
            return authHeader.substring(7);
        }

        return extractTokenFromCookies(request, "accessToken");
    }

    private void updateTokenCookie(
            HttpServletResponse response,
            String token
    ) {
        Cookie tokenCookie = new Cookie("accessToken", token);
        tokenCookie.setPath("/");
        tokenCookie.setHttpOnly(true);
        tokenCookie.setMaxAge(tokenMaxAge);
        response.addCookie(tokenCookie);
    }

    private String extractTokenFromCookies(
            HttpServletRequest request,
            String cookieName
    ) {
        Cookie cookie = WebUtils.getCookie(request, cookieName);

        return (cookie != null) ? cookie.getValue() : null;
    }
}
