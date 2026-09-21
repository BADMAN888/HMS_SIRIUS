package com.sirius.sirius.service.reservation;

import com.sirius.sirius.exeption.ReservationConflictException;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class ReservationConflictService {

    private static final String OVERLAP_CONSTRAINT =
            "reservations_room_dates_no_overlap";

    public <T> T saveAndFlush(Supplier<T> action) {
        try {
            return action.get();
        } catch (DataIntegrityViolationException ex) {
            handle(ex);
            throw ex;
        }
    }

    private void handle(DataIntegrityViolationException ex) {
        Throwable cause = ex;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintException
                    && OVERLAP_CONSTRAINT.equals(
                    constraintException.getConstraintName())) {

                throw new ReservationConflictException(
                        "Room is already reserved for the selected dates"
                );
            }

            cause = cause.getCause();
        }

        throw ex;
    }
}