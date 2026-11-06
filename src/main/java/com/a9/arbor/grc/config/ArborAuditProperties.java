package com.a9.arbor.grc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "arbor.audit")
public class ArborAuditProperties {

    private Reports reports = new Reports();
    private ExternalAccess externalAccess = new ExternalAccess();

    @Data
    public static class Reports {
        /** {@code local} (default) or {@code blob} (Azure, reuses evidence blob connection). */
        private String storage = "local";
        private String storageRoot = ".run/audit-reports";
        /** Footer label on every PDF page (req §4.4 classification). */
        private String classificationFooter = "CONFIDENTIAL";
    }

    @Data
    public static class ExternalAccess {
        /** Default OTP invite lifetime in days (req §5.2 vendor parity). */
        private int expiryDays = 7;
        /** When true, invite API echoes access link + OTP for local/E2E (never enable in production). */
        private boolean exposeCredentialsInResponse = true;
    }
}
