package com.sirius.sirius.exeption;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            NotFoundException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            ValidationException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ConflictException ex,
            HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleDtoValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error(
                "Unexpected exception on {} {}",
                request.getMethod(),
                request.getRequestURI(),
                ex
        );

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected server error",
                request
        );
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }
    @ExceptionHandler(ReservationConflictException.class)
    public ResponseEntity<ErrorResponse> handleReservationConflict(
            ReservationConflictException ex,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request
        );
    }
}