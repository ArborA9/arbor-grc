package com.a9.arbor.grc.modules.tprm.pack;

import com.a9.arbor.grc.modules.tprm.domain.TprmAnswer;
import com.a9.arbor.grc.modules.tprm.domain.TprmVerdict;

import java.util.List;

public final class TprmPosture {

    private TprmPosture() {
    }

    public record Item(
            int weight,
            boolean mandatory,
            TprmAnswer answer,
            TprmVerdict verdict
    ) {
    }

    public record Result(double percent, boolean knockout, boolean compliant) {
    }

    public static Result evaluate(List<Item> items, double target) {
        double earned = 0;
        double denom = 0;
        boolean knockout = false;
        for (Item item : items) {
            if (item.answer() == null || item.answer() == TprmAnswer.NA) {
                continue;
            }
            denom += item.weight();
            earned += item.weight() * credit(item.answer());
            if (item.mandatory() && (item.answer() == TprmAnswer.NO
                    || item.verdict() == TprmVerdict.DOWNGRADED
                    || item.verdict() == TprmVerdict.RETURNED)) {
                knockout = true;
            }
        }
        double percent = denom == 0 ? 0 : earned / denom;
        boolean compliant = !knockout && percent + 1e-9 >= target;
        return new Result(percent, knockout, compliant);
    }

    private static double credit(TprmAnswer answer) {
        return switch (answer) {
            case YES -> 1.0;
            case PARTIAL -> 0.5;
            case NO -> 0.0;
            case NA -> 0.0;
        };
    }
}
