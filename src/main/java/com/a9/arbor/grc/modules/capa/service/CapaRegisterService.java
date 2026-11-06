package com.a9.arbor.grc.modules.capa.service;

import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAudit;
import com.a9.arbor.grc.modules.auditmgmt.domain.GrcAuditStatus;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditFindingRepository;
import com.a9.arbor.grc.modules.auditmgmt.repo.GrcAuditRepository;
import com.a9.arbor.grc.modules.capa.domain.GrcCapaRegister;
import com.a9.arbor.grc.modules.capa.repo.CapaRepository;
import com.a9.arbor.grc.modules.capa.repo.GrcCapaRegisterRepository;
import com.a9.arbor.grc.modules.capa.web.dto.CapaAvailableFindingResponse;
import com.a9.arbor.grc.modules.capa.web.dto.CapaRegisterResponse;
import com.a9.arbor.grc.modules.capa.web.dto.CreateCapaRegisterRequest;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Year;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CapaRegisterService {

    private static final Set<GrcAuditStatus> FINDING_AUDIT_STATUSES = EnumSet.complementOf(
            EnumSet.of(GrcAuditStatus.CANCELLED));

    private final GrcCapaRegisterRepository registerRepository;
    private final CapaRepository capaRepository;
    private final GrcAuditRepository auditRepository;
    private final GrcAuditFindingRepository findingRepository;
    private final AuditTrailService auditTrailService;

    public CapaRegisterService(GrcCapaRegisterRepository registerRepository,
                               CapaRepository capaRepository,
                               GrcAuditRepository auditRepository,
                               GrcAuditFindingRepository findingRepository,
                               AuditTrailService auditTrailService) {
        this.registerRepository = registerRepository;
        this.capaRepository = capaRepository;
        this.auditRepository = auditRepository;
        this.findingRepository = findingRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional(readOnly = true)
    public List<CapaRegisterResponse> list(User viewer) {
        assertCanView(viewer);
        List<GrcCapaRegister> rows = registerRepository.findAllByOrderByRegisterYearDescIdDesc();
        Map<Long, GrcAudit> audits = auditsById(rows.stream().map(GrcCapaRegister::getAuditId).toList());
        return rows.stream()
                .map(r -> CapaRegisterResponse.from(
                        r,
                        r.getAuditId() != null ? audits.get(r.getAuditId()) : null,
                        (int) capaRepository.countByCapaRegisterId(r.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CapaRegisterResponse get(User viewer, long id) {
        assertCanView(viewer);
        GrcCapaRegister r = require(id);
        GrcAudit audit = r.getAuditId() != null ? auditRepository.findById(r.getAuditId()).orElse(null) : null;
        return CapaRegisterResponse.from(r, audit, (int) capaRepository.countByCapaRegisterId(r.getId()));
    }

    @Transactional(readOnly = true)
    public List<CapaAvailableFindingResponse> listAvailableFindings(User viewer) {
        assertCanView(viewer);
        Map<Long, GrcAudit> audits = auditRepository.findAll().stream()
                .filter(a -> FINDING_AUDIT_STATUSES.contains(a.getStatus()))
                .collect(Collectors.toMap(GrcAudit::getId, a -> a, (a, b) -> a));
        return findingRepository.findByCapaIdIsNullOrderByDisplayCodeAsc().stream()
                .filter(f -> audits.containsKey(f.getAuditId()))
                .map(f -> CapaAvailableFindingResponse.from(f, audits.get(f.getAuditId())))
                .toList();
    }

    @Transactional
    public CapaRegisterResponse create(User admin, CreateCapaRegisterRequest req) {
        assertAdmin(admin);
        int year = req.year();
        String name = req.name().trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }
        if (registerRepository.existsByRegisterYear(year)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A CAPA register for " + year + " already exists");
        }
        GrcCapaRegister r = newRegister(admin.getId(), year, name);
        registerRepository.save(r);
        trail(admin, AuditActionType.CREATED, r, name);
        return CapaRegisterResponse.from(r, null, 0);
    }

    @Transactional
    public GrcCapaRegister requireOrCreateForAudit(User actor, GrcAudit audit) {
        int year = yearOf(audit);
        return registerRepository.findFirstByRegisterYearOrderByIdAsc(year)
                .orElseGet(() -> registerRepository.save(
                        newRegister(actor.getId(), year, "CAPA Register " + year)));
    }

    public GrcCapaRegister require(long id) {
        return registerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CAPA register not found"));
    }

    private static GrcCapaRegister newRegister(long createdByUserId, int year, String name) {
        GrcCapaRegister r = new GrcCapaRegister();
        r.setRegisterYear(year);
        r.setName(name);
        r.setConductedBy("Admin");
        r.setDescription(name);
        r.setCreatedByUserId(createdByUserId);
        return r;
    }

    private static int yearOf(GrcAudit audit) {
        if (audit.getEndDate() != null) {
            return audit.getEndDate().getYear();
        }
        if (audit.getStartDate() != null) {
            return audit.getStartDate().getYear();
        }
        return Year.now().getValue();
    }

    private Map<Long, GrcAudit> auditsById(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        return auditRepository.findAllById(distinct).stream()
                .collect(Collectors.toMap(GrcAudit::getId, a -> a));
    }

    private static void assertAdmin(User user) {
        if (user.getRoles().stream().noneMatch(r -> r == Role.OWNER || r == Role.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    private static void assertCanView(User user) {
        if (user.getRoles().stream().anyMatch(r ->
                r == Role.OWNER || r == Role.ADMIN || r == Role.GLOBAL_READER
                        || r == Role.INTERNAL_AUDITOR || r == Role.EXTERNAL_AUDITOR || r == Role.CONTRIBUTOR)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "CAPA not available");
    }

    private void trail(User user, AuditActionType action, GrcCapaRegister r, String label) {
        String roles = user.getRoles().stream().map(Enum::name).sorted().reduce((a, b) -> a + "," + b).orElse("");
        auditTrailService.record(
                action, "CAPA", user.getId(), roles,
                "CAPA_REGISTER", String.valueOf(r.getId()), label, null, null, null, null, null);
    }
}
