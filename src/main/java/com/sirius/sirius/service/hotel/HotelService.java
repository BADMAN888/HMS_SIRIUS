package com.sirius.sirius.service.hotel;

import com.sirius.sirius.dto.HotelRequest;
import com.sirius.sirius.dto.HotelResponse;
import com.sirius.sirius.exeption.ConflictException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.mappers.HotelMapper;
import com.sirius.sirius.store.entity.hotel.HotelEntity;
import com.sirius.sirius.store.entity.biling.LegalEntityEntity;
import com.sirius.sirius.store.repository.HotelRepository;
import com.sirius.sirius.store.repository.LegalEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotelRepository;
    private final LegalEntityRepository legalEntityRepository;
    private final HotelMapper hotelMapper;

    @Transactional(readOnly = true)
    public HotelResponse getHotelByName(String name) {
        HotelEntity hotel = hotelRepository.findByHotelName(name)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Hotel with name: " + name + " not found"
                        )
                );

        return hotelMapper.toResponse(hotel);
    }

    @Transactional(readOnly = true)
    public HotelResponse getHotelById(Long id) {
        HotelEntity hotel = hotelRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Hotel with id: " + id + " not found"
                        )
                );

        return hotelMapper.toResponse(hotel);
    }

    @Transactional
    public HotelResponse setLegalEntities(Long hotelId, List<Long> legalEntityIds) {
        HotelEntity hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new NotFoundException("Hotel not found with id: " + hotelId));
        List<Long> uniqueIds = legalEntityIds == null
                ? List.of()
                : legalEntityIds.stream().distinct().toList();
        List<LegalEntityEntity> legalEntities = legalEntityRepository.findAllById(uniqueIds);
        if (legalEntities.size() != uniqueIds.size()) {
            throw new NotFoundException("One or more legal entities were not found");
        }
        hotel.setLegalEntities(new HashSet<>(legalEntities));
        return hotelMapper.toResponse(hotelRepository.save(hotel));
    }

    @Transactional
    public HotelResponse createHotel(HotelRequest request) {
        if (hotelRepository.existsByHotelName(request.hotelName())) {
            throw new ConflictException(
                    "Hotel with name: " + request.hotelName() + " already exists"
            );
        }

        HotelEntity hotel = hotelMapper.toEntity(request);
        HotelEntity savedHotel = hotelRepository.save(hotel);

        return hotelMapper.toResponse(savedHotel);
    }

    @Transactional
    public HotelResponse updateHotel(Long id, HotelRequest request) {
        HotelEntity hotel = hotelRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Hotel not found with id: " + id
                        )
                );

        if (!hotel.getHotelName().equals(request.hotelName())
                && hotelRepository.existsByHotelName(request.hotelName())) {
            throw new ConflictException(
                    "Hotel with name: " + request.hotelName() + " already exists"
            );
        }

        hotelMapper.updateEntity(request, hotel);

        HotelEntity updatedHotel = hotelRepository.save(hotel);

        return hotelMapper.toResponse(updatedHotel);
    }

    @Transactional
    public void deleteHotel(Long id) {
        HotelEntity hotel = hotelRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Hotel not found with id: " + id
                        )
                );

        hotelRepository.delete(hotel);
    }
}
