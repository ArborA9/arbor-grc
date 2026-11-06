package com.a9.arbor.grc.modules.tprm.web.dto;

import com.a9.arbor.grc.modules.tprm.domain.VendorType;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/** Treat JSON "" as null so draft Create vendor does not 400 on empty vendor type. */
public class EmptyVendorTypeDeserializer extends JsonDeserializer<VendorType> {

    @Override
    public VendorType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String raw = p.getValueAsString();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return VendorType.valueOf(raw.trim());
    }
}
