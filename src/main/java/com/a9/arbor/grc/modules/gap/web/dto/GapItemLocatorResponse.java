package com.a9.arbor.grc.modules.gap.web.dto;

/**
 * Resolves a Gap control (notification {@code recordId}) to the assessment and project to open.
 */
public record GapItemLocatorResponse(Long itemId, Long assessmentId, Long projectId) {
}
