package com.sirius.sirius.exeption;

public class ForbiddenException extends RuntimeException {

  public ForbiddenException(String message) {
    super(message);
  }
}