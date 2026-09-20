package com.sirius.sirius.service;

import com.sirius.sirius.dto.RoomRequest;
import com.sirius.sirius.dto.RoomResponse;
import com.sirius.sirius.exeption.BadRequestException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.mappers.RoomMapper;
import com.sirius.sirius.store.entity.hotel.HotelEntity;
import com.sirius.sirius.store.entity.hotel.RoomCategoryEntity;
import com.sirius.sirius.store.entity.hotel.RoomEntity;
import com.sirius.sirius.store.enums.RoomStatus;
import com.sirius.sirius.store.repository.HotelRepository;
import com.sirius.sirius.store.repository.RoomCategoryRepository;
import com.sirius.sirius.store.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final RoomCategoryRepository roomCategoryRepository;
    private final RoomMapper roomMapper;

    public RoomResponse getById(Long id) {
        RoomEntity room = roomRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Room with id: " + id + " not found"
                        )
                );

        return roomMapper.toResponse(room);
    }

    public List<RoomResponse> getAll() {
        return roomRepository.findAll()
                .stream()
                .map(roomMapper::toResponse)
                .toList();
    }

    public List<RoomResponse> getAllByHotelId(Long hotelId) {

        if (!hotelRepository.existsById(hotelId)) {
            throw new NotFoundException(
                    "Hotel with id: " + hotelId + " not found"
            );
        }

        return roomRepository.findAllByHotelId(hotelId)
                .stream()
                .map(roomMapper::toResponse)
                .toList();
    }

    public List<RoomResponse> getAllByCategoryId(Long categoryId) {

        if (!roomCategoryRepository.existsById(categoryId)) {
            throw new NotFoundException(
                    "Room category with id: " + categoryId + " not found"
            );
        }

        return roomRepository.findAllByCategoryId(categoryId)
                .stream()
                .map(roomMapper::toResponse)
                .toList();
    }

    public List<RoomResponse> getAllByStatus(
            Long hotelId,
            RoomStatus status
    ) {

        if (!hotelRepository.existsById(hotelId)) {
            throw new NotFoundException(
                    "Hotel with id: " + hotelId + " not found"
            );
        }

        return roomRepository.findAllByHotelIdAndStatus(
                        hotelId,
                        status
                )
                .stream()
                .map(roomMapper::toResponse)
                .toList();
    }

    @Transactional
    public RoomResponse create(RoomRequest request) {

        HotelEntity hotel = hotelRepository.findById(request.hotelId())
                .orElseThrow(() ->
                        new NotFoundException(
                                "Hotel with id: " + request.hotelId()
                                        + " not found"
                        )
                );

        RoomCategoryEntity category =
                roomCategoryRepository.findById(request.categoryId())
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Room category with id: "
                                                + request.categoryId()
                                                + " not found"
                                )
                        );

        if (roomRepository.existsByRoomNumberAndHotelId(
                request.roomNumber(),
                request.hotelId()
        )) {
            throw new BadRequestException(
                    "Room with number: " + request.roomNumber()
                            + " already exists in this hotel"
            );
        }

        RoomEntity room = roomMapper.toEntity(request);

        room.setHotel(hotel);
        room.setCategory(category);

        RoomEntity savedRoom = roomRepository.save(room);

        return roomMapper.toResponse(savedRoom);
    }

    @Transactional
    public RoomResponse update(
            Long id,
            RoomRequest request
    ) {

        RoomEntity room = roomRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Room with id: " + id + " not found"
                        )
                );

        HotelEntity hotel = hotelRepository.findById(request.hotelId())
                .orElseThrow(() ->
                        new NotFoundException(
                                "Hotel with id: " + request.hotelId()
                                        + " not found"
                        )
                );

        RoomCategoryEntity category =
                roomCategoryRepository.findById(request.categoryId())
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Room category with id: "
                                                + request.categoryId()
                                                + " not found"
                                )
                        );

        if ((!room.getRoomNumber().equals(request.roomNumber())
                || !room.getHotel().getId().equals(request.hotelId()))
                && roomRepository.existsByRoomNumberAndHotelId(
                request.roomNumber(),
                request.hotelId()
        )) {
            throw new BadRequestException(
                    "Room with number: " + request.roomNumber()
                            + " already exists in this hotel"
            );
        }

        roomMapper.updateEntity(request, room);

        room.setHotel(hotel);
        room.setCategory(category);

        RoomEntity updatedRoom = roomRepository.save(room);

        return roomMapper.toResponse(updatedRoom);
    }

    @Transactional
    public void delete(Long id) {

        RoomEntity room = roomRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Room with id: " + id + " not found"
                        )
                );

        roomRepository.delete(room);
    }
}