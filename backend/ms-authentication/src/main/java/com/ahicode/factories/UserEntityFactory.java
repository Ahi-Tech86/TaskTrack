package com.ahicode.factories;

import com.ahicode.dtos.TemporaryUserDto;
import com.ahicode.storage.entities.UserEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static com.ahicode.enums.AppRole.ADMIN;
import static com.ahicode.enums.AppRole.USER;

@Component
public class UserEntityFactory {
    public UserEntity makeUserEntity(TemporaryUserDto userDto) {
        return UserEntity.builder()
                .email(userDto.getEmail())
                .nickname(userDto.getNickname())
                .firstname(userDto.getFirstname())
                .lastname(userDto.getLastname())
                .role(USER)
                .createAt(Instant.now())
                .build();
    }

    public UserEntity makeAdminUserEntity(TemporaryUserDto userDto) {
        return UserEntity.builder()
                .email(userDto.getEmail())
                .nickname(userDto.getNickname())
                .firstname(userDto.getFirstname())
                .lastname(userDto.getLastname())
                .role(ADMIN)
                .createAt(Instant.now())
                .build();
    }
}
