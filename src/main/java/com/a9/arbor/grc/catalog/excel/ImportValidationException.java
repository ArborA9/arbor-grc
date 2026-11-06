package com.a9.arbor.grc.catalog.excel;

import java.util.List;

public class ImportValidationException extends RuntimeException {

    private final List<ImportRowError> errors;

    public ImportValidationException(List<ImportRowError> errors) {
        super("Excel import validation failed (" + errors.size() + " error(s))");
        this.errors = List.copyOf(errors);
    }

    public List<ImportRowError> getErrors() {
        return errors;
    }
}
