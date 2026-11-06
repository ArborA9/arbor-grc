package com.a9.arbor.grc.catalog.service;

import com.a9.arbor.grc.catalog.domain.Framework;
import com.a9.arbor.grc.catalog.domain.FrameworkNode;
import com.a9.arbor.grc.catalog.domain.FrameworkQuestion;
import com.a9.arbor.grc.catalog.repo.FrameworkNodeRepository;
import com.a9.arbor.grc.catalog.repo.FrameworkQuestionRepository;
import com.a9.arbor.grc.catalog.repo.FrameworkRepository;
import com.a9.arbor.grc.catalog.web.dto.FrameworkQuestionImportResult;
import com.a9.arbor.grc.catalog.web.dto.FrameworkQuestionResponse;
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
import java.util.Map;

@Service
public class FrameworkQuestionService {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkNodeRepository nodeRepository;
    private final FrameworkQuestionRepository questionRepository;

    public FrameworkQuestionService(FrameworkRepository frameworkRepository,
                                    FrameworkNodeRepository nodeRepository,
                                    FrameworkQuestionRepository questionRepository) {
        this.frameworkRepository = frameworkRepository;
        this.nodeRepository = nodeRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public List<FrameworkQuestionResponse> listActive(long frameworkId) {
        requireFramework(frameworkId);
        return questionRepository.findByFrameworkIdAndActiveTrueOrderByDisplayOrderAscQuestionCodeAsc(frameworkId)
                .stream()
                .map(FrameworkQuestionResponse::from)
                .toList();
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public byte[] sampleExcel(long frameworkId) {
        requireFramework(frameworkId);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Questions");
            Row header = sheet.createRow(0);
            String[] cols = {
                    "node_code", "question_code", "question_type", "question_text",
                    "evidence_guidance", "display_order"
            };
            for (int i = 0; i < cols.length; i++) {
                header.createCell(i).setCellValue(cols[i]);
            }
            Row example = sheet.createRow(1);
            example.createCell(0).setCellValue("A.5.1");
            example.createCell(1).setCellValue("Q1");
            example.createCell(2).setCellValue("Existence");
            example.createCell(3).setCellValue("(replace with real question: do not invent in product)");
            example.createCell(4).setCellValue("");
            example.createCell(5).setCellValue(1);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to build sample: " + e.getMessage());
        }
    }

    /**
     * Import creates a new version_number and marks prior versions inactive.
     * Does not invent content: only persists rows from the spreadsheet.
     */
    @Transactional(transactionManager = "catalogTransactionManager")
    public FrameworkQuestionImportResult importExcel(long frameworkId, MultipartFile file) {
        Framework fw = requireFramework(frameworkId);
        Map<String, FrameworkNode> nodesByCode = new HashMap<>();
        for (FrameworkNode n : nodeRepository.findByFrameworkIdOrderByLevelAscDisplayOrderAscNodeCodeAsc(frameworkId)) {
            nodesByCode.put(n.getNodeCode(), n);
        }
        if (nodesByCode.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Framework has no control nodes");
        }

        int nextVersion = questionRepository.maxVersion(frameworkId) + 1;
        List<FrameworkQuestion> existing = questionRepository
                .findByFrameworkIdAndActiveTrueOrderByDisplayOrderAscQuestionCodeAsc(frameworkId);
        for (FrameworkQuestion q : existing) {
            q.setActive(false);
            questionRepository.save(q);
        }

        DataFormatter fmt = new DataFormatter();
        int imported = 0;
        int skipped = 0;
        try (InputStream in = file.getInputStream(); Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workbook has no sheets");
            }
            Row header = sheet.getRow(0);
            if (header == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing header row");
            }
            Map<String, Integer> col = new HashMap<>();
            for (int c = 0; c < header.getLastCellNum(); c++) {
                String name = fmt.formatCellValue(header.getCell(c)).trim().toLowerCase();
                if (!name.isEmpty()) {
                    col.put(name, c);
                }
            }
            requireCol(col, "node_code");
            requireCol(col, "question_code");
            requireCol(col, "question_text");

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                String nodeCode = cell(fmt, row, col.get("node_code"));
                String qCode = cell(fmt, row, col.get("question_code"));
                String text = cell(fmt, row, col.get("question_text"));
                if (nodeCode.isBlank() && qCode.isBlank() && text.isBlank()) {
                    continue;
                }
                if (nodeCode.isBlank() || qCode.isBlank() || text.isBlank()) {
                    skipped++;
                    continue;
                }
                FrameworkNode node = nodesByCode.get(nodeCode);
                if (node == null) {
                    skipped++;
                    continue;
                }
                FrameworkQuestion q = new FrameworkQuestion();
                q.setFrameworkId(fw.getId());
                q.setFrameworkNodeId(node.getId());
                q.setQuestionCode(qCode.trim());
                // The legacy sheet has one code per framework; splitting it this way matches the V10
                // backfill so the (control_code, question_number) key keeps the old uniqueness guarantee.
                q.setControlCode(node.getNodeCode());
                q.setQuestionNumber(qCode.trim());
                q.setQuestionType(blankToNull(cell(fmt, row, col.getOrDefault("question_type", -1))));
                q.setQuestionText(text.trim());
                q.setEvidenceGuidance(blankToNull(cell(fmt, row, col.getOrDefault("evidence_guidance", -1))));
                String orderRaw = cell(fmt, row, col.getOrDefault("display_order", -1));
                q.setDisplayOrder(orderRaw.isBlank() ? imported + 1 : parseIntSafe(orderRaw, imported + 1));
                q.setVersionNumber(nextVersion);
                q.setActive(true);
                questionRepository.save(q);
                imported++;
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Import failed: " + e.getMessage());
        }
        if (imported == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No valid question rows imported (check node_code matches framework controls)");
        }
        return new FrameworkQuestionImportResult(nextVersion, imported, skipped);
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public List<FrameworkQuestionResponse> listActiveForNode(long frameworkId, long frameworkNodeId) {
        requireFramework(frameworkId);
        return questionRepository
                .findByFrameworkIdAndFrameworkNodeIdAndActiveTrueOrderByDisplayOrderAsc(frameworkId, frameworkNodeId)
                .stream()
                .map(FrameworkQuestionResponse::from)
                .toList();
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public java.util.Optional<FrameworkQuestion> findActive(long questionId) {
        return questionRepository.findById(questionId).filter(FrameworkQuestion::isActive);
    }

    private Framework requireFramework(long id) {
        return frameworkRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Framework not found"));
    }

    private static void requireCol(Map<String, Integer> col, String name) {
        if (!col.containsKey(name)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing column: " + name);
        }
    }

    private static String cell(DataFormatter fmt, Row row, int idx) {
        if (idx < 0 || row.getCell(idx) == null) {
            return "";
        }
        return fmt.formatCellValue(row.getCell(idx)).trim();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static int parseIntSafe(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
