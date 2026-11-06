package com.a9.arbor.grc.catalog.service;

import com.a9.arbor.grc.catalog.domain.Framework;
import com.a9.arbor.grc.catalog.domain.FrameworkNode;
import com.a9.arbor.grc.catalog.domain.FrameworkNodeAttribute;
import com.a9.arbor.grc.catalog.repo.FrameworkNodeAttributeRepository;
import com.a9.arbor.grc.catalog.repo.FrameworkNodeRepository;
import com.a9.arbor.grc.catalog.repo.FrameworkRepository;
import com.a9.arbor.grc.catalog.web.dto.FrameworkNodeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Read-only catalog lookups for tenant modules (Gap scoring, audit checklist metadata). */
@Service
public class FrameworkQueryService {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkNodeRepository nodeRepository;
    private final FrameworkNodeAttributeRepository nodeAttributeRepository;

    public FrameworkQueryService(FrameworkRepository frameworkRepository,
                                 FrameworkNodeRepository nodeRepository,
                                 FrameworkNodeAttributeRepository nodeAttributeRepository) {
        this.frameworkRepository = frameworkRepository;
        this.nodeRepository = nodeRepository;
        this.nodeAttributeRepository = nodeAttributeRepository;
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public Optional<Framework> findFramework(long frameworkId) {
        return frameworkRepository.findById(frameworkId);
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public Map<Long, FrameworkNodeResponse> nodesById(long frameworkId) {
        List<FrameworkNode> nodes = nodeRepository
                .findByFrameworkIdOrderByLevelAscDisplayOrderAscNodeCodeAsc(frameworkId);
        Map<Long, Map<String, String>> attrsByNode = new HashMap<>();
        if (!nodes.isEmpty()) {
            List<Long> ids = nodes.stream().map(FrameworkNode::getId).toList();
            for (FrameworkNodeAttribute attribute : nodeAttributeRepository.findByNodeIdIn(ids)) {
                attrsByNode
                        .computeIfAbsent(attribute.getNodeId(), k -> new LinkedHashMap<>())
                        .put(attribute.getAttrKey(), attribute.getAttrValue());
            }
        }
        Map<Long, FrameworkNodeResponse> out = new HashMap<>();
        for (FrameworkNode node : nodes) {
            out.put(node.getId(), FrameworkNodeResponse.from(
                    node, attrsByNode.getOrDefault(node.getId(), Map.of())));
        }
        return out;
    }

    /**
     * One control and its ancestors. Save and the control drawer only need this path,
     * not every node and attribute in the framework.
     */
    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public Map<Long, FrameworkNodeResponse> nodePath(long frameworkId, long nodeId) {
        Map<Long, FrameworkNode> byId = new LinkedHashMap<>();
        Long current = nodeId;
        int guard = 0;
        while (current != null && guard++ < 12) {
            FrameworkNode node = nodeRepository.findById(current).orElse(null);
            if (node == null || node.getFrameworkId() == null || node.getFrameworkId() != frameworkId) {
                break;
            }
            byId.put(node.getId(), node);
            current = node.getParentId();
        }
        if (byId.isEmpty()) {
            return Map.of();
        }
        Map<Long, Map<String, String>> attrsByNode = new HashMap<>();
        for (FrameworkNodeAttribute attribute : nodeAttributeRepository.findByNodeIdIn(byId.keySet())) {
            attrsByNode
                    .computeIfAbsent(attribute.getNodeId(), k -> new LinkedHashMap<>())
                    .put(attribute.getAttrKey(), attribute.getAttrValue());
        }
        Map<Long, FrameworkNodeResponse> out = new HashMap<>();
        for (FrameworkNode node : byId.values()) {
            out.put(node.getId(), FrameworkNodeResponse.from(
                    node, attrsByNode.getOrDefault(node.getId(), Map.of())));
        }
        return out;
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public Framework requireFramework(long frameworkId) {
        return frameworkRepository.findById(frameworkId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Framework not found"));
    }
}
