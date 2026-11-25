
package com.sirius.sirius.service.profile;

import com.sirius.sirius.dto.ProfileRequest;
import com.sirius.sirius.dto.ProfileReservationResponse;
import com.sirius.sirius.dto.ProfileResponse;
import com.sirius.sirius.dto.ProfilePageResponse;
import com.sirius.sirius.dto.ProfileMergeRequest;
import com.sirius.sirius.dto.SimilarProfilePairResponse;
import com.sirius.sirius.store.enums.Nationality;
import com.sirius.sirius.store.entity.biling.*;
import com.sirius.sirius.store.entity.reservation.GuestDocumentEntity;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.mappers.ProfileMapper;
import com.sirius.sirius.store.entity.reservation.ProfileEntity;
import com.sirius.sirius.store.repository.ProfileRepository;
import com.sirius.sirius.store.repository.ReservationRepository;
import com.sirius.sirius.store.repository.HotelRepository;
import com.sirius.sirius.store.repository.FolioRepository;
import com.sirius.sirius.store.repository.AccrualRepository;
import com.sirius.sirius.store.repository.PaymentRepository;
import com.sirius.sirius.store.repository.BalanceAccountRepository;
import com.sirius.sirius.store.repository.GuestDocumentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.time.LocalDate;
import java.util.Locale;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
    private final FolioRepository folioRepository;
    private final AccrualRepository accrualRepository;
    private final PaymentRepository paymentRepository;
    private final BalanceAccountRepository balanceAccountRepository;
    private final GuestDocumentRepository guestDocumentRepository;

    public ProfilePageResponse searchProfiles(Long requestedHotelId, int page, int size,
            String firstName, String lastName, String middleName, String phone, String email,
            LocalDate dateOfBirth, Nationality nationality, Boolean vip, Boolean blacklisted) {
        Long hotelId = resolveSearchHotel(requestedHotelId);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 20);
        var result = profileRepository.searchProfiles(hotelId, clean(firstName), clean(lastName), clean(middleName),
                clean(phone), clean(email), dateOfBirth, nationality, vip, blacklisted,
                PageRequest.of(safePage, safeSize, Sort.by("lastName").ascending().and(Sort.by("firstName").ascending()).and(Sort.by("id").ascending())));
        return new ProfilePageResponse(result.getContent().stream().map(profileMapper::toResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    public List<SimilarProfilePairResponse> findSimilarProfiles(Long requestedHotelId) {
        Long hotelId = resolveSearchHotel(requestedHotelId);
        List<ProfileEntity> profiles = hotelId == null ? profileRepository.findAll() : profileRepository.findAllByHotel_Id(hotelId);
        List<SimilarProfilePairResponse> pairs = new ArrayList<>();
        Map<String, List<ProfileEntity>> buckets = new HashMap<>();
        for (ProfileEntity profile : profiles) {
            if (profile.getHotel() == null) continue;
            addBucket(buckets, "phone:" + normalize(profile.getPhoneNumber()), profile.getPhoneNumber(), profile);
            addBucket(buckets, "email:" + normalize(profile.getEmail()), profile.getEmail(), profile);
            String first = normalize(profile.getFirstName()), last = normalize(profile.getLastName());
            addBucket(buckets, "name:" + first + ":" + last, first, profile);
            if (!first.isBlank() && !last.isBlank()) addBucket(buckets, "fuzzy:" + first.charAt(0) + ":" + last.charAt(0), first + last, profile);
        }
        Set<String> seen = new HashSet<>();
        for (List<ProfileEntity> bucket : buckets.values()) for (int i = 0; i < bucket.size(); i++) for (int j = i + 1; j < bucket.size(); j++) {
            ProfileEntity a = bucket.get(i), b = bucket.get(j);
            long low = Math.min(a.getId(), b.getId()), high = Math.max(a.getId(), b.getId());
            if (!seen.add(low + ":" + high)) continue;
            String reason = similarityReason(a, b);
            if (reason != null) pairs.add(new SimilarProfilePairResponse(profileMapper.toResponse(a), profileMapper.toResponse(b), reason));
        }
        return pairs;
    }

    private static void addBucket(Map<String, List<ProfileEntity>> buckets, String key, String value, ProfileEntity profile) {
        if (value != null && !value.isBlank()) buckets.computeIfAbsent(key, ignored -> new ArrayList<>()).add(profile);
    }

    @Transactional
    public ProfileResponse mergeProfiles(ProfileMergeRequest request) {
        if (request.keepProfileId().equals(request.mergeProfileId())) throw new ForbiddenException("Оберіть два різні профайли");
        ProfileEntity keep = findProfile(request.keepProfileId());
        ProfileEntity duplicate = findProfile(request.mergeProfileId());
        if (keep.getHotel() == null || duplicate.getHotel() == null || !keep.getHotel().getId().equals(duplicate.getHotel().getId())) {
            throw new ForbiddenException("Об’єднати можна лише профайли одного готелю");
        }
        reservationsForMerge(duplicate.getId()).forEach(reservation -> {
            if (reservation.getPrimaryGuest() != null && reservation.getPrimaryGuest().getId().equals(duplicate.getId())) reservation.setPrimaryGuest(keep);
            if (reservation.getGuests().remove(duplicate)) reservation.getGuests().add(keep);
        });
        folioRepository.findAllByPayerId(duplicate.getId()).forEach(folio -> folio.setPayer(keep));
        folioRepository.findAllByRecipientId(duplicate.getId()).forEach(folio -> folio.setRecipient(keep));
        accrualRepository.findAllByRecipientId(duplicate.getId()).forEach(accrual -> accrual.setRecipient(keep));
        paymentRepository.findAllByPayerId(duplicate.getId()).forEach(payment -> payment.setPayer(keep));
        balanceAccountRepository.findAllByCustomerId(duplicate.getId()).forEach(account -> account.setCustomer(keep));
        guestDocumentRepository.findAllByProfileId(duplicate.getId()).forEach(document -> document.setProfile(keep));
        copyMissingFields(keep, duplicate);
        keep.setVip(keep.isVip() || duplicate.isVip());
        keep.setInBlackList(keep.isInBlackList() || duplicate.isInBlackList());
        profileRepository.save(keep);
        profileRepository.delete(duplicate);
        return profileMapper.toResponse(keep);
    }

    private List<com.sirius.sirius.store.entity.reservation.ReservationEntity> reservationsForMerge(Long profileId) {
        return reservationRepository.findAllByGuestProfileId(profileId);
    }

    private Long resolveSearchHotel(Long requestedHotelId) {
        if (hotelAccess.isAdmin()) return requestedHotelId;
        Long currentHotelId = hotelAccess.currentHotelId();
        if (currentHotelId == null || requestedHotelId != null && !currentHotelId.equals(requestedHotelId)) throw new ForbiddenException("Немає доступу до профайлів цього готелю");
        return currentHotelId;
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static String normalize(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", ""); }
    private static String similarityReason(ProfileEntity a, ProfileEntity b) {
        String aPhone = normalize(a.getPhoneNumber()), bPhone = normalize(b.getPhoneNumber());
        String aEmail = normalize(a.getEmail()), bEmail = normalize(b.getEmail());
        if (!aPhone.isBlank() && aPhone.equals(bPhone)) return "Однаковий номер телефону";
        if (!aEmail.isBlank() && aEmail.equals(bEmail)) return "Однакова електронна пошта";
        String aName = normalize(a.getFirstName()) + normalize(a.getLastName());
        String bName = normalize(b.getFirstName()) + normalize(b.getLastName());
        if (aName.length() >= 5 && (aName.equals(bName) || editDistance(aName, bName) <= 2)) return "Схожі ім’я та прізвище";
        return null;
    }
    private static int editDistance(String a, String b) {
        int[] row = new int[b.length() + 1]; for (int j=0;j<=b.length();j++) row[j]=j;
        for (int i=1;i<=a.length();i++) { int prev=row[0]; row[0]=i; for (int j=1;j<=b.length();j++) { int old=row[j]; row[j]=Math.min(Math.min(row[j]+1,row[j-1]+1),prev+(a.charAt(i-1)==b.charAt(j-1)?0:1)); prev=old; } }
        return row[b.length()];
    }
    private static void copyMissingFields(ProfileEntity a, ProfileEntity b) {
        if (blank(a.getFirstName())) a.setFirstName(b.getFirstName()); if (blank(a.getLastName())) a.setLastName(b.getLastName());
        if (blank(a.getMiddleName())) a.setMiddleName(b.getMiddleName()); if (blank(a.getPhoneNumber())) a.setPhoneNumber(b.getPhoneNumber());
        if (blank(a.getEmail())) a.setEmail(b.getEmail()); if (a.getGender()==null) a.setGender(b.getGender());
        if (a.getDateOfBirth()==null) a.setDateOfBirth(b.getDateOfBirth()); if (a.getNationality()==null) a.setNationality(b.getNationality());
        if (blank(a.getAddress())) a.setAddress(b.getAddress()); if (blank(a.getRegistrationAddress())) a.setRegistrationAddress(b.getRegistrationAddress());
        if (blank(a.getComment())) a.setComment(b.getComment());
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }

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
