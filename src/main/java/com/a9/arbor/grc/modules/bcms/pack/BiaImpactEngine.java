package com.a9.arbor.grc.modules.bcms.pack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pack 2026 Impact Analysis engine (D–H first-3 / 75% RTO). No Excel formulas in the JVM.
 */
public final class BiaImpactEngine {

    public enum Category { STRATEGIC, FINANCIAL, OPERATIONAL, COMPLIANCE }

    public enum Band { H0_4, H4_8, H8_24, H24_48, H48_72 }

    public static final int[] MAO_HOURS = {4, 8, 24, 48, 72};

    public static final String COMPLETE_READY = "Complete";
    public static final String INCOMPLETE = "Incomplete";

    private BiaImpactEngine() {
    }

    public record ImpactResult(
            Integer peak,
            Integer tier,
            String categoryLabel,
            Integer maoHours,
            Integer rtoHours,
            boolean morRequired,
            String rtoCaption,
            boolean anyRating,
            boolean allRatingsPresent
    ) {
    }

    public record RankInput(long processId, int sortOrder, Integer tier) {
    }

    public record RankResult(long processId, int recoveryPriority, Integer criticalRank) {
    }

    public record ProcessFields(
            String processCode,
            String processName,
            String description,
            String flowTrigger,
            String flowSteps,
            String flowOutput,
            Long ownerUserId,
            String frequency,
            boolean depStaffOk,
            String depPremises,
            boolean depItOk,
            String depRecords
    ) {
    }

    public record MorFields(
            boolean morRequired,
            boolean minStaffOk,
            boolean recoveryOk,
            boolean resourcesOk,
            boolean thirdPartyContactsOk,
            boolean recoveryCommentsOk,
            boolean siteOtherOk
    ) {
    }

    public record Completeness(boolean complete, String label, List<String> missing) {
    }

    /**
     * @param ratings 4 categories × 5 bands, nullable 1–5
     */
    public static ImpactResult calculate(Integer[][] ratings) {
        if (ratings == null || ratings.length != 4) {
            throw new IllegalArgumentException("Ratings must be 4 categories");
        }
        for (Integer[] row : ratings) {
            if (row == null || row.length != 5) {
                throw new IllegalArgumentException("Each category must have 5 bands");
            }
        }

        Integer peak = null;
        int filled = 0;
        for (int c = 0; c < 4; c++) {
            for (int b = 0; b < 5; b++) {
                Integer v = ratings[c][b];
                if (v != null) {
                    filled++;
                    if (peak == null || v > peak) {
                        peak = v;
                    }
                }
            }
        }
        boolean any = filled > 0;
        boolean all = filled == 20;
        if (!any) {
            return new ImpactResult(null, null, null, null, null, false, null, false, false);
        }

        int tier = 5;
        int mao = 72;
        for (int b = 0; b < 5; b++) {
            Integer bandMax = null;
            for (int c = 0; c < 4; c++) {
                Integer v = ratings[c][b];
                if (v != null && (bandMax == null || v > bandMax)) {
                    bandMax = v;
                }
            }
            if (bandMax != null && bandMax >= 3) {
                tier = b + 1;
                mao = MAO_HOURS[b];
                break;
            }
        }
        int rto = (mao * 3) / 4;
        boolean mor = tier <= 3;
        String caption;
        if (noBandHitThree(ratings)) {
            caption = "RTO is 75% of MAO (" + mao + " h) = " + rto
                    + " h. No band reached impact 3; Normal - Tier 5.";
        } else {
            caption = "RTO is 75% of MAO (" + mao + " h) = " + rto + " h. First impact ≥ 3 in "
                    + BiaLookups.bandLabel(Band.values()[tier - 1]) + ".";
        }
        return new ImpactResult(peak, tier, BiaLookups.categoryLabel(tier), mao, rto, mor, caption, true, all);
    }

    private static boolean noBandHitThree(Integer[][] ratings) {
        for (int b = 0; b < 5; b++) {
            Integer bandMax = null;
            for (int c = 0; c < 4; c++) {
                Integer v = ratings[c][b];
                if (v != null && (bandMax == null || v > bandMax)) {
                    bandMax = v;
                }
            }
            if (bandMax != null && bandMax >= 3) {
                return false;
            }
        }
        return true;
    }

    public static List<RankResult> rank(List<RankInput> processes) {
        List<RankInput> inScope = processes.stream()
                .filter(p -> p.tier() != null && p.tier() > 0)
                .sorted(Comparator
                        .comparingInt(RankInput::tier)
                        .thenComparingInt(RankInput::sortOrder)
                        .thenComparingLong(RankInput::processId))
                .toList();
        List<RankResult> out = new ArrayList<>();
        int criticalSeq = 0;
        int pri = 0;
        for (RankInput p : inScope) {
            pri++;
            Integer crit = null;
            if (p.tier() <= 3) {
                criticalSeq++;
                crit = criticalSeq;
            }
            out.add(new RankResult(p.processId(), pri, crit));
        }
        return out;
    }

    public static Completeness completeness(ProcessFields process, ImpactResult impact, MorFields mor) {
        List<String> missing = new ArrayList<>();
        require(missing, process.processCode(), "Process code");
        require(missing, process.processName(), "Business process name");
        require(missing, process.description(), "Process description");
        require(missing, process.flowTrigger(), "Process flow - trigger");
        require(missing, process.flowSteps(), "Process flow - key steps");
        require(missing, process.flowOutput(), "Process flow - output");
        if (process.ownerUserId() == null) {
            missing.add("Process owner");
        }
        require(missing, process.frequency(), "Process frequency");
        if (!process.depStaffOk()) {
            missing.add("Dependency - minimum staff");
        }
        require(missing, process.depPremises(), "Dependency - premises");
        if (!process.depItOk()) {
            missing.add("Dependency - asset type");
        }
        require(missing, process.depRecords(), "Dependency - vital records");
        if (impact == null || !impact.allRatingsPresent()) {
            missing.add("All 20 impact ratings (1-5)");
        }
        if (impact != null && impact.morRequired()) {
            if (mor == null) {
                missing.add("Minimum operating requirements");
            } else {
                if (!mor.minStaffOk()) {
                    missing.add("MOR minimum staff");
                }
                if (!mor.recoveryOk()) {
                    missing.add("Recovery strategy");
                }
                if (!mor.recoveryCommentsOk()) {
                    missing.add("Recovery strategy comments");
                }
                if (!mor.resourcesOk()) {
                    missing.add("MOR resources");
                }
                if (!mor.siteOtherOk()) {
                    missing.add("Other site");
                }
                if (!mor.thirdPartyContactsOk()) {
                    missing.add("MOR third party contacts");
                }
            }
        }
        boolean ok = missing.isEmpty();
        return new Completeness(ok, ok ? COMPLETE_READY : INCOMPLETE, List.copyOf(missing));
    }

    public static Integer[][] emptyGrid() {
        Integer[][] g = new Integer[4][5];
        for (int c = 0; c < 4; c++) {
            g[c] = new Integer[5];
        }
        return g;
    }

    public static int categoryIndex(Category c) {
        return c.ordinal();
    }

    public static int bandIndex(Band b) {
        return b.ordinal();
    }

    private static void require(List<String> missing, String value, String label) {
        if (value == null || value.isBlank()) {
            missing.add(label);
        }
    }
}
