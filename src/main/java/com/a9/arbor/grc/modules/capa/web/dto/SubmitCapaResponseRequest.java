package com.a9.arbor.grc.modules.capa.web.dto;

/**
 * Contributor submission for CAPA - Excel columns O-P
 */
public record SubmitCapaResponseRequest(
        // O: Response Description (Contributor)
        String responseDescription,
        // P: Upload Evidence note (Contributor) - actual files via evidence library
        String contributorEvidenceNote
) {
}
