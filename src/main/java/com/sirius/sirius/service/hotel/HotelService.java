package com.sirius.sirius.service.hotel;

import com.sirius.sirius.dto.HotelRequest;
import com.sirius.sirius.dto.HotelResponse;
import com.sirius.sirius.exeption.BadRequestException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.mappers.HotelMapper;
import com.sirius.sirius.store.entity.hotel.HotelEntity;
import com.sirius.sirius.store.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HotelService {
    private final HotelRepository hotelRepository;
    private final HotelMapper hotelMapper;

    public HotelResponse getHotelByName(String name) {
        HotelEntity hotel = hotelRepository.findByHotelName(name).orElseThrow(() ->
                new NotFoundException("Hotel with name: " + name + " not found")
        );
        return hotelMapper.toResponse(hotel);
    }

    public HotelResponse getHotelById(Long id) {
        HotelEntity hotel = hotelRepository.findById(id).orElseThrow(() ->
                new NotFoundException("Hotel with id: " + id + " not found")
        );
        return hotelMapper.toResponse(hotel);
    }

    @Transactional
    public HotelResponse createHotel(HotelRequest request) {
        if (hotelRepository.existsByHotelName(request.hotelName())) {
            throw new BadRequestException("Hotel with name: " + request.hotelName() + " already exists");
        }
        HotelEntity hotel = hotelMapper.toEntity(request);
        HotelEntity savedHotel = hotelRepository.save(hotel);

        return hotelMapper.toResponse(savedHotel);
    }

    @Transactional
    public HotelResponse updateHotel(Long id, HotelRequest request) {

        HotelEntity hotel = hotelRepository.findById(id)
                .orElseThrow(() ->
                        new BadRequestException("Hotel not found with id: " + id)
                );

        if (!hotel.getHotelName().equals(request.hotelName())
                && hotelRepository.existsByHotelName(request.hotelName())) {
            throw new NotFoundException(
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
                        new NotFoundException("Hotel not found with id: " + id)
                );

        hotelRepository.delete(hotel);
    }
}
