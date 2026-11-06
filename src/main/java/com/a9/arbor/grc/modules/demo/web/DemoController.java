package com.a9.arbor.grc.modules.demo.web;

import com.a9.arbor.grc.modules.demo.domain.PingRecord;
import com.a9.arbor.grc.modules.demo.repo.PingRecordRepository;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Tenant-scoped demo endpoints. Every read/write here is routed to the current tenant's own
 * database, proving isolation. Requires the {@code X-Tenant-ID} header (set by the resolver filter).
 */
@RestController
@RequestMapping("/api/demo/ping")
@ConditionalOnProperty(name = "arbor.security.demo-endpoints-enabled", havingValue = "true", matchIfMissing = true)
public class DemoController {

    private final PingRecordRepository repository;

    public DemoController(PingRecordRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    @Transactional
    public Map<String, Object> create(@RequestBody Map<String, String> body) {
        PingRecord record = new PingRecord();
        record.setNote(body.getOrDefault("note", "(no note)"));
        record = repository.save(record);
        return Map.of(
                "tenant", TenantContext.getTenantId(),
                "id", record.getId(),
                "note", record.getNote());
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Map<String, Object> list() {
        List<Map<String, Object>> records = repository.findAll().stream()
                .map(r -> Map.<String, Object>of(
                        "id", r.getId(),
                        "note", r.getNote(),
                        "createdAt", r.getCreatedAt()))
                .toList();
        return Map.of(
                "tenant", TenantContext.getTenantId(),
                "count", records.size(),
                "records", records);
    }
}
