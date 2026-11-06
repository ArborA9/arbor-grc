package com.a9.arbor.grc.modules.tprm.pack;

import com.a9.arbor.grc.modules.tprm.domain.TprmCertType;
import com.a9.arbor.grc.modules.tprm.domain.TprmCriticality;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Pack rules R1–R3 plus elevation flag and one-level override. */
public final class TprmCategorisation {

    private TprmCategorisation() {
    }

    public record Input(
            String f1, String f2, String f3, String f4, String f5, String f6,
            Collection<TprmCertType> certs,
            String overrideTier
    ) {
    }

    public record Result(
            int f1, int f2, int f3, int f4, int f5, int f6,
            int inherent,
            int assuranceCredit,
            int adjusted,
            String calculatedTier,
            String effectiveTier,
            TprmCriticality criticality,
            double postureTarget,
            int reassessmentMonths,
            boolean elevationFlag
    ) {
    }

    public static Result calculate(Input input) {
        int s1 = TprmLookups.score(TprmLookups.F1, input.f1());
        int s2 = TprmLookups.score(TprmLookups.F2, input.f2());
        int s3 = TprmLookups.score(TprmLookups.F3, input.f3());
        int s4 = TprmLookups.score(TprmLookups.F4, input.f4());
        int s5 = TprmLookups.score(TprmLookups.F5, input.f5());
        int s6 = TprmLookups.score(TprmLookups.F6, input.f6());
        int inherent = s1 + s2 + s3 + s4 + s5 + s6;
        int credit = assuranceCredit(input.certs());
        int adjusted = Math.max(6, inherent - credit);
        String calculated = TprmLookups.tierForAdjusted(adjusted);
        boolean elevation = (s1 == 4 || s2 == 4)
                && ("TIER_3".equals(calculated) || "TIER_4".equals(calculated));
        String effective = calculated;
        if (input.overrideTier() != null && !input.overrideTier().isBlank()) {
            effective = input.overrideTier();
        }
        TprmCriticality crit = TprmLookups.critical(effective)
                ? TprmCriticality.CRITICAL : TprmCriticality.NON_CRITICAL;
        return new Result(s1, s2, s3, s4, s5, s6, inherent, credit, adjusted, calculated, effective,
                crit, TprmLookups.targetForTier(effective), TprmLookups.monthsForTier(effective), elevation);
    }

    /** Distinct certs other than NONE, capped at 3 (pack lists 0–3; mapping assumed). */
    public static int assuranceCredit(Collection<TprmCertType> certs) {
        if (certs == null || certs.isEmpty()) {
            return 0;
        }
        Set<TprmCertType> distinct = certs.stream()
                .filter(Objects::nonNull)
                .filter(TprmCertType::countsTowardCredit)
                .collect(Collectors.toSet());
        return Math.min(3, distinct.size());
    }
}
