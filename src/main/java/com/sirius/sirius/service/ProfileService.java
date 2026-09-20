package com.sirius.sirius.service;

import com.sirius.sirius.dto.ProfileRequest;
import com.sirius.sirius.dto.ProfileResponse;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.mappers.GuestDocumentMapper;
import com.sirius.sirius.mappers.ProfileMapper;
import com.sirius.sirius.store.entity.reservation.GuestDocumentEntity;
import com.sirius.sirius.store.entity.reservation.ProfileEntity;
import com.sirius.sirius.store.repository.GuestDocumentRepository;
import com.sirius.sirius.store.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final GuestDocumentRepository guestDocumentRepository;

    private final ProfileMapper profileMapper;
    private final GuestDocumentMapper guestDocumentMapper;

    public ProfileResponse getById(Long id) {
        ProfileEntity profile = profileRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Profile with id: " + id + " not found"
                        )
                );

        return profileMapper.toResponse(profile);
    }

    public List<ProfileResponse> getAll() {
        return profileRepository.findAll()
                .stream()
                .map(profileMapper::toResponse)
                .toList();
    }

    public ProfileResponse create(ProfileRequest request) {

        if (request.phoneNumber() != null
                && profileRepository.existsByPhoneNumber(request.phoneNumber())) {

            throw new IllegalArgumentException(
                    "Profile with phone number: "
                            + request.phoneNumber()
                            + " already exists"
            );
        }

        if (request.email() != null
                && profileRepository.existsByEmail(request.email())) {

            throw new IllegalArgumentException(
                    "Profile with email: "
                            + request.email()
                            + " already exists"
            );
        }

        ProfileEntity profile = profileMapper.toEntity(request);

        if (request.guestDocument() != null) {

            if (guestDocumentRepository.existsByDocumentNumber(
                    request.guestDocument().documentNumber())) {

                throw new IllegalArgumentException(
                        "Guest document with number: "
                                + request.guestDocument().documentNumber()
                                + " already exists"
                );
            }

            GuestDocumentEntity document =
                    guestDocumentMapper.toEntity(request.guestDocument());

            profile.setGuestDocument(document);
        }

        ProfileEntity savedProfile =
                profileRepository.save(profile);

        return profileMapper.toResponse(savedProfile);
    }

    public ProfileResponse update(Long id, ProfileRequest request) {

        ProfileEntity profile = profileRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Profile with id: " + id + " not found"
                        )
                );

        if (request.phoneNumber() != null
                && !request.phoneNumber().equals(profile.getPhoneNumber())
                && profileRepository.existsByPhoneNumber(request.phoneNumber())) {

            throw new IllegalArgumentException(
                    "Profile with phone number: "
                            + request.phoneNumber()
                            + " already exists"
            );
        }

        if (request.email() != null
                && !request.email().equals(profile.getEmail())
                && profileRepository.existsByEmail(request.email())) {

            throw new IllegalArgumentException(
                    "Profile with email: "
                            + request.email()
                            + " already exists"
            );
        }

        profileMapper.updateEntity(request, profile);

        if (request.guestDocument() != null) {

            if (profile.getGuestDocument() == null) {

                if (guestDocumentRepository.existsByDocumentNumber(
                        request.guestDocument().documentNumber())) {

                    throw new IllegalArgumentException(
                            "Guest document with number: "
                                    + request.guestDocument().documentNumber()
                                    + " already exists"
                    );
                }

                GuestDocumentEntity document =
                        guestDocumentMapper.toEntity(request.guestDocument());

                profile.setGuestDocument(document);

            } else {

                GuestDocumentEntity document =
                        profile.getGuestDocument();

                if (!document.getDocumentNumber()
                        .equals(request.guestDocument().documentNumber())
                        && guestDocumentRepository.existsByDocumentNumber(
                        request.guestDocument().documentNumber())) {

                    throw new IllegalArgumentException(
                            "Guest document with number: "
                                    + request.guestDocument().documentNumber()
                                    + " already exists"
                    );
                }

                guestDocumentMapper.updateEntity(
                        request.guestDocument(),
                        document
                );
            }
        }

        ProfileEntity updatedProfile =
                profileRepository.save(profile);

        return profileMapper.toResponse(updatedProfile);
    }

    public void delete(Long id) {

        ProfileEntity profile = profileRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Profile with id: " + id + " not found"
                        )
                );

        profileRepository.delete(profile);
    }
}