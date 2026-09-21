package com.sirius.sirius.exeption;

public class NoAvailableRoomsException extends ConflictException {

    public NoAvailableRoomsException(String message) {
        super(message);
    }
}