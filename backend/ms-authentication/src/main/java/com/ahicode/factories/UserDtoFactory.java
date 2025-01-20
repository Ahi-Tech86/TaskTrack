package com.ahicode.factories;

import com.ahicode.dtos.UserDto;
import com.ahicode.storage.entities.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserDtoFactory {
    public UserDto makeUserDto(UserEntity entity) {
        return UserDto.builder()
                .email(entity.getEmail())
                .nickname(entity.getNickname())
                .firstname(entity.getFirstname())
                .lastname(entity.getLastname())
                .build();
    }
}
