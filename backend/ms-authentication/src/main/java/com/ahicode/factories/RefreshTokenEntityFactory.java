package com.ahicode.factories;

import com.ahicode.storage.entities.RefreshTokenEntity;
import com.ahicode.storage.entities.UserEntity;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class RefreshTokenEntityFactory {
    public RefreshTokenEntity makeRefreshTokenEntity(UserEntity user, String token, Date expirationTime) {
        return RefreshTokenEntity.builder()
                .user(user)
                .token(token)
                .createAt(new Date(System.currentTimeMillis()))
                .expiresAt(expirationTime)
                .build();
    }
}
