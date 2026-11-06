package com.a9.arbor.grc.modules.tprm.service;

import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.tprm.domain.TprmControl;
import com.a9.arbor.grc.modules.tprm.domain.TprmQuestionnaire;
import com.a9.arbor.grc.modules.tprm.domain.TprmQuestionnaireItem;
import com.a9.arbor.grc.modules.tprm.pack.TprmControlDisplay;
import com.a9.arbor.grc.modules.tprm.repo.TprmControlRepository;
import com.a9.arbor.grc.modules.tprm.repo.TprmQuestionnaireItemRepository;
import com.a9.arbor.grc.modules.tprm.repo.TprmQuestionnaireRepository;
import com.a9.arbor.grc.modules.tprm.web.dto.QuestionnaireItemRequest;
import com.a9.arbor.grc.modules.tprm.web.dto.QuestionnaireResponse;
import com.a9.arbor.grc.modules.tprm.web.dto.UpsertQuestionnaireRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TprmQuestionnaireService {

    private static final String CORE_NAME = "Core TPRM questionnaire";
    private static final String XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String CUSTOM_MODULE = "CUSTOM";

    private final TprmQuestionnaireRepository questionnaireRepository;
    private final TprmQuestionnaireItemRepository itemRepository;
    private final TprmControlRepository controlRepository;
    private final TprmControlLibrarySeeder seeder;
    private final TprmQuestionnaireExcel excel;
    private final TprmVendorService vendorService;
    private final AuditTrailService auditTrailService;

    public TprmQuestionnaireService(TprmQuestionnaireRepository questionnaireRepository,
                                    TprmQuestionnaireItemRepository itemRepository,
                                    TprmControlRepository controlRepository,
                                    TprmControlLibrarySeeder seeder,
                                    TprmQuestionnaireExcel excel,
                                    TprmVendorService vendorService,
                                    AuditTrailService auditTrailService) {
        this.questionnaireRepository = questionnaireRepository;
        this.itemRepository = itemRepository;
        this.controlRepository = controlRepository;
        this.seeder = seeder;
        this.excel = excel;
        this.vendorService = vendorService;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public List<QuestionnaireResponse> list(User user) {
        seeder.seedIfEmpty();
        seedCoreIfEmpty(user);
        return questionnaireRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public QuestionnaireResponse get(User user, long id) {
        seeder.seedIfEmpty();
        return toResponse(require(id));
    }

    @Transactional
    public QuestionnaireResponse create(User admin, UpsertQuestionnaireRequest req) {
        vendorService.requireAdmin(admin);
        seeder.seedIfEmpty();
        String name = req.name().trim();
        if (questionnaireRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A questionnaire named \"" + name + "\" already exists");
        }
        TprmQuestionnaire q = new TprmQuestionnaire();
        q.setName(name);
        q.setCreatedByUserId(admin.getId());
        q = questionnaireRepository.save(q);
        auditTrailService.record(AuditActionType.CREATED, "TPRM", admin.getId(),
                TprmVendorService.adminRoles(admin), "TPRM_QUESTIONNAIRE", String.valueOf(q.getId()),
                q.getName(), null, null, "created", null, null);
        // New questionnaires start empty. Do not copy the 50 library controls.
        return toResponse(q);
    }

    @Transactional
    public QuestionnaireResponse save(User admin, long id, UpsertQuestionnaireRequest req) {
        vendorService.requireAdmin(admin);
        TprmQuestionnaire q = require(id);
        String name = req.name().trim();
        if (questionnaireRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A questionnaire named \"" + name + "\" already exists");
        }
        q.setName(name);
        questionnaireRepository.save(q);
        if (req.items() != null) {
            replaceItems(q, req.items(), Boolean.TRUE.equals(req.draft()));
        }
        return toResponse(q);
    }

    @Transactional(readOnly = true)
    public ExportFile sample(long id) {
        TprmQuestionnaire q = require(id);
        return blankTemplate(slug(q.getName()) + "-questionnaire-template.xlsx");
    }

    @Transactional(readOnly = true)
    public ExportFile librarySample() {
        return blankTemplate("tprm-questionnaire-template.xlsx");
    }

    @Transactional
    public QuestionnaireResponse upload(User admin, long id, MultipartFile file) {
        vendorService.requireAdmin(admin);
        TprmQuestionnaire q = require(id);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read upload");
        }
        List<TprmQuestionnaireExcel.TemplateParsed> parsed = excel.parseTemplate(bytes);
        if (parsed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Questionnaire has no control rows");
        }
        List<QuestionnaireItemRequest> rows = new ArrayList<>();
        for (TprmQuestionnaireExcel.TemplateParsed row : parsed) {
            rows.add(new QuestionnaireItemRequest(
                    null, row.controlCode(), row.domain(), row.requirement(),
                    row.requiredEvidence(), row.evidenceMandatory()));
        }
        appendItems(q, rows);
        return toResponse(q);
    }

    TprmQuestionnaire require(long id) {
        return questionnaireRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Questionnaire not found"));
    }

    List<TprmQuestionnaireItem> items(long questionnaireId) {
        return itemRepository.findByQuestionnaireIdOrderBySortOrderAscIdAsc(questionnaireId);
    }

    /** Resolve a library control or create a CUSTOM row so projects always have control_id. */
    public TprmControl ensureControl(TprmQuestionnaireItem item) {
        String userCode = item.getControlCode() != null ? item.getControlCode().trim() : "";
        if (userCode.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Control ID is required");
        }
        TprmControl library = controlRepository.findByControlCodeIgnoreCase(userCode)
                .filter(c -> !CUSTOM_MODULE.equals(c.getModule()))
                .orElse(null);
        if (library != null) {
            item.setControlId(library.getId());
            itemRepository.save(item);
            return library;
        }
        TprmControl custom = null;
        if (item.getControlId() != null) {
            custom = controlRepository.findById(item.getControlId()).orElse(null);
            if (custom != null && !CUSTOM_MODULE.equals(custom.getModule())) {
                custom = null;
            }
        }
        if (custom == null) {
            custom = new TprmControl();
            custom.setControlCode(internalCode(item));
            custom.setAskedFromTier(4);
            custom.setModule(CUSTOM_MODULE);
            custom.setWeight(1);
            custom.setWeightLabel("Standard");
            custom.setSortOrder(item.getSortOrder());
        }
        applyCustomControl(custom, item);
        custom = controlRepository.save(custom);
        item.setControlId(custom.getId());
        itemRepository.save(item);
        return custom;
    }

    private void replaceItems(TprmQuestionnaire q, List<QuestionnaireItemRequest> reqs, boolean draft) {
        List<QuestionnaireItemRequest> rows = new ArrayList<>();
        Map<String, Integer> seen = new HashMap<>();
        for (QuestionnaireItemRequest r : reqs) {
            String code = trimToNull(r.controlCode());
            if (code == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Control ID is required");
            }
            if (!draft && (trimToNull(r.domain()) == null || trimToNull(r.requirement()) == null)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Domain and Control requirement are required");
            }
            String key = code.toLowerCase(Locale.ROOT);
            if (seen.merge(key, 1, Integer::sum) > 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Control ID \"" + code + "\" already exists on this questionnaire");
            }
            rows.add(r);
        }
        Map<Long, TprmQuestionnaireItem> existing = new HashMap<>();
        for (TprmQuestionnaireItem item : itemRepository.findByQuestionnaireIdOrderBySortOrderAscIdAsc(q.getId())) {
            existing.put(item.getId(), item);
        }
        LinkedHashSet<Long> incomingIds = new LinkedHashSet<>();
        for (QuestionnaireItemRequest r : rows) {
            if (r.id() != null) {
                incomingIds.add(r.id());
            }
        }
        for (TprmQuestionnaireItem old : existing.values()) {
            if (!incomingIds.contains(old.getId())) {
                itemRepository.delete(old);
            }
        }
        itemRepository.flush();
        int order = 1;
        for (QuestionnaireItemRequest r : rows) {
            TprmQuestionnaireItem item;
            if (r.id() != null) {
                item = existing.get(r.id());
                if (item == null || !item.getQuestionnaireId().equals(q.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question not found");
                }
            } else {
                item = new TprmQuestionnaireItem();
                item.setQuestionnaireId(q.getId());
            }
            item.setControlCode(trimToNull(r.controlCode()));
            item.setDomain(trimToNull(r.domain()));
            item.setRequirement(trimToNull(r.requirement()));
            item.setRequiredEvidence(trimToNull(r.requiredEvidence()));
            item.setEvidenceMandatory(Boolean.TRUE.equals(r.evidenceMandatory()));
            item.setSortOrder(order++);
            item = itemRepository.save(item);
            ensureControl(item);
        }
    }

    /** Excel upload adds rows after existing questions. Same Control ID as an existing row is rejected. */
    private void appendItems(TprmQuestionnaire q, List<QuestionnaireItemRequest> reqs) {
        List<TprmQuestionnaireItem> existing =
                itemRepository.findByQuestionnaireIdOrderBySortOrderAscIdAsc(q.getId());
        Map<String, String> existingCodes = new HashMap<>();
        int order = 0;
        for (TprmQuestionnaireItem item : existing) {
            String code = trimToNull(item.getControlCode());
            if (code != null) {
                existingCodes.put(code.toLowerCase(Locale.ROOT), code);
            }
            order = Math.max(order, item.getSortOrder());
        }
        Map<String, Integer> seen = new HashMap<>();
        List<QuestionnaireItemRequest> rows = new ArrayList<>();
        for (QuestionnaireItemRequest r : reqs) {
            String code = trimToNull(r.controlCode());
            if (code == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Control ID is required");
            }
            String key = code.toLowerCase(Locale.ROOT);
            if (seen.merge(key, 1, Integer::sum) > 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Control ID \"" + code + "\" is duplicated in the upload");
            }
            String clash = existingCodes.get(key);
            if (clash != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Control ID \"" + clash + "\" already exists on this questionnaire");
            }
            rows.add(r);
        }
        for (QuestionnaireItemRequest r : rows) {
            TprmQuestionnaireItem item = new TprmQuestionnaireItem();
            item.setQuestionnaireId(q.getId());
            item.setControlCode(trimToNull(r.controlCode()));
            item.setDomain(trimToNull(r.domain()));
            item.setRequirement(trimToNull(r.requirement()));
            item.setRequiredEvidence(trimToNull(r.requiredEvidence()));
            item.setEvidenceMandatory(Boolean.TRUE.equals(r.evidenceMandatory()));
            item.setSortOrder(++order);
            item = itemRepository.save(item);
            ensureControl(item);
        }
    }

    private void seedCoreIfEmpty(User admin) {
        if (questionnaireRepository.count() > 0) {
            return;
        }
        TprmQuestionnaire q = new TprmQuestionnaire();
        q.setName(CORE_NAME);
        q.setCreatedByUserId(admin.getId());
        q = questionnaireRepository.save(q);
        // Named starter only. Admins add questions on screen or by upload.
    }

    private QuestionnaireResponse toResponse(TprmQuestionnaire q) {
        Map<Long, TprmControl> controls = new HashMap<>();
        for (TprmControl c : controlRepository.findAllByOrderBySortOrderAsc()) {
            controls.put(c.getId(), c);
        }
        List<QuestionnaireResponse.ItemView> items = new ArrayList<>();
        for (TprmQuestionnaireItem item : itemRepository.findByQuestionnaireIdOrderBySortOrderAscIdAsc(q.getId())) {
            TprmControl c = item.getControlId() != null ? controls.get(item.getControlId()) : null;
            String code = TprmControlDisplay.first(item.getControlCode(), c != null ? c.getControlCode() : null);
            if (code.isBlank()) {
                continue;
            }
            items.add(new QuestionnaireResponse.ItemView(
                    item.getId(), item.getControlId(), code,
                    TprmControlDisplay.first(item.getDomain(), c != null ? c.getDomain() : null),
                    TprmControlDisplay.first(item.getRequirement(), c != null ? c.getRequirement() : null),
                    TprmControlDisplay.first(item.getRequiredEvidence(), c != null ? c.getRequiredEvidence() : null),
                    item.isEvidenceMandatory(), item.getSortOrder()));
        }
        return QuestionnaireResponse.from(q, items);
    }

    private ExportFile blankTemplate(String filename) {
        return new ExportFile(excel.writeBlankTemplate(), filename, XLSX);
    }

    private void applyCustomControl(TprmControl custom, TprmQuestionnaireItem item) {
        custom.setDomain(clip(blankToDash(item.getDomain()), 80));
        String requirement = blankToDash(item.getRequirement());
        custom.setRequirement(clip(requirement, 500));
        custom.setDescription(clip(requirement, 4000));
        custom.setRequiredEvidence(clip(blankToDash(item.getRequiredEvidence()), 2000));
        custom.setMandatory(item.isEvidenceMandatory());
        custom.setSortOrder(item.getSortOrder());
    }

    private static String internalCode(TprmQuestionnaireItem item) {
        String code = "Q" + item.getQuestionnaireId() + "I" + item.getId();
        return clip(code, 40);
    }

    private static String trimToNull(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static String blankToDash(String v) {
        String t = trimToNull(v);
        return t == null ? "-" : t;
    }

    private static String clip(String v, int max) {
        if (v == null) {
            return null;
        }
        return v.length() <= max ? v : v.substring(0, max);
    }

    private static String slug(String name) {
        String s = name == null ? "questionnaire" : name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        return s.isBlank() ? "questionnaire" : s.replaceAll("^-+|-+$", "");
    }
}
