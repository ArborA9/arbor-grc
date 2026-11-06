import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  Checkbox,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Snackbar,
  Stack,
  Typography,
} from '@mui/material'
import ContributorDashboard from './ContributorDashboard'
import ContributorControlList from './components/contributor/ContributorControlList'
import ContributorEvidencePanel from './components/contributor/ContributorEvidencePanel'
import ContributorGapQuestionStep from './components/contributor/ContributorGapQuestionStep'
import ContributorGapReturnedCard from './components/contributor/ContributorGapReturnedCard'
import {
  ContributorRail,
  ContributorWorkArea,
  contributorPadX,
} from './components/contributor/ContributorShell'
import ContributorReturnedBanner from './components/contributor/ContributorReturnedBanner'
import ContributorSummaryBanner from './components/contributor/ContributorSummaryBanner'
import ContributorTaskList from './components/contributor/ContributorTaskList'
import { groupIntoAssignments, type ContributorAssignment } from './components/contributor/assignmentGrouping'
import {
  CONTRIBUTOR_NAV_CATALOG,
  contributorNavCaption,
  contributorNavIdForTask,
  contributorNavTitle,
  contributorWorkBackLabel,
  filterNavForContributor,
  firstContributorNavId,
  navContainsId,
  taskMatchesContributorNav,
} from './components/contributor/contributorNav'
import { firstGapCommentIssue, GAP_COMMENT_HINT, gapContributorTextIssue } from './components/gap/gapContributorText'
import {
  cardVisualStatus,
  CONTRIBUTOR_TABS,
  statusBadge,
  type ContributorTab,
} from './components/contributor/contribUtils'
import {
  api,
  type EvidenceClassification,
  EVIDENCE_CLASSIFICATIONS,
  EVIDENCE_CLASSIFICATION_LABELS,
  type EvidenceSummary,
  type FrameworkQuestion,
  type GapCustomQuestion,
  type GapImplementationStatus,
  type RiskLevel,
  type Session,
  type TransferMethod,
  type TreatmentOption,
  type WorkTaskDetail,
  type WorkTaskSummary,
  type IncidentType,
  type NotificationItem,
  type ReportIncidentBody,
} from './api'
import { formatTenantDate } from './uiQuality/tenantLocale'
import { resolveCapaNotificationTarget } from './notifications/capaNotificationTarget'
import { resolveGapNotificationTarget } from './notifications/gapNotificationTarget'
import { resolveRiskNotificationTarget } from './notifications/riskNotificationTarget'
import BcmsBiaPage from './BcmsBiaPage'
import AssessmentsPage from './AssessmentsPage'
import RiskPage from './RiskPage'
import AuditsPage from './AuditsPage'
import { CAPA_EXCEL } from './components/audit/capaExcelFields'
import { capaSourceLabel } from './components/audit/capaRegisterUtils'
import KriPage from './KriPage'
import IncidentsPage from './IncidentsPage'
import PrivacyPage from './PrivacyPage'
import PoliciesPage from './PoliciesPage'
import DiscardChangesDialog from './components/DiscardChangesDialog'
import SamaMaturityGatesEditor from './components/gap/SamaMaturityGatesEditor'
import {
  emptyGates,
  gatesComplete,
  normalizeGates,
  type GateAnswer,
} from './components/gap/samaMaturityGates'
import StatusChip from './components/StatusChip'
import { CompactTextField, FormSelect, NARRATIVE_FIELD_MAX_WIDTH, NARRATIVE_MAX_ROWS, narrativeReadScrollSx, RiskLevelBadge, SettingsCard, twoColFieldSx } from './components/ui'
import WorkspaceBootScreen from './components/login/WorkspaceBootScreen'
import { sectionLabelSx } from './components/ui/FormFieldLabel'
import { kriRagToBadge } from './components/ui/riskBadgeMappers'
import { gapQuestionRecordId, loadGapQuestionEvidence } from './components/gap/gapEvidence'
import { EXCEL_FIELD, EXCEL_TREAT_LABEL, acceptanceLevel2Label, isIdentificationEvidence } from './components/risk/excelRiskFields'
import { RISK_EVIDENCE_MAX_BYTES, RISK_EVIDENCE_MAX_FILES } from './components/risk/riskEvidenceLimits'
import ExcelRiskDossier, { EXCEL_DOSSIER_SECTIONS } from './components/risk/ExcelRiskDossier'
import RiskFormShell from './components/risk/RiskFormShell'
import RiskRecordCreateProgress from './components/risk/RiskRecordCreateProgress'
import ExcelAcceptanceContributor, { ExcelAcceptanceApprover } from './components/risk/ExcelAcceptanceContributor'
import { brand, layout, semantic } from './theme'
import { frameworkDisplayName } from './uiQuality/frameworkLabel'
import { maxToleranceReviewIso, todayIso, toleranceReviewDateError } from './validation'
import { productErrorMessage } from './uiQuality/productError'
import { setArborTitle } from './uiQuality/pageTitle'
import { useBeforeUnload } from './uiQuality/useBeforeUnload'
import { roleLabel, roleLabels } from './uiQuality/roleLabels'
import { useTenantLocale } from './uiQuality/TenantLocaleContext'
import { readNavParam, writeNavParam } from './uiQuality/navUrl'
import AppShell from './components/AppShell'
import { AppShellPageProvider, type AppShellPageConfig } from './components/AppShellPageContext'
import NotificationBell from './components/NotificationBell'
import {
  UnsavedChangesGuardProvider,
  useUnsavedChangesGuardApi,
  useRegisterUnsavedChanges,
} from './uiQuality/UnsavedChangesGuard'

const IMPL: { value: GapImplementationStatus; label: string }[] = [
  { value: 'FULLY_IMPLEMENTED', label: 'Fully Implemented' },
  { value: 'PARTIALLY_IMPLEMENTED', label: 'Partially Implemented' },
  { value: 'PLANNED', label: 'Planned' },
  { value: 'NOT_IMPLEMENTED', label: 'Not Implemented' },
]

function implStatusLabel(status: GapImplementationStatus | null | undefined): string {
  return IMPL.find((o) => o.value === status)?.label ?? 'Not set by Admin yet'
}

function isSamaTask(d: WorkTaskDetail): boolean {
  const code = (d.frameworkCode ?? '').toUpperCase()
  return code.includes('SAMA')
}

const TREAT: TreatmentOption[] = ['TREAT', 'TOLERATE', 'TERMINATE', 'TRANSFER']
const TREAT_LABEL = EXCEL_TREAT_LABEL
const TRANSFER_METHODS: TransferMethod[] = ['INSURANCE', 'OUTSOURCE', 'CONTRACT', 'OTHER']
const LEVELS: RiskLevel[] = ['VERY_LOW', 'LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH']

const EVIDENCE_ACCEPT = '.pdf,.docx,.xlsx,.png,.jpg,.jpeg'
const MAX_EVIDENCE_BYTES = 25 * 1024 * 1024

function riskContributorEvidenceStage(proposed: TreatmentOption | null | ''): string {
  if (proposed === 'TRANSFER') return 'TRANSFER'
  if (proposed === 'TREAT') return 'REVIEW'
  return 'REVIEW'
}

function isRiskTask(d: WorkTaskDetail): boolean {
  return (
    (d.task.sourceModule === 'RISK' &&
      d.task.sourceRecordType !== 'RISK_MILESTONE' &&
      d.task.sourceRecordType !== 'RISK_ACCEPTANCE') ||
    (d.riskRecord != null &&
      d.task.sourceRecordType !== 'RISK_MILESTONE' &&
      d.task.sourceRecordType !== 'RISK_ACCEPTANCE')
  )
}

function isAcceptanceTask(d: WorkTaskDetail): boolean {
  return d.task.sourceModule === 'RISK' && d.task.sourceRecordType === 'RISK_ACCEPTANCE'
}

function initialRiskWorkTab(d: WorkTaskDetail): 'details' | 'action' {
  return isAcceptanceTask(d) ? 'action' : 'details'
}

function isMilestoneTask(d: WorkTaskDetail): boolean {
  return d.task.sourceRecordType === 'RISK_MILESTONE' || d.treatmentMilestone != null
}

function isCapaTask(d: WorkTaskDetail): boolean {
  return d.task.sourceModule === 'CAPA' || d.capa != null
}

function isBiaTask(d: WorkTaskDetail): boolean {
  return d.task.sourceModule === 'BCMS' || d.bia != null
}

function isRopaTask(d: WorkTaskDetail): boolean {
  return (
    (d.task.sourceModule === 'PRIVACY' && d.task.sourceRecordType === 'PRIVACY_ROPA') ||
    d.privacyRopa != null
  )
}

function isDpiaTask(d: WorkTaskDetail): boolean {
  return (
    (d.task.sourceModule === 'PRIVACY' && d.task.sourceRecordType === 'PRIVACY_DPIA') ||
    d.privacyDpia != null
  )
}

function isPolicyTask(d: WorkTaskDetail): boolean {
  return d.task.sourceModule === 'POLICY' || d.policy != null
}

function isKriTask(d: WorkTaskDetail): boolean {
  return d.task.sourceModule === 'KRI' || d.kri != null
}

function isIncidentTask(d: WorkTaskDetail): boolean {
  return d.task.sourceModule === 'INCIDENT' || d.incident != null
}

function contributorTreatmentPlanText(risk: NonNullable<WorkTaskDetail['riskRecord']>): string {
  return risk.activeTreatmentPlan?.actionDescription || risk.treatmentPlan || ''
}

const RETURN_REVISION_REASON =
  'Update Treatment Action Plan Detail or add evidence to submit'

function returnedRiskNeedsRevision(
  risk: NonNullable<WorkTaskDetail['riskRecord']>,
  tapDetail: string,
  evidence: EvidenceSummary[],
): boolean {
  if (risk.status !== 'RE_OPENED') return false
  const detailsChanged = tapDetail.trim() !== contributorTreatmentPlanText(risk).trim()
  const since = risk.updatedAt
  const newEvidence = evidence.some(
    (e) =>
      !isIdentificationEvidence(e.workflowStage) &&
      Boolean(since) &&
      e.updatedAt >= since,
  )
  return !detailsChanged && !newEvidence
}

/** Department BIA status for My Tasks title card (not work-task ASSIGNED → Not started). */
function biaCycleStatusBadge(status: string): { label: string; bg: string; color: string } {
  const label =
    status === 'PENDING_BC_HEAD'
      ? 'Pending Business Continuity Head'
      : status.replace(/_/g, ' ').toUpperCase()
  if (status === 'CLOSED' || status === 'APPROVED' || status === 'READY_TO_CLOSE') {
    return { label, bg: semantic.gap.compliant.bg, color: semantic.gap.compliant.text }
  }
  if (status === 'RETURNED' || status === 'RETURNED_TO_ADMIN') {
    return { label, bg: semantic.riskChip.medium.bg, color: semantic.riskChip.medium.text }
  }
  if (
    status === 'SUBMITTED' ||
    status === 'PENDING_BC_HEAD' ||
    status === 'ASSIGNED' ||
    status === 'IN_PROGRESS'
  ) {
    return { label, bg: brand.goldLight, color: brand.goldDark }
  }
  return { label, bg: '#F3F4F6', color: '#6B7280' }
}

function AdminNoteField({
  label,
  value,
  field,
}: {
  label: string
  value: string
  /** Read-only field. `scroll` keeps a fixed height and scrolls long text. */
  field?: 'line' | 'scroll'
}) {
  const text = value.trim() || '-'
  return (
    <Box sx={{ minWidth: 0 }}>
      <Typography sx={{ ...sectionLabelSx, height: 'auto', mb: 0.5, lineHeight: 1.2, color: brand.goldDark, fontWeight: 700 }}>{label}</Typography>
      {field ? (
        <Box
          tabIndex={field === 'scroll' ? 0 : undefined}
          role="textbox"
          aria-readonly="true"
          aria-label={label}
          sx={{
            fontSize: '0.75rem',
            lineHeight: 1.4,
            color: text === '-' ? brand.muted : brand.ink,
            bgcolor: '#F5F5F4',
            border: `1px solid ${brand.border}`,
            borderRadius: '6px',
            px: 1,
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-word',
            ...(field === 'scroll'
              ? {
                  minHeight: 56,
                  maxHeight: 72,
                  overflowY: 'auto',
                  overflowX: 'hidden',
                  py: 0.75,
                }
              : {
                  minHeight: 32,
                  display: 'flex',
                  alignItems: 'center',
                  py: 0.5,
                }),
          }}
        >
          {text}
        </Box>
      ) : (
        <Typography sx={{ fontSize: '0.75rem', lineHeight: 1.35, color: brand.ink, whiteSpace: 'pre-wrap' }}>
          {text}
        </Typography>
      )}
    </Box>
  )
}

function displayTaskTitle(title: string | undefined, sourceRecordType?: string) {
  if (!title) return title
  if (sourceRecordType === 'BIA_HEAD') return title.replace('BIA Head review:', 'BIA Head Review:')
  return title
}

/** Contributor-only home: Admin chrome, assignment-scoped left nav, My Tasks work. */
export default function ContributorHome(props: {
  session: Session
  onLogout: () => void
}) {
  return (
    <UnsavedChangesGuardProvider>
      <ContributorHomeBody {...props} />
    </UnsavedChangesGuardProvider>
  )
}

function ContributorHomeBody({
  session,
  onLogout,
}: {
  session: Session
  onLogout: () => void
}) {
  const { requestNavigation } = useUnsavedChangesGuardApi()
  const { locale } = useTenantLocale()
  const [active, setActive] = useState(() => readNavParam() || '')
  const [appShellPage, setAppShellPage] = useState<AppShellPageConfig | null>(null)
  const [tasks, setTasks] = useState<WorkTaskSummary[]>([])
  const [ownedRiskNav, setOwnedRiskNav] = useState<string[]>([])
  const [workspaceReady, setWorkspaceReady] = useState(false)
  const [busy, setBusy] = useState(false)
  const [gapEvidenceBusy, setGapEvidenceBusy] = useState(false)
  const [gapSubmitting, setGapSubmitting] = useState(false)
  const [riskSubmitting, setRiskSubmitting] = useState(false)
  const [error, setErrorRaw] = useState('')
  const setError = useCallback((msg: unknown) => setErrorRaw(productErrorMessage(msg)), [])
  const [ok, setOk] = useState('')
  const [detail, setDetail] = useState<WorkTaskDetail | null>(null)
  const [capaNotifyOpen, setCapaNotifyOpen] = useState<{ capaId: number; epoch: number } | null>(null)
  const [maturityGates, setMaturityGates] = useState<GateAnswer[]>(emptyGates())
  const [observations, setObservations] = useState('')
  const [gapDescription, setGapDescription] = useState('')
  const [contributorComments, setContributorComments] = useState('')
  const [controlQuestions, setControlQuestions] = useState<FrameworkQuestion[]>([])
  const [customQuestions, setCustomQuestions] = useState<GapCustomQuestion[]>([])
  const [questionAnswers, setQuestionAnswers] = useState<
    Record<string, { answer: string; note: string }>
  >({})
  const [confirmTreatment, setConfirmTreatment] = useState(true)
  const [riskWorkTab, setRiskWorkTab] = useState<'details' | 'action'>('details')
  const [treatmentOption, setTreatmentOption] = useState<TreatmentOption | ''>('')
  const [justification, setJustification] = useState('')
  const [comments, setComments] = useState('')
  const [actionDescription, setActionDescription] = useState('')
  const [acceptanceJustification, setAcceptanceJustification] = useState('')
  const [businessRationale, setBusinessRationale] = useState('')
  const [conditionsOfAcceptance, setConditionsOfAcceptance] = useState('')
  const [compensatingControls, setCompensatingControls] = useState('')
  const [compControlEffectiveness, setCompControlEffectiveness] = useState<RiskLevel>('MEDIUM')
  const [reviewDate, setReviewDate] = useState('')
  const [contributorDeclaration, setContributorDeclaration] = useState(false)
  const [eliminationJustification, setEliminationJustification] = useState('')
  const [eliminationHow, setEliminationHow] = useState('')
  const [activityStopped, setActivityStopped] = useState(false)
  const [transferMethod, setTransferMethod] = useState<TransferMethod>('INSURANCE')
  const [thirdPartyName, setThirdPartyName] = useState('')
  const [transferDescription, setTransferDescription] = useState('')
  const [residualAfterTransfer, setResidualAfterTransfer] = useState('')
  const [transferCompensatingControls, setTransferCompensatingControls] = useState('')
  const [infosecStatusContributor, setInfosecStatusContributor] = useState('')
  const [ownerUserId, setOwnerUserId] = useState<number | ''>('')
  const [approver2UserId, setApprover2UserId] = useState<number | ''>('')
  const [approver3UserId, setApprover3UserId] = useState<number | ''>('')
  const [level3ApproverEnabled, setLevel3ApproverEnabled] = useState(false)
  const [acceptDecideComments, setAcceptDecideComments] = useState('')
  const [acceptDecideDeclared, setAcceptDecideDeclared] = useState(false)
  const [eligibleRiskContributors, setEligibleRiskContributors] = useState<
    { id: number; email: string; fullName: string }[]
  >([])
  const [directoryUsers, setDirectoryUsers] = useState<
    { id: number; email: string; fullName: string }[]
  >([])
  const [orgNameById, setOrgNameById] = useState<Record<number, string>>({})
  const [selfUserId, setSelfUserId] = useState<number | undefined>(undefined)
  useEffect(() => {
    void api.authMe().then((u) => setSelfUserId(u.id)).catch(() => setSelfUserId(undefined))
  }, [])
  const [linkedEvidence, setLinkedEvidence] = useState<EvidenceSummary[]>([])
  const [questionEvidence, setQuestionEvidence] = useState<Record<string, EvidenceSummary[]>>({})
  const [evidenceFile, setEvidenceFile] = useState<File | null>(null)
  const [milestoneProgressNotes, setMilestoneProgressNotes] = useState('')
  const [evidenceClass, setEvidenceClass] = useState<EvidenceClassification | ''>('')
  const [ropaEvidenceStage, setRopaEvidenceStage] = useState<'WORKBOOK_SUBMITTED' | 'APPROVAL_EVIDENCE'>(
    'WORKBOOK_SUBMITTED',
  )
  const [dpiaEvidenceStage, setDpiaEvidenceStage] = useState<'WORKBOOK_SUBMITTED' | 'APPROVAL_EVIDENCE'>(
    'WORKBOOK_SUBMITTED',
  )
  const [capaRootCause, setCapaRootCause] = useState('')
  const [capaCorrective, setCapaCorrective] = useState('')
  const [capaPreventive, setCapaPreventive] = useState('')
  const [capaNotes, setCapaNotes] = useState('')
  const [policyReadConfirmed, setPolicyReadConfirmed] = useState(false)
  const [kriValue, setKriValue] = useState('')
  const [kriNotes, setKriNotes] = useState('')
  const [incidentNotes, setIncidentNotes] = useState('')
  const [incidentResolve, setIncidentResolve] = useState(false)
  const [reportOpen, setReportOpen] = useState(false)
  const [sortAsc, setSortAsc] = useState(true)
  const [selectedAssignmentId, setSelectedAssignmentId] = useState<string | null>(null)
  const [gapWork, setGapWork] = useState<ContributorAssignment | null>(null)
  const [gapQuestionIndex, setGapQuestionIndex] = useState(0)
  const [discardOpen, setDiscardOpen] = useState(false)
  const [riskGridSize, setRiskGridSize] = useState<3 | 4 | 5>(5)
  // Track which project the user was viewing when they opened a risk (to return there on Close)
  const [riskProjectContext, setRiskProjectContext] = useState<{ projectId: number; riskType: 'IS_RISK' | 'ERM' | 'PRIVACY_RISK' } | null>(null)
  const [reportForm, setReportForm] = useState<ReportIncidentBody>({
    title: '',
    incidentType: 'SECURITY',
    description: '',
    occurredAt: null,
    affectedArea: '',
  })

  // --- Dirty leave guard: track the last saved form values ---
  const [cleanSnapshot, setCleanSnapshot] = useState<string | null>(null)
  const [cleanEpoch, setCleanEpoch] = useState(0)

  const currentFormSnapshot = useMemo(() => {
    if (!detail) return null
    return JSON.stringify({
      // Gap fields
      observations,
      gapDescription,
      contributorComments,
      questionAnswers,
      maturityGates,
      // Risk fields
      treatmentOption,
      justification,
      comments,
      actionDescription,
      acceptanceJustification,
      businessRationale,
      conditionsOfAcceptance,
      compensatingControls,
      compControlEffectiveness,
      reviewDate,
      eliminationJustification,
      eliminationHow,
      activityStopped,
      transferMethod,
      thirdPartyName,
      transferDescription,
      residualAfterTransfer,
      transferCompensatingControls,
      infosecStatusContributor,
      approver2UserId,
      approver3UserId,
      level3ApproverEnabled,
      // Milestone
      milestoneProgressNotes,
      // CAPA
      capaRootCause,
      capaCorrective,
      capaPreventive,
      capaNotes,
      // KRI
      kriValue,
      kriNotes,
      // Incident
      incidentNotes,
    })
  }, [
    detail,
    observations, gapDescription, contributorComments, questionAnswers, maturityGates,
    treatmentOption, justification, comments, actionDescription,
    acceptanceJustification, businessRationale, conditionsOfAcceptance,
    compensatingControls, compControlEffectiveness, reviewDate,
    eliminationJustification, eliminationHow, activityStopped,
    transferMethod, thirdPartyName, transferDescription,
    residualAfterTransfer, transferCompensatingControls,
    infosecStatusContributor, approver2UserId, approver3UserId, level3ApproverEnabled,
    milestoneProgressNotes,
    capaRootCause, capaCorrective, capaPreventive, capaNotes,
    kriValue, kriNotes, incidentNotes,
  ])

  const formDirty = useMemo(() => {
    if (!currentFormSnapshot || !cleanSnapshot) return false
    return currentFormSnapshot !== cleanSnapshot
  }, [currentFormSnapshot, cleanSnapshot])

  // Ref for save draft - allows useRegisterUnsavedChanges to call it even though it's defined later
  const saveDraftRef = useRef<(() => Promise<void>) | null>(null)

  const isGapTask = Boolean(detail?.gapItem)
  const isTaskSubmitted = detail?.task?.status === 'SUBMITTED'
  // Only activate dirty guard when actually editing a task detail (not on list views)
  const isEditingTask = Boolean(detail) && !isTaskSubmitted

  // Don't prompt for unsaved changes on list views or already-submitted tasks
  useRegisterUnsavedChanges(
    isEditingTask ? {
      dirty: formDirty,
      title: 'Leave without saving?',
      message: 'You have unsaved changes that will be lost.',
      onSaveDraft: isGapTask ? () => saveDraftRef.current?.() : undefined,
      saveDraftLabel: 'Save Draft',
    } : null
  )

  // Capture initial snapshot when opening a task detail (after state settles)
  const detailId = detail?.task?.id ?? null
  const snapshotRef = useRef(currentFormSnapshot)
  const workScrollRef = useRef<HTMLDivElement | null>(null)
  snapshotRef.current = currentFormSnapshot

  const settleTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  useEffect(() => {
    if (settleTimer.current) clearTimeout(settleTimer.current)
    if (detailId && !isTaskSubmitted) {
      // Small delay to let all state settle after async loads
      settleTimer.current = setTimeout(() => {
        setCleanSnapshot(snapshotRef.current)
      }, 150)
      return () => {
        if (settleTimer.current) clearTimeout(settleTimer.current)
      }
    }
    setCleanSnapshot(null)
  }, [detailId, isTaskSubmitted]) // Only re-run when task changes, not on every field edit

  useEffect(() => {
    if (cleanEpoch === 0) return
    if (settleTimer.current) clearTimeout(settleTimer.current)
    setCleanSnapshot(snapshotRef.current)
  }, [cleanEpoch])
  // --- End dirty leave guard ---

  const load = useCallback(async () => {
    setBusy(true)
    setError('')
    try {
      const [taskList, matrices, isOwned, ermOwned] = await Promise.all([
        api.listMyTasks(),
        api.getRiskMatrices().catch(() => null),
        api.listRisks('IS_RISK').catch(() => []),
        api.listRisks('ERM').catch(() => []),
      ])
      setTasks(taskList)
      const ownedNav: string[] = []
      if (isOwned.length > 0) ownedNav.push('risk')
      if (ermOwned.length > 0) ownedNav.push('erm-risk')
      setOwnedRiskNav(ownedNav)
      const n = Number(matrices?.gridSize)
      if (n === 3 || n === 4 || n === 5) setRiskGridSize(n)
    } catch (e) {
      setError(String(e))
    } finally {
      setBusy(false)
      setWorkspaceReady(true)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  const nav = useMemo(
    () => filterNavForContributor(CONTRIBUTOR_NAV_CATALOG, tasks, ownedRiskNav),
    [tasks, ownedRiskNav],
  )

  const [biaHomeNonce, setBiaHomeNonce] = useState(0)
  const goNav = useCallback(
    (id: string) => {
      requestNavigation(() => {
        const reenterBiaHome = id === active && id === 'bcms-bia'
        writeNavParam(id, 'push')
        setActive(id)
        setDetail(null)
        setSelectedAssignmentId(null)
        setGapWork(null)
        setCapaNotifyOpen(null)
        if (reenterBiaHome) setBiaHomeNonce((n) => n + 1)
      })
    },
    [active, requestNavigation],
  )

  useEffect(() => {
    if (!navContainsId(nav, active)) {
      const deep = readNavParam()
      if (deep && navContainsId(nav, deep)) {
        setActive(deep)
      } else {
        const first = firstContributorNavId(nav)
        setActive(first ?? '')
        writeNavParam(first ?? '', 'replace')
      }
    }
  }, [nav, active])

  useEffect(() => {
    const onPop = () => setActive(readNavParam() || firstContributorNavId(nav) || '')
    window.addEventListener('popstate', onPop)
    return () => window.removeEventListener('popstate', onPop)
  }, [])

  const scopedTasks = useMemo(
    () => (active === 'overview' ? tasks : tasks.filter((t) => taskMatchesContributorNav(t, active))),
    [tasks, active],
  )

  const listTabs = useMemo((): ContributorTab[] => {
    if (active === 'overview') return [...CONTRIBUTOR_TABS]
    if (active === 'assessments') return ['GAP']
    if (active === 'risk' || active === 'erm-risk') return ['RISK', 'RISK_ACCEPTANCE']
    if (active === 'audit-capa') return ['AUDIT']
    return ['ACTIVITY']
  }, [active])

  const openTaskSeq = useRef(0)
  const openTask = async (id: number) => {
    const seq = ++openTaskSeq.current
    try {
      const d = await api.getMyTask(id)
      setDetail(d)
      setRiskWorkTab(initialRiskWorkTab(d))
      setAcceptDecideComments('')
      setAcceptDecideDeclared(false)
      setGapQuestionIndex(0)
      setEvidenceFile(null)
      setEvidenceClass('')
      setLinkedEvidence([])
      setQuestionEvidence({})
      setPolicyReadConfirmed(false)
      setKriValue('')
      setKriNotes('')
      setIncidentNotes('')
      setIncidentResolve(false)
      setMilestoneProgressNotes(d.treatmentMilestone?.progressNotes ?? '')
      if (d.gapItem) {
        setMaturityGates(normalizeGates(d.gapItem.maturityGates))
        setObservations(d.gapItem.observations ?? '')
        setGapDescription(d.gapItem.gapDescription ?? '')
        setContributorComments(d.gapItem.contributorComments ?? '')
        setControlQuestions([])
        setCustomQuestions([])
        setQuestionAnswers({})
        try {
          setLinkedEvidence(
            await api.listEvidenceLinked('GAP', 'GAP_ITEM', String(d.gapItem.id)),
          )
        } catch {
          setLinkedEvidence([])
        }
        if (d.gapAssessmentId != null) {
          try {
            const [qs, ans, customQs] = await Promise.all([
              api.listGapItemQuestions(d.gapAssessmentId, d.gapItem.id),
              api.listGapItemAnswers(d.gapAssessmentId, d.gapItem.id),
              api.listGapCustomQuestions(d.gapAssessmentId, d.gapItem.id),
            ])
            setControlQuestions(qs)
            setCustomQuestions(customQs)
            const map: Record<string, { answer: string; note: string }> = {}
            for (const q of qs) {
              map[`c:${q.id}`] = { answer: '', note: '' }
            }
            for (const q of customQs) {
              map[`u:${q.id}`] = { answer: '', note: '' }
            }
            for (const a of ans) {
              if (a.questionId != null) {
                map[`c:${a.questionId}`] = { answer: a.answer ?? '', note: a.note ?? '' }
              }
              if (a.customQuestionId != null) {
                map[`u:${a.customQuestionId}`] = { answer: a.answer ?? '', note: a.note ?? '' }
              }
            }
            setQuestionAnswers(map)
            const keys = [
              ...qs.map((q) => `c:${q.id}`),
              ...customQs.map((q) => `u:${q.id}`),
            ]
            setQuestionEvidence(await loadGapQuestionEvidence(d.gapItem.id, keys))
          } catch {
            setControlQuestions([])
            setCustomQuestions([])
            setQuestionAnswers({})
            setQuestionEvidence({})
          }
        }
      }
      if (d.treatmentMilestone) {
        try {
          setLinkedEvidence(
            await api.listEvidenceLinked(
              'RISK',
              'RISK_MILESTONE',
              String(d.treatmentMilestone.id),
            ),
          )
        } catch {
          setLinkedEvidence([])
        }
      } else if (d.riskRecord) {
        setConfirmTreatment(true)
        setTreatmentOption(d.riskRecord.treatmentOption ?? '')
        setJustification('')
        setComments('')
        setActionDescription(contributorTreatmentPlanText(d.riskRecord))
        const acc = d.riskRecord.acceptanceForm
        setAcceptanceJustification(acc?.justification ?? '')
        setBusinessRationale(acc?.businessRationale ?? '')
        setConditionsOfAcceptance(acc?.conditionsOfAcceptance ?? '')
        setCompensatingControls(acc?.compensatingControls ?? '')
        setCompControlEffectiveness(acc?.compControlEffectiveness ?? 'MEDIUM')
        setReviewDate(acc?.reviewDate ?? '')
        setContributorDeclaration(false)
        setEliminationJustification('')
        setEliminationHow('')
        setActivityStopped(false)
        setTransferMethod('INSURANCE')
        setThirdPartyName('')
        setTransferDescription('')
        setResidualAfterTransfer('')
        setTransferCompensatingControls('')
        setInfosecStatusContributor(d.riskRecord.infosecStatusContributor ?? '')
        setOwnerUserId(acc?.approver1UserId ?? d.riskRecord.riskOwnerUserId ?? '')
        setApprover2UserId(acc?.approver2UserId ?? '')
        setApprover3UserId(acc?.approver3UserId ?? '')
        setLevel3ApproverEnabled(acc?.approver3UserId != null)
        setEligibleRiskContributors([])
        setDirectoryUsers([])
        try {
          setDirectoryUsers(await api.listUserDirectory())
        } catch {
          setDirectoryUsers([])
        }
        if (d.riskRecord.orgUnitId != null) {
          try {
            setEligibleRiskContributors(await api.listRiskEligibleContributors(d.riskRecord.orgUnitId))
          } catch {
            setEligibleRiskContributors([])
          }
        }
        try {
          const units = await api.listOrgUnits()
          const names: Record<number, string> = {}
          const walk = (nodes: { id: number; name: string; children?: unknown[] }[]) => {
            for (const n of nodes) {
              names[n.id] = n.name
              if (Array.isArray(n.children) && n.children.length) {
                walk(n.children as { id: number; name: string; children?: unknown[] }[])
              }
            }
          }
          walk(units)
          setOrgNameById(names)
        } catch {
          setOrgNameById({})
        }
        try {
          setLinkedEvidence(
            await api.listEvidenceLinked('RISK', 'RISK_RECORD', String(d.riskRecord.id)),
          )
        } catch {
          setLinkedEvidence([])
        }
      }
      if (d.capa) {
        setCapaRootCause(d.capa.rootCause ?? '')
        setCapaCorrective(d.capa.correctiveAction ?? '')
        setCapaPreventive(d.capa.preventiveAction ?? '')
        setCapaNotes(d.capa.progressNotes ?? '')
        try {
          setLinkedEvidence(
            await api.listEvidenceLinked('CAPA', 'CAPA', String(d.capa.id)),
          )
        } catch {
          setLinkedEvidence([])
        }
      }
      if (d.privacyRopa) {
        setRopaEvidenceStage('WORKBOOK_SUBMITTED')
        try {
          setLinkedEvidence(
            await api.listEvidenceLinked('PRIVACY', 'PRIVACY_ROPA', String(d.privacyRopa.id)),
          )
        } catch {
          setLinkedEvidence([])
        }
      }
      if (d.privacyDpia) {
        setDpiaEvidenceStage('WORKBOOK_SUBMITTED')
        try {
          setLinkedEvidence(
            await api.listEvidenceLinked('PRIVACY', 'PRIVACY_DPIA', String(d.privacyDpia.id)),
          )
        } catch {
          setLinkedEvidence([])
        }
      }
      if (d.kri) {
        setKriValue(d.kri.currentValue != null ? String(d.kri.currentValue) : '')
        setKriNotes('')
      }
      // Gap answers arrive after several requests; take the clean copy once the form is loaded.
      if (seq === openTaskSeq.current) setCleanEpoch((n) => n + 1)
    } catch (e) {
      setError(String(e))
    }
  }

  useEffect(() => {
    workScrollRef.current?.scrollTo({ top: 0 })
  }, [detail?.task.id])

  const openAssignedGap = async (projectId: number, gapAssessmentId: number | null) => {
    let gapTasks = tasks.filter(
      (t) => t.sourceModule === 'GAP' && t.sourceRecordType === 'GAP_ITEM',
    )
    if (gapAssessmentId != null) {
      try {
        const g = await api.getGapAssessment(gapAssessmentId)
        const itemIds = new Set((g.items ?? []).map((i) => i.id))
        gapTasks = gapTasks.filter((t) => itemIds.has(t.sourceRecordId))
      } catch (e) {
        setError(e)
        return
      }
    } else {
      gapTasks = gapTasks.filter((t) => t.projectId === projectId)
    }
    if (gapTasks.length === 0) {
      setError('No Gap controls assigned to you in this assessment.')
      return
    }
    const grouped = groupIntoAssignments(gapTasks, 'GAP')
    const assignment: ContributorAssignment = grouped[0] ?? {
      id: `gap-open:${projectId}:${gapAssessmentId ?? 'all'}`,
      tab: 'GAP',
      title: gapTasks[0]!.projectName?.trim() || gapTasks[0]!.title,
      subtitle: gapTasks[0]!.detail ?? '',
      assignerName: gapTasks[0]!.assignedByName ?? '',
      tasks: gapTasks,
    }
    setGapWork(assignment)
  }

  const openNotification = (n: NotificationItem) => {
    void (async () => {
      try {
        const target = await resolveGapNotificationTarget(n)
        if (target) {
          const task = tasks.find(
            (t) => t.sourceModule === 'GAP' && t.sourceRecordId === target.itemId,
          )
          requestNavigation(() => {
            writeNavParam('assessments', 'push')
            setActive('assessments')
            setSelectedAssignmentId(null)
            setGapWork(null)
          })
          if (task) {
            await openTask(task.id)
            return
          }
          await openAssignedGap(target.projectId, target.gapId)
          return
        }
      } catch (e) {
        setError(e)
      }
      try {
        const target = await resolveRiskNotificationTarget(n)
        if (target) {
          requestNavigation(() => {
            writeNavParam(target.navId, 'push')
            setActive(target.navId)
            setSelectedAssignmentId(null)
            setGapWork(null)
          })
          await openAssignedRisk(target.riskId, target.riskType, target.projectId)
          return
        }
      } catch (e) {
        setError(e)
      }
      try {
        const target = resolveCapaNotificationTarget(n)
        if (target) {
          requestNavigation(() => {
            writeNavParam('audit-capa', 'push')
            setActive('audit-capa')
            setDetail(null)
            setSelectedAssignmentId(null)
            setGapWork(null)
            setCapaNotifyOpen({ capaId: target.capaId, epoch: Date.now() })
          })
          return
        }
      } catch (e) {
        setError(e)
        return
      }
      if (n.link && n.link !== 'audits') goNav(n.link)
    })()
  }

  const openAssignedRisk = async (riskId: number, riskType: 'IS_RISK' | 'ERM' | 'PRIVACY_RISK', projectId: number | null) => {
    // Remember which project we came from so Close returns there
    setRiskProjectContext(projectId != null ? { projectId, riskType } : null)

    const typeOk = (t: WorkTaskSummary) => t.riskType === riskType || (riskType === 'IS_RISK' && t.riskType == null)
    const recordTasks = (list: WorkTaskSummary[]) =>
      list.filter(
        (t) =>
          t.sourceModule === 'RISK' &&
          typeOk(t) &&
          (t.sourceRecordType === 'RISK_RECORD' || t.sourceRecordType === 'RISK_ACCEPTANCE') &&
          t.sourceRecordId === riskId,
      )
    let riskTasks = recordTasks(tasks)
    if (riskTasks.length === 0) {
      // The register can show a new assignment before this page's task list reloads.
      try {
        const [fresh, risk] = await Promise.all([
          api.listMyTasks(),
          api.getRisk(riskId).catch(() => null),
        ])
        setTasks(fresh)
        riskTasks = recordTasks(fresh)
        if (riskTasks.length === 0 && risk) {
          const milestoneIds = new Set((risk.activeTreatmentPlan?.milestones ?? []).map((m) => m.id))
          riskTasks = fresh.filter(
            (t) =>
              t.sourceModule === 'RISK' &&
              t.sourceRecordType === 'RISK_MILESTONE' &&
              milestoneIds.has(t.sourceRecordId),
          )
        }
      } catch (e) {
        setError(e)
        return false
      }
    }
    const actionable = riskTasks.filter((t) => t.status !== 'SUBMITTED')
    const pool = actionable.length > 0 ? actionable : riskTasks
    if (pool.length === 0) {
      return false
    }
    const preferred =
      pool.find((t) => t.sourceRecordType === 'RISK_ACCEPTANCE') ??
      pool.find((t) => t.sourceRecordType === 'RISK_RECORD') ??
      pool[0]!
    await openTask(preferred.id)
    return true
  }

  const submitBia = async () => {
    if (!detail?.bia) return
    setBusy(true)
    setError('')
    try {
      const d = await api.submitBiaTask(detail.task.id)
      setDetail(d)
      setOk('BIA submitted for Admin review')
      await load()
    } catch (e) {
      setError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const submitRopa = async () => {
    if (!detail?.privacyRopa) return
    const hasWorkbook = linkedEvidence.some((e) => e.workflowStage === 'WORKBOOK_SUBMITTED')
    const hasApproval = linkedEvidence.some((e) => e.workflowStage === 'APPROVAL_EVIDENCE')
    if (!hasWorkbook || !hasApproval) {
      setError(
        'Attach filled workbook (WORKBOOK_SUBMITTED) and approval evidence (APPROVAL_EVIDENCE) before submit',
      )
      return
    }
    setBusy(true)
    setError('')
    try {
      const d = await api.submitRopaTask(detail.task.id)
      setDetail(d)
      setOk('RoPA workbook package submitted for Admin review')
      await load()
    } catch (e) {
      setError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const uploadRopaEvidence = async () => {
    if (!detail?.privacyRopa || !evidenceFile) return
    if (!evidenceClass) {
      setError('Select a classification')
      return
    }
    setBusy(true)
    setError('')
    try {
      await api.uploadEvidence({
        file: evidenceFile,
        classification: evidenceClass,
        title: evidenceFile.name,
        moduleCode: 'PRIVACY',
        recordType: 'PRIVACY_ROPA',
        recordId: String(detail.privacyRopa.id),
        workflowStage: ropaEvidenceStage,
      })
      setEvidenceFile(null)
      setLinkedEvidence(
        await api.listEvidenceLinked('PRIVACY', 'PRIVACY_ROPA', String(detail.privacyRopa.id)),
      )
      setOk('Evidence linked')
    } catch (e) {
      setError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const submitDpia = async () => {
    if (!detail?.privacyDpia) return
    const hasWorkbook = linkedEvidence.some((e) => e.workflowStage === 'WORKBOOK_SUBMITTED')
    const hasApproval = linkedEvidence.some((e) => e.workflowStage === 'APPROVAL_EVIDENCE')
    if (!hasWorkbook || !hasApproval) {
      setError(
        'Attach filled workbook (WORKBOOK_SUBMITTED) and approval evidence (APPROVAL_EVIDENCE) before submit',
      )
      return
    }
    setBusy(true)
    setError('')
    try {
      const d = await api.submitDpiaTask(detail.task.id)
      setDetail(d)
      setOk('DPIA workbook package submitted for Admin review')
      await load()
    } catch (e) {
      setError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const uploadDpiaEvidence = async () => {
    if (!detail?.privacyDpia || !evidenceFile) return
    if (!evidenceClass) {
      setError('Select a classification')
      return
    }
    setBusy(true)
    setError('')
    try {
      await api.uploadEvidence({
        file: evidenceFile,
        classification: evidenceClass,
        title: evidenceFile.name,
        moduleCode: 'PRIVACY',
        recordType: 'PRIVACY_DPIA',
        recordId: String(detail.privacyDpia.id),
        workflowStage: dpiaEvidenceStage,
      })
      setEvidenceFile(null)
      setLinkedEvidence(
        await api.listEvidenceLinked('PRIVACY', 'PRIVACY_DPIA', String(detail.privacyDpia.id)),
      )
      setOk('Evidence linked')
    } catch (e) {
      setError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const gapStepQuestions = useMemo(
    () => [
      ...controlQuestions.map((q) => ({ key: `c:${q.id}`, questionText: q.questionText })),
      ...customQuestions.map((q) => ({ key: `u:${q.id}`, questionText: q.questionText })),
    ],
    [controlQuestions, customQuestions],
  )

  const gapQuestionsReason = gapStepQuestions.some((q) => !(questionAnswers[q.key]?.answer ?? '').trim())
    ? 'Select an assessment on each question'
    : gapStepQuestions.some((q) => !(questionAnswers[q.key]?.note ?? '').trim())
      ? 'Your response is required on each question'
      : ''
  const gapAssessmentComplete = gapStepQuestions.length === 0 || gapQuestionsReason === ''
  const currentGapKey = gapStepQuestions[gapQuestionIndex]?.key
  const currentGapAnswer = (questionAnswers[currentGapKey ?? '']?.answer ?? '').trim()
  const currentGapNote = (questionAnswers[currentGapKey ?? '']?.note ?? '').trim()
  const currentGapStepReason = !currentGapKey
    ? ''
    : !currentGapAnswer
      ? 'Select an assessment to continue'
      : !currentGapNote
        ? 'Your response is required'
        : gapContributorTextIssue(currentGapNote) ?? ''

  const currentQuestionEvidence =
    gapStepQuestions[gapQuestionIndex] != null
      ? (questionEvidence[gapStepQuestions[gapQuestionIndex].key] ?? [])
      : linkedEvidence

  const buildGapAnswerPayload = () => [
    ...controlQuestions.map((q) => ({
      questionId: q.id,
      customQuestionId: null as number | null,
      answer: questionAnswers[`c:${q.id}`]?.answer || null,
      note: questionAnswers[`c:${q.id}`]?.note || null,
    })),
    ...customQuestions.map((q) => ({
      questionId: null as number | null,
      customQuestionId: q.id,
      answer: questionAnswers[`u:${q.id}`]?.answer || null,
      note: questionAnswers[`u:${q.id}`]?.note || null,
    })),
  ]

  const gapCommentIssue = firstGapCommentIssue(
    contributorComments,
    ...Object.values(questionAnswers).map((row) => row.note),
  )
  const gapSubmitReason = !detail
    ? ''
    : isSamaTask(detail) && !gatesComplete(maturityGates)
      ? 'Complete the maturity gates'
      : gapStepQuestions.length > 0 && !gapAssessmentComplete
        ? gapQuestionsReason
        : gapStepQuestions.length === 0 && !contributorComments.trim()
          ? 'Add a response before submitting'
          : gapCommentIssue || ''

  const uploadGapEvidence = async () => {
    if (!detail?.gapItem || !evidenceFile) return
    if (!evidenceClass) {
      setError('Select a classification')
      return
    }
    if (evidenceFile.size > RISK_EVIDENCE_MAX_BYTES) {
      setError('File is too large. Maximum size is 10MB.')
      return
    }
    const currentQuestion = gapStepQuestions[gapQuestionIndex]
    const recordType = currentQuestion ? 'GAP_QUESTION' : 'GAP_ITEM'
    const recordId = currentQuestion
      ? gapQuestionRecordId(detail.gapItem.id, currentQuestion.key)
      : String(detail.gapItem.id)
    const onRecord = currentQuestion ? currentQuestionEvidence : linkedEvidence
    const mine = onRecord.filter((e) => selfUserId != null && e.createdByUserId === selfUserId).length
    if (mine >= RISK_EVIDENCE_MAX_FILES) {
      setError(
        `You can attach at most ${RISK_EVIDENCE_MAX_FILES} files to ${currentQuestion ? 'this question' : 'this control'}`,
      )
      return
    }
    setGapEvidenceBusy(true)
    setError('')
    try {
      await api.uploadEvidence({
        file: evidenceFile,
        classification: evidenceClass,
        title: evidenceFile.name,
        moduleCode: 'GAP',
        recordType,
        recordId,
        workflowStage: 'CONTRIBUTOR_SUBMIT',
      })
      setEvidenceFile(null)
      if (currentQuestion) {
        const list = await api.listEvidenceLinked('GAP', 'GAP_QUESTION', recordId)
        setQuestionEvidence((prev) => ({ ...prev, [currentQuestion.key]: list }))
        setOk('Evidence uploaded for this question')
      } else {
        setLinkedEvidence(
          await api.listEvidenceLinked('GAP', 'GAP_ITEM', String(detail.gapItem.id)),
        )
        setOk('Evidence uploaded and linked to this control')
      }
    } catch (e) {
      setError(String(e))
    } finally {
      setGapEvidenceBusy(false)
    }
  }

  const unlinkAndRefresh = async (
    ev: EvidenceSummary,
    moduleCode: string,
    recordType: string,
    recordId: string,
    refresh: () => Promise<void>,
  ) => {
    setBusy(true)
    setError('')
    try {
      await api.unlinkEvidenceFromRecord(ev.id, { moduleCode, recordType, recordId })
      await refresh()
      setOk('Evidence removed')
    } catch (e) {
      setError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const removeGapEvidence = async (ev: EvidenceSummary) => {
    if (!detail?.gapItem) return
    const itemId = detail.gapItem.id
    const currentQuestion = gapStepQuestions[gapQuestionIndex]
    const recordType = currentQuestion ? 'GAP_QUESTION' : 'GAP_ITEM'
    const recordId = currentQuestion
      ? gapQuestionRecordId(itemId, currentQuestion.key)
      : String(itemId)
    await unlinkAndRefresh(ev, 'GAP', recordType, recordId, async () => {
      if (currentQuestion) {
        const list = await api.listEvidenceLinked('GAP', 'GAP_QUESTION', recordId)
        setQuestionEvidence((prev) => ({ ...prev, [currentQuestion.key]: list }))
      } else {
        setLinkedEvidence(await api.listEvidenceLinked('GAP', 'GAP_ITEM', String(itemId)))
      }
    })
  }

  const submitGap = async () => {
    if (!detail || !detail.gapItem || detail.gapAssessmentId == null) return
    const sama = isSamaTask(detail)
    if (sama && !gatesComplete(maturityGates)) return
    if (gapStepQuestions.length > 0 && !gapAssessmentComplete) {
      setError(gapQuestionsReason)
      return
    }
    if (gapStepQuestions.length === 0 && !contributorComments.trim()) {
      setError('Add a response before submitting')
      return
    }
    if (gapCommentIssue) {
      setError(gapCommentIssue)
      return
    }
    setGapSubmitting(true)
    setError('')
    try {
      if (gapStepQuestions.length > 0) {
        await api.upsertGapItemAnswers(
          detail.gapAssessmentId,
          detail.gapItem.id,
          buildGapAnswerPayload(),
        )
      }
      const d = await api.submitGapTask(detail.task.id, {
        ...(sama ? { maturityGates } : {}),
        ...(gapStepQuestions.length === 0
          ? { contributorComments: contributorComments.trim() }
          : {}),
      })
      setDetail(d)
      if (d.gapItem) setMaturityGates(normalizeGates(d.gapItem.maturityGates))
      setOk('Task submitted: Admin will review final status')
      await load()
    } catch (e) {
      setError(String(e))
    } finally {
      setGapSubmitting(false)
    }
  }

  const saveGapDraft = async () => {
    if (!detail?.gapItem || detail.gapAssessmentId == null) return false
    if (gapCommentIssue) {
      setError(gapCommentIssue)
      return false
    }
    const sama = isSamaTask(detail)
    setBusy(true)
    setError('')
    try {
      if (gapStepQuestions.length > 0) {
        await api.upsertGapItemAnswers(
          detail.gapAssessmentId,
          detail.gapItem.id,
          buildGapAnswerPayload(),
        )
      }
      const d = await api.saveGapDraft(detail.task.id, {
        ...(sama ? { maturityGates } : {}),
        ...(gapStepQuestions.length === 0
          ? { contributorComments: contributorComments.trim() || null }
          : {}),
      })
      setDetail(d)
      if (d.gapItem) setMaturityGates(normalizeGates(d.gapItem.maturityGates))
      setOk('Draft saved')
      await load()
      setCleanEpoch((n) => n + 1)
      return true
    } catch (e) {
      setError(String(e))
      return false
    } finally {
      setBusy(false)
    }
  }

  // Connect save draft ref for dirty leave guard
  saveDraftRef.current = async () => {
    await saveGapDraft()
  }

  const submitMilestone = async () => {
    if (!detail?.treatmentMilestone) return
    if (linkedEvidence.length === 0) {
      setError('Upload at least one evidence file before completing this milestone')
      return
    }
    setBusy(true)
    setError('')
    try {
      const d = await api.submitMilestoneTask(detail.task.id, {
        progressNotes: milestoneProgressNotes.trim() || null,
      })
      setDetail(d)
      setOk('Milestone completed')
      await load()
    } catch (e) {
      setError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const uploadRiskEvidence = async () => {
    if (!detail?.riskRecord || !evidenceFile) return
    if (!evidenceClass) {
      setError('Select a classification')
      return
    }
    if (evidenceFile.size > RISK_EVIDENCE_MAX_BYTES) {
      setError('File is too large. Maximum size is 10MB.')
      return
    }
    const mine = linkedEvidence.filter((e) => meId != null && e.createdByUserId === meId).length
    if (mine >= RISK_EVIDENCE_MAX_FILES) {
      setError('You can attach at most 5 files to this risk')
      return
    }
    setBusy(true)
    setError('')
    try {
      const proposed = (treatmentOption || detail.riskRecord.treatmentOption) as TreatmentOption | null
      await api.uploadEvidence({
        file: evidenceFile,
        classification: evidenceClass,
        title: evidenceFile.name,
        moduleCode: 'RISK',
        recordType: 'RISK_RECORD',
        recordId: String(detail.riskRecord.id),
        workflowStage: riskContributorEvidenceStage(proposed),
      })
      setEvidenceFile(null)
      setLinkedEvidence(
        await api.listEvidenceLinked('RISK', 'RISK_RECORD', String(detail.riskRecord.id)),
      )
      setOk('Evidence uploaded and linked to this risk')
    } catch (e) {
      setError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const uploadMilestoneEvidence = async () => {
    if (!detail?.treatmentMilestone || !evidenceFile) return
    if (!evidenceClass) {
      setError('Select a classification')
      return
    }
    if (evidenceFile.size > RISK_EVIDENCE_MAX_BYTES) {
      setError('File is too large. Maximum size is 10MB.')
      return
    }
    const mine = linkedEvidence.filter((e) => meId != null && e.createdByUserId === meId).length
    if (mine >= RISK_EVIDENCE_MAX_FILES) {
      setError('You can attach at most 5 files to this risk')
      return
    }
    setBusy(true)
    setError('')
    try {
      await api.uploadEvidence({
        file: evidenceFile,
        classification: evidenceClass,
        title: evidenceFile.name,
        moduleCode: 'RISK',
        recordType: 'RISK_MILESTONE',
        recordId: String(detail.treatmentMilestone.id),
        workflowStage: 'TREATMENT',
      })
      setEvidenceFile(null)
      setLinkedEvidence(
        await api.listEvidenceLinked(
          'RISK',
          'RISK_MILESTONE',
          String(detail.treatmentMilestone.id),
        ),
      )
      setOk('Evidence uploaded and linked to this milestone')
    } catch (e) {
      setError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const uploadCapaEvidence = async () => {
    if (!detail?.capa || !evidenceFile) return
    if (!evidenceClass) {
      setError('Select a classification')
      return
    }
    if (evidenceFile.size > MAX_EVIDENCE_BYTES) {
      setError('Evidence file must be 25MB or less')
      return
    }
    setBusy(true)
    setError('')
    try {
      await api.uploadEvidence({
        file: evidenceFile,
        classification: evidenceClass,
        title: evidenceFile.name,
        moduleCode: 'CAPA',
        recordType: 'CAPA',
        recordId: String(detail.capa.id),
        workflowStage: 'IN_PROGRESS',
      })
      setEvidenceFile(null)
      setLinkedEvidence(
        await api.listEvidenceLinked('CAPA', 'CAPA', String(detail.capa.id)),
      )
      setOk('Evidence uploaded and linked to this CAPA')
    } catch (e) {
      setError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const submitRisk = async () => {
    if (!detail?.riskRecord) return
    const proposed = (treatmentOption || detail.riskRecord.treatmentOption) as TreatmentOption | null
    const changed =
      !confirmTreatment ||
      (proposed != null && proposed !== detail.riskRecord.treatmentOption)
    if (changed && !justification.trim()) {
      setError('Justification required when changing or rejecting the treatment option')
      return
    }
    const treatNeedsEvidence = Boolean(
      detail.riskRecord.activeTreatmentPlan?.evidenceRequired?.trim(),
    )
    const acceptanceLoop =
      detail.riskRecord.riskProjectId != null && detail.riskRecord.status === 'PENDING_ACCEPTANCE'
    if (detail.riskRecord.riskProjectId != null && !acceptanceLoop && !actionDescription.trim()) {
      setError('Treatment Action Plan Detail is required')
      return
    }
    if (returnedRiskNeedsRevision(detail.riskRecord, actionDescription, linkedEvidence)) {
      setError(RETURN_REVISION_REASON)
      return
    }
    if (
      ((acceptanceLoop === false && treatNeedsEvidence) ||
        (detail.riskRecord.riskProjectId == null &&
          (proposed === 'TRANSFER' ||
            proposed === 'TERMINATE' ||
            (proposed === 'TREAT' && treatNeedsEvidence)))) &&
      !linkedEvidence.some((e) => !isIdentificationEvidence(e.workflowStage))
    ) {
        setError(
          proposed === 'TREAT' || treatNeedsEvidence
            ? 'Upload evidence required by the treatment plan before submitting'
            : 'Upload at least one evidence file before submitting Terminate/Transfer',
        )
        return
    }
    if (!acceptanceLoop && proposed === 'TOLERATE' && detail.riskRecord.riskProjectId == null && !contributorDeclaration) {
      setError('Contributor declaration is required for Tolerate')
      return
    }
    if (acceptanceLoop && !contributorDeclaration) {
      setError('Contributor declaration is required for Risk Acceptance')
      return
    }
    if (needsAcceptanceForm && tolerateReviewDateError) {
      setError(tolerateReviewDateError)
      return
    }
    if (acceptanceLoop) {
      if (!acceptanceJustification.trim() || !compensatingControls.trim()) {
        setError('Complete Section 2 of the Risk Acceptance form')
        return
      }
      const level2Label = acceptanceLevel2Label(detail.riskRecord.riskType)
      if (approver2UserId === '') {
        setError(`Select ${level2Label}`)
        return
      }
      if (level3ApproverEnabled && approver3UserId === '') {
        setError('Select Level 3 approver')
        return
      }
      if (ownerUserId === '') {
        setError('Select Risk Owner')
        return
      }
      const ownerId = ownerUserId
      const custodianId = detail.riskRecord.assignedContributorId
      if (approver2UserId === ownerId || approver2UserId === custodianId) {
        setError(`${level2Label} cannot be the Risk Owner or Risk Custodian`)
        return
      }
      if (
        level3ApproverEnabled &&
        approver3UserId !== '' &&
        approver3UserId === approver2UserId
      ) {
        setError(`${level2Label} and Level 3 approver must be different users`)
        return
      }
      if (!contributorDeclaration) {
        setError('Contributor declaration is required')
        return
      }
    }
    setBusy(true)
    setRiskSubmitting(true)
    setError('')
    try {
      const d = await api.submitRiskTask(detail.task.id, {
        confirmTreatment,
        treatmentOption: proposed,
        justification: justification.trim() || null,
        comments: comments.trim() || null,
        actionDescription: actionDescription.trim() || null,
        acceptanceJustification: acceptanceJustification.trim() || null,
        businessRationale: businessRationale.trim() || null,
        conditionsOfAcceptance: acceptanceLoop ? null : conditionsOfAcceptance.trim() || null,
        compensatingControls: compensatingControls.trim() || null,
        compControlOwnerId: acceptanceLoop ? null : detail.riskRecord.riskOwnerUserId,
        compControlEffectiveness: acceptanceLoop ? null : compControlEffectiveness,
        reviewDate: reviewDate || null,
        contributorDeclaration,
        eliminationJustification: eliminationJustification.trim() || null,
        eliminationHow: eliminationHow.trim() || null,
        activityStopped,
        transferMethod: proposed === 'TRANSFER' ? transferMethod : null,
        thirdPartyName: thirdPartyName.trim() || null,
        transferDescription: transferDescription.trim() || null,
        residualAfterTransfer: residualAfterTransfer.trim() || null,
        transferCompensatingControls: transferCompensatingControls.trim() || null,
        infosecStatusContributor: infosecStatusContributor.trim() || null,
        approver2UserId: acceptanceLoop && approver2UserId !== '' ? approver2UserId : null,
        riskOwnerUserId: acceptanceLoop && ownerUserId !== '' ? ownerUserId : null,
        approver3UserId:
          acceptanceLoop && level3ApproverEnabled && approver3UserId !== '' ? approver3UserId : null,
      })
      setDetail(d)
      setOk('Risk task submitted: Admin will validate')
      await load()
    } catch (e) {
      setError(String(e))
    } finally {
      setRiskSubmitting(false)
      setBusy(false)
    }
  }

  const decideAcceptance = async (decision: 'APPROVED' | 'REJECTED') => {
    if (!detail?.riskRecord) return
    if (!acceptDecideDeclared) {
      setError('Declaration is required')
      return
    }
    if (decision === 'REJECTED' && !acceptDecideComments.trim()) {
      setError('Reject reason is required')
      return
    }
    setBusy(true)
    setError('')
    const form = detail.riskRecord.acceptanceForm
    const st = form?.status ?? ''
    const level2Label = acceptanceLevel2Label(detail.riskRecord.riskType)
    const ciso =
      form?.approver2Name?.trim() ||
      (form?.approver2UserId != null ? `User #${form.approver2UserId}` : level2Label)
    const hasLevel3 = form?.approver3UserId != null
    const level3Name =
      form?.approver3Name?.trim() ||
      (form?.approver3UserId != null ? `User #${form.approver3UserId}` : 'Level 3 approver')
    try {
      await api.ownerDecideRiskAcceptance(detail.riskRecord.id, {
        decision,
        comments: acceptDecideComments.trim() || null,
        declaration: true,
      })
      setDetail(null)
      let ok = 'Rejected: returned to Contributor'
      if (decision === 'APPROVED') {
        if (st === 'PENDING_RISK_OWNER') {
          ok = hasLevel3
            ? `Approved: sent to ${level2Label}: ${ciso}. Level 3: ${level3Name}`
            : `Approved: sent to ${level2Label}: ${ciso}. Next: Admin`
        } else if (st === 'PENDING_APPROVER_2') {
          ok = hasLevel3 ? `Approved: sent to Level 3: ${level3Name}` : 'Approved: sent to Admin'
        } else if (st === 'PENDING_APPROVER_3') {
          ok = 'Approved: sent to Admin'
        } else {
          ok = 'Approved: sent to the next approver'
        }
      }
      setOk(ok)
      await load()
    } catch (e) {
      setError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const acknowledgePolicy = async () => {
    if (!detail?.policy || !policyReadConfirmed) return
    setBusy(true)
    setError('')
    try {
      const d = await api.acknowledgePolicyTask(detail.task.id)
      setDetail(d)
      setOk('Policy acknowledged')
      await load()
    } catch (e) {
      setError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const submitKri = async () => {
    if (!detail?.kri) return
    if (kriValue.trim() === '') {
      setError('KRI value is required')
      return
    }
    setBusy(true)
    setError('')
    try {
      const d = await api.submitKriTask(detail.task.id, {
        value: Number(kriValue),
        notes: kriNotes.trim() || null,
      })
      setDetail(d)
      setOk(`KRI value recorded: RAG ${d.kri?.rag ?? 'n/a'}`)
      await load()
    } catch (e) {
      setError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const submitIncident = async () => {
    if (!detail?.incident) return
    setBusy(true)
    setError('')
    try {
      const d = await api.submitIncidentTask(detail.task.id, {
        progressNotes: incidentNotes.trim() || null,
        markResolved: incidentResolve,
      })
      setDetail(d)
      setOk(incidentResolve ? 'Incident resolved' : 'Incident progress saved')
      await load()
    } catch (e) {
      setError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const reportIncident = async () => {
    if (!reportForm.title.trim()) return
    setBusy(true)
    setError('')
    try {
      const created = await api.reportIncident({
        ...reportForm,
        title: reportForm.title.trim(),
        description: reportForm.description?.trim() || null,
        affectedArea: reportForm.affectedArea?.trim() || null,
      })
      setReportOpen(false)
      setReportForm({
        title: '',
        incidentType: 'SECURITY',
        description: '',
        occurredAt: null,
        affectedArea: '',
      })
      setOk(`Reported ${created.displayId}`)
      await load()
    } catch (e) {
      setError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const submitCapa = async () => {
    if (!detail?.capa) return
    if (!capaRootCause.trim() || !capaCorrective.trim()) {
      setError(`${CAPA_EXCEL.rootCause} and ${CAPA_EXCEL.correctiveActionPlan} are required`)
      return
    }
    if (linkedEvidence.length === 0) {
      setError('Upload at least one evidence file before submitting CAPA')
      return
    }
    setBusy(true)
    setError('')
    try {
      const d = await api.submitCapaTask(detail.task.id, {
        rootCause: capaRootCause.trim(),
        correctiveAction: capaCorrective.trim(),
        preventiveAction: capaPreventive.trim() || null,
        progressNotes: capaNotes.trim() || null,
      })
      setDetail(d)
      setOk('CAPA submitted for Admin verification')
      await load()
    } catch (e) {
      setError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const milestoneMode = detail != null && isMilestoneTask(detail)
  const riskMode = detail != null && !milestoneMode && (detail.riskRecord != null || isRiskTask(detail) || isAcceptanceTask(detail))
  const acceptanceApproveMode = detail != null && isAcceptanceTask(detail)
  const capaMode = detail != null && isCapaTask(detail)
  const biaMode = detail != null && isBiaTask(detail)
  const ropaMode = detail != null && isRopaTask(detail)
  const dpiaMode = detail != null && isDpiaTask(detail)
  const policyMode = detail != null && isPolicyTask(detail)
  const kriMode = detail != null && isKriTask(detail)
  const incidentMode = detail != null && isIncidentTask(detail)

  const selectedAssignment = useMemo(() => {
    if (gapWork) return gapWork
    if (!selectedAssignmentId) return null
    for (const tab of ['GAP', 'RISK', 'RISK_ACCEPTANCE', 'AUDIT', 'ACTIVITY'] as ContributorTab[]) {
      const hit = groupIntoAssignments(scopedTasks, tab).find((a) => a.id === selectedAssignmentId)
      if (hit) return hit
    }
    return null
  }, [gapWork, scopedTasks, selectedAssignmentId])

  const proposedTreatment = detail?.riskRecord
    ? ((treatmentOption || detail.riskRecord.treatmentOption) as TreatmentOption | null)
    : null
  const isProjectRisk = Boolean(detail?.riskRecord?.riskProjectId)
  const riskPickerUsers = directoryUsers.length > 0 ? directoryUsers : eligibleRiskContributors
  const meId = riskPickerUsers.find((u) => u.email === session.email)?.id
  const needsAcceptanceForm = isProjectRisk
    ? detail?.riskRecord?.status === 'PENDING_ACCEPTANCE'
    : proposedTreatment === 'TOLERATE'
  const tolerateReviewDateError = useMemo(() => {
    if (!needsAcceptanceForm) return ''
    return toleranceReviewDateError(reviewDate, true)
  }, [needsAcceptanceForm, reviewDate])
  const reviewDateMin = todayIso()
  const reviewDateMax = maxToleranceReviewIso()

  const taskDirty = useMemo(() => {
    if (!detail || detail.task.status === 'SUBMITTED') return false
    if (detail.gapItem && !riskMode && !capaMode && !biaMode && !ropaMode && !dpiaMode && !policyMode && !kriMode && !incidentMode) {
      return formDirty || evidenceFile != null
    }
    if (detail.riskRecord && !milestoneMode) {
      const proposed = treatmentOption || detail.riskRecord.treatmentOption
      return (
        justification.trim() !== '' ||
        comments.trim() !== '' ||
        actionDescription !== contributorTreatmentPlanText(detail.riskRecord) ||
        infosecStatusContributor !== (detail.riskRecord.infosecStatusContributor ?? '') ||
        evidenceFile != null ||
        !confirmTreatment ||
        proposed !== detail.riskRecord.treatmentOption ||
        acceptanceJustification.trim() !== '' ||
        businessRationale.trim() !== '' ||
        compensatingControls.trim() !== '' ||
        eliminationJustification.trim() !== '' ||
        transferDescription.trim() !== ''
      )
    }
    if (detail.treatmentMilestone && milestoneMode) {
      return (
        milestoneProgressNotes !== (detail.treatmentMilestone.progressNotes ?? '') ||
        evidenceFile != null
      )
    }
    if (detail.capa) {
      return (
        capaRootCause !== (detail.capa.rootCause ?? '') ||
        capaCorrective !== (detail.capa.correctiveAction ?? '') ||
        capaPreventive !== (detail.capa.preventiveAction ?? '') ||
        capaNotes !== (detail.capa.progressNotes ?? '') ||
        evidenceFile != null
      )
    }
    if (detail.bia) {
      return false
    }
    if (detail.kri) {
      return kriValue.trim() !== '' || kriNotes.trim() !== ''
    }
    if (detail.incident) {
      return incidentNotes.trim() !== '' || incidentResolve
    }
    if (detail.policy) {
      return policyReadConfirmed
    }
    return false
  }, [
    detail,
    formDirty,
    riskMode,
    milestoneMode,
    capaMode,
    biaMode,
    policyMode,
    kriMode,
    incidentMode,
    observations,
    gapDescription,
    contributorComments,
    evidenceFile,
    questionAnswers,
    maturityGates,
    justification,
    comments,
    actionDescription,
    infosecStatusContributor,
    confirmTreatment,
    treatmentOption,
    acceptanceJustification,
    businessRationale,
    compensatingControls,
    eliminationJustification,
    transferDescription,
    capaRootCause,
    capaCorrective,
    capaPreventive,
    capaNotes,
    milestoneProgressNotes,
    kriValue,
    kriNotes,
    incidentNotes,
    incidentResolve,
    policyReadConfirmed,
  ])

  useBeforeUnload(taskDirty)
  useEffect(() => {
    const title = displayTaskTitle(detail?.task.title, detail?.task.sourceRecordType)
      || contributorNavTitle(active)
    setArborTitle(title)
  }, [detail?.task.title, detail?.task.sourceRecordType, active])

  const requestBackFromDetail = () => {
    requestNavigation(() => {
      if (taskDirty) {
        setDiscardOpen(true)
        return
      }
      setDetail(null)
    })
  }

  const requestCloseTask = () => {
    requestNavigation(() => {
      if (taskDirty) {
        setDiscardOpen(true)
        return
      }
      setDetail(null)
    })
  }

  const riskSubmitReason = (() => {
    if (!detail?.riskRecord || acceptanceApproveMode) return ''
    const proposed = (treatmentOption || detail.riskRecord.treatmentOption) as TreatmentOption | null
    const changed =
      !confirmTreatment || (proposed != null && proposed !== detail.riskRecord.treatmentOption)
    if (changed && !justification.trim()) return 'Add a justification to submit'
    if (isProjectRisk && !needsAcceptanceForm && !actionDescription.trim()) {
      return 'Write the Treatment Action Plan Detail'
    }
    if (returnedRiskNeedsRevision(detail.riskRecord, actionDescription, linkedEvidence)) {
      return RETURN_REVISION_REASON
    }
    const treatNeedsEvidence = Boolean(detail.riskRecord.activeTreatmentPlan?.evidenceRequired?.trim())
    if (
      ((isProjectRisk && !needsAcceptanceForm && treatNeedsEvidence) ||
        (!isProjectRisk &&
          (proposed === 'TRANSFER' ||
            proposed === 'TERMINATE' ||
            (proposed === 'TREAT' && treatNeedsEvidence)))) &&
      linkedEvidence.length === 0
    ) {
      return proposed === 'TREAT'
        ? 'Upload the evidence required by the treatment plan'
        : 'Upload at least one evidence file for Terminate or Transfer'
    }
    if (proposed === 'TOLERATE' && !isProjectRisk && !contributorDeclaration) return 'Confirm the declaration to submit'
    if (needsAcceptanceForm && !contributorDeclaration) return 'Confirm the declaration to submit'
    if (needsAcceptanceForm && tolerateReviewDateError) return tolerateReviewDateError
    if (needsAcceptanceForm && isProjectRisk && !acceptanceJustification.trim()) {
      return 'Write the Justification for Accepting'
    }
    if (needsAcceptanceForm && isProjectRisk && !compensatingControls.trim()) {
      return 'Describe Compensating Controls'
    }
    if (needsAcceptanceForm && isProjectRisk && ownerUserId === '') return 'Select Risk Owner'
    if (needsAcceptanceForm && isProjectRisk && approver2UserId === '') {
      return `Select ${acceptanceLevel2Label(detail.riskRecord.riskType)}`
    }
    if (needsAcceptanceForm && isProjectRisk && level3ApproverEnabled && approver3UserId === '') {
      return 'Select Level 3 approver'
    }
    return ''
  })()

  const gapMode =
    detail != null &&
    !riskMode &&
    !capaMode &&
    !biaMode &&
    !ropaMode &&
    !dpiaMode &&
    !policyMode &&
    !kriMode &&
    !incidentMode &&
    detail.gapItem != null
  const onLastGapQuestion =
    gapStepQuestions.length === 0 || gapQuestionIndex >= gapStepQuestions.length - 1

  const dialogTitle = incidentMode
    ? detail?.incident
      ? `${detail.incident.displayId}: ${detail.incident.title}`
      : detail?.task.title
    : kriMode
    ? detail?.kri
      ? `${detail.kri.displayId}: ${detail.kri.name}`
      : detail?.task.title
    : policyMode
    ? detail?.policy
      ? `Policy: ${detail.policy.title} (${detail.policy.versionLabel})`
      : detail?.task.title
    : biaMode
      ? detail?.bia
        ? `BIA: ${detail.bia.displayId}`
        : detail?.task.title
      : ropaMode
        ? detail?.privacyRopa
          ? `RoPA: ${detail.privacyRopa.displayId} · ${detail.privacyRopa.activityName}`
          : detail?.task.title
      : dpiaMode
        ? detail?.privacyDpia
          ? `DPIA: ${detail.privacyDpia.displayId} · ${detail.privacyDpia.title}`
          : detail?.task.title
      : capaMode
        ? detail?.capa
          ? `CAPA: ${detail.capa.title}`
          : detail?.task.title
        : riskMode
          ? detail?.riskRecord
            ? `${detail.riskRecord.displayId}: ${detail.riskRecord.title}`
            : detail?.task.title
          : detail?.gapItem
            ? `${detail.gapItem.nodeCode}: ${detail.gapItem.title}`
            : detail?.task.title

  const gapReturned = detail?.gapItem?.reviewStatus === 'SENT_BACK'
  const taskTitleBadge = detail
    ? detail.bia
      ? biaCycleStatusBadge(detail.bia.status)
      : statusBadge(gapReturned ? 'RETURNED' : cardVisualStatus(detail.task))
    : null

  const taskTitleCard = gapMode && detail?.gapItem ? (
    <Box sx={{ mb: 1.5 }}>
      <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 1, flexWrap: 'wrap' }}>
        <Typography sx={{ fontSize: '0.9375rem', fontWeight: 600, letterSpacing: '-0.01em', color: brand.ink }}>
          {detail.gapItem.nodeCode}
        </Typography>
        <Typography sx={{ fontSize: '0.9375rem', fontWeight: 600, letterSpacing: '-0.01em', color: brand.ink }}>
          {detail.gapItem.title}
        </Typography>
      </Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap', mt: 0.5 }}>
        {taskTitleBadge ? (
          <Typography
            component="span"
            sx={{
              fontSize: '0.6875rem',
              fontWeight: 700,
              px: 1,
              py: 0.25,
              borderRadius: 20,
              bgcolor: taskTitleBadge.bg,
              color: taskTitleBadge.color,
            }}
          >
            {taskTitleBadge.label}
          </Typography>
        ) : null}
        {detail.task.dueDate ? (
          <Typography sx={{ fontSize: '0.75rem', color: brand.muted }}>Due {formatTenantDate(detail.task.dueDate)}</Typography>
        ) : null}
        {gapStepQuestions.length > 0 ? (
          <Typography sx={{ fontSize: '0.75rem', color: brand.muted }}>
            Question {gapQuestionIndex + 1} of {gapStepQuestions.length}
          </Typography>
        ) : null}
      </Box>
    </Box>
  ) : detail && taskTitleBadge ? (
    <Box
      sx={
        gapMode
          ? { bgcolor: 'transparent', px: 0, py: 0, mb: 2.5 }
          : riskMode
          ? {
              bgcolor: brand.paper,
              border: `1px solid ${brand.border}`,
              borderRadius: 1,
              px: 1.25,
              py: 0.625,
              mb: 1,
            }
          : {
              bgcolor: brand.paper,
              border: `1px solid ${brand.border}`,
              borderRadius: 1,
              p: '16px 18px',
              mb: 2,
              boxShadow: brand.shadowCard,
            }
      }
    >
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 1,
          mb: riskMode ? 0.25 : 1,
        }}
      >
        <Typography
          sx={{
            fontSize: riskMode ? '0.625rem' : '0.6875rem',
            fontWeight: 700,
            color: brand.goldDark,
            letterSpacing: '0.06em',
            textTransform: 'uppercase',
          }}
        >
          {frameworkDisplayName(undefined, detail.frameworkCode) !== '-'
            ? frameworkDisplayName(undefined, detail.frameworkCode)
            : detail.task.sourceModule}
          {detail.assessmentName ? ` · ${detail.assessmentName}` : ''}
        </Typography>
        <Typography
          component="span"
          sx={{
            fontSize: '0.59375rem',
            fontWeight: 700,
            px: riskMode ? 1 : 1.25,
            py: riskMode ? 0.125 : 0.375,
            borderRadius: 20,
            bgcolor: taskTitleBadge.bg,
            color: taskTitleBadge.color,
          }}
        >
          {taskTitleBadge.label}
        </Typography>
      </Box>
      <Typography
        sx={{
          fontSize: gapMode ? '1.35rem' : riskMode ? '0.8125rem' : '0.875rem',
          fontWeight: 700,
          letterSpacing: gapMode ? '-0.02em' : 0,
          color: brand.ink,
          mb: riskMode ? 0 : 0.5,
        }}
      >
        {dialogTitle}
      </Typography>
      <Typography sx={{ fontSize: riskMode ? '0.625rem' : '0.65625rem', color: brand.muted }}>
        {detail.task.assignedByName ? `Assigned by ${detail.task.assignedByName}` : 'Assigned task'}
        {!gapMode && detail.task.dueDate ? ` · Due ${formatTenantDate(detail.task.dueDate)}` : ''}
      </Typography>
      {gapMode && detail.task.dueDate ? (
        <Typography sx={{ fontSize: '0.875rem', fontWeight: 700, color: brand.ink, mt: 0.75 }}>
          Due date: {formatTenantDate(detail.task.dueDate)}
        </Typography>
      ) : null}
    </Box>
  ) : null

  const headerTitle =
    displayTaskTitle(detail?.task.title, detail?.task.sourceRecordType) ??
    selectedAssignment?.title ??
    appShellPage?.title ??
    contributorNavTitle(active || 'contributor-dashboard')
  const showRoleChips = !appShellPage?.hideRoleChips || Boolean(detail || selectedAssignment)
  const gapChrome = Boolean(detail || selectedAssignment)

  const roleChips = (
    <Stack direction="row" spacing={0.5} alignItems="center">
      {session.roles.slice(0, 2).map((r) => (
        <Chip key={r} size="small" variant="outlined" label={roleLabel(r)} />
      ))}
    </Stack>
  )

  return (
    <AppShellPageProvider onPageChange={setAppShellPage}>
    {!workspaceReady ? (
      <WorkspaceBootScreen
        title="Opening your workspace"
        message="Loading your assigned work. This usually takes a few seconds."
      />
    ) : (
    <AppShell
      nav={nav}
      active={active}
      onNavChange={goNav}
      showLicensedModulesNav={false}
      email={session.email}
      accountMeta={`${roleLabels(session.roles)} · assigned work only`}
      roleChips={showRoleChips ? roleChips : undefined}
      headerCompact={Boolean(gapMode && detail)}
      headerTitle={headerTitle}
      onHeaderTitleClick={gapChrome ? undefined : appShellPage?.onTitleClick}
      headerSubtitle={gapChrome || appShellPage ? undefined : contributorNavCaption(active || 'contributor-dashboard')}
      headerBreadcrumbRoot={
        gapChrome
          ? {
              label: contributorWorkBackLabel(active),
              onClick: () => {
                requestNavigation(() => {
                  setDetail(null)
                  setGapWork(null)
                  setSelectedAssignmentId(null)
                })
              },
            }
          : appShellPage?.breadcrumbRoot
      }
      headerBreadcrumb={
        gapChrome
          ? detail
            ? [
                frameworkDisplayName(undefined, detail.frameworkCode) !== '-'
                  ? frameworkDisplayName(undefined, detail.frameworkCode)
                  : null,
                detail.assessmentName,
              ]
                .filter(Boolean)
                .join(' / ') || headerTitle
            : 'Assigned controls'
          : appShellPage?.breadcrumbTrail
      }
      headerTabs={gapChrome ? undefined : appShellPage?.tabs}
      headerMeta={gapChrome ? undefined : appShellPage?.headerMeta}
      workspaceName={locale.displayName || session.tenantId || 'Workspace'}
      flushContent={appShellPage?.flushContent ?? true}
      pageActions={gapChrome ? undefined : appShellPage?.actions}
      actions={
        <NotificationBell
          onNavigate={goNav}
          onOpenNotification={openNotification}
          onViewAll={() => goNav(firstContributorNavId(nav) || 'contributor-dashboard')}
        />
      }
      onLogout={onLogout}
      canChangePassword={session.authType !== 'SSO'}
    >
      <Box
        sx={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          minHeight: 0,
          height: '100%',
          overflow: 'hidden',
        }}
      >
      {!detail && !selectedAssignment ? (
            <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0 }}>
              {error && (
                <ContributorRail sx={{ pt: 1, flexShrink: 0 }}>
                  <Typography color="error" variant="body2">
                    {error}
                  </Typography>
                </ContributorRail>
              )}
              {ok && (
                <ContributorRail sx={{ pt: 1, flexShrink: 0 }}>
                  <Typography color="success.main" variant="body2">
                    {ok}
                  </Typography>
                </ContributorRail>
              )}
              <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
                <ContributorAssignedRegisters
                  active={active}
                  biaHomeNonce={biaHomeNonce}
                  session={session}
                  onError={setError}
                  onOk={setOk}
                  onNavChange={goNav}
                  onOpenTask={(id) => void openTask(id)}
                  onOpenAssignedGap={(projectId, gapId) => void openAssignedGap(projectId, gapId)}
                  onOpenAssignedRisk={(riskId, riskType, projectId) =>
                    openAssignedRisk(riskId, riskType, projectId)
                  }
                  riskProjectContext={riskProjectContext}
                  capaNotifyOpen={active === 'audit-capa' ? capaNotifyOpen : null}
                />
              </Box>
            </Box>
          ) : !detail && selectedAssignment ? (
            <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0 }}>
              {error && (
                <Typography color="error" variant="body2" sx={{ px: 2, pt: 1, flexShrink: 0 }}>
                  {error}
                </Typography>
              )}
              <ContributorControlList
                assignment={selectedAssignment}
                onOpenTask={(id) => void openTask(id)}
              />
            </Box>
          ) : biaMode ? (
            <ContributorWorkArea>
              <Box
                sx={{
                  flex: 1,
                  minHeight: 0,
                  overflow: 'hidden',
                  display: 'flex',
                  flexDirection: 'column',
                  px: contributorPadX,
                  py: 2,
                  boxSizing: 'border-box',
                }}
              >
                {error && (
                  <Typography color="error" variant="body2" sx={{ mb: 1, flexShrink: 0 }}>
                    {error}
                  </Typography>
                )}
                {ok && (
                  <Typography color="success.main" variant="body2" sx={{ mb: 1, flexShrink: 0 }}>
                    {ok}
                  </Typography>
                )}
                {detail && (
                  <BcmsBiaPage
                    session={session}
                    onError={setError}
                    onOk={setOk}
                    embeddedCycleId={detail.bia!.id}
                    rightHeader={taskTitleCard}
                    onSubmitted={() => {
                      const taskId = detail.task.id
                      void (async () => {
                        await load()
                        await openTask(taskId)
                      })()
                    }}
                  />
                )}
              </Box>
            </ContributorWorkArea>
          ) : (
            <ContributorWorkArea
              sx={{
                overflow: 'hidden',
                position: 'relative',
                ...(gapMode ? { bgcolor: brand.canvas } : {}),
              }}
            >
            {riskSubmitting && detail?.riskRecord ? (
              <RiskRecordCreateProgress
                isErm={detail.riskRecord.riskType === 'ERM'}
                isPrivacy={detail.riskRecord.riskType === 'PRIVACY_RISK'}
                mode="submit"
              />
            ) : null}
            <ContributorRail
              sx={{
                py: riskMode || gapMode ? 1.25 : 3,
                pb: 0,
                display: 'flex',
                flexDirection: 'column',
                flex: 1,
                minHeight: 0,
                ...(riskMode || gapMode
                  ? { maxWidth: '100%', px: { xs: 1.5, sm: 2, lg: 2.5 } }
                  : { maxWidth: layout.contributorFormMax, mx: 'auto' }),
              }}
            >
            <Box
              ref={workScrollRef}
              sx={{
                width: '100%',
                flex: 1,
                minHeight: 0,
                overflow:
                  detail && riskMode && detail.riskRecord && riskWorkTab === 'details'
                    ? 'hidden'
                    : 'auto',
                display: detail && riskMode && detail.riskRecord ? 'flex' : undefined,
                flexDirection: detail && riskMode && detail.riskRecord ? 'column' : undefined,
                pb: riskMode || gapMode ? 1 : 2,
              }}
            >
              {error && (
                <Typography color="error" variant="body2" sx={{ mb: 1 }}>
                  {error}
                </Typography>
              )}
              {ok && (
                <Typography color="success.main" variant="body2" sx={{ mb: 1 }}>
                  {ok}
                </Typography>
              )}
              {detail && detail.task.status === 'SUBMITTED' && (
                <Alert severity="success" sx={{ mb: 2 }}>
                  {riskMode
                    ? 'Risk task submitted: Admin will validate.'
                    : gapMode
                      ? 'Control submitted: awaiting review.'
                      : 'Task submitted successfully.'}
                </Alert>
              )}
              {!gapMode ? <Box sx={{ flexShrink: 0 }}>{taskTitleCard}</Box> : null}
          {detail && incidentMode && detail.incident && (
            <Stack spacing={1.5} sx={{ mt: 1, minWidth: { sm: 420 } }}>
              <Typography variant="caption" color="text.secondary">
                {detail.incident.priority} · {detail.incident.incidentType} · {detail.incident.status}
                {detail.incident.responseDueAt
                  ? ` · respond by ${detail.incident.responseDueAt.replace('T', ' ').slice(0, 16)}`
                  : ''}
              </Typography>
              {detail.incident.description && (
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                  {detail.incident.description}
                </Typography>
              )}
              <CompactTextField
                label="Progress / resolution notes"
                multiline
                minRows={3}
                fieldIdPrefix="contrib-inc"
                value={incidentNotes}
                onChange={(e) => setIncidentNotes(e.target.value)}
                disabled={detail.task.status === 'SUBMITTED' || detail.incident.status === 'RESOLVED' || detail.incident.status === 'CLOSED'}
              />
              <FormControlLabel
                control={
                  <Checkbox
                    checked={incidentResolve}
                    disabled={detail.task.status === 'SUBMITTED' || detail.incident.status === 'RESOLVED' || detail.incident.status === 'CLOSED'}
                    onChange={(e) => setIncidentResolve(e.target.checked)}
                  />
                }
                label="Mark resolved"
              />
            </Stack>
          )}
          {detail && kriMode && detail.kri && (
            <Stack spacing={1.5} sx={{ mt: 1, minWidth: { sm: 420 } }}>
              <Typography variant="caption" color="text.secondary">
                {detail.kri.direction === 'HIGHER_IS_WORSE' ? 'Higher is worse' : 'Lower is worse'}
                {' · '}
                {detail.kri.frequency}
                {detail.kri.unit ? ` · ${detail.kri.unit}` : ''}
                {detail.kri.nextDueDate ? ` · due ${detail.kri.nextDueDate}` : ''}
              </Typography>
              <Typography variant="body2">
                Thresholds: Green {detail.kri.greenThreshold} / Amber {detail.kri.amberThreshold} /
                Red {detail.kri.redThreshold}
              </Typography>
              {detail.kri.rag && (
                <RiskLevelBadge level={kriRagToBadge(detail.kri.rag)} label={`Current RAG: ${detail.kri.rag}`} />
              )}
              {detail.kri.metricDescription && (
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                  {detail.kri.metricDescription}
                </Typography>
              )}
              <CompactTextField
                label="Value"
                type="number"
                required
                fieldIdPrefix="contrib-kri"
                value={kriValue}
                onChange={(e) => setKriValue(e.target.value)}
                disabled={detail.task.status === 'SUBMITTED'}
              />
              <CompactTextField
                label="Notes"
                multiline
                minRows={2}
                fieldIdPrefix="contrib-kri"
                value={kriNotes}
                onChange={(e) => setKriNotes(e.target.value)}
                disabled={detail.task.status === 'SUBMITTED'}
              />
            </Stack>
          )}
          {detail && policyMode && detail.policy && (
            <Stack spacing={1.5} sx={{ mt: 1, minWidth: { sm: 420 } }}>
              <Typography variant="caption" color="text.secondary">
                {detail.policy.category} · {detail.policy.versionLabel}
                {detail.policy.ackDeadline ? ` · due ${detail.policy.ackDeadline}` : ''}
              </Typography>
              <Card variant="outlined" sx={{ p: 1.5, bgcolor: brand.canvas, whiteSpace: 'pre-wrap', maxHeight: 320, overflow: 'auto' }}>
                <Typography variant="body2">
                  {detail.policy.bodyText ?? 'No body text: see attached file.'}
                </Typography>
              </Card>
              {detail.policy.hasFile && (
                <Button
                  size="small"
                  variant="outlined"
                  onClick={() =>
                    void api
                      .downloadPolicyFile(detail.policy!.policyId, detail.policy!.originalFilename ?? undefined)
                      .catch((e) => setError(String(e)))
                  }
                >
                  Download {detail.policy.originalFilename}
                </Button>
              )}
              {detail.policy.alreadyAcknowledged ? (
                <Typography variant="body2" color="success.main">
                  You already acknowledged this policy.
                </Typography>
              ) : (
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={policyReadConfirmed}
                      disabled={detail.task.status === 'SUBMITTED'}
                      onChange={(e) => setPolicyReadConfirmed(e.target.checked)}
                    />
                  }
                  label="I have read and acknowledge this policy"
                />
              )}
            </Stack>
          )}
          {detail && ropaMode && detail.privacyRopa && (
            <Stack spacing={1.5} sx={{ mt: 1, minWidth: { sm: 420 } }}>
              <Typography variant="body2" color="text.secondary">
                Download the Arbor RoPA template, fill offline, then upload the completed workbook and approval
                evidence (email PDF or signed approval). Both stages are required to submit.
              </Typography>
              <Button
                variant="outlined"
                size="small"
                disabled={busy}
                onClick={() =>
                  void api
                    .downloadPrivacyRopaTemplate()
                    .catch((e) => setError(e instanceof Error ? e.message : 'Download failed'))
                }
              >
                Download RoPA Template
              </Button>
              <FormSelect
                label="Evidence stage"
                value={ropaEvidenceStage}
                disabled={detail.task.status === 'SUBMITTED'}
                onChange={(v) =>
                  setRopaEvidenceStage(v as 'WORKBOOK_SUBMITTED' | 'APPROVAL_EVIDENCE')
                }
                options={[
                  { value: 'WORKBOOK_SUBMITTED', label: 'Filled workbook' },
                  { value: 'APPROVAL_EVIDENCE', label: 'Approval evidence' },
                ]}
              />
              <ContributorEvidencePanel
                linkedEvidence={linkedEvidence}
                evidenceFile={evidenceFile}
                evidenceClass={evidenceClass}
                evidenceClasses={EVIDENCE_CLASSIFICATIONS}
                evidenceAccept={EVIDENCE_ACCEPT}
                busy={busy}
                disabled={detail.task.status === 'SUBMITTED'}
                required
                onSelectFile={setEvidenceFile}
                onEvidenceClass={setEvidenceClass}
                onUpload={() => void uploadRopaEvidence()}
                onDownload={(id) => void api.downloadEvidence(id)}
                onRemove={(ev) => {
                  const recordId = String(detail.privacyRopa!.id)
                  return void unlinkAndRefresh(ev, 'PRIVACY', 'PRIVACY_ROPA', recordId, async () => {
                    setLinkedEvidence(
                      await api.listEvidenceLinked('PRIVACY', 'PRIVACY_ROPA', recordId),
                    )
                  })
                }}
              />
              <Typography variant="caption" color="text.secondary">
                Linked: workbook{' '}
                {linkedEvidence.some((e) => e.workflowStage === 'WORKBOOK_SUBMITTED') ? '✓' : '-'} · approval{' '}
                {linkedEvidence.some((e) => e.workflowStage === 'APPROVAL_EVIDENCE') ? '✓' : '-'}
              </Typography>
            </Stack>
          )}
          {detail && dpiaMode && detail.privacyDpia && (
            <Stack spacing={1.5} sx={{ mt: 1, minWidth: { sm: 420 } }}>
              <Typography variant="body2" color="text.secondary">
                Download the Arbor DPIA template, complete Pre-DPIA / Questions / Assessment offline, then upload
                the filled workbook and approval evidence. Both stages are required to submit.
              </Typography>
              {detail.privacyDpia.fullDpiaMandatory && (
                <Typography variant="caption" color="warning.main">
                  Full DPIA mandatory (screening Q4–Q7 triggered).
                </Typography>
              )}
              <Button
                variant="outlined"
                size="small"
                disabled={busy}
                onClick={() =>
                  void api
                    .downloadPrivacyDpiaTemplate()
                    .catch((e) => setError(e instanceof Error ? e.message : 'Download failed'))
                }
              >
                Download DPIA Template
              </Button>
              <FormSelect
                label="Evidence stage"
                value={dpiaEvidenceStage}
                disabled={detail.task.status === 'SUBMITTED'}
                onChange={(v) =>
                  setDpiaEvidenceStage(v as 'WORKBOOK_SUBMITTED' | 'APPROVAL_EVIDENCE')
                }
                options={[
                  { value: 'WORKBOOK_SUBMITTED', label: 'Filled workbook' },
                  { value: 'APPROVAL_EVIDENCE', label: 'Approval evidence' },
                ]}
              />
              <ContributorEvidencePanel
                linkedEvidence={linkedEvidence}
                evidenceFile={evidenceFile}
                evidenceClass={evidenceClass}
                evidenceClasses={EVIDENCE_CLASSIFICATIONS}
                evidenceAccept={EVIDENCE_ACCEPT}
                busy={busy}
                disabled={detail.task.status === 'SUBMITTED'}
                required
                onSelectFile={setEvidenceFile}
                onEvidenceClass={setEvidenceClass}
                onUpload={() => void uploadDpiaEvidence()}
                onDownload={(id) => void api.downloadEvidence(id)}
                onRemove={(ev) => {
                  const recordId = String(detail.privacyDpia!.id)
                  return void unlinkAndRefresh(ev, 'PRIVACY', 'PRIVACY_DPIA', recordId, async () => {
                    setLinkedEvidence(
                      await api.listEvidenceLinked('PRIVACY', 'PRIVACY_DPIA', recordId),
                    )
                  })
                }}
              />
              <Typography variant="caption" color="text.secondary">
                Linked: workbook{' '}
                {linkedEvidence.some((e) => e.workflowStage === 'WORKBOOK_SUBMITTED') ? '✓' : '-'} · approval{' '}
                {linkedEvidence.some((e) => e.workflowStage === 'APPROVAL_EVIDENCE') ? '✓' : '-'}
              </Typography>
            </Stack>
          )}
          {detail && milestoneMode && detail.treatmentMilestone && detail.riskRecord && (
            <Box
              sx={{
                bgcolor: brand.paper,
                border: `1px solid ${brand.border}`,
                borderRadius: 1,
                p: 2.5,
                boxShadow: brand.shadowCard,
              }}
            >
              <Stack spacing={1.5}>
                <Typography variant="subtitle2" fontWeight={700}>
                  Treatment milestone: {detail.treatmentMilestone.milestoneTitle}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Risk {detail.riskRecord.displayId} · {detail.riskRecord.title}
                  {detail.treatmentMilestone.dueDate ? ` · due ${detail.treatmentMilestone.dueDate}` : ''}
                </Typography>
                <CompactTextField
                  label="Progress notes"
                  fieldIdPrefix="milestone"
                  fullWidth
                  multiline
                  minRows={3}
                  value={milestoneProgressNotes}
                  onChange={(e) => setMilestoneProgressNotes(e.target.value)}
                  disabled={detail.task.status === 'SUBMITTED'}
                />
                <ContributorEvidencePanel
                  linkedEvidence={linkedEvidence}
                  evidenceFile={evidenceFile}
                  evidenceClass={evidenceClass}
                  evidenceClasses={EVIDENCE_CLASSIFICATIONS}
                  evidenceAccept={EVIDENCE_ACCEPT}
                  maxBytes={RISK_EVIDENCE_MAX_BYTES}
                  maxFiles={RISK_EVIDENCE_MAX_FILES}
                  currentUserId={meId}
                  busy={busy}
                  disabled={detail.task.status === 'SUBMITTED'}
                  required
                  onSelectFile={setEvidenceFile}
                  onEvidenceClass={setEvidenceClass}
                  onUpload={() => void uploadMilestoneEvidence()}
                  onDownload={(id) => void api.downloadEvidence(id)}
                  onRemove={(ev) => {
                    const recordId = String(detail.treatmentMilestone!.id)
                    return void unlinkAndRefresh(ev, 'RISK', 'RISK_MILESTONE', recordId, async () => {
                      setLinkedEvidence(
                        await api.listEvidenceLinked('RISK', 'RISK_MILESTONE', recordId),
                      )
                    })
                  }}
                />
              </Stack>
            </Box>
          )}
          {detail && riskMode && detail.riskRecord && (
            <Stack
              spacing={1}
              sx={{
                flex: riskWorkTab === 'details' ? 1 : 'none',
                minHeight: riskWorkTab === 'details' ? 0 : undefined,
              }}
            >
              <Stack direction="row" spacing={0.5} sx={{ flexShrink: 0 }}>
                <Button
                  size="small"
                  variant={riskWorkTab === 'details' ? 'contained' : 'outlined'}
                  onClick={() => setRiskWorkTab('details')}
                >
                  Risk Details
                </Button>
                <Button
                  size="small"
                  variant={riskWorkTab === 'action' ? 'contained' : 'outlined'}
                  onClick={() => setRiskWorkTab('action')}
                >
                  Your Action
                </Button>
              </Stack>
              {riskWorkTab === 'details' && (
                <Stack spacing={1} sx={{ flex: 1, minHeight: 0 }}>
                  <RiskFormShell sections={[...EXCEL_DOSSIER_SECTIONS]}>
                    <ExcelRiskDossier
                      risk={detail.riskRecord}
                      gridSize={riskGridSize}
                      linkedEvidence={linkedEvidence}
                      showContributor
                      onEvidenceError={(m) => setError(m)}
                      orgLabel={(id) =>
                        id == null
                          ? ''
                          : detail.riskRecord?.orgUnitName || orgNameById[id] || `Org unit #${id}`
                      }
                      userLabel={(id) => {
                        if (id == null) return ''
                        if (id === detail.riskRecord?.riskOwnerUserId && detail.riskRecord.riskOwnerName) {
                          return detail.riskRecord.riskOwnerName
                        }
                        if (
                          id === detail.riskRecord?.assignedContributorId &&
                          detail.riskRecord.assignedContributorName
                        ) {
                          return detail.riskRecord.assignedContributorName
                        }
                        const u = riskPickerUsers.find((x) => x.id === id)
                        return u ? u.fullName : ''
                      }}
                    />
                  </RiskFormShell>
                </Stack>
              )}
              {riskWorkTab === 'action' && (
                <Box sx={{ flex: 'none' }}>
              {acceptanceApproveMode && (
                <ExcelAcceptanceApprover
                  risk={detail.riskRecord}
                  form={detail.riskRecord.acceptanceForm}
                  disabled={detail.task.status === 'SUBMITTED'}
                  comments={acceptDecideComments}
                  onComments={setAcceptDecideComments}
                  declared={acceptDecideDeclared}
                  onDeclared={setAcceptDecideDeclared}
                  busy={busy}
                  gridSize={riskGridSize}
                  onApprove={() => void decideAcceptance('APPROVED')}
                  onReject={() => void decideAcceptance('REJECTED')}
                />
              )}
              {!acceptanceApproveMode && (
            <Stack spacing={1.5}>
              {detail.riskRecord.rejectionReason && (
                <Alert severity="warning">
                  {/^acceptance required\.?$/i.test(detail.riskRecord.rejectionReason.trim())
                    ? 'Returned: Risk Acceptance required.'
                    : (
                      <>
                        {'Returned for revision: '}<strong>{detail.riskRecord.rejectionReason.trim()}</strong>
                      </>
                    )}
                </Alert>
              )}
              {!isProjectRisk && !needsAcceptanceForm && (
              <>
              {detail.riskRecord.infosecStatusInitial && (
                <Typography variant="body2">
                  InfoSec (initial): {detail.riskRecord.infosecStatusInitial}
                </Typography>
              )}
              <Typography variant="body2">
                Proposed:{' '}
                {detail.riskRecord.treatmentOption
                  ? TREAT_LABEL[detail.riskRecord.treatmentOption]
                  : '-'}
              </Typography>
              <FormControlLabel
                control={
                  <Checkbox
                    checked={confirmTreatment}
                    disabled={detail.task.status === 'SUBMITTED'}
                    onChange={(e) => setConfirmTreatment(e.target.checked)}
                  />
                }
                label="Confirm treatment option"
              />
              <FormSelect
                label={EXCEL_FIELD.treatmentOption}
                wrapLabel
                value={treatmentOption}
                options={TREAT.map((t) => ({ value: t, label: TREAT_LABEL[t] }))}
                onChange={(v) => setTreatmentOption(v as TreatmentOption)}
                disabled={detail.task.status === 'SUBMITTED' || needsAcceptanceForm}
              />
              {(!confirmTreatment ||
                (treatmentOption && treatmentOption !== detail.riskRecord.treatmentOption)) && (
                <CompactTextField
                  label="Treatment Description"
                  wrapLabel
                  required
                  multiline
                  minRows={2}
                  fieldIdPrefix="contrib-risk"
                  disabled={detail.task.status === 'SUBMITTED'}
                  value={justification}
                  onChange={(e) => setJustification(e.target.value)}
                  maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
                />
              )}
              <CompactTextField
                label="Comments"
                multiline
                minRows={2}
                fieldIdPrefix="contrib-risk"
                disabled={detail.task.status === 'SUBMITTED'}
                value={comments}
                onChange={(e) => setComments(e.target.value)}
                maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
              />
              </>
              )}
              {!isProjectRisk && (
              <Box
                sx={{
                  p: 1.25,
                  borderRadius: 1,
                  border: `1px solid ${brand.border}`,
                  bgcolor: brand.hover,
                }}
              >
                <Typography variant="subtitle2" fontWeight={700} sx={{ mb: 0.5 }}>
                  InfoSec status (contributor)
                </Typography>
                <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1 }}>
                  Update the current InfoSec posture before submitting: this is recorded with your
                  treatment response.
                </Typography>
                <CompactTextField
                  label="InfoSec status update"
                  multiline
                  minRows={2}
                  fieldIdPrefix="contrib-risk"
                  disabled={detail.task.status === 'SUBMITTED'}
                  value={infosecStatusContributor}
                  onChange={(e) => setInfosecStatusContributor(e.target.value)}
                />
              </Box>
              )}

              {isProjectRisk && !needsAcceptanceForm && (
                <SettingsCard
                  compact
                  autoHeight
                  title="Your action"
                  subtitle="Treatment Action Plan Detail and Attached Evidence"
                >
                  <Stack spacing={1}>
                  {detail.riskRecord.treatmentChangeJustification?.trim() ? (
                    <CompactTextField
                      label={EXCEL_FIELD.treatmentJustification}
                      wrapLabel
                      multiline
                      minRows={2}
                      maxRows={NARRATIVE_MAX_ROWS}
                      fieldIdPrefix="contrib-admin-plan"
                      value={detail.riskRecord.treatmentChangeJustification}
                      onChange={() => undefined}
                      slotProps={{ htmlInput: { readOnly: true } }}
                    />
                  ) : null}
                  <Box sx={twoColFieldSx}>
                    <CompactTextField
                      label={EXCEL_FIELD.treatmentActionPlan}
                      wrapLabel
                      required
                      multiline
                      minRows={6}
                      maxRows={NARRATIVE_MAX_ROWS}
                      fieldIdPrefix="contrib-risk"
                      disabled={detail.task.status === 'SUBMITTED'}
                      value={actionDescription}
                      onChange={(e) => setActionDescription(e.target.value)}
                    />
                    <ContributorEvidencePanel
                      heading={EXCEL_FIELD.attachedEvidence}
                      linkedEvidence={linkedEvidence.filter((e) => !isIdentificationEvidence(e.workflowStage))}
                      evidenceFile={evidenceFile}
                      evidenceClass={evidenceClass}
                      evidenceClasses={EVIDENCE_CLASSIFICATIONS}
                      evidenceAccept={EVIDENCE_ACCEPT}
                      maxBytes={RISK_EVIDENCE_MAX_BYTES}
                      maxFiles={RISK_EVIDENCE_MAX_FILES}
                      currentUserId={meId}
                      busy={busy && !riskSubmitting}
                      hideInlineProgress
                      disabled={detail.task.status === 'SUBMITTED' || riskSubmitting}
                      required={Boolean(detail.riskRecord.activeTreatmentPlan?.evidenceRequired?.trim())}
                      requirementText={detail.riskRecord.activeTreatmentPlan?.evidenceRequired}
                      onSelectFile={setEvidenceFile}
                      onEvidenceClass={setEvidenceClass}
                      onUpload={() => void uploadRiskEvidence()}
                      onDownload={(id) => void api.downloadEvidence(id)}
                      onRemove={(ev) => {
                        const recordId = String(detail.riskRecord!.id)
                        return void unlinkAndRefresh(ev, 'RISK', 'RISK_RECORD', recordId, async () => {
                          setLinkedEvidence(
                            await api.listEvidenceLinked('RISK', 'RISK_RECORD', recordId),
                          )
                        })
                      }}
                    />
                  </Box>
                  </Stack>
                </SettingsCard>
              )}
              {!isProjectRisk &&
                (treatmentOption === 'TREAT' ||
                  (!treatmentOption && detail.riskRecord.treatmentOption === 'TREAT')) && (
                <CompactTextField
                  label={EXCEL_FIELD.treatmentActionPlan}
                  multiline
                  minRows={2}
                  maxRows={NARRATIVE_MAX_ROWS}
                  fieldIdPrefix="contrib-risk"
                  disabled={detail.task.status === 'SUBMITTED'}
                  value={actionDescription}
                  onChange={(e) => setActionDescription(e.target.value)}
                  maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
                />
              )}

              {needsAcceptanceForm && isProjectRisk && (
                <ExcelAcceptanceContributor
                  risk={detail.riskRecord}
                  form={detail.riskRecord.acceptanceForm}
                  users={riskPickerUsers}
                  disabled={detail.task.status === 'SUBMITTED'}
                  gridSize={riskGridSize}
                  justification={acceptanceJustification}
                  onJustification={setAcceptanceJustification}
                  compensatingControls={compensatingControls}
                  onCompensatingControls={setCompensatingControls}
                  businessRationale={businessRationale}
                  onBusinessRationale={setBusinessRationale}
                  reviewDate={reviewDate}
                  onReviewDate={setReviewDate}
                  reviewDateMin={reviewDateMin}
                  reviewDateMax={reviewDateMax}
                  reviewDateError={tolerateReviewDateError}
                  declared={contributorDeclaration}
                  onDeclared={setContributorDeclaration}
                  ownerUserId={ownerUserId}
                  onOwnerUserId={setOwnerUserId}
                  approver2UserId={approver2UserId}
                  onApprover2UserId={setApprover2UserId}
                  approver3UserId={approver3UserId}
                  onApprover3UserId={setApprover3UserId}
                  level3Enabled={level3ApproverEnabled}
                  onLevel3Enabled={setLevel3ApproverEnabled}
                />
              )}
              {needsAcceptanceForm && !isProjectRisk && (
                <Stack spacing={1.5}>
                  <CompactTextField
                    label={EXCEL_FIELD.justificationForAccepting}
                    required
                    multiline
                    minRows={2}
                    maxRows={NARRATIVE_MAX_ROWS}
                    fieldIdPrefix="contrib-risk"
                    disabled={detail.task.status === 'SUBMITTED'}
                    value={acceptanceJustification}
                    onChange={(e) => setAcceptanceJustification(e.target.value)}
                  />
                  <CompactTextField
                    label={EXCEL_FIELD.compensatingControls}
                    required
                    multiline
                    minRows={2}
                    maxRows={NARRATIVE_MAX_ROWS}
                    fieldIdPrefix="contrib-risk"
                    disabled={detail.task.status === 'SUBMITTED'}
                    value={compensatingControls}
                    onChange={(e) => setCompensatingControls(e.target.value)}
                  />
                  <CompactTextField
                    label={EXCEL_FIELD.businessRationale}
                    multiline
                    minRows={2}
                    maxRows={NARRATIVE_MAX_ROWS}
                    fieldIdPrefix="contrib-risk"
                    disabled={detail.task.status === 'SUBMITTED'}
                    value={businessRationale}
                    onChange={(e) => setBusinessRationale(e.target.value)}
                  />
                  <CompactTextField
                    label="Conditions of acceptance"
                    required
                    multiline
                    minRows={2}
                    fieldIdPrefix="contrib-risk"
                    disabled={detail.task.status === 'SUBMITTED'}
                    value={conditionsOfAcceptance}
                    onChange={(e) => setConditionsOfAcceptance(e.target.value)}
                  />
                  <FormSelect
                    label="Comp control effectiveness"
                    value={compControlEffectiveness}
                    options={LEVELS.map((l) => ({ value: l, label: l.replace(/_/g, ' ') }))}
                    onChange={(v) => setCompControlEffectiveness(v as RiskLevel)}
                    disabled={detail.task.status === 'SUBMITTED'}
                  />
                  <CompactTextField
                    label={EXCEL_FIELD.acceptanceReviewDate}
                    type="date"
                    required
                    fieldIdPrefix="contrib-risk"
                    InputLabelProps={{ shrink: true }}
                    inputProps={{ min: reviewDateMin, max: reviewDateMax }}
                    disabled={detail.task.status === 'SUBMITTED'}
                    value={reviewDate}
                    onChange={(e) => setReviewDate(e.target.value)}
                    error={!!tolerateReviewDateError}
                    helperText={tolerateReviewDateError}
                  />
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={contributorDeclaration}
                        disabled={detail.task.status === 'SUBMITTED'}
                        onChange={(e) => setContributorDeclaration(e.target.checked)}
                      />
                    }
                    label="I declare the acceptance details are accurate"
                  />
                </Stack>
              )}

              {!isProjectRisk &&
                (treatmentOption === 'TERMINATE' ||
                (!treatmentOption && detail.riskRecord.treatmentOption === 'TERMINATE')) && (
                <Stack spacing={1}>
                  <CompactTextField
                    label="Elimination justification"
                    required
                    multiline
                    minRows={2}
                    fieldIdPrefix="contrib-risk"
                    disabled={detail.task.status === 'SUBMITTED'}
                    value={eliminationJustification}
                    onChange={(e) => setEliminationJustification(e.target.value)}
                  />
                  <CompactTextField
                    label="How risk is eliminated"
                    required
                    multiline
                    minRows={2}
                    fieldIdPrefix="contrib-risk"
                    disabled={detail.task.status === 'SUBMITTED'}
                    value={eliminationHow}
                    onChange={(e) => setEliminationHow(e.target.value)}
                  />
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={activityStopped}
                        disabled={detail.task.status === 'SUBMITTED'}
                        onChange={(e) => setActivityStopped(e.target.checked)}
                      />
                    }
                    label="Activity creating the risk has been stopped"
                  />
                </Stack>
              )}

              {!isProjectRisk &&
                (treatmentOption === 'TRANSFER' ||
                (!treatmentOption && detail.riskRecord.treatmentOption === 'TRANSFER')) && (
                <Stack spacing={1}>
                  <FormSelect
                    label="Transfer method"
                    value={transferMethod}
                    options={TRANSFER_METHODS.map((m) => ({ value: m, label: m }))}
                    onChange={(v) => setTransferMethod(v as TransferMethod)}
                    fullWidth
                    disabled={detail.task.status === 'SUBMITTED'}
                  />
                  <CompactTextField
                    label="Third party"
                    required
                    fieldIdPrefix="contrib-risk"
                    disabled={detail.task.status === 'SUBMITTED'}
                    value={thirdPartyName}
                    onChange={(e) => setThirdPartyName(e.target.value)}
                  />
                  <CompactTextField
                    label="Transfer description"
                    required
                    multiline
                    minRows={2}
                    fieldIdPrefix="contrib-risk"
                    disabled={detail.task.status === 'SUBMITTED'}
                    value={transferDescription}
                    onChange={(e) => setTransferDescription(e.target.value)}
                  />
                  <CompactTextField
                    label="Residual after transfer"
                    required
                    multiline
                    minRows={2}
                    fieldIdPrefix="contrib-risk"
                    disabled={detail.task.status === 'SUBMITTED'}
                    value={residualAfterTransfer}
                    onChange={(e) => setResidualAfterTransfer(e.target.value)}
                  />
                  <CompactTextField
                    label="Compensating controls during transfer (mandatory)"
                    required
                    multiline
                    minRows={2}
                    fieldIdPrefix="contrib-risk"
                    disabled={detail.task.status === 'SUBMITTED'}
                    value={transferCompensatingControls}
                    onChange={(e) => setTransferCompensatingControls(e.target.value)}
                  />
                </Stack>
              )}

              {!isProjectRisk && proposedTreatment !== 'TOLERATE' && proposedTreatment != null && (
                <ContributorEvidencePanel
                  linkedEvidence={linkedEvidence.filter((e) => !isIdentificationEvidence(e.workflowStage))}
                  evidenceFile={evidenceFile}
                  evidenceClass={evidenceClass}
                  evidenceClasses={EVIDENCE_CLASSIFICATIONS}
                  evidenceAccept={EVIDENCE_ACCEPT}
                  maxBytes={RISK_EVIDENCE_MAX_BYTES}
                  maxFiles={RISK_EVIDENCE_MAX_FILES}
                  currentUserId={meId}
                  busy={busy && !riskSubmitting}
                  hideInlineProgress
                  disabled={detail.task.status === 'SUBMITTED' || riskSubmitting}
                  required={
                    proposedTreatment === 'TERMINATE' ||
                    proposedTreatment === 'TRANSFER' ||
                    Boolean(detail.riskRecord.activeTreatmentPlan?.evidenceRequired?.trim())
                  }
                  requirementText={
                    proposedTreatment === 'TREAT'
                      ? detail.riskRecord.activeTreatmentPlan?.evidenceRequired
                      : undefined
                  }
                  onSelectFile={setEvidenceFile}
                  onEvidenceClass={setEvidenceClass}
                  onUpload={() => void uploadRiskEvidence()}
                  onDownload={(id) => void api.downloadEvidence(id)}
                  onRemove={(ev) => {
                    const recordId = String(detail.riskRecord!.id)
                    return void unlinkAndRefresh(ev, 'RISK', 'RISK_RECORD', recordId, async () => {
                      setLinkedEvidence(
                        await api.listEvidenceLinked('RISK', 'RISK_RECORD', recordId),
                      )
                    })
                  }}
                />
              )}
            </Stack>
              )}
                </Box>
              )}
            </Stack>
          )}
          {detail && capaMode && detail.capa && (
            <Stack spacing={1.5} sx={{ mt: 1 }}>
              <Typography variant="caption" color="text.secondary">
                {capaSourceLabel(detail.capa)} · {detail.capa.priority} · {detail.capa.controlRef ?? '-'}
              </Typography>
              {detail.capa.description && (
                <Typography variant="body2">{detail.capa.description}</Typography>
              )}
              {detail.capa.rejectionReason && (
                <Typography variant="body2" color="error">
                  Rejected: {detail.capa.rejectionReason}
                </Typography>
              )}
              <CompactTextField
                label={CAPA_EXCEL.rootCause}
                required
                multiline
                minRows={2}
                disabled={detail.task.status === 'SUBMITTED'}
                value={capaRootCause}
                onChange={(e) => setCapaRootCause(e.target.value)}
                helperText={!capaRootCause.trim() ? 'Required to submit' : undefined}
              />
              <CompactTextField
                label={CAPA_EXCEL.correctiveActionPlan}
                required
                multiline
                minRows={2}
                disabled={detail.task.status === 'SUBMITTED'}
                value={capaCorrective}
                onChange={(e) => setCapaCorrective(e.target.value)}
                helperText={!capaCorrective.trim() ? 'Required to submit' : undefined}
              />
              <CompactTextField
                label="Preventive action (optional)"
                multiline
                minRows={2}
                disabled={detail.task.status === 'SUBMITTED'}
                value={capaPreventive}
                onChange={(e) => setCapaPreventive(e.target.value)}
              />
              <CompactTextField
                label="Progress notes"
                multiline
                minRows={2}
                disabled={detail.task.status === 'SUBMITTED'}
                value={capaNotes}
                onChange={(e) => setCapaNotes(e.target.value)}
              />
              <ContributorEvidencePanel
                linkedEvidence={linkedEvidence}
                evidenceFile={evidenceFile}
                evidenceClass={evidenceClass}
                evidenceClasses={EVIDENCE_CLASSIFICATIONS}
                evidenceAccept={EVIDENCE_ACCEPT}
                busy={busy}
                disabled={detail.task.status === 'SUBMITTED'}
                required
                onSelectFile={setEvidenceFile}
                onEvidenceClass={setEvidenceClass}
                onUpload={() => void uploadCapaEvidence()}
                onDownload={(id) => void api.downloadEvidence(id)}
                onRemove={(ev) => {
                  const recordId = String(detail.capa!.id)
                  return void unlinkAndRefresh(ev, 'CAPA', 'CAPA', recordId, async () => {
                    setLinkedEvidence(await api.listEvidenceLinked('CAPA', 'CAPA', recordId))
                  })
                }}
              />
            </Stack>
          )}

          {detail && !riskMode && !capaMode && !biaMode && !ropaMode && !dpiaMode && !policyMode && !kriMode && !incidentMode && detail.gapItem && (
            <Box>
            <Stack spacing={2}>
              {detail.gapItem.reviewStatus === 'SENT_BACK' && (
                <ContributorGapReturnedCard reason={detail.gapItem.sendBackReason} />
              )}
              <Box sx={{ bgcolor: brand.paper, borderRadius: 1, p: 2 }}>
              {taskTitleCard}
              <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' },
                  columnGap: 3,
                  rowGap: 1,
                  alignItems: 'start',
                }}
              >
                <Box>
                  <Typography sx={{ fontSize: '0.75rem', color: brand.muted, lineHeight: 1.35 }}>
                    {(frameworkDisplayName(undefined, detail.frameworkCode) !== '-'
                      ? frameworkDisplayName(undefined, detail.frameworkCode)
                      : detail.task.sourceModule)}
                    {detail.assessmentName ? ` · ${detail.assessmentName}` : ''}
                  </Typography>
                  <Typography sx={{ fontSize: '0.75rem', color: brand.muted, lineHeight: 1.35 }}>
                    {detail.task.assignedByName ? `Assigned by ${detail.task.assignedByName}` : 'Assigned task'}
                  </Typography>
                  <Stack spacing={1.25} sx={{ mt: 1.5 }}>
                    {!isSamaTask(detail) ? (
                      <Box sx={{ maxWidth: { sm: 280 } }}>
                        <AdminNoteField
                          field="line"
                          label="Implementation status"
                          value={
                            detail.gapItem.implementationStatus
                              ? implStatusLabel(detail.gapItem.implementationStatus)
                              : ''
                          }
                        />
                      </Box>
                    ) : null}
                    <AdminNoteField field="scroll" label="Gap description" value={observations} />
                    <AdminNoteField field="scroll" label="Remarks" value={gapDescription} />
                    {detail.gapItem.reviewStatus === 'SENT_BACK' ? (
                      <Typography sx={{ fontSize: '0.6875rem', color: brand.muted }}>
                        Set by Admin. Update your answers and evidence, then submit.
                      </Typography>
                    ) : null}
                  </Stack>
                </Box>
                {(detail.gapItem.definitionText ||
                  detail.gapItem.whatItMeans?.trim() ||
                  detail.gapItem.evidenceGuidance?.trim()) && (
                  <Box>
                    <Typography sx={{ ...sectionLabelSx, mb: 0.5, color: brand.goldDark, fontWeight: 700 }}>About this control</Typography>
                    {detail.gapItem.definitionText ? (
                      <Typography
                        sx={{
                          fontSize: '0.75rem',
                          lineHeight: 1.4,
                          color: brand.ink,
                          ...narrativeReadScrollSx,
                        }}
                      >
                        {detail.gapItem.definitionText}
                      </Typography>
                    ) : null}
                    {detail.gapItem.whatItMeans?.trim() || detail.gapItem.evidenceGuidance?.trim() ? (
                      <Stack spacing={1} sx={{ pt: 1 }}>
                        {detail.gapItem.whatItMeans?.trim() ? (
                          <AdminNoteField label="What it means" value={detail.gapItem.whatItMeans} />
                        ) : null}
                        {detail.gapItem.evidenceGuidance?.trim() ? (
                          <AdminNoteField label="Evidence" value={detail.gapItem.evidenceGuidance} />
                        ) : null}
                      </Stack>
                    ) : null}
                  </Box>
                )}
              </Box>
              {detail.gapItem.reviewStatus === 'FINALIZED' && (
                <Typography sx={{ fontSize: '0.75rem', color: brand.muted }}>
                  Admin final status: {detail.gapItem.finalImplementationStatus?.replace(/_/g, ' ') ?? '-'}
                  {detail.gapItem.adminReviewNotes?.trim() ? `. ${detail.gapItem.adminReviewNotes}` : ''}
                </Typography>
              )}
              {isSamaTask(detail) ? (
                <Box sx={{ mt: 2 }}>
                  <SamaMaturityGatesEditor
                    gates={maturityGates}
                    onChange={setMaturityGates}
                    disabled={detail.task.status === 'SUBMITTED'}
                  />
                </Box>
              ) : null}
              </Box>
              {gapStepQuestions.length > 0 && (
                <ContributorGapQuestionStep
                  questions={gapStepQuestions}
                  questionIndex={gapQuestionIndex}
                  answers={questionAnswers}
                  controlCode={detail.gapItem.nodeCode}
                  controlTitle={detail.gapItem.title}
                  returned={detail.gapItem.reviewStatus === 'SENT_BACK'}
                  disabled={detail.task.status === 'SUBMITTED' || gapSubmitting}
                  onAnswer={(key, answer) =>
                    setQuestionAnswers((prev) => ({
                      ...prev,
                      [key]: { answer, note: prev[key]?.note ?? '' },
                    }))
                  }
                  onNote={(key, note) =>
                    setQuestionAnswers((prev) => ({
                      ...prev,
                      [key]: { answer: prev[key]?.answer ?? '', note },
                    }))
                  }
                  showUpload
                  linkedEvidence={currentQuestionEvidence}
                  evidenceFile={evidenceFile}
                  evidenceClass={evidenceClass}
                  evidenceClasses={EVIDENCE_CLASSIFICATIONS}
                  busy={gapEvidenceBusy}
                  evidenceAccept={EVIDENCE_ACCEPT}
                  maxBytes={RISK_EVIDENCE_MAX_BYTES}
                  maxFiles={RISK_EVIDENCE_MAX_FILES}
                  currentUserId={selfUserId}
                  onSelectFile={setEvidenceFile}
                  onEvidenceClass={setEvidenceClass}
                  onUpload={() => void uploadGapEvidence()}
                  onEvidenceError={setError}
                  onDownload={(id) => void api.downloadEvidence(id)}
                  onRemoveEvidence={(ev) => void removeGapEvidence(ev)}
                  onGoToQuestion={(index) => {
                    setEvidenceFile(null)
                    setGapQuestionIndex(index)
                  }}
                />
              )}
              {gapStepQuestions.length === 0 && (
                <Box>
                  <CompactTextField
                    label="Response"
                    required
                    multiline
                    minRows={3}
                    maxRows={NARRATIVE_MAX_ROWS}
                    maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
                    fieldIdPrefix="contrib-gap"
                    disabled={detail.task.status === 'SUBMITTED'}
                    value={contributorComments}
                    onChange={(e) => setContributorComments(e.target.value)}
                    error={Boolean(gapContributorTextIssue(contributorComments))}
                    helperText={
                      gapContributorTextIssue(contributorComments) ??
                      `Required when no questions are assigned. ${GAP_COMMENT_HINT}`
                    }
                  />
                  <Box sx={{ mt: 1.5 }}>
                    <ContributorEvidencePanel
                      headingUppercase
                      linkedEvidence={linkedEvidence}
                      evidenceFile={evidenceFile}
                      evidenceClass={evidenceClass}
                      evidenceClasses={EVIDENCE_CLASSIFICATIONS}
                      busy={gapEvidenceBusy}
                      disabled={detail.task.status === 'SUBMITTED' || gapSubmitting || busy}
                      emptyLabel="No files linked yet"
                      heading="Evidence (optional)"
                      evidenceAccept={EVIDENCE_ACCEPT}
                      maxBytes={RISK_EVIDENCE_MAX_BYTES}
                      maxFiles={RISK_EVIDENCE_MAX_FILES}
                      currentUserId={selfUserId}
                      capScopeLabel="this control"
                      onSelectFile={setEvidenceFile}
                      onEvidenceClass={setEvidenceClass}
                      onUpload={() => void uploadGapEvidence()}
                      onError={setError}
                      onDownload={(id) => void api.downloadEvidence(id)}
                      onRemove={(ev) => void removeGapEvidence(ev)}
                    />
                  </Box>
                </Box>
              )}
            </Stack>
            </Box>
          )}
            </Box>
            <Box
              sx={{
                display: 'flex',
                flexWrap: 'wrap',
                gap: 1,
                justifyContent: gapMode ? 'space-between' : 'flex-end',
                alignItems: 'center',
                flexShrink: 0,
                minHeight: gapMode ? 54 : undefined,
                px: 0.5,
                py: gapMode ? 0.75 : 1,
                bgcolor: brand.paper,
                borderTop: `1px solid ${brand.border}`,
              }}
            >
              {gapMode && gapStepQuestions.length > 0 && (
                <>
                  <Button
                    size="small"
                    variant="outlined"
                    disabled={gapQuestionIndex <= 0 || detail.task.status === 'SUBMITTED' || gapSubmitting || busy || gapEvidenceBusy}
                    onClick={() => {
                      setEvidenceFile(null)
                      setGapQuestionIndex((i) => Math.max(0, i - 1))
                    }}
                    sx={{ minHeight: 34 }}
                  >
                    ← Previous
                  </Button>
                  <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 0.5 }}>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1.25, alignItems: 'center', justifyContent: 'flex-end' }}>
                  <Button
                    size="small"
                    variant="outlined"
                    disabled={busy || gapSubmitting || gapEvidenceBusy || Boolean(gapCommentIssue) || detail.task.status === 'SUBMITTED'}
                    onClick={() => void saveGapDraft()}
                    sx={{ minHeight: 34 }}
                  >
                    {busy ? 'Saving…' : 'Save Draft'}
                  </Button>
                  {gapQuestionIndex < gapStepQuestions.length - 1 ? (
                    <Button
                      size="small"
                      variant="contained"
                      disabled={
                        busy ||
                        gapSubmitting ||
                        gapEvidenceBusy ||
                        detail.task.status === 'SUBMITTED' ||
                        Boolean(currentGapStepReason)
                      }
                      onClick={() => {
                        void (async () => {
                          const saved = await saveGapDraft()
                          if (!saved) return
                          setEvidenceFile(null)
                          setGapQuestionIndex((i) => i + 1)
                        })()
                      }}
                      sx={{ minHeight: 34, px: 2 }}
                    >
                      {busy ? 'Saving…' : 'Save & Next →'}
                    </Button>
                  ) : (
                    <Button
                      variant="contained"
                      disabled={detail.task.status === 'SUBMITTED' || Boolean(gapSubmitReason) || busy || gapSubmitting || gapEvidenceBusy}
                      onClick={() => void submitGap()}
                      sx={{ px: 2.5 }}
                    >
                      {gapSubmitting ? 'Submitting…' : 'Submit Control'}
                    </Button>
                  )}
                  </Box>
                  {gapQuestionIndex < gapStepQuestions.length - 1 &&
                  detail.task.status !== 'SUBMITTED' &&
                  currentGapStepReason &&
                  currentGapStepReason !== 'Select an assessment to continue' ? (
                    <Typography variant="caption" color="text.secondary">
                      {currentGapStepReason}
                    </Typography>
                  ) : gapQuestionIndex >= gapStepQuestions.length - 1 &&
                    detail.task.status !== 'SUBMITTED' &&
                    gapSubmitReason &&
                    gapSubmitReason !== 'Select an assessment on each question' ? (
                    <Typography variant="caption" color="text.secondary">
                      {gapSubmitReason}
                    </Typography>
                  ) : null}
                  </Box>
                </>
              )}
              {(!gapMode || gapStepQuestions.length === 0) && (
                <Button onClick={requestCloseTask}>Close</Button>
              )}
          {detail?.task.status !== 'SUBMITTED' && milestoneMode && (
            <Button
              variant="contained"
              disabled={busy || linkedEvidence.length === 0}
              onClick={() => void submitMilestone()}
            >
              Complete Milestone
            </Button>
          )}
          {detail?.task.status !== 'SUBMITTED' && riskMode && riskWorkTab === 'action' && !acceptanceApproveMode && (
            <Box>
              <Button
                variant="contained"
                disabled={busy || Boolean(riskSubmitReason)}
                onClick={() => void submitRisk()}
              >
                {busy ? 'Submitting…' : 'Submit'}
              </Button>
              {riskSubmitReason ? (
                <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
                  {riskSubmitReason}
                </Typography>
              ) : null}
            </Box>
          )}
          {detail?.task.status !== 'SUBMITTED' && capaMode && (
            <Box>
              <Button
                variant="contained"
                disabled={
                  !capaRootCause.trim() || !capaCorrective.trim() || linkedEvidence.length === 0 || busy
                }
                onClick={() => void submitCapa()}
              >
                Submit CAPA
              </Button>
              {!capaRootCause.trim() || !capaCorrective.trim() || linkedEvidence.length === 0 ? (
                <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
                  Add root cause, corrective action, and at least one file to submit
                </Typography>
              ) : null}
            </Box>
          )}
          {detail?.task.status !== 'SUBMITTED' && ropaMode && (
            <Button
              variant="contained"
              disabled={
                busy ||
                !linkedEvidence.some((e) => e.workflowStage === 'WORKBOOK_SUBMITTED') ||
                !linkedEvidence.some((e) => e.workflowStage === 'APPROVAL_EVIDENCE')
              }
              onClick={() => void submitRopa()}
            >
              Submit RoPA Package
            </Button>
          )}
          {detail?.task.status !== 'SUBMITTED' && dpiaMode && (
            <Button
              variant="contained"
              disabled={
                busy ||
                !linkedEvidence.some((e) => e.workflowStage === 'WORKBOOK_SUBMITTED') ||
                !linkedEvidence.some((e) => e.workflowStage === 'APPROVAL_EVIDENCE')
              }
              onClick={() => void submitDpia()}
            >
              Submit DPIA Package
            </Button>
          )}
          {detail?.task.status !== 'SUBMITTED' && policyMode && !detail?.policy?.alreadyAcknowledged && (
            <Button variant="contained" disabled={!policyReadConfirmed || busy} onClick={() => void acknowledgePolicy()}>
              Acknowledge
            </Button>
          )}
          {detail?.task.status !== 'SUBMITTED' && kriMode && (
            <Button
              variant="contained"
              disabled={kriValue.trim() === '' || busy}
              onClick={() => void submitKri()}
            >
              Submit Value
            </Button>
          )}
          {detail?.task.status !== 'SUBMITTED' &&
            incidentMode &&
            detail?.incident?.status !== 'RESOLVED' &&
            detail?.incident?.status !== 'CLOSED' && (
            <Box>
            <Button
              variant="contained"
              disabled={busy || (incidentResolve && !incidentNotes.trim())}
              onClick={() => void submitIncident()}
            >
              {incidentResolve ? 'Resolve' : 'Save Progress'}
            </Button>
            {incidentResolve && !incidentNotes.trim() ? (
              <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
                Add progress notes to resolve
              </Typography>
            ) : null}
            </Box>
          )}
          {gapMode && gapStepQuestions.length === 0 && (
            <Box>
            <Button
              variant="contained"
              disabled={detail.task.status === 'SUBMITTED' || Boolean(gapSubmitReason) || busy || gapSubmitting || gapEvidenceBusy}
              onClick={() => void submitGap()}
            >
              {gapSubmitting ? 'Submitting…' : 'Submit'}
            </Button>
            {detail.task.status !== 'SUBMITTED' && gapSubmitReason ? (
              <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
                {gapSubmitReason}
              </Typography>
            ) : null}
            </Box>
          )}
            </Box>
            </ContributorRail>
            </ContributorWorkArea>
          )}
      </Box>

      <Snackbar
        open={!!ok}
        autoHideDuration={3000}
        onClose={() => setOk('')}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
        message={ok}
      />
      <Snackbar
        open={!!error}
        autoHideDuration={5000}
        onClose={() => setError('')}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
        message={error}
      />
      <DiscardChangesDialog
        open={discardOpen}
        title="Discard unsaved task changes?"
        message="You have unsaved edits on this task. If you leave now, your changes will be lost."
        onKeepEditing={() => setDiscardOpen(false)}
        onDiscard={() => {
          setDiscardOpen(false)
          setDetail(null)
        }}
      />

      <Dialog open={reportOpen} onClose={() => setReportOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Report Incident</DialogTitle>
        <DialogContent>
          <Stack spacing={1.5} sx={{ mt: 1 }}>
            <CompactTextField
              label="Title"
              fullWidth
              required
              value={reportForm.title}
              onChange={(e) => setReportForm({ ...reportForm, title: e.target.value })}
            />
            <FormSelect
              label="Type"
              fullWidth
              value={reportForm.incidentType}
              onChange={(v) => setReportForm({ ...reportForm, incidentType: v as IncidentType })}
              options={(
                [
                  'SECURITY',
                  'OPERATIONAL',
                  'DATA_BREACH',
                  'PRIVACY',
                  'PHYSICAL',
                  'OTHER',
                ] as IncidentType[]
              ).map((t) => ({ value: t, label: t.replace(/_/g, ' ') }))}
            />
            <CompactTextField
              label="Description"
              fullWidth
              multiline
              minRows={3}
              value={reportForm.description ?? ''}
              onChange={(e) => setReportForm({ ...reportForm, description: e.target.value })}
            />
            <CompactTextField
              label="Affected area"
              fullWidth
              value={reportForm.affectedArea ?? ''}
              onChange={(e) => setReportForm({ ...reportForm, affectedArea: e.target.value })}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setReportOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={busy || !reportForm.title.trim()}
            onClick={() => void reportIncident()}
          >
            Submit Report
          </Button>
        </DialogActions>
      </Dialog>
    </AppShell>
    )}
    </AppShellPageProvider>
  )
}

function ContributorAssignedRegisters({
  active,
  biaHomeNonce = 0,
  session,
  onError,
  onOk,
  onNavChange,
  onOpenTask,
  onOpenAssignedGap,
  onOpenAssignedRisk,
  riskProjectContext,
  capaNotifyOpen = null,
}: {
  active: string
  biaHomeNonce?: number
  session: Session
  onError: (msg: string) => void
  onOk: (msg: string) => void
  onNavChange: (id: string) => void
  onOpenTask: (taskId: number) => void
  onOpenAssignedGap?: (projectId: number, gapAssessmentId: number | null) => void
  onOpenAssignedRisk?: (
    riskId: number,
    riskType: 'IS_RISK' | 'ERM' | 'PRIVACY_RISK',
    projectId: number | null,
  ) => boolean | Promise<boolean>
  /** Restore this project when returning from risk detail */
  riskProjectContext?: { projectId: number; riskType: 'IS_RISK' | 'ERM' | 'PRIVACY_RISK' } | null
  capaNotifyOpen?: { capaId: number; epoch: number } | null
}) {
  if (active === 'contributor-dashboard') {
    return (
      <ContributorDashboard
        session={session}
        onError={onError}
        onNavigate={onNavChange}
        onOpenTask={onOpenTask}
      />
    )
  }
  if (active === 'assessments') {
    return (
      <AssessmentsPage
        session={session}
        onError={onError}
        onOk={onOk}
        focus="gap"
        onOpenAssignedGap={onOpenAssignedGap}
      />
    )
  }
  if (active === 'risk') {
    return (
      <RiskPage
        session={session}
        riskType="IS_RISK"
        onError={onError}
        onOk={onOk}
        initialProjectId={riskProjectContext?.riskType === 'IS_RISK' ? riskProjectContext.projectId : null}
        onOpenAssignedRisk={
          onOpenAssignedRisk ? (riskId, projectId) => onOpenAssignedRisk(riskId, 'IS_RISK', projectId) : undefined
        }
      />
    )
  }
  if (active === 'erm-risk') {
    return (
      <RiskPage
        session={session}
        riskType="ERM"
        onError={onError}
        onOk={onOk}
        initialProjectId={riskProjectContext?.riskType === 'ERM' ? riskProjectContext.projectId : null}
        onOpenAssignedRisk={
          onOpenAssignedRisk ? (riskId, projectId) => onOpenAssignedRisk(riskId, 'ERM', projectId) : undefined
        }
      />
    )
  }
  if (active === 'audit-capa') {
    return (
      <AuditsPage
        session={session}
        navId="audit-capa"
        onNavChange={onNavChange}
        onError={onError}
        onOk={onOk}
        openCapaFromNotification={capaNotifyOpen}
      />
    )
  }
  if (active === 'bcms-bia') {
    return <BcmsBiaPage key={`bia-${biaHomeNonce}`} session={session} onError={onError} onOk={onOk} />
  }
  if (active === 'kri') {
    return <KriPage session={session} onError={onError} onOk={onOk} />
  }
  if (active === 'incidents') {
    return <IncidentsPage session={session} onError={onError} onOk={onOk} />
  }
  if (active === 'privacy-ropa') {
    return <PrivacyPage session={session} section="ropa" onError={onError} onOk={onOk} />
  }
  if (active === 'privacy-dpia' || active === 'privacy-dpia-projects') {
    return <PrivacyPage session={session} section="dpia-projects" onError={onError} onOk={onOk} />
  }
  if (active === 'privacy-dpia-questionnaires') {
    return <PrivacyPage session={session} section="dpia-questionnaires" onError={onError} onOk={onOk} />
  }
  if (active === 'privacy-risk') {
    return (
      <RiskPage
        session={session}
        riskType="PRIVACY_RISK"
        onError={onError}
        onOk={onOk}
        initialProjectId={riskProjectContext?.riskType === 'PRIVACY_RISK' ? riskProjectContext.projectId : null}
        onOpenAssignedRisk={
          onOpenAssignedRisk ? (riskId, projectId) => onOpenAssignedRisk(riskId, 'PRIVACY_RISK', projectId) : undefined
        }
      />
    )
  }
  if (active === 'policies') {
    return <PoliciesPage session={session} onError={onError} onOk={onOk} />
  }
  return null
}

export function isContributorOnlySession(session: Session): boolean {
  const elevated = ['OWNER', 'ADMIN', 'GLOBAL_READER', 'INTERNAL_AUDITOR', 'EXTERNAL_AUDITOR', 'SUPER_ADMIN']
  return session.roles.includes('CONTRIBUTOR') && !session.roles.some((r) => elevated.includes(r))
}
