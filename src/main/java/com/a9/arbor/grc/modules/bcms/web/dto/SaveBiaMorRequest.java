package com.a9.arbor.grc.modules.bcms.web.dto;

import java.util.List;

public record SaveBiaMorRequest(
        Integer capacityPct,
        Integer minStaff,
        String people,
        String technology,
        String premises,
        String equipment,
        String recordsText,
        String thirdPartySupport,
        String recoveryStrategy,
        String workaround,
        List<BiaPersonRef> minStaffPeople,
        List<String> recoveryStrategies,
        String internetConnectivity,
        String intranetConnectivity,
        String vpnAccess,
        String siteType,
        List<String> softwareApplications,
        Integer laptops,
        String m365,
        List<BiaThirdPartyContact> thirdPartyContacts,
        Boolean enforceRequired
) {
}
