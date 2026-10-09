package com.sirius.sirius.service.auth;

import com.sirius.sirius.store.entity.RefreshTokenEntity;
import com.sirius.sirius.store.entity.UserEntity;
import com.sirius.sirius.store.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final long REFRESH_TOKEN_DAYS = 30;

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RefreshTokenEntity create(UserEntity user) {
        RefreshTokenEntity refreshToken = new RefreshTokenEntity();

        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(
                Instant.now().plus(REFRESH_TOKEN_DAYS, ChronoUnit.DAYS)
        );
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshTokenEntity rotate(String token) {
        RefreshTokenEntity current = refreshTokenRepository.findByToken(token)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid refresh token")
                );

        if (current.isRevoked() ||
                !current.getExpiresAt().isAfter(Instant.now())) {
            throw new IllegalArgumentException(
                    "Refresh token is expired or revoked"
            );
        }

        current.setRevoked(true);
        refreshTokenRepository.save(current);

        return create(current.getUser());
    }
}