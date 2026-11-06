package com.a9.arbor.grc.modules.export;

import com.a9.arbor.grc.modules.audit.domain.AuditLog;
import com.a9.arbor.grc.modules.bcms.domain.BiaProcess;
import com.a9.arbor.grc.modules.capa.domain.Capa;
import com.a9.arbor.grc.modules.evidence.domain.Evidence;
import com.a9.arbor.grc.modules.gap.domain.GapApplicability;
import com.a9.arbor.grc.modules.gap.domain.GapAssessment;
import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.gap.web.dto.GapLayerScoreSummary;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyDsr;
import com.a9.arbor.grc.modules.risk.domain.RiskRecord;
import com.a9.arbor.grc.modules.tprm.domain.TprmVendor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Excel writers for the Reports hub (req §19.3): compliance posture, risk registers, CAPA, Privacy, TPRM, etc. */
@Component
public class ReportWorkbookWriter {

    public byte[] writeCompliancePosture(List<GapAssessment> assessments) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Compliance Posture");
            String[] headers = {"Name", "Framework", "Framework code", "Version", "Overall score %", "RAG", "Status"};
            writeHeader(sheet, headers);
            int rowIdx = 1;
            for (GapAssessment a : assessments) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(nullToEmpty(a.getName()));
                row.createCell(1).setCellValue(nullToEmpty(a.getFrameworkName()));
                row.createCell(2).setCellValue(nullToEmpty(a.getFrameworkCode()));
                row.createCell(3).setCellValue(nullToEmpty(a.getFrameworkVersion()));
                row.createCell(4).setCellValue(a.getOverallScore() != null ? a.getOverallScore().doubleValue() : 0);
                row.createCell(5).setCellValue(nullToEmpty(a.getRagStatus()));
                row.createCell(6).setCellValue(a.getStatus() != null ? a.getStatus().name() : "");
            }
            autoSize(sheet, headers.length);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build Compliance Posture Excel", e);
        }
    }

    public byte[] writeIsRiskRegister(List<RiskRecord> risks) {
        return writeRiskRegister("IS Risk Register", risks);
    }

    public byte[] writeErmRiskRegister(List<RiskRecord> risks) {
        return writeRiskRegister("ERM Risk Summary", risks);
    }

    public byte[] writeSingleRisk(RiskRecord risk) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet detail = workbook.createSheet("Risk");
            writeHeader(detail, new String[]{"Field", "Value"});
            int rowIdx = 1;
            for (String[] pair : RiskExportSupport.detailRows(risk)) {
                Row row = detail.createRow(rowIdx++);
                row.createCell(0).setCellValue(pair[0]);
                row.createCell(1).setCellValue(pair[1] != null ? pair[1] : "");
            }
            autoSize(detail, 2);

            Sheet register = workbook.createSheet("Register row");
            String[] headers = RiskExportSupport.registerHeaders();
            writeHeader(register, headers);
            Row data = register.createRow(1);
            String[] values = RiskExportSupport.registerRow(risk);
            for (int i = 0; i < values.length; i++) {
                data.createCell(i).setCellValue(values[i]);
            }
            autoSize(register, headers.length);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build single-risk Excel", e);
        }
    }

    private byte[] writeRiskRegister(String sheetName, List<RiskRecord> risks) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            String[] headers = RiskExportSupport.registerHeaders();
            writeHeader(sheet, headers);
            int rowIdx = 1;
            for (RiskRecord r : risks) {
                Row row = sheet.createRow(rowIdx++);
                String[] values = RiskExportSupport.registerRow(r);
                for (int i = 0; i < values.length; i++) {
                    row.createCell(i).setCellValue(values[i]);
                }
            }
            autoSize(sheet, headers.length);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build " + sheetName + " Excel", e);
        }
    }

    public byte[] writeCapaStatus(List<Capa> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("CAPA Status");
            String[] headers = {
                    "Register", "Title", "Status", "Priority", "Severity", "Target date", "Closed at",
            };
            writeHeader(sheet, headers);
            int rowIdx = 1;
            for (Capa c : rows) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(c.getRegisterCode() != null ? c.getRegisterCode().name() : "");
                row.createCell(1).setCellValue(nullToEmpty(c.getTitle()));
                row.createCell(2).setCellValue(c.getStatus() != null ? c.getStatus().name() : "");
                row.createCell(3).setCellValue(c.getPriority() != null ? c.getPriority().name() : "");
                row.createCell(4).setCellValue(nullToEmpty(c.getSeverity()));
                row.createCell(5).setCellValue(c.getTargetDate() != null ? c.getTargetDate().toString() : "");
                row.createCell(6).setCellValue(c.getClosedAt() != null ? c.getClosedAt().toString() : "");
            }
            autoSize(sheet, headers.length);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build CAPA Status Excel", e);
        }
    }

    public byte[] writeTprmVendorRisk(List<TprmVendor> vendors) {
        return writeNamedSheet("TPRM Vendor Register", new String[]{
                "Vendor", "Type", "Tier", "Criticality", "Posture %", "Compliance", "State", "Next reassessment", "Contract end"
        }, vendorRegisterRows(vendors));
    }

    public byte[] writeNamedSheet(String sheetName, String[] headers, List<String[]> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            writeHeader(sheet, headers);
            int rowIdx = 1;
            for (String[] values : rows) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < values.length; i++) {
                    row.createCell(i).setCellValue(values[i] != null ? values[i] : "");
                }
            }
            autoSize(sheet, headers.length);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build " + sheetName + " Excel", e);
        }
    }

    private static List<String[]> vendorRegisterRows(List<TprmVendor> vendors) {
        List<String[]> rows = new ArrayList<>();
        for (TprmVendor v : vendors) {
            rows.add(new String[]{
                    nullToEmpty(v.getName()),
                    v.getVendorType() != null ? v.getVendorType().name() : "",
                    nullToEmpty(v.getTier()),
                    v.getCriticality() != null ? v.getCriticality().name() : "",
                    v.getPosturePercent() != null ? v.getPosturePercent().toPlainString() : "",
                    v.getComplianceStatus() != null ? v.getComplianceStatus().name() : "",
                    v.getLifecycleState() != null ? v.getLifecycleState().name() : "",
                    v.getNextReviewDate() != null ? v.getNextReviewDate().toString() : "",
                    v.getContractEnd() != null ? v.getContractEnd().toString() : ""
            });
        }
        return rows;
    }

    public byte[] writeBiaSummary(List<BiaProcess> processes) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("BIA Summary");
            String[] headers = {"Process", "Criticality", "Tier", "RTO", "MAO", "Review"};
            writeHeader(sheet, headers);
            int rowIdx = 1;
            for (BiaProcess b : processes) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(nullToEmpty(b.getProcessName()));
                row.createCell(1).setCellValue(b.getCategoryLabel() != null ? b.getCategoryLabel() : "");
                if (b.getCriticalityTier() != null) {
                    row.createCell(2).setCellValue(b.getCriticalityTier());
                } else {
                    row.createCell(2).setCellValue("");
                }
                if (b.getRtoHours() != null) {
                    row.createCell(3).setCellValue(b.getRtoHours());
                } else {
                    row.createCell(3).setCellValue("");
                }
                if (b.getMaoHours() != null) {
                    row.createCell(4).setCellValue(b.getMaoHours());
                } else {
                    row.createCell(4).setCellValue("");
                }
                row.createCell(5).setCellValue(b.getReviewOutcome() != null ? b.getReviewOutcome().display() : "");
            }
            autoSize(sheet, headers.length);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build BIA Summary Excel", e);
        }
    }

    public byte[] writeEvidenceExpiry(List<Evidence> evidence) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Evidence Expiry");
            String[] headers = {"Title", "Classification", "Status", "Expires on", "Version"};
            writeHeader(sheet, headers);
            int rowIdx = 1;
            for (Evidence e : evidence) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(nullToEmpty(e.getTitle()));
                row.createCell(1).setCellValue(e.getClassification() != null ? e.getClassification().name() : "");
                row.createCell(2).setCellValue(e.getStatus() != null ? e.getStatus().name() : "");
                row.createCell(3).setCellValue(e.getExpiresOn() != null ? e.getExpiresOn().toString() : "");
                row.createCell(4).setCellValue(e.getCurrentVersion());
            }
            autoSize(sheet, headers.length);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build Evidence Expiry Excel", e);
        }
    }

    public byte[] writeUserActivity(List<AuditLog> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("User Activity");
            String[] headers = {"Created at", "Action", "Module", "Record", "User role", "Record name"};
            writeHeader(sheet, headers);
            int rowIdx = 1;
            for (AuditLog a : rows) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(a.getCreatedAt() != null ? a.getCreatedAt().toString() : "");
                row.createCell(1).setCellValue(a.getActionType() != null ? a.getActionType().name() : "");
                row.createCell(2).setCellValue(nullToEmpty(a.getModule()));
                row.createCell(3).setCellValue(nullToEmpty(a.getRecordType()));
                row.createCell(4).setCellValue(nullToEmpty(a.getUserRole()));
                row.createCell(5).setCellValue(nullToEmpty(a.getRecordName()));
            }
            autoSize(sheet, headers.length);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build User Activity Excel", e);
        }
    }

    public byte[] writePrivacyDsr(List<PrivacyDsr> dsrs) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("DSR Statistics");
            String[] headers = {"Display ID", "Type", "Subject", "Status", "Due", "Received", "Close outcome"};
            writeHeader(sheet, headers);
            int rowIdx = 1;
            for (PrivacyDsr d : dsrs) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(nullToEmpty(d.getDisplayId()));
                row.createCell(1).setCellValue(d.getRequestType() != null ? d.getRequestType().name() : "");
                row.createCell(2).setCellValue(nullToEmpty(d.getSubjectName()));
                row.createCell(3).setCellValue(d.getStatus() != null ? d.getStatus().name() : "");
                row.createCell(4).setCellValue(d.getDueAt() != null ? d.getDueAt().toString() : "");
                row.createCell(5).setCellValue(d.getReceivedAt() != null ? d.getReceivedAt().toString() : "");
                row.createCell(6).setCellValue(d.getCloseOutcome() != null ? d.getCloseOutcome().name() : "");
            }
            autoSize(sheet, headers.length);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build DSR Statistics Excel", e);
        }
    }

    /** §19.3 #05 UAE IA V2.1 Compliance: layers, domains, control scores. */
    public byte[] writeUaeIaCompliance(GapAssessment assessment, GapLayerScoreSummary layers,
                                       List<GapItem> items) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet cover = workbook.createSheet("Cover");
            writeHeader(cover, new String[]{"Field", "Value"});
            int c = 1;
            cover.createRow(c++).createCell(0).setCellValue("Assessment");
            cover.getRow(c - 1).createCell(1).setCellValue(nullToEmpty(assessment.getName()));
            cover.createRow(c++).createCell(0).setCellValue("Framework");
            cover.getRow(c - 1).createCell(1).setCellValue(
                    nullToEmpty(assessment.getFrameworkName()) + " ("
                            + nullToEmpty(assessment.getFrameworkCode()) + ")");
            cover.createRow(c++).createCell(0).setCellValue("Version");
            cover.getRow(c - 1).createCell(1).setCellValue(nullToEmpty(assessment.getFrameworkVersion()));
            cover.createRow(c).createCell(0).setCellValue("Status");
            cover.getRow(c).createCell(1).setCellValue(
                    assessment.getStatus() != null ? assessment.getStatus().name() : "");
            autoSize(cover, 2);

            Sheet layersSheet = workbook.createSheet("Layer scores");
            writeHeader(layersSheet, new String[]{"Layer", "Score %", "RAG", "Applicable", "Scored"});
            writeLayerRow(layersSheet, 1, "Management", layers != null ? layers.management() : null);
            writeLayerRow(layersSheet, 2, "Technical", layers != null ? layers.technical() : null);
            writeLayerRow(layersSheet, 3, "Overall (UAE IA)", layers != null ? layers.overallUaeIa() : null);
            autoSize(layersSheet, 5);

            Sheet domains = workbook.createSheet("Domain scores");
            writeHeader(domains, new String[]{"Code", "Domain", "Score %", "RAG", "Applicable", "Scored"});
            int dIdx = 1;
            if (layers != null && layers.domains() != null) {
                for (GapLayerScoreSummary.DomainScore d : layers.domains()) {
                    Row row = domains.createRow(dIdx++);
                    row.createCell(0).setCellValue(nullToEmpty(d.code()));
                    row.createCell(1).setCellValue(nullToEmpty(d.name()));
                    row.createCell(2).setCellValue(d.score() != null ? d.score().doubleValue() : 0);
                    row.createCell(3).setCellValue(nullToEmpty(d.rag()));
                    row.createCell(4).setCellValue(d.applicableCount());
                    row.createCell(5).setCellValue(d.scoredCount());
                }
            }
            autoSize(domains, 6);

            Sheet controls = workbook.createSheet("Control scores");
            writeHeader(controls, new String[]{"Code", "Title", "Applicability", "Final status", "Score"});
            int iIdx = 1;
            for (GapItem item : items) {
                if (item.getApplicability() == GapApplicability.NOT_APPLICABLE) {
                    continue;
                }
                var effective = item.getFinalImplementationStatus() != null
                        ? item.getFinalImplementationStatus()
                        : item.getImplementationStatus();
                Row row = controls.createRow(iIdx++);
                row.createCell(0).setCellValue(nullToEmpty(item.getNodeCode()));
                row.createCell(1).setCellValue(nullToEmpty(item.getTitle()));
                row.createCell(2).setCellValue(
                        item.getApplicability() != null ? item.getApplicability().name() : "");
                row.createCell(3).setCellValue(effective != null ? effective.name() : "");
                row.createCell(4).setCellValue(item.getScore() != null ? item.getScore().doubleValue() : 0);
            }
            autoSize(controls, 5);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build UAE IA Compliance Excel", e);
        }
    }

    /** §19.3 #06 NCEMA V3 Compliance: pillar scores + control detail. */
    public byte[] writeNcemaCompliance(GapAssessment assessment, GapLayerScoreSummary layers,
                                       List<GapItem> items) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet cover = workbook.createSheet("Cover");
            writeHeader(cover, new String[]{"Field", "Value"});
            int c = 1;
            cover.createRow(c++).createCell(0).setCellValue("Assessment");
            cover.getRow(c - 1).createCell(1).setCellValue(nullToEmpty(assessment.getName()));
            cover.createRow(c++).createCell(0).setCellValue("Framework");
            cover.getRow(c - 1).createCell(1).setCellValue(
                    nullToEmpty(assessment.getFrameworkName()) + " ("
                            + nullToEmpty(assessment.getFrameworkCode()) + ")");
            cover.createRow(c).createCell(0).setCellValue("Overall score %");
            cover.getRow(c).createCell(1).setCellValue(
                    assessment.getOverallScore() != null ? assessment.getOverallScore().doubleValue() : 0);
            autoSize(cover, 2);

            Sheet pillars = workbook.createSheet("Pillar scores");
            writeHeader(pillars, new String[]{"Code", "Pillar", "Score %", "RAG", "Applicable", "Scored"});
            int pIdx = 1;
            if (layers != null && layers.domains() != null) {
                for (GapLayerScoreSummary.DomainScore d : layers.domains()) {
                    Row row = pillars.createRow(pIdx++);
                    row.createCell(0).setCellValue(nullToEmpty(d.code()));
                    row.createCell(1).setCellValue(nullToEmpty(d.name()));
                    row.createCell(2).setCellValue(d.score() != null ? d.score().doubleValue() : 0);
                    row.createCell(3).setCellValue(nullToEmpty(d.rag()));
                    row.createCell(4).setCellValue(d.applicableCount());
                    row.createCell(5).setCellValue(d.scoredCount());
                }
            }
            autoSize(pillars, 6);

            Sheet controls = workbook.createSheet("Control scores");
            writeHeader(controls, new String[]{"Code", "Title", "Applicability", "Final status", "Score"});
            int iIdx = 1;
            for (GapItem item : items) {
                if (item.getApplicability() == com.a9.arbor.grc.modules.gap.domain.GapApplicability.NOT_APPLICABLE) {
                    continue;
                }
                var effective = item.getFinalImplementationStatus() != null
                        ? item.getFinalImplementationStatus()
                        : item.getImplementationStatus();
                Row row = controls.createRow(iIdx++);
                row.createCell(0).setCellValue(nullToEmpty(item.getNodeCode()));
                row.createCell(1).setCellValue(nullToEmpty(item.getTitle()));
                row.createCell(2).setCellValue(
                        item.getApplicability() != null ? item.getApplicability().name() : "");
                row.createCell(3).setCellValue(effective != null ? effective.name() : "");
                row.createCell(4).setCellValue(item.getScore() != null ? item.getScore().doubleValue() : 0);
            }
            autoSize(controls, 5);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build NCEMA Compliance Excel", e);
        }
    }

    private static void writeLayerRow(Sheet sheet, int rowIdx, String label,
                                      GapLayerScoreSummary.LayerScore layer) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(label);
        if (layer == null) {
            row.createCell(1).setCellValue("");
            row.createCell(2).setCellValue("");
            row.createCell(3).setCellValue(0);
            row.createCell(4).setCellValue(0);
            return;
        }
        row.createCell(1).setCellValue(layer.score() != null ? layer.score().doubleValue() : 0);
        row.createCell(2).setCellValue(nullToEmpty(layer.rag()));
        row.createCell(3).setCellValue(layer.applicableCount());
        row.createCell(4).setCellValue(layer.scoredCount());
    }

    private static void writeHeader(Sheet sheet, String[] headers) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }
    }

    private static void autoSize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
