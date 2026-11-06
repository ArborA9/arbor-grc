package com.a9.arbor.grc.modules.iam.service;

import com.a9.arbor.grc.modules.bcms.repo.BiaCycleRepository;
import com.a9.arbor.grc.modules.capa.domain.Capa;
import com.a9.arbor.grc.modules.capa.repo.CapaRepository;
import com.a9.arbor.grc.modules.gap.domain.GapItem;
import com.a9.arbor.grc.modules.gap.repo.GapItemRepository;
import com.a9.arbor.grc.modules.gap.service.GapEvidenceLinks;
import com.a9.arbor.grc.modules.iam.domain.RoleAccess;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.risk.repo.RiskRecordRepository;
import com.a9.arbor.grc.modules.task.repo.WorkTaskRepository;
import com.a9.arbor.grc.modules.task.service.MyTasksService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

/**
 * Layer-3 Contributor access: assignee (and Risk Owner) checks at the service layer.
 */
@Service
public class ContributorAccessService {

    private final GapItemRepository gapItemRepository;
    private final RiskRecordRepository riskRecordRepository;
    private final CapaRepository capaRepository;
    private final BiaCycleRepository biaCycleRepository;
    private final WorkTaskRepository workTaskRepository;
    private final com.a9.arbor.grc.modules.incident.repo.IncidentRecordRepository incidentRecordRepository;
    private final com.a9.arbor.grc.modules.privacy.repo.PrivacyRopaRepository privacyRopaRepository;
    private final com.a9.arbor.grc.modules.privacy.repo.PrivacyDpiaRepository privacyDpiaRepository;
    private final com.a9.arbor.grc.modules.privacy.repo.DpiaInstanceQuestionRepository dpiaInstanceQuestionRepository;
    private final com.a9.arbor.grc.modules.privacy.repo.DpiaDepartmentRepository dpiaDepartmentRepository;

    public ContributorAccessService(GapItemRepository gapItemRepository,
                                    RiskRecordRepository riskRecordRepository,
                                    CapaRepository capaRepository,
                                    BiaCycleRepository biaCycleRepository,
                                    WorkTaskRepository workTaskRepository,
                                    com.a9.arbor.grc.modules.incident.repo.IncidentRecordRepository incidentRecordRepository,
                                    com.a9.arbor.grc.modules.privacy.repo.PrivacyRopaRepository privacyRopaRepository,
                                    com.a9.arbor.grc.modules.privacy.repo.PrivacyDpiaRepository privacyDpiaRepository,
                                    com.a9.arbor.grc.modules.privacy.repo.DpiaInstanceQuestionRepository dpiaInstanceQuestionRepository,
                                    com.a9.arbor.grc.modules.privacy.repo.DpiaDepartmentRepository dpiaDepartmentRepository) {
        this.gapItemRepository = gapItemRepository;
        this.riskRecordRepository = riskRecordRepository;
        this.capaRepository = capaRepository;
        this.biaCycleRepository = biaCycleRepository;
        this.workTaskRepository = workTaskRepository;
        this.incidentRecordRepository = incidentRecordRepository;
        this.privacyRopaRepository = privacyRopaRepository;
        this.privacyDpiaRepository = privacyDpiaRepository;
        this.dpiaInstanceQuestionRepository = dpiaInstanceQuestionRepository;
        this.dpiaDepartmentRepository = dpiaDepartmentRepository;
    }

    public boolean canAccessLinkedRecord(User user, String moduleCode, String recordType, String recordId) {
        if (user == null || !RoleAccess.isContributorOnly(user)) {
            return true;
        }
        if (moduleCode == null || recordType == null || recordId == null) {
            return false;
        }
        String module = moduleCode.trim().toUpperCase(Locale.ROOT);
        String type = recordType.trim();
        String idRaw = recordId.trim();
        if (MyTasksService.MODULE_GAP.equals(module) && MyTasksService.TYPE_GAP_QUESTION.equals(type)) {
            Long itemId = GapEvidenceLinks.itemIdFromQuestionRecord(idRaw);
            if (itemId == null) {
                return false;
            }
            return gapItemRepository.findById(itemId)
                    .map(item -> user.getId().equals(item.getAssignedToUserId()) && item.isReleasedToContributor())
                    .orElse(false);
        }
        long id;
        try {
            id = Long.parseLong(idRaw);
        } catch (NumberFormatException ex) {
            return false;
        }
        if (MyTasksService.MODULE_GAP.equals(module) && MyTasksService.TYPE_GAP_ITEM.equals(type)) {
            return gapItemRepository.findById(id)
                    .map(item -> user.getId().equals(item.getAssignedToUserId()) && item.isReleasedToContributor())
                    .orElse(false);
        }
        if (MyTasksService.MODULE_RISK.equals(module) && MyTasksService.TYPE_RISK_RECORD.equals(type)) {
            return riskRecordRepository.findById(id)
                    .map(risk -> user.getId().equals(risk.getAssignedContributorId())
                            || user.getId().equals(risk.getRiskOwnerUserId())
                            || workTaskRepository
                                    .findBySourceModuleAndSourceRecordTypeAndSourceRecordIdAndAssignedToUserId(
                                            MyTasksService.MODULE_RISK,
                                            MyTasksService.TYPE_RISK_ACCEPTANCE,
                                            id,
                                            user.getId())
                                    .isPresent())
                    .orElse(false);
        }
        if (MyTasksService.MODULE_RISK.equals(module) && MyTasksService.TYPE_RISK_ACCEPTANCE.equals(type)) {
            return workTaskRepository
                    .findBySourceModuleAndSourceRecordTypeAndSourceRecordIdAndAssignedToUserId(
                            MyTasksService.MODULE_RISK,
                            MyTasksService.TYPE_RISK_ACCEPTANCE,
                            id,
                            user.getId())
                    .isPresent();
        }
        if (MyTasksService.MODULE_CAPA.equals(module) && MyTasksService.TYPE_CAPA.equals(type)) {
            return capaRepository.findById(id)
                    .map(c -> user.getId().equals(c.getOwnerUserId()))
                    .orElse(false);
        }
        if (MyTasksService.MODULE_BCMS.equals(module) && MyTasksService.TYPE_BIA.equals(type)) {
            return biaCycleRepository.findById(id)
                    .map(bia -> user.getId().equals(bia.getAssignedToUserId()))
                    .orElse(false);
        }
        if (MyTasksService.MODULE_POLICY.equals(module) && MyTasksService.TYPE_POLICY_ACK.equals(type)) {
            return workTaskRepository
                    .findBySourceModuleAndSourceRecordTypeAndSourceRecordIdAndAssignedToUserId(
                            MyTasksService.MODULE_POLICY, MyTasksService.TYPE_POLICY_ACK, id, user.getId())
                    .isPresent();
        }
        if (MyTasksService.MODULE_RISK.equals(module) && MyTasksService.TYPE_RISK_MILESTONE.equals(type)) {
            return workTaskRepository
                    .findBySourceModuleAndSourceRecordTypeAndSourceRecordId(
                            MyTasksService.MODULE_RISK, MyTasksService.TYPE_RISK_MILESTONE, id)
                    .map(t -> user.getId().equals(t.getAssignedToUserId()))
                    .orElse(false);
        }
        if (MyTasksService.MODULE_INCIDENT.equals(module) && MyTasksService.TYPE_INCIDENT.equals(type)) {
            return incidentRecordRepository.findById(id)
                    .map(inc -> user.getId().equals(inc.getOwnerUserId())
                            || user.getId().equals(inc.getReporterUserId()))
                    .orElse(false);
        }
        if (MyTasksService.MODULE_PRIVACY.equals(module) && MyTasksService.TYPE_PRIVACY_ROPA.equals(type)) {
            return privacyRopaRepository.findById(id)
                    .map(r -> user.getId().equals(r.getAssignedContributorId()))
                    .orElse(false);
        }
        if (MyTasksService.MODULE_PRIVACY.equals(module) && MyTasksService.TYPE_PRIVACY_DPIA.equals(type)) {
            return privacyDpiaRepository.findById(id)
                    .map(d -> user.getId().equals(d.getAssignedContributorId()))
                    .orElse(false);
        }
        if (MyTasksService.MODULE_PRIVACY.equals(module)
                && com.a9.arbor.grc.modules.evidence.domain.PrivacyEvidenceStages.RECORD_DPIA_INSTANCE_QUESTION.equals(type)) {
            return dpiaInstanceQuestionRepository.findById(id)
                    .flatMap(q -> dpiaDepartmentRepository.findById(q.getDpiaDepartmentId()))
                    .map(d -> user.getId().equals(d.getChampionUserId()))
                    .orElse(false);
        }
        return false;
    }

    public void assertCanAccessLinkedRecord(User user, String moduleCode, String recordType, String recordId) {
        if (!RoleAccess.isContributorOnly(user)) {
            return;
        }
        if (!canAccessLinkedRecord(user, moduleCode, recordType, recordId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Not assigned to this record");
        }
    }

    public void assertCanAccessGapItem(User user, long gapItemId) {
        assertCanAccessLinkedRecord(user, MyTasksService.MODULE_GAP, MyTasksService.TYPE_GAP_ITEM,
                String.valueOf(gapItemId));
    }

    public void assertCanAccessCapa(User user, long capaId) {
        assertCanAccessLinkedRecord(user, MyTasksService.MODULE_CAPA, MyTasksService.TYPE_CAPA,
                String.valueOf(capaId));
    }

    public void assertCanAccessBia(User user, long biaId) {
        assertCanAccessLinkedRecord(user, MyTasksService.MODULE_BCMS, MyTasksService.TYPE_BIA,
                String.valueOf(biaId));
    }

    /** Gap item ids assigned to this Contributor (for evidence queries). */
    public List<String> assignedGapItemIds(User user) {
        return gapItemRepository.findByAssignedToUserId(user.getId()).stream()
                .filter(GapItem::isReleasedToContributor)
                .map(GapItem::getId)
                .map(String::valueOf)
                .toList();
    }

    /** Risk record ids visible to Contributor (assignee or risk owner). */
    public List<String> visibleRiskRecordIds(User user) {
        return riskRecordRepository.findIdsVisibleToContributor(user.getId()).stream()
                .map(String::valueOf)
                .toList();
    }

    /** CAPA ids owned by this Contributor. */
    public List<String> ownedCapaIds(User user) {
        return capaRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(c -> user.getId().equals(c.getOwnerUserId()))
                .map(Capa::getId)
                .map(String::valueOf)
                .toList();
    }
}
