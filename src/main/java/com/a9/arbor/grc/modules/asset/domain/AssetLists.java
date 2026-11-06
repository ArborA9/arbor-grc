package com.a9.arbor.grc.modules.asset.domain;

import java.util.List;

/** Dropdown vocabularies from Excel Lists sheet. */
public final class AssetLists {

    private AssetLists() {
    }

    public static final List<String> CLASSIFICATIONS = List.of(
            "Secret", "Sensitive", "Confidential", "Open");

    public static final List<String> ASSET_TYPES = List.of(
            "Information / Data",
            "Software / Application",
            "Hardware / Endpoint",
            "Server / Compute",
            "Network / Communications",
            "Storage / Media",
            "Service (internal or outsourced)",
            "Facility / Environment",
            "People / Role",
            "Intangible (reputation, IP)",
            "OT / ICS / SCADA",
            "Other");

    public static final List<String> ASSET_SUB_TYPES = List.of(
            "Database",
            "Structured electronic records",
            "Unstructured file share",
            "Hardcopy records",
            "Email / mailbox data",
            "Backup set / archive",
            "Business application",
            "COTS software",
            "In-house developed application",
            "Operating system image",
            "Middleware / integration",
            "Web application",
            "Mobile application",
            "API / web service",
            "Physical server",
            "Virtual machine",
            "Container platform",
            "Hypervisor host",
            "Desktop",
            "Laptop",
            "Tablet",
            "Mobile phone",
            "Thin client",
            "Firewall",
            "Router",
            "Switch",
            "Load balancer / WAF",
            "Wireless controller / AP",
            "VPN concentrator",
            "IDS / IPS",
            "Proxy / secure web gateway",
            "SAN / NAS",
            "Tape / removable media",
            "USB / portable media",
            "Cloud tenancy / subscription",
            "SaaS platform",
            "Managed service",
            "Utility service",
            "Data centre",
            "Comms room",
            "Office area",
            "Station / depot",
            "Privileged role",
            "Key person dependency",
            "CCTV / physical security system",
            "Access control system",
            "Alarm / detection panel",
            "PABX / telephony",
            "Radio / TETRA",
            "Printer / MFD",
            "IoT sensor",
            "Other");

    public static final List<String> HOSTING_MODELS = List.of(
            "On-premises",
            "Private cloud",
            "Public cloud - IaaS",
            "Public cloud - PaaS",
            "SaaS",
            "Colocation",
            "Hybrid",
            "Mobile / field",
            "Third-party premises",
            "Not applicable");

    public static final List<String> DATA_RESIDENCIES = List.of(
            "UAE only",
            "UAE primary, GCC DR",
            "GCC",
            "Outside UAE",
            "Multiple / unclear",
            "Not applicable",
            "Other");

    public static final List<String> ENVIRONMENTS = List.of(
            "Production",
            "Disaster recovery",
            "Staging / pre-production",
            "Test",
            "Development");

    public static final List<String> LIFECYCLE_STATUSES = List.of(
            "Active",
            "Active - end of support",
            "In development",
            "Retired pending disposal",
            "Disposed");

    public static final List<String> TRI_STATES = List.of("Yes", "No", "Not applicable");

    public static String typeLabel(AssetType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case DATA -> "Information / Data";
            case APPLICATION, SOFTWARE -> "Software / Application";
            case HARDWARE -> "Hardware / Endpoint";
            case NETWORK -> "Network / Communications";
            case CLOUD -> "Cloud tenancy / subscription";
            case PHYSICAL, FACILITY -> "Facility / Environment";
            case PEOPLE -> "People / Role";
            case SERVICE -> "Service (internal or outsourced)";
            case INTANGIBLE -> "Intangible (reputation, IP)";
            case OT_ICS -> "OT / ICS / SCADA";
            case OTHER -> "Other";
        };
    }

    public static AssetType typeFromLabel(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        String t = label.trim();
        if (t.equalsIgnoreCase("Information / Data") || t.equalsIgnoreCase("DATA")) {
            return AssetType.DATA;
        }
        if (t.toLowerCase().contains("software") || t.toLowerCase().contains("application")) {
            return AssetType.APPLICATION;
        }
        if (t.toLowerCase().contains("hardware") || t.toLowerCase().contains("endpoint")) {
            return AssetType.HARDWARE;
        }
        if (t.toLowerCase().contains("server") || t.toLowerCase().contains("compute")) {
            return AssetType.HARDWARE;
        }
        if (t.toLowerCase().contains("network")) {
            return AssetType.NETWORK;
        }
        if (t.toLowerCase().contains("storage") || t.toLowerCase().contains("media")) {
            return AssetType.HARDWARE;
        }
        if (t.toLowerCase().contains("service")) {
            return AssetType.SERVICE;
        }
        if (t.toLowerCase().contains("facility") || t.toLowerCase().contains("environment")) {
            return AssetType.FACILITY;
        }
        if (t.toLowerCase().contains("people") || t.toLowerCase().contains("role")) {
            return AssetType.PEOPLE;
        }
        if (t.toLowerCase().contains("intangible")) {
            return AssetType.INTANGIBLE;
        }
        if (t.toLowerCase().contains("ot") || t.toLowerCase().contains("ics") || t.toLowerCase().contains("scada")) {
            return AssetType.OT_ICS;
        }
        if (t.equalsIgnoreCase("Other") || t.equalsIgnoreCase("OTHER")) {
            return AssetType.OTHER;
        }
        return AssetType.OTHER;
    }
}
