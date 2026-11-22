package com.sirius.sirius.document.generator;

import com.sirius.sirius.document.dto.DocumentData;
import com.sirius.sirius.document.model.DocumentFormat;
import com.sirius.sirius.document.model.DocumentType;
import com.sirius.sirius.exeption.ValidationException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

@Component
public class PdfDocumentGenerator implements DocumentGenerator {
    private final ResourceLoader resourceLoader;

    public PdfDocumentGenerator(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public DocumentFormat format() {
        return DocumentFormat.PDF;
    }

    @Override
    public byte[] generate(DocumentType type, DocumentData data) {
        if (type != DocumentType.REGISTRATION_CARD) {
            throw new ValidationException("Unsupported PDF document type: " + type);
        }
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            Resource fontResource = resourceLoader.getResource("classpath:fonts/registration-card-font.ttf");
            try (InputStream fontInput = fontResource.getInputStream();
                 PDPageContentStream content = new PDPageContentStream(document, page)) {
                PDType0Font font = PDType0Font.load(document, fontInput);
                float y = page.getMediaBox().getHeight() - 90;
                content.beginText();
                content.setFont(font, 18);
                content.newLineAtOffset(72, y);
                content.showText("РЕЄСТРАЦІЙНА КАРТА ГОСТЯ");
                content.setFont(font, 12);
                content.newLineAtOffset(0, -48);
                for (String line : List.of(
                        "ПІБ: " + data.primaryGuestFullName(),
                        "Номер бронювання: " + valueOrBlank(data.reservationNumber()),
                        "Дата заїзду: " + valueOrBlank(data.checkInDate()),
                        "Дата виїзду: " + valueOrBlank(data.checkOutDate()),
                        "Номер кімнати: " + valueOrBlank(data.roomNumber()))) {
                    content.showText(line);
                    content.newLineAtOffset(0, -28);
                }
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not generate registration card PDF", exception);
        }
    }

    private String valueOrBlank(String value) {
        return value == null ? "" : value;
    }
}
