package com.sirius.sirius.service.auth;

import com.sirius.sirius.dto.AdminUserRequest;
import com.sirius.sirius.dto.AdminUserResponse;
import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.ForbiddenException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.exeption.UnauthorizedException;
import com.sirius.sirius.store.entity.UserEntity;
import com.sirius.sirius.store.entity.hotel.HotelEntity;
import com.sirius.sirius.store.enums.Role;
import com.sirius.sirius.store.repository.HotelRepository;
import com.sirius.sirius.store.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserService {

    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserResponse createHotelUser(AdminUserRequest request) {
        requireAdmin();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ConflictException("Користувач із таким email вже існує");
        }
        HotelEntity hotel = hotelRepository.findById(request.hotelId())
                .orElseThrow(() -> new NotFoundException("Готель не знайдено"));
        UserEntity user = new UserEntity();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setHotel(hotel);
        return toResponse(userRepository.save(user));
    }

    public List<AdminUserResponse> getUsers() {
        requireAdmin();
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    public AdminUserResponse assignHotel(Long userId, Long hotelId) {
        requireAdmin();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Користувача не знайдено"));
        if (user.getRole() == Role.ADMIN) {
            throw new ConflictException("Адміністратори мають глобальний доступ і не прив'язуються до одного готелю");
        }
        HotelEntity hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new NotFoundException("Готель не знайдено"));
        user.setHotel(hotel);
        return toResponse(user);
    }

    private AdminUserResponse toResponse(UserEntity user) {
        return new AdminUserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                user.getHotel() == null ? null : user.getHotel().getId());
    }

    private void requireAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserEntity user)) {
            throw new UnauthorizedException("Потрібна авторизація");
        }
        if (user.getRole() != Role.ADMIN) throw new ForbiddenException("Доступ лише для адміністратора");
    }
}
