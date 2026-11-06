package com.a9.arbor.grc.modules.export;

import com.a9.arbor.grc.modules.gap.domain.GapAssessment;
import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.soa.domain.Soa;
import com.a9.arbor.grc.modules.soa.domain.SoaItem;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/** Certification-shaped Excel stubs for SoA / Gap (Path B6). */
@Component
public class AssessmentWorkbookWriter {

    public byte[] writeSoa(Soa soa, List<SoaItem> items) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet cover = workbook.createSheet("Cover");
            int r = 0;
            coverRow(cover, r++, "Document", "Statement of Applicability");
            coverRow(cover, r++, "Framework", nullToEmpty(soa.getFrameworkName()));
            coverRow(cover, r++, "Code", nullToEmpty(soa.getFrameworkCode()));
            coverRow(cover, r++, "Version", nullToEmpty(soa.getFrameworkVersion()));
            coverRow(cover, r++, "Status", soa.getStatus() != null ? soa.getStatus().name() : "");
            coverRow(cover, r++, "Project ID", String.valueOf(soa.getProjectId()));
            coverRow(cover, r++, "SoA ID", String.valueOf(soa.getId()));
            coverRow(cover, r++, "Completed at", soa.getCompletedAt() != null ? soa.getCompletedAt().toString() : "");
            cover.autoSizeColumn(0);
            cover.autoSizeColumn(1);

            Sheet sheet = workbook.createSheet("Controls");
            String[] headers = {
                    "Control code", "Title", "Applicability", "Justification", "Remarks"
            };
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int rowIdx = 1;
            for (SoaItem item : items) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(nullToEmpty(item.getNodeCode()));
                row.createCell(1).setCellValue(nullToEmpty(item.getTitle()));
                row.createCell(2).setCellValue(
                        item.getApplicability() != null ? item.getApplicability().name() : "");
                row.createCell(3).setCellValue(nullToEmpty(item.getJustification()));
                row.createCell(4).setCellValue(nullToEmpty(item.getRemarks()));
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build SoA Excel", e);
        }
    }

    public byte[] writeGap(GapAssessment assessment, List<GapItem> items) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet cover = workbook.createSheet("Cover");
            int r = 0;
            coverRow(cover, r++, "Document", "Gap Assessment");
            coverRow(cover, r++, "Name", nullToEmpty(assessment.getName()));
            coverRow(cover, r++, "Framework", nullToEmpty(assessment.getFrameworkName()));
            coverRow(cover, r++, "Code", nullToEmpty(assessment.getFrameworkCode()));
            coverRow(cover, r++, "Version", nullToEmpty(assessment.getFrameworkVersion()));
            coverRow(cover, r++, "Status", assessment.getStatus() != null ? assessment.getStatus().name() : "");
            coverRow(cover, r++, "Type",
                    assessment.getAssessmentType() != null ? assessment.getAssessmentType().name() : "");
            coverRow(cover, r++, "Overall score %",
                    assessment.getOverallScore() != null ? assessment.getOverallScore().toPlainString() : "");
            coverRow(cover, r++, "RAG", nullToEmpty(assessment.getRagStatus()));
            coverRow(cover, r++, "Project ID", String.valueOf(assessment.getProjectId()));
            coverRow(cover, r++, "Assessment ID", String.valueOf(assessment.getId()));
            cover.autoSizeColumn(0);
            cover.autoSizeColumn(1);

            Sheet sheet = workbook.createSheet("Controls");
            String[] headers = {
                    "Control code", "Title", "Applicability",
                    "Contributor status", "Admin final status", "Review status",
                    "Score", "Observations", "Gap description", "Admin notes"
            };
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int rowIdx = 1;
            for (GapItem item : items) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(nullToEmpty(item.getNodeCode()));
                row.createCell(1).setCellValue(nullToEmpty(item.getTitle()));
                row.createCell(2).setCellValue(
                        item.getApplicability() != null ? item.getApplicability().name() : "");
                row.createCell(3).setCellValue(
                        item.getImplementationStatus() != null ? item.getImplementationStatus().name() : "");
                row.createCell(4).setCellValue(
                        item.getFinalImplementationStatus() != null
                                ? item.getFinalImplementationStatus().name() : "");
                row.createCell(5).setCellValue(
                        item.getReviewStatus() != null ? item.getReviewStatus().name() : "");
                if (item.getScore() != null) {
                    row.createCell(6).setCellValue(item.getScore());
                } else {
                    row.createCell(6).setCellValue("");
                }
                row.createCell(7).setCellValue(nullToEmpty(item.getObservations()));
                row.createCell(8).setCellValue(nullToEmpty(item.getGapDescription()));
                row.createCell(9).setCellValue(nullToEmpty(item.getAdminReviewNotes()));
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build Gap Excel", e);
        }
    }

    private static void coverRow(Sheet sheet, int rowIdx, String label, String value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value != null ? value : "");
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
