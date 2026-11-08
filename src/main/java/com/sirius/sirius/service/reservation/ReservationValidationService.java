
package com.sirius.sirius.service.reservation;

import com.sirius.sirius.exeption.BadRequestException;
import com.sirius.sirius.exeption.NotFoundException;
import com.sirius.sirius.store.entity.biling.CompanyEntity;
import com.sirius.sirius.store.entity.biling.RateEntity;
import com.sirius.sirius.store.entity.hotel.RoomCategoryEntity;
import com.sirius.sirius.store.entity.reservation.ReservationEntity;
import com.sirius.sirius.store.enums.ReservationStatus;
import com.sirius.sirius.store.repository.CompanyRepository;
import com.sirius.sirius.store.repository.RateRepository;
import com.sirius.sirius.store.repository.RoomCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationValidationService {

    private final RateRepository rateRepository;
    private final RoomCategoryRepository roomCategoryRepository;
    private final CompanyRepository companyRepository;

    public RateEntity findRate(Long rateId) {
        return rateRepository.findById(rateId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Rate with id: " + rateId + " not found"
                        )
                );
    }

    public RoomCategoryEntity findPaymentCategory(Long categoryId) {
        return roomCategoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Payment category with id: "
                                        + categoryId
                                        + " not found"
                        )
                );
    }

    public CompanyEntity findCompany(Long companyId) {
        if (companyId == null) {
            return null;
        }

        return companyRepository.findById(companyId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Company with id: " + companyId + " not found"
                        )
                );
    }

    public void validatePaymentCategory(
            Long hotelId,
            RoomCategoryEntity paymentCategory
    ) {
        if (hotelId == null
                || paymentCategory == null
                || paymentCategory.getHotel() == null
                || paymentCategory.getHotel().getId() == null) {
            throw new BadRequestException(
                    "Room and payment category must belong to a hotel"
            );
        }

        if (!hotelId.equals(paymentCategory.getHotel().getId())) {
            throw new BadRequestException(
                    "Payment category must belong to the same hotel as the selected room"
            );
        }
    }

    public void validateUpdateStatus(ReservationEntity reservation) {
        if (reservation.getStatus() != ReservationStatus.RESERVED
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BadRequestException(
                    "Reservation cannot be updated in status: "
                            + reservation.getStatus()
            );
        }
    }
}
