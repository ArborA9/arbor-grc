package com.a9.arbor.grc.modules.auditmgmt.service;

import com.a9.arbor.grc.config.ArborAuditProperties;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@Component
@ConditionalOnProperty(name = "arbor.audit.reports.storage", havingValue = "local", matchIfMissing = true)
public class LocalAuditReportStore implements AuditReportStore {

    private final ArborAuditProperties properties;

    public LocalAuditReportStore(ArborAuditProperties properties) {
        this.properties = properties;
    }

    @Override
    public String backend() {
        return "local";
    }

    @Override
    public StoredReport store(long auditId, byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PDF is required");
        }
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant required");
        }
        String key = tenantId + "/audits/" + auditId + "/report-" + Instant.now().toEpochMilli() + ".pdf";
        Path dest = root().resolve(key).normalize();
        if (!dest.startsWith(root())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storage path");
        }
        try {
            Files.createDirectories(dest.getParent());
            Files.write(dest, pdfBytes);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store audit report", e);
        }
        return new StoredReport(key, pdfBytes.length);
    }

    @Override
    public Resource open(String storageKey) {
        Path path = root().resolve(storageKey).normalize();
        if (!path.startsWith(root()) || !Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found");
        }
        return new FileSystemResource(path);
    }

    private Path root() {
        return Path.of(properties.getReports().getStorageRoot()).toAbsolutePath().normalize();
    }
}
