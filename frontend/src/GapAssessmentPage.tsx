import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import {
  Box,
  Button,
  Card,
  Checkbox,
  Collapse,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  IconButton,
  LinearProgress,
  CircularProgress,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
  alpha,
} from '@mui/material'
import FlagIcon from '@mui/icons-material/Flag'
import FlagOutlinedIcon from '@mui/icons-material/FlagOutlined'
import {
  api,
  type EvidenceSummary,
  type FrameworkQuestion,
  type FrameworkSummary,
  type GapApplicability,
  type GapAssessmentDetail,
  type GapAssessmentSummary,
  type GapAssessmentType,
  type GapImplementationStatus,
  type GapItem,
  type GapItemReviewStatus,
  type GapItemSubmission,
  type GapCustomQuestion,
  type OrgUnitNode,
  type Session,
  type SoaSummary,
  type UserSummary,
} from './api'
import { isGapAssessor } from './gapAssessor'
import AssessmentRoundedIcon from '@mui/icons-material/AssessmentRounded'
import ExpandMoreRoundedIcon from '@mui/icons-material/ExpandMoreRounded'
import EvidenceAttachedList from './components/evidence/EvidenceAttachedList'
import { gapQuestionRecordId, loadGapQuestionEvidence } from './components/gap/gapEvidence'
import StatusChip from './components/StatusChip'
import {
  AttentionBanner,
  BusyButton,
  CompactTextField,
  ConfirmActionDialog,
  FieldActionRow,
  FilterPill,
  INPUT_LIMIT,
  FormFieldLabel,
  FormSelect,
  RegisterListFrame,
  RegisterToolbar,
  ReviewDossierDrawer,
  RiskLevelBadge,
  SettingsCard,
  PageLoading,
  WorkspaceEmptyPanel,
  registerTableSx,
  primaryTableCellSx,
  primaryRegisterNameSx,
  compactTableCellSx,
  lastTableCellSx,
} from './components/ui'
import { readOnlyCaptionSx, sectionLabelSx } from './components/ui/FormFieldLabel'
import DiscardChangesDialog from './components/DiscardChangesDialog'
import { useListReady } from './uiQuality/useListReady'
import { useOptionalUnsavedChangesGuardApi, useRegisterUnsavedChanges } from './uiQuality/UnsavedChangesGuard'
import { usePagedRows } from './uiQuality/usePagedRows'
import { useBeforeUnload } from './uiQuality/useBeforeUnload'
import { formatTenantDate } from './uiQuality/tenantLocale'
import { NARRATIVE_FIELD_MAX_WIDTH, NARRATIVE_MAX_ROWS, narrativeReadScrollSx, twoColFieldSx } from './components/ui/formLayout'
import { uaeIaRagToBadge } from './components/ui/riskBadgeMappers'
import { GAP_REGISTER_COLUMNS } from './components/gap/gapRegisterColumns'
import { useRegisterColumnPicker } from './hooks/useRegisterColumnPicker'
import GapDomainSections, {
  gapControlReleased,
  gapReleaseBlockReason,
  gapSendBackBlockReason,
  gapWorkflowLabel,
} from './components/gap/GapDomainSections'
import SamaMaturityGatesEditor from './components/gap/SamaMaturityGatesEditor'
import {
  emptyGates,
  maturityFromGates,
  normalizeGates,
  type GateAnswer,
} from './components/gap/samaMaturityGates'
import {
  computeGapProgressStats,
  deriveGapDomain,
  formatGapScorePct,
  gapQuestionDisplayCode,
  gapStatusFromItem,
  gapScoreRagBand,
  gapDomainSectionTab,
  gapSectionTabCounts,
  groupItemsByDomain,
  isSamaFramework,
  resolveGapScoringProfile,
  type GapSectionTab,
} from './components/gap/gapUi'
import type { GapScoringProfile } from './components/gap/gapUi'
import { brand, semantic } from './theme'
import { actionDueDateError, dueDateBoundError, todayIso, useFormFields } from './validation'
import { frameworkDisplayName, frameworkLabel } from './uiQuality/frameworkLabel'

const IMPL: { value: GapImplementationStatus; label: string }[] = [
  { value: 'FULLY_IMPLEMENTED', label: 'Fully Implemented' },
  { value: 'PARTIALLY_IMPLEMENTED', label: 'Partially Implemented' },
  { value: 'PLANNED', label: 'Planned' },
  { value: 'NOT_IMPLEMENTED', label: 'Not Implemented' },
]

const SAMA_MATURITY: { value: number; label: string }[] = [
  { value: 0, label: '0: Non-existent' },
  { value: 1, label: '1: Initial' },
  { value: 2, label: '2: Repeatable' },
  { value: 3, label: '3: Defined (target)' },
  { value: 4, label: '4: Managed' },
  { value: 5, label: '5: Optimized' },
]

const SAMA_TARGET_MATURITY = 3

const GAP_ANSWER_LABEL: Record<string, string> = {
  YES: 'Compliant',
  NO: 'Not Compliant',
  PARTIAL: 'Partially',
  NA: 'N/A',
}

function drawerStepId(step: number) {
  return `gap-drawer-step-${step}`
}

function scrollDrawerStep(step: number) {
  document.getElementById(drawerStepId(step))?.scrollIntoView({ block: 'start', behavior: 'smooth' })
}

function DrawerCard({
  step,
  title,
  subtitle,
  children,
}: {
  step?: number
  title: string
  subtitle?: string
  children: ReactNode
}) {
  return (
    <Box
      id={step != null ? drawerStepId(step) : undefined}
      sx={{ scrollMarginTop: 12, pt: 2.5, mt: 0.5, borderTop: `1px solid ${brand.border}` }}
    >
      {title ? (
        <Typography sx={{ fontSize: '0.6875rem', fontWeight: 700, letterSpacing: '0.06em', textTransform: 'uppercase', color: brand.goldDark }}>
          {title}
        </Typography>
      ) : null}
      {subtitle ? (
        <Typography sx={{ fontSize: '0.75rem', color: brand.muted, mt: 0.35, mb: 1.25 }}>{subtitle}</Typography>
      ) : title ? (
        <Box sx={{ mb: 1.25 }} />
      ) : null}
      {children}
    </Box>
  )
}

function CreamStepTabs({
  value,
  options,
  onChange,
}: {
  value: number
  options: { id: number; label: string }[]
  onChange: (id: number) => void
}) {
  if (options.length <= 1) return null
  return (
    <Stack direction="row" spacing={0} alignItems="stretch" sx={{ borderBottom: `1px solid ${brand.border}` }}>
      {options.map((opt, i) => {
        const active = value === opt.id
        const done = opt.id < value
        return (
          <Box
            key={opt.id}
            component="button"
            type="button"
            onClick={() => onChange(opt.id)}
            sx={{
              flex: 1,
              display: 'flex',
              alignItems: 'center',
              gap: 1,
              border: 0,
              borderBottom: active ? `2px solid ${brand.gold}` : '2px solid transparent',
              bgcolor: 'transparent',
              color: active ? brand.ink : brand.muted,
              px: 0.5,
              py: 1,
              cursor: 'pointer',
              fontFamily: 'inherit',
              textAlign: 'left',
            }}
          >
            <Box
              sx={{
                width: 22,
                height: 22,
                borderRadius: '50%',
                flexShrink: 0,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '0.6875rem',
                fontWeight: 700,
                bgcolor: active ? brand.gold : done ? brand.ink : 'transparent',
                color: active || done ? '#fff' : brand.muted,
                border: active || done ? 0 : `1px solid ${brand.border}`,
              }}
            >
              {done ? '✓' : String(i + 1).padStart(2, '0')}
            </Box>
            <Typography sx={{ fontSize: '0.8125rem', fontWeight: active ? 700 : 550, color: 'inherit' }}>
              {opt.label}
            </Typography>
          </Box>
        )
      })}
    </Stack>
  )
}

function prettyGapType(value: string): string {
  return value.replace(/_/g, ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase())
}

function reviewTone(status: GapItemReviewStatus): 'danger' | 'warning' | 'info' | 'success' | 'neutral' {
  switch (status) {
    case 'SENT_BACK':
      return 'warning'
    case 'SUBMITTED':
      return 'info'
    case 'FINALIZED':
      return 'success'
    default:
      return 'neutral'
  }
}

function itemHasGap(item: GapItem, sama: boolean): boolean {
  if (item.applicability !== 'APPLICABLE') return false
  if (sama) {
    const m = item.finalMaturity ?? item.maturity
    return m != null && m < SAMA_TARGET_MATURITY
  }
  const effective = item.finalImplementationStatus ?? item.implementationStatus
  return effective != null && effective !== 'FULLY_IMPLEMENTED'
}

type ItemFilter = 'ALL' | 'UNASSIGNED' | 'OPEN' | 'HAS_GAP' | 'COMPLIANT' | 'SENT_BACK' | 'FLAGGED'

function layerPanelTitle(profile: GapScoringProfile): string {
  switch (profile) {
    case 'UAE_IA':
      return 'UAE IA Scores'
    case 'SAMA':
      return 'SAMA domain / subdomain scores'
    case 'NCEMA':
      return 'NCEMA pillar scores'
    default:
      return 'Domain scores'
  }
}

function layerPanel(
  layers: NonNullable<GapAssessmentDetail['assessment']['layerScores']>,
  profile: GapScoringProfile,
) {
  const rows: { label: string; score: number | null; rag: string | null }[] = []
  if (profile !== 'UAE_IA') {
    if (layers.management) {
      rows.push({ label: 'Management', ...layers.management })
    }
    if (layers.technical) {
      rows.push({ label: 'Technical', ...layers.technical })
    }
  }
  for (const d of layers.domains) {
    rows.push({ label: d.name || d.code, score: d.score, rag: d.rag })
  }
  if (layers.overallUaeIa) {
    const overallLabel =
      profile === 'SAMA'
        ? 'Overall (SAMA)'
        : profile === 'NCEMA'
          ? 'Overall (NCEMA)'
          : profile === 'UAE_IA'
            ? 'Overall UAE IA'
            : 'Overall'
    rows.push({ label: overallLabel, ...layers.overallUaeIa })
  }
  return rows
}

function layerScoreTone(score: number | null | undefined, rag: string | null | undefined) {
  const band = gapScoreRagBand(score, rag)
  if (!band) {
    return { color: brand.ink, bg: brand.paper, border: brand.border }
  }
  const level = uaeIaRagToBadge(band)
  const color = semantic.risk[level]
  return {
    color,
    bg: alpha(color, 0.12),
    border: alpha(color, 0.28),
  }
}

function LayerScoreCard({
  title,
  layer,
}: {
  title: string
  layer: { score: number | null; rag: string | null; applicableCount: number; scoredCount: number } | null
}) {
  const tone = layerScoreTone(layer?.score, layer?.rag)
  return (
    <SettingsCard
      compact
      autoHeight
      title={title}
      subtitle={`${layer?.scoredCount ?? 0} of ${layer?.applicableCount ?? 0} controls assessed`}
    >
      <Stack direction="row" alignItems="baseline" spacing={1.25}>
        <Typography sx={{ fontSize: '1.5rem', fontWeight: 700, lineHeight: 1, color: tone.color }}>
          {layer?.score != null ? `${formatGapScorePct(layer.score)}%` : '-'}
        </Typography>
        {layer?.rag ? <RiskLevelBadge level={uaeIaRagToBadge(layer.rag)} label={layer.rag} /> : null}
      </Stack>
    </SettingsCard>
  )
}

/** Read-only value in the same shape as a create-form field. */
function ReadOnlyValue({
  label,
  value,
  onClick,
  scroll,
}: {
  label: string
  value: string
  onClick?: () => void
  scroll?: boolean
}) {
  const text = value.trim() || '-'
  return (
    <Box sx={{ minWidth: 0 }}>
      <Typography sx={{ fontSize: '0.6875rem', fontWeight: 600, color: brand.muted, mb: 0.5, lineHeight: 1.2 }}>
        {label}
      </Typography>
      <Box
        component={onClick ? 'button' : 'div'}
        type={onClick ? 'button' : undefined}
        onClick={onClick}
        role="textbox"
        aria-readonly="true"
        aria-label={label}
        sx={{
          width: '100%',
          textAlign: 'left',
          font: 'inherit',
          fontSize: '0.75rem',
          lineHeight: 1.4,
          color: text === '-' ? brand.muted : brand.ink,
          bgcolor: brand.hover,
          border: `1px solid ${brand.border}`,
          borderRadius: '6px',
          px: 1,
          cursor: onClick ? 'pointer' : 'default',
          ...(scroll
            ? {
                minHeight: 40,
                maxHeight: 72,
                overflowY: 'auto',
                py: 0.75,
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
              }
            : {
                minHeight: 32,
                display: 'flex',
                alignItems: 'center',
                py: 0.5,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }),
        }}
      >
        {text}
      </Box>
    </Box>
  )
}

export default function GapAssessmentPage({
  session,
  projectId,
  projectFrameworks,
  projectLabel,
  projectName,
  embedded,
  onError,
  onOk,
  onOpenSoa,
  onPageActionsChange,
  onHeaderMetaChange,
  soaApplicable = true,
  initialGapId,
  initialItemId,
  startCreate,
  recordOnly,
  onLeaveRecord,
}: {
  session: Session
  projectId: number
  projectFrameworks: FrameworkSummary[]
  /** Project cycle label for AppShell breadcrumb (e.g. "ADFCA · 2026"). */
  projectLabel?: string
  /** Project name only (identity row). */
  projectName?: string
  embedded?: boolean
  onError: (msg: string) => void
  onOk: (msg: string) => void
  onOpenSoa?: (soaId: number) => void
  /** Lift Export / Complete / New assessment CTAs to AppShell topbar (kit Screen 3). */
  onPageActionsChange?: (actions: ReactNode | null) => void
  onHeaderMetaChange?: (meta: { title: string; breadcrumbTrail: string } | null) => void
  /** False for Gap-only modules (BCMS, QMS, OHSMS, SAMA). */
  soaApplicable?: boolean
  initialGapId?: number | null
  /** After the assessment opens, open this control drawer (notification deep-link). */
  initialItemId?: number | null
  startCreate?: boolean
  /** Queue open: this Gap only. No list, no New. Back leaves the queue. */
  recordOnly?: boolean
  onLeaveRecord?: () => void
}) {
  const canAdmin = session.roles.some((r) => ['OWNER', 'ADMIN'].includes(r))
  const canExport = session.roles.some((r) => ['OWNER', 'ADMIN'].includes(r))
  const canEdit = session.roles.some((r) => ['OWNER', 'ADMIN', 'CONTRIBUTOR'].includes(r))

  const [rows, setRows] = useState<GapAssessmentSummary[]>([])
  const [soas, setSoas] = useState<SoaSummary[]>([])
  const [detail, setDetail] = useState<GapAssessmentDetail | null>(null)
  const [busy, setBusy] = useState(false)
  const [drawerWorking, setDrawerWorking] = useState<{ title: string; message: string } | null>(null)
  const { listReady, markReady } = useListReady()
  const [discardDrawer, setDiscardDrawer] = useState(false)
  const [createOpen, setCreateOpen] = useState(false)
  const [creating, setCreating] = useState(false)
  const [openingDetail, setOpeningDetail] = useState(false)
  const [frameworkId, setFrameworkId] = useState<number | ''>('')
  const [name, setName] = useState('')
  const [assessmentType, setAssessmentType] = useState<GapAssessmentType>('INTERNAL')
  const [scope, setScope] = useState('')
  const [targetDate, setTargetDate] = useState('')
  const [assessorUserId, setAssessorUserId] = useState<number | ''>('')
  const [assessmentDepth, setAssessmentDepth] = useState<3 | 4>(4)
  const [allUsers, setAllUsers] = useState<UserSummary[]>([])
  const [soaPromptOpen, setSoaPromptOpen] = useState(false)
  const [soaPromptOverrides, setSoaPromptOverrides] = useState<GapItem[]>([])
  const [soaPromptSoaId, setSoaPromptSoaId] = useState<number | null>(null)
  const [search, setSearch] = useState('')
  const [controlQuery, setControlQuery] = useState('')

  const [editItem, setEditItem] = useState<GapItem | null>(null)
  const [applicability, setApplicability] = useState<GapApplicability>('APPLICABLE')
  const [overrideJustification, setOverrideJustification] = useState('')
  const [implementationStatus, setImplementationStatus] = useState<GapImplementationStatus | ''>('')
  const [maturityGates, setMaturityGates] = useState<GateAnswer[]>(emptyGates())
  const [observations, setObservations] = useState('')
  const [whatItMeans, setWhatItMeans] = useState('')
  const [evidenceGuidance, setEvidenceGuidance] = useState('')
  const [gapDescription, setGapDescription] = useState('')
  const [finalStatus, setFinalStatus] = useState<GapImplementationStatus | ''>('')
  const [finalMaturity, setFinalMaturity] = useState<number | ''>('')
  const [adminNotes, setAdminNotes] = useState('')
  const [sendBackReason, setSendBackReason] = useState('')
  const [submissions, setSubmissions] = useState<GapItemSubmission[]>([])
  const [contributors, setContributors] = useState<UserSummary[]>([])
  const [assignUserId, setAssignUserId] = useState<number | ''>('')
  const [assignDue, setAssignDue] = useState('')
  const [assignOrgUnitId, setAssignOrgUnitId] = useState<number | ''>('')
  const [assignAllQuestions, setAssignAllQuestions] = useState(true)
  const [assignFwQuestionIds, setAssignFwQuestionIds] = useState<number[]>([])
  const [assignCustomQuestionIds, setAssignCustomQuestionIds] = useState<number[]>([])
  const [orgUnits, setOrgUnits] = useState<OrgUnitNode[]>([])
  const [itemEvidence, setItemEvidence] = useState<EvidenceSummary[]>([])
  const [questionEvidence, setQuestionEvidence] = useState<Record<string, EvidenceSummary[]>>({})
  const [controlQuestions, setControlQuestions] = useState<FrameworkQuestion[]>([])
  const [customQuestions, setCustomQuestions] = useState<GapCustomQuestion[]>([])
  const [newCustomQuestion, setNewCustomQuestion] = useState('')
  const [justAddedCustomQuestionId, setJustAddedCustomQuestionId] = useState<number | null>(null)
  const [addingQuestion, setAddingQuestion] = useState(false)
  const [questionAnswers, setQuestionAnswers] = useState<
    Record<number, { answer: string; note: string }>
  >({})
  const [answerBaseline, setAnswerBaseline] = useState<string | null>(null)
  const [customAnswers, setCustomAnswers] = useState<Record<number, { answer: string; note: string }>>({})
  const editItemId = editItem?.id ?? null
  useEffect(() => {
    setJustAddedCustomQuestionId(null)
  }, [editItemId])

  const [itemFilter, setItemFilter] = useState<ItemFilter>('ALL')
  const [selectedIds, setSelectedIds] = useState<number[]>([])
  const [sendConfirmOpen, setSendConfirmOpen] = useState(false)
  const [drawerSendOpen, setDrawerSendOpen] = useState(false)
  const [sendBackOpen, setSendBackOpen] = useState(false)
  const [bulkSendBackReason, setBulkSendBackReason] = useState('')
  const [listAction, setListAction] = useState<'send' | 'send-back' | null>(null)
  const [flagBusy, setFlagBusy] = useState(false)
  const [drawerStep, setDrawerStep] = useState(1)
  const pendingGapItem = useRef<GapItem | null>(null)
  const openedInitial = useRef(false)
  const drawerScrollCleanup = useRef<(() => void) | null>(null)
  const startedCreate = useRef(false)
  const [sectionTab, setSectionTab] = useState<GapSectionTab>('CLAUSE')
  const [layerScoresOpen, setLayerScoresOpen] = useState(false)
  const [openDomains, setOpenDomains] = useState<Record<string, boolean>>({})
  const gapColumns = useRegisterColumnPicker(GAP_REGISTER_COLUMNS)
  const createFields = useFormFields<'targetDate'>()
  const assignFields = useFormFields<'assignDue'>()

  const dateMin = todayIso()
  const targetDateError = useMemo(() => actionDueDateError(targetDate), [targetDate])
  const assessmentTargetDate = detail?.assessment.targetDate ?? ''
  const assignDueError = useMemo(
    () => dueDateBoundError(assignDue, assessmentTargetDate, 'Gap Assessment target date'),
    [assignDue, assessmentTargetDate],
  )

  const load = useCallback(async () => {
    setBusy(true)
    try {
      const list = await api.listGapAssessments(projectId)
      setRows(list)
      try {
        setSoas(await api.listSoas(projectId))
      } catch {
        setSoas([])
      }
      if (canAdmin) {
        const users = await api.listUsers()
        setAllUsers(users)
        setContributors(users.filter((u) => u.roles.includes('CONTRIBUTOR')))
        const tree = await api.listOrgUnits(false).catch(() => [])
        const flat: OrgUnitNode[] = []
        const walk = (nodes: OrgUnitNode[]) => {
          for (const n of nodes) {
            if (n.active) flat.push(n)
            if (n.children?.length) walk(n.children)
          }
        }
        walk(tree)
        setOrgUnits(flat)
      }
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
      markReady()
    }
  }, [onError, projectId, canAdmin, markReady])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    if (!canAdmin) return
    if (assignOrgUnitId === '') {
      setContributors(allUsers.filter((u) => u.roles.includes('CONTRIBUTOR')))
      return
    }
    void api
      .listEligibleContributors(assignOrgUnitId)
      .then((rows) =>
        setContributors(
          rows.map(
            (r) =>
              ({
                id: r.id,
                email: r.email,
                fullName: r.fullName,
                roles: ['CONTRIBUTOR'],
                status: 'ACTIVE',
              }) as UserSummary,
          ),
        ),
      )
      .catch(() => setContributors([]))
  }, [assignOrgUnitId, allUsers, canAdmin])

  const openDetail = useCallback(async (id: number) => {
    setOpeningDetail(true)
    try {
      const d = await api.getGapAssessment(id)
      setDetail(d)
      setItemFilter('ALL')
      const profile = resolveGapScoringProfile(d.assessment.frameworkCode, d.assessment.frameworkName)
      const counts = gapSectionTabCounts(d.items, profile)
      if (profile === 'ISO_PACK') {
        setSectionTab(counts.clause > 0 ? 'CLAUSE' : counts.annex > 0 ? 'ANNEX' : 'CLOUD')
      } else {
        setSectionTab('ALL')
      }
    } catch (e) {
      onError(String(e))
      if (recordOnly) onLeaveRecord?.()
    } finally {
      setOpeningDetail(false)
    }
  }, [onError, recordOnly, onLeaveRecord])

  const closeDetail = useCallback(() => {
    if (busy) return
    if (recordOnly) {
      onLeaveRecord?.()
      return
    }
    setEditItem(null)
    setDetail(null)
  }, [busy, recordOnly, onLeaveRecord])

  const dismissCreate = () => {
    if (creating) return
    setCreateOpen(false)
    setAssessmentDepth(4)
    if (recordOnly && !detail) onLeaveRecord?.()
  }

  const create = async () => {
    if (frameworkId === '' || !name.trim() || !scope.trim() || assessorUserId === '' || creating) return
    setCreating(true)
    setBusy(true)
    try {
      const createFw = projectFrameworks.find((f) => f.id === frameworkId)
      const createUaeIa =
        resolveGapScoringProfile(createFw?.code, createFw?.name) === 'UAE_IA'
      const d = await api.createGapAssessment({
        projectId,
        frameworkId,
        name: name.trim(),
        assessmentType,
        targetDate: targetDate.trim() || null,
        scope: scope.trim(),
        assessorUserId,
        ...(createUaeIa ? { assessmentDepth } : {}),
      })
      setDetail(d)
      await load()
      onOk(
        d.assessment.soaId != null
          ? 'Gap assessment created (applicability from complete SoA)'
          : 'Gap assessment created (all controls Applicable: no SoA required)',
      )
      setCreateOpen(false)
      setName('')
      setFrameworkId('')
      setScope('')
      setTargetDate('')
      setAssessorUserId('')
      setAssessmentDepth(4)
    } catch (e) {
      onError(String(e))
    } finally {
      setCreating(false)
      setBusy(false)
    }
  }

  const syncEditForm = (item: GapItem) => {
    setEditItem(item)
    const lockedProfile = resolveGapScoringProfile(
      detail?.assessment.frameworkCode,
      detail?.assessment.frameworkName,
    )
    setApplicability(
      lockedProfile === 'UAE_IA' || lockedProfile === 'NCEMA' ? 'APPLICABLE' : item.applicability,
    )
    setOverrideJustification(item.overrideJustification ?? '')
    setImplementationStatus(item.implementationStatus ?? '')
    setMaturityGates(normalizeGates(item.maturityGates))
    setObservations(item.observations ?? '')
    setGapDescription(item.gapDescription ?? '')
    setWhatItMeans(item.whatItMeans ?? '')
    setEvidenceGuidance(item.evidenceGuidance ?? '')
    setFinalStatus(item.finalImplementationStatus ?? item.implementationStatus ?? '')
    setFinalMaturity(item.finalMaturity ?? item.maturity ?? '')
    setAdminNotes(item.adminReviewNotes ?? '')
    setSendBackReason('')
    setAssignUserId(item.assignedToUserId ?? '')
    setAssignDue(item.dueDate ?? '')
    setAssignAllQuestions(item.questionAssignmentMode !== 'SELECTED')
    setAssignFwQuestionIds(item.assignedFrameworkQuestionIds ?? [])
    setAssignCustomQuestionIds(item.assignedCustomQuestionIds ?? [])
  }

  const openEdit = (item: GapItem) => {
    if (
      item.reviewStatus === 'SENT_BACK' ||
      item.reviewStatus === 'SUBMITTED' ||
      item.reviewStatus === 'FINALIZED'
    ) {
      setDrawerStep(3)
    } else if (canAdmin && item.applicability === 'APPLICABLE' && !item.assignedToUserId) {
      setDrawerStep(2)
    } else {
      setDrawerStep(1)
    }
    syncEditForm(item)
    setSubmissions([])
    setItemEvidence([])
    setQuestionEvidence({})
    setControlQuestions([])
    setQuestionAnswers({})
    setCustomAnswers({})
    setAnswerBaseline(null)
    void api
      .listEvidenceLinked('GAP', 'GAP_ITEM', String(item.id))
      .then(setItemEvidence)
      .catch(() => setItemEvidence([]))
    if (detail) {
      void api
        .getGapItem(detail.assessment.id, item.id)
        .then((full) => {
          setEditItem((cur) => (cur?.id === full.id ? full : cur))
          const lockedProfile = resolveGapScoringProfile(
            detail.assessment.frameworkCode,
            detail.assessment.frameworkName,
          )
          setApplicability(
            lockedProfile === 'UAE_IA' || lockedProfile === 'NCEMA'
              ? 'APPLICABLE'
              : full.applicability,
          )
          setOverrideJustification(full.overrideJustification ?? '')
          setImplementationStatus(full.implementationStatus ?? '')
          setMaturityGates(normalizeGates(full.maturityGates))
          setObservations(full.observations ?? '')
          setGapDescription(full.gapDescription ?? '')
          setWhatItMeans(full.whatItMeans ?? '')
          setEvidenceGuidance(full.evidenceGuidance ?? '')
          setFinalStatus(full.finalImplementationStatus ?? full.implementationStatus ?? '')
          setFinalMaturity(full.finalMaturity ?? full.maturity ?? '')
          setAdminNotes(full.adminReviewNotes ?? '')
          setAssignUserId(full.assignedToUserId ?? '')
          setAssignDue(full.dueDate ?? '')
          setAssignAllQuestions(full.questionAssignmentMode !== 'SELECTED')
          setAssignFwQuestionIds(full.assignedFrameworkQuestionIds ?? [])
          setAssignCustomQuestionIds(full.assignedCustomQuestionIds ?? [])
        })
        .catch((e) => onError(String(e)))
      void api
        .listGapItemSubmissions(detail.assessment.id, item.id)
        .then(setSubmissions)
        .catch(() => setSubmissions([]))
      void Promise.all([
        api.listGapItemQuestions(detail.assessment.id, item.id),
        api.listGapItemAnswers(detail.assessment.id, item.id),
        api.listGapCustomQuestions(detail.assessment.id, item.id),
      ])
        .then(([qs, ans, custom]) => {
          setControlQuestions(qs)
          setCustomQuestions(custom)
          if (item.questionAssignmentMode !== 'SELECTED') {
            setAssignFwQuestionIds(qs.map((q) => q.id))
            setAssignCustomQuestionIds(custom.map((q) => q.id))
          }
          const map: Record<number, { answer: string; note: string }> = {}
          for (const q of qs) {
            map[q.id] = { answer: '', note: '' }
          }
          const customMap: Record<number, { answer: string; note: string }> = {}
          for (const a of ans) {
            if (a.questionId != null) {
              map[a.questionId] = { answer: a.answer ?? '', note: a.note ?? '' }
            } else if (a.customQuestionId != null) {
              customMap[a.customQuestionId] = { answer: a.answer ?? '', note: a.note ?? '' }
            }
          }
          setQuestionAnswers(map)
          setCustomAnswers(customMap)
          setAnswerBaseline(JSON.stringify(map))
          const keys = [
            ...qs.map((q) => `c:${q.id}`),
            ...custom.map((q) => `u:${q.id}`),
          ]
          void loadGapQuestionEvidence(item.id, keys).then(setQuestionEvidence)
        })
        .catch(() => {
          setControlQuestions([])
          setCustomQuestions([])
          setQuestionAnswers({})
          setCustomAnswers({})
          setAnswerBaseline(JSON.stringify({}))
          setQuestionEvidence({})
        })
    }
  }

  const openEditRef = useRef(openEdit)
  openEditRef.current = openEdit
  const handleItemClick = useCallback((item: GapItem) => {
    openEditRef.current(item)
  }, [])
  const toggleDomain = useCallback((key: string) => {
    setOpenDomains((prev) => ({ ...prev, [key]: !prev[key] }))
  }, [])

  const applyGapWrite = async (item: GapItem) => {
    if (!detail) return
    const header = await api.getGapAssessmentSummary(detail.assessment.id)
    setDetail({
      assessment: header,
      items: detail.items.map((i) => (i.id === item.id ? { ...i, ...item } : i)),
    })
    setRows((prev) => prev.map((r) => (r.id === header.id ? header : r)))
  }

  const assignedReview =
    !!editItem?.assignedToUserId &&
    (editItem.reviewStatus === 'SUBMITTED' ||
      editItem.reviewStatus === 'FINALIZED' ||
      editItem.reviewStatus === 'SENT_BACK')

  const saveItem = async (quiet = false): Promise<boolean> => {
    if (!detail || !editItem) return false
    const applicabilityLocked = scoringProfile === 'UAE_IA' || scoringProfile === 'NCEMA'
    const applicabilityChanged = !applicabilityLocked && applicability !== editItem.applicability
    if (applicabilityChanged && !overrideJustification.trim()) {
      onError('Override justification required when changing applicability')
      return false
    }
    if (canAdmin && assignUserId !== '' && assignDueError) {
      onError('Fix the assignment due date')
      return false
    }
    if (!quiet) {
      setBusy(true)
      setDrawerWorking({
        title: 'Saving this control',
        message: 'Stay on this page.',
      })
    }
    let latest: GapItem | null = null
    try {
      // Applicability first. Assign rejects N/A, so N/A → Applicable + Contributor
      // must persist Applicable before the assign call.
      if (assignedReview && canAdmin) {
        latest = await api.updateGapItem(detail.assessment.id, editItem.id, {
          applicability,
          overrideJustification: applicabilityChanged ? overrideJustification.trim() : undefined,
        })
      } else {
        latest = await api.updateGapItem(detail.assessment.id, editItem.id, {
          applicability,
          overrideJustification: applicabilityChanged ? overrideJustification.trim() : undefined,
          ...(isSamaFramework(detail.assessment.frameworkCode, detail.assessment.frameworkName)
            ? {
                maturityGates:
                  applicability === 'NOT_APPLICABLE' ? null : maturityGates,
              }
            : {
                implementationStatus:
                  applicability === 'NOT_APPLICABLE'
                    ? null
                    : implementationStatus === ''
                      ? undefined
                      : implementationStatus,
              }),
          observations,
          gapDescription,
          whatItMeans,
          evidenceGuidance,
        })
        const answersDirty =
          answerBaseline != null && JSON.stringify(questionAnswers) !== answerBaseline
        if (controlQuestions.length > 0 && canEdit && answersDirty) {
          await api.upsertGapItemAnswers(
            detail.assessment.id,
            editItem.id,
            controlQuestions.map((q) => ({
              questionId: q.id,
              answer: questionAnswers[q.id]?.answer || null,
              note: questionAnswers[q.id]?.note || null,
            })),
          )
        }
      }
      if (canAdmin && assignDirty && assignUserId !== '' && applicability === 'APPLICABLE') {
        latest = await api.assignGapItem(detail.assessment.id, editItem.id, {
          userId: Number(assignUserId),
          dueDate: assignDue || null,
          assignAllQuestions,
          frameworkQuestionIds: assignAllQuestions ? null : assignFwQuestionIds,
          customQuestionIds: assignAllQuestions ? null : assignCustomQuestionIds,
          release: false,
        })
      }
      if (latest) await applyGapWrite(latest)
      const next = pendingGapItem.current
      pendingGapItem.current = null
      if (next) {
        openEdit(next)
      } else if (latest) {
        syncEditForm(latest)
      } else {
        setEditItem(null)
      }
      const held =
        latest != null &&
        latest.applicability === 'APPLICABLE' &&
        latest.releasedToContributor === false &&
        latest.reviewStatus === 'OPEN'
      if (!quiet) {
        onOk(
          held
            ? 'Saved as Draft. Use Send To Contributor when you are ready to notify the contributor.'
            : 'Control updated',
        )
      }
      return true
    } catch (e) {
      if (latest) {
        try {
          await applyGapWrite(latest)
        } catch {
          /* keep the Save error */
        }
      }
      onError(String(e))
      return false
    } finally {
      if (!quiet) {
        setDrawerWorking(null)
        setBusy(false)
      }
    }
  }

  const finalizeItem = async () => {
    if (!detail || !editItem) return
    const sama = isSamaFramework(detail.assessment.frameworkCode, detail.assessment.frameworkName)
    if (sama && finalMaturity === '') return
    if (!sama && finalStatus === '') return
    setBusy(true)
    setDrawerWorking({
      title: sama ? 'Setting final maturity' : 'Setting the final status',
      message: 'Stay on this page.',
    })
    try {
      const latest = await api.finalizeGapItem(detail.assessment.id, editItem.id, {
        ...(sama
          ? { finalMaturity: finalMaturity as number }
          : { finalImplementationStatus: finalStatus as GapImplementationStatus }),
        adminReviewNotes: adminNotes.trim() || null,
      })
      await applyGapWrite(latest)
      setEditItem(null)
      onOk(sama ? 'Final maturity set' : 'Final status set')
    } catch (e) {
      onError(String(e))
    } finally {
      setDrawerWorking(null)
      setBusy(false)
    }
  }

  const sendBackItem = async () => {
    if (!detail || !editItem || !sendBackReason.trim()) {
      onError('Send-back reason is required')
      return
    }
    const sama = isSamaFramework(detail.assessment.frameworkCode, detail.assessment.frameworkName)
    if (!sama && finalStatus === '') {
      onError('Set implementation status when sending back')
      return
    }
    setBusy(true)
    setDrawerWorking({
      title: 'Sending back to the Contributor',
      message: 'Returning this control. Stay on this page.',
    })
    try {
      const latest = await api.sendBackGapItem(detail.assessment.id, editItem.id, {
        reason: sendBackReason.trim(),
        ...(!sama && finalStatus !== '' ? { implementationStatus: finalStatus } : {}),
      })
      await applyGapWrite(latest)
      setEditItem(null)
      onOk('Control sent back to Contributor')
    } catch (e) {
      onError(String(e))
    } finally {
      setDrawerWorking(null)
      setBusy(false)
    }
  }

  const complete = async () => {
    if (!detail) return
    setBusy(true)
    try {
      const d = await api.completeGapAssessment(detail.assessment.id)
      setDetail(d)
      await load()
      onOk('Gap assessment completed')
      const overrides = d.items.filter((i) => i.applicabilityOverride)
      if (overrides.length > 0 && d.assessment.soaId != null) {
        setSoaPromptOverrides(overrides)
        setSoaPromptSoaId(d.assessment.soaId)
        setSoaPromptOpen(true)
      }
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const reopen = async () => {
    if (!detail) return
    setBusy(true)
    try {
      const d = await api.reopenGapAssessment(detail.assessment.id)
      setDetail(d)
      await load()
      onOk('Assessment reopened')
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const editable = detail && detail.assessment.status !== 'COMPLETED'
  const adminCanRemoveEvidence =
    canAdmin &&
    Boolean(editable) &&
    editItem != null &&
    editItem.reviewStatus !== 'SUBMITTED' &&
    editItem.reviewStatus !== 'FINALIZED'

  const refreshItemEvidence = (itemId: number) => {
    void api
      .listEvidenceLinked('GAP', 'GAP_ITEM', String(itemId))
      .then(setItemEvidence)
      .catch(() => setItemEvidence([]))
  }

  const removeAdminGapEvidence = async (
    ev: EvidenceSummary,
    recordType: 'GAP_ITEM' | 'GAP_QUESTION',
    recordId: string,
  ) => {
    if (!editItem) return
    setBusy(true)
    try {
      await api.unlinkEvidenceFromRecord(ev.id, { moduleCode: 'GAP', recordType, recordId })
      if (recordType === 'GAP_ITEM') {
        refreshItemEvidence(editItem.id)
      } else {
        const keys = [
          ...controlQuestions.map((q) => `c:${q.id}`),
          ...customQuestions.map((q) => `u:${q.id}`),
        ]
        void loadGapQuestionEvidence(editItem.id, keys).then(setQuestionEvidence)
      }
      onOk('Evidence removed')
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }
  const shellLifted = Boolean(embedded && onPageActionsChange)

  const exportReport = useCallback(() => {
    if (!detail) return
    void api.downloadGapExport(detail.assessment.id).catch((e) => onError(String(e)))
  }, [detail, onError])

  const openCreate = useCallback((nextFrameworkId?: number) => {
    setAssessmentDepth(4)
    if (nextFrameworkId != null) setFrameworkId(nextFrameworkId)
    setCreateOpen(true)
  }, [])

  const existingGapFw = new Set(rows.map((r) => r.frameworkId))
  const createOptions = projectFrameworks
  const creatingFramework = frameworkId === '' ? null : createOptions.find((f) => f.id === frameworkId)

  useEffect(() => {
    if (openedInitial.current || initialGapId == null || !listReady) return
    openedInitial.current = true
    void openDetail(initialGapId)
  }, [initialGapId, listReady, openDetail])

  const openedNotifyItem = useRef<number | null>(null)
  useEffect(() => {
    if (!detail || initialItemId == null) return
    if (openedNotifyItem.current === initialItemId) return
    const item = detail.items.find((i) => i.id === initialItemId)
    if (!item) return
    openedNotifyItem.current = initialItemId
    openEdit(item)
  }, [detail, initialItemId])

  useEffect(() => {
    if (startedCreate.current || !startCreate) return
    startedCreate.current = true
    openCreate()
  }, [startCreate, openCreate])

  useEffect(() => {
    if (!onHeaderMetaChange || !embedded) return undefined
    if (!detail) {
      onHeaderMetaChange({
        title: 'Gap Assessment',
        breadcrumbTrail: projectLabel ?? '',
      })
    } else {
      const fw = frameworkDisplayName(detail.assessment.frameworkName, detail.assessment.frameworkCode)
      const cycle = projectLabel ?? detail.assessment.name
      onHeaderMetaChange({
        title: 'Gap Assessment',
        breadcrumbTrail: `${fw} · ${cycle}`,
      })
    }
    return () => onHeaderMetaChange(null)
  }, [onHeaderMetaChange, embedded, detail, projectLabel])

  useEffect(() => {
    if (!onPageActionsChange || !embedded) return undefined
    const actions: ReactNode[] = []
    if (detail || recordOnly) {
      actions.push(
        <Button key="back" variant="outlined" size="small" disabled={busy} onClick={closeDetail}>
          Back
        </Button>,
      )
    }
    if (canAdmin && !recordOnly && createOptions.length > 0) {
      actions.push(
        <Button
          key="new"
          variant="outlined"
          size="small"
          disabled={busy}
          onClick={() => openCreate()}
        >
          New Gap Assessment
        </Button>,
      )
    }
    if (detail) {
      if (
        canExport &&
        (detail.assessment.status === 'IN_PROGRESS' || detail.assessment.status === 'COMPLETED')
      ) {
        actions.push(
          <Button
            key="export"
            variant="outlined"
            color="primary"
            size="small"
            disabled={busy}
            onClick={exportReport}
          >
            Export Report
          </Button>,
        )
      }
      if (canAdmin && editable) {
        actions.push(
          <BusyButton
            key="complete"
            variant="contained"
            size="small"
            busy={busy}
            disabled={Boolean(completeBlocked)}
            disabledReason={completeBlocked || undefined}
            onClick={() => void complete()}
          >
            Complete Assessment
          </BusyButton>,
        )
      }
      if (canAdmin && detail.assessment.status === 'COMPLETED') {
        actions.push(
          <Button
            key="reopen"
            variant="outlined"
            size="small"
            disabled={busy}
            onClick={() => void reopen()}
          >
            Reopen Assessment
          </Button>,
        )
      }
    }
    onPageActionsChange(actions.length > 0 ? <>{actions}</> : null)
    return () => onPageActionsChange(null)
  }, [
    onPageActionsChange,
    embedded,
    canAdmin,
    canExport,
    createOptions.length,
    recordOnly,
    detail,
    editable,
    busy,
    closeDetail,
    openCreate,
    exportReport,
    complete,
    reopen,
  ])

  const sameIdList = (a: number[], b: number[]) => {
    if (a.length !== b.length) return false
    const sa = [...a].sort((x, y) => x - y)
    const sb = [...b].sort((x, y) => x - y)
    return sa.every((v, i) => v === sb[i])
  }
  const baselineAllQuestions = editItem?.questionAssignmentMode !== 'SELECTED'
  const baselineFwIds = editItem?.assignedFrameworkQuestionIds ?? []
  const baselineCustomIds = editItem?.assignedCustomQuestionIds ?? []
  const assignDirty =
    !!editItem &&
    (assignUserId !== (editItem.assignedToUserId ?? '') ||
      assignDue !== (editItem.dueDate ?? '') ||
      assignAllQuestions !== baselineAllQuestions ||
      (!assignAllQuestions &&
        (!sameIdList(assignFwQuestionIds, baselineFwIds) ||
          !sameIdList(assignCustomQuestionIds, baselineCustomIds))) ||
      (assignAllQuestions && !baselineAllQuestions))

  const creatingFrameworkName = creatingFramework
    ? frameworkLabel(creatingFramework)
    : 'the selected framework'
  const visibleRows = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return rows
    return rows.filter(
      (r) =>
        r.name.toLowerCase().includes(q) ||
        r.frameworkCode.toLowerCase().includes(q) ||
        r.frameworkName.toLowerCase().includes(q),
    )
  }, [rows, search])
  const gapListPaged = usePagedRows(visibleRows)

  const completeBlocked = useMemo(() => {
    if (!detail) return ''
    const applicable = detail.items.filter((i) => i.applicability === 'APPLICABLE')
    const sama = isSamaFramework(detail.assessment.frameworkCode, detail.assessment.frameworkName)
    const noFinal = applicable.filter((i) =>
      sama ? i.finalMaturity == null : !i.finalImplementationStatus,
    )
    if (noFinal.length > 0) {
      return sama
        ? `Finalize maturity on all applicable controls (${noFinal.length} remaining)`
        : `Finalize implementation status on all applicable controls (${noFinal.length} remaining)`
    }
    const pending = applicable.filter((i) => i.reviewStatus === 'SUBMITTED')
    if (pending.length > 0) {
      return `Resolve submitted controls first (${pending.length} still submitted)`
    }
    return ''
  }, [detail])

  const drawerDirty =
    !!editItem &&
    (assignDirty ||
      applicability !== editItem.applicability ||
      overrideJustification !== (editItem.overrideJustification ?? '') ||
      implementationStatus !== (editItem.implementationStatus ?? '') ||
      JSON.stringify(maturityGates) !== JSON.stringify(normalizeGates(editItem.maturityGates)) ||
      observations !== (editItem.observations ?? '') ||
      gapDescription !== (editItem.gapDescription ?? '') ||
      whatItMeans !== (editItem.whatItMeans ?? '') ||
      evidenceGuidance !== (editItem.evidenceGuidance ?? '') ||
      sendBackReason.trim() !== '' ||
      adminNotes !== (editItem.adminReviewNotes ?? '') ||
      (detail && isSamaFramework(detail.assessment.frameworkCode, detail.assessment.frameworkName)
        ? finalMaturity !== (editItem.finalMaturity ?? editItem.maturity ?? '')
        : finalStatus !== (editItem.finalImplementationStatus ?? editItem.implementationStatus ?? '')) ||
      (answerBaseline != null && JSON.stringify(questionAnswers) !== answerBaseline))

  const gapLeaveNav = useOptionalUnsavedChangesGuardApi()
  useRegisterUnsavedChanges(
    editItem && drawerDirty
      ? {
          dirty: true,
          title: 'Leave without saving this control?',
          message: 'You have unsaved Gap control edits. Save to keep them, or discard to leave.',
          saveDraftLabel:
            editItem && !assignedReview && !gapControlReleased(editItem) ? 'Save Draft' : 'Save',
          onSaveDraft: async () => {
            const ok = await saveItem()
            if (!ok) throw new Error('not saved')
          },
        }
      : null,
  )
  useBeforeUnload(Boolean(editItem) && drawerDirty)

  const requestCloseDrawer = () => {
    if (drawerWorking) return
    pendingGapItem.current = null
    if (!drawerDirty) {
      setEditItem(null)
      return
    }
    if (gapLeaveNav) {
      gapLeaveNav.requestNavigation(() => setEditItem(null))
      return
    }
    setDiscardDrawer(true)
  }

  const requestOpenItem = (item: GapItem) => {
    if (drawerWorking) return
    if (editItem && drawerDirty && editItem.id !== item.id) {
      pendingGapItem.current = item
      if (gapLeaveNav) {
        gapLeaveNav.requestNavigation(() => {
          pendingGapItem.current = null
          openEdit(item)
        })
        return
      }
      setDiscardDrawer(true)
      return
    }
    openEdit(item)
  }

  const samaAssessment = useMemo(
    () =>
      detail
        ? isSamaFramework(detail.assessment.frameworkCode, detail.assessment.frameworkName)
        : false,
    [detail],
  )

  const selectedItems = (detail?.items ?? []).filter((item) => selectedIds.includes(item.id))
  const releaseBlock = selectedItems.map((item) => gapReleaseBlockReason(item, samaAssessment)).find(Boolean) ?? null
  const drawerSendBlock =
    !editItem || gapControlReleased(editItem) || applicability !== 'APPLICABLE'
      ? null
      : assignUserId === ''
        ? 'Select a contributor'
        : !assignDue
          ? 'Due date is required'
          : assignDueError
            ? assignDueError
            : samaAssessment
              ? maturityFromGates(maturityGates) == null
                ? 'Select a classification'
                : !observations.trim()
                  ? 'Gap description is required'
                  : null
              : implementationStatus === ''
                ? 'Select a classification'
                : !observations.trim()
                  ? 'Gap description is required'
                  : null
  const sendBackBlock =
    selectedItems.map((item) => gapSendBackBlockReason(item, samaAssessment)).find(Boolean) ?? null

  const toggleSelected = (id: number) => {
    setSelectedIds((prev) => (prev.includes(id) ? prev.filter((row) => row !== id) : [...prev, id]))
  }

  const setSelectedMany = (ids: number[], selected: boolean) => {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      for (const id of ids) {
        if (selected) next.add(id)
        else next.delete(id)
      }
      return [...next]
    })
  }

  const applyMany = async (rows: GapItem[]) => {
    for (const row of rows) {
      await applyGapWrite(row)
    }
  }

  const sendSelected = async () => {
    if (!detail || selectedItems.length === 0 || releaseBlock) return
    setListAction('send')
    try {
      const rows = await api.sendGapItems(
        detail.assessment.id,
        selectedItems.map((item) => item.id),
      )
      await applyMany(rows)
      setSelectedIds([])
      setSendConfirmOpen(false)
      onOk(
        rows.length === 1
          ? 'Sent. The contributor has one email for this control.'
          : `Sent ${rows.length} controls. Each contributor has one email listing their controls.`,
      )
    } catch (e) {
      onError(String(e))
    } finally {
      setListAction(null)
    }
  }

  const sendOpenControl = async () => {
    if (!detail || !editItem || drawerSendBlock) return
    setDrawerWorking({
      title: 'Sending this control',
      message: 'The contributor is notified when this finishes.',
    })
    try {
      if (drawerDirty) {
        const saved = await saveItem(true)
        if (!saved) return
      }
      const rows = await api.sendGapItems(detail.assessment.id, [editItem.id])
      const latest = rows[0]
      if (latest) {
        await applyGapWrite(latest)
        openEdit(latest)
      }
      setSelectedIds((prev) => prev.filter((id) => id !== editItem.id))
      setDrawerSendOpen(false)
      onOk('Sent. The contributor has one email for this control.')
    } catch (e) {
      onError(String(e))
    } finally {
      setDrawerWorking(null)
    }
  }

  const sendBackSelected = async () => {
    if (!detail || selectedItems.length === 0 || sendBackBlock || !bulkSendBackReason.trim()) return
    setListAction('send-back')
    try {
      const rows = await api.sendBackGapItems(
        detail.assessment.id,
        selectedItems.map((item) => item.id),
        bulkSendBackReason.trim(),
      )
      await applyMany(rows)
      setSelectedIds([])
      setBulkSendBackReason('')
      setSendBackOpen(false)
      onOk(
        rows.length === 1
          ? 'Sent back. The contributor has one email for this control.'
          : `Sent back ${rows.length} controls. Each contributor has one email listing their controls.`,
      )
    } catch (e) {
      onError(String(e))
    } finally {
      setListAction(null)
    }
  }

  const toggleFlag = async () => {
    if (!detail || !editItem || flagBusy) return
    const next = !editItem.flaggedForLater
    setFlagBusy(true)
    try {
      const latest = await api.setGapItemFlag(detail.assessment.id, editItem.id, next)
      await applyGapWrite(latest)
      setEditItem((cur) =>
        cur?.id === latest.id ? { ...cur, flaggedForLater: latest.flaggedForLater } : cur,
      )
      onOk(next ? 'Flagged for Follow Up' : 'Flag cleared')
    } catch (e) {
      onError(String(e))
    } finally {
      setFlagBusy(false)
    }
  }

  const sendBackIntent = sendBackReason.trim() !== ''
  const itemFinalized = editItem?.reviewStatus === 'FINALIZED'
  const questionNotAssigned = (kind: 'c' | 'u', id: number) => {
    if (!editItem?.assignedToUserId || editItem.questionAssignmentMode !== 'SELECTED') return false
    const ids =
      kind === 'c' ? editItem.assignedFrameworkQuestionIds : editItem.assignedCustomQuestionIds
    return !(ids ?? []).includes(id)
  }
  const assessmentLocked = detail?.assessment.status === 'COMPLETED'
  const finalizedSaveAllowed =
    !!editItem &&
    (assignDirty ||
      applicability !== editItem.applicability ||
      overrideJustification !== (editItem.overrideJustification ?? ''))
  const sendBackBlockedReason = assessmentLocked
    ? 'Assessment is complete'
    : !sendBackReason.trim()
      ? 'Enter a send-back reason'
      : !samaAssessment && finalStatus === ''
        ? 'Set implementation status to send back'
        : undefined
  const sendBackEnableHint = assessmentLocked
    ? 'Send back stays disabled while the assessment is complete.'
    : samaAssessment
      ? sendBackBlockedReason
        ? 'Send back enables when a reason is set.'
        : 'Send back is enabled. Use it below. Save and Set final maturity stay disabled.'
      : sendBackBlockedReason
        ? 'Send back enables when implementation status and a reason are set.'
        : 'Send back is enabled. Use it below. Save and Set final status stay disabled.'
  const finalizeBlockedReason = assessmentLocked
    ? 'Assessment is complete'
    : itemFinalized
      ? samaAssessment
        ? 'Final maturity is already set'
        : 'Final status is already set'
      : sendBackIntent
        ? 'Clear send-back reason to set a final status'
        : samaAssessment
          ? finalMaturity === ''
            ? 'Select a final maturity'
            : undefined
          : finalStatus === ''
            ? 'Select a final implementation status'
            : detail
                && resolveGapScoringProfile(
                  detail.assessment.frameworkCode,
                  detail.assessment.frameworkName,
                ) === 'UAE_IA'
                && finalStatus !== 'FULLY_IMPLEMENTED'
              ? 'Final status can only be set when the control is Fully Implemented'
              : undefined
  const saveBlockedReason = assessmentLocked
    ? 'Assessment is complete'
    : sendBackIntent
      ? 'Use Send back to return this control'
      : itemFinalized && !finalizedSaveAllowed
        ? 'Control is finalized'
        : canAdmin && assignUserId !== '' && assignDueError
          ? assignDueError
          : undefined

  const scoringProfile = useMemo(
    () =>
      detail
        ? resolveGapScoringProfile(detail.assessment.frameworkCode, detail.assessment.frameworkName)
        : 'ISO_PACK',
    [detail],
  )

  const filteredItems = useMemo(() => {
    if (!detail) return []
    return detail.items.filter((item) => {
      if (itemFilter === 'UNASSIGNED') {
        if (!(item.applicability === 'APPLICABLE' && !item.assignedToUserId)) return false
      } else if (itemFilter === 'OPEN') {
        if (item.reviewStatus !== 'OPEN') return false
      } else if (itemFilter === 'HAS_GAP') {
        if (!itemHasGap(item, samaAssessment)) return false
      } else if (itemFilter === 'COMPLIANT') {
        if (gapStatusFromItem(item) !== 'compliant') return false
      } else if (itemFilter === 'SENT_BACK') {
        if (item.reviewStatus !== 'SENT_BACK') return false
      } else if (itemFilter === 'FLAGGED') {
        if (!item.flaggedForLater || item.reviewStatus === 'FINALIZED') return false
      }
      const q = controlQuery.trim().toLowerCase()
      if (q) {
        const hay = `${item.nodeCode} ${item.title}`.toLowerCase()
        if (!hay.includes(q)) return false
      }
      if (scoringProfile === 'ISO_PACK' && sectionTab !== 'ALL') {
        const { key } = deriveGapDomain(item.nodeCode, scoringProfile)
        if (gapDomainSectionTab(key) !== sectionTab) return false
      }
      return true
    })
  }, [detail, itemFilter, samaAssessment, scoringProfile, sectionTab, controlQuery])

  const sectionCounts = useMemo(
    () => gapSectionTabCounts(detail?.items ?? [], scoringProfile),
    [detail, scoringProfile],
  )

  const filterCounts = useMemo(() => {
    if (!detail) return { all: 0, unassigned: 0, open: 0, hasGap: 0, compliant: 0, sentBack: 0, flagged: 0 }
    const items = detail.items
    return {
      all: items.length,
      unassigned: items.filter(
        (i) => i.applicability === 'APPLICABLE' && !i.assignedToUserId,
      ).length,
      open: items.filter((i) => i.reviewStatus === 'OPEN').length,
      hasGap: items.filter((i) => itemHasGap(i, samaAssessment)).length,
      compliant: items.filter((i) => gapStatusFromItem(i) === 'compliant').length,
      sentBack: items.filter((i) => i.reviewStatus === 'SENT_BACK').length,
      flagged: items.filter((i) => i.flaggedForLater && i.reviewStatus !== 'FINALIZED').length,
    }
  }, [detail, samaAssessment])

  const progressStats = useMemo(
    () => computeGapProgressStats(detail?.items ?? [], detail?.assessment.overallScore),
    [detail],
  )

  const gapNavItems = useMemo(() => {
    if (editItem && filteredItems.some((i) => i.id === editItem.id)) return filteredItems
    return detail?.items ?? []
  }, [editItem, filteredItems, detail])
  const gapNavIndex = editItem ? gapNavItems.findIndex((i) => i.id === editItem.id) : -1

  const filterPillOptions = useMemo(
    () => [
      { value: 'ALL' as const, label: 'All', count: filterCounts.all },
      { value: 'UNASSIGNED' as const, label: 'Unassigned', count: filterCounts.unassigned },
      { value: 'OPEN' as const, label: 'Open', count: filterCounts.open },
      { value: 'HAS_GAP' as const, label: 'Has gap', count: filterCounts.hasGap },
      { value: 'COMPLIANT' as const, label: 'Compliant', count: filterCounts.compliant },
      { value: 'SENT_BACK' as const, label: 'Sent Back', count: filterCounts.sentBack },
      { value: 'FLAGGED' as const, label: 'Flagged', count: filterCounts.flagged },
    ],
    [filterCounts],
  )

  useEffect(() => {
    setLayerScoresOpen(false)
    setSelectedIds([])
    setSendConfirmOpen(false)
    setSendBackOpen(false)
  }, [detail?.assessment.id])

  useEffect(() => {
    if (!detail) return
    const groups = groupItemsByDomain(filteredItems, scoringProfile)
    setOpenDomains((prev) => {
      const next: Record<string, boolean> = {}
      groups.forEach((g) => {
        next[g.key] = prev[g.key] ?? false
      })
      return next
    })
  }, [detail?.assessment.id, filteredItems, scoringProfile])

  useEffect(() => {
    if (!editItem) return
    const t = window.setTimeout(() => scrollDrawerStep(drawerStep), 80)
    return () => window.clearTimeout(t)
    // Scroll to the section chosen when this control opened, not on every tab click.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editItem?.id])

  const bindDrawerScroll = useCallback((node: HTMLDivElement | null) => {
    drawerScrollCleanup.current?.()
    drawerScrollCleanup.current = null
    if (!node) return
    const stepOf = (el: HTMLElement) => Number(el.id.replace('gap-drawer-step-', ''))
    const syncStep = () => {
      const nodes = [...node.querySelectorAll<HTMLElement>('[id^="gap-drawer-step-"]')]
      if (nodes.length === 0) return
      let scroller: HTMLElement = node
      let walk: HTMLElement | null = node
      while (walk) {
        const oy = getComputedStyle(walk).overflowY
        if ((oy === 'auto' || oy === 'scroll' || oy === 'overlay') && walk.scrollHeight > walk.clientHeight + 2) {
          scroller = walk
          break
        }
        walk = walk.parentElement
      }
      const rootTop = scroller.getBoundingClientRect().top
      const atBottom = scroller.scrollTop + scroller.clientHeight >= scroller.scrollHeight - 12
      let current = stepOf(nodes[0])
      if (atBottom && scroller.scrollTop > 8) {
        current = stepOf(nodes[nodes.length - 1])
      } else {
        for (const el of nodes) {
          if (el.getBoundingClientRect().top - rootTop <= 72) current = stepOf(el)
        }
      }
      if (!Number.isFinite(current)) return
      setDrawerStep((prev) => (prev === current ? prev : current))
    }
    const targets: HTMLElement[] = []
    let el: HTMLElement | null = node
    while (el && el !== document.body) {
      targets.push(el)
      el = el.parentElement
    }
    targets.forEach((target) => target.addEventListener('scroll', syncStep, { passive: true }))
    const observer = new ResizeObserver(() => syncStep())
    observer.observe(node)
    const kick = window.setTimeout(syncStep, 120)
    drawerScrollCleanup.current = () => {
      window.clearTimeout(kick)
      targets.forEach((target) => target.removeEventListener('scroll', syncStep))
      observer.disconnect()
    }
  }, [])

  const toolbar = (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      spacing={1}
      alignItems={{ sm: 'center' }}
      sx={
        embedded
          ? {
              px: 2,
              pt: 1,
              pb: 0.5,
              borderTop: `1px solid ${brand.hairline}`,
              flexShrink: 0,
            }
          : undefined
      }
    >
      <Box sx={{ flex: 1, minWidth: 0 }}>
        <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
          Gap Assessment
        </Typography>
        <Typography variant="caption" color="text.secondary" display="block">
          Score applicable controls · {soaApplicable ? 'SoA optional' : 'SoA not applicable'} · assign
          Contributors to review submissions
        </Typography>
      </Box>
      {canAdmin && !recordOnly && (
        <Button
          size="small"
          variant="contained"
          disabled={busy || createOptions.length === 0}
          onClick={() => openCreate()}
        >
          New Gap Assessment
        </Button>
      )}
    </Stack>
  )

  return (
    <Stack spacing={embedded ? 0 : 1} sx={{ flex: 1, minHeight: 0, height: embedded ? '100%' : undefined }}>
      {!shellLifted && (
        <Box sx={{ px: embedded ? 0 : 0.5, py: 0.5, flexShrink: 0 }}>{toolbar}</Box>
      )}

      <Box sx={{ flex: 1, minHeight: 0, px: embedded ? 0.75 : 0, pb: embedded ? 0.75 : 0, pt: embedded ? 0.5 : 0, display: 'flex' }}>
        <Card sx={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0, minWidth: 0, overflow: 'hidden' }}>
          {!detail ? (
            recordOnly || !listReady || openingDetail ? (
              <Box sx={{ p: 3, flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <PageLoading label="Loading Gap Assessment…" />
              </Box>
            ) : rows.length === 0 ? (
              <WorkspaceEmptyPanel
                title="No Gap Assessment Yet"
                message={
                  projectFrameworks.length === 0
                    ? 'Add a framework to this project first.'
                    : 'Create an assessment for one project framework.'
                }
                steps={
                  projectFrameworks.length > 0
                    ? [
                        { title: 'Create', detail: 'Use New Gap Assessment in the header. Pick a framework and name it.' },
                        { title: 'Score', detail: 'Open each control. Assign a Contributor when you need evidence.' },
                        { title: 'Complete', detail: 'Finalize remaining controls, then complete the assessment.' },
                      ]
                    : undefined
                }
              />
            ) : (
              <>
                <RegisterToolbar
                  search={search}
                  searchPlaceholder="Search assessments…"
                  onSearchChange={setSearch}
                  onClearFilters={search ? () => setSearch('') : undefined}
                />
                <RegisterListFrame
                  ready={listReady}
                  count={gapListPaged.count}
                  emptyMessage="No assessments match this search."
                  page={gapListPaged.page}
                  rowsPerPage={gapListPaged.rowsPerPage}
                  onPageChange={gapListPaged.setPage}
                  onRowsPerPageChange={gapListPaged.setRowsPerPage}
                >
                  <TableContainer sx={{ flex: 1 }}>
                    <Table size="small" stickyHeader sx={registerTableSx}>
                      <TableHead>
                        <TableRow>
                          <TableCell sx={{ ...primaryTableCellSx, width: '32%', pl: 1.5 }}>Assessment</TableCell>
                          <TableCell sx={{ ...compactTableCellSx, width: '18%' }}>Framework</TableCell>
                          <TableCell sx={{ ...compactTableCellSx, width: '14%' }}>Type</TableCell>
                          <TableCell sx={{ ...compactTableCellSx, width: '16%' }}>Status</TableCell>
                          <TableCell sx={{ ...compactTableCellSx, ...lastTableCellSx, width: '20%' }} align="right">
                            Score
                          </TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {gapListPaged.paged.map((r) => (
                          <TableRow
                            key={r.id}
                            hover
                            sx={{ cursor: 'pointer' }}
                            onClick={() => void openDetail(r.id)}
                          >
                            <TableCell sx={{ ...primaryTableCellSx, width: '32%', pl: 1.5 }}>
                              <Box sx={primaryRegisterNameSx}>{r.name}</Box>
                            </TableCell>
                            <TableCell sx={{ ...compactTableCellSx, width: '18%' }}>
                              {frameworkDisplayName(r.frameworkName, r.frameworkCode)}
                            </TableCell>
                            <TableCell sx={{ ...compactTableCellSx, width: '14%' }}>
                              {prettyGapType(r.assessmentType)}
                            </TableCell>
                            <TableCell sx={{ ...compactTableCellSx, width: '16%' }}>
                              <StatusChip
                                label={prettyGapType(r.status)}
                                tone={
                                  r.status === 'COMPLETED'
                                    ? 'success'
                                    : r.status === 'IN_PROGRESS'
                                      ? 'info'
                                      : 'warning'
                                }
                              />
                            </TableCell>
                            <TableCell sx={{ ...compactTableCellSx, ...lastTableCellSx, width: '20%' }} align="right">
                              {r.overallScore != null ? `${r.overallScore}%` : `${r.scoredCount}/${r.applicableCount}`}
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </RegisterListFrame>
              </>
            )
          ) : (
            <>
              <Box sx={{ px: 1.25, pt: 1.25, pb: 1, flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', overflow: 'auto' }}>
                <Box sx={{ mb: 1.5, pb: 1.5, borderBottom: `1px solid ${brand.border}`, flexShrink: 0 }}>
                  <Box
                    sx={{
                      display: 'flex',
                      alignItems: 'flex-end',
                      gap: 1.25,
                      flexWrap: 'wrap',
                    }}
                  >
                    <Box
                      sx={{
                        flex: '1 1 520px',
                        minWidth: 0,
                        display: 'grid',
                        gridTemplateColumns: {
                          xs: '1fr',
                          sm: 'repeat(2, minmax(0, 1fr))',
                          lg: scoringProfile === 'UAE_IA'
                            ? 'repeat(7, minmax(0, 1fr))'
                            : 'repeat(6, minmax(0, 1fr))',
                        },
                        gap: 1,
                      }}
                    >
                      <ReadOnlyValue label="Assessment Name" value={detail.assessment.name ?? ''} />
                      <ReadOnlyValue label="Project" value={projectName || projectLabel || ''} />
                      <ReadOnlyValue
                        label="Framework"
                        value={frameworkDisplayName(detail.assessment.frameworkName, detail.assessment.frameworkCode)}
                      />
                      <ReadOnlyValue label="Type" value={prettyGapType(detail.assessment.assessmentType)} />
                      <ReadOnlyValue
                        label="Due"
                        value={detail.assessment.targetDate ? formatTenantDate(detail.assessment.targetDate) : ''}
                      />
                      <ReadOnlyValue
                        label="SoA"
                        value={
                          detail.assessment.soaId != null
                            ? 'Linked'
                            : soaApplicable
                              ? 'Not used'
                              : 'Not applicable'
                        }
                        onClick={
                          detail.assessment.soaId != null
                            ? () => onOpenSoa?.(detail.assessment.soaId as number)
                            : undefined
                        }
                      />
                      {scoringProfile === 'UAE_IA' ? (
                        <ReadOnlyValue label="Depth" value={String(detail.assessment.assessmentDepth ?? 4)} />
                      ) : null}
                    </Box>
                    <Stack
                      direction="row"
                      spacing={1}
                      alignItems="center"
                      flexWrap="wrap"
                      useFlexGap
                      sx={{ flex: '0 0 auto', minHeight: 32, pb: 0.25 }}
                    >
                      <StatusChip
                        label={prettyGapType(detail.assessment.status)}
                        tone={
                          detail.assessment.status === 'COMPLETED'
                            ? 'success'
                            : detail.assessment.status === 'IN_PROGRESS'
                              ? 'info'
                              : 'warning'
                        }
                      />
                      {!shellLifted && canExport &&
                        (detail.assessment.status === 'IN_PROGRESS' || detail.assessment.status === 'COMPLETED') && (
                          <Button size="small" variant="outlined" disabled={busy} onClick={exportReport}>
                            Export Report
                          </Button>
                        )}
                      {!shellLifted && canAdmin && editable && (
                        <BusyButton
                          size="small"
                          variant="contained"
                          busy={busy}
                          disabled={Boolean(completeBlocked)}
                          disabledReason={completeBlocked || undefined}
                          onClick={() => void complete()}
                        >
                          Complete Assessment
                        </BusyButton>
                      )}
                      {!shellLifted && canAdmin && detail.assessment.status === 'COMPLETED' && (
                        <Button size="small" variant="outlined" disabled={busy} onClick={() => void reopen()}>
                          Reopen Assessment
                        </Button>
                      )}
                    </Stack>
                  </Box>
                  <Box sx={{ mt: 1 }}>
                    <ReadOnlyValue label="Scope" value={detail.assessment.scope ?? ''} scroll />
                  </Box>
                  {completeBlocked && canAdmin && editable ? (
                    <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.75 }}>
                      {completeBlocked}
                    </Typography>
                  ) : null}
                  <Box sx={{ display: 'flex', alignItems: 'flex-end', gap: 3, mt: 2, flexWrap: 'wrap' }}>
                    <Box sx={{ minWidth: 220, flex: '1 1 280px' }}>
                      <Typography sx={{ fontSize: '1.25rem', fontWeight: 700, lineHeight: 1, letterSpacing: '-0.02em' }}>
                        {formatGapScorePct(progressStats.completionPct)}%
                      </Typography>
                      <Typography sx={{ fontSize: '0.75rem', color: brand.muted, mt: 0.35, mb: 0.75 }}>
                        {scoringProfile === 'UAE_IA' ? 'UAE IA score' : 'Overall completion'}
                        {progressStats.inScopeCount > 0 ? ` · ${progressStats.assessedCount} of ${progressStats.inScopeCount} assessed` : ''}
                      </Typography>
                      <LinearProgress
                        variant="determinate"
                        value={Math.min(100, Math.max(0, progressStats.completionPct))}
                        sx={{ height: 8, borderRadius: 4, bgcolor: brand.border, '& .MuiLinearProgress-bar': { bgcolor: brand.gold } }}
                      />
                    </Box>
                    <Stack direction="row" spacing={2.5} flexWrap="wrap" useFlexGap>
                      {(
                        [
                          ['Compliant', progressStats.compliant, '#166534'],
                          ['Partial', progressStats.partial, '#CA8A04'],
                          ['Planned', progressStats.planned, '#6D28D9'],
                          ['Non-compliant', progressStats.nonCompliant, '#B91C1C'],
                          ['Not assessed', progressStats.notAssessed, brand.muted],
                        ] as const
                      ).map(([label, value, color]) => (
                        <Box key={label}>
                          <Typography sx={{ fontSize: '0.9375rem', fontWeight: 700, color, lineHeight: 1 }}>{value}</Typography>
                          <Typography sx={{ fontSize: '0.6875rem', color: brand.muted, mt: 0.35 }}>{label}</Typography>
                        </Box>
                      ))}
                    </Stack>
                  </Box>
                </Box>

                <Box sx={{ flex: '1 1 auto', minHeight: 320, display: 'flex', flexDirection: 'column' }}>
                <RegisterToolbar
                  search={controlQuery}
                  searchPlaceholder="Search controls…"
                  onSearchChange={setControlQuery}
                  filters={[
                    {
                      id: 'view',
                      label: 'View',
                      value: itemFilter,
                      minWidth: 168,
                      options: filterPillOptions.map((o) => ({
                        value: o.value,
                        label: `${o.label} (${o.count})`,
                      })),
                      onChange: (v) => setItemFilter(v as ItemFilter),
                    },
                  ]}
                  onClearFilters={() => {
                    setItemFilter('ALL')
                    setControlQuery('')
                  }}
                  columnPicker={gapColumns.columnPicker}
                  trailing={
                    <Stack direction="row" spacing={0.75} alignItems="center" flexWrap="wrap" useFlexGap>
                      <FilterPill
                        dense
                        label="Unassigned"
                        count={filterCounts.unassigned}
                        active={itemFilter === 'UNASSIGNED'}
                        onClick={() =>
                          setItemFilter((prev) => (prev === 'UNASSIGNED' ? 'ALL' : 'UNASSIGNED'))
                        }
                      />
                      <FilterPill
                        dense
                        label="Flagged"
                        count={filterCounts.flagged}
                        active={itemFilter === 'FLAGGED'}
                        onClick={() =>
                          setItemFilter((prev) => (prev === 'FLAGGED' ? 'ALL' : 'FLAGGED'))
                        }
                      />
                      <FilterPill
                        dense
                        label="Sent Back"
                        count={filterCounts.sentBack}
                        active={itemFilter === 'SENT_BACK'}
                        onClick={() =>
                          setItemFilter((prev) => (prev === 'SENT_BACK' ? 'ALL' : 'SENT_BACK'))
                        }
                      />
                      {scoringProfile === 'ISO_PACK' ? (
                        <>
                          {(
                            [
                              { id: 'CLAUSE' as const, label: 'Clauses', count: sectionCounts.clause },
                              { id: 'ANNEX' as const, label: 'Annex', count: sectionCounts.annex },
                              ...(sectionCounts.cloud > 0
                                ? [{ id: 'CLOUD' as const, label: 'Cloud', count: sectionCounts.cloud }]
                                : []),
                            ] as { id: GapSectionTab; label: string; count: number }[]
                          ).map((t) => (
                            <Box
                              key={t.id}
                              component="button"
                              type="button"
                              onClick={() => setSectionTab(t.id)}
                              sx={{
                                border: `1px solid ${sectionTab === t.id ? brand.accent : brand.hairlineStrong}`,
                                bgcolor: sectionTab === t.id ? brand.accentMuted : 'transparent',
                                color: sectionTab === t.id ? brand.accentHover : 'text.secondary',
                                borderRadius: 1,
                                px: 1.25,
                                py: 0.45,
                                fontSize: '0.75rem',
                                fontWeight: 650,
                                cursor: 'pointer',
                                fontFamily: 'inherit',
                                whiteSpace: 'nowrap',
                              }}
                            >
                              {t.label} ({t.count})
                            </Box>
                          ))}
                        </>
                      ) : null}
                    </Stack>
                  }
                />

                {detail.assessment.layerScores && (
                  <Box
                    sx={{
                      mt: 1.25,
                      mb: 1,
                      flexShrink: 0,
                      border: `1px solid ${brand.border}`,
                      borderLeft: `3px solid ${brand.gold}`,
                      borderRadius: 0.75,
                      bgcolor: brand.paper,
                      boxShadow: brand.shadowCard,
                      overflow: 'hidden',
                    }}
                  >
                    <Box
                      component="button"
                      type="button"
                      onClick={() => setLayerScoresOpen((prev) => !prev)}
                      aria-expanded={layerScoresOpen}
                      sx={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 1,
                        width: '100%',
                        border: 0,
                        px: 1.25,
                        py: 0.75,
                        bgcolor: brand.paper,
                        cursor: 'pointer',
                        font: 'inherit',
                        textAlign: 'left',
                        '&:hover': { bgcolor: brand.hover },
                      }}
                    >
                      <AssessmentRoundedIcon sx={{ fontSize: 18, color: brand.gold, flexShrink: 0 }} />
                      <Typography
                        sx={{
                          fontSize: '0.875rem',
                          fontWeight: 650,
                          color: brand.goldDark,
                          flex: 1,
                          minWidth: 0,
                        }}
                      >
                        {layerPanelTitle(scoringProfile)}
                      </Typography>
                      <Typography
                        sx={{
                          fontSize: '0.875rem',
                          fontWeight: 650,
                          lineHeight: 1.3,
                          color: brand.goldDark,
                          flexShrink: 0,
                        }}
                      >
                        {layerScoresOpen ? 'Hide' : 'Show'}
                      </Typography>
                      <ExpandMoreRoundedIcon
                        sx={{
                          fontSize: 18,
                          color: brand.goldDark,
                          transform: layerScoresOpen ? 'rotate(0deg)' : 'rotate(-90deg)',
                          transition: 'transform .15s ease',
                        }}
                      />
                    </Box>
                    <Collapse in={layerScoresOpen} unmountOnExit>
                      <Box sx={{ px: 1.25, pb: 1.25, pt: 0.25, bgcolor: brand.paper }}>
                      {scoringProfile === 'UAE_IA' ? (
                        <Typography
                          sx={{ ...readOnlyCaptionSx, mb: 0, mt: 0.5, fontWeight: 500 }}
                        >
                          Control Status % follows the UAE IA pack. Unstarted controls count as 0
                          in these averages.
                        </Typography>
                      ) : null}
                      {scoringProfile === 'UAE_IA' ? (
                        <Stack
                          direction={{ xs: 'column', sm: 'row' }}
                          spacing={1.5}
                          sx={{ mt: 1 }}
                          alignItems="stretch"
                        >
                          <Box sx={{ flex: 1, minWidth: 0 }}>
                            <LayerScoreCard
                              title="Management"
                              layer={detail.assessment.layerScores.management}
                            />
                          </Box>
                          <Box sx={{ flex: 1, minWidth: 0 }}>
                            <LayerScoreCard
                              title="Technical"
                              layer={detail.assessment.layerScores.technical}
                            />
                          </Box>
                        </Stack>
                      ) : null}
                      <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mt: 0.75 }}>
                        {layerPanel(detail.assessment.layerScores, scoringProfile).map((row) => {
                          const tone = layerScoreTone(row.score, row.rag)
                          return (
                          <Box
                            key={row.label}
                            sx={{
                              px: 1.25,
                              py: 0.75,
                              borderRadius: 1,
                              border: `1px solid ${tone.border}`,
                              minWidth: 120,
                              bgcolor: tone.bg,
                            }}
                          >
                            <Typography variant="caption" color="text.secondary" display="block">
                              {row.label}
                            </Typography>
                            <Typography variant="body2" fontWeight={700} sx={{ color: tone.color }}>
                              {row.score != null ? `${formatGapScorePct(row.score)}%` : '-'}
                            </Typography>
                            {row.rag && (
                              <RiskLevelBadge level={uaeIaRagToBadge(row.rag)} label={row.rag} />
                            )}
                          </Box>
                          )
                        })}
                      </Stack>
                      </Box>
                    </Collapse>
                  </Box>
                )}

                {canAdmin && editable && selectedItems.length === 0 &&
                filteredItems.some((item) => gapWorkflowLabel(item) === 'Draft') ? (
                  <Stack
                    direction={{ xs: 'column', sm: 'row' }}
                    spacing={1}
                    alignItems={{ sm: 'center' }}
                    sx={{
                      mt: 1,
                      px: 1.25,
                      py: 0.75,
                      borderRadius: 1,
                      bgcolor: brand.hover,
                      border: `1px solid ${brand.border}`,
                    }}
                  >
                    <BusyButton
                      size="small"
                      variant="contained"
                      disabled
                      disabledReason="Select a Draft control"
                    >
                      Send To Contributor
                    </BusyButton>
                    <Typography variant="body2" sx={{ color: brand.muted }}>
                      Select one or more Draft controls, then choose Send To Contributor to notify the contributor.
                    </Typography>
                  </Stack>
                ) : null}
                {canAdmin && editable && selectedItems.length > 0 ? (
                  <Stack
                    direction={{ xs: 'column', sm: 'row' }}
                    spacing={1}
                    alignItems={{ sm: 'center' }}
                    sx={{ mt: 1, px: 0.5 }}
                  >
                    <Typography variant="body2" sx={{ fontWeight: 650, minWidth: 0 }}>
                      {selectedItems.length} selected
                    </Typography>
                    <BusyButton
                      size="small"
                      variant="contained"
                      busy={listAction === 'send'}
                      busyLabel="Sending…"
                      disabled={Boolean(releaseBlock) || listAction != null}
                      disabledReason={releaseBlock ?? undefined}
                      onClick={() => setSendConfirmOpen(true)}
                    >
                      Send To Contributor
                    </BusyButton>
                    {selectedItems.some((item) => item.reviewStatus === 'SUBMITTED') ? (
                      <BusyButton
                        size="small"
                        variant="outlined"
                        busy={listAction === 'send-back'}
                        busyLabel="Sending Back…"
                        disabled={Boolean(sendBackBlock) || listAction != null}
                        disabledReason={sendBackBlock ?? undefined}
                        onClick={() => {
                          setBulkSendBackReason('')
                          setSendBackOpen(true)
                        }}
                      >
                        Send Back
                      </BusyButton>
                    ) : null}
                    <Button size="small" disabled={listAction != null} onClick={() => setSelectedIds([])}>
                      Clear
                    </Button>
                    <Typography variant="caption" color="text.secondary" sx={{ flex: 1 }}>
                      {releaseBlock ??
                        'Each contributor receives one email listing the selected controls.'}
                    </Typography>
                  </Stack>
                ) : null}
                <Box sx={{ mt: 1, flex: 1, minHeight: 240, overflow: 'auto' }}>
                  <GapDomainSections
                    items={filteredItems}
                    contributors={contributors}
                    openDomains={openDomains}
                    onToggleDomain={toggleDomain}
                    onItemClick={handleItemClick}
                    clickable
                    isVisible={gapColumns.isVisible}
                    gridTemplateColumns={
                      canAdmin && editable ? `36px ${gapColumns.gridTemplateColumns}` : gapColumns.gridTemplateColumns
                    }
                    scoringProfile={scoringProfile}
                    selectable={Boolean(canAdmin && editable)}
                    selectedIds={new Set(selectedIds)}
                    onToggleSelected={toggleSelected}
                    onSetSelected={setSelectedMany}
                  />
                </Box>
                </Box>
              </Box>
            </>
          )}
        </Card>
      </Box>

      <Dialog
        open={createOpen}
        onClose={() => dismissCreate()}
        disableEscapeKeyDown={creating}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle>New Gap Assessment</DialogTitle>
        <DialogContent>
          <Stack spacing={1.5} sx={{ mt: 1 }}>
            <FormSelect
              label="Project framework"
              fullWidth
              value={frameworkId === '' ? '' : String(frameworkId)}
              onChange={(v) => setFrameworkId(v === '' ? '' : Number(v))}
              disabled={creating}
              options={createOptions.map((f) => ({
                value: String(f.id),
                label: frameworkLabel(f),
              }))}
            />
            {(() => {
              const createFw = projectFrameworks.find((f) => f.id === frameworkId)
              const createUaeIa =
                resolveGapScoringProfile(createFw?.code, createFw?.name) === 'UAE_IA'
              const lines = [
                createUaeIa
                  ? ''
                  : 'SoA is optional. If a complete SoA exists for the framework, applicability is copied; otherwise every control starts as Applicable and you set N/A on the Gap.',
                frameworkId !== '' && existingGapFw.has(frameworkId)
                  ? 'This framework already has a Gap. This creates another run.'
                  : '',
              ].filter(Boolean)
              if (lines.length === 0) return null
              return (
                <Typography variant="caption" color="text.secondary">
                  {lines.join(' ')}
                </Typography>
              )
            })()}
            <FieldActionRow>
              <Box sx={{ flex: 1, minWidth: 0 }}>
                <CompactTextField
                  label="Assessment Name"
                  fullWidth
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g. Jul 2026 self-assessment"
                  disabled={creating}
                />
              </Box>
              <FormSelect
                label="Type"
                value={assessmentType}
                onChange={(v) => setAssessmentType(v as GapAssessmentType)}
                disabled={creating}
                options={[
                  { value: 'INTERNAL', label: 'Internal' },
                  { value: 'EXTERNAL', label: 'External' },
                ]}
              />
            </FieldActionRow>
            {(() => {
              const createFw = projectFrameworks.find((f) => f.id === frameworkId)
              const createUaeIa =
                resolveGapScoringProfile(createFw?.code, createFw?.name) === 'UAE_IA'
              if (!createUaeIa) return null
              return (
                <>
                  <FormSelect
                    label="Assessment depth"
                    value={String(assessmentDepth)}
                    onChange={(v) => setAssessmentDepth(v === '3' ? 3 : 4)}
                    disabled={creating}
                    options={[
                      { value: '4', label: 'Level 4: sub-controls (default)' },
                      { value: '3', label: 'Level 3: controls only' },
                    ]}
                  />
                  <Typography variant="caption" color="text.secondary">
                    Level 4 seeds every Gap-routed sub-control. Level 3 seeds one row per L3
                    control. This cannot be changed after create.
                  </Typography>
                </>
              )
            })()}
            <CompactTextField
              label="Scope"
              fullWidth
              required
              multiline
              minRows={2}
              maxRows={NARRATIVE_MAX_ROWS}
              value={scope}
              onChange={(e) => setScope(e.target.value)}
              placeholder="Org units, systems, or locations in scope"
              disabled={creating}
            />
            <FieldActionRow>
              <Box sx={{ flex: 1, minWidth: 0 }}>
                <FormSelect
                  label="Assessor"
                  fullWidth
                  required
                  value={assessorUserId === '' ? '' : String(assessorUserId)}
                  onChange={(v) => setAssessorUserId(v === '' ? '' : Number(v))}
                  disabled={creating}
                  hint="Admin or Owner"
                  options={[
                    { value: '', label: '-' },
                    ...allUsers.filter(isGapAssessor).map((u) => ({
                      value: String(u.id),
                      label: `${u.fullName} (${u.email})`,
                    })),
                  ]}
                />
              </Box>
              <CompactTextField
                label="Target date"
                type="date"
                InputLabelProps={{ shrink: true }}
                inputProps={{ min: dateMin }}
                value={targetDate}
                onChange={(e) => setTargetDate(e.target.value)}
                error={Boolean(createFields.fieldError('targetDate', targetDateError))}
                helperText={createFields.fieldError('targetDate', targetDateError) || undefined}
                onBlur={() => createFields.touch('targetDate')}
                disabled={creating}
              />
            </FieldActionRow>
            {creating && (
              <Box role="status" aria-live="polite" aria-busy="true">
                <LinearProgress aria-label="Creating gap assessment" />
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.75 }}>
                  Adding one Gap row for each control in {creatingFrameworkName}. Large frameworks can
                  take a minute or two. Stay on this page.
                </Typography>
              </Box>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => dismissCreate()} disabled={creating}>
            Cancel
          </Button>
          <BusyButton
            variant="contained"
            busy={creating}
            disabled={
              frameworkId === '' ||
              !name.trim() ||
              !scope.trim() ||
              assessorUserId === '' ||
              creating ||
              !!targetDateError
            }
            disabledReason={
              frameworkId === ''
                ? 'Select a project framework'
                : !name.trim()
                  ? 'Enter an assessment name'
                  : !scope.trim()
                    ? 'Enter a scope'
                    : assessorUserId === ''
                      ? 'Select an assessor'
                      : targetDateError || undefined
            }
            onClick={() => void create()}
          >
            {creating ? 'Creating…' : 'Create'}
          </BusyButton>
        </DialogActions>
      </Dialog>

      <ReviewDossierDrawer
        bodyRef={bindDrawerScroll}
        open={!!editItem}
        onClose={requestCloseDrawer}
        title={
          editItem
            ? `${editItem.nodeCode} - ${editItem.title}`
            : '-'
        }
        titleWeight={400}
        index={gapNavIndex < 0 ? 0 : gapNavIndex}
        total={gapNavItems.length}
        busy={Boolean(drawerWorking)}
        width={{ xs: '100%', sm: '92vw', md: '45%' }}
        onPrev={() => {
          if (gapNavIndex > 0) requestOpenItem(gapNavItems[gapNavIndex - 1])
        }}
        onNext={() => {
          if (gapNavIndex >= 0 && gapNavIndex < gapNavItems.length - 1) {
            requestOpenItem(gapNavItems[gapNavIndex + 1])
          }
        }}
        chips={
          editItem ? (
            <>
              <StatusChip
                label={editItem.applicability === 'NOT_APPLICABLE' ? 'Not applicable' : 'Applicable'}
                tone={editItem.applicability === 'NOT_APPLICABLE' ? 'neutral' : 'success'}
              />
              <StatusChip
                label={prettyGapType(editItem.reviewStatus)}
                tone={reviewTone(editItem.reviewStatus)}
              />
              {editItem.assignedToUserId ? (
                <StatusChip label="Assigned" tone="info" />
              ) : (
                <StatusChip label="Unassigned" tone="warning" />
              )}
              {drawerDirty ? <StatusChip label="Unsaved" tone="warning" /> : null}
            </>
          ) : null
        }
        toolbar={
          <CreamStepTabs
            value={drawerStep}
            onChange={(id) => {
              setDrawerStep(id)
              scrollDrawerStep(id)
            }}
            options={[
              { id: 1, label: 'Control' },
              ...(canAdmin ? [{ id: 2, label: 'Assign' }] : []),
              ...(applicability === 'APPLICABLE' && assignedReview
                ? [{ id: 3, label: 'Response' }]
                : []),
              ...(canAdmin && applicability === 'APPLICABLE' ? [{ id: 4, label: 'Decision' }] : []),
            ]}
          />
        }
        banner={
          editItem?.reviewStatus === 'SENT_BACK' ? (
            <AttentionBanner
              dense
              tone="warning"
              emphasis="Sent Back to Contributor"
              message={
                editItem.sendBackReason
                  ? editItem.sendBackReason
                  : `Status: ${editItem.implementationStatus?.replace(/_/g, ' ') ?? '-'}`
              }
            />
          ) : null
        }
        workingOverlay={
          drawerWorking ? (
            <Box
              role="status"
              aria-live="polite"
              aria-busy="true"
              sx={{
                position: 'absolute',
                inset: 0,
                zIndex: 4,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                px: 2.5,
                bgcolor: 'rgba(250, 250, 248, 0.94)',
              }}
            >
              <Box sx={{ width: '100%', maxWidth: 340 }}>
                <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1.25, mb: 1.25 }}>
                  <CircularProgress size={22} sx={{ color: brand.goldDark, mt: 0.15, flexShrink: 0 }} />
                  <Typography sx={{ fontSize: '0.9375rem', fontWeight: 700, color: brand.ink, lineHeight: 1.35 }}>
                    {drawerWorking.title}
                  </Typography>
                </Box>
                <Typography sx={{ fontSize: '0.8125rem', color: brand.muted, lineHeight: 1.5, mb: 1.5 }}>
                  {drawerWorking.message}
                </Typography>
                <LinearProgress
                  aria-label={drawerWorking.title}
                  sx={{
                    height: 3,
                    borderRadius: 1,
                    bgcolor: brand.goldLight,
                    '& .MuiLinearProgress-bar': { bgcolor: brand.goldDark },
                  }}
                />
              </Box>
            </Box>
          ) : null
        }
        footer={
          <>
            <Button onClick={requestCloseDrawer} disabled={Boolean(drawerWorking)} size="small">
              Close
            </Button>
            {canAdmin && editable && !itemFinalized && editItem?.applicability === 'APPLICABLE' ? (
              <Tooltip title={editItem.flaggedForLater ? 'Clear flag' : 'Flag for follow up'}>
                <span>
                  <IconButton
                    size="small"
                    disabled={Boolean(drawerWorking) || flagBusy}
                    aria-label={editItem.flaggedForLater ? 'Clear flag' : 'Flag for follow up'}
                    aria-pressed={Boolean(editItem.flaggedForLater)}
                    onClick={() => void toggleFlag()}
                    sx={{
                      width: 32,
                      height: 32,
                      borderRadius: 1,
                      border: `1px solid ${editItem.flaggedForLater ? '#FECACA' : brand.border}`,
                      bgcolor: editItem.flaggedForLater ? '#FEF2F2' : 'transparent',
                      color: editItem.flaggedForLater ? '#DC2626' : brand.muted,
                      '&:hover': {
                        bgcolor: editItem.flaggedForLater ? '#FEE2E2' : brand.hover,
                      },
                    }}
                  >
                    {editItem.flaggedForLater ? (
                      <FlagIcon sx={{ fontSize: 16 }} />
                    ) : (
                      <FlagOutlinedIcon sx={{ fontSize: 16 }} />
                    )}
                  </IconButton>
                </span>
              </Tooltip>
            ) : null}
            {editable && canEdit && (!assignedReview || canAdmin) && (
              <BusyButton
                size="small"
                variant={
                  (canAdmin &&
                    editable &&
                    editItem &&
                    applicability === 'APPLICABLE' &&
                    !gapControlReleased(editItem) &&
                    !drawerSendBlock) ||
                  (canAdmin && assignedReview && editItem?.reviewStatus !== 'SENT_BACK')
                    ? 'outlined'
                    : 'contained'
                }
                busy={Boolean(drawerWorking)}
                disabled={Boolean(saveBlockedReason) || !drawerDirty || Boolean(drawerWorking)}
                disabledReason={saveBlockedReason || (!drawerDirty ? 'No changes to save' : undefined)}
                onClick={() => void saveItem()}
              >
                {drawerWorking?.title === 'Saving this control'
                  ? 'Saving…'
                  : editItem && !assignedReview && !gapControlReleased(editItem)
                    ? 'Save Draft'
                    : 'Save'}
              </BusyButton>
            )}
            {canAdmin &&
              assignedReview &&
              !itemFinalized &&
              editItem?.reviewStatus !== 'SENT_BACK' &&
              editItem?.assignedToUserId && (
                <BusyButton
                  size="small"
                  variant={sendBackIntent && !sendBackBlockedReason ? 'contained' : 'outlined'}
                  busy={Boolean(drawerWorking)}
                  disabled={Boolean(sendBackBlockedReason) || Boolean(drawerWorking)}
                  disabledReason={sendBackBlockedReason ?? sendBackEnableHint}
                  onClick={() => void sendBackItem()}
                >
                  {drawerWorking?.title === 'Sending back to the Contributor' ? 'Sending Back…' : 'Send Back'}
                </BusyButton>
              )}
            {canAdmin &&
              assignedReview &&
              editItem?.reviewStatus !== 'SENT_BACK' && (
                <BusyButton
                  size="small"
                  variant={sendBackIntent ? 'outlined' : 'contained'}
                  busy={Boolean(drawerWorking)}
                  disabled={Boolean(finalizeBlockedReason) || Boolean(drawerWorking)}
                  disabledReason={finalizeBlockedReason}
                  onClick={() => void finalizeItem()}
                >
                  {drawerWorking && drawerWorking.title.startsWith('Setting')
                    ? 'Setting…'
                    : samaAssessment
                      ? 'Set Final Maturity'
                      : 'Set Final Status'}
                </BusyButton>
              )}
            {canAdmin && editable && editItem && applicability === 'APPLICABLE' && !gapControlReleased(editItem) ? (
              <BusyButton
                size="small"
                variant={drawerSendBlock ? 'outlined' : 'contained'}
                busy={drawerWorking?.title === 'Sending this control'}
                busyLabel="Sending…"
                disabled={Boolean(drawerSendBlock) || Boolean(drawerWorking)}
                disabledReason={drawerSendBlock ?? undefined}
                onClick={() => setDrawerSendOpen(true)}
              >
                Send To Contributor
              </BusyButton>
            ) : null}
          </>
        }
      >
          <Stack spacing={1.25}>
            <DrawerCard step={1} title="">
              <CompactTextField
                label="Control requirement"
                multiline
                minRows={2}
                maxRows={NARRATIVE_MAX_ROWS}
                fullWidth
                maxLength={false}
                disabled
                sx={{ '& .MuiInputBase-input': { textAlign: 'justify' } }}
                value={
                  editItem?.definitionText?.trim() &&
                  editItem.definitionText.trim() !== (editItem.title ?? '').trim()
                    ? editItem.definitionText
                    : editItem?.title || '-'
                }
              />
              {editItem?.priority ? (
                <Typography sx={{ fontSize: '0.6875rem', fontWeight: 650, color: brand.muted, mt: 0.75 }}>
                  Priority: {editItem.priority}
                </Typography>
              ) : null}
              {(whatItMeans.trim() || (canAdmin && editable)) && (
                <Box sx={{ mt: 1.25 }}>
                  {canAdmin && editable ? (
                    <CompactTextField
                      label="What it means"
                      multiline
                      minRows={2}
                      maxRows={NARRATIVE_MAX_ROWS}
                      fullWidth
                      value={whatItMeans}
                      onChange={(e) => setWhatItMeans(e.target.value)}
                    />
                  ) : (
                    <>
                      <Typography sx={{ fontSize: '0.6875rem', fontWeight: 650, color: brand.muted }}>
                        What it means
                      </Typography>
                      <Typography sx={{ fontSize: '0.8125rem', textAlign: 'justify', whiteSpace: 'pre-wrap' }}>
                        {whatItMeans.trim() || '-'}
                      </Typography>
                    </>
                  )}
                </Box>
              )}
              {(evidenceGuidance.trim() || (canAdmin && editable)) && (
                <Box sx={{ mt: 1.25 }}>
                  {canAdmin && editable ? (
                    <CompactTextField
                      label="Evidence"
                      multiline
                      minRows={2}
                      maxRows={NARRATIVE_MAX_ROWS}
                      fullWidth
                      value={evidenceGuidance}
                      onChange={(e) => setEvidenceGuidance(e.target.value)}
                    />
                  ) : (
                    <>
                      <Typography sx={{ fontSize: '0.6875rem', fontWeight: 650, color: brand.muted }}>
                        Evidence
                      </Typography>
                      <Typography sx={{ fontSize: '0.8125rem', textAlign: 'justify', whiteSpace: 'pre-wrap' }}>
                        {evidenceGuidance.trim() || '-'}
                      </Typography>
                    </>
                  )}
                </Box>
              )}
            </DrawerCard>
            <DrawerCard title="Applicability">
              <Stack spacing={1}>
                {scoringProfile === 'UAE_IA' || scoringProfile === 'NCEMA' ? (
                  <StatusChip label="Applicable" tone="success" />
                ) : (
                <FormSelect
                  label="Applicability"
                  value={applicability}
                  options={[
                    { value: 'APPLICABLE', label: 'Applicable' },
                    { value: 'NOT_APPLICABLE', label: 'Not applicable' },
                  ]}
                  onChange={(v) => setApplicability(v as GapApplicability)}
                  disabled={!canAdmin && !!assignedReview}
                />
                )}
                {editItem && applicability !== editItem.applicability && (
                  <CompactTextField
                    label="Override justification"
                    required
                    multiline
                    minRows={2}
                    maxRows={NARRATIVE_MAX_ROWS}
                    fieldIdPrefix="gap"
                    value={overrideJustification}
                    onChange={(e) => setOverrideJustification(e.target.value)}
                    helperText={
                      detail?.assessment.soaId != null
                        ? 'Required when changing SoA applicability for this assessment only'
                        : 'Required when changing applicability on this assessment'
                    }
                  />
                )}
              </Stack>
            </DrawerCard>
            {canAdmin && editable && applicability === 'APPLICABLE' && (
              <>
              <DrawerCard step={2} title="Assignment" subtitle="Who should complete this control?">
                <Stack spacing={1}>
                  <Box sx={twoColFieldSx}>
                  <FormSelect
                    label="Org unit (filter contributors)"
                    value={String(assignOrgUnitId)}
                    options={[
                      { value: '', label: 'All contributors' },
                      ...orgUnits.map((u) => ({ value: String(u.id), label: u.name })),
                    ]}
                    onChange={(v) => {
                      setAssignOrgUnitId(v === '' ? '' : Number(v))
                      setAssignUserId('')
                    }}
                  />
                  <FormSelect
                    label="Contributor"
                    required
                    value={String(assignUserId)}
                    options={contributors.map((u) => ({
                      value: String(u.id),
                      label: `${u.fullName} (${u.email})`,
                    }))}
                    onChange={(v) => setAssignUserId(Number(v))}
                    disabled={contributors.length === 0}
                  />
                  </Box>
                  {contributors.length === 0 && (
                    <Typography variant="caption" color="warning.main">
                      No Contributor users in this tenant. Invite one with the Contributor role.
                    </Typography>
                  )}
                  <CompactTextField
                    label="Due date"
                    required
                    type="date"
                    fieldIdPrefix="gap"
                    value={assignDue}
                    onChange={(e) => setAssignDue(e.target.value)}
                    error={!!assignDueError || !!assignFields.fieldError('assignDue', assignDueError)}
                    helperText={
                      assignDueError ||
                      (assessmentTargetDate
                        ? 'Today or later, and on or before the Gap Assessment target date'
                        : 'Today or later')
                    }
                    onBlur={() => assignFields.touch('assignDue')}
                    inputProps={{ min: dateMin, ...(assessmentTargetDate ? { max: assessmentTargetDate } : {}) }}
                  />
                  <Box sx={{ pt: 0.5 }}>
                    <Typography sx={{ fontSize: '0.6875rem', fontWeight: 700, letterSpacing: '0.06em', textTransform: 'uppercase', color: brand.muted }}>
                      Assignment summary
                    </Typography>
                    <Typography sx={{ fontSize: '0.8125rem', color: brand.ink, mt: 0.5 }}>
                      {allUsers.find((u) => u.id === assignUserId)?.fullName ?? 'No contributor yet'}
                      {' · '}
                      {assignAllQuestions
                        ? `${controlQuestions.length + customQuestions.length} questions`
                        : `${assignFwQuestionIds.length + assignCustomQuestionIds.length} questions`}
                      {' · Due '}
                      {formatTenantDate(assignDue)}
                    </Typography>
                    <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.75 }}>
                      {editItem && gapControlReleased(editItem)
                        ? 'This control is already with the contributor. Save updates the assignment without another email.'
                        : 'Save Draft stores this control as it is. Send To Contributor turns on when the contributor, due date, classification, and Gap description are filled.'}
                    </Typography>
                  </Box>
                </Stack>
              </DrawerCard>
              <DrawerCard title="Questions to be assigned">
                <Stack spacing={1}>
                  <FormControlLabel
                    control={
                      <Checkbox
                        size="small"
                        checked={assignAllQuestions}
                        onChange={(e) => {
                          const all = e.target.checked
                          setAssignAllQuestions(all)
                          if (all) {
                            setAssignFwQuestionIds(controlQuestions.map((q) => q.id))
                            setAssignCustomQuestionIds(customQuestions.map((q) => q.id))
                          }
                        }}
                      />
                    }
                    label={<Typography sx={{ fontSize: '0.8rem' }}>Assign All Questions</Typography>}
                  />
                  <Stack spacing={0.5}>
                    <Typography variant="caption" color="text.secondary">
                      {assignAllQuestions
                        ? 'All questions below go to the Contributor'
                        : 'Select questions for the Contributor'}
                    </Typography>
                    {!assignAllQuestions && (
                      <Stack direction="row" spacing={1}>
                        <Button
                          size="small"
                          onClick={() => {
                            setAssignFwQuestionIds(controlQuestions.map((q) => q.id))
                            setAssignCustomQuestionIds(customQuestions.map((q) => q.id))
                          }}
                        >
                          Select All
                        </Button>
                        <Button
                          size="small"
                          onClick={() => {
                            setAssignFwQuestionIds([])
                            setAssignCustomQuestionIds([])
                          }}
                        >
                          Clear
                        </Button>
                      </Stack>
                    )}
                    {controlQuestions.map((q, i) => (
                      <FormControlLabel
                        key={`fw-${q.id}`}
                        control={
                          <Checkbox
                            size="small"
                            checked={assignAllQuestions || assignFwQuestionIds.includes(q.id)}
                            onChange={(e) => {
                              if (assignAllQuestions && !e.target.checked) {
                                setAssignAllQuestions(false)
                                setAssignFwQuestionIds(
                                  controlQuestions.map((row) => row.id).filter((id) => id !== q.id),
                                )
                                setAssignCustomQuestionIds(customQuestions.map((row) => row.id))
                                return
                              }
                              setAssignFwQuestionIds((prev) =>
                                e.target.checked
                                  ? [...prev, q.id]
                                  : prev.filter((id) => id !== q.id),
                              )
                            }}
                          />
                        }
                        label={
                          <Typography sx={{ fontSize: '0.75rem' }}>
                            {gapQuestionDisplayCode(q.questionCode, i)}: {q.questionText}
                          </Typography>
                        }
                      />
                    ))}
                    {customQuestions.map((q) => (
                      <FormControlLabel
                        key={`cu-${q.id}`}
                        control={
                          <Checkbox
                            size="small"
                            checked={assignAllQuestions || assignCustomQuestionIds.includes(q.id)}
                            onChange={(e) => {
                              if (assignAllQuestions && !e.target.checked) {
                                setAssignAllQuestions(false)
                                setAssignFwQuestionIds(controlQuestions.map((row) => row.id))
                                setAssignCustomQuestionIds(
                                  customQuestions.map((row) => row.id).filter((id) => id !== q.id),
                                )
                                return
                              }
                              setAssignCustomQuestionIds((prev) =>
                                e.target.checked
                                  ? [...prev, q.id]
                                  : prev.filter((id) => id !== q.id),
                              )
                            }}
                          />
                        }
                        label={
                          <Typography sx={{ fontSize: '0.75rem' }}>Custom: {q.questionText}</Typography>
                        }
                      />
                    ))}
                    {controlQuestions.length === 0 && customQuestions.length === 0 && (
                      <Typography variant="caption" color="text.secondary">
                        No questions on this control yet.
                      </Typography>
                    )}
                  </Stack>
                </Stack>
              </DrawerCard>
              <DrawerCard
                title="Assessment-specific questions"
                subtitle="Extra questions for this assessment only. They go to the Contributor with the catalog set."
              >
                    <Stack spacing={1}>
                      {customQuestions.length > 0 ? (
                        <Stack spacing={0.5}>
                          {customQuestions.map((cq, i) => {
                            const justAdded = cq.id === justAddedCustomQuestionId
                            return (
                              <Box
                                key={cq.id}
                                sx={{
                                  px: 1,
                                  py: 0.65,
                                  borderRadius: 0.75,
                                  bgcolor: justAdded ? brand.goldLight : brand.paper,
                                  border: `1px solid ${justAdded ? brand.goldMid : brand.border}`,
                                }}
                              >
                                <Typography
                                  sx={{
                                    fontSize: '0.75rem',
                                    fontWeight: 600,
                                    color: justAdded ? brand.goldDark : brand.ink,
                                    ...narrativeReadScrollSx,
                                  }}
                                >
                                  Custom {i + 1}: {cq.questionText}
                                </Typography>
                              </Box>
                            )
                          })}
                        </Stack>
                      ) : (
                        <Typography sx={{ fontSize: '0.75rem', color: brand.muted }}>
                          None added yet.
                        </Typography>
                      )}
                      {detail && editItem && (
                        <FieldActionRow>
                          <CompactTextField
                            label="Add custom question"
                            fieldIdPrefix="gap-cq"
                            fullWidth
                            maxLength={INPUT_LIMIT.customQuestion}
                            value={newCustomQuestion}
                            onChange={(e) => setNewCustomQuestion(e.target.value)}
                          />
                          <BusyButton
                            size="small"
                            variant="contained"
                            busy={addingQuestion}
                            busyLabel="Adding…"
                            sx={{ flexShrink: 0, whiteSpace: 'nowrap', height: 32 }}
                            disabled={!newCustomQuestion.trim() || addingQuestion || Boolean(drawerWorking)}
                            disabledReason={
                              !newCustomQuestion.trim() ? 'Enter a question to add' : undefined
                            }
                            onClick={() => {
                              if (!detail || !editItem || !newCustomQuestion.trim() || addingQuestion) return
                              void (async () => {
                                setAddingQuestion(true)
                                try {
                                  const cq = await api.addGapCustomQuestion(
                                    detail.assessment.id,
                                    editItem.id,
                                    newCustomQuestion.trim(),
                                  )
                                  setCustomQuestions((prev) => [...prev, cq])
                                  setJustAddedCustomQuestionId(cq.id)
                                  if (assignAllQuestions) {
                                    setAssignCustomQuestionIds((prev) =>
                                      prev.includes(cq.id) ? prev : [...prev, cq.id],
                                    )
                                  }
                                  setNewCustomQuestion('')
                                  onOk('Custom question added')
                                } catch (e) {
                                  onError(String(e))
                                } finally {
                                  setAddingQuestion(false)
                                }
                              })()
                            }}
                          >
                            Add Question
                          </BusyButton>
                        </FieldActionRow>
                      )}
                    </Stack>
              </DrawerCard>
              </>
            )}
            {applicability === 'APPLICABLE' && assignedReview && (
              <DrawerCard step={3} title="Contributor response">
                {editItem?.contributorComments?.trim() ? (
                  <Box sx={{ mb: 1.25, maxWidth: NARRATIVE_FIELD_MAX_WIDTH }}>
                    <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
                      Contributor comments
                    </Typography>
                    <Typography variant="body2" sx={narrativeReadScrollSx}>
                      {editItem.contributorComments.trim()}
                    </Typography>
                  </Box>
                ) : null}
                {samaAssessment && assignedReview && (
                  <Box sx={{ mb: 1 }}>
                    <SamaMaturityGatesEditor
                      gates={normalizeGates(editItem?.maturityGates)}
                      onChange={() => {}}
                      disabled
                    />
                  </Box>
                )}
            {controlQuestions.length > 0 && (
              <Box sx={{ mb: 1.5 }}>
                <Typography variant="subtitle2" sx={{ mb: 0.75 }}>
                  Questions
                </Typography>
                <Stack spacing={1.25}>
                  {controlQuestions.map((q, i) => (
                    <Box key={q.id}>
                      <Typography variant="body2" sx={{ mb: 0.5, ...narrativeReadScrollSx }}>
                        {gapQuestionDisplayCode(q.questionCode, i)}
                        {q.questionType ? ` · ${q.questionType}` : ''}: {q.questionText}
                      </Typography>
                      {q.evidenceGuidance && (
                        <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 0.5, ...narrativeReadScrollSx }}>
                          Evidence: {q.evidenceGuidance}
                        </Typography>
                      )}
                      {questionNotAssigned('c', q.id) ? (
                        <Typography variant="body2" color="text.secondary">
                          Not assigned
                        </Typography>
                      ) : (
                      <>
                      <Stack spacing={1}>
                        {!canEdit ||
                        detail?.assessment.status === 'COMPLETED' ||
                        !!editItem?.assignedToUserId ? (
                          <Box sx={twoColFieldSx}>
                            <Box>
                              <FormFieldLabel>Compliance Status</FormFieldLabel>
                              <Typography
                                variant="body2"
                                sx={{
                                  lineHeight: 1.4,
                                  color:
                                    questionAnswers[q.id]?.answer
                                      ? 'text.primary'
                                      : 'text.secondary',
                                }}
                              >
                                {GAP_ANSWER_LABEL[questionAnswers[q.id]?.answer ?? ''] ?? '-'}
                              </Typography>
                            </Box>
                            <Box sx={{ minWidth: 0 }}>
                              <FormFieldLabel>Answer</FormFieldLabel>
                              <Typography
                                variant="body2"
                                sx={{
                                  ...narrativeReadScrollSx,
                                  lineHeight: 1.4,
                                  color: questionAnswers[q.id]?.note?.trim()
                                    ? 'text.primary'
                                    : 'text.secondary',
                                }}
                              >
                                {questionAnswers[q.id]?.note?.trim() || '-'}
                              </Typography>
                            </Box>
                          </Box>
                        ) : (
                        <FormSelect
                          label="Compliance Status"
                          value={questionAnswers[q.id]?.answer ?? ''}
                          options={[
                            { value: '', label: '-' },
                            { value: 'YES', label: 'Compliant' },
                            { value: 'NO', label: 'Not Compliant' },
                            { value: 'PARTIAL', label: 'Partially' },
                            { value: 'NA', label: 'N/A' },
                          ]}
                          onChange={(v) =>
                            setQuestionAnswers((prev) => ({
                              ...prev,
                              [q.id]: { answer: v, note: prev[q.id]?.note ?? '' },
                            }))
                          }
                          minWidth={140}
                        />
                        )}
                        {canEdit &&
                        detail?.assessment.status !== 'COMPLETED' &&
                        !editItem?.assignedToUserId ? (
                          <CompactTextField
                            label="Answer"
                            fieldIdPrefix="gap-q"
                            multiline
                            minRows={2}
                            maxRows={NARRATIVE_MAX_ROWS}
                            maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
                            value={questionAnswers[q.id]?.note ?? ''}
                            onChange={(e) =>
                              setQuestionAnswers((prev) => ({
                                ...prev,
                                [q.id]: { answer: prev[q.id]?.answer ?? '', note: e.target.value },
                              }))
                            }
                          />
                        ) : null}
                      </Stack>
                      <Typography sx={{ ...sectionLabelSx, mt: 1, mb: 0.25 }}>
                        Evidence
                      </Typography>
                      <EvidenceAttachedList
                        items={questionEvidence[`c:${q.id}`] ?? []}
                        emptyLabel="No evidence on this question"
                        onError={onError}
                        onRemove={
                          adminCanRemoveEvidence && editItem
                            ? (ev) =>
                                void removeAdminGapEvidence(
                                  ev,
                                  'GAP_QUESTION',
                                  gapQuestionRecordId(editItem.id, `c:${q.id}`),
                                )
                            : undefined
                        }
                      />
                      </>
                      )}
                    </Box>
                  ))}
                </Stack>
              </Box>
            )}
            {customQuestions.length > 0 && (
              <Box sx={{ mt: 2.5, pt: 2, borderTop: `1px solid ${brand.border}` }}>
                <Typography variant="subtitle2" sx={{ mb: 0.75 }}>
                  Assessment-specific questions
                </Typography>
                {customQuestions.map((cq) => (
                  <Box key={cq.id} sx={{ mb: 1 }}>
                    <Typography variant="body2" sx={{ mb: 0.5, ...narrativeReadScrollSx }}>
                      Custom: {cq.questionText}
                    </Typography>
                    {questionNotAssigned('u', cq.id) ? (
                      <Typography variant="body2" color="text.secondary">
                        Not assigned
                      </Typography>
                    ) : (
                    <>
                    <Box sx={twoColFieldSx}>
                      <Box>
                        <FormFieldLabel>Compliance Status</FormFieldLabel>
                        <Typography
                          variant="body2"
                          sx={{
                            lineHeight: 1.4,
                            color: customAnswers[cq.id]?.answer ? 'text.primary' : 'text.secondary',
                          }}
                        >
                          {GAP_ANSWER_LABEL[customAnswers[cq.id]?.answer ?? ''] ?? '-'}
                        </Typography>
                      </Box>
                      <Box sx={{ minWidth: 0 }}>
                        <FormFieldLabel>Answer</FormFieldLabel>
                        <Typography
                          variant="body2"
                          sx={{
                            ...narrativeReadScrollSx,
                            lineHeight: 1.4,
                            color: customAnswers[cq.id]?.note?.trim() ? 'text.primary' : 'text.secondary',
                          }}
                        >
                          {customAnswers[cq.id]?.note?.trim() || '-'}
                        </Typography>
                      </Box>
                    </Box>
                    <Typography sx={{ ...sectionLabelSx, mt: 1, mb: 0.25 }}>
                      Evidence
                    </Typography>
                    <EvidenceAttachedList
                      items={questionEvidence[`u:${cq.id}`] ?? []}
                      emptyLabel="No evidence on this question"
                      onError={onError}
                      onRemove={
                        adminCanRemoveEvidence && editItem
                          ? (ev) =>
                              void removeAdminGapEvidence(
                                ev,
                                'GAP_QUESTION',
                                gapQuestionRecordId(editItem.id, `u:${cq.id}`),
                              )
                          : undefined
                      }
                    />
                    </>
                    )}
                  </Box>
                ))}
              </Box>
            )}
            {controlQuestions.length === 0 && customQuestions.length === 0 && editItem && (
              <Typography variant="caption" color="text.secondary">
                No catalog questions loaded for this control yet (import via Platform Console).
              </Typography>
            )}
            {itemEvidence.length > 0 && (
            <Box>
              <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
                Evidence
              </Typography>
              <EvidenceAttachedList
                items={itemEvidence}
                onError={onError}
                onRemove={
                  adminCanRemoveEvidence && editItem
                    ? (ev) => void removeAdminGapEvidence(ev, 'GAP_ITEM', String(editItem.id))
                    : undefined
                }
              />
            </Box>
            )}
              </DrawerCard>
            )}
            {canAdmin && applicability === 'APPLICABLE' && (
              <DrawerCard step={4} title="Admin decision">
                {samaAssessment ? (
                  assignedReview ? (
                    itemFinalized || assessmentLocked ? (
                      <Box>
                        <FormFieldLabel>Final maturity (target {SAMA_TARGET_MATURITY})</FormFieldLabel>
                        <Typography variant="body2" sx={{ minHeight: 32, display: 'flex', alignItems: 'center' }}>
                          {SAMA_MATURITY.find((o) => o.value === finalMaturity)?.label ?? '-'}
                        </Typography>
                      </Box>
                    ) : (
                      <FormSelect
                        label={`Final maturity (target ${SAMA_TARGET_MATURITY})`}
                        value={finalMaturity === '' ? '' : String(finalMaturity)}
                        options={SAMA_MATURITY.map((o) => ({
                          value: String(o.value),
                          label: o.label,
                        }))}
                        onChange={(v) => setFinalMaturity(v === '' ? '' : Number(v))}
                      />
                    )
                  ) : (
                    <SamaMaturityGatesEditor gates={maturityGates} onChange={setMaturityGates} />
                  )
                ) : (
                  <>
                    {itemFinalized || assessmentLocked ? (
                      <Box>
                        <FormFieldLabel>Implementation status</FormFieldLabel>
                        <Typography variant="body2" sx={{ minHeight: 32, display: 'flex', alignItems: 'center' }}>
                          {IMPL.find((o) => o.value === (assignedReview ? finalStatus : implementationStatus))
                            ?.label ?? '-'}
                        </Typography>
                      </Box>
                    ) : (
                      <Box role="radiogroup" aria-label="Implementation status">
                        <Typography sx={{ fontSize: '0.875rem', fontWeight: 600, color: brand.ink, mb: 1 }}>
                          How should this control be classified?
                          <Typography component="span" sx={{ color: brand.goldDark, ml: 0.25, fontWeight: 700 }}>
                            *
                          </Typography>
                        </Typography>
                        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 1 }}>
                        {IMPL.map((o) => {
                          const current = assignedReview ? finalStatus : implementationStatus
                          const active = current === o.value
                          return (
                            <Box
                              key={o.value}
                              component="button"
                              type="button"
                              role="radio"
                              aria-checked={active}
                              onClick={() => {
                                if (assignedReview) setFinalStatus(o.value)
                                else setImplementationStatus(o.value)
                              }}
                              sx={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: 1,
                                width: '100%',
                                textAlign: 'left',
                                px: 1.25,
                                py: 1,
                                borderRadius: 1,
                                border: `1px solid ${active ? brand.gold : brand.border}`,
                                boxShadow: active ? `inset 0 0 0 1px ${brand.gold}` : 'none',
                                bgcolor: active ? '#FFFBF3' : brand.paper,
                                cursor: 'pointer',
                                font: 'inherit',
                                transition: 'border-color 120ms ease, background-color 120ms ease',
                              }}
                            >
                              <Box
                                sx={{
                                  width: 16,
                                  height: 16,
                                  borderRadius: '50%',
                                  flexShrink: 0,
                                  border: `2px solid ${active ? brand.gold : brand.border}`,
                                  bgcolor: active ? brand.gold : 'transparent',
                                  boxShadow: active ? 'inset 0 0 0 2px #fff' : 'none',
                                }}
                              />
                              <Typography sx={{ fontSize: '0.875rem', fontWeight: 600, color: brand.ink }}>
                                {o.label}
                              </Typography>
                            </Box>
                          )
                        })}
                        </Box>
                      </Box>
                    )}
                    <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
                      {itemFinalized
                        ? 'Final status is set.'
                        : scoringProfile === 'UAE_IA'
                          ? 'Final status can only be set when the control is Fully Implemented.'
                          : assignedReview
                            ? 'Required to confirm the score or to send back to the Contributor.'
                            : 'Set when you confirm or send back. Contributor can see it but cannot change it.'}
                    </Typography>
                  </>
                )}
                {assignedReview ? (
                  <>
                    <Box sx={{ mt: 1, maxWidth: NARRATIVE_FIELD_MAX_WIDTH }}>
                      <FormFieldLabel>Gap description</FormFieldLabel>
                      <Typography
                        variant="body2"
                        sx={{
                          ...narrativeReadScrollSx,
                          color: editItem?.observations?.trim() ? 'text.primary' : 'text.secondary',
                        }}
                      >
                        {editItem?.observations?.trim() || '-'}
                      </Typography>
                    </Box>
                    <Box sx={{ mt: 1, maxWidth: NARRATIVE_FIELD_MAX_WIDTH }}>
                      <FormFieldLabel>Remarks</FormFieldLabel>
                      <Typography
                        variant="body2"
                        sx={{
                          ...narrativeReadScrollSx,
                          color: editItem?.gapDescription?.trim() ? 'text.primary' : 'text.secondary',
                        }}
                      >
                        {editItem?.gapDescription?.trim() || '-'}
                      </Typography>
                    </Box>
                    <Box sx={{ mt: 1 }}>
                      {itemFinalized || assessmentLocked ? (
                        <Box sx={{ maxWidth: NARRATIVE_FIELD_MAX_WIDTH }}>
                          <FormFieldLabel>Admin review notes</FormFieldLabel>
                          <Typography
                            variant="body2"
                            sx={{
                              ...narrativeReadScrollSx,
                              color: adminNotes.trim() ? 'text.primary' : 'text.secondary',
                            }}
                          >
                            {adminNotes.trim() || '-'}
                          </Typography>
                        </Box>
                      ) : (
                        <CompactTextField
                          label="Admin review notes"
                          multiline
                          minRows={2}
                          maxRows={NARRATIVE_MAX_ROWS}
                          maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
                          fieldIdPrefix="gap-admin"
                          value={adminNotes}
                          onChange={(e) => setAdminNotes(e.target.value)}
                        />
                      )}
                    </Box>
                    {editItem?.reviewStatus !== 'SENT_BACK' && !itemFinalized && !assessmentLocked && (
                      <Box
                        sx={{
                          mt: 1,
                          p: 1.25,
                          pl: 1.5,
                          borderRadius: 1,
                          maxWidth: NARRATIVE_FIELD_MAX_WIDTH,
                          bgcolor: brand.paper,
                          border: `1px solid ${brand.border}`,
                          position: 'relative',
                          '&::before': {
                            content: '""',
                            position: 'absolute',
                            left: 0,
                            top: 0,
                            bottom: 0,
                            width: 3,
                            bgcolor: brand.gold,
                            borderRadius: '8px 0 0 8px',
                          },
                        }}
                      >
                        <Typography
                          sx={{
                            fontSize: '0.75rem',
                            fontWeight: 700,
                            color: brand.goldDark,
                            mb: 0.75,
                            lineHeight: 1.35,
                          }}
                        >
                          Return to Contributor
                        </Typography>
                        <CompactTextField
                          label="Send-back reason"
                          multiline
                          minRows={2}
                          maxRows={NARRATIVE_MAX_ROWS}
                          maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
                          fieldIdPrefix="gap-admin"
                          value={sendBackReason}
                          onChange={(e) => setSendBackReason(e.target.value)}
                        />
                      </Box>
                    )}
                  </>
                ) : (
                  <>
                    <Box sx={{ mt: 1 }}>
                      <CompactTextField
                        label="Gap description"
                        required
                        multiline
                        minRows={2}
                        maxRows={NARRATIVE_MAX_ROWS}
                        maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
                        fieldIdPrefix="gap-admin"
                        value={observations}
                        onChange={(e) => setObservations(e.target.value)}
                      />
                    </Box>
                    <Box sx={{ mt: 1 }}>
                      <CompactTextField
                        label="Remarks"
                        multiline
                        minRows={2}
                        maxRows={NARRATIVE_MAX_ROWS}
                        maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
                        fieldIdPrefix="gap-admin"
                        value={gapDescription}
                        onChange={(e) => setGapDescription(e.target.value)}
                      />
                    </Box>
                  </>
                )}
              </DrawerCard>
            )}
          </Stack>
      </ReviewDossierDrawer>

      <Dialog open={soaPromptOpen} onClose={() => setSoaPromptOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Update the SoA?</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 1.5 }}>
            {soaPromptOverrides.length} control
            {soaPromptOverrides.length === 1 ? '' : 's'} had applicability changes during this Gap
            Assessment. Changes apply to this assessment only: review and update the SoA if needed.
          </Typography>
          <Stack spacing={0.75}>
            {soaPromptOverrides.map((i) => (
              <Typography key={i.id} variant="caption" display="block">
                {i.nodeCode} → {i.applicability === 'APPLICABLE' ? 'Applicable' : 'N/A'}
                {i.overrideJustification ? `: ${i.overrideJustification}` : ''}
              </Typography>
            ))}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSoaPromptOpen(false)}>Dismiss</Button>
          <Button
            variant="contained"
            onClick={() => {
              setSoaPromptOpen(false)
              if (soaPromptSoaId != null && onOpenSoa) {
                onOpenSoa(soaPromptSoaId)
              }
            }}
            disabled={!onOpenSoa || soaPromptSoaId == null}
          >
            Open SoA
          </Button>
        </DialogActions>
      </Dialog>
      <ConfirmActionDialog
        open={drawerSendOpen}
        title="Send this control to the contributor?"
        message="The contributor will see this control and receive one email."
        confirmLabel="Send"
        confirmColor="primary"
        busy={drawerWorking?.title === 'Sending this control'}
        busyLabel="Sending…"
        confirmDisabled={Boolean(drawerSendBlock)}
        disabledReason={drawerSendBlock ?? undefined}
        onCancel={() => {
          if (drawerWorking?.title !== 'Sending this control') setDrawerSendOpen(false)
        }}
        onConfirm={() => void sendOpenControl()}
      />
      <ConfirmActionDialog
        open={sendConfirmOpen}
        title="Send selected controls?"
        message={
          selectedItems.length === 1
            ? 'The contributor will see this control and receive one email.'
            : `${selectedItems.length} controls will be sent. Each contributor receives one email listing their controls.`
        }
        confirmLabel="Send"
        confirmColor="primary"
        busy={listAction === 'send'}
        busyLabel="Sending…"
        confirmDisabled={Boolean(releaseBlock)}
        disabledReason={releaseBlock ?? undefined}
        onCancel={() => {
          if (listAction == null) setSendConfirmOpen(false)
        }}
        onConfirm={() => void sendSelected()}
      />
      <ConfirmActionDialog
        open={sendBackOpen}
        title="Send back selected controls?"
        message="Each contributor receives one email listing the controls in this selection, with the reason below."
        confirmLabel="Send Back"
        confirmColor="primary"
        busy={listAction === 'send-back'}
        busyLabel="Sending Back…"
        confirmDisabled={!bulkSendBackReason.trim() || Boolean(sendBackBlock)}
        disabledReason={
          sendBackBlock ?? (!bulkSendBackReason.trim() ? 'A reason is required' : undefined)
        }
        onCancel={() => {
          if (listAction == null) setSendBackOpen(false)
        }}
        onConfirm={() => void sendBackSelected()}
      >
        <CompactTextField
          label="Reason"
          required
          multiline
          minRows={2}
          maxRows={4}
          fullWidth
          value={bulkSendBackReason}
          onChange={(e) => setBulkSendBackReason(e.target.value)}
        />
      </ConfirmActionDialog>
      <DiscardChangesDialog
        open={discardDrawer}
        title="Discard unsaved control changes?"
        message="You have unsaved edits on this control. Save to keep them, or discard to close."
        saveDraftLabel="Save"
        onSaveDraft={() => {
          void (async () => {
            const ok = await saveItem()
            if (ok) setDiscardDrawer(false)
          })()
        }}
        onKeepEditing={() => {
          pendingGapItem.current = null
          setDiscardDrawer(false)
        }}
        onDiscard={() => {
          const next = pendingGapItem.current
          pendingGapItem.current = null
          setDiscardDrawer(false)
          if (next) openEdit(next)
          else setEditItem(null)
        }}
      />
    </Stack>
  )
}
