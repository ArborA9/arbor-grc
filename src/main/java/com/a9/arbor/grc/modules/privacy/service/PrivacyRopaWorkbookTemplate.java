package com.a9.arbor.grc.modules.privacy.service;

import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.privacy.domain.RopaDesignation;
import com.a9.arbor.grc.modules.privacy.domain.RopaDpiaStatus;
import com.a9.arbor.grc.modules.privacy.domain.RopaLegalBasis;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Arbor-branded RoPA workbook template (PIMS SSD column shape). Mode B MVP is attachment-first -
 * this file is for offline fill; parse-into-fields is deferred.
 */
@Component
public class PrivacyRopaWorkbookTemplate {

    public static final String SHEET_ROPA = "RoPA";
    public static final String SHEET_REF = "References";

    public static final String[] HEADERS = {
            "entity", "sector", "division", "section", "designation",
            "activity_name", "processing_description", "purpose",
            "data_categories", "sensitive_data_categories", "data_subjects",
            "data_subject_volume", "data_subject_volume_note",
            "processing_frequency", "processing_frequency_note",
            "source_of_personal_data", "method_of_receiving", "legal_basis",
            "share_internal", "share_internal_details",
            "share_operator", "share_operator_details",
            "share_external", "share_external_details", "external_entity_location",
            "third_country_transfers", "adequate_protection", "cross_border_transfer_basis",
            "safeguards_implemented", "third_country_transfer_details",
            "storage_systems", "retention_period", "security_measures",
            "dpia_status", "representative_name", "representative_employee_id", "comments"
    };

    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public ExportFile buildTemplate() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet ropa = wb.createSheet(SHEET_ROPA);
            Row header = ropa.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
                ropa.setColumnWidth(i, 18 * 256);
            }
            Sheet ref = wb.createSheet(SHEET_REF);
            ref.createRow(0).createCell(0).setCellValue("Legal basis (UAE / PIMS)");
            int r = 1;
            for (RopaLegalBasis b : RopaLegalBasis.values()) {
                ref.createRow(r++).createCell(0).setCellValue(b.name());
            }
            r = 1;
            ref.getRow(0).createCell(1).setCellValue("Designation");
            for (RopaDesignation d : RopaDesignation.values()) {
                Row row = ref.getRow(r);
                if (row == null) {
                    row = ref.createRow(r);
                }
                row.createCell(1).setCellValue(d.name());
                r++;
            }
            r = 1;
            ref.getRow(0).createCell(2).setCellValue("DPIA status");
            for (RopaDpiaStatus s : RopaDpiaStatus.values()) {
                Row row = ref.getRow(r);
                if (row == null) {
                    row = ref.createRow(r);
                }
                row.createCell(2).setCellValue(s.name());
                r++;
            }
            r = 1;
            ref.getRow(0).createCell(3).setCellValue("Volume (per month)");
            for (String v : Arrays.asList("0-1000", "1000-100000", "100000-1M", "Over 1M")) {
                Row row = ref.getRow(r);
                if (row == null) {
                    row = ref.createRow(r);
                }
                row.createCell(3).setCellValue(v);
                r++;
            }
            r = 1;
            ref.getRow(0).createCell(4).setCellValue("Frequency");
            for (String v : Arrays.asList("Daily", "Weekly", "Monthly", "Quarterly", "Annually", "Ad hoc", "One off")) {
                Row row = ref.getRow(r);
                if (row == null) {
                    row = ref.createRow(r);
                }
                row.createCell(4).setCellValue(v);
                r++;
            }

            Sheet instructions = wb.createSheet("Instructions");
            instructions.createRow(0).createCell(0).setCellValue(
                    "Arbor GRC: RoPA workbook (Mode B). Fill the RoPA sheet offline, then upload "
                            + "as WORKBOOK_SUBMITTED evidence together with APPROVAL_EVIDENCE "
                            + "(email PDF or signed approval). Structured field import is not required for MVP.");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new ExportFile(out.toByteArray(), "arbor-ropa-template.xlsx", CONTENT_TYPE);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not build RoPA template");
        }
    }
}
