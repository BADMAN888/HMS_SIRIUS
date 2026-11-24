package com.sirius.sirius.controller;

import com.sirius.sirius.document.dto.ReportDocumentRequest;
import com.sirius.sirius.document.model.DocumentFormat;
import com.sirius.sirius.document.service.ReportDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports/documents")
@RequiredArgsConstructor
public class ReportDocumentController {
    private final ReportDocumentService reportDocumentService;

    @PostMapping
    @PreAuthorize("@hotelAccess.canAccessHotel(#p0.hotelId())")
    public ResponseEntity<byte[]> generate(
            @Valid @RequestBody ReportDocumentRequest request,
            @RequestParam(defaultValue = "PDF") DocumentFormat format
    ) {
        var document = reportDocumentService.generate(request, format);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.fileName() + "\"")
                .body(document.content());
    }

    @PostMapping("/preview")
    @PreAuthorize("@hotelAccess.canAccessHotel(#p0.hotelId())")
    public ResponseEntity<byte[]> preview(@Valid @RequestBody ReportDocumentRequest request) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"hotel-report-preview.pdf\"")
                .body(reportDocumentService.preview(request));
    }
}
