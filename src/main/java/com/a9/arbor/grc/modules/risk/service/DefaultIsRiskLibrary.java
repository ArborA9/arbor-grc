package com.a9.arbor.grc.modules.risk.service;

import com.a9.arbor.grc.catalog.domain.Framework;
import com.a9.arbor.grc.catalog.domain.FrameworkTypeCode;
import com.a9.arbor.grc.catalog.domain.TenantFramework;
import com.a9.arbor.grc.catalog.repo.TenantFrameworkRepository;
import com.a9.arbor.grc.catalog.web.dto.FrameworkNodeResponse;
import com.a9.arbor.grc.modules.risk.domain.TreatmentOption;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Arbor Default Risk workbooks (classpath {@code /default-risk/}). Used to seed an IS Risk
 * project when the chosen framework has a matching Excel. Does not invent rows.
 */
@Component
public class DefaultIsRiskLibrary {

    public record DefaultRiskRow(
            String controlCode,
            String title,
            String description,
            String observedGap,
            String threats,
            TreatmentOption treatment
    ) {
    }

    private static final String RESOURCE_DIR = "/default-risk/";
    private static final Map<FrameworkTypeCode, String> FILES = Map.of(
            FrameworkTypeCode.ISO_27001, "ISO27001-2022_Risk_Register.xlsx",
            FrameworkTypeCode.ISO_27017, "ISO27017-2015_Risk_Register.xlsx",
            FrameworkTypeCode.UAE_IA, "UAE_IA_V2.1_Risk_Register.xlsx",
            FrameworkTypeCode.ISO_22301, "ISO22301-2019_Risk_Register.xlsx",
            FrameworkTypeCode.ISO_20000, "ISO20000-1-2018_Risk_Register.xlsx",
            FrameworkTypeCode.ISO_42001, "ISO42001-2023_Risk_Register.xlsx"
    );

    private final TenantFrameworkRepository tenantFrameworkRepository;
    private final ConcurrentHashMap<String, List<DefaultRiskRow>> cache = new ConcurrentHashMap<>();

    public DefaultIsRiskLibrary(TenantFrameworkRepository tenantFrameworkRepository) {
        this.tenantFrameworkRepository = tenantFrameworkRepository;
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public boolean includeCloud(long frameworkId) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return false;
        }
        return tenantFrameworkRepository.findByTenantIdAndFrameworkId(tenantId, frameworkId)
                .map(TenantFramework::isIncludeCloud)
                .orElse(false);
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public int countMatching(Framework framework, Map<Long, FrameworkNodeResponse> nodes) {
        return matchingRows(framework, nodes).size();
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public List<DefaultRiskRow> matchingRows(Framework framework, Map<Long, FrameworkNodeResponse> nodes) {
        List<DefaultRiskRow> out = new ArrayList<>();
        Map<String, FrameworkNodeResponse> byCode = indexNodes(nodes);
        FrameworkTypeCode type = typeOf(framework);
        if (type == null) {
            return List.of();
        }
        for (DefaultRiskRow row : rowsFor(type)) {
            if (byCode.containsKey(norm(row.controlCode()))) {
                out.add(row);
            }
        }
        if (type == FrameworkTypeCode.ISO_27001) {
            boolean cloud = false;
            String tenantId = TenantContext.getTenantId();
            if (tenantId != null && !tenantId.isBlank()) {
                cloud = tenantFrameworkRepository.findByTenantIdAndFrameworkId(tenantId, framework.getId())
                        .map(TenantFramework::isIncludeCloud)
                        .orElse(false);
            }
            if (cloud) {
                for (DefaultRiskRow row : rowsFor(FrameworkTypeCode.ISO_27017)) {
                    if (byCode.containsKey(norm(row.controlCode()))) {
                        out.add(row);
                    }
                }
            }
        }
        return out;
    }

    public List<DefaultRiskRow> rowsFor(FrameworkTypeCode type) {
        String file = FILES.get(type);
        if (file == null) {
            return List.of();
        }
        return cache.computeIfAbsent(file, this::loadFile);
    }

    static FrameworkTypeCode typeOf(Framework framework) {
        if (framework == null || framework.getCode() == null) {
            return null;
        }
        try {
            return FrameworkTypeCode.valueOf(framework.getCode().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private List<DefaultRiskRow> loadFile(String filename) {
        String path = RESOURCE_DIR + filename;
        try (InputStream in = DefaultIsRiskLibrary.class.getResourceAsStream(path)) {
            if (in == null) {
                return List.of();
            }
            try (Workbook wb = new XSSFWorkbook(in)) {
                Sheet sheet = wb.getSheet("Risk Register");
                if (sheet == null && wb.getNumberOfSheets() > 0) {
                    sheet = wb.getSheetAt(0);
                }
                if (sheet == null) {
                    return List.of();
                }
                return parseSheet(sheet);
            }
        } catch (Exception e) {
            return List.of();
        }
    }

    private static List<DefaultRiskRow> parseSheet(Sheet sheet) {
        Row header = sheet.getRow(0);
        if (header == null) {
            return List.of();
        }
        DataFormatter fmt = new DataFormatter();
        Map<String, Integer> col = headerMap(fmt, header);
        Integer titleCol = first(col, "risk title", "title");
        Integer descCol = first(col, "risk description", "description");
        Integer gapCol = first(col, "gap / observation", "gap / observation", "observed gap");
        Integer ctrlCol = first(col, "control ref", "control / clause reference", "control id");
        Integer threatCol = first(col, "associated threat", "threats");
        Integer treatCol = first(col, "treatment option", "treatment option (4t)");
        if (titleCol == null || ctrlCol == null) {
            return List.of();
        }
        List<DefaultRiskRow> rows = new ArrayList<>();
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            String title = cell(fmt, row, titleCol);
            String code = cell(fmt, row, ctrlCol);
            if (title.isBlank() || code.isBlank()) {
                continue;
            }
            String desc = descCol == null ? "" : cell(fmt, row, descCol);
            String gap = gapCol == null ? "" : cell(fmt, row, gapCol);
            String threat = threatCol == null ? "" : cell(fmt, row, threatCol);
            TreatmentOption treatment = parseTreatment(treatCol == null ? "" : cell(fmt, row, treatCol));
            rows.add(new DefaultRiskRow(
                    code.trim(),
                    truncate(title, 1024),
                    truncate(desc.isBlank() ? title : desc, 8000),
                    truncate(gap.isBlank() ? title : gap, 8000),
                    truncate(threat.isBlank() ? "Operational Failure" : threat, 8000),
                    treatment));
        }
        return List.copyOf(rows);
    }

    private static Map<String, FrameworkNodeResponse> indexNodes(Map<Long, FrameworkNodeResponse> nodes) {
        Map<String, FrameworkNodeResponse> byCode = new LinkedHashMap<>();
        if (nodes == null) {
            return byCode;
        }
        for (FrameworkNodeResponse n : nodes.values()) {
            if (n == null || n.retired()) {
                continue;
            }
            if (n.code() != null && !n.code().isBlank()) {
                byCode.putIfAbsent(norm(n.code()), n);
            }
            if (n.officialRef() != null && !n.officialRef().isBlank()) {
                byCode.putIfAbsent(norm(n.officialRef()), n);
            }
        }
        return byCode;
    }

    static TreatmentOption parseTreatment(String raw) {
        if (raw == null || raw.isBlank()) {
            return TreatmentOption.TREAT;
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
        return TreatmentOption.TREAT;
    }

    private static Map<String, Integer> headerMap(DataFormatter fmt, Row header) {
        Map<String, Integer> col = new LinkedHashMap<>();
        for (int c = 0; c < header.getLastCellNum(); c++) {
            String name = fmt.formatCellValue(header.getCell(c));
            if (name != null && !name.isBlank()) {
                col.put(name.trim().toLowerCase(Locale.ROOT), c);
            }
        }
        return col;
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

    private static String cell(DataFormatter fmt, Row row, int idx) {
        if (row.getCell(idx) == null) {
            return "";
        }
        String v = fmt.formatCellValue(row.getCell(idx));
        return v == null ? "" : v.trim();
    }

    private static String norm(String code) {
        return code == null ? "" : code.trim().toLowerCase(Locale.ROOT);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
