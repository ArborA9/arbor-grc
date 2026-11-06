package com.a9.arbor.grc.modules.demo.web;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.security.RequiresModule;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Proves API-layer module gating ({@link RequiresModule}). Returns 403 when ISMS is not licensed.
 */
@RestController
@RequestMapping("/api/demo")
@ConditionalOnProperty(name = "arbor.security.demo-endpoints-enabled", havingValue = "true", matchIfMissing = true)
public class ModuleGateDemoController {

    @GetMapping("/isms-gate")
    @RequiresModule(ModuleCode.ISMS)
    public Map<String, String> ismsGate() {
        return Map.of(
                "tenant", TenantContext.getTenantId(),
                "module", "ISMS",
                "status", "licensed");
    }
}
