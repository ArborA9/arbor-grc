package com.a9.arbor.grc.modules.org.web.dto;

/** Lightweight, universally-readable session policy for the SPA idle timer and workspace name. */
public record SessionPolicyResponse(int idleTimeoutMinutes, String displayName) {
}
