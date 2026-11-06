package com.a9.arbor.grc.modules.bcms.pack;

import com.a9.arbor.grc.modules.bcms.web.dto.BiaAssetDependency;
import com.a9.arbor.grc.modules.bcms.web.dto.BiaPersonRef;
import com.a9.arbor.grc.modules.bcms.web.dto.BiaThirdPartyContact;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/** JSON lists stored on bia_process / bia_mor text columns. */
public final class BiaStructuredJson {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final TypeReference<List<BiaPersonRef>> PEOPLE = new TypeReference<>() {
    };
    private static final TypeReference<List<BiaAssetDependency>> ASSETS = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {
    };
    private static final TypeReference<List<BiaThirdPartyContact>> CONTACTS = new TypeReference<>() {
    };

    private BiaStructuredJson() {
    }

    public static String writePeople(List<BiaPersonRef> rows) {
        return write(rows);
    }

    public static List<BiaPersonRef> people(String json) {
        return read(json, PEOPLE);
    }

    public static String writeAssets(List<BiaAssetDependency> rows) {
        return write(rows);
    }

    public static List<BiaAssetDependency> assets(String json) {
        return read(json, ASSETS);
    }

    public static String writeStrings(List<String> rows) {
        return write(rows);
    }

    public static List<String> strings(String json) {
        return read(json, STRINGS);
    }

    public static String writeContacts(List<BiaThirdPartyContact> rows) {
        return write(rows);
    }

    public static List<BiaThirdPartyContact> contacts(String json) {
        return read(json, CONTACTS);
    }

    public static boolean hasPeople(String json) {
        return people(json).stream().anyMatch(p -> p != null && notBlank(p.displayName()));
    }

    public static boolean hasAssets(String json) {
        List<BiaAssetDependency> rows = assets(json);
        if (rows.isEmpty()) {
            return false;
        }
        for (BiaAssetDependency row : rows) {
            if (row == null || isBlank(row.type()) || isBlank(row.detail())) {
                return false;
            }
            if (BiaLookups.assetDetailIsInteger(row.type()) && !isInteger(row.detail())) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasContacts(String json) {
        List<BiaThirdPartyContact> rows = contacts(json);
        if (rows.isEmpty()) {
            return false;
        }
        for (BiaThirdPartyContact c : rows) {
            if (c == null || isBlank(c.name()) || isBlank(c.email())) {
                return false;
            }
        }
        return true;
    }

    public static String peopleSummary(String json) {
        List<String> names = people(json).stream()
                .filter(p -> p != null && notBlank(p.displayName()))
                .map(p -> notBlank(p.jobTitle()) ? p.displayName() + " (" + p.jobTitle() + ")" : p.displayName())
                .toList();
        return names.isEmpty() ? null : String.join("; ", names);
    }

    public static String assetsSummary(String json) {
        List<String> bits = assets(json).stream()
                .filter(a -> a != null && notBlank(a.type()))
                .map(a -> BiaLookups.assetLabel(a.type()) + ": " + (a.detail() == null ? "" : a.detail().trim()))
                .toList();
        return bits.isEmpty() ? null : String.join("; ", bits);
    }

    private static String write(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize BIA list", e);
        }
    }

    private static <T> List<T> read(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank() || !json.trim().startsWith("[")) {
            return List.of();
        }
        try {
            List<T> rows = MAPPER.readValue(json, type);
            return rows == null ? List.of() : List.copyOf(rows);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private static boolean isInteger(String raw) {
        if (raw == null) {
            return false;
        }
        String t = raw.trim();
        return t.matches("\\d+");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static boolean notBlank(String s) {
        return !isBlank(s);
    }
}
