package com.a9.arbor.grc.security;

import com.a9.arbor.grc.catalog.domain.ModuleCode;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API-layer licensing gate (req §3.5 layer 2). Unlicensed → 403.
 * Enforced by {@link ModuleLicenseInterceptor}.
 * Pass one or more codes: any licensed module satisfies the gate
 * (service layer still enforces type-specific registers, e.g. IS Risk vs ERM).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresModule {
    ModuleCode[] value();
}
