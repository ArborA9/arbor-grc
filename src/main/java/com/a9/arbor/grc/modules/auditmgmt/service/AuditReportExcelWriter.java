package com.a9.arbor.grc.modules.auditmgmt.service;

import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAudit;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditChecklistItem;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditFinding;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Full audit project workbook. Every working-paper and finding field is a cell, untruncated. */
@Component
public class AuditReportExcelWriter {

    static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final String[] CHECKLIST_HEADERS = {
            "Checklist",
            "Clause / Ref",
            "Section",
            "Control",
            "Test focus",
            "SoA / type",
            "Evidence source",
            "In scope",
            "Conclusion",
            "Self-assessment",
            "Finding ID",
            "Finding description",
            "Auditor note",
            "Evidence examined",
            "Persons interviewed",
            "Date reviewed"
    };

    private static final String[] FINDING_HEADERS = {
            "Finding ID",
            "Severity",
            "Title",
            "Clause / Ref",
            "Control",
            "Description",
            "Recommended action",
            "Raised",
            "CAPA"
    };

    public byte[] write(AuditReportData data) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles styles = new Styles(wb);
            writeEngagement(wb.createSheet("Engagement"), styles, data);
            writeTeam(wb.createSheet("Team"), styles, data);
            writeChecklist(wb.createSheet("Checklist"), styles, data);
            writeFindings(wb.createSheet("Findings"), styles, data);
            if (!data.narratives().isEmpty()) {
                writeNarratives(wb.createSheet("Domain narratives"), styles, data);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not export the audit report", e);
        }
    }

    private static void writeEngagement(Sheet sheet, Styles styles, AuditReportData data) {
        GrcAudit audit = data.audit();
        String[][] rows = {
                {"Organisation", AuditReportLabels.dash(data.organisation())},
                {"Report", AuditReportLabels.reportTitle(audit.getAuditType())},
                {"Audit ID", AuditReportLabels.dash(audit.getDisplayId())},
                {"Audit name", AuditReportLabels.dash(audit.getName())},
                {"Status", AuditReportLabels.statusLabel(audit.getStatus(), data.findings().size())},
                {"Type", AuditReportLabels.typeLabel(audit.getAuditType())},
                {"Mode", AuditReportLabels.modeLabel(audit.getAuditMode())},
                {"Framework", data.frameworkLine()},
                {"Checklists", data.checklistNames()},
                {"Period", AuditReportLabels.period(audit.getStartDate(), audit.getEndDate())},
                {"Lead auditor", person(data.leadName(), data.leadEmail())},
                {"Auditee", AuditReportLabels.dash(data.auditeeName())},
                {"Auditor organisation", AuditReportLabels.dash(audit.getAuditorOrganisation())},
                {"Modules in scope", data.scopeModules().isEmpty() ? "-" : String.join(", ", data.scopeModules())},
                {"Scope notes", AuditReportLabels.dash(audit.getScopeNotes())},
                {"Control subset", data.scopeNodeCount() == 0 ? "-" : data.scopeNodeCount() + " controls"},
                {"Executive summary", AuditReportLabels.dash(audit.getExecutiveSummary())},
                {"Controls in scope", String.valueOf(data.controlsInScope())},
                {"Reviewed", String.valueOf(data.reviewed())},
                {"Still open", String.valueOf(data.stillOpen())},
                {"Findings", String.valueOf(data.findings().size())},
                {"Report issued", AuditReportLabels.date(audit.getReportIssuedDate())},
                {"Lead auditor signed", signed(data.auditorSignedBy(), audit.getAuditorSignedAt())},
                {"Admin signed", signed(data.adminSignedBy(), audit.getAdminSignedAt())},
                {"Generated", AuditReportLabels.instant(data.generatedAt())},
                {"Generated by", AuditReportLabels.dash(data.generatedBy())},
                {"Classification", AuditReportLabels.dash(data.classification())}
        };
        header(sheet, styles, "Field", "Value");
        int r = 1;
        for (String[] pair : rows) {
            Row row = sheet.createRow(r++);
            cell(row, 0, pair[0], styles.label);
            cell(row, 1, pair[1], styles.wrap);
            int lines = Math.min(14, Math.max(1, pair[1].length() / 90 + countBreaks(pair[1])));
            if (lines > 1) {
                row.setHeightInPoints(lines * 15f);
            }
        }
        sheet.setColumnWidth(0, 28 * 256);
        sheet.setColumnWidth(1, 88 * 256);
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, Math.max(1, rows.length), 0, 1));
    }

    private static void writeTeam(Sheet sheet, Styles styles, AuditReportData data) {
        header(sheet, styles, "Name", "Email", "Role");
        int r = 1;
        if (data.team().isEmpty()) {
            Row row = sheet.createRow(r);
            cell(row, 0, "-", styles.wrap);
            cell(row, 1, "-", styles.wrap);
            cell(row, 2, "-", styles.wrap);
        } else {
            for (AuditReportData.TeamMember member : data.team()) {
                Row row = sheet.createRow(r++);
                cell(row, 0, AuditReportLabels.dash(member.name()), styles.wrap);
                cell(row, 1, AuditReportLabels.dash(member.email()), styles.wrap);
                cell(row, 2, AuditReportLabels.dash(member.role()), styles.wrap);
            }
        }
        widths(sheet, 32, 40, 20);
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, Math.max(1, sheet.getLastRowNum()), 0, 2));
    }

    private static void writeChecklist(Sheet sheet, Styles styles, AuditReportData data) {
        header(sheet, styles, CHECKLIST_HEADERS);
        int r = 1;
        for (AuditReportData.ChecklistSheet checklist : data.sheets()) {
            for (GrcAuditChecklistItem item : checklist.items()) {
                Row row = sheet.createRow(r++);
                String findingId = "-";
                if (item.getFindingId() != null) {
                    findingId = data.findings().stream()
                            .filter(f -> item.getFindingId().equals(f.finding().getId()))
                            .map(f -> AuditReportLabels.dash(f.finding().getDisplayCode()))
                            .findFirst()
                            .orElse("-");
                }
                String[] values = {
                        AuditReportLabels.dash(checklist.name()),
                        AuditReportLabels.dash(item.getNodeCode()),
                        AuditReportLabels.dash(item.getSection()),
                        AuditReportLabels.dash(item.getTitle()),
                        AuditReportLabels.dash(item.getTestFocus()),
                        AuditReportLabels.dash(item.getSoaApplicability()),
                        AuditReportLabels.dash(item.getEvidenceModule()),
                        AuditReportLabels.yesNo(item.isInScope()),
                        AuditReportLabels.conclusion(item.getConclusion()),
                        AuditReportLabels.assessment(item.getAssessment()),
                        findingId,
                        note(data, item.getFindingDescription()),
                        note(data, item.getNotes()),
                        note(data, item.getEvidenceExamined()),
                        AuditReportLabels.dash(item.getPersonsInterviewed()),
                        AuditReportLabels.date(item.getReviewedDate())
                };
                for (int c = 0; c < values.length; c++) {
                    cell(row, c, values[c], styles.wrap);
                }
            }
        }
        if (r == 1) {
            Row row = sheet.createRow(1);
            cell(row, 0, "-", styles.wrap);
        }
        int[] widths = {22, 16, 18, 36, 42, 22, 28, 12, 16, 16, 14, 42, 36, 36, 24, 16};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, Math.max(1, sheet.getLastRowNum()), 0, CHECKLIST_HEADERS.length - 1));
    }

    private static void writeFindings(Sheet sheet, Styles styles, AuditReportData data) {
        header(sheet, styles, FINDING_HEADERS);
        int r = 1;
        if (data.findings().isEmpty()) {
            Row row = sheet.createRow(1);
            cell(row, 0, "-", styles.wrap);
        } else {
            for (AuditReportData.FindingLine line : data.findings()) {
                GrcAuditFinding f = line.finding();
                Row row = sheet.createRow(r++);
                String[] values = {
                        AuditReportLabels.dash(f.getDisplayCode()),
                        AuditReportLabels.severity(f.getSeverity()),
                        AuditReportLabels.dash(f.getTitle()),
                        AuditReportLabels.dash(line.clauseRef()),
                        AuditReportLabels.dash(f.getControlRef()),
                        AuditReportLabels.dash(f.getDescription()),
                        AuditReportLabels.dash(f.getRecommendedAction()),
                        AuditReportLabels.instant(f.getCreatedAt()),
                        AuditReportLabels.dash(line.capaLabel())
                };
                for (int c = 0; c < values.length; c++) {
                    cell(row, c, values[c], styles.wrap);
                }
                row.setHeightInPoints(Math.min(90f, 15f * Math.max(2,
                        (f.getDescription() == null ? 0 : f.getDescription().length()) / 80)));
            }
        }
        int[] widths = {14, 16, 36, 16, 20, 48, 40, 22, 28};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, Math.max(1, sheet.getLastRowNum()), 0, FINDING_HEADERS.length - 1));
    }

    private static void writeNarratives(Sheet sheet, Styles styles, AuditReportData data) {
        header(sheet, styles, "Code", "Domain", "Narrative");
        int r = 1;
        for (AuditReportData.DomainNarrative narrative : data.narratives()) {
            Row row = sheet.createRow(r++);
            cell(row, 0, AuditReportLabels.dash(narrative.code()), styles.wrap);
            cell(row, 1, AuditReportLabels.dash(narrative.name()), styles.wrap);
            cell(row, 2, AuditReportLabels.dash(narrative.narrative()), styles.wrap);
        }
        widths(sheet, 16, 32, 80);
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, Math.max(1, sheet.getLastRowNum()), 0, 2));
    }

    private static String note(AuditReportData data, String value) {
        if (!data.includeWorkingNotes()) {
            return "-";
        }
        return AuditReportLabels.dash(value);
    }

    private static String person(String name, String email) {
        String n = AuditReportLabels.dash(name);
        if (email == null || email.isBlank()) {
            return n;
        }
        if ("-".equals(n)) {
            return email.trim();
        }
        return n + " (" + email.trim() + ")";
    }

    private static String signed(String name, java.time.Instant at) {
        if (at == null && (name == null || name.isBlank())) {
            return "Not signed";
        }
        String who = AuditReportLabels.dash(name);
        String when = AuditReportLabels.instant(at);
        if ("-".equals(who)) {
            return when;
        }
        if ("-".equals(when)) {
            return who;
        }
        return who + ", " + when;
    }

    private static int countBreaks(String value) {
        int n = 1;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '\n') {
                n++;
            }
        }
        return n;
    }

    private static void header(Sheet sheet, Styles styles, String... titles) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints(22);
        for (int i = 0; i < titles.length; i++) {
            cell(row, i, titles[i], styles.header);
        }
    }

    private static void cell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private static void widths(Sheet sheet, int... chars) {
        for (int i = 0; i < chars.length; i++) {
            sheet.setColumnWidth(i, chars[i] * 256);
        }
    }

    private static final class Styles {
        final CellStyle header;
        final CellStyle wrap;
        final CellStyle label;

        Styles(XSSFWorkbook wb) {
            XSSFColor sand = new XSSFColor(new byte[]{(byte) 250, (byte) 247, (byte) 242}, new DefaultIndexedColorMap());
            XSSFCellStyle head = wb.createCellStyle();
            head.setFillForegroundColor(sand);
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            head.setWrapText(true);
            head.setVerticalAlignment(VerticalAlignment.CENTER);
            head.setBorderBottom(BorderStyle.THIN);
            Font headFont = wb.createFont();
            headFont.setBold(true);
            headFont.setFontHeightInPoints((short) 11);
            head.setFont(headFont);
            this.header = head;

            CellStyle body = wb.createCellStyle();
            body.setWrapText(true);
            body.setVerticalAlignment(VerticalAlignment.TOP);
            this.wrap = body;

            Font labelFont = wb.createFont();
            labelFont.setBold(true);
            CellStyle field = wb.createCellStyle();
            field.setFont(labelFont);
            field.setVerticalAlignment(VerticalAlignment.TOP);
            field.setWrapText(true);
            this.label = field;
        }
    }
}
