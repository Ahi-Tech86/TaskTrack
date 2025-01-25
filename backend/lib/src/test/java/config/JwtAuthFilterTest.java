package config;

import com.ahicode.config.JwtAuthFilter;
import com.ahicode.config.UserAuthenticationProvider;
import com.ahicode.enums.AppRole;
import com.ahicode.services.MessageProducerServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.WebUtils;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class JwtAuthFilterTest {
    private JwtAuthFilter jwtAuthFilter;
    private UserAuthenticationProvider provider;
    private MessageProducerServiceImpl messageProducerService;

    @BeforeEach
    void setup() {
        provider = Mockito.mock(UserAuthenticationProvider.class);
        messageProducerService = Mockito.mock(MessageProducerServiceImpl.class);
        jwtAuthFilter = new JwtAuthFilter(provider, messageProducerService);
    }

    @Test
    void shouldAllowAccessToAllowedPaths() throws Exception {
        FilterChain filterChain = mock(FilterChain.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        // setup request URI for allowed path
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void shouldSendAuthenticationErrorWhenTokensAreExpired() throws Exception {
        FilterChain filterChain = mock(FilterChain.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/api/v1/protected");
        // setup http header like null
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        when(provider.isAccessTokenExpired(any())).thenReturn(true);
        when(provider.isRefreshTokenExpired(any())).thenReturn(true);

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(response, times(1))
                .sendError(HttpStatus.UNAUTHORIZED.value(), "The refresh token was lost or is not valid");
    }

    @Test
    void shouldAuthenticateUserWithValidAccessToken() throws ServletException, IOException {
        String validAccessToken = "validAccessToken";
        String validRefreshToken = "validRefreshToken";
        FilterChain filterChain = mock(FilterChain.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        Cookie refreshTokenCookie = new Cookie("refreshToken", validRefreshToken);

        when(request.getRequestURI()).thenReturn("/auth/v1/protected");
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + validAccessToken);
        when(request.getCookies()).thenReturn(new Cookie[]{refreshTokenCookie});
        when(provider.isRefreshTokenValid(validRefreshToken)).thenReturn(true);
        when(provider.isRefreshTokenExpired(validRefreshToken)).thenReturn(false);
        when(provider.isAccessTokenExpired(validAccessToken)).thenReturn(false);
        when(provider.isAccessTokenValid(validAccessToken)).thenReturn(true);

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(provider, times(1)).authenticatedAccessValidation(validAccessToken);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void shouldGenerateNewAccessTokenWithValidRefreshToken() throws ServletException, IOException {
        String newAccessToken = "newAccessToken";
        String validRefreshToken = "validRefresherToken";
        FilterChain filterChain = mock(FilterChain.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        Cookie refreshTokenCookie = new Cookie("refreshToken", validRefreshToken);

        when(request.getRequestURI()).thenReturn("/auth/v1/protected");
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        when(request.getCookies()).thenReturn(new Cookie[]{refreshTokenCookie});
        when(provider.isAccessTokenExpired(null)).thenReturn(true);
        when(provider.isRefreshTokenValid(validRefreshToken)).thenReturn(true);
        when(provider.isRefreshTokenExpired(validRefreshToken)).thenReturn(false);
        when(provider.extractUserId(validRefreshToken)).thenReturn(1L);
        when(provider.extractRole(validRefreshToken)).thenReturn(AppRole.USER);
        when(provider.extractEmail(validRefreshToken)).thenReturn("mock@example.com");
        when(provider.generateAccessToken(1L, "mock@example.com", AppRole.USER)).thenReturn(newAccessToken);

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(response, times(1)).addCookie(any(Cookie.class));
        verify(provider, times(1)).authenticatedAccessValidation(newAccessToken);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void shouldThrowErrorForInvalidAccessToken() throws ServletException, IOException {
        String invalidAccessToken = "invalidAccessToken";
        String validRefreshToken = "validRefresherToken";
        FilterChain filterChain = mock(FilterChain.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        Cookie refreshTokenCookie = new Cookie("refreshToken", validRefreshToken);

        when(request.getRequestURI()).thenReturn("/auth/v1/protected");
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + invalidAccessToken);
        when(request.getCookies()).thenReturn(new Cookie[]{refreshTokenCookie});
        when(provider.isRefreshTokenExpired(validRefreshToken)).thenReturn(false);
        when(provider.isRefreshTokenValid(validRefreshToken)).thenReturn(true);
        when(provider.isAccessTokenExpired(invalidAccessToken)).thenReturn(false);
        when(provider.isAccessTokenValid(invalidAccessToken)).thenReturn(false);

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(response, times(1))
                .sendError(HttpStatus.UNAUTHORIZED.value(), "The access token is not valid");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldThrowErrorForMissingRefreshToken() throws ServletException, IOException {
        FilterChain filterChain = mock(FilterChain.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getRequestURI()).thenReturn("/api/v1/protected");
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        when(WebUtils.getCookie(request, "refreshToken")).thenReturn(null);

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(response, times(1))
                .sendError(HttpStatus.UNAUTHORIZED.value(), "The refresh token was lost or is not valid");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldHandleInvalidRefreshToken() throws ServletException, IOException {
        String refreshToken = "invalidRefreshToken";
        FilterChain filterChain = mock(FilterChain.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);

        when(request.getRequestURI()).thenReturn("/api/v1/protected");
        when(request.getCookies()).thenReturn(new Cookie[]{refreshTokenCookie});
        when(provider.isAccessTokenExpired(any())).thenReturn(true);
        when(provider.isRefreshTokenValid(refreshToken)).thenReturn(false);

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(response, times(1))
                .sendError(HttpStatus.UNAUTHORIZED.value(), "The refresh token was lost or is not valid");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldThrowErrorForExpiredRefreshToken() throws ServletException, IOException {
        String refreshToken = "expiredRefreshToken";
        FilterChain filterChain = mock(FilterChain.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);

        when(request.getRequestURI()).thenReturn("/auth/v1/protected");
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        when(request.getCookies()).thenReturn(new Cookie[]{refreshTokenCookie});
        when(provider.isAccessTokenExpired(null)).thenReturn(true);
        when(provider.isRefreshTokenValid(refreshToken)).thenReturn(true);
        when(provider.isRefreshTokenExpired(refreshToken)).thenReturn(true);

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(response, times(1))
                .sendError(HttpStatus.UNAUTHORIZED.value(), "The refresh token is expired, please authorize again");
        verify(filterChain, never()).doFilter(request, response);
    }
}
