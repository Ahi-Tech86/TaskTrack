package com.ahicode.services.impl;

import com.ahicode.enums.AppRole;
import com.ahicode.exceptions.AppException;
import com.ahicode.factories.RefreshTokenEntityFactory;
import com.ahicode.services.EncryptionService;
import com.ahicode.services.JwtService;
import com.ahicode.services.TokenService;
import com.ahicode.storage.entities.RefreshTokenEntity;
import com.ahicode.storage.entities.UserEntity;
import com.ahicode.storage.repositories.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    @Value("${application.security.jwt.refresh-token.expiration}")
    private Long refreshTokenExpiration;

    private final JwtService jwtService;
    private final TokenRepository repository;
    private final RefreshTokenEntityFactory factory;
    private final EncryptionService encryptionService;

    @Override
    @Transactional
    public void createAndSaveToken(UserEntity user) {
        Long userId = user.getId();
        AppRole userRole = user.getRole();
        String userNickname = user.getNickname();

        String refreshToken = jwtService.generateRefreshToken(userId, userNickname, userRole);
        Date expirationTime = new Date(System.currentTimeMillis() + refreshTokenExpiration);

        String encryptedToken = encryptionService.encrypt(refreshToken);

        RefreshTokenEntity tokenEntity = factory.makeRefreshTokenEntity(user, encryptedToken, expirationTime);
        repository.saveAndFlush(tokenEntity);
    }

    @Override
    public String getTokenByUserNickname(String nickname) {
        Logger logger = LoggerFactory.getLogger(getClass());

        try {
            RefreshTokenEntity refreshTokenEntity = repository.findByNickname(nickname).orElseThrow(
                    () -> new AppException(
                            String.format("Token for user with nickname %s doesn't exists", nickname), HttpStatus.NOT_FOUND
                    )
            );

            String encryptedToken = refreshTokenEntity.getToken();
            String decryptedToken = encryptionService.decrypt(encryptedToken);

            if (jwtService.isRefreshTokenExpired(decryptedToken)) {
                String newRefreshToken = jwtService.generateRefreshToken(
                        jwtService.extractUserIdFromAccessToken(decryptedToken),
                        nickname,
                        jwtService.extractRoleFromAccessToken(decryptedToken)
                );
                String encryptedNewToken = encryptionService.encrypt(newRefreshToken);
                refreshTokenEntity.setToken(encryptedNewToken);
                refreshTokenEntity.setCreateAt(new Date(System.currentTimeMillis()));
                refreshTokenEntity.setExpiresAt(new Date(System.currentTimeMillis() + refreshTokenExpiration));
                repository.saveAndFlush(refreshTokenEntity);
                log.info("Refresh token for user {} was successfully updated", nickname);

                return newRefreshToken;
            }

            return decryptedToken;
        } catch (AppException exception) {
            logger.error("Error when receiving a token for a user with nickname {}: {}", nickname, exception.getMessage());
            throw exception;
        } catch (Exception exception) {
            logger.error("An unexpected error occurred {}", exception.getMessage());
            throw new AppException("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
