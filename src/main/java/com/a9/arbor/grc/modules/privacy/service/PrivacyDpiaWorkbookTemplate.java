package com.a9.arbor.grc.modules.privacy.service;

import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.privacy.service.DpiaMethodologyBank.Principle;
import com.a9.arbor.grc.modules.privacy.service.DpiaMethodologyBank.Question;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Arbor DPIA multi-sheet template (PIMS-shaped). Mode B MVP is attachment-first. */
@Component
public class PrivacyDpiaWorkbookTemplate {

    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public ExportFile buildTemplate() {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet overview = wb.createSheet("Overview");
            overview.createRow(0).createCell(0).setCellValue("Arbor GRC: DPIA Overview");
            overview.createRow(1).createCell(0).setCellValue("Title");
            overview.createRow(2).createCell(0).setCellValue("Linked RoPA ID");
            overview.createRow(3).createCell(0).setCellValue("Completer");
            overview.createRow(4).createCell(0).setCellValue("DPO approval");
            overview.createRow(6).createCell(0).setCellValue("Risk register (R-001…)");
            Row riskHdr = overview.createRow(7);
            riskHdr.createCell(0).setCellValue("Ref");
            riskHdr.createCell(1).setCellValue("Risk");
            riskHdr.createCell(2).setCellValue("Measures");
            riskHdr.createCell(3).setCellValue("Owner");
            riskHdr.createCell(4).setCellValue("Target date");
            riskHdr.createCell(5).setCellValue("Status");

            Sheet pre = wb.createSheet("Pre-DPIA (Screening)");
            pre.createRow(0).createCell(0).setCellValue("No.");
            pre.getRow(0).createCell(1).setCellValue("Question");
            pre.getRow(0).createCell(2).setCellValue("Answer");
            pre.createRow(1).createCell(0).setCellValue(1);
            pre.getRow(1).createCell(1).setCellValue("What is the name of the project or business process?");
            pre.createRow(2).createCell(0).setCellValue(2);
            pre.getRow(2).createCell(1).setCellValue("Which functions will be involved?");
            pre.createRow(3).createCell(0).setCellValue(3);
            pre.getRow(3).createCell(1).setCellValue("Do you process personal data as part of the project?");
            int r = 4;
            int n = 4;
            for (String q : DpiaMethodologyBank.SCREENING_Q4_TO_Q7) {
                Row row = pre.createRow(r++);
                row.createCell(0).setCellValue(n++);
                row.createCell(1).setCellValue(q);
            }
            pre.createRow(r).createCell(0).setCellValue(
                    "If Yes to any of Q4–7 you must complete a full DPIA (Questions + Assessment sheets).");

            Sheet qs = wb.createSheet("DPIA Questions");
            Row qh = qs.createRow(0);
            qh.createCell(0).setCellValue("#");
            qh.createCell(1).setCellValue("Principle");
            qh.createCell(2).setCellValue("Question");
            qh.createCell(3).setCellValue("Answer");
            int qi = 1;
            for (Question q : DpiaMethodologyBank.QUESTIONS) {
                Row row = qs.createRow(qi++);
                row.createCell(0).setCellValue(q.number());
                row.createCell(1).setCellValue(q.principle());
                row.createCell(2).setCellValue(q.question());
            }

            Sheet assess = wb.createSheet("DPIA Assessment");
            Row ah = assess.createRow(0);
            String[] headers = {
                    "#", "Principles", "Description", "Compliance Status", "Current Controls", "Notes",
                    "Risks", "Likelihood", "Impact", "Inherent Risk", "Recommended Measures",
                    "Residual Risk", "Risk Treatment", "Owner", "Target Date", "Status"
            };
            for (int i = 0; i < headers.length; i++) {
                ah.createCell(i).setCellValue(headers[i]);
            }
            int ai = 1;
            for (Principle p : DpiaMethodologyBank.PRINCIPLES) {
                Row row = assess.createRow(ai++);
                row.createCell(0).setCellValue(p.number());
                row.createCell(1).setCellValue(p.name());
                row.createCell(2).setCellValue(p.description());
            }

            Sheet cfg = wb.createSheet("Config");
            cfg.createRow(0).createCell(0).setCellValue("Likelihood");
            int i = 1;
            for (String v : DpiaMethodologyBank.LIKELIHOOD) {
                cfg.createRow(i++).createCell(0).setCellValue(v);
            }
            cfg.getRow(0).createCell(1).setCellValue("Impact");
            i = 1;
            for (String v : DpiaMethodologyBank.IMPACT) {
                Row row = cfg.getRow(i);
                if (row == null) {
                    row = cfg.createRow(i);
                }
                row.createCell(1).setCellValue(v);
                i++;
            }
            cfg.getRow(0).createCell(2).setCellValue("Treatment");
            i = 1;
            for (String v : DpiaMethodologyBank.TREATMENT) {
                Row row = cfg.getRow(i);
                if (row == null) {
                    row = cfg.createRow(i);
                }
                row.createCell(2).setCellValue(v);
                i++;
            }
            cfg.createRow(8).createCell(0).setCellValue(
                    "Risk = Likelihood × Impact (1–5 scales). Mode B: upload filled workbook + approval evidence.");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new ExportFile(out.toByteArray(), "arbor-dpia-template.xlsx", CONTENT_TYPE);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not build DPIA template");
        }
    }
}
