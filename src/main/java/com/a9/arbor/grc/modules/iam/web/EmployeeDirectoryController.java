package com.a9.arbor.grc.modules.iam.web;

import com.a9.arbor.grc.modules.iam.service.EmployeeDirectoryService;
import com.a9.arbor.grc.modules.iam.web.dto.EmployeeDirectorySearchResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Live Entra / Graph employee search with Arbor user fallback in the BIA people picker. */
@RestController
@RequestMapping("/api/directory")
public class EmployeeDirectoryController {

    private final EmployeeDirectoryService employeeDirectoryService;

    public EmployeeDirectoryController(EmployeeDirectoryService employeeDirectoryService) {
        this.employeeDirectoryService = employeeDirectoryService;
    }

    @GetMapping("/employees")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','CONTRIBUTOR')")
    public EmployeeDirectorySearchResponse search(@RequestParam("q") String q) {
        return employeeDirectoryService.search(q);
    }
}
