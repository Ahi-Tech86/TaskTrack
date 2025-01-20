package com.ahicode.services;

import com.ahicode.storage.entities.UserEntity;

public interface TokenService {
    void createAndSaveToken(UserEntity user);
    String getTokenByUserNickname(String nickname);
}
