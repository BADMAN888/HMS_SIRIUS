
package com.sirius.sirius.service.profile;

import com.sirius.sirius.dto.ProfileRequest;
import com.sirius.sirius.dto.ProfileReservationResponse;
import com.sirius.sirius.dto.ProfileResponse;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.mappers.ProfileMapper;
import com.sirius.sirius.store.entity.reservation.ProfileEntity;
import com.sirius.sirius.store.repository.ProfileRepository;
import com.sirius.sirius.store.repository.ReservationRepository;
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

    public ProfileResponse getById(Long id) {
        ProfileEntity profile = findProfile(id);

        return profileMapper.toResponse(profile);
    }

    public List<ProfileResponse> getAll() {
        return profileRepository.findAllWithGuestDocuments()
                .stream()
                .map(profileMapper::toResponse)
                .toList();
    }

    @Transactional
    public ProfileResponse create(ProfileRequest request) {
        ProfileEntity profile = profileMapper.toEntity(request);

        ProfileEntity savedProfile = profileRepository.save(profile);

        return profileMapper.toResponse(savedProfile);
    }

    @Transactional
    public ProfileResponse update(Long id, ProfileRequest request) {
        ProfileEntity profile = findProfile(id);

        profileMapper.updateEntity(request, profile);

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
                .map(reservation -> new ProfileReservationResponse(
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
        return profileRepository.findByIdWithGuestDocuments(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Profile with id: " + id + " not found"
                        )
                );
    }
}
