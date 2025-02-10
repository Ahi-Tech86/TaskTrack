package com.ahicode.config;

import com.ahicode.services.TokenSetService;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private static final Set<String> UNSECURED_PATHS = Set.of(
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

    private final TokenSetService tokenSetService;

    public AuthenticationFilter(TokenSetService tokenSetService) {
        super(Config.class);
        this.tokenSetService = tokenSetService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            if (UNSECURED_PATHS.contains(path)) {
                return chain.filter(exchange);
            }

            String accessToken = exchange.getRequest().getHeaders().getFirst("Authorization");
            String refreshToken = null;

            if (accessToken == null) {
                accessToken = exchange.getRequest().getCookies().getFirst("accessToken").getValue();
                refreshToken = exchange.getRequest().getCookies().getFirst("refreshToken").getValue();
            }

            if (accessToken != null && refreshToken != null) {
                boolean isAccessTokenInBlacklist = tokenSetService.isTokenInBlackList(accessToken);
                boolean isRefreshTokenInBlacklist = tokenSetService.isTokenInBlackList(refreshToken);

                if (isAccessTokenInBlacklist || isRefreshTokenInBlacklist) {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
            }

            return chain.filter(exchange);
        };
    }

    public static class Config {

    }
}
