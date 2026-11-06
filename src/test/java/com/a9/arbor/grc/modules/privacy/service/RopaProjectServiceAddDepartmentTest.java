package com.a9.arbor.grc.modules.privacy.service;

import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.iam.repo.UserRepository;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.org.domain.OrgUnit;
import com.a9.arbor.grc.modules.org.repo.OrgUnitRepository;
import com.a9.arbor.grc.modules.privacy.domain.RopaDepartment;
import com.a9.arbor.grc.modules.privacy.domain.RopaDepartmentStatus;
import com.a9.arbor.grc.modules.privacy.domain.RopaProject;
import com.a9.arbor.grc.modules.privacy.domain.RopaProjectStatus;
import com.a9.arbor.grc.modules.privacy.repo.PrivacyRopaRepository;
import com.a9.arbor.grc.modules.privacy.repo.RopaDepartmentRepository;
import com.a9.arbor.grc.modules.privacy.repo.RopaProjectRepository;
import com.a9.arbor.grc.modules.privacy.service.RopaProcessImportWorkbook;
import com.a9.arbor.grc.modules.privacy.web.dto.RopaProjectDtos;
import com.a9.arbor.grc.modules.task.domain.WorkTask;
import com.a9.arbor.grc.modules.task.repo.WorkTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RopaProjectServiceAddDepartmentTest {

    private RopaProjectRepository projectRepository;
    private RopaDepartmentRepository departmentRepository;
    private PrivacyRopaRepository ropaRepository;
    private OrgUnitRepository orgUnitRepository;
    private UserRepository userRepository;
    private WorkTaskRepository workTaskRepository;
    private NotificationService notificationService;
    private RopaProjectService service;

    private User admin;
    private RopaProject project;

    @BeforeEach
    void setUp() {
        projectRepository = mock(RopaProjectRepository.class);
        departmentRepository = mock(RopaDepartmentRepository.class);
        ropaRepository = mock(PrivacyRopaRepository.class);
        orgUnitRepository = mock(OrgUnitRepository.class);
        userRepository = mock(UserRepository.class);
        workTaskRepository = mock(WorkTaskRepository.class);
        notificationService = mock(NotificationService.class);
        service = new RopaProjectService(projectRepository, departmentRepository, ropaRepository,
                orgUnitRepository, userRepository, workTaskRepository,
                mock(PrivacyRopaService.class), mock(PrivacyDpiaService.class), notificationService,
                mock(RopaProcessImportWorkbook.class));

        admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@example.com");
        admin.setRoles(Set.of(Role.ADMIN));

        project = new RopaProject();
        project.setId(10L);
        project.setName("ROPA project for 2026");
        project.setStatus(RopaProjectStatus.DRAFT);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any(RopaProject.class))).thenAnswer(inv -> inv.getArgument(0));

        OrgUnit unit = new OrgUnit();
        unit.setId(20L);
        unit.setName("FINANCE");
        when(orgUnitRepository.findById(20L)).thenReturn(Optional.of(unit));
        when(departmentRepository.existsByRopaProjectIdAndOrgUnitId(10L, 20L)).thenReturn(false);
        when(departmentRepository.save(any(RopaDepartment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ropaRepository.findByRopaDepartmentIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(workTaskRepository.findBySourceModuleAndSourceRecordTypeAndSourceRecordId(
                anyString(), anyString(), any())).thenReturn(Optional.empty());
        when(workTaskRepository.save(any(WorkTask.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private User user(long id, String name, Role role) {
        User u = new User();
        u.setId(id);
        u.setFullName(name);
        u.setEmail(name.replace(' ', '.') + "@example.com");
        u.setRoles(role == null ? Set.of() : Set.of(role));
        return u;
    }

    @Test
    void addWithHeadAndChampionStartsInProgressWithTaskAndNotification() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(user(5L, "Champion", Role.CONTRIBUTOR)));
        when(userRepository.findById(6L)).thenReturn(Optional.of(user(6L, "Head", null)));

        RopaProjectDtos.DepartmentResponse res = service.addDepartment(admin, 10L,
                new RopaProjectDtos.AddDepartmentRequest(20L, 6L, 5L));

        assertEquals(RopaDepartmentStatus.IN_PROGRESS, res.status());
        assertEquals(5L, res.championUserId());
        assertEquals(6L, res.departmentHeadUserId());
        assertEquals(RopaProjectStatus.IN_PROGRESS, project.getStatus());
        verify(workTaskRepository).save(any(WorkTask.class));
        verify(notificationService).trigger(eq("ROPA_ASSIGNED"), anyString(), eq(List.of("5")), any());
    }

    @Test
    void addWithoutChampionRejected() {
        assertThrows(ResponseStatusException.class, () ->
                service.addDepartment(admin, 10L, new RopaProjectDtos.AddDepartmentRequest(20L, 6L, null)));
        verify(workTaskRepository, never()).save(any(WorkTask.class));
        verify(notificationService, never()).trigger(anyString(), anyString(), any(), any());
    }
}
