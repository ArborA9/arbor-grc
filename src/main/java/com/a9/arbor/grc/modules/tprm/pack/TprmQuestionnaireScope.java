package com.a9.arbor.grc.modules.tprm.pack;

import com.a9.arbor.grc.modules.tprm.domain.TprmControl;
import com.a9.arbor.grc.modules.tprm.domain.VendorType;

import java.util.List;

public final class TprmQuestionnaireScope {

    private TprmQuestionnaireScope() {
    }

    public static boolean include(TprmControl control, VendorType type, String tier) {
        int vendorTier = TprmLookups.tierNumber(tier);
        if (control.getAskedFromTier() < vendorTier) {
            return false;
        }
        String module = control.getModule();
        if ("CLOUD".equals(module)) {
            return type.cloudAddon();
        }
        if ("AI".equals(module)) {
            return type.aiAddon();
        }
        if (type.omitSoftwareControls()
                && ("D5-04".equals(control.getControlCode()) || "D5-05".equals(control.getControlCode()))) {
            return false;
        }
        return true;
    }

    public static List<TprmControl> scoped(List<TprmControl> library, VendorType type, String tier) {
        return library.stream()
                .filter(c -> include(c, type, tier))
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .toList();
    }
}
