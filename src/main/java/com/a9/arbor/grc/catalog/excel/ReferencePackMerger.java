package com.a9.arbor.grc.catalog.excel;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Joins reference-pack rows onto the nodes the Master import created.
 *
 * <p>Pure: it takes the node codes and returns what to write, so the join rules live in one place
 * rather than spread through the persistence path.
 *
 * <p>Three outcomes per row, in order:
 * <ol>
 *   <li><b>Exact</b> — the ref is a node code. The pack's questions replace that control's Master
 *       questions, because the packs are the richer authored source.</li>
 *   <li><b>Ancestor attach</b> — the pack is authored deeper than the Master hierarchy goes
 *       (ISO 22301 {@code 4.3} against a catalog that stops at {@code 4}). The questions attach to
 *       the nearest existing ancestor and keep their authored ref in {@code source_ref}; Master
 *       questions on that ancestor <em>survive</em>, because the pack said nothing about the ancestor
 *       itself. No node is created — routing flags would have to be invented, and they drive Gap and
 *       SoA scope.</li>
 *   <li><b>Unmatched</b> — no ancestor exists either (ISO 42001 clauses 8–10, ISO 27701 clause 5).
 *       Reported per row and dropped; the raw row survives in {@code framework_import_row} for the
 *       framework author.</li>
 * </ol>
 */
@Component
public class ReferencePackMerger {

    /** {@code what_it_means} / {@code evidence}; suffixed with the ref when attached to an ancestor. */
    static final String WHAT_IT_MEANS = "what_it_means";
    static final String EVIDENCE = "evidence";

    private static final int MAX_ATTR_KEY = 64;

    /**
     * @param sheets    every sheet of the pack targeting one framework — the ISO 27001 pack splits
     *                  clauses and Annex A across two, and question numbering has to be continuous
     * @param nodeCodes the framework's existing node codes
     */
    public ReferenceMergePlan merge(List<ParsedReferencePack.Sheet> sheets, Set<String> nodeCodes) {
        List<ReferenceMergePlan.QuestionWrite> questions = new ArrayList<>();
        Map<String, ReferenceMergePlan.AttributeWrite> attributes = new LinkedHashMap<>();
        Set<String> replaced = new LinkedHashSet<>();
        Set<String> attached = new LinkedHashSet<>();
        List<ImportStagingRow> staging = new ArrayList<>();
        List<ImportRowError> errors = new ArrayList<>();
        List<String> info = new ArrayList<>();
        Map<String, Integer> ordinalByCode = new LinkedHashMap<>();

        int refRows = 0;
        int exactRows = 0;
        int attachedRows = 0;
        int unmatchedRows = 0;
        int silentRows = 0;

        for (ParsedReferencePack.Sheet sheet : sheets) {
            staging.addAll(sheet.bannerStaging());
            errors.addAll(sheet.errors());
            for (ParsedReferencePack.ReferenceRow row : sheet.rows()) {
                refRows++;
                String ref = row.ref();
                boolean isExact = nodeCodes.contains(ref);
                String target = isExact ? ref : nearestAncestor(ref, nodeCodes);

                if (target == null) {
                    unmatchedRows++;
                    String message = "ref '" + ref + "' matches no node and has no ancestor in the "
                            + "framework — row dropped, nothing was invented to hold it";
                    errors.add(new ImportRowError(row.excelRow(), "ref", message));
                    staging.add(reference(sheet, row, ImportStagingRow.Status.ERROR, message));
                    continue;
                }
                if (isExact) {
                    exactRows++;
                    replaced.add(target);
                } else {
                    attachedRows++;
                    attached.add(target);
                    info.add("Row " + row.excelRow() + " (" + sheet.sheetName() + "): ref '" + ref
                            + "' attached to nearest ancestor '" + target + "', "
                            + segmentsBetween(ref, target) + " level(s) up — "
                            + row.questions().size() + " question(s) keep source_ref '" + ref + "'");
                }

                for (String text : row.questions()) {
                    questions.add(new ReferenceMergePlan.QuestionWrite(
                            target, isExact ? null : ref,
                            ordinalByCode.merge(target, 1, Integer::sum), text));
                }
                if (row.questions().isEmpty()) {
                    silentRows++;
                }

                String suffix = isExact ? "" : ":" + ref;
                putAttribute(attributes, target, WHAT_IT_MEANS + suffix, row.whatItMeans());
                putAttribute(attributes, target, EVIDENCE + suffix, row.evidence());

                staging.add(reference(sheet, row, ImportStagingRow.Status.OK,
                        (isExact ? "matched node '" : "attached to ancestor '") + target + "', "
                                + row.questions().size() + " question(s)"));
            }
        }

        // Only knowable once every sheet has been read: Annex A can exact-match a code that the
        // clauses sheet attached to, and an exact match always outranks an attachment.
        attached.removeAll(replaced);

        if (silentRows > 0) {
            info.add(silentRows + " matched row(s) carried no question text — attributes written, "
                    + "no questions");
        }

        return new ReferenceMergePlan(
                List.copyOf(questions),
                List.copyOf(attributes.values()),
                Set.copyOf(replaced),
                Set.copyOf(attached),
                List.copyOf(staging),
                List.copyOf(errors),
                List.copyOf(info),
                new ReferenceMergePlan.Counts(refRows, exactRows, attachedRows, unmatchedRows));
    }

    /** Trims trailing dotted segments until a real node code is hit: {@code A.4.1 → A.4 → A}. */
    private static String nearestAncestor(String ref, Set<String> nodeCodes) {
        String current = ref;
        while (true) {
            int dot = current.lastIndexOf('.');
            if (dot <= 0) {
                return null;
            }
            current = current.substring(0, dot);
            if (nodeCodes.contains(current)) {
                return current;
            }
        }
    }

    private static int segmentsBetween(String ref, String ancestor) {
        int count = 0;
        for (int i = ancestor.length(); i < ref.length(); i++) {
            if (ref.charAt(i) == '.') {
                count++;
            }
        }
        return count;
    }

    private static ImportStagingRow reference(ParsedReferencePack.Sheet sheet,
                                              ParsedReferencePack.ReferenceRow row,
                                              ImportStagingRow.Status status,
                                              String message) {
        return new ImportStagingRow(sheet.sheetName(), row.excelRow(),
                ImportStagingRow.Kind.REFERENCE, row.rawJson(), status, message);
    }

    private static void putAttribute(Map<String, ReferenceMergePlan.AttributeWrite> attributes,
                                     String target,
                                     String key,
                                     String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String attrKey = key.length() > MAX_ATTR_KEY ? key.substring(0, MAX_ATTR_KEY) : key;
        attributes.putIfAbsent(target + '\u0000' + attrKey,
                new ReferenceMergePlan.AttributeWrite(target, attrKey, value));
    }
}
