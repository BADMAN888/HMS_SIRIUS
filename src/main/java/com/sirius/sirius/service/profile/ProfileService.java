
package com.sirius.sirius.service.profile;

import com.sirius.sirius.dto.ProfileRequest;
import com.sirius.sirius.dto.ProfileReservationResponse;
import com.sirius.sirius.dto.ProfileResponse;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.mappers.ProfileMapper;
import com.sirius.sirius.store.entity.reservation.ProfileEntity;
import com.sirius.sirius.store.repository.ProfileRepository;
import com.sirius.sirius.store.repository.ReservationRepository;
import com.sirius.sirius.store.repository.HotelRepository;
import com.sirius.sirius.security.HotelAccessService;
import com.sirius.sirius.store.entity.hotel.HotelEntity;
import com.sirius.sirius.exeption.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;
    private final ReservationRepository reservationRepository;
    private final HotelRepository hotelRepository;
    private final HotelAccessService hotelAccess;

    public ProfileResponse getById(Long id) {
        ProfileEntity profile = findProfile(id);

        return profileMapper.toResponse(profile);
    }

    public List<ProfileResponse> getAll() {
        return profileRepository.findAllWithGuestDocuments()
                .stream()
                .filter(profile -> hotelAccess.isAdmin()
                        || profile.getHotel() != null && hotelAccess.canAccessHotel(profile.getHotel().getId()))
                .map(profileMapper::toResponse)
                .toList();
    }

    public List<ProfileResponse> getAllBlacklistedOrVip() {
        return profileRepository.findAllBlacklistedOrVip()
                .stream()
                .filter(profile -> hotelAccess.isAdmin()
                        || profile.getHotel() != null && hotelAccess.canAccessHotel(profile.getHotel().getId()))
                .map(profileMapper::toResponse)
                .toList();
    }

    @Transactional
    public ProfileResponse create(ProfileRequest request) {
        ProfileEntity profile = profileMapper.toEntity(request);
        profile.setHotel(resolveProfileHotel(request.hotelId()));

        ProfileEntity savedProfile = profileRepository.save(profile);

        return profileMapper.toResponse(savedProfile);
    }

    @Transactional
    public ProfileResponse update(Long id, ProfileRequest request) {
        ProfileEntity profile = findProfile(id);

        profileMapper.updateEntity(request, profile);
        if (hotelAccess.isAdmin() && request.hotelId() != null) {
            profile.setHotel(resolveProfileHotel(request.hotelId()));
        }

        ProfileEntity updatedProfile = profileRepository.save(profile);

        return profileMapper.toResponse(updatedProfile);
    }

    @Transactional
    public void delete(Long id) {
        ProfileEntity profile = findProfile(id);

        profileRepository.delete(profile);
    }

    public List<ProfileReservationResponse> getReservations(Long profileId) {
        findProfile(profileId);

        return reservationRepository.findAllByGuestProfileId(profileId)
                .stream()
                .filter(reservation -> hotelAccess.canAccessHotel(reservation.getRoom().getHotel().getId()))
                .map(reservation -> new ProfileReservationResponse(
                        reservation.getId(),
                        reservation.getConfirmationNumber(),
                        reservation.getCheckIn(),
                        reservation.getCheckOut(),
                        reservation.getRoom().getCategory().getName(),
                        reservation.getRoom().getRoomNumber(),
                        reservation.getRate().getName()
                ))
                .toList();
    }

    private ProfileEntity findProfile(Long id) {
        ProfileEntity profile = profileRepository.findByIdWithGuestDocuments(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Profile with id: " + id + " not found"
                        )
                );
        if (!hotelAccess.isAdmin()
                && (profile.getHotel() == null || !hotelAccess.canAccessHotel(profile.getHotel().getId()))) {
            throw new ForbiddenException("Немає доступу до профілю гостя цього готелю");
        }
        return profile;
    }

    private HotelEntity resolveProfileHotel(Long requestedHotelId) {
        if (hotelAccess.isAdmin() && requestedHotelId == null) return null;
        Long hotelId = hotelAccess.isAdmin() ? requestedHotelId : hotelAccess.currentHotelId();
        if (hotelId == null) {
            throw new ForbiddenException("Профіль гостя має бути прив'язаний до готелю");
        }
        if (!hotelAccess.canAccessHotel(hotelId)) {
            throw new ForbiddenException("Немає доступу до вибраного готелю");
        }
        return hotelRepository.findById(hotelId)
                .orElseThrow(() -> new NotFoundException("Готель не знайдено"));
    }
}
