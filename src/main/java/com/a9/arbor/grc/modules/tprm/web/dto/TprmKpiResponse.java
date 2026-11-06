package com.a9.arbor.grc.modules.tprm.web.dto;

public record TprmKpiResponse(
        long total,
        long tier1,
        long tier2,
        long tier3,
        long tier4,
        long critical,
        long nonCritical,
        long compliant,
        long notCompliant,
        long issued,
        long submitted,
        long reassessmentDue
) {
}
