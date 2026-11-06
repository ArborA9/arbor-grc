package com.a9.arbor.grc.modules.tprm.web.dto;

import com.a9.arbor.grc.modules.tprm.domain.ContactRole;
import com.a9.arbor.grc.modules.tprm.domain.TprmCertType;
import com.a9.arbor.grc.modules.tprm.domain.TprmCompliance;
import com.a9.arbor.grc.modules.tprm.domain.TprmCriticality;
import com.a9.arbor.grc.modules.tprm.domain.TprmVendor;
import com.a9.arbor.grc.modules.tprm.domain.TprmVendorCert;
import com.a9.arbor.grc.modules.tprm.domain.TprmVendorContact;
import com.a9.arbor.grc.modules.tprm.domain.VendorLifecycleState;
import com.a9.arbor.grc.modules.tprm.domain.VendorStatus;
import com.a9.arbor.grc.modules.tprm.domain.VendorType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record VendorResponse(
        Long id,
        String name,
        VendorType vendorType,
        String website,
        String servicesInScope,
        String projectDescription,
        String orgOwner,
        String contractCode,
        LocalDate contractStart,
        LocalDate contractEnd,
        String f1,
        String f2,
        String f3,
        String f4,
        String f5,
        String f6,
        Integer f1Score,
        Integer f2Score,
        Integer f3Score,
        Integer f4Score,
        Integer f5Score,
        Integer f6Score,
        Integer inherentScore,
        Integer assuranceCredit,
        Integer adjustedScore,
        String tier,
        String calculatedTier,
        TprmCriticality criticality,
        BigDecimal postureTarget,
        Integer reassessmentMonths,
        String overrideTier,
        String overrideJustification,
        boolean elevationFlag,
        boolean profileComplete,
        List<String> missingProfileFields,
        VendorLifecycleState lifecycleState,
        TprmCompliance complianceStatus,
        BigDecimal posturePercent,
        LocalDate nextReviewDate,
        Long linkedErmRiskId,
        Long currentAssessmentId,
        boolean raiseErmPrompted,
        /** True when the vendor record is locked (current project assigned or closed). */
        boolean categorisationFrozen,
        VendorStatus status,
        List<ContactView> contacts,
        List<CertView> certs,
        Instant createdAt,
        Instant updatedAt
) {
    public record ContactView(Long id, ContactRole role, String name, String email, String mobile) {
    }

    public record CertView(Long id, TprmCertType certType, LocalDate expiryDate, String otherLabel) {
    }

    public static VendorResponse from(TprmVendor v, List<TprmVendorContact> contacts, List<TprmVendorCert> certs,
                                      String calculatedTier, boolean profileComplete,
                                      List<String> missingProfileFields, boolean raiseErmPrompted,
                                      boolean categorisationFrozen) {
        return new VendorResponse(
                v.getId(), v.getName(), v.getVendorType(), v.getWebsite(), v.getServicesInScope(),
                v.getProjectDescription(), v.getOrgOwner(), v.getContractCode(), v.getContractStart(),
                v.getContractEnd(), v.getF1(), v.getF2(), v.getF3(), v.getF4(), v.getF5(), v.getF6(),
                v.getF1Score(), v.getF2Score(), v.getF3Score(), v.getF4Score(), v.getF5Score(), v.getF6Score(),
                v.getInherentScore(), v.getAssuranceCredit(), v.getAdjustedScore(), v.getTier(), calculatedTier,
                v.getCriticality(), v.getPostureTarget(), v.getReassessmentMonths(), v.getOverrideTier(),
                v.getOverrideJustification(), v.isElevationFlag(), profileComplete, missingProfileFields,
                v.getLifecycleState(),
                v.getComplianceStatus(), v.getPosturePercent(), v.getNextReviewDate(), v.getLinkedErmRiskId(),
                v.getCurrentAssessmentId(), raiseErmPrompted, categorisationFrozen, v.getStatus(),
                contacts.stream().map(c -> new ContactView(c.getId(), c.getRole(), c.getName(), c.getEmail(), c.getMobile())).toList(),
                certs.stream().map(c -> new CertView(c.getId(), c.getCertType(), c.getExpiryDate(), c.getOtherLabel())).toList(),
                v.getCreatedAt(), v.getUpdatedAt());
    }
}
