package com.a9.arbor.grc.modules.auditmgmt.domain;

public enum GrcAuditStatus {
    DRAFT,
    ACCESS_GRANTED,
    IN_PROGRESS,
    REPORT_ISSUED,
    CLOSED,
    CANCELLED;

    public boolean setupEditable() {
        return this == DRAFT || this == ACCESS_GRANTED;
    }

    public boolean fieldworkOpen() {
        return this == ACCESS_GRANTED || this == IN_PROGRESS;
    }

    public boolean notesVisibleToAdmin() {
        return this == REPORT_ISSUED || this == CLOSED;
    }

    public boolean terminal() {
        return this == CLOSED || this == CANCELLED || this == REPORT_ISSUED;
    }
}
