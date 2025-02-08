package com.ahicode.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutingConfig {

    @Value("${authentication.service.url}")
    private String authServiceUrl;

    @Autowired
    private AuthenticationFilter filter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth_service",
                        route -> route
                                .path("/api/v1/auth/**")
                                .filters(f -> f.filter(filter.apply(new AuthenticationFilter.Config())))
                                .uri("lb://AUTH-SERVICE")
                )
                .build();
    }
}
