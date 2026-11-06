package com.a9.arbor.grc.modules.privacy.web.dto;

import com.a9.arbor.grc.modules.privacy.domain.RopaDesignation;
import com.a9.arbor.grc.modules.privacy.domain.RopaDpiaStatus;
import com.a9.arbor.grc.modules.privacy.domain.RopaIntakeMode;
import com.a9.arbor.grc.modules.privacy.domain.RopaLegalBasis;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRopaRequest(
        @NotBlank @Size(max = 255) String activityName,
        @Size(max = 255) String entityName,
        @Size(max = 255) String sectorName,
        @Size(max = 255) String divisionName,
        @Size(max = 255) String sectionName,
        String processingDescription,
        @Size(max = 2000) String purpose,
        RopaLegalBasis legalBasis,
        @Size(max = 1000) String dataCategories,
        @Size(max = 1000) String sensitiveDataCategories,
        @Size(max = 500) String dataSubjects,
        @Size(max = 64) String dataSubjectVolume,
        @Size(max = 500) String dataSubjectVolumeNote,
        @Size(max = 64) String processingFrequency,
        @Size(max = 500) String processingFrequencyNote,
        @Size(max = 2000) String sourceOfPersonalData,
        @Size(max = 2000) String methodOfReceiving,
        @Size(max = 1000) String recipients,
        Boolean shareInternal,
        @Size(max = 2000) String shareInternalDetails,
        Boolean shareOperator,
        @Size(max = 2000) String shareOperatorDetails,
        Boolean shareExternal,
        @Size(max = 2000) String shareExternalDetails,
        @Size(max = 500) String externalEntityLocation,
        Boolean thirdCountryTransfers,
        Boolean adequateProtection,
        @Size(max = 2000) String crossBorderTransferBasis,
        @Size(max = 2000) String safeguardsImplemented,
        RopaDpiaStatus dpiaStatus,
        Boolean dpiaRequired,
        @Size(max = 2000) String thirdCountryTransferDetails,
        @Size(max = 255) String retentionPeriod,
        @Size(max = 2000) String securityMeasures,
        @Size(max = 2000) String storageSystems,
        @Size(max = 255) String representativeName,
        @Size(max = 64) String representativeEmployeeId,
        String comments,
        @NotNull RopaDesignation designation,
        RopaIntakeMode intakeMode,
        Long orgUnitId,
        Long ropaDepartmentId,
        String legalBasesJson,
        String adequacyNote,
        Boolean draft
) {
}
