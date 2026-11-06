package com.a9.arbor.grc.modules.bcms.service;

import com.a9.arbor.grc.modules.bcms.domain.BiaCycle;
import com.a9.arbor.grc.modules.bcms.domain.BiaImpactCell;
import com.a9.arbor.grc.modules.bcms.domain.BiaMor;
import com.a9.arbor.grc.modules.bcms.domain.BiaProcess;
import com.a9.arbor.grc.modules.bcms.pack.BiaImpactEngine;
import com.a9.arbor.grc.modules.bcms.pack.BiaLookups;
import com.a9.arbor.grc.modules.bcms.web.dto.BiaImpactCellRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.SaveBiaImpactRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.SaveBiaMorRequest;
import com.a9.arbor.grc.modules.bcms.web.dto.UpsertBiaProcessRequest;
import com.a9.arbor.grc.modules.export.ExportFile;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/** Pack-shaped BIA workbook: Cover, Dashboard, BIA, Impact Analysis, MOR, Reference. */
@Component
public class BiaWorkbookWriter {

    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final String[] BIA_HEADERS = {
            "Process Code", "Business Process Name", "Process Description",
            "Process Flow - Trigger / Inflow",
            "Process Flow - Key Steps / Activities Performed by the Organization",
            "Process Flow - Output / Outflow",
            "Activities Performed by Third Party (if any)", "Third Party / Supplier Name",
            "Process Owner Org Unit Id", "Process Frequency", "Peak / Most Critical Period",
            "Dependency - Minimum Staff (roles & numbers)",
            "Dependency - Premises / Facilities",
            "Dependency - IT Systems & Applications",
            "Dependency - Equipment",
            "Dependency - Vital Records / Data",
            "Dependency - Third Party / Suppliers",
            "Peak Impact Score (auto)", "Criticality Tier # (auto)", "Criticality Category (auto)",
            "Recovery Time Objective - RTO (auto)", "Maximum Acceptable Outage - MAO (auto)",
            "Recovery Priority (auto)", "Critical Process Rank (auto)", "RTO Justification (auto)",
            "Minimum Operating Requirements Applicable? (auto)",
            "Related Risk Code (if applicable)", "Additional Justification / Notes (optional)",
            "Completeness Check (auto)", "Submitted By (BC Champion)", "Submission Date",
            "Specialist Review Outcome", "Reviewer Name (BC Specialist)", "Review Date",
            "Reviewer Comments / Reason for Return"
    };

    public interface ImportSink {
        void accept(long cycleId, UpsertBiaProcessRequest process, SaveBiaImpactRequest impact, SaveBiaMorRequest mor);
    }

    public ExportFile template() {
        return new ExportFile(write(null, List.of(), id -> List.of(), id -> null), "bia-template.xlsx", CONTENT_TYPE);
    }

    public ExportFile export(BiaCycle cycle,
                             List<BiaProcess> processes,
                             Function<Long, List<BiaImpactCell>> impactFn,
                             Function<Long, BiaMor> morFn) {
        String name = cycle != null ? cycle.getDisplayId().replaceAll("[^a-zA-Z0-9._-]+", "-") : "bia";
        return new ExportFile(write(cycle, processes, impactFn, morFn), name + ".xlsx", CONTENT_TYPE);
    }

    public void importInto(long cycleId, byte[] bytes, ImportSink sink) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet bia = requireSheet(workbook, "BIA");
            rejectOldBands(workbook);
            Map<String, Integer> col = mapHeader(bia.getRow(0));
            requireCol(col, "Process Code");
            requireCol(col, "Business Process Name");
            DataFormatter fmt = new DataFormatter();
            Sheet impactSheet = workbook.getSheet("Impact Analysis");
            Sheet morSheet = workbook.getSheet("MOR");
            Map<String, Integer> impactCol = impactSheet != null && impactSheet.getRow(0) != null
                    ? mapHeader(impactSheet.getRow(0)) : Map.of();
            Map<String, Integer> morCol = morSheet != null && morSheet.getRow(0) != null
                    ? mapHeader(morSheet.getRow(0)) : Map.of();

            for (int i = 1; i <= bia.getLastRowNum(); i++) {
                Row row = bia.getRow(i);
                if (row == null || isBlankRow(row, fmt)) {
                    continue;
                }
                String code = cell(row, col.get("Process Code"), fmt);
                String name = cell(row, col.get("Business Process Name"), fmt);
                if (code == null || code.isBlank() || name == null || name.isBlank()) {
                    continue;
                }
                UpsertBiaProcessRequest process = new UpsertBiaProcessRequest(
                        code, name,
                        cell(row, col.get("Process Description"), fmt),
                        cell(row, col.get("Process Flow - Trigger / Inflow"), fmt),
                        cell(row, col.get("Process Flow - Key Steps / Activities Performed by the Organization"), fmt),
                        cell(row, col.get("Process Flow - Output / Outflow"), fmt),
                        cell(row, col.get("Activities Performed by Third Party (if any)"), fmt),
                        cell(row, col.get("Third Party / Supplier Name"), fmt),
                        parseLong(cell(row, col.get("Process Owner Org Unit Id"), fmt)),
                        parseLong(cell(row, col.get("Process Owner User Id"), fmt)),
                        cell(row, col.get("Process Frequency"), fmt),
                        cell(row, col.get("Peak / Most Critical Period"), fmt),
                        cell(row, col.get("Dependency - Minimum Staff (roles & numbers)"), fmt),
                        cell(row, col.get("Dependency - Premises / Facilities"), fmt),
                        cell(row, col.get("Dependency - IT Systems & Applications"), fmt),
                        cell(row, col.get("Dependency - Equipment"), fmt),
                        cell(row, col.get("Dependency - Vital Records / Data"), fmt),
                        cell(row, col.get("Dependency - Third Party / Suppliers"), fmt),
                        cell(row, col.get("Related Risk Code (if applicable)"), fmt),
                        cell(row, col.get("Additional Justification / Notes (optional)"), fmt),
                        null, null);
                SaveBiaImpactRequest impact = readImpact(impactSheet, impactCol, code, fmt);
                SaveBiaMorRequest mor = readMor(morSheet, morCol, code, fmt);
                sink.accept(cycleId, process, impact, mor);
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read workbook");
        }
    }

    private byte[] write(BiaCycle cycle,
                         List<BiaProcess> processes,
                         Function<Long, List<BiaImpactCell>> impactFn,
                         Function<Long, BiaMor> morFn) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeCover(workbook, cycle);
            writeDashboard(workbook, processes, morFn);
            Sheet bia = workbook.createSheet("BIA");
            header(bia, BIA_HEADERS);
            int r = 1;
            for (BiaProcess p : processes) {
                Row row = bia.createRow(r++);
                int c = 0;
                set(row, c++, p.getProcessCode());
                set(row, c++, p.getProcessName());
                set(row, c++, p.getDescription());
                set(row, c++, p.getFlowTrigger());
                set(row, c++, p.getFlowSteps());
                set(row, c++, p.getFlowOutput());
                set(row, c++, p.getThirdPartyActivities());
                set(row, c++, p.getSupplierName());
                if (p.getOwnerOrgUnitId() != null) {
                    row.createCell(c++).setCellValue(p.getOwnerOrgUnitId());
                } else {
                    c++;
                }
                set(row, c++, p.getFrequency());
                set(row, c++, p.getPeakPeriod());
                set(row, c++, p.getDepStaff());
                set(row, c++, p.getDepPremises());
                set(row, c++, p.getDepIt());
                set(row, c++, p.getDepEquipment());
                set(row, c++, p.getDepRecords());
                set(row, c++, p.getDepThirdParty());
                setNum(row, c++, p.getPeakScore());
                setNum(row, c++, p.getCriticalityTier());
                set(row, c++, p.getCategoryLabel());
                setNum(row, c++, p.getRtoHours());
                setNum(row, c++, p.getMaoHours());
                setNum(row, c++, p.getRecoveryPriority());
                setNum(row, c++, p.getCriticalRank());
                set(row, c++, p.getRtoCaption());
                set(row, c++, p.isMorRequired() ? "Yes - complete MOR sheet" : "Not required - Normal tier");
                set(row, c++, p.getRelatedRiskCode());
                set(row, c++, p.getChampionNotes());
                set(row, c++, p.getCompletenessLabel());
                set(row, c++, p.getSubmittedByUserId() != null ? String.valueOf(p.getSubmittedByUserId()) : "");
                set(row, c++, p.getSubmittedAt() != null ? p.getSubmittedAt().toString() : "");
                set(row, c++, p.getReviewOutcome() != null ? p.getReviewOutcome().display() : "");
                set(row, c++, p.getReviewerName());
                set(row, c++, p.getReviewDate() != null ? p.getReviewDate().toString() : "");
                set(row, c++, p.getReviewComment());
            }

            Sheet impact = workbook.createSheet("Impact Analysis");
            header(impact, new String[]{
                    "Process Code", "Business Process Name", "Impact Category",
                    "Impact if unavailable for 0-4 Hours (1-5)",
                    "Impact if unavailable for 4-8 Hours (1-5)",
                    "Impact if unavailable for 8-24 Hrs (1-5)",
                    "Impact if unavailable for 24-48 hrs 1-2 days (1-5)",
                    "Impact if unavailable for 48 - 72 hrs - 2-3 Days (1-5)",
                    "Peak Impact for this Category (auto)", "Remarks"
            });
            int ir = 1;
            for (BiaProcess p : processes) {
                List<BiaImpactCell> cells = impactFn.apply(p.getId());
                for (BiaImpactEngine.Category cat : BiaImpactEngine.Category.values()) {
                    Row row = impact.createRow(ir++);
                    set(row, 0, p.getProcessCode());
                    set(row, 1, p.getProcessName());
                    set(row, 2, title(cat.name()));
                    Integer peak = null;
                    String remarks = null;
                    for (BiaImpactEngine.Band band : BiaImpactEngine.Band.values()) {
                        BiaImpactCell cell = cells.stream()
                                .filter(x -> x.getCategory() == cat && x.getBand() == band)
                                .findFirst().orElse(null);
                        int col = 3 + band.ordinal();
                        if (cell != null && cell.getRating() != null) {
                            row.createCell(col).setCellValue(cell.getRating());
                            peak = peak == null ? cell.getRating() : Math.max(peak, cell.getRating());
                        }
                        if (cell != null && cell.getRemarks() != null && !cell.getRemarks().isBlank()) {
                            remarks = cell.getRemarks();
                        }
                    }
                    setNum(row, 8, peak);
                    set(row, 9, remarks);
                }
            }

            Sheet mor = workbook.createSheet("MOR");
            header(mor, new String[]{
                    "Process Code", "Business Process Name", "Criticality Category",
                    "Recovery Time Objective - RTO", "Maximum Acceptable Outage - MAO", "MOR Required?",
                    "Minimum Operating Capacity Required %",
                    "Minimum Staff Required During Disruption (number)",
                    "Minimum People & Skills Requirement (roles / named staff / backups)",
                    "Minimum Technology & Systems Requirement",
                    "Minimum Premises / Workspace Requirement",
                    "Minimum Equipment Requirement",
                    "Minimum Vital Records / Data Requirement",
                    "Minimum Third Party / Supplier Support (optional)",
                    "Recovery Strategy",
                    "Workaround / Manual Procedure During Disruption (optional)",
                    "MOR Completeness (auto)"
            });
            int mr = 1;
            for (BiaProcess p : processes) {
                BiaMor m = morFn.apply(p.getId());
                Row row = mor.createRow(mr++);
                set(row, 0, p.getProcessCode());
                set(row, 1, p.getProcessName());
                set(row, 2, p.getCategoryLabel());
                setNum(row, 3, p.getRtoHours());
                setNum(row, 4, p.getMaoHours());
                set(row, 5, p.isMorRequired() ? "Yes - Critical" : "No - Normal tier");
                if (m != null) {
                    setNum(row, 6, m.getCapacityPct());
                    setNum(row, 7, m.getMinStaff());
                    set(row, 8, m.getPeople());
                    set(row, 9, m.getTechnology());
                    set(row, 10, m.getPremises());
                    set(row, 11, m.getEquipment());
                    set(row, 12, m.getRecordsText());
                    set(row, 13, m.getThirdPartySupport());
                    set(row, 14, m.getRecoveryStrategy());
                    set(row, 15, m.getWorkaround());
                    set(row, 16, p.isMorRequired() ? (p.isCompletenessOk() ? "Complete" : "Incomplete - MOR fields outstanding")
                            : "Not applicable - Normal tier");
                } else {
                    set(row, 16, p.isMorRequired() ? "Incomplete - MOR fields outstanding" : "Not applicable - Normal tier");
                }
            }

            Sheet ref = workbook.createSheet("Reference");
            ref.createRow(0).createCell(0).setCellValue("Impact scale");
            int rr = 1;
            for (BiaLookups.ImpactScale s : BiaLookups.IMPACT_SCALE) {
                Row row = ref.createRow(rr++);
                row.createCell(0).setCellValue(s.rating());
                row.createCell(1).setCellValue(s.level());
                row.createCell(2).setCellValue(s.definition());
            }
            rr++;
            Row fh = ref.createRow(rr++);
            fh.createCell(0).setCellValue("Process Frequency");
            fh.createCell(1).setCellValue("Recovery Strategy");
            fh.createCell(2).setCellValue("Specialist Review Outcome");
            int max = Math.max(BiaLookups.FREQUENCIES.size(),
                    Math.max(BiaLookups.RECOVERY_STRATEGIES.size(), BiaLookups.REVIEW_OUTCOMES.size()));
            for (int i = 0; i < max; i++) {
                Row row = ref.createRow(rr++);
                if (i < BiaLookups.FREQUENCIES.size()) {
                    row.createCell(0).setCellValue(BiaLookups.FREQUENCIES.get(i));
                }
                if (i < BiaLookups.RECOVERY_STRATEGIES.size()) {
                    row.createCell(1).setCellValue(BiaLookups.RECOVERY_STRATEGIES.get(i));
                }
                if (i < BiaLookups.REVIEW_OUTCOMES.size()) {
                    row.createCell(2).setCellValue(BiaLookups.REVIEW_OUTCOMES.get(i));
                }
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build BIA workbook", e);
        }
    }

    private void writeCover(XSSFWorkbook workbook, BiaCycle cycle) {
        Sheet s = workbook.createSheet("Cover");
        s.createRow(0).createCell(0).setCellValue("BUSINESS IMPACT ANALYSIS (BIA)");
        if (cycle != null) {
            rowKV(s, 2, "Department org unit id", String.valueOf(cycle.getOrgUnitId()));
            rowKV(s, 3, "BIA Reference Number", cycle.getDisplayId());
            rowKV(s, 4, "Issue Date", cycle.getIssueDate() != null ? cycle.getIssueDate().toString() : "");
            rowKV(s, 5, "Next Review Date", cycle.getNextReviewDate() != null ? cycle.getNextReviewDate().toString() : "");
            rowKV(s, 6, "Version", cycle.getVersionLabel());
            rowKV(s, 7, "Status", cycle.getStatus() != null ? cycle.getStatus().name() : "");
            rowKV(s, 9, "Objective", cycle.getObjective());
            rowKV(s, 10, "Scope", cycle.getScopeText());
            rowKV(s, 11, "MBCO", cycle.getMbco());
        }
    }

    private void writeDashboard(XSSFWorkbook workbook, List<BiaProcess> processes, Function<Long, BiaMor> morFn) {
        Sheet s = workbook.createSheet("Dashboard");
        int inScope = 0;
        int critical = 0;
        int normal = 0;
        int[] tiers = new int[6];
        for (BiaProcess p : processes) {
            inScope++;
            Integer t = p.getCriticalityTier();
            if (t == null) {
                continue;
            }
            if (t <= 3) {
                critical++;
            } else {
                normal++;
            }
            if (t >= 1 && t <= 5) {
                tiers[t]++;
            }
        }
        rowKV(s, 0, "Processes in scope", String.valueOf(inScope));
        rowKV(s, 1, "Critical processes (Tier 1-3)", String.valueOf(critical));
        rowKV(s, 2, "Normal processes (Tier 4-5)", String.valueOf(normal));
        String[] captions = {"", "RTO 3 h / MAO 4 h", "RTO 6 h / MAO 8 h", "RTO 18 h / MAO 24 h",
                "RTO 36 h / MAO 48 h", "RTO 54 h / MAO 72 h"};
        for (int t = 1; t <= 5; t++) {
            Row row = s.createRow(3 + t);
            row.createCell(0).setCellValue(BiaLookups.categoryLabel(t));
            row.createCell(1).setCellValue(captions[t]);
            row.createCell(2).setCellValue(tiers[t]);
        }
        Row head = s.createRow(10);
        String[] h = {"Recovery Order", "Business Process", "Criticality Category", "RTO", "MAO",
                "Min. Operating Capacity", "Min. Staff", "Recovery Strategy"};
        for (int i = 0; i < h.length; i++) {
            head.createCell(i).setCellValue(h[i]);
        }
        int r = 11;
        List<BiaProcess> ranked = processes.stream()
                .filter(p -> p.getCriticalRank() != null)
                .sorted((a, b) -> Integer.compare(a.getCriticalRank(), b.getCriticalRank()))
                .toList();
        for (BiaProcess p : ranked) {
            BiaMor m = morFn.apply(p.getId());
            Row row = s.createRow(r++);
            row.createCell(0).setCellValue(p.getCriticalRank());
            set(row, 1, p.getProcessName());
            set(row, 2, p.getCategoryLabel());
            setNum(row, 3, p.getRtoHours());
            setNum(row, 4, p.getMaoHours());
            if (m != null) {
                setNum(row, 5, m.getCapacityPct());
                setNum(row, 6, m.getMinStaff());
                set(row, 7, m.getRecoveryStrategy());
            }
        }
    }

    private SaveBiaImpactRequest readImpact(Sheet sheet, Map<String, Integer> col, String code, DataFormatter fmt) {
        if (sheet == null || col.isEmpty()) {
            return new SaveBiaImpactRequest(List.of());
        }
        List<BiaImpactCellRequest> cells = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            if (!code.equalsIgnoreCase(cell(row, col.get("Process Code"), fmt))) {
                continue;
            }
            String catName = cell(row, col.get("Impact Category"), fmt);
            if (catName == null) {
                continue;
            }
            BiaImpactEngine.Category cat = parseCategoryLabel(catName);
            String[] bandHeaders = {
                    "Impact if unavailable for 0-4 Hours (1-5)",
                    "Impact if unavailable for 4-8 Hours (1-5)",
                    "Impact if unavailable for 8-24 Hrs (1-5)",
                    "Impact if unavailable for 24-48 hrs 1-2 days (1-5)",
                    "Impact if unavailable for 48 - 72 hrs - 2-3 Days (1-5)"
            };
            String remarks = cell(row, col.get("Remarks"), fmt);
            BiaImpactEngine.Band[] bands = BiaImpactEngine.Band.values();
            for (int b = 0; b < bands.length; b++) {
                Integer rating = parseInt(cell(row, col.get(bandHeaders[b]), fmt));
                cells.add(new BiaImpactCellRequest(cat.name(), bands[b].name(), rating, remarks));
            }
        }
        return new SaveBiaImpactRequest(cells);
    }

    private SaveBiaMorRequest readMor(Sheet sheet, Map<String, Integer> col, String code, DataFormatter fmt) {
        if (sheet == null || col.isEmpty()) {
            return null;
        }
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            if (!code.equalsIgnoreCase(cell(row, col.get("Process Code"), fmt))) {
                continue;
            }
            return new SaveBiaMorRequest(
                    parseInt(cell(row, col.get("Minimum Operating Capacity Required %"), fmt)),
                    parseInt(cell(row, col.get("Minimum Staff Required During Disruption (number)"), fmt)),
                    cell(row, col.get("Minimum People & Skills Requirement (roles / named staff / backups)"), fmt),
                    cell(row, col.get("Minimum Technology & Systems Requirement"), fmt),
                    cell(row, col.get("Minimum Premises / Workspace Requirement"), fmt),
                    cell(row, col.get("Minimum Equipment Requirement"), fmt),
                    cell(row, col.get("Minimum Vital Records / Data Requirement"), fmt),
                    cell(row, col.get("Minimum Third Party / Supplier Support (optional)"), fmt),
                    cell(row, col.get("Recovery Strategy"), fmt),
                    cell(row, col.get("Workaround / Manual Procedure During Disruption (optional)"), fmt),
                    null, null, null, null, null, null, null, null, null, null, null);
        }
        return null;
    }

    private void rejectOldBands(XSSFWorkbook workbook) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            Row header = sheet.getRow(0);
            if (header == null) {
                continue;
            }
            DataFormatter fmt = new DataFormatter();
            for (Cell cell : header) {
                String v = fmt.formatCellValue(cell).toLowerCase(Locale.ROOT);
                if (v.contains("0-12") || v.contains("12-48") || v.contains("2-7 day") || v.contains("14-30")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "This file uses the retired 0-12 / 12-48 / 2-7 day impact bands. "
                                    + "Use the 0-4 / 4-8 / 8-24 / 24-48 / 48-72 hour Impact Analysis layout.");
                }
            }
        }
    }

    private static BiaImpactEngine.Category parseCategoryLabel(String raw) {
        String n = raw.trim().toUpperCase(Locale.ROOT);
        for (BiaImpactEngine.Category c : BiaImpactEngine.Category.values()) {
            if (c.name().equals(n) || title(c.name()).equalsIgnoreCase(raw.trim())) {
                return c;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown impact category: " + raw);
    }

    private static String title(String enumName) {
        String lower = enumName.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static Sheet requireSheet(XSSFWorkbook workbook, String name) {
        Sheet sheet = workbook.getSheet(name);
        if (sheet == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workbook is missing sheet '" + name + "'");
        }
        return sheet;
    }

    private static void header(Sheet sheet, String[] headers) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            row.createCell(i).setCellValue(headers[i]);
        }
    }

    private static void rowKV(Sheet sheet, int r, String k, String v) {
        Row row = sheet.createRow(r);
        row.createCell(0).setCellValue(k);
        row.createCell(1).setCellValue(v != null ? v : "");
    }

    private static void set(Row row, int col, String v) {
        if (v != null) {
            row.createCell(col).setCellValue(v);
        }
    }

    private static void setNum(Row row, int col, Integer v) {
        if (v != null) {
            row.createCell(col).setCellValue(v);
        }
    }

    private static Map<String, Integer> mapHeader(Row header) {
        Map<String, Integer> col = new LinkedHashMap<>();
        DataFormatter fmt = new DataFormatter();
        for (Cell cell : header) {
            String name = fmt.formatCellValue(cell).trim();
            if (!name.isEmpty()) {
                col.put(name, cell.getColumnIndex());
            }
        }
        return col;
    }

    private static void requireCol(Map<String, Integer> col, String name) {
        if (!col.containsKey(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing column: " + name);
        }
    }

    private static String cell(Row row, Integer idx, DataFormatter fmt) {
        if (idx == null) {
            return null;
        }
        Cell c = row.getCell(idx);
        if (c == null) {
            return null;
        }
        String v = fmt.formatCellValue(c).trim();
        return v.isEmpty() ? null : v;
    }

    private static Long parseLong(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(v.replace(".0", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseInt(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return (int) Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isBlankRow(Row row, DataFormatter fmt) {
        for (Cell cell : row) {
            if (!fmt.formatCellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }
}
