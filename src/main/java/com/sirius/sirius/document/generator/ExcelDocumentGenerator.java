package com.sirius.sirius.document.generator;

import com.sirius.sirius.document.dto.DocumentData;
import com.sirius.sirius.document.model.DocumentFormat;
import com.sirius.sirius.document.model.DocumentType;
import com.sirius.sirius.exeption.ValidationException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

@Component
public class ExcelDocumentGenerator implements DocumentGenerator {
    private final ResourceLoader resourceLoader;

    public ExcelDocumentGenerator(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public DocumentFormat format() {
        return DocumentFormat.XLSX;
    }

    @Override
    public byte[] generate(DocumentType type, DocumentData data) {
        if (type != DocumentType.REGISTRATION_CARD && type != DocumentType.BOOKING_CONFIRMATION) {
            throw new ValidationException("Unsupported Excel document type: " + type);
        }
        String template = type == DocumentType.REGISTRATION_CARD
                ? "registration-card.xlsx" : "booking-confirmation.xlsx";
        Resource resource = resourceLoader.getResource("classpath:templates/" + template);
        if (!resource.exists()) throw new IllegalStateException("Registration card Excel template was not found");
        Map<String, String> values = Map.ofEntries(
                Map.entry("{{hotelName}}", valueOrBlank(data.hotelName())),
                Map.entry("{{hotelAddress}}", valueOrBlank(data.hotelAddress())),
                Map.entry("{{hotelPhone}}", valueOrBlank(data.hotelPhone())),
                Map.entry("{{hotelEmail}}", valueOrBlank(data.hotelEmail())),
                Map.entry("{{primaryGuestFullName}}", valueOrBlank(data.primaryGuestFullName())),
                Map.entry("{{dateOfBirth}}", valueOrBlank(data.dateOfBirth())),
                Map.entry("{{phoneNumber}}", valueOrBlank(data.phoneNumber())),
                Map.entry("{{email}}", valueOrBlank(data.email())),
                Map.entry("{{nationality}}", valueOrBlank(data.nationality())),
                Map.entry("{{address}}", valueOrBlank(data.address())),
                Map.entry("{{confirmationNumber}}", valueOrBlank(data.reservationNumber())),
                Map.entry("{{checkIn}}", valueOrBlank(data.checkInDate())),
                Map.entry("{{checkInTime}}", valueOrBlank(data.checkInTime())),
                Map.entry("{{checkOut}}", valueOrBlank(data.checkOutDate())),
                Map.entry("{{checkOutTime}}", valueOrBlank(data.checkOutTime())),
                Map.entry("{{roomNumber}}", valueOrBlank(data.roomNumber())),
                Map.entry("{{roomCategory}}", valueOrBlank(data.roomCategory())),
                Map.entry("{{rateName}}", valueOrBlank(data.rateName())),
                Map.entry("{{guestCount}}", valueOrBlank(data.guestCount())),
                Map.entry("{{reservationStatus}}", valueOrBlank(data.reservationStatus()))
        );
        try (InputStream input = resource.getInputStream();
             Workbook workbook = WorkbookFactory.create(input);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (var sheet : workbook) {
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        if (cell.getCellType() != CellType.STRING) continue;
                        String value = cell.getStringCellValue();
                        for (var entry : values.entrySet()) value = value.replace(entry.getKey(), entry.getValue());
                        // openpyxl templates store text as inline strings. Clear the
                        // inline payload before writing a shared string with Apache POI.
                        cell.setCellType(CellType.BLANK);
                        cell.setCellValue(value);
                    }
                }
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not generate registration card Excel file", exception);
        }
    }

    private String valueOrBlank(String value) {
        return value == null ? "" : value;
    }
}
