package com.sirius.sirius.service.auth;

import com.sirius.sirius.dto.auth.AuthRequest;
import com.sirius.sirius.dto.auth.AuthResponse;
import com.sirius.sirius.security.JwtService;
import com.sirius.sirius.store.entity.UserEntity;
import com.sirius.sirius.store.enums.Role;
import com.sirius.sirius.store.repository.UserRepository;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;



@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(AuthRequest request) {

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ValidationException("Email already exists");
        }

        UserEntity user = new UserEntity();

        user.setEmail(request.email());
        user.setPassword(
                passwordEncoder.encode(request.password())
        );
        user.setRole(Role.USER);

        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());

        return new AuthResponse(token);
    }

    public AuthResponse login(AuthRequest request) {

        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new ValidationException("Invalid credentials")
                );

        boolean matches = passwordEncoder.matches(
                request.password(),
                user.getPassword()
        );

        if (!matches) {
            throw new ValidationException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail());

        return new AuthResponse(token);
    }
}