package com.sirius.sirius.exeption;

public class BadRequestException extends ValidationException {

    public BadRequestException(String message) {
        super(message);
    }
}