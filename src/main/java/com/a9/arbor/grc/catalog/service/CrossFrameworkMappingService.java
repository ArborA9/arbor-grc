package com.a9.arbor.grc.catalog.service;

import com.a9.arbor.grc.catalog.domain.CrossFrameworkMapping;
import com.a9.arbor.grc.catalog.domain.CrossFrameworkMappingType;
import com.a9.arbor.grc.catalog.domain.Framework;
import com.a9.arbor.grc.catalog.domain.FrameworkNode;
import com.a9.arbor.grc.catalog.repo.CrossFrameworkMappingRepository;
import com.a9.arbor.grc.catalog.repo.FrameworkNodeRepository;
import com.a9.arbor.grc.catalog.repo.FrameworkRepository;
import com.a9.arbor.grc.catalog.web.dto.CrossFrameworkMappingEndpoint;
import com.a9.arbor.grc.catalog.web.dto.CrossFrameworkMappingImportResult;
import com.a9.arbor.grc.catalog.web.dto.CrossFrameworkMappingResponse;
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
import java.util.Optional;

/**
 * A3 cross-framework control mappings: Type A catalog. Does not invent mapping content;
 * only persists Super Admin spreadsheet rows.
 */
@Service
public class CrossFrameworkMappingService {

    private static final DataFormatter FORMATTER = new DataFormatter();

    private final CrossFrameworkMappingRepository mappingRepository;
    private final FrameworkRepository frameworkRepository;
    private final FrameworkNodeRepository nodeRepository;

    public CrossFrameworkMappingService(CrossFrameworkMappingRepository mappingRepository,
                                        FrameworkRepository frameworkRepository,
                                        FrameworkNodeRepository nodeRepository) {
        this.mappingRepository = mappingRepository;
        this.frameworkRepository = frameworkRepository;
        this.nodeRepository = nodeRepository;
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public byte[] sampleExcel() {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("CrossFrameworkMappings");
            Row header = sheet.createRow(0);
            String[] cols = {
                    "source_framework_code", "source_framework_version", "source_node_code",
                    "target_framework_code", "target_framework_version", "target_node_code",
                    "mapping_type", "justification"
            };
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }
            Row example = sheet.createRow(1);
            example.createCell(0).setCellValue("ISO_27001");
            example.createCell(1).setCellValue("2022");
            example.createCell(2).setCellValue("A.5.1");
            example.createCell(3).setCellValue("UAE_IA");
            example.createCell(4).setCellValue("2.1");
            example.createCell(5).setCellValue("M.1.1");
            example.createCell(6).setCellValue("EQUIVALENT");
            example.createCell(7).setCellValue("(replace with A9 curated justification)");
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to build sample: " + e.getMessage());
        }
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public CrossFrameworkMappingImportResult importExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
        }
        int imported = 0;
        int skipped = 0;
        Map<String, Framework> frameworkCache = new HashMap<>();
        Map<String, FrameworkNode> nodeCache = new HashMap<>();

        try (InputStream in = file.getInputStream(); Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workbook has no sheets");
            }
            Map<String, Integer> colIndex = headerIndex(sheet.getRow(0));
            requireColumns(colIndex);

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlankRow(row, colIndex)) {
                    continue;
                }
                String sourceFwCode = cell(row, colIndex, "source_framework_code");
                String sourceFwVer = cell(row, colIndex, "source_framework_version");
                String sourceNodeCode = cell(row, colIndex, "source_node_code");
                String targetFwCode = cell(row, colIndex, "target_framework_code");
                String targetFwVer = cell(row, colIndex, "target_framework_version");
                String targetNodeCode = cell(row, colIndex, "target_node_code");
                String mappingTypeRaw = cell(row, colIndex, "mapping_type");
                String justification = cell(row, colIndex, "justification");

                if (sourceFwCode.isBlank() || sourceFwVer.isBlank() || sourceNodeCode.isBlank()
                        || targetFwCode.isBlank() || targetFwVer.isBlank() || targetNodeCode.isBlank()
                        || mappingTypeRaw.isBlank()) {
                    skipped++;
                    continue;
                }

                CrossFrameworkMappingType mappingType;
                try {
                    mappingType = CrossFrameworkMappingType.valueOf(mappingTypeRaw.trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    skipped++;
                    continue;
                }

                Optional<FrameworkNode> sourceNode = resolveNode(
                        frameworkCache, nodeCache, sourceFwCode, sourceFwVer, sourceNodeCode);
                Optional<FrameworkNode> targetNode = resolveNode(
                        frameworkCache, nodeCache, targetFwCode, targetFwVer, targetNodeCode);
                if (sourceNode.isEmpty() || targetNode.isEmpty()) {
                    skipped++;
                    continue;
                }
                if (sourceNode.get().getId().equals(targetNode.get().getId())) {
                    skipped++;
                    continue;
                }

                Long sourceId = sourceNode.get().getId();
                Long targetId = targetNode.get().getId();
                CrossFrameworkMapping rowEntity = mappingRepository.findBySourceNodeId(sourceId).stream()
                        .filter(m -> m.getTargetNodeId().equals(targetId))
                        .findFirst()
                        .orElseGet(() -> {
                            CrossFrameworkMapping m = new CrossFrameworkMapping();
                            m.setSourceNodeId(sourceId);
                            m.setTargetNodeId(targetId);
                            return m;
                        });
                rowEntity.setMappingType(mappingType);
                rowEntity.setJustification(blankToNull(justification));
                mappingRepository.save(rowEntity);
                imported++;
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Import failed: " + e.getMessage());
        }
        return new CrossFrameworkMappingImportResult(imported, skipped);
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public List<CrossFrameworkMappingResponse> listForNode(long nodeId) {
        FrameworkNode anchor = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Framework node not found"));
        List<CrossFrameworkMapping> rows = mappingRepository.findBySourceNodeIdOrTargetNodeId(nodeId, nodeId);
        List<CrossFrameworkMappingResponse> out = new ArrayList<>();
        for (CrossFrameworkMapping row : rows) {
            boolean outbound = row.getSourceNodeId().equals(anchor.getId());
            long peerNodeId = outbound ? row.getTargetNodeId() : row.getSourceNodeId();
            FrameworkNode peerNode = nodeRepository.findById(peerNodeId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Mapped node missing: " + peerNodeId));
            Framework peerFramework = frameworkRepository.findById(peerNode.getFrameworkId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Mapped framework missing"));
            out.add(new CrossFrameworkMappingResponse(
                    row.getId(),
                    row.getMappingType().name(),
                    row.getJustification(),
                    toEndpoint(peerFramework, peerNode),
                    outbound ? "OUTBOUND" : "INBOUND"));
        }
        return out;
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public List<CrossFrameworkMapping> dualCreditMappings(long sourceNodeId) {
        return mappingRepository.findBySourceNodeIdAndMappingTypeIn(
                sourceNodeId,
                List.of(CrossFrameworkMappingType.EQUIVALENT, CrossFrameworkMappingType.PARTIAL));
    }

    private Optional<FrameworkNode> resolveNode(Map<String, Framework> frameworkCache,
                                                Map<String, FrameworkNode> nodeCache,
                                                String fwCode, String fwVer, String nodeCode) {
        String fwKey = fwCode.trim().toUpperCase(Locale.ROOT) + "|" + fwVer.trim();
        Framework fw = frameworkCache.computeIfAbsent(fwKey, k ->
                frameworkRepository.findByCodeAndVersion(fwCode.trim(), fwVer.trim()).orElse(null));
        if (fw == null) {
            return Optional.empty();
        }
        String nodeKey = fw.getId() + "|" + nodeCode.trim();
        FrameworkNode node = nodeCache.computeIfAbsent(nodeKey, k ->
                nodeRepository.findByFrameworkIdAndNodeCode(fw.getId(), nodeCode.trim()).orElse(null));
        return Optional.ofNullable(node);
    }

    private static CrossFrameworkMappingEndpoint toEndpoint(Framework fw, FrameworkNode node) {
        return new CrossFrameworkMappingEndpoint(
                fw.getId(),
                fw.getCode(),
                fw.getName(),
                fw.getVersion(),
                node.getId(),
                node.getNodeCode(),
                node.getTitle());
    }

    private static Map<String, Integer> headerIndex(Row header) {
        if (header == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Header row missing");
        }
        Map<String, Integer> out = new HashMap<>();
        for (int i = 0; i < header.getLastCellNum(); i++) {
            String name = FORMATTER.formatCellValue(header.getCell(i)).trim().toLowerCase(Locale.ROOT);
            if (!name.isEmpty()) {
                out.put(name, i);
            }
        }
        return out;
    }

    private static void requireColumns(Map<String, Integer> colIndex) {
        String[] required = {
                "source_framework_code", "source_framework_version", "source_node_code",
                "target_framework_code", "target_framework_version", "target_node_code",
                "mapping_type"
        };
        for (String col : required) {
            if (!colIndex.containsKey(col)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing column: " + col);
            }
        }
    }

    private static String cell(Row row, Map<String, Integer> colIndex, String col) {
        Integer idx = colIndex.get(col);
        if (idx == null) {
            return "";
        }
        return FORMATTER.formatCellValue(row.getCell(idx)).trim();
    }

    private static boolean isBlankRow(Row row, Map<String, Integer> colIndex) {
        for (String col : List.of("source_framework_code", "target_framework_code", "source_node_code")) {
            if (!cell(row, colIndex, col).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
