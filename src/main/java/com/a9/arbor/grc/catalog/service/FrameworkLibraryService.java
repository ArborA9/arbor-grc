package com.a9.arbor.grc.catalog.service;

import com.a9.arbor.grc.catalog.domain.Framework;
import com.a9.arbor.grc.catalog.domain.FrameworkImportBatch;
import com.a9.arbor.grc.catalog.domain.FrameworkImportRow;
import com.a9.arbor.grc.catalog.domain.FrameworkNode;
import com.a9.arbor.grc.catalog.domain.FrameworkNodeAttribute;
import com.a9.arbor.grc.catalog.domain.FrameworkQuestion;
import com.a9.arbor.grc.catalog.domain.FrameworkStatus;
import com.a9.arbor.grc.catalog.domain.FrameworkTypeCode;
import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.catalog.domain.Tenant;
import com.a9.arbor.grc.catalog.domain.TenantFramework;
import com.a9.arbor.grc.catalog.excel.ArborFrameworkExcelParser;
import com.a9.arbor.grc.catalog.excel.FrameworkSampleExcelWriter;
import com.a9.arbor.grc.catalog.excel.FrameworkSourceFormat;
import com.a9.arbor.grc.catalog.excel.FrameworkSourceFormatDetector;
import com.a9.arbor.grc.catalog.excel.GrcChecklistExcelParser;
import com.a9.arbor.grc.catalog.excel.ImportStagingRow;
import com.a9.arbor.grc.catalog.excel.MasterImportParser;
import com.a9.arbor.grc.catalog.excel.Nist80053PackParser;
import com.a9.arbor.grc.catalog.excel.PackFrameworkBuilder;
import com.a9.arbor.grc.catalog.excel.PackFrameworkPlan;
import com.a9.arbor.grc.catalog.excel.ParsedFrameworkRow;
import com.a9.arbor.grc.catalog.excel.ParsedMasterWorkbook;
import com.a9.arbor.grc.catalog.excel.ParsedReferencePack;
import com.a9.arbor.grc.catalog.excel.PciDssPackParser;
import com.a9.arbor.grc.catalog.excel.ReferenceMergePlan;
import com.a9.arbor.grc.catalog.excel.ReferencePackMerger;
import com.a9.arbor.grc.catalog.excel.ReferencePackParser;
import com.a9.arbor.grc.catalog.repo.FrameworkImportBatchRepository;
import com.a9.arbor.grc.catalog.repo.FrameworkImportRowRepository;
import com.a9.arbor.grc.catalog.repo.FrameworkNodeAttributeRepository;
import com.a9.arbor.grc.catalog.repo.FrameworkNodeRepository;
import com.a9.arbor.grc.catalog.repo.FrameworkQuestionRepository;
import com.a9.arbor.grc.catalog.repo.FrameworkRepository;
import com.a9.arbor.grc.catalog.repo.TenantFrameworkRepository;
import com.a9.arbor.grc.catalog.repo.TenantRepository;
import com.a9.arbor.grc.catalog.web.dto.FrameworkDetailResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkImportReportLine;
import com.a9.arbor.grc.catalog.web.dto.FrameworkImportResultResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkNodeResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkRoutingCounts;
import com.a9.arbor.grc.catalog.web.dto.FrameworkSummaryResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkTenantAssignmentResponse;
import com.a9.arbor.grc.catalog.web.dto.FrameworkTypeResponse;
import com.a9.arbor.grc.catalog.web.dto.UpdateFrameworkTenantsRequest;
import com.a9.arbor.grc.catalog.web.dto.UpdateModulesRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FrameworkLibraryService {

    private final FrameworkRepository frameworkRepository;
    private final FrameworkNodeRepository nodeRepository;
    private final FrameworkNodeAttributeRepository nodeAttributeRepository;
    private final FrameworkQuestionRepository questionRepository;
    private final FrameworkImportBatchRepository importBatchRepository;
    private final FrameworkImportRowRepository importRowRepository;
    private final TenantFrameworkRepository tenantFrameworkRepository;
    private final TenantRepository tenantRepository;
    private final ModuleLicensingService moduleLicensingService;
    private final com.a9.arbor.grc.modules.iam.service.AdminModuleAccessService adminModuleAccessService;
    private final ArborFrameworkExcelParser excelParser;
    private final GrcChecklistExcelParser checklistExcelParser;
    private final MasterImportParser masterImportParser;
    private final ReferencePackParser referencePackParser;
    private final ReferencePackMerger referencePackMerger;
    private final PackFrameworkBuilder packFrameworkBuilder;
    private final PciDssPackParser pciDssPackParser;
    private final Nist80053PackParser nist80053PackParser;
    private final FrameworkSourceFormatDetector sourceFormatDetector;
    private final FrameworkSampleExcelWriter sampleExcelWriter;
    private final FrameworkPackCatalog frameworkPackCatalog;

    public FrameworkLibraryService(FrameworkRepository frameworkRepository,
                                   FrameworkNodeRepository nodeRepository,
                                   FrameworkNodeAttributeRepository nodeAttributeRepository,
                                   FrameworkQuestionRepository questionRepository,
                                   FrameworkImportBatchRepository importBatchRepository,
                                   FrameworkImportRowRepository importRowRepository,
                                   TenantFrameworkRepository tenantFrameworkRepository,
                                   TenantRepository tenantRepository,
                                   ModuleLicensingService moduleLicensingService,
                                   com.a9.arbor.grc.modules.iam.service.AdminModuleAccessService adminModuleAccessService,
                                   ArborFrameworkExcelParser excelParser,
                                   GrcChecklistExcelParser checklistExcelParser,
                                   MasterImportParser masterImportParser,
                                   ReferencePackParser referencePackParser,
                                   ReferencePackMerger referencePackMerger,
                                   PackFrameworkBuilder packFrameworkBuilder,
                                   PciDssPackParser pciDssPackParser,
                                   Nist80053PackParser nist80053PackParser,
                                   FrameworkSourceFormatDetector sourceFormatDetector,
                                   FrameworkSampleExcelWriter sampleExcelWriter,
                                   FrameworkPackCatalog frameworkPackCatalog) {
        this.frameworkRepository = frameworkRepository;
        this.nodeRepository = nodeRepository;
        this.nodeAttributeRepository = nodeAttributeRepository;
        this.questionRepository = questionRepository;
        this.importBatchRepository = importBatchRepository;
        this.importRowRepository = importRowRepository;
        this.tenantFrameworkRepository = tenantFrameworkRepository;
        this.tenantRepository = tenantRepository;
        this.moduleLicensingService = moduleLicensingService;
        this.adminModuleAccessService = adminModuleAccessService;
        this.excelParser = excelParser;
        this.checklistExcelParser = checklistExcelParser;
        this.masterImportParser = masterImportParser;
        this.referencePackParser = referencePackParser;
        this.referencePackMerger = referencePackMerger;
        this.packFrameworkBuilder = packFrameworkBuilder;
        this.pciDssPackParser = pciDssPackParser;
        this.nist80053PackParser = nist80053PackParser;
        this.sourceFormatDetector = sourceFormatDetector;
        this.sampleExcelWriter = sampleExcelWriter;
        this.frameworkPackCatalog = frameworkPackCatalog;
    }

    /**
     * Catalog types shown under Modules: only frameworks present as importable packs.
     * PCI DSS and other enum-only stubs are omitted.
     */
    public List<FrameworkTypeResponse> listTypes() {
        Set<FrameworkTypeCode> packTypes = discoverPackBackedTypes();
        List<FrameworkTypeResponse> out = new ArrayList<>();
        for (FrameworkTypeCode type : FrameworkTypeCode.values()) {
            if (packTypes.contains(type)) {
                out.add(FrameworkTypeResponse.from(type));
            }
        }
        return out;
    }

    /** Types discoverable from importable files under {@code arbor.frameworks.dir}. */
    Set<FrameworkTypeCode> discoverPackBackedTypes() {
        Set<FrameworkTypeCode> types = EnumSet.noneOf(FrameworkTypeCode.class);
        for (FrameworkPackCatalog.PackFile pack : frameworkPackCatalog.listAllPacks()) {
            if (!pack.importable()) {
                continue;
            }
            for (FrameworkTypeCode type : typesFromPackFilename(pack.filename())) {
                types.add(type);
            }
        }
        return types;
    }

    public byte[] sampleExcel(String typeCode) {
        return sampleExcelWriter.writeSample(parseType(typeCode));
    }

    public String sampleFilename(String typeCode) {
        FrameworkTypeCode type = parseType(typeCode);
        return "arbor_" + type.name().toLowerCase(Locale.ROOT) + "_controls_sample.xlsx";
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public List<FrameworkSummaryResponse> listAll() {
        return frameworkRepository.findAllByOrderByCodeAscVersionDesc().stream()
                .map(this::toLibrarySummary)
                .toList();
    }

    private FrameworkSummaryResponse toLibrarySummary(Framework framework) {
        int[] split = librarySplitCounts(framework.getId());
        // Doc 02 T-11: expose clause vs annex/control split for the SA library table.
        return FrameworkSummaryResponse.from(
                framework, null, null, null, null, hasCloudNodes(framework.getId()),
                split[0], split[1]);
    }

    /** CLAUSE nodes vs ANNEX_CONTROL + CLOUD + CONTROL (annexures column). */
    private int[] librarySplitCounts(Long frameworkId) {
        long clause = nodeRepository.countByFrameworkIdAndRecordType(frameworkId, "CLAUSE");
        long annex = nodeRepository.countByFrameworkIdAndRecordType(frameworkId, "ANNEX_CONTROL")
                + nodeRepository.countByFrameworkIdAndRecordType(frameworkId, "CLOUD");
        long control = nodeRepository.countByFrameworkIdAndRecordType(frameworkId, "CONTROL");
        return new int[] {(int) clause, (int) (annex + control)};
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public FrameworkDetailResponse getDetail(Long id) {
        Framework framework = requireFramework(id);
        return new FrameworkDetailResponse(
                FrameworkSummaryResponse.from(framework, null, null, null, null, hasCloudNodes(id)),
                toNodeResponses(id));
    }

    /**
     * Import frameworks from Excel. The layout is detected from sheet shape, not the filename.
     * <ul>
     *   <li>Master Import workbook + no {@code typeCode} → import every framework sheet;
     *       {@code version} comes from each sheet's {@code framework_version}</li>
     *   <li>Master Import + {@code typeCode} → import that framework only</li>
     *   <li>GRC Checklist workbook + no {@code typeCode} → import all four sheets</li>
     *   <li>GRC Checklist + {@code typeCode} → import that sheet only</li>
     *   <li>Arbor canonical {@code Controls} sheet → requires {@code typeCode} + {@code version}</li>
     * </ul>
     */
    @Transactional(transactionManager = "catalogTransactionManager")
    public FrameworkImportResultResponse importExcel(String typeCode, String version,
                                                     MultipartFile file, boolean dryRun) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read upload: " + e.getMessage());
        }
        return importExcel(typeCode, version, bytes, file.getOriginalFilename(), dryRun);
    }

    /**
     * Bytes + filename entry used by folder-based Import V2 and the multipart upload path.
     * Re-import of an existing code+version is refused: Super Admin must delete first (V2).
     */
    @Transactional(transactionManager = "catalogTransactionManager")
    public FrameworkImportResultResponse importExcel(String typeCode, String version,
                                                     byte[] bytes, String filename, boolean dryRun) {
        if (bytes == null || bytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
        }
        return switch (sourceFormatDetector.detect(bytes)) {
            case MASTER_IMPORT -> importMaster(bytes, filename, typeCode, version, dryRun);
            case REFERENCE_PACK -> importReferencePack(bytes, filename, typeCode, version, dryRun);
            case PCI_DSS_PACK -> importPciDssPack(bytes, filename, typeCode, version, dryRun);
            case NIST_800_53_PACK -> importNist80053Pack(bytes, filename, typeCode, version, dryRun);
            case GRC_CHECKLIST -> importChecklist(bytes, filename, typeCode, version, dryRun);
            case ARBOR_CONTROLS -> importArborControls(bytes, filename, typeCode, version, dryRun);
        };
    }

    private FrameworkImportResultResponse importChecklist(byte[] bytes, String filename,
                                                          String typeCode, String version,
                                                          boolean dryRun) {
        List<FrameworkDetailResponse> imported = new ArrayList<>();
        List<FrameworkImportReportLine> report = new ArrayList<>();
        if (typeCode == null || typeCode.isBlank()) {
            Map<FrameworkTypeCode, List<ParsedFrameworkRow>> all = checklistExcelParser.parseAll(bytes);
            for (FrameworkTypeCode type : GrcChecklistExcelParser.supportedTypes()) {
                List<ParsedFrameworkRow> rows = all.get(type);
                if (dryRun) {
                    report.add(dryRunFromRows(type.displayName(), type, type.defaultVersion(), rows,
                            List.of(), List.of(), null));
                } else {
                    imported.add(getDetail(persistNodes(
                            type, type.defaultVersion(), filename, rows).frameworkId()));
                }
            }
        } else {
            FrameworkTypeCode type = parseType(typeCode);
            if (!GrcChecklistExcelParser.supportsType(type)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Type " + type.name() + " is not in the GRC Checklist workbook: "
                                + "use the Arbor Controls template, or omit type to import all four");
            }
            String ver = resolveVersion(version, type);
            List<ParsedFrameworkRow> rows = checklistExcelParser.parse(bytes, type);
            if (dryRun) {
                report.add(dryRunFromRows(type.displayName(), type, ver, rows,
                        List.of(), List.of(), null));
            } else {
                imported.add(getDetail(persistNodes(type, ver, filename, rows).frameworkId()));
            }
        }
        if (dryRun) {
            return new FrameworkImportResultResponse(List.of(), List.copyOf(report));
        }
        return new FrameworkImportResultResponse(imported);
    }

    private FrameworkImportResultResponse importArborControls(byte[] bytes, String filename,
                                                              String typeCode, String version,
                                                              boolean dryRun) {
        if (typeCode == null || typeCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "typeCode is required for Arbor Controls template imports "
                            + "(omit type only for GRC Checklist or Master Import multi-import)");
        }
        if (version == null || version.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "version is required");
        }
        FrameworkTypeCode type = parseType(typeCode);
        List<ParsedFrameworkRow> rows = excelParser.parse(new java.io.ByteArrayInputStream(bytes));
        if (dryRun) {
            FrameworkImportReportLine line = dryRunFromRows("Controls", type, version.trim(), rows,
                    List.of(), List.of(), null);
            return new FrameworkImportResultResponse(List.of(), List.of(line));
        }
        return new FrameworkImportResultResponse(List.of(
                getDetail(persistNodes(type, version.trim(), filename, rows).frameworkId())));
    }

    /**
     * Master Import (layout S1). Unlike the legacy templates this reports per row instead of failing
     * the upload: a bad row is excluded and named in the report, so one malformed control cannot cost
     * the other 211.
     */
    private FrameworkImportResultResponse importMaster(byte[] bytes, String filename,
                                                       String typeCode, String version,
                                                       boolean dryRun) {
        ParsedMasterWorkbook parsed = masterImportParser.parse(bytes);
        FrameworkTypeCode requested = typeCode == null || typeCode.isBlank() ? null : parseType(typeCode);

        List<ParsedMasterWorkbook.FrameworkSheet> targets = parsed.frameworks().stream()
                .filter(sheet -> requested == null || sheet.type() == requested)
                .toList();
        if (targets.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, requested == null
                    ? "Master Import workbook has no importable framework sheet"
                    : "Master Import workbook has no framework sheet for " + requested.name());
        }
        if (version != null && !version.isBlank()) {
            for (ParsedMasterWorkbook.FrameworkSheet sheet : targets) {
                if (!version.trim().equals(sheet.version())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "version comes from the sheet's framework_version for Master Import files: "
                                    + sheet.sourceFrameworkCode() + " is " + sheet.version()
                                    + ", request asked for " + version.trim());
                }
            }
        }

        // Checked up front, across every targeted sheet, so a seven-framework upload can never
        // half-apply: re-importing a PUBLISHED code+version stays a 409.
        List<String> publishedBlocks = new ArrayList<>();
        for (ParsedMasterWorkbook.FrameworkSheet sheet : targets) {
            frameworkRepository.findByCodeAndVersion(sheet.type().name(), sheet.version())
                    .filter(existing -> existing.getStatus() == FrameworkStatus.PUBLISHED)
                    .ifPresent(existing -> publishedBlocks.add(
                            existing.getCode() + " " + existing.getVersion()));
        }
        if (!publishedBlocks.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Published framework(s) " + String.join(", ", publishedBlocks)
                            + " are immutable: bump framework_version in the sheet to import a new "
                            + "version. Node codes are the identity SoA and Gap items hang off.");
        }

        List<FrameworkDetailResponse> imported = new ArrayList<>();
        List<FrameworkImportReportLine> report = new ArrayList<>();

        for (ParsedMasterWorkbook.FrameworkSheet sheet : targets) {
            if (dryRun) {
                // Dry-run parses fully but skips batch staging: REQUIRES_NEW deferred to a later
                // increment so failed uploads still leave no half-written catalog rows.
                report.add(new FrameworkImportReportLine(
                        sheet.sheetName(),
                        sheet.sourceFrameworkCode(),
                        sheet.type().name(),
                        sheet.version(),
                        dryRunStatus(sheet.errors().size()),
                        null,
                        null,
                        sheet.contentRows(),
                        sheet.rows().size(),
                        (int) sheet.leafCount(),
                        sheet.questions().size(),
                        sheet.errors().size(),
                        routingCounts(sheet.rows()),
                        null,
                        sheet.errors(),
                        sheet.info(),
                        null));
                continue;
            }

            FrameworkImportBatch batch = startBatch(FrameworkSourceFormat.MASTER_IMPORT, filename,
                    sheet.sourceFrameworkCode(), sheet.version(), sheet.staging().size(), false);
            PersistedNodes persisted = persistNodes(
                    sheet.type(), sheet.version(), filename, sheet.rows());
            int questions = persistQuestions(persisted, sheet.questions());

            batch.setFrameworkId(persisted.frameworkId());
            batch.setStatus(sheet.errors().isEmpty() ? "IMPORTED" : "IMPORTED_WITH_ERRORS");
            batch.setErrorCount(sheet.errors().size());
            batch.setReport(String.join("\n", sheet.info()));
            batch.setFinishedAt(Instant.now());
            importBatchRepository.save(batch);
            writeStaging(batch.getId(), sheet.staging());

            imported.add(getDetail(persisted.frameworkId()));
            report.add(new FrameworkImportReportLine(
                    sheet.sheetName(),
                    sheet.sourceFrameworkCode(),
                    sheet.type().name(),
                    sheet.version(),
                    batch.getStatus(),
                    batch.getId(),
                    persisted.frameworkId(),
                    sheet.contentRows(),
                    sheet.rows().size(),
                    (int) sheet.leafCount(),
                    questions,
                    sheet.errors().size(),
                    routingCounts(sheet.rows()),
                    null,
                    sheet.errors(),
                    sheet.info(),
                    null));
        }

        if (requested == null) {
            for (ParsedMasterWorkbook.SkippedSheet skipped : parsed.skipped()) {
                if (dryRun) {
                    report.add(FrameworkImportReportLine.skipped(
                            skipped.sheetName(), skipped.sourceFrameworkCode(), null,
                            skipped.staging().size(), skipped.reason()));
                    continue;
                }
                FrameworkImportBatch batch = startBatch(FrameworkSourceFormat.MASTER_IMPORT,
                        filename, skipped.sourceFrameworkCode(), null, skipped.staging().size(), false);
                batch.setStatus("SKIPPED");
                batch.setReport(skipped.reason());
                batch.setFinishedAt(Instant.now());
                importBatchRepository.save(batch);
                writeStaging(batch.getId(), skipped.staging());
                report.add(FrameworkImportReportLine.skipped(
                        skipped.sheetName(), skipped.sourceFrameworkCode(), batch.getId(),
                        skipped.staging().size(), skipped.reason()));
            }
        }

        if (dryRun) {
            return new FrameworkImportResultResponse(List.of(), List.copyOf(report));
        }
        return new FrameworkImportResultResponse(List.copyOf(imported), List.copyOf(report));
    }

    /**
     * Reference pack (layout S4), which does one of two things per sheet.
     *
     * <p><b>Enrich</b>, where the Master import already created the framework: it adds the authored
     * question bank plus {@code what_it_means} / {@code evidence}, and touches no node. Per-control
     * rule: where the pack names a control, its questions replace that control's Master questions;
     * where it is silent, the Master questions stay active. Both sets coexist at different
     * {@code version_number}s, which is safe because every consumer reads {@code active = true}
     * rather than a pinned version.
     *
     * <p><b>Create</b> (F3b), for the frameworks that have no Master sheet at all and so no hierarchy
     * to attach to. Restricted to {@link PackFrameworkBuilder#supports}, so the ISO frameworks keep
     * the Master as their authority rather than silently falling back to an inferred hierarchy.
     */
    private FrameworkImportResultResponse importReferencePack(byte[] bytes, String filename,
                                                             String typeCode, String version,
                                                             boolean dryRun) {
        FrameworkTypeCode filenameDefault = typeHintFromFilename(filename);
        ParsedReferencePack parsed = referencePackParser.parse(bytes, filenameDefault);
        FrameworkTypeCode requested = typeCode == null || typeCode.isBlank() ? null : parseType(typeCode);

        Map<FrameworkTypeCode, List<ParsedReferencePack.Sheet>> byType = new LinkedHashMap<>();
        for (ParsedReferencePack.Sheet sheet : parsed.sheets()) {
            if (requested == null || sheet.type() == requested) {
                byType.computeIfAbsent(sheet.type(), k -> new ArrayList<>()).add(sheet);
            }
        }
        if (byType.isEmpty()) {
            if (requested != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Reference pack has no sheet for " + requested.name());
            }
            String detail = parsed.skipped().stream()
                    .map(s -> "'" + s.sheetName() + "' " + s.reason())
                    .collect(Collectors.joining("; "));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No sheet in this reference pack can be imported. "
                            + (detail.isBlank() ? "No sheet carried a clause/control ref column." : detail));
        }

        List<PackTarget> targets = new ArrayList<>();
        List<FrameworkImportReportLine> report = new ArrayList<>();
        List<String> existsBlocks = new ArrayList<>();
        for (FrameworkTypeCode type : byType.keySet()) {
            String ver = resolveVersion(version, type, filename);
            Framework framework = frameworkRepository.findByCodeAndVersion(type.name(), ver).orElse(null);
            if (framework != null) {
                // V2: re-import only after Super Admin deletes the existing framework first.
                existsBlocks.add(framework.getCode() + " " + framework.getVersion()
                        + " (" + framework.getStatus() + ")");
                continue;
            }
            if (!packFrameworkBuilder.supports(type)) {
                report.add(FrameworkImportReportLine.skipped(
                        byType.get(type).stream().map(ParsedReferencePack.Sheet::sheetName)
                                .collect(Collectors.joining(", ")),
                        type.name(), null,
                        byType.get(type).stream().mapToInt(ParsedReferencePack.Sheet::contentRows).sum(),
                        "skipped, " + type.name() + " cannot be created from this pack layout"));
                continue;
            }
            targets.add(new PackTarget(type, ver, null));
        }
        if (!existsBlocks.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Framework already exists: delete it first before re-importing: "
                            + String.join(", ", existsBlocks));
        }
        if (targets.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Reference pack matched no importable framework: "
                            + report.stream().map(FrameworkImportReportLine::message)
                            .collect(Collectors.joining("; ")));
        }

        List<FrameworkDetailResponse> imported = new ArrayList<>();
        for (PackTarget target : targets) {
            List<ParsedReferencePack.Sheet> sheets = byType.get(target.type());
            List<ParsedReferencePack.DomainObjective> objectives =
                    target.type() == FrameworkTypeCode.UAE_IA
                            ? parsed.domainObjectives()
                            : List.of();
            FrameworkImportReportLine line =
                    createFromPack(target.type(), target.version(), sheets, filename, dryRun, objectives);
            report.add(line);
            if (!dryRun && line.frameworkId() != null) {
                imported.add(getDetail(line.frameworkId()));
            }
        }

        if (requested == null) {
            for (ParsedReferencePack.SkippedSheet skipped : parsed.skipped()) {
                report.add(FrameworkImportReportLine.skipped(
                        skipped.sheetName(), null, null, 0, skipped.reason()));
            }
        }
        if (dryRun) {
            return new FrameworkImportResultResponse(List.of(), List.copyOf(report));
        }
        return new FrameworkImportResultResponse(List.copyOf(imported), List.copyOf(report));
    }

    /** Best-effort type from V2 pack filenames when sheet names are generic (e.g. "Clause"). */
    static FrameworkTypeCode typeHintFromFilename(String filename) {
        List<FrameworkTypeCode> types = typesFromPackFilename(filename);
        return types.isEmpty() ? null : types.get(0);
    }

    /**
     * All framework types a pack filename implies. Dual packs (e.g. NIST CSF + UAE PDPL) return both.
     * Used for Modules catalog filtering (Doc 02 T-04).
     */
    static List<FrameworkTypeCode> typesFromPackFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return List.of();
        }
        String n = filename.toLowerCase(Locale.ROOT).replace(" ", "");
        List<FrameworkTypeCode> out = new ArrayList<>();
        if (n.contains("27001")) {
            out.add(FrameworkTypeCode.ISO_27001);
        }
        if (n.contains("22301")) {
            out.add(FrameworkTypeCode.ISO_22301);
        }
        if (n.contains("42001")) {
            out.add(FrameworkTypeCode.ISO_42001);
        }
        if (n.contains("27701")) {
            out.add(FrameworkTypeCode.ISO_27701);
        }
        if (n.contains("20000")) {
            out.add(FrameworkTypeCode.ISO_20000);
        }
        if (n.contains("9001")) {
            out.add(FrameworkTypeCode.ISO_9001);
        }
        if (n.contains("45001")) {
            out.add(FrameworkTypeCode.ISO_45001);
        }
        if (n.contains("31000") || n.contains("iso31000")) {
            out.add(FrameworkTypeCode.ISO_31000);
        }
        if (n.contains("uae_ia") || n.contains("uaeia") || n.contains("grc_uae_ia")) {
            out.add(FrameworkTypeCode.UAE_IA);
        }
        if (n.contains("ncema")) {
            out.add(FrameworkTypeCode.NCEMA_V3);
        }
        if (n.contains("sama")) {
            out.add(FrameworkTypeCode.SAMA_CSF);
        }
        if (n.contains("cis")) {
            out.add(FrameworkTypeCode.CIS_V8);
        }
        if (n.contains("gdpr")) {
            out.add(FrameworkTypeCode.GDPR);
        }
        if (n.contains("800-53") || n.contains("800_53") || n.contains("nist-sp-800")) {
            out.add(FrameworkTypeCode.NIST_800_53);
        }
        // Dual Privacy workbook (NIST_CSF_UAE_PDPL.xlsx): both types live in one file.
        if (n.contains("pdpl")) {
            out.add(FrameworkTypeCode.UAE_PDPL);
            if (n.contains("nist") || n.contains("csf")) {
                out.add(FrameworkTypeCode.NIST_CSF);
            }
        } else if (n.contains("nist") && n.contains("csf")) {
            out.add(FrameworkTypeCode.NIST_CSF);
        }
        return List.copyOf(out.stream().distinct().toList());
    }

    /** Prefer year/version embedded in the pack filename (e.g. ISO27701-2025 → 2025). */
    static String versionHintFromFilename(String filename, FrameworkTypeCode type) {
        if (filename == null || filename.isBlank() || type == null) {
            return null;
        }
        String n = filename;
        if (type == FrameworkTypeCode.ISO_27701) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("27701[-_\\s]?(\\d{4})", java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(n);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }

    /** @param existing null when the pack is to create the framework rather than enrich it */
    private record PackTarget(FrameworkTypeCode type, String version, Framework existing) {
    }

    /**
     * Creates a framework the Master workbook never carried, from the pack alone.
     * Hierarchy and routing come from
     * {@link PackFrameworkBuilder}; everything after that is the Master import's persistence path, so
     * a pack-created framework is indistinguishable downstream from an imported one.
     */
    private FrameworkImportReportLine createFromPack(FrameworkTypeCode type,
                                                     String ver,
                                                     List<ParsedReferencePack.Sheet> sheets,
                                                     String filename,
                                                     boolean dryRun) {
        return createFromPack(type, ver, sheets, filename, dryRun, List.of());
    }

    private FrameworkImportReportLine createFromPack(FrameworkTypeCode type,
                                                     String ver,
                                                     List<ParsedReferencePack.Sheet> sheets,
                                                     String filename,
                                                     boolean dryRun,
                                                     List<ParsedReferencePack.DomainObjective> domainObjectives) {
        PackFrameworkPlan plan = packFrameworkBuilder.build(type, sheets, domainObjectives);
        String sheetNames = sheets.stream().map(ParsedReferencePack.Sheet::sheetName)
                .collect(Collectors.joining(", "));
        int contentRows = sheets.stream().mapToInt(ParsedReferencePack.Sheet::contentRows).sum();
        String createMessage = "created from reference pack: nodes " + plan.nodesByLevel()
                + " by level, no Master sheet exists for this framework";
        return persistCreatedFramework(type, ver, filename, plan, FrameworkSourceFormat.REFERENCE_PACK,
                sheetNames, contentRows, createMessage, dryRun);
    }

    private FrameworkImportResultResponse importPciDssPack(byte[] bytes, String filename,
                                                           String typeCode, String version,
                                                           boolean dryRun) {
        requirePackType(typeCode, FrameworkTypeCode.PCI_DSS);
        FrameworkTypeCode type = FrameworkTypeCode.PCI_DSS;
        String ver = resolveVersion(version, type);
        rejectIfPublished(type, ver);

        PackFrameworkPlan plan = pciDssPackParser.parse(bytes);
        FrameworkImportReportLine line = persistCreatedFramework(
                type, ver, filename, plan, FrameworkSourceFormat.PCI_DSS_PACK,
                "All 12 Requirements - Master", plan.routedNodes(),
                "created from PCI DSS pack: nodes " + plan.nodesByLevel() + " by level",
                dryRun);
        if (dryRun) {
            return new FrameworkImportResultResponse(List.of(), List.of(line));
        }
        return new FrameworkImportResultResponse(
                List.of(getDetail(line.frameworkId())), List.of(line));
    }

    private FrameworkImportResultResponse importNist80053Pack(byte[] bytes, String filename,
                                                              String typeCode, String version,
                                                              boolean dryRun) {
        requirePackType(typeCode, FrameworkTypeCode.NIST_800_53);
        FrameworkTypeCode type = FrameworkTypeCode.NIST_800_53;
        String ver = resolveVersion(version, type);
        rejectIfPublished(type, ver);

        PackFrameworkPlan plan = nist80053PackParser.parse(bytes);
        FrameworkImportReportLine line = persistCreatedFramework(
                type, ver, filename, plan, FrameworkSourceFormat.NIST_800_53_PACK,
                "NIST SP 800-53 family tabs", plan.routedNodes(),
                "created from NIST SP 800-53 pack: nodes " + plan.nodesByLevel() + " by level",
                dryRun);
        if (dryRun) {
            return new FrameworkImportResultResponse(List.of(), List.of(line));
        }
        return new FrameworkImportResultResponse(
                List.of(getDetail(line.frameworkId())), List.of(line));
    }

    private FrameworkImportReportLine persistCreatedFramework(FrameworkTypeCode type,
                                                              String ver,
                                                              String filename,
                                                              PackFrameworkPlan plan,
                                                              FrameworkSourceFormat format,
                                                              String sheetNames,
                                                              int contentRows,
                                                              String createMessage,
                                                              boolean dryRun) {
        if (plan.routedNodes() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pack produced no module-routed node for " + type.name() + ": nothing for SoA, "
                            + "Gap, Risk or Audit to seed from. Check the ref column parsed.");
        }

        if (dryRun) {
            return new FrameworkImportReportLine(
                    sheetNames,
                    null,
                    type.name(),
                    ver,
                    dryRunStatus(plan.errors().size()),
                    null,
                    null,
                    contentRows,
                    plan.nodes().size(),
                    plan.routedNodes(),
                    plan.questions().size(),
                    plan.errors().size(),
                    routingCounts(plan.nodes()),
                    null,
                    plan.errors(),
                    plan.info(),
                    createMessage);
        }

        FrameworkImportBatch batch = startBatch(format, filename, type.name(), ver,
                plan.staging().size(), false);
        PersistedNodes persisted = persistNodes(type, ver, filename, plan.nodes());
        int questions = persistQuestions(persisted, plan.questions());

        batch.setFrameworkId(persisted.frameworkId());
        batch.setStatus(plan.errors().isEmpty() ? "IMPORTED" : "IMPORTED_WITH_ERRORS");
        batch.setErrorCount(plan.errors().size());
        batch.setReport(String.join("\n", plan.info()));
        batch.setFinishedAt(Instant.now());
        importBatchRepository.save(batch);
        writeStaging(batch.getId(), plan.staging());

        return new FrameworkImportReportLine(
                sheetNames,
                null,
                type.name(),
                ver,
                batch.getStatus(),
                batch.getId(),
                persisted.frameworkId(),
                contentRows,
                plan.nodes().size(),
                plan.routedNodes(),
                questions,
                plan.errors().size(),
                routingCounts(plan.nodes()),
                null,
                plan.errors(),
                plan.info(),
                createMessage);
    }

    private static void requirePackType(String typeCode, FrameworkTypeCode expected) {
        if (typeCode != null && !typeCode.isBlank() && parseType(typeCode) != expected) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Workbook is a " + expected.name() + " pack: omit typeCode or pass "
                            + expected.name());
        }
    }

    private void rejectIfPublished(FrameworkTypeCode type, String ver) {
        frameworkRepository.findByCodeAndVersion(type.name(), ver)
                .filter(existing -> existing.getStatus() == FrameworkStatus.PUBLISHED)
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Published framework " + existing.getCode() + " " + existing.getVersion()
                                    + " is immutable: publish a new version before re-importing.");
                });
    }

    private FrameworkImportReportLine enrich(Framework framework,
                                             List<ParsedReferencePack.Sheet> sheets,
                                             String filename,
                                             boolean dryRun) {
        Map<String, Long> idByCode = new LinkedHashMap<>();
        for (FrameworkNode node : nodeRepository
                .findByFrameworkIdOrderByLevelAscDisplayOrderAscNodeCodeAsc(framework.getId())) {
            idByCode.put(node.getNodeCode(), node.getId());
        }
        ReferenceMergePlan plan = referencePackMerger.merge(sheets, idByCode.keySet());

        String sheetNames = sheets.stream().map(ParsedReferencePack.Sheet::sheetName)
                .collect(Collectors.joining(", "));
        int contentRows = sheets.stream().mapToInt(ParsedReferencePack.Sheet::contentRows).sum();

        int replacedQuestions = 0;
        for (String code : plan.replacedCodes()) {
            replacedQuestions += questionRepository
                    .findByFrameworkIdAndFrameworkNodeIdAndActiveTrueOrderByDisplayOrderAsc(
                            framework.getId(), idByCode.get(code)).size();
        }
        int keptQuestions =
                (int) questionRepository.countByFrameworkIdAndActiveTrue(framework.getId()) - replacedQuestions;

        FrameworkImportReportLine.MergeCounts mergeCounts = new FrameworkImportReportLine.MergeCounts(
                plan.counts().refRows(),
                plan.counts().exactMatches(),
                plan.counts().ancestorAttachments(),
                plan.counts().unmatchedRefs(),
                replacedQuestions,
                keptQuestions,
                plan.attributes().size());

        if (dryRun) {
            return new FrameworkImportReportLine(
                    sheetNames,
                    null,
                    framework.getCode(),
                    framework.getVersion(),
                    dryRunStatus(plan.errors().size()),
                    null,
                    null,
                    contentRows,
                    0,
                    0,
                    plan.questions().size(),
                    plan.errors().size(),
                    null,
                    mergeCounts,
                    plan.errors(),
                    plan.info(),
                    null);
        }

        FrameworkImportBatch batch = startBatch(FrameworkSourceFormat.REFERENCE_PACK, filename,
                framework.getCode(), framework.getVersion(), plan.staging().size(), false);

        for (String code : plan.replacedCodes()) {
            for (FrameworkQuestion existing : questionRepository
                    .findByFrameworkIdAndFrameworkNodeIdAndActiveTrueOrderByDisplayOrderAsc(
                            framework.getId(), idByCode.get(code))) {
                existing.setActive(false);
                questionRepository.save(existing);
            }
        }
        // Questions attached to an ancestor sit alongside whatever the Master put there, so they are
        // numbered after it: question_number stays a readable per-control ordinal.
        Map<String, Integer> offsetByCode = new HashMap<>();
        for (String code : plan.attachedCodes()) {
            offsetByCode.put(code, questionRepository
                    .findByFrameworkIdAndFrameworkNodeIdAndActiveTrueOrderByDisplayOrderAsc(
                            framework.getId(), idByCode.get(code)).size());
        }

        int nextVersion = questionRepository.maxVersion(framework.getId()) + 1;
        int order = 0;
        for (ReferenceMergePlan.QuestionWrite source : plan.questions()) {
            int number = source.ordinal() + offsetByCode.getOrDefault(source.nodeCode(), 0);
            FrameworkQuestion question = new FrameworkQuestion();
            question.setFrameworkId(framework.getId());
            question.setFrameworkNodeId(idByCode.get(source.nodeCode()));
            question.setControlCode(source.nodeCode());
            question.setQuestionNumber("Q" + number);
            question.setQuestionCode(source.nodeCode() + "-Q" + number);
            question.setSourceRef(source.sourceRef());
            question.setQuestionText(source.questionText());
            // The packs carry no question_type, answer_options or is_mandatory column. Left unset
            // rather than defaulted: inventing a mandatory flag would gate contributors on data the
            // client never authored.
            question.setDisplayOrder(++order);
            question.setVersionNumber(nextVersion);
            question.setActive(true);
            questionRepository.save(question);
        }

        for (ReferenceMergePlan.AttributeWrite source : plan.attributes()) {
            Long nodeId = idByCode.get(source.nodeCode());
            FrameworkNodeAttribute attribute = nodeAttributeRepository
                    .findByNodeIdAndAttrKey(nodeId, source.attrKey())
                    .orElseGet(FrameworkNodeAttribute::new);
            attribute.setNodeId(nodeId);
            attribute.setAttrKey(source.attrKey());
            attribute.setAttrValue(source.attrValue());
            nodeAttributeRepository.save(attribute);
        }

        // The question bank changed, so an earlier sign-off no longer covers this framework.
        if (framework.getStatus() == FrameworkStatus.VALIDATED) {
            framework.setStatus(FrameworkStatus.DRAFT);
            framework.setValidatedBy(null);
            framework.setValidatedAt(null);
            frameworkRepository.save(framework);
        }

        batch.setFrameworkId(framework.getId());
        batch.setStatus(plan.errors().isEmpty() ? "ENRICHED" : "ENRICHED_WITH_ERRORS");
        batch.setErrorCount(plan.errors().size());
        batch.setReport(String.join("\n", plan.info()));
        batch.setFinishedAt(Instant.now());
        importBatchRepository.save(batch);
        writeStaging(batch.getId(), plan.staging());

        return new FrameworkImportReportLine(
                sheetNames,
                null,
                framework.getCode(),
                framework.getVersion(),
                batch.getStatus(),
                batch.getId(),
                framework.getId(),
                contentRows,
                0,
                0,
                plan.questions().size(),
                plan.errors().size(),
                null,
                mergeCounts,
                plan.errors(),
                plan.info(),
                null);
    }

    private static FrameworkImportReportLine.RoutingCounts routingCounts(List<ParsedFrameworkRow> rows) {
        int soa = 0;
        int gap = 0;
        int audit = 0;
        int risk = 0;
        for (ParsedFrameworkRow row : rows) {
            soa += row.inSoa() ? 1 : 0;
            gap += row.inGapAssessment() ? 1 : 0;
            audit += row.inAuditChecklist() ? 1 : 0;
            risk += row.inRiskRegister() ? 1 : 0;
        }
        return new FrameworkImportReportLine.RoutingCounts(soa, gap, audit, risk);
    }

    private static String dryRunStatus(int errorCount) {
        return errorCount > 0 ? "DRY_RUN_WITH_ERRORS" : "DRY_RUN";
    }

    private FrameworkImportReportLine dryRunFromRows(String sheetName,
                                                     FrameworkTypeCode type,
                                                     String version,
                                                     List<ParsedFrameworkRow> rows,
                                                     List<com.a9.arbor.grc.catalog.excel.ImportRowError> errors,
                                                     List<String> info,
                                                     String message) {
        int leafNodes = (int) rows.stream().filter(ParsedFrameworkRow::routedToAnyModule).count();
        return new FrameworkImportReportLine(
                sheetName,
                null,
                type.name(),
                version,
                dryRunStatus(errors.size()),
                null,
                null,
                rows.size(),
                rows.size(),
                leafNodes,
                0,
                errors.size(),
                routingCounts(rows),
                null,
                errors,
                info,
                message);
    }

    private FrameworkImportBatch startBatch(FrameworkSourceFormat format, String filename,
                                            String sourceFrameworkCode, String version, int rowCount,
                                            boolean dryRun) {
        FrameworkImportBatch batch = new FrameworkImportBatch();
        batch.setSourceFilename(filename == null ? "(unnamed upload)" : filename);
        batch.setSourceFormat(format.name());
        batch.setStatus("RUNNING");
        batch.setDryRun(dryRun);
        batch.setFrameworkCode(sourceFrameworkCode);
        batch.setFrameworkVersion(version);
        batch.setRowCount(rowCount);
        batch.setErrorCount(0);
        batch.setStartedBy(currentEmail());
        batch.setStartedAt(Instant.now());
        return importBatchRepository.save(batch);
    }

    private void writeStaging(Long batchId, List<ImportStagingRow> rows) {
        for (ImportStagingRow source : rows) {
            FrameworkImportRow row = new FrameworkImportRow();
            row.setBatchId(batchId);
            row.setSheetName(source.sheetName());
            row.setRowIndex(source.rowIndex());
            row.setRowKind(source.kind().name());
            row.setRawJson(source.rawJson());
            row.setStatus(source.status().name());
            row.setMessage(truncate(source.message(), 1024));
            importRowRepository.save(row);
        }
    }

    private PersistedNodes persistNodes(FrameworkTypeCode type,
                                        String ver,
                                        String sourceFilename,
                                        List<ParsedFrameworkRow> rows) {
        // Parents must be persisted before children: sort by level then display order.
        List<ParsedFrameworkRow> ordered = rows.stream()
                .sorted(Comparator.comparingInt(ParsedFrameworkRow::level)
                        .thenComparingInt(ParsedFrameworkRow::displayOrder)
                        .thenComparing(ParsedFrameworkRow::code))
                .toList();

        Framework framework = frameworkRepository.findByCodeAndVersion(type.name(), ver).orElse(null);
        int nextQuestionVersion = 1;
        if (framework != null) {
            if (framework.getStatus() == FrameworkStatus.PUBLISHED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Published framework " + type.name() + " " + ver
                                + " is immutable: import a new version");
            }
            nextQuestionVersion = questionRepository.maxVersion(framework.getId()) + 1;
            replaceNodes(framework.getId());
            framework.setValidatedBy(null);
            framework.setValidatedAt(null);
            framework.setPublishedBy(null);
            framework.setPublishedAt(null);
        } else {
            framework = new Framework();
            framework.setCode(type.name());
            framework.setVersion(ver);
        }

        framework.setName(type.displayName());
        framework.setModuleCode(type.moduleCode());
        framework.setStatus(FrameworkStatus.DRAFT);
        framework.setImportedBy(currentEmail());
        framework.setImportedAt(Instant.now());
        framework.setSourceFilename(sourceFilename);
        framework = frameworkRepository.save(framework);

        Map<String, Long> idByCode = new HashMap<>();
        int controlCount = 0;
        for (ParsedFrameworkRow row : ordered) {
            FrameworkNode node = new FrameworkNode();
            node.setFrameworkId(framework.getId());
            if (row.parentCode() != null) {
                Long parentId = idByCode.get(row.parentCode());
                if (parentId == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Missing parent for " + row.code());
                }
                node.setParentId(parentId);
            }
            node.setLevel(row.level());
            node.setNodeCode(row.code());
            node.setTitle(row.title());
            node.setDefinitionText(row.definition());
            node.setControlType(row.controlType());
            node.setPriority(row.priority());
            node.setDisplayOrder(row.displayOrder());
            node.setRecordType(row.recordType());
            node.setOfficialRef(row.officialRef());
            node.setInSoa(row.inSoa());
            node.setInGapAssessment(row.inGapAssessment());
            node.setInAuditChecklist(row.inAuditChecklist());
            node.setInRiskRegister(row.inRiskRegister());
            node = nodeRepository.save(node);
            idByCode.put(row.code(), node.getId());
            for (Map.Entry<String, String> attribute : row.attributes().entrySet()) {
                FrameworkNodeAttribute entity = new FrameworkNodeAttribute();
                entity.setNodeId(node.getId());
                entity.setAttrKey(attribute.getKey());
                entity.setAttrValue(attribute.getValue());
                nodeAttributeRepository.save(entity);
            }
            // control_count is "routed to at least one module". record_type = CONTROL would score
            // ISO 22301 zero, since all 18 of its nodes are CLAUSE, and validate() rejects zero.
            if (row.routedToAnyModule()) {
                controlCount++;
            }
        }
        framework.setControlCount(controlCount);
        frameworkRepository.save(framework);

        return new PersistedNodes(framework.getId(), idByCode, nextQuestionVersion);
    }

    /**
     * Questions arrive in the same workbook as the nodes, so they are written here rather than
     * through the separate legacy {@code FrameworkQuestionService} upload. The version number keeps
     * climbing across re-imports even though {@link #replaceNodes} removes the old rows: the FK to
     * {@code framework_node} means old questions cannot outlive the nodes they hang off.
     */
    private int persistQuestions(PersistedNodes persisted, List<ParsedMasterWorkbook.Question> questions) {
        int imported = 0;
        for (ParsedMasterWorkbook.Question source : questions) {
            Long nodeId = persisted.idByCode().get(source.controlCode());
            if (nodeId == null) {
                // Unreachable: the parser only emits questions whose control_code resolved.
                continue;
            }
            FrameworkQuestion question = new FrameworkQuestion();
            question.setFrameworkId(persisted.frameworkId());
            question.setFrameworkNodeId(nodeId);
            question.setQuestionCode(source.controlCode() + "-" + source.questionNumber());
            question.setControlCode(source.controlCode());
            question.setQuestionNumber(source.questionNumber());
            question.setQuestionType(source.questionType());
            question.setQuestionText(source.questionText());
            question.setAnswerOptions(source.answerOptions());
            question.setEvidenceGuidance(source.evidenceGuidance());
            question.setMandatory(source.mandatory());
            question.setDisplayOrder(source.displayOrder());
            question.setVersionNumber(persisted.nextQuestionVersion());
            question.setActive(true);
            questionRepository.save(question);
            imported++;
        }
        return imported;
    }

    private record PersistedNodes(Long frameworkId, Map<String, Long> idByCode, int nextQuestionVersion) {
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 1) + "…";
    }

    private static String resolveVersion(String version, FrameworkTypeCode type) {
        return resolveVersion(version, type, null);
    }

    private static String resolveVersion(String version, FrameworkTypeCode type, String filename) {
        if (version != null && !version.isBlank()) {
            return version.trim();
        }
        String fromFile = versionHintFromFilename(filename, type);
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile;
        }
        return type.defaultVersion();
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public FrameworkSummaryResponse validate(Long id) {
        Framework framework = requireFramework(id);
        if (framework.getStatus() != FrameworkStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only DRAFT frameworks can be validated (current: " + framework.getStatus() + ")");
        }
        if (framework.getControlCount() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Framework has no controls (level 3–4) to validate");
        }
        framework.setStatus(FrameworkStatus.VALIDATED);
        framework.setValidatedBy(currentEmail());
        framework.setValidatedAt(Instant.now());
        return FrameworkSummaryResponse.from(frameworkRepository.save(framework));
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public FrameworkSummaryResponse publish(Long id) {
        Framework framework = requireFramework(id);
        if (framework.getStatus() != FrameworkStatus.VALIDATED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only VALIDATED frameworks can be published (current: " + framework.getStatus() + ")");
        }
        framework.setStatus(FrameworkStatus.PUBLISHED);
        framework.setPublishedBy(currentEmail());
        framework.setPublishedAt(Instant.now());
        return FrameworkSummaryResponse.from(frameworkRepository.save(framework));
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public void deleteDraft(Long id) {
        deleteFramework(id);
    }

    /**
     * Super Admin delete for re-import workflow (V2). Any status allowed if no tenant activation.
     */
    @Transactional(transactionManager = "catalogTransactionManager")
    public void deleteFramework(Long id) {
        Framework framework = requireFramework(id);
        if (tenantFrameworkRepository.existsByFrameworkId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Framework is activated by a tenant: deactivate tenants before delete");
        }
        // Import rows cascade from batch (fk_fir_batch ON DELETE CASCADE).
        importBatchRepository.deleteByFrameworkId(id);
        replaceNodes(id);
        frameworkRepository.delete(framework);
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public List<FrameworkSummaryResponse> listAvailableForTenant(String tenantId) {
        Set<ModuleCode> enabled = enabledModules(tenantId);
        if (enabled.isEmpty()) {
            return List.of();
        }
        Set<Long> activatedIds = tenantFrameworkRepository.findByTenantIdOrderByActivatedAtDesc(tenantId)
                .stream()
                .map(TenantFramework::getFrameworkId)
                .collect(Collectors.toSet());
        List<Framework> frameworks = frameworkRepository
                .findByStatusAndModuleCodeInOrderByCodeAscVersionDesc(FrameworkStatus.PUBLISHED, enabled);
        Set<Long> entitled = moduleLicensingService.entitledFrameworkIds(tenantId);
        frameworks = frameworks.stream().filter(f -> entitled.contains(f.getId())).toList();
        Map<Long, FrameworkRoutingCounts> counts = routingCounts(
                frameworks.stream().map(Framework::getId).toList());
        return frameworks.stream()
                .map(f -> {
                    int[] split = librarySplitCounts(f.getId());
                    return FrameworkSummaryResponse.from(f, activatedIds.contains(f.getId()), null,
                            counts.getOrDefault(f.getId(), FrameworkRoutingCounts.NONE),
                            null,
                            hasCloudNodes(f.getId()),
                            split[0],
                            split[1]);
                })
                .toList();
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public List<FrameworkSummaryResponse> listActivatedForTenant(String tenantId) {
        Set<ModuleCode> enabled = enabledModules(tenantId);
        List<TenantFramework> activations = tenantFrameworkRepository.findByTenantIdOrderByActivatedAtDesc(tenantId);
        Map<Long, FrameworkRoutingCounts> counts = routingCounts(
                activations.stream().map(TenantFramework::getFrameworkId).toList());
        List<FrameworkSummaryResponse> out = new ArrayList<>();
        for (TenantFramework tf : activations) {
            frameworkRepository.findById(tf.getFrameworkId()).ifPresent(f -> {
                // Doc 02 T-02: hide frameworks whose module is no longer licensed.
                if (f.getModuleCode() == null || !enabled.contains(f.getModuleCode())) {
                    return;
                }
                // Doc 02 T-02: hide if Super Admin did not entitle this framework under the module.
                if (!moduleLicensingService.isFrameworkEntitled(tenantId, f.getId())) {
                    return;
                }
                int[] split = librarySplitCounts(f.getId());
                out.add(FrameworkSummaryResponse.from(f, true, tf.getActivatedAt(),
                        counts.getOrDefault(f.getId(), FrameworkRoutingCounts.NONE),
                        tf.isIncludeCloud(),
                        hasCloudNodes(f.getId()),
                        split[0],
                        split[1]));
            });
        }
        return out;
    }

    private Map<Long, FrameworkRoutingCounts> routingCounts(Collection<Long> frameworkIds) {
        if (frameworkIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, FrameworkRoutingCounts> counts = new LinkedHashMap<>();
        for (Object[] row : nodeRepository.routingCounts(frameworkIds)) {
            counts.put(((Number) row[0]).longValue(), new FrameworkRoutingCounts(
                    ((Number) row[1]).intValue(), ((Number) row[2]).intValue(),
                    ((Number) row[3]).intValue(), ((Number) row[4]).intValue()));
        }
        return counts;
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public FrameworkSummaryResponse activate(String tenantId, Long frameworkId, String actorEmail) {
        return activate(tenantId, frameworkId, actorEmail, false);
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public FrameworkSummaryResponse activate(String tenantId, Long frameworkId, String actorEmail,
                                             boolean includeCloud) {
        Framework framework = requireFramework(frameworkId);
        if (framework.getStatus() != FrameworkStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Framework is not published");
        }
        if (!moduleLicensingService.isEnabled(tenantId, framework.getModuleCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Module " + framework.getModuleCode() + " is not licensed for this tenant");
        }
        adminModuleAccessService.assertMayUse(framework.getModuleCode(), true);
        moduleLicensingService.requireWritable(tenantId, framework.getModuleCode());
        if (!moduleLicensingService.isFrameworkEntitled(tenantId, frameworkId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Framework is not licensed for this tenant under " + framework.getModuleCode());
        }
        TenantFramework existing = tenantFrameworkRepository
                .findByTenantIdAndFrameworkId(tenantId, frameworkId)
                .orElse(null);
        if (existing != null) {
            int[] split = librarySplitCounts(frameworkId);
            return FrameworkSummaryResponse.from(framework, true, existing.getActivatedAt(), null,
                    existing.isIncludeCloud(), hasCloudNodes(frameworkId),
                    split[0], split[1]);
        }
        TenantFramework row = new TenantFramework();
        row.setTenantId(tenantId);
        row.setFrameworkId(frameworkId);
        row.setActivatedAt(Instant.now());
        row.setActivatedBy(actorEmail);
        row.setIncludeCloud(includeCloud);
        tenantFrameworkRepository.save(row);
        int[] split = librarySplitCounts(frameworkId);
        return FrameworkSummaryResponse.from(framework, true, row.getActivatedAt(), null,
                row.isIncludeCloud(), hasCloudNodes(frameworkId),
                split[0], split[1]);
    }

    @Transactional(transactionManager = "catalogTransactionManager")
    public void deactivate(String tenantId, Long frameworkId) {
        Framework framework = requireFramework(frameworkId);
        adminModuleAccessService.assertMayUse(framework.getModuleCode(), true);
        moduleLicensingService.requireWritable(tenantId, framework.getModuleCode());
        if (!tenantFrameworkRepository.existsByTenantIdAndFrameworkId(tenantId, frameworkId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Framework is not activated");
        }
        tenantFrameworkRepository.deleteByTenantIdAndFrameworkId(tenantId, frameworkId);
    }

    /**
     * Super Admin: list every tenant with assignment + module-licence status for a published framework.
     */
    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public List<FrameworkTenantAssignmentResponse> listTenantAssignments(Long frameworkId) {
        Framework framework = requirePublished(frameworkId);
        Map<String, TenantFramework> byTenant = tenantFrameworkRepository.findByFrameworkId(frameworkId)
                .stream()
                .collect(Collectors.toMap(TenantFramework::getTenantId, tf -> tf, (a, b) -> a));

        List<Tenant> tenants = tenantRepository.findAll().stream()
                .sorted(Comparator.comparing(Tenant::getTenantId))
                .toList();
        List<FrameworkTenantAssignmentResponse> out = new ArrayList<>();
        for (Tenant tenant : tenants) {
            boolean licensed = moduleLicensingService.isEnabled(tenant.getTenantId(), framework.getModuleCode());
            TenantFramework tf = byTenant.get(tenant.getTenantId());
            out.add(new FrameworkTenantAssignmentResponse(
                    tenant.getTenantId(),
                    tenant.getName(),
                    licensed,
                    tf != null,
                    tf != null ? tf.getActivatedAt() : null));
        }
        return out;
    }

    /**
     * Super Admin: activate/deactivate a published framework for selected tenants.
     * Does <b>not</b> silently license modules: pass {@code licenseModule=true} to license
     * the framework's module in the same request when it is currently off.
     */
    @Transactional(transactionManager = "catalogTransactionManager")
    public List<FrameworkTenantAssignmentResponse> updateTenantAssignments(
            Long frameworkId, UpdateFrameworkTenantsRequest request) {
        Framework framework = requirePublished(frameworkId);
        String actor = currentEmail();
        for (UpdateFrameworkTenantsRequest.TenantToggle toggle : request.tenants()) {
            if (toggle.tenantId() == null || toggle.tenantId().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tenantId is required");
            }
            if (!tenantRepository.existsByTenantId(toggle.tenantId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown tenant: " + toggle.tenantId());
            }
            boolean assign = Boolean.TRUE.equals(toggle.assigned());
            if (assign) {
                boolean licensed = moduleLicensingService.isEnabled(toggle.tenantId(), framework.getModuleCode());
                if (!licensed) {
                    if (!Boolean.TRUE.equals(toggle.licenseModule())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Module " + framework.getModuleCode()
                                        + " is not licensed for " + toggle.tenantId()
                                        + ". License it under Modules, or pass licenseModule=true.");
                    }
                    moduleLicensingService.updateForTenant(
                            toggle.tenantId(),
                            new UpdateModulesRequest(List.of(
                                    new UpdateModulesRequest.ModuleToggle(
                                            framework.getModuleCode().name(), true,
                                            List.of(frameworkId), toggle.expiresOn()))),
                            actor);
                } else {
                    moduleLicensingService.ensureFrameworkEntitled(toggle.tenantId(), frameworkId, actor);
                }
                activate(toggle.tenantId(), frameworkId, actor);
            } else {
                if (tenantFrameworkRepository.existsByTenantIdAndFrameworkId(toggle.tenantId(), frameworkId)) {
                    deactivate(toggle.tenantId(), frameworkId);
                }
            }
        }
        return listTenantAssignments(frameworkId);
    }

    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public FrameworkDetailResponse getForTenant(String tenantId, Long frameworkId) {
        Framework framework = requireFramework(frameworkId);
        if (framework.getStatus() != FrameworkStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Framework not found");
        }
        if (!moduleLicensingService.isEnabled(tenantId, framework.getModuleCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Module " + framework.getModuleCode() + " is not licensed for this tenant");
        }
        if (!moduleLicensingService.isFrameworkEntitled(tenantId, frameworkId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Framework is not licensed for this tenant under " + framework.getModuleCode());
        }
        boolean activated = tenantFrameworkRepository.existsByTenantIdAndFrameworkId(tenantId, frameworkId);
        Instant activatedAt = null;
        Boolean includeCloud = null;
        if (activated) {
            TenantFramework tf = tenantFrameworkRepository.findByTenantIdAndFrameworkId(tenantId, frameworkId)
                    .orElse(null);
            if (tf != null) {
                activatedAt = tf.getActivatedAt();
                includeCloud = tf.isIncludeCloud();
            }
        }
        List<FrameworkNodeResponse> nodes = toNodeResponses(frameworkId);
        int[] split = librarySplitCounts(frameworkId);
        return new FrameworkDetailResponse(
                FrameworkSummaryResponse.from(framework, activated, activatedAt, null, includeCloud,
                        hasCloudNodes(frameworkId), split[0], split[1]),
                nodes);
    }

    /**
     * Every node the framework routes to at least one module. Callers that seed a specific module
     * should name it: see {@link #getActivatedControls(String, Long, ModuleRouting)}.
     */
    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public FrameworkDetailResponse getActivatedControls(String tenantId, Long frameworkId) {
        return getActivatedControls(tenantId, frameworkId, ModuleRouting.ANY);
    }

    /**
     * The nodes this module seeds from, per the framework's own import contract.
     * Framework must be activated for the tenant.
     *
     * <p>Replaces the shape heuristic ("definition text, or level ≥ 3") that every consumer shared
     * until F6. That heuristic also disagreed with {@code control_count}, which F2 had already
     * redefined as "routed to at least one module"; the two now agree by construction.
     */
    @Transactional(transactionManager = "catalogTransactionManager", readOnly = true)
    public FrameworkDetailResponse getActivatedControls(String tenantId, Long frameworkId,
                                                        ModuleRouting routing) {
        FrameworkDetailResponse detail = getForTenant(tenantId, frameworkId);
        if (!Boolean.TRUE.equals(detail.framework().activated())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Activate the framework before creating a SoA or Gap Assessment");
        }
        String code = detail.framework().code();
        List<FrameworkNodeResponse> controls = detail.nodes().stream()
                .filter(n -> !n.retired())
                .filter(CatalogRoutingOverrides.effective(code, routing))
                .toList();
        if (controls.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, routing.emptyMessage(code));
        }
        return new FrameworkDetailResponse(detail.framework(), controls);
    }

    private boolean hasCloudNodes(Long frameworkId) {
        return nodeRepository.countByFrameworkIdAndRecordType(frameworkId, "CLOUD") > 0;
    }

    private List<FrameworkNodeResponse> toNodeResponses(Long frameworkId) {
        List<FrameworkNode> nodes =
                nodeRepository.findByFrameworkIdOrderByDisplayOrderAscNodeCodeAsc(frameworkId);
        if (nodes.isEmpty()) {
            return List.of();
        }
        List<Long> ids = nodes.stream().map(FrameworkNode::getId).toList();
        Map<Long, Map<String, String>> attrsByNode = new HashMap<>();
        for (FrameworkNodeAttribute attribute : nodeAttributeRepository.findByNodeIdIn(ids)) {
            attrsByNode
                    .computeIfAbsent(attribute.getNodeId(), k -> new LinkedHashMap<>())
                    .put(attribute.getAttrKey(), attribute.getAttrValue());
        }
        return nodes.stream()
                .map(n -> FrameworkNodeResponse.from(n, attrsByNode.getOrDefault(n.getId(), Map.of())))
                .toList();
    }

    /**
     * Both dependents are foreign-keyed to {@code framework_node} with {@code ON DELETE CASCADE}, so
     * the database removed them anyway. Deleting them here first keeps the persistence context in
     * step and makes the data loss on re-import visible in the code.
     */
    private void replaceNodes(Long frameworkId) {
        questionRepository.deleteByFrameworkId(frameworkId);
        nodeAttributeRepository.deleteByFrameworkId(frameworkId);
        nodeRepository.clearParents(frameworkId);
        nodeRepository.deleteByFrameworkId(frameworkId);
        nodeRepository.flush();
    }

    private Framework requireFramework(Long id) {
        return frameworkRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Framework not found"));
    }

    private Framework requirePublished(Long id) {
        Framework framework = requireFramework(id);
        if (framework.getStatus() != FrameworkStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only PUBLISHED frameworks can be assigned to tenants");
        }
        return framework;
    }

    private Set<ModuleCode> enabledModules(String tenantId) {
        EnumSet<ModuleCode> set = EnumSet.noneOf(ModuleCode.class);
        for (String code : moduleLicensingService.enabledCodes(tenantId)) {
            set.add(ModuleCode.valueOf(code));
        }
        return adminModuleAccessService.retainLicensed(set);
    }

    private static FrameworkTypeCode parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "typeCode is required");
        }
        try {
            return FrameworkTypeCode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown framework type: " + raw);
        }
    }

    private static String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || Objects.equals(auth.getPrincipal(), "anonymousUser")) {
            return "system";
        }
        return auth.getName();
    }
}
