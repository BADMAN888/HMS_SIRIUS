package com.sirius.sirius.document.generator;

import com.sirius.sirius.document.dto.DocumentData;
import com.sirius.sirius.document.model.DocumentFormat;
import com.sirius.sirius.document.model.DocumentType;
import com.sirius.sirius.exeption.ValidationException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Component
public class PdfDocumentGenerator implements DocumentGenerator {
    private static final Color NAVY = new Color(23, 59, 80);
    private static final Color TEAL = new Color(38, 114, 123);
    private static final Color GOLD = new Color(197, 155, 85);
    private static final Color INK = new Color(23, 37, 45);
    private static final Color MUTED = new Color(100, 119, 129);
    private static final Color PALE = new Color(242, 246, 247);
    private static final Color PALE_ALT = new Color(234, 241, 243);
    private static final Color BORDER = new Color(213, 224, 227);
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
        if (type != DocumentType.REGISTRATION_CARD && type != DocumentType.BOOKING_CONFIRMATION) {
            throw new ValidationException("Unsupported PDF document type: " + type);
        }
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            Resource fontResource = resourceLoader.getResource("classpath:fonts/registration-card-font.ttf");
            try (InputStream fontInput = fontResource.getInputStream();
                 PDPageContentStream content = new PDPageContentStream(document, page)) {
                PDFont font = PDType0Font.load(document, fontInput);
                if (type == DocumentType.REGISTRATION_CARD) drawDocument(content, page, font, data);
                else drawConfirmation(content, page, font, data);
            }
            document.save(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not generate registration card PDF", exception);
        }
    }

    private void drawConfirmation(PDPageContentStream content, PDPage page, PDFont font, DocumentData data)
            throws Exception {
        float x = 38;
        float width = page.getMediaBox().getWidth() - 76;
        float gap = 6;
        float cellWidth = (width - gap) / 2;
        content.setNonStrokingColor(GOLD);
        content.addRect(x, 744, 3, 42);
        content.fill();
        drawText(content, font, valueOrBlank(data.hotelName()), 11, x + 13, 777, NAVY);
        drawText(content, font, "ПІДТВЕРДЖЕННЯ БРОНЮВАННЯ  /  BOOKING CONFIRMATION",
                14, x + 13, 755, Color.BLACK);
        String contact = String.join("   |   ", Stream.of(data.hotelAddress(), data.hotelPhone(), data.hotelEmail())
                .filter(value -> value != null && !value.isBlank()).toList());
        drawText(content, font, contact, 8, x + 13, 738, MUTED);

        drawText(content, font, "Дякуємо за бронювання. Збережіть номер підтвердження для звернень до готелю.",
                8.4f, x, 713, MUTED);
        drawText(content, font, "Thank you for your reservation. Keep this confirmation number for your records.",
                8.4f, x, 700, MUTED);
        section(content, font, "ДЕТАЛІ БРОНЮВАННЯ / BOOKING DETAILS", x, 675);

        field(content, font, x, 656, cellWidth, "Номер підтвердження / Confirmation No.",
                data.reservationNumber(), PALE_ALT);
        field(content, font, x + cellWidth + gap, 656, cellWidth, "Статус / Status",
                data.reservationStatus(), PALE_ALT);
        field(content, font, x, 621, cellWidth, "Гість / Guest",
                data.primaryGuestFullName(), PALE);
        field(content, font, x + cellWidth + gap, 621, cellWidth, "Кількість гостей / Guests",
                data.guestCount(), PALE);
        field(content, font, x, 586, cellWidth, "Заїзд / Check-in",
                join(data.checkInDate(), data.checkInTime()), PALE_ALT);
        field(content, font, x + cellWidth + gap, 586, cellWidth, "Виїзд / Check-out",
                join(data.checkOutDate(), data.checkOutTime()), PALE_ALT);
        field(content, font, x, 551, cellWidth, "Номер кімнати / Room No.",
                data.roomNumber(), PALE);
        field(content, font, x + cellWidth + gap, 551, cellWidth, "Категорія / Room category",
                data.roomCategory(), PALE);
        field(content, font, x, 516, width, "Тариф / Rate plan", data.rateName(), PALE_ALT);

        drawText(content, font, "Якщо вам потрібна допомога зі змінами бронювання, зв’яжіться з готелем.",
                8.2f, x, 477, MUTED);
        drawText(content, font, "For changes or assistance, please contact the hotel using the details above.",
                8.2f, x, 464, MUTED);
    }

    private void drawDocument(PDPageContentStream content, PDPage page, PDFont font, DocumentData data)
            throws Exception {
        float x = 38;
        float width = page.getMediaBox().getWidth() - 76;

        content.setNonStrokingColor(GOLD);
        content.addRect(x, 744, 3, 42);
        content.fill();
        drawText(content, font, valueOrBlank(data.hotelName()), 11, x + 13, 777, NAVY);
        drawText(content, font, "РЕЄСТРАЦІЙНА КАРТА ГОСТЯ  /  GUEST REGISTRATION CARD", 15, x + 13, 755, Color.BLACK);
        String contact = String.join("   |   ", Stream.of(data.hotelAddress(), data.hotelPhone(), data.hotelEmail())
                .filter(value -> value != null && !value.isBlank()).toList());
        drawText(content, font, contact, 8, x + 13, 738, MUTED);

        section(content, font, "ДАНІ ГОСТЯ / GUEST DETAILS", x, 713);
        float gap = 6;
        float cellWidth = (width - gap) / 2;
        field(content, font, x, 672, cellWidth, "ПІБ / Full name", data.primaryGuestFullName(), PALE);
        field(content, font, x + cellWidth + gap, 672, cellWidth, "Дата народження / Date of birth", data.dateOfBirth(), PALE);
        field(content, font, x, 637, cellWidth, "Телефон / Phone", data.phoneNumber(), PALE_ALT);
        field(content, font, x + cellWidth + gap, 637, cellWidth, "Email", data.email(), PALE_ALT);
        field(content, font, x, 602, cellWidth, "Громадянство / Nationality", data.nationality(), PALE);
        field(content, font, x + cellWidth + gap, 602, cellWidth, "Адреса / Address", data.address(), PALE);

        section(content, font, "ІНФОРМАЦІЯ ПРО ПРОЖИВАННЯ / STAY DETAILS", x, 555);
        field(content, font, x, 535, cellWidth, "Бронювання / Reservation No.", data.reservationNumber(), PALE_ALT);
        field(content, font, x + cellWidth + gap, 535, cellWidth, "Номер кімнати / Room No.", data.roomNumber(), PALE_ALT);
        field(content, font, x, 500, cellWidth, "Заїзд / Check-in",
                join(data.checkInDate(), data.checkInTime()), PALE);
        field(content, font, x + cellWidth + gap, 500, cellWidth, "Виїзд / Check-out",
                join(data.checkOutDate(), data.checkOutTime()), PALE);

        section(content, font, "ПРАВИЛА ПРОЖИВАННЯ / HOTEL RULES", x, 451);
        float half = width / 2;
        ruleHeader(content, font, x, 431, half, "УКРАЇНСЬКОЮ");
        ruleHeader(content, font, x + half, 431, half, "ENGLISH");
        List<String> ua = List.of(
                "Дотримуйтесь тиші та поважайте інших гостей.",
                "Куріння дозволено лише у спеціально відведених місцях.",
                "Бережіть майно готелю та повідомляйте адміністрацію про пошкодження.",
                "Не передавайте ключ-карту стороннім та не запрошуйте незареєстрованих відвідувачів без погодження.",
                "Дотримуйтесь правил пожежної безпеки та не користуйтеся несправним обладнанням.",
                "Дотримуйтесь часу виїзду, вказаного в інформації про проживання. Додатковий час погоджується з адміністрацією.");
        List<String> en = List.of(
                "Please respect quiet hours and other guests.",
                "Smoking is permitted only in designated areas.",
                "Please take care of hotel property and report any damage to reception.",
                "Do not give your key card to others or invite unregistered visitors without approval.",
                "Follow fire-safety instructions and do not use faulty equipment.",
                "Please check out at the time shown above. Arrange late check-out with reception.");
        for (int i = 0; i < ua.size(); i++) {
            float top = 409 - i * 28;
            Color fill = i % 2 == 0 ? Color.WHITE : PALE;
            ruleCell(content, font, x, top, half, ua.get(i), fill);
            ruleCell(content, font, x + half, top, half, en.get(i), fill);
        }

        drawText(content, font,
                "Підтверджую правильність наданих даних, ознайомлення з правилами проживання та зобов’язуюсь їх дотримуватися.",
                7.4f, x, 222, MUTED);
        drawText(content, font,
                "I confirm the information is correct, have read the hotel rules, and agree to comply with them.",
                7.4f, x, 210, MUTED);
        drawText(content, font, "Підпис гостя / Guest signature:", 8.5f, x, 187, NAVY);
        drawLine(content, x + 143, 184, x + 350, 184, BORDER);
        drawText(content, font, "Дата / Date:", 8.5f, x + 372, 187, NAVY);
        drawLine(content, x + 420, 184, x + width, 184, BORDER);
    }

    private void section(PDPageContentStream content, PDFont font, String text, float x, float y) throws Exception {
        drawText(content, font, text, 9.2f, x, y, NAVY);
    }

    private void field(PDPageContentStream content, PDFont font, float x, float top, float width,
                       String label, String value, Color fill) throws Exception {
        float height = 30;
        content.setNonStrokingColor(fill);
        content.addRect(x, top - height, width, height);
        content.fill();
        strokeRect(content, x, top - height, width, height, BORDER);
        drawText(content, font, label, 7.5f, x + 7, top - 10, TEAL);
        drawFitText(content, font, valueOrBlank(value), 9, 7, width - 14, x + 7, top - 23, INK);
    }

    private void ruleHeader(PDPageContentStream content, PDFont font, float x, float top, float width, String text)
            throws Exception {
        content.setNonStrokingColor(NAVY);
        content.addRect(x, top - 18, width, 18);
        content.fill();
        drawFitText(content, font, text, 7.7f, 7.0f, width - 12, x + 6, top - 12, Color.WHITE);
    }

    private void ruleCell(PDPageContentStream content, PDFont font, float x, float top, float width,
                          String text, Color fill) throws Exception {
        float height = 28;
        content.setNonStrokingColor(fill);
        content.addRect(x, top - height, width, height);
        content.fill();
        strokeRect(content, x, top - height, width, height, BORDER);
        List<String> lines = wrap(text, font, 7.0f, width - 12, 2);
        float y = top - 10;
        for (String line : lines) {
            drawText(content, font, line, 7.0f, x + 6, y, INK);
            y -= 9;
        }
    }

    private List<String> wrap(String text, PDFont font, float fontSize, float maxWidth, int maxLines) throws Exception {
        List<String> result = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.getStringWidth(candidate) / 1000 * fontSize > maxWidth && !line.isEmpty()) {
                result.add(line.toString());
                line.setLength(0);
                line.append(word);
                if (result.size() == maxLines) break;
            } else {
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            }
        }
        if (result.size() < maxLines && !line.isEmpty()) result.add(line.toString());
        return result;
    }

    private void drawFitText(PDPageContentStream content, PDFont font, String text, float maxSize, float minSize,
                             float width, float x, float y, Color color) throws Exception {
        float size = maxSize;
        while (size > minSize && font.getStringWidth(text) / 1000 * size > width) size -= 0.25f;
        drawText(content, font, text, size, x, y, color);
    }

    private void drawText(PDPageContentStream content, PDFont font, String text, float size,
                          float x, float y, Color color) throws Exception {
        content.beginText();
        content.setNonStrokingColor(color);
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(valueOrBlank(text));
        content.endText();
    }

    private void strokeRect(PDPageContentStream content, float x, float y, float width, float height, Color color)
            throws Exception {
        content.setStrokingColor(color);
        content.setLineWidth(0.45f);
        content.addRect(x, y, width, height);
        content.stroke();
    }

    private void drawLine(PDPageContentStream content, float x1, float y1, float x2, float y2, Color color)
            throws Exception {
        content.setStrokingColor(color);
        content.setLineWidth(0.7f);
        content.moveTo(x1, y1);
        content.lineTo(x2, y2);
        content.stroke();
    }

    private String join(String first, String second) {
        String a = valueOrBlank(first);
        String b = valueOrBlank(second);
        return (a + "   " + b).trim();
    }

    private String valueOrBlank(String value) {
        return value == null ? "" : value;
    }
}
