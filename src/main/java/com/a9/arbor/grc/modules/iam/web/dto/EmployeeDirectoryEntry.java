package com.a9.arbor.grc.modules.iam.web.dto;

/**
 * One Entra / Graph person. Not an Arbor user id. UI can later show any of these fields;
 * unused fields stay in the JSON as null / empty.
 */
public record EmployeeDirectoryEntry(
        String directoryId,
        String email,
        String displayName,
        String jobTitle,
        String department,
        String managerName,
        String managerEmail,
        String managerJobTitle,
        String managerDepartment
) {
}
