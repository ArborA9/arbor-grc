import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useAppShellPage } from './components/AppShellPageContext'
import {
  Alert,
  Box,
  Button,
  Card,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControl,
  FormControlLabel,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
  alpha,
} from '@mui/material'
import {
  api,
  type AssetSummary,
  type DpiaRiskTitleCandidate,
  type EvidenceClassification,
  type EvidenceSummary,
  type FrameworkDetail,
  type FrameworkSummary,
  type OrgUnitNode,
  type RiskCategory,
  type RiskLevel,
  type ResidualScoringMode,
  type RiskStatus,
  type RiskMatricesConfig,
  type ControlAssessmentStatus,
  type RiskBulkImportResult,
  type RiskScoreHistoryEntry,
  type RiskStageNote,
  type RiskSummary,
  type RiskKpis,
  type RiskProjectSummary,
  type RiskFormDraft,
  type Session,
  type TreatmentOption,
  type UserSummary,
  EVIDENCE_CLASSIFICATIONS,
  RISK_EVIDENCE_STAGES,
  type RiskEvidenceStage,
} from './api'
import StatusChip from './components/StatusChip'
import { type RiskDetailSection } from './components/RiskDetailDrawer'
import RiskDetailNav from './components/risk/RiskDetailNav'
import RiskCreateEditForm, { visibleFormSections } from './components/risk/RiskCreateEditForm'
import ExcelRiskDossier, { EXCEL_DOSSIER_SECTIONS } from './components/risk/ExcelRiskDossier'
import ProjectCreateProgress from './components/risk/ProjectCreateProgress'
import { ExcelAcceptanceDossier, AcceptanceProgressCard } from './components/risk/ExcelAcceptanceContributor'
import RiskFormShell from './components/risk/RiskFormShell'
import DiscardChangesDialog from './components/DiscardChangesDialog'
import { formatAed, formatTenantDateTime } from './uiQuality/tenantLocale'
import { frameworkLabel } from './uiQuality/frameworkLabel'
import { useBeforeUnload } from './uiQuality/useBeforeUnload'
import { useOptionalUnsavedChangesGuardApi, useRegisterUnsavedChanges } from './uiQuality/UnsavedChangesGuard'
import RiskRegisterTable, { type RiskDraftRow } from './components/risk/RiskRegisterTable'
import RiskRegisterToolbar from './components/risk/RiskRegisterToolbar'
import {
  RISK_REGISTER_MAX_COLUMNS,
  riskRegisterColumnsForType,
} from './components/risk/riskRegisterColumns'
import { BusyButton, ColumnPicker, CompactTextField, ConfirmActionDialog, FormFieldLabel, KpiCard, KpiCardGrid, PageLoading, RegisterListFrame, RegisterPagination, RegisterToolbar, RiskLevelBadge, SettingsCard, AttentionBanner, apiLevelTone, controlEffectivenessTone, RATING_TONE_COLOR, complexFormContainerSx, controlActionSx, sectionAnchorSx, twoColFieldSx, registerTableSx, primaryTableCellSx, primaryRegisterNameSx, compactTableCellSx, lastTableCellSx, MEDIUM_FORM_MAX_WIDTH } from './components/ui'
import { useListReady } from './uiQuality/useListReady'
import { usePagedRows } from './uiQuality/usePagedRows'
import { usePersistedState } from './uiQuality/usePersistedState'
import { apiRiskLevelToBadge } from './components/ui/riskBadgeMappers'
import FormSelect from './components/ui/FormSelect'
import ContributorEvidencePanel from './components/contributor/ContributorEvidencePanel'
import { useColumnVisibility } from './hooks/useColumnVisibility'
import {
  computeRiskKpis,
  filterRiskRows,
  kpiCountForBand,
  hasActiveRiskFilters,
  sortRiskRows,
  waitingMyApproval,
  type RiskPriorityFilter,
  type RiskSortDir,
  type RiskSortKey,
  type RiskStatusFilter,
} from './components/risk/riskUi'
import {
  ALL_RISK_LEVELS,
  AXIS_PRESETS,
  activeScoreLevels,
  axesFromConfig,
  calcInherent,
  calcResidual,
  calcResidualPi,
  defaultAxes,
  defaultInherent,
  defaultResidual,
  defaultResidualCe,
  autoResidualScore,
  ceReductionPercent,
  colRank,
  appetiteMaxResidual,
  excelAction,
  excelAutoClosable,
  excelBandLabel,
  excelLevelFromScore,
  excelManualScoreError,
  excelRatingToLevel,
  excelScore,
  hasDistinctLevels,
  foldBandToGrid,
  parseGridSize,
  priorityBandsForGrid,
  type MatrixAxes,
  type MatrixGridSize,
} from './components/risk/riskMatrix'
import { CONTROL_EFFECTIVENESS_LABEL, fmtLevel } from './riskLevelLabels'
import {
  CE_EXCEL_LABEL,
  LIKELIHOOD_EXCEL_LABEL,
  EXCEL_FIELD,
  EXCEL_TREAT_LABEL,
  ADMIN_FINISH_ACTIONS,
  EXCEL_DATE_FIELD_WIDTH,
  acceptanceApproverDisplay,
  acceptanceLevel2Label,
  acceptanceLevel3Label,
  acceptanceRequiredLabel,
  configStatusLabel,
  evaluationDecisionLabel,
  ermCauseFromForm,
  ermCauseToForm,
  isExcelFinishedRow,
  reviewAlertLabel,
  showPostSubmitFields,
  scaledExcelField,
  treatmentProgressLabel,
  withinAppetiteLabel,
  isIdentificationEvidence,
} from './components/risk/excelRiskFields'
import { collectRiskRequiredErrors, focusFirstRiskError, RISK_INPUT_ID } from './components/risk/riskFormRequired'
import { nextCopyTitle } from './components/risk/copyRiskTitle'
import { MAX_REFERENCE_ATTACHMENTS } from './components/risk/ReferenceAttachmentField'
import { RISK_EVIDENCE_MAX_BYTES, RISK_EVIDENCE_MAX_FILES } from './components/risk/riskEvidenceLimits'
import { brand, semantic } from './theme'

function queuedReferenceSummary(
  item: { file: File; classification: EvidenceClassification },
  index: number,
): EvidenceSummary {
  return {
    id: -(index + 1),
    title: item.file.name,
    classification: item.classification,
    status: 'ACTIVE',
    expiresOn: null,
    currentVersion: 1,
    latestFilename: item.file.name,
    latestSizeBytes: item.file.size,
    linkCount: 0,
    updatedAt: new Date(0).toISOString(),
    workflowStage: 'IDENTIFICATION',
  }
}
import { actionDueDateError, dateRangeError, maxToleranceReviewIso, todayIso } from './validation'
import { RiskHeatMapPanel, type RiskHeatMapDrill } from './RiskHeatMap'

const RISK_DETAIL_SECTIONS: RiskDetailSection[] = [
  { id: 'risk-section-identification', label: 'Identification' },
  { id: 'risk-section-evidence', label: 'Evidence' },
  { id: 'risk-section-scoring', label: 'Scoring' },
  { id: 'risk-section-notes', label: 'Notes' },
  { id: 'risk-section-ownership', label: 'Ownership' },
  { id: 'risk-section-treatment', label: 'Treatment' },
  { id: 'risk-section-meta', label: 'Meta' },
  { id: 'risk-section-actions', label: 'Actions' },
]

const VIEW_EXCEL_SECTIONS: RiskDetailSection[] = [...EXCEL_DOSSIER_SECTIONS]

const TREAT: TreatmentOption[] = ['TREAT', 'TOLERATE', 'TERMINATE', 'TRANSFER']
const IS_RISK_FRAMEWORK_MODULES = ['ISMS', 'ERM', 'ITSM', 'AIMS', 'BCMS'] as const
const IS_CATS: RiskCategory[] = [
  'STRATEGIC',
  'OPERATIONAL',
  'FINANCIAL',
  'COMPLIANCE',
  'IT',
  'REPUTATIONAL',
  'OTHER',
]
const ERM_CATS: RiskCategory[] = [
  'STRATEGIC',
  'OPERATIONAL',
  'FINANCIAL',
  'COMPLIANCE',
  'IT',
  'REPUTATIONAL',
  'OTHER',
]
const TREAT_LABEL: Record<TreatmentOption, string> = EXCEL_TREAT_LABEL

const LEVEL_RANK: Record<RiskLevel, number> = {
  VERY_LOW: 1,
  LOW: 2,
  MEDIUM: 3,
  HIGH: 4,
  VERY_HIGH: 5,
}

function riskEvidenceStageFor(r: RiskSummary): RiskEvidenceStage {
  if (r.treatmentOption === 'TRANSFER' || r.status === 'TRANSFERRED') return 'TRANSFER'
  if (['DRAFT', 'RE_OPENED', 'AUTO_CLOSED'].includes(r.status)) return 'IDENTIFICATION'
  if (r.status === 'ASSIGNED') return 'ASSIGNMENT'
  if (['SUBMITTED', 'UNDER_VALIDATION', 'PENDING_ADMIN_REVIEW'].includes(r.status)) return 'REVIEW'
  if (['PENDING_ACCEPTANCE', 'TOLERATED'].includes(r.status)) return 'TOLERANCE'
  return 'TREATMENT'
}

function priorityFromResidual(r: RiskLevel, gridSize: MatrixGridSize): string {
  return fmtLevel(foldBandToGrid(r, gridSize))
}

function drillFilterLabel(d: RiskHeatMapDrill | null): string | null {
  if (!d) return null
  const row = d.scoreBasis === 'INHERENT' ? 'Probability' : 'Residual probability'
  const col = d.scoreBasis === 'INHERENT' ? 'Impact' : 'Inherent risk'
  return `${row} ${fmtLevel(d.rowLevel)} × ${col} ${fmtLevel(d.colLevel)}`
}
function fmtWhen(iso: string | null | undefined): string {
  if (!iso) return '-'
  return formatTenantDateTime(iso)
}

/** True send-back / cancel only. "Acceptance required" is a workflow note, not a return. */
function shouldShowRejectionBanner(status: string, reason: string | null | undefined): boolean {
  const text = reason?.trim()
  if (!text) return false
  if (status === 'CANCELLED') return true
  if (status === 'RE_OPENED' || status === 'ASSIGNED') return true
  if (status === 'PENDING_ACCEPTANCE' && !/^acceptance required\.?$/i.test(text)) return true
  return false
}

/** Header cluster: same title case as Export / Close (not shouty ASSIGNED). */
function headerActionStatusLabel(status: string, excel: boolean): string {
  const raw = excel ? configStatusLabel(status) : status.replace(/_/g, ' ')
  return raw.toLowerCase().replace(/\b\w/g, (ch) => ch.toUpperCase())
}

function HeaderActionStatus({ label }: { label: string }) {
  return (
    <Box
      component="span"
      role="status"
      aria-label={`Status ${label}`}
      sx={{
        ...controlActionSx,
        display: 'inline-flex',
        alignItems: 'center',
        px: 1.5,
        border: `1px solid ${brand.accent}`,
        borderRadius: '6px',
        fontSize: '0.75rem',
        fontWeight: 600,
        lineHeight: 1.25,
        color: brand.accentHover,
        bgcolor: 'transparent',
        textTransform: 'none',
        userSelect: 'none',
      }}
    >
      {label}
    </Box>
  )
}

function flattenOrgUnits(nodes: OrgUnitNode[], acc: OrgUnitNode[] = []): OrgUnitNode[] {
  for (const n of nodes) {
    acc.push(n)
    if (n.children?.length) flattenOrgUnits(n.children, acc)
  }
  return acc
}

function scrollParent(el: HTMLElement): HTMLElement | null {
  let node: HTMLElement | null = el.parentElement
  while (node) {
    const overflow = getComputedStyle(node).overflowY
    if (overflow === 'auto' || overflow === 'scroll') return node
    node = node.parentElement
  }
  return null
}

/** Keep After Submission and the approve actions in the detail pane after target scoring. */
function revealAfterSubmissionAndActions() {
  const post = document.getElementById('risk-section-post-submit')
  const actions = document.getElementById('risk-section-actions')
  if (!post || !actions) return
  const root = scrollParent(post)
  if (!root) return
  const rootTop = root.getBoundingClientRect().top
  const pad = 8
  const postTop = post.getBoundingClientRect().top - rootTop
  const actionsBottom = actions.getBoundingClientRect().bottom - rootTop
  const viewH = root.clientHeight
  const overflowBelow = actionsBottom - (viewH - pad)
  if (overflowBelow <= 4) return
  const roomAbovePost = postTop - pad
  const delta = Math.min(overflowBelow, Math.max(0, roomAbovePost))
  if (delta < 4) return
  root.scrollTo({ top: root.scrollTop + delta, behavior: 'smooth' })
}

/** Bring Remarks and the action button into view after an Action choice. */
function revealActionFields() {
  const actions = document.getElementById('risk-section-actions')
  if (!actions) return
  const root = scrollParent(actions)
  if (!root) return
  const pad = 8
  const bottom = actions.getBoundingClientRect().bottom - root.getBoundingClientRect().top
  const overflow = bottom - (root.clientHeight - pad)
  if (overflow < 4) return
  root.scrollTo({ top: root.scrollTop + overflow, behavior: 'smooth' })
}

function DetailSection({
  title,
  subtitle,
  sectionId,
  children,
}: {
  title: string
  subtitle?: string
  sectionId: string
  children: ReactNode
}) {
  return (
    <Box id={sectionId} sx={sectionAnchorSx}>
      <SettingsCard compact autoHeight title={title} subtitle={subtitle}>
        {children}
      </SettingsCard>
    </Box>
  )
}

function detailValueEmpty(value: ReactNode): boolean {
  if (value == null || value === false) return true
  if (typeof value === 'number') return false
  if (typeof value === 'string') {
    const t = value.trim()
    return t === '' || t === '-' || t === '-' || t === '–'
  }
  return false
}

function excelCaption(label: string): string {
  return label.replace(/\s*\(auto\)\s*/gi, '').replace(/\s+/g, ' ').trim()
}

function DetailField({
  label,
  value,
  tone,
  span,
}: {
  label: string
  value: ReactNode
  tone?: 'critical' | 'high' | 'medium' | 'low' | 'veryLow'
  span?: boolean
}) {
  if (detailValueEmpty(value)) return null
  return (
    <Box sx={{ minWidth: 0, gridColumn: span ? { md: '1 / -1' } : undefined }}>
      <FormFieldLabel>{excelCaption(label)}</FormFieldLabel>
      {tone ? (
        <Box sx={{ minHeight: 32, display: 'flex', alignItems: 'center' }}>
          <RiskLevelBadge level={tone} label={String(value)} />
        </Box>
      ) : (
        <Typography sx={{ fontSize: '0.75rem', color: brand.ink, lineHeight: 1.4, whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
          {value}
        </Typography>
      )}
    </Box>
  )
}

function DetailLevelField({ label, level }: { label: string; level: RiskLevel | null | undefined }) {
  if (!level) return null
  return (
    <Box sx={{ minWidth: 0 }}>
      <FormFieldLabel>{excelCaption(label)}</FormFieldLabel>
      <Box sx={{ minHeight: 32, display: 'flex', alignItems: 'center' }}>
        <RiskLevelBadge level={apiRiskLevelToBadge(level)} label={fmtLevel(level)} />
      </Box>
    </Box>
  )
}

function DetailFieldGrid({ children }: { children: ReactNode }) {
  return (
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' },
        columnGap: 2,
        rowGap: 1,
        alignItems: 'start',
      }}
    >
      {children}
    </Box>
  )
}

function DetailTwoCol({ children }: { children: ReactNode }) {
  return (
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: { xs: '1fr', lg: '1fr 1fr' },
        gap: { xs: 0, lg: 2 },
        alignItems: 'start',
      }}
    >
      {children}
    </Box>
  )
}

function axisSlotLabel(index: number, count: number, axis: 'row' | 'col'): string {
  // Level 1 always = lowest severity; Level N = highest (matches column order; row indices are top→bottom).
  const levelNum = axis === 'col' ? index + 1 : count - index
  if (axis === 'col') {
    if (index === 0) return `Level ${levelNum} (lowest)`
    if (index === count - 1) return `Level ${levelNum} (highest)`
  } else {
    if (index === count - 1) return `Level ${levelNum} (lowest)`
    if (index === 0) return `Level ${levelNum} (highest)`
  }
  return `Level ${levelNum}`
}

function AxisLevelEditor({
  label,
  hint,
  levels,
  axis,
  onChange,
}: {
  label: string
  hint: string
  levels: RiskLevel[]
  axis: 'row' | 'col'
  onChange: (next: RiskLevel[]) => void
}) {
  return (
    <Box>
      <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 0.5 }}>
        {label}: {hint}
      </Typography>
      <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
        {levels.map((lvl, i) => (
          <FormSelect
            key={`${label}-${i}`}
            label={axisSlotLabel(i, levels.length, axis)}
            value={lvl}
            options={ALL_RISK_LEVELS.map((l) => ({ value: l, label: fmtLevel(l) }))}
            onChange={(v) => {
              const next = [...levels]
              next[i] = v as RiskLevel
              onChange(next)
            }}
            minWidth={130}
          />
        ))}
      </Stack>
    </Box>
  )
}

function MatrixEditor({
  title,
  rowLabel,
  colLabel,
  axes,
  grid,
  onChange,
}: {
  title: string
  rowLabel: string
  colLabel: string
  axes: MatrixAxes
  grid: RiskLevel[][]
  onChange: (next: RiskLevel[][]) => void
}) {
  const rows = axes.rows
  const cols = axes.cols
  return (
    <Box>
      <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
        {title}
      </Typography>
      <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 0.75 }}>
        {rowLabel} / {colLabel}
      </Typography>
      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell />
              {cols.map((c) => (
                <TableCell key={c} align="center">
                  {fmtLevel(c)}
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((rowLevel, r) => (
              <TableRow key={rowLevel}>
                <TableCell sx={{ whiteSpace: 'nowrap' }}>{fmtLevel(rowLevel)}</TableCell>
                {cols.map((_col, c) => (
                  <TableCell key={`${r}-${c}`} align="center" sx={{ p: 0.5 }}>
                    <Select
                      size="small"
                      value={grid[r][c]}
                      onChange={(e) => {
                        const next = grid.map((row) => [...row])
                        next[r][c] = e.target.value as RiskLevel
                        onChange(next)
                      }}
                      sx={{ minWidth: 110, fontSize: '0.75rem' }}
                    >
                      {ALL_RISK_LEVELS.map((l) => (
                        <MenuItem key={l} value={l}>
                          {fmtLevel(l)}
                        </MenuItem>
                      ))}
                    </Select>
                  </TableCell>
                ))}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  )
}

function ResidualTrend({ history }: { history: RiskScoreHistoryEntry[] }) {
  const w = 320
  const h = 72
  const pad = 8
  if (history.length === 0) return null
  const pts = history.map((entry, i) => {
    const x =
      history.length === 1
        ? w / 2
        : pad + (i / (history.length - 1)) * (w - pad * 2)
    const y = h - pad - ((LEVEL_RANK[entry.residualRisk] - 1) / 4) * (h - pad * 2)
    return `${x},${y}`
  })
  return (
    <Box sx={{ overflowX: 'auto' }}>
      <svg width={w} height={h} viewBox={`0 0 ${w} ${h}`} role="img" aria-label="Residual risk trend">
        <line x1={pad} y1={pad} x2={w - pad} y2={pad} stroke={brand.hairline} />
        <line x1={pad} y1={h / 2} x2={w - pad} y2={h / 2} stroke={brand.hairline} strokeDasharray="3 3" />
        <line x1={pad} y1={h - pad} x2={w - pad} y2={h - pad} stroke={brand.hairline} />
        <polyline
          fill="none"
          stroke={brand.accent ?? '#0B6E99'}
          strokeWidth={2}
          points={pts.join(' ')}
        />
        {history.map((entry, i) => {
          const x =
            history.length === 1
              ? w / 2
              : pad + (i / (history.length - 1)) * (w - pad * 2)
          const y = h - pad - ((LEVEL_RANK[entry.residualRisk] - 1) / 4) * (h - pad * 2)
          return <circle key={entry.id} cx={x} cy={y} r={3.5} fill={brand.accent ?? '#0B6E99'} />
        })}
      </svg>
      <Typography variant="caption" color="text.secondary" display="block">
        Y-axis: residual Very Low → Very High over time
      </Typography>
    </Box>
  )
}

export default function RiskPage({
  session,
  riskType,
  onError,
  onOk,
  initialDrill = null,
  onClearDrill,
  onOpenAssignedRisk,
  initialProjectId = null,
  openFromNotification = null,
  startOnAcceptance = false,
  lockToProject = false,
}: {
  session: Session
  riskType: 'IS_RISK' | 'ERM' | 'PRIVACY_RISK'
  onError: (msg: string) => void
  onOk: (msg: string) => void
  /** Stay inside the initially selected project (no project list). Used by Privacy Risk. */
  lockToProject?: boolean
  initialDrill?: RiskHeatMapDrill | null
  onClearDrill?: () => void
  /** Contributor: open assigned work when there is a task. Return false to show the read-only row. */
  onOpenAssignedRisk?: (riskId: number, projectId: number | null) => void | boolean | Promise<boolean>
  /** When returning from a risk detail, restore this project. */
  initialProjectId?: number | null
  openFromNotification?: { riskId: number; epoch: number } | null
  /** Open on the Risk Acceptance project list. */
  startOnAcceptance?: boolean
}) {
  const isErm = riskType === 'ERM'
  const isPrivacy = riskType === 'PRIVACY_RISK'
  const cats = isErm ? ERM_CATS : IS_CATS
  const canAdmin = session.roles.some((r) => ['OWNER', 'ADMIN'].includes(r))
  const canExport = session.roles.some((r) => ['OWNER', 'ADMIN'].includes(r))
  const [rows, setRows] = useState<RiskSummary[]>([])
  const loadSeq = useRef(0)
  const [assets, setAssets] = useState<AssetSummary[]>([])
  const [users, setUsers] = useState<UserSummary[]>([])
  const [orgUnits, setOrgUnits] = useState<OrgUnitNode[]>([])
  const [detail, setDetail] = useState<RiskSummary | null>(null)
  const [busy, setBusy] = useState(false)
  const [referenceBusy, setReferenceBusy] = useState(false)
  const { listReady, markReady } = useListReady()
  const [formMode, setFormMode] = useState<'none' | 'create' | 'edit'>('none')
  const [copyFromLabel, setCopyFromLabel] = useState<string | null>(null)
  const [assignOpen, setAssignOpen] = useState(false)
  const [ownerOpen, setOwnerOpen] = useState(false)
  const [ownerPick, setOwnerPick] = useState<number | ''>('')
  const [cancelOpen, setCancelOpen] = useState(false)
  const [cancelReason, setCancelReason] = useState('')
  const [drill, setDrill] = useState<RiskHeatMapDrill | null>(initialDrill)
  const [search, setSearch] = usePersistedState(`reg-risk-${riskType}-q`, '')
  const [statusFilter, setStatusFilter] = useState<RiskStatusFilter>('ALL')
  const [priorityFilter, setPriorityFilter] = useState<RiskPriorityFilter>('ALL')
  const [waitingApprovalOnly, setWaitingApprovalOnly] = useState(false)
  const [sortKey, setSortKey] = useState<RiskSortKey | null>(null)
  const [sortDir, setSortDir] = useState<RiskSortDir>('asc')
  const [exportBusy, setExportBusy] = useState(false)
  const [detailExportBusy, setDetailExportBusy] = useState(false)
  const [bulkImportOpen, setBulkImportOpen] = useState(false)
  const [bulkImportBusy, setBulkImportBusy] = useState(false)
  const [bulkImportResult, setBulkImportResult] = useState<RiskBulkImportResult | null>(null)
  const bulkFileRef = useRef<HTMLInputElement | null>(null)
  const [heatMapOpen, setHeatMapOpen] = useState(false)
  const [discardDialogOpen, setDiscardDialogOpen] = useState(false)
  const pendingDetailActionRef = useRef<(() => void) | null>(null)

  const [title, setTitle] = useState('')
  const [dpiaRiskCandidates, setDpiaRiskCandidates] = useState<DpiaRiskTitleCandidate[]>([])
  const [sourceDpiaAssessmentRowId, setSourceDpiaAssessmentRowId] = useState<number | ''>('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [category, setCategory] = useState<RiskCategory | ''>(isErm ? '' : 'IT')
  const [categoryOther, setCategoryOther] = useState('')
  const [description, setDescription] = useState('')
  const [assetId, setAssetId] = useState<number | ''>('')
  const [orgUnitId, setOrgUnitId] = useState<number | ''>('')
  const [strategicObjective, setStrategicObjective] = useState('')
  const [financialImpactAed, setFinancialImpactAed] = useState('')
  const [observedGap, setObservedGap] = useState('')
  const [threats, setThreats] = useState('')
  const [adminNotes, setAdminNotes] = useState('')
  const [probability, setProbability] = useState<RiskLevel>('MEDIUM')
  const [impact, setImpact] = useState<RiskLevel>('MEDIUM')
  const [controlEff, setControlEff] = useState<RiskLevel>('MEDIUM')
  const [residualScoringMode, setResidualScoringMode] =
    useState<ResidualScoringMode>('CONTROL_EFFECTIVENESS')
  const [manualResidualScore, setManualResidualScore] = useState('')
  const [cause, setCause] = useState('')
  const [causeOther, setCauseOther] = useState('')
  const [dateIdentified, setDateIdentified] = useState(todayIso)
  const [nextReviewDate, setNextReviewDate] = useState('')
  const [thirdPartyName, setThirdPartyName] = useState('')
  const [treatmentJustification, setTreatmentJustification] = useState('')
  const [targetControlEff, setTargetControlEff] = useState<RiskLevel>('HIGH')
  const [compensatingControls, setCompensatingControls] = useState('')
  const [projects, setProjects] = useState<RiskProjectSummary[]>([])
  const [catalogsReady, setCatalogsReady] = useState(false)
  const catalogsReadyRef = useRef(false)
  catalogsReadyRef.current = catalogsReady
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(initialProjectId)
  const [homeKpis, setHomeKpis] = useState<RiskKpis | null>(null)
  const [risksReady, setRisksReady] = useState(initialProjectId == null)
  const scopedRows = useMemo(() => {
    if (selectedProjectId == null) return rows
    return rows.filter((r) => r.riskProjectId === selectedProjectId)
  }, [rows, selectedProjectId])
  const [acceptanceView, setAcceptanceView] = useState(startOnAcceptance)
  const [acceptanceProjectQuery, setAcceptanceProjectQuery] = useState('')
  const [newProjectName, setNewProjectName] = useState('')
  const [newProjectYear, setNewProjectYear] = useState(String(new Date().getFullYear()))
  const [newProjectFrameworkId, setNewProjectFrameworkId] = useState<number | ''>('')
  const [projectCreating, setProjectCreating] = useState(false)
  const [seedProgress, setSeedProgress] = useState({ done: 0, total: 0 })
  const [postTargetCe, setPostTargetCe] = useState<RiskLevel | ''>('')
  const [adminAction, setAdminAction] = useState('')
  const [actionRemarks, setActionRemarks] = useState('')
  const [approveNextReviewDate, setApproveNextReviewDate] = useState('')
  const [residualProbability, setResidualProbability] = useState<RiskLevel>('MEDIUM')
  const [residualImpact, setResidualImpact] = useState<RiskLevel>('MEDIUM')
  const [existingControls, setExistingControls] = useState('')
  const [treatmentOption, setTreatmentOption] = useState<TreatmentOption>('TREAT')
  const [treatmentPlan, setTreatmentPlan] = useState('')
  const [planActionOwnerId, setPlanActionOwnerId] = useState<number | ''>('')
  const [planCustodianUserId, setPlanCustodianUserId] = useState<number | ''>('')
  const [planStartDate, setPlanStartDate] = useState('')
  const [planTargetDate, setPlanTargetDate] = useState('')
  const [planEvidenceRequired, setPlanEvidenceRequired] = useState('')
  const [displayIdOverride, setDisplayIdOverride] = useState('')
  const [overrideAutoClose, setOverrideAutoClose] = useState(false)
  const [autoCloseJustification, setAutoCloseJustification] = useState('')
  const [frameworks, setFrameworks] = useState<FrameworkSummary[]>([])
  const [frameworkDetail, setFrameworkDetail] = useState<FrameworkDetail | null>(null)
  const [frameworkId, setFrameworkId] = useState<number | ''>('')
  const [frameworkNodeId, setFrameworkNodeId] = useState<number | ''>('')
  const [controlAssessmentStatus, setControlAssessmentStatus] =
    useState<ControlAssessmentStatus | null>(null)
  const [eligibleContributors, setEligibleContributors] = useState<
    { id: number; email: string; fullName: string }[]
  >([])
  const [riskOwnerUserId, setRiskOwnerUserId] = useState<number | ''>('')
  const [assignUserId, setAssignUserId] = useState<number | ''>('')
  const [rejectReason, setRejectReason] = useState('')
  const [milestoneTitle, setMilestoneTitle] = useState('')
  const [milestoneDueDate, setMilestoneDueDate] = useState('')
  const [milestoneNotes, setMilestoneNotes] = useState<Record<number, string>>({})
  const [adminAcceptComments, setAdminAcceptComments] = useState('')
  const [adminDeclaration, setAdminDeclaration] = useState(false)
  const [ownerDeclaration, setOwnerDeclaration] = useState(false)
  const [ownerComments, setOwnerComments] = useState('')
  const [reopenReason, setReopenReason] = useState('')
  const [infosecStatusInitial, setInfosecStatusInitial] = useState('')
  const [infosecStatusFinal, setInfosecStatusFinal] = useState('')
  const [changeTreatmentOption, setChangeTreatmentOption] = useState<TreatmentOption>('TREAT')
  const [changeTreatmentJustification, setChangeTreatmentJustification] = useState('')
  const [closeProbability, setCloseProbability] = useState<RiskLevel>('MEDIUM')
  const [closeImpact, setCloseImpact] = useState<RiskLevel>('MEDIUM')
  const [closeControlEff, setCloseControlEff] = useState<RiskLevel>('MEDIUM')
  const [closeNotes, setCloseNotes] = useState('')
  const [scoreHistory, setScoreHistory] = useState<RiskScoreHistoryEntry[]>([])
  const [stageNotes, setStageNotes] = useState<RiskStageNote[]>([])
  const [noteBody, setNoteBody] = useState('')
  const [confirmNoteId, setConfirmNoteId] = useState<number | null>(null)
  const [noteInternalOnly, setNoteInternalOnly] = useState(false)
  const [inherentGrid, setInherentGrid] = useState<RiskLevel[][]>(() => defaultInherent(5))
  const [residualGrid, setResidualGrid] = useState<RiskLevel[][]>(() => defaultResidual(5))
  const [matrixMeta, setMatrixMeta] = useState<RiskMatricesConfig | null>(null)
  const [matricesOpen, setMatricesOpen] = useState(false)
  const [editGridSize, setEditGridSize] = useState<MatrixGridSize>(5)
  const [editRowLevels, setEditRowLevels] = useState<RiskLevel[]>(() => defaultAxes(5).rows)
  const [editColLevels, setEditColLevels] = useState<RiskLevel[]>(() => defaultAxes(5).cols)
  const [editInherent, setEditInherent] = useState<RiskLevel[][]>(() => defaultInherent(5))
  const [editResidual, setEditResidual] = useState<RiskLevel[][]>(() => defaultResidual(5))
  const [matrixChangeReason, setMatrixChangeReason] = useState('')
  const [createEvidenceFiles, setCreateEvidenceFiles] = useState<File[]>([])
  const [createQueuedReferences, setCreateQueuedReferences] = useState<
    { file: File; classification: EvidenceClassification }[]
  >([])
  const [createEvidenceClass, setCreateEvidenceClass] =
    useState<EvidenceClassification | ''>('')
  const [riskDrafts, setRiskDrafts] = useState<RiskFormDraft[]>([])
  const [activeDraftId, setActiveDraftId] = useState<number | null>(null)
  const [draftAttachments, setDraftAttachments] = useState<EvidenceSummary[]>([])
  const [draftBaseline, setDraftBaseline] = useState<string | null>(null)
  const draftBaselinePending = useRef(false)
  const [deleteDraftTarget, setDeleteDraftTarget] = useState<RiskFormDraft | null>(null)
  const [linkedEvidence, setLinkedEvidence] = useState<EvidenceSummary[]>([])
  const [detailEvidenceFile, setDetailEvidenceFile] = useState<File | null>(null)
  const [detailEvidenceClass, setDetailEvidenceClass] =
    useState<EvidenceClassification | ''>('')
  const [detailEvidenceStage, setDetailEvidenceStage] =
    useState<RiskEvidenceStage>('IDENTIFICATION')

  const flatOrgs = useMemo(() => flattenOrgUnits(orgUnits), [orgUnits])
  const selectedNode = useMemo(
    () => frameworkDetail?.nodes.find((n) => n.id === frameworkNodeId) ?? null,
    [frameworkDetail, frameworkNodeId],
  )
  const previewUaeIa = selectedNode?.priority
    ? selectedNode.priority.toUpperCase().match(/P[1-4]/)?.[0] ?? null
    : null

  const matrixGridSize = parseGridSize(matrixMeta?.gridSize)
  useEffect(() => {
    const allowed = new Set<RiskPriorityFilter>(['ALL', ...priorityBandsForGrid(matrixGridSize)])
    if (!allowed.has(priorityFilter)) setPriorityFilter('ALL')
  }, [matrixGridSize, priorityFilter])
  const matrixAxes = useMemo(
    () => axesFromConfig(matrixGridSize, matrixMeta?.rowLevels, matrixMeta?.colLevels),
    [matrixGridSize, matrixMeta?.rowLevels, matrixMeta?.colLevels],
  )
  const editAxes = useMemo(
    () => ({ rows: editRowLevels, cols: editColLevels }),
    [editRowLevels, editColLevels],
  )
  const scoreLevels = useMemo(() => activeScoreLevels(matrixAxes), [matrixAxes])
  const axesValid =
    hasDistinctLevels(editRowLevels) &&
    hasDistinctLevels(editColLevels) &&
    editRowLevels.length === editGridSize &&
    editColLevels.length === editGridSize

  const previewInherent = calcInherent(inherentGrid, matrixAxes, probability, impact)
  const residualCeGrid = useMemo(
    () => defaultResidualCe(matrixGridSize, matrixAxes),
    [matrixGridSize, matrixAxes],
  )
  const previewResidual =
    residualScoringMode === 'MANUAL_PROBABILITY_IMPACT'
      ? calcResidualPi(matrixAxes, residualProbability, residualImpact)
      : calcResidual(residualCeGrid, matrixAxes, controlEff, previewInherent)
  const previewInherentScore = excelScore(matrixAxes, probability, impact)
  const residualManualError =
    residualScoringMode === 'MANUAL_PROBABILITY_IMPACT'
      ? excelManualScoreError(manualResidualScore, matrixGridSize, {
          required: true,
          max: previewInherentScore,
        })
      : ''
  const autoPreviewResidual = autoResidualScore(
    previewInherentScore,
    ceReductionPercent(matrixGridSize, colRank(matrixAxes, controlEff)),
  )
  const previewResidualScore =
    residualScoringMode === 'MANUAL_PROBABILITY_IMPACT' && !residualManualError
      ? Number(manualResidualScore)
      : autoPreviewResidual
  const previewInherentBand = excelBandLabel(matrixGridSize, previewInherentScore)
  const previewResidualBand =
    previewResidualScore != null
      ? excelBandLabel(matrixGridSize, previewResidualScore)
      : fmtLevel(previewResidual)
  const previewInherentAction = excelAction(matrixGridSize, previewInherentScore)
  const previewResidualAction =
    previewResidualScore != null
      ? excelAction(matrixGridSize, previewResidualScore)
      : `${CONTROL_EFFECTIVENESS_LABEL} × Inherent (auto)`
  const previewPriority = priorityFromResidual(previewResidual, matrixGridSize)
  const willAutoClose = excelAutoClosable(matrixGridSize, previewResidualScore)
  const appetiteMax =
    matrixMeta?.appetiteMaxResidual ??
    appetiteMaxResidual(matrixGridSize, (matrixMeta?.appetiteRating as RiskLevel | undefined) ?? 'LOW')
  const previewReductionPct = `${Math.round(
    ceReductionPercent(matrixGridSize, colRank(matrixAxes, controlEff)) * 100,
  )}%`
  const previewWithinAppetite = withinAppetiteLabel(
    previewResidualScore != null ? previewResidualScore <= appetiteMax : null,
  )
  const previewEvalDecision = willAutoClose
    ? evaluationDecisionLabel('AUTO_CLOSE')
    : previewResidualScore != null && previewResidualScore <= appetiteMax
      ? evaluationDecisionLabel('MONITOR')
      : evaluationDecisionLabel('TREATMENT_REQUIRED')
  const previewTargetResidualScore = autoResidualScore(
    previewInherentScore,
    ceReductionPercent(matrixGridSize, colRank(matrixAxes, targetControlEff)),
  )
  const previewTargetResidualBand = excelBandLabel(matrixGridSize, previewTargetResidualScore)
  const previewAcceptanceRequired = acceptanceRequiredLabel(
    treatmentOption,
    previewTargetResidualScore,
    appetiteMax,
  )

  const userLabel = useCallback(
    (id: number | null | undefined) => {
      if (id == null) return '-'
      const u = users.find((x) => x.id === id)
      return u ? `${u.fullName} (${u.email})` : `User #${id}`
    },
    [users],
  )
  const orgLabel = useCallback(
    (id: number | null | undefined) => {
      if (id == null) return '-'
      const o = flatOrgs.find((x) => x.id === id)
      return o ? o.name : `Org unit #${id}`
    },
    [flatOrgs],
  )
  const assetLabel = useCallback(
    (id: number | null | undefined) => {
      if (id == null) return '-'
      const a = assets.find((x) => x.id === id)
      return a ? `${a.name} (${a.criticality ?? '-'})` : `Asset #${id}`
    },
    [assets],
  )

  const load = useCallback(async () => {
    const seq = ++loadSeq.current
    setBusy(true)
    const homeOnly = selectedProjectId == null && !acceptanceView && drill == null
    if (!homeOnly) {
      setRisksReady(false)
    }
    try {
      const filters =
        drill == null
          ? undefined
          : drill.scoreBasis === 'INHERENT'
            ? {
                probability: drill.rowLevel,
                impact: drill.colLevel,
                openOnly: drill.openOnly,
              }
            : {
                ...(drill.scoreBasis === 'RESIDUAL'
                  ? {
                      residualProbability: drill.rowLevel,
                      residualImpact: drill.colLevel,
                      controlEffectiveness: drill.rowLevel,
                      inherentRisk: drill.colLevel,
                    }
                  : {}),
                openOnly: drill.openOnly,
              }
      const projectRowsP = api.listRiskProjects(riskType).catch(() => [] as RiskProjectSummary[])
      const listP = homeOnly
        ? Promise.resolve(null as RiskSummary[] | null)
        : api.listRisks(riskType, {
            ...filters,
            projectId: selectedProjectId ?? undefined,
            ...(acceptanceView ? { status: 'ACCEPTED' } : {}),
          })
      const kpisP =
        homeOnly && canAdmin
          ? api.listRiskKpis(riskType).catch(() => null)
          : Promise.resolve(null as RiskKpis | null)
      const draftsP =
        canAdmin && selectedProjectId != null && !acceptanceView
          ? api.listRiskDrafts(riskType, selectedProjectId).catch(() => [] as RiskFormDraft[])
          : Promise.resolve([] as RiskFormDraft[])
      const [list, projectRows, kpis, drafts] = await Promise.all([listP, projectRowsP, kpisP, draftsP])
      if (seq !== loadSeq.current) return
      setProjects(projectRows)
      setRiskDrafts(drafts)
      if (homeOnly) {
        setRows([])
        setHomeKpis(kpis)
        setRisksReady(true)
      } else {
        setRows(list ?? [])
        setHomeKpis(null)
        setRisksReady(true)
      }
      setBusy(false)
      markReady()

      if (catalogsReadyRef.current) {
        return
      }

      const catalogJobs: Promise<unknown>[] = [
        api
          .listOrgUnits(false)
          .then((units) => {
            if (seq !== loadSeq.current) return
            setOrgUnits(units)
          })
          .catch(() => {
            if (seq !== loadSeq.current) return
            setOrgUnits([])
          }),
        api
          .getRiskMatrices()
          .then((m) => {
            if (seq !== loadSeq.current) return
            setInherentGrid(m.inherent)
            setResidualGrid(m.residual)
            setMatrixMeta(m)
          })
          .catch(() => {
            /* keep defaults */
          }),
      ]
      if (canAdmin) {
        catalogJobs.push(
          api
            .listUsers()
            .then((u) => {
              if (seq !== loadSeq.current) return
              setUsers(u)
            })
            .catch(() => {
              if (seq !== loadSeq.current) return
              setUsers([])
            }),
        )
      }
      if (!isErm) {
        catalogJobs.push(
          api
            .listAssets()
            .then((assetList) => {
              if (seq !== loadSeq.current) return
              setAssets(assetList.filter((a) => a.status === 'ACTIVE'))
            })
            .catch(() => {
              if (seq !== loadSeq.current) return
              setAssets([])
            }),
          api
            .listActivatedFrameworks()
            .then((activated) => {
              if (seq !== loadSeq.current) return
              setFrameworks(
                activated.filter((f) =>
                  IS_RISK_FRAMEWORK_MODULES.includes(
                    f.moduleCode as (typeof IS_RISK_FRAMEWORK_MODULES)[number],
                  ),
                ),
              )
            })
            .catch(() => {
              if (seq !== loadSeq.current) return
              setFrameworks([])
            }),
        )
      } else {
        setAssets([])
        setFrameworks([])
      }
      void Promise.all(catalogJobs).then(() => {
        if (seq !== loadSeq.current) return
        setCatalogsReady(true)
      })
    } catch (e) {
      if (seq !== loadSeq.current) return
      onError(String(e))
      setBusy(false)
      setRisksReady(true)
      markReady()
    }
  }, [canAdmin, drill, isErm, onError, riskType, markReady, selectedProjectId, acceptanceView])

  useEffect(() => {
    setDrill(initialDrill)
  }, [initialDrill])

  useEffect(() => {
    if (!openFromNotification) return
    if (onOpenAssignedRisk) {
      void Promise.resolve(onOpenAssignedRisk(openFromNotification.riskId, selectedProjectId)).then((opened) => {
        if (opened === false) void openDetail(openFromNotification.riskId)
      })
      return
    }
    void openDetail(openFromNotification.riskId)
    // epoch is the click identity.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [openFromNotification?.epoch])

  useEffect(() => {
    if (!detail) {
      setLinkedEvidence([])
      return
    }
    setDetailEvidenceStage(riskEvidenceStageFor(detail))
    let cancelled = false
    void api
      .listEvidenceLinked('RISK', 'RISK_RECORD', String(detail.id))
      .then((rows) => {
        if (!cancelled) setLinkedEvidence(rows)
      })
      .catch(() => {
        if (!cancelled) setLinkedEvidence([])
      })
    return () => {
      cancelled = true
    }
  }, [detail])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    if (!isPrivacy || formMode !== 'create') {
      setDpiaRiskCandidates([])
      return
    }
    let cancelled = false
    void api
      .listDpiaRiskTitleCandidates()
      .then((rows) => {
        if (!cancelled) setDpiaRiskCandidates(rows.filter((r) => !r.raisedRiskId))
      })
      .catch(() => {
        if (!cancelled) setDpiaRiskCandidates([])
      })
    return () => {
      cancelled = true
    }
  }, [isPrivacy, formMode])

  useEffect(() => {
    setDetail(null)
    setFormMode('none')
    resetCreateForm()
  }, [isErm])

  useEffect(() => {
    if (frameworkId === '') {
      setFrameworkDetail(null)
      setFrameworkNodeId('')
      return
    }
    let cancelled = false
    void api
      .getTenantFramework(frameworkId)
      .then((d) => {
        if (!cancelled) setFrameworkDetail(d)
      })
      .catch(() => {
        if (!cancelled) setFrameworkDetail(null)
      })
    return () => {
      cancelled = true
    }
  }, [frameworkId])

  useEffect(() => {
    if (isErm) return
    const p = projects.find((x) => x.id === selectedProjectId)
    if (p?.frameworkId != null) setFrameworkId(p.frameworkId)
  }, [isErm, selectedProjectId, projects, formMode])

  useEffect(() => {
    if (isErm || frameworkId === '' || frameworkNodeId === '') {
      setControlAssessmentStatus(null)
      return
    }
    let cancelled = false
    void api
      .getControlAssessmentStatus(frameworkId, frameworkNodeId)
      .then((s) => {
        if (!cancelled) setControlAssessmentStatus(s)
      })
      .catch(() => {
        if (!cancelled) setControlAssessmentStatus(null)
      })
    return () => {
      cancelled = true
    }
  }, [isErm, frameworkId, frameworkNodeId])

  useEffect(() => {
    const unitId =
      formMode === 'create'
        ? orgUnitId === ''
          ? null
          : orgUnitId
        : (detail?.orgUnitId ?? null)
    if (unitId == null) {
      setEligibleContributors([])
      return
    }
    let cancelled = false
    void api
      .listRiskEligibleContributors(unitId)
      .then((rows) => {
        if (cancelled) return
        setEligibleContributors(rows)
        setAssignUserId((prev) => (prev !== '' && rows.some((u) => u.id === prev) ? prev : ''))
      })
      .catch(() => {
        if (!cancelled) {
          setEligibleContributors([])
          setAssignUserId('')
        }
      })
    return () => {
      cancelled = true
    }
  }, [assignOpen, detail?.orgUnitId, formMode, orgUnitId])

  const resetCreateForm = () => {
    setFieldErrors({})
    setTitle('')
    setSourceDpiaAssessmentRowId('')
    setDescription('')
    setStrategicObjective('')
    setFinancialImpactAed('')
    setObservedGap('')
    setThreats('')
    setAdminNotes('')
    setExistingControls('')
    setTreatmentPlan('')
    setPlanActionOwnerId('')
    setPlanCustodianUserId('')
    setPlanStartDate('')
    setPlanTargetDate('')
    setPlanEvidenceRequired('')
    setDisplayIdOverride('')
    setOverrideAutoClose(false)
    setAutoCloseJustification('')
    const projectFw = !isErm
      ? projects.find((x) => x.id === selectedProjectId)?.frameworkId ?? null
      : null
    if (projectFw != null) {
      setFrameworkId(projectFw)
    } else {
      setFrameworkId('')
      setFrameworkDetail(null)
    }
    setFrameworkNodeId('')
    if (projectFw == null) {
      setControlAssessmentStatus(null)
    }
    setAssetId('')
    setOrgUnitId('')
    setRiskOwnerUserId('')
    setProbability('MEDIUM')
    setImpact('MEDIUM')
    setControlEff('MEDIUM')
    setResidualScoringMode('CONTROL_EFFECTIVENESS')
    setResidualProbability('MEDIUM')
    setResidualImpact('MEDIUM')
    setManualResidualScore('')
    setCause('')
    setCauseOther('')
    setDateIdentified(todayIso())
    setNextReviewDate('')
    setThirdPartyName('')
    setTreatmentJustification('')
    setTargetControlEff('HIGH')
    setCompensatingControls('')
    setAssignUserId('')
    setTreatmentOption('TREAT')
    setCategory(isErm ? '' : 'IT')
    setCategoryOther('')
    setInfosecStatusInitial('')
    setCreateEvidenceFiles([])
    setCreateQueuedReferences([])
    setCreateEvidenceClass('')
    setCopyFromLabel(null)
    setActiveDraftId(null)
    setDraftAttachments([])
    setDraftBaseline(null)
    draftBaselinePending.current = false
  }

  const openDetail = async (id: number) => {
    try {
      const r = await api.getRisk(id)
      setDetail(r)
      setRejectReason('')
      setMilestoneTitle('')
      setAdminAcceptComments('')
      setAdminDeclaration(false)
      setOwnerDeclaration(false)
      setOwnerComments('')
      setReopenReason('')
      setInfosecStatusFinal(r.infosecStatusFinal ?? '')
      setPostTargetCe(r.targetControlEffectiveness ?? '')
      setAdminAction('')
      setActionRemarks('')
      setApproveNextReviewDate(r.nextReviewDate ?? maxToleranceReviewIso())
      const plan = r.activeTreatmentPlan
      if (plan) {
        setTreatmentPlan(plan.actionDescription ?? r.treatmentPlan ?? '')
        setPlanActionOwnerId(plan.actionOwnerId ?? '')
        setPlanCustodianUserId(plan.custodianUserId ?? '')
        setPlanStartDate(plan.startDate ?? '')
        setPlanTargetDate(plan.targetDate ?? '')
        setPlanEvidenceRequired(plan.evidenceRequired ?? '')
      } else {
        setTreatmentPlan(r.treatmentPlan ?? '')
        setPlanActionOwnerId('')
        setPlanCustodianUserId('')
        setPlanStartDate('')
        setPlanTargetDate('')
        setPlanEvidenceRequired('')
      }
      setChangeTreatmentOption(r.treatmentOption ?? 'TREAT')
      setChangeTreatmentJustification('')
      setCloseProbability(r.probability)
      setCloseImpact(r.impact)
      setCloseControlEff(r.controlEffectiveness)
      setCloseNotes('')
      setNoteBody('')
      setNoteInternalOnly(false)
      setDetailEvidenceFile(null)
      setDetailEvidenceClass('')
      const [hist, notes] = await Promise.all([
        api.listRiskScoreHistory(id),
        api.listRiskNotes(id),
      ])
      setScoreHistory(hist)
      setStageNotes(notes)
    } catch (e) {
      onError(String(e))
    }
  }

  const refreshNotesAndHistory = async (id: number) => {
    const [hist, notes] = await Promise.all([
      api.listRiskScoreHistory(id),
      api.listRiskNotes(id),
    ])
    setScoreHistory(hist)
    setStageNotes(notes)
  }

  const clearFieldError = useCallback((id: string) => {
    setFieldErrors((prev) => {
      if (!prev[id]) return prev
      const next = { ...prev }
      delete next[id]
      return next
    })
  }, [])

  const create = async (): Promise<boolean> => {
    if (!selectedProjectId) {
      onError('Open a risk project before creating a risk')
      return false
    }
    if (residualManualError) {
      onError(residualManualError)
      return false
    }
    const errors = collectRiskRequiredErrors({
      isErm,
      dateIdentified,
      title,
      description,
      observedGap,
      category,
      categoryOther,
      orgUnitId,
      strategicObjective,
      cause,
      causeOther,
      threats,
      riskOwnerUserId,
      residualScoringMode,
      manualResidualScore,
      willAutoClose,
      overrideAutoClose,
      autoCloseJustification,
      treatmentOption,
      treatmentJustification,
    })
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors)
      window.setTimeout(() => focusFirstRiskError(errors), 0)
      return false
    }
    setFieldErrors({})
    setBusy(true)
    try {
      const fin =
        isErm && financialImpactAed.trim() !== '' ? Number(financialImpactAed) : null
      if (fin != null && Number.isNaN(fin)) {
        onError('Financial impact must be a number')
        return false
      }
      const fw = frameworks.find((f) => f.id === frameworkId)
      const node = selectedNode
      const body = {
        riskType,
        displayIdOverride: displayIdOverride.trim() || null,
        title: title.trim(),
        description: description.trim() || null,
        category: isErm ? (category as RiskCategory) : 'IT',
        categoryOther: isErm && category === 'OTHER' ? categoryOther.trim() : null,
        orgUnitId: orgUnitId === '' ? null : orgUnitId,
        assetId: !isErm && assetId !== '' ? assetId : null,
        frameworkId: !isErm && frameworkId !== '' ? frameworkId : null,
        frameworkCode: !isErm ? fw?.code ?? null : null,
        frameworkNodeId: !isErm && frameworkNodeId !== '' ? frameworkNodeId : null,
        nodeCode: !isErm ? node?.code ?? null : null,
        strategicObjective: isErm ? strategicObjective.trim() : null,
        financialImpactAed: isErm ? fin : null,
        observedGap: observedGap.trim() || null,
        threats: !isErm ? threats.trim() || null : null,
        probability,
        impact,
        controlEffectiveness: controlEff,
        residualScoringMode,
        residualProbability:
          residualScoringMode === 'MANUAL_PROBABILITY_IMPACT' ? residualProbability : null,
        residualImpact: residualScoringMode === 'MANUAL_PROBABILITY_IMPACT' ? residualImpact : null,
        existingControls: existingControls.trim() || null,
        treatmentOption,
        treatmentPlan: null,
        treatmentJustification: treatmentJustification.trim() || null,
        compensatingControls: null,
        treatmentPlanDetail:
          planTargetDate || planEvidenceRequired.trim()
            ? {
                actionDescription: null,
                actionOwnerId: null,
                custodianUserId: null,
                startDate: null,
                targetDate: planTargetDate || null,
                evidenceRequired: planEvidenceRequired.trim() || null,
              }
            : null,
        riskOwnerUserId: riskOwnerUserId === '' ? null : riskOwnerUserId,
        adminNotes: canAdmin && adminNotes.trim() ? adminNotes.trim() : null,
        overrideAutoClose: willAutoClose ? overrideAutoClose : false,
        autoCloseOverrideJustification:
          willAutoClose && overrideAutoClose ? autoCloseJustification.trim() : null,
        infosecStatusInitial: !isErm && infosecStatusInitial.trim() ? infosecStatusInitial.trim() : null,
        riskProjectId: selectedProjectId,
        dateIdentified: dateIdentified || todayIso(),
        nextReviewDate: null,
        thirdPartyName: null,
        cause: isErm ? ermCauseFromForm(cause, causeOther) : null,
        targetControlEffectiveness: null,
        residualScore:
          residualScoringMode === 'MANUAL_PROBABILITY_IMPACT' && manualResidualScore.trim()
            ? Number(manualResidualScore)
            : null,
        assignedContributorId: assignUserId === '' ? null : assignUserId,
        sourceDpiaAssessmentRowId:
          isPrivacy && sourceDpiaAssessmentRowId !== '' ? Number(sourceDpiaAssessmentRowId) : null,
      }
      const queued = [...createQueuedReferences]
      if (createEvidenceFiles[0] && createEvidenceClass) {
        queued.push({ file: createEvidenceFiles[0], classification: createEvidenceClass })
      }
      const fromDraft = formMode !== 'edit' ? draftAttachments.length : 0
      if (queued.length + fromDraft > MAX_REFERENCE_ATTACHMENTS) {
        onError(`You can attach at most ${MAX_REFERENCE_ATTACHMENTS} files to this risk`)
        return false
      }
      const r =
        formMode === 'edit' && detail
          ? await api.updateRisk(detail.id, body)
          : activeDraftId != null
            ? await api.createRiskFromDraft(activeDraftId, body)
            : await api.createRisk(body)
      if (formMode !== 'edit' && queued.length > 0) {
        for (const item of queued) {
          await api.uploadEvidence({
            file: item.file,
            classification: item.classification,
            title: item.file.name,
            moduleCode: 'RISK',
            recordType: 'RISK_RECORD',
            recordId: String(r.id),
            workflowStage: 'IDENTIFICATION',
          })
        }
      }
      setFormMode('none')
      resetCreateForm()
      onOk(
        r.status === 'AUTO_CLOSED'
          ? `${r.displayId} auto-closed (residual ${r.residualRisk}): residual VL/L closes at creation`
          : formMode === 'edit'
            ? `${r.displayId} updated`
            : queued.length + fromDraft > 0
              ? `${r.displayId} created with ${queued.length + fromDraft} reference attachment${queued.length + fromDraft === 1 ? '' : 's'}`
              : `${r.displayId} created`,
      )
      await load()
      if (formMode === 'edit') {
        setDetail(r)
      }
      return true
    } catch (e) {
      const msg = String(e)
      if (msg.toLowerCase().includes('already exists')) {
        const errors = { [RISK_INPUT_ID.title]: msg }
        setFieldErrors(errors)
        window.setTimeout(() => focusFirstRiskError(errors), 0)
      }
      onError(msg)
      return false
    } finally {
      setBusy(false)
    }
  }

  const openEdit = (record: RiskSummary | null = detail) => {
    if (!record || (record.status !== 'DRAFT' && record.status !== 'RE_OPENED')) return
    setTitle(record.title)
    setDescription(record.description ?? '')
    setCategory(record.category)
    setCategoryOther(record.categoryOther ?? '')
    setOrgUnitId(record.orgUnitId ?? '')
    setAssetId(record.assetId ?? '')
    setFrameworkId(record.frameworkId ?? '')
    setFrameworkNodeId(record.frameworkNodeId ?? '')
    setStrategicObjective(record.strategicObjective ?? '')
    setFinancialImpactAed(
      record.financialImpactAed != null ? String(record.financialImpactAed) : '',
    )
    setObservedGap(record.observedGap ?? '')
    setThreats(record.threats ?? '')
    setProbability(record.probability)
    setImpact(record.impact)
    setControlEff(record.controlEffectiveness)
    const loadedCause = ermCauseToForm(record.cause)
    setCause(loadedCause.cause)
    setCauseOther(loadedCause.causeOther)
    setDateIdentified(record.dateIdentified ?? todayIso())
    setNextReviewDate(record.nextReviewDate ?? '')
    setThirdPartyName(record.thirdPartyName ?? '')
    setTreatmentJustification(record.treatmentChangeJustification ?? '')
    setTargetControlEff(record.targetControlEffectiveness ?? 'HIGH')
    setCompensatingControls(record.transferCompensatingControls ?? '')
    setManualResidualScore(
      record.residualScoringMode === 'MANUAL_PROBABILITY_IMPACT' && record.residualScore != null
        ? String(record.residualScore)
        : '',
    )
    setResidualScoringMode(record.residualScoringMode ?? 'CONTROL_EFFECTIVENESS')
    setResidualProbability(record.residualProbability ?? record.probability)
    setResidualImpact(record.residualImpact ?? record.impact)
    setExistingControls(record.existingControls ?? '')
    setTreatmentOption(record.treatmentOption ?? 'TREAT')
    setTreatmentPlan(record.treatmentPlan ?? '')
    setRiskOwnerUserId(record.riskOwnerUserId ?? '')
    setAdminNotes(record.adminNotes ?? '')
    setInfosecStatusInitial(record.infosecStatusInitial ?? '')
    setOverrideAutoClose(!!record.autoCloseOverride)
    setAutoCloseJustification(record.autoCloseOverrideJustification ?? '')
    setDisplayIdOverride('')
    setFieldErrors({})
    if (record.activeTreatmentPlan) {
      setPlanActionOwnerId(record.activeTreatmentPlan.actionOwnerId ?? '')
      setPlanCustodianUserId(record.activeTreatmentPlan.custodianUserId ?? '')
      setPlanStartDate(record.activeTreatmentPlan.startDate ?? '')
      setPlanTargetDate(record.activeTreatmentPlan.targetDate ?? '')
      setPlanEvidenceRequired(record.activeTreatmentPlan.evidenceRequired ?? '')
    } else {
      setPlanActionOwnerId('')
      setPlanCustodianUserId('')
      setPlanStartDate('')
      setPlanTargetDate('')
      setPlanEvidenceRequired('')
    }
    setDetail(record)
    setFormMode('edit')
  }

  const openEditFromRegister = async (id: number) => {
    try {
      const r = await api.getRisk(id)
      openEdit(r)
    } catch (e) {
      onError(String(e))
    }
  }

  const applyCopiedRisk = (source: RiskSummary) => {
    if (selectedProjectId == null) {
      onError('Open a risk project before copying a risk')
      return
    }
    resetCreateForm()
    setTitle(nextCopyTitle(source.title, scopedRows.map((r) => r.title)))
    setDisplayIdOverride('')
    setDateIdentified(todayIso())
    setDescription(source.description ?? '')
    setCategory(isErm ? source.category : 'IT')
    setCategoryOther(source.categoryOther ?? '')
    setOrgUnitId(source.orgUnitId ?? '')
    setAssetId(source.assetId ?? '')
    setFrameworkId(source.frameworkId ?? '')
    setFrameworkNodeId(source.frameworkNodeId ?? '')
    setStrategicObjective(source.strategicObjective ?? '')
    setFinancialImpactAed(
      source.financialImpactAed != null ? String(source.financialImpactAed) : '',
    )
    setObservedGap(source.observedGap ?? '')
    setThreats(source.threats ?? '')
    setProbability(source.probability)
    setImpact(source.impact)
    setControlEff(source.controlEffectiveness)
    const loadedCause = ermCauseToForm(source.cause)
    setCause(loadedCause.cause)
    setCauseOther(loadedCause.causeOther)
    setTreatmentJustification(source.treatmentChangeJustification ?? '')
    setManualResidualScore(
      source.residualScoringMode === 'MANUAL_PROBABILITY_IMPACT' && source.residualScore != null
        ? String(source.residualScore)
        : '',
    )
    setResidualScoringMode(source.residualScoringMode ?? 'CONTROL_EFFECTIVENESS')
    setResidualProbability(source.residualProbability ?? source.probability)
    setResidualImpact(source.residualImpact ?? source.impact)
    setExistingControls(source.existingControls ?? '')
    setTreatmentOption(source.treatmentOption ?? 'TREAT')
    setRiskOwnerUserId(source.riskOwnerUserId ?? '')
    setInfosecStatusInitial(source.infosecStatusInitial ?? '')
    setPlanTargetDate(source.activeTreatmentPlan?.targetDate ?? '')
    setPlanEvidenceRequired(source.activeTreatmentPlan?.evidenceRequired ?? '')
    setAssignUserId(source.assignedContributorId ?? '')
    setCopyFromLabel(source.displayId)
    setDetail(null)
    setFormMode('create')
  }

  const requestCopyFromRegister = (id: number) => {
    void (async () => {
      setBusy(true)
      try {
        const source = await api.getRisk(id)
        applyCopiedRisk(source)
      } catch (e) {
        onError(String(e))
      } finally {
        setBusy(false)
      }
    })()
  }

  const planEditable =
    detail != null && ['MITIGATING', 'OVERDUE'].includes(detail.status)

  const saveTreatmentPlan = async () => {
    if (!detail) return
    setBusy(true)
    try {
      const r = await api.upsertRiskTreatmentPlan(detail.id, {
        actionDescription: treatmentPlan.trim() || null,
        actionOwnerId: planActionOwnerId === '' ? null : planActionOwnerId,
        custodianUserId: planCustodianUserId === '' ? null : planCustodianUserId,
        startDate: planStartDate || null,
        targetDate: planTargetDate || null,
        evidenceRequired: planEvidenceRequired.trim() || null,
        markInProgress: detail.status === 'MITIGATING' || detail.status === 'OVERDUE',
      })
      setDetail(r)
      onOk('Treatment plan saved')
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const saveOwner = async () => {
    if (!detail || ownerPick === '') return
    setBusy(true)
    try {
      const r = await api.reassignRiskOwner(detail.id, ownerPick)
      setOwnerOpen(false)
      setDetail(r)
      onOk('Risk Owner updated')
      await load()
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const assignNeedsTargetDate =
    detail != null && detail.riskProjectId != null && !detail.activeTreatmentPlan?.targetDate
  const assignBlockedReason: string | null =
    detail?.orgUnitId == null
      ? `Set ${EXCEL_FIELD.division} on the risk first`
      : eligibleContributors.length === 0
        ? `No ${EXCEL_FIELD.riskCustodian} in this ${EXCEL_FIELD.division.toLowerCase()}`
        : assignNeedsTargetDate
          ? `Set the ${EXCEL_FIELD.targetDate} on the risk first`
          : assignUserId === ''
            ? `Select a ${EXCEL_FIELD.riskCustodian}`
            : null

  const assign = async () => {
    if (!detail || assignUserId === '' || assignBlockedReason != null) return
    setBusy(true)
    try {
      const r = await api.assignRisk(detail.id, {
        userId: assignUserId,
        treatmentOption: detail.treatmentOption ?? 'TREAT',
        treatmentPlan: detail.treatmentPlan,
      })
      setAssignOpen(false)
      setAssignUserId('')
      setDetail(r)
      setRejectReason('')
      setAdminAcceptComments('')
      setOwnerComments('')
      setReopenReason('')
      setMilestoneTitle('')
      setChangeTreatmentJustification('')
      setCloseNotes('')
      setNoteBody('')
      setInfosecStatusFinal(r.infosecStatusFinal ?? '')
      setCloseProbability(r.probability)
      setCloseImpact(r.impact)
      setCloseControlEff(r.controlEffectiveness)
      await load()
      onOk('Risk assigned to Contributor')
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const run = async (fn: () => Promise<RiskSummary>, okMsg: string) => {
    setBusy(true)
    try {
      const r = await fn()
      setDetail(r)
      setRejectReason('')
      setAdminAcceptComments('')
      setOwnerComments('')
      setReopenReason('')
      setMilestoneTitle('')
      setChangeTreatmentJustification('')
      setCloseNotes('')
      setNoteBody('')
      setDetailEvidenceFile(null)
      await load()
      try {
        await refreshNotesAndHistory(r.id)
      } catch {
        /* detail still usable without history refresh */
      }
      onOk(okMsg)
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const meId = users.find((u) => u.email === session.email)?.id
  const myRiskEvidenceCount = linkedEvidence.filter(
    (e) => meId != null && e.createdByUserId === meId,
  ).length
  const isRiskOwner =
    detail?.riskOwnerUserId != null &&
    users.find((u) => u.email === session.email)?.id === detail.riskOwnerUserId
  const newRiskLabel = isErm ? 'New ERM Risk' : isPrivacy ? 'New Privacy Risk' : 'New Information Security Risk'
  const newRiskButtonLabel = isErm ? '+ New ERM Risk' : isPrivacy ? '+ New Privacy Risk' : '+ New Information Security Risk'
  const emptyMsg = isPrivacy
    ? 'No Privacy Risks yet. Raise one from a DPIA, or add a Privacy Risk here.'
    : isErm
    ? 'No ERM risks yet.'
    : 'No Information Security Risks yet. Create one or import the template. Import adds rows; it does not replace existing risks.'
  const terminal = ['CLOSED', 'AUTO_CLOSED', 'AVOIDED', 'TRANSFERRED', 'TOLERATED', 'ACCEPTED']
  const selectedProject = projects.find((p) => p.id === selectedProjectId) ?? null
  const detailIndex = detail ? scopedRows.findIndex((r) => r.id === detail.id) : -1

  const riskKpis = useMemo(() => {
    if (selectedProjectId == null && !acceptanceView && drill == null && homeKpis) {
      return homeKpis
    }
    return computeRiskKpis(scopedRows, matrixGridSize)
  }, [acceptanceView, drill, homeKpis, matrixGridSize, scopedRows, selectedProjectId])
  const registerSourceRows = useMemo(
    () => (acceptanceView ? scopedRows.filter((r) => r.status === 'ACCEPTED') : scopedRows),
    [acceptanceView, scopedRows],
  )
  const waitingApprovalCount = useMemo(
    () => registerSourceRows.filter((r) => waitingMyApproval(r, meId, canAdmin)).length,
    [registerSourceRows, meId, canAdmin],
  )
  const filteredRows = useMemo(
    () =>
      filterRiskRows(registerSourceRows, {
        search,
        statusFilter,
        priorityFilter,
        gridSize: matrixGridSize,
        waitingMyApproval: waitingApprovalOnly,
        meId,
        isAdmin: canAdmin,
      }),
    [registerSourceRows, search, statusFilter, priorityFilter, matrixGridSize, waitingApprovalOnly, meId, canAdmin],
  )
  const displayRows = useMemo(() => {
    if (!sortKey) return filteredRows
    return sortRiskRows(filteredRows, sortKey, sortDir)
  }, [filteredRows, sortKey, sortDir])
  const riskPaged = usePagedRows(displayRows)
  const projectPaged = usePagedRows(projects)
  const acceptanceProjectRows = useMemo(() => {
    const counts = new Map<number, number>()
    for (const row of rows) {
      if (row.status === 'ACCEPTED' && row.riskProjectId != null) {
        counts.set(row.riskProjectId, (counts.get(row.riskProjectId) ?? 0) + 1)
      }
    }
    const q = acceptanceProjectQuery.trim().toLowerCase()
    return projects
      .map((project) => ({ project, accepted: counts.get(project.id) ?? 0 }))
      .filter((row) => row.accepted > 0)
      .filter((row) => !q || `${row.project.name} ${row.project.year}`.toLowerCase().includes(q))
      .sort((a, b) => b.accepted - a.accepted || a.project.name.localeCompare(b.project.name))
  }, [acceptanceProjectQuery, projects, rows])
  const acceptanceProjectTotals = useMemo(() => {
    const counts = new Map<number, number>()
    for (const row of rows) {
      if (row.status === 'ACCEPTED' && row.riskProjectId != null) {
        counts.set(row.riskProjectId, (counts.get(row.riskProjectId) ?? 0) + 1)
      }
    }
    let accepted = 0
    let projectCount = 0
    for (const project of projects) {
      const n = counts.get(project.id) ?? 0
      if (n > 0) {
        projectCount += 1
        accepted += n
      }
    }
    return { projectCount, accepted }
  }, [projects, rows])
  const acceptanceProjectPaged = usePagedRows(acceptanceProjectRows)
  const assignedWorkMode = Boolean(onOpenAssignedRisk)
  const assignedProjectTotals = useMemo(() => {
    let assignedCount = 0
    let needsActionCount = 0
    for (const p of projects) {
      assignedCount += p.riskCount
      needsActionCount += p.needsActionCount ?? 0
    }
    return { projectCount: projects.length, assignedCount, needsActionCount }
  }, [projects])

  const filtersActive = useMemo(
    () => hasActiveRiskFilters({ search, statusFilter, priorityFilter, waitingMyApproval: waitingApprovalOnly }),
    [search, statusFilter, priorityFilter, waitingApprovalOnly],
  )

  const draftTableRows = useMemo((): RiskDraftRow[] => {
    if (
      acceptanceView ||
      drill != null ||
      statusFilter !== 'ALL' ||
      priorityFilter !== 'ALL' ||
      waitingApprovalOnly
    ) {
      return []
    }
    const q = search.trim().toLowerCase()
    const text = (v: unknown) => (typeof v === 'string' ? v.trim() : '')
    const id = (v: unknown) => (typeof v === 'number' ? v : null)
    const level = (v: unknown, labels: Record<string, string>) =>
      typeof v === 'string' && labels[v] ? labels[v] : ''
    return riskDrafts
      .filter((d) => !q || (d.title ?? '').toLowerCase().includes(q))
      .map((d) => {
        const p = d.payload
        const cat = text(p.category)
        const ownerId = id(p.riskOwnerUserId)
        const owner = ownerId == null ? '' : (users.find((u) => u.id === ownerId)?.fullName ?? '')
        const unitId = id(p.orgUnitId)
        const custodianId = id(p.assignUserId)
        const custodian = custodianId == null ? '' : (users.find((u) => u.id === custodianId)?.fullName ?? '')
        const treatment = text(p.treatmentOption)
        const riskLevel = (v: unknown) =>
          typeof v === 'string' && (ALL_RISK_LEVELS as readonly string[]).includes(v) ? (v as RiskLevel) : null
        const prob = riskLevel(p.probability)
        const imp = riskLevel(p.impact)
        const ce = riskLevel(p.controlEff)
        const inherentRisk = prob && imp ? calcInherent(inherentGrid, matrixAxes, prob, imp) : null
        const manualScore = Number(text(p.manualResidualScore))
        const residualRisk =
          p.residualScoringMode === 'MANUAL_PROBABILITY_IMPACT'
            ? text(p.manualResidualScore) && Number.isFinite(manualScore) && manualScore > 0
              ? excelLevelFromScore(matrixGridSize, manualScore)
              : null
            : inherentRisk && ce && prob && imp
              ? calcResidual(inherentGrid, matrixAxes, ce, inherentRisk, prob, imp)
              : null
        const cause = text(p.cause)
        return {
          id: d.id,
          title: d.title?.trim() || 'Untitled draft',
          untitled: !d.title?.trim(),
          treatmentOption: treatment ? (treatment as TreatmentOption) : null,
          inherentRisk,
          residualRisk,
          cells: {
            custodian,
            cause: cause.startsWith('Other') && text(p.causeOther) ? `Other: ${text(p.causeOther)}` : cause,
            nextReviewDate: text(p.nextReviewDate),
            treatmentPlan: text(p.treatmentPlan),
            targetCe: level(p.targetControlEff, CE_EXCEL_LABEL),
            category: !isErm
              ? ''
              : cat === 'OTHER' && text(p.categoryOther)
                ? `Other: ${text(p.categoryOther)}`
                : cat.replace(/_/g, ' '),
            owner,
            orgUnit: unitId == null ? '' : orgLabel(unitId),
            dateIdentified: text(p.dateIdentified),
            description: text(p.description),
            observedGap: text(p.observedGap),
            strategicObjective: text(p.strategicObjective),
            threats: text(p.threats),
            existingControls: text(p.existingControls),
            likelihood: level(p.probability, LIKELIHOOD_EXCEL_LABEL),
            impact: level(p.impact, LIKELIHOOD_EXCEL_LABEL),
            controlEffectiveness: level(p.controlEff, CE_EXCEL_LABEL),
            treatmentDescription: text(p.treatmentJustification),
            evidenceRequired: text(p.planEvidenceRequired),
            targetDate: text(p.planTargetDate),
            referenceAttachment: d.attachmentCount > 0 ? `${d.attachmentCount} file${d.attachmentCount === 1 ? '' : 's'}` : '',
          },
        }
      })
  }, [
    acceptanceView,
    drill,
    statusFilter,
    priorityFilter,
    waitingApprovalOnly,
    search,
    riskDrafts,
    users,
    orgLabel,
    isErm,
    inherentGrid,
    matrixAxes,
    matrixGridSize,
  ])

  const clearRegisterFilters = useCallback(() => {
    setSearch('')
    setStatusFilter('ALL')
    setPriorityFilter('ALL')
    setWaitingApprovalOnly(false)
    setDrill(null)
    onClearDrill?.()
  }, [onClearDrill])

  const registerColumns = useMemo(
    () => riskRegisterColumnsForType(isErm, matrixGridSize),
    [isErm, matrixGridSize],
  )
  const riskColumnVisibility = useColumnVisibility(registerColumns, {
    maxVisible: RISK_REGISTER_MAX_COLUMNS,
  })

  const handleSort = useCallback((key: RiskSortKey) => {
    setSortKey((prev) => {
      if (prev === key) {
        setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))
        return key
      }
      setSortDir('asc')
      return key
    })
  }, [])

  const runExport = useCallback(async () => {
    setExportBusy(true)
    try {
      await api.exportReport(isErm ? 'erm_risk' : 'is_risk', 'xlsx')
      onOk(`${isErm ? 'ERM' : 'IS'} risk register exported`)
    } catch (e) {
      onError(String(e))
    } finally {
      setExportBusy(false)
    }
  }, [isErm, onError, onOk])

  const runDetailExport = useCallback(async () => {
    if (!detail) return
    setDetailExportBusy(true)
    try {
      await api.exportRisk(detail.id, 'xlsx')
      onOk(`${detail.displayId || (isErm ? 'ERM' : 'IS')} risk exported`)
    } catch (e) {
      onError(String(e))
    } finally {
      setDetailExportBusy(false)
    }
  }, [detail, isErm, onError, onOk])

  const downloadBulkTemplate = useCallback(async () => {
    setBusy(true)
    try {
      await api.downloadRiskBulkTemplate(riskType, selectedProjectId)
      onOk(`${isErm ? 'ERM' : 'IS'} risk bulk template downloaded`)
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }, [isErm, onError, onOk, riskType, selectedProjectId])

  const runBulkImport = useCallback(
    async (file: File) => {
      setBulkImportBusy(true)
      setBulkImportResult(null)
      try {
        const result = await api.importRiskBulk(riskType, file, selectedProjectId)
        setBulkImportResult(result)
        await load()
        if (result.failedCount === 0) {
          onOk(`Created ${result.createdCount} ${isErm ? 'ERM' : 'IS'} risk(s)`)
        } else {
          onOk(
            `Created ${result.createdCount}; ${result.failedCount} row(s) failed: see report`,
          )
        }
      } catch (e) {
        onError(String(e))
      } finally {
        setBulkImportBusy(false)
      }
    },
    [isErm, load, onError, onOk, riskType, selectedProjectId],
  )

  const openBulkImport = useCallback(() => {
    setBulkImportResult(null)
    setBulkImportOpen(true)
  }, [])

  const openMatrices = useCallback(() => {
    const size = parseGridSize(matrixMeta?.gridSize)
    const axes = axesFromConfig(size, matrixMeta?.rowLevels, matrixMeta?.colLevels)
    setEditGridSize(size)
    setEditRowLevels([...axes.rows])
    setEditColLevels([...axes.cols])
    setEditInherent(inherentGrid.map((r) => [...r]))
    setEditResidual(residualGrid.map((r) => [...r]))
    setMatrixChangeReason('')
    setMatricesOpen(true)
  }, [inherentGrid, matrixMeta?.colLevels, matrixMeta?.gridSize, matrixMeta?.rowLevels, residualGrid])

  const applyEditAxes = useCallback(
    (rows: RiskLevel[], cols: RiskLevel[]) => {
      const axes = { rows, cols }
      setEditRowLevels(rows)
      setEditColLevels(cols)
      setEditInherent(defaultInherent(editGridSize, axes))
      setEditResidual(defaultResidual(editGridSize, axes))
    },
    [editGridSize],
  )

  const openCreateRisk = useCallback(() => {
    if (selectedProjectId == null) {
      onError('Create or open a risk project first')
      return
    }
    resetCreateForm()
    setDetail(null)
    setFormMode('create')
  }, [resetCreateForm, selectedProjectId, onError])

  const selectProject = useCallback((id: number | null, stayOnAcceptance = false) => {
    setSelectedProjectId(id)
    setAcceptanceView(stayOnAcceptance)
    setDetail(null)
    setFormMode('none')
  }, [])

  const upsertProject = useCallback((row: RiskProjectSummary) => {
    setProjects((prev) => [row, ...prev.filter((x) => x.id !== row.id)])
  }, [])

  const refreshProjects = useCallback(async () => {
    try {
      const projectRows = await api.listRiskProjects(riskType)
      setProjects(projectRows)
    } catch {
      /* keep the list we already have */
    }
  }, [riskType])

  const projectNameTaken = projects.some(
    (p) => p.name.trim().toLowerCase() === newProjectName.trim().toLowerCase(),
  )

  const creatingFramework = frameworks.find((f) => f.id === newProjectFrameworkId)
  const creatingFrameworkLabel = creatingFramework ? frameworkLabel(creatingFramework) : null

  const createProject = async () => {
    if (!newProjectName.trim()) return
    if (projectNameTaken) {
      onError(`A project named "${newProjectName.trim()}" already exists`)
      return
    }
    const year = Number(newProjectYear)
    if (!Number.isFinite(year) || year < 2000) {
      onError('Enter a valid year')
      return
    }
    if (!isErm && !isPrivacy && newProjectFrameworkId === '') {
      onError('Information Security Risk projects need a framework')
      return
    }
    setProjectCreating(true)
    setSeedProgress({ done: 0, total: 0 })
    setBusy(true)
    try {
      const p = await api.createRiskProject({
        projectType: riskType,
        name: newProjectName.trim(),
        year,
        frameworkId: !isErm && !isPrivacy && newProjectFrameworkId !== '' ? newProjectFrameworkId : null,
      })
      let finished = p
      upsertProject(p)
      setSeedProgress({
        done: p.riskCount,
        total: p.seedTotal ?? 0,
      })
      if (p.seedStatus === 'RUNNING') {
        const deadline = Date.now() + 8 * 60 * 1000
        while (Date.now() < deadline) {
          await new Promise((r) => window.setTimeout(r, 400))
          finished = await api.getRiskProject(p.id)
          upsertProject(finished)
          setSeedProgress({
            done: finished.riskCount,
            total: finished.seedTotal || finished.riskCount,
          })
          if (finished.seedStatus === 'DONE') break
          if (finished.seedStatus === 'FAILED') {
            await refreshProjects()
            onError('Seeding default risks failed. The project was created; open it from the list.')
            setNewProjectName('')
            setNewProjectFrameworkId('')
            return
          }
        }
        if (finished.seedStatus !== 'DONE') {
          await refreshProjects()
          onError('Seeding is still running. The project is on the list; open it in a moment.')
          setNewProjectName('')
          setNewProjectFrameworkId('')
          return
        }
      }
      await refreshProjects()
      onOk(
        !isErm && !isPrivacy && finished.riskCount > 0
          ? `${finished.name} created with ${finished.riskCount} default risk(s)`
          : `${finished.name} created`,
      )
      setNewProjectName('')
      setNewProjectFrameworkId('')
      selectProject(finished.id)
    } catch (e) {
      onError(String(e))
      await refreshProjects()
    } finally {
      setBusy(false)
      setProjectCreating(false)
    }
  }

  const saveTargetScoring = async (ce: RiskLevel) => {
    if (!detail) return
    setBusy(true)
    try {
      const r = await api.postTreatmentRisk(detail.id, {
        targetControlEffectiveness: ce,
        targetResidualScore: null,
        targetResidualManual: false,
      })
      setDetail(r)
      await load()
      requestAnimationFrame(() => {
        requestAnimationFrame(() => revealAfterSubmissionAndActions())
      })
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const openHeatMap = useCallback(() => setHeatMapOpen(true), [])

  const moduleListLabel = isErm ? 'Enterprise Risk' : isPrivacy ? 'Privacy Risk' : 'Information Security Risk'
  const registerTitle = acceptanceView
    ? selectedProject
      ? selectedProject.name
      : 'Risk Acceptance'
    : selectedProject
      ? selectedProject.name
      : assignedWorkMode
        ? moduleListLabel
        : isErm
          ? 'ERM Risk Projects'
          : isPrivacy
            ? 'Privacy Risk Projects'
            : 'Information Security Risk projects'
  const registerBreadcrumbRoot = acceptanceView
    ? selectedProject
      ? { label: 'Risk Acceptance', onClick: () => selectProject(null, true) }
      : undefined
    : selectedProject
      ? lockToProject
        ? undefined
        : { label: moduleListLabel, onClick: () => selectProject(null) }
      : undefined
  const registerBreadcrumb = acceptanceView
    ? selectedProject
      ? String(selectedProject.year)
      : isErm
        ? 'Risk Management › Enterprise Risk'
        : isPrivacy
          ? 'Privacy › Privacy Risk'
          : 'Risk Management › Information Security Risk'
    : selectedProject
      ? assignedWorkMode
        ? selectedProject.name
        : String(selectedProject.year)
      : assignedWorkMode
        ? 'Projects'
        : isErm
          ? 'Risk Management › Enterprise Risk'
          : isPrivacy
            ? 'Privacy › Privacy Risk'
            : 'Risk Management › Information Security Risk'

  const registerActions = useMemo(
    () => (
      <>
        {acceptanceView ? (
          selectedProjectId != null ? (
            <Button
              variant="outlined"
              size="small"
              disabled={busy}
              onClick={() => selectProject(null, true)}
            >
              Projects
            </Button>
          ) : null
        ) : (
          <>
            {selectedProjectId != null && !lockToProject && (
              <Button variant="outlined" size="small" disabled={busy} onClick={() => selectProject(null)}>
                All Projects
              </Button>
            )}
            {selectedProjectId != null && (
              <Button variant="outlined" size="small" disabled={busy} onClick={openHeatMap}>
                Heat Map
              </Button>
            )}
            {canAdmin && selectedProjectId != null && (
              <Button variant="contained" size="small" disabled={busy} onClick={openCreateRisk}>
                {newRiskButtonLabel}
              </Button>
            )}
          </>
        )}
      </>
    ),
    [
      acceptanceView,
      busy,
      canAdmin,
      lockToProject,
      openHeatMap,
      openCreateRisk,
      newRiskButtonLabel,
      selectedProjectId,
      selectProject,
    ],
  )

  const detailDirty = useMemo(() => {
    if (!detail) return false
    const excel = detail.riskProjectId != null
    const reviewOpen = detail.status === 'SUBMITTED' || detail.status === 'UNDER_VALIDATION'
    // Excel dossier Close: Identification is read-only. Hidden close-score / InfoSec
    // leftovers from another record must not prompt after Assign.
    if (
      excel &&
      !reviewOpen &&
      detail.status !== 'PENDING_ADMIN_REVIEW' &&
      detail.status !== 'PENDING_ACCEPTANCE'
    ) {
      return detailEvidenceFile != null
    }
    return (
      Boolean(noteBody.trim()) ||
      (reviewOpen && Boolean(rejectReason.trim())) ||
      Boolean(adminAcceptComments.trim()) ||
      Boolean(ownerComments.trim()) ||
      Boolean(reopenReason.trim()) ||
      Boolean(milestoneTitle.trim()) ||
      Boolean(changeTreatmentJustification.trim()) ||
      Boolean(closeNotes.trim()) ||
      detailEvidenceFile != null ||
      infosecStatusFinal !== (detail.infosecStatusFinal ?? '') ||
      closeProbability !== detail.probability ||
      closeImpact !== detail.impact ||
      closeControlEff !== detail.controlEffectiveness
    )
  }, [
    detail,
    noteBody,
    rejectReason,
    adminAcceptComments,
    ownerComments,
    reopenReason,
    milestoneTitle,
    changeTreatmentJustification,
    closeNotes,
    detailEvidenceFile,
    infosecStatusFinal,
    closeProbability,
    closeImpact,
    closeControlEff,
  ])

  const treatPlanTargetError = useMemo(
    () => (treatmentOption === 'TREAT' ? actionDueDateError(planTargetDate) : ''),
    [treatmentOption, planTargetDate],
  )
  const treatPlanRangeError = useMemo(
    () => (treatmentOption === 'TREAT' ? dateRangeError(planStartDate, planTargetDate) : ''),
    [treatmentOption, planStartDate, planTargetDate],
  )
  const treatPlanDateError = treatPlanTargetError || treatPlanRangeError
  const milestoneDueDateError = useMemo(() => actionDueDateError(milestoneDueDate), [milestoneDueDate])
  const dateMin = todayIso()

  const detailDirtyRef = useRef(false)
  detailDirtyRef.current = detailDirty
  const runIfCleanOrConfirm = useCallback((action: () => void) => {
    if (!detailDirtyRef.current) {
      action()
      return
    }
    pendingDetailActionRef.current = action
    setDiscardDialogOpen(true)
  }, [])

  const requestCloseDetail = useCallback(() => {
    runIfCleanOrConfirm(() => setDetail(null))
  }, [runIfCleanOrConfirm])

  const requestCopyFromDetail = () => {
    if (!detail) return
    runIfCleanOrConfirm(() => applyCopiedRisk(detail))
  }

  const requestOpenDetail = useCallback(
    (id: number) => {
      if (onOpenAssignedRisk) {
        void Promise.resolve(onOpenAssignedRisk(id, selectedProjectId)).then((opened) => {
          if (opened === false) void openDetail(id)
        })
        return
      }
      if (detail && detail.id !== id) {
        runIfCleanOrConfirm(() => void openDetail(id))
        return
      }
      void openDetail(id)
    },
    [detail, runIfCleanOrConfirm, onOpenAssignedRisk, selectedProjectId],
  )

  const submittedReview =
    detail != null && ['SUBMITTED', 'UNDER_VALIDATION'].includes(detail.status)
  const excelClick = detail != null && detail.riskProjectId != null
  const excelPostApprove =
    detail != null && isExcelFinishedRow(detail.status, detail.finalRiskStatus)
  const showExcelPostSubmit =
    detail != null && showPostSubmitFields(detail.status, detail.finalRiskStatus)
  const hasContributorWork =
    detail != null &&
    (Boolean(detail.activeTreatmentPlan?.actionDescription?.trim() || detail.treatmentPlan?.trim()) ||
      linkedEvidence.length > 0)

  const canUploadDetailEvidence =
    canAdmin &&
    detail != null &&
    !['CLOSED', 'AUTO_CLOSED', 'SUBMITTED', 'UNDER_VALIDATION', 'PENDING_ACCEPTANCE'].includes(
      detail.status,
    )

  const uploadDetailEvidence = useCallback(() => {
    if (!detailEvidenceFile || !detail) return
    if (!detailEvidenceClass) {
      onError('Select a classification')
      return
    }
    if (detailEvidenceFile.size > RISK_EVIDENCE_MAX_BYTES) {
      onError('File is too large. Maximum size is 10MB.')
      return
    }
    if (myRiskEvidenceCount >= RISK_EVIDENCE_MAX_FILES) {
      onError('You can attach at most 5 files to this risk')
      return
    }
    setBusy(true)
    void api
      .uploadEvidence({
        file: detailEvidenceFile,
        classification: detailEvidenceClass,
        title: detailEvidenceFile.name,
        moduleCode: 'RISK',
        recordType: 'RISK_RECORD',
        recordId: String(detail.id),
        workflowStage: detailEvidenceStage,
      })
      .then(async () => {
        setDetailEvidenceFile(null)
        setDetailEvidenceClass('')
        setLinkedEvidence(await api.listEvidenceLinked('RISK', 'RISK_RECORD', String(detail.id)))
        onOk('Evidence linked')
      })
      .catch((e) => onError(String(e)))
      .finally(() => setBusy(false))
  }, [
    detail,
    detailEvidenceClass,
    detailEvidenceFile,
    detailEvidenceStage,
    myRiskEvidenceCount,
    onError,
    onOk,
  ])

  const detailEvidenceUpload = (
    <Box sx={{ maxWidth: MEDIUM_FORM_MAX_WIDTH }}>
    <ContributorEvidencePanel
      showHeading={false}
      linkedEvidence={linkedEvidence}
      evidenceFile={detailEvidenceFile}
      evidenceClass={detailEvidenceClass}
      evidenceClasses={EVIDENCE_CLASSIFICATIONS}
      maxBytes={RISK_EVIDENCE_MAX_BYTES}
      maxFiles={RISK_EVIDENCE_MAX_FILES}
      currentUserId={meId}
      busy={busy}
      disabled={!canUploadDetailEvidence}
      emptyLabel="No linked evidence yet"
      onError={onError}
      onSelectFile={setDetailEvidenceFile}
      onEvidenceClass={setDetailEvidenceClass}
      onUpload={uploadDetailEvidence}
      onDownload={(id) => void api.downloadEvidence(id).catch((e) => onError(String(e)))}
      trailingFields={
        <FormSelect
          label="Stage"
          minWidth={160}
          value={detailEvidenceStage}
          onChange={(v) => setDetailEvidenceStage(v as RiskEvidenceStage)}
          options={RISK_EVIDENCE_STAGES.map((s) => ({
            value: s,
            label: s
              .toLowerCase()
              .replace(/_/g, ' ')
              .replace(/\b\w/g, (ch) => ch.toUpperCase()),
          }))}
        />
      }
    />
    </Box>
  )

  const viewFormSections = useMemo(() => {
    if (!detail) return RISK_DETAIL_SECTIONS
    if (excelClick) {
      return detail.acceptanceForm
        ? [...EXCEL_DOSSIER_SECTIONS, { id: 'risk-section-acceptance', label: 'Acceptance' }]
        : VIEW_EXCEL_SECTIONS
    }
    return RISK_DETAIL_SECTIONS.filter((s) => {
      if (s.id === 'risk-section-evidence') return linkedEvidence.length > 0 || canUploadDetailEvidence
      if (s.id === 'risk-section-notes') return stageNotes.length > 0
      if (s.id === 'risk-section-ownership' || s.id === 'risk-section-meta') return !submittedReview
      if (s.id === 'risk-section-actions') return false
      return true
    }).map((s) => (s.id === 'risk-section-scoring' ? { ...s, label: 'Analysis' } : s))
  }, [detail, canUploadDetailEvidence, linkedEvidence.length, stageNotes.length, submittedReview, excelClick])

  const canHeaderEdit =
    canAdmin && detail != null && (detail.status === 'DRAFT' || detail.status === 'RE_OPENED')
  const canHeaderAssign =
    canAdmin &&
    detail != null &&
    (detail.status === 'DRAFT' || detail.status === 'RE_OPENED' || detail.status === 'ASSIGNED')
  const canReassignOwner =
    canAdmin &&
    detail != null &&
    !['CLOSED', 'AUTO_CLOSED', 'ACCEPTED', 'AVOIDED', 'TRANSFERRED', 'CANCELLED', 'TOLERATED'].includes(
      detail.status,
    )
  const canHeaderCancel = canAdmin && detail != null && detail.status === 'ASSIGNED'
  const canHeaderCopy = canAdmin && detail != null && selectedProjectId != null
  const showDetailWorkflowActions = Boolean(
    detail &&
      !(excelClick && excelPostApprove) &&
      (!submittedReview || canAdmin) &&
      ((canAdmin &&
        detail.riskProjectId == null &&
        ![
          'DRAFT',
          'ASSIGNED',
          'RE_OPENED',
          'SUBMITTED',
          'UNDER_VALIDATION',
          'PENDING_ACCEPTANCE',
          'PENDING_ADMIN_REVIEW',
        ].includes(detail.status)) ||
        (canAdmin && (detail.status === 'SUBMITTED' || detail.status === 'UNDER_VALIDATION')) ||
        (canAdmin &&
          detail.riskProjectId == null &&
          (detail.status === 'MITIGATING' || detail.status === 'OVERDUE')) ||
        (detail.status === 'PENDING_ACCEPTANCE' &&
          (waitingMyApproval(detail, meId, false) || !detail.acceptanceForm)) ||
        (canAdmin && detail.status === 'PENDING_ADMIN_REVIEW') ||
        (canAdmin && detail.riskProjectId == null && terminal.includes(detail.status))),
  )

  const detailBreadcrumbTrail = useMemo(() => {
    if (!detail) return ''
    const parts: string[] = []
    const drillLabel = drillFilterLabel(drill)
    if (drillLabel) parts.push(drillLabel)
    parts.push(
      detail.title,
      detail.category,
      `Residual ${fmtLevel(detail.residualRating ?? detail.residualRisk)}`,
    )
    return parts.join(' · ')
  }, [detail, drill])

  const detailActions = useMemo(() => {
    if (!detail) return null
    return (
      <Stack direction="row" spacing={1} alignItems="center" flexWrap="nowrap" useFlexGap>
        <RiskDetailNav
          rows={scopedRows}
          detail={detail}
          detailIndex={detailIndex}
          onSelectRisk={requestOpenDetail}
        />
        {canReassignOwner && (
          <Button
            variant="outlined"
            size="small"
            disabled={busy}
            onClick={() => {
              setOwnerPick(detail.riskOwnerUserId ?? '')
              setOwnerOpen(true)
            }}
          >
            {EXCEL_FIELD.riskOwner}
          </Button>
        )}
        {canHeaderAssign && (
          <Button
            variant="contained"
            size="small"
            disabled={busy}
            onClick={() => {
              setAssignUserId('')
              setAssignOpen(true)
            }}
          >
            {detail.status === 'ASSIGNED'
              ? `Reassign ${EXCEL_FIELD.riskCustodian}`
              : `Assign ${EXCEL_FIELD.riskCustodian}`}
          </Button>
        )}
        {canHeaderCancel && (
          <Button
            variant="outlined"
            size="small"
            disabled={busy}
            onClick={() => {
              setCancelReason('')
              setCancelOpen(true)
            }}
          >
            Cancel Risk
          </Button>
        )}
        <Stack direction="row" spacing={1} alignItems="center" flexWrap="nowrap" useFlexGap>
          <HeaderActionStatus label={headerActionStatusLabel(detail.status, excelClick)} />
          {canExport && (
            <BusyButton
              size="small"
              variant="outlined"
              busy={detailExportBusy}
              onClick={() => void runDetailExport()}
              sx={{ textTransform: 'none', fontWeight: 600 }}
            >
              Export
            </BusyButton>
          )}
          {canHeaderCopy && (
            <Button variant="outlined" size="small" disabled={busy} onClick={requestCopyFromDetail}>
              Copy Risk
            </Button>
          )}
          {canHeaderEdit && (
            <Button variant="outlined" size="small" disabled={busy} onClick={() => openEdit()}>
              Edit
            </Button>
          )}
          <Button variant="outlined" size="small" disabled={busy} onClick={requestCloseDetail}>
            Close
          </Button>
        </Stack>
      </Stack>
    )
  }, [
    detail,
    busy,
    requestCloseDetail,
    canHeaderEdit,
    canHeaderAssign,
    canReassignOwner,
    canHeaderCopy,
    canHeaderCancel,
    requestCopyFromDetail,
    excelClick,
    scopedRows,
    detailIndex,
    requestOpenDetail,
    canExport,
    detailExportBusy,
    runDetailExport,
    openEdit,
  ])

  const formSections = useMemo(
    () => visibleFormSections(formMode === 'edit', canAdmin),
    [formMode, canAdmin],
  )

  const createDisabledReason =
    selectedProjectId == null
      ? 'Open a risk project before creating a risk'
      : !catalogsReady
        ? 'Loading lists'
        : treatPlanDateError
        ? treatPlanDateError
        : residualManualError
          ? residualManualError
          : isErm && !category
            ? `${EXCEL_FIELD.riskCategory} is required`
            : !treatmentJustification.trim()
              ? `${EXCEL_FIELD.treatmentJustification} is required`
              : undefined

  const createDisabled = busy || Boolean(createDisabledReason)

  const cancelForm = useCallback(() => {
    setFormMode('none')
    resetCreateForm()
  }, [resetCreateForm])

  const formSnapshot = useMemo(
    () => ({
      title,
      description,
      category,
      categoryOther,
      orgUnitId,
      assetId,
      strategicObjective,
      financialImpactAed,
      observedGap,
      threats,
      probability,
      impact,
      controlEff,
      residualScoringMode,
      manualResidualScore,
      residualProbability,
      residualImpact,
      cause,
      causeOther,
      dateIdentified,
      nextReviewDate,
      thirdPartyName,
      treatmentJustification,
      targetControlEff,
      compensatingControls,
      existingControls,
      treatmentOption,
      treatmentPlan,
      planActionOwnerId,
      planCustodianUserId,
      planStartDate,
      planTargetDate,
      planEvidenceRequired,
      riskOwnerUserId,
      assignUserId,
      adminNotes,
      overrideAutoClose,
      autoCloseJustification,
      displayIdOverride,
      frameworkId,
      frameworkNodeId,
      infosecStatusInitial,
    }),
    [
      title,
      description,
      category,
      categoryOther,
      orgUnitId,
      assetId,
      strategicObjective,
      financialImpactAed,
      observedGap,
      threats,
      probability,
      impact,
      controlEff,
      residualScoringMode,
      manualResidualScore,
      residualProbability,
      residualImpact,
      cause,
      causeOther,
      dateIdentified,
      nextReviewDate,
      thirdPartyName,
      treatmentJustification,
      targetControlEff,
      compensatingControls,
      existingControls,
      treatmentOption,
      treatmentPlan,
      planActionOwnerId,
      planCustodianUserId,
      planStartDate,
      planTargetDate,
      planEvidenceRequired,
      riskOwnerUserId,
      assignUserId,
      adminNotes,
      overrideAutoClose,
      autoCloseJustification,
      displayIdOverride,
      frameworkId,
      frameworkNodeId,
      infosecStatusInitial,
    ],
  )

  useEffect(() => {
    if (formMode === 'create' && draftBaselinePending.current) {
      draftBaselinePending.current = false
      setDraftBaseline(JSON.stringify({ ...formSnapshot, createEvidenceName: '' }))
    }
  }, [formMode, formSnapshot])

  const formDirty = useMemo(() => {
    if (formMode === 'none') return false
    const snap = {
      ...formSnapshot,
      createEvidenceName: [
        ...createQueuedReferences.map((item) => item.file.name),
        ...createEvidenceFiles.map((f) => f.name),
      ].join('|'),
    }
    if (formMode === 'create' && activeDraftId != null) {
      if (draftBaselinePending.current || draftBaseline == null) return false
      return JSON.stringify(snap) !== draftBaseline
    }
    if (formMode === 'create') {
      return JSON.stringify(snap) !== JSON.stringify({
        title: '',
        description: '',
        category: isErm ? '' : 'IT',
        categoryOther: '',
        orgUnitId: '',
        assetId: '',
        strategicObjective: '',
        financialImpactAed: '',
        observedGap: '',
        threats: '',
        probability: 'MEDIUM',
        impact: 'MEDIUM',
        controlEff: 'MEDIUM',
        residualScoringMode: 'CONTROL_EFFECTIVENESS',
        manualResidualScore: '',
        residualProbability: 'MEDIUM',
        residualImpact: 'MEDIUM',
        cause: '',
        causeOther: '',
        dateIdentified: todayIso(),
        nextReviewDate: '',
        thirdPartyName: '',
        treatmentJustification: '',
        targetControlEff: 'HIGH',
        compensatingControls: '',
        existingControls: '',
        treatmentOption: 'TREAT',
        treatmentPlan: '',
        planActionOwnerId: '',
        planCustodianUserId: '',
        planStartDate: '',
        planTargetDate: '',
        planEvidenceRequired: '',
        riskOwnerUserId: '',
        assignUserId: '',
        adminNotes: '',
        overrideAutoClose: false,
        autoCloseJustification: '',
        displayIdOverride: '',
        frameworkId: !isErm
          ? (projects.find((x) => x.id === selectedProjectId)?.frameworkId ?? '')
          : '',
        frameworkNodeId: '',
        infosecStatusInitial: '',
        createEvidenceName: '',
      })
    }
    if (formMode === 'edit' && detail) {
      const plan = detail.activeTreatmentPlan
      return JSON.stringify(snap) !== JSON.stringify({
        title: detail.title,
        description: detail.description ?? '',
        category: detail.category,
        categoryOther: detail.categoryOther ?? '',
        orgUnitId: detail.orgUnitId ?? '',
        assetId: detail.assetId ?? '',
        strategicObjective: detail.strategicObjective ?? '',
        financialImpactAed:
          detail.financialImpactAed != null ? String(detail.financialImpactAed) : '',
        observedGap: detail.observedGap ?? '',
        threats: detail.threats ?? '',
        probability: detail.probability,
        impact: detail.impact,
        controlEff: detail.controlEffectiveness,
        residualScoringMode: detail.residualScoringMode ?? 'CONTROL_EFFECTIVENESS',
        manualResidualScore:
          detail.residualScoringMode === 'MANUAL_PROBABILITY_IMPACT' && detail.residualScore != null
            ? String(detail.residualScore)
            : '',
        residualProbability: detail.residualProbability ?? detail.probability,
        residualImpact: detail.residualImpact ?? detail.impact,
        cause: ermCauseToForm(detail.cause).cause,
        causeOther: ermCauseToForm(detail.cause).causeOther,
        dateIdentified: detail.dateIdentified ?? todayIso(),
        nextReviewDate: detail.nextReviewDate ?? '',
        thirdPartyName: detail.thirdPartyName ?? '',
        treatmentJustification: detail.treatmentChangeJustification ?? '',
        targetControlEff: detail.targetControlEffectiveness ?? 'HIGH',
        compensatingControls: detail.transferCompensatingControls ?? '',
        existingControls: detail.existingControls ?? '',
        treatmentOption: detail.treatmentOption ?? 'TREAT',
        treatmentPlan: detail.treatmentPlan ?? '',
        planActionOwnerId: plan?.actionOwnerId ?? '',
        planCustodianUserId: plan?.custodianUserId ?? '',
        planStartDate: plan?.startDate ?? '',
        planTargetDate: plan?.targetDate ?? '',
        planEvidenceRequired: plan?.evidenceRequired ?? '',
        riskOwnerUserId: detail.riskOwnerUserId ?? '',
        assignUserId: '',
        adminNotes: detail.adminNotes ?? '',
        overrideAutoClose: !!detail.autoCloseOverride,
        autoCloseJustification: detail.autoCloseOverrideJustification ?? '',
        displayIdOverride: '',
        frameworkId: detail.frameworkId ?? '',
        frameworkNodeId: detail.frameworkNodeId ?? '',
        infosecStatusInitial: detail.infosecStatusInitial ?? '',
        createEvidenceName: '',
      })
    }
    return false
  }, [
    formMode,
    formSnapshot,
    activeDraftId,
    draftBaseline,
    title,
    description,
    category,
    categoryOther,
    orgUnitId,
    assetId,
    strategicObjective,
    financialImpactAed,
    observedGap,
    threats,
    probability,
    impact,
    controlEff,
    residualScoringMode,
    manualResidualScore,
    residualProbability,
    residualImpact,
    cause,
    causeOther,
    dateIdentified,
    nextReviewDate,
    thirdPartyName,
    treatmentJustification,
    targetControlEff,
    compensatingControls,
    existingControls,
    treatmentOption,
    treatmentPlan,
    planActionOwnerId,
    planCustodianUserId,
    planStartDate,
    planTargetDate,
    planEvidenceRequired,
    riskOwnerUserId,
    assignUserId,
    adminNotes,
    overrideAutoClose,
    autoCloseJustification,
    displayIdOverride,
    frameworkId,
    frameworkNodeId,
    infosecStatusInitial,
    createEvidenceFiles,
    createQueuedReferences,
    detail,
    isErm,
    projects,
    selectedProjectId,
  ])
  const createIncomplete =
    formMode === 'create' &&
    (Boolean(createDisabledReason) ||
      Object.keys(
        collectRiskRequiredErrors({
          isErm,
          dateIdentified,
          title,
          description,
          observedGap,
          category,
          categoryOther,
          orgUnitId,
          strategicObjective,
          cause,
          causeOther,
          threats,
          riskOwnerUserId,
          residualScoringMode,
          manualResidualScore,
          willAutoClose,
          overrideAutoClose,
          autoCloseJustification,
          treatmentOption,
          treatmentJustification,
        }),
      ).length > 0)

  const refreshDrafts = useCallback(async () => {
    if (!canAdmin || selectedProjectId == null || acceptanceView) {
      setRiskDrafts([])
      return
    }
    try {
      setRiskDrafts(await api.listRiskDrafts(riskType, selectedProjectId))
    } catch {
      /* keep current list */
    }
  }, [canAdmin, selectedProjectId, acceptanceView, riskType])

  const saveDraft = async (): Promise<boolean> => {
    if (selectedProjectId == null) {
      onError('Open a risk project before saving a draft')
      return false
    }
    const queued = [...createQueuedReferences]
    if (createEvidenceFiles[0] && createEvidenceClass) {
      queued.push({ file: createEvidenceFiles[0], classification: createEvidenceClass })
    }
    if (queued.length + draftAttachments.length > MAX_REFERENCE_ATTACHMENTS) {
      onError(`You can attach at most ${MAX_REFERENCE_ATTACHMENTS} files to this risk`)
      return false
    }
    setBusy(true)
    try {
      const saved = await api.saveRiskDraft(activeDraftId, {
        riskType,
        riskProjectId: selectedProjectId,
        payload: { ...formSnapshot },
      })
      for (const item of queued) {
        await api.uploadEvidence({
          file: item.file,
          classification: item.classification,
          title: item.file.name,
          moduleCode: 'RISK',
          recordType: 'RISK_DRAFT',
          recordId: String(saved.id),
          workflowStage: 'IDENTIFICATION',
        })
      }
      setCreateQueuedReferences([])
      setCreateEvidenceFiles([])
      setCreateEvidenceClass('')
      setActiveDraftId(saved.id)
      setDraftAttachments(await api.listEvidenceLinked('RISK', 'RISK_DRAFT', String(saved.id)))
      draftBaselinePending.current = true
      setDraftBaseline(null)
      setFieldErrors({})
      onOk('Draft saved. It stays in this project until you create the risk or delete the draft.')
      void refreshDrafts()
      return true
    } catch (e) {
      onError(String(e))
      return false
    } finally {
      setBusy(false)
    }
  }

  const applyDraftPayload = (p: Record<string, unknown>) => {
    const put = (key: string, set: (v: never) => void) => {
      const v = p[key]
      if (v !== undefined && v !== null) set(v as never)
    }
    put('title', setTitle)
    put('description', setDescription)
    put('category', setCategory)
    put('categoryOther', setCategoryOther)
    put('orgUnitId', setOrgUnitId)
    put('assetId', setAssetId)
    put('strategicObjective', setStrategicObjective)
    put('financialImpactAed', setFinancialImpactAed)
    put('observedGap', setObservedGap)
    put('threats', setThreats)
    put('probability', setProbability)
    put('impact', setImpact)
    put('controlEff', setControlEff)
    put('residualScoringMode', setResidualScoringMode)
    put('manualResidualScore', setManualResidualScore)
    put('residualProbability', setResidualProbability)
    put('residualImpact', setResidualImpact)
    put('cause', setCause)
    put('causeOther', setCauseOther)
    put('dateIdentified', setDateIdentified)
    put('nextReviewDate', setNextReviewDate)
    put('thirdPartyName', setThirdPartyName)
    put('treatmentJustification', setTreatmentJustification)
    put('targetControlEff', setTargetControlEff)
    put('compensatingControls', setCompensatingControls)
    put('existingControls', setExistingControls)
    put('treatmentOption', setTreatmentOption)
    put('treatmentPlan', setTreatmentPlan)
    put('planActionOwnerId', setPlanActionOwnerId)
    put('planCustodianUserId', setPlanCustodianUserId)
    put('planStartDate', setPlanStartDate)
    put('planTargetDate', setPlanTargetDate)
    put('planEvidenceRequired', setPlanEvidenceRequired)
    put('riskOwnerUserId', setRiskOwnerUserId)
    put('assignUserId', setAssignUserId)
    put('adminNotes', setAdminNotes)
    put('overrideAutoClose', setOverrideAutoClose)
    put('autoCloseJustification', setAutoCloseJustification)
    put('displayIdOverride', setDisplayIdOverride)
    put('frameworkId', setFrameworkId)
    put('frameworkNodeId', setFrameworkNodeId)
    put('infosecStatusInitial', setInfosecStatusInitial)
  }

  const openDraft = async (draftId: number) => {
    setBusy(true)
    try {
      const [d, files] = await Promise.all([
        api.getRiskDraft(draftId),
        api.listEvidenceLinked('RISK', 'RISK_DRAFT', String(draftId)),
      ])
      resetCreateForm()
      applyDraftPayload(d.payload)
      setActiveDraftId(d.id)
      setDraftAttachments(files)
      setFieldErrors({})
      draftBaselinePending.current = true
      setDetail(null)
      setFormMode('create')
    } catch (e) {
      onError(String(e))
      void refreshDrafts()
    } finally {
      setBusy(false)
    }
  }

  const confirmDeleteDraft = async () => {
    const target = deleteDraftTarget
    if (!target) return
    setBusy(true)
    try {
      await api.deleteRiskDraft(target.id)
      setDeleteDraftTarget(null)
      onOk('Draft deleted')
      await refreshDrafts()
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const riskLeaveNav = useOptionalUnsavedChangesGuardApi()
  useRegisterUnsavedChanges(
    formMode !== 'none' && formDirty
      ? {
          dirty: true,
          title: 'Leave without saving this risk?',
          message: createIncomplete
            ? 'Some required fields are still empty. Save as draft keeps what you entered in this project so you can finish later. Discard to leave without keeping it.'
            : 'You have unsaved risk edits. Save to keep them, or discard to leave.',
          saveDraftLabel: createIncomplete ? 'Save as Draft' : 'Save',
          onSaveDraft: async () => {
            const ok = createIncomplete ? await saveDraft() : await create()
            if (!ok) throw new Error('not saved')
          },
        }
      : null,
  )
  useBeforeUnload(formDirty || projectCreating)

  const requestCancelForm = useCallback(() => {
    if (!formDirty) {
      cancelForm()
      return
    }
    if (riskLeaveNav) {
      riskLeaveNav.requestNavigation(cancelForm)
      return
    }
    pendingDetailActionRef.current = cancelForm
    setDiscardDialogOpen(true)
  }, [formDirty, cancelForm, riskLeaveNav])

  const formActions = useMemo(
    () => (
      <>
        <Button variant="outlined" size="small" disabled={busy} onClick={requestCancelForm}>
          Cancel
        </Button>
        {formMode === 'create' ? (
          <BusyButton
            variant="outlined"
            size="small"
            disabled={busy || !formDirty}
            disabledReason={busy ? undefined : !formDirty ? 'No changes to save' : undefined}
            onClick={() => void saveDraft()}
          >
            Save Draft
          </BusyButton>
        ) : null}
        <BusyButton
          variant="contained"
          size="small"
          disabled={createDisabled}
          disabledReason={createDisabledReason}
          onClick={() => void create()}
        >
          {busy
            ? 'Saving…'
            : formMode === 'edit'
              ? 'Save Changes'
              : isErm
                ? 'Create ERM Risk'
                : isPrivacy
                  ? 'Create Privacy Risk'
                  : 'Create Information Security Risk'}
        </BusyButton>
      </>
    ),
    [busy, requestCancelForm, createDisabled, createDisabledReason, formMode, isErm, create, formDirty, saveDraft],
  )

  useAppShellPage(
    formMode === 'create'
      ? {
          title: newRiskLabel,
          breadcrumbTrail: copyFromLabel
            ? `Copied from ${copyFromLabel}`
            : activeDraftId != null
              ? 'Unfinished draft'
              : 'New',
          breadcrumbRoot: { label: registerTitle, onClick: requestCancelForm },
          actions: formActions,
          hideContextLabel: true,
          hideRoleChips: true,
        }
      : formMode === 'edit' && detail
        ? {
            title: `Edit ${detail.displayId}`,
            breadcrumbTrail: detail.title,
            breadcrumbRoot: { label: registerTitle, onClick: requestCancelForm },
            actions: formActions,
            hideContextLabel: true,
            hideRoleChips: true,
          }
        : detail
          ? {
              title: detail.displayId,
              breadcrumbTrail: excelClick ? detail.title : detailBreadcrumbTrail,
              breadcrumbRoot: { label: registerTitle, onClick: requestCloseDetail },
              actions: detailActions,
              hideContextLabel: true,
              hideRoleChips: true,
            }
          : {
              title: registerTitle,
              breadcrumbTrail: registerBreadcrumb,
              breadcrumbRoot: registerBreadcrumbRoot,
              actions: registerActions,
              hideContextLabel: true,
              hideRoleChips: true,
            },
    [
      formMode,
      copyFromLabel,
      activeDraftId,
      newRiskLabel,
      registerTitle,
      requestCancelForm,
      formActions,
      detail,
      requestCloseDetail,
      detailBreadcrumbTrail,
      excelClick,
      detailActions,
      registerBreadcrumb,
      registerBreadcrumbRoot,
      registerActions,
      acceptanceView,
    ],
  )

  return (
    <Stack spacing={1} sx={{ flex: 1, minHeight: 0, height: '100%', overflow: 'hidden' }}>
      {!detail && formMode === 'none' && acceptanceView && selectedProjectId == null && listReady && risksReady ? (
        <KpiCardGrid columns={2} dense>
          <KpiCard
            label="Projects"
            value={acceptanceProjectTotals.projectCount}
            sub="With at least one accepted risk"
            accentColor={brand.gold}
            compact
          />
          <KpiCard
            label="Accepted risks"
            value={acceptanceProjectTotals.accepted}
            sub={isErm ? 'Enterprise Risk' : isPrivacy ? 'Privacy Risk' : 'Information Security Risk'}
            accentColor={brand.gold}
            compact
          />
        </KpiCardGrid>
      ) : null}
      {!detail && formMode === 'none' && !acceptanceView && (
        assignedWorkMode && selectedProjectId == null ? (
          listReady && projects.length > 0 ? (
            <KpiCardGrid columns={3} dense>
              <KpiCard
                label="Projects"
                value={assignedProjectTotals.projectCount}
                sub="With work assigned to you"
                accentColor={brand.gold}
                compact
              />
              <KpiCard
                label="Assigned to you"
                value={assignedProjectTotals.assignedCount}
                sub="Risks across these projects"
                accentColor={brand.gold}
                compact
              />
              <KpiCard
                label="Needs action"
                value={assignedProjectTotals.needsActionCount}
                sub={
                  assignedProjectTotals.needsActionCount === 0
                    ? 'Nothing waiting on you'
                    : 'Waiting on you'
                }
                accentColor={
                  assignedProjectTotals.needsActionCount > 0
                    ? semantic.risk.medium
                    : semantic.risk.low
                }
                compact
              />
            </KpiCardGrid>
          ) : null
        ) : (
        <KpiCardGrid columns={priorityBandsForGrid(matrixGridSize).length + 1} dense>
          {priorityBandsForGrid(matrixGridSize).map((band) => (
            <KpiCard
              key={band}
              label={fmtLevel(band)}
              value={kpiCountForBand(riskKpis, band)}
              accentColor={RATING_TONE_COLOR[apiLevelTone(band) ?? 'medium']}
              compact
            />
          ))}
          <KpiCard
            label="Total open"
            value={riskKpis.totalOpen}
            accentColor={brand.gold}
            compact
          />
        </KpiCardGrid>
        )
      )}

      {!detail && drill && (
        <Alert
          severity="info"
          onClose={() => {
            setDrill(null)
            onClearDrill?.()
          }}
          sx={{ flexShrink: 0, py: 0.25 }}
        >
          Filtered to open risks · {drill.scoreBasis === 'INHERENT' ? 'Probability' : 'Residual probability'}{' '}
          {String(drill.rowLevel).replace(/_/g, ' ')} ×{' '}
          {drill.scoreBasis === 'INHERENT' ? 'Impact' : 'Residual impact'}{' '}
          {String(drill.colLevel).replace(/_/g, ' ')} · {displayRows.length} shown
        </Alert>
      )}

      <Box sx={{ flex: 1, minHeight: 0, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
        {formMode !== 'none' ? (
          <RiskFormShell sections={formSections}>
            {isPrivacy && formMode === 'create' && (
              <Box sx={{ mb: 1.5 }}>
                <SettingsCard
                  autoHeight
                  title="From DPIA assessment"
                  subtitle="Optional. Pick a Risk Title recorded on a DPIA DPO assessment to prefill this Privacy Risk."
                >
                  <FormSelect
                    label="DPIA risk title"
                    value={sourceDpiaAssessmentRowId === '' ? '' : String(sourceDpiaAssessmentRowId)}
                    options={[
                      {
                        value: '',
                        label:
                          dpiaRiskCandidates.length === 0
                            ? 'No open DPIA risk titles'
                            : 'None - enter manually',
                      },
                      ...dpiaRiskCandidates.map((c) => ({
                        value: String(c.assessmentRowId),
                        label: `${c.riskTitle} (${c.projectName ?? 'DPIA'} / ${c.departmentName ?? 'Department'})`,
                      })),
                    ]}
                    onChange={(v) => {
                      if (!v) {
                        setSourceDpiaAssessmentRowId('')
                        return
                      }
                      const id = Number(v)
                      const c = dpiaRiskCandidates.find((x) => x.assessmentRowId === id)
                      setSourceDpiaAssessmentRowId(id)
                      if (c) {
                        setTitle(c.riskTitle)
                        setDescription(
                          [
                            c.principle ? `DPIA principle: ${c.principle}` : null,
                            c.projectName ? `DPIA project: ${c.projectName}` : null,
                            c.departmentName ? `Department: ${c.departmentName}` : null,
                          ]
                            .filter(Boolean)
                            .join('\n'),
                        )
                        if (c.orgUnitId) setOrgUnitId(c.orgUnitId)
                      }
                    }}
                    fullWidth
                    disabled={dpiaRiskCandidates.length === 0}
                  />
                </SettingsCard>
              </Box>
            )}
            <RiskCreateEditForm
              isErm={isErm}
              isPrivacy={isPrivacy}
              editMode={formMode === 'edit'}
              copyFromLabel={copyFromLabel}
              canAdmin={canAdmin}
              cats={cats}
              flatOrgs={flatOrgs}
              users={users}
              assets={assets}
              frameworks={frameworks}
              frameworkDetail={frameworkDetail}
              frameworkId={frameworkId}
              frameworkNodeId={frameworkNodeId}
              controlAssessmentStatus={controlAssessmentStatus}
              previewUaeIa={previewUaeIa}
              scoreLevels={scoreLevels}
              matrixGridSize={matrixGridSize}
              previewInherent={previewInherent}
              previewResidual={previewResidual}
              previewInherentScore={previewInherentScore}
              previewResidualScore={previewResidualScore}
              previewInherentBand={previewInherentBand}
              previewResidualBand={previewResidualBand}
              previewInherentAction={previewInherentAction}
              previewResidualAction={previewResidualAction}
              previewPriority={previewPriority}
              willAutoClose={willAutoClose}
              treatPlanDateError={treatPlanDateError}
              dateMin={dateMin}
              displayIdOverride={displayIdOverride}
              setDisplayIdOverride={setDisplayIdOverride}
              title={title}
              setTitle={setTitle}
              strategicObjective={strategicObjective}
              setStrategicObjective={setStrategicObjective}
              category={category}
              setCategory={(v) => {
                setCategory(v)
                if (v !== 'OTHER') setCategoryOther('')
              }}
              categoryOther={categoryOther}
              setCategoryOther={setCategoryOther}
              orgUnitId={orgUnitId}
              setOrgUnitId={setOrgUnitId}
              riskOwnerUserId={riskOwnerUserId}
              setRiskOwnerUserId={setRiskOwnerUserId}
              financialImpactAed={financialImpactAed}
              setFinancialImpactAed={setFinancialImpactAed}
              assetId={assetId}
              setAssetId={setAssetId}
              setFrameworkId={setFrameworkId}
              setFrameworkNodeId={setFrameworkNodeId}
              description={description}
              setDescription={setDescription}
              observedGap={observedGap}
              setObservedGap={setObservedGap}
              threats={threats}
              setThreats={setThreats}
              probability={probability}
              setProbability={setProbability}
              impact={impact}
              setImpact={setImpact}
              controlEff={controlEff}
              setControlEff={setControlEff}
              residualScoringMode={residualScoringMode}
              setResidualScoringMode={setResidualScoringMode}
              residualProbability={residualProbability}
              setResidualProbability={setResidualProbability}
              residualImpact={residualImpact}
              setResidualImpact={setResidualImpact}
              overrideAutoClose={overrideAutoClose}
              setOverrideAutoClose={setOverrideAutoClose}
              autoCloseJustification={autoCloseJustification}
              setAutoCloseJustification={setAutoCloseJustification}
              existingControls={existingControls}
              setExistingControls={setExistingControls}
              infosecStatusInitial={infosecStatusInitial}
              setInfosecStatusInitial={setInfosecStatusInitial}
              treatmentOption={treatmentOption}
              setTreatmentOption={setTreatmentOption}
              treatmentPlan={treatmentPlan}
              setTreatmentPlan={setTreatmentPlan}
              planActionOwnerId={planActionOwnerId}
              setPlanActionOwnerId={setPlanActionOwnerId}
              planCustodianUserId={planCustodianUserId}
              setPlanCustodianUserId={setPlanCustodianUserId}
              planStartDate={planStartDate}
              setPlanStartDate={setPlanStartDate}
              planTargetDate={planTargetDate}
              setPlanTargetDate={setPlanTargetDate}
              planEvidenceRequired={planEvidenceRequired}
              setPlanEvidenceRequired={setPlanEvidenceRequired}
              adminNotes={adminNotes}
              setAdminNotes={setAdminNotes}
              createEvidenceFiles={createEvidenceFiles}
              setCreateEvidenceFiles={setCreateEvidenceFiles}
              createEvidenceClass={createEvidenceClass}
              setCreateEvidenceClass={setCreateEvidenceClass}
              identificationEvidence={
                formMode === 'edit'
                  ? linkedEvidence.filter((e) => isIdentificationEvidence(e.workflowStage))
                  : [...draftAttachments, ...createQueuedReferences.map(queuedReferenceSummary)]
              }
              evidenceQuotaUsed={
                formMode === 'edit'
                  ? myRiskEvidenceCount
                  : draftAttachments.length + createQueuedReferences.length
              }
              onUploadReference={() => {
                const file = createEvidenceFiles[0]
                if (!file || !createEvidenceClass) return
                if (formMode === 'edit' && detail) {
                  const remaining = MAX_REFERENCE_ATTACHMENTS - myRiskEvidenceCount
                  if (remaining <= 0) {
                    onError('You can attach at most 5 files to this risk')
                    return
                  }
                  setBusy(true)
                  setReferenceBusy(true)
                  void (async () => {
                    try {
                      await api.uploadEvidence({
                        file,
                        classification: createEvidenceClass,
                        title: file.name,
                        moduleCode: 'RISK',
                        recordType: 'RISK_RECORD',
                        recordId: String(detail.id),
                        workflowStage: 'IDENTIFICATION',
                      })
                      setCreateEvidenceFiles([])
                      setCreateEvidenceClass('')
                      setLinkedEvidence(
                        await api.listEvidenceLinked('RISK', 'RISK_RECORD', String(detail.id)),
                      )
                      onOk('Reference attachment uploaded')
                    } catch (e) {
                      onError(String(e))
                    } finally {
                      setReferenceBusy(false)
                      setBusy(false)
                    }
                  })()
                  return
                }
                if (draftAttachments.length + createQueuedReferences.length >= MAX_REFERENCE_ATTACHMENTS) {
                  onError('You can attach at most 5 files to this risk')
                  return
                }
                setCreateQueuedReferences((prev) => [
                  ...prev,
                  { file, classification: createEvidenceClass },
                ])
                setCreateEvidenceFiles([])
                setCreateEvidenceClass('')
              }}
              onRemoveReference={(ev) => {
                if (ev.id < 0) {
                  const idx = -ev.id - 1
                  setCreateQueuedReferences((prev) => prev.filter((_, i) => i !== idx))
                  return
                }
                if (formMode === 'create' && activeDraftId != null) {
                  const draftKey = String(activeDraftId)
                  setBusy(true)
                  setReferenceBusy(true)
                  void api
                    .unlinkEvidenceFromRecord(ev.id, {
                      moduleCode: 'RISK',
                      recordType: 'RISK_DRAFT',
                      recordId: draftKey,
                    })
                    .then(async () => {
                      setDraftAttachments(await api.listEvidenceLinked('RISK', 'RISK_DRAFT', draftKey))
                      onOk('Reference attachment removed from the draft')
                      void refreshDrafts()
                    })
                    .catch((e) => onError(String(e)))
                    .finally(() => {
                      setReferenceBusy(false)
                      setBusy(false)
                    })
                  return
                }
                if (formMode === 'edit' && detail) {
                  setBusy(true)
                  setReferenceBusy(true)
                  void api
                    .unlinkEvidenceFromRecord(ev.id, {
                      moduleCode: 'RISK',
                      recordType: 'RISK_RECORD',
                      recordId: String(detail.id),
                    })
                    .then(async () => {
                      setLinkedEvidence(
                        await api.listEvidenceLinked('RISK', 'RISK_RECORD', String(detail.id)),
                      )
                      onOk('Reference attachment removed')
                    })
                    .catch((e) => onError(String(e)))
                    .finally(() => {
                      setReferenceBusy(false)
                      setBusy(false)
                    })
                }
              }}
              onEvidenceError={onError}
              formBusy={busy}
              referenceBusy={referenceBusy}
              cause={cause}
              setCause={setCause}
              causeOther={causeOther}
              setCauseOther={setCauseOther}
              manualResidualScore={manualResidualScore}
              setManualResidualScore={setManualResidualScore}
              residualScoreError={residualManualError}
              assignUserId={assignUserId}
              setAssignUserId={setAssignUserId}
              eligibleContributors={eligibleContributors}
              dateIdentified={dateIdentified}
              setDateIdentified={setDateIdentified}
              nextReviewDate={nextReviewDate}
              setNextReviewDate={setNextReviewDate}
              thirdPartyName={thirdPartyName}
              setThirdPartyName={setThirdPartyName}
              treatmentJustification={treatmentJustification}
              setTreatmentJustification={setTreatmentJustification}
              targetControlEff={targetControlEff}
              setTargetControlEff={setTargetControlEff}
              compensatingControls={compensatingControls}
              setCompensatingControls={setCompensatingControls}
              previewReductionPct={previewReductionPct}
              previewWithinAppetite={previewWithinAppetite}
              previewEvalDecision={previewEvalDecision}
              previewAcceptanceRequired={previewAcceptanceRequired}
              previewTargetResidualScore={previewTargetResidualScore}
              previewTargetResidualBand={previewTargetResidualBand}
              fieldErrors={fieldErrors}
              clearFieldError={clearFieldError}
            />
          </RiskFormShell>
        ) : !detail && selectedProjectId == null && acceptanceView ? (
          <Card
            sx={{
              flex: 1,
              minHeight: 0,
              overflow: 'hidden',
              display: 'flex',
              flexDirection: 'column',
              boxShadow: brand.shadowCard,
            }}
          >
            <RegisterToolbar
              search={acceptanceProjectQuery}
              searchPlaceholder="Search projects"
              onSearchChange={setAcceptanceProjectQuery}
            />
            <RegisterListFrame
              ready={listReady && risksReady}
              count={acceptanceProjectPaged.count}
              emptyMessage={
                acceptanceProjectQuery.trim()
                  ? 'No projects match this search.'
                  : 'No projects with accepted risks yet.'
              }
              page={acceptanceProjectPaged.page}
              rowsPerPage={acceptanceProjectPaged.rowsPerPage}
              onPageChange={acceptanceProjectPaged.setPage}
              onRowsPerPageChange={acceptanceProjectPaged.setRowsPerPage}
            >
              <TableContainer sx={{ flex: 1 }}>
                <Table size="small" stickyHeader sx={registerTableSx}>
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ ...primaryTableCellSx, pl: 1.5 }}>Project</TableCell>
                      <TableCell sx={compactTableCellSx}>Year</TableCell>
                      <TableCell sx={compactTableCellSx} align="right">
                        Accepted risks
                      </TableCell>
                      <TableCell sx={{ ...compactTableCellSx, ...lastTableCellSx }}>Status</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {acceptanceProjectPaged.paged.map(({ project: p, accepted }) => (
                      <TableRow
                        key={p.id}
                        hover
                        onClick={() => selectProject(p.id, true)}
                        sx={{ cursor: 'pointer' }}
                      >
                        <TableCell sx={{ ...primaryTableCellSx, pl: 1.5 }}>
                          <Typography noWrap sx={primaryRegisterNameSx}>
                            {p.name}
                          </Typography>
                        </TableCell>
                        <TableCell sx={compactTableCellSx}>{p.year}</TableCell>
                        <TableCell sx={compactTableCellSx} align="right">
                          {accepted}
                        </TableCell>
                        <TableCell sx={{ ...compactTableCellSx, ...lastTableCellSx }}>
                          <StatusChip
                            label={p.status.replace(/_/g, ' ')}
                            tone={p.status === 'ACTIVE' ? 'success' : 'neutral'}
                          />
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </RegisterListFrame>
          </Card>
        ) : !detail && selectedProjectId == null && !acceptanceView ? (
          assignedWorkMode ? (
            <Card
              sx={{
                flex: 1,
                minHeight: 0,
                overflow: 'hidden',
                display: 'flex',
                flexDirection: 'column',
                boxShadow: brand.shadowCard,
              }}
            >
              <RegisterListFrame
                ready={listReady}
                count={projectPaged.count}
                emptyMessage="No risks assigned to you in this module yet."
                page={projectPaged.page}
                rowsPerPage={projectPaged.rowsPerPage}
                onPageChange={projectPaged.setPage}
                onRowsPerPageChange={projectPaged.setRowsPerPage}
              >
                <TableContainer sx={{ flex: 1 }}>
                  <Table size="small" stickyHeader sx={registerTableSx}>
                    <TableHead>
                      <TableRow>
                        <TableCell sx={{ ...primaryTableCellSx, pl: 1.5 }}>Project</TableCell>
                        <TableCell sx={compactTableCellSx}>Year</TableCell>
                        <TableCell sx={compactTableCellSx} align="right">
                          Assigned to you
                        </TableCell>
                        <TableCell sx={compactTableCellSx} align="right">
                          Needs action
                        </TableCell>
                        <TableCell sx={{ ...compactTableCellSx, ...lastTableCellSx }}>Status</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {projectPaged.paged.map((p) => {
                        const needsAction = p.needsActionCount ?? 0
                        return (
                          <TableRow
                            key={p.id}
                            hover
                            onClick={() => selectProject(p.id)}
                            sx={{ cursor: 'pointer' }}
                          >
                            <TableCell sx={{ ...primaryTableCellSx, pl: 1.5 }}>
                              <Typography noWrap sx={primaryRegisterNameSx}>
                                {p.name}
                              </Typography>
                            </TableCell>
                            <TableCell sx={compactTableCellSx}>{p.year}</TableCell>
                            <TableCell sx={compactTableCellSx} align="right">
                              {p.riskCount}
                            </TableCell>
                            <TableCell sx={compactTableCellSx} align="right">
                              {needsAction > 0 ? (
                                <StatusChip label={String(needsAction)} tone="warning" />
                              ) : (
                                '0'
                              )}
                            </TableCell>
                            <TableCell sx={{ ...compactTableCellSx, ...lastTableCellSx }}>
                              <StatusChip
                                label={p.status.replace(/_/g, ' ')}
                                tone={p.status === 'ACTIVE' ? 'success' : 'neutral'}
                              />
                            </TableCell>
                          </TableRow>
                        )
                      })}
                    </TableBody>
                  </Table>
                </TableContainer>
              </RegisterListFrame>
            </Card>
          ) : (
          <Box
            sx={{
              flex: 1,
              minHeight: 0,
              overflow: 'auto',
              position: 'relative',
              ...complexFormContainerSx,
            }}
          >
            {projectCreating && (
              <ProjectCreateProgress
                isErm={isErm}
                isPrivacy={isPrivacy}
                projectName={newProjectName}
                frameworkLabel={creatingFrameworkLabel}
                seeded={seedProgress.done}
                total={seedProgress.total}
              />
            )}
            {!listReady ? (
              <PageLoading />
            ) : (
              <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: { xs: '1fr', md: 'minmax(0, 1.2fr) minmax(0, 0.8fr)' },
                  gap: 1.25,
                  alignItems: 'stretch',
                }}
              >
                <SettingsCard
                  heading
                  autoHeight
                  title={isErm ? 'ERM projects' : isPrivacy ? 'Privacy Risk projects' : 'Information Security Risk projects'}
                  subtitle={
                    isErm
                      ? 'Open a project to work the enterprise risk register'
                      : isPrivacy
                        ? 'Open a project to work the Privacy Risk register. Risks mainly arrive here from a DPIA; you can also add a Privacy Risk in the project.'
                        : 'Open a project to work the Information Security Risk register'
                  }
                >
                  {projects.length === 0 ? (
                    <Typography sx={{ fontSize: '0.75rem', color: brand.mutedLight, lineHeight: 1.45 }}>
                      {isErm
                        ? 'Create a project to open the register.'
                        : isPrivacy
                          ? 'Create a project to open the register.'
                          : 'Create a project to open the register. Default risks load when Arbor has a register for that framework.'}
                    </Typography>
                  ) : (
                    <Stack spacing={0.75}>
                      {projects.map((p) => (
                        <Box
                          key={p.id}
                          onClick={() => {
                            if (!projectCreating) selectProject(p.id)
                          }}
                          sx={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            gap: 1,
                            px: 1.25,
                            py: 1,
                            border: `1px solid ${brand.border}`,
                            borderRadius: 1,
                            cursor: projectCreating ? 'wait' : 'pointer',
                            bgcolor: brand.paper,
                            pointerEvents: projectCreating ? 'none' : 'auto',
                            '&:hover': { bgcolor: projectCreating ? brand.paper : brand.hover },
                          }}
                        >
                          <Box sx={{ minWidth: 0 }}>
                            <Typography
                              noWrap
                              sx={{ fontSize: '0.8125rem', fontWeight: 700, color: brand.ink, lineHeight: 1.35 }}
                            >
                              {p.name}
                            </Typography>
                            <Typography sx={{ fontSize: '0.6875rem', color: brand.mutedLight, mt: 0.15 }}>
                              {p.year} · {p.riskCount} risk{p.riskCount === 1 ? '' : 's'}
                            </Typography>
                          </Box>
                          <StatusChip
                            label={p.status.replace(/_/g, ' ')}
                            tone={p.status === 'ACTIVE' ? 'success' : 'neutral'}
                          />
                        </Box>
                      ))}
                    </Stack>
                  )}
                </SettingsCard>
                {canAdmin && (
                  <SettingsCard
                    heading
                    autoHeight
                    title="New Project"
                    subtitle={
                      isErm
                        ? 'Name and year for this ERM cycle'
                        : isPrivacy
                          ? 'Name and year for this Privacy Risk cycle'
                          : 'Name, year, and framework. Default risks load when a register exists for that framework.'
                    }
                  >
                    <Stack spacing={1.25}>
                      <CompactTextField
                        label="Name"
                        required
                        fullWidth
                        value={newProjectName}
                        onChange={(e) => setNewProjectName(e.target.value)}
                        placeholder={isErm ? 'ERM 2026' : isPrivacy ? 'Privacy Risk 2026' : 'ISMS 2026'}
                        disabled={projectCreating}
                        error={projectNameTaken}
                        helperText={
                          projectNameTaken
                            ? `A project named "${newProjectName.trim()}" already exists`
                            : undefined
                        }
                      />
                      <FormSelect
                        label="Year"
                        fullWidth
                        value={newProjectYear}
                        onChange={setNewProjectYear}
                        disabled={projectCreating}
                        options={[
                          ...Array.from(
                            new Set(
                              [-1, 0, 1, 2]
                                .map((d) => String(new Date().getFullYear() + d))
                                .concat(newProjectYear),
                            ),
                          )
                            .sort()
                            .map((y) => ({ value: y, label: y })),
                        ]}
                      />
                      {!isErm && !isPrivacy && (
                        <FormSelect
                          label="Framework"
                          value={newProjectFrameworkId === '' ? '' : String(newProjectFrameworkId)}
                          disabled={projectCreating}
                          options={[
                            { value: '', label: 'Select framework…' },
                            ...frameworks.map((f) => ({
                              value: String(f.id),
                              label: frameworkLabel(f),
                            })),
                          ]}
                          onChange={(v) => setNewProjectFrameworkId(v === '' ? '' : Number(v))}
                          fullWidth
                        />
                      )}
                      <BusyButton
                        variant="contained"
                        size="small"
                        busy={projectCreating}
                        disabled={
                          !newProjectName.trim() ||
                          projectNameTaken ||
                          (!isErm && !isPrivacy && newProjectFrameworkId === '')
                        }
                        onClick={() => void createProject()}
                        sx={{ alignSelf: 'flex-start' }}
                      >
                        {projectCreating
                          ? isErm
                            ? 'Creating…'
                            : (seedProgress.total ?? 0) > 0
                              ? 'Seeding Default Risks…'
                              : 'Creating…'
                          : 'Create Project'}
                      </BusyButton>
                    </Stack>
                  </SettingsCard>
                )}
              </Box>
            )}
          </Box>
          )
        ) : !detail ? (
          <Card
            sx={{
              flex: 1,
              minHeight: 0,
              overflow: 'hidden',
              display: 'flex',
              flexDirection: 'column',
              boxShadow: brand.shadowCard,
            }}
          >
            <RiskRegisterToolbar
              search={search}
              onSearchChange={setSearch}
              statusFilter={statusFilter}
              priorityFilter={priorityFilter}
              onStatusFilterChange={setStatusFilter}
              onPriorityFilterChange={setPriorityFilter}
              gridSize={matrixGridSize}
              onClearFilters={clearRegisterFilters}
              filtersActive={filtersActive}
              waitingMyApproval={waitingApprovalOnly}
              waitingMyApprovalCount={waitingApprovalCount}
              onWaitingMyApprovalChange={acceptanceView ? undefined : setWaitingApprovalOnly}
              hideStatusFilter={acceptanceView}
              canExport={canExport}
              exportBusy={exportBusy}
              onExport={() => void runExport()}
              extraActions={
                canAdmin && selectedProjectId != null && !acceptanceView ? (
                  <>
                    <Button
                      size="small"
                      variant="outlined"
                      disabled={busy}
                      onClick={() => void downloadBulkTemplate()}
                      sx={{
                        borderColor: brand.border,
                        color: brand.inkSoft,
                        textTransform: 'none',
                        fontWeight: 500,
                        fontSize: '0.75rem',
                      }}
                    >
                      Template
                    </Button>
                    <Button
                      size="small"
                      variant="outlined"
                      disabled={busy}
                      onClick={openBulkImport}
                      sx={{
                        borderColor: brand.border,
                        color: brand.inkSoft,
                        textTransform: 'none',
                        fontWeight: 500,
                        fontSize: '0.75rem',
                      }}
                    >
                      Import
                    </Button>
                  </>
                ) : null
              }
              columnPicker={
                <ColumnPicker
                  columns={riskColumnVisibility.columns}
                  visibleIds={riskColumnVisibility.visibleIds}
                  onToggle={riskColumnVisibility.toggle}
                  onReset={riskColumnVisibility.reset}
                  maxVisible={RISK_REGISTER_MAX_COLUMNS}
                />
              }
            />
            <Box
              sx={{
                flex: 1,
                minHeight: 0,
                minWidth: 0,
                overflowX: 'hidden',
                overflowY: 'auto',
                display: 'flex',
                flexDirection: 'column',
              }}
            >
              {!listReady || !risksReady ? (
                <PageLoading />
              ) : (
              <RiskRegisterTable
                rows={riskPaged.paged}
                totalCount={registerSourceRows.length}
                selectedId={null}
                emptyMessage={
                  registerSourceRows.length === 0
                    ? acceptanceView
                      ? 'No accepted risks yet.'
                      : emptyMsg
                    : 'No risks match the current search or filters.'
                }
                onRowClick={requestOpenDetail}
                onEdit={
                  canAdmin
                    ? (id) => void openEditFromRegister(id)
                    : undefined
                }
                onCopy={
                  canAdmin && selectedProjectId != null
                    ? requestCopyFromRegister
                    : undefined
                }
                ownerLabel={(id) => {
                  if (id == null) return '-'
                  const u = users.find((x) => x.id === id)
                  return u ? u.fullName : `User #${id}`
                }}
                sortKey={sortKey}
                sortDir={sortDir}
                onSort={handleSort}
                onViewAll={filtersActive || drill ? clearRegisterFilters : undefined}
                visibleColumns={riskColumnVisibility.columns.filter((c) =>
                  riskColumnVisibility.visibleIds.includes(c.id),
                )}
                gridTemplateColumns={riskColumnVisibility.gridTemplateColumns}
                draftRows={riskPaged.page === 0 ? draftTableRows : []}
                onDraftOpen={canAdmin ? (id) => void openDraft(id) : undefined}
                onDraftDelete={
                  canAdmin
                    ? (id) => setDeleteDraftTarget(riskDrafts.find((d) => d.id === id) ?? null)
                    : undefined
                }
              />
              )}
              <RegisterPagination
                count={riskPaged.count}
                page={riskPaged.page}
                rowsPerPage={riskPaged.rowsPerPage}
                onPageChange={riskPaged.setPage}
                onRowsPerPageChange={riskPaged.setRowsPerPage}
              />
            </Box>
          </Card>
        ) : (
          <RiskFormShell sections={viewFormSections}>
            {detail.status === 'AUTO_CLOSED' && detail.finalRiskStatus !== 'APPROVE' ? (
              <Alert severity="info" sx={{ py: 0, mb: 1.25 }}>
                Auto-closed at creation because residual risk was {fmtLevel(detail.residualRisk)} (Very
                Low or Low).
                {excelClick
                  ? ''
                  : ' Re-open with a mandatory reason to enter the 4T workflow.'}
              </Alert>
            ) : null}
            <Box sx={complexFormContainerSx}>
              <Stack spacing={1.25}>
              {excelClick && (
                <>
                  {shouldShowRejectionBanner(detail.status, detail.rejectionReason) ? (
                    <AttentionBanner
                      tone="warning"
                      emphasis={detail.status === 'CANCELLED' ? 'Cancelled' : 'Returned / rejected'}
                      message={detail.rejectionReason}
                    />
                  ) : null}
                  <ExcelRiskDossier
                    risk={detail}
                    orgLabel={orgLabel}
                    userLabel={userLabel}
                    showContributor={hasContributorWork}
                    showPostSubmit={showExcelPostSubmit}
                    linkedEvidence={linkedEvidence}
                    onEvidenceError={onError}
                    gridSize={matrixGridSize}
                    headerSlot={
                      detail.acceptanceForm ? (
                        <AcceptanceProgressCard
                          risk={detail}
                          form={detail.acceptanceForm}
                          userLabel={userLabel}
                        />
                      ) : null
                    }
                    evidenceSlot={
                      canUploadDetailEvidence || linkedEvidence.length > 0 ? (
                        <Box id="risk-section-evidence" sx={sectionAnchorSx}>
                          <SettingsCard
                            compact
                            autoHeight
                            titleCaps
                            title="Evidence"
                            subtitle="Browse a file, then set classification and stage, then upload"
                          >
                            {detailEvidenceUpload}
                          </SettingsCard>
                        </Box>
                      ) : null
                    }
                  />
                  {detail.acceptanceForm ? (
                    <ExcelAcceptanceDossier
                      risk={detail}
                      form={detail.acceptanceForm}
                      userLabel={userLabel}
                      gridSize={matrixGridSize}
                    />
                  ) : null}
                </>
              )}
              {!excelClick && (
              <>
              <DetailSection
                title="Risk identification"
                subtitle="What the risk is, and whose it is"
                sectionId="risk-section-identification"
              >
                <DetailFieldGrid>
                  <DetailField label={EXCEL_FIELD.dateIdentified} value={detail.dateIdentified} />
                  {isErm && (
                    <DetailField
                      label={EXCEL_FIELD.riskCategory}
                      value={
                        detail.category === 'OTHER' && detail.categoryOther?.trim()
                          ? `Other: ${detail.categoryOther.trim()}`
                          : detail.category.replace(/_/g, ' ')
                      }
                    />
                  )}
                  <DetailField label={EXCEL_FIELD.division} value={orgLabel(detail.orgUnitId)} />
                  {isErm ? (
                    <DetailField label={EXCEL_FIELD.strategicObjective} value={detail.strategicObjective} />
                  ) : !isPrivacy ? (
                    <DetailField label={EXCEL_FIELD.controlRef} value={detail.nodeCode} />
                  ) : null}
                  <DetailField span label={EXCEL_FIELD.riskDescription} value={detail.description} />
                  <DetailField
                    label={isErm ? EXCEL_FIELD.riskCause : EXCEL_FIELD.associatedThreat}
                    value={isErm ? detail.cause : detail.threats}
                  />
                  <DetailField label={EXCEL_FIELD.riskOwner} value={userLabel(detail.riskOwnerUserId)} />
                  <DetailField label={EXCEL_FIELD.riskCustodian} value={userLabel(detail.assignedContributorId)} />
                  <DetailField span label="Custodian comments" value={detail.custodianComments} />
                  {detail.tprmVendorId != null ? (
                    <DetailField label="TPRM vendor" value={`Vendor #${detail.tprmVendorId}`} />
                  ) : null}
                  {detail.sourceGapAssessmentId != null && (
                    <DetailField
                      label="Source Gap"
                      value={`Assessment #${detail.sourceGapAssessmentId} · item #${detail.sourceGapItemId}`}
                    />
                  )}
                  {submittedReview && detail.workTaskId != null && (
                    <DetailField label="Work task" value={`#${detail.workTaskId}`} />
                  )}
                </DetailFieldGrid>
              </DetailSection>

              {(linkedEvidence.length > 0 || canUploadDetailEvidence) && (
              <DetailSection
                title="Evidence"
                subtitle="Browse a file, then set classification and stage, then upload"
                sectionId="risk-section-evidence"
              >
                {detailEvidenceUpload}
              </DetailSection>
              )}

              <DetailSection
                title="Risk analysis"
                subtitle="Likelihood × Impact, then reduced by the controls in place"
                sectionId="risk-section-scoring"
              >
                <Box
                  sx={{
                    display: 'grid',
                    gridTemplateColumns: {
                      xs: '1fr',
                      md: 'minmax(0, 0.88fr) minmax(0, 0.88fr) minmax(0, 1.24fr)',
                    },
                    gap: 1.25,
                    alignItems: 'start',
                  }}
                >
                  <SettingsCard
                    compact
                    autoHeight
                    title="Inherent risk"
                    action={
                      <RiskLevelBadge
                        level={apiRiskLevelToBadge(
                          excelRatingToLevel(detail.inherentRating ?? detail.inherentRisk),
                        )}
                        label={fmtLevel(detail.inherentRating ?? detail.inherentRisk)}
                      />
                    }
                  >
                    <Stack spacing={1}>
                      <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 1 }}>
                        <DetailField
                          label={scaledExcelField('likelihood', matrixGridSize)}
                          value={fmtLevel(detail.probability)}
                        />
                        <DetailField
                          label={scaledExcelField('impact', matrixGridSize)}
                          value={fmtLevel(detail.impact)}
                        />
                      </Box>
                      <DetailField
                        label={EXCEL_FIELD.inherentScore}
                        value={
                          detail.inherentScore != null
                            ? `${detail.inherentScore} · ${fmtLevel(detail.inherentRating ?? detail.inherentRisk)}`
                            : null
                        }
                      />
                    </Stack>
                  </SettingsCard>
                  <SettingsCard compact autoHeight title="Effective controls">
                    <Stack spacing={1}>
                      <DetailField label={EXCEL_FIELD.existingControls} value={detail.existingControls} />
                      <DetailField
                        label={scaledExcelField('controlEffectiveness', matrixGridSize)}
                        value={
                          detail.controlEffectiveness
                            ? (CE_EXCEL_LABEL[detail.controlEffectiveness] ?? fmtLevel(detail.controlEffectiveness))
                            : null
                        }
                      />
                      <DetailField
                        label={EXCEL_FIELD.riskReduction}
                        value={
                          detail.riskReductionPct != null
                            ? `${Math.round(Number(detail.riskReductionPct) * (Number(detail.riskReductionPct) <= 1 ? 100 : 1))}%`
                            : null
                        }
                      />
                    </Stack>
                  </SettingsCard>
                  <SettingsCard
                    compact
                    autoHeight
                    title="Residual risk"
                    action={
                      <RiskLevelBadge
                        level={apiRiskLevelToBadge(
                          excelRatingToLevel(detail.residualRating ?? detail.residualRisk),
                        )}
                        label={fmtLevel(detail.residualRating ?? detail.residualRisk)}
                      />
                    }
                  >
                    <Stack spacing={1}>
                      {detail.residualScoringMode === 'MANUAL_PROBABILITY_IMPACT' && (
                        <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 1 }}>
                          <DetailLevelField label="Residual probability" level={detail.residualProbability} />
                          <DetailLevelField label="Residual impact" level={detail.residualImpact} />
                        </Box>
                      )}
                      <DetailField
                        label={EXCEL_FIELD.residualScore}
                        value={
                          detail.residualScore != null
                            ? `${detail.residualScore} · ${fmtLevel(detail.residualRating ?? detail.residualRisk)}`
                            : null
                        }
                      />
                      <DetailField
                        label={EXCEL_FIELD.withinAppetite}
                        value={
                          detail.withinAppetite === false
                            ? 'Exceeds'
                            : detail.withinAppetite
                              ? 'Within appetite'
                              : null
                        }
                        tone={detail.withinAppetite === false ? 'critical' : detail.withinAppetite ? 'low' : undefined}
                      />
                      <DetailField
                        label={EXCEL_FIELD.evaluationDecision}
                        value={
                          detail.evaluationDecision === 'TREATMENT_REQUIRED'
                            ? 'Treatment required'
                            : detail.evaluationDecision === 'AUTO_CLOSE' ||
                                detail.evaluationDecision === 'AUTO_CLOSED'
                              ? 'Auto-closed'
                              : detail.evaluationDecision === 'MONITOR'
                                ? 'Within appetite - monitor only'
                                : evaluationDecisionLabel(detail.evaluationDecision)
                        }
                        tone={
                          detail.evaluationDecision === 'TREATMENT_REQUIRED'
                            ? 'critical'
                            : 'low'
                        }
                      />
                    </Stack>
                  </SettingsCard>
                </Box>
                {detail.autoCloseOverride && (
                  <Box sx={{ mt: 1 }}>
                    <DetailField label="Auto-close override" value={detail.autoCloseOverrideJustification} />
                  </Box>
                )}
                {scoreHistory.length > 1 && (
                  <Box sx={{ mt: 1.5 }}>
                    <Typography variant="caption" fontWeight={700} display="block" sx={{ mb: 0.5 }}>
                      Residual trend
                    </Typography>
                    <ResidualTrend history={scoreHistory} />
                    <TableContainer sx={{ mt: 1, maxHeight: 180 }}>
                      <Table size="small" stickyHeader>
                        <TableHead>
                          <TableRow>
                            <TableCell>When</TableCell>
                            <TableCell>P</TableCell>
                            <TableCell>I</TableCell>
                            <TableCell>CE</TableCell>
                            <TableCell>Inherent</TableCell>
                            <TableCell>Residual</TableCell>
                            <TableCell>Reason</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {[...scoreHistory].reverse().map((h) => (
                            <TableRow key={h.id}>
                              <TableCell>{fmtWhen(h.recordedAt)}</TableCell>
                              <TableCell>{fmtLevel(h.probability)}</TableCell>
                              <TableCell>{fmtLevel(h.impact)}</TableCell>
                              <TableCell>{fmtLevel(h.controlEffectiveness)}</TableCell>
                              <TableCell>
                                <RiskLevelBadge
                                  level={apiRiskLevelToBadge(h.inherentRisk)}
                                  label={fmtLevel(h.inherentRisk)}
                                />
                              </TableCell>
                              <TableCell>
                                <RiskLevelBadge
                                  level={apiRiskLevelToBadge(h.residualRisk)}
                                  label={fmtLevel(h.residualRisk)}
                                />
                              </TableCell>
                              <TableCell>{h.reason ?? '-'}</TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  </Box>
                )}
              </DetailSection>

              {stageNotes.length > 0 && (
              <DetailSection
                title="Stage notes"
                subtitle="Workflow commentary: visible per internal-only flag"
                sectionId="risk-section-notes"
              >
                <Stack spacing={1}>
                  {stageNotes.map((n) => (
                    <Box
                      key={n.id}
                      sx={{
                        p: 1.25,
                        border: `1px solid ${brand.hairline}`,
                        borderRadius: 1,
                        bgcolor: n.internalOnly ? brand.accentMuted : 'transparent',
                      }}
                    >
                      <Stack direction="row" justifyContent="space-between" alignItems="flex-start" gap={1}>
                        <Box>
                          <Typography variant="caption" color="text.secondary">
                            {n.workflowStage} · {userLabel(n.createdByUserId)} · {fmtWhen(n.createdAt)}
                            {n.internalOnly ? ' · Internal only' : ''}
                          </Typography>
                          <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                            {n.body}
                          </Typography>
                        </Box>
                        {(canAdmin || meId === n.createdByUserId) && (
                          <Button
                            size="small"
                            color="error"
                            disabled={busy}
                            onClick={() => setConfirmNoteId(n.id)}
                          >
                            Delete
                          </Button>
                        )}
                      </Stack>
                    </Box>
                  ))}
                  {!submittedReview && (canAdmin || (meId != null && detail.assignedContributorId === meId)) && (
                    <>
                  <CompactTextField
                    label="Add note"
                    multiline
                    minRows={2}
                    fieldIdPrefix="risk-note"
                    value={noteBody}
                    onChange={(e) => setNoteBody(e.target.value)}
                  />
                  {canAdmin && (
                    <FormControlLabel
                      control={
                        <Checkbox
                          checked={noteInternalOnly}
                          onChange={(e) => setNoteInternalOnly(e.target.checked)}
                        />
                      }
                      label="Internal only (Admin + Owner)"
                    />
                  )}
                  <Button
                    variant="outlined"
                    disabled={busy || !noteBody.trim()}
                    onClick={() =>
                      void run(async () => {
                        await api.addRiskNote(detail.id, {
                          body: noteBody.trim(),
                          internalOnly: canAdmin ? noteInternalOnly : false,
                        })
                        setNoteBody('')
                        setNoteInternalOnly(false)
                        await refreshNotesAndHistory(detail.id)
                        return api.getRisk(detail.id)
                      }, 'Note added')
                    }
                  >
                    Add Note
                  </Button>
                    </>
                  )}
                </Stack>
              </DetailSection>
              )}

              {!submittedReview && (
              <DetailSection
                title="Ownership & assignment"
                subtitle="Risk owner, contributor, and lifecycle timestamps"
                sectionId="risk-section-ownership"
              >
                <DetailTwoCol>
                  <Stack spacing={0}>
                    <DetailField label={EXCEL_FIELD.riskOwner} value={userLabel(detail.riskOwnerUserId)} />
                    <DetailField label={EXCEL_FIELD.riskCustodian} value={userLabel(detail.assignedContributorId)} />
                    <DetailField label="Created by" value={userLabel(detail.createdByUserId)} />
                  </Stack>
                  <Stack spacing={0}>
                    <DetailField label="Assigned at" value={fmtWhen(detail.assignedAt)} />
                    <DetailField label="Submitted at" value={fmtWhen(detail.submittedAt)} />
                    <DetailField label="Closed at" value={fmtWhen(detail.closedAt)} />
                    <DetailField
                      label={EXCEL_FIELD.treatmentProgress}
                      value={treatmentProgressLabel({
                        startDate: detail.activeTreatmentPlan?.startDate,
                        targetDate: detail.activeTreatmentPlan?.targetDate,
                        status: detail.activeTreatmentPlan?.status,
                      })}
                    />
                    <DetailField
                      label={EXCEL_FIELD.reviewAlert}
                      value={reviewAlertLabel(detail.nextReviewDate)}
                    />
                    {(canAdmin || isRiskOwner) && detail.adminNotes ? (
                      <DetailField label="Internal notes" value={detail.adminNotes} />
                    ) : null}
                  </Stack>
                </DetailTwoCol>
              </DetailSection>
              )}

              <DetailSection
                title="Treatment"
                subtitle="4T option, evidence required, and target date"
                sectionId="risk-section-treatment"
              >
                <DetailFieldGrid>
                <DetailField
                  label={EXCEL_FIELD.treatmentOption}
                  value={detail.treatmentOption ? TREAT_LABEL[detail.treatmentOption] : null}
                />
                <DetailField
                  span
                  label={EXCEL_FIELD.treatmentJustification}
                  value={detail.treatmentChangeJustification}
                />
                <DetailField label="Validation notes" value={detail.validationNotes} />
                {!isErm && (
                  <>
                    <DetailField label={isPrivacy ? 'Privacy status (initial)' : 'InfoSec status (initial)'} value={detail.infosecStatusInitial} />
                    <DetailField label={isPrivacy ? 'Privacy status (contributor)' : 'InfoSec status (contributor)'} value={detail.infosecStatusContributor} />
                    <DetailField label={isPrivacy ? 'Privacy status (final)' : 'InfoSec status (final)'} value={detail.infosecStatusFinal} />
                  </>
                )}
                <DetailField
                  label={EXCEL_FIELD.evidenceRequired}
                  value={detail.activeTreatmentPlan?.evidenceRequired}
                />
                <DetailField
                  label={EXCEL_FIELD.targetDate}
                  value={detail.activeTreatmentPlan?.targetDate}
                />
                </DetailFieldGrid>
                {shouldShowRejectionBanner(detail.status, detail.rejectionReason) && (
                  <Alert severity="warning" sx={{ py: 0.5, mt: 1 }}>
                    {detail.status === 'CANCELLED'
                      ? 'Cancelled: '
                      : 'Returned / rejected: previous submission reason highlighted: '}
                    <strong>{detail.rejectionReason}</strong>
                  </Alert>
                )}

                {canAdmin &&
                  detail.status === 'UNDER_VALIDATION' && (
                  <Stack spacing={1} sx={{ mt: 1, p: 1.25, border: `1px solid ${brand.hairline}`, borderRadius: 1 }}>
                    <Typography variant="caption" fontWeight={700}>
                      Change treatment (post-submission)
                    </Typography>
                    <FormSelect
                      label="New 4T option"
                      value={changeTreatmentOption}
                      options={TREAT.map((t) => ({ value: t, label: TREAT_LABEL[t] }))}
                      onChange={(v) => setChangeTreatmentOption(v as TreatmentOption)}
                      fullWidth
                    />
                    <CompactTextField
                      label="Justification (required)"
                      multiline
                      minRows={2}
                      fieldIdPrefix="risk-treat"
                      value={changeTreatmentJustification}
                      onChange={(e) => setChangeTreatmentJustification(e.target.value)}
                    />
                    <Button
                      variant="outlined"
                      disabled={busy || !changeTreatmentJustification.trim()}
                      onClick={() =>
                        void run(
                          () =>
                            api.changeRiskTreatmentOption(detail.id, {
                              treatmentOption: changeTreatmentOption,
                              justification: changeTreatmentJustification.trim(),
                            }),
                          'Treatment option updated',
                        )
                      }
                    >
                      Apply Treatment Change
                    </Button>
                  </Stack>
                )}

                {(detail.eliminationJustification || detail.eliminationHow) && (
                  <Box sx={{ mt: 0.5 }}>
                    <Typography variant="caption" fontWeight={700} display="block">
                      Terminate (Avoid)
                    </Typography>
                    <DetailField label="Justification" value={detail.eliminationJustification} />
                    <DetailField label="How eliminated" value={detail.eliminationHow} />
                    <DetailField
                      label="Activity stopped"
                      value={
                        detail.activityStopped == null
                          ? null
                          : detail.activityStopped
                            ? 'Yes'
                            : 'No'
                      }
                    />
                  </Box>
                )}

                {detail.transferMethod && (
                  <Box sx={{ mt: 0.5 }}>
                    <Typography variant="caption" fontWeight={700} display="block">
                      Transfer
                    </Typography>
                    <DetailField label="Method" value={detail.transferMethod} />
                    <DetailField label="Third party" value={detail.thirdPartyName} />
                    <DetailField label="Description" value={detail.transferDescription} />
                    <DetailField label="Residual after transfer" value={detail.residualAfterTransfer} />
                    <DetailField
                      label="Compensating controls"
                      value={detail.transferCompensatingControls}
                    />
                  </Box>
                )}

                {detail.acceptanceForm && (
                  <Box
                    sx={{
                      mt: 0.5,
                      p: 1.25,
                      border: `1px solid ${brand.hairline}`,
                      borderRadius: 1,
                    }}
                  >
                    <Typography variant="caption" fontWeight={700} display="block" sx={{ mb: 1 }}>
                      Risk acceptance form ({detail.acceptanceForm.status})
                    </Typography>
                    <Typography variant="caption" fontWeight={700} color="text.secondary" display="block" sx={{ mb: 0.5 }}>
                      Section 1: Risk identification (read-only)
                    </Typography>
                    <DetailField label="Risk title" value={detail.title} />
                    <DetailField label="Description" value={detail.description} />
                    <DetailField label="Inherent / residual" value={`${detail.inherentRisk} / ${detail.residualRisk}`} />
                    <DetailField label="Threats" value={detail.threats} />
                    <DetailField label="Existing controls" value={detail.existingControls} />
                    {!isErm && (
                      <DetailField label={isPrivacy ? 'Privacy (initial)' : 'InfoSec (initial)'} value={detail.infosecStatusInitial} />
                    )}
                    <Typography variant="caption" fontWeight={700} color="text.secondary" display="block" sx={{ mt: 1, mb: 0.5 }}>
                      Section 2: Contributor acceptance
                    </Typography>
                    <DetailField label="Justification" value={detail.acceptanceForm.justification} />
                    <DetailField label={EXCEL_FIELD.businessRationale} value={detail.acceptanceForm.businessRationale} />
                    <DetailField
                      label="Conditions"
                      value={detail.acceptanceForm.conditionsOfAcceptance}
                    />
                    <DetailField
                      label="Compensating controls"
                      value={detail.acceptanceForm.compensatingControls}
                    />
                    <DetailField
                      label="Comp. control owner"
                      value={userLabel(detail.acceptanceForm.compControlOwnerId)}
                    />
                    <DetailField
                      label="Comp. effectiveness"
                      value={fmtLevel(detail.acceptanceForm.compControlEffectiveness)}
                    />
                    <DetailField label="Review date" value={detail.acceptanceForm.reviewDate} />
                    <Typography variant="caption" fontWeight={700} color="text.secondary" display="block" sx={{ mt: 1, mb: 0.5 }}>
                      Section 3: Risk Acceptance
                    </Typography>
                    <DetailField
                      label={EXCEL_FIELD.riskOwner}
                      value={acceptanceApproverDisplay(
                        detail.acceptanceForm.approver1Name?.trim() ||
                          (detail.acceptanceForm.approver1UserId != null
                            ? userLabel(detail.acceptanceForm.approver1UserId)
                            : userLabel(detail.riskOwnerUserId)),
                        detail.acceptanceForm.riskOwnerDecision,
                        detail.acceptanceForm.status === 'PENDING_RISK_OWNER',
                        detail.acceptanceForm.riskOwnerDecision === 'APPROVED' ||
                          detail.acceptanceForm.status === 'PENDING_APPROVER_2' ||
                          detail.acceptanceForm.status === 'PENDING_APPROVER_3' ||
                          detail.acceptanceForm.status === 'PENDING_ADMIN',
                        detail.acceptanceForm.riskOwnerComments,
                      )}
                    />
                    <DetailField
                      label={acceptanceLevel2Label(detail.riskType)}
                      value={acceptanceApproverDisplay(
                        detail.acceptanceForm.approver2Name?.trim() ||
                          (detail.acceptanceForm.approver2UserId != null
                            ? userLabel(detail.acceptanceForm.approver2UserId)
                            : null),
                        detail.acceptanceForm.approver2Decision,
                        detail.acceptanceForm.status === 'PENDING_APPROVER_2',
                        detail.acceptanceForm.status === 'PENDING_APPROVER_3' ||
                          detail.acceptanceForm.status === 'PENDING_ADMIN',
                        detail.acceptanceForm.approver2Comments,
                      )}
                    />
                    {detail.acceptanceForm.approver3UserId != null ? (
                    <DetailField
                      label={acceptanceLevel3Label()}
                      value={acceptanceApproverDisplay(
                        detail.acceptanceForm.approver3Name?.trim() ||
                          userLabel(detail.acceptanceForm.approver3UserId),
                        detail.acceptanceForm.approver3Decision,
                        detail.acceptanceForm.status === 'PENDING_APPROVER_3',
                        detail.acceptanceForm.status === 'PENDING_ADMIN',
                        detail.acceptanceForm.approver3Comments,
                      )}
                    />
                    ) : (
                    <DetailField label={acceptanceLevel3Label()} value="Not used" />
                    )}
                    <Typography variant="caption" fontWeight={700} color="text.secondary" display="block" sx={{ mt: 1, mb: 0.5 }}>
                      Section 4: Admin final decision
                    </Typography>
                    <DetailField
                      label="Admin decision"
                      value={
                        detail.acceptanceForm.adminDecision
                          ? `${detail.acceptanceForm.adminDecision}${
                              detail.acceptanceForm.adminComments
                                ? `: ${detail.acceptanceForm.adminComments}`
                                : ''
                            }`
                          : null
                      }
                    />
                  </Box>
                )}

                {canAdmin && planEditable && detail.treatmentOption === 'TREAT' && (
                  <Box
                    sx={{
                      mt: 0.5,
                      p: 1.25,
                      border: `1px solid ${brand.border}`,
                      borderRadius: 1,
                      bgcolor: brand.hover,
                    }}
                  >
                    <Typography variant="caption" fontWeight={700} display="block" sx={{ mb: 1 }}>
                      Treatment plan {detail.activeTreatmentPlan ? `(${detail.activeTreatmentPlan.status})` : ''}
                    </Typography>
                    <Stack spacing={1}>
                      <CompactTextField
                        label="Action description"
                        fieldIdPrefix="plan-edit"
                        multiline
                        minRows={2}
                        fullWidth
                        value={treatmentPlan}
                        onChange={(e) => setTreatmentPlan(e.target.value)}
                      />
                      <FormSelect
                        label="Action owner"
                        value={planActionOwnerId === '' ? '' : String(planActionOwnerId)}
                        options={[
                          { value: '', label: '- Select -' },
                          ...users
                            .filter((u) => u.roles.some((r) => ['CONTRIBUTOR', 'ADMIN', 'OWNER'].includes(r)))
                            .map((u) => ({ value: String(u.id), label: u.fullName })),
                        ]}
                        onChange={(v) => setPlanActionOwnerId(v === '' ? '' : Number(v))}
                        maxWidth={240}
                      />
                      <FormSelect
                        label="Custodian"
                        value={planCustodianUserId === '' ? '' : String(planCustodianUserId)}
                        options={[
                          { value: '', label: '- Select -' },
                          ...users
                            .filter((u) => u.roles.some((r) => ['CONTRIBUTOR', 'ADMIN', 'OWNER'].includes(r)))
                            .map((u) => ({ value: String(u.id), label: u.fullName })),
                        ]}
                        onChange={(v) => setPlanCustodianUserId(v === '' ? '' : Number(v))}
                        maxWidth={240}
                      />
                      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                        <CompactTextField
                          label="Start date"
                          type="date"
                          fieldIdPrefix="plan-edit"
                          fullWidth
                          InputLabelProps={{ shrink: true }}
                          value={planStartDate}
                          onChange={(e) => setPlanStartDate(e.target.value)}
                        />
                        <CompactTextField
                          label="Target date"
                          type="date"
                          fieldIdPrefix="plan-edit"
                          fullWidth
                          InputLabelProps={{ shrink: true }}
                          value={planTargetDate}
                          onChange={(e) => setPlanTargetDate(e.target.value)}
                        />
                      </Stack>
                      <CompactTextField
                        label="Evidence required"
                        fieldIdPrefix="plan-edit"
                        multiline
                        minRows={2}
                        fullWidth
                        value={planEvidenceRequired}
                        onChange={(e) => setPlanEvidenceRequired(e.target.value)}
                      />
                      <Button size="small" variant="contained" disabled={busy} onClick={() => void saveTreatmentPlan()}>
                        Save Treatment Plan
                      </Button>
                    </Stack>
                  </Box>
                )}

                {detail.activeTreatmentPlan &&
                  (planEditable ||
                    Boolean(
                      detail.activeTreatmentPlan.actionDescription ||
                        detail.activeTreatmentPlan.actionOwnerId ||
                        detail.activeTreatmentPlan.startDate ||
                        detail.activeTreatmentPlan.resourcesRequired ||
                        detail.activeTreatmentPlan.costEstimateAed != null ||
                        (detail.activeTreatmentPlan.milestones ?? []).length > 0,
                    )) && (
                  <Box sx={{ mt: 0.5 }}>
                    {(!canAdmin || !planEditable) && (
                      <>
                        {(detail.activeTreatmentPlan.actionDescription ||
                          detail.activeTreatmentPlan.actionOwnerId ||
                          detail.activeTreatmentPlan.startDate ||
                          detail.activeTreatmentPlan.resourcesRequired ||
                          detail.activeTreatmentPlan.costEstimateAed != null) && (
                        <Typography variant="caption" fontWeight={700} display="block" sx={{ mb: 0.5 }}>
                          Action plan ({detail.activeTreatmentPlan.status})
                        </Typography>
                        )}
                        <DetailFieldGrid>
                        <DetailField
                          span
                          label={EXCEL_FIELD.treatmentActionPlan}
                          value={detail.activeTreatmentPlan.actionDescription}
                        />
                        <DetailField
                          label={EXCEL_FIELD.actionOwner}
                          value={userLabel(detail.activeTreatmentPlan.actionOwnerId)}
                        />
                        <DetailField label={EXCEL_FIELD.startDate} value={detail.activeTreatmentPlan.startDate} />
                        <DetailField
                          label="Resources"
                          value={detail.activeTreatmentPlan.resourcesRequired}
                        />
                        <DetailField
                          label="Cost (AED)"
                          value={
                            detail.activeTreatmentPlan.costEstimateAed != null
                              ? formatAed(detail.activeTreatmentPlan.costEstimateAed)
                              : null
                          }
                        />
                        </DetailFieldGrid>
                      </>
                    )}
                    {canAdmin && planEditable && (detail.activeTreatmentPlan.milestones ?? []).length > 0 && (
                      <Typography variant="caption" fontWeight={700} display="block" sx={{ mb: 0.5, mt: 1 }}>
                        Milestones
                      </Typography>
                    )}
                    {(detail.activeTreatmentPlan.milestones ?? []).map((m) => (
                      <Stack key={m.id} spacing={0.75} sx={{ mt: 0.5 }}>
                        <Stack direction="row" spacing={1} alignItems="center">
                          <Typography variant="body2" sx={{ flex: 1 }}>
                            {m.completed ? '✓' : '○'} {m.milestoneTitle}
                            {m.dueDate ? ` · due ${m.dueDate}` : ''}
                          </Typography>
                          {canAdmin && detail.status === 'MITIGATING' && !m.completed && (
                            <Button
                              size="small"
                              disabled={busy}
                              onClick={() =>
                                void run(
                                  () =>
                                    api.completeRiskMilestone(detail.id, m.id, {
                                      progressNotes: milestoneNotes[m.id]?.trim() || null,
                                    }),
                                  'Milestone completed',
                                )
                              }
                            >
                              Complete
                            </Button>
                          )}
                        </Stack>
                        {m.progressNotes && (
                          <Typography variant="caption" color="text.secondary" sx={{ pl: 2 }}>
                            Notes: {m.progressNotes}
                          </Typography>
                        )}
                        {canAdmin && detail.status === 'MITIGATING' && !m.completed && (
                          <CompactTextField
                            label="Progress notes"
                            fieldIdPrefix={`ms-${m.id}`}
                            size="small"
                            fullWidth
                            multiline
                            minRows={2}
                            value={milestoneNotes[m.id] ?? ''}
                            onChange={(e) =>
                              setMilestoneNotes((prev) => ({ ...prev, [m.id]: e.target.value }))
                            }
                          />
                        )}
                      </Stack>
                    ))}
                  </Box>
                )}
              </DetailSection>

              {!submittedReview && (
              <DetailSection title="Record meta" subtitle="System timestamps and cross-links" sectionId="risk-section-meta">
                <DetailTwoCol>
                  <Stack spacing={0}>
                    <DetailField label="Created" value={fmtWhen(detail.createdAt)} />
                    <DetailField label="Updated" value={fmtWhen(detail.updatedAt)} />
                  </Stack>
                  <Stack spacing={0}>
                    {detail.workTaskId != null && (
                      <DetailField label="Work task" value={`#${detail.workTaskId}`} />
                    )}
                  </Stack>
                </DetailTwoCol>
              </DetailSection>
              )}
              </>
              )}

              {showDetailWorkflowActions && (
              <DetailSection
                title="Actions"
                subtitle={
                  submittedReview
                    ? undefined
                    : excelClick && detail.status === 'PENDING_ACCEPTANCE'
                        ? detail.acceptanceForm
                          ? `Waiting on Risk Owner, ${acceptanceLevel2Label(detail.riskType)}, or Level 3`
                          : 'The acceptance form is with the custodian. Approval opens after they submit.'
                        : excelClick && detail.status === 'PENDING_ADMIN_REVIEW'
                          ? 'Admin Accept or Send Back'
                      : 'Workflow steps available for your role on this status'
                }
                sectionId="risk-section-actions"
              >
                {canAdmin &&
                  detail.riskProjectId == null &&
                  !['DRAFT', 'ASSIGNED', 'RE_OPENED', 'SUBMITTED', 'UNDER_VALIDATION', 'PENDING_ACCEPTANCE', 'PENDING_ADMIN_REVIEW'].includes(
                    detail.status,
                  ) && (
                  <Button
                    variant="outlined"
                    disabled={busy}
                    onClick={() => {
                      setBusy(true)
                      void api
                        .raiseRiskCapa(detail.id)
                        .then((capa) => onOk(`CAPA #${capa.id} opened in Register 1 (${capa.sourceType})`))
                        .catch((e) => onError(String(e)))
                        .finally(() => setBusy(false))
                    }}
                  >
                    Raise CAPA
                  </Button>
                )}
                {canAdmin && (detail.status === 'SUBMITTED' || detail.status === 'UNDER_VALIDATION') && (
                  <Stack spacing={1}>
                    {detail.riskProjectId != null && (() => {
                      const liveTarget =
                        postTargetCe === ''
                          ? null
                          : autoResidualScore(
                              detail.inherentScore ?? 0,
                              ceReductionPercent(matrixGridSize, colRank(matrixAxes, postTargetCe)),
                            )
                      const liveAcceptance = acceptanceRequiredLabel(
                        detail.treatmentOption,
                        Number.isFinite(liveTarget) ? liveTarget : null,
                        appetiteMax,
                      )
                      const acceptanceSplit = liveAcceptance.indexOf(' - ')
                      const acceptanceStatus = acceptanceSplit === -1 ? liveAcceptance : liveAcceptance.slice(0, acceptanceSplit)
                      const acceptanceReason = acceptanceSplit === -1 ? '' : liveAcceptance.slice(acceptanceSplit + 3)
                      const acceptanceScore = Number.isFinite(liveTarget) ? liveTarget : null
                      const acceptanceDetail =
                        acceptanceReason && acceptanceScore != null
                          ? `${acceptanceReason} · treated residual risk score - ${acceptanceScore}`
                          : acceptanceReason
                      const acceptanceYes = liveAcceptance.startsWith('YES')
                      const actionChoices: { value: string; label: string }[] = acceptanceYes
                        ? [
                            { value: 'SEND_ACCEPTANCE', label: 'Risk Acceptance' },
                            { value: 'RE_OPENED', label: 'Send Back' },
                          ]
                        : ADMIN_FINISH_ACTIONS.map((a) => ({ value: a.value, label: a.label }))
                      const actionValue = actionChoices.some((o) => o.value === adminAction) ? adminAction : ''
                      const sendForAcceptance = actionValue === 'SEND_ACCEPTANCE'
                      const sendBack = actionValue === 'RE_OPENED'
                      const actionButtonLabel = sendForAcceptance
                        ? 'Risk Acceptance'
                        : sendBack
                          ? 'Send Back'
                          : 'Approve'
                      const scoringSaved =
                        detail.targetControlEffectiveness != null &&
                        detail.targetControlEffectiveness === postTargetCe
                      return (
                      <Stack spacing={1.25}>
                        <Box
                          sx={{
                            display: 'flex',
                            alignItems: 'flex-start',
                            gap: 2,
                            flexWrap: 'nowrap',
                          }}
                        >
                          <FormSelect<RiskLevel | ''>
                            label={scaledExcelField(isErm ? 'targetCe' : 'treatmentCe', matrixGridSize)}
                            minWidth={270}
                            maxWidth={300}
                            placeholder="Select"
                            value={postTargetCe}
                            options={scoreLevels.map((l) => ({
                              value: l,
                              label: CE_EXCEL_LABEL[l] ?? fmtLevel(l),
                              tone: controlEffectivenessTone(l),
                            }))}
                            onChange={(v) => {
                              const next = v as RiskLevel
                              setPostTargetCe(next)
                              setAdminAction('')
                              void saveTargetScoring(next)
                            }}
                          />
                          <Box sx={{ flex: '1 1 220px', minWidth: 180 }}>
                            <FormFieldLabel>{EXCEL_FIELD.acceptanceRequired}</FormFieldLabel>
                            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, minHeight: 32 }}>
                              <Typography sx={{ fontSize: '0.75rem', fontWeight: 700, lineHeight: 1.35 }}>
                                {acceptanceStatus}
                              </Typography>
                              {acceptanceDetail ? (
                                <Typography sx={{ fontSize: '0.75rem', color: 'text.secondary', lineHeight: 1.35 }}>
                                  {acceptanceDetail}
                                </Typography>
                              ) : null}
                            </Box>
                          </Box>
                        </Box>
                        {scoringSaved && (
                          <Box
                            sx={{
                              border: `1px solid ${brand.border}`,
                              borderRadius: 1,
                              p: 1.25,
                              bgcolor: brand.tableHeader,
                            }}
                          >
                            <Typography
                              sx={{
                                fontSize: '0.6875rem',
                                color: brand.muted,
                                lineHeight: 1.4,
                                mb: 1,
                              }}
                            >
                              {acceptanceYes
                                ? 'Risk Acceptance opens the acceptance sheet. Send Back returns the risk so the custodian can add comments or evidence.'
                                : `Writes ${EXCEL_FIELD.action} and ${EXCEL_FIELD.nextReviewDate} to the register. Both required.`}
                            </Typography>
                            <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 2, flexWrap: 'wrap' }}>
                              <FormSelect
                                label={EXCEL_FIELD.action}
                                wrapLabel
                                required
                                minWidth={220}
                                maxWidth={280}
                                placeholder="Select"
                                value={actionValue}
                                options={actionChoices}
                                onChange={(v) => {
                                  setAdminAction(v)
                                  requestAnimationFrame(() => {
                                    requestAnimationFrame(() => revealActionFields())
                                  })
                                }}
                              />
                              {!!actionValue && !sendBack && !sendForAcceptance && (
                                <CompactTextField
                                  label={EXCEL_FIELD.nextReviewDate}
                                  wrapLabel
                                  required
                                  type="date"
                                  value={approveNextReviewDate}
                                  onChange={(e) => setApproveNextReviewDate(e.target.value)}
                                  error={!!actionDueDateError(approveNextReviewDate, true)}
                                  helperText={
                                    actionDueDateError(approveNextReviewDate, true) ||
                                    'Today or later.'
                                  }
                                  inputProps={{ min: dateMin }}
                                  maxWidth={EXCEL_DATE_FIELD_WIDTH}
                                />
                              )}
                            </Box>
                            {sendBack ? (
                              <CompactTextField
                                label="Send-back reason"
                                wrapLabel
                                required
                                value={rejectReason}
                                onChange={(e) => setRejectReason(e.target.value)}
                                helperText="Required to send this risk back to the custodian."
                                sx={{ mt: 1 }}
                              />
                            ) : actionValue ? (
                              <CompactTextField
                                label="Remarks"
                                wrapLabel
                                required
                                value={actionRemarks}
                                onChange={(e) => setActionRemarks(e.target.value)}
                                sx={{ mt: 1 }}
                              />
                            ) : null}
                          </Box>
                        )}
                        {scoringSaved && (
                        <Box sx={{ display: 'flex', justifyContent: 'flex-end', width: '100%' }}>
                          <Button
                            variant="contained"
                            color="primary"
                            disabled={
                              busy ||
                              !actionValue ||
                              (sendBack && !rejectReason.trim()) ||
                              (!sendBack && !actionRemarks.trim()) ||
                              (!sendBack &&
                                !sendForAcceptance &&
                                !!actionDueDateError(approveNextReviewDate, true))
                            }
                            onClick={() => {
                              if (sendForAcceptance) {
                                void run(
                                  () =>
                                    api.decideRiskFinalStatus(
                                      detail.id,
                                      'REJECT',
                                      actionRemarks.trim(),
                                      { startAcceptance: true },
                                    ),
                                  'Sent to Risk Acceptance.',
                                )
                                return
                              }
                              if (sendBack) {
                                void run(
                                  () =>
                                    api.decideRiskFinalStatus(detail.id, 'REJECT', rejectReason.trim(), {
                                      sendBack: true,
                                    }),
                                  'Sent back to custodian',
                                )
                                return
                              }
                              void run(
                                () =>
                                  api.decideRiskFinalStatus(detail.id, 'APPROVE', actionRemarks.trim(), {
                                    actionStatus: actionValue as RiskStatus,
                                    nextReviewDate: approveNextReviewDate,
                                  }),
                                `Approved · ${
                                  actionChoices.find((a) => a.value === actionValue)?.label ??
                                  configStatusLabel(actionValue)
                                }`,
                              )
                            }}
                          >
                            {actionButtonLabel}
                          </Button>
                        </Box>
                        )}
                      </Stack>
                      )
                    })()}
                  </Stack>
                )}
                {canAdmin && detail.riskProjectId == null && detail.status === 'UNDER_VALIDATION' && (
                  <Stack spacing={1}>
                    {!isErm && (
                      <CompactTextField
                        label={isPrivacy ? 'Privacy status (final)' : 'InfoSec status (final)'}
                        multiline
                        minRows={2}
                        fieldIdPrefix="risk-val"
                        value={infosecStatusFinal}
                        onChange={(e) => setInfosecStatusFinal(e.target.value)}
                        helperText="Confirmed at validation (C5)"
                      />
                    )}
                    <Button
                      variant="contained"
                      disabled={busy}
                      onClick={() =>
                        void run(
                          () =>
                            api.approveRiskValidation(
                              detail.id,
                              null,
                              !isErm ? infosecStatusFinal.trim() || null : null,
                            ),
                          'Treatment path approved',
                        )
                      }
                    >
                      Approve Treatment Path
                    </Button>
                    <CompactTextField
                      label="Reject reason"
                      fieldIdPrefix="risk-val"
                      value={rejectReason}
                      onChange={(e) => setRejectReason(e.target.value)}
                    />
                    <Button
                      color="error"
                      disabled={busy || !rejectReason.trim()}
                      onClick={() =>
                        void run(
                          () => api.rejectRiskValidation(detail.id, rejectReason.trim()),
                          'Send back to Risk Custodian',
                        )
                      }
                    >
                      Reject / Send Back
                    </Button>
                  </Stack>
                )}
                {canAdmin && detail.riskProjectId == null && (detail.status === 'MITIGATING' || detail.status === 'OVERDUE') && (
                  <Stack spacing={1}>
                    <CompactTextField
                      label="New milestone"
                      fieldIdPrefix="risk-close"
                      value={milestoneTitle}
                      onChange={(e) => setMilestoneTitle(e.target.value)}
                    />
                    <CompactTextField
                      label="Milestone due date (optional)"
                      type="date"
                      fieldIdPrefix="risk-close"
                      value={milestoneDueDate}
                      onChange={(e) => setMilestoneDueDate(e.target.value)}
                      error={!!milestoneDueDateError}
                      helperText={milestoneDueDateError || undefined}
                      inputProps={{ min: dateMin }}
                    />
                    <Button
                      disabled={busy || !milestoneTitle.trim() || !!milestoneDueDateError}
                      onClick={() =>
                        void run(
                          () =>
                            api.addRiskMilestone(detail.id, {
                              milestoneTitle: milestoneTitle.trim(),
                              dueDate: milestoneDueDate || null,
                            }),
                          'Milestone added',
                        ).then(() => {
                          setMilestoneTitle('')
                          setMilestoneDueDate('')
                        })
                      }
                    >
                      Add Milestone
                    </Button>
                    <Typography variant="caption" color="text.secondary">
                      Residual re-score at close (optional)
                    </Typography>
                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                      <FormSelect
                        label="Probability"
                        value={closeProbability}
                        options={scoreLevels.map((l) => ({
                          value: l,
                          label: fmtLevel(l),
                          tone: apiLevelTone(l),
                        }))}
                        onChange={(v) => setCloseProbability(v as RiskLevel)}
                        fullWidth
                      />
                      <FormSelect
                        label="Impact"
                        value={closeImpact}
                        options={scoreLevels.map((l) => ({
                          value: l,
                          label: fmtLevel(l),
                          tone: apiLevelTone(l),
                        }))}
                        onChange={(v) => setCloseImpact(v as RiskLevel)}
                        fullWidth
                      />
                      <FormSelect
                        label={CONTROL_EFFECTIVENESS_LABEL}
                        value={closeControlEff}
                        options={scoreLevels.map((l) => ({
                          value: l,
                          label: fmtLevel(l),
                          tone: apiLevelTone(l),
                        }))}
                        onChange={(v) => setCloseControlEff(v as RiskLevel)}
                        fullWidth
                      />
                    </Stack>
                    <CompactTextField
                      label="Close notes"
                      fieldIdPrefix="risk-close"
                      value={closeNotes}
                      onChange={(e) => setCloseNotes(e.target.value)}
                    />
                    <Button
                      variant="contained"
                      disabled={busy}
                      onClick={() =>
                        void run(
                          () =>
                            api.closeRisk(detail.id, {
                              probability: closeProbability,
                              impact: closeImpact,
                              controlEffectiveness: closeControlEff,
                              closeNotes: closeNotes.trim() || null,
                            }),
                          'Risk closed',
                        )
                      }
                    >
                      Close Risk
                    </Button>
                  </Stack>
                )}
                {detail.status === 'PENDING_ACCEPTANCE' && !detail.acceptanceForm && (
                  <Typography variant="body2" color="text.secondary">
                    {detail.assignedContributorName
                      ? `The acceptance form is with ${detail.assignedContributorName}. Approval opens after they submit.`
                      : 'The acceptance form is with the custodian. Approval opens after they submit.'}
                  </Typography>
                )}
                {waitingMyApproval(detail, meId, false) &&
                  detail.status === 'PENDING_ACCEPTANCE' && (
                  <Stack spacing={1}>
                    <FormControlLabel
                      control={
                        <Checkbox
                          checked={ownerDeclaration}
                          onChange={(e) => setOwnerDeclaration(e.target.checked)}
                        />
                      }
                      label="I declare Risk Owner review"
                    />
                    <CompactTextField
                      label="Comments"
                      fieldIdPrefix="risk-owner"
                      value={ownerComments}
                      onChange={(e) => setOwnerComments(e.target.value)}
                    />
                    <Stack direction="row" spacing={1}>
                      <Button
                        variant="contained"
                        disabled={busy || !ownerDeclaration}
                        onClick={() =>
                          void run(
                            () =>
                              api.ownerDecideRiskAcceptance(detail.id, {
                                decision: 'APPROVED',
                                comments: ownerComments.trim() || null,
                                declaration: true,
                              }),
                            'Sent to Admin',
                          )
                        }
                      >
                        Approve Acceptance
                      </Button>
                      <Button
                        color="error"
                        disabled={busy || !ownerDeclaration}
                        onClick={() =>
                          void run(
                            () =>
                              api.ownerDecideRiskAcceptance(detail.id, {
                                decision: 'REJECTED',
                                comments: ownerComments.trim() || null,
                                declaration: true,
                              }),
                            'Rejected by Risk Owner',
                          )
                        }
                      >
                        Reject
                      </Button>
                    </Stack>
                  </Stack>
                )}
                {canAdmin && detail.status === 'PENDING_ADMIN_REVIEW' && (
                  <Stack spacing={1.25}>
                    <Box
                      sx={{
                        border: `1px solid ${brand.border}`,
                        borderRadius: 1,
                        p: 1.25,
                        bgcolor: brand.tableHeader,
                      }}
                    >
                      <Typography
                        sx={{
                          fontSize: '0.6875rem',
                          color: brand.muted,
                          lineHeight: 1.4,
                          mb: 1,
                        }}
                      >
                        The three approvers have accepted this risk. Set the next review date to finish.
                      </Typography>
                      <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 2, flexWrap: 'wrap' }}>
                        <FormSelect
                          label={EXCEL_FIELD.action}
                          wrapLabel
                          required
                          minWidth={200}
                          maxWidth={240}
                          value="RISK_ACCEPTED"
                          options={[{ value: 'RISK_ACCEPTED', label: 'Risk Accepted' }]}
                          onChange={() => undefined}
                        />
                        <CompactTextField
                          label={EXCEL_FIELD.nextReviewDate}
                          wrapLabel
                          required
                          type="date"
                          value={approveNextReviewDate}
                          onChange={(e) => setApproveNextReviewDate(e.target.value)}
                          error={!!actionDueDateError(approveNextReviewDate, true)}
                          helperText={
                            actionDueDateError(approveNextReviewDate, true) || 'Today or later.'
                          }
                          inputProps={{ min: dateMin }}
                          maxWidth={EXCEL_DATE_FIELD_WIDTH}
                        />
                      </Box>
                    </Box>
                    <Box sx={{ display: 'flex', justifyContent: 'flex-end', width: '100%' }}>
                      <Button
                        variant="contained"
                        disabled={busy || !!actionDueDateError(approveNextReviewDate, true)}
                        onClick={() =>
                          void run(
                            () =>
                              api.adminDecideRiskAcceptance(detail.id, {
                                decision: 'ACCEPTED',
                                comments: null,
                                declaration: true,
                                nextReviewDate: approveNextReviewDate,
                              }),
                            'Risk accepted',
                          )
                        }
                      >
                        Approve
                      </Button>
                    </Box>
                  </Stack>
                )}
                {canAdmin && detail.riskProjectId == null && terminal.includes(detail.status) && (
                  <Stack spacing={1}>
                    <CompactTextField
                      label="Reopen reason (mandatory)"
                      fieldIdPrefix="risk-reopen"
                      value={reopenReason}
                      onChange={(e) => setReopenReason(e.target.value)}
                    />
                    <Button
                      disabled={busy || !reopenReason.trim()}
                      onClick={() =>
                        void run(
                          () => api.reopenRisk(detail.id, reopenReason.trim()),
                          'Risk re-opened',
                        )
                      }
                    >
                      Re-open
                    </Button>
                  </Stack>
                )}
                {!canAdmin &&
                  !isRiskOwner &&
                  !['DRAFT', 'SUBMITTED', 'UNDER_VALIDATION', 'MITIGATING'].includes(
                    detail.status,
                  ) && (
                    <Typography variant="caption" color="text.secondary">
                      No actions available for your role on this status.
                    </Typography>
                  )}
              </DetailSection>
              )}
              </Stack>
            </Box>
          </RiskFormShell>
        )}
      </Box>


      <Dialog
        open={heatMapOpen}
        onClose={() => setHeatMapOpen(false)}
        maxWidth="sm"
        fullWidth
        PaperProps={{ sx: { maxWidth: 420 } }}
      >
        <DialogContent sx={{ p: 2 }}>
          <RiskHeatMapPanel
            riskType={riskType}
            compact
            presentation="dialog"
            onError={onError}
            onCellClick={(d) => {
              setDrill(d)
              setHeatMapOpen(false)
            }}
          />
        </DialogContent>
        <DialogActions sx={{ px: 2, pb: 1.5, pt: 0 }}>
          <Button size="small" onClick={() => setHeatMapOpen(false)}>
            Close
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={bulkImportOpen}
        onClose={() => !bulkImportBusy && setBulkImportOpen(false)}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle>Bulk Import {isErm ? 'ERM' : isPrivacy ? 'Privacy' : 'IS'} risks</DialogTitle>
        <DialogContent>
          <Stack spacing={1.5} sx={{ mt: 1 }}>
            <Typography variant="body2" color="text.secondary">
              Download the template, fill the required fields, then upload. New rows are added to
              this project. Risks already created here stay.
            </Typography>
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
              <Button
                size="small"
                variant="outlined"
                disabled={busy || bulkImportBusy}
                onClick={() => void downloadBulkTemplate()}
              >
                Download Template
              </Button>
              <Button
                size="small"
                variant="contained"
                disabled={bulkImportBusy}
                onClick={() => bulkFileRef.current?.click()}
              >
                {bulkImportBusy ? 'Importing…' : 'Upload Excel'}
              </Button>
              <input
                ref={bulkFileRef}
                type="file"
                hidden
                accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                onChange={(e) => {
                  const f = e.target.files?.[0]
                  e.target.value = ''
                  if (f) void runBulkImport(f)
                }}
              />
            </Stack>
            {bulkImportResult && (
              <Box>
                <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
                  Created {bulkImportResult.createdCount} · Failed {bulkImportResult.failedCount}
                  {bulkImportResult.ignoredCount > 0
                    ? ` · ${bulkImportResult.ignoredCount} sample row(s) ignored`
                    : ''}
                </Typography>
                <TableContainer sx={{ maxHeight: 280 }}>
                  <Table size="small" stickyHeader>
                    <TableHead>
                      <TableRow>
                        <TableCell>Row</TableCell>
                        <TableCell>Result</TableCell>
                        <TableCell>Detail</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {bulkImportResult.rows.map((r) => (
                        <TableRow key={r.excelRow}>
                          <TableCell>{r.excelRow}</TableCell>
                          <TableCell>{r.ignored ? 'Ignored' : r.success ? 'OK' : 'Failed'}</TableCell>
                          <TableCell>
                            {r.success
                              ? [r.displayId, r.error].filter(Boolean).join(': ')
                              : (r.error ?? '-')}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Box>
            )}
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 2, pb: 1.5, pt: 0 }}>
          <Button size="small" disabled={bulkImportBusy} onClick={() => setBulkImportOpen(false)}>
            Close
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={matricesOpen} onClose={() => setMatricesOpen(false)} fullWidth maxWidth="md">
        <DialogTitle>Risk Matrices (Tenant)</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Alert severity="info">
              Matrix size applies to both inherent and residual scoring. Changes apply to new scores only -
              existing risks are not recalculated. Version {matrixMeta?.version ?? 0}
              {matrixMeta?.usingDefaults ? ' (using §13.3 defaults)' : ''}.
            </Alert>
            <FormSelect
              label="Matrix size"
              minWidth={220}
              value={String(editGridSize)}
              onChange={(v) => {
                const next = Number(v) as MatrixGridSize
                const axes = defaultAxes(next)
                setEditGridSize(next)
                applyEditAxes([...axes.rows], [...axes.cols])
              }}
              options={([3, 4, 5] as const).map((n) => ({
                value: String(n),
                label: `${n}×${n}`,
              }))}
            />
            <Box>
              <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 0.75 }}>
                Axis level presets: choose which severity bands appear on each axis
              </Typography>
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                {AXIS_PRESETS[editGridSize].map((preset) => (
                  <Button
                    key={preset.label}
                    size="small"
                    variant="outlined"
                    onClick={() => applyEditAxes([...preset.rows], [...preset.cols])}
                  >
                    {preset.label}
                  </Button>
                ))}
              </Stack>
            </Box>
            <AxisLevelEditor
              label="Row axis levels"
              hint="Level 1 = lowest severity, Level N = highest (top row of grid = highest)"
              axis="row"
              levels={editRowLevels}
              onChange={(rows) => applyEditAxes(rows, editColLevels)}
            />
            <AxisLevelEditor
              label="Column axis levels"
              hint="Level 1 = lowest severity, Level N = highest (left column = lowest)"
              axis="col"
              levels={editColLevels}
              onChange={(cols) => applyEditAxes(editRowLevels, cols)}
            />
            {!axesValid && (
              <Alert severity="warning">
                Each axis level must be distinct: pick a preset or adjust the dropdowns above.
              </Alert>
            )}
            <MatrixEditor
              title="Inherent = Probability × Impact"
              rowLabel="Probability ↓"
              colLabel="Impact →"
              axes={editAxes}
              grid={editInherent}
              onChange={setEditInherent}
            />
            <MatrixEditor
              title="Residual P×I grid (manual path: Excel product bands)"
              rowLabel="Residual probability ↓"
              colLabel="Residual impact →"
              axes={editAxes}
              grid={editResidual}
              onChange={setEditResidual}
            />
            <Typography variant="caption" color="text.secondary">
              Auto residual uses Control effectiveness × Inherent (§13.3), not this grid.
            </Typography>
            <CompactTextField
              label="Change reason"
              required
              multiline
              minRows={2}
              value={matrixChangeReason}
              onChange={(e) => setMatrixChangeReason(e.target.value)}
              helperText={!matrixChangeReason.trim() ? 'Required to save matrix changes' : undefined}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => {
              const axes = defaultAxes(editGridSize)
              applyEditAxes([...axes.rows], [...axes.cols])
            }}
          >
            Reset to Defaults
          </Button>
          <Button onClick={() => setMatricesOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={busy || !matrixChangeReason.trim() || !axesValid}
            onClick={() =>
              void (async () => {
                setBusy(true)
                try {
                  const m = await api.updateRiskMatrices({
                    gridSize: editGridSize,
                    rowLevels: editRowLevels,
                    colLevels: editColLevels,
                    inherent: editInherent,
                    residual: editResidual,
                    changeReason: matrixChangeReason.trim(),
                  })
                  setInherentGrid(m.inherent)
                  setResidualGrid(m.residual)
                  setMatrixMeta(m)
                  setMatricesOpen(false)
                  onOk(`Matrices updated to version ${m.version}`)
                } catch (e) {
                  onError(String(e))
                } finally {
                  setBusy(false)
                }
              })()
            }
          >
            Save Matrices
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={ownerOpen} onClose={() => setOwnerOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>{EXCEL_FIELD.riskOwner}</DialogTitle>
        <DialogContent>
          <FormSelect
            label={EXCEL_FIELD.riskOwner}
            value={ownerPick === '' ? '' : String(ownerPick)}
            options={[
              { value: '', label: 'Select' },
              ...users.map((u) => ({
                value: String(u.id),
                label: `${u.fullName} (${u.email})`,
              })),
            ]}
            onChange={(v) => setOwnerPick(v === '' ? '' : Number(v))}
            fullWidth
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOwnerOpen(false)}>Cancel</Button>
          <Button variant="contained" disabled={busy || ownerPick === ''} onClick={() => void saveOwner()}>
            Save
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={assignOpen} onClose={() => setAssignOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>{detail?.status === 'ASSIGNED' ? `Reassign ${EXCEL_FIELD.riskCustodian}` : `Assign ${EXCEL_FIELD.riskCustodian}`}</DialogTitle>
        <DialogContent>
          <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 1, mb: 1 }}>
            {detail?.orgUnitId == null
              ? `Set ${EXCEL_FIELD.division} on the risk before assigning a ${EXCEL_FIELD.riskCustodian}.`
              : `Filtered to ${EXCEL_FIELD.division.toLowerCase()} (${orgLabel(detail.orgUnitId)}).`}
          </Typography>
          <FormSelect
            label={EXCEL_FIELD.riskCustodian}
            value={assignUserId === '' ? '' : String(assignUserId)}
            options={[
              {
                value: '',
                label:
                  detail?.orgUnitId == null
                    ? `Set ${EXCEL_FIELD.division} first`
                    : eligibleContributors.length === 0
                      ? 'No users in this division'
                      : 'Select risk custodian…',
              },
              ...eligibleContributors.map((u) => ({
                value: String(u.id),
                label: `${u.fullName} (${u.email})`,
              })),
            ]}
            onChange={(v) => setAssignUserId(v === '' ? '' : Number(v))}
            disabled={detail?.orgUnitId == null || eligibleContributors.length === 0}
            fullWidth
          />
          {detail?.orgUnitId == null ? (
            <Alert severity="info" sx={{ mt: 1.5, py: 0 }}>
              This risk has no {EXCEL_FIELD.division.toLowerCase()}. Edit the risk, choose a division, then
              assign a {EXCEL_FIELD.riskCustodian}.
            </Alert>
          ) : eligibleContributors.length === 0 ? (
            <Alert severity="warning" sx={{ mt: 1.5, py: 0 }}>
              No Contributor is linked to {orgLabel(detail.orgUnitId)}. Link a Contributor to this
              division in Settings → Users, then assign again.
            </Alert>
          ) : assignNeedsTargetDate ? (
            <Alert severity="info" sx={{ mt: 1.5, py: 0 }}>
              The {EXCEL_FIELD.riskCustodian} task is due on the {EXCEL_FIELD.targetDate}. Set the{' '}
              {EXCEL_FIELD.targetDate} on the risk, then assign.
            </Alert>
          ) : null}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAssignOpen(false)}>Cancel</Button>
          <BusyButton
            variant="contained"
            busy={busy}
            busyLabel="Assigning…"
            disabled={busy || assignBlockedReason != null}
            disabledReason={busy ? undefined : assignBlockedReason ?? undefined}
            onClick={() => void assign()}
          >
            Assign
          </BusyButton>
        </DialogActions>
      </Dialog>

      <ConfirmActionDialog
        open={cancelOpen}
        title="Cancel this risk?"
        message="The custodian task will close. This risk stays on the register as Cancelled. This cannot be undone from Assigned."
        confirmLabel={busy ? 'Cancelling…' : 'Cancel Risk'}
        confirmColor="error"
        busy={busy}
        onCancel={() => setCancelOpen(false)}
        onConfirm={() => {
          if (!detail) return
          void run(async () => {
            const r = await api.cancelRisk(detail.id, cancelReason)
            setCancelOpen(false)
            setCancelReason('')
            return r
          }, 'Risk cancelled')
        }}
      >
        <CompactTextField
          label="Reason"
          wrapLabel
          multiline
          minRows={2}
          value={cancelReason}
          onChange={(e) => setCancelReason(e.target.value)}
          fieldIdPrefix="cancel-risk"
        />
      </ConfirmActionDialog>
      <ConfirmActionDialog
        open={deleteDraftTarget != null}
        title="Delete this draft?"
        message={`"${deleteDraftTarget?.title?.trim() || 'Untitled draft'}" and its reference attachments will be removed. No risk is created. This cannot be undone.`}
        confirmLabel={busy ? 'Deleting…' : 'Delete Draft'}
        confirmColor="error"
        busy={busy}
        onCancel={() => setDeleteDraftTarget(null)}
        onConfirm={() => void confirmDeleteDraft()}
      />
      <ConfirmActionDialog
        open={confirmNoteId != null}
        title="Delete this note?"
        message="This removes the note from the risk. This cannot be undone."
        confirmLabel={busy ? 'Deleting…' : 'Delete'}
        confirmColor="error"
        busy={busy}
        onCancel={() => setConfirmNoteId(null)}
        onConfirm={() => {
          if (confirmNoteId == null || !detail) return
          const noteId = confirmNoteId
          setConfirmNoteId(null)
          void run(async () => {
            await api.deleteRiskNote(detail.id, noteId)
            await refreshNotesAndHistory(detail.id)
            return api.getRisk(detail.id)
          }, 'Note deleted')
        }}
      />
      <DiscardChangesDialog
        open={discardDialogOpen}
        onKeepEditing={() => {
          setDiscardDialogOpen(false)
          pendingDetailActionRef.current = null
        }}
        onDiscard={() => {
          setDiscardDialogOpen(false)
          const action = pendingDetailActionRef.current
          pendingDetailActionRef.current = null
          action?.()
        }}
      />
    </Stack>
  )
}
