package com.ahicode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class ApiGatewayRunner {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayRunner.class, args);
    }
}