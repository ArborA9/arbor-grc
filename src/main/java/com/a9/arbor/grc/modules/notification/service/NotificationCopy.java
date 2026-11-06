package com.a9.arbor.grc.modules.notification.service;

import com.a9.arbor.grc.tenancy.TenantContext;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Human-readable notification placeholders (roles, sign-in method, workspace id). */
public final class NotificationCopy {

    private NotificationCopy() {
    }

    static Map<String, String> enrich(Map<String, String> data) {
        Map<String, String> out = new LinkedHashMap<>();
        if (data != null) {
            out.putAll(data);
        }
        String tid = TenantContext.getTenantId();
        String workspaceId = tid == null || tid.isBlank() ? "-" : tid;
        out.putIfAbsent("workspaceId", workspaceId);

        String authRaw = firstNonBlank(out.get("authType"), out.get("auth_type"));
        if (out.containsKey("roles")) {
            out.put("roles", humanRoles(out.get("roles")));
        }
        if (out.containsKey("role")) {
            out.put("role", humanRole(out.get("role")));
        }
        if (authRaw != null) {
            out.put("authType", humanAuth(authRaw));
        }
        out.putIfAbsent("signInHint", signInHint(authRaw, workspaceId));
        return out;
    }

    static String humanRole(String raw) {
        if (raw == null || raw.isBlank()) {
            return "-";
        }
        String key = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return switch (key) {
            case "SUPER_ADMIN" -> "Super Admin";
            case "OWNER" -> "Owner";
            case "ADMIN" -> "Admin";
            case "CONTRIBUTOR" -> "Contributor";
            case "INTERNAL_AUDITOR" -> "Internal Auditor";
            case "EXTERNAL_AUDITOR" -> "External Auditor";
            case "GLOBAL_READER" -> "Global Reader";
            case "VENDOR" -> "Vendor";
            default -> titleCase(raw.trim());
        };
    }

    static String humanRoles(String csv) {
        if (csv == null || csv.isBlank()) {
            return "-";
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(NotificationCopy::humanRole)
                .collect(Collectors.joining(", "));
    }

    static String humanAuth(String raw) {
        if (raw == null || raw.isBlank()) {
            return "your usual sign-in";
        }
        String key = raw.trim().toUpperCase(Locale.ROOT);
        if (key.contains("SSO") || key.contains("MICROSOFT") || key.contains("ENTRA")) {
            return "Microsoft";
        }
        if (key.contains("LOCAL") || key.contains("PASSWORD")) {
            return "email and password";
        }
        return raw.trim();
    }

    static String signInHint(String authRaw, String workspaceId) {
        String id = workspaceId == null || workspaceId.isBlank() ? "-" : workspaceId;
        String auth = humanAuth(authRaw);
        if ("Microsoft".equals(auth)) {
            return "Sign in with Microsoft. Enter workspace id " + id + " on the sign-in page.";
        }
        if ("email and password".equals(auth)) {
            return "Sign in with your email and password. Enter workspace id " + id + " on the sign-in page.";
        }
        return "Sign in at the link below. Enter workspace id " + id + " on the sign-in page.";
    }

    public static boolean skipGenericAppLink(String triggerType, String body) {
        if (body != null && body.toLowerCase(Locale.ROOT).contains("http")) {
            return true;
        }
        if (triggerType == null) {
            return false;
        }
        String t = triggerType.toUpperCase(Locale.ROOT);
        return t.contains("OTP") || t.endsWith("_LINK") || t.contains("QUESTIONNAIRE_SENT")
                || t.contains("QUESTIONNAIRE_RETURNED") || t.contains("PROJECT_CLOSED")
                || t.equals("PASSWORD_RESET") || t.equals("ACCOUNT_ACTIVATION") || t.startsWith("LICENSE_");
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }

    private static String titleCase(String raw) {
        String spaced = raw.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Arrays.stream(spaced.split("\\s+"))
                .filter(s -> !s.isEmpty())
                .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
                .collect(Collectors.joining(" "));
    }
}
