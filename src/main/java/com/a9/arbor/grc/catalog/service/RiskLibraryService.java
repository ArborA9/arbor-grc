package com.a9.arbor.grc.catalog.service;

import com.a9.arbor.grc.catalog.domain.Framework;
import com.a9.arbor.grc.catalog.domain.FrameworkNode;
import com.a9.arbor.grc.catalog.domain.FrameworkTypeCode;
import com.a9.arbor.grc.catalog.domain.RiskTemplate;
import com.a9.arbor.grc.catalog.repo.FrameworkNodeRepository;
import com.a9.arbor.grc.catalog.repo.FrameworkRepository;
import com.a9.arbor.grc.catalog.repo.RiskTemplateRepository;
import com.a9.arbor.grc.catalog.web.dto.RiskTemplateImportResult;
import com.a9.arbor.grc.catalog.web.dto.RiskTemplateResponse;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A4 curated risk library: Super Admin Excel import into catalog (Type A).
 * Does not invent risk text; only persists spreadsheet rows.
 */
@Service
public class RiskLibraryService {

    private static final Set<String> SKIP_SHEETS = Set.of("risk summary", "risk matrix");

    private final FrameworkRepository frameworkRepository;
    private final FrameworkNodeRepository nodeRepository;
    private final RiskTemplateRepository templateRepository;

    public RiskLibraryService(FrameworkRepository frameworkRepository,
                              FrameworkNodeRepository nodeRepository,
                              RiskTemplateRepository templateRepository) {
        this.frameworkRepository = frameworkRepository;
        this.nodeRepository = nodeRepository;
        this.templateRepository = templateRepository;
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public List<RiskTemplateResponse> listActive(long frameworkId) {
        requireFramework(frameworkId);
        return templateRepository.findByFrameworkIdAndActiveTrueOrderByDisplayOrderAscSourceRiskIdAsc(frameworkId)
                .stream()
                .map(RiskTemplateResponse::from)
                .toList();
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public List<RiskTemplate> listActiveEntities(long frameworkId) {
        return templateRepository.findByFrameworkIdAndActiveTrueOrderByDisplayOrderAscSourceRiskIdAsc(frameworkId);
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public byte[] sampleExcel(long frameworkId) {
        requireFramework(frameworkId);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("RiskLibrary");
            Row header = sheet.createRow(0);
            String[] cols = {
                    "source_risk_id", "title", "description", "category", "control_node_code",
                    "typical_threats", "suggested_probability", "suggested_impact",
                    "suggested_control_effectiveness", "treatment_option", "treatment_recommendation"
            };
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }
            Row example = sheet.createRow(1);
            example.createCell(0).setCellValue("SAMPLE-001");
            example.createCell(1).setCellValue("(replace with curated risk title: do not invent in product)");
            example.createCell(2).setCellValue("");
            example.createCell(3).setCellValue("IT");
            example.createCell(4).setCellValue("A.5.1");
            example.createCell(5).setCellValue("");
            example.createCell(6).setCellValue("MEDIUM");
            example.createCell(7).setCellValue("HIGH");
            example.createCell(8).setCellValue("MEDIUM");
            example.createCell(9).setCellValue("TREAT");
            example.createCell(10).setCellValue("");
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to build sample: " + e.getMessage());
        }
    }

    /**
     * Import creates a new version and marks prior versions inactive.
     * Accepts Arbor sample columns or client Risk_Register sheet columns.
     */
    @Transactional(transactionManager = "catalogTransactionManager")
    public RiskTemplateImportResult importExcel(long frameworkId, MultipartFile file) {
        Framework fw = requireFramework(frameworkId);
        Map<String, FrameworkNode> nodesByCode = new HashMap<>();
        for (FrameworkNode n : nodeRepository.findByFrameworkIdOrderByLevelAscDisplayOrderAscNodeCodeAsc(frameworkId)) {
            nodesByCode.put(n.getNodeCode().toLowerCase(Locale.ROOT), n);
            nodesByCode.putIfAbsent(n.getNodeCode(), n);
        }
        if (nodesByCode.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Framework has no control nodes");
        }

        int nextVersion = templateRepository.maxVersion(frameworkId) + 1;
        for (RiskTemplate t : templateRepository
                .findByFrameworkIdAndActiveTrueOrderByDisplayOrderAscSourceRiskIdAsc(frameworkId)) {
            t.setActive(false);
            templateRepository.save(t);
        }

        DataFormatter fmt = new DataFormatter();
        int imported = 0;
        int skipped = 0;
        try (InputStream in = file.getInputStream(); Workbook wb = new XSSFWorkbook(in)) {
            List<Sheet> sheets = selectSheets(wb, fw);
            if (sheets.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No importable risk sheets found");
            }
            for (Sheet sheet : sheets) {
                Row header = sheet.getRow(0);
                if (header == null) {
                    continue;
                }
                Map<String, Integer> col = headerMap(fmt, header);
                boolean arbor = col.containsKey("source_risk_id") || col.containsKey("title");
                boolean client = col.containsKey("unique risk id") || col.containsKey("risk description");
                if (!arbor && !client) {
                    skipped++;
                    continue;
                }
                for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) {
                        continue;
                    }
                    ParsedRow parsed = arbor
                            ? parseArbor(fmt, row, col)
                            : parseClient(fmt, row, col);
                    if (parsed == null) {
                        continue;
                    }
                    if (parsed.blank()) {
                        continue;
                    }
                    if (parsed.sourceRiskId.isBlank() || parsed.title.isBlank()) {
                        skipped++;
                        continue;
                    }
                    FrameworkNode node = resolveNode(nodesByCode, parsed.controlNodeCode);
                    if (parsed.controlNodeCode != null && !parsed.controlNodeCode.isBlank() && node == null) {
                        skipped++;
                        continue;
                    }
                    RiskTemplate t = new RiskTemplate();
                    t.setFrameworkId(fw.getId());
                    t.setFrameworkNodeId(node != null ? node.getId() : null);
                    t.setSourceRiskId(truncate(parsed.sourceRiskId, 64));
                    t.setTitle(truncate(parsed.title, 512));
                    t.setDescription(blankToNull(parsed.description));
                    t.setCategory(parsed.category);
                    t.setTypicalThreats(blankToNull(parsed.typicalThreats));
                    t.setSuggestedProbability(parsed.probability);
                    t.setSuggestedImpact(parsed.impact);
                    t.setSuggestedControlEffectiveness(parsed.controlEffectiveness);
                    t.setTreatmentOption(blankToNull(parsed.treatmentOption));
                    t.setTreatmentRecommendation(blankToNull(parsed.treatmentRecommendation));
                    t.setDisplayOrder(imported + 1);
                    t.setVersionNumber(nextVersion);
                    t.setActive(true);
                    templateRepository.save(t);
                    imported++;
                }
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Import failed: " + e.getMessage());
        }
        if (imported == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No valid risk library rows imported (check control codes match framework nodes)");
        }
        return new RiskTemplateImportResult(nextVersion, imported, skipped);
    }

    private List<Sheet> selectSheets(Workbook wb, Framework fw) {
        List<Sheet> preferred = new ArrayList<>();
        List<Sheet> fallback = new ArrayList<>();
        String needle = sheetNeedle(fw);
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            Sheet s = wb.getSheetAt(i);
            String name = s.getSheetName() == null ? "" : s.getSheetName().trim();
            if (SKIP_SHEETS.contains(name.toLowerCase(Locale.ROOT))) {
                continue;
            }
            fallback.add(s);
            if (needle != null && name.toLowerCase(Locale.ROOT).contains(needle)) {
                preferred.add(s);
            }
        }
        if (!preferred.isEmpty()) {
            return preferred;
        }
        // Single-sheet Arbor sample
        if (fallback.size() == 1) {
            return fallback;
        }
        // Multi-sheet client workbook without matching name: use first data sheet only
        return fallback.isEmpty() ? List.of() : List.of(fallback.get(0));
    }

    private static String sheetNeedle(Framework fw) {
        String code = fw.getCode() == null ? "" : fw.getCode().toUpperCase(Locale.ROOT);
        if (code.contains("27001") || matchesType(fw, FrameworkTypeCode.ISO_27001)) {
            return "iso 27001";
        }
        if (code.contains("20000") || matchesType(fw, FrameworkTypeCode.ISO_20000)) {
            return "iso 20000";
        }
        if (code.contains("27017") || matchesType(fw, FrameworkTypeCode.ISO_27017)) {
            return "iso 27017";
        }
        if (code.contains("UAE") || code.contains("IA") || matchesType(fw, FrameworkTypeCode.UAE_IA)) {
            return "uae ia";
        }
        return null;
    }

    private static boolean matchesType(Framework fw, FrameworkTypeCode type) {
        return fw.getCode() != null && fw.getCode().toUpperCase(Locale.ROOT).contains(type.name().replace("_", ""));
    }

    private record ParsedRow(
            String sourceRiskId,
            String title,
            String description,
            String category,
            String controlNodeCode,
            String typicalThreats,
            String probability,
            String impact,
            String controlEffectiveness,
            String treatmentOption,
            String treatmentRecommendation
    ) {
        boolean blank() {
            return (sourceRiskId == null || sourceRiskId.isBlank())
                    && (title == null || title.isBlank());
        }
    }

    private static ParsedRow parseArbor(DataFormatter fmt, Row row, Map<String, Integer> col) {
        String id = cell(fmt, row, col, "source_risk_id");
        String title = cell(fmt, row, col, "title");
        if (id.isBlank() && title.isBlank()) {
            return null;
        }
        return new ParsedRow(
                id.isBlank() ? title : id,
                title.isBlank() ? id : title,
                cell(fmt, row, col, "description"),
                normalizeCategory(cell(fmt, row, col, "category")),
                cell(fmt, row, col, "control_node_code"),
                cell(fmt, row, col, "typical_threats"),
                normalizeLevel(cell(fmt, row, col, "suggested_probability"), "MEDIUM"),
                normalizeLevel(cell(fmt, row, col, "suggested_impact"), "MEDIUM"),
                normalizeLevel(cell(fmt, row, col, "suggested_control_effectiveness"), "MEDIUM"),
                normalizeTreatment(cell(fmt, row, col, "treatment_option")),
                cell(fmt, row, col, "treatment_recommendation"));
    }

    private static ParsedRow parseClient(DataFormatter fmt, Row row, Map<String, Integer> col) {
        String id = cell(fmt, row, col, "unique risk id");
        String desc = cell(fmt, row, col, "risk description");
        if (id.isBlank() && desc.isBlank()) {
            return null;
        }
        String title = desc.isBlank() ? id : truncate(desc, 512);
        String assetType = cell(fmt, row, col, "asset type");
        String affected = cell(fmt, row, col, "affected function(s)/asset(s)");
        return new ParsedRow(
                id.isBlank() ? ("ROW-" + row.getRowNum()) : id,
                title,
                desc,
                categoryFromAssetType(assetType),
                cell(fmt, row, col, "control id"),
                affected,
                levelFromNumericOrLabel(cell(fmt, row, col, "likelihood"), "MEDIUM"),
                levelFromNumericOrLabel(cell(fmt, row, col, "impact"), "MEDIUM"),
                "MEDIUM",
                normalizeTreatment(cell(fmt, row, col, "treatment option")),
                cell(fmt, row, col, "treatment recommendation"));
    }

    private static FrameworkNode resolveNode(Map<String, FrameworkNode> nodesByCode, String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        FrameworkNode n = nodesByCode.get(code);
        if (n != null) {
            return n;
        }
        return nodesByCode.get(code.toLowerCase(Locale.ROOT));
    }

    private static Map<String, Integer> headerMap(DataFormatter fmt, Row header) {
        Map<String, Integer> col = new HashMap<>();
        for (int c = 0; c < header.getLastCellNum(); c++) {
            String name = fmt.formatCellValue(header.getCell(c)).trim().toLowerCase(Locale.ROOT);
            if (!name.isEmpty()) {
                col.put(name, c);
            }
        }
        return col;
    }

    private static String cell(DataFormatter fmt, Row row, Map<String, Integer> col, String name) {
        Integer idx = col.get(name);
        if (idx == null || row.getCell(idx) == null) {
            return "";
        }
        return fmt.formatCellValue(row.getCell(idx)).trim();
    }

    private static String normalizeLevel(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return levelFromNumericOrLabel(raw, fallback);
    }

    private static String levelFromNumericOrLabel(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String s = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return switch (s) {
            case "1", "VL", "VERY_LOW", "VERYLOW" -> "VERY_LOW";
            case "2", "L", "LOW" -> "LOW";
            case "3", "M", "MED", "MEDIUM" -> "MEDIUM";
            case "4", "H", "HIGH" -> "HIGH";
            case "5", "VH", "VERY_HIGH", "VERYHIGH", "CRITICAL" -> "VERY_HIGH";
            default -> fallback;
        };
    }

    private static String normalizeCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return "IT";
        }
        String s = raw.trim().toUpperCase(Locale.ROOT);
        return switch (s) {
            case "STRATEGIC", "OPERATIONAL", "FINANCIAL", "COMPLIANCE", "IT", "REPUTATIONAL",
                 "LEGAL", "ENVIRONMENTAL" -> s;
            default -> categoryFromAssetType(s);
        };
    }

    private static String categoryFromAssetType(String assetType) {
        if (assetType == null || assetType.isBlank()) {
            return "IT";
        }
        String s = assetType.trim().toUpperCase(Locale.ROOT);
        return switch (s) {
            case "FACILITY" -> "OPERATIONAL";
            case "VENDOR-MANAGED", "VENDOR_MANAGED", "VENDOR" -> "COMPLIANCE";
            default -> "IT";
        };
    }

    private static String normalizeTreatment(String raw) {
        if (raw == null || raw.isBlank()) {
            return "TREAT";
        }
        String s = raw.trim().toUpperCase(Locale.ROOT);
        if (s.contains("MITIGAT") || s.equals("TREAT")) {
            return "TREAT";
        }
        if (s.contains("ACCEPT") || s.contains("TOLERAT")) {
            return "TOLERATE";
        }
        if (s.contains("AVOID") || s.contains("TERMINAT")) {
            return "TERMINATE";
        }
        if (s.contains("TRANSFER")) {
            return "TRANSFER";
        }
        return "TREAT";
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }

    private Framework requireFramework(long id) {
        return frameworkRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Framework not found"));
    }
}
