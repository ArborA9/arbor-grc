package com.a9.arbor.grc.modules.risk.service;

import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.risk.domain.RiskCategory;
import com.a9.arbor.grc.modules.risk.domain.RiskLevel;
import com.a9.arbor.grc.modules.risk.domain.RiskType;
import com.a9.arbor.grc.modules.risk.domain.TreatmentOption;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Arbor bulk-create templates for IS Risk / ERM Risk (§20.1).
 * Sheets: Instructions · Risks · Reference (dropdowns).
 */
@Component
public class RiskBulkImportWorkbook {

    public static final String SHEET_RISKS = "Risks";
    public static final String SHEET_REF = "Reference";
    public static final int MAX_ROWS = 1000;
    public static final String SAMPLE_RISK_ID = "SAMPLE";
    static final String SAMPLE_IGNORED_MESSAGE =
            "Sample row ignored. You can delete it or keep it. Arbor does not import Risk ID SAMPLE.";

    /** Same list as the create form: Config sheet categories plus Other. */
    static final String[] CATEGORIES = {
            "Strategic", "Operational", "Financial", "Compliance", "IT", "Reputational", "Other"
    };

    /** Portal create-form labels. Parser also accepts the older snake_case headers. */
    static final Map<RiskLevel, String> LEVEL_LABEL = Map.of(
            RiskLevel.VERY_LOW, "Very Low",
            RiskLevel.LOW, "Low",
            RiskLevel.MEDIUM, "Medium",
            RiskLevel.HIGH, "High",
            RiskLevel.VERY_HIGH, "Very High");

    static final Map<RiskLevel, String> CE_LABEL = Map.of(
            RiskLevel.VERY_LOW, "Ineffective",
            RiskLevel.LOW, "Weak",
            RiskLevel.MEDIUM, "Moderate",
            RiskLevel.HIGH, "Strong",
            RiskLevel.VERY_HIGH, "Very strong");

    public static final String[] IS_HEADERS = {
            "Risk ID",
            "Date Identified",
            "Risk Title",
            "Risk Description",
            "Gap / Observation",
            "Division / Department",
            "Control Ref",
            "Associated Threat",
            "Risk Owner",
            "Risk Custodian",
            "Likelihood (1-5)",
            "Impact (1-5)",
            "Existing Controls",
            "Control Effectiveness (1-5)",
            "Treatment Option",
            "Treatment Action Plan",
            "Evidence Required",
            "Target Date"
    };

    public static final String[] ERM_HEADERS = {
            "Risk ID",
            "Date Identified",
            "Risk Title",
            "Risk Description",
            "Gap / Observation",
            "Risk Category",
            "Specify category",
            "Division / Department",
            "Strategic Objective (Manual)",
            "Risk Cause / Source",
            "Specify cause",
            "Risk Owner",
            "Risk Custodian",
            "Likelihood (1-5)",
            "Impact (1-5)",
            "Existing Controls",
            "Control Effectiveness (1-5)",
            "Treatment Option",
            "Treatment Action Plan",
            "Evidence Required",
            "Target Date"
    };

    private static final String[] ERM_CAUSES = {
            "Economic Conditions", "Regulatory Change", "Technology Failure", "Supplier Failure",
            "Talent Shortage", "Process Breakdown", "Project Delivery", "Fraud / Corruption",
            "Natural Hazard", "Public Perception", "Contractual Dispute", "Capacity Constraint",
            "Other"
    };

    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public record ParsedRiskRow(
            int excelRow,
            String title,
            String strategicObjective,
            String category,
            String categoryOther,
            String orgUnitName,
            String probability,
            String impact,
            String controlEffectiveness,
            String description,
            String threats,
            String observedGap,
            String existingControls,
            String frameworkCode,
            String nodeCode,
            String financialImpactAed,
            String tprmVendorName,
            String riskOwnerEmail,
            String contributorEmail,
            String treatmentOption,
            String displayIdOverride,
            String overrideAutoClose,
            String autoCloseOverrideJustification,
            String dateIdentified,
            String cause,
            String causeOther,
            String treatmentJustification,
            String evidenceRequired,
            String targetDate
    ) {
    }

    public ExportFile buildTemplate(RiskType type,
                                    List<String> orgUnitNames,
                                    List<String> contributorEmails,
                                    List<String> ownerEmails,
                                    List<String> frameworkCodes,
                                    List<String> vendorNames,
                                    List<String> controlRefs,
                                    List<String> associatedThreats,
                                    String projectName,
                                    int gridSize,
                                    List<RiskLevel> scale) {
        boolean erm = type == RiskType.ERM;
        String likelihoodHeader = scaleHeader("Likelihood", gridSize);
        String impactHeader = scaleHeader("Impact", gridSize);
        String ceHeader = scaleHeader("Control Effectiveness", gridSize);
        String[] headers = java.util.Arrays.stream(erm ? ERM_HEADERS : IS_HEADERS)
                .map(h -> h.endsWith(" (1-5)") ? scaleHeader(h.substring(0, h.length() - 6), gridSize) : h)
                .toArray(String[]::new);
        List<String> levelNames = scale.stream().map(l -> LEVEL_LABEL.get(l)).toList();
        List<String> ceNames = scale.stream().map(l -> CE_LABEL.get(l)).toList();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet instructions = wb.createSheet("Instructions");
            writeInstructions(instructions, erm, projectName, likelihoodHeader, impactHeader, ceHeader,
                    levelNames, ceNames);

            Sheet risks = wb.createSheet(SHEET_RISKS);
            Row header = risks.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
                risks.setColumnWidth(i, 5200);
            }

            Sheet ref = wb.createSheet(SHEET_REF);
            writeReference(wb, ref, erm, orgUnitNames, contributorEmails, ownerEmails, frameworkCodes,
                    vendorNames, controlRefs, associatedThreats, levelNames, ceNames);
            addValidations(wb, risks, headers, erm,
                    orgUnitNames.size(), contributorEmails.size(), ownerEmails.size(),
                    associatedThreats.size(), controlRefs.size(), likelihoodHeader, impactHeader, ceHeader);

            Row sample = risks.createRow(1);
            CellStyle sampleStyle = wb.createCellStyle();
            sampleStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            sampleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            for (int i = 0; i < headers.length; i++) {
                sample.createCell(i).setCellStyle(sampleStyle);
            }
            sample.getCell(col(headers, "Risk ID")).setCellValue(SAMPLE_RISK_ID);
            sample.getCell(col(headers, "Risk Title")).setCellValue("Sample risk (ignored on upload)");
            sample.getCell(col(headers, "Risk Description"))
                    .setCellValue("Example only. Delete this row or keep it. Arbor ignores Risk ID SAMPLE.");
            sample.getCell(col(headers, "Date Identified")).setCellValue(java.time.LocalDate.now().toString());
            if (!orgUnitNames.isEmpty()) {
                sample.getCell(col(headers, "Division / Department")).setCellValue(orgUnitNames.get(0));
            }
            if (!ownerEmails.isEmpty()) {
                sample.getCell(col(headers, "Risk Owner")).setCellValue(ownerEmails.get(0));
            }
            int mid = scale.size() / 2;
            sample.getCell(col(headers, likelihoodHeader)).setCellValue(levelNames.get(mid));
            sample.getCell(col(headers, impactHeader)).setCellValue(levelNames.get(Math.max(0, scale.size() - 2)));
            sample.getCell(col(headers, ceHeader)).setCellValue(ceNames.get(mid));
            sample.getCell(col(headers, "Treatment Option")).setCellValue("Treat (Mitigate)");
            sample.getCell(col(headers, "Treatment Action Plan"))
                    .setCellValue("Patch production access reviews this quarter.");
            if (erm) {
                sample.getCell(col(headers, "Gap / Observation")).setCellValue("No approved launch plan.");
                sample.getCell(col(headers, "Risk Category")).setCellValue("Strategic");
                sample.getCell(col(headers, "Strategic Objective (Manual)")).setCellValue("Grow market share");
                sample.getCell(col(headers, "Risk Cause / Source")).setCellValue("Process Breakdown");
            } else {
                sample.getCell(col(headers, "Gap / Observation")).setCellValue("Access reviews are not complete.");
                if (!controlRefs.isEmpty()) {
                    sample.getCell(col(headers, "Control Ref")).setCellValue(controlRefs.get(0));
                }
                sample.getCell(col(headers, "Associated Threat")).setCellValue("Operational Failure");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            String filename = erm ? "erm-risk-bulk-template.xlsx" : "is-risk-bulk-template.xlsx";
            return new ExportFile(out.toByteArray(), filename, CONTENT_TYPE);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not build template");
        }
    }

    public List<ParsedRiskRow> parse(byte[] bytes, RiskType type) {
        boolean erm = type == RiskType.ERM;
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet(SHEET_RISKS);
            if (sheet == null) {
                sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            }
            if (sheet == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workbook has no sheets");
            }
            Row header = sheet.getRow(0);
            if (header == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing header row on Risks sheet");
            }
            Map<String, Integer> col = mapHeader(header);
            requireColumns(col, erm);
            DataFormatter fmt = new DataFormatter();
            List<ParsedRiskRow> out = new ArrayList<>();
            int last = sheet.getLastRowNum();
            for (int i = 1; i <= last; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row, fmt, Math.max(header.getLastCellNum(), 8))) {
                    continue;
                }
                out.add(new ParsedRiskRow(
                        i + 1,
                        cell(row, col.get("title"), fmt),
                        erm ? cell(row, col.get("strategic_objective"), fmt) : null,
                        cell(row, col.get("category"), fmt),
                        cell(row, col.get("category_other"), fmt),
                        cell(row, col.get("org_unit_name"), fmt),
                        cell(row, first(col, "probability", "likelihood"), fmt),
                        cell(row, col.get("impact"), fmt),
                        cell(row, col.get("control_effectiveness"), fmt),
                        cell(row, first(col, "description", "risk_description"), fmt),
                        cell(row, first(col, "threats", "associated_threat"), fmt),
                        cell(row, first(col, "observed_gap", "gap_observation"), fmt),
                        cell(row, col.get("existing_controls"), fmt),
                        erm ? null : cell(row, col.get("framework_code"), fmt),
                        erm ? null : cell(row, first(col, "node_code", "control_ref"), fmt),
                        erm ? cell(row, col.get("financial_impact_aed"), fmt) : null,
                        erm ? cell(row, col.get("tprm_vendor_name"), fmt) : null,
                        cell(row, col.get("risk_owner_email"), fmt),
                        cell(row, col.get("contributor_email"), fmt),
                        cell(row, col.get("treatment_option"), fmt),
                        cell(row, col.get("display_id_override"), fmt),
                        cell(row, col.get("override_auto_close"), fmt),
                        cell(row, col.get("auto_close_override_justification"), fmt),
                        cell(row, col.get("date_identified"), fmt),
                        erm ? cell(row, col.get("cause"), fmt) : null,
                        erm ? cell(row, col.get("cause_other"), fmt) : null,
                        cell(row, col.get("treatment_justification"), fmt),
                        cell(row, col.get("evidence_required"), fmt),
                        cell(row, col.get("target_date"), fmt)
                ));
            }
            if (out.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No data rows to import");
            }
            if (out.size() > MAX_ROWS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "At most " + MAX_ROWS + " rows per import (found " + out.size() + ")");
            }
            return out;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Excel file");
        }
    }

    private static void requireColumns(Map<String, Integer> col, boolean erm) {
        List<String> missing = new ArrayList<>();
        if (!col.containsKey("title")) missing.add("Risk Title");
        if (!col.containsKey("org_unit_name")) missing.add("Division / Department");
        if (!col.containsKey("probability") && !col.containsKey("likelihood")) {
            missing.add("Likelihood");
        }
        if (!col.containsKey("impact")) missing.add("Impact");
        if (!col.containsKey("control_effectiveness")) {
            missing.add("Control Effectiveness");
        }
        if (erm && !col.containsKey("strategic_objective")) missing.add("Strategic Objective (Manual)");
        if (erm && !col.containsKey("category")) missing.add("Risk Category");
        if (!erm && !col.containsKey("description") && !col.containsKey("risk_description")) {
            missing.add("Risk Description");
        }
        if (!erm && !col.containsKey("observed_gap") && !col.containsKey("gap_observation")) {
            missing.add("Gap / Observation");
        }
        if (!erm && !col.containsKey("threats") && !col.containsKey("associated_threat")) {
            missing.add("Associated Threat");
        }
        if (!erm && !col.containsKey("risk_owner_email")) missing.add("Risk Owner");
        if (!erm && !col.containsKey("date_identified")) missing.add("Date Identified");
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Missing column '" + missing.get(0) + "': use the Arbor "
                            + (erm ? "ERM" : "IS") + " risk template. Headers must match the portal.");
        }
    }

    private static void writeInstructions(Sheet sheet, boolean erm, String projectName,
                                          String likelihoodHeader, String impactHeader, String ceHeader,
                                          List<String> levelNames, List<String> ceNames) {
        String scored = likelihoodHeader + ", " + impactHeader + ", " + ceHeader;
        List<String> lines = new ArrayList<>();
        lines.add(erm ? "Arbor GRC: ERM Risk bulk create template" : "Arbor GRC: IS Risk bulk create template");
        if (projectName != null && !projectName.isBlank()) {
            lines.add("Project: " + projectName + ". Upload adds new rows to this project. Existing risks stay.");
        } else {
            lines.add("Upload adds new rows. Existing risks are kept. Create-only: no updates or deletes.");
        }
        lines.add("1. Fill the Risks sheet (one row = one new risk). Column names match the portal. Do not rename them.");
        lines.add("2. Use dropdowns on the Reference sheet where provided.");
        if (erm) {
            lines.add("3. Required: Date Identified, Risk Title, Risk Description, Gap / Observation, Risk Category, Division / Department, Strategic Objective (Manual), Risk Cause / Source, Risk Owner, " + scored + ", Treatment Action Plan.");
        } else {
            lines.add("3. Required: Date Identified, Risk Title, Risk Description, Gap / Observation, Division / Department, Associated Threat, Risk Owner, " + scored + ", Treatment Action Plan.");
        }
        lines.add("4. Risk ID is optional. Leave blank for the next ERM-YYYY-NNN or ISR-YYYY-NNN. A filled Risk ID must be unique.");
        lines.add("Note: the grey row with Risk ID SAMPLE is an example. Delete it or keep it. Arbor ignores it on upload.");
        lines.add("5. Risk Owner and Risk Custodian: pick the email from the Reference list (same people as the portal).");
        lines.add("6. Risk Custodian assigns after create (skipped if residual auto-closes).");
        lines.add("7. Valid rows are created; failed rows appear in the report. Max " + MAX_ROWS + " data rows.");
        lines.add("8. Your risk matrix is " + levelNames.size() + "x" + levelNames.size() + ". Likelihood and Impact: "
                + String.join(", ", levelNames) + " (lowest to highest). Control Effectiveness: "
                + String.join(", ", ceNames) + ". A number works too: 1 is the lowest, " + levelNames.size()
                + " the highest.");
        lines.add("9. Treatment Option: Treat (Mitigate), Tolerate (Accept), Terminate (Avoid), Transfer.");
        if (!erm) {
            lines.add("10. Associated Threat: pick from the Reference list.");
        }
        for (int i = 0; i < lines.size(); i++) {
            sheet.createRow(i).createCell(0).setCellValue(lines.get(i));
        }
        sheet.setColumnWidth(0, 20000);
    }

    private static void writeReference(XSSFWorkbook wb, Sheet ref, boolean erm,
                                       List<String> orgUnits,
                                       List<String> contributors,
                                       List<String> owners,
                                       List<String> frameworks,
                                       List<String> vendors,
                                       List<String> controlRefs,
                                       List<String> associatedThreats,
                                       List<String> levelNames,
                                       List<String> ceNames) {
        String[] headers = {
                "Division / Department", "Risk Custodian", "Risk Owner",
                "Risk Category", "Likelihood / Impact", "Treatment Option", "Framework", "Vendor",
                "Control Ref", "Associated Threat", "Risk Cause / Source", "Control Effectiveness"
        };
        Row h = ref.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            h.createCell(i).setCellValue(headers[i]);
            ref.setColumnWidth(i, 5500);
        }
        String[] cats = CATEGORIES;
        String[] levels = levelNames.toArray(String[]::new);
        String[] treatments = new String[]{"Treat (Mitigate)", "Tolerate (Accept)", "Terminate (Avoid)", "Transfer"};
        int max = Math.max(Math.max(orgUnits.size(), contributors.size()),
                Math.max(owners.size(), Math.max(cats.length, Math.max(levels.length,
                        Math.max(treatments.length, Math.max(frameworks.size(),
                                Math.max(vendors.size(), Math.max(controlRefs.size(),
                                        Math.max(associatedThreats.size(), ERM_CAUSES.length)))))))));
        for (int i = 0; i < max; i++) {
            Row r = ref.createRow(i + 1);
            if (i < orgUnits.size()) r.createCell(0).setCellValue(orgUnits.get(i));
            if (i < contributors.size()) r.createCell(1).setCellValue(contributors.get(i));
            if (i < owners.size()) r.createCell(2).setCellValue(owners.get(i));
            if (i < cats.length) r.createCell(3).setCellValue(cats[i]);
            if (i < levels.length) r.createCell(4).setCellValue(levels[i]);
            if (i < treatments.length) r.createCell(5).setCellValue(treatments[i]);
            if (i < frameworks.size()) r.createCell(6).setCellValue(frameworks.get(i));
            if (i < vendors.size()) r.createCell(7).setCellValue(vendors.get(i));
            if (i < controlRefs.size()) r.createCell(8).setCellValue(controlRefs.get(i));
            if (i < associatedThreats.size()) r.createCell(9).setCellValue(associatedThreats.get(i));
            if (erm && i < ERM_CAUSES.length) r.createCell(10).setCellValue(ERM_CAUSES[i]);
            if (i < ceNames.size()) r.createCell(11).setCellValue(ceNames.get(i));
        }
        defineListName(wb, "OrgUnits", 0, orgUnits.size());
        defineListName(wb, "Contributors", 1, contributors.size());
        defineListName(wb, "Owners", 2, owners.size());
        defineListName(wb, "Categories", 3, cats.length);
        defineListName(wb, "Levels", 4, levels.length);
        defineListName(wb, "CeLevels", 11, ceNames.size());
        defineListName(wb, "Treatments", 5, treatments.length);
        defineListName(wb, "ControlRefs", 8, controlRefs.size());
        defineListName(wb, "AssociatedThreats", 9, associatedThreats.size());
        if (erm) {
            defineListName(wb, "Causes", 10, ERM_CAUSES.length);
        }
    }

    private static void defineListName(XSSFWorkbook wb, String name, int colIndex, int count) {
        if (count <= 0) {
            return;
        }
        Name n = wb.createName();
        n.setNameName(name);
        String col = excelCol(colIndex);
        n.setRefersToFormula(SHEET_REF + "!$" + col + "$2:$" + col + "$" + (count + 1));
    }

    private static String excelCol(int index) {
        return String.valueOf((char) ('A' + index));
    }

    private static void addValidations(XSSFWorkbook wb, Sheet risks, String[] headers, boolean erm,
                                       int orgCount, int contribCount, int ownerCount,
                                       int threatCount, int controlCount,
                                       String likelihoodHeader, String impactHeader, String ceHeader) {
        DataValidationHelper helper = risks.getDataValidationHelper();
        int lastDataRow = MAX_ROWS;
        addNamedValidation(helper, risks, headers, likelihoodHeader, "Levels", lastDataRow);
        addNamedValidation(helper, risks, headers, impactHeader, "Levels", lastDataRow);
        addNamedValidation(helper, risks, headers, ceHeader, "CeLevels", lastDataRow);
        addNamedValidation(helper, risks, headers, "Treatment Option", "Treatments", lastDataRow);
        if (orgCount > 0) {
            addNamedValidation(helper, risks, headers, "Division / Department", "OrgUnits", lastDataRow);
        }
        if (contribCount > 0) {
            addNamedValidation(helper, risks, headers, "Risk Custodian", "Contributors", lastDataRow);
        }
        if (ownerCount > 0) {
            addNamedValidation(helper, risks, headers, "Risk Owner", "Owners", lastDataRow);
        }
        if (erm) {
            addNamedValidation(helper, risks, headers, "Risk Category", "Categories", lastDataRow);
            addNamedValidation(helper, risks, headers, "Risk Cause / Source", "Causes", lastDataRow);
            return;
        }
        if (threatCount > 0) {
            addNamedValidation(helper, risks, headers, "Associated Threat", "AssociatedThreats", lastDataRow);
        }
        if (controlCount > 0) {
            addNamedValidation(helper, risks, headers, "Control Ref", "ControlRefs", lastDataRow);
        }
    }

    private static void addNamedValidation(DataValidationHelper helper, Sheet sheet, String[] headers,
                                           String header, String namedRange, int lastRow) {
        int c = col(headers, header);
        if (c < 0) return;
        DataValidationConstraint constraint = helper.createFormulaListConstraint(namedRange);
        CellRangeAddressList address = new CellRangeAddressList(1, lastRow, c, c);
        DataValidation validation = helper.createValidation(constraint, address);
        validation.setSuppressDropDownArrow(false);
        validation.setShowErrorBox(true);
        sheet.addValidationData(validation);
    }

    private static void addListValidation(DataValidationHelper helper, Sheet sheet, String[] headers,
                                          String header, String[] values, int lastRow) {
        int c = col(headers, header);
        if (c < 0) return;
        DataValidationConstraint constraint = helper.createExplicitListConstraint(values);
        CellRangeAddressList address = new CellRangeAddressList(1, lastRow, c, c);
        DataValidation validation = helper.createValidation(constraint, address);
        validation.setSuppressDropDownArrow(false);
        sheet.addValidationData(validation);
    }

    private static int col(String[] headers, String name) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].equals(name)) return i;
        }
        return -1;
    }

    private static Map<String, Integer> mapHeader(Row header) {
        DataFormatter fmt = new DataFormatter();
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Cell cell : header) {
            String v = fmt.formatCellValue(cell);
            if (v != null && !v.isBlank()) {
                map.put(canonicalHeader(v), cell.getColumnIndex());
            }
        }
        return map;
    }

    static String canonicalHeader(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT)
                .replace("(auto)", " ")
                .replace("(manual)", " ")
                .replace("(1-5)", " ")
                .replace("(4t)", " ")
                .replace("(email)", " ")
                .replace('/', ' ')
                .replace('-', ' ')
                .replace('_', ' ');
        s = s.replaceAll("\\s+", " ").trim();
        return switch (s) {
            case "risk title", "title" -> "title";
            case "risk description", "description" -> "description";
            case "gap observation", "observed gap", "observed_gap" -> "observed_gap";
            case "division department", "org unit name", "org_unit_name", "division" -> "org_unit_name";
            case "associated threat", "threats" -> "threats";
            case "control ref", "control clause reference", "node code", "node_code", "control id" -> "node_code";
            case "risk owner", "risk owner email", "risk_owner_email" -> "risk_owner_email";
            case "risk custodian", "contributor", "contributor email", "contributor_email" -> "contributor_email";
            case "likelihood", "probability" -> "probability";
            case "impact" -> "impact";
            case "control effectiveness", "control_effectiveness" -> "control_effectiveness";
            case "treatment option", "treatment option 4t", "treatment_option" -> "treatment_option";
            case "existing controls", "existing_controls" -> "existing_controls";
            case "date identified", "date_identified" -> "date_identified";
            case "strategic objective", "strategic_objective" -> "strategic_objective";
            case "category", "risk category" -> "category";
            case "specify category", "category other", "category_other" -> "category_other";
            case "risk cause", "risk cause source", "cause" -> "cause";
            case "specify cause", "cause other", "cause_other" -> "cause_other";
            case "treatment action plan", "treatment description", "treatment justification", "treatment_justification" -> "treatment_justification";
            case "evidence required", "evidence_required" -> "evidence_required";
            case "target date", "target_date" -> "target_date";
            case "risk id", "riskid", "display id override", "display_id_override" -> "display_id_override";
            case "framework code", "framework_code" -> "framework_code";
            case "financial impact aed", "financial_impact_aed" -> "financial_impact_aed";
            case "tprm vendor name", "tprm_vendor_name" -> "tprm_vendor_name";
            case "override auto close", "override_auto_close" -> "override_auto_close";
            case "auto close override justification", "auto_close_override_justification" -> "auto_close_override_justification";
            default -> s.replace(' ', '_');
        };
    }

    private static Integer first(Map<String, Integer> col, String... keys) {
        for (String k : keys) {
            Integer idx = col.get(k);
            if (idx != null) {
                return idx;
            }
        }
        return null;
    }

    private static String cell(Row row, Integer idx, DataFormatter fmt) {
        if (idx == null) return null;
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        String v = fmt.formatCellValue(cell);
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static boolean isBlankRow(Row row, DataFormatter fmt, int cols) {
        for (int i = 0; i < cols; i++) {
            Cell c = row.getCell(i);
            if (c != null) {
                String v = fmt.formatCellValue(c);
                if (v != null && !v.isBlank()) return false;
            }
        }
        return true;
    }

    static boolean isSampleRow(ParsedRiskRow row) {
        return row.displayIdOverride() != null && SAMPLE_RISK_ID.equalsIgnoreCase(row.displayIdOverride().trim());
    }

    /** Only the create-form categories; anything else is rejected. */
    static RiskCategory parseCategory(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String n = raw.trim();
        for (String allowed : CATEGORIES) {
            if (allowed.equalsIgnoreCase(n)) {
                return RiskCategory.valueOf(allowed.toUpperCase(Locale.ROOT));
            }
        }
        throw new IllegalArgumentException("Risk Category \"" + n + "\" is not valid. Use "
                + String.join(", ", CATEGORIES) + ".");
    }

    static String scaleHeader(String base, int gridSize) {
        return gridSize == 5 ? base + " (1-5)" : base;
    }

    /**
     * Resolves a Likelihood / Impact / Control Effectiveness cell against the tenant scale
     * (distinct matrix axis levels, lowest first). Accepts the screen names, the CE names and
     * a number meaning the position on that scale (on 3x3, 3 is the top level).
     */
    static RiskLevel parseLevel(String raw, List<RiskLevel> scale) {
        if (raw == null || raw.isBlank()) return null;
        String n = raw.trim().toUpperCase(Locale.ROOT).replace('-', ' ').replace('_', ' ')
                .replaceAll("\\s+", " ");
        if (n.endsWith(".0")) {
            n = n.substring(0, n.length() - 2);
        }
        if (n.matches("\\d+")) {
            int k = Integer.parseInt(n);
            if (k < 1 || k > scale.size()) {
                throw new IllegalArgumentException("out of range");
            }
            return scale.get(k - 1);
        }
        RiskLevel level = null;
        for (RiskLevel l : RiskLevel.values()) {
            if (n.equals(LEVEL_LABEL.get(l).toUpperCase(Locale.ROOT))
                    || n.equals(CE_LABEL.get(l).toUpperCase(Locale.ROOT))
                    || n.equals(l.name().replace('_', ' '))) {
                level = l;
            }
        }
        if (level == null) {
            level = switch (n.replace(" ", "")) {
                case "VERYLOW", "VL" -> RiskLevel.VERY_LOW;
                case "L" -> RiskLevel.LOW;
                case "MED", "M" -> RiskLevel.MEDIUM;
                case "H" -> RiskLevel.HIGH;
                case "VERYHIGH", "VH", "VERYSTRONG" -> RiskLevel.VERY_HIGH;
                default -> null;
            };
        }
        if (level == null || !scale.contains(level)) {
            throw new IllegalArgumentException("not on scale");
        }
        return level;
    }

    static TreatmentOption parseTreatment(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim().toUpperCase(Locale.ROOT);
        if (s.contains("MITIGAT") || s.equals("TREAT") || s.equals("T1")) {
            return TreatmentOption.TREAT;
        }
        if (s.contains("ACCEPT") || s.contains("TOLERAT") || s.equals("T2")) {
            return TreatmentOption.TOLERATE;
        }
        if (s.contains("AVOID") || s.contains("TERMINAT") || s.equals("T3")) {
            return TreatmentOption.TERMINATE;
        }
        if (s.contains("TRANSFER") || s.equals("T4")) {
            return TreatmentOption.TRANSFER;
        }
        return TreatmentOption.valueOf(s.replace(' ', '_'));
    }

    static Boolean parseBool(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String n = raw.trim().toUpperCase(Locale.ROOT);
        if (n.equals("TRUE") || n.equals("YES") || n.equals("Y") || n.equals("1")) return true;
        if (n.equals("FALSE") || n.equals("NO") || n.equals("N") || n.equals("0")) return false;
        throw new IllegalArgumentException("Expected TRUE/FALSE, got: " + raw);
    }

    static BigDecimal parseDecimal(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return new BigDecimal(raw.trim().replace(",", ""));
    }
}
