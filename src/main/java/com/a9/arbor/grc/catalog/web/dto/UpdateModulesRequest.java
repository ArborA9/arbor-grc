package com.a9.arbor.grc.catalog.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record UpdateModulesRequest(
        @NotEmpty @Valid List<ModuleToggle> modules,
        /**
         * Shared expiry applied to every module in this request that does not set its own
         * {@code expiresOn}. When omitted on a newly licensed module, Arbor defaults to 1 year.
         */
        LocalDate expiresOn
) {
    public UpdateModulesRequest(List<ModuleToggle> modules) {
        this(modules, null);
    }

    public record ModuleToggle(
            @NotNull String code,
            @NotNull Boolean enabled,
            /**
             * When enabling: null/omit = latest published version per framework code (full pack);
             * explicit list = only those ids (1, some, or all). Empty list is rejected when
             * published frameworks exist. Super Admin UI always sends an explicit list.
             * Ignored when disabling.
             */
            List<Long> frameworkIds,
            /** Per-module expiry (yyyy-MM-dd). Overrides request-level {@code expiresOn}. */
            LocalDate expiresOn
    ) {
        public ModuleToggle(String code, Boolean enabled, List<Long> frameworkIds) {
            this(code, enabled, frameworkIds, null);
        }
    }
}
