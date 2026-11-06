package com.a9.arbor.grc.catalog.domain;

/**
 * Standard Type A framework types (req §9.2 / §21.2 A1). ISO 27001 / 20000 / 27017 / UAE IA
 * accept the GRC Checklist workbook; other types use the Arbor canonical Controls template.
 */
public enum FrameworkTypeCode {
    ISO_27001("ISO/IEC 27001", ModuleCode.ISMS, "2022"),
    // V2.1 is the version the client's reference pack carries; the V2.0 rows it supersedes came from
    // the generated sample workbook during E2E runs, not from client content.
    UAE_IA("UAE IA (TDRA)", ModuleCode.ISMS, "V2.1"),
    ISO_27017("ISO/IEC 27017", ModuleCode.ISMS, "2015"),
    ISO_31000("ISO 31000", ModuleCode.ERM, "2018"),
    ISO_22301("ISO 22301", ModuleCode.BCMS, "2019"),
    NCEMA_V3("NCEMA V3", ModuleCode.BCMS, "V3"),
    UAE_PDPL("UAE PDPL", ModuleCode.PRIVACY, "2021"),
    // Pack filename is ISO27701-2025_*; default must match Import V2 (Doc 02 T-09).
    ISO_27701("ISO/IEC 27701", ModuleCode.PRIVACY, "2025"),
    // Product catalog title is ISO 20000. Import writes this name; do not use the part number.
    ISO_20000("ISO 20000", ModuleCode.ITSM, "2018"),
    ISO_42001("ISO/IEC 42001", ModuleCode.AIMS, "2023"),
    // F3c. No new ModuleCode: the two NIST frameworks and CIS are security control sets (ISMS) and
    // GDPR sits with the other data-protection law (PRIVACY). A new module would mean TenantModule
    // rows, @RequiresModule wiring and licensing UI for no product benefit.
    NIST_CSF("NIST CSF", ModuleCode.ISMS, "2.0"),
    CIS_V8("CIS Controls", ModuleCode.ISMS, "v8"),
    // "2016" rather than the usual "2016/679": version reaches URLs and filenames, and a slash there
    // is a trap for no gain.
    GDPR("GDPR", ModuleCode.PRIVACY, "2016"),
    PCI_DSS("PCI DSS", ModuleCode.ISMS, "4.0.1"),
    NIST_800_53("NIST SP 800-53", ModuleCode.ISMS, "Rev5"),
    ISO_9001("ISO 9001", ModuleCode.QMS, "2015"),
    ISO_45001("ISO 45001", ModuleCode.OHSMS, "2018"),
    SAMA_CSF("SAMA CSF", ModuleCode.SAMA, "2017");

    private final String displayName;
    private final ModuleCode moduleCode;
    private final String defaultVersion;

    FrameworkTypeCode(String displayName, ModuleCode moduleCode, String defaultVersion) {
        this.displayName = displayName;
        this.moduleCode = moduleCode;
        this.defaultVersion = defaultVersion;
    }

    public String displayName() {
        return displayName;
    }

    public ModuleCode moduleCode() {
        return moduleCode;
    }

    public String defaultVersion() {
        return defaultVersion;
    }
}
