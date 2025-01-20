package com.ahicode.storage.repositories;

import com.ahicode.storage.entities.RefreshTokenEntity;
import io.lettuce.core.dynamic.annotation.Param;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<RefreshTokenEntity, Long> {
    @NotNull Optional<RefreshTokenEntity> findById(@NotNull Long id);

    @Query("SELECT rt FROM RefreshTokenEntity rt WHERE rt.user.nickname = :nickname")
    Optional<RefreshTokenEntity> findByNickname(@Param("nickname") String nickname);
}
