package com.a9.arbor.grc.modules.auditmgmt.domain;

public enum AuditStandard {
    ISO_27001,
    ISO_20000,
    UAE_IA,
    ISO_27701,
    MULTIPLE;

    public ChecklistTemplateKey defaultTemplate() {
        return switch (this) {
            case ISO_20000 -> ChecklistTemplateKey.ISO_20000;
            case UAE_IA -> ChecklistTemplateKey.UAE_IA;
            default -> ChecklistTemplateKey.ISO_27001;
        };
    }

    public String displayName() {
        return switch (this) {
            case ISO_27001 -> "ISO 27001";
            case ISO_20000 -> "ISO 20000";
            case UAE_IA -> "UAE IA";
            case ISO_27701 -> "ISO 27701";
            case MULTIPLE -> "Multiple";
        };
    }
}
