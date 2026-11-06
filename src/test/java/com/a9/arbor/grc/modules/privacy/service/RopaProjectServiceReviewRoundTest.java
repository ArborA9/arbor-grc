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
import com.a9.arbor.grc.modules.task.repo.WorkTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RopaProjectServiceReviewRoundTest {

    private PrivacyRopaRepository ropaRepository;
    private NotificationService notificationService;
    private RopaProjectService service;

    private User admin;
    private User champion;
    private User head;
    private RopaDepartment department;
    private PrivacyRopa processA;
    private PrivacyRopa processB;

    @BeforeEach
    void setUp() {
        RopaDepartmentRepository departmentRepository = mock(RopaDepartmentRepository.class);
        ropaRepository = mock(PrivacyRopaRepository.class);
        WorkTaskRepository workTaskRepository = mock(WorkTaskRepository.class);
        notificationService = mock(NotificationService.class);
        service = new RopaProjectService(mock(RopaProjectRepository.class), departmentRepository,
                ropaRepository, mock(OrgUnitRepository.class), mock(UserRepository.class),
                workTaskRepository, mock(PrivacyRopaService.class), mock(PrivacyDpiaService.class),
                notificationService, mock(RopaProcessImportWorkbook.class));

        admin = user(1L, Role.ADMIN);
        champion = user(5L, Role.CONTRIBUTOR);
        head = user(9L, Role.CONTRIBUTOR);

        department = new RopaDepartment();
        department.setId(7L);
        department.setRopaProjectId(10L);
        department.setOrgUnitId(20L);
        department.setChampionUserId(5L);
        department.setDepartmentHeadUserId(9L);
        department.setStatus(RopaDepartmentStatus.UNDER_REVIEW);
        department.setWorkTaskId(99L);
        department.setHeadWorkTaskId(100L);
        when(departmentRepository.findById(7L)).thenReturn(Optional.of(department));
        when(departmentRepository.save(any(RopaDepartment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(workTaskRepository.findById(anyLong())).thenReturn(Optional.empty());

        processA = process(11L, "ACT-1", RopaReviewOutcome.REVIEWED);
        processB = process(12L, "ACT-2", RopaReviewOutcome.OPEN);
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

    private static PrivacyRopa process(long id, String name, RopaReviewOutcome outcome) {
        PrivacyRopa row = new PrivacyRopa();
        row.setId(id);
        row.setActivityName(name);
        row.setRopaDepartmentId(7L);
        row.setReviewOutcome(outcome);
        row.setHeadOutcome(RopaHeadOutcome.PENDING);
        return row;
    }

    @Test
    void dpoReturnDoesNotBounceTheDepartmentMidRound() {
        service.reviewProcess(admin, 7L, 12L,
                new RopaProjectDtos.ReviewProcessRequest(
                        false, null, "Clarify retention", "RETURNED", null, null, null, null));
        assertEquals(RopaReviewOutcome.RETURNED, processB.getReviewOutcome());
        assertEquals(RopaDepartmentStatus.UNDER_REVIEW, department.getStatus());
    }

    @Test
    void finishReviewBlockedWhileProcessesUndecided() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.finishReview(admin, 7L));
        assertEquals(409, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("ACT-2"));
    }

    @Test
    void finishReviewReleasesReturnedProcessesToChampion() {
        processB.setReviewOutcome(RopaReviewOutcome.RETURNED);
        RopaProjectDtos.DepartmentResponse res = service.finishReview(admin, 7L);
        assertEquals(RopaDepartmentStatus.IN_PROGRESS, res.status());
        assertEquals("ADMIN", res.returnFrom());
        assertTrue(res.returnReason().contains("ACT-2"));
    }

    @Test
    void finishReviewAllReviewedGoesBackToChampionForHeadSignOff() {
        processB.setReviewOutcome(RopaReviewOutcome.REVIEWED);
        RopaProjectDtos.DepartmentResponse res = service.finishReview(admin, 7L);
        assertEquals(RopaDepartmentStatus.READY_FOR_HEAD_SIGN_OFF, res.status());
    }

    @Test
    void championSendsToHeadAfterFinish() {
        department.setStatus(RopaDepartmentStatus.READY_FOR_HEAD_SIGN_OFF);
        processB.setReviewOutcome(RopaReviewOutcome.REVIEWED);
        RopaProjectDtos.DepartmentResponse res = service.sendToHead(champion, 7L);
        assertEquals(RopaDepartmentStatus.PENDING_HEAD_SIGN_OFF, res.status());
    }

    @Test
    void adminCannotSendToHeadAnymore() {
        department.setStatus(RopaDepartmentStatus.READY_FOR_HEAD_SIGN_OFF);
        processB.setReviewOutcome(RopaReviewOutcome.REVIEWED);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.sendToHead(admin, 7L));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void championSendBlockedBeforeDpoFinishes() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.sendToHead(champion, 7L));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    @SuppressWarnings("unchecked")
    void finalHeadApprovalNotifiesAdminsReadyToClose() {
        department.setStatus(RopaDepartmentStatus.PENDING_HEAD_SIGN_OFF);
        processA.setHeadOutcome(RopaHeadOutcome.APPROVED);
        processB.setHeadOutcome(RopaHeadOutcome.APPROVED);
        service.headApprove(head, 7L, new RopaProjectDtos.DecideRequest("ok"));
        verify(notificationService).trigger(eq("ROPA_READY_TO_CLOSE"), eq("7"),
                eq(List.of("OWNER", "ADMIN")), any(Map.class));
    }
}
