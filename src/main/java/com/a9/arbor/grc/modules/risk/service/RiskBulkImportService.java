package com.a9.arbor.grc.modules.risk.service;

import com.a9.arbor.grc.catalog.domain.FrameworkNode;
import com.a9.arbor.grc.catalog.repo.FrameworkNodeRepository;
import com.a9.arbor.grc.catalog.service.FrameworkLibraryService;
import com.a9.arbor.grc.catalog.web.dto.FrameworkSummaryResponse;
import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.domain.UserStatus;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.org.domain.OrgUnit;
import com.a9.arbor.grc.modules.org.repo.OrgUnitRepository;
import com.a9.arbor.grc.modules.risk.domain.MatrixAxes;
import com.a9.arbor.grc.modules.risk.domain.RiskCategory;
import com.a9.arbor.grc.modules.risk.domain.RiskLevel;
import com.a9.arbor.grc.modules.risk.domain.RiskProject;
import com.a9.arbor.grc.modules.risk.domain.RiskStatus;
import com.a9.arbor.grc.modules.risk.domain.RiskType;
import com.a9.arbor.grc.modules.risk.domain.TreatmentOption;
import com.a9.arbor.grc.modules.risk.repo.RiskProjectRepository;
import com.a9.arbor.grc.modules.risk.web.dto.AssignRiskRequest;
import com.a9.arbor.grc.modules.risk.web.dto.CreateRiskRequest;
import com.a9.arbor.grc.modules.risk.web.dto.RiskBulkImportResultResponse;
import com.a9.arbor.grc.modules.risk.web.dto.RiskBulkImportResultResponse.RiskBulkImportRowResult;
import com.a9.arbor.grc.modules.risk.web.dto.RiskRecordResponse;
import com.a9.arbor.grc.modules.tprm.domain.TprmVendor;
import com.a9.arbor.grc.modules.tprm.repo.TprmVendorRepository;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RiskBulkImportService {

    private final RiskBulkImportWorkbook workbook;
    private final RiskService riskService;
    private final OrgUnitRepository orgUnitRepository;
    private final UserRepository userRepository;
    private final FrameworkLibraryService frameworkLibraryService;
    private final FrameworkNodeRepository frameworkNodeRepository;
    private final TprmVendorRepository tprmVendorRepository;
    private final RiskProjectRepository riskProjectRepository;
    private final RiskMatrixService riskMatrixService;

    private record Scale(int gridSize, List<RiskLevel> levels) {}

    public RiskBulkImportService(RiskBulkImportWorkbook workbook,
                                 RiskService riskService,
                                 OrgUnitRepository orgUnitRepository,
                                 UserRepository userRepository,
                                 FrameworkLibraryService frameworkLibraryService,
                                 FrameworkNodeRepository frameworkNodeRepository,
                                 TprmVendorRepository tprmVendorRepository,
                                 RiskProjectRepository riskProjectRepository,
                                 RiskMatrixService riskMatrixService) {
        this.riskMatrixService = riskMatrixService;
        this.workbook = workbook;
        this.riskService = riskService;
        this.orgUnitRepository = orgUnitRepository;
        this.userRepository = userRepository;
        this.frameworkLibraryService = frameworkLibraryService;
        this.frameworkNodeRepository = frameworkNodeRepository;
        this.tprmVendorRepository = tprmVendorRepository;
        this.riskProjectRepository = riskProjectRepository;
    }

    @Transactional(readOnly = true)
    public ExportFile downloadTemplate(User admin, RiskType type, Long projectId) {
        riskService.assertLicensed(type);
        RiskProject project = requireProject(type, projectId);
        List<OrgUnit> units = orgUnitRepository.findAllByOrderByLevelNumAscNameAsc().stream()
                .filter(OrgUnit::isActive)
                .toList();
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .toList();
        List<String> orgNames = units.stream().map(OrgUnit::getName).distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        List<String> contributors = users.stream()
                .filter(u -> u.getRoles().contains(Role.CONTRIBUTOR))
                .map(User::getEmail)
                .filter(Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        List<String> owners = users.stream()
                .map(User::getEmail)
                .filter(Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        List<String> frameworks = type == RiskType.IS_RISK
                ? frameworkLibraryService.listActivatedForTenant(requireTenantId()).stream()
                .filter(f -> IsRiskApplicableModules.includes(
                        com.a9.arbor.grc.catalog.domain.ModuleCode.valueOf(f.moduleCode())))
                .map(FrameworkSummaryResponse::code)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList()
                : List.of();
        List<String> vendors = type == RiskType.ERM
                ? tprmVendorRepository.findAllByOrderByNameAsc().stream()
                .map(TprmVendor::getName)
                .filter(Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList()
                : List.of();
        List<String> controlRefs = List.of();
        if (type == RiskType.IS_RISK && project != null && project.getFrameworkId() != null) {
            controlRefs = frameworkNodeRepository
                    .findByFrameworkIdOrderByDisplayOrderAscNodeCodeAsc(project.getFrameworkId())
                    .stream()
                    .filter(n -> !n.isRetired() && n.getNodeCode() != null && !n.getNodeCode().isBlank())
                    .map(FrameworkNode::getNodeCode)
                    .distinct()
                    .toList();
        }
        List<String> threats = type == RiskType.IS_RISK
                ? List.of("External Attack", "Ransomware", "Phishing", "Credential Theft", "Insider Misuse",
                "Data Leakage", "Supply Chain", "Physical Intrusion", "Environmental Failure",
                "Operational Failure", "Regulatory Non-Compliance", "Device Loss")
                : List.of();
        String projectName = project != null ? project.getName() : null;
        Scale scale = tenantScale();
        return workbook.buildTemplate(type, orgNames, contributors, owners, frameworks, vendors,
                controlRefs, threats, projectName, scale.gridSize(), scale.levels());
    }

    /** Distinct matrix axis levels, lowest first: the same list the create form offers. */
    private Scale tenantScale() {
        MatrixAxes axes = riskMatrixService.currentAxes();
        java.util.EnumSet<RiskLevel> set = java.util.EnumSet.noneOf(RiskLevel.class);
        set.addAll(java.util.Arrays.asList(axes.rowLevels()));
        set.addAll(java.util.Arrays.asList(axes.colLevels()));
        return new Scale(riskMatrixService.currentGridSize().size(), List.copyOf(set));
    }

    /**
     * Create-only bulk import: valid rows committed; failed rows collected in the report.
     * Existing risks are never updated or deleted. When {@code projectId} is set, new rows
     * are added to that project alongside any risks already created in the UI.
     * Not wrapped in one transaction so successful creates survive failed rows.
     */
    public RiskBulkImportResultResponse importExcel(User admin, RiskType type, byte[] bytes, Long projectId) {
        riskService.assertLicensed(type);
        RiskProject project = requireProject(type, projectId);
        List<RiskBulkImportWorkbook.ParsedRiskRow> parsed = workbook.parse(bytes, type);

        Map<String, List<OrgUnit>> orgByName = orgUnitRepository.findAllByOrderByLevelNumAscNameAsc().stream()
                .filter(OrgUnit::isActive)
                .collect(Collectors.groupingBy(u -> u.getName().trim().toLowerCase(Locale.ROOT)));
        Map<String, User> userByEmail = userRepository.findAll().stream()
                .filter(u -> u.getEmail() != null)
                .collect(Collectors.toMap(
                        u -> u.getEmail().trim().toLowerCase(Locale.ROOT),
                        u -> u,
                        (a, b) -> a));
        Map<String, FrameworkSummaryResponse> fwByCode = type == RiskType.IS_RISK
                ? frameworkLibraryService.listActivatedForTenant(requireTenantId()).stream()
                .filter(f -> {
                    try {
                        return IsRiskApplicableModules.includes(
                                com.a9.arbor.grc.catalog.domain.ModuleCode.valueOf(f.moduleCode()));
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toMap(
                        f -> f.code().trim().toUpperCase(Locale.ROOT),
                        f -> f,
                        (a, b) -> a))
                : Map.of();
        Map<String, List<TprmVendor>> vendorByName = type == RiskType.ERM
                ? tprmVendorRepository.findAllByOrderByNameAsc().stream()
                .collect(Collectors.groupingBy(v -> v.getName().trim().toLowerCase(Locale.ROOT)))
                : Map.of();

        Scale scale = tenantScale();
        List<RiskBulkImportRowResult> results = new ArrayList<>();
        int created = 0;
        int failed = 0;
        int ignored = 0;

        for (RiskBulkImportWorkbook.ParsedRiskRow row : parsed) {
            if (RiskBulkImportWorkbook.isSampleRow(row)) {
                results.add(new RiskBulkImportRowResult(row.excelRow(), false, true, null, null,
                        RiskBulkImportWorkbook.SAMPLE_IGNORED_MESSAGE));
                ignored++;
                continue;
            }
            try {
                CreateRiskRequest req = toCreateRequest(type, row, orgByName, userByEmail, fwByCode, vendorByName,
                        project, scale);
                RiskRecordResponse createdRisk = riskService.create(admin, req);
                String note = null;
                if (row.contributorEmail() != null && !row.contributorEmail().isBlank()) {
                    if (createdRisk.status() == RiskStatus.AUTO_CLOSED) {
                        note = "created (auto-closed: contributor not assigned)";
                    } else {
                        try {
                            User contributor = resolveUser(row.contributorEmail(), userByEmail, "Risk Custodian (email)");
                            if (!contributor.getRoles().contains(Role.CONTRIBUTOR)) {
                                throw new IllegalArgumentException("Risk Custodian must be a Contributor user");
                            }
                            TreatmentOption treatment = RiskBulkImportWorkbook.parseTreatment(row.treatmentOption());
                            riskService.assign(admin, createdRisk.id(),
                                    new AssignRiskRequest(contributor.getId(), null, treatment, null));
                            createdRisk = riskService.get(admin, createdRisk.id());
                        } catch (Exception assignEx) {
                            String am = assignEx instanceof ResponseStatusException rse && rse.getReason() != null
                                    ? rse.getReason()
                                    : (assignEx.getMessage() != null ? assignEx.getMessage() : "assign failed");
                            note = "created but assign failed: " + am;
                        }
                    }
                }
                results.add(new RiskBulkImportRowResult(
                        row.excelRow(),
                        true,
                        false,
                        createdRisk.id(),
                        createdRisk.displayId(),
                        note));
                created++;
            } catch (Exception ex) {
                String msg = ex instanceof ResponseStatusException rse && rse.getReason() != null
                        ? rse.getReason()
                        : (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
                results.add(new RiskBulkImportRowResult(row.excelRow(), false, false, null, null, msg));
                failed++;
            }
        }

        results.sort(Comparator.comparingInt(RiskBulkImportRowResult::excelRow));
        return new RiskBulkImportResultResponse(created, failed, 0, ignored, results);
    }

    private CreateRiskRequest toCreateRequest(RiskType type,
                                              RiskBulkImportWorkbook.ParsedRiskRow row,
                                              Map<String, List<OrgUnit>> orgByName,
                                              Map<String, User> userByEmail,
                                              Map<String, FrameworkSummaryResponse> fwByCode,
                                              Map<String, List<TprmVendor>> vendorByName,
                                              RiskProject project,
                                              Scale scale) {
        if (blank(row.title())) {
            throw new IllegalArgumentException("Risk Title is required");
        }
        if (blank(row.orgUnitName())) {
            throw new IllegalArgumentException("Division / Department is required");
        }
        RiskCategory category = type == RiskType.IS_RISK && blank(row.category())
                ? RiskCategory.IT
                : RiskBulkImportWorkbook.parseCategory(row.category());
        RiskLevel probability = level(row.probability(), "Likelihood", scale, RiskBulkImportWorkbook.LEVEL_LABEL);
        RiskLevel impact = level(row.impact(), "Impact", scale, RiskBulkImportWorkbook.LEVEL_LABEL);
        RiskLevel controlEff = level(row.controlEffectiveness(), "Control Effectiveness", scale,
                RiskBulkImportWorkbook.CE_LABEL);
        if (category == null || probability == null || impact == null || controlEff == null) {
            throw new IllegalArgumentException(
                    type == RiskType.IS_RISK
                            ? "Likelihood, Impact, and Control Effectiveness are required"
                            : "Risk Category, Likelihood, Impact, and Control Effectiveness are required");
        }
        String categoryOther = blank(row.categoryOther()) ? null : row.categoryOther().trim();
        if (category == RiskCategory.OTHER && (categoryOther == null || categoryOther.isBlank())) {
            throw new IllegalArgumentException("Specify category is required when Risk Category is Other");
        }

        Long orgUnitId = resolveOrgUnit(row.orgUnitName(), orgByName);
        if (type == RiskType.IS_RISK && blank(row.riskOwnerEmail())) {
            throw new IllegalArgumentException("Risk Owner (email) is required");
        }
        Long ownerId = blank(row.riskOwnerEmail())
                ? null
                : resolveUser(row.riskOwnerEmail(), userByEmail, "Risk Owner (email)").getId();
        Long custodianId = blank(row.contributorEmail())
                ? null
                : resolveUser(row.contributorEmail(), userByEmail, "Risk Custodian (email)").getId();

        Long frameworkId = null;
        String frameworkCode = null;
        Long frameworkNodeId = null;
        String nodeCode = null;
        if (type == RiskType.IS_RISK) {
            if (project != null && project.getFrameworkId() != null) {
                final Long projectFwId = project.getFrameworkId();
                frameworkId = projectFwId;
                FrameworkSummaryResponse fromProject = fwByCode.values().stream()
                        .filter(f -> f.id().equals(projectFwId))
                        .findFirst()
                        .orElse(null);
                frameworkCode = fromProject != null ? fromProject.code() : null;
            }
            if (!blank(row.frameworkCode())) {
                FrameworkSummaryResponse fw = fwByCode.get(row.frameworkCode().trim().toUpperCase(Locale.ROOT));
                if (fw == null) {
                    throw new IllegalArgumentException(
                            "Framework \"" + row.frameworkCode().trim() + "\" is not active for this tenant");
                }
                if (frameworkId != null && !frameworkId.equals(fw.id())) {
                    throw new IllegalArgumentException("Framework must match this project's framework");
                }
                frameworkId = fw.id();
                frameworkCode = fw.code();
            }
            if (!blank(row.nodeCode())) {
                if (frameworkId == null) {
                    throw new IllegalArgumentException("Control Ref requires a project framework");
                }
                FrameworkNode node = frameworkNodeRepository
                        .findByFrameworkIdAndNodeCode(frameworkId, row.nodeCode().trim())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Unknown Control Ref for this framework: " + row.nodeCode()));
                frameworkNodeId = node.getId();
                nodeCode = node.getNodeCode();
            }
        }

        String strategicObjective = null;
        BigDecimal financialImpact = null;
        Long vendorId = null;
        if (type == RiskType.ERM) {
            if (blank(row.strategicObjective())) {
                throw new IllegalArgumentException("Strategic Objective (Manual) is required");
            }
            strategicObjective = row.strategicObjective().trim();
            try {
                financialImpact = RiskBulkImportWorkbook.parseDecimal(row.financialImpactAed());
            } catch (Exception e) {
                throw new IllegalArgumentException("Financial Impact (AED) must be a number");
            }
            if (!blank(row.tprmVendorName())) {
                String vendor = row.tprmVendorName().trim();
                List<TprmVendor> matches = vendorByName.get(vendor.toLowerCase(Locale.ROOT));
                if (matches == null || matches.isEmpty()) {
                    throw new IllegalArgumentException("Third Party \"" + vendor + "\" not found in the vendor register");
                }
                if (matches.size() > 1) {
                    throw new IllegalArgumentException(
                            "Third Party \"" + vendor + "\" matches more than one vendor. Rename vendors first.");
                }
                vendorId = matches.get(0).getId();
            }
        }

        Boolean override = null;
        try {
            override = RiskBulkImportWorkbook.parseBool(row.overrideAutoClose());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Override auto-close must be Yes or No");
        }
        TreatmentOption treatment = null;
        try {
            treatment = RiskBulkImportWorkbook.parseTreatment(row.treatmentOption());
        } catch (Exception e) {
            throw new IllegalArgumentException("Treatment Option \"" + row.treatmentOption().trim()
                    + "\" is not valid. Use Treat (Mitigate), Tolerate (Accept), Terminate (Avoid) or Transfer.");
        }

        String description = firstNonBlank(row.description(), row.title());
        String observedGap = firstNonBlank(row.observedGap());
        if (type == RiskType.IS_RISK && blank(observedGap)) {
            throw new IllegalArgumentException("Gap / Observation is required");
        }
        if (type == RiskType.ERM && blank(observedGap)) {
            observedGap = "Imported";
        }
        String threats = type == RiskType.IS_RISK ? firstNonBlank(row.threats()) : null;
        if (type == RiskType.IS_RISK && blank(threats)) {
            throw new IllegalArgumentException("Associated Threat is required");
        }
        String cause = null;
        if (type == RiskType.ERM) {
            String causeRaw = firstNonBlank(row.cause());
            if (causeRaw != null && "other".equalsIgnoreCase(causeRaw)) {
                cause = firstNonBlank(row.causeOther());
                if (blank(cause)) {
                    throw new IllegalArgumentException("Specify cause is required when Risk Cause / Source is Other");
                }
            } else if (causeRaw != null) {
                cause = causeRaw;
            } else {
                cause = "Process Breakdown";
            }
        }
        String treatmentJustification = firstNonBlank(row.treatmentJustification());
        java.time.LocalDate targetDate = null;
        if (!blank(row.targetDate())) {
            try {
                targetDate = parseDate(row.targetDate());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Target Date must be yyyy-MM-dd");
            }
        }
        com.a9.arbor.grc.modules.risk.web.dto.TreatmentPlanDraftRequest planDraft = null;
        if (!blank(row.evidenceRequired()) || targetDate != null) {
            planDraft = new com.a9.arbor.grc.modules.risk.web.dto.TreatmentPlanDraftRequest(
                    null, null, null, null, targetDate, null, null,
                    blank(row.evidenceRequired()) ? null : row.evidenceRequired().trim());
        }
        java.time.LocalDate dateIdentified = parseDate(row.dateIdentified());
        if (type == RiskType.IS_RISK && dateIdentified == null) {
            throw new IllegalArgumentException("Date Identified is required");
        }
        Long projectId = project != null ? project.getId() : null;

        return new CreateRiskRequest(
                type,
                blank(row.displayIdOverride()) ? null : row.displayIdOverride().trim(),
                row.title().trim(),
                description,
                category,
                categoryOther,
                orgUnitId,
                null,
                frameworkId,
                frameworkCode,
                frameworkNodeId,
                nodeCode,
                null,
                observedGap,
                threats,
                probability,
                impact,
                blank(row.existingControls()) ? null : row.existingControls().trim(),
                controlEff,
                null,
                null,
                null,
                treatment,
                null,
                planDraft,
                ownerId,
                null,
                strategicObjective,
                financialImpact,
                vendorId,
                override,
                blank(row.autoCloseOverrideJustification()) ? null : row.autoCloseOverrideJustification().trim(),
                null,
                null,
                null,
                null,
                projectId,
                dateIdentified != null ? dateIdentified : java.time.LocalDate.now(),
                cause,
                null,
                null,
                custodianId,
                null,
                null,
                treatmentJustification,
                null,
                null,
                null,
                null);
    }

    private RiskProject requireProject(RiskType type, Long projectId) {
        if (projectId == null) {
            return null;
        }
        RiskProject p = riskProjectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Risk project not found"));
        if (p.getProjectType() != type) {
            throw new ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Risk type must match the project");
        }
        return p;
    }

    private static java.time.LocalDate parseDate(String raw) {
        if (blank(raw)) {
            return null;
        }
        String s = raw.trim();
        try {
            return java.time.LocalDate.parse(s);
        } catch (Exception ignored) {
            // try other patterns
        }
        for (java.time.format.DateTimeFormatter fmt : List.of(
                java.time.format.DateTimeFormatter.ofPattern("d/M/uuuu"),
                java.time.format.DateTimeFormatter.ofPattern("d-M-uuuu"))) {
            try {
                return java.time.LocalDate.parse(s, fmt);
            } catch (Exception ignored) {
                // try next
            }
        }
        throw new IllegalArgumentException("Date Identified must be yyyy-MM-dd");
    }

    private static Long resolveOrgUnit(String name, Map<String, List<OrgUnit>> orgByName) {
        String n = name.trim();
        List<OrgUnit> matches = orgByName.get(n.toLowerCase(Locale.ROOT));
        if (matches == null || matches.isEmpty()) {
            throw new IllegalArgumentException(
                    "Division / Department \"" + n + "\" not found. Pick a name from the Reference sheet.");
        }
        if (matches.size() > 1) {
            throw new IllegalArgumentException(
                    "Division / Department \"" + n + "\" matches more than one unit. Rename units in Settings.");
        }
        return matches.get(0).getId();
    }

    private static User resolveUser(String email, Map<String, User> byEmail, String field) {
        String e = email.trim();
        User u = byEmail.get(e.toLowerCase(Locale.ROOT));
        if (u == null) {
            throw new IllegalArgumentException(field + " \"" + e + "\" not found. Pick an email from the Reference sheet.");
        }
        if (u.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException(field + " \"" + e + "\" is not an active user");
        }
        return u;
    }

    private static RiskLevel level(String raw, String field, Scale scale, Map<RiskLevel, String> labels) {
        try {
            return RiskBulkImportWorkbook.parseLevel(raw, scale.levels());
        } catch (IllegalArgumentException e) {
            List<String> names = scale.levels().stream().map(labels::get).toList();
            String allowed = names.size() == 1
                    ? names.get(0)
                    : String.join(", ", names.subList(0, names.size() - 1)) + " or " + names.get(names.size() - 1);
            throw new IllegalArgumentException(field + " \"" + raw.trim() + "\" is not valid. Use " + allowed
                    + " (or 1 to " + names.size() + ").");
        }
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (!blank(v)) {
                return v.trim();
            }
        }
        return null;
    }

    private static String requireTenantId() {
        String tid = TenantContext.getTenantId();
        if (tid == null || tid.isBlank()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Tenant required");
        }
        return tid;
    }
}
