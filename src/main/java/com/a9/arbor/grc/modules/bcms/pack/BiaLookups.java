package com.a9.arbor.grc.modules.bcms.pack;

import java.util.List;

/** Reference sheet pick lists + 1–5 scale. Does not include the stale §2 RTO/MAO table. */
public final class BiaLookups {

    private BiaLookups() {
    }

    public static final String DEFAULT_OBJECTIVE =
            "To identify the business impacts of the failure or unavailability of the organization's "
                    + "key processes and the services that support them, so that resources and recovery "
                    + "effort can be prioritised.";

    public static final String DEFAULT_SCOPE =
            "Rate impact against a worst-case scenario - the process is interrupted at the worst "
                    + "possible time (the most urgent time of day, week, month or year). Apply the same "
                    + "assumption to every process.";

    public static final String DEFAULT_MBCO =
            "1. Provide end-user support services during a disruption, to all stakeholders - "
                    + "internal and external.\n"
                    + "2. Provide back-end support where required, including the delivery of enablement services.";

    public static final List<String> FREQUENCIES = List.of(
            "Continuous",
            "Daily",
            "Weekly",
            "Monthly",
            "Quarterly",
            "Bi-Annually",
            "Annually",
            "Upon Request / Ad-hoc");

    public static final String RECOVERY_OTHER = "Other (describe in comments)";

    public static final List<String> RECOVERY_STRATEGIES = List.of(
            "Premises: Remote working",
            "Premises: Alternate / recovery site",
            "People & Skills: Cross-trained or alternate staff",
            "People & Skills: Third party / secondment support",
            "Equipment: Business continuity laptops",
            "Technology: Manual workaround",
            "Technology: DR site / backup system restore",
            "Suppliers: SLA with alternate supplier",
            "Records: Digital copies held on SharePoint",
            RECOVERY_OTHER);

    public static final String YES = "Yes";
    public static final String NO = "No";
    public static final String SITE_PRIMARY = "Primary Site";
    public static final String SITE_SECONDARY = "Secondary Site";
    public static final String SITE_OTHER = "Other";
    public static final String DETAIL_STRING = "STRING";
    public static final String DETAIL_INTEGER = "INTEGER";

    public record AssetType(String code, String label, String detailKind) {
    }

    public static final List<AssetType> ASSET_TYPES = List.of(
            new AssetType("SOFTWARE", "Software / Application", DETAIL_STRING),
            new AssetType("HARDWARE", "Hardware / Endpoint", DETAIL_INTEGER),
            new AssetType("SERVER", "Server / Compute", DETAIL_INTEGER),
            new AssetType("NETWORK", "Network / Communications", DETAIL_INTEGER),
            new AssetType("FACILITY", "Facility / Environment", DETAIL_STRING),
            new AssetType("OT", "OT / ICS / SCADA", DETAIL_STRING));

    public static String assetLabel(String code) {
        if (code == null) {
            return "";
        }
        for (AssetType t : ASSET_TYPES) {
            if (t.code().equalsIgnoreCase(code) || t.label().equalsIgnoreCase(code.trim())) {
                return t.label();
            }
        }
        return code;
    }

    public static boolean assetDetailIsInteger(String code) {
        if (code == null) {
            return false;
        }
        for (AssetType t : ASSET_TYPES) {
            if (t.code().equalsIgnoreCase(code) || t.label().equalsIgnoreCase(code.trim())) {
                return DETAIL_INTEGER.equals(t.detailKind());
            }
        }
        return false;
    }

    public static boolean isKnownAssetType(String code) {
        return resolveAssetType(code) != null;
    }

    public static AssetType resolveAssetType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        for (AssetType a : ASSET_TYPES) {
            if (a.code().equalsIgnoreCase(t) || a.label().equalsIgnoreCase(t)) {
                return a;
            }
        }
        return null;
    }

    public static String resolveFrequency(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        for (String f : FREQUENCIES) {
            if (f.equalsIgnoreCase(t)) {
                return f;
            }
        }
        return null;
    }

    public static BiaImpactEngine.Category resolveCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String n = raw.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        try {
            return BiaImpactEngine.Category.valueOf(n);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static final List<String> REVIEW_OUTCOMES = List.of(
            "Pending Review",
            "Verified",
            "Returned for Revision");

    public record ImpactScale(int rating, String level, String definition) {
    }

    public static final List<ImpactScale> IMPACT_SCALE = List.of(
            new ImpactScale(1, "Insignificant",
                    "No noticeable effect. Absorbed by normal working; no external visibility."),
            new ImpactScale(2, "Minor",
                    "Minor inconvenience. Handled inside the section; no customer or regulator impact."),
            new ImpactScale(3, "Medium",
                    "Noticeable disruption. Service levels, deadlines or commitments start to be missed; escalation required."),
            new ImpactScale(4, "Major",
                    "Significant disruption. Statutory or contractual deadlines breached, external stakeholders affected."),
            new ImpactScale(5, "Critical",
                    "Severe or irreversible disruption. Legal breach, major reputational damage, or a mandated service cannot be delivered."));

    public static String categoryLabel(int tier) {
        return switch (tier) {
            case 1 -> "Critical - Tier 1";
            case 2 -> "Critical - Tier 2";
            case 3 -> "Critical - Tier 3";
            case 4 -> "Normal - Tier 4";
            default -> "Normal - Tier 5";
        };
    }

    public static String bandLabel(BiaImpactEngine.Band band) {
        return switch (band) {
            case H0_4 -> "0-4 hours";
            case H4_8 -> "4-8 hours";
            case H8_24 -> "8-24 hours";
            case H24_48 -> "24-48 hours";
            case H48_72 -> "48-72 hours";
        };
    }
}
