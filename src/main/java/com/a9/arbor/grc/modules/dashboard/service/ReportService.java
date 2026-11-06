package com.a9.arbor.grc.modules.dashboard.service;

import com.a9.arbor.grc.catalog.domain.ModuleCode;
import com.a9.arbor.grc.catalog.domain.Tenant;
import com.a9.arbor.grc.catalog.repo.TenantRepository;
import com.a9.arbor.grc.catalog.service.FrameworkScoringSupport;
import com.a9.arbor.grc.catalog.service.ModuleLicensingService;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.repo.AuditLogRepository;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.auditmgmt.domain.AuditType;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAudit;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditFindingRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditRepository;
import com.a9.arbor.grc.modules.auditmgmt.service.AuditReportService;
import com.a9.arbor.grc.modules.bcms.domain.BcmsExercise;
import com.a9.arbor.grc.modules.bcms.domain.ExerciseStatus;
import com.a9.arbor.grc.modules.bcms.domain.RecoveryPlan;
import com.a9.arbor.grc.modules.bcms.domain.RecoveryPlanStatus;
import com.a9.arbor.grc.modules.bcms.repo.BcmsExerciseRepository;
import com.a9.arbor.grc.modules.bcms.domain.BiaReviewOutcome;
import com.a9.arbor.grc.modules.bcms.repo.BiaProcessRepository;
import com.a9.arbor.grc.modules.bcms.repo.RecoveryPlanRepository;
import com.a9.arbor.grc.modules.capa.domain.CapaStatus;
import com.a9.arbor.grc.modules.capa.repo.CapaRepository;
import com.a9.arbor.grc.modules.dashboard.web.dto.ReportDescriptor;
import com.a9.arbor.grc.modules.evidence.repo.EvidenceRepository;
import com.a9.arbor.grc.modules.export.ExecutiveSummaryBrief;
import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.export.ReportPdfWriter;
import com.a9.arbor.grc.modules.export.ReportWorkbookWriter;
import com.a9.arbor.grc.modules.export.RiskExportSupport;
import com.a9.arbor.grc.modules.gap.domain.GapAssessment;
import com.a9.arbor.grc.modules.gap.domain.GapAssessmentStatus;
import com.a9.arbor.grc.modules.gap.repo.GapAssessmentRepository;
import com.a9.arbor.grc.modules.gap.repo.GapItemRepository;
import com.a9.arbor.grc.modules.gap.service.GapAssessmentService;
import com.a9.arbor.grc.modules.gap.service.GapLayerScoreJson;
import com.a9.arbor.grc.modules.gap.web.dto.GapLayerScoreSummary;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.service.AdminModuleAccessService;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.privacy.repo.PrivacyDsrRepository;
import com.a9.arbor.grc.modules.risk.domain.RiskStatus;
import com.a9.arbor.grc.modules.risk.domain.RiskType;
import com.a9.arbor.grc.modules.risk.domain.RiskLevel;
import com.a9.arbor.grc.modules.risk.repo.RiskRecordRepository;
import com.a9.arbor.grc.modules.soa.service.SoaService;
import com.a9.arbor.grc.modules.tprm.domain.TprmVendor;
import com.a9.arbor.grc.modules.tprm.domain.VendorStatus;
import com.a9.arbor.grc.modules.tprm.repo.TprmVendorRepository;
import com.a9.arbor.grc.modules.tprm.service.TprmReportService;
import com.a9.arbor.grc.tenancy.TenantContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Standard reports for the Reports hub (req §19.3). SoA / Gap delegate to per-record export
 * services; the rest are cross-record snapshots built here. PDF + Excel where catalog lists both.
 */
@Service
public class ReportService {

    private static final Map<String, ReportDescriptor> CATALOG = buildCatalog();
    /**
     * Same features as the hidden nav items: NCEMA, recovery plans, exercises, DSR.
     * Generators stay so the rows can come back by removing the id.
     */
    private static final Set<String> HIDDEN_FROM_HUB = Set.of(
            "ncema_compliance", "bcp_summary", "exercise_report", "privacy_dsr");
    private static final String XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PDF = "application/pdf";

    private final ModuleLicensingService moduleLicensingService;
    private final AdminModuleAccessService adminModuleAccessService;
    private final AuditTrailService auditTrailService;
    private final ReportWorkbookWriter workbookWriter;
    private final ReportPdfWriter pdfWriter;
    private final GapAssessmentRepository gapAssessmentRepository;
    private final RiskRecordRepository riskRecordRepository;
    private final CapaRepository capaRepository;
    private final SoaService soaService;
    private final GapAssessmentService gapAssessmentService;
    private final TprmVendorRepository tprmVendorRepository;
    private final TprmReportService tprmReportService;
    private final BiaProcessRepository biaProcessRepository;
    private final EvidenceRepository evidenceRepository;
    private final AuditLogRepository auditLogRepository;
    private final PrivacyDsrRepository privacyDsrRepository;
    private final TenantRepository tenantRepository;
    private final GapItemRepository gapItemRepository;
    private final GrcAuditRepository grcAuditRepository;
    private final GrcAuditFindingRepository grcAuditFindingRepository;
    private final AuditReportService auditReportService;
    private final RecoveryPlanRepository recoveryPlanRepository;
    private final BcmsExerciseRepository bcmsExerciseRepository;

    public ReportService(ModuleLicensingService moduleLicensingService,
                         AdminModuleAccessService adminModuleAccessService,
                         AuditTrailService auditTrailService,
                         ReportWorkbookWriter workbookWriter,
                         ReportPdfWriter pdfWriter,
                         GapAssessmentRepository gapAssessmentRepository,
                         RiskRecordRepository riskRecordRepository,
                         CapaRepository capaRepository,
                         SoaService soaService,
                         GapAssessmentService gapAssessmentService,
                         TprmVendorRepository tprmVendorRepository,
                         TprmReportService tprmReportService,
                         BiaProcessRepository biaProcessRepository,
                         EvidenceRepository evidenceRepository,
                         AuditLogRepository auditLogRepository,
                         PrivacyDsrRepository privacyDsrRepository,
                         TenantRepository tenantRepository,
                         GapItemRepository gapItemRepository,
                         GrcAuditRepository grcAuditRepository,
                         GrcAuditFindingRepository grcAuditFindingRepository,
                         AuditReportService auditReportService,
                         RecoveryPlanRepository recoveryPlanRepository,
                         BcmsExerciseRepository bcmsExerciseRepository) {
        this.moduleLicensingService = moduleLicensingService;
        this.adminModuleAccessService = adminModuleAccessService;
        this.auditTrailService = auditTrailService;
        this.workbookWriter = workbookWriter;
        this.pdfWriter = pdfWriter;
        this.gapAssessmentRepository = gapAssessmentRepository;
        this.riskRecordRepository = riskRecordRepository;
        this.capaRepository = capaRepository;
        this.soaService = soaService;
        this.gapAssessmentService = gapAssessmentService;
        this.tprmVendorRepository = tprmVendorRepository;
        this.tprmReportService = tprmReportService;
        this.biaProcessRepository = biaProcessRepository;
        this.evidenceRepository = evidenceRepository;
        this.auditLogRepository = auditLogRepository;
        this.privacyDsrRepository = privacyDsrRepository;
        this.tenantRepository = tenantRepository;
        this.gapItemRepository = gapItemRepository;
        this.grcAuditRepository = grcAuditRepository;
        this.grcAuditFindingRepository = grcAuditFindingRepository;
        this.auditReportService = auditReportService;
        this.recoveryPlanRepository = recoveryPlanRepository;
        this.bcmsExerciseRepository = bcmsExerciseRepository;
    }

    private static Map<String, ReportDescriptor> buildCatalog() {
        Map<String, ReportDescriptor> map = new LinkedHashMap<>();
        List<String> xlsxPdf = List.of("xlsx", "pdf");
        map.put("executive_grc_summary", ReportDescriptor.of("executive_grc_summary",
                "Executive GRC Summary", List.of("pdf"), "PLATFORM"));
        map.put("compliance_posture", ReportDescriptor.of("compliance_posture", "Overall Compliance Posture",
                xlsxPdf, "PLATFORM"));
        map.put("soa", new ReportDescriptor("soa", "Statement of Applicability", xlsxPdf, "ISMS", true));
        map.put("gap", new ReportDescriptor("gap", "Gap Assessment", xlsxPdf, "ISMS", true));
        map.put("uae_ia_compliance", new ReportDescriptor("uae_ia_compliance", "UAE IA V2.1 Compliance",
                xlsxPdf, "ISMS", true));
        map.put("ncema_compliance", new ReportDescriptor("ncema_compliance", "NCEMA V3 Compliance",
                xlsxPdf, "BCMS", true));
        map.put("is_risk", ReportDescriptor.of("is_risk", "IS Risk Register", xlsxPdf, "ISMS"));
        map.put("capa_status", ReportDescriptor.of("capa_status", "CAPA Status", xlsxPdf, "PLATFORM"));
        map.put("audit_trail", ReportDescriptor.of("audit_trail", "Audit Trail", List.of("csv"), "PLATFORM"));
        map.put("erm_risk", ReportDescriptor.of("erm_risk", "ERM Risk Summary", xlsxPdf, "ERM"));
        map.put("tprm_vendor_risk", ReportDescriptor.of("tprm_vendor_risk", "TPRM Vendor Register", xlsxPdf, "ERM"));
        map.put("tprm_categorisation", ReportDescriptor.of("tprm_categorisation", "TPRM Categorisation Worksheet", xlsxPdf, "ERM"));
        map.put("tprm_questionnaire_responses", ReportDescriptor.of("tprm_questionnaire_responses", "TPRM Questionnaire Responses", xlsxPdf, "ERM"));
        map.put("tprm_posture", ReportDescriptor.of("tprm_posture", "TPRM Posture vs Target", xlsxPdf, "ERM"));
        map.put("tprm_findings", ReportDescriptor.of("tprm_findings", "TPRM Findings Tracker", xlsxPdf, "ERM"));
        map.put("bia_summary", ReportDescriptor.of("bia_summary", "BIA Summary", xlsxPdf, "BCMS"));
        map.put("bcp_summary", ReportDescriptor.of("bcp_summary", "BCP Summary", List.of("pdf"), "BCMS"));
        map.put("exercise_report", new ReportDescriptor("exercise_report", "Exercise Report",
                List.of("pdf"), "BCMS", true));
        map.put("evidence_expiry", ReportDescriptor.of("evidence_expiry", "Evidence Expiry", List.of("xlsx"), "PLATFORM"));
        map.put("user_activity", ReportDescriptor.of("user_activity", "User Activity", List.of("xlsx"), "PLATFORM"));
        map.put("privacy_dsr", ReportDescriptor.of("privacy_dsr", "DSR Statistics", xlsxPdf, "PRIVACY"));
        map.put("internal_audit_report", new ReportDescriptor("internal_audit_report",
                "Internal Audit Report", List.of("pdf"), "AUDIT", true));
        map.put("external_audit_report", new ReportDescriptor("external_audit_report",
                "External Audit Report", List.of("pdf"), "AUDIT", true));
        return map;
    }

    @Transactional(readOnly = true)
    public List<ReportDescriptor> list() {
        String tenantId = TenantContext.getTenantId();
        return CATALOG.values().stream()
                .filter(r -> !HIDDEN_FROM_HUB.contains(r.id()))
                .filter(r -> "PLATFORM".equals(r.module()) || isModuleLicensed(tenantId, r.module(), false))
                .toList();
    }

    @Transactional
    public ExportFile export(User user, String id, String format, Long soaId, Long gapId, Long auditId,
                             Long exerciseId, String variant) {
        ReportDescriptor descriptor = CATALOG.get(id);
        if (descriptor == null || HIDDEN_FROM_HUB.contains(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown report: " + id);
        }
        String tenantId = TenantContext.getTenantId();
        if (!"PLATFORM".equals(descriptor.module()) && !isModuleLicensed(tenantId, descriptor.module(), true)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Module " + descriptor.module() + " is not licensed for this workspace");
        }
        if (!descriptor.formats().stream().anyMatch(f -> f.equalsIgnoreCase(format))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Report " + id + " does not support format " + format);
        }

        boolean pdf = "pdf".equalsIgnoreCase(format);
        return switch (id) {
            case "executive_grc_summary" -> exportExecutiveSummary(user, pdf, variant);
            case "compliance_posture" -> exportCompliancePosture(user, pdf);
            case "is_risk" -> exportIsRisk(user, pdf);
            case "capa_status" -> exportCapaStatus(user, pdf);
            case "audit_trail" -> auditTrailService.exportCsv(user, null, null, null);
            case "soa" -> exportSoa(user, soaId, pdf);
            case "gap" -> exportGap(user, gapId, pdf);
            case "uae_ia_compliance" -> exportUaeIaCompliance(user, gapId, pdf);
            case "ncema_compliance" -> exportNcemaCompliance(user, gapId, pdf);
            case "internal_audit_report" -> exportLockedAudit(user, auditId, AuditType.INTERNAL);
            case "external_audit_report" -> exportLockedAudit(user, auditId, AuditType.EXTERNAL);
            case "erm_risk" -> exportErmRisk(user, pdf);
            case "tprm_vendor_risk" -> exportTprmVendorRisk(user, pdf);
            case "tprm_categorisation" -> exportTprmCategorisation(user, pdf);
            case "tprm_questionnaire_responses" -> exportTprmResponses(user, pdf);
            case "tprm_posture" -> exportTprmPosture(user, pdf);
            case "tprm_findings" -> exportTprmFindings(user, pdf);
            case "bia_summary" -> exportBiaSummary(user, pdf);
            case "bcp_summary" -> exportBcpSummary(user, pdf);
            case "exercise_report" -> exportExerciseReport(user, exerciseId, pdf);
            case "evidence_expiry" -> exportEvidenceExpiry(user);
            case "user_activity" -> exportUserActivity(user);
            case "privacy_dsr" -> exportPrivacyDsr(user, pdf);
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown report: " + id);
        };
    }

    private ExportFile exportSoa(User user, Long soaId, boolean pdf) {
        if (soaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pick a Statement of Applicability from Assessments first, then export with ?soaId=");
        }
        return pdf ? soaService.exportPdf(user, soaId) : soaService.exportExcel(user, soaId);
    }

    private ExportFile exportGap(User user, Long gapId, boolean pdf) {
        if (gapId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pick a Gap Assessment from Assessments first, then export with ?gapId=");
        }
        return pdf ? gapAssessmentService.exportPdf(user, gapId) : gapAssessmentService.exportExcel(user, gapId);
    }

    private ExportFile exportUaeIaCompliance(User user, Long gapId, boolean pdf) {
        if (gapId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pick a UAE IA Gap Assessment first, then export with ?gapId=");
        }
        GapAssessment assessment = gapAssessmentRepository.findById(gapId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap assessment not found"));
        Set<GapAssessmentStatus> allowed = EnumSet.of(
                GapAssessmentStatus.IN_PROGRESS, GapAssessmentStatus.COMPLETED);
        if (!allowed.contains(assessment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Gap assessment must be IN_PROGRESS or COMPLETED before export (status="
                            + assessment.getStatus() + ")");
        }
        if (!FrameworkScoringSupport.isUaeIa(assessment.getFrameworkCode(), assessment.getFrameworkName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Report requires a UAE IA Gap Assessment (framework="
                            + assessment.getFrameworkCode() + ")");
        }
        GapLayerScoreSummary layers = GapLayerScoreJson.parse(assessment.getLayerScoreJson());
        if (layers == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "UAE IA layer scores are not available yet: score applicable controls first");
        }
        var items = gapItemRepository.findByGapAssessmentIdOrderByNodeCodeAsc(gapId);
        if (pdf) {
            byte[] bytes = pdfWriter.writeUaeIaCompliance(orgName(), user, assessment, layers, items);
            audit(user, "UAE_IA_COMPLIANCE", String.valueOf(gapId), "uae-ia-compliance.pdf", items.size());
            return new ExportFile(bytes, "uae-ia-compliance-" + gapId + ".pdf", PDF);
        }
        byte[] bytes = workbookWriter.writeUaeIaCompliance(assessment, layers, items);
        audit(user, "UAE_IA_COMPLIANCE", String.valueOf(gapId), "uae-ia-compliance.xlsx", items.size());
        return new ExportFile(bytes, "uae-ia-compliance-" + gapId + ".xlsx", XLSX);
    }

    private ExportFile exportNcemaCompliance(User user, Long gapId, boolean pdf) {
        if (gapId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pick a NCEMA Gap Assessment first, then export with ?gapId=");
        }
        GapAssessment assessment = gapAssessmentRepository.findById(gapId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gap assessment not found"));
        Set<GapAssessmentStatus> allowed = EnumSet.of(
                GapAssessmentStatus.IN_PROGRESS, GapAssessmentStatus.COMPLETED);
        if (!allowed.contains(assessment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Gap assessment must be IN_PROGRESS or COMPLETED before export (status="
                            + assessment.getStatus() + ")");
        }
        if (!FrameworkScoringSupport.isNcema(assessment.getFrameworkCode(), assessment.getFrameworkName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Report requires a NCEMA Gap Assessment (framework="
                            + assessment.getFrameworkCode() + ")");
        }
        GapLayerScoreSummary layers = GapLayerScoreJson.parse(assessment.getLayerScoreJson());
        var items = gapItemRepository.findByGapAssessmentIdOrderByNodeCodeAsc(gapId);
        if (pdf) {
            byte[] bytes = pdfWriter.writeNcemaCompliance(orgName(), user, assessment, layers, items);
            audit(user, "NCEMA_COMPLIANCE", String.valueOf(gapId), "ncema-compliance.pdf", items.size());
            return new ExportFile(bytes, "ncema-compliance-" + gapId + ".pdf", PDF);
        }
        byte[] bytes = workbookWriter.writeNcemaCompliance(assessment, layers, items);
        audit(user, "NCEMA_COMPLIANCE", String.valueOf(gapId), "ncema-compliance.xlsx", items.size());
        return new ExportFile(bytes, "ncema-compliance-" + gapId + ".xlsx", XLSX);
    }

    private ExportFile exportExecutiveSummary(User user, boolean pdf, String variant) {
        if (!pdf) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Executive GRC Summary is available as PDF only");
        }
        boolean external = "external".equalsIgnoreCase(variant);
        ExecutiveSummaryBrief brief = buildExecutiveBrief();
        byte[] bytes = pdfWriter.writeExecutiveSummary(orgName(), user, external, brief);
        audit(user, "EXECUTIVE_GRC_SUMMARY", null,
                external ? "executive-summary-external.pdf" : "executive-summary-internal.pdf",
                brief.frameworks().size());
        return new ExportFile(bytes,
                "executive-grc-summary-" + (external ? "external" : "internal") + ".pdf", PDF);
    }

    private ExportFile exportBcpSummary(User user, boolean pdf) {
        if (!pdf) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BCP Summary is available as PDF only");
        }
        List<RecoveryPlan> plans = recoveryPlanRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(p -> p.getStatus() == RecoveryPlanStatus.APPROVED)
                .toList();
        byte[] bytes = pdfWriter.writeBcpSummary(orgName(), user, plans);
        audit(user, "BCP_SUMMARY", null, "bcp-summary.pdf", plans.size());
        return new ExportFile(bytes, "bcp-summary-" + tenantOrDefault() + ".pdf", PDF);
    }

    private ExportFile exportExerciseReport(User user, Long exerciseId, boolean pdf) {
        if (!pdf) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exercise Report is available as PDF only");
        }
        if (exerciseId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pick a completed exercise first, then export with ?exerciseId=");
        }
        BcmsExercise exercise = bcmsExerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found"));
        if (exercise.getStatus() != ExerciseStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Exercise must be COMPLETED before the formal report is available");
        }
        byte[] bytes = pdfWriter.writeExerciseReport(orgName(), user, exercise);
        audit(user, "EXERCISE_REPORT", String.valueOf(exerciseId), "exercise-report.pdf", 1);
        return new ExportFile(bytes, "exercise-report-" + exerciseId + ".pdf", PDF);
    }

    private ExecutiveSummaryBrief buildExecutiveBrief() {
        var assessments = gapAssessmentRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(a -> a.getStatus() == GapAssessmentStatus.IN_PROGRESS
                        || a.getStatus() == GapAssessmentStatus.COMPLETED)
                .filter(a -> a.getOverallScore() != null)
                .filter(a -> !FrameworkScoringSupport.isNcema(a.getFrameworkCode(), a.getFrameworkName()))
                .toList();
        String overall;
        List<ExecutiveSummaryBrief.FrameworkScore> frameworks;
        if (assessments.isEmpty()) {
            overall = "-";
            frameworks = List.of();
        } else {
            BigDecimal sum = assessments.stream()
                    .map(GapAssessment::getOverallScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avg = sum.divide(BigDecimal.valueOf(assessments.size()), 1, RoundingMode.HALF_UP);
            overall = avg.toPlainString() + "%";
            frameworks = assessments.stream()
                    .map(a -> {
                        String code = a.getFrameworkCode() == null || a.getFrameworkCode().isBlank()
                                ? "-" : a.getFrameworkCode().trim();
                        String name = a.getFrameworkName() == null || a.getFrameworkName().isBlank()
                                ? code : a.getFrameworkName().trim();
                        return new ExecutiveSummaryBrief.FrameworkScore(
                                code, name, a.getOverallScore().toPlainString() + "%");
                    })
                    .toList();
        }
        long openIs = riskRecordRepository.findByRiskTypeOrderByUpdatedAtDesc(RiskType.IS_RISK).stream()
                .filter(r -> r.getStatus() != RiskStatus.CLOSED).count();
        long openErm = riskRecordRepository.findByRiskTypeOrderByUpdatedAtDesc(RiskType.ERM).stream()
                .filter(r -> r.getStatus() != RiskStatus.CLOSED).count();
        long critical = riskRecordRepository.findAll().stream()
                .filter(r -> r.getStatus() != RiskStatus.CLOSED)
                .filter(r -> r.getResidualRisk() == RiskLevel.HIGH || r.getResidualRisk() == RiskLevel.VERY_HIGH)
                .count();
        long overdue = riskRecordRepository.findAll().stream()
                .filter(r -> r.getStatus() == RiskStatus.OVERDUE).count();
        long openCapas = capaRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(c -> c.getStatus() != CapaStatus.CLOSED).count();
        long overdueCapas = capaRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(c -> c.getStatus() == CapaStatus.OVERDUE).count();
        long openFindings = 0;
        for (GrcAudit audit : grcAuditRepository.findAllByOrderByUpdatedAtDesc()) {
            if (!audit.isLocked()) {
                openFindings += grcAuditFindingRepository.countByAuditId(audit.getId());
            }
        }
        long highVendors = tprmVendorRepository.findAllByOrderByNameAsc().stream()
                .filter(v -> v.getStatus() == VendorStatus.ACTIVE)
                .filter(v -> v.getCriticality() == com.a9.arbor.grc.modules.tprm.domain.TprmCriticality.CRITICAL
                        || v.getComplianceStatus() == com.a9.arbor.grc.modules.tprm.domain.TprmCompliance.NOT_COMPLIANT)
                .count();
        var uae = gapAssessmentRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(a -> a.getOverallScore() != null)
                .filter(a -> FrameworkScoringSupport.isUaeIa(a.getFrameworkCode(), a.getFrameworkName()))
                .findFirst();
        String uaeScore = uae.map(a -> a.getOverallScore().toPlainString() + "%").orElse("-");
        String uaeCode = uae.map(a -> a.getFrameworkCode() == null ? "" : a.getFrameworkCode()).orElse("");
        return new ExecutiveSummaryBrief(
                overall,
                uaeScore,
                uaeCode,
                String.valueOf(openIs),
                String.valueOf(openErm),
                String.valueOf(critical),
                String.valueOf(overdue),
                String.valueOf(openCapas),
                String.valueOf(overdueCapas),
                String.valueOf(openFindings),
                String.valueOf(highVendors),
                biaApprovedPct(),
                frameworks);
    }

    private String biaApprovedPct() {
        var processes = biaProcessRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(b -> b.getReviewOutcome() != BiaReviewOutcome.REJECTED)
                .toList();
        if (processes.isEmpty()) {
            return "-";
        }
        long verified = processes.stream()
                .filter(b -> b.getReviewOutcome().specialistDone())
                .count();
        return Math.round(verified * 100.0 / processes.size()) + "%";
    }

    private ExportFile exportLockedAudit(User user, Long auditId, AuditType expectedType) {
        if (auditId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pick a locked " + expectedType.name().toLowerCase() + " audit, then export with ?auditId=");
        }
        GrcAudit audit = grcAuditRepository.findById(auditId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit not found"));
        if (audit.getAuditType() != expectedType) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Audit type must be " + expectedType.name() + " for this report");
        }
        if (!audit.isLocked()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Audit must be locked (dual-signed) before the formal PDF is available");
        }
        ExportFile file = auditReportService.download(user, auditId);
        audit(user, expectedType == AuditType.INTERNAL ? "INTERNAL_AUDIT_REPORT" : "EXTERNAL_AUDIT_REPORT",
                String.valueOf(auditId), file.filename(), 1);
        return file;
    }

    private ExportFile exportCompliancePosture(User user, boolean pdf) {
        var assessments = gapAssessmentRepository.findAllByOrderByUpdatedAtDesc();
        if (pdf) {
            byte[] bytes = pdfWriter.writeCompliancePosture(orgName(), user, assessments);
            audit(user, "COMPLIANCE_POSTURE", null, "compliance-posture.pdf", assessments.size());
            return new ExportFile(bytes, "compliance-posture-" + tenantOrDefault() + ".pdf", PDF);
        }
        byte[] bytes = workbookWriter.writeCompliancePosture(assessments);
        audit(user, "COMPLIANCE_POSTURE", null, "compliance-posture.xlsx", assessments.size());
        return new ExportFile(bytes, "compliance-posture-" + tenantOrDefault() + ".xlsx", XLSX);
    }

    private ExportFile exportIsRisk(User user, boolean pdf) {
        var risks = riskRecordRepository.findByRiskTypeOrderByUpdatedAtDesc(RiskType.IS_RISK);
        if (pdf) {
            byte[] bytes = pdfWriter.writeRiskRegister(orgName(), user, "IS Risk Register", risks);
            audit(user, "IS_RISK_REGISTER", null, "is-risk-register.pdf", risks.size());
            return new ExportFile(bytes, "is-risk-register-" + tenantOrDefault() + ".pdf", PDF);
        }
        byte[] bytes = workbookWriter.writeIsRiskRegister(risks);
        audit(user, "IS_RISK_REGISTER", null, "is-risk-register.xlsx", risks.size());
        return new ExportFile(bytes, "is-risk-register-" + tenantOrDefault() + ".xlsx", XLSX);
    }

    private ExportFile exportErmRisk(User user, boolean pdf) {
        var risks = riskRecordRepository.findByRiskTypeOrderByUpdatedAtDesc(RiskType.ERM);
        if (pdf) {
            byte[] bytes = pdfWriter.writeRiskRegister(orgName(), user, "ERM Risk Summary", risks);
            audit(user, "ERM_RISK_SUMMARY", null, "erm-risk-summary.pdf", risks.size());
            return new ExportFile(bytes, "erm-risk-summary-" + tenantOrDefault() + ".pdf", PDF);
        }
        byte[] bytes = workbookWriter.writeErmRiskRegister(risks);
        audit(user, "ERM_RISK_SUMMARY", null, "erm-risk-summary.xlsx", risks.size());
        return new ExportFile(bytes, "erm-risk-summary-" + tenantOrDefault() + ".xlsx", XLSX);
    }

    /** One IS or ERM risk (detail + register-row sheets). Same export roles as the register. */
    @Transactional
    public ExportFile exportSingleRisk(User user, long riskId, String format) {
        var risk = riskRecordRepository.findById(riskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Risk not found"));
        String module = switch (risk.getRiskType()) {
            case ERM -> "ERM";
            case PRIVACY_RISK -> "PRIVACY";
            case IS_RISK -> "ISMS";
        };
        if (!isModuleLicensed(TenantContext.getTenantId(), module, true)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Module " + module + " is not licensed for this workspace");
        }
        boolean pdf = "pdf".equalsIgnoreCase(format);
        if (!pdf && !"xlsx".equalsIgnoreCase(format)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format must be xlsx or pdf");
        }
        String stem = RiskExportSupport.filenameStem(risk);
        String recordType = switch (risk.getRiskType()) {
            case ERM -> "ERM_RISK";
            case PRIVACY_RISK -> "PRIVACY_RISK";
            case IS_RISK -> "IS_RISK";
        };
        if (pdf) {
            byte[] bytes = pdfWriter.writeSingleRisk(orgName(), user, risk);
            audit(user, recordType, String.valueOf(risk.getId()), stem + ".pdf", 1);
            return new ExportFile(bytes, stem + ".pdf", PDF);
        }
        byte[] bytes = workbookWriter.writeSingleRisk(risk);
        audit(user, recordType, String.valueOf(risk.getId()), stem + ".xlsx", 1);
        return new ExportFile(bytes, stem + ".xlsx", XLSX);
    }

    private ExportFile exportTprmVendorRisk(User user, boolean pdf) {
        ExportFile file = tprmReportService.vendorRegister(user, orgName(), tenantOrDefault(), pdf);
        audit(user, "TPRM_VENDOR_REGISTER", null, file.filename(), 0);
        return file;
    }

    private ExportFile exportTprmCategorisation(User user, boolean pdf) {
        ExportFile file = tprmReportService.categorisation(user, orgName(), tenantOrDefault(), pdf);
        audit(user, "TPRM_CATEGORISATION", null, file.filename(), 0);
        return file;
    }

    private ExportFile exportTprmResponses(User user, boolean pdf) {
        ExportFile file = tprmReportService.responses(user, orgName(), tenantOrDefault(), pdf);
        audit(user, "TPRM_RESPONSES", null, file.filename(), 0);
        return file;
    }

    private ExportFile exportTprmPosture(User user, boolean pdf) {
        ExportFile file = tprmReportService.posture(user, orgName(), tenantOrDefault(), pdf);
        audit(user, "TPRM_POSTURE", null, file.filename(), 0);
        return file;
    }

    private ExportFile exportTprmFindings(User user, boolean pdf) {
        ExportFile file = tprmReportService.findings(user, orgName(), tenantOrDefault(), pdf);
        audit(user, "TPRM_FINDINGS", null, file.filename(), 0);
        return file;
    }

    private ExportFile exportBiaSummary(User user, boolean pdf) {
        var processes = biaProcessRepository.findAllByOrderByUpdatedAtDesc();
        if (pdf) {
            byte[] bytes = pdfWriter.writeBiaSummary(orgName(), user, processes);
            audit(user, "BIA_SUMMARY", null, "bia-summary.pdf", processes.size());
            return new ExportFile(bytes, "bia-summary-" + tenantOrDefault() + ".pdf", PDF);
        }
        byte[] bytes = workbookWriter.writeBiaSummary(processes);
        audit(user, "BIA_SUMMARY", null, "bia-summary.xlsx", processes.size());
        return new ExportFile(bytes, "bia-summary-" + tenantOrDefault() + ".xlsx", XLSX);
    }

    private ExportFile exportEvidenceExpiry(User user) {
        var rows = evidenceRepository.findActiveExpiringOrExpired(LocalDate.now().plusDays(30));
        byte[] bytes = workbookWriter.writeEvidenceExpiry(rows);
        audit(user, "EVIDENCE_EXPIRY", null, "evidence-expiry.xlsx", rows.size());
        return new ExportFile(bytes, "evidence-expiry-" + tenantOrDefault() + ".xlsx", XLSX);
    }

    private ExportFile exportUserActivity(User user) {
        var rows = auditLogRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, 2000)).getContent();
        byte[] bytes = workbookWriter.writeUserActivity(rows);
        audit(user, "USER_ACTIVITY", null, "user-activity.xlsx", rows.size());
        return new ExportFile(bytes, "user-activity-" + tenantOrDefault() + ".xlsx", XLSX);
    }

    private ExportFile exportPrivacyDsr(User user, boolean pdf) {
        var dsrs = privacyDsrRepository.findAllByOrderByUpdatedAtDesc();
        if (pdf) {
            byte[] bytes = pdfWriter.writePrivacyDsr(orgName(), user, dsrs);
            audit(user, "PRIVACY_DSR", null, "privacy-dsr.pdf", dsrs.size());
            return new ExportFile(bytes, "privacy-dsr-" + tenantOrDefault() + ".pdf", PDF);
        }
        byte[] bytes = workbookWriter.writePrivacyDsr(dsrs);
        audit(user, "PRIVACY_DSR", null, "privacy-dsr.xlsx", dsrs.size());
        return new ExportFile(bytes, "privacy-dsr-" + tenantOrDefault() + ".xlsx", XLSX);
    }

    private ExportFile exportCapaStatus(User user, boolean pdf) {
        var capas = capaRepository.findAllByOrderByUpdatedAtDesc();
        if (pdf) {
            byte[] bytes = pdfWriter.writeCapaStatus(orgName(), user, capas);
            audit(user, "CAPA_STATUS", null, "capa-status.pdf", capas.size());
            return new ExportFile(bytes, "capa-status-" + tenantOrDefault() + ".pdf", PDF);
        }
        byte[] bytes = workbookWriter.writeCapaStatus(capas);
        audit(user, "CAPA_STATUS", null, "capa-status.xlsx", capas.size());
        return new ExportFile(bytes, "capa-status-" + tenantOrDefault() + ".xlsx", XLSX);
    }

    private String orgName() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return "-";
        }
        return tenantRepository.findByTenantId(tenantId).map(Tenant::getName).orElse(tenantId);
    }

    private boolean isModuleLicensed(String tenantId, String moduleCode, boolean write) {
        if (tenantId == null || tenantId.isBlank()) {
            return false;
        }
        try {
            ModuleCode code = ModuleCode.valueOf(moduleCode);
            return moduleLicensingService.isEnabled(tenantId, code)
                    && adminModuleAccessService.mayUse(code, write);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void audit(User user, String recordType, String recordId, String recordName, int rows) {
        String role = user.getRoles().stream().map(Role::name).sorted().findFirst().orElse("UNKNOWN");
        auditTrailService.record(AuditActionType.EXPORT, "REPORTS", user.getId(), role,
                recordType, recordId, recordName, "export", null, "rows=" + rows, null, null);
    }

    private String tenantOrDefault() {
        String tenantId = TenantContext.getTenantId();
        return tenantId != null ? tenantId : "tenant";
    }
}
