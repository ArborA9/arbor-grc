package com.a9.arbor.grc.modules.privacy.service;

import com.a9.arbor.grc.modules.privacy.domain.RopaLegalBasis;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

/**
 * Client RoPA sheet (Record of Processing Activities RoPA.xlsx) field contract:
 * dropdown value sets from the sheet data validations and cell comments (row 5).
 * The sheet hover row also lists "One off" for Frequency, so it is accepted too.
 */
public final class RopaSheetFields {

    public static final List<String> PERSONAL_DATA = List.of(
            "Name", "Date of Birth/Age", "Address", "Email", "Telephone",
            "Bank Account Details", "Photo", "Passport", "Visa", "Work Permits");

    public static final List<String> SENSITIVE_DATA = List.of(
            "Family or ethnic origin", "Political/philosophical opinions", "Religious beliefs",
            "Criminal record", "Biometric data", "Health-related information",
            "Data related to the provision of healthcare services that reveals the person's health status");

    public static final List<String> DATA_SUBJECT_CATEGORIES = List.of(
            "Employees", "Customers", "Other individuals e.g., Third party service providers");

    public static final List<String> DATA_SUBJECT_VOLUMES = List.of(
            "0-1000", "1,000-100,000", "100,000-1M", "over 1M");

    public static final List<String> FREQUENCIES = List.of(
            "Daily", "Weekly", "Monthly", "Quarterly", "Annually", "Ad hoc", "One off");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RopaSheetFields() {
    }

    /** Parses a multi-choice value: JSON array from the UI, or legacy comma text. */
    public static List<String> parseList(String raw, String fieldLabel) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("[")) {
            try {
                List<String> parsed = MAPPER.readValue(trimmed, new TypeReference<>() {
                });
                return parsed.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        fieldLabel + " must be a list of values");
            }
        }
        return List.of(trimmed.split(",")).stream()
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /**
     * Validates Contributor processing-activity fields against the sheet value sets.
     * Draft saves may omit fields (list values still must be valid if provided).
     * Full save (draft=false) requires every champion field on the form.
     */
    public static void validateContributorFields(String processingDescription,
                                                 String dataCategories,
                                                 String sensitiveDataCategories,
                                                 String dataSubjects,
                                                 String dataSubjectVolume,
                                                 String processingFrequency,
                                                 String sourceOfPersonalData,
                                                 String methodOfReceiving,
                                                 String legalBasesJson,
                                                 String shareInternalDetails,
                                                 Boolean shareExternal,
                                                 String shareExternalDetails,
                                                 String externalEntityLocation,
                                                 String adequacyNote,
                                                 String safeguardsImplemented,
                                                 String storageSystems,
                                                 String retentionPeriod,
                                                 String securityMeasures,
                                                 boolean draft) {
        List<String> personal = parseList(dataCategories, "List of Personal Data");
        List<String> sensitive = parseList(sensitiveDataCategories, "Categories of Sensitive Data");
        List<String> subjects = parseList(dataSubjects, "Data Subject categories");
        List<String> bases = parseList(legalBasesJson, "Legal basis for processing");

        requireAllIn(personal, Set.copyOf(PERSONAL_DATA), "List of Personal Data");
        requireAllIn(sensitive, Set.copyOf(SENSITIVE_DATA), "Categories of Sensitive Data");
        requireAllIn(subjects, Set.copyOf(DATA_SUBJECT_CATEGORIES), "Data Subject categories");
        if (dataSubjectVolume != null && !dataSubjectVolume.isBlank()
                && !DATA_SUBJECT_VOLUMES.contains(dataSubjectVolume.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No. of data subjects whose personal data is processed must be one of: "
                            + String.join(", ", DATA_SUBJECT_VOLUMES));
        }
        if (processingFrequency != null && !processingFrequency.isBlank()
                && !FREQUENCIES.contains(processingFrequency.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Frequency of processing must be one of: " + String.join(", ", FREQUENCIES));
        }
        for (String basis : bases) {
            try {
                RopaLegalBasis.valueOf(basis);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Legal basis for processing contains an unsupported value: " + basis);
            }
        }

        if (draft) {
            return;
        }

        requireText(processingDescription, "Description of processing");
        if (personal.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "List of Personal Data is required");
        }
        if (sensitive.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categories of Sensitive Data is required");
        }
        if (subjects.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data Subject categories is required");
        }
        requireText(dataSubjectVolume, "No. of data subjects whose personal data is processed");
        requireText(processingFrequency, "Frequency of processing");
        requireText(sourceOfPersonalData, "Source of Personal Data");
        requireText(methodOfReceiving, "Method of receiving Personal Data");
        if (bases.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Legal basis for processing is required");
        }
        requireText(shareInternalDetails, "Internal department");
        if (Boolean.TRUE.equals(shareExternal)
                && (shareExternalDetails == null || shareExternalDetails.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "External entity detail is required when External entities is Yes");
        }
        requireText(externalEntityLocation, "Location");
        requireText(adequacyNote, "Adequate level of data protection");
        requireText(safeguardsImplemented, "What safeguards have been implemented?");
        requireText(storageSystems, "Systems / other location where the personal data is stored");
        requireText(retentionPeriod, "Retention period");
        requireText(securityMeasures, "Organizational, administrative, and technical measures for personal data security");
    }

    private static void requireText(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldLabel + " is required");
        }
    }

    private static void requireAllIn(List<String> values, Set<String> allowed, String fieldLabel) {
        for (String value : values) {
            if (!allowed.contains(value)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        fieldLabel + " contains an unsupported value: " + value);
            }
        }
    }
}
