
package com.sirius.sirius.service.profile;

import com.sirius.sirius.dto.GuestDocumentRequest;
import com.sirius.sirius.dto.GuestDocumentResponse;
import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.exeption.ValidationException;
import com.sirius.sirius.mappers.GuestDocumentMapper;
import com.sirius.sirius.store.entity.reservation.GuestDocumentEntity;
import com.sirius.sirius.store.entity.reservation.ProfileEntity;
import com.sirius.sirius.store.repository.GuestDocumentRepository;
import com.sirius.sirius.store.repository.ProfileRepository;
import com.sirius.sirius.security.HotelAccessService;
import com.sirius.sirius.exeption.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuestDocumentService {

    private final GuestDocumentRepository guestDocumentRepository;
    private final ProfileRepository profileRepository;
    private final GuestDocumentMapper guestDocumentMapper;
    private final HotelAccessService hotelAccess;

    public GuestDocumentResponse getById(Long id) {
        GuestDocumentEntity document = findDocument(id);
        ensureProfileAccess(document.getProfile());

        return guestDocumentMapper.toResponse(document);
    }

    public List<GuestDocumentResponse> getAll() {
        return guestDocumentRepository.findAll()
                .stream()
                .filter(document -> hotelAccess.isAdmin() || document.getProfile().getHotel() != null
                        && hotelAccess.canAccessHotel(document.getProfile().getHotel().getId()))
                .map(guestDocumentMapper::toResponse)
                .toList();
    }

    @Transactional
    public GuestDocumentResponse create(
            Long profileId,
            GuestDocumentRequest request
    ) {
        validateDates(request);
        validateUniqueDocument(request, null);

        ProfileEntity profile = profileRepository.findById(profileId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Profile with id: " + profileId + " not found"
                        )
                );
        ensureProfileAccess(profile);

        GuestDocumentEntity document =
                guestDocumentMapper.toEntity(request);

        document.setProfile(profile);
        profile.getGuestDocuments().add(document);

        GuestDocumentEntity savedDocument =
                guestDocumentRepository.save(document);

        return guestDocumentMapper.toResponse(savedDocument);
    }

    @Transactional
    public GuestDocumentResponse update(
            Long id,
            GuestDocumentRequest request
    ) {
        validateDates(request);

        GuestDocumentEntity document = findDocument(id);
        ensureProfileAccess(document.getProfile());

        validateUniqueDocument(request, document);

        guestDocumentMapper.updateEntity(request, document);

        GuestDocumentEntity updatedDocument =
                guestDocumentRepository.save(document);

        return guestDocumentMapper.toResponse(updatedDocument);
    }

    @Transactional
    public void delete(Long id) {
        GuestDocumentEntity document = findDocument(id);
        ensureProfileAccess(document.getProfile());

        guestDocumentRepository.delete(document);
    }

    private GuestDocumentEntity findDocument(Long id) {
        return guestDocumentRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Guest document with id: " + id + " not found"
                        )
                );
    }

    private void ensureProfileAccess(ProfileEntity profile) {
        if (!hotelAccess.isAdmin() && (profile.getHotel() == null
                || !hotelAccess.canAccessHotel(profile.getHotel().getId()))) {
            throw new ForbiddenException("Немає доступу до документів гостя цього готелю");
        }
    }

    private void validateDates(GuestDocumentRequest request) {
        if (request.issueDate() != null
                && request.expiryDate() != null
                && request.issueDate().isAfter(request.expiryDate())) {

            throw new ValidationException(
                    "Document issue date cannot be after expiry date"
            );
        }
    }

    private void validateUniqueDocument(
            GuestDocumentRequest request,
            GuestDocumentEntity existingDocument
    ) {
        boolean documentExists;

        if (existingDocument == null) {
            documentExists =
                    guestDocumentRepository
                            .existsByDocumentTypeAndDocumentNumber(
                                    request.documentType(),
                                    request.documentNumber()
                            );
        } else {
            documentExists =
                    guestDocumentRepository
                            .existsByDocumentTypeAndDocumentNumberAndIdNot(
                                    request.documentType(),
                                    request.documentNumber(),
                                    existingDocument.getId()
                            );
        }

        if (documentExists) {
            throw new ConflictException(
                    "Guest document with type "
                            + request.documentType()
                            + " and number "
                            + request.documentNumber()
                            + " already exists"
            );
        }
    }
}
