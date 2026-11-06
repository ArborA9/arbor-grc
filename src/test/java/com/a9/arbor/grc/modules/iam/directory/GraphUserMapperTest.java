package com.a9.arbor.grc.modules.iam.directory;

import com.a9.arbor.grc.modules.iam.web.dto.EmployeeDirectoryEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GraphUserMapperTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mapsNameTitleDepartmentAndManager() throws Exception {
        var json = mapper.readTree("""
                [
                  {
                    "id": "abc",
                    "displayName": "Jane Doe",
                    "jobTitle": "CISO",
                    "department": "Information Security",
                    "mail": "jane@adfca.gov.ae",
                    "userPrincipalName": "jane@adfca.gov.ae",
                    "manager": {
                      "displayName": "John Smith",
                      "mail": "john@adfca.gov.ae",
                      "jobTitle": "Director",
                      "department": "IT"
                    }
                  }
                ]
                """);
        List<EmployeeDirectoryEntry> rows = GraphUserMapper.fromUsersArray(json);
        assertEquals(1, rows.size());
        EmployeeDirectoryEntry row = rows.get(0);
        assertEquals("abc", row.directoryId());
        assertEquals("jane@adfca.gov.ae", row.email());
        assertEquals("Jane Doe", row.displayName());
        assertEquals("CISO", row.jobTitle());
        assertEquals("Information Security", row.department());
        assertEquals("John Smith", row.managerName());
        assertEquals("john@adfca.gov.ae", row.managerEmail());
        assertEquals("Director", row.managerJobTitle());
        assertEquals("IT", row.managerDepartment());
    }

    @Test
    void usesUpnWhenMailMissingAndLeavesManagerEmpty() throws Exception {
        var json = mapper.readTree("""
                [
                  {
                    "id": "n1",
                    "displayName": "No Mail",
                    "jobTitle": "Analyst",
                    "department": "Risk",
                    "userPrincipalName": "nomail@adfca.gov.ae"
                  }
                ]
                """);
        EmployeeDirectoryEntry row = GraphUserMapper.fromUsersArray(json).get(0);
        assertEquals("nomail@adfca.gov.ae", row.email());
        assertEquals("Analyst", row.jobTitle());
        assertEquals("Risk", row.department());
        assertNull(row.managerName());
        assertNull(row.managerEmail());
    }
}
