package com.sirius.sirius.document.generator;

import com.deepoove.poi.XWPFTemplate;
import com.sirius.sirius.document.dto.DocumentData;
import com.sirius.sirius.document.model.DocumentFormat;
import com.sirius.sirius.document.model.DocumentType;
import com.sirius.sirius.exeption.ValidationException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

@Component
public class WordDocumentGenerator implements DocumentGenerator {
    private final ResourceLoader resourceLoader;

    public WordDocumentGenerator(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public DocumentFormat format() {
        return DocumentFormat.DOCX;
    }

    @Override
    public byte[] generate(DocumentType type, DocumentData data) {
        if (type != DocumentType.REGISTRATION_CARD && type != DocumentType.BOOKING_CONFIRMATION) {
            throw new ValidationException("Unsupported Word document type: " + type);
        }
        String templateName = type == DocumentType.REGISTRATION_CARD
                ? "registration-card.docx" : "booking-confirmation.docx";
        Resource resource = resourceLoader.getResource("classpath:templates/" + templateName);
        if (!resource.exists()) {
            throw new IllegalStateException("Registration card template was not found");
        }
        Map<String, Object> variables = Map.ofEntries(
                Map.entry("hotelName", valueOrBlank(data.hotelName())),
                Map.entry("hotelAddress", valueOrBlank(data.hotelAddress())),
                Map.entry("hotelPhone", valueOrBlank(data.hotelPhone())),
                Map.entry("hotelEmail", valueOrBlank(data.hotelEmail())),
                Map.entry("primaryGuestFullName", valueOrBlank(data.primaryGuestFullName())),
                Map.entry("dateOfBirth", valueOrBlank(data.dateOfBirth())),
                Map.entry("phoneNumber", valueOrBlank(data.phoneNumber())),
                Map.entry("email", valueOrBlank(data.email())),
                Map.entry("nationality", valueOrBlank(data.nationality())),
                Map.entry("address", valueOrBlank(data.address())),
                Map.entry("confirmationNumber", valueOrBlank(data.reservationNumber())),
                Map.entry("roomNumber", valueOrBlank(data.roomNumber())),
                Map.entry("checkIn", valueOrBlank(data.checkInDate())),
                Map.entry("checkInTime", valueOrBlank(data.checkInTime())),
                Map.entry("checkOut", valueOrBlank(data.checkOutDate())),
                Map.entry("checkOutTime", valueOrBlank(data.checkOutTime())),
                Map.entry("roomCategory", valueOrBlank(data.roomCategory())),
                Map.entry("rateName", valueOrBlank(data.rateName())),
                Map.entry("guestCount", valueOrBlank(data.guestCount())),
                Map.entry("reservationStatus", valueOrBlank(data.reservationStatus()))
        );
        try (InputStream input = resource.getInputStream();
             XWPFTemplate template = XWPFTemplate.compile(input).render(variables);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            template.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not generate registration card", exception);
        }
    }

    private String valueOrBlank(String value) {
        return value == null ? "" : value;
    }
}
