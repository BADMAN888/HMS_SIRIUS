package com.sirius.sirius.security;

import com.sirius.sirius.dto.ReservationRequest;
import com.sirius.sirius.dto.AccrualRequest;
import com.sirius.sirius.dto.PackageAccrualRequest;
import com.sirius.sirius.dto.FolioRequest;
import com.sirius.sirius.dto.CashRegisterRequest;
import com.sirius.sirius.store.entity.UserEntity;
import com.sirius.sirius.store.enums.Role;
import com.sirius.sirius.store.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("hotelAccess")
public class HotelAccessService {

    private final ReservationRepository reservations;
    private final RoomRepository rooms;
    private final RoomCategoryRepository categories;
    private final RateRepository rates;
    private final FacilityRepository facilities;
    private final FolioRepository folios;
    private final AccrualRepository accruals;
    private final PaymentRepository payments;
    private final BalanceAccountRepository accounts;
    private final CashRegisterRepository cashRegisters;
    private final ProfileRepository profiles;
    private final GuestDocumentRepository guestDocuments;

    public HotelAccessService(
            ReservationRepository reservations,
            RoomRepository rooms,
            RoomCategoryRepository categories,
            RateRepository rates,
            FacilityRepository facilities,
            FolioRepository folios,
            AccrualRepository accruals,
            PaymentRepository payments,
            BalanceAccountRepository accounts,
            CashRegisterRepository cashRegisters,
            ProfileRepository profiles,
            GuestDocumentRepository guestDocuments
    ) {
        this.reservations = reservations;
        this.rooms = rooms;
        this.categories = categories;
        this.rates = rates;
        this.facilities = facilities;
        this.folios = folios;
        this.accruals = accruals;
        this.payments = payments;
        this.accounts = accounts;
        this.cashRegisters = cashRegisters;
        this.profiles = profiles;
        this.guestDocuments = guestDocuments;
    }

    public boolean isAdmin() {
        UserEntity user = currentUser();
        return user != null && user.getRole() == Role.ADMIN;
    }

    public boolean canAccessHotel(Long hotelId) {
        if (hotelId == null) return false;
        UserEntity user = currentUser();
        return user != null && (user.getRole() == Role.ADMIN
                || user.getHotel() != null && hotelId.equals(user.getHotel().getId()));
    }

    public boolean hasAssignedHotel() {
        UserEntity user = currentUser();
        return user != null && (user.getRole() == Role.ADMIN || user.getHotel() != null);
    }

    public boolean canCreateReservation(ReservationRequest request) {
        if (isAdmin()) return true;
        Long hotelId = currentHotelId();
        boolean profilesMatch = request != null
                && (request.guestIds() == null || request.guestIds().stream().allMatch(this::canAccessProfile))
                && (request.primaryGuestId() == null || canAccessProfile(request.primaryGuestId()));
        return request != null && hotelId != null && profilesMatch
                && rooms.existsByIdAndHotelId(request.roomId(), hotelId)
                && rates.existsByIdAndCategory_Hotel_Id(request.rateId(), hotelId)
                && categories.existsByIdAndHotelId(request.paymentCategoryId(), hotelId);
    }

    public boolean canUpdateReservation(Long reservationId, ReservationRequest request) {
        return canAccessReservation(reservationId) && canCreateReservation(request);
    }

    public boolean canCreateAccrual(AccrualRequest request) {
        if (isAdmin()) return true;
        return request != null && canAccessFacility(request.facilityId())
                && (request.reservationId() == null || canAccessReservation(request.reservationId()))
                && (request.folioId() == null || canAccessFolio(request.folioId()));
    }

    public boolean canCreatePackageAccrual(PackageAccrualRequest request) {
        if (isAdmin()) return true;
        return request != null && canAccessFacility(request.facilityId())
                && (request.reservationId() == null || canAccessReservation(request.reservationId()))
                && (request.folioId() == null || canAccessFolio(request.folioId()));
    }

    public boolean canCreateFolio(FolioRequest request) {
        if (isAdmin()) return true;
        return request != null && canAccessHotel(request.hotelId())
                && (request.reservationId() == null || canAccessReservation(request.reservationId()));
    }

    public boolean canCreateCashRegister(CashRegisterRequest request) {
        return request != null && canAccessHotel(request.hotelId());
    }

    public boolean canSearchCashOperations(Long hotelId, Long registerId) {
        if (isAdmin()) return true;
        if (hotelId != null) return canAccessHotel(hotelId);
        return registerId != null && canAccessCashRegister(registerId);
    }

    public boolean canAccessReservation(Long id) {
        if (isAdmin()) return true;
        Long hotelId = currentHotelId();
        return hotelId != null && reservations.existsByIdAndRoom_Hotel_Id(id, hotelId);
    }

    public boolean canAccessRoom(Long id) {
        if (isAdmin()) return true;
        Long hotelId = currentHotelId();
        return hotelId != null && rooms.existsByIdAndHotelId(id, hotelId);
    }

    public boolean canAccessCategory(Long id) {
        if (isAdmin()) return true;
        Long hotelId = currentHotelId();
        return hotelId != null && categories.existsByIdAndHotelId(id, hotelId);
    }

    public boolean canAccessRate(Long id) {
        if (isAdmin()) return true;
        Long hotelId = currentHotelId();
        return hotelId != null && rates.existsByIdAndCategory_Hotel_Id(id, hotelId);
    }

    public boolean canAccessFacility(Long id) {
        if (isAdmin()) return true;
        Long hotelId = currentHotelId();
        return hotelId != null && facilities.existsByIdAndHotelId(id, hotelId);
    }

    public boolean canAccessFolio(Long id) {
        if (isAdmin()) return true;
        Long hotelId = currentHotelId();
        return hotelId != null && folios.existsByIdAndHotelId(id, hotelId);
    }

    public boolean canAccessAccrual(Long id) {
        if (isAdmin()) return true;
        Long hotelId = currentHotelId();
        return hotelId != null && accruals.existsByIdAndHotelId(id, hotelId);
    }

    public boolean canAccessPayment(Long id) {
        if (isAdmin()) return true;
        Long hotelId = currentHotelId();
        return hotelId != null && payments.existsByIdAndFolio_Hotel_Id(id, hotelId);
    }

    public boolean canAccessAccount(Long id) {
        if (isAdmin()) return true;
        Long hotelId = currentHotelId();
        return hotelId != null && accounts.existsByIdAndHotelId(id, hotelId);
    }

    public boolean canAccessCashRegister(Long id) {
        if (isAdmin()) return true;
        Long hotelId = currentHotelId();
        return hotelId != null && cashRegisters.existsByIdAndHotelId(id, hotelId);
    }

    public boolean canAccessProfile(Long id) {
        if (isAdmin()) return true;
        Long hotelId = currentHotelId();
        return hotelId != null && profiles.existsByIdAndHotelId(id, hotelId);
    }

    public boolean canAccessGuestDocument(Long id) {
        if (isAdmin()) return true;
        Long hotelId = currentHotelId();
        return hotelId != null && guestDocuments.existsByIdAndProfile_Hotel_Id(id, hotelId);
    }

    public Long currentHotelId() {
        UserEntity user = currentUser();
        if (user == null) return null;
        if (user.getRole() == Role.ADMIN) return null;
        return user.getHotel() == null ? null : user.getHotel().getId();
    }

    private UserEntity currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof UserEntity user
                ? user : null;
    }
}
