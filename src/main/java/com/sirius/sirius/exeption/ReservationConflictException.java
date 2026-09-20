package com.sirius.sirius.exeption;

public class ReservationConflictException extends RuntimeException {

    public ReservationConflictException() {
        super("Room is already reserved for selected dates");
    }
}