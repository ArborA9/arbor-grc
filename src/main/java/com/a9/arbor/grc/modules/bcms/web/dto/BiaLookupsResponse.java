package com.a9.arbor.grc.modules.bcms.web.dto;

import com.a9.arbor.grc.modules.bcms.pack.BiaLookups;

import java.util.List;

public record BiaLookupsResponse(
        List<String> frequencies,
        List<String> recoveryStrategies,
        List<String> reviewOutcomes,
        List<BiaLookups.ImpactScale> impactScale,
        List<BandInfo> bands,
        List<String> categories,
        List<BiaLookups.AssetType> assetTypes,
        List<OrgUnitOption> orgUnits,
        List<UserOption> users
) {
    public record BandInfo(String code, String label, int maoHours, int rtoHours) {
    }

    public record OrgUnitOption(long id, String name) {
    }

    public record UserOption(long id, String name) {
    }

    public static BiaLookupsResponse pack(List<OrgUnitOption> orgUnits, List<UserOption> users) {
        return new BiaLookupsResponse(
                BiaLookups.FREQUENCIES,
                BiaLookups.RECOVERY_STRATEGIES,
                BiaLookups.REVIEW_OUTCOMES,
                BiaLookups.IMPACT_SCALE,
                List.of(
                        new BandInfo("H0_4", "0-4 hours", 4, 3),
                        new BandInfo("H4_8", "4-8 hours", 8, 6),
                        new BandInfo("H8_24", "8-24 hours", 24, 18),
                        new BandInfo("H24_48", "24-48 hours", 48, 36),
                        new BandInfo("H48_72", "48-72 hours", 72, 54)),
                List.of("STRATEGIC", "FINANCIAL", "OPERATIONAL", "COMPLIANCE"),
                BiaLookups.ASSET_TYPES,
                orgUnits == null ? List.of() : orgUnits,
                users == null ? List.of() : users);
    }
}
