package com.sirius.sirius.service.billing;

import com.sirius.sirius.dto.FacilityRequest;
import com.sirius.sirius.dto.FacilityResponse;
import com.sirius.sirius.mappers.FacilityMapper;
import com.sirius.sirius.store.entity.biling.FacilityEntity;
import com.sirius.sirius.store.entity.hotel.HotelEntity;
import com.sirius.sirius.store.enums.TaxStatus;
import com.sirius.sirius.store.repository.FacilityRepository;
import com.sirius.sirius.store.repository.HotelRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final HotelRepository hotelRepository;
    private final FacilityMapper facilityMapper;

    @Transactional
    public FacilityResponse create(FacilityRequest request) {
        validateTaxStatus(request.taxStatus());

        HotelEntity hotel = getHotel(request.hotelId());

        FacilityEntity facility = facilityMapper.toEntity(request);
        facility.setHotel(hotel);

        return facilityMapper.toResponse(
                facilityRepository.save(facility)
        );
    }

    public FacilityResponse getById(Long id) {
        return facilityMapper.toResponse(getFacility(id));
    }

    public List<FacilityResponse> getAllByHotelId(Long hotelId) {
        ensureHotelExists(hotelId);

        return facilityRepository.findAllByHotelId(hotelId)
                .stream()
                .map(facilityMapper::toResponse)
                .toList();
    }

    public List<FacilityResponse> getActiveByHotelId(Long hotelId) {
        ensureHotelExists(hotelId);

        return facilityRepository.findAllByHotelIdAndActiveTrue(hotelId)
                .stream()
                .map(facilityMapper::toResponse)
                .toList();
    }

    @Transactional
    public FacilityResponse update(Long id, FacilityRequest request) {
        FacilityEntity facility = getFacility(id);

        if (!facility.getHotel().getId().equals(request.hotelId())) {
            throw new IllegalArgumentException(
                    "Не можна змінювати готель для вже створеної послуги"
            );
        }

        validateTaxStatus(request.taxStatus());

        facilityMapper.updateEntity(request, facility);

        return facilityMapper.toResponse(
                facilityRepository.save(facility)
        );
    }

    @Transactional
    public FacilityResponse deactivate(Long id) {
        FacilityEntity facility = getFacility(id);

        facility.setActive(false);

        return facilityMapper.toResponse(
                facilityRepository.save(facility)
        );
    }

    @Transactional
    public FacilityResponse activate(Long id) {
        FacilityEntity facility = getFacility(id);

        facility.setActive(true);

        return facilityMapper.toResponse(
                facilityRepository.save(facility)
        );
    }

    private void validateTaxStatus(TaxStatus taxStatus) {
        if (taxStatus == TaxStatus.EXEMPT) {
            throw new IllegalArgumentException(
                    "EXEMPT не може бути статусом послуги"
            );
        }
    }

    private FacilityEntity getFacility(Long id) {
        return facilityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Послугу з ID " + id + " не знайдено"
                ));
    }

    private HotelEntity getHotel(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Готель з ID " + id + " не знайдено"
                ));
    }

    private void ensureHotelExists(Long hotelId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new EntityNotFoundException(
                    "Готель з ID " + hotelId + " не знайдено"
            );
        }
    }
}