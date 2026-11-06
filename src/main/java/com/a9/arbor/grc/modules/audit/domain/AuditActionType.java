package com.a9.arbor.grc.modules.audit.domain;

/** Req §8 action_type vocabulary. */
public enum AuditActionType {
    CREATED,
    UPDATED,
    DELETED,
    APPROVED,
    REJECTED,
    LOGIN,
    LOGOUT,
    EXPORT,
    IMPERSONATED
}
