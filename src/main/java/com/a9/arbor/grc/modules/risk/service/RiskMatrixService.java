package com.a9.arbor.grc.modules.risk.service;

import com.a9.arbor.grc.modules.audit.domain.AuditActionType;
import com.a9.arbor.grc.modules.audit.service.AuditTrailService;
import com.a9.arbor.grc.modules.iam.domain.Role;
import com.a9.arbor.grc.modules.iam.domain.User;
import com.a9.arbor.grc.modules.notification.service.NotificationService;
import com.a9.arbor.grc.modules.org.domain.OrgProfile;
import com.a9.arbor.grc.modules.org.repo.OrgProfileRepository;
import com.a9.arbor.grc.modules.risk.domain.ExcelRiskScoring;
import com.a9.arbor.grc.modules.risk.domain.MatrixAxes;
import com.a9.arbor.grc.modules.risk.domain.MatrixGridSize;
import com.a9.arbor.grc.modules.risk.domain.RiskLevel;
import com.a9.arbor.grc.modules.risk.domain.RiskMatrix;
import com.a9.arbor.grc.modules.risk.domain.RiskMatrixConfig;
import com.a9.arbor.grc.modules.risk.repo.RiskMatrixConfigRepository;
import com.a9.arbor.grc.modules.risk.web.dto.RiskMatricesResponse;
import com.a9.arbor.grc.modules.risk.web.dto.UpdateRiskMatricesRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Tenant risk matrices: grid size / axes from config; cell scores from client Excel
 * (Probability × Impact product + rating bands). Axis changes are not applied
 * retroactively to frozen historical risks.
 */
@Service
public class RiskMatrixService {

    private final RiskMatrixConfigRepository configRepository;
    private final OrgProfileRepository orgProfileRepository;
    private final ObjectMapper objectMapper;
    private final AuditTrailService auditTrailService;
    private final NotificationService notificationService;

    public RiskMatrixService(RiskMatrixConfigRepository configRepository,
                             OrgProfileRepository orgProfileRepository,
                             ObjectMapper objectMapper,
                             AuditTrailService auditTrailService,
                             NotificationService notificationService) {
        this.configRepository = configRepository;
        this.orgProfileRepository = orgProfileRepository;
        this.objectMapper = objectMapper;
        this.auditTrailService = auditTrailService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public MatrixGridSize currentGridSize() {
        return loadGrids().gridSize();
    }

    @Transactional(readOnly = true)
    public MatrixAxes currentAxes() {
        return loadGrids().axes();
    }

    @Transactional(readOnly = true)
    public RiskLevel inherent(RiskLevel probability, RiskLevel impact) {
        Grids grids = loadGrids();
        grids.axes().requireActive(probability, "Likelihood");
        grids.axes().requireActive(impact, "Impact");
        return RiskMatrix.probabilityImpact(grids.gridSize(), grids.axes(), probability, impact);
    }

    /** Numeric P×I product for API/UI (Excel workbook). */
    @Transactional(readOnly = true)
    public int score(RiskLevel probability, RiskLevel impact) {
        Grids grids = loadGrids();
        grids.axes().requireActive(probability, "Likelihood");
        grids.axes().requireActive(impact, "Impact");
        return ExcelRiskScoring.score(grids.axes(), probability, impact);
    }

    @Transactional(readOnly = true)
    public String rating(RiskLevel probability, RiskLevel impact) {
        Grids grids = loadGrids();
        return ExcelRiskScoring.rating(grids.gridSize(), score(probability, impact));
    }

    @Transactional(readOnly = true)
    public String action(RiskLevel probability, RiskLevel impact) {
        Grids grids = loadGrids();
        return ExcelRiskScoring.action(grids.gridSize(), score(probability, impact));
    }

    /** Excel rating key: Accept / Accept and monitor → auto-close. */
    @Transactional(readOnly = true)
    public boolean autoClosableScore(RiskLevel probability, RiskLevel impact) {
        Grids grids = loadGrids();
        return ExcelRiskScoring.autoClosable(grids.gridSize(), score(probability, impact));
    }

    /**
     * Auto residual: Control effectiveness × Inherent (§13.3 CE matrix).
     * Manual residual uses {@link #residualProbabilityImpact}.
     */
    @Transactional(readOnly = true)
    public RiskLevel residual(RiskLevel controlEffectiveness, RiskLevel inherent) {
        Grids grids = loadGrids();
        grids.axes().requireActive(controlEffectiveness, "Control Effectiveness");
        grids.axes().requireActive(inherent, "Inherent risk");
        return RiskMatrix.residual(grids.gridSize(), grids.axes(), controlEffectiveness, inherent);
    }

    /**
     * Residual = Probability × Impact after controls (Excel residual sheet: same math as inherent).
     */
    @Transactional(readOnly = true)
    public RiskLevel residualProbabilityImpact(RiskLevel probability, RiskLevel impact) {
        return inherent(probability, impact);
    }

    /** @deprecated prefer {@link #autoClosableScore} */
    public static boolean autoClosable(RiskLevel residual) {
        return RiskMatrix.autoClosable(residual);
    }

    @Transactional(readOnly = true)
    public RiskMatricesResponse getMatrices() {
        return configRepository.findFirstByOrderByIdAsc()
                .map(this::toResponse)
                .orElseGet(this::defaultResponse);
    }

    @Transactional
    public RiskMatricesResponse updateMatrices(User admin, UpdateRiskMatricesRequest req) {
        MatrixGridSize gridSize = MatrixGridSize.fromInt(req.gridSize() > 0 ? req.gridSize() : 5);
        MatrixAxes axes = new MatrixAxes(req.rowLevels(), req.colLevels());
        axes.validateFor(gridSize);
        validateGrid(req.inherent(), gridSize, "Inherent");
        validateGrid(req.residual(), gridSize, "Residual");
        if (req.changeReason() == null || req.changeReason().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Change reason is required");
        }
        if (isGridLocked() && req.gridSize() != currentGridSize().size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Risk matrix grid size is locked after organisation setup");
        }

        RiskMatrixConfig cfg = configRepository.findFirstByOrderByIdAsc().orElseGet(RiskMatrixConfig::new);
        boolean creating = cfg.getId() == null;
        if (creating) {
            cfg.setVersion(1);
        } else {
            cfg.setVersion(cfg.getVersion() + 1);
        }
        cfg.setGridSize(gridSize.size());
        cfg.setRowAxisJson(toAxisJson(axes.rowLevels()));
        cfg.setColAxisJson(toAxisJson(axes.colLevels()));
        cfg.setInherentJson(toJson(req.inherent()));
        cfg.setResidualJson(toJson(req.residual()));
        cfg.setChangeReason(req.changeReason().trim());
        cfg.setUpdatedByUserId(admin.getId());
        configRepository.save(cfg);

        String role = admin.getRoles().stream().map(Role::name).sorted().findFirst().orElse("UNKNOWN");
        auditTrailService.record(
                AuditActionType.UPDATED, "RISK", admin.getId(), role,
                "RISK_MATRIX", String.valueOf(cfg.getId()), "Risk matrices v" + cfg.getVersion(),
                "version", creating ? "0" : String.valueOf(cfg.getVersion() - 1),
                String.valueOf(cfg.getVersion()), null, null);

        notificationService.trigger(
                "RISK_MATRIX_CHANGED",
                String.valueOf(cfg.getId()),
                List.of("ADMIN", "OWNER"),
                Map.of(
                        "version", String.valueOf(cfg.getVersion()),
                        "gridSize", String.valueOf(cfg.getGridSize()),
                        "reason", cfg.getChangeReason()));

        return toResponse(cfg);
    }

    private RiskMatricesResponse defaultResponse() {
        MatrixGridSize gridSize = MatrixGridSize.FIVE;
        MatrixAxes axes = MatrixAxes.defaults(gridSize);
        return new RiskMatricesResponse(
                RiskMatrix.defaultInherent(gridSize),
                RiskMatrix.defaultResidual(gridSize),
                gridSize.size(),
                axes.rowLevels(),
                axes.colLevels(),
                0,
                null,
                null,
                null,
                true, isGridLocked(), appetiteRating(), appetiteMax());
    }

    private RiskMatricesResponse toResponse(RiskMatrixConfig cfg) {
        MatrixGridSize gridSize = MatrixGridSize.fromInt(cfg.getGridSize());
        MatrixAxes axes = loadAxes(cfg, gridSize);
        return new RiskMatricesResponse(
                parseGrid(cfg.getInherentJson(), gridSize),
                parseGrid(cfg.getResidualJson(), gridSize),
                cfg.getGridSize(),
                axes.rowLevels(),
                axes.colLevels(),
                cfg.getVersion(),
                cfg.getChangeReason(),
                cfg.getUpdatedByUserId(),
                cfg.getUpdatedAt(),
                false, isGridLocked(), appetiteRating(), appetiteMax());
    }

    private record Grids(MatrixGridSize gridSize, MatrixAxes axes, RiskLevel[][] inherent, RiskLevel[][] residual) {
    }

    private Grids loadGrids() {
        return configRepository.findFirstByOrderByIdAsc()
                .map(cfg -> {
                    MatrixGridSize size = MatrixGridSize.fromInt(cfg.getGridSize());
                    MatrixAxes axes = loadAxes(cfg, size);
                    return new Grids(
                            size,
                            axes,
                            parseGrid(cfg.getInherentJson(), size),
                            parseGrid(cfg.getResidualJson(), size));
                })
                .orElseGet(() -> {
                    MatrixGridSize size = MatrixGridSize.FIVE;
                    MatrixAxes axes = MatrixAxes.defaults(size);
                    return new Grids(
                            size,
                            axes,
                            RiskMatrix.defaultInherent(),
                            RiskMatrix.defaultResidual());
                });
    }

    private MatrixAxes loadAxes(RiskMatrixConfig cfg, MatrixGridSize gridSize) {
        if (cfg.getRowAxisJson() == null || cfg.getColAxisJson() == null
                || cfg.getRowAxisJson().isBlank() || cfg.getColAxisJson().isBlank()) {
            return MatrixAxes.defaults(gridSize);
        }
        try {
            RiskLevel[] rows = objectMapper.readValue(cfg.getRowAxisJson(), RiskLevel[].class);
            RiskLevel[] cols = objectMapper.readValue(cfg.getColAxisJson(), RiskLevel[].class);
            MatrixAxes axes = new MatrixAxes(rows, cols);
            axes.validateFor(gridSize);
            return axes;
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Corrupt risk matrix axis JSON");
        }
    }

    private void validateGrid(RiskLevel[][] grid, MatrixGridSize gridSize, String label) {
        int n = gridSize.size();
        if (grid == null || grid.length != n) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    label + " matrix must be " + n + "×" + n);
        }
        for (int r = 0; r < n; r++) {
            if (grid[r] == null || grid[r].length != n) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        label + " matrix must be " + n + "×" + n);
            }
            for (int c = 0; c < n; c++) {
                if (grid[r][c] == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            label + " matrix cell [" + r + "][" + c + "] is required");
                }
            }
        }
    }

    private RiskLevel[][] parseGrid(String json, MatrixGridSize gridSize) {
        try {
            RiskLevel[][] grid = objectMapper.readValue(json, RiskLevel[][].class);
            validateGrid(grid, gridSize, "Stored");
            return grid;
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Corrupt risk matrix JSON");
        }
    }

    private String toJson(RiskLevel[][] grid) {
        try {
            return objectMapper.writeValueAsString(grid);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid matrix payload");
        }
    }

    private String toAxisJson(RiskLevel[] levels) {
        try {
            return objectMapper.writeValueAsString(levels);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid axis levels");
        }
    }

    public boolean isGridLocked() {
        return orgProfile().map(OrgProfile::isRiskMatrixLocked).orElse(false);
    }

    public RiskLevel currentAppetite() {
        String raw = orgProfile().map(OrgProfile::getRiskAppetiteRating).orElse("LOW");
        try {
            return RiskLevel.valueOf(raw);
        } catch (Exception e) {
            return RiskLevel.LOW;
        }
    }

    public void ensureGridSize(int gridSize) {
        if (isGridLocked()) {
            return;
        }
        MatrixGridSize size = MatrixGridSize.fromInt(gridSize);
        MatrixAxes axes = MatrixAxes.defaults(size);
        RiskMatrixConfig cfg = configRepository.findFirstByOrderByIdAsc().orElseGet(RiskMatrixConfig::new);
        cfg.setGridSize(size.size());
        cfg.setRowAxisJson(toAxisJson(axes.rowLevels()));
        cfg.setColAxisJson(toAxisJson(axes.colLevels()));
        if (cfg.getInherentJson() == null) {
            cfg.setInherentJson(toJson(RiskMatrix.defaultInherent(size)));
            cfg.setResidualJson(toJson(RiskMatrix.defaultResidual(size)));
            cfg.setVersion(1);
        }
        if (cfg.getChangeReason() == null) {
            cfg.setChangeReason("Organisation setup");
        }
        configRepository.save(cfg);
    }

    private String appetiteRating() {
        return currentAppetite().name();
    }

    private Integer appetiteMax() {
        return ExcelRiskScoring.appetiteMaxResidual(currentGridSize(), currentAppetite());
    }

    private java.util.Optional<OrgProfile> orgProfile() {
        return orgProfileRepository.findAll().stream().findFirst();
    }
}
