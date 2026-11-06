package com.a9.arbor.grc.modules.tprm.domain;

public enum VendorType {
    IT_SERVICES,
    NON_IT_SERVICES,
    CLOUD_SERVICE_PROVIDER,
    AI_SERVICE_PROVIDER,
    MSSP,
    SOFTWARE_PRODUCT,
    PROFESSIONAL_SERVICES;

    public String label() {
        return switch (this) {
            case IT_SERVICES -> "IT Services";
            case NON_IT_SERVICES -> "Non-IT Services";
            case CLOUD_SERVICE_PROVIDER -> "Cloud Service Provider";
            case AI_SERVICE_PROVIDER -> "AI Service Provider";
            case MSSP -> "Managed Security Service Provider";
            case SOFTWARE_PRODUCT -> "Software / Product Vendor";
            case PROFESSIONAL_SERVICES -> "Professional Services / Consultancy";
        };
    }

    public boolean cloudAddon() {
        return this == CLOUD_SERVICE_PROVIDER || this == AI_SERVICE_PROVIDER || this == MSSP;
    }

    public boolean aiAddon() {
        return this == AI_SERVICE_PROVIDER;
    }

    public boolean omitSoftwareControls() {
        return this == NON_IT_SERVICES || this == PROFESSIONAL_SERVICES;
    }
}
