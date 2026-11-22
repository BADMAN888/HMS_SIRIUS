package com.sirius.sirius.document.service;

import com.sirius.sirius.document.model.DocumentFormat;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.util.ByteArrayDataSource;

@Service
public class BookingConfirmationEmailService {
    private final JavaMailSender mailSender;
    private final DocumentService documentService;
    private final String fromAddress;

    public BookingConfirmationEmailService(
            JavaMailSender mailSender,
            DocumentService documentService,
            @Value("${app.mail.from:}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.documentService = documentService;
        this.fromAddress = fromAddress;
    }

    public void send(Long reservationId, String recipient, String subject) {
        if (fromAddress.isBlank()) {
            throw new IllegalStateException("Set APP_MAIL_FROM and SMTP settings to send booking confirmations");
        }
        var confirmation = documentService.generateBookingConfirmation(reservationId, DocumentFormat.PDF);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText("Вітаємо! У вкладенні підтвердження вашого бронювання.\n\n" +
                    "Hello! Please find your booking confirmation attached.");
            helper.addAttachment(confirmation.fileName(), new ByteArrayDataSource(
                    confirmation.content(), confirmation.contentType()));
            mailSender.send(message);
        } catch (MessagingException exception) {
            throw new IllegalStateException("Could not prepare booking confirmation email", exception);
        }
    }
}
