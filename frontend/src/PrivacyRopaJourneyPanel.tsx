import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  Checkbox,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  ToggleButton,
  ToggleButtonGroup,
  Tooltip,
  Typography,
} from '@mui/material'
import {
  api,
  ROPA_DESIGNATIONS,
  ROPA_FREQUENCIES,
  ROPA_LEGAL_BASES,
  type CreatePrivacyRopaBody,
  type OrgUnitNode,
  type PrivacyRopa,
  type RopaDepartment,
  type RopaLegalBasis,
  type RopaProject,
  type UserSummary,
} from './api'
import StatusChip from './components/StatusChip'
import BiaJourneyPanel, { type BiaJourneyStep } from './components/bcms/BiaJourneyPanel'
import RopaProcessExcelBar from './components/privacy/RopaProcessExcelBar'
import BiaCompactMultiSelect from './components/bcms/BiaCompactMultiSelect'
import {
  CompactTextField,
  ConfirmActionDialog,
  FormSectionShell,
  FormSelect,
  KpiCard,
  PageLoading,
  RegisterToolbar,
  SettingsCard,
  SimpleFormLayout,
} from './components/ui'
import { formFieldGridSx, sectionAnchorSx, twoColFieldSx } from './components/ui/formLayout'
import { useAppShellPage } from './components/AppShellPageContext'
import {
  useOptionalUnsavedChangesGuardApi,
  useRegisterUnsavedChanges,
} from './uiQuality/UnsavedChangesGuard'
import { brand, semantic } from './theme'

/* Client RoPA sheet (Record of Processing Activities RoPA.xlsx): value sets from
   the sheet dropdown validations (row 5 / cell comments). Keep in sync with
   backend RopaSheetFields, which rejects values outside these lists. */
const PERSONAL_DATA = [
  'Name',
  'Date of Birth/Age',
  'Address',
  'Email',
  'Telephone',
  'Bank Account Details',
  'Photo',
  'Passport',
  'Visa',
  'Work Permits',
]
const SENSITIVE_DATA = [
  'Family or ethnic origin',
  'Political/philosophical opinions',
  'Religious beliefs',
  'Criminal record',
  'Biometric data',
  'Health-related information',
  "Data related to the provision of healthcare services that reveals the person's health status",
]
const SUBJECT_CATEGORIES = ['Employees', 'Customers', 'Other individuals e.g., Third party service providers']
const VOLUMES = ['0-1000', '1,000-100,000', '100,000-1M', 'over 1M']

/** Sheet row 3 labels for the Data Controller or Data Processor dropdown (col C). */
const DESIGNATION_LABELS: Record<string, string> = {
  CONTROLLER: 'Controller',
  JOINT_CONTROLLER: 'Joint Controller',
  PROCESSOR: 'Processor',
}

/** Sheet row 5 labels for the Legal basis dropdown (col L), keyed by stored enum. */
const LEGAL_BASIS_LABELS: Record<string, string> = {
  CONSENT: 'Consent',
  PROTECT_PUBLIC_INTEREST: 'Protect public interest',
  DATA_SUBJECT_ACTIONS: "Data subject's actions",
  LEGAL_CLAIMS_DEFENSE: 'Legal claims/defense',
  JUDICIAL_SECURITY_PROCEDURES: 'Judicial/security procedures',
  OCCUPATIONAL_HEALTH_ASSESSMENTS: 'Occupational health assessments',
  PUBLIC_HEALTH_PROTECTION: 'Public health protection',
  ARCHIVAL_SCIENTIFIC_STUDIES: 'Archival/scientific studies',
  PROTECTING_DATA_SUBJECT_INTERESTS: "Protecting data subject's interests",
  EMPLOYMENT_SOCIAL_SECURITY: 'Employment/social security obligations',
  CONTRACT_PERFORMANCE: 'Contract performance',
  COMPLIANCE_WITH_OTHER_LAWS: 'Compliance with other laws',
  OTHER_CASES_AS_PER_REGULATIONS: 'Other cases as per regulations',
}

/**
 * Sheet row 4 ("Column Hover") text. Shown only after the mouse has rested on
 * the field for a full second, every time: MUI Tooltip's own enterDelay has a
 * hysteresis that opens the next tooltip instantly once one has shown, which
 * made tooltips fire on every mouse pass across the form. A controlled open
 * state with our own timer has no hysteresis. Clicking or tabbing into the
 * field cancels a pending tooltip so it never covers typing.
 */
function HoverField({ text, children }: { text: string; children: ReactNode }) {
  const [open, setOpen] = useState(false)
  const timer = useRef<number | null>(null)
  const anchor = useRef<{ x: number; y: number } | null>(null)
  const cancel = () => {
    if (timer.current != null) {
      window.clearTimeout(timer.current)
      timer.current = null
    }
  }
  const hide = () => {
    cancel()
    anchor.current = null
    setOpen(false)
  }
  const rest = (e: { clientX: number; clientY: number }) => {
    cancel()
    anchor.current = { x: e.clientX, y: e.clientY }
    timer.current = window.setTimeout(() => setOpen(true), 1000)
  }
  // A running mouse keeps resetting the timer: the tooltip only opens after
  // a full second with no real movement (small hand jitter is tolerated).
  const move = (e: { clientX: number; clientY: number }) => {
    if (open) return
    const a = anchor.current
    if (!a) return
    if (Math.abs(e.clientX - a.x) + Math.abs(e.clientY - a.y) > 8) rest(e)
  }
  useEffect(() => cancel, [])
  return (
    <Tooltip
      arrow
      open={open}
      disableHoverListener
      disableFocusListener
      disableTouchListener
      placement="top-start"
      title={<span style={{ whiteSpace: 'pre-line' }}>{text}</span>}
      slotProps={{ tooltip: { sx: { maxWidth: 420, fontSize: '0.6875rem', lineHeight: 1.45 } } }}
    >
      <Box
        sx={{ width: '100%', minWidth: 0 }}
        onMouseEnter={rest}
        onMouseMove={move}
        onMouseLeave={hide}
        onMouseDown={hide}
        onFocus={hide}
      >
        {children}
      </Box>
    </Tooltip>
  )
}

const HOVER = {
  activity:
    'What is the processing activity that you process personal data for (e.g., for particular services...)?',
  designation:
    'Controller: determines the purpose and means when processing personal data.\nJoint Controller: determines the purpose and means together with other Controllers.\nProcessor: takes instructions from the Controller (e.g., service provider).',
  description: 'Activities performed as part of the processing activity',
  personalData:
    'Name, Date of Birth/Age, Address (Personal), Email, telephone, Bank Account Details, Photo, Passport, Visa, Work Permits',
  sensitiveData:
    "Family or ethnic origin, political/philosophical opinions, religious beliefs, criminal record, biometric data, health-related information, including physical, psychological, mental, genetic, or sexual condition, data related to the provision of healthcare services that reveals the person's health status",
  dataSubjects: 'Employees, customers, other individuals e.g., third party service providers',
  volume: '0-1000; 1,000-100,000; 100,000-1M; or over 1M. (Per month)',
  frequency:
    'Indicate how often personal data is processed in relation to each activity. Common frequencies include:\n- Daily\n- Weekly\n- Monthly\n- Quarterly\n- Annually\n- Ad hoc\n- One off',
  source: 'Data Subject or other source (e.g. third party)',
  method: 'In writing, verbally, email, online form, or other method',
  legalBasis:
    "If the personal data is received directly from the data subject:\n- Consent\n- Protect public interest\n- Data subject's actions\n- Legal claims/defense\n- Judicial/security procedures\n- Occupational health assessments\n- Public health protection\n- Archival/scientific studies\n- Protecting data subject's interests\n- Employment/social security obligations\n- Contract performance\n- Compliance with other laws\n- Other cases as per regulations",
  internal:
    'Do you share the personal data with any department within DCD and/or Operators? If yes, please identify the department',
  external:
    'Do you share the personal data with any external entities (third parties)? If yes, please specify which ones, their sectors of operation',
  location: 'Location of an external entity (country)',
  adequacy:
    '1. State or Province has legislation addressing Personal Data Protection, including privacy measures, controls, and enforcement by a regulatory authority.\n\n2. State or Province has a bilateral or multilateral agreement on Personal Data Protection with the receiving country.\n(Reference to Article 22 of the UAE PDPL)',
  safeguards:
    "1. Personal Data may be transferred if a contract imposes protective measures, even if the receiving country has no data protection laws.\n\n2. Transfer is allowed if the Data Subject gives explicit consent, and it doesn't conflict with public or security interests.\n\n3. Transfer is necessary to fulfil legal obligations or defend rights before judicial entities.\n\n4. Transfer is needed to sign or implement a contract in the Data Subject's interest.\n\n5. Transfer is required for international judicial cooperation.\n\n6. Transfer may occur to protect the public interest.\n(Reference to Article 23 of the UAE PDPL)",
  storage: 'Systems / other location where the personal data is stored (please provide as much information as possible)',
  retention: 'Retention period of personal data and the reason (e.g.,... etc.)',
  security: 'General description of measures, e.g., restriction of access, encryption, policies, procedures, etc.',
  comments: 'Please specify any comments relevant to the personal data processing activity',
} as const
function RailValue({ children }: { children: string }) {
  return (
    <Box component="span" sx={{ display: 'block', fontSize: '1.125rem', lineHeight: 1.2, overflowWrap: 'anywhere' }}>
      {children}
    </Box>
  )
}

const DEPT_STATUSES = [
  'NOT_STARTED',
  'IN_PROGRESS',
  'UNDER_REVIEW',
  'READY_FOR_HEAD_SIGN_OFF',
  'PENDING_HEAD_SIGN_OFF',
  'APPROVED',
  'CLOSED',
] as const

function flattenOrg(nodes: OrgUnitNode[]): OrgUnitNode[] {
  const out: OrgUnitNode[] = []
  const walk = (list: OrgUnitNode[]) => {
    for (const n of list) {
      out.push(n)
      if (n.children?.length) walk(n.children)
    }
  }
  walk(nodes)
  return out
}

function parseList(raw: string | null | undefined): string[] {
  if (!raw?.trim()) return []
  try {
    const parsed = JSON.parse(raw) as unknown
    if (Array.isArray(parsed)) return parsed.map((v) => String(v))
  } catch {
    /* legacy comma text */
  }
  return raw
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
}

function statusLabel(s: string): string {
  return s.replace(/_/g, ' ')
}

function deptTone(s: string): 'success' | 'info' | 'warning' | 'neutral' {
  if (s === 'CLOSED' || s === 'APPROVED') return 'success'
  if (s === 'UNDER_REVIEW' || s === 'PENDING_HEAD_SIGN_OFF' || s === 'READY_FOR_HEAD_SIGN_OFF') return 'info'
  if (s === 'IN_PROGRESS') return 'warning'
  return 'neutral'
}

const baselineFieldGridSx = { ...formFieldGridSx, alignItems: 'end' as const }

/** Arbor ROPA DPO sheet: any Yes → DPIA required (no free override). */
const DPIA_CRITERIA: { key: 'critLargeSensitive' | 'critSystematic' | 'critAutomated' | 'critLegalImpact'; label: string }[] = [
  {
    key: 'critLargeSensitive',
    label: 'Will the processing involve a large volume of Sensitive Personal Data?',
  },
  {
    key: 'critSystematic',
    label:
      'Will the processing involve a systematic and comprehensive assessment of the personal aspects of the Data Subject?',
  },
  {
    key: 'critAutomated',
    label:
      'Will the processing involve automated processing of personal data, including profiling of the Data Subject?',
  },
  {
    key: 'critLegalImpact',
    label: 'Will the processing have legal consequences or serious impact on the Data Subject?',
  },
]

type CritAnswer = '' | 'YES' | 'NO'

function critFromApi(v: boolean | null | undefined): CritAnswer {
  if (v === true) return 'YES'
  if (v === false) return 'NO'
  return ''
}

function critToBool(v: CritAnswer): boolean | null {
  if (v === 'YES') return true
  if (v === 'NO') return false
  return null
}

function emptyReviewDraft() {
  return {
    critLargeSensitive: '' as CritAnswer,
    critSystematic: '' as CritAnswer,
    critAutomated: '' as CritAnswer,
    critLegalImpact: '' as CritAnswer,
    dpiaComments: '',
    action: 'REVIEWED' as 'REVIEWED' | 'RETURNED',
    comments: '',
  }
}

function DpiaCriteriaYesNo({
  value,
  onChange,
  readOnly,
}: {
  value: CritAnswer
  onChange?: (v: CritAnswer) => void
  readOnly?: boolean
}) {
  if (readOnly) {
    return (
      <Typography
        variant="body2"
        sx={{
          fontWeight: 700,
          color: value === 'YES' ? semantic.risk.high : value === 'NO' ? semantic.risk.low : brand.muted,
        }}
      >
        {value === 'YES' ? 'Yes' : value === 'NO' ? 'No' : '-'}
      </Typography>
    )
  }
  return (
    <ToggleButtonGroup
      exclusive
      size="small"
      value={value || null}
      onChange={(_e, next) => {
        if (next === 'YES' || next === 'NO') onChange?.(next)
      }}
      sx={{
        '& .MuiToggleButton-root': {
          px: 1.5,
          py: 0.25,
          fontSize: '0.75rem',
          fontWeight: 600,
          textTransform: 'none',
          borderColor: brand.border,
          minWidth: 52,
        },
        '& .MuiToggleButton-root[value="YES"].Mui-selected': {
          bgcolor: '#FEE2E2',
          color: semantic.risk.high,
          borderColor: semantic.risk.high,
          '&:hover': { bgcolor: '#FECACA' },
        },
        '& .MuiToggleButton-root[value="NO"].Mui-selected': {
          bgcolor: '#DCFCE7',
          color: semantic.risk.low,
          borderColor: semantic.risk.low,
          '&:hover': { bgcolor: '#BBF7D0' },
        },
      }}
    >
      <ToggleButton value="YES">Yes</ToggleButton>
      <ToggleButton value="NO">No</ToggleButton>
    </ToggleButtonGroup>
  )
}

const emptyProcess = (): CreatePrivacyRopaBody => ({
  activityName: '',
  legalBasis: 'CONSENT',
  designation: 'CONTROLLER',
  shareExternal: false,
  intakeMode: 'FORM',
})

export default function PrivacyRopaJourneyPanel({
  canWrite,
  onError,
  onOk,
}: {
  canWrite: boolean
  onError: (m: string) => void
  onOk: (m: string) => void
}) {
  const [projects, setProjects] = useState<RopaProject[]>([])
  const [loading, setLoading] = useState(true)
  const [projectName, setProjectName] = useState('')
  const [projectYear, setProjectYear] = useState(() => new Date().getFullYear())
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null)
  const [selectedDeptId, setSelectedDeptId] = useState<number | null>(null)
  const [creatingProject, setCreatingProject] = useState(false)
  const [creatingDepartment, setCreatingDepartment] = useState(false)
  const [processScreenOpen, setProcessScreenOpen] = useState(false)
  const [processes, setProcesses] = useState<PrivacyRopa[]>([])
  const [orgUnits, setOrgUnits] = useState<OrgUnitNode[]>([])
  const [users, setUsers] = useState<UserSummary[]>([])
  const [myChampionDeptIds, setMyChampionDeptIds] = useState<Set<number>>(new Set())
  const [myHeadDeptIds, setMyHeadDeptIds] = useState<Set<number>>(new Set())
  const [search, setSearch] = useState('')
  const [projectStatusFilter, setProjectStatusFilter] = useState('ALL')
  const [deptFilter, setDeptFilter] = useState('ALL')
  const [deptForm, setDeptForm] = useState({ orgUnitId: '', headUserId: '', championUserId: '' })
  const [assignForm, setAssignForm] = useState({ headUserId: '', championUserId: '' })
  const [processForm, setProcessForm] = useState<CreatePrivacyRopaBody>(emptyProcess)
  const [editingProcessId, setEditingProcessId] = useState<number | null>(null)
  const [dirty, setDirty] = useState(false)
  const [reviewDraft, setReviewDraft] = useState<{
    critLargeSensitive: '' | 'YES' | 'NO'
    critSystematic: '' | 'YES' | 'NO'
    critAutomated: '' | 'YES' | 'NO'
    critLegalImpact: '' | 'YES' | 'NO'
    dpiaComments: string
    action: 'REVIEWED' | 'RETURNED'
    comments: string
  }>({
    critLargeSensitive: '',
    critSystematic: '',
    critAutomated: '',
    critLegalImpact: '',
    dpiaComments: '',
    action: 'REVIEWED',
    comments: '',
  })
  const [headDraft, setHeadDraft] = useState<{ comments: string }>({ comments: '' })
  // Multi-select review (like BC Head in BIA): decide several processes at once.
  // DPO bulk is Return-only; Mark Reviewed needs the four DPIA criteria per process.
  const [dpoSelectedIds, setDpoSelectedIds] = useState<number[]>([])
  const [headSelectedIds, setHeadSelectedIds] = useState<number[]>([])
  const [bulkReviewComments, setBulkReviewComments] = useState('')
  const [bulkHeadComments, setBulkHeadComments] = useState('')
  const [headNotes, setHeadNotes] = useState('')
  const [confirmCancelDpia, setConfirmCancelDpia] = useState(false)
  const [confirmSendHead, setConfirmSendHead] = useState(false)
  const [confirmCloseDept, setConfirmCloseDept] = useState(false)
  const [busy, setBusy] = useState(false)
  const guardApi = useOptionalUnsavedChangesGuardApi()

  const load = useCallback(async () => {
    try {
      const [projs, org, tasks] = await Promise.all([
        api.listRopaProjects(),
        api.listOrgUnits().catch(() => [] as OrgUnitNode[]),
        api.listMyTasks().catch(() => []),
      ])
      setProjects(projs)
      setOrgUnits(flattenOrg(org))
      const mine = tasks.filter((t) => t.sourceModule === 'PRIVACY')
      setMyChampionDeptIds(
        new Set(mine.filter((t) => t.sourceRecordType === 'ROPA_DEPARTMENT').map((t) => t.sourceRecordId)),
      )
      setMyHeadDeptIds(new Set(mine.filter((t) => t.sourceRecordType === 'ROPA_HEAD').map((t) => t.sourceRecordId)))
      if (canWrite) {
        const us = await api.listUsers().catch(() => [] as UserSummary[])
        setUsers(us)
      }
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Failed to load ROPA projects')
    } finally {
      setLoading(false)
    }
  }, [canWrite, onError])

  useEffect(() => {
    void load()
  }, [load])

  const project = useMemo(
    () => projects.find((p) => p.id === selectedProjectId) ?? null,
    [projects, selectedProjectId],
  )
  const myDeptIds = useMemo(
    () => new Set([...myChampionDeptIds, ...myHeadDeptIds]),
    [myChampionDeptIds, myHeadDeptIds],
  )
  const visibleDepartments = useMemo(() => {
    if (!project) return []
    if (canWrite) return project.departments
    return project.departments.filter((d) => myDeptIds.has(d.id))
  }, [project, canWrite, myDeptIds])
  const department = useMemo(
    () => visibleDepartments.find((d) => d.id === selectedDeptId) ?? null,
    [visibleDepartments, selectedDeptId],
  )

  const loadProcesses = useCallback(
    async (deptId: number | null) => {
      if (deptId == null) {
        setProcesses([])
        return
      }
      try {
        setProcesses(await api.listRopaProcesses(deptId))
      } catch (e) {
        onError(e instanceof Error ? e.message : 'Failed to load processing activities')
      }
    },
    [onError],
  )

  useEffect(() => {
    void loadProcesses(department?.id ?? null)
  }, [department?.id, loadProcesses])

  useEffect(() => {
    if (!department) return
    setAssignForm({
      headUserId: department.departmentHeadUserId ? String(department.departmentHeadUserId) : '',
      championUserId: department.championUserId ? String(department.championUserId) : '',
    })
    // Sync the assign card when a different department is opened, not on every refresh.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [department?.id])

  // Bulk review selection never carries across departments or workflow stages.
  useEffect(() => {
    setDpoSelectedIds([])
    setHeadSelectedIds([])
    setBulkReviewComments('')
    setBulkHeadComments('')
  }, [department?.id, department?.status])

  const refresh = async () => {
    await load()
    await loadProcesses(department?.id ?? null)
  }

  const act = async (fn: () => Promise<unknown>, ok: string) => {
    setBusy(true)
    try {
      await fn()
      onOk(ok)
      await refresh()
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Action failed')
    } finally {
      setBusy(false)
    }
  }

  const championUsers = users.filter((u) => u.roles.includes('CONTRIBUTOR'))
  // Word journey: only the Privacy Champion fills and submits. Admin (DPO) reviews.
  const canFill =
    department != null && department.status === 'IN_PROGRESS' && myChampionDeptIds.has(department.id)
  // Sign-off is the assigned Department Head's alone: Admin (DPO) waits, then closes.
  const isHeadViewer = department != null && myHeadDeptIds.has(department.id)
  const isChampionViewer = department != null && myChampionDeptIds.has(department.id)
  const selectedProcess = useMemo(
    () => processes.find((p) => p.id === editingProcessId) ?? null,
    [processes, editingProcessId],
  )
  const canEditProcess =
    canFill &&
    (editingProcessId == null ||
      selectedProcess?.reviewOutcome !== 'REVIEWED' ||
      selectedProcess?.headOutcome === 'RETURNED')

  const setField = (patch: Partial<CreatePrivacyRopaBody>) => {
    setProcessForm((prev) => ({ ...prev, ...patch }))
    setDirty(true)
  }

  const setList = (key: 'dataCategories' | 'sensitiveDataCategories' | 'dataSubjects', next: string[]) => {
    setField({ [key]: JSON.stringify(next) } as Partial<CreatePrivacyRopaBody>)
  }

  const selectedLegal = useMemo(() => {
    try {
      const parsed = processForm.legalBasesJson ? (JSON.parse(processForm.legalBasesJson) as string[]) : []
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return processForm.legalBasis ? [processForm.legalBasis] : []
    }
  }, [processForm.legalBasesJson, processForm.legalBasis])

  const setLegalFromLabels = (labels: string[]) => {
    const next = labels
      .map((label) => Object.keys(LEGAL_BASIS_LABELS).find((k) => LEGAL_BASIS_LABELS[k] === label) ?? label)
      .filter((v) => v in LEGAL_BASIS_LABELS)
    setField({ legalBasesJson: JSON.stringify(next), legalBasis: (next[0] as RopaLegalBasis) ?? 'CONSENT' })
  }

  const processSaveDisabledReason = !processForm.activityName?.trim()
    ? 'Processing Activity is required'
    : !processForm.designation
      ? 'Data Controller or Data Processor is required'
      : !processForm.processingDescription?.trim()
        ? 'Description of processing is required'
        : parseList(processForm.dataCategories).length === 0
          ? 'List of Personal Data is required'
          : parseList(processForm.sensitiveDataCategories).length === 0
            ? 'Categories of Sensitive Data is required'
            : parseList(processForm.dataSubjects).length === 0
              ? 'Data Subject categories is required'
              : !processForm.dataSubjectVolume?.trim()
                ? 'No. of data subjects is required'
                : !processForm.processingFrequency?.trim()
                  ? 'Frequency of processing is required'
                  : !processForm.sourceOfPersonalData?.trim()
                    ? 'Source of Personal Data is required'
                    : !processForm.methodOfReceiving?.trim()
                      ? 'Method of receiving Personal Data is required'
                      : selectedLegal.length === 0
                        ? 'Legal basis for processing is required'
                        : !processForm.shareInternalDetails?.trim()
                          ? 'Internal department is required'
                          : processForm.shareExternal && !processForm.shareExternalDetails?.trim()
                            ? 'External entity detail is required when External entities is Yes'
                            : !processForm.externalEntityLocation?.trim()
                              ? 'Location is required'
                              : !processForm.adequacyNote?.trim()
                                ? 'Adequate level of data protection is required'
                                : !processForm.safeguardsImplemented?.trim()
                                  ? 'Safeguards implemented is required'
                                  : !processForm.storageSystems?.trim()
                                    ? 'Storage systems / location is required'
                                    : !processForm.retentionPeriod?.trim()
                                      ? 'Retention period is required'
                                      : !processForm.securityMeasures?.trim()
                                        ? 'Security measures is required'
                                        : null

  const openProcess = (row: PrivacyRopa | null) => {
    if (row) {
      setEditingProcessId(row.id)
      setProcessForm({
        activityName: row.activityName,
        designation: row.designation,
        processingDescription: row.processingDescription ?? '',
        dataCategories: row.dataCategories ?? '',
        sensitiveDataCategories: row.sensitiveDataCategories ?? '',
        dataSubjects: row.dataSubjects ?? '',
        dataSubjectVolume: row.dataSubjectVolume ?? '',
        processingFrequency: row.processingFrequency ?? '',
        sourceOfPersonalData: row.sourceOfPersonalData ?? '',
        methodOfReceiving: row.methodOfReceiving ?? '',
        legalBasis: row.legalBasis,
        legalBasesJson: row.legalBasesJson ?? '',
        shareInternalDetails: row.shareInternalDetails ?? '',
        shareExternal: row.shareExternal,
        shareExternalDetails: row.shareExternalDetails ?? '',
        externalEntityLocation: row.externalEntityLocation ?? '',
        adequacyNote: row.adequacyNote ?? '',
        safeguardsImplemented: row.safeguardsImplemented ?? '',
        storageSystems: row.storageSystems ?? '',
        retentionPeriod: row.retentionPeriod ?? '',
        securityMeasures: row.securityMeasures ?? '',
        intakeMode: 'FORM',
      })
      setReviewDraft({
        critLargeSensitive: critFromApi(row.dpiaCritLargeSensitive),
        critSystematic: critFromApi(row.dpiaCritSystematicAssessment),
        critAutomated: critFromApi(row.dpiaCritAutomatedProfiling),
        critLegalImpact: critFromApi(row.dpiaCritLegalSeriousImpact),
        dpiaComments: row.dpiaComments ?? '',
        action: row.reviewOutcome === 'RETURNED' ? 'RETURNED' : 'REVIEWED',
        comments: row.comments ?? '',
      })
      setHeadDraft({ comments: row.headComments ?? '' })
    } else {
      setEditingProcessId(null)
      setProcessForm(emptyProcess())
      setReviewDraft(emptyReviewDraft())
      setHeadDraft({ comments: '' })
    }
    setDirty(false)
    setProcessScreenOpen(true)
  }

  const closeProcessScreen = () => {
    setProcessScreenOpen(false)
    setEditingProcessId(null)
    setProcessForm(emptyProcess())
    setDirty(false)
  }

  /** Persist the process form. Throws so the leave-guard can keep the dialog open on failure. */
  const persistProcess = async () => {
    if (!department) throw new Error('No department is open')
    if (processSaveDisabledReason) throw new Error(processSaveDisabledReason)
    if (editingProcessId != null) {
      await api.updateRopaProcess(department.id, editingProcessId, { ...processForm, draft: false })
    } else {
      await api.createRopaProcess(department.id, { ...processForm, draft: false })
    }
    setDirty(false)
    await loadProcesses(department.id)
    await load()
  }

  /**
   * Save the process as a draft at any point: partial fields are fine, the
   * record stays in the list marked Draft, and the department cannot be
   * submitted until every draft is completed with Save Process.
   */
  const persistDraft = async () => {
    if (!department) throw new Error('No department is open')
    const body: CreatePrivacyRopaBody = {
      ...processForm,
      activityName: processForm.activityName?.trim() ? processForm.activityName : 'Untitled draft',
      draft: true,
    }
    const saved =
      editingProcessId != null
        ? await api.updateRopaProcess(department.id, editingProcessId, body)
        : await api.createRopaProcess(department.id, body)
    setEditingProcessId(saved.id)
    setDirty(false)
    await loadProcesses(department.id)
    await load()
    return saved
  }

  const saveDraftProcess = async () => {
    if (!department) return
    setBusy(true)
    try {
      await persistDraft()
      onOk('Draft saved. You can finish it any time from the process list')
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Draft save failed')
    } finally {
      setBusy(false)
    }
  }

  const saveProcess = async () => {
    if (!department || processSaveDisabledReason) return
    setBusy(true)
    try {
      const editing = editingProcessId != null
      await persistProcess()
      onOk(editing ? 'Processing activity updated' : 'Processing activity added')
      closeProcessScreen()
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Save failed')
    } finally {
      setBusy(false)
    }
  }

  const reviewCriteriaComplete =
    reviewDraft.critLargeSensitive !== '' &&
    reviewDraft.critSystematic !== '' &&
    reviewDraft.critAutomated !== '' &&
    reviewDraft.critLegalImpact !== ''
  const reviewDerivedDpiaRequired =
    reviewDraft.critLargeSensitive === 'YES' ||
    reviewDraft.critSystematic === 'YES' ||
    reviewDraft.critAutomated === 'YES' ||
    reviewDraft.critLegalImpact === 'YES'

  /** Apply the DPO review draft. Throws for the leave-guard, like persistProcess. */
  const persistReview = async () => {
    if (!department || !selectedProcess) throw new Error('No process is open')
    if (reviewDraft.action === 'REVIEWED' && !reviewCriteriaComplete) {
      throw new Error('Answer all four DPIA criteria before marking Reviewed')
    }
    await api.reviewRopaProcess(department.id, selectedProcess.id, {
      dpiaComments: reviewDraft.dpiaComments || null,
      comments: reviewDraft.comments || null,
      outcome: reviewDraft.action,
      dpiaCritLargeSensitive: critToBool(reviewDraft.critLargeSensitive),
      dpiaCritSystematicAssessment: critToBool(reviewDraft.critSystematic),
      dpiaCritAutomatedProfiling: critToBool(reviewDraft.critAutomated),
      dpiaCritLegalSeriousImpact: critToBool(reviewDraft.critLegalImpact),
    })
    await loadProcesses(department.id)
    await load()
  }

  const reviewProcess = () => {
    if (!department || !selectedProcess) return
    const outcome = reviewDraft.action
    void act(
      persistReview,
      outcome === 'RETURNED'
        ? 'Returned to Privacy Champion'
        : reviewDerivedDpiaRequired
          ? 'Reviewed. DPIA required (from criteria). Open DPIA Projects to assess.'
          : 'Reviewed. No DPIA required',
    ).then(() => closeProcessScreen())
  }

  /* ----- Whole-ROPA unsaved-changes guard -----
   * One registration covers every editable ROPA surface: process form, DPO
   * review draft, Head comments/notes, bulk comments, assign card, and the
   * project/department create forms. Back buttons and left-nav both go
   * through this guard (Keep editing / Save / Discard). Save is offered only
   * where a real save exists (process form, DPO review); the rest are
   * protected by Keep editing / Discard. */
  const reviewDraftDirty =
    canWrite &&
    department?.status === 'UNDER_REVIEW' &&
    processScreenOpen &&
    selectedProcess != null &&
    (reviewDraft.critLargeSensitive !== critFromApi(selectedProcess.dpiaCritLargeSensitive) ||
      reviewDraft.critSystematic !== critFromApi(selectedProcess.dpiaCritSystematicAssessment) ||
      reviewDraft.critAutomated !== critFromApi(selectedProcess.dpiaCritAutomatedProfiling) ||
      reviewDraft.critLegalImpact !== critFromApi(selectedProcess.dpiaCritLegalSeriousImpact) ||
      reviewDraft.dpiaComments !== (selectedProcess.dpiaComments ?? '') ||
      reviewDraft.action !== (selectedProcess.reviewOutcome === 'RETURNED' ? 'RETURNED' : 'REVIEWED') ||
      reviewDraft.comments !== (selectedProcess.comments ?? ''))
  const headDraftDirty =
    department?.status === 'PENDING_HEAD_SIGN_OFF' &&
    department != null &&
    myHeadDeptIds.has(department.id) &&
    processScreenOpen &&
    selectedProcess != null &&
    headDraft.comments !== (selectedProcess.headComments ?? '')
  const headNotesDirty =
    !processScreenOpen && department?.status === 'PENDING_HEAD_SIGN_OFF' && headNotes.trim() !== ''
  const assignDirty =
    department != null &&
    !processScreenOpen &&
    (department.status === 'NOT_STARTED' ||
      department.status === 'IN_PROGRESS' ||
      department.status === 'READY_FOR_HEAD_SIGN_OFF') &&
    (assignForm.championUserId !== (department.championUserId ? String(department.championUserId) : '') ||
      assignForm.headUserId !== (department.departmentHeadUserId ? String(department.departmentHeadUserId) : ''))
  const bulkDirty =
    !processScreenOpen &&
    (department?.status === 'UNDER_REVIEW' || department?.status === 'PENDING_HEAD_SIGN_OFF') &&
    (bulkReviewComments.trim() !== '' || bulkHeadComments.trim() !== '')
  const projectFormDirty =
    creatingProject && (projectName.trim() !== '' || projectYear !== new Date().getFullYear())
  const deptFormDirty =
    creatingDepartment &&
    (deptForm.orgUnitId !== '' || deptForm.headUserId !== '' || deptForm.championUserId !== '')
  const ropaDirty =
    dirty ||
    reviewDraftDirty ||
    headDraftDirty ||
    headNotesDirty ||
    assignDirty ||
    bulkDirty ||
    projectFormDirty ||
    deptFormDirty
  const saveOnLeave = dirty
    ? async () => {
        await persistDraft()
        onOk('Draft saved')
      }
    : reviewDraftDirty
      ? async () => {
          await persistReview()
          onOk('Review saved')
        }
      : undefined
  useRegisterUnsavedChanges(
    ropaDirty
      ? {
          dirty: true,
          title: 'Unsaved ROPA changes',
          message: saveOnLeave
            ? 'You have unsaved ROPA changes. Save to keep your work, or discard to leave without saving.'
            : 'You have unsaved ROPA changes. Keep editing to stay on this screen, or discard to leave without saving.',
          onSaveDraft: saveOnLeave,
          saveDraftLabel: dirty ? 'Save Draft' : 'Save',
        }
      : null,
  )

  const headReviewProcess = async (outcome: 'APPROVED' | 'RETURNED') => {
    if (!department || !selectedProcess) return
    setBusy(true)
    try {
      const updated = await api.headReviewRopaProcess(department.id, selectedProcess.id, {
        outcome,
        comments: headDraft.comments || null,
      })
      setHeadDraft({ comments: updated.headComments ?? '' })
      onOk(
        outcome === 'APPROVED'
          ? 'Process approved by Department Head'
          : 'Process marked Return. It goes back to the Privacy Champion when you return the ROPA below',
      )
      await loadProcesses(department.id)
      await load()
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Head decision failed')
    } finally {
      setBusy(false)
    }
  }

  /** Bulk DPO return only. Mark Reviewed needs the four Arbor DPIA criteria per process. */
  const bulkReturnProcesses = async () => {
    if (!department || dpoSelectedIds.length === 0) return
    if (!bulkReviewComments.trim()) {
      onError('Review comments are required to return processes')
      return
    }
    const targets = processes.filter((p) => dpoSelectedIds.includes(p.id))
    setBusy(true)
    let done = 0
    try {
      for (const p of targets) {
        await api.reviewRopaProcess(department.id, p.id, {
          comments: bulkReviewComments || null,
          outcome: 'RETURNED',
        })
        done += 1
      }
      onOk(`${done} process${done === 1 ? '' : 'es'} marked Return`)
      setDpoSelectedIds([])
      setBulkReviewComments('')
      await loadProcesses(department.id)
      await load()
    } catch (e) {
      onError(
        done > 0
          ? `Bulk return failed (${done} of ${targets.length} applied)`
          : e instanceof Error
            ? e.message
            : 'Bulk return failed',
      )
      if (done > 0) {
        await loadProcesses(department.id)
        await load()
      }
    } finally {
      setBusy(false)
    }
  }

  /** Bulk Department Head decision for the checkbox-selected processes. */
  const bulkHeadReviewProcesses = async (outcome: 'APPROVED' | 'RETURNED') => {
    if (!department || headSelectedIds.length === 0) return
    const targets = processes.filter((p) => headSelectedIds.includes(p.id))
    setBusy(true)
    let done = 0
    try {
      for (const p of targets) {
        await api.headReviewRopaProcess(department.id, p.id, {
          outcome,
          comments: bulkHeadComments || null,
        })
        done += 1
      }
      onOk(
        outcome === 'APPROVED'
          ? `${done} process${done === 1 ? '' : 'es'} approved by Department Head`
          : `${done} process${done === 1 ? '' : 'es'} marked Return`,
      )
      setHeadSelectedIds([])
      setBulkHeadComments('')
      await loadProcesses(department.id)
      await load()
    } catch (e) {
      onError(
        e instanceof Error
          ? `${e.message} (${done} of ${targets.length} applied)`
          : `Bulk Head decision failed (${done} of ${targets.length} applied)`,
      )
      await loadProcesses(department.id)
      await load()
    } finally {
      setBusy(false)
    }
  }

  const openProject = (p: RopaProject) => {
    setSelectedProjectId(p.id)
    setSelectedDeptId(null)
    setProcessScreenOpen(false)
    setDeptFilter('ALL')
    setSearch('')
  }

  const backToProjects = () => {
    setSelectedProjectId(null)
    setSelectedDeptId(null)
    setProcessScreenOpen(false)
    setCreatingDepartment(false)
    setSearch('')
  }

  const backToProject = () => {
    setSelectedDeptId(null)
    setProcessScreenOpen(false)
    setCreatingDepartment(false)
  }

  const projectAllClosed =
    project != null && project.departments.length > 0 && project.departments.every((d) => d.status === 'CLOSED')
  const completeWhy =
    project == null || project.status === 'COMPLETED'
      ? null
      : project.departments.length === 0
        ? 'Add at least one department first'
        : !projectAllClosed
          ? 'Every department must be Closed before completing the project'
          : null

  const shellActions = canWrite ? (
    creatingProject || creatingDepartment || processScreenOpen || department ? undefined : project ? (
      <Stack direction="row" spacing={1}>
        <Button size="small" variant="contained" onClick={() => setCreatingDepartment(true)}>
          Add Department
        </Button>
        {project.status !== 'COMPLETED' && (
          <Tooltip title={completeWhy ?? ''} disableHoverListener={!completeWhy}>
            <span>
              <Button
                size="small"
                variant="outlined"
                disabled={busy || Boolean(completeWhy)}
                onClick={() => void act(() => api.completeRopaProject(project.id), 'ROPA project completed')}
              >
                Complete Project
              </Button>
            </span>
          </Tooltip>
        )}
      </Stack>
    ) : (
      <Button
        size="small"
        variant="contained"
        onClick={() => {
          setProjectYear(new Date().getFullYear())
          setProjectName('')
          setCreatingProject(true)
        }}
      >
        New Project
      </Button>
    )
  ) : undefined

  const pageTitle = creatingProject
    ? 'New ROPA Project'
    : creatingDepartment
      ? 'Add Department'
      : processScreenOpen
        ? (selectedProcess?.displayId ?? 'New processing activity')
        : department
          ? (department.orgUnitName ?? 'Department')
          : project
            ? project.name
            : 'ROPA'

  // Process banner chip follows the stage the process is in: DPO review
  // outcome until Head sign-off, then the Head decision for that process.
  const headStageChip =
    selectedProcess != null &&
    (department?.status === 'PENDING_HEAD_SIGN_OFF' ||
      selectedProcess.headOutcome === 'APPROVED' ||
      selectedProcess.headOutcome === 'RETURNED')
  const processChipLabel = !selectedProcess
    ? null
    : selectedProcess.draft
      ? 'DRAFT'
      : headStageChip
        ? (selectedProcess.headOutcome ?? 'PENDING')
        : (selectedProcess.reviewOutcome ?? 'OPEN')
  const processChipTone: 'success' | 'warning' | 'neutral' = !selectedProcess
    ? 'neutral'
    : selectedProcess.draft
      ? 'warning'
      : headStageChip
      ? selectedProcess.headOutcome === 'APPROVED'
        ? 'success'
        : selectedProcess.headOutcome === 'RETURNED'
          ? 'warning'
          : 'neutral'
      : selectedProcess.reviewOutcome === 'REVIEWED'
        ? 'success'
        : selectedProcess.reviewOutcome === 'RETURNED'
          ? 'warning'
          : 'neutral'

  useAppShellPage(
    {
      title: pageTitle,
      breadcrumbTrail: creatingProject
        ? 'New'
        : creatingDepartment
          ? 'New'
          : processScreenOpen
            ? (selectedProcess?.activityName ?? department?.orgUnitName ?? 'New')
            : department
              ? project?.year != null
                ? String(project.year)
                : 'ROPA'
              : project
                ? project.year != null
                  ? String(project.year)
                  : statusLabel(project.status)
                : 'Privacy',
      breadcrumbRoot: processScreenOpen
        ? {
            label: department?.orgUnitName ?? 'Department',
            onClick: () => guardApi.requestNavigation(closeProcessScreen),
          }
        : department || creatingDepartment
          ? { label: project?.name ?? 'ROPA', onClick: () => guardApi.requestNavigation(backToProject) }
          : project
            ? { label: 'ROPA', onClick: () => guardApi.requestNavigation(backToProjects) }
            : creatingProject
              ? { label: 'ROPA', onClick: () => guardApi.requestNavigation(() => setCreatingProject(false)) }
              : undefined,
      onTitleClick: processScreenOpen
        ? () => guardApi.requestNavigation(closeProcessScreen)
        : department || creatingDepartment
          ? () => guardApi.requestNavigation(backToProject)
          : project
            ? () => guardApi.requestNavigation(backToProjects)
            : undefined,
      // Status chips sit on the right side of the banner (actions slot),
      // not in the center header-meta slot.
      actions: department ? (
        <Stack direction="row" spacing={0.5} alignItems="center">
          {!processScreenOpen && (
            <StatusChip label={statusLabel(department.status)} tone={deptTone(department.status)} />
          )}
          {!processScreenOpen && (
            <StatusChip
              label={`${processes.length} process${processes.length === 1 ? '' : 'es'}`}
              tone="neutral"
            />
          )}
          {processScreenOpen && selectedProcess && processChipLabel && (
            <StatusChip label={processChipLabel} tone={processChipTone} />
          )}
        </Stack>
      ) : (
        shellActions
      ),
      hideContextLabel: true,
      hideRoleChips: true,
      flushContent: Boolean(department) && !processScreenOpen,
    },
    [
      pageTitle,
      project?.id,
      department?.id,
      department?.status,
      creatingProject,
      creatingDepartment,
      processScreenOpen,
      selectedProcess?.id,
      selectedProcess?.displayId,
      selectedProcess?.activityName,
      selectedProcess?.reviewOutcome,
      selectedProcess?.headOutcome,
      selectedProcess?.draft,
      project?.year,
      processes.length,
      dirty,
      canWrite,
    ],
  )

  if (loading) return <PageLoading label="Loading ROPA projects..." />

  /* ---------- Create project ---------- */
  if (creatingProject) {
    const nameTaken = projects.some(
      (p) => p.name.trim().toLowerCase() === projectName.trim().toLowerCase() && projectName.trim() !== '',
    )
    const createWhy = nameTaken ? `A project named "${projectName.trim()}" already exists` : ''
    return (
      <SimpleFormLayout
        actions={
          <Stack spacing={0.5} alignItems="flex-end">
            <Stack direction="row" spacing={1}>
              <Button onClick={() => guardApi.requestNavigation(() => setCreatingProject(false))}>Cancel</Button>
              <Button
                variant="contained"
                disabled={busy || !projectName.trim() || Boolean(createWhy)}
                onClick={() =>
                  void act(async () => {
                    const created = await api.createRopaProject({ name: projectName.trim(), year: projectYear })
                    setCreatingProject(false)
                    setProjectName('')
                    openProject(created)
                  }, 'ROPA project created')
                }
              >
                Create
              </Button>
            </Stack>
            {createWhy ? (
              <Typography variant="caption" color="text.secondary">
                {createWhy}
              </Typography>
            ) : null}
          </Stack>
        }
      >
        <SettingsCard autoHeight title="ROPA Project">
          <Box sx={formFieldGridSx}>
            <FormSelect
              label="Assessment year"
              required
              value={String(projectYear)}
              options={Array.from({ length: 10 }, (_, i) => {
                const y = String(new Date().getFullYear() + i)
                return { value: y, label: y }
              })}
              onChange={(v) => setProjectYear(Number(v))}
              fullWidth
            />
            <CompactTextField
              label="Project name"
              required
              fieldIdPrefix="ropa-project"
              value={projectName}
              onChange={(e) => setProjectName(e.target.value)}
              error={nameTaken}
              helperText={nameTaken ? `A project named "${projectName.trim()}" already exists` : undefined}
            />
          </Box>
        </SettingsCard>
      </SimpleFormLayout>
    )
  }

  /* ---------- Project register ---------- */
  if (!project) {
    const visibleProjects = canWrite
      ? projects
      : projects.filter((p) => p.departments.some((d) => myDeptIds.has(d.id)))
    const filteredProjects = visibleProjects.filter((p) => {
      if (projectStatusFilter !== 'ALL' && p.status !== projectStatusFilter) return false
      return p.name.toLowerCase().includes(search.trim().toLowerCase())
    })
    return (
      <Stack spacing={1} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
        <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          <RegisterToolbar
            search={search}
            searchPlaceholder="Search project name"
            onSearchChange={setSearch}
            onClearFilters={() => {
              setSearch('')
              setProjectStatusFilter('ALL')
            }}
            filters={[
              {
                id: 'status',
                label: 'Status',
                value: projectStatusFilter,
                options: [
                  { value: 'ALL', label: 'All' },
                  { value: 'DRAFT', label: 'Draft' },
                  { value: 'IN_PROGRESS', label: 'In Progress' },
                  { value: 'COMPLETED', label: 'Completed' },
                ],
                onChange: setProjectStatusFilter,
                minWidth: 140,
              },
            ]}
          />
          <TableContainer sx={{ flex: 1, minHeight: 0 }}>
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  <TableCell>Year</TableCell>
                  <TableCell>Project</TableCell>
                  <TableCell>Departments</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filteredProjects.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={4}>
                      <Typography color="text.secondary" sx={{ py: 2 }}>
                        {visibleProjects.length === 0
                          ? canWrite
                            ? 'No ROPA projects yet. Create a project to add departments.'
                            : 'No ROPA projects assigned to you yet.'
                          : 'No projects match these filters.'}
                      </Typography>
                    </TableCell>
                  </TableRow>
                )}
                {filteredProjects.map((p) => (
                  <TableRow key={p.id} hover sx={{ cursor: 'pointer' }} onClick={() => openProject(p)}>
                    <TableCell>{p.year ?? '-'}</TableCell>
                    <TableCell>{p.name}</TableCell>
                    <TableCell>{canWrite ? p.departments.length : p.departments.filter((d) => myDeptIds.has(d.id)).length}</TableCell>
                    <TableCell>
                      <StatusChip
                        label={statusLabel(p.status)}
                        tone={p.status === 'COMPLETED' ? 'success' : p.status === 'IN_PROGRESS' ? 'info' : 'neutral'}
                      />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Card>
      </Stack>
    )
  }

  /* ---------- Add department ---------- */
  if (creatingDepartment) {
    const takenOrgIds = new Set(project.departments.map((d) => d.orgUnitId))
    const availableUnits = orgUnits.filter((u) => !takenOrgIds.has(u.id))
    const createWhy =
      availableUnits.length === 0
        ? 'Every organisation unit already has a department in this project'
        : !deptForm.orgUnitId
          ? 'Department is required'
          : !deptForm.headUserId
            ? 'Department Head is required'
            : !deptForm.championUserId
              ? 'Privacy Champion is required'
              : ''
    return (
      <SimpleFormLayout
        actions={
          <Stack spacing={0.5} alignItems="flex-end">
            <Stack direction="row" spacing={1}>
              <Button onClick={() => guardApi.requestNavigation(() => setCreatingDepartment(false))}>Cancel</Button>
              <Button
                variant="contained"
                disabled={busy || Boolean(createWhy)}
                onClick={() =>
                  void act(async () => {
                    const created = await api.addRopaDepartment(project.id, {
                      orgUnitId: Number(deptForm.orgUnitId),
                      departmentHeadUserId: Number(deptForm.headUserId),
                      championUserId: Number(deptForm.championUserId),
                    })
                    setCreatingDepartment(false)
                    setDeptForm({ orgUnitId: '', headUserId: '', championUserId: '' })
                    setSelectedDeptId(created.id)
                  }, 'Department added')
                }
              >
                Create
              </Button>
            </Stack>
            {createWhy ? (
              <Typography variant="caption" color="text.secondary">
                {createWhy}
              </Typography>
            ) : null}
          </Stack>
        }
      >
        <SettingsCard autoHeight title="Department">
          <Box sx={formFieldGridSx}>
            <FormSelect
              label="Department"
              required
              value={deptForm.orgUnitId}
              options={[
                { value: '', label: availableUnits.length === 0 ? 'No departments left' : 'Select' },
                ...availableUnits.map((u) => ({ value: String(u.id), label: u.name })),
              ]}
              onChange={(v) => setDeptForm({ ...deptForm, orgUnitId: v })}
              disabled={availableUnits.length === 0}
              fullWidth
            />
            <FormSelect
              label="Department Head"
              required
              value={deptForm.headUserId}
              options={[
                { value: '', label: 'Select' },
                ...users.map((u) => ({ value: String(u.id), label: u.fullName || u.email })),
              ]}
              onChange={(v) => setDeptForm({ ...deptForm, headUserId: v })}
              fullWidth
            />
            <FormSelect
              label="Privacy Champion"
              required
              value={deptForm.championUserId}
              options={[
                { value: '', label: 'Select' },
                ...championUsers.map((u) => ({ value: String(u.id), label: u.fullName || u.email })),
              ]}
              onChange={(v) => setDeptForm({ ...deptForm, championUserId: v })}
              fullWidth
            />
          </Box>
        </SettingsCard>
      </SimpleFormLayout>
    )
  }

  /* ---------- Department register (inside project) ---------- */
  if (!department) {
    const counts = {
      total: visibleDepartments.length,
      awaitingDpo: visibleDepartments.filter((d) => d.status === 'UNDER_REVIEW').length,
      withHead: visibleDepartments.filter((d) => d.status === 'PENDING_HEAD_SIGN_OFF').length,
      inProgress: visibleDepartments.filter((d) => d.status === 'IN_PROGRESS').length,
      closed: visibleDepartments.filter((d) => d.status === 'CLOSED').length,
    }
    const filteredDepts = visibleDepartments.filter((d) => {
      if (deptFilter !== 'ALL' && d.status !== deptFilter) return false
      return (d.orgUnitName ?? '').toLowerCase().includes(search.trim().toLowerCase())
    })
    const kpiCards = [
      { label: 'Department ROPAs', value: counts.total, filter: 'ALL', accent: brand.gold },
      { label: 'Awaiting Data Privacy Officer review', value: counts.awaitingDpo, filter: 'UNDER_REVIEW', accent: counts.awaitingDpo ? semantic.risk.high : brand.gold },
      { label: 'With Department Head', value: counts.withHead, filter: 'PENDING_HEAD_SIGN_OFF', accent: counts.withHead ? semantic.risk.medium : brand.gold },
      { label: 'In Progress', value: counts.inProgress, filter: 'IN_PROGRESS', accent: brand.gold },
      { label: 'Closed', value: counts.closed, filter: 'CLOSED', accent: semantic.risk.low },
    ]
    return (
      <Stack spacing={1} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: 'repeat(2, 1fr)', md: 'repeat(5, 1fr)' },
            gap: 1,
          }}
        >
          {kpiCards.map((k) => (
            <KpiCard
              key={k.label}
              compact
              label={k.label}
              value={String(k.value)}
              accentColor={k.accent}
              onClick={() => setDeptFilter(deptFilter === k.filter ? 'ALL' : k.filter)}
            />
          ))}
        </Box>
        <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          <RegisterToolbar
            search={search}
            searchPlaceholder="Search department"
            onSearchChange={setSearch}
            onClearFilters={() => {
              setSearch('')
              setDeptFilter('ALL')
            }}
            filters={[
              {
                id: 'status',
                label: 'Status',
                value: deptFilter,
                options: [
                  { value: 'ALL', label: 'All' },
                  ...DEPT_STATUSES.map((s) => ({
                    value: s,
                    label: `${statusLabel(s)} (${visibleDepartments.filter((d) => d.status === s).length})`,
                  })),
                ],
                onChange: setDeptFilter,
                minWidth: 180,
              },
            ]}
          />
          <TableContainer sx={{ flex: 1, minHeight: 0 }}>
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  <TableCell>Department</TableCell>
                  <TableCell>Department Head</TableCell>
                  <TableCell>Privacy Champion</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Processes</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filteredDepts.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5}>
                      <Typography color="text.secondary" sx={{ py: 2 }}>
                        {visibleDepartments.length === 0
                          ? canWrite
                            ? 'No departments in this project yet. Use Add Department.'
                            : 'No departments assigned to you in this project yet.'
                          : 'No departments match these filters.'}
                      </Typography>
                    </TableCell>
                  </TableRow>
                )}
                {filteredDepts.map((d) => (
                  <TableRow
                    key={d.id}
                    hover
                    sx={{ cursor: 'pointer' }}
                    onClick={() => {
                      setSelectedDeptId(d.id)
                      setProcessScreenOpen(false)
                    }}
                  >
                    <TableCell>{d.orgUnitName ?? `#${d.orgUnitId}`}</TableCell>
                    <TableCell>{d.departmentHeadName ?? '-'}</TableCell>
                    <TableCell>{d.championName ?? '-'}</TableCell>
                    <TableCell>
                      <StatusChip label={statusLabel(d.status)} tone={deptTone(d.status)} />
                    </TableCell>
                    <TableCell>{d.processCount}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Card>
      </Stack>
    )
  }

  /* ---------- Process screen ---------- */
  if (processScreenOpen) {
    const reviewOpen = canWrite && department.status === 'UNDER_REVIEW' && selectedProcess != null
    // Admin always sees the review section. The Champion (and other
    // contributors) only see it once Admin has acted on the process:
    // Reviewed, or Returned (they need the comments to make fixes).
    const showReviewSection =
      selectedProcess != null &&
      (canWrite || (selectedProcess.reviewOutcome != null && selectedProcess.reviewOutcome !== 'OPEN'))
    // The assigned Head decides here while the department is with them.
    // Everyone else sees this section only once the Head has acted on the process.
    const headActing = department.status === 'PENDING_HEAD_SIGN_OFF' && myHeadDeptIds.has(department.id)
    const showHeadSection =
      selectedProcess != null &&
      (headActing ||
        (selectedProcess.headOutcome != null && selectedProcess.headOutcome !== 'PENDING') ||
        Boolean(selectedProcess.headComments))
    // During DPO review, lead with the criteria card so Admin lands on it first.
    const reviewSectionNav = showReviewSection
      ? [{ id: 'ropa-review', label: 'Data Privacy Officer Review' }]
      : []
    const sections = reviewOpen
      ? [
          ...reviewSectionNav,
          { id: 'ropa-activity', label: 'Processing Activity' },
          { id: 'ropa-details', label: 'Details of Processing Activity' },
          { id: 'ropa-legal', label: 'Legal Basis' },
          { id: 'ropa-transfer', label: 'Personal Data Transfer / Disclosures' },
          { id: 'ropa-storage', label: 'Storage & Security' },
          ...(showHeadSection ? [{ id: 'ropa-head-review', label: 'Department Head Review' }] : []),
        ]
      : [
          { id: 'ropa-activity', label: 'Processing Activity' },
          { id: 'ropa-details', label: 'Details of Processing Activity' },
          { id: 'ropa-legal', label: 'Legal Basis' },
          { id: 'ropa-transfer', label: 'Personal Data Transfer / Disclosures' },
          { id: 'ropa-storage', label: 'Storage & Security' },
          ...reviewSectionNav,
          ...(showHeadSection ? [{ id: 'ropa-head-review', label: 'Department Head Review' }] : []),
        ]

    const dpoReviewCard =
      selectedProcess && showReviewSection ? (
            <Box id="ropa-review" sx={{ ...sectionAnchorSx, mt: reviewOpen ? 0 : 1.5 }}>
              <SettingsCard
                autoHeight
                title="Data Privacy Officer Review"
                subtitle={
                  reviewOpen
                    ? 'Answer the four DPIA criteria. Required is set automatically from your answers.'
                    : undefined
                }
              >
                {reviewOpen ? (
                  <Stack spacing={1.5}>
                    <TableContainer
                      sx={{
                        border: `1px solid ${brand.border}`,
                        borderRadius: 1,
                        overflow: 'hidden',
                      }}
                    >
                      <Table size="small">
                        <TableHead>
                          <TableRow sx={{ bgcolor: brand.borderLight }}>
                            <TableCell sx={{ width: 44, fontWeight: 700 }}>No.</TableCell>
                            <TableCell sx={{ fontWeight: 700 }}>Criteria</TableCell>
                            <TableCell align="right" sx={{ width: 140, fontWeight: 700 }}>
                              Answer
                            </TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {DPIA_CRITERIA.map((c, i) => {
                            const value = reviewDraft[c.key]
                            return (
                              <TableRow
                                key={c.key}
                                sx={{
                                  bgcolor:
                                    value === 'YES' ? 'rgba(220, 38, 38, 0.04)' : value === 'NO' ? 'rgba(22, 163, 74, 0.04)' : undefined,
                                  '& td': { borderColor: brand.border, py: 1 },
                                }}
                              >
                                <TableCell sx={{ color: brand.muted, fontWeight: 600 }}>{i + 1}</TableCell>
                                <TableCell sx={{ fontSize: '0.8125rem', lineHeight: 1.35, pr: 2 }}>
                                  {c.label}
                                </TableCell>
                                <TableCell align="right">
                                  <Box sx={{ display: 'inline-flex', justifyContent: 'flex-end' }}>
                                    <DpiaCriteriaYesNo
                                      value={value}
                                      onChange={(v) => setReviewDraft({ ...reviewDraft, [c.key]: v })}
                                    />
                                  </Box>
                                </TableCell>
                              </TableRow>
                            )
                          })}
                        </TableBody>
                      </Table>
                    </TableContainer>
                    <Alert
                      severity={reviewCriteriaComplete && reviewDerivedDpiaRequired ? 'warning' : 'info'}
                      sx={{ py: 0.75, '& .MuiAlert-message': { width: '100%' } }}
                    >
                      <Typography variant="body2">
                        If you answered Yes to any of questions 1-4, you must perform a DPIA. After this
                        review, open Privacy → DPIA → Projects.
                      </Typography>
                      <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 0.75 }}>
                        <Typography variant="body2" color="text.secondary">
                          DPIA required (from criteria):
                        </Typography>
                        <StatusChip
                          label={
                            reviewCriteriaComplete ? (reviewDerivedDpiaRequired ? 'Yes' : 'No') : 'Answer all four'
                          }
                          tone={
                            !reviewCriteriaComplete
                              ? 'neutral'
                              : reviewDerivedDpiaRequired
                                ? 'warning'
                                : 'success'
                          }
                        />
                      </Stack>
                    </Alert>
                    <Box sx={baselineFieldGridSx}>
                      <HoverField text={HOVER.comments}>
                        <CompactTextField
                          label="DPIA comments"
                          fieldIdPrefix="ropa-review"
                          value={reviewDraft.dpiaComments}
                          onChange={(e) => setReviewDraft({ ...reviewDraft, dpiaComments: e.target.value })}
                        />
                      </HoverField>
                      <FormSelect
                        label="Review action"
                        value={reviewDraft.action}
                        options={[
                          { value: 'REVIEWED', label: 'Reviewed' },
                          { value: 'RETURNED', label: 'Return to Champion' },
                        ]}
                        onChange={(v) =>
                          setReviewDraft({ ...reviewDraft, action: v as 'REVIEWED' | 'RETURNED' })
                        }
                        fullWidth
                      />
                      <CompactTextField
                        label="Review comments"
                        required={reviewDraft.action === 'RETURNED'}
                        fieldIdPrefix="ropa-review"
                        value={reviewDraft.comments}
                        onChange={(e) => setReviewDraft({ ...reviewDraft, comments: e.target.value })}
                      />
                    </Box>
                    <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                      <Button
                        size="small"
                        variant="contained"
                        disabled={
                          busy ||
                          (reviewDraft.action === 'RETURNED' && !reviewDraft.comments.trim()) ||
                          (reviewDraft.action === 'REVIEWED' && !reviewCriteriaComplete)
                        }
                        onClick={() => {
                          if (
                            reviewDraft.action === 'REVIEWED' &&
                            !reviewDerivedDpiaRequired &&
                            selectedProcess.linkedDpiaId
                          ) {
                            setConfirmCancelDpia(true)
                            return
                          }
                          reviewProcess()
                        }}
                      >
                        {reviewDraft.action === 'RETURNED' ? 'Return to Champion' : 'Mark Reviewed'}
                      </Button>
                      {reviewDraft.action === 'REVIEWED' && !reviewCriteriaComplete ? (
                        <Typography variant="caption" color="text.secondary">
                          Answer all four DPIA criteria before marking Reviewed
                        </Typography>
                      ) : null}
                      {reviewDraft.action === 'RETURNED' && !reviewDraft.comments.trim() && (
                        <Typography variant="caption" color="text.secondary">
                          Review comments are required to return a process
                        </Typography>
                      )}
                    </Stack>
                  </Stack>
                ) : (
                  <Stack spacing={1}>
                    <Typography variant="body2">
                      Review: {selectedProcess.reviewOutcome ?? 'OPEN'}
                    </Typography>
                    <TableContainer
                      sx={{ border: `1px solid ${brand.border}`, borderRadius: 1, overflow: 'hidden' }}
                    >
                      <Table size="small">
                        <TableHead>
                          <TableRow sx={{ bgcolor: brand.borderLight }}>
                            <TableCell sx={{ width: 44, fontWeight: 700 }}>No.</TableCell>
                            <TableCell sx={{ fontWeight: 700 }}>Criteria</TableCell>
                            <TableCell align="right" sx={{ width: 80, fontWeight: 700 }}>
                              Answer
                            </TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {DPIA_CRITERIA.map((c, i) => {
                            const raw =
                              c.key === 'critLargeSensitive'
                                ? selectedProcess.dpiaCritLargeSensitive
                                : c.key === 'critSystematic'
                                  ? selectedProcess.dpiaCritSystematicAssessment
                                  : c.key === 'critAutomated'
                                    ? selectedProcess.dpiaCritAutomatedProfiling
                                    : selectedProcess.dpiaCritLegalSeriousImpact
                            const ans = critFromApi(raw)
                            return (
                              <TableRow key={c.key} sx={{ '& td': { borderColor: brand.border, py: 0.75 } }}>
                                <TableCell sx={{ color: brand.muted, fontWeight: 600 }}>{i + 1}</TableCell>
                                <TableCell sx={{ fontSize: '0.8125rem', lineHeight: 1.35 }}>{c.label}</TableCell>
                                <TableCell align="right">
                                  <DpiaCriteriaYesNo value={ans} readOnly />
                                </TableCell>
                              </TableRow>
                            )
                          })}
                        </TableBody>
                      </Table>
                    </TableContainer>
                    <Stack direction="row" spacing={1} alignItems="center">
                      <Typography variant="body2" color="text.secondary">
                        DPIA required:
                      </Typography>
                      <StatusChip
                        label={selectedProcess.dpiaRequired ? 'Yes' : 'No'}
                        tone={selectedProcess.dpiaRequired ? 'warning' : 'success'}
                      />
                      {selectedProcess.linkedDpiaId ? (
                        <Typography variant="caption" color="text.secondary">
                          (DPIA #{selectedProcess.linkedDpiaId})
                        </Typography>
                      ) : null}
                    </Stack>
                    <Typography variant="body2" color="text.secondary">
                      DPIA comments: {selectedProcess.dpiaComments ?? '-'}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Review comments: {selectedProcess.comments ?? '-'}
                    </Typography>
                    {department.status !== 'UNDER_REVIEW' && (
                      <Typography variant="body2" color="text.secondary">
                        Data Privacy Officer review opens when the department is Under Review.
                      </Typography>
                    )}
                  </Stack>
                )}
              </SettingsCard>
            </Box>
      ) : null

    return (
      <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, overflow: 'auto', pb: 3 }}>
        {selectedProcess?.reviewOutcome === 'RETURNED' && department.status === 'UNDER_REVIEW' && (
          <Alert severity="warning">
            Returned by the Data Privacy Officer. See the comments in the Data Privacy Officer Review section
            below. You can edit this process once the Data Privacy Officer completes the review and sends it
            back to you.
          </Alert>
        )}
        {selectedProcess?.headOutcome === 'RETURNED' && department.status === 'PENDING_HEAD_SIGN_OFF' && (
          <Alert severity="warning">
            Returned by the Department Head. See the comments in the Department Head Review section below. You
            can edit this process once the Department Head sends the ROPA back to you.
          </Alert>
        )}

        <FormSectionShell fill={false} initialSectionId={reviewOpen ? 'ropa-review' : undefined} sections={sections}>
          {reviewOpen ? dpoReviewCard : null}
          <Box id="ropa-activity" sx={{ ...sectionAnchorSx, mt: 1.5 }}>
            <SettingsCard autoHeight title="Processing Activity">
              <Stack spacing={1.25}>
                <Box sx={baselineFieldGridSx}>
                  <HoverField text={HOVER.activity}>
                  <CompactTextField
                    label="Processing Activity"
                    required
                    fieldIdPrefix="ropa-process"
                    value={processForm.activityName ?? ''}
                    onChange={(e) => setField({ activityName: e.target.value })}
                    disabled={!canEditProcess}
                  />
                  </HoverField>
                  <HoverField text={HOVER.designation}>
                  <FormSelect
                    label="DATA CONTROLLER OR DATA PROCESSOR?"
                    wrapLabel
                    required
                    value={processForm.designation ?? 'CONTROLLER'}
                    options={ROPA_DESIGNATIONS.map((d) => ({
                      value: d,
                      label: DESIGNATION_LABELS[d] ?? d,
                    }))}
                    onChange={(v) => setField({ designation: v as CreatePrivacyRopaBody['designation'] })}
                    disabled={!canEditProcess}
                    fullWidth
                  />
                  </HoverField>
                </Box>
                <HoverField text={HOVER.description}>
                <CompactTextField
                  label="Description of processing"
                  required
                  multiline
                  minRows={2}
                  fieldIdPrefix="ropa-process"
                  value={processForm.processingDescription ?? ''}
                  onChange={(e) => setField({ processingDescription: e.target.value })}
                  disabled={!canEditProcess}
                />
                </HoverField>
              </Stack>
            </SettingsCard>
          </Box>

          <Box id="ropa-details" sx={{ ...sectionAnchorSx, mt: 1.5 }}>
            <SettingsCard autoHeight title="Details of Processing Activity">
              <Stack spacing={1.25}>
                <Box sx={formFieldGridSx}>
                  <HoverField text={HOVER.personalData}>
                  <BiaCompactMultiSelect
                    id="ropa-personal-data"
                    label="LIST OF PERSONAL DATA *"
                    options={PERSONAL_DATA}
                    value={parseList(processForm.dataCategories)}
                    onChange={(next) => setList('dataCategories', next)}
                    disabled={!canEditProcess}
                    noun="personal data types"
                  />
                  </HoverField>
                  <HoverField text={HOVER.sensitiveData}>
                  <BiaCompactMultiSelect
                    id="ropa-sensitive-data"
                    label="CATEGORIES OF SENSITIVE DATA *"
                    options={SENSITIVE_DATA}
                    value={parseList(processForm.sensitiveDataCategories)}
                    onChange={(next) => setList('sensitiveDataCategories', next)}
                    disabled={!canEditProcess}
                    noun="sensitive data categories"
                  />
                  </HoverField>
                  <HoverField text={HOVER.dataSubjects}>
                  <BiaCompactMultiSelect
                    id="ropa-data-subjects"
                    label="DATA SUBJECT CATEGORIES *"
                    options={SUBJECT_CATEGORIES}
                    value={parseList(processForm.dataSubjects)}
                    onChange={(next) => setList('dataSubjects', next)}
                    disabled={!canEditProcess}
                    noun="data subject categories"
                  />
                  </HoverField>
                </Box>
                <Box sx={baselineFieldGridSx}>
                  <HoverField text={HOVER.volume}>
                  <FormSelect
                    label="NO. OF DATA SUBJECTS WHOSE PERSONAL DATA IS PROCESSED"
                    wrapLabel
                    required
                    value={processForm.dataSubjectVolume ?? ''}
                    options={[{ value: '', label: 'Select' }, ...VOLUMES.map((v) => ({ value: v, label: v }))]}
                    onChange={(v) => setField({ dataSubjectVolume: v })}
                    disabled={!canEditProcess}
                    fullWidth
                  />
                  </HoverField>
                  <HoverField text={HOVER.frequency}>
                  <FormSelect
                    label="Frequency of processing"
                    required
                    value={processForm.processingFrequency ?? ''}
                    options={[{ value: '', label: 'Select' }, ...ROPA_FREQUENCIES.map((v) => ({ value: v, label: v }))]}
                    onChange={(v) => setField({ processingFrequency: v })}
                    disabled={!canEditProcess}
                    fullWidth
                  />
                  </HoverField>
                  <HoverField text={HOVER.source}>
                  <CompactTextField
                    label="Source of Personal Data"
                    required
                    fieldIdPrefix="ropa-process"
                    value={processForm.sourceOfPersonalData ?? ''}
                    onChange={(e) => setField({ sourceOfPersonalData: e.target.value })}
                    disabled={!canEditProcess}
                  />
                  </HoverField>
                  <HoverField text={HOVER.method}>
                  <CompactTextField
                    label="Method of receiving Personal Data"
                    required
                    fieldIdPrefix="ropa-process"
                    value={processForm.methodOfReceiving ?? ''}
                    onChange={(e) => setField({ methodOfReceiving: e.target.value })}
                    disabled={!canEditProcess}
                  />
                  </HoverField>
                </Box>
              </Stack>
            </SettingsCard>
          </Box>

          <Box id="ropa-legal" sx={{ ...sectionAnchorSx, mt: 1.5 }}>
            <SettingsCard autoHeight title="Legal Basis">
              <HoverField text={HOVER.legalBasis}>
              <BiaCompactMultiSelect
                id="ropa-legal-basis"
                label="LEGAL BASIS FOR PROCESSING *"
                options={ROPA_LEGAL_BASES.map((b) => LEGAL_BASIS_LABELS[b] ?? b)}
                value={selectedLegal.map((b) => LEGAL_BASIS_LABELS[b] ?? b)}
                onChange={setLegalFromLabels}
                disabled={!canEditProcess}
                noun="legal bases"
              />
              </HoverField>
            </SettingsCard>
          </Box>

          <Box id="ropa-transfer" sx={{ ...sectionAnchorSx, mt: 1.5 }}>
            <SettingsCard autoHeight title="Personal Data Transfer / Disclosures">
              <Stack spacing={1.25}>
                <Box sx={baselineFieldGridSx}>
                  <HoverField text={HOVER.internal}>
                  <CompactTextField
                    label="Internal department"
                    required
                    fieldIdPrefix="ropa-process"
                    value={processForm.shareInternalDetails ?? ''}
                    onChange={(e) =>
                      setField({ shareInternalDetails: e.target.value, shareInternal: !!e.target.value })
                    }
                    disabled={!canEditProcess}
                  />
                  </HoverField>
                  <HoverField text={HOVER.external}>
                  <FormSelect
                    label="External entities"
                    required
                    value={processForm.shareExternal ? 'YES' : 'NO'}
                    options={[
                      { value: 'NO', label: 'No' },
                      { value: 'YES', label: 'Yes' },
                    ]}
                    onChange={(v) => setField({ shareExternal: v === 'YES' })}
                    disabled={!canEditProcess}
                    fullWidth
                  />
                  </HoverField>
                  {processForm.shareExternal && (
                    <CompactTextField
                      label="External entity detail"
                      required
                      fieldIdPrefix="ropa-process"
                      value={processForm.shareExternalDetails ?? ''}
                      onChange={(e) => setField({ shareExternalDetails: e.target.value })}
                      disabled={!canEditProcess}
                    />
                  )}
                  <HoverField text={HOVER.location}>
                  <CompactTextField
                    label="Location"
                    required
                    fieldIdPrefix="ropa-process"
                    value={processForm.externalEntityLocation ?? ''}
                    onChange={(e) => setField({ externalEntityLocation: e.target.value })}
                    disabled={!canEditProcess}
                  />
                  </HoverField>
                </Box>
                <Box sx={{ ...twoColFieldSx, alignItems: 'start' }}>
                  <HoverField text={HOVER.adequacy}>
                  <CompactTextField
                    label="DOES THE COUNTRY HAVE AN ADEQUATE LEVEL OF DATA PROTECTION? (Y/N)"
                    wrapLabel
                    required
                    fieldIdPrefix="ropa-process"
                    value={processForm.adequacyNote ?? ''}
                    onChange={(e) => setField({ adequacyNote: e.target.value })}
                    disabled={!canEditProcess}
                  />
                  </HoverField>
                  <HoverField text={HOVER.safeguards}>
                  <CompactTextField
                    label="What safeguards have been implemented?"
                    required
                    multiline
                    minRows={2}
                    fieldIdPrefix="ropa-process"
                    value={processForm.safeguardsImplemented ?? ''}
                    onChange={(e) => setField({ safeguardsImplemented: e.target.value })}
                    disabled={!canEditProcess}
                  />
                  </HoverField>
                </Box>
              </Stack>
            </SettingsCard>
          </Box>

          <Box id="ropa-storage" sx={{ ...sectionAnchorSx, mt: 1.5 }}>
            <SettingsCard autoHeight title="Storage & Security">
              <Stack spacing={1.25}>
                <Box sx={{ ...twoColFieldSx, alignItems: 'end' }}>
                  <HoverField text={HOVER.storage}>
                  <CompactTextField
                    label="SYSTEMS / OTHER LOCATION WHERE THE PERSONAL DATA IS STORED"
                    wrapLabel
                    required
                    fieldIdPrefix="ropa-process"
                    value={processForm.storageSystems ?? ''}
                    onChange={(e) => setField({ storageSystems: e.target.value })}
                    disabled={!canEditProcess}
                  />
                  </HoverField>
                  <HoverField text={HOVER.retention}>
                  <CompactTextField
                    label="Retention period"
                    required
                    fieldIdPrefix="ropa-process"
                    value={processForm.retentionPeriod ?? ''}
                    onChange={(e) => setField({ retentionPeriod: e.target.value })}
                    disabled={!canEditProcess}
                  />
                  </HoverField>
                </Box>
                <HoverField text={HOVER.security}>
                <CompactTextField
                  label="ORGANIZATIONAL, ADMINISTRATIVE, AND TECHNICAL MEASURES FOR PERSONAL DATA SECURITY"
                  wrapLabel
                  required
                  multiline
                  minRows={2}
                  fieldIdPrefix="ropa-process"
                  value={processForm.securityMeasures ?? ''}
                  onChange={(e) => setField({ securityMeasures: e.target.value })}
                  disabled={!canEditProcess}
                />
                </HoverField>
              </Stack>
            </SettingsCard>
          </Box>

          {!reviewOpen ? dpoReviewCard : null}
          {selectedProcess && showHeadSection && (
            <Box id="ropa-head-review" sx={{ ...sectionAnchorSx, mt: 1.5 }}>
              <SettingsCard autoHeight title="Department Head Review">
                {headActing ? (
                  <Stack spacing={1.25}>
                    <Box sx={baselineFieldGridSx}>
                      <Box>
                        <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 0.5 }}>
                          HEAD DECISION
                        </Typography>
                        <StatusChip
                          label={selectedProcess.headOutcome ?? 'PENDING'}
                          tone={selectedProcess.headOutcome === 'APPROVED' ? 'success' : selectedProcess.headOutcome === 'RETURNED' ? 'warning' : 'neutral'}
                        />
                      </Box>
                      <CompactTextField
                        label="Head comments"
                        fieldIdPrefix="ropa-head-review"
                        value={headDraft.comments}
                        onChange={(e) => setHeadDraft({ comments: e.target.value })}
                      />
                    </Box>
                    <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                      <Button
                        size="small"
                        variant="contained"
                        disabled={busy}
                        onClick={() => void headReviewProcess('APPROVED')}
                      >
                        Approve Process
                      </Button>
                      <Button
                        size="small"
                        color="warning"
                        variant="outlined"
                        disabled={busy || !headDraft.comments.trim()}
                        onClick={() => void headReviewProcess('RETURNED')}
                      >
                        Return Process
                      </Button>
                      {!headDraft.comments.trim() && (
                        <Typography variant="caption" color="text.secondary">
                          Comments are required to return a process
                        </Typography>
                      )}
                    </Stack>
                  </Stack>
                ) : (
                  <Stack spacing={0.5}>
                    <Typography variant="body2">Head: {selectedProcess.headOutcome ?? 'PENDING'}</Typography>
                    <Typography variant="body2" color="text.secondary">
                      Comments: {selectedProcess.headComments ?? '-'}
                    </Typography>
                    {department.status !== 'PENDING_HEAD_SIGN_OFF' && (selectedProcess.headOutcome == null || selectedProcess.headOutcome === 'PENDING') && (
                      <Typography variant="body2" color="text.secondary">
                        Department Head review happens after the Data Privacy Officer review, when the ROPA is sent for sign-off.
                      </Typography>
                    )}
                  </Stack>
                )}
              </SettingsCard>
            </Box>
          )}
        </FormSectionShell>

        {canEditProcess && (
          <Stack spacing={0.5} alignItems="flex-end">
            <Stack direction="row" spacing={1}>
              <Button disabled={busy} onClick={() => guardApi.requestNavigation(closeProcessScreen)}>
                Cancel
              </Button>
              <Button variant="outlined" disabled={busy} onClick={() => void saveDraftProcess()}>
                Save Draft
              </Button>
              <Button variant="contained" disabled={busy || !!processSaveDisabledReason} onClick={() => void saveProcess()}>
                {busy ? 'Saving...' : editingProcessId ? 'Save Process' : 'Add Process'}
              </Button>
            </Stack>
            {processSaveDisabledReason && (
              <Typography variant="caption" color="text.secondary">
                {processSaveDisabledReason}
              </Typography>
            )}
          </Stack>
        )}

        <ConfirmActionDialog
          open={confirmCancelDpia}
          title="Cancel the opened DPIA?"
          message="Marking DPIA not required will cancel the In Progress DPIA opened from this process if the DPO has not continued it."
          confirmLabel="Mark Not Required"
          onCancel={() => setConfirmCancelDpia(false)}
          onConfirm={() => {
            setConfirmCancelDpia(false)
            reviewProcess()
          }}
        />
      </Stack>
    )
  }

  /* ---------- Department detail ---------- */
  const headApprovedCount = processes.filter((pr) => pr.headOutcome === 'APPROVED').length
  const headReturnedCount = processes.filter((pr) => pr.headOutcome === 'RETURNED').length
  const headDecidedCount = headApprovedCount + headReturnedCount
  const allHeadApproved = processes.length > 0 && headApprovedCount === processes.length
  const allHeadDecided = processes.length > 0 && headDecidedCount === processes.length
  const reviewedCount = processes.filter((p) => p.reviewOutcome === 'REVIEWED').length
  const dpiaRequiredCount = processes.filter((p) => p.dpiaRequired).length
  const dpoReturnedCount = processes.filter((pr) => pr.reviewOutcome === 'RETURNED').length
  const allDpoDecided =
    processes.length > 0 &&
    processes.every((pr) => pr.reviewOutcome === 'REVIEWED' || pr.reviewOutcome === 'RETURNED')
  // Multi-select review scopes (BIA BC Head pattern): only undecided rows get a checkbox.
  const dpoReviewOpen = canWrite && department.status === 'UNDER_REVIEW'
  const headReviewOpen = isHeadViewer && department.status === 'PENDING_HEAD_SIGN_OFF'
  const dpoScope = processes.filter((p) => (p.reviewOutcome ?? 'OPEN') === 'OPEN')
  const headScope = processes.filter((p) => (p.headOutcome ?? 'PENDING') === 'PENDING')
  const bulkScope = dpoReviewOpen ? dpoScope : headScope
  const bulkSelectedIds = dpoReviewOpen ? dpoSelectedIds : headSelectedIds
  const setBulkSelectedIds = dpoReviewOpen ? setDpoSelectedIds : setHeadSelectedIds
  const phase: 'champion' | 'dpo_review' | 'finish_review' | 'champion_send' | 'await_head' | 'close' | 'done' =
    department.status === 'CLOSED'
      ? 'done'
      : department.status === 'APPROVED'
        ? 'close'
        : department.status === 'PENDING_HEAD_SIGN_OFF'
          ? 'await_head'
          : department.status === 'READY_FOR_HEAD_SIGN_OFF'
            ? 'champion_send'
            : department.status === 'UNDER_REVIEW' && allDpoDecided
              ? 'finish_review'
              : department.status === 'UNDER_REVIEW'
                ? 'dpo_review'
                : 'champion'
  const stateFor = (index: 0 | 1 | 2 | 3): BiaJourneyStep['state'] => {
    if (phase === 'done') return 'done'
    // With the Champion again to forward to the Head: Champion active, DPO done.
    if (phase === 'champion_send') return index === 0 ? 'active' : index === 1 ? 'done' : 'todo'
    const activeIndex =
      phase === 'champion' ? 0 : phase === 'dpo_review' || phase === 'finish_review' ? 1 : phase === 'await_head' ? 2 : 3
    if (index < activeIndex) return 'done'
    if (index === activeIndex) return 'active'
    return 'todo'
  }
  const journeySteps: BiaJourneyStep[] = [
    {
      id: 'champion',
      label: 'Privacy Champion',
      hint: phase === 'champion' ? `${processes.length} processing activities` : undefined,
      person: department.championName ?? undefined,
      state: stateFor(0),
    },
    {
      id: 'dpo_review',
      label: 'Data Privacy Officer Review',
      hint:
        phase === 'dpo_review' ||
        phase === 'finish_review' ||
        phase === 'champion_send' ||
        phase === 'await_head' ||
        phase === 'close' ||
        phase === 'done'
          ? `${reviewedCount} of ${processes.length} reviewed`
          : undefined,
      state: stateFor(1),
    },
    {
      id: 'dept_head',
      label: 'Department Head',
      hint:
        phase === 'champion_send'
          ? 'With Champion to send'
          : department.status === 'PENDING_HEAD_SIGN_OFF' || department.status === 'APPROVED' || department.status === 'CLOSED'
            ? `${headApprovedCount} of ${processes.length} approved`
            : undefined,
      person: department.departmentHeadName ?? undefined,
      state: stateFor(2),
    },
    {
      id: 'close',
      label: 'Close',
      hint: phase === 'close' ? 'Ready to close' : undefined,
      state: stateFor(3),
    },
  ]
  const journeyActionTitle =
    phase === 'champion'
      ? department.status === 'NOT_STARTED'
        ? 'Assign the Privacy Champion'
        : 'Waiting on Privacy Champion'
      : phase === 'dpo_review'
        ? 'Review processes'
        : phase === 'finish_review'
          ? 'Complete review'
          : phase === 'champion_send'
            ? isChampionViewer
              ? 'Send to Department Head'
              : 'With Privacy Champion'
            : phase === 'await_head'
              ? isHeadViewer
                ? 'Review each process'
                : 'Awaiting Department Head'
              : phase === 'close'
                ? 'Close ROPA'
                : 'Workflow complete'
  const nextToReview = processes.find((p) => p.reviewOutcome !== 'REVIEWED') ?? processes[0] ?? null
  const showAssignCard =
    canWrite &&
    (department.status === 'NOT_STARTED' ||
      department.status === 'IN_PROGRESS' ||
      department.status === 'READY_FOR_HEAD_SIGN_OFF')
  // The Champion sees Processes while filling. Admin (DPO) does not see the
  // section until the Champion has submitted (Under Review and later).
  const showProcessesCard =
    myChampionDeptIds.has(department.id) ||
    department.status === 'UNDER_REVIEW' ||
    department.status === 'READY_FOR_HEAD_SIGN_OFF' ||
    department.status === 'PENDING_HEAD_SIGN_OFF' ||
    department.status === 'APPROVED' ||
    department.status === 'CLOSED'

  return (
    <Box
      sx={{
        flex: 1,
        minHeight: 0,
        display: 'flex',
        flexDirection: 'column',
        overflow: { xs: 'auto', md: 'hidden' },
        boxSizing: 'border-box',
      }}
    >
      {department.returnReason && department.status === 'IN_PROGRESS' && (
        <Alert severity="warning" sx={{ mb: 1.5, flexShrink: 0 }}>
          Returned by {department.returnFrom === 'HEAD' ? 'Department Head' : 'Admin'}: {department.returnReason}
        </Alert>
      )}
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', md: '252px minmax(0, 1fr)' },
          gap: 1.5,
          alignItems: 'stretch',
          flex: { md: 1 },
          minHeight: { md: 0 },
          overflow: { md: 'hidden' },
        }}
      >
        <Box sx={{ minHeight: 0, overflow: { md: 'auto' } }}>
          <SettingsCard autoHeight title="Dashboard">
            <Stack spacing={1}>
              <KpiCard compact label="Department" value={<RailValue>{department.orgUnitName ?? '-'}</RailValue>} accentColor={brand.gold} />
              <KpiCard compact label="Processing activities" value={<RailValue>{String(processes.length)}</RailValue>} accentColor={brand.gold} />
              <KpiCard
                compact
                label="DPIA required"
                value={<RailValue>{String(dpiaRequiredCount)}</RailValue>}
                accentColor={dpiaRequiredCount > 0 ? semantic.risk.medium : brand.gold}
              />
              <KpiCard compact label="Reviewed" value={<RailValue>{`${reviewedCount} of ${processes.length}`}</RailValue>} accentColor={brand.gold} />
              <KpiCard
                compact
                label="Processes"
                value={<RailValue>{String(processes.length)}</RailValue>}
                accentColor={brand.gold}
              />
            </Stack>
          </SettingsCard>
        </Box>

        <Stack spacing={1.25} sx={{ minHeight: 0, overflow: { md: 'auto' }, pb: 3 }}>
          <BiaJourneyPanel title="Workflow" steps={journeySteps} actionTitle={journeyActionTitle}>
            {phase === 'champion' && department.status === 'NOT_STARTED' && (
              <Typography variant="body2" color="text.secondary">
                {canWrite
                  ? 'Use Assign Privacy Champion below to assign this ROPA to the champion. The champion then fills the processing activities.'
                  : 'Admin has not assigned this ROPA yet. You will be able to fill processing activities once it is assigned to you.'}
              </Typography>
            )}
            {phase === 'champion' && department.status === 'IN_PROGRESS' && (
              <Typography variant="body2" color="text.secondary">
                {canFill
                  ? 'Add or open processing activities below and fill the RoPA sheet fields. Submit the department when every business process is captured.'
                  : 'The assigned Privacy Champion is currently completing this ROPA. On submission it moves to Data Privacy Officer Review.'}
              </Typography>
            )}
            {phase === 'dpo_review' && (
              <Stack spacing={1.25}>
                <Typography variant="body2" color="text.secondary">
                  {canWrite
                    ? 'Open each process: the Data Privacy Officer Review criteria appear first. Answer the four Yes/No questions (DPIA required is set automatically), add comments, and Mark Reviewed or Return. You can also select several processes below to Return them together. When every process is decided, Complete Review sends returned ones back to the Champion, or if all are Reviewed sends the ROPA back to the Champion to forward to the Department Head.'
                    : 'The Data Privacy Officer is reviewing each process and checking DPIA eligibility.'}
                </Typography>
                {canWrite && nextToReview && (
                  <Button
                    size="small"
                    variant="contained"
                    disabled={busy}
                    onClick={() => openProcess(nextToReview)}
                    sx={{ alignSelf: 'flex-start' }}
                  >
                    {nextToReview.reviewOutcome === 'REVIEWED' ? 'Open a Process' : `Review ${nextToReview.activityName}`}
                  </Button>
                )}
              </Stack>
            )}
            {phase === 'finish_review' && (
              <Stack spacing={1.25}>
                <Typography variant="body2" color="text.secondary">
                  {canWrite
                    ? dpoReturnedCount > 0
                      ? `Every process is decided. ${dpoReturnedCount} marked Return will go back to the Privacy Champion when you complete the review; the Reviewed processes stay locked.`
                      : 'Every process is Reviewed. Finish the review to send this ROPA back to the Privacy Champion, who sends it to the Department Head for sign-off.'
                    : 'The Data Privacy Officer has reviewed every process and is finishing the review.'}
                </Typography>
                {canWrite && (
                  <Button
                    size="small"
                    variant="contained"
                    disabled={busy}
                    onClick={() =>
                      void act(
                        () => api.finishRopaReview(department.id),
                        dpoReturnedCount > 0
                          ? 'Review completed. Returned processes sent back to the Privacy Champion'
                          : 'Review completed. Sent back to the Privacy Champion for Head sign-off',
                      )
                    }
                    sx={{ alignSelf: 'flex-start' }}
                  >
                    Complete Review
                  </Button>
                )}
              </Stack>
            )}
            {phase === 'champion_send' && (
              <Stack spacing={1.25}>
                <Typography variant="body2" color="text.secondary">
                  {isChampionViewer
                    ? `The Data Privacy Officer has completed reviewing this ROPA. Send it to your Department Head${department.departmentHeadName ? ` (${department.departmentHeadName})` : ''} for sign-off. If they need changes, they will return processes to you here.`
                    : 'The Data Privacy Officer review is complete. Waiting for the Privacy Champion to send this ROPA to the Department Head for sign-off.'}
                </Typography>
                {isChampionViewer && (
                  <Button
                    size="small"
                    variant="contained"
                    disabled={busy}
                    onClick={() => setConfirmSendHead(true)}
                    sx={{ alignSelf: 'flex-start' }}
                  >
                    Send to Department Head
                  </Button>
                )}
              </Stack>
            )}
            {phase === 'await_head' && isHeadViewer && (
              <Stack spacing={1.5}>
                <Typography variant="body2" color="text.secondary">
                  Open each process below and Approve or Return it, or select several processes and decide
                  them together. Approved processes stay locked. Returned processes go back to the Privacy
                  Champion with your comments when you send the ROPA back. {headDecidedCount} of{' '}
                  {processes.length} decided ({headApprovedCount} approved,{' '}
                  {headReturnedCount} to return).
                </Typography>
                <CompactTextField
                  label="Head notes (overall, optional)"
                  multiline
                  minRows={2}
                  fieldIdPrefix="ropa-head"
                  value={headNotes}
                  onChange={(e) => setHeadNotes(e.target.value)}
                />
                <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                  <Button
                    size="small"
                    variant="contained"
                    disabled={busy || !allHeadApproved}
                    onClick={() =>
                      void act(() => api.headApproveRopa(department.id, headNotes), 'Signed off. Ready for DPO to close')
                    }
                  >
                    Approve ROPA
                  </Button>
                  <Button
                    size="small"
                    color="warning"
                    variant="outlined"
                    disabled={busy || !allHeadDecided || headReturnedCount === 0}
                    onClick={() =>
                      void act(
                        () => api.headReturnRopa(department.id, headNotes || undefined),
                        'Returned processes sent back to the Privacy Champion',
                      )
                    }
                  >
                    Return to Champion
                  </Button>
                  <Typography variant="caption" color="text.secondary">
                    {!allHeadDecided
                      ? `Decide every process first (${headDecidedCount} of ${processes.length} decided)`
                      : headReturnedCount === 0
                        ? 'Every process is approved: use Approve ROPA'
                        : `${headReturnedCount} process${headReturnedCount === 1 ? '' : 'es'} will go back to the Champion`}
                  </Typography>
                </Stack>
              </Stack>
            )}
            {phase === 'await_head' && !isHeadViewer && (
              <Typography variant="body2" color="text.secondary">
                Waiting for <strong>{department.departmentHeadName ?? 'Department Head'}</strong> to sign off. You can
                still open processes below to view them.
              </Typography>
            )}
            {phase === 'close' && (
              <Stack spacing={1.25}>
                <Typography variant="body2" color="text.secondary">
                  The Department Head has signed off. The DPO closes this department ROPA to complete the workflow.
                </Typography>
                {canWrite && (
                  <Button
                    size="small"
                    variant="contained"
                    disabled={busy}
                    onClick={() => setConfirmCloseDept(true)}
                    sx={{ alignSelf: 'flex-start' }}
                  >
                    Close ROPA
                  </Button>
                )}
              </Stack>
            )}
            {phase === 'done' && (
              <Typography variant="body2" color="text.secondary">
                This{department.orgUnitName ? ` ${department.orgUnitName}` : ' department'} ROPA is closed. Open any
                process below to view it.
              </Typography>
            )}
          </BiaJourneyPanel>

          {showAssignCard && (
            <SettingsCard
              autoHeight
              title={department.championUserId != null ? 'Privacy Champion' : 'Assign Privacy Champion'}
            >
              {department.championUserId != null ? (
                <Stack spacing={1} sx={{ mb: 1.5 }}>
                  <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                    <StatusChip label="Assigned" tone="warning" />
                    <Typography variant="body2">
                      Assigned to <strong>{department.championName ?? `User ${department.championUserId}`}</strong>
                    </Typography>
                  </Stack>
                  <Typography variant="body2" color="text.secondary">
                    Reassign the Privacy Champion or change the Department Head below.
                  </Typography>
                </Stack>
              ) : (
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
                  Assign this ROPA to a Privacy Champion (Contributor) and Department Head. The champion fills
                  processing activities and submits to the DPO.
                </Typography>
              )}
              <Stack direction="row" spacing={1.5} alignItems="flex-end" flexWrap="wrap" useFlexGap>
                <Box sx={{ flex: '1 1 220px', minWidth: 180, maxWidth: 340 }}>
                  <FormSelect
                    label="Privacy Champion"
                    required
                    value={assignForm.championUserId}
                    options={[
                      { value: '', label: 'Select contributor' },
                      ...championUsers.map((u) => ({ value: String(u.id), label: u.fullName || u.email })),
                    ]}
                    onChange={(v) => setAssignForm({ ...assignForm, championUserId: v })}
                    fullWidth
                  />
                </Box>
                <Box sx={{ flex: '1 1 220px', minWidth: 180, maxWidth: 340 }}>
                  <FormSelect
                    label="Department Head"
                    required
                    value={assignForm.headUserId}
                    options={[
                      { value: '', label: 'Select' },
                      ...users.map((u) => ({ value: String(u.id), label: u.fullName || u.email })),
                    ]}
                    onChange={(v) => setAssignForm({ ...assignForm, headUserId: v })}
                    fullWidth
                  />
                </Box>
                <Button
                  size="small"
                  variant="contained"
                  disabled={busy || !assignForm.championUserId || !assignForm.headUserId}
                  onClick={() =>
                    void act(
                      () =>
                        api.assignRopaDepartment(department.id, {
                          championUserId: Number(assignForm.championUserId),
                          departmentHeadUserId: Number(assignForm.headUserId),
                        }),
                      department.status === 'NOT_STARTED' ? 'Assigned to Privacy Champion' : 'Assignment updated',
                    )
                  }
                  sx={{ flexShrink: 0, height: 32 }}
                >
                  {department.championUserId != null ? 'Reassign' : 'Assign'}
                </Button>
              </Stack>
              {(!assignForm.championUserId || !assignForm.headUserId) && (
                <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.75 }}>
                  {!assignForm.championUserId
                    ? 'Privacy Champion is required to assign'
                    : 'Department Head is required to assign'}
                </Typography>
              )}
            </SettingsCard>
          )}

          {showProcessesCard && (
          <SettingsCard autoHeight title="Processes">
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
              {canFill
                ? 'Open a process from the list, or add a new one. Fill the RoPA sheet fields for every business process in this department.'
                : headReviewOpen
                  ? 'Select processes and choose Approve or Return below, or open a process and use the Department Head Review section. Then Approve ROPA or Return to Champion above.'
                  : dpoReviewOpen
                    ? 'Select processes and record DPIA required, comments, and Reviewed or Return below, or click a process row to review it on its own.'
                    : 'Click a process row to view its RoPA sheet fields.'}
            </Typography>
            <Table size="small">
              <TableHead>
                <TableRow>
                  {(dpoReviewOpen || headReviewOpen) && (
                    <TableCell padding="checkbox">
                      <Checkbox
                        size="small"
                        checked={bulkScope.length > 0 && bulkScope.every((p) => bulkSelectedIds.includes(p.id))}
                        indeterminate={
                          bulkSelectedIds.length > 0 && !bulkScope.every((p) => bulkSelectedIds.includes(p.id))
                        }
                        onChange={() => {
                          setBulkSelectedIds((prev) =>
                            bulkScope.every((p) => prev.includes(p.id)) ? [] : bulkScope.map((p) => p.id),
                          )
                        }}
                        inputProps={{ 'aria-label': 'Select all processes' }}
                      />
                    </TableCell>
                  )}
                  <TableCell>ID</TableCell>
                  <TableCell>Processing Activity</TableCell>
                  <TableCell>Controller / Processor</TableCell>
                  <TableCell>DPO Review</TableCell>
                  <TableCell>Department Head Review</TableCell>
                  <TableCell>DPIA</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {processes.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={dpoReviewOpen || headReviewOpen ? 7 : 6}>
                      <Typography color="text.secondary" sx={{ py: 1 }}>
                        No processing activities yet.
                      </Typography>
                    </TableCell>
                  </TableRow>
                )}
                {processes.map((row) => (
                  <TableRow
                    key={row.id}
                    hover
                    selected={bulkSelectedIds.includes(row.id)}
                    sx={{ cursor: 'pointer' }}
                    onClick={() => openProcess(row)}
                  >
                    {(dpoReviewOpen || headReviewOpen) && (
                      <TableCell padding="checkbox" onClick={(e) => e.stopPropagation()}>
                        {bulkScope.some((p) => p.id === row.id) && (
                          <Checkbox
                            size="small"
                            checked={bulkSelectedIds.includes(row.id)}
                            onChange={() => {
                              setBulkSelectedIds((prev) =>
                                prev.includes(row.id) ? prev.filter((id) => id !== row.id) : [...prev, row.id],
                              )
                            }}
                            inputProps={{ 'aria-label': `Select ${row.displayId}` }}
                          />
                        )}
                      </TableCell>
                    )}
                    <TableCell>{row.displayId}</TableCell>
                    <TableCell>
                      <Stack direction="row" spacing={1} alignItems="center">
                        <span>{row.activityName}</span>
                        {row.draft ? <StatusChip label="Draft" tone="warning" /> : null}
                      </Stack>
                    </TableCell>
                    <TableCell>{row.designation.replace(/_/g, ' ')}</TableCell>
                    <TableCell>{row.reviewOutcome ?? 'OPEN'}</TableCell>
                    <TableCell>
                      {row.headOutcome === 'APPROVED' || row.headOutcome === 'RETURNED'
                        ? row.headOutcome
                        : department.status === 'PENDING_HEAD_SIGN_OFF' ||
                            department.status === 'APPROVED' ||
                            department.status === 'CLOSED'
                          ? 'PENDING'
                          : '-'}
                    </TableCell>
                    <TableCell>
                      {row.dpiaRequired ? 'Yes' : 'No'}
                      {row.linkedDpiaId ? ` (DPIA #${row.linkedDpiaId})` : ''}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            {dpoReviewOpen && dpoSelectedIds.length > 0 && (
              <Stack spacing={0.75} sx={{ mt: 1.5 }}>
                <Typography variant="caption" color="text.secondary">
                  Mark Reviewed needs the four DPIA criteria on each process. Bulk select is for Return only.
                </Typography>
                <Stack direction="row" spacing={1} alignItems="flex-end" flexWrap="wrap" useFlexGap>
                  <Typography variant="body2" sx={{ pb: 0.75 }}>
                    {dpoSelectedIds.length} selected
                  </Typography>
                  <Box sx={{ flex: '1 1 200px', minWidth: 160 }}>
                    <CompactTextField
                      label="Review comments"
                      fieldIdPrefix="ropa-bulk-review"
                      value={bulkReviewComments}
                      onChange={(e) => setBulkReviewComments(e.target.value)}
                    />
                  </Box>
                  <Button
                    size="small"
                    color="warning"
                    variant="outlined"
                    disabled={busy || !bulkReviewComments.trim()}
                    onClick={() => void bulkReturnProcesses()}
                  >
                    Return to Champion
                  </Button>
                </Stack>
                {!bulkReviewComments.trim() && (
                  <Typography variant="caption" color="text.secondary">
                    Review comments are required to return processes
                  </Typography>
                )}
              </Stack>
            )}
            {dpoReviewOpen && dpoSelectedIds.length === 0 && dpoScope.length > 0 && (
              <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 1 }}>
                Open a process to answer DPIA criteria and Mark Reviewed, or select rows to return several at once.
              </Typography>
            )}
            {headReviewOpen && headSelectedIds.length > 0 && (
              <Stack spacing={0.75} sx={{ mt: 1.5 }}>
                <Stack direction="row" spacing={1} alignItems="flex-end" flexWrap="wrap" useFlexGap>
                  <Typography variant="body2" sx={{ pb: 0.75 }}>
                    {headSelectedIds.length} selected
                  </Typography>
                  <Box sx={{ flex: '1 1 220px', minWidth: 180 }}>
                    <CompactTextField
                      label="Head comments"
                      fieldIdPrefix="ropa-bulk-head"
                      value={bulkHeadComments}
                      onChange={(e) => setBulkHeadComments(e.target.value)}
                    />
                  </Box>
                  <Button
                    size="small"
                    variant="contained"
                    disabled={busy}
                    onClick={() => void bulkHeadReviewProcesses('APPROVED')}
                  >
                    Approve Selected
                  </Button>
                  <Button
                    size="small"
                    color="warning"
                    variant="outlined"
                    disabled={busy || !bulkHeadComments.trim()}
                    onClick={() => void bulkHeadReviewProcesses('RETURNED')}
                  >
                    Return Selected
                  </Button>
                </Stack>
                {!bulkHeadComments.trim() && (
                  <Typography variant="caption" color="text.secondary">
                    Comments are required to return processes
                  </Typography>
                )}
              </Stack>
            )}
            {headReviewOpen && headSelectedIds.length === 0 && headScope.length > 0 && (
              <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 1 }}>
                Select processes using the checkboxes to decide several at once.
              </Typography>
            )}
            {canFill && (
              <Stack spacing={0.75} sx={{ mt: 1.5 }}>
                <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                  <Button size="small" variant="contained" disabled={busy} onClick={() => openProcess(null)}>
                    Add New Process
                  </Button>
                  <RopaProcessExcelBar
                    departmentId={department.id}
                    busy={busy}
                    onError={onError}
                    onImported={(report) => {
                      void loadProcesses(department.id)
                      void load()
                      if (report.createdCount > 0) {
                        onOk(
                          `Imported ${report.createdCount} draft process${report.createdCount === 1 ? '' : 'es'}. Open each draft and Save Process before Submit ROPA`,
                        )
                      }
                    }}
                  />
                  <Button
                    size="small"
                    variant="contained"
                    disabled={busy || processes.length === 0}
                    onClick={() =>
                      void act(
                        () => api.submitRopaDepartment(department.id),
                        department.returnFrom === 'HEAD' && processes.every((pr) => pr.reviewOutcome === 'REVIEWED')
                          ? 'Submitted to Department Head'
                          : 'Submitted to DPO for review',
                      )
                    }
                  >
                    {department.returnFrom === 'HEAD' ? 'Submit to Department Head' : 'Submit ROPA'}
                  </Button>
                </Stack>
                {processes.length === 0 && (
                  <Typography variant="caption" color="text.secondary">
                    Add at least one processing activity before submitting.
                  </Typography>
                )}
                {processes.some((pr) => pr.draft) && (
                  <Typography variant="caption" color="text.secondary">
                    Draft processes cannot be submitted. Open each draft and use Save Process to complete it.
                  </Typography>
                )}
              </Stack>
            )}
          </SettingsCard>
          )}
        </Stack>
      </Box>

      <ConfirmActionDialog
        open={confirmSendHead}
        title="Send to Department Head?"
        message={`Send the ${department.orgUnitName ?? 'department'} ROPA to the Department Head for sign-off?`}
        confirmLabel="Send"
        confirmColor="primary"
        onCancel={() => setConfirmSendHead(false)}
        onConfirm={() => {
          setConfirmSendHead(false)
          void act(() => api.sendRopaToHead(department.id), 'Sent to Department Head')
        }}
      />
      <ConfirmActionDialog
        open={confirmCloseDept}
        title="Close this ROPA?"
        message="The Department Head has signed off. Closing completes this department in the ROPA project."
        confirmLabel="Close ROPA"
        confirmColor="primary"
        onCancel={() => setConfirmCloseDept(false)}
        onConfirm={() => {
          setConfirmCloseDept(false)
          void act(() => api.closeRopaDepartment(department.id), 'Department closed')
        }}
      />
    </Box>
  )
}
