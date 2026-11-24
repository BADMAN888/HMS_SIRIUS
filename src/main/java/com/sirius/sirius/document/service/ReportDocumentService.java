package com.sirius.sirius.document.service;

import com.sirius.sirius.document.dto.GeneratedDocument;
import com.sirius.sirius.document.dto.ReportDocumentRequest;
import com.sirius.sirius.document.model.DocumentFormat;
import com.sirius.sirius.exeption.ValidationException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class ReportDocumentService {
    private static final String XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final Color NAVY = new Color(23, 59, 80);
    private static final Color TEAL = new Color(38, 114, 123);
    private static final Color PALE = new Color(242, 246, 247);
    private static final Color ALT = new Color(234, 241, 243);
    private static final Color BORDER = new Color(213, 224, 227);

    private final ResourceLoader resourceLoader;

    public ReportDocumentService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public GeneratedDocument generate(ReportDocumentRequest request, DocumentFormat format) {
        validate(request);
        byte[] bytes = switch (format) {
            case PDF -> generatePdf(request);
            case DOCX -> generateDocx(request);
            case XLSX -> generateXlsx(request);
        };
        String contentType = switch (format) {
            case PDF -> "application/pdf";
            case DOCX -> DOCX_MIME;
            case XLSX -> XLSX_MIME;
        };
        String extension = switch (format) { case PDF -> "pdf"; case DOCX -> "docx"; case XLSX -> "xlsx"; };
        String filename = "hotel-report-%s-%s.%s".formatted(request.fromDate(), request.toDate(), extension);
        return new GeneratedDocument(bytes, contentType, filename);
    }

    public byte[] preview(ReportDocumentRequest request) {
        return generate(request, DocumentFormat.PDF).content();
    }

    private byte[] generateXlsx(ReportDocumentRequest request) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Звіт");
            var titleStyle = workbook.createCellStyle();
            var titleFont = workbook.createFont(); titleFont.setBold(true); titleFont.setFontHeightInPoints((short) 16); titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            titleStyle.setFont(titleFont);
            var headerStyle = workbook.createCellStyle(); headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex()); headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN); headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            var headerFont = workbook.createFont(); headerFont.setBold(true); headerFont.setColor(IndexedColors.WHITE.getIndex()); headerStyle.setFont(headerFont);
            var dateStyle = workbook.createCellStyle(); var mutedFont = workbook.createFont(); mutedFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex()); dateStyle.setFont(mutedFont);

            var title = sheet.createRow(0); title.createCell(0).setCellValue(request.title()); title.getCell(0).setCellStyle(titleStyle);
            var hotel = sheet.createRow(1); hotel.createCell(0).setCellValue("Готель: " + request.hotelName()); hotel.getCell(0).setCellStyle(dateStyle);
            var period = sheet.createRow(2); period.createCell(0).setCellValue("Період: " + request.fromDate() + " — " + request.toDate()); period.getCell(0).setCellStyle(dateStyle);
            var header = sheet.createRow(4); header.setHeightInPoints(26);
            for (int column = 0; column < request.columns().size(); column++) { var cell = header.createCell(column); cell.setCellValue(request.columns().get(column)); cell.setCellStyle(headerStyle); }
            int rowIndex = 5;
            for (List<String> values : safeRows(request)) {
                var row = sheet.createRow(rowIndex++);
                for (int column = 0; column < request.columns().size(); column++) row.createCell(column).setCellValue(column < values.size() ? blank(values.get(column)) : "");
            }
            sheet.createFreezePane(0, 5);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(4, Math.max(4, rowIndex - 1), 0, request.columns().size() - 1));
            for (int column = 0; column < request.columns().size(); column++) sheet.setColumnWidth(column, Math.min(10000, Math.max(3000, request.columns().get(column).length() * 320)));
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) { throw new IllegalStateException("Could not generate Excel report", exception); }
    }

    private byte[] generateDocx(ReportDocumentRequest request) {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFParagraph title = document.createParagraph(); title.setAlignment(ParagraphAlignment.CENTER);
            var titleRun = title.createRun(); titleRun.setText(request.title()); titleRun.setBold(true); titleRun.setFontSize(16); titleRun.setFontFamily("Arial");
            XWPFParagraph metadata = document.createParagraph(); metadata.setAlignment(ParagraphAlignment.CENTER);
            var metadataRun = metadata.createRun(); metadataRun.setText(request.hotelName() + "   |   " + request.fromDate() + " — " + request.toDate()); metadataRun.setFontSize(9); metadataRun.setColor("647781");
            XWPFTable table = document.createTable(1, request.columns().size());
            for (int c = 0; c < request.columns().size(); c++) {
                var cell = table.getRow(0).getCell(c); cell.setColor("173B50");
                var paragraph = cell.getParagraphs().get(0); var run = paragraph.createRun(); run.setText(request.columns().get(c)); run.setBold(true); run.setColor("FFFFFF"); run.setFontSize(8);
            }
            for (List<String> values : safeRows(request)) {
                var row = table.createRow();
                for (int c = 0; c < request.columns().size(); c++) {
                    var cell = row.getCell(c); cell.setVerticalAlignment(org.apache.poi.xwpf.usermodel.XWPFTableCell.XWPFVertAlign.CENTER);
                    var paragraph = cell.getParagraphs().get(0); paragraph.setSpacingAfter(0);
                    var run = paragraph.createRun(); run.setText(c < values.size() ? blank(values.get(c)) : ""); run.setFontSize(8);
                }
            }
            document.write(output);
            return output.toByteArray();
        } catch (Exception exception) { throw new IllegalStateException("Could not generate Word report", exception); }
    }

    private byte[] generatePdf(ReportDocumentRequest request) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream();
             InputStream fontInput = fontResource().getInputStream()) {
            PDFont font = PDType0Font.load(document, fontInput);
            int columnCount = request.columns().size();
            float margin = 28f; float pageWidth = PDRectangle.A4.getHeight(); float pageHeight = PDRectangle.A4.getWidth();
            float tableWidth = pageWidth - margin * 2; float columnWidth = tableWidth / columnCount;
            PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight)); document.addPage(page);
            PDPageContentStream stream = new PDPageContentStream(document, page);
            float y = drawPdfHeader(stream, pageHeight, margin, tableWidth, font, request);
            y = drawPdfTableHeader(stream, y, margin, columnWidth, request.columns(), font);
            int rowNumber = 0;
            for (List<String> values : safeRows(request)) {
                List<List<String>> wrapped = new java.util.ArrayList<>(); int lineCount = 1;
                for (int c = 0; c < columnCount; c++) { var lines = wrap(font, c < values.size() ? blank(values.get(c)) : "", Math.max(26, columnWidth - 8), 2); wrapped.add(lines); lineCount = Math.max(lineCount, lines.size()); }
                float rowHeight = Math.max(29, lineCount * 9 + 9);
                if (y - rowHeight < 32) {
                    stream.close(); page = new PDPage(new PDRectangle(pageWidth, pageHeight)); document.addPage(page);
                    stream = new PDPageContentStream(document, page); y = pageHeight - 32;
                    y = drawPdfTableHeader(stream, y, margin, columnWidth, request.columns(), font);
                }
                stream.setLineWidth(.45f); stream.setStrokingColor(BORDER); stream.setNonStrokingColor(rowNumber++ % 2 == 0 ? PALE : ALT);
                stream.addRect(margin, y - rowHeight, tableWidth, rowHeight); stream.fill(); stream.addRect(margin, y - rowHeight, tableWidth, rowHeight); stream.stroke();
                for (int c = 0; c < columnCount; c++) {
                    float x = margin + c * columnWidth;
                    if (c > 0) { stream.moveTo(x,y); stream.lineTo(x,y-rowHeight); stream.stroke(); }
                    List<String> lines=wrapped.get(c); float baseline=y-13;
                    for(String line:lines){drawText(stream,font,line,6.5f,x+4,baseline,new Color(23,37,45));baseline-=9;}
                }
                y -= rowHeight;
            }
            stream.close();
            document.save(output); return output.toByteArray();
        } catch (Exception exception) { throw new IllegalStateException("Could not generate PDF report", exception); }
    }

    private float drawPdfHeader(PDPageContentStream stream,float pageHeight,float margin,float width,PDFont font,ReportDocumentRequest request) throws Exception {
        stream.setNonStrokingColor(NAVY); stream.addRect(margin,pageHeight-81,width,53); stream.fill();
        drawText(stream,font,request.title(),15,margin+12,pageHeight-48,Color.WHITE);
        drawText(stream,font,request.hotelName(),8.5f,margin+12,pageHeight-65,new Color(225,239,244));
        drawText(stream,font,request.fromDate()+" — "+request.toDate(),8.5f,margin+width-155,pageHeight-65,new Color(225,239,244));
        return drawPdfTableHeader(stream,pageHeight-92,margin,width/request.columns().size(),request.columns(),font);
    }
    private float drawPdfTableHeader(PDPageContentStream stream,float y,float margin,float columnWidth,List<String> columns,PDFont font) throws Exception {
        float height=27; stream.setNonStrokingColor(TEAL); stream.addRect(margin,y-height,columnWidth*columns.size(),height); stream.fill();
        for(int c=0;c<columns.size();c++){float x=margin+c*columnWidth;drawText(stream,font,wrap(font,columns.get(c),Math.max(25,columnWidth-8),2).get(0),6.5f,x+4,y-16,Color.WHITE);if(c>0){stream.setStrokingColor(new Color(255,255,255,90));stream.moveTo(x,y);stream.lineTo(x,y-height);stream.stroke();}}
        return y-height;
    }
    private void drawText(PDPageContentStream stream,PDFont font,String value,float size,float x,float y,Color color) throws Exception {
        stream.beginText(); stream.setFont(font,size); stream.setNonStrokingColor(color); stream.newLineAtOffset(x,y); stream.showText(blank(value)); stream.endText();
    }
    private List<String> wrap(PDFont font,String value,float width,int maxLines) throws Exception {
        List<String> lines=new java.util.ArrayList<>(); StringBuilder line=new StringBuilder();
        for(String word:blank(value).replace('\n',' ').split("\\s+")){
            if(word.isBlank())continue; String candidate=line.isEmpty()?word:line+" "+word;
            if(font.getStringWidth(candidate)/1000*6.5f<=width){line.setLength(0);line.append(candidate);}else{if(!line.isEmpty())lines.add(line.toString());line.setLength(0);line.append(word);}
            if(lines.size()>=maxLines)break;
        }
        if(lines.size()<maxLines&&!line.isEmpty())lines.add(line.toString());
        if(lines.size()==maxLines && value.length()>lines.stream().mapToInt(String::length).sum()) {int last=lines.size()-1;String current=lines.get(last);lines.set(last,current.length()>3?current.substring(0,current.length()-3)+"…":current);}
        return lines.isEmpty()?List.of(""):lines;
    }
    private Resource fontResource() { return resourceLoader.getResource("classpath:fonts/registration-card-font.ttf"); }
    private List<List<String>> safeRows(ReportDocumentRequest request) { return request.rows()==null?List.of():request.rows(); }
    private String blank(String value) { return value==null?"":value; }
    private void validate(ReportDocumentRequest request) {
        if (request.columns().isEmpty() || request.columns().size()>25) throw new ValidationException("Звіт має містити від 1 до 25 колонок");
        if (request.rows()!=null && request.rows().stream().anyMatch(row->row!=null&&row.size()>25)) throw new ValidationException("У звіті забагато колонок");
        LocalDate from=LocalDate.parse(request.fromDate()); LocalDate to=LocalDate.parse(request.toDate());
        if(to.isBefore(from))throw new ValidationException("Кінцева дата має бути не раніше початкової");
    }
}
