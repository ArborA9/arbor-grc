package com.a9.arbor.grc.catalog.domain;

/**
 * What happens when a tenant has JIT provisioning enabled and an SSO login has no matching Arbor
 * account. {@code BLOCK} keeps today's behaviour (deny + notify Admin); {@code CONTRIBUTOR}
 * auto-creates the user as a Contributor (still notifies Admin so org-unit assignment can follow).
 */
public enum JitDefaultRole {
    BLOCK,
    CONTRIBUTOR
}
