package com.sirius.sirius.document.service;

import com.sirius.sirius.document.dto.DocumentData;
import com.sirius.sirius.document.dto.GeneratedDocument;
import com.sirius.sirius.document.generator.DocumentGenerator;
import com.sirius.sirius.document.model.DocumentFormat;
import com.sirius.sirius.document.model.DocumentType;
import com.sirius.sirius.dto.ProfileResponse;
import com.sirius.sirius.dto.ReservationResponse;
import com.sirius.sirius.exeption.ValidationException;
import com.sirius.sirius.service.reservation.ReservationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
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
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private final ReservationService reservationService;
    private final Map<DocumentFormat, DocumentGenerator> generators;

    public DocumentService(ReservationService reservationService, List<DocumentGenerator> generators) {
        this.reservationService = reservationService;
        this.generators = generators.stream().collect(Collectors.toMap(
                DocumentGenerator::format,
                generator -> generator,
                (first, duplicate) -> { throw new IllegalStateException("Duplicate document generator"); },
                () -> new EnumMap<>(DocumentFormat.class)
        ));
    }

    @Transactional(readOnly = true)
    public GeneratedDocument generateRegistrationCard(Long reservationId, DocumentFormat format) {
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
        DocumentData data = new DocumentData(fullName, reservation.confirmationNumber(),
                reservation.checkIn() == null ? null : reservation.checkIn().format(DATE_FORMAT),
                reservation.checkOut() == null ? null : reservation.checkOut().format(DATE_FORMAT),
                reservation.room() == null ? null : reservation.room().roomNumber());
        DocumentGenerator generator = generators.get(format);
        if (generator == null) throw new ValidationException("Формат документа не підтримується: " + format);
        String extension = format.name().toLowerCase();
        String contentType = switch (format) {
            case PDF -> "application/pdf";
            case DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        };
        return new GeneratedDocument(generator.generate(DocumentType.REGISTRATION_CARD, data),
                contentType, "registration-card-" + reservationId + "." + extension);
    }

    @Transactional(readOnly = true)
    public GeneratedDocument generateRegistrationCardPreview(Long reservationId) {
        GeneratedDocument pdf = generateRegistrationCard(reservationId, DocumentFormat.PDF);
        try (var document = Loader.loadPDF(pdf.content());
             ByteArrayOutputStream image = new ByteArrayOutputStream()) {
            var page = new PDFRenderer(document).renderImageWithDPI(0, 180, ImageType.RGB);
            ImageIO.write(page, "png", image);
            return new GeneratedDocument(image.toByteArray(), "image/png",
                    "registration-card-" + reservationId + "-preview.png");
        } catch (Exception exception) {
            throw new IllegalStateException("Could not render registration card preview", exception);
        }
    }
}
