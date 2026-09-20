package com.sirius.sirius.exeption;

public class NoAvailableRoomsException extends RuntimeException {
    public NoAvailableRoomsException(String message) {
        super(message);
    }
}
