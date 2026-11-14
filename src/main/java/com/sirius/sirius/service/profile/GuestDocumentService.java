
package com.sirius.sirius.service.profile;

import com.sirius.sirius.dto.GuestDocumentRequest;
import com.sirius.sirius.dto.GuestDocumentResponse;
import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.mappers.GuestDocumentMapper;
import com.sirius.sirius.store.entity.reservation.GuestDocumentEntity;
import com.sirius.sirius.store.entity.reservation.ProfileEntity;
import com.sirius.sirius.store.repository.GuestDocumentRepository;
import com.sirius.sirius.store.repository.ProfileRepository;
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

    public GuestDocumentResponse getById(Long id) {
        GuestDocumentEntity document = findDocument(id);

        return guestDocumentMapper.toResponse(document);
    }

    public List<GuestDocumentResponse> getAll() {
        return guestDocumentRepository.findAll()
                .stream()
                .map(guestDocumentMapper::toResponse)
                .toList();
    }

    @Transactional
    public GuestDocumentResponse create(
            Long profileId,
            GuestDocumentRequest request
    ) {
        ProfileEntity profile = profileRepository.findById(profileId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Profile with id: " + profileId + " not found"
                        )
                );

        validateUniqueDocument(request, null);

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
        GuestDocumentEntity document = findDocument(id);

        validateUniqueDocument(request, document);

        guestDocumentMapper.updateEntity(request, document);

        GuestDocumentEntity updatedDocument =
                guestDocumentRepository.save(document);

        return guestDocumentMapper.toResponse(updatedDocument);
    }

    @Transactional
    public void delete(Long id) {
        GuestDocumentEntity document = findDocument(id);

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
