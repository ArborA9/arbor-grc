package com.a9.arbor.grc.modules.privacy.service;

import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.privacy.domain.DpiaResponseType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Admin template for adding DPIA questionnaire questions. Sample row is ignored on upload. */
@Component
public class DpiaQuestionnaireImportWorkbook {

    public static final String[] HEADERS = {
            "Code", "Section", "Principle", "Question", "Response type", "Required", "Guidance", "Options"
    };

    public record ParsedQuestion(
            int excelRow,
            String code,
            String section,
            String principle,
            String question,
            String responseTypeRaw,
            String requiredRaw,
            String guidance,
            String optionsRaw,
            boolean sample
    ) {
    }

    public record ParsedBook(List<ParsedQuestion> questions) {
    }

    public ExportFile template() {
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = book.createSheet("Questions");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
                sheet.setColumnWidth(i, 22 * 256);
            }
            Row sample = sheet.createRow(1);
            String[] values = {
                    "SAMPLE", "Sample section", "Lawfulness",
                    "Replace this row with a real question. Rows whose Code is SAMPLE are ignored.",
                    "FREE_TEXT", "Yes", "Optional guidance for the Champion.",
                    "Only for MULTI_SELECT. Separate options with a semicolon."
            };
            for (int i = 0; i < values.length; i++) {
                sample.createCell(i).setCellValue(values[i]);
            }
            book.write(out);
            return new ExportFile(
                    out.toByteArray(),
                    "dpia-questionnaire-questions-template.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not build the template");
        }
    }

    public ParsedBook parse(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload an Excel file");
        }
        try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = book.getSheet("Questions");
            if (sheet == null) {
                sheet = book.getNumberOfSheets() > 0 ? book.getSheetAt(0) : null;
            }
            if (sheet == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The workbook has no sheet");
            }
            DataFormatter fmt = new DataFormatter();
            Row header = sheet.getRow(0);
            if (header == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The first row must be the column headers");
            }
            for (int i = 0; i < HEADERS.length; i++) {
                String got = cell(fmt, header, i);
                if (!HEADERS[i].equalsIgnoreCase(got)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Column " + (i + 1) + " must be \"" + HEADERS[i] + "\"");
                }
            }
            List<ParsedQuestion> rows = new ArrayList<>();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                String code = cell(fmt, row, 0);
                String question = cell(fmt, row, 3);
                if (code.isBlank() && question.isBlank()) {
                    continue;
                }
                rows.add(new ParsedQuestion(
                        r + 1,
                        code,
                        cell(fmt, row, 1),
                        cell(fmt, row, 2),
                        question,
                        cell(fmt, row, 4),
                        cell(fmt, row, 5),
                        cell(fmt, row, 6),
                        cell(fmt, row, 7),
                        "SAMPLE".equalsIgnoreCase(code)));
            }
            return new ParsedBook(rows);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the Excel file");
        }
    }

    public static DpiaResponseType parseResponseType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Response type is required");
        }
        String key = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        return switch (key) {
            case "FREE_TEXT", "TEXT" -> DpiaResponseType.FREE_TEXT;
            case "FREE_TEXT_ATTACHMENT", "TEXT_ATTACHMENT", "ATTACHMENT" -> DpiaResponseType.FREE_TEXT_ATTACHMENT;
            case "YES_NO", "YES/NO" -> DpiaResponseType.YES_NO;
            case "YES_NO_PARTLY", "YES/NO/PARTLY" -> DpiaResponseType.YES_NO_PARTLY;
            case "MULTI_SELECT", "MULTISELECT" -> DpiaResponseType.MULTI_SELECT;
            case "CONFIRMATION" -> DpiaResponseType.CONFIRMATION;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Response type must be one of: FREE_TEXT, FREE_TEXT_ATTACHMENT, YES_NO, YES_NO_PARTLY, MULTI_SELECT, CONFIRMATION");
        };
    }

    public static boolean parseRequired(String raw) {
        if (raw == null || raw.isBlank()) {
            return true;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        if (v.equals("yes") || v.equals("y") || v.equals("true") || v.equals("required")) {
            return true;
        }
        if (v.equals("no") || v.equals("n") || v.equals("false") || v.equals("optional")) {
            return false;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required must be Yes or No");
    }

    public static List<String> parseOptions(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String part : raw.split(";")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    private static String cell(DataFormatter fmt, Row row, int index) {
        Cell cell = row.getCell(index);
        return cell == null ? "" : fmt.formatCellValue(cell).trim();
    }
}
