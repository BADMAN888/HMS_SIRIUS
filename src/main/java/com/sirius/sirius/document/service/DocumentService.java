package com.sirius.sirius.document.service;

import com.sirius.sirius.document.dto.DocumentData;
import com.sirius.sirius.document.dto.GeneratedDocument;
import com.sirius.sirius.document.generator.DocumentGenerator;
import com.sirius.sirius.document.model.DocumentFormat;
import com.sirius.sirius.document.model.DocumentType;
import com.sirius.sirius.dto.ProfileResponse;
import com.sirius.sirius.dto.ReservationResponse;
import com.sirius.sirius.dto.HotelResponse;
import com.sirius.sirius.dto.RoomCategoryResponse;
import com.sirius.sirius.exeption.ValidationException;
import com.sirius.sirius.service.hotel.HotelService;
import com.sirius.sirius.service.hotel.RoomCategoryService;
import com.sirius.sirius.service.reservation.ReservationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DocumentService {
    private static final String DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private final ReservationService reservationService;
    private final HotelService hotelService;
    private final RoomCategoryService roomCategoryService;
    private final Map<DocumentFormat, DocumentGenerator> generators;

    public DocumentService(ReservationService reservationService, HotelService hotelService,
                           RoomCategoryService roomCategoryService,
                           List<DocumentGenerator> generators) {
        this.reservationService = reservationService;
        this.hotelService = hotelService;
        this.roomCategoryService = roomCategoryService;
        this.generators = generators.stream().collect(Collectors.toMap(
                DocumentGenerator::format,
                generator -> generator,
                (first, duplicate) -> { throw new IllegalStateException("Duplicate document generator"); },
                () -> new EnumMap<>(DocumentFormat.class)
        ));
    }

    @Transactional(readOnly = true)
    public GeneratedDocument generateRegistrationCard(Long reservationId, DocumentFormat format) {
        return generateReservationDocument(reservationId, format, DocumentType.REGISTRATION_CARD,
                "registration-card");
    }

    @Transactional(readOnly = true)
    public GeneratedDocument generateBookingConfirmation(Long reservationId, DocumentFormat format) {
        return generateReservationDocument(reservationId, format, DocumentType.BOOKING_CONFIRMATION,
                "booking-confirmation");
    }

    private GeneratedDocument generateReservationDocument(Long reservationId, DocumentFormat format,
                                                            DocumentType type, String filePrefix) {
        ReservationResponse reservation = reservationService.getById(reservationId);
        ProfileResponse guest = reservation.primaryGuest();
        if (guest == null) {
            throw new ValidationException("Для бронювання не вказано основного гостя");
        }
        String fullName = Stream.of(guest.lastName(), guest.firstName(), guest.middleName())
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" "));
        if (fullName.isBlank()) {
            throw new ValidationException("ПІБ основного гостя не заповнено");
        }
        HotelResponse hotel = reservation.room() == null || reservation.room().hotelId() == null
                ? null : hotelService.getHotelById(reservation.room().hotelId());
        RoomCategoryResponse category = reservation.room() == null || reservation.room().categoryId() == null
                ? null : roomCategoryService.getById(reservation.room().categoryId());
        DocumentData data = new DocumentData(
                hotel == null ? null : hotel.hotelName(),
                hotel == null ? null : hotel.address(),
                hotel == null ? null : hotel.phone(),
                hotel == null ? null : hotel.email(),
                fullName,
                guest.dateOfBirth() == null ? null : guest.dateOfBirth().format(DATE_FORMAT),
                guest.phoneNumber(),
                guest.email(),
                guest.nationality() == null ? null : guest.nationality().name().replace('_', ' '),
                firstNonBlank(guest.registrationAddress(), guest.address()),
                reservation.confirmationNumber(),
                reservation.checkIn() == null ? null : reservation.checkIn().toLocalDate().format(DATE_FORMAT),
                reservation.checkIn() == null ? null : reservation.checkIn().toLocalTime().format(TIME_FORMAT),
                reservation.checkOut() == null ? null : reservation.checkOut().toLocalDate().format(DATE_FORMAT),
                reservation.checkOut() == null ? null : reservation.checkOut().toLocalTime().format(TIME_FORMAT),
                reservation.room() == null ? null : reservation.room().roomNumber(),
                category == null ? null : category.name(),
                reservation.rate() == null ? null : reservation.rate().name(),
                guestCount(reservation.adults(), reservation.children()),
                reservation.status() == null ? null : statusLabel(reservation.status().name()));
        DocumentGenerator generator = generators.get(format);
        if (generator == null) throw new ValidationException("Формат документа не підтримується: " + format);
        String extension = format.name().toLowerCase();
        String contentType = switch (format) {
            case PDF -> "application/pdf";
            case DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        };
        return new GeneratedDocument(generator.generate(type, data),
                contentType, filePrefix + "-" + reservationId + "." + extension);
    }

    private String guestCount(Integer adults, Integer children) {
        int adultCount = adults == null ? 0 : adults;
        int childCount = children == null ? 0 : children;
        String adultLabel = adultCount == 1 ? "дорослий / adult" : "дорослих / adults";
        String guestCount = adultCount + " " + adultLabel;
        if (childCount > 0) {
            String childLabel = childCount == 1 ? "дитина / child" : "дітей / children";
            guestCount += ", " + childCount + " " + childLabel;
        }
        return guestCount;
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "RESERVED" -> "Підтверджено / Confirmed";
            case "IN_HOUSE" -> "Гість проживає / In house";
            case "COMPLETED" -> "Завершено / Completed";
            case "CANCELLED" -> "Скасовано / Cancelled";
            case "NO_SHOW" -> "Не заїхав / No-show";
            default -> status;
        };
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    @Transactional(readOnly = true)
    public GeneratedDocument generateRegistrationCardPreview(Long reservationId) {
        return generatePreview(reservationId, DocumentType.REGISTRATION_CARD);
    }

    @Transactional(readOnly = true)
    public GeneratedDocument generateBookingConfirmationPreview(Long reservationId) {
        return generatePreview(reservationId, DocumentType.BOOKING_CONFIRMATION);
    }

    private GeneratedDocument generatePreview(Long reservationId, DocumentType type) {
        GeneratedDocument pdf = type == DocumentType.REGISTRATION_CARD
                ? generateRegistrationCard(reservationId, DocumentFormat.PDF)
                : generateBookingConfirmation(reservationId, DocumentFormat.PDF);
        try (var document = Loader.loadPDF(pdf.content());
             ByteArrayOutputStream image = new ByteArrayOutputStream()) {
            var page = new PDFRenderer(document).renderImageWithDPI(0, 180, ImageType.RGB);
            ImageIO.write(page, "png", image);
            String filePrefix = type == DocumentType.REGISTRATION_CARD ? "registration-card" : "booking-confirmation";
            return new GeneratedDocument(image.toByteArray(), "image/png",
                    filePrefix + "-" + reservationId + "-preview.png");
        } catch (Exception exception) {
            throw new IllegalStateException("Could not render registration card preview", exception);
        }
    }
}
