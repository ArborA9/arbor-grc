package com.a9.arbor.grc.modules.auditmgmt.service;

import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAudit;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditChecklistItem;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditFinding;

import java.time.Instant;
import java.util.List;

/** Everything the audit project report prints. Excel keeps full text. PDF lays the same fields out. */
public record AuditReportData(
        String organisation,
        String generatedBy,
        String classification,
        Instant generatedAt,
        GrcAudit audit,
        String leadName,
        String leadEmail,
        String auditeeName,
        String auditorSignedBy,
        String adminSignedBy,
        List<TeamMember> team,
        List<String> scopeModules,
        int scopeNodeCount,
        List<ChecklistSheet> sheets,
        List<FindingLine> findings,
        List<DomainNarrative> narratives,
        boolean includeWorkingNotes
) {
    public record TeamMember(String name, String email, String role) {
    }

    public record ChecklistSheet(String name, List<GrcAuditChecklistItem> items) {
    }

    public record FindingLine(GrcAuditFinding finding, String clauseRef, String capaLabel) {
    }

    public record DomainNarrative(String code, String name, String narrative) {
    }

    public int controlsInScope() {
        int n = 0;
        for (ChecklistSheet sheet : sheets) {
            for (GrcAuditChecklistItem item : sheet.items()) {
                if (item.isInScope()) {
                    n++;
                }
            }
        }
        return n;
    }

    public int reviewed() {
        int n = 0;
        for (ChecklistSheet sheet : sheets) {
            for (GrcAuditChecklistItem item : sheet.items()) {
                if (AuditReportLabels.countsAsReviewed(audit, item)) {
                    n++;
                }
            }
        }
        return n;
    }

    public int stillOpen() {
        return Math.max(0, controlsInScope() - reviewed());
    }

    public String checklistNames() {
        if (sheets.isEmpty()) {
            return "-";
        }
        return sheets.stream().map(ChecklistSheet::name).reduce((a, b) -> a + ", " + b).orElse("-");
    }

    public String frameworkLine() {
        String name = AuditReportLabels.dash(audit.getFrameworkName());
        String code = audit.getFrameworkCode() == null ? "" : audit.getFrameworkCode().trim();
        String version = audit.getFrameworkVersion() == null ? "" : audit.getFrameworkVersion().trim();
        if (code.isBlank() || "MULTIPLE".equalsIgnoreCase(code) || "Multiple".equalsIgnoreCase(name)) {
            return name;
        }
        String line = name;
        if (!code.isBlank() && !name.equalsIgnoreCase(code)) {
            line = name + " (" + code + ")";
        }
        if (!version.isBlank()) {
            line = line + " v" + version;
        }
        return line;
    }
}
