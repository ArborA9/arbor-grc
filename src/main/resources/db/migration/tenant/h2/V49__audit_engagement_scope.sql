-- Phase A — audit engagement team + data scope

CREATE TABLE IF NOT EXISTS grc_audit_team_member (
    audit_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    team_role VARCHAR(16) NOT NULL,
    CONSTRAINT pk_grc_audit_team PRIMARY KEY (audit_id, user_id),
    CONSTRAINT fk_grc_audit_team_audit FOREIGN KEY (audit_id) REFERENCES grc_audit (id) ON DELETE CASCADE,
    CONSTRAINT fk_grc_audit_team_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE INDEX IF NOT EXISTS idx_grc_audit_team_user ON grc_audit_team_member (user_id);

CREATE TABLE IF NOT EXISTS grc_audit_scope_node (
    audit_id BIGINT NOT NULL,
    framework_node_id BIGINT NOT NULL,
    CONSTRAINT pk_grc_audit_scope_node PRIMARY KEY (audit_id, framework_node_id),
    CONSTRAINT fk_grc_audit_scope_node_audit FOREIGN KEY (audit_id) REFERENCES grc_audit (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS grc_audit_scope_module (
    audit_id BIGINT NOT NULL,
    module_code VARCHAR(32) NOT NULL,
    CONSTRAINT pk_grc_audit_scope_module PRIMARY KEY (audit_id, module_code),
    CONSTRAINT fk_grc_audit_scope_module_audit FOREIGN KEY (audit_id) REFERENCES grc_audit (id) ON DELETE CASCADE
);

INSERT INTO grc_audit_team_member (audit_id, user_id, team_role)
SELECT a.id, a.lead_auditor_user_id, 'LEAD'
FROM grc_audit a
WHERE NOT EXISTS (
    SELECT 1 FROM grc_audit_team_member t
    WHERE t.audit_id = a.id AND t.user_id = a.lead_auditor_user_id
);

INSERT INTO grc_audit_scope_module (audit_id, module_code)
SELECT a.id, m.module_code
FROM grc_audit a
CROSS JOIN (
    SELECT 'GAP' AS module_code UNION ALL
    SELECT 'SOA' UNION ALL
    SELECT 'RISK' UNION ALL
    SELECT 'EVIDENCE'
) m
WHERE NOT EXISTS (
    SELECT 1 FROM grc_audit_scope_module s WHERE s.audit_id = a.id
);
