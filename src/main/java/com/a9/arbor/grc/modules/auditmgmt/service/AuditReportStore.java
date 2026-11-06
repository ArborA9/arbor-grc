package com.a9.arbor.grc.modules.auditmgmt.service;

import org.springframework.core.io.Resource;

/** Binary storage for locked audit report PDFs. */
public interface AuditReportStore {

    String backend();

    StoredReport store(long auditId, byte[] pdfBytes);

    Resource open(String storageKey);

    record StoredReport(String storageKey, long sizeBytes) {
    }
}
