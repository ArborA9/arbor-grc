package com.a9.arbor.grc.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditorAccessFilterTest {

    @Test
    void auditorSignIsAssignedFieldworkWrite() {
        assertFalse(AuditorAccessFilter.isFieldworkWrite("/api/audits/12/close"));
        assertTrue(AuditorAccessFilter.isFieldworkWrite("/api/audits/12/auditor-sign"));
        assertTrue(AuditorAccessFilter.isFieldworkWrite("/api/audits/12/scope"));
        assertTrue(AuditorAccessFilter.isFieldworkWrite("/api/audits/12/findings"));
        assertFalse(AuditorAccessFilter.isFieldworkWrite("/api/capa"));
        assertFalse(AuditorAccessFilter.isFieldworkWrite("/api/audits/12/admin-sign"));
        assertTrue(AuditorAccessFilter.isFieldworkWrite("/api/audits/12/checklists/upload"));
        assertTrue(AuditorAccessFilter.isFieldworkWrite("/api/audits/12/checklists/3/upload"));
    }

    @Test
    void blankChecklistTemplateIsNotADataExport() {
        assertTrue(AuditorAccessFilter.isBlankChecklistTemplate("/api/audits/checklist-library/template.xlsx"));
        assertFalse(AuditorAccessFilter.isDataExport("/api/audits/checklist-library/template.xlsx"));
        assertTrue(AuditorAccessFilter.isDataExport("/api/risks/template.xlsx"));
    }

    @Test
    void requestPathJoinsServletMappingAndPathInfo() {
        MockHttpServletRequest mapped = new MockHttpServletRequest("POST", "/api/audits/5/auditor-sign");
        mapped.setServletPath("/api");
        mapped.setPathInfo("/audits/5/auditor-sign");
        assertTrue(AuditorAccessFilter.isFieldworkWrite(AuditorAccessFilter.servletPath(mapped)));

        MockHttpServletRequest full = new MockHttpServletRequest("POST", "/api/audits/5/auditor-sign/");
        full.setServletPath("/api/audits/5/auditor-sign/");
        assertTrue(AuditorAccessFilter.isFieldworkWrite(AuditorAccessFilter.servletPath(full)));
    }
}
