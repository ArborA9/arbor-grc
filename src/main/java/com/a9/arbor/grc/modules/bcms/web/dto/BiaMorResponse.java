package com.a9.arbor.grc.modules.bcms.web.dto;

import com.a9.arbor.grc.modules.bcms.domain.BiaMor;
import com.a9.arbor.grc.modules.bcms.pack.BiaStructuredJson;

import java.util.List;

public record BiaMorResponse(
        Long id,
        Long processId,
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
        List<BiaThirdPartyContact> thirdPartyContacts
) {
    public static BiaMorResponse from(BiaMor m) {
        return new BiaMorResponse(
                m.getId(), m.getProcessId(), m.getCapacityPct(), m.getMinStaff(),
                m.getPeople(), m.getTechnology(), m.getPremises(), m.getEquipment(),
                m.getRecordsText(), m.getThirdPartySupport(), m.getRecoveryStrategy(), m.getWorkaround(),
                BiaStructuredJson.people(m.getMinStaffPeople()),
                BiaStructuredJson.strings(m.getRecoveryStrategies()),
                m.getInternetYn(), m.getIntranetYn(), m.getVpnYn(), m.getSiteType(),
                BiaStructuredJson.strings(m.getSoftwareApps()),
                m.getLaptops(), m.getM365Yn(),
                BiaStructuredJson.contacts(m.getThirdPartyContacts()));
    }
}
