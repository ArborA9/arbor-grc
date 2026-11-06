package com.a9.arbor.grc.modules.auditmgmt.service;

import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.auditmgmt.domain.ChecklistTemplateKey;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditChecklistLibrary;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditChecklistLibraryItem;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditChecklistLibraryItemRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditChecklistLibraryRepository;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditChecklistLibraryItemRequest;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.AuditChecklistLibraryResponse;
import com.a9.arbor.grc.modules.auditmgmt.web.dto.UpsertAuditChecklistLibraryRequest;
import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AuditChecklistLibraryService {

    private static final String XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final GrcAuditChecklistLibraryRepository libraryRepository;
    private final GrcAuditChecklistLibraryItemRepository itemRepository;
    private final AuditChecklistLibrary classpathLibrary;
    private final AuditChecklistLibraryExcel excel;
    private final AuditTrailService auditTrailService;

    public AuditChecklistLibraryService(GrcAuditChecklistLibraryRepository libraryRepository,
                                        GrcAuditChecklistLibraryItemRepository itemRepository,
                                        AuditChecklistLibrary classpathLibrary,
                                        AuditChecklistLibraryExcel excel,
                                        AuditTrailService auditTrailService) {
        this.libraryRepository = libraryRepository;
        this.itemRepository = itemRepository;
        this.classpathLibrary = classpathLibrary;
        this.excel = excel;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public List<AuditChecklistLibraryResponse> list(User user) {
        ensureSeeded(user.getId());
        return libraryRepository.findAllByOrderBySystemSeededDescNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AuditChecklistLibraryResponse get(User user, long id) {
        ensureSeeded(user.getId());
        return toResponse(require(id));
    }

    @Transactional
    public AuditChecklistLibraryResponse create(User admin, UpsertAuditChecklistLibraryRequest req) {
        assertAdmin(admin);
        ensureSeeded(admin.getId());
        String name = req.name().trim();
        if (libraryRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A checklist named \"" + name + "\" already exists");
        }
        GrcAuditChecklistLibrary lib = new GrcAuditChecklistLibrary();
        lib.setName(name);
        lib.setTemplateKey(ChecklistTemplateKey.CUSTOM);
        lib.setSystemSeeded(false);
        lib.setCreatedByUserId(admin.getId());
        lib = libraryRepository.save(lib);
        if (req.items() != null && !req.items().isEmpty()) {
            replaceItems(lib, req.items());
        }
        auditTrailService.record(AuditActionType.CREATED, "AUDIT", admin.getId(),
                rolesSnap(admin), "AUDIT_CHECKLIST", String.valueOf(lib.getId()),
                lib.getName(), null, null, "created", null, null);
        return toResponse(lib);
    }

    @Transactional
    public AuditChecklistLibraryResponse save(User admin, long id, UpsertAuditChecklistLibraryRequest req) {
        assertAdmin(admin);
        GrcAuditChecklistLibrary lib = require(id);
        String name = req.name().trim();
        if (libraryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A checklist named \"" + name + "\" already exists");
        }
        lib.setName(name);
        libraryRepository.save(lib);
        if (req.items() != null) {
            replaceItems(lib, req.items());
        }
        return toResponse(lib);
    }

    @Transactional
    public void delete(User admin, long id) {
        assertAdmin(admin);
        GrcAuditChecklistLibrary lib = require(id);
        if (lib.isSystemSeeded()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The starter template checklists cannot be deleted");
        }
        itemRepository.deleteByLibraryId(lib.getId());
        libraryRepository.delete(lib);
    }

    @Transactional(readOnly = true)
    public ExportFile blankTemplate() {
        return new ExportFile(excel.writeBlankTemplate(), "audit-checklist-template.xlsx", XLSX);
    }

    @Transactional
    public AuditChecklistLibraryResponse upload(User admin, long id, MultipartFile file) {
        assertAdmin(admin);
        GrcAuditChecklistLibrary lib = require(id);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read upload");
        }
        List<AuditChecklistLibraryExcel.Parsed> parsed = excel.parseTemplate(bytes);
        if (parsed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Checklist has no rows");
        }
        List<AuditChecklistLibraryItemRequest> rows = new ArrayList<>();
        for (AuditChecklistLibraryExcel.Parsed row : parsed) {
            rows.add(new AuditChecklistLibraryItemRequest(
                    null, row.clauseRef(), row.section(), row.title(),
                    row.testFocus(), row.soaApplicability(), row.evidenceLives()));
        }
        replaceItems(lib, rows);
        return toResponse(lib);
    }

    @Transactional
    public void ensureSeeded(long createdByUserId) {
        for (AuditChecklistLibrary.Template template : classpathLibrary.all()) {
            if (libraryRepository.existsByTemplateKeyAndSystemSeededTrue(template.key())) {
                continue;
            }
            String name = template.name();
            if (libraryRepository.existsByNameIgnoreCase(name)) {
                name = template.key().defaultName();
            }
            GrcAuditChecklistLibrary lib = new GrcAuditChecklistLibrary();
            lib.setName(name);
            lib.setTemplateKey(template.key());
            lib.setSystemSeeded(true);
            lib.setCreatedByUserId(createdByUserId);
            lib = libraryRepository.save(lib);
            int order = 0;
            for (AuditChecklistLibrary.Line line : template.lines()) {
                GrcAuditChecklistLibraryItem item = new GrcAuditChecklistLibraryItem();
                item.setLibraryId(lib.getId());
                item.setSortOrder(order++);
                item.setClauseRef(trimClause(line.clauseRef()));
                item.setSection(blankToNull(line.section()));
                item.setTitle(line.title() == null || line.title().isBlank()
                        ? trimClause(line.clauseRef()) : line.title().trim());
                item.setTestFocus(blankToNull(line.testFocus()));
                item.setSoaApplicability(blankToNull(line.soaApplicability()));
                item.setEvidenceLives(blankToNull(line.evidenceLives()));
                itemRepository.save(item);
            }
        }
    }

    public GrcAuditChecklistLibrary require(long id) {
        return libraryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Checklist not found"));
    }

    public GrcAuditChecklistLibrary requireByTemplateKey(ChecklistTemplateKey key) {
        return libraryRepository.findFirstByTemplateKeyAndSystemSeededTrue(key)
                .or(() -> libraryRepository.findFirstByTemplateKeyOrderByIdAsc(key))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Checklist " + key.defaultName() + " is not in the library"));
    }

    public List<GrcAuditChecklistLibraryItem> items(long libraryId) {
        return itemRepository.findByLibraryIdOrderBySortOrderAscIdAsc(libraryId);
    }

    private void replaceItems(GrcAuditChecklistLibrary lib, List<AuditChecklistLibraryItemRequest> rows) {
        Set<String> seen = new LinkedHashSet<>();
        for (AuditChecklistLibraryItemRequest row : rows) {
            String clause = row.clauseRef() == null ? "" : row.clauseRef().trim();
            if (clause.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each row needs a Clause / Annex A Ref");
            }
            String key = clause.toLowerCase(Locale.ROOT);
            if (!seen.add(key)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Clause ref \"" + clause + "\" is duplicated");
            }
            if (row.title() == null || row.title().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each row needs a Clause / Control Title");
            }
        }
        itemRepository.deleteByLibraryId(lib.getId());
        int order = 0;
        for (AuditChecklistLibraryItemRequest row : rows) {
            GrcAuditChecklistLibraryItem item = new GrcAuditChecklistLibraryItem();
            item.setLibraryId(lib.getId());
            item.setSortOrder(order++);
            item.setClauseRef(trimClause(row.clauseRef().trim()));
            item.setSection(blankToNull(row.section()));
            item.setTitle(row.title().trim());
            item.setTestFocus(blankToNull(row.testFocus()));
            item.setSoaApplicability(blankToNull(row.soaApplicability()));
            item.setEvidenceLives(blankToNull(row.evidenceLives()));
            itemRepository.save(item);
        }
    }

    private AuditChecklistLibraryResponse toResponse(GrcAuditChecklistLibrary lib) {
        List<AuditChecklistLibraryResponse.ItemView> items = itemRepository
                .findByLibraryIdOrderBySortOrderAscIdAsc(lib.getId()).stream()
                .map(AuditChecklistLibraryResponse.ItemView::from)
                .toList();
        return AuditChecklistLibraryResponse.from(lib, items);
    }

    private static void assertAdmin(User user) {
        if (user.getRoles().stream().noneMatch(r -> r == Role.OWNER || r == Role.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    private static String rolesSnap(User user) {
        return user.getRoles().stream().map(Enum::name).sorted().reduce((a, b) -> a + "," + b).orElse("");
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static String trimClause(String clause) {
        if (clause == null || clause.isBlank()) {
            return "UNSET";
        }
        String v = clause.trim();
        return v.length() <= 80 ? v : v.substring(0, 80);
    }
}
