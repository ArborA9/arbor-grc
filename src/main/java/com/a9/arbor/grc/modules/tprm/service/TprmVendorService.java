package com.a9.arbor.grc.modules.tprm.service;

import com.a9.arbor.grc.common.validation.EmailRules;
import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.tprm.domain.AssessmentStatus;
import com.a9.arbor.grc.modules.tprm.domain.ContactRole;
import com.a9.arbor.grc.modules.tprm.domain.TprmAnswer;
import com.a9.arbor.grc.modules.tprm.domain.TprmAssessment;
import com.a9.arbor.grc.modules.tprm.domain.TprmCertType;
import com.a9.arbor.grc.modules.tprm.domain.TprmCompliance;
import com.a9.arbor.grc.modules.tprm.domain.TprmCriticality;
import com.a9.arbor.grc.modules.tprm.domain.TprmVendor;
import com.a9.arbor.grc.modules.tprm.domain.TprmVendorCert;
import com.a9.arbor.grc.modules.tprm.domain.TprmVendorContact;
import com.a9.arbor.grc.modules.tprm.domain.TprmVerdict;
import com.a9.arbor.grc.modules.tprm.domain.VendorLifecycleState;
import com.a9.arbor.grc.modules.tprm.domain.VendorStatus;
import com.a9.arbor.grc.modules.tprm.domain.VendorType;
import com.a9.arbor.grc.modules.tprm.pack.TprmCategorisation;
import com.a9.arbor.grc.modules.tprm.pack.TprmLookups;
import com.a9.arbor.grc.modules.tprm.pack.TprmPhoneRules;
import com.a9.arbor.grc.modules.tprm.repo.TprmAssessmentRepository;
import com.a9.arbor.grc.modules.tprm.repo.TprmVendorCertRepository;
import com.a9.arbor.grc.modules.tprm.repo.TprmVendorContactRepository;
import com.a9.arbor.grc.modules.iam.service.UserService;
import com.a9.arbor.grc.modules.tprm.repo.TprmVendorRepository;
import com.a9.arbor.grc.modules.tprm.web.dto.TprmKpiResponse;
import com.a9.arbor.grc.modules.tprm.web.dto.TprmLookupsResponse;
import com.a9.arbor.grc.modules.tprm.web.dto.UpsertVendorRequest;
import com.a9.arbor.grc.modules.tprm.web.dto.VendorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class TprmVendorService {

    private static final Set<AssessmentStatus> FREEZE = Set.of(
            AssessmentStatus.ISSUED, AssessmentStatus.IN_PROGRESS, AssessmentStatus.SUBMITTED,
            AssessmentStatus.IN_REVIEW, AssessmentStatus.RETURNED, AssessmentStatus.COMPLETE);

    static final String DETAILS_LOCKED_MESSAGE =
            "Vendor details are locked after the project is assigned or closed";

    static final String ORG_OWNER_LABEL = "Organization Project Owner";

    private final TprmVendorRepository vendorRepository;
    private final TprmVendorContactRepository contactRepository;
    private final TprmVendorCertRepository certRepository;
    private final TprmAssessmentRepository assessmentRepository;
    private final TprmControlLibrarySeeder seeder;
    private final AuditTrailService auditTrailService;
    private final UserService userService;

    public TprmVendorService(TprmVendorRepository vendorRepository,
                             TprmVendorContactRepository contactRepository,
                             TprmVendorCertRepository certRepository,
                             TprmAssessmentRepository assessmentRepository,
                             TprmControlLibrarySeeder seeder,
                             AuditTrailService auditTrailService,
                             UserService userService) {
        this.vendorRepository = vendorRepository;
        this.contactRepository = contactRepository;
        this.certRepository = certRepository;
        this.assessmentRepository = assessmentRepository;
        this.seeder = seeder;
        this.auditTrailService = auditTrailService;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public TprmLookupsResponse lookups() {
        seeder.seedIfEmpty();
        List<TprmLookupsResponse.NamedValue> types = new ArrayList<>();
        for (VendorType t : VendorType.values()) {
            types.add(new TprmLookupsResponse.NamedValue(t.name(), t.label()));
        }
        Map<String, List<String>> factors = new LinkedHashMap<>();
        factors.put("f1", TprmLookups.options(TprmLookups.F1));
        factors.put("f2", TprmLookups.options(TprmLookups.F2));
        factors.put("f3", TprmLookups.options(TprmLookups.F3));
        factors.put("f4", TprmLookups.options(TprmLookups.F4));
        factors.put("f5", TprmLookups.options(TprmLookups.F5));
        factors.put("f6", TprmLookups.options(TprmLookups.F6));
        List<TprmLookupsResponse.NamedValue> certs = new ArrayList<>();
        for (TprmCertType c : TprmCertType.values()) {
            certs.add(new TprmLookupsResponse.NamedValue(c.name(), c.label()));
        }
        List<String> answers = new ArrayList<>();
        for (TprmAnswer a : TprmAnswer.values()) {
            answers.add(a.name());
        }
        List<String> verdicts = new ArrayList<>();
        for (TprmVerdict v : TprmVerdict.values()) {
            if (v != TprmVerdict.DOWNGRADED) {
                verdicts.add(v.name());
            }
        }
        return new TprmLookupsResponse(types, factors, certs, answers, verdicts);
    }

    @Transactional(readOnly = true)
    public TprmKpiResponse kpis() {
        seeder.seedIfEmpty();
        List<TprmVendor> vendors = vendorRepository.findAllByOrderByNameAsc();
        LocalDate today = LocalDate.now();
        long total = vendors.size();
        long tier1 = vendors.stream().filter(v -> "TIER_1".equals(v.getTier())).count();
        long tier2 = vendors.stream().filter(v -> "TIER_2".equals(v.getTier())).count();
        long tier3 = vendors.stream().filter(v -> "TIER_3".equals(v.getTier())).count();
        long tier4 = vendors.stream().filter(v -> "TIER_4".equals(v.getTier())).count();
        long critical = vendors.stream().filter(v -> v.getCriticality() == TprmCriticality.CRITICAL).count();
        long nonCritical = vendors.stream().filter(v -> v.getCriticality() == TprmCriticality.NON_CRITICAL).count();
        long compliant = vendors.stream().filter(v -> v.getComplianceStatus() == TprmCompliance.COMPLIANT).count();
        long notCompliant = vendors.stream().filter(v -> v.getComplianceStatus() == TprmCompliance.NOT_COMPLIANT).count();
        long issued = vendors.stream().filter(v -> v.getLifecycleState() == VendorLifecycleState.ISSUED
                || v.getLifecycleState() == VendorLifecycleState.RETURNED).count();
        long submitted = vendors.stream().filter(v -> v.getLifecycleState() == VendorLifecycleState.SUBMITTED
                || v.getLifecycleState() == VendorLifecycleState.IN_REVIEW).count();
        long due = vendors.stream()
                .filter(v -> v.getNextReviewDate() != null && !v.getNextReviewDate().isAfter(today))
                .count();
        return new TprmKpiResponse(total, tier1, tier2, tier3, tier4, critical, nonCritical, compliant, notCompliant,
                issued, submitted, due);
    }

    @Transactional(readOnly = true)
    public List<VendorResponse> list() {
        seeder.seedIfEmpty();
        return vendorRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public VendorResponse get(long id) {
        seeder.seedIfEmpty();
        return toResponse(requireVendor(id));
    }

    @Transactional
    public VendorResponse create(User admin, UpsertVendorRequest req) {
        requireAdmin(admin);
        seeder.seedIfEmpty();
        String name = requireUniqueName(req.name(), null);
        TprmVendor v = new TprmVendor();
        v.setStatus(VendorStatus.ACTIVE);
        v.setCreatedByUserId(admin.getId());
        applyBody(v, req, false, name);
        v = vendorRepository.save(v);
        replaceContacts(v.getId(), req.contacts());
        replaceCerts(v.getId(), req.certs());
        applyCategorisation(v, false);
        v = vendorRepository.save(v);

        auditTrailService.record(AuditActionType.CREATED, "TPRM", admin.getId(),
                adminRoles(admin), "TPRM_VENDOR", String.valueOf(v.getId()),
                v.getName(), null, null, null, null, null);
        return toResponse(v);
    }

    @Transactional
    public VendorResponse update(User admin, long id, UpsertVendorRequest req) {
        requireAdmin(admin);
        TprmVendor v = requireVendor(id);
        if (detailsLocked(v)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, DETAILS_LOCKED_MESSAGE);
        }
        String name = requireUniqueName(req.name(), id);
        applyBody(v, req, false, name);
        replaceContacts(v.getId(), req.contacts());
        replaceCerts(v.getId(), req.certs());
        applyCategorisation(v, false);
        v.setUpdatedAt(Instant.now());
        v = vendorRepository.save(v);

        auditTrailService.record(AuditActionType.UPDATED, "TPRM", admin.getId(),
                adminRoles(admin), "TPRM_VENDOR", String.valueOf(v.getId()),
                v.getName(), null, null, null, null, null);
        return toResponse(v);
    }

    void applyCategorisation(TprmVendor vendor, boolean frozen) {
        if (frozen) {
            refreshLifecycleFromProfile(vendor);
            return;
        }
        if (!factorsComplete(vendor)) {
            vendor.setF1Score(null);
            vendor.setF2Score(null);
            vendor.setF3Score(null);
            vendor.setF4Score(null);
            vendor.setF5Score(null);
            vendor.setF6Score(null);
            vendor.setInherentScore(null);
            vendor.setAssuranceCredit(null);
            vendor.setAdjustedScore(null);
            vendor.setTier(null);
            vendor.setCriticality(null);
            vendor.setPostureTarget(null);
            vendor.setReassessmentMonths(null);
            vendor.setElevationFlag(false);
            refreshLifecycleFromProfile(vendor);
            return;
        }
        List<TprmCertType> certs = certRepository.findByVendorIdOrderByIdAsc(vendor.getId()).stream()
                .map(TprmVendorCert::getCertType)
                .toList();
        String override = blankToNull(vendor.getOverrideTier());
        if (override != null) {
            TprmCategorisation.Result preview = TprmCategorisation.calculate(new TprmCategorisation.Input(
                    vendor.getF1(), vendor.getF2(), vendor.getF3(), vendor.getF4(), vendor.getF5(), vendor.getF6(),
                    certs, null));
            String allowed = TprmLookups.nextTierUp(preview.calculatedTier());
            if (!allowed.equals(override)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Override must be a documented one-level raise to " + allowed);
            }
            if (vendor.getOverrideJustification() == null || vendor.getOverrideJustification().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Override justification is required");
            }
        }
        TprmCategorisation.Result r;
        try {
            r = TprmCategorisation.calculate(new TprmCategorisation.Input(
                    vendor.getF1(), vendor.getF2(), vendor.getF3(), vendor.getF4(), vendor.getF5(), vendor.getF6(),
                    certs, override));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        vendor.setF1Score(r.f1());
        vendor.setF2Score(r.f2());
        vendor.setF3Score(r.f3());
        vendor.setF4Score(r.f4());
        vendor.setF5Score(r.f5());
        vendor.setF6Score(r.f6());
        vendor.setInherentScore(r.inherent());
        vendor.setAssuranceCredit(r.assuranceCredit());
        vendor.setAdjustedScore(r.adjusted());
        vendor.setTier(r.effectiveTier());
        vendor.setCriticality(r.criticality());
        vendor.setPostureTarget(BigDecimal.valueOf(r.postureTarget()));
        vendor.setReassessmentMonths(r.reassessmentMonths());
        vendor.setElevationFlag(r.elevationFlag());
        refreshLifecycleFromProfile(vendor);
    }

    boolean profileReady(TprmVendor vendor) {
        return missingProfileFields(vendor).isEmpty();
    }

    /**
     * Excel Vendor Profile mandatory fields still blank (pack labels). Empty when the profile
     * may advance past J1 (CATEGORISED / generate / issue).
     */
    List<String> missingProfileFields(TprmVendor vendor) {
        List<String> missing = new ArrayList<>();
        if (!notBlank(vendor.getName())) {
            missing.add("Vendor legal name");
        }
        if (vendor.getVendorType() == null) {
            missing.add("Vendor type");
        }
        TprmVendorContact spoc = primarySpoc(vendor.getId());
        if (spoc == null || !notBlank(spoc.getName())) {
            missing.add("Vendor primary SPOC - name");
        }
        if (spoc == null || !EmailRules.isValid(spoc.getEmail())) {
            missing.add("Vendor primary SPOC - email");
        }
        if (spoc == null || !notBlank(spoc.getMobile())) {
            missing.add("Vendor primary SPOC - mobile");
        } else if (!isValidMobile(spoc.getMobile())) {
            missing.add("Vendor primary SPOC - mobile");
        }
        if (!notBlank(vendor.getServicesInScope())) {
            missing.add("Services in scope");
        }
        if (!notBlank(vendor.getProjectDescription())) {
            missing.add("Project / service description");
        }
        if (!notBlank(vendor.getOrgOwner())) {
            missing.add(ORG_OWNER_LABEL);
        }
        if (!notBlank(vendor.getContractCode())) {
            missing.add("Contract reference / Contract Code");
        }
        if (vendor.getContractStart() == null || vendor.getContractEnd() == null) {
            missing.add("Contract start and end date");
        }
        if (!notBlank(vendor.getF1())) {
            missing.add("Highest Organization information classification exposed");
        }
        if (!notBlank(vendor.getF2())) {
            missing.add("Level of access to Organization systems");
        }
        if (!notBlank(vendor.getF3())) {
            missing.add("Business criticality of the service");
        }
        if (!notBlank(vendor.getF4())) {
            missing.add("Delivery environment - whose assets, and where");
        }
        if (!notBlank(vendor.getF5())) {
            missing.add("Personal data involved");
        }
        if (!notBlank(vendor.getF6())) {
            missing.add("Concentration and substitutability");
        }
        List<TprmVendorCert> certs = certRepository.findByVendorIdOrderByIdAsc(vendor.getId());
        if (certs.isEmpty()) {
            missing.add("Independent certification held");
        } else {
            boolean needsExpiry = certs.stream()
                    .anyMatch(c -> c.getCertType() != null && c.getCertType() != TprmCertType.NONE
                            && c.getExpiryDate() == null);
            if (needsExpiry) {
                missing.add("Certification expiry date");
            }
            boolean needsOther = certs.stream()
                    .anyMatch(c -> c.getCertType() == TprmCertType.OTHER
                            && (c.getOtherLabel() == null || c.getOtherLabel().isBlank()));
            if (needsOther) {
                missing.add("Other certification name");
            }
        }
        return List.copyOf(missing);
    }

    TprmVendorContact primarySpoc(long vendorId) {
        return contactRepository.findByVendorIdOrderByIdAsc(vendorId).stream()
                .filter(c -> c.getRole() == ContactRole.PRIMARY_SPOC)
                .findFirst()
                .orElse(null);
    }

    List<TprmVendorContact> contacts(long vendorId) {
        return contactRepository.findByVendorIdOrderByIdAsc(vendorId);
    }

    boolean detailsLocked(TprmVendor vendor) {
        if (vendor.getCurrentAssessmentId() == null) {
            return false;
        }
        return assessmentRepository.findById(vendor.getCurrentAssessmentId())
                .map(a -> FREEZE.contains(a.getStatus()))
                .orElse(false);
    }

    boolean categorisationFrozen(TprmVendor vendor) {
        return detailsLocked(vendor);
    }

    VendorResponse toResponse(TprmVendor v) {
        List<TprmVendorContact> contacts = contactRepository.findByVendorIdOrderByIdAsc(v.getId());
        List<TprmVendorCert> certs = certRepository.findByVendorIdOrderByIdAsc(v.getId());
        String calculated = null;
        if (factorsComplete(v)) {
            calculated = TprmCategorisation.calculate(new TprmCategorisation.Input(
                    v.getF1(), v.getF2(), v.getF3(), v.getF4(), v.getF5(), v.getF6(),
                    certs.stream().map(TprmVendorCert::getCertType).toList(), null)).calculatedTier();
        }
        List<String> missing = missingProfileFields(v);
        return VendorResponse.from(v, contacts, certs, calculated, missing.isEmpty(), missing, raiseErmPrompted(v),
                categorisationFrozen(v));
    }

    boolean raiseErmPrompted(TprmVendor vendor) {
        if (vendor.getLinkedErmRiskId() != null) {
            return false;
        }
        return assessmentRepository.findByVendorIdOrderByCreatedAtDesc(vendor.getId()).stream()
                .anyMatch(a -> a.isRaiseErmPrompted() && a.getLinkedErmRiskId() == null
                        && a.getStatus() == AssessmentStatus.COMPLETE);
    }

    TprmVendor requireVendor(long id) {
        return vendorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor not found"));
    }

    void persist(TprmVendor vendor) {
        vendor.setUpdatedAt(Instant.now());
        vendorRepository.save(vendor);
    }

    void requireAdmin(User user) {
        if (user.getRoles().stream().noneMatch(r -> r == Role.OWNER || r == Role.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    private String requireUniqueName(String raw, Long excludeId) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vendor legal name is required");
        }
        String name = raw.trim();
        boolean taken = excludeId == null
                ? vendorRepository.existsByNameIgnoreCase(name)
                : vendorRepository.existsByNameIgnoreCaseAndIdNot(name, excludeId);
        if (taken) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A vendor named \"" + name + "\" already exists");
        }
        return name;
    }

    private void applyBody(TprmVendor v, UpsertVendorRequest req, boolean frozen, String name) {
        v.setName(name);
        if (!frozen) {
            v.setVendorType(req.vendorType());
        }
        v.setWebsite(blankToNull(req.website()));
        v.setServicesInScope(blankToNull(req.servicesInScope()));
        v.setProjectDescription(blankToNull(req.projectDescription()));
        v.setOrgOwner(requireKnownUser(req.orgOwner()));
        v.setContractCode(blankToNull(req.contractCode()));
        v.setContractStart(req.contractStart());
        v.setContractEnd(req.contractEnd());
        if (!frozen) {
            v.setF1(blankToNull(req.f1()));
            v.setF2(blankToNull(req.f2()));
            v.setF3(blankToNull(req.f3()));
            v.setF4(blankToNull(req.f4()));
            v.setF5(blankToNull(req.f5()));
            v.setF6(blankToNull(req.f6()));
            v.setOverrideTier(blankToNull(req.overrideTier()));
            v.setOverrideJustification(blankToNull(req.overrideJustification()));
        }
    }

    private void replaceContacts(long vendorId, List<UpsertVendorRequest.ContactBody> bodies) {
        contactRepository.deleteByVendorId(vendorId);
        contactRepository.flush();
        if (bodies == null || bodies.isEmpty()) {
            return;
        }
        int spoc = 0;
        int contributors = 0;
        for (UpsertVendorRequest.ContactBody b : bodies) {
            if (b.email() == null || b.email().isBlank()) {
                continue;
            }
            EmailRules.requireValid(b.email(), "Contact email");
            TprmVendorContact c = new TprmVendorContact();
            c.setVendorId(vendorId);
            c.setRole(b.role() != null ? b.role() : ContactRole.CONTRIBUTOR);
            c.setName(blankToNull(b.name()));
            c.setEmail(b.email().trim().toLowerCase(Locale.ROOT));
            c.setMobile(TprmPhoneRules.normalize(b.mobile()));
            if (c.getRole() == ContactRole.PRIMARY_SPOC) {
                spoc++;
                if (c.getMobile() != null && !isValidMobile(c.getMobile())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TprmPhoneRules.errorMessage());
                }
            } else if (c.getRole() == ContactRole.CONTRIBUTOR) {
                contributors++;
                if (c.getMobile() != null && !isValidMobile(c.getMobile())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TprmPhoneRules.errorMessage());
                }
            } else if (c.getMobile() != null && !isValidMobile(c.getMobile())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TprmPhoneRules.errorMessage());
            }
            contactRepository.save(c);
        }
        if (spoc > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only one primary SPOC is allowed");
        }
        if (contributors > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only one Additional vendor contributor is allowed");
        }
    }

    private void replaceCerts(long vendorId, List<UpsertVendorRequest.CertBody> bodies) {
        certRepository.deleteByVendorId(vendorId);
        certRepository.flush();
        if (bodies == null || bodies.isEmpty()) {
            return;
        }
        for (UpsertVendorRequest.CertBody b : bodies) {
            if (b.certType() == null) {
                continue;
            }
            if (b.certType() != TprmCertType.NONE && b.expiryDate() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Certification expiry date is required when a certificate is selected");
            }
            if (b.certType() == TprmCertType.OTHER && (b.otherLabel() == null || b.otherLabel().isBlank())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Describe the other certification");
            }
            TprmVendorCert c = new TprmVendorCert();
            c.setVendorId(vendorId);
            c.setCertType(b.certType());
            c.setExpiryDate(b.certType() == TprmCertType.NONE ? null : b.expiryDate());
            c.setOtherLabel(b.certType() == TprmCertType.OTHER ? blankToNull(b.otherLabel()) : null);
            certRepository.save(c);
        }
    }

    private void refreshLifecycleFromProfile(TprmVendor vendor) {
        if (vendor.getLifecycleState() == VendorLifecycleState.ISSUED
                || vendor.getLifecycleState() == VendorLifecycleState.SUBMITTED
                || vendor.getLifecycleState() == VendorLifecycleState.IN_REVIEW
                || vendor.getLifecycleState() == VendorLifecycleState.RETURNED
                || vendor.getLifecycleState() == VendorLifecycleState.COMPLETE
                || vendor.getLifecycleState() == VendorLifecycleState.QUESTIONNAIRE_READY) {
            return;
        }
        if (profileReady(vendor) && vendor.getTier() != null) {
            vendor.setLifecycleState(VendorLifecycleState.CATEGORISED);
        } else {
            vendor.setLifecycleState(VendorLifecycleState.PROFILE_INCOMPLETE);
        }
    }

    private static boolean factorsComplete(TprmVendor v) {
        return notBlank(v.getF1()) && notBlank(v.getF2()) && notBlank(v.getF3())
                && notBlank(v.getF4()) && notBlank(v.getF5()) && notBlank(v.getF6());
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String requireKnownUser(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String name = raw.trim();
        return userService.listDirectoryUsers().stream()
                .filter(u -> name.equalsIgnoreCase(u.getFullName()) || name.equalsIgnoreCase(u.getEmail()))
                .findFirst()
                .map(User::getFullName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        ORG_OWNER_LABEL + " must be a user"));
    }

    private static boolean isValidMobile(String mobile) {
        return TprmPhoneRules.isValid(mobile);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static String adminRoles(User admin) {
        return admin.getRoles().stream().map(Enum::name).sorted().reduce((a, b) -> a + "," + b).orElse("");
    }
}
