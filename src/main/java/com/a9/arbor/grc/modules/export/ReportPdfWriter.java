package com.a9.arbor.grc.modules.export;

import com.a9.arbor.grc.config.ArborAuditProperties;
import com.a9.arbor.grc.modules.bcms.domain.BcmsExercise;
import com.a9.arbor.grc.modules.bcms.domain.BiaProcess;
import com.a9.arbor.grc.modules.bcms.domain.RecoveryPlan;
import com.a9.arbor.grc.modules.capa.domain.Capa;
import com.a9.arbor.grc.modules.gap.domain.GapAssessment;
import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyDsr;
import com.a9.arbor.grc.modules.risk.domain.RiskRecord;
import com.a9.arbor.grc.modules.soa.domain.Soa;
import com.a9.arbor.grc.modules.soa.domain.SoaItem;
import com.a9.arbor.grc.modules.tprm.domain.TprmVendor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.a9.arbor.grc.modules.export.ReportPdfSupport.safe;

/** PDF writers for Reports hub (§19.3 PDF + Excel). */
@Component
public class ReportPdfWriter {

    private final ArborAuditProperties auditProperties;

    public ReportPdfWriter(ArborAuditProperties auditProperties) {
        this.auditProperties = auditProperties;
    }

    public byte[] writeCompliancePosture(String orgName, User user, List<GapAssessment> assessments) {
        return ReportPdfSupport.write(orgName, "Overall Compliance Posture", by(user), footer(), page -> {
            page.writeHeading("Gap assessments");
            if (assessments.isEmpty()) {
                page.writeLine("No gap assessments.");
                return;
            }
            List<String[]> rows = new ArrayList<>();
            for (GapAssessment a : assessments) {
                rows.add(new String[]{
                        safe(a.getName()),
                        safe(a.getFrameworkCode()),
                        a.getOverallScore() != null ? a.getOverallScore().toPlainString() : "-",
                        safe(a.getRagStatus()),
                        a.getStatus() != null ? a.getStatus().name() : "-"
                });
            }
            page.writeTable(
                    new String[]{"Name", "Framework", "Score %", "RAG", "Status"},
                    rows,
                    new float[]{2.2f, 1.2f, 0.9f, 0.9f, 1.2f});
        });
    }

    public byte[] writeRiskRegister(String orgName, User user, String title, List<RiskRecord> risks) {
        return ReportPdfSupport.write(orgName, title, by(user), footer(), page -> {
            page.writeHeading("Risks");
            if (risks.isEmpty()) {
                page.writeLine("No risks.");
                return;
            }
            List<String[]> rows = new ArrayList<>();
            for (RiskRecord r : risks) {
                rows.add(new String[]{
                        safe(r.getDisplayId()),
                        safe(r.getTitle()),
                        r.getStatus() != null ? r.getStatus().name() : "-",
                        r.getResidualRisk() != null ? r.getResidualRisk().name() : "-",
                        r.getTreatmentOption() != null ? r.getTreatmentOption().name() : "-"
                });
            }
            page.writeTable(
                    new String[]{"ID", "Title", "Status", "Residual", "Treatment"},
                    rows,
                    new float[]{1.1f, 2.8f, 1.2f, 1.1f, 1.2f});
        });
    }

    public byte[] writeSingleRisk(String orgName, User user, RiskRecord risk) {
        String title = RiskExportSupport.documentTitle(risk);
        return ReportPdfSupport.write(orgName, title, by(user), footer(), page -> {
            page.writeHeading(title);
            page.writeLine("Display ID: " + safe(risk.getDisplayId()));
            page.writeLine("Title: " + safe(risk.getTitle()));
            page.blank();
            List<String[]> rows = new ArrayList<>();
            for (String[] pair : RiskExportSupport.detailRows(risk)) {
                rows.add(new String[]{
                        pair[0],
                        ReportPdfSupport.truncate(pair[1] != null ? pair[1] : "", 400)
                });
            }
            page.writeTable(
                    new String[]{"Field", "Value"},
                    rows,
                    new float[]{2.2f, 5.0f});
        });
    }

    public byte[] writeCapaStatus(String orgName, User user, List<Capa> rows) {
        return ReportPdfSupport.write(orgName, "CAPA Status", by(user), footer(), page -> {
            page.writeHeading("CAPA records");
            if (rows.isEmpty()) {
                page.writeLine("No CAPA records.");
                return;
            }
            List<String[]> table = new ArrayList<>();
            for (Capa c : rows) {
                table.add(new String[]{
                        c.getRegisterCode() != null ? c.getRegisterCode().name() : "-",
                        safe(c.getTitle()),
                        c.getStatus() != null ? c.getStatus().name() : "-",
                        c.getPriority() != null ? c.getPriority().name() : "-",
                        c.getTargetDate() != null ? c.getTargetDate().toString() : "-"
                });
            }
            page.writeTable(
                    new String[]{"Register", "Title", "Status", "Priority", "Target"},
                    table,
                    new float[]{1.1f, 2.6f, 1.1f, 1f, 1.1f});
        });
    }

    public byte[] writeTprmVendorRisk(String orgName, User user, List<TprmVendor> vendors) {
        List<String[]> rows = new ArrayList<>();
        for (TprmVendor v : vendors) {
            rows.add(new String[]{
                    safe(v.getName()),
                    v.getVendorType() != null ? v.getVendorType().name() : "-",
                    v.getTier() != null ? v.getTier() : "-",
                    v.getCriticality() != null ? v.getCriticality().name() : "-",
                    v.getComplianceStatus() != null ? v.getComplianceStatus().name() : "-",
                    v.getNextReviewDate() != null ? v.getNextReviewDate().toString() : "-"
            });
        }
        return writeSimpleTable(orgName, user, "TPRM Vendor Register",
                new String[]{"Vendor", "Type", "Tier", "Criticality", "Compliance", "Next reassessment"},
                rows, new float[]{2.2f, 1.3f, 0.8f, 1.1f, 1.1f, 1.2f});
    }

    public byte[] writeSimpleTable(String orgName, User user, String title, String[] headers,
                                   List<String[]> rows, float[] widths) {
        return ReportPdfSupport.write(orgName, title, by(user), footer(), page -> {
            page.writeHeading(title);
            if (rows.isEmpty()) {
                page.writeLine("No rows.");
                return;
            }
            page.writeTable(headers, rows, widths);
        });
    }

    public byte[] writeBiaSummary(String orgName, User user, List<BiaProcess> processes) {
        return ReportPdfSupport.write(orgName, "BIA Summary", by(user), footer(), page -> {
            page.writeHeading("Processes");
            if (processes.isEmpty()) {
                page.writeLine("No BIA records.");
                return;
            }
            List<String[]> rows = new ArrayList<>();
            for (BiaProcess b : processes) {
                rows.add(new String[]{
                        safe(b.getProcessName()),
                        b.getCategoryLabel() != null ? b.getCategoryLabel() : "-",
                        b.getRtoHours() != null ? b.getRtoHours().toString() : "-",
                        b.getMaoHours() != null ? b.getMaoHours().toString() : "-",
                        b.getReviewOutcome() != null ? b.getReviewOutcome().display() : "-"
                });
            }
            page.writeTable(
                    new String[]{"Process", "Criticality", "RTO (h)", "MAO (h)", "Review"},
                    rows,
                    new float[]{2.2f, 1.4f, 0.8f, 0.8f, 1.2f});
        });
    }

    public byte[] writePrivacyDsr(String orgName, User user, List<PrivacyDsr> dsrs) {
        return ReportPdfSupport.write(orgName, "DSR Statistics", by(user), footer(), page -> {
            page.writeHeading("Data subject requests");
            if (dsrs.isEmpty()) {
                page.writeLine("No DSRs.");
                return;
            }
            List<String[]> rows = new ArrayList<>();
            for (PrivacyDsr d : dsrs) {
                rows.add(new String[]{
                        safe(d.getDisplayId()),
                        d.getRequestType() != null ? d.getRequestType().name() : "-",
                        d.getStatus() != null ? d.getStatus().name() : "-",
                        d.getDueAt() != null ? d.getDueAt().toString() : "-",
                        d.getReceivedAt() != null ? d.getReceivedAt().toString() : "-"
                });
            }
            page.writeTable(
                    new String[]{"Display ID", "Type", "Status", "Due", "Received"},
                    rows,
                    new float[]{1.2f, 1.2f, 1.1f, 1.5f, 1.5f});
        });
    }

    public byte[] writeSoa(String orgName, User user, Soa soa, List<SoaItem> items) {
        return ReportPdfSupport.write(orgName, "Statement of Applicability", by(user), footer(), page -> {
            page.writeHeading("Cover");
            page.writeLine("Framework: " + safe(soa.getFrameworkName()) + " (" + safe(soa.getFrameworkCode())
                    + " v" + safe(soa.getFrameworkVersion()) + ")");
            page.writeLine("Status: " + (soa.getStatus() != null ? soa.getStatus().name() : "-"));
            page.writeLine("SoA ID: " + soa.getId());
            page.blank();
            page.writeHeading("Controls");
            List<String[]> rows = new ArrayList<>();
            for (SoaItem item : items) {
                rows.add(new String[]{
                        safe(item.getNodeCode()),
                        safe(item.getTitle()),
                        item.getApplicability() != null ? item.getApplicability().name() : "-",
                        safe(item.getJustification())
                });
            }
            page.writeTable(
                    new String[]{"Code", "Title", "Applicability", "Justification"},
                    rows,
                    new float[]{0.9f, 2.2f, 1.2f, 2.5f});
            page.writeSignatureBlock();
        });
    }

    public byte[] writeGap(String orgName, User user, GapAssessment assessment, List<GapItem> items) {
        return ReportPdfSupport.write(orgName, "Gap Assessment", by(user), footer(), page -> {
            page.writeHeading("Cover");
            page.writeLine("Name: " + safe(assessment.getName()));
            page.writeLine("Framework: " + safe(assessment.getFrameworkName()) + " ("
                    + safe(assessment.getFrameworkCode()) + ")");
            page.writeLine("Status: " + (assessment.getStatus() != null ? assessment.getStatus().name() : "-"));
            page.writeLine("Overall score: "
                    + (assessment.getOverallScore() != null ? assessment.getOverallScore().toPlainString() + "%" : "-")
                    + "  RAG: " + safe(assessment.getRagStatus()));
            page.blank();
            page.writeHeading("Controls");
            List<String[]> rows = new ArrayList<>();
            for (GapItem item : items) {
                var effective = item.getFinalImplementationStatus() != null
                        ? item.getFinalImplementationStatus()
                        : item.getImplementationStatus();
                rows.add(new String[]{
                        safe(item.getNodeCode()),
                        safe(item.getTitle()),
                        item.getApplicability() != null ? item.getApplicability().name() : "-",
                        effective != null ? effective.name() : "-",
                        item.getScore() != null ? item.getScore().toString() : "-"
                });
            }
            page.writeTable(
                    new String[]{"Code", "Title", "Applicability", "Final status", "Score"},
                    rows,
                    new float[]{0.9f, 2.2f, 1.1f, 1.2f, 0.7f});
        });
    }

    /** §19.3 #05 UAE IA V2.1 Compliance: NESA/TDRA layer scores. */
    public byte[] writeUaeIaCompliance(String orgName, User user,
                                       GapAssessment assessment,
                                       com.a9.arbor.grc.modules.gap.web.dto.GapLayerScoreSummary layers,
                                       List<GapItem> items) {
        return ReportPdfSupport.write(orgName, "UAE IA V2.1 Compliance", by(user), footer(), page -> {
            page.writeHeading("Cover");
            page.writeLine("Assessment: " + safe(assessment.getName()));
            page.writeLine("Framework: " + safe(assessment.getFrameworkName()) + " ("
                    + safe(assessment.getFrameworkCode()) + " · " + safe(assessment.getFrameworkVersion()) + ")");
            page.writeLine("Status: " + (assessment.getStatus() != null ? assessment.getStatus().name() : "-"));
            page.blank();
            page.writeHeading("Management / Technical / Overall");
            List<String[]> layerRows = new ArrayList<>();
            layerRows.add(layerRow("Management", layers.management()));
            layerRows.add(layerRow("Technical", layers.technical()));
            layerRows.add(layerRow("Overall (UAE IA)", layers.overallUaeIa()));
            page.writeTable(
                    new String[]{"Layer", "Score %", "RAG", "Applicable", "Scored"},
                    layerRows,
                    new float[]{2f, 1f, 1f, 1f, 1f});
            page.writeLine("Overall is the average of every applicable control Status % from the UAE IA pack. Unstarted controls count as 0. Domain scores are a breakdown of the same controls.");
            page.blank();
            page.writeHeading("Domain scores");
            if (layers.domains() == null || layers.domains().isEmpty()) {
                page.writeLine("No domain scores.");
            } else {
                List<String[]> domainRows = new ArrayList<>();
                for (var d : layers.domains()) {
                    domainRows.add(new String[]{
                            safe(d.code()),
                            safe(d.name()),
                            d.score() != null ? d.score().toPlainString() : "-",
                            safe(d.rag()),
                            String.valueOf(d.applicableCount()),
                            String.valueOf(d.scoredCount())
                    });
                }
                page.writeTable(
                        new String[]{"Code", "Domain", "Score %", "RAG", "Applicable", "Scored"},
                        domainRows,
                        new float[]{0.8f, 2f, 0.9f, 0.9f, 1f, 0.9f});
            }
            page.blank();
            page.writeHeading("Control scores");
            List<String[]> controlRows = new ArrayList<>();
            for (GapItem item : items) {
                if (item.getApplicability() == com.a9.arbor.grc.modules.gap.domain.GapApplicability.NOT_APPLICABLE) {
                    continue;
                }
                var effective = item.getFinalImplementationStatus() != null
                        ? item.getFinalImplementationStatus()
                        : item.getImplementationStatus();
                controlRows.add(new String[]{
                        safe(item.getNodeCode()),
                        safe(item.getTitle()),
                        effective != null ? effective.name() : "-",
                        item.getScore() != null ? item.getScore().toString() : "-"
                });
            }
            page.writeTable(
                    new String[]{"Code", "Title", "Final status", "Score"},
                    controlRows,
                    new float[]{0.9f, 2.8f, 1.3f, 0.7f});
        });
    }

    /** §19.3 #06 NCEMA V3 Compliance: pillar scores + control detail. */
    public byte[] writeNcemaCompliance(String orgName, User user,
                                       GapAssessment assessment,
                                       com.a9.arbor.grc.modules.gap.web.dto.GapLayerScoreSummary layers,
                                       List<GapItem> items) {
        return ReportPdfSupport.write(orgName, "NCEMA V3 Compliance", by(user), footer(), page -> {
            page.writeHeading("Cover");
            page.writeLine("Assessment: " + safe(assessment.getName()));
            page.writeLine("Framework: " + safe(assessment.getFrameworkName()) + " ("
                    + safe(assessment.getFrameworkCode()) + ")");
            page.writeLine("Overall score: "
                    + (assessment.getOverallScore() != null ? assessment.getOverallScore().toPlainString() + "%" : "-")
                    + "  RAG: " + safe(assessment.getRagStatus()));
            page.blank();
            page.writeHeading("Pillar scores");
            if (layers == null || layers.domains() == null || layers.domains().isEmpty()) {
                page.writeLine("No pillar scores: score applicable controls first.");
            } else {
                List<String[]> pillarRows = new ArrayList<>();
                for (var p : layers.domains()) {
                    pillarRows.add(new String[]{
                            safe(p.code()),
                            safe(p.name()),
                            p.score() != null ? p.score().toPlainString() : "-",
                            safe(p.rag()),
                            String.valueOf(p.applicableCount()),
                            String.valueOf(p.scoredCount())
                    });
                }
                page.writeTable(
                        new String[]{"Code", "Pillar", "Score %", "RAG", "Applicable", "Scored"},
                        pillarRows,
                        new float[]{0.8f, 2f, 0.9f, 0.9f, 1f, 0.9f});
            }
            page.blank();
            page.writeHeading("Control scores");
            List<String[]> controlRows = new ArrayList<>();
            for (GapItem item : items) {
                if (item.getApplicability() == com.a9.arbor.grc.modules.gap.domain.GapApplicability.NOT_APPLICABLE) {
                    continue;
                }
                var effective = item.getFinalImplementationStatus() != null
                        ? item.getFinalImplementationStatus()
                        : item.getImplementationStatus();
                controlRows.add(new String[]{
                        safe(item.getNodeCode()),
                        safe(item.getTitle()),
                        effective != null ? effective.name() : "-",
                        item.getScore() != null ? item.getScore().toString() : "-"
                });
            }
            page.writeTable(
                    new String[]{"Code", "Title", "Final status", "Score"},
                    controlRows,
                    new float[]{0.9f, 2.8f, 1.3f, 0.7f});
        });
    }

    /** §19.3 #01 Executive GRC Summary: internal board pack or external summary. */
    public byte[] writeExecutiveSummary(String orgName, User user, boolean external, ExecutiveSummaryBrief brief) {
        return ExecutiveSummaryPdf.write(orgName, preparedName(user), external, brief, footer());
    }

    /** §19.3 #11 BCP Summary: approved recovery plans. */
    public byte[] writeBcpSummary(String orgName, User user, List<RecoveryPlan> plans) {
        return ReportPdfSupport.write(orgName, "BCP Summary", by(user), footer(), page -> {
            page.writeHeading("Approved recovery plans");
            if (plans.isEmpty()) {
                page.writeLine("No approved recovery plans.");
                return;
            }
            List<String[]> rows = new ArrayList<>();
            for (RecoveryPlan p : plans) {
                rows.add(new String[]{
                        safe(p.getName()),
                        p.getPlanType() != null ? p.getPlanType().name() : "-",
                        p.getStatus() != null ? p.getStatus().name() : "-",
                        p.getReviewDate() != null ? p.getReviewDate().toString() : "-",
                        ReportPdfSupport.truncate(p.getScope(), 80)
                });
            }
            page.writeTable(
                    new String[]{"Plan", "Type", "Status", "Review date", "Scope"},
                    rows,
                    new float[]{1.6f, 0.9f, 0.9f, 1f, 2.4f});
            page.writeSignatureBlock();
        });
    }

    /** §19.3 #12 Exercise Report: completed exercise results. */
    public byte[] writeExerciseReport(String orgName, User user, BcmsExercise exercise) {
        return ReportPdfSupport.write(orgName, "Exercise Report", by(user), footer(), page -> {
            page.writeHeading("Exercise");
            page.writeLine("Name: " + safe(exercise.getName()));
            page.writeLine("Type: " + (exercise.getExerciseType() != null ? exercise.getExerciseType().name() : "-"));
            page.writeLine("Status: " + (exercise.getStatus() != null ? exercise.getStatus().name() : "-"));
            page.writeLine("Scheduled: " + (exercise.getScheduledDate() != null ? exercise.getScheduledDate().toString() : "-"));
            page.writeLine("Completed: " + (exercise.getCompletedAt() != null ? exercise.getCompletedAt().toString() : "-"));
            page.blank();
            if (exercise.getObjectives() != null) {
                page.writeHeading("Objectives");
                page.writeLine(ReportPdfSupport.truncate(exercise.getObjectives(), 500));
                page.blank();
            }
            if (exercise.getResults() != null) {
                page.writeHeading("Results");
                page.writeLine(ReportPdfSupport.truncate(exercise.getResults(), 500));
                page.blank();
            }
            if (exercise.getGapsIdentified() != null) {
                page.writeHeading("Gaps identified");
                page.writeLine(ReportPdfSupport.truncate(exercise.getGapsIdentified(), 500));
                page.blank();
            }
            if (exercise.getLessonsLearned() != null) {
                page.writeHeading("Lessons learned");
                page.writeLine(ReportPdfSupport.truncate(exercise.getLessonsLearned(), 500));
            }
        });
    }

    private static String[] layerRow(String label,
                                     com.a9.arbor.grc.modules.gap.web.dto.GapLayerScoreSummary.LayerScore layer) {
        if (layer == null) {
            return new String[]{label, "-", "-", "-", "-"};
        }
        return new String[]{
                label,
                layer.score() != null ? layer.score().toPlainString() : "-",
                safe(layer.rag()),
                String.valueOf(layer.applicableCount()),
                String.valueOf(layer.scoredCount())
        };
    }

    private String footer() {
        return auditProperties.getReports().getClassificationFooter();
    }

    private static String by(User user) {
        if (user == null) {
            return "-";
        }
        return user.getFullName() + " <" + user.getEmail() + ">";
    }

    /** Board header: the person's name, not the mailbox. */
    private static String preparedName(User user) {
        if (user == null) {
            return "-";
        }
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName().trim();
        }
        return user.getEmail() == null || user.getEmail().isBlank() ? "-" : user.getEmail().trim();
    }
}
