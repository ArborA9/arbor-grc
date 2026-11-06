package com.a9.arbor.grc.modules.tprm.service;

import com.a9.arbor.grc.modules.export.ExportFile;
import com.a9.arbor.grc.modules.export.ReportPdfWriter;
import com.a9.arbor.grc.modules.export.ReportWorkbookWriter;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.tprm.domain.TprmAssessment;
import com.a9.arbor.grc.modules.tprm.domain.TprmAssessmentItem;
import com.a9.arbor.grc.modules.tprm.domain.TprmControl;
import com.a9.arbor.grc.modules.tprm.domain.TprmFinding;
import com.a9.arbor.grc.modules.tprm.domain.TprmVendor;
import com.a9.arbor.grc.modules.tprm.pack.TprmControlDisplay;
import com.a9.arbor.grc.modules.tprm.repo.TprmAssessmentItemRepository;
import com.a9.arbor.grc.modules.tprm.repo.TprmAssessmentRepository;
import com.a9.arbor.grc.modules.tprm.repo.TprmControlRepository;
import com.a9.arbor.grc.modules.tprm.repo.TprmFindingRepository;
import com.a9.arbor.grc.modules.tprm.repo.TprmVendorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TprmReportService {

    private static final String XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PDF = "application/pdf";

    private final TprmVendorRepository vendorRepository;
    private final TprmAssessmentRepository assessmentRepository;
    private final TprmAssessmentItemRepository itemRepository;
    private final TprmControlRepository controlRepository;
    private final TprmFindingRepository findingRepository;
    private final ReportWorkbookWriter workbookWriter;
    private final ReportPdfWriter pdfWriter;

    public TprmReportService(TprmVendorRepository vendorRepository,
                             TprmAssessmentRepository assessmentRepository,
                             TprmAssessmentItemRepository itemRepository,
                             TprmControlRepository controlRepository,
                             TprmFindingRepository findingRepository,
                             ReportWorkbookWriter workbookWriter,
                             ReportPdfWriter pdfWriter) {
        this.vendorRepository = vendorRepository;
        this.assessmentRepository = assessmentRepository;
        this.itemRepository = itemRepository;
        this.controlRepository = controlRepository;
        this.findingRepository = findingRepository;
        this.workbookWriter = workbookWriter;
        this.pdfWriter = pdfWriter;
    }

    @Transactional(readOnly = true)
    public ExportFile vendorRegister(User user, String orgName, String tenant, boolean pdf) {
        List<TprmVendor> vendors = vendorRepository.findAllByOrderByNameAsc();
        if (pdf) {
            return new ExportFile(pdfWriter.writeTprmVendorRisk(orgName, user, vendors),
                    "tprm-vendor-register-" + tenant + ".pdf", PDF);
        }
        return new ExportFile(workbookWriter.writeTprmVendorRisk(vendors),
                "tprm-vendor-register-" + tenant + ".xlsx", XLSX);
    }

    @Transactional(readOnly = true)
    public ExportFile categorisation(User user, String orgName, String tenant, boolean pdf) {
        String[] headers = {"Vendor", "F1", "F2", "F3", "F4", "F5", "F6", "Inherent", "Credit", "Adjusted",
                "Tier", "Override", "Criticality"};
        List<String[]> rows = new ArrayList<>();
        for (TprmVendor v : vendorRepository.findAllByOrderByNameAsc()) {
            rows.add(new String[]{
                    n(v.getName()), n(v.getF1()), n(v.getF2()), n(v.getF3()), n(v.getF4()), n(v.getF5()), n(v.getF6()),
                    num(v.getInherentScore()), num(v.getAssuranceCredit()), num(v.getAdjustedScore()),
                    n(v.getTier()), n(v.getOverrideTier()),
                    v.getCriticality() != null ? v.getCriticality().name() : ""
            });
        }
        return file("TPRM Categorisation", "tprm-categorisation", headers, rows, user, orgName, tenant, pdf,
                new float[]{1.6f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.7f, 0.8f, 0.7f, 0.8f, 0.7f, 0.8f, 1f});
    }

    @Transactional(readOnly = true)
    public ExportFile responses(User user, String orgName, String tenant, boolean pdf) {
        String[] headers = {"Vendor", "Assessment", "Control ID", "Domain", "Answer", "Justification", "Verdict"};
        List<String[]> rows = new ArrayList<>();
        Map<Long, TprmVendor> vendors = vendorsById();
        Map<Long, TprmControl> controls = controlsById();
        for (TprmAssessment a : assessmentRepository.findAllByOrderByCreatedAtDesc()) {
            TprmVendor v = vendors.get(a.getVendorId());
            for (TprmAssessmentItem item : itemRepository.findByAssessmentIdOrderBySortOrderAscIdAsc(a.getId())) {
                TprmControl c = controls.get(item.getControlId());
                rows.add(new String[]{
                        v != null ? n(v.getName()) : "",
                        n(a.getDisplayId()),
                        c != null ? n(TprmControlDisplay.code(item, c)) : n(item.getControlCode()),
                        c != null ? n(TprmControlDisplay.domain(item, c)) : n(item.getDomain()),
                        item.getVendorAnswer() != null ? item.getVendorAnswer().name() : "",
                        n(item.getJustification()),
                        item.getReviewVerdict() != null ? item.getReviewVerdict().name() : ""
                });
            }
        }
        return file("TPRM Questionnaire Responses", "tprm-questionnaire-responses", headers, rows,
                user, orgName, tenant, pdf, new float[]{1.6f, 1f, 0.8f, 1.4f, 0.8f, 1.6f, 1.1f});
    }

    @Transactional(readOnly = true)
    public ExportFile posture(User user, String orgName, String tenant, boolean pdf) {
        String[] headers = {"Vendor", "Tier", "Target", "Posture %", "Compliance", "Assessment"};
        List<String[]> rows = new ArrayList<>();
        Map<Long, TprmVendor> vendors = vendorsById();
        for (TprmAssessment a : assessmentRepository.findAllByOrderByCreatedAtDesc()) {
            TprmVendor v = vendors.get(a.getVendorId());
            rows.add(new String[]{
                    v != null ? n(v.getName()) : "",
                    n(a.getSnapshotTier()),
                    a.getSnapshotTarget() != null ? a.getSnapshotTarget().toPlainString() : "",
                    a.getPosturePercent() != null ? a.getPosturePercent().toPlainString() : "",
                    a.getCompliant() == null ? "" : a.getCompliant() ? "COMPLIANT" : "NOT_COMPLIANT",
                    n(a.getDisplayId())
            });
        }
        return file("TPRM Posture vs Target", "tprm-posture", headers, rows, user, orgName, tenant, pdf,
                new float[]{2f, 0.9f, 0.8f, 0.9f, 1.2f, 1.1f});
    }

    @Transactional(readOnly = true)
    public ExportFile findings(User user, String orgName, String tenant, boolean pdf) {
        String[] headers = {"Vendor", "Control ID", "Domain", "Title", "Status", "Due"};
        List<String[]> rows = new ArrayList<>();
        Map<Long, TprmVendor> vendors = vendorsById();
        for (TprmFinding f : findingRepository.findAllByOrderByCreatedAtDesc()) {
            TprmVendor v = vendors.get(f.getVendorId());
            rows.add(new String[]{
                    v != null ? n(v.getName()) : "",
                    n(f.getControlCode()),
                    n(f.getDomain()),
                    n(f.getTitle()),
                    f.getStatus() != null ? f.getStatus().name() : "",
                    f.getDueDate() != null ? f.getDueDate().toString() : ""
            });
        }
        return file("TPRM Findings Tracker", "tprm-findings", headers, rows, user, orgName, tenant, pdf,
                new float[]{1.2f, 1.05f, 1.65f, 2.55f, 0.7f, 0.95f});
    }

    @Transactional(readOnly = true)
    public ExportFile project(User user, String orgName, long assessmentId, boolean pdf) {
        TprmAssessment a = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Project not found"));
        TprmVendor vendor = vendorRepository.findById(a.getVendorId()).orElse(null);
        String[] headers = {"Control ID", "Domain", "Requirement", "Answer", "Justification", "Verdict", "Comment"};
        List<String[]> rows = new ArrayList<>();
        Map<Long, TprmControl> controls = controlsById();
        for (TprmAssessmentItem item : itemRepository.findByAssessmentIdOrderBySortOrderAscIdAsc(a.getId())) {
            TprmControl c = controls.get(item.getControlId());
            rows.add(new String[]{
                    c != null ? n(TprmControlDisplay.code(item, c)) : n(item.getControlCode()),
                    c != null ? n(TprmControlDisplay.domain(item, c)) : n(item.getDomain()),
                    c != null ? n(TprmControlDisplay.requirement(item, c)) : n(item.getRequirement()),
                    item.getVendorAnswer() != null ? item.getVendorAnswer().name() : "",
                    n(item.getJustification()),
                    item.getReviewVerdict() != null ? item.getReviewVerdict().name() : "",
                    n(item.getReviewComment())
            });
        }
        String title = (a.getTitle() != null ? a.getTitle() : a.getDisplayId())
                + (vendor != null ? " - " + vendor.getName() : "");
        String slug = "tprm-project-" + a.getDisplayId();
        return file(title, slug, headers, rows, user, orgName, "project", pdf,
                new float[]{0.9f, 1.2f, 2.2f, 0.8f, 1.6f, 1.1f, 1.4f});
    }

    private ExportFile file(String title, String slug, String[] headers, List<String[]> rows,
                            User user, String orgName, String tenant, boolean pdf, float[] widths) {
        if (pdf) {
            return new ExportFile(pdfWriter.writeSimpleTable(orgName, user, title, headers, rows, widths),
                    slug + "-" + tenant + ".pdf", PDF);
        }
        return new ExportFile(workbookWriter.writeNamedSheet(title, headers, rows),
                slug + "-" + tenant + ".xlsx", XLSX);
    }

    private Map<Long, TprmVendor> vendorsById() {
        Map<Long, TprmVendor> map = new HashMap<>();
        for (TprmVendor v : vendorRepository.findAll()) {
            map.put(v.getId(), v);
        }
        return map;
    }

    private Map<Long, TprmControl> controlsById() {
        Map<Long, TprmControl> map = new HashMap<>();
        for (TprmControl c : controlRepository.findAll()) {
            map.put(c.getId(), c);
        }
        return map;
    }

    private static String n(String s) {
        return s == null ? "" : s;
    }

    private static String num(Integer n) {
        return n == null ? "" : String.valueOf(n);
    }
}
