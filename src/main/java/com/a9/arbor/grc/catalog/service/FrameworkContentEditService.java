package com.a9.arbor.grc.catalog.service;

import com.a9.arbor.grc.catalog.domain.CatalogContentAudit;
import com.a9.arbor.grc.catalog.domain.Framework;
import com.a9.arbor.grc.catalog.domain.FrameworkNode;
import com.a9.arbor.grc.catalog.domain.FrameworkNodeAttribute;
import com.a9.arbor.grc.catalog.repo.CatalogContentAuditRepository;
import com.a9.arbor.grc.catalog.repo.FrameworkNodeAttributeRepository;
import com.a9.arbor.grc.catalog.repo.FrameworkNodeRepository;
import com.a9.arbor.grc.catalog.repo.FrameworkRepository;
import com.a9.arbor.grc.catalog.repo.TenantFrameworkRepository;
import com.a9.arbor.grc.catalog.web.dto.CreateFrameworkNodeRequest;
import com.a9.arbor.grc.catalog.web.dto.FrameworkDetailResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkNodeResponse;
import com.a9.arbor.grc.catalog.web.dto.RenameSheetTabRequest;
import com.a9.arbor.grc.catalog.web.dto.ReorderFrameworkNodesRequest;
import com.a9.arbor.grc.catalog.web.dto.UpdateFrameworkNodeRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Super Admin in-place catalog content editing (Framework Import V2 Phase 6C).
 * Tenant Admin fork is deferred.
 */
@Service
public class FrameworkContentEditService {

    private static final Set<String> EDITABLE_ATTRS = Set.of(
            "what_it_means", "evidence", "default_applicability", "sheet_tab", "section_type");

    private final FrameworkRepository frameworkRepository;
    private final FrameworkNodeRepository nodeRepository;
    private final FrameworkNodeAttributeRepository attributeRepository;
    private final TenantFrameworkRepository tenantFrameworkRepository;
    private final CatalogContentAuditRepository auditRepository;
    private final FrameworkLibraryService frameworkLibraryService;

    public FrameworkContentEditService(FrameworkRepository frameworkRepository,
                                       FrameworkNodeRepository nodeRepository,
                                       FrameworkNodeAttributeRepository attributeRepository,
                                       TenantFrameworkRepository tenantFrameworkRepository,
                                       CatalogContentAuditRepository auditRepository,
                                       FrameworkLibraryService frameworkLibraryService) {
        this.frameworkRepository = frameworkRepository;
        this.nodeRepository = nodeRepository;
        this.attributeRepository = attributeRepository;
        this.tenantFrameworkRepository = tenantFrameworkRepository;
        this.auditRepository = auditRepository;
        this.frameworkLibraryService = frameworkLibraryService;
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public FrameworkNodeResponse updateNode(long frameworkId, long nodeId,
                                            UpdateFrameworkNodeRequest req, String actorEmail) {
        FrameworkNode node = requireNode(frameworkId, nodeId);
        if (req.code() != null && !req.code().isBlank() && !req.code().trim().equals(node.getNodeCode())) {
            String code = req.code().trim();
            nodeRepository.findByFrameworkIdAndNodeCode(frameworkId, code).ifPresent(other -> {
                if (!other.getId().equals(nodeId)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Node code already exists: " + code);
                }
            });
            audit(frameworkId, nodeId, "UPDATE", "code", node.getNodeCode(), code, actorEmail);
            node.setNodeCode(code);
        }
        change(frameworkId, nodeId, "title", node.getTitle(), req.title().trim(), actorEmail,
                () -> node.setTitle(req.title().trim()));
        change(frameworkId, nodeId, "definition", node.getDefinitionText(), blankToNull(req.definition()), actorEmail,
                () -> node.setDefinitionText(blankToNull(req.definition())));
        if (req.controlType() != null) {
            change(frameworkId, nodeId, "controlType", node.getControlType(), blankToNull(req.controlType()), actorEmail,
                    () -> node.setControlType(blankToNull(req.controlType())));
        }
        if (req.priority() != null) {
            change(frameworkId, nodeId, "priority", node.getPriority(), blankToNull(req.priority()), actorEmail,
                    () -> node.setPriority(blankToNull(req.priority())));
        }
        if (req.recordType() != null) {
            change(frameworkId, nodeId, "recordType", node.getRecordType(), blankToNull(req.recordType()), actorEmail,
                    () -> node.setRecordType(blankToNull(req.recordType())));
        }
        if (req.officialRef() != null) {
            change(frameworkId, nodeId, "officialRef", node.getOfficialRef(), blankToNull(req.officialRef()), actorEmail,
                    () -> node.setOfficialRef(blankToNull(req.officialRef())));
        }
        if (req.inSoa() != null) {
            change(frameworkId, nodeId, "inSoa", String.valueOf(node.isInSoa()), String.valueOf(req.inSoa()), actorEmail,
                    () -> node.setInSoa(req.inSoa()));
        }
        if (req.inGapAssessment() != null) {
            change(frameworkId, nodeId, "inGapAssessment", String.valueOf(node.isInGapAssessment()),
                    String.valueOf(req.inGapAssessment()), actorEmail,
                    () -> node.setInGapAssessment(req.inGapAssessment()));
        }
        if (req.inAuditChecklist() != null) {
            change(frameworkId, nodeId, "inAuditChecklist", String.valueOf(node.isInAuditChecklist()),
                    String.valueOf(req.inAuditChecklist()), actorEmail,
                    () -> node.setInAuditChecklist(req.inAuditChecklist()));
        }
        if (req.inRiskRegister() != null) {
            change(frameworkId, nodeId, "inRiskRegister", String.valueOf(node.isInRiskRegister()),
                    String.valueOf(req.inRiskRegister()), actorEmail,
                    () -> node.setInRiskRegister(req.inRiskRegister()));
        }
        nodeRepository.save(node);
        if (req.attributes() != null) {
            upsertAttributes(frameworkId, nodeId, req.attributes(), actorEmail);
        }
        return toResponse(node);
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public FrameworkNodeResponse createNode(long frameworkId, CreateFrameworkNodeRequest req, String actorEmail) {
        requireFramework(frameworkId);
        String code = req.code().trim();
        if (nodeRepository.findByFrameworkIdAndNodeCode(frameworkId, code).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Node code already exists: " + code);
        }
        int level = 1;
        Long parentId = req.parentId();
        if (parentId != null) {
            FrameworkNode parent = requireNode(frameworkId, parentId);
            level = parent.getLevel() + 1;
        }
        int nextOrder = nodeRepository.findByFrameworkIdOrderByDisplayOrderAscNodeCodeAsc(frameworkId).stream()
                .mapToInt(FrameworkNode::getDisplayOrder)
                .max()
                .orElse(0) + 1;

        FrameworkNode node = new FrameworkNode();
        node.setFrameworkId(frameworkId);
        node.setParentId(parentId);
        node.setLevel(level);
        node.setNodeCode(code);
        node.setTitle(req.title().trim());
        node.setDefinitionText(blankToNull(req.definition()));
        node.setControlType(blankToNull(req.controlType()));
        node.setPriority(blankToNull(req.priority()));
        node.setRecordType(blankToNull(req.recordType()) != null ? blankToNull(req.recordType()) : "CONTROL");
        node.setOfficialRef(blankToNull(req.officialRef()));
        node.setDisplayOrder(nextOrder);
        node.setInSoa(req.inSoa());
        node.setInGapAssessment(req.inGapAssessment());
        node.setInAuditChecklist(req.inAuditChecklist());
        node.setInRiskRegister(req.inRiskRegister());
        nodeRepository.save(node);

        if (req.attributes() != null && !req.attributes().isEmpty()) {
            upsertAttributes(frameworkId, node.getId(), req.attributes(), actorEmail);
        }
        audit(frameworkId, node.getId(), "CREATE", null, null, code + ": " + node.getTitle(), actorEmail);
        return toResponse(node);
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public FrameworkDetailResponse reorder(long frameworkId, ReorderFrameworkNodesRequest req, String actorEmail) {
        requireFramework(frameworkId);
        List<FrameworkNode> all = nodeRepository.findByFrameworkIdOrderByDisplayOrderAscNodeCodeAsc(frameworkId);
        Map<Long, FrameworkNode> byId = new HashMap<>();
        for (FrameworkNode n : all) {
            byId.put(n.getId(), n);
        }
        Set<Long> seen = new HashSet<>();
        int order = 1;
        for (Long id : req.nodeIdsInOrder()) {
            FrameworkNode node = byId.get(id);
            if (node == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown node id: " + id);
            }
            if (!seen.add(id)) {
                continue;
            }
            if (node.getDisplayOrder() != order) {
                node.setDisplayOrder(order);
                nodeRepository.save(node);
            }
            order++;
        }
        // Append any nodes not listed (e.g. other sheets) after the reordered set.
        for (FrameworkNode node : all) {
            if (seen.contains(node.getId())) {
                continue;
            }
            if (node.getDisplayOrder() != order) {
                node.setDisplayOrder(order);
                nodeRepository.save(node);
            }
            order++;
        }
        audit(frameworkId, null, "REORDER", "displayOrder", null,
                "reordered " + seen.size() + " nodes", actorEmail);
        return frameworkLibraryService.getDetail(frameworkId);
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public FrameworkDetailResponse renameSheetTab(long frameworkId, RenameSheetTabRequest req, String actorEmail) {
        requireFramework(frameworkId);
        String from = req.fromLabel().trim();
        String to = req.toLabel().trim();
        if (from.equals(to)) {
            return frameworkLibraryService.getDetail(frameworkId);
        }
        List<FrameworkNode> nodes = nodeRepository.findByFrameworkIdOrderByDisplayOrderAscNodeCodeAsc(frameworkId);
        int count = 0;
        for (FrameworkNode node : nodes) {
            var opt = attributeRepository.findByNodeIdAndAttrKey(node.getId(), "sheet_tab");
            if (opt.isEmpty() || !from.equals(opt.get().getAttrValue())) {
                continue;
            }
            FrameworkNodeAttribute attr = opt.get();
            attr.setAttrValue(to);
            attributeRepository.save(attr);
            count++;
        }
        if (count == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No nodes with sheet tab: " + from);
        }
        audit(frameworkId, null, "RENAME_TAB", "sheet_tab", from, to + " (" + count + " nodes)", actorEmail);
        return frameworkLibraryService.getDetail(frameworkId);
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public FrameworkNodeResponse retireNode(long frameworkId, long nodeId, String actorEmail) {
        FrameworkNode node = requireNode(frameworkId, nodeId);
        if (node.isRetired()) {
            return toResponse(node);
        }
        node.setRetiredAt(Instant.now());
        nodeRepository.save(node);
        audit(frameworkId, nodeId, "RETIRE", "retiredAt", null, node.getRetiredAt().toString(), actorEmail);
        return toResponse(node);
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public FrameworkNodeResponse unretireNode(long frameworkId, long nodeId, String actorEmail) {
        FrameworkNode node = requireNode(frameworkId, nodeId);
        if (!node.isRetired()) {
            return toResponse(node);
        }
        String old = node.getRetiredAt() != null ? node.getRetiredAt().toString() : null;
        node.setRetiredAt(null);
        nodeRepository.save(node);
        audit(frameworkId, nodeId, "UNRETIRE", "retiredAt", old, null, actorEmail);
        return toResponse(node);
    }

    /**
     * Hard-delete only when the framework has no tenant activations. Otherwise soft-retire.
     */
    @Transactional(transactionManager = "catalogTransactionManager")
    public FrameworkDetailResponse deleteOrRetireNode(long frameworkId, long nodeId, String actorEmail) {
        FrameworkNode node = requireNode(frameworkId, nodeId);
        boolean activated = tenantFrameworkRepository.existsByFrameworkId(frameworkId);
        List<FrameworkNode> children = nodeRepository.findByFrameworkIdOrderByDisplayOrderAscNodeCodeAsc(frameworkId)
                .stream()
                .filter(n -> Objects.equals(n.getParentId(), nodeId))
                .toList();
        if (!children.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Retire or re-parent child nodes first (" + children.size() + " children)");
        }
        if (activated) {
            retireNode(frameworkId, nodeId, actorEmail);
            return frameworkLibraryService.getDetail(frameworkId);
        }
        attributeRepository.findByNodeIdOrderByAttrKeyAsc(nodeId).forEach(attributeRepository::delete);
        nodeRepository.delete(node);
        audit(frameworkId, nodeId, "DELETE", null, node.getNodeCode(), null, actorEmail);
        return frameworkLibraryService.getDetail(frameworkId);
    }

    private void upsertAttributes(long frameworkId, long nodeId, Map<String, String> attrs, String actorEmail) {
        for (Map.Entry<String, String> e : attrs.entrySet()) {
            String key = e.getKey() == null ? "" : e.getKey().trim().toLowerCase(Locale.ROOT);
            if (!EDITABLE_ATTRS.contains(key)) {
                continue;
            }
            String newVal = blankToNull(e.getValue());
            var existing = attributeRepository.findByNodeIdAndAttrKey(nodeId, key);
            String oldVal = existing.map(FrameworkNodeAttribute::getAttrValue).orElse(null);
            if (Objects.equals(oldVal, newVal)) {
                continue;
            }
            if (newVal == null) {
                existing.ifPresent(attributeRepository::delete);
            } else if (existing.isPresent()) {
                FrameworkNodeAttribute attr = existing.get();
                attr.setAttrValue(newVal);
                attributeRepository.save(attr);
            } else {
                FrameworkNodeAttribute attr = new FrameworkNodeAttribute();
                attr.setNodeId(nodeId);
                attr.setAttrKey(key);
                attr.setAttrValue(newVal);
                attributeRepository.save(attr);
            }
            audit(frameworkId, nodeId, "UPDATE", "attr:" + key, oldVal, newVal, actorEmail);
        }
    }

    private FrameworkNodeResponse toResponse(FrameworkNode node) {
        Map<String, String> attrs = new LinkedHashMap<>();
        for (FrameworkNodeAttribute a : attributeRepository.findByNodeIdOrderByAttrKeyAsc(node.getId())) {
            attrs.put(a.getAttrKey(), a.getAttrValue());
        }
        return FrameworkNodeResponse.from(node, attrs);
    }

    private Framework requireFramework(long id) {
        return frameworkRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Framework not found"));
    }

    private FrameworkNode requireNode(long frameworkId, long nodeId) {
        FrameworkNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Node not found"));
        if (!node.getFrameworkId().equals(frameworkId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Node not found");
        }
        return node;
    }

    private void change(long frameworkId, long nodeId, String field, String oldVal, String newVal,
                        String actorEmail, Runnable apply) {
        if (Objects.equals(oldVal, newVal)) {
            return;
        }
        apply.run();
        audit(frameworkId, nodeId, "UPDATE", field, oldVal, newVal, actorEmail);
    }

    private void audit(long frameworkId, Long nodeId, String action, String field,
                       String oldVal, String newVal, String actorEmail) {
        CatalogContentAudit row = new CatalogContentAudit();
        row.setFrameworkId(frameworkId);
        row.setNodeId(nodeId);
        row.setAction(action);
        row.setFieldName(field);
        row.setOldValue(truncate(oldVal));
        row.setNewValue(truncate(newVal));
        row.setActorEmail(actorEmail);
        row.setCreatedAt(Instant.now());
        auditRepository.save(row);
    }

    private static String truncate(String v) {
        if (v == null) {
            return null;
        }
        return v.length() > 8000 ? v.substring(0, 8000) : v;
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
