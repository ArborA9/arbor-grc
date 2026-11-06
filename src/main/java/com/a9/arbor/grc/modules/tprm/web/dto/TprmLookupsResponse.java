package com.a9.arbor.grc.modules.tprm.web.dto;

import java.util.List;
import java.util.Map;

public record TprmLookupsResponse(
        List<NamedValue> vendorTypes,
        Map<String, List<String>> factors,
        List<NamedValue> certs,
        List<String> answers,
        List<String> verdicts
) {
    public record NamedValue(String value, String label) {
    }
}
