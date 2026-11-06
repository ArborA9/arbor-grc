package com.a9.arbor.grc.modules.privacy.service;

import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.org.repo.OrgUnitRepository;
import com.a9.arbor.grc.modules.privacy.domain.PrivacyRopa;
import com.a9.arbor.grc.modules.privacy.domain.RopaDepartment;
import com.a9.arbor.grc.modules.privacy.domain.RopaDepartmentStatus;
import com.a9.arbor.grc.modules.privacy.domain.RopaHeadOutcome;
import com.a9.arbor.grc.modules.privacy.domain.RopaReviewOutcome;
import com.a9.arbor.grc.modules.privacy.repo.PrivacyRopaRepository;
import com.a9.arbor.grc.modules.privacy.repo.RopaDepartmentRepository;
import com.a9.arbor.grc.modules.privacy.repo.RopaProjectRepository;
import com.a9.arbor.grc.modules.privacy.service.RopaProcessImportWorkbook;
import com.a9.arbor.grc.modules.privacy.web.dto.RopaProjectDtos;
import com.a9.arbor.grc.modules.privacy.web.dto.RopaResponse;
import com.a9.arbor.grc.modules.privacy.web.dto.UpdateRopaRequest;
import com.a9.arbor.grc.modules.task.repo.WorkTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RopaProjectServiceHeadReviewTest {

    private RopaDepartmentRepository departmentRepository;
    private PrivacyRopaRepository ropaRepository;
    private RopaProjectService service;

    private User head;
    private User champion;
    private User admin;
    private RopaDepartment department;
    private PrivacyRopa processA;
    private PrivacyRopa processB;

    @BeforeEach
    void setUp() {
        departmentRepository = mock(RopaDepartmentRepository.class);
        ropaRepository = mock(PrivacyRopaRepository.class);
        WorkTaskRepository workTaskRepository = mock(WorkTaskRepository.class);
        service = new RopaProjectService(mock(RopaProjectRepository.class), departmentRepository,
                ropaRepository, mock(OrgUnitRepository.class), mock(UserRepository.class),
                workTaskRepository, mock(PrivacyRopaService.class), mock(PrivacyDpiaService.class),
                mock(NotificationService.class), mock(RopaProcessImportWorkbook.class));

        head = user(9L, Role.CONTRIBUTOR);
        champion = user(5L, Role.CONTRIBUTOR);
        admin = user(1L, Role.ADMIN);

        department = new RopaDepartment();
        department.setId(7L);
        department.setRopaProjectId(10L);
        department.setOrgUnitId(20L);
        department.setChampionUserId(5L);
        department.setDepartmentHeadUserId(9L);
        department.setStatus(RopaDepartmentStatus.PENDING_HEAD_SIGN_OFF);
        department.setWorkTaskId(99L);
        department.setHeadWorkTaskId(100L);
        when(departmentRepository.findById(7L)).thenReturn(Optional.of(department));
        when(departmentRepository.save(any(RopaDepartment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(workTaskRepository.findById(anyLong())).thenReturn(Optional.empty());

        processA = process(11L, "ACT-1");
        processB = process(12L, "ACT-2");
        when(ropaRepository.findById(11L)).thenReturn(Optional.of(processA));
        when(ropaRepository.findById(12L)).thenReturn(Optional.of(processB));
        when(ropaRepository.findByRopaDepartmentIdOrderByCreatedAtAsc(7L))
                .thenReturn(List.of(processA, processB));
        when(ropaRepository.save(any(PrivacyRopa.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static User user(long id, Role role) {
        User u = new User();
        u.setId(id);
        u.setRoles(Set.of(role));
        return u;
    }

    private static PrivacyRopa process(long id, String name) {
        PrivacyRopa row = new PrivacyRopa();
        row.setId(id);
        row.setActivityName(name);
        row.setRopaDepartmentId(7L);
        row.setReviewOutcome(RopaReviewOutcome.REVIEWED);
        row.setHeadOutcome(RopaHeadOutcome.PENDING);
        return row;
    }

    private static RopaProjectDtos.HeadReviewProcessRequest decide(String outcome, String comments) {
        return new RopaProjectDtos.HeadReviewProcessRequest(outcome, comments);
    }

    @Test
    void headCanApproveOneProcess() {
        RopaResponse res = service.headReviewProcess(head, 7L, 11L, decide("APPROVED", null));
        assertEquals(RopaHeadOutcome.APPROVED, res.headOutcome());
        assertEquals(RopaHeadOutcome.PENDING, processB.getHeadOutcome());
        assertEquals(RopaDepartmentStatus.PENDING_HEAD_SIGN_OFF, department.getStatus());
    }

    @Test
    void headReturnNeedsComments() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.headReviewProcess(head, 7L, 11L, decide("RETURNED", " ")));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void onlyAssignedHeadCanReviewProcesses() {
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.headReviewProcess(champion, 7L, 11L, decide("APPROVED", null)))
                .getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.headReviewProcess(admin, 7L, 11L, decide("APPROVED", null)))
                .getStatusCode().value());
    }

    @Test
    void finalApproveBlockedUntilEveryProcessApproved() {
        service.headReviewProcess(head, 7L, 11L, decide("APPROVED", null));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.headApprove(head, 7L, new RopaProjectDtos.DecideRequest(null)));
        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("ACT-2"));

        service.headReviewProcess(head, 7L, 12L, decide("APPROVED", null));
        RopaProjectDtos.DepartmentResponse res =
                service.headApprove(head, 7L, new RopaProjectDtos.DecideRequest("ok"));
        assertEquals(RopaDepartmentStatus.APPROVED, res.status());
    }

    @Test
    void finalReturnBlockedWhileProcessesUndecided() {
        service.headReviewProcess(head, 7L, 11L, decide("RETURNED", "Fix the retention period"));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.headReturn(head, 7L, new RopaProjectDtos.DecideRequest(null)));
        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("ACT-2"));
    }

    @Test
    void mixedDecisionsReturnOnlyTheReturnedProcessesToChampion() {
        service.headReviewProcess(head, 7L, 11L, decide("APPROVED", null));
        service.headReviewProcess(head, 7L, 12L, decide("RETURNED", "Fix the retention period"));

        RopaProjectDtos.DepartmentResponse res =
                service.headReturn(head, 7L, new RopaProjectDtos.DecideRequest(null));

        assertEquals(RopaDepartmentStatus.IN_PROGRESS, res.status());
        assertEquals("HEAD", res.returnFrom());
        assertEquals(RopaHeadOutcome.APPROVED, processA.getHeadOutcome());
        assertEquals(RopaHeadOutcome.RETURNED, processB.getHeadOutcome());
    }

    @Test
    void championCanEditAHeadReturnedProcessDespiteDpoReview() {
        department.setStatus(RopaDepartmentStatus.IN_PROGRESS);
        processB.setHeadOutcome(RopaHeadOutcome.RETURNED);
        // Partial rename as draft (full Save Process still requires every champion field).
        UpdateRopaRequest req = new UpdateRopaRequest(
                "ACT-2 updated", null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, true);
        assertDoesNotThrow(() -> service.updateProcess(champion, 7L, 12L, req));
    }

    @Test
    void championResubmitAfterHeadReturnGoesStraightBackToHead() {
        department.setStatus(RopaDepartmentStatus.IN_PROGRESS);
        department.setReturnFrom("HEAD");
        processA.setHeadOutcome(RopaHeadOutcome.APPROVED);
        processB.setHeadOutcome(RopaHeadOutcome.RETURNED);

        RopaProjectDtos.DepartmentResponse res = service.submitDepartment(champion, 7L);

        assertEquals(RopaDepartmentStatus.PENDING_HEAD_SIGN_OFF, res.status());
        assertEquals(RopaHeadOutcome.APPROVED, processA.getHeadOutcome());
        assertEquals(RopaHeadOutcome.PENDING, processB.getHeadOutcome());
    }

    @Test
    void championResubmitWithANewProcessGoesViaDpoFirst() {
        department.setStatus(RopaDepartmentStatus.IN_PROGRESS);
        department.setReturnFrom("HEAD");
        processA.setHeadOutcome(RopaHeadOutcome.APPROVED);
        processB.setHeadOutcome(RopaHeadOutcome.RETURNED);
        processB.setReviewOutcome(RopaReviewOutcome.OPEN);

        RopaProjectDtos.DepartmentResponse res = service.submitDepartment(champion, 7L);

        assertEquals(RopaDepartmentStatus.UNDER_REVIEW, res.status());
    }
}
