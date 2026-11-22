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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DocumentService {
    private static final String DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int DOCUMENT_CACHE_LIMIT = 24;
    private static final int PREVIEW_CACHE_LIMIT = 8;
    private final ReservationService reservationService;
    private final HotelService hotelService;
    private final RoomCategoryService roomCategoryService;
    private final Map<DocumentFormat, DocumentGenerator> generators;
    private final Map<DocumentCacheKey, GeneratedDocument> documentCache = new ConcurrentHashMap<>();
    private final Map<PreviewCacheKey, GeneratedDocument> previewCache = new ConcurrentHashMap<>();

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
        DocumentData data = loadDocumentData(reservationId);
        DocumentCacheKey key = new DocumentCacheKey(reservationId, type, format, data);
        return cached(documentCache, key, DOCUMENT_CACHE_LIMIT,
                () -> createDocument(reservationId, format, type, filePrefix, data));
    }

    private GeneratedDocument createDocument(Long reservationId, DocumentFormat format,
                                              DocumentType type, String filePrefix, DocumentData data) {
        DocumentGenerator generator = generators.get(format);
        if (generator == null) throw new ValidationException("Формат документа не підтримується: " + format);
        String extension = format.name().toLowerCase();
        String contentType = switch (format) {
            case PDF -> "application/pdf";
            case DOCX -> DOCX_MIME;
            case XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        };
        return new GeneratedDocument(generator.generate(type, data), contentType,
                filePrefix + "-" + reservationId + "." + extension);
    }

    private DocumentData loadDocumentData(Long reservationId) {
        ReservationResponse reservation = reservationService.getById(reservationId);
        ProfileResponse guest = reservation.primaryGuest();
        String fullName = guest == null
                ? reservation.guestName()
                : Stream.of(guest.lastName(), guest.firstName(), guest.middleName())
                        .filter(part -> part != null && !part.isBlank())
                        .map(String::trim)
                        .collect(Collectors.joining(" "));
        if (fullName == null || fullName.isBlank()) {
            throw new ValidationException("ПІБ основного гостя не заповнено");
        }
        HotelResponse hotel = reservation.room() == null || reservation.room().hotelId() == null
                ? null : hotelService.getHotelById(reservation.room().hotelId());
        RoomCategoryResponse category = reservation.room() == null || reservation.room().categoryId() == null
                ? null : roomCategoryService.getById(reservation.room().categoryId());
        return new DocumentData(
                hotel == null ? null : hotel.hotelName(),
                hotel == null ? null : hotel.address(),
                hotel == null ? null : hotel.phone(),
                hotel == null ? null : hotel.email(),
                fullName,
                guest == null || guest.dateOfBirth() == null ? null : guest.dateOfBirth().format(DATE_FORMAT),
                guest == null ? null : guest.phoneNumber(),
                guest == null ? null : guest.email(),
                guest == null || guest.nationality() == null ? null : guest.nationality().name().replace('_', ' '),
                guest == null ? null : firstNonBlank(guest.registrationAddress(), guest.address()),
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
        String filePrefix = type == DocumentType.REGISTRATION_CARD ? "registration-card" : "booking-confirmation";
        DocumentData data = loadDocumentData(reservationId);
        DocumentCacheKey pdfKey = new DocumentCacheKey(reservationId, type, DocumentFormat.PDF, data);
        GeneratedDocument pdf = cached(documentCache, pdfKey, DOCUMENT_CACHE_LIMIT,
                () -> createDocument(reservationId, DocumentFormat.PDF, type, filePrefix, data));
        PreviewCacheKey previewKey = new PreviewCacheKey(reservationId, type, data);
        return cached(previewCache, previewKey, PREVIEW_CACHE_LIMIT,
                () -> renderPreview(pdf, filePrefix, reservationId));
    }

    private GeneratedDocument renderPreview(GeneratedDocument pdf, String filePrefix, Long reservationId) {
        try (var document = Loader.loadPDF(pdf.content());
             ByteArrayOutputStream image = new ByteArrayOutputStream()) {
            var page = new PDFRenderer(document).renderImageWithDPI(0, 144, ImageType.RGB);
            ImageIO.write(page, "png", image);
            return new GeneratedDocument(image.toByteArray(), "image/png",
                    filePrefix + "-" + reservationId + "-preview.png");
        } catch (Exception exception) {
            throw new IllegalStateException("Could not render reservation document preview", exception);
        }
    }

    private <K> GeneratedDocument cached(Map<K, GeneratedDocument> cache, K key, int limit,
                                          Supplier<GeneratedDocument> factory) {
        GeneratedDocument result = cache.computeIfAbsent(key, ignored -> factory.get());
        if (cache.size() > limit) {
            for (K staleKey : cache.keySet()) {
                if (!staleKey.equals(key) && cache.remove(staleKey) != null) break;
            }
        }
        return result;
    }

    private record DocumentCacheKey(Long reservationId, DocumentType type,
                                    DocumentFormat format, DocumentData data) {}

    private record PreviewCacheKey(Long reservationId, DocumentType type, DocumentData data) {}
}
