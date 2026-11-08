package com.sirius.sirius.exeption;

@Deprecated
public class BadRequestException extends ValidationException {

    public BadRequestException(String message) {
        super(message);
    }
}