package com.sirius.sirius.service.profile;

import com.sirius.sirius.dto.GuestDocumentRequest;
import com.sirius.sirius.dto.GuestDocumentResponse;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.mappers.GuestDocumentMapper;
import com.sirius.sirius.store.entity.reservation.GuestDocumentEntity;
import com.sirius.sirius.store.repository.GuestDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GuestDocumentService {

    private final GuestDocumentRepository guestDocumentRepository;
    private final GuestDocumentMapper guestDocumentMapper;

    public GuestDocumentResponse getById(Long id) {
        GuestDocumentEntity document = guestDocumentRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Guest document with id: " + id + " not found")
                );

        return guestDocumentMapper.toResponse(document);
    }

    public List<GuestDocumentResponse> getAll() {
        return guestDocumentRepository.findAll()
                .stream()
                .map(guestDocumentMapper::toResponse)
                .toList();
    }

    public GuestDocumentResponse create(GuestDocumentRequest request) {

        if (guestDocumentRepository.existsByDocumentNumber(request.documentNumber())) {
            throw new IllegalArgumentException(
                    "Guest document with number: "
                            + request.documentNumber()
                            + " already exists"
            );
        }

        GuestDocumentEntity document =
                guestDocumentMapper.toEntity(request);

        GuestDocumentEntity savedDocument =
                guestDocumentRepository.save(document);

        return guestDocumentMapper.toResponse(savedDocument);
    }

    public GuestDocumentResponse update(Long id, GuestDocumentRequest request) {

        GuestDocumentEntity document = guestDocumentRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Guest document with id: " + id + " not found")
                );

        if (!document.getDocumentNumber().equals(request.documentNumber())
                && guestDocumentRepository.existsByDocumentNumber(request.documentNumber())) {

            throw new IllegalArgumentException(
                    "Guest document with number: "
                            + request.documentNumber()
                            + " already exists"
            );
        }

        guestDocumentMapper.updateEntity(request, document);

        GuestDocumentEntity updatedDocument =
                guestDocumentRepository.save(document);

        return guestDocumentMapper.toResponse(updatedDocument);
    }

    public void delete(Long id) {

        GuestDocumentEntity document = guestDocumentRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Guest document with id: " + id + " not found")
                );

        guestDocumentRepository.delete(document);
    }
}