package com.a9.arbor.grc.modules.tprm.web.dto;

import com.a9.arbor.grc.modules.tprm.domain.ContactRole;
import com.a9.arbor.grc.modules.tprm.domain.TprmCertType;
import com.a9.arbor.grc.modules.tprm.domain.VendorType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record UpsertVendorRequest(
        @NotBlank @Size(max = 200) String name,
        @JsonDeserialize(using = EmptyVendorTypeDeserializer.class)
        VendorType vendorType,
        @Size(max = 500) String website,
        @Size(max = 1000) String servicesInScope,
        @Size(max = 2000) String projectDescription,
        @Size(max = 200) String orgOwner,
        @Size(max = 60) String contractCode,
        @JsonDeserialize(using = EmptyLocalDateDeserializer.class)
        LocalDate contractStart,
        @JsonDeserialize(using = EmptyLocalDateDeserializer.class)
        LocalDate contractEnd,
        String f1,
        String f2,
        String f3,
        String f4,
        String f5,
        String f6,
        String overrideTier,
        @Size(max = 2000) String overrideJustification,
        @Valid List<ContactBody> contacts,
        @Valid List<CertBody> certs
) {
    public record ContactBody(
            ContactRole role,
            @Size(max = 120) String name,
            @Email @Size(max = 255) String email,
            @Size(max = 40) String mobile
    ) {
    }

    public record CertBody(
            TprmCertType certType,
            @JsonDeserialize(using = EmptyLocalDateDeserializer.class)
            LocalDate expiryDate,
            @Size(max = 200) String otherLabel
    ) {
    }
}
