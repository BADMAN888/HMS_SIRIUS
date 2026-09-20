package com.sirius.sirius.exeption;

public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException(Long roomId) {
        super("Room with id " + roomId + " not found");
    }
}
