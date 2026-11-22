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
        if (type != DocumentType.REGISTRATION_CARD) {
            throw new ValidationException("Unsupported Excel document type: " + type);
        }
        Resource resource = resourceLoader.getResource("classpath:templates/registration-card.xlsx");
        if (!resource.exists()) throw new IllegalStateException("Registration card Excel template was not found");
        Map<String, String> values = Map.of(
                "{{primaryGuestFullName}}", data.primaryGuestFullName(),
                "{{reservationNumber}}", valueOrBlank(data.reservationNumber()),
                "{{checkInDate}}", valueOrBlank(data.checkInDate()),
                "{{checkOutDate}}", valueOrBlank(data.checkOutDate()),
                "{{roomNumber}}", valueOrBlank(data.roomNumber())
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
