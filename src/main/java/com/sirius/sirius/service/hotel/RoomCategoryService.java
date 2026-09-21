package com.sirius.sirius.service.hotel;

import com.sirius.sirius.dto.RoomCategoryRequest;
import com.sirius.sirius.dto.RoomCategoryResponse;
import com.sirius.sirius.exeption.BadRequestException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.mappers.RoomCategoryMapper;
import com.sirius.sirius.store.entity.hotel.HotelEntity;
import com.sirius.sirius.store.entity.hotel.RoomCategoryEntity;
import com.sirius.sirius.store.repository.HotelRepository;
import com.sirius.sirius.store.repository.RoomCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomCategoryService {

    private final RoomCategoryRepository roomCategoryRepository;
    private final HotelRepository hotelRepository;
    private final RoomCategoryMapper roomCategoryMapper;

    public RoomCategoryResponse getById(Long id) {
        RoomCategoryEntity category = roomCategoryRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Room category with id: " + id + " not found"
                        )
                );

        return roomCategoryMapper.toResponse(category);
    }

    public List<RoomCategoryResponse> getAll() {
        return roomCategoryRepository.findAll()
                .stream()
                .map(roomCategoryMapper::toResponse)
                .toList();
    }

    public List<RoomCategoryResponse> getAllByHotelId(Long hotelId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new NotFoundException(
                    "Hotel with id: " + hotelId + " not found"
            );
        }

        return roomCategoryRepository.findAllByHotelId(hotelId)
                .stream()
                .map(roomCategoryMapper::toResponse)
                .toList();
    }

    @Transactional
    public RoomCategoryResponse create(RoomCategoryRequest request) {

        HotelEntity hotel = hotelRepository.findById(request.hotelId())
                .orElseThrow(() ->
                        new NotFoundException(
                                "Hotel with id: " + request.hotelId() + " not found"
                        )
                );

        if (roomCategoryRepository.existsByNameAndHotelId(
                request.name(),
                request.hotelId()
        )) {
            throw new BadRequestException(
                    "Room category with name: " + request.name()
                            + " already exists in this hotel"
            );
        }

        RoomCategoryEntity category =
                roomCategoryMapper.toEntity(request);

        category.setHotel(hotel);

        RoomCategoryEntity savedCategory =
                roomCategoryRepository.save(category);

        return roomCategoryMapper.toResponse(savedCategory);
    }

    @Transactional
    public RoomCategoryResponse update(
            Long id,
            RoomCategoryRequest request
    ) {
        RoomCategoryEntity category = roomCategoryRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Room category with id: " + id + " not found"
                        )
                );

        HotelEntity hotel = hotelRepository.findById(request.hotelId())
                .orElseThrow(() ->
                        new NotFoundException(
                                "Hotel with id: " + request.hotelId() + " not found"
                        )
                );

        if ((!category.getName().equals(request.name())
                || !category.getHotel().getId().equals(request.hotelId()))
                && roomCategoryRepository.existsByNameAndHotelId(
                request.name(),
                request.hotelId()
        )) {
            throw new BadRequestException(
                    "Room category with name: " + request.name()
                            + " already exists in this hotel"
            );
        }

        category.setName(request.name());
        category.setDescription(request.description());
        category.setCountOfBeds(request.countOfBeds());
        category.setHotel(hotel);

        RoomCategoryEntity updatedCategory =
                roomCategoryRepository.save(category);

        return roomCategoryMapper.toResponse(updatedCategory);
    }

    @Transactional
    public void delete(Long id) {

        RoomCategoryEntity category = roomCategoryRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Room category with id: " + id + " not found"
                        )
                );

        roomCategoryRepository.delete(category);
    }
}