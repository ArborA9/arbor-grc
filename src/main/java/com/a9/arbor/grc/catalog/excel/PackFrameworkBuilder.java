package com.a9.arbor.grc.catalog.excel;

import com.a9.arbor.grc.catalog.domain.FrameworkTypeCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Builds a complete framework — hierarchy included — out of a reference pack, for the frameworks the
 * Master workbook never carried.
 *
 * <p>The packs have no {@code level1_code..level4_code} columns and no routing flags, so both are
 * derived here:
 *
 * <ul>
 *   <li><b>Hierarchy comes from the ref codes, titles from the banner rows.</b> Not the other way
 *       round: the codes are complete by construction, whereas a missing banner would silently drop a
 *       whole branch. Verified against UAE IA, where the two agree exactly — 15 domains and 47
 *       sub-domains, every one bannered.</li>
 *   <li><b>Routing is a per-framework policy</b> declared in {@link #ROUTING}, because no pack states
 *       it. Anything not covered stays unrouted and so is excluded from {@code control_count}.</li>
 * </ul>
 *
 * <p>Nothing is invented beyond structural keys: titles, statements and questions are the client's
 * text or absent. Where a key has to be synthesised — a PDPL part code, a disambiguated duplicate
 * ref — it is reported in {@link PackFrameworkPlan#info()}.
 */
@Component
public class PackFrameworkBuilder {

    /** {@code node_code} / {@code title} column widths from {@code V5__framework_library.sql}. */
    private static final int MAX_CODE = 64;
    private static final int MAX_TITLE = 512;

    static final String APPLICABILITY = "applicability";

    /**
     * A banner that names a node: {@code "M1. STRATEGY AND PLANNING"}, {@code "M1.1 ENTITY CONTEXT"},
     * {@code "A.2 Policies related to AI — Objective: …"} (ISO Annex themes).
     *
     * <p>Two code shapes: letter+dotted ({@code A.2}, {@code A.6.1}) and letter-optional+digits
     * ({@code M1}, {@code 6.1}). Category banners ({@code "▌ MANAGEMENT CONTROLS"}) do not match —
     * they describe the {@code M}/{@code T} split, which is already recoverable from the code's first
     * character and is deliberately not a node. See §F3b on why there is no Category level.
     */
    private static final Pattern CODED_BANNER = Pattern.compile(
            "^([A-Za-z]+(?:\\.\\d+)+|[A-Za-z]*\\d+(?:\\.\\d+)*)\\.?\\s+(.+)$");

    /** Trailing objective blurb on ISO Annex theme banners (keep the short theme name as title). */
    private static final Pattern OBJECTIVE_SUFFIX = Pattern.compile(
            "\\s*[\\u2014\\u2013\\-]\\s*Objective\\s*:.*$", Pattern.CASE_INSENSITIVE);

    /**
     * ISO 42001-style annex theme row: {@code "ANNEX A — A.2 POLICIES RELATED TO AI | …"}.
     */
    private static final Pattern ANNEX_CODED_BANNER = Pattern.compile(
            "^ANNEX\\s+[A-Za-z]\\s*[\\u2014\\u2013\\-]\\s*([A-Za-z]\\.\\d+)\\s+(.+?)(?:\\s*\\|.*)?$",
            Pattern.CASE_INSENSITIVE);

    /**
     * ISO 27701-style annex table row: {@code "ANNEX A — TABLE A.1: CONTROLS FOR PII CONTROLLERS | …"}.
     */
    private static final Pattern ANNEX_TABLE_BANNER = Pattern.compile(
            "^ANNEX\\s+[A-Za-z]\\s*[\\u2014\\u2013\\-]\\s*TABLE\\s+([A-Za-z]\\.\\d+)\\s*:\\s*(.+?)(?:\\s*\\|.*)?$",
            Pattern.CASE_INSENSITIVE);

    /**
     * SAMA domain or subdomain banner: {@code "3.1 — Cyber Security Leadership and Governance"}
     * or {@code "3.1.1  Cyber Security Governance"} (spaces, no dash).
     */
    private static final Pattern SAMA_BANNER = Pattern.compile(
            "^(\\d+(?:\\.\\d+)*)(?:\\s*[\\u2014\\u2013\\-]\\s+|\\s+)(.+)$");

    /**
     * ISO clause section headers in V2 packs: {@code "SECTION 6 - PLANNING"} (ASCII hyphen) or with
     * an em/en dash. Captures the section number as the node code.
     */
    private static final Pattern SECTION_NUMBER_BANNER = Pattern.compile(
            "^SECTION\\s+(\\d+(?:\\.\\d+)*)\\s*[\\u2014\\u2013\\-]\\s*(.+)$",
            Pattern.CASE_INSENSITIVE);

    /**
     * A banner that opens a numbered section: {@code "PART 1 — GENERAL PROVISIONS"} (UAE PDPL),
     * {@code "CHAPTER 1 — GENERAL PROVISIONS"} (GDPR). The client gives these no code of their own, so
     * one is synthesised from the keyword and number and reported.
     *
     * <p>The number may be a range — GDPR has {@code "CHAPTER 6-8 — SUPERVISORY AUTHORITIES AND
     * ENFORCEMENT"} — and the separator must be an em or en dash, not a plain hyphen, or that range
     * would be read as chapter 6 titled "8 — Supervisory…". {@code SECTION} banners with a plain
     * hyphen are handled by {@link #SECTION_NUMBER_BANNER} instead (ISO packs).
     */
    private static final Pattern PART_BANNER = Pattern.compile(
            "^(PART|CHAPTER)\\s+(\\d+(?:\\s*-\\s*\\d+)?)\\s*[\\u2014\\u2013]\\s*(.+)$",
            Pattern.CASE_INSENSITIVE);

    /**
     * {@code "FUNCTION: GV — GOVERN"} (NIST CSF). Unlike the parts above, these carry their own code,
     * and it is the prefix of every category ref beneath them ({@code GV.OC}).
     */
    private static final Pattern FUNCTION_BANNER = Pattern.compile(
            "^FUNCTION:\\s*([A-Z]{2,6})\\s*[\\u2014\\u2013]\\s*(.+)$", Pattern.CASE_INSENSITIVE);

    private static final Routing UNROUTED = new Routing(false, false, false, false);

    /**
     * Per-framework routing, set with A9 rather than read from the workbook.
     *
     * <p><b>UAE IA</b> — the pack's own header calls the depth-4 rows "the detailed audit criteria
     * within each control", so the 134 controls drive SoA, Gap and Risk while the 446
     * sub-requirements drive the audit checklist.
     *
     * <p><b>UAE PDPL and GDPR</b> — Gap and audit only. The Statement of Applicability is an ISO 27001
     * Annex A construct for selecting controls against a risk assessment, which does not fit legal
     * articles that apply by force of law: there is no "not applicable, justified" for a statute.
     *
     * <p><b>NIST CSF</b> — Gap, audit and risk. Its categories are risk-management outcomes and are a
     * natural target for a risk link, but CSF has no applicability statement, so no SoA.
     *
     * <p><b>CIS v8</b> — all four. The 18 safeguard groups are controls in the ordinary sense, and
     * scoping them (the IG1/IG2/IG3 implementation groups) is exactly what an SoA records.
     */
    /**
     * How a pack expresses structure. {@code DOTTED} reads it from the ref codes ({@code M1.1.1});
     * {@code SECTIONED} has flat refs ({@code Art.4}) whose only grouping is the banner above them;
     * {@code FLAT} has no hierarchy to recover.
     */
    private enum Layout {
        DOTTED, SECTIONED, FLAT, SAMA
    }

    private static final Map<FrameworkTypeCode, Map<Integer, Routing>> ROUTING = new LinkedHashMap<>();
    private static final Map<FrameworkTypeCode, Layout> LAYOUTS = new LinkedHashMap<>();

    static {
        Routing isoLeaf = new Routing(true, true, true, true);
        Routing guidanceNone = new Routing(false, false, false, false);
        Map<Integer, Routing> isoLevels = Map.of(
                1, isoLeaf,
                2, isoLeaf,
                3, isoLeaf,
                4, isoLeaf);

        ROUTING.put(FrameworkTypeCode.UAE_IA, Map.of(
                // Control (L3): SoA + Risk. Gap scores sub-controls and rolls up (V2 Phase 5).
                3, new Routing(true, false, false, true),
                // Sub-control (L4): Gap + Audit.
                4, new Routing(false, true, true, false)));
        ROUTING.put(FrameworkTypeCode.UAE_PDPL, Map.of(
                2, new Routing(false, true, true, false)));
        ROUTING.put(FrameworkTypeCode.GDPR, Map.of(
                2, new Routing(false, true, true, false)));
        ROUTING.put(FrameworkTypeCode.NIST_CSF, Map.of(
                2, new Routing(false, true, true, true)));
        ROUTING.put(FrameworkTypeCode.CIS_V8, Map.of(
                1, new Routing(true, true, true, true)));
        ROUTING.put(FrameworkTypeCode.ISO_27001, isoLevels);
        ROUTING.put(FrameworkTypeCode.ISO_22301, isoLevels);
        ROUTING.put(FrameworkTypeCode.ISO_42001, isoLevels);
        ROUTING.put(FrameworkTypeCode.ISO_27701, isoLevels);
        ROUTING.put(FrameworkTypeCode.ISO_20000, isoLevels);
        ROUTING.put(FrameworkTypeCode.ISO_9001, isoLevels);
        ROUTING.put(FrameworkTypeCode.ISO_45001, isoLevels);
        ROUTING.put(FrameworkTypeCode.NCEMA_V3, isoLevels);
        ROUTING.put(FrameworkTypeCode.SAMA_CSF, Map.of(1, isoLeaf, 2, isoLeaf, 3, isoLeaf));
        // Guidance only — importable hierarchy, no SoA/Gap/Audit/Risk leaves.
        ROUTING.put(FrameworkTypeCode.ISO_31000, Map.of(
                1, guidanceNone, 2, guidanceNone, 3, guidanceNone, 4, guidanceNone));

        LAYOUTS.put(FrameworkTypeCode.UAE_IA, Layout.DOTTED);
        LAYOUTS.put(FrameworkTypeCode.UAE_PDPL, Layout.SECTIONED);
        LAYOUTS.put(FrameworkTypeCode.GDPR, Layout.SECTIONED);
        LAYOUTS.put(FrameworkTypeCode.NIST_CSF, Layout.SECTIONED);
        LAYOUTS.put(FrameworkTypeCode.CIS_V8, Layout.FLAT);
        LAYOUTS.put(FrameworkTypeCode.ISO_27001, Layout.DOTTED);
        LAYOUTS.put(FrameworkTypeCode.ISO_22301, Layout.DOTTED);
        LAYOUTS.put(FrameworkTypeCode.ISO_42001, Layout.DOTTED);
        LAYOUTS.put(FrameworkTypeCode.ISO_27701, Layout.DOTTED);
        LAYOUTS.put(FrameworkTypeCode.ISO_20000, Layout.DOTTED);
        LAYOUTS.put(FrameworkTypeCode.ISO_9001, Layout.DOTTED);
        LAYOUTS.put(FrameworkTypeCode.ISO_45001, Layout.DOTTED);
        LAYOUTS.put(FrameworkTypeCode.NCEMA_V3, Layout.DOTTED);
        LAYOUTS.put(FrameworkTypeCode.ISO_31000, Layout.DOTTED);
        LAYOUTS.put(FrameworkTypeCode.SAMA_CSF, Layout.SAMA);
    }

    /** Whether this framework's hierarchy can be derived from its pack at all. */
    public boolean supports(FrameworkTypeCode type) {
        return LAYOUTS.containsKey(type);
    }

    public PackFrameworkPlan build(FrameworkTypeCode type, List<ParsedReferencePack.Sheet> sheets) {
        return build(type, sheets, List.of());
    }

    public PackFrameworkPlan build(FrameworkTypeCode type,
                                   List<ParsedReferencePack.Sheet> sheets,
                                   List<ParsedReferencePack.DomainObjective> domainObjectives) {
        Builder builder = new Builder(type);
        Layout layout = LAYOUTS.get(type);
        for (ParsedReferencePack.Sheet sheet : sheets) {
            switch (layout) {
                case DOTTED -> builder.dotted(sheet);
                case SECTIONED -> builder.sectioned(sheet);
                case FLAT -> builder.flat(sheet);
                case SAMA -> builder.sama(sheet);
            }
        }
        builder.applyDomainObjectives(domainObjectives);
        return builder.plan();
    }

    // ------------------------------------------------------------------ per-import state

    private static final class Builder {

        private final FrameworkTypeCode type;
        private final Map<Integer, Routing> routing;
        private final Map<String, Draft> byCode = new LinkedHashMap<>();
        private final List<ParsedMasterWorkbook.Question> questions = new ArrayList<>();
        private final List<ImportStagingRow> staging = new ArrayList<>();
        private final List<ImportRowError> errors = new ArrayList<>();
        private final List<String> info = new ArrayList<>();
        private int order;
        private int missingTitles;

        Builder(FrameworkTypeCode type) {
            this.type = type;
            this.routing = ROUTING.getOrDefault(type, Map.of());
        }

        // -------------------------------------------------------------- dotted refs (UAE IA)

        void dotted(ParsedReferencePack.Sheet sheet) {
            Map<String, String> titles = bannerTitles(sheet);
            staging.addAll(sheet.bannerStaging());
            errors.addAll(sheet.errors());

            for (ParsedReferencePack.ReferenceRow row : sheet.rows()) {
                String[] segments = row.ref().split("\\.");
                String parent = null;
                for (int depth = 1; depth < segments.length; depth++) {
                    String code = String.join(".", Arrays.copyOfRange(segments, 0, depth));
                    // Doc 02 T-08: do not invent a lone "A" folder for Annex refs (A.5.1 → A → A.5).
                    if (isLetterOnlyAnnexRoot(code)) {
                        continue;
                    }
                    Draft ancestor = byCode.get(code);
                    if (ancestor == null) {
                        String title = titles.get(code);
                        if (title == null) {
                            missingTitles++;
                        }
                        ancestor = add(code, depth, parent, title == null ? code : title, row.excelRow());
                    }
                    parent = ancestor.code;
                }
                leaf(sheet, row, segments.length, parent);
            }
        }

        /**
         * Titles for the nodes above the authored refs. Ambiguity is impossible in practice — each
         * banner names a distinct code — so the first one wins and a repeat is reported.
         */
        private Map<String, String> bannerTitles(ParsedReferencePack.Sheet sheet) {
            Map<String, String> titles = new LinkedHashMap<>();
            for (ParsedReferencePack.Banner banner : sheet.banners()) {
                String text = clean(banner.text());
                Matcher coded = CODED_BANNER.matcher(text);
                if (coded.matches()) {
                    // Prefer the theme/objective banner over an earlier ALL-CAPS ANNEX row.
                    titles.put(coded.group(1), bannerTitle(coded.group(2)));
                    continue;
                }
                Matcher annexTable = ANNEX_TABLE_BANNER.matcher(text);
                if (annexTable.matches()) {
                    titles.putIfAbsent(annexTable.group(1), bannerTitle(annexTable.group(2)));
                    continue;
                }
                Matcher annexCoded = ANNEX_CODED_BANNER.matcher(text);
                if (annexCoded.matches()) {
                    titles.putIfAbsent(annexCoded.group(1), bannerTitle(annexCoded.group(2)));
                    continue;
                }
                Matcher section = SECTION_NUMBER_BANNER.matcher(text);
                if (section.matches()) {
                    titles.putIfAbsent(section.group(1), bannerTitle(section.group(2)));
                }
            }
            return titles;
        }

        // -------------------------------------------------------------- banner sections (PDPL)

        void sectioned(ParsedReferencePack.Sheet sheet) {
            errors.addAll(sheet.errors());

            // Banners and rows interleave, and a section owns the rows that follow it, so this one
            // walks the sheet in document order rather than by code.
            List<Object> document = new ArrayList<>();
            document.addAll(sheet.banners());
            document.addAll(sheet.rows());
            document.sort(Comparator.comparingInt(Builder::excelRowOf));

            Draft section = null;
            for (Object entry : document) {
                if (entry instanceof ParsedReferencePack.Banner banner) {
                    staging.add(banner.staging());
                    Section parsed = section(clean(banner.text()));
                    if (parsed != null) {
                        section = add(parsed.code(), 1, null, parsed.title(), banner.excelRow());
                        if (parsed.synthesisedCode()) {
                            info.add("Row " + banner.excelRow() + ": section '" + parsed.title()
                                    + "' has no code in the pack — filed as '" + parsed.code() + "'");
                        }
                    } else if (section != null && section.definition == null) {
                        // The line under a section header is its description, not another section.
                        section.definition = banner.text();
                    }
                } else {
                    ParsedReferencePack.ReferenceRow row = (ParsedReferencePack.ReferenceRow) entry;
                    leaf(sheet, row, section == null ? 1 : 2, section == null ? null : section.code);
                }
            }
        }

        private static int excelRowOf(Object entry) {
            return entry instanceof ParsedReferencePack.Banner banner
                    ? banner.excelRow()
                    : ((ParsedReferencePack.ReferenceRow) entry).excelRow();
        }

        // -------------------------------------------------------------- no hierarchy (CIS v8)

        void flat(ParsedReferencePack.Sheet sheet) {
            errors.addAll(sheet.errors());
            staging.addAll(sheet.bannerStaging());
            for (ParsedReferencePack.ReferenceRow row : sheet.rows()) {
                leaf(sheet, row, 1, null);
            }
        }

        /**
         * SAMA CSF: Domain (L1) → Subdomain (L2) → Control (L3). Leaf codes are
         * {@code {subdomain}-{ctrl#}} (e.g. {@code 3.1.1-1}) so Ctrl # values do not collide.
         * Section titles come from the banner rows ({@code 3.1}, {@code 3.1.1}).
         */
        void sama(ParsedReferencePack.Sheet sheet) {
            errors.addAll(sheet.errors());
            staging.addAll(sheet.bannerStaging());
            Map<String, String> titles = samaBannerTitles(sheet);
            for (ParsedReferencePack.ReferenceRow row : sheet.rows()) {
                String subdomain = row.structuralParent();
                String domain = domainOfSubdomain(subdomain);
                if (domain != null && byCode.get(domain) == null) {
                    add(domain, 1, null, titled(domain, titles), row.excelRow());
                }
                if (subdomain != null && byCode.get(subdomain) == null) {
                    add(subdomain, 2, domain, titled(subdomain, titles), row.excelRow());
                }
                leaf(sheet, row, 3, subdomain);
            }
        }

        /** Banner text to node title. A code with no banner is named after the code. */
        private String titled(String code, Map<String, String> titles) {
            String title = titles.get(code);
            if (title == null || title.isBlank()) {
                missingTitles++;
                return code;
            }
            return title;
        }

        private Map<String, String> samaBannerTitles(ParsedReferencePack.Sheet sheet) {
            Map<String, String> titles = new LinkedHashMap<>();
            for (ParsedReferencePack.Banner banner : sheet.banners()) {
                Matcher coded = SAMA_BANNER.matcher(clean(banner.text()));
                if (coded.matches()) {
                    titles.put(coded.group(1), bannerTitle(coded.group(2)));
                }
            }
            return titles;
        }

        private static String domainOfSubdomain(String subdomain) {
            if (subdomain == null || subdomain.isBlank()) {
                return null;
            }
            String[] parts = subdomain.split("\\.");
            if (parts.length >= 2) {
                return parts[0] + "." + parts[1];
            }
            return subdomain;
        }

        // -------------------------------------------------------------- shared

        /**
         * Writes the authored row onto its node. A ref that already exists as an auto-created
         * ancestor <em>fills that node in</em> rather than duplicating it; a ref that collides with
         * another authored row is suffixed, because {@code node_code} is unique per framework and
         * dropping the row would lose client text. Both are reported.
         */
        private void leaf(ParsedReferencePack.Sheet sheet,
                          ParsedReferencePack.ReferenceRow row,
                          int level,
                          String parent) {
            String code = row.ref();
            Draft existing = byCode.get(code);
            if (existing != null && existing.authored) {
                String unique = disambiguate(code);
                info.add("Row " + row.excelRow() + " (" + sheet.sheetName() + "): ref '" + code
                        + "' is already used by '" + existing.title + "' — this row ('"
                        + (row.title() == null ? "untitled" : row.title()) + "') was filed as '"
                        + unique + "'. Two different articles share one ref in the source workbook.");
                code = unique;
                existing = null;
            }

            Draft draft = existing != null
                    ? existing
                    : add(code, level, parent, null, row.excelRow());
            draft.authored = true;
            draft.title = truncate(
                    row.title() != null ? row.title()
                            : row.definition() != null ? row.definition() : code,
                    MAX_TITLE);
            draft.definition = row.definition();
            draft.priority = row.priority();
            draft.recordKind = sheetSectionKind(sheet.sheetName());
            draft.routing = routing.getOrDefault(level, UNROUTED);
            put(draft.attributes, APPLICABILITY, row.applicability());
            put(draft.attributes, ReferencePackMerger.WHAT_IT_MEANS, row.whatItMeans());
            put(draft.attributes, ReferencePackMerger.EVIDENCE, row.evidence());
            put(draft.attributes, "section_type", draft.recordKind);
            put(draft.attributes, "sheet_tab", sheet.sheetName());
            if (row.applicability() != null && !row.applicability().isBlank()) {
                String app = row.applicability().trim().toLowerCase(Locale.ROOT);
                if (app.contains("always")) {
                    put(draft.attributes, "default_applicability", "ALWAYS");
                } else if (app.contains("risk")) {
                    put(draft.attributes, "default_applicability", "RISK_BASED");
                } else {
                    put(draft.attributes, "default_applicability", "CHOOSABLE");
                }
            }

            int number = 0;
            for (String text : row.questions()) {
                number++;
                questions.add(new ParsedMasterWorkbook.Question(
                        row.excelRow(), draft.code, "Q" + number,
                        // The packs carry no question_type / answer_options / is_mandatory column;
                        // see §F3 "deliberately not written".
                        null, text, null, null, false, questions.size() + 1));
            }

            staging.add(new ImportStagingRow(sheet.sheetName(), row.excelRow(),
                    ImportStagingRow.Kind.REFERENCE, row.rawJson(), ImportStagingRow.Status.OK,
                    "level " + level + " node '" + draft.code + "', " + row.questions().size()
                            + " question(s)"));
        }

        private void applyDomainObjectives(List<ParsedReferencePack.DomainObjective> objectives) {
            if (objectives == null || objectives.isEmpty()) {
                return;
            }
            int attached = 0;
            for (ParsedReferencePack.DomainObjective objective : objectives) {
                if (objective == null || objective.code() == null || objective.code().isBlank()) {
                    continue;
                }
                if (objective.objective() == null || objective.objective().isBlank()) {
                    continue;
                }
                Draft draft = byCode.get(objective.code().trim());
                if (draft == null) {
                    continue;
                }
                put(draft.attributes, "domain_objective", objective.objective().trim());
                if (objective.title() != null && !objective.title().isBlank()
                        && (draft.title == null || draft.title.equals(draft.code))) {
                    draft.title = truncate(objective.title(), MAX_TITLE);
                }
                attached++;
            }
            if (attached > 0) {
                info.add("domain_objective attributes attached to " + attached + " domain/sub-domain node(s)");
            }
        }

        private String disambiguate(String code) {
            for (int suffix = 2; ; suffix++) {
                String candidate = truncate(code, MAX_CODE - 2) + "#" + suffix;
                if (!byCode.containsKey(candidate)) {
                    return candidate;
                }
            }
        }

        private Draft add(String code, int level, String parent, String title, int excelRow) {
            Draft draft = new Draft();
            draft.code = truncate(code, MAX_CODE);
            draft.level = level;
            draft.parent = parent;
            draft.title = truncate(title == null ? code : title, MAX_TITLE);
            draft.displayOrder = ++order;
            draft.excelRow = excelRow;
            draft.routing = UNROUTED;
            byCode.put(draft.code, draft);
            return draft;
        }

        PackFrameworkPlan plan() {
            if (missingTitles > 0) {
                info.add(missingTitles + " parent node(s) had no banner to take a title from and are "
                        + "named after their code");
            }
            // framework_node_attribute has no read endpoint until F4, so the only way to see what was
            // stored — and the only way an E2E can assert it — is this line.
            Map<String, Integer> written = new LinkedHashMap<>();
            byCode.values().forEach(d -> d.attributes.keySet()
                    .forEach(key -> written.merge(key, 1, Integer::sum)));
            if (!written.isEmpty()) {
                info.add("attributes written: " + written.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining(", ")));
            }
            List<ParsedFrameworkRow> nodes = byCode.values().stream()
                    .map(d -> new ParsedFrameworkRow(d.excelRow, d.level, d.parent, d.code, d.title,
                            d.definition, null, d.priority, d.displayOrder,
                            d.authored
                                    ? (d.recordKind != null ? d.recordKind : "CONTROL")
                                    : "SECTION",
                            null,
                            d.routing.inSoa(), d.routing.inGap(), d.routing.inAudit(),
                            d.routing.inRisk(), d.attributes))
                    .toList();
            return new PackFrameworkPlan(nodes, questions, staging, errors, info);
        }
    }

    static String sheetSectionKind(String sheetName) {
        if (sheetName == null) {
            return "CONTROL";
        }
        String s = sheetName.toLowerCase(Locale.ROOT);
        if (s.contains("27017") || s.contains("cloud")) {
            return "CLOUD";
        }
        if (s.contains("annex") || s.contains("anexure") || s.contains("annexure")) {
            return "ANNEX_CONTROL";
        }
        if (s.contains("clause")) {
            return "CLAUSE";
        }
        if (s.contains("article")) {
            return "ARTICLE";
        }
        return "CONTROL";
    }

    private static final class Draft {
        private String code;
        private int level;
        private String parent;
        private String title;
        private String definition;
        private String priority;
        private int displayOrder;
        private int excelRow;
        private boolean authored;
        private String recordKind;
        private Routing routing;
        private final Map<String, String> attributes = new LinkedHashMap<>();
    }

    record Routing(boolean inSoa, boolean inGap, boolean inAudit, boolean inRisk) {
    }

    /**
     * @param synthesisedCode true when the pack gave the section no code and one was made up from its
     *                        keyword and number, which is worth telling A9 about
     */
    private record Section(String code, String title, boolean synthesisedCode) {
    }

    /** Null when the line is prose under a section rather than a section header. */
    private static Section section(String text) {
        Matcher function = FUNCTION_BANNER.matcher(text);
        if (function.matches()) {
            return new Section(function.group(1).toUpperCase(Locale.ROOT), function.group(2).trim(), false);
        }
        Matcher sectionNumber = SECTION_NUMBER_BANNER.matcher(text);
        if (sectionNumber.matches()) {
            return new Section(sectionNumber.group(1), stripLeadingTitleSep(sectionNumber.group(2)), false);
        }
        Matcher part = PART_BANNER.matcher(text);
        if (part.matches()) {
            String keyword = part.group(1);
            String code = Character.toUpperCase(keyword.charAt(0))
                    + keyword.substring(1).toLowerCase(Locale.ROOT) + "." + part.group(2);
            return new Section(code, part.group(3).trim(), true);
        }
        return null;
    }

    private static void put(Map<String, String> attributes, String key, String value) {
        if (value != null && !value.isBlank()) {
            attributes.put(key, value);
        }
    }

    /** Strips the decorative bar the UAE IA category banners lead with, and collapses runs of spaces. */
    private static String clean(String text) {
        return text == null ? "" : text.replace('\u258c', ' ').replaceAll("\\s+", " ").trim();
    }

    /** {@code "6.1 - Actions…"} → {@code "Actions…"} (Doc 02 T-07). */
    private static String stripLeadingTitleSep(String title) {
        if (title == null) {
            return null;
        }
        return title.replaceFirst("^[\\u2014\\u2013\\-]+\\s*", "").trim();
    }

    /**
     * Banner title cleanup: strip leading dash separators and trailing
     * {@code — Objective: …} blurbs so Annex themes stay short (Excel theme name).
     */
    private static String bannerTitle(String raw) {
        String title = stripLeadingTitleSep(raw);
        if (title == null || title.isBlank()) {
            return title;
        }
        return OBJECTIVE_SUFFIX.matcher(title).replaceFirst("").trim();
    }

    /** True for a synthetic annex letter root such as {@code A} (not {@code A.5}). */
    private static boolean isLetterOnlyAnnexRoot(String code) {
        return code != null && code.matches("^[A-Za-z]$");
    }

    private static String truncate(String value, int max) {
        return value != null && value.length() > max ? value.substring(0, max) : value;
    }
}
