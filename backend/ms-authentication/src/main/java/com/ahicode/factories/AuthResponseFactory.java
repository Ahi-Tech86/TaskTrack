package com.ahicode.factories;

import com.ahicode.dtos.AuthResponse;
import com.ahicode.dtos.UserDto;
import org.springframework.stereotype.Component;

@Component
public class AuthResponseFactory {
    public AuthResponse makeAuthResponse(UserDto userDto, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .userDto(userDto)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
