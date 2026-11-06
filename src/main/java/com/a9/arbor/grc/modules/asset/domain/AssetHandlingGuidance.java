package com.a9.arbor.grc.modules.asset.domain;

/**
 * Handling guidance keyed by Confidentiality rating (hidden Classification and Handling sheet).
 * Labels on that sheet differ from Valuation/Lists classification names: we key by C, not label.
 */
public final class AssetHandlingGuidance {

    public record Guidance(
            int confidentiality,
            String sheetLabel,
            String labelling,
            String storage,
            String transfer,
            String access,
            String disposal
    ) {
    }

    private AssetHandlingGuidance() {
    }

    public static Guidance forConfidentiality(int confidentiality) {
        return switch (confidentiality) {
            case 5 -> new Guidance(5, "Secret",
                    "Mark SECRET on every page, in the document footer, in the email subject line and on physical media front and back.",
                    "Encrypted at rest on dedicated storage inside a restricted enclave. Hardcopy in a locked safe within a controlled room.",
                    "Encrypted channel only, recipient verified out of band. No personal email, no consumer file sharing, no removable media without written owner approval.",
                    "Named individuals only, approved by the asset owner. MFA mandatory. Access reviewed quarterly. All access logged and monitored.",
                    "Cross-cut shredding to DIN P-5 or higher, or certified destruction with the certificate retained. Electronic media physically destroyed or cryptographically erased.");
            case 4 -> new Guidance(4, "Confidential",
                    "Mark CONFIDENTIAL on the document and in the email subject. Apply the label in document metadata where the platform supports it.",
                    "Encrypted at rest. Hardcopy in a locked cabinet in a controlled area.",
                    "Encrypted channel only, using approved corporate file transfer or secure email. No consumer cloud storage.",
                    "Role-based on need-to-know, approved by the asset owner. MFA for remote and privileged access. Access reviewed semi-annually.",
                    "Cross-cut shredding to DIN P-4 or higher. Electronic media securely wiped to a recognised standard with a record kept.");
            case 3 -> new Guidance(3, "Restricted",
                    "Mark RESTRICTED at least on the cover page and in the document footer.",
                    "Access-controlled storage on corporate systems. Hardcopy not left unattended.",
                    "Corporate channels only. Encryption required for any transfer leaving the corporate network.",
                    "Role-based, approved by the line manager or system owner. Reviewed annually.",
                    "Shredding for hardcopy. Standard secure wipe for electronic media.");
            case 2 -> new Guidance(2, "Internal",
                    "Mark INTERNAL where useful. No external distribution without owner approval.",
                    "Corporate systems. No control beyond normal office security.",
                    "Corporate channels. Encryption in transit where the platform provides it.",
                    "Available to authenticated staff by default; restricted where the owner requires it.",
                    "Normal disposal for hardcopy carrying no personal data. Standard wipe for media.");
            default -> new Guidance(1, "Public",
                    "No label required. May carry an approved-for-release marking.",
                    "No confidentiality control needed. Integrity and availability controls still apply.",
                    "Any channel. Verify the integrity of what is published.",
                    "Open. Publishing rights restricted to authorised communications staff.",
                    "Normal disposal.");
        };
    }
}
