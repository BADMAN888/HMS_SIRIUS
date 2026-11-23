package com.sirius.sirius.service.auth;

import com.sirius.sirius.store.entity.RefreshTokenEntity;
import com.sirius.sirius.store.entity.UserEntity;
import com.sirius.sirius.store.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.Clock;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    @Test
    void rotationLocksAndRevokesThePresentedTokenBeforeIssuingAnother() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        RefreshTokenService service = new RefreshTokenService(repository, Clock.systemUTC());
        UserEntity user = new UserEntity();
        RefreshTokenEntity oldToken = new RefreshTokenEntity();
        oldToken.setToken("old-token");
        oldToken.setUser(user);
        oldToken.setExpiresAt(Instant.now().plusSeconds(600));
        when(repository.findByTokenForUpdate("old-token")).thenReturn(Optional.of(oldToken));
        when(repository.save(any(RefreshTokenEntity.class))).thenAnswer(call -> call.getArgument(0));

        RefreshTokenEntity rotated = service.rotate("old-token");

        assertTrue(oldToken.isRevoked());
        assertNotNull(rotated.getToken());
        assertNotEquals("old-token", rotated.getToken());
        assertFalse(rotated.isRevoked());
        verify(repository).findByTokenForUpdate("old-token");
        verify(repository, times(2)).save(any(RefreshTokenEntity.class));
    }
}
