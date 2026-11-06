package com.a9.arbor.grc.catalog.excel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared hierarchy / uniqueness checks for parsed framework rows. */
public final class ParsedFrameworkRowValidator {

    private ParsedFrameworkRowValidator() {
    }

    public static void validateOrThrow(List<ParsedFrameworkRow> rows, List<ImportRowError> priorErrors) {
        List<ImportRowError> errors = new ArrayList<>(priorErrors);
        Set<String> codes = new HashSet<>();
        Map<String, Integer> levelByCode = new HashMap<>();
        for (ParsedFrameworkRow row : rows) {
            if (!codes.add(row.code())) {
                errors.add(new ImportRowError(row.excelRow(), "code", "Duplicate code: " + row.code()));
            }
            levelByCode.put(row.code(), row.level());
        }
        for (ParsedFrameworkRow row : rows) {
            if (row.parentCode() == null) {
                continue;
            }
            Integer parentLevel = levelByCode.get(row.parentCode());
            if (parentLevel == null) {
                errors.add(new ImportRowError(row.excelRow(), "parent_code",
                        "Unknown parent_code: " + row.parentCode()));
            } else if (parentLevel != row.level() - 1) {
                errors.add(new ImportRowError(row.excelRow(), "parent_code",
                        "parent must be exactly one level above (parent level "
                                + parentLevel + ", row level " + row.level() + ")"));
            }
        }
        if (!errors.isEmpty()) {
            throw new ImportValidationException(errors);
        }
    }
}
