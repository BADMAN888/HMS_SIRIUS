package com.sirius.sirius.service.auth;

import com.sirius.sirius.dto.auth.AuthRequest;
import com.sirius.sirius.dto.auth.AuthResponse;
import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.UnauthorizedException;
import com.sirius.sirius.exeption.ForbiddenException;
import com.sirius.sirius.store.entity.RefreshTokenEntity;
import com.sirius.sirius.store.entity.UserEntity;
import com.sirius.sirius.store.enums.Role;
import com.sirius.sirius.security.JwtService;
import com.sirius.sirius.store.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.auth.self-registration-enabled:false}")
    private boolean selfRegistrationEnabled;

    @Transactional
    public AuthResponse register(AuthRequest request) {
        if (!selfRegistrationEnabled) {
            throw new ForbiddenException("Самостійну реєстрацію вимкнено; обліковий запис створює адміністратор готелю");
        }
        String email = normalizeEmail(request.email());
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ConflictException(
                    "User with this email already exists"
            );
        }

        UserEntity user = new UserEntity();

        user.setEmail(email);
        user.setPassword(
                passwordEncoder.encode(request.password())
        );
        user.setRole(Role.USER);

        userRepository.save(user);

        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        UserEntity user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() ->
                        new UnauthorizedException("Invalid credentials")
                );

        boolean matches = passwordEncoder.matches(
                request.password(),
                user.getPassword()
        );

        if (!matches) {
            throw new UnauthorizedException("Invalid credentials");
        }

        return createAuthResponse(user);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshTokenEntity rotated =
                refreshTokenService.rotate(refreshToken);

        String accessToken =
                jwtService.generateToken(
                        rotated.getUser().getEmail()
                );

        return new AuthResponse(
                accessToken,
                rotated.getToken()
        );
    }

    private AuthResponse createAuthResponse(UserEntity user) {
        String accessToken =
                jwtService.generateToken(user.getEmail());

        RefreshTokenEntity refreshToken =
                refreshTokenService.create(user);

        return new AuthResponse(
                accessToken,
                refreshToken.getToken()
        );
    }
}
