package com.a9.arbor.grc.modules.tprm.pack;

import com.a9.arbor.grc.modules.tprm.domain.TprmAnswer;
import com.a9.arbor.grc.modules.tprm.domain.TprmControl;
import com.a9.arbor.grc.modules.tprm.domain.VendorType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TprmPostureTest {

    @Test
    void yesFullPartialHalfNoZeroNaExcluded() {
        TprmPosture.Result r = TprmPosture.evaluate(List.of(
                new TprmPosture.Item(2, false, TprmAnswer.YES, null),
                new TprmPosture.Item(2, false, TprmAnswer.PARTIAL, null),
                new TprmPosture.Item(2, false, TprmAnswer.NO, null),
                new TprmPosture.Item(10, false, TprmAnswer.NA, null)
        ), 0.6);
        assertEquals(0.5, r.percent(), 1e-9);
        assertFalse(r.knockout());
        assertFalse(r.compliant());
    }

    @Test
    void mandatoryNoIsKnockout() {
        TprmPosture.Result r = TprmPosture.evaluate(List.of(
                new TprmPosture.Item(3, true, TprmAnswer.NO, null),
                new TprmPosture.Item(1, false, TprmAnswer.YES, null)
        ), 0.6);
        assertTrue(r.knockout());
        assertFalse(r.compliant());
    }
}

class TprmQuestionnaireScopeTest {

    @Test
    void omitsSoftwareBuildForNonIt() {
        TprmControl d504 = control("D5-04", "CORE", 4);
        TprmControl d101 = control("D1-01", "CORE", 4);
        TprmControl cloud = control("C1-01", "CLOUD", 4);
        TprmControl ai = control("A1-01", "AI", 4);
        assertFalse(TprmQuestionnaireScope.include(d504, VendorType.NON_IT_SERVICES, "TIER_4"));
        assertTrue(TprmQuestionnaireScope.include(d101, VendorType.NON_IT_SERVICES, "TIER_4"));
        assertFalse(TprmQuestionnaireScope.include(cloud, VendorType.NON_IT_SERVICES, "TIER_1"));
        assertTrue(TprmQuestionnaireScope.include(cloud, VendorType.CLOUD_SERVICE_PROVIDER, "TIER_1"));
        assertTrue(TprmQuestionnaireScope.include(ai, VendorType.AI_SERVICE_PROVIDER, "TIER_1"));
        assertFalse(TprmQuestionnaireScope.include(ai, VendorType.CLOUD_SERVICE_PROVIDER, "TIER_1"));
    }

    private static TprmControl control(String code, String module, int askedFrom) {
        TprmControl c = new TprmControl();
        c.setControlCode(code);
        c.setModule(module);
        c.setAskedFromTier(askedFrom);
        c.setSortOrder(1);
        return c;
    }
}
