package com.a9.arbor.grc.modules.tprm.domain;

public enum TprmCertType {
    NONE,
    ISO_27001,
    ISO_27017_27018,
    ISO_42001,
    SOC2_TYPE_II,
    UAE_IA,
    OTHER;

    public String label() {
        return switch (this) {
            case NONE -> "None";
            case ISO_27001 -> "ISO/IEC 27001";
            case ISO_27017_27018 -> "ISO/IEC 27017/27018";
            case ISO_42001 -> "ISO/IEC 42001";
            case SOC2_TYPE_II -> "SOC 2 Type II";
            case UAE_IA -> "UAE IA compliance attestation";
            case OTHER -> "Others";
        };
    }

    public boolean countsTowardCredit() {
        return this != NONE;
    }
}
