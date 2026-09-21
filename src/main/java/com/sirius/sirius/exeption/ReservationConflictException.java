package com.sirius.sirius.exeption;

public class ReservationConflictException extends ConflictException {

    public ReservationConflictException(String message) {
        super(message);
    }
}