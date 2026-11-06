package com.a9.arbor.grc.modules.evidence.domain;

/**
 * Mandatory classification tag (req §4.4 / §17.2). Fixed labels for v1;
 * tenant-renamable levels ({@code DATA_CLASSIFICATION}) come later.
 * <p>Contributor may tag all four levels on upload. Vault browse of others' files
 * is still {@link #CONFIDENTIAL} and {@link #OPEN} only. Evidence linked to an
 * assigned Gap/Risk/CAPA/Privacy record is visible to that contributor at any
 * classification (including Secret/Sensitive).
 */
public enum EvidenceClassification {
    SECRET(1),
    SENSITIVE(2),
    CONFIDENTIAL(3),
    OPEN(4);

    private final int levelOrder;

    EvidenceClassification(int levelOrder) {
        this.levelOrder = levelOrder;
    }

    /** Higher number = less restricted. */
    public int levelOrder() {
        return levelOrder;
    }

    /** Vault-browse visibility for contributors (not linked assigned-record access). */
    public boolean visibleToContributor() {
        return this == CONFIDENTIAL || this == OPEN;
    }
}
