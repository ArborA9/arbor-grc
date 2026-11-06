package com.a9.arbor.grc.modules.gap.service;

/**
 * Evidence link identity for a Gap control question.
 * {@code recordType=GAP_QUESTION}, {@code recordId={itemId}:{questionKey}}
 * e.g. {@code 501:c:12} (catalog) or {@code 501:u:3} (custom).
 */
public final class GapEvidenceLinks {

    public static final String RECORD_TYPE = "GAP_QUESTION";

    private GapEvidenceLinks() {
    }

    public static String questionRecordId(long gapItemId, String questionKey) {
        return gapItemId + ":" + questionKey;
    }

    public static Long itemIdFromQuestionRecord(String recordId) {
        if (recordId == null) {
            return null;
        }
        String raw = recordId.trim();
        int colon = raw.indexOf(':');
        if (colon <= 0) {
            return null;
        }
        try {
            return Long.parseLong(raw.substring(0, colon));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
