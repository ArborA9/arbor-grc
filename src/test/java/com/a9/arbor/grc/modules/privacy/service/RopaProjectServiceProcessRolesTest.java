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
import com.a9.arbor.grc.modules.privacy.domain.RopaDesignation;
import com.a9.arbor.grc.modules.privacy.repo.PrivacyRopaRepository;
import com.a9.arbor.grc.modules.privacy.repo.RopaDepartmentRepository;
import com.a9.arbor.grc.modules.privacy.repo.RopaProjectRepository;
import com.a9.arbor.grc.modules.privacy.service.RopaProcessImportWorkbook;
import com.a9.arbor.grc.modules.privacy.web.dto.CreateRopaRequest;
import com.a9.arbor.grc.modules.privacy.web.dto.RopaProjectDtos;
import com.a9.arbor.grc.modules.task.repo.WorkTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RopaProjectServiceProcessRolesTest {

    private RopaDepartmentRepository departmentRepository;
    private PrivacyRopaRepository ropaRepository;
    private WorkTaskRepository workTaskRepository;
    private RopaProjectService service;

    private User admin;
    private User champion;
    private RopaDepartment department;

    @BeforeEach
    void setUp() {
        departmentRepository = mock(RopaDepartmentRepository.class);
        ropaRepository = mock(PrivacyRopaRepository.class);
        workTaskRepository = mock(WorkTaskRepository.class);
        service = new RopaProjectService(mock(RopaProjectRepository.class), departmentRepository,
                ropaRepository, mock(OrgUnitRepository.class), mock(UserRepository.class),
                workTaskRepository, mock(PrivacyRopaService.class), mock(PrivacyDpiaService.class),
                mock(NotificationService.class), mock(RopaProcessImportWorkbook.class));

        admin = new User();
        admin.setId(1L);
        admin.setRoles(Set.of(Role.ADMIN));
        champion = new User();
        champion.setId(5L);
        champion.setRoles(Set.of(Role.CONTRIBUTOR));

        department = new RopaDepartment();
        department.setId(7L);
        department.setRopaProjectId(10L);
        department.setOrgUnitId(20L);
        department.setChampionUserId(5L);
        department.setStatus(RopaDepartmentStatus.IN_PROGRESS);
        department.setWorkTaskId(99L);
        when(departmentRepository.findById(7L)).thenReturn(Optional.of(department));
        when(departmentRepository.save(any(RopaDepartment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CreateRopaRequest minimalRequest() {
        return new CreateRopaRequest(
                "Activity", null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                RopaDesignation.CONTROLLER, null, null, null, null, null, null);
    }

    @Test
    void adminCannotAddProcess() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.createProcess(admin, 7L, minimalRequest()));
        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Privacy Champion"));
    }

    @Test
    void adminCannotSubmitDepartment() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.submitDepartment(admin, 7L));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void adminCannotSignOffDepartment() {
        department.setStatus(RopaDepartmentStatus.PENDING_HEAD_SIGN_OFF);
        department.setDepartmentHeadUserId(9L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.headApprove(admin, 7L, new RopaProjectDtos.DecideRequest("ok")));
        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Department Head"));
    }

    @Test
    void departmentHeadCanSignOff() {
        department.setStatus(RopaDepartmentStatus.PENDING_HEAD_SIGN_OFF);
        department.setDepartmentHeadUserId(9L);
        User head = new User();
        head.setId(9L);
        head.setRoles(Set.of(Role.CONTRIBUTOR));
        PrivacyRopa approved = new PrivacyRopa();
        approved.setActivityName("ACT-1");
        approved.setHeadOutcome(RopaHeadOutcome.APPROVED);
        when(ropaRepository.findByRopaDepartmentIdOrderByCreatedAtAsc(7L)).thenReturn(List.of(approved));

        RopaProjectDtos.DepartmentResponse res =
                service.headApprove(head, 7L, new RopaProjectDtos.DecideRequest("ok"));

        assertEquals(RopaDepartmentStatus.APPROVED, res.status());
    }

    @Test
    void championCannotSubmitWhileDraftProcessesExist() {
        PrivacyRopa draft = new PrivacyRopa();
        draft.setActivityName("Half done");
        draft.setDraft(true);
        when(ropaRepository.findByRopaDepartmentIdOrderByCreatedAtAsc(7L)).thenReturn(List.of(draft));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.submitDepartment(champion, 7L));
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("draft"));
    }

    @Test
    void championCanSubmitDepartment() {
        PrivacyRopa process = new PrivacyRopa();
        when(ropaRepository.findByRopaDepartmentIdOrderByCreatedAtAsc(7L)).thenReturn(List.of(process));
        when(workTaskRepository.findById(99L)).thenReturn(Optional.empty());

        RopaProjectDtos.DepartmentResponse res = service.submitDepartment(champion, 7L);

        assertEquals(RopaDepartmentStatus.UNDER_REVIEW, res.status());
    }
}
