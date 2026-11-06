package com.a9.arbor.grc.modules.activity.service;

import com.a9.arbor.grc.modules.activity.domain.ActivityTypeCode;
import com.a9.arbor.grc.modules.activity.web.dto.ActivityTypeResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityTypeRegistry {

    public List<ActivityTypeResponse> list() {
        return List.of(
                new ActivityTypeResponse(
                        ActivityTypeCode.ROPA.name(),
                        "Record of Processing Activities (RoPA)",
                        "PRIVACY",
                        "Processing activity register per PIMS Activity / UAE PDPL",
                        true,
                        "/privacy/ropa"),
                new ActivityTypeResponse(
                        ActivityTypeCode.DPIA.name(),
                        "Data Protection Impact Assessment (DPIA)",
                        "PRIVACY",
                        "DPIA linked from RoPA when flagged required",
                        true,
                        "/privacy/dpia"),
                new ActivityTypeResponse(
                        ActivityTypeCode.BIA.name(),
                        "Business Impact Analysis (BIA)",
                        "BCMS",
                        "Registered for Phase 8: BCMS BIA activity (stub)",
                        false,
                        "/bcms/bia"));
    }
}
