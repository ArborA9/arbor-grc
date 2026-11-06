package com.a9.arbor.grc.modules.auditmgmt.domain;

public enum ChecklistTemplateKey {
    ISO_27001,
    ISO_20000,
    UAE_IA,
    CUSTOM;

    public String defaultName() {
        return switch (this) {
            case ISO_27001 -> "Checklist ISO 27001";
            case ISO_20000 -> "Checklist ISO 20000";
            case UAE_IA -> "Checklist UAE IA";
            case CUSTOM -> "Custom checklist";
        };
    }
}
