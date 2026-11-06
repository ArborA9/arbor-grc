package com.a9.arbor.grc.modules.dashboard.web.dto;

import java.util.List;

/**
 * One entry in the Reports hub (req §19.3). {@code requiresProject} means the export endpoint
 * needs a specific record id (SoA / Gap / locked Audit) selected by the client before export.
 */
public record ReportDescriptor(
        String id,
        String name,
        List<String> formats,
        String module,
        boolean requiresProject) {

    public static ReportDescriptor of(String id, String name, List<String> formats, String module) {
        return new ReportDescriptor(id, name, formats, module, false);
    }
}
