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
        if (type != DocumentType.REGISTRATION_CARD) {
            throw new ValidationException("Unsupported Word document type: " + type);
        }
        Resource resource = resourceLoader.getResource("classpath:templates/registration-card.docx");
        if (!resource.exists()) {
            throw new IllegalStateException("Registration card template was not found");
        }
        Map<String, Object> variables = Map.of(
                "primaryGuestFullName", data.primaryGuestFullName(),
                "reservationNumber", valueOrBlank(data.reservationNumber()),
                "checkInDate", valueOrBlank(data.checkInDate()),
                "checkOutDate", valueOrBlank(data.checkOutDate()),
                "roomNumber", valueOrBlank(data.roomNumber())
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
