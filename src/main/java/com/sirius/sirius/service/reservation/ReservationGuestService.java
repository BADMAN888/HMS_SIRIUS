
package com.sirius.sirius.service.reservation;

import com.sirius.sirius.exeption.BadRequestException;
import com.sirius.sirius.exeption.NotFoundException;
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
            throw new BadRequestException(
                    "Reservation must have at least one guest"
            );
        }

        Set<Long> uniqueGuestIds = new HashSet<>(guestIds);

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
            throw new BadRequestException(
                    "Primary guest must be specified"
            );
        }

        return guests.stream()
                .filter(guest -> guest.getId().equals(primaryGuestId))
                .findFirst()
                .orElseThrow(() ->
                        new BadRequestException(
                                "Primary guest must belong to reservation guests"
                        )
                );
    }
}
