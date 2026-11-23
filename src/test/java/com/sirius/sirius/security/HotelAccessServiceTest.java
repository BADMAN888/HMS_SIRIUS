package com.sirius.sirius.security;

import com.sirius.sirius.store.entity.UserEntity;
import com.sirius.sirius.store.entity.hotel.HotelEntity;
import com.sirius.sirius.store.enums.Role;
import com.sirius.sirius.store.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HotelAccessServiceTest {

    private ReservationRepository reservations;
    private RoomRepository rooms;
    private HotelAccessService service;

    @BeforeEach
    void setUp() {
        reservations = mock(ReservationRepository.class);
        rooms = mock(RoomRepository.class);
        service = new HotelAccessService(
                reservations,
                rooms,
                mock(RoomCategoryRepository.class),
                mock(RateRepository.class),
                mock(FacilityRepository.class),
                mock(FolioRepository.class),
                mock(AccrualRepository.class),
                mock(PaymentRepository.class),
                mock(BalanceAccountRepository.class),
                mock(CashRegisterRepository.class),
                mock(ProfileRepository.class),
                mock(GuestDocumentRepository.class)
        );
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void hotelUserCanOnlyAccessTheAssignedHotel() {
        authenticate(Role.USER, 41L);

        assertTrue(service.canAccessHotel(41L));
        assertFalse(service.canAccessHotel(42L));
        assertEquals(41L, service.currentHotelId());
    }

    @Test
    void adminHasGlobalHotelAccess() {
        authenticate(Role.ADMIN, null);

        assertTrue(service.canAccessHotel(41L));
        assertTrue(service.hasAssignedHotel());
        assertTrue(service.isAdmin());
    }

    @Test
    void reservationLookupIsConstrainedToTheAssignedHotel() {
        authenticate(Role.USER, 41L);
        when(reservations.existsByIdAndRoom_Hotel_Id(10L, 41L)).thenReturn(true);

        assertTrue(service.canAccessReservation(10L));
        verify(reservations).existsByIdAndRoom_Hotel_Id(10L, 41L);
        verifyNoMoreInteractions(reservations);
    }

    @Test
    void unassignedUserCannotAccessAnyHotel() {
        authenticate(Role.USER, null);

        assertFalse(service.hasAssignedHotel());
        assertFalse(service.canAccessHotel(41L));
        assertNull(service.currentHotelId());
    }

    private void authenticate(Role role, Long hotelId) {
        UserEntity user = new UserEntity();
        user.setRole(role);
        if (hotelId != null) {
            HotelEntity hotel = new HotelEntity();
            hotel.setId(hotelId);
            user.setHotel(hotel);
        }
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, "", List.of())
        );
    }
}
