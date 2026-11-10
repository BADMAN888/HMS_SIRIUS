package com.sirius.sirius.service.reservation;

import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.exeption.ValidationException;
import com.sirius.sirius.store.entity.reservation.ProfileEntity;
import com.sirius.sirius.store.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReservationGuestService {

    private final ProfileRepository profileRepository;

    public Set<ProfileEntity> findGuests(Set<Long> guestIds) {
        if (guestIds == null || guestIds.isEmpty()) {
            throw new ValidationException(
                    "Reservation must have at least one guest"
            );
        }

        Set<Long> uniqueGuestIds = new HashSet<>(guestIds);

        if (uniqueGuestIds.contains(null)) {
            throw new ValidationException(
                    "Guest ids cannot contain null values"
            );
        }

        List<ProfileEntity> guests =
                profileRepository.findAllById(uniqueGuestIds);

        if (guests.size() != uniqueGuestIds.size()) {
            throw new NotFoundException(
                    "One or more guests not found"
            );
        }

        return new HashSet<>(guests);
    }

    public ProfileEntity findPrimaryGuest(
            Long primaryGuestId,
            Set<ProfileEntity> guests
    ) {
        if (primaryGuestId == null) {
            throw new ValidationException(
                    "Primary guest must be specified"
            );
        }

        if (guests == null || guests.isEmpty()) {
            throw new ValidationException(
                    "Reservation must have at least one guest"
            );
        }

        return guests.stream()
                .filter(guest -> guest.getId().equals(primaryGuestId))
                .findFirst()
                .orElseThrow(() ->
                        new ValidationException(
                                "Primary guest must belong to reservation guests"
                        )
                );
    }
}