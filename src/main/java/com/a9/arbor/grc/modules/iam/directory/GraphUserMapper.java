package com.a9.arbor.grc.modules.iam.directory;

import com.a9.arbor.grc.modules.iam.web.dto.EmployeeDirectoryEntry;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/** Maps Microsoft Graph user + manager JSON onto {@link EmployeeDirectoryEntry}. */
public final class GraphUserMapper {

    private GraphUserMapper() {
    }

    public static List<EmployeeDirectoryEntry> fromUsersArray(JsonNode value) {
        List<EmployeeDirectoryEntry> out = new ArrayList<>();
        if (value == null || !value.isArray()) {
            return out;
        }
        for (JsonNode user : value) {
            EmployeeDirectoryEntry entry = fromUser(user, user.path("manager"));
            if (entry != null) {
                out.add(entry);
            }
        }
        return out;
    }

    public static EmployeeDirectoryEntry fromUser(JsonNode user, JsonNode manager) {
        if (user == null || user.isMissingNode() || user.isNull()) {
            return null;
        }
        String email = firstEmail(user);
        String name = text(user, "displayName");
        if (isBlank(email) && isBlank(name)) {
            return null;
        }
        JsonNode mgr = usablePerson(manager);
        return new EmployeeDirectoryEntry(
                text(user, "id"),
                email,
                name,
                text(user, "jobTitle"),
                text(user, "department"),
                mgr == null ? null : text(mgr, "displayName"),
                mgr == null ? null : firstEmail(mgr),
                mgr == null ? null : text(mgr, "jobTitle"),
                mgr == null ? null : text(mgr, "department"));
    }

    public static EmployeeDirectoryEntry withManager(EmployeeDirectoryEntry user, JsonNode manager) {
        if (user == null) {
            return null;
        }
        JsonNode mgr = usablePerson(manager);
        if (mgr == null) {
            return user;
        }
        return new EmployeeDirectoryEntry(
                user.directoryId(),
                user.email(),
                user.displayName(),
                user.jobTitle(),
                user.department(),
                text(mgr, "displayName"),
                firstEmail(mgr),
                text(mgr, "jobTitle"),
                text(mgr, "department"));
    }

    static JsonNode usablePerson(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isObject()) {
            return null;
        }
        if (node.path("error").isObject()) {
            return null;
        }
        if (isBlank(text(node, "displayName")) && isBlank(firstEmail(node))) {
            return null;
        }
        return node;
    }

    static String firstEmail(JsonNode node) {
        String mail = text(node, "mail");
        if (!isBlank(mail)) {
            return mail.trim().toLowerCase();
        }
        String upn = text(node, "userPrincipalName");
        return isBlank(upn) ? null : upn.trim().toLowerCase();
    }

    static String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        String value = node.get(field).asText();
        return isBlank(value) || "null".equals(value) ? null : value;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
