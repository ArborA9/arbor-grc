import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  MenuItem,
  Select,
  Stack,
  Checkbox,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import AddRoundedIcon from '@mui/icons-material/AddRounded'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import {
  api,
  type BiaCycleRecord,
  type BiaLookups,
  type BiaPersonRef,
  type BiaProcessImportResult,
  type BiaProcessRecord,
  type BiaProjectRecord,
  type BiaProjectStatus,
  type BiaStatus,
  type OrgUnitNode,
  type Session,
  type BiaThirdPartyContact,
  type UpsertBiaProcessBody,
  type UserSummary,
} from './api'
import BiaPersonPicker, { biaPersonKey } from './components/bcms/BiaPersonPicker'
import BiaCompactMultiSelect from './components/bcms/BiaCompactMultiSelect'
import BiaAssetTypeRows from './components/bcms/BiaAssetTypeRows'
import BiaProcessExcelBar from './components/bcms/BiaProcessExcelBar'
import StatusChip from './components/StatusChip'
import { useAppShellPage } from './components/AppShellPageContext'
import {
  CompactTextField,
  FormFieldLabel,
  FormSectionShell,
  FormSelect,
  KpiCard,
  KpiCardGrid,
  PageLoading,
  RegisterToolbar,
  SettingsCard,
  SimpleFormLayout,
  registerTableSx,
  ConfirmActionDialog,
  FieldActionRow,
  BusyButton,
} from './components/ui'
import BiaJourneyPanel, { type BiaJourneyStep } from './components/bcms/BiaJourneyPanel'
import { compactSelectSx } from './components/ui/FormFieldLabel'
import { COMPACT_SELECT_MAX_WIDTH, controlActionSx, formFieldGridSx, sectionAnchorSx, NARRATIVE_MAX_ROWS, narrativeReadScrollSx } from './components/ui/formLayout'
import { BCMS_BIA_COLUMNS } from './hooks/registerColumns'
import { useRegisterColumnPicker } from './hooks/useRegisterColumnPicker'
import {
  useOptionalUnsavedChangesGuardApi,
  useRegisterUnsavedChanges,
} from './uiQuality/UnsavedChangesGuard'
import { actionDueDateErrorIfChanged, todayIso } from './validation'
import { brand, semantic } from './theme'
import {
  BIA_RECOVERY_OTHER,
  BIA_SITE_OTHER,
  focusBiaField,
  parseWholeNumber,
  validateBiaDraft,
  validateBiaFullSave,
  type BiaFieldErrors,
  type MorLike,
} from './biaProcessValidation'

function reviewOutcomeDisplay(raw?: string | null, label?: string | null) {
  if (label) return label
  if (!raw) return '-'
  return raw.replace(/_/g, ' ')
}

function nameWithEmail(name?: string | null, email?: string | null) {
  const person = (name ?? '').trim()
  const mail = (email ?? '').trim()
  if (person && mail) return `${person} (${mail})`
  return person || mail || '-'
}

/** Latest process step. Rejected is not a current choice; leftover rows stay blank. */
function processStatusLabel(
  p: { reviewOutcome?: string | null; headVerified?: boolean },
  cycleStatus: string,
): string {
  if (p.reviewOutcome === 'REJECTED') return '-'
  if (cycleStatus === 'RETURNED_TO_ADMIN') return 'Returned by Business Continuity Head'
  if (p.reviewOutcome === 'RETURNED_FOR_REVISION') return 'Returned by Admin'
  if (p.headVerified) return 'Verified by Business Continuity Head'
  const adminDone = p.reviewOutcome === 'VERIFIED' || p.reviewOutcome === 'APPROVED'
  if (cycleStatus === 'PENDING_BC_HEAD' && adminDone) return 'Pending Business Continuity Head Verification'
  if (cycleStatus === 'READY_TO_CLOSE' || cycleStatus === 'CLOSED' || cycleStatus === 'APPROVED') {
    return adminDone ? 'Verified by Business Continuity Head' : '-'
  }
  if (adminDone) return 'Verified by Admin'
  if (cycleStatus === 'SUBMITTED') return 'Pending Admin Verification'
  return '-'
}

/** Specialist sign-off. Approved remains for BIAs reviewed before Verified replaced it. */
function specialistDone(outcome?: string | null) {
  return outcome === 'VERIFIED' || outcome === 'APPROVED'
}

/** Verdict for the Outcome select: this process only. Pending, Approved, and Rejected stay blank. */
function reviewFormOutcome(p: BiaProcessRecord | null): string {
  if (!p || p.reviewOutcome === 'PENDING_REVIEW') return ''
  if (p.reviewOutcome === 'APPROVED' || p.reviewOutcome === 'REJECTED') return ''
  const label = (p.reviewOutcomeLabel || reviewOutcomeDisplay(p.reviewOutcome)).trim()
  if (!label || label === '-' || label.toLowerCase() === 'pending review') return ''
  if (label === 'Approved' || label === 'Rejected') return ''
  return label
}

function ReadOnlyValue({ label, value, required, scroll }: { label: string; value: string; required?: boolean; scroll?: boolean }) {
  return (
    <Box>
      <Typography
        component="label"
        sx={{
          display: 'block',
          fontSize: '0.6875rem',
          fontWeight: 650,
          color: brand.muted,
          mb: 0.35,
        }}
      >
        {label}
        {required ? ' *' : ''}
      </Typography>
      <Typography
        variant="body2"
        sx={
          scroll
            ? { minHeight: 32, ...narrativeReadScrollSx }
            : { minHeight: 32, display: 'flex', alignItems: 'center' }
        }
      >
        {value.trim() ? value : '-'}
      </Typography>
    </Box>
  )
}

function plusDays(n: number) {
  const d = new Date(`${todayIso()}T00:00:00`)
  d.setDate(d.getDate() + n)
  return d.toISOString().slice(0, 10)
}

const BIA_DEPT_STOP = new Set(['AND', 'OR', 'THE', 'OF', 'FOR', 'TO', 'IN', 'A', 'AN', 'AT', 'BY', 'ON'])

/** Matches backend: initials (max 6) or first 6 chars of a single-word name. */
function shortBiaDepartmentCode(name: string) {
  const parts = name.trim().toUpperCase().split(/[^A-Z0-9]+/).filter(Boolean)
  const initials: string[] = []
  let wordCount = 0
  for (const part of parts) {
    if (BIA_DEPT_STOP.has(part)) continue
    wordCount++
    initials.push(part[0]!)
    if (initials.length >= 6) return initials.slice(0, 6).join('')
  }
  if (wordCount >= 2 && initials.length > 0) return initials.join('')
  const first = parts[0]
  if (!first) return 'DEPT'
  return first.length <= 6 ? first : first.slice(0, 6)
}

/** Matches backend auto BIA reference: BIA-{year}-{SHORT}-{orgUnitId} (+ -n if taken). */
function nextBiaReference(
  year: number,
  departmentName: string,
  orgUnitId: number,
  taken: Iterable<string>,
) {
  const used = new Set(
    [...taken].map((id) => id.trim().toLowerCase()).filter(Boolean),
  )
  const short = shortBiaDepartmentCode(departmentName)
  const base = `BIA-${year}-${short}-${orgUnitId}`
  if (!used.has(base.toLowerCase())) return base
  for (let i = 2; i < 1000; i++) {
    const id = `${base}-${i}`
    if (!used.has(id.toLowerCase())) return id
  }
  return base
}

const STATUSES: BiaStatus[] = [
  'DRAFT',
  'ASSIGNED',
  'IN_PROGRESS',
  'SUBMITTED',
  'RETURNED',
  'PENDING_BC_HEAD',
  'RETURNED_TO_ADMIN',
  'READY_TO_CLOSE',
  'CLOSED',
  'APPROVED',
]
const CATEGORIES = ['STRATEGIC', 'FINANCIAL', 'OPERATIONAL', 'COMPLIANCE'] as const
const BANDS = ['H0_4', 'H4_8', 'H8_24', 'H24_48', 'H48_72'] as const
const BAND_LABELS = ['0-4 h', '4-8 h', '8-24 h', '24-48 h', '48-72 h']
/** Excel pack UI: implementation stays below; hide until product asks to restore. */
const SHOW_EXCEL_PACK = false

const PACK_FREQUENCIES = [
  'Continuous',
  'Daily',
  'Weekly',
  'Monthly',
  'Quarterly',
  'Bi-Annually',
  'Annually',
  'Upon Request / Ad-hoc',
]
const CAT_LABELS: Record<string, string> = {
  STRATEGIC: 'Strategic',
  FINANCIAL: 'Financial',
  OPERATIONAL: 'Operational',
  COMPLIANCE: 'Compliance',
}

const IMPACT_SCALE: Record<number, { name: string; chip: { bg: string; text: string } }> = {
  1: { name: 'Insignificant', chip: semantic.riskChip.low },
  2: { name: 'Minor', chip: semantic.riskChip.low },
  3: { name: 'Medium', chip: semantic.riskChip.medium },
  4: { name: 'Major', chip: semantic.riskChip.high },
  5: { name: 'Critical', chip: semantic.riskChip.critical },
}

function impactChipTone(n: number | '' | null | undefined) {
  if (n === '' || n == null) return semantic.riskChip.veryLow
  return IMPACT_SCALE[n]?.chip ?? semantic.riskChip.veryLow
}

function tierChipTone(tier?: number | null) {
  if (tier == null) return semantic.riskChip.veryLow
  if (tier <= 2) return semantic.riskChip.critical
  if (tier === 3) return semantic.riskChip.high
  if (tier === 4) return semantic.riskChip.medium
  return semantic.riskChip.low
}

function hasAnyRating(ratings: Record<string, number | ''>) {
  return Object.values(ratings).some((v) => typeof v === 'number')
}

/** Client-side preview of pack scoring so MOR appears before Save (matches BiaImpactEngine). */
function previewImpactFromRatings(ratings: Record<string, number | ''>) {
  const MAO_HOURS = [4, 8, 24, 48, 72]
  const BAND_LABELS_FULL = ['0-4 h', '4-8 h', '8-24 h', '24-48 h', '48-72 h']
  const tierLabel = (tier: number) => {
    if (tier === 1) return 'Critical - Tier 1'
    if (tier === 2) return 'Critical - Tier 2'
    if (tier === 3) return 'Critical - Tier 3'
    if (tier === 4) return 'Normal - Tier 4'
    return 'Normal - Tier 5'
  }
  let peak: number | null = null
  let filled = 0
  for (const cat of CATEGORIES) {
    for (const band of BANDS) {
      const v = ratings[`${cat}|${band}`]
      if (typeof v === 'number') {
        filled++
        if (peak == null || v > peak) peak = v
      }
    }
  }
  if (filled === 0) {
    return {
      peakScore: null as number | null,
      criticalityTier: null as number | null,
      categoryLabel: null as string | null,
      maoHours: null as number | null,
      rtoHours: null as number | null,
      morRequired: false,
      rtoCaption: null as string | null,
      allRatingsPresent: false,
    }
  }
  let tier = 5
  let mao = 72
  let hitThree = false
  for (let b = 0; b < BANDS.length; b++) {
    let bandMax: number | null = null
    for (const cat of CATEGORIES) {
      const v = ratings[`${cat}|${BANDS[b]}`]
      if (typeof v === 'number' && (bandMax == null || v > bandMax)) bandMax = v
    }
    if (bandMax != null && bandMax >= 3) {
      tier = b + 1
      mao = MAO_HOURS[b]
      hitThree = true
      break
    }
  }
  const rto = Math.floor((mao * 3) / 4)
  const morRequired = tier <= 3
  const rtoCaption = hitThree
    ? `RTO is 75% of MAO (${mao} h) = ${rto} h. First impact ≥ 3 in ${BAND_LABELS_FULL[tier - 1]}.`
    : `RTO is 75% of MAO (${mao} h) = ${rto} h. No band reached impact 3; Normal - Tier 5.`
  return {
    peakScore: peak,
    criticalityTier: tier,
    categoryLabel: tierLabel(tier),
    maoHours: mao,
    rtoHours: rto,
    morRequired,
    rtoCaption,
    allRatingsPresent: filled === 20,
  }
}

function MetricChip({
  label,
  value,
  tone,
}: {
  label?: string
  value: string
  tone: { bg: string; text: string }
}) {
  return (
    <Box
      sx={{
        display: 'inline-flex',
        flexDirection: label ? 'column' : undefined,
        alignItems: label ? 'flex-start' : 'center',
        justifyContent: 'center',
        minWidth: label ? 72 : undefined,
        height: label ? undefined : 28,
        px: label ? 1.25 : 1,
        py: label ? 0.75 : 0,
        borderRadius: 1,
        bgcolor: tone.bg,
        color: tone.text,
      }}
    >
      {label && (
        <Typography sx={{ fontSize: '0.625rem', fontWeight: 700, letterSpacing: '0.06em', textTransform: 'uppercase', opacity: 0.8 }}>
          {label}
        </Typography>
      )}
      <Typography sx={{ fontSize: label ? '0.8125rem' : '0.75rem', fontWeight: 600, lineHeight: 1.3, whiteSpace: 'nowrap' }}>
        {value}
      </Typography>
    </Box>
  )
}

function ratingFloor(ratings: Record<string, number | ''>, cat: string, bandIdx: number) {
  let floor = 1
  for (let i = 0; i < bandIdx; i++) {
    const v = ratings[`${cat}|${BANDS[i]}`]
    if (typeof v === 'number' && v > floor) floor = v
  }
  return floor
}

function applyRating(
  prev: Record<string, number | ''>,
  cat: string,
  band: (typeof BANDS)[number],
  value: number | '',
) {
  const next = { ...prev, [`${cat}|${band}`]: value }
  if (value === '') return next
  const idx = BANDS.indexOf(band)
  for (let i = idx + 1; i < BANDS.length; i++) {
    const key = `${cat}|${BANDS[i]}`
    const v = next[key]
    if (typeof v === 'number' && v < value) next[key] = value
  }
  return next
}

function flatten(nodes: OrgUnitNode[]): OrgUnitNode[] {
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

function tone(s: BiaStatus): 'success' | 'warning' | 'info' | 'danger' | 'neutral' {
  if (s === 'CLOSED' || s === 'APPROVED' || s === 'READY_TO_CLOSE') return 'success'
  if (s === 'SUBMITTED' || s === 'PENDING_BC_HEAD') return 'info'
  if (s === 'RETURNED' || s === 'RETURNED_TO_ADMIN') return 'danger'
  if (s === 'ASSIGNED' || s === 'IN_PROGRESS') return 'warning'
  return 'neutral'
}

/** User-facing BIA cycle status (not raw enum tokens). */
function biaStatusLabel(s: string) {
  if (s === 'PENDING_BC_HEAD') return 'Pending Business Continuity Head'
  return s.replace(/_/g, ' ')
}

function defaultBiaProjectName(year: number, existing: { name: string }[]) {
  const taken = new Set(existing.map((p) => p.name.trim().toLowerCase()))
  const base = `BIA ${year}`
  if (!taken.has(base.toLowerCase())) return base
  for (let n = 2; n < 100; n++) {
    const candidate = `${base} (${n})`
    if (!taken.has(candidate.toLowerCase())) return candidate
  }
  return `${base} ${Date.now()}`
}

function emptyProcess(): UpsertBiaProcessBody {
  return {
    processCode: '',
    processName: '',
    description: '',
    flowTrigger: '',
    flowSteps: '',
    flowOutput: '',
    thirdPartyActivities: '',
    supplierName: '',
    ownerOrgUnitId: null,
    ownerUserId: null,
    frequency: '',
    peakPeriod: '',
    depStaff: '',
    depPremises: '',
    depIt: '',
    depEquipment: '',
    depRecords: '',
    depThirdParty: '',
    relatedRiskCode: '',
    championNotes: '',
    depStaffPeople: [],
    depAssets: [],
  }
}

const TP_NONE = new Set(['', 'none', 'n/a', 'na', '-', '-'])

function isNoneish(v?: string | null) {
  return TP_NONE.has((v ?? '').trim().toLowerCase())
}

function inferThirdPartyInvolved(form: UpsertBiaProcessBody): boolean {
  return !isNoneish(form.thirdPartyActivities) || !isNoneish(form.supplierName) || !isNoneish(form.depThirdParty)
}

function processSaveBody(form: UpsertBiaProcessBody, tpInvolved: boolean): UpsertBiaProcessBody {
  const staff = form.depStaffPeople ?? []
  const assets = (form.depAssets ?? []).filter((a) => a.type.trim())
  const base: UpsertBiaProcessBody = {
    ...form,
    frequency: form.frequency || null,
    peakPeriod: null,
    depEquipment: null,
    depStaffPeople: staff,
    depAssets: assets,
    depStaff: null,
    depIt: null,
  }
  if (tpInvolved) return base
  return {
    ...base,
    thirdPartyActivities: 'None',
    supplierName: 'N/A',
    depThirdParty: 'N/A',
  }
}

function recoveryOtherComments(mor: MorLike): string | null {
  if (!mor.recoveryStrategies.includes(BIA_RECOVERY_OTHER)) return null
  const text = mor.recoveryComments.trim()
  return text || null
}

function siteOtherText(mor: MorLike): string | null {
  if (mor.siteType !== BIA_SITE_OTHER) return null
  const text = mor.siteOther.trim()
  return text || null
}

function emptyMor(): MorLike {
  return {
    minStaffPeople: [],
    recoveryStrategies: [],
    recoveryComments: '',
    internetConnectivity: '',
    intranetConnectivity: '',
    vpnAccess: '',
    siteType: '',
    siteOther: '',
    softwareApplications: [''],
    laptops: '',
    m365: '',
    thirdPartyContacts: [],
  }
}

function fromMor(p: BiaProcessRecord): MorLike {
  const apps = p.mor?.softwareApplications?.length ? p.mor.softwareApplications : ['']
  return {
    minStaffPeople: p.mor?.minStaffPeople ?? [],
    recoveryStrategies: p.mor?.recoveryStrategies?.length
      ? p.mor.recoveryStrategies
      : p.mor?.recoveryStrategy
        ? [p.mor.recoveryStrategy]
        : [],
    recoveryComments: p.mor?.workaround ?? '',
    internetConnectivity: p.mor?.internetConnectivity ?? '',
    intranetConnectivity: p.mor?.intranetConnectivity ?? '',
    vpnAccess: p.mor?.vpnAccess ?? '',
    siteType: p.mor?.siteType ?? '',
    siteOther: p.mor?.premises ?? '',
    softwareApplications: apps,
    laptops: p.mor?.laptops != null ? String(p.mor.laptops) : '',
    m365: p.mor?.m365 ?? '',
    thirdPartyContacts: p.mor?.thirdPartyContacts ?? [],
  }
}

function personSnap(p: BiaPersonRef) {
  return {
    source: p.source,
    directoryId: p.directoryId ?? null,
    userId: p.userId ?? null,
    email: (p.email ?? '').trim().toLowerCase(),
    displayName: (p.displayName ?? '').trim(),
    jobTitle: p.jobTitle ?? null,
  }
}

function peopleSnap(rows: BiaPersonRef[] | null | undefined) {
  return (rows ?? []).map(personSnap).sort((a, b) => biaPersonKey(a).localeCompare(biaPersonKey(b)))
}

/** Snapshot used only to decide if the user edited. Ignores load-time normalisation. */
function biaDirtySnapshot(
  form: UpsertBiaProcessBody,
  tp: boolean,
  ratings: Record<string, number | ''>,
  mor: MorLike,
): string {
  const allowed = new Set((form.depStaffPeople ?? []).map(biaPersonKey))
  const minStaff = (mor.minStaffPeople ?? []).filter((p) => allowed.has(biaPersonKey(p)))
  const rated: Record<string, number> = {}
  for (const key of Object.keys(ratings).sort()) {
    const value = ratings[key]
    if (typeof value === 'number') rated[key] = value
  }
  const text = (v?: string | null) => v ?? ''
  return JSON.stringify({
    tp,
    form: {
      processCode: text(form.processCode),
      processName: text(form.processName),
      description: text(form.description),
      flowTrigger: text(form.flowTrigger),
      flowSteps: text(form.flowSteps),
      flowOutput: text(form.flowOutput),
      thirdPartyActivities: text(form.thirdPartyActivities),
      supplierName: text(form.supplierName),
      ownerOrgUnitId: form.ownerOrgUnitId ?? null,
      ownerUserId: form.ownerUserId ?? null,
      frequency: text(form.frequency),
      depPremises: text(form.depPremises),
      depRecords: text(form.depRecords),
      depThirdParty: text(form.depThirdParty),
      depStaffPeople: peopleSnap(form.depStaffPeople),
      depAssets: (form.depAssets ?? [])
        .filter((row) => row.type.trim() || row.detail.trim())
        .map((row) => ({ type: row.type, detail: row.detail })),
    },
    ratings: rated,
    mor: {
      minStaffPeople: peopleSnap(minStaff),
      recoveryStrategies: mor.recoveryStrategies,
      recoveryComments: text(mor.recoveryComments),
      internetConnectivity: text(mor.internetConnectivity),
      intranetConnectivity: text(mor.intranetConnectivity),
      vpnAccess: text(mor.vpnAccess),
      siteType: text(mor.siteType),
      siteOther: text(mor.siteOther),
      softwareApplications: mor.softwareApplications.filter((s) => s.trim()),
      laptops: text(mor.laptops),
      m365: text(mor.m365),
      thirdPartyContacts: mor.thirdPartyContacts
        .filter((row) => row.name.trim() || row.email.trim() || (row.phone ?? '').trim())
        .map((row) => ({ name: row.name, email: row.email, phone: row.phone ?? '' })),
    },
  })
}

function fromProcess(p: BiaProcessRecord): UpsertBiaProcessBody {
  return {
    processCode: p.processCode,
    processName: p.processName,
    description: p.description ?? '',
    flowTrigger: p.flowTrigger ?? '',
    flowSteps: p.flowSteps ?? '',
    flowOutput: p.flowOutput ?? '',
    thirdPartyActivities: p.thirdPartyActivities ?? '',
    supplierName: p.supplierName ?? '',
    ownerOrgUnitId: p.ownerOrgUnitId,
    ownerUserId: p.ownerUserId,
    frequency: p.frequency ?? '',
    peakPeriod: p.peakPeriod ?? '',
    depStaff: p.depStaff ?? '',
    depPremises: p.depPremises ?? '',
    depIt: p.depIt ?? '',
    depEquipment: p.depEquipment ?? '',
    depRecords: p.depRecords ?? '',
    depThirdParty: p.depThirdParty ?? '',
    relatedRiskCode: p.relatedRiskCode ?? '',
    championNotes: p.championNotes ?? '',
    depStaffPeople: p.depStaffPeople ?? [],
    depAssets: p.depAssets ?? [],
  }
}

export default function BcmsBiaPage({
  session,
  onError,
  onOk,
  embeddedCycleId,
  rightHeader,
  onSubmitted,
}: {
  session: Session
  onError: (m: string) => void
  onOk: (m: string) => void
  embeddedCycleId?: number
  rightHeader?: ReactNode
  onSubmitted?: () => void
}) {
  const isAdmin = session.roles.includes('ADMIN') || session.roles.includes('OWNER')
  const biaColumns = useRegisterColumnPicker(BCMS_BIA_COLUMNS)
  const [projects, setProjects] = useState<BiaProjectRecord[]>([])
  const [projectId, setProjectId] = useState<number | null>(null)
  const [creatingProject, setCreatingProject] = useState(false)
  const [projectYear, setProjectYear] = useState(new Date().getFullYear())
  const [projectName, setProjectName] = useState('')
  const [rows, setRows] = useState<BiaCycleRecord[]>([])
  const [units, setUnits] = useState<OrgUnitNode[]>([])
  const [users, setUsers] = useState<UserSummary[]>([])
  const [directory, setDirectory] = useState<{ id: number; email: string; fullName: string }[]>([])
  const [lookups, setLookups] = useState<BiaLookups | null>(null)
  const [filter, setFilter] = useState<'ALL' | BiaStatus>('ALL')
  const [search, setSearch] = useState('')
  const [projectYearFilter, setProjectYearFilter] = useState('ALL')
  const [projectStatusFilter, setProjectStatusFilter] = useState<'ALL' | BiaProjectStatus>('ALL')
  const [orgFilter, setOrgFilter] = useState<'ALL' | number>('ALL')
  const [detail, setDetail] = useState<BiaCycleRecord | null>(null)
  const [creating, setCreating] = useState(false)
  const [orgUnitId, setOrgUnitId] = useState<number | ''>('')
  const [assigneeId, setAssigneeId] = useState<number | ''>('')
  const [dueDate, setDueDate] = useState(plusDays(30))
  const [headUserId, setHeadUserId] = useState<number | ''>('')
  const [headNotes, setHeadNotes] = useState('')
  const [headSelectedIds, setHeadSelectedIds] = useState<number[]>([])
  const [viewerUserId, setViewerUserId] = useState<number | null>(null)
  const [processForm, setProcessForm] = useState<UpsertBiaProcessBody>(emptyProcess())
  const [tpInvolved, setTpInvolved] = useState(false)
  const [selectedProcessId, setSelectedProcessId] = useState<number | null>(null)
  const [processScreenOpen, setProcessScreenOpen] = useState(false)
  const [ratings, setRatings] = useState<Record<string, number | ''>>({})
  const [impactEditing, setImpactEditing] = useState(true)
  const [mor, setMor] = useState(emptyMor())
  const [reviewOutcome, setReviewOutcome] = useState('')
  const [reviewComment, setReviewComment] = useState('')
  const [busyAction, setBusyAction] = useState<string | null>(null)
  const busy = busyAction != null
  const [confirmDeleteProcess, setConfirmDeleteProcess] = useState(false)
  const [confirmSendHead, setConfirmSendHead] = useState(false)
  const [confirmCloseBia, setConfirmCloseBia] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<BiaFieldErrors>({})
  const [saveBlockReason, setSaveBlockReason] = useState('')
  const [listLoading, setListLoading] = useState(true)
  const [detailLoading, setDetailLoading] = useState(false)
  const [processBaseline, setProcessBaseline] = useState('')
  const saveDraftRef = useRef<() => Promise<void>>(async () => {})
  const loadListRef = useRef<() => Promise<void>>(async () => {})
  const { requestNavigation } = useOptionalUnsavedChangesGuardApi()

  const processSnapshot = (
    form: UpsertBiaProcessBody,
    tp: boolean,
    nextRatings: Record<string, number | ''>,
    nextMor: ReturnType<typeof emptyMor>,
  ) => biaDirtySnapshot(form, tp, nextRatings, nextMor)
  const liveSnapRef = useRef('')
  liveSnapRef.current = processSnapshot(processForm, tpInvolved, ratings, mor)

  const processDirty =
    processScreenOpen &&
    processBaseline !== '' &&
    processSnapshot(processForm, tpInvolved, ratings, mor) !== processBaseline

  useRegisterUnsavedChanges(
    processScreenOpen
      ? {
          dirty: processDirty,
          title: 'Leave process without saving?',
          message:
            'You have unsaved process edits. Save a draft to keep your work, or discard to leave without saving.',
          onSaveDraft: () => saveDraftRef.current(),
        }
      : null,
  )

  const performCloseProcess = useCallback(() => {
    setProcessScreenOpen(false)
    setSelectedProcessId(null)
    setProcessForm(emptyProcess())
    setTpInvolved(false)
    setRatings({})
    setImpactEditing(true)
    setMor(emptyMor())
    setProcessBaseline('')
  }, [])

  const performCloseDetail = useCallback(() => {
    setDetail(null)
    setCreating(false)
    setProcessScreenOpen(false)
    setSelectedProcessId(null)
    setProcessBaseline('')
  }, [])

  const performCloseProject = useCallback(() => {
    performCloseDetail()
    setProjectId(null)
    setRows([])
  }, [performCloseDetail])

  const requestLeave = useCallback(
    (proceed: () => void) => {
      requestNavigation(proceed)
    },
    [requestNavigation],
  )

  const closeDetail = useCallback(() => {
    requestLeave(() => {
      void (async () => {
        await loadListRef.current()
        performCloseDetail()
      })()
    })
  }, [requestLeave, performCloseDetail])

  const closeProject = useCallback(() => {
    requestLeave(performCloseProject)
  }, [requestLeave, performCloseProject])

  const closeProcessScreen = useCallback(() => {
    requestLeave(performCloseProcess)
  }, [requestLeave, performCloseProcess])

  const selectedProject = projects.find((p) => p.id === projectId) ?? null

  const listActions = useMemo(
    () =>
      isAdmin ? (
        projectId == null ? (
          <Button size="small" variant="contained" onClick={() => {
            const y = new Date().getFullYear()
            setCreatingProject(true)
            setProjectYear(y)
            setProjectName(defaultBiaProjectName(y, projects))
          }}>
            New Project
          </Button>
        ) : (
          <Button size="small" variant="contained" onClick={() => {
            setCreating(true)
            setDetail(null)
            setOrgUnitId('')
          }}>
            Add Department BIA
          </Button>
        )
      ) : undefined,
    [isAdmin, projectId],
  )

  const detailStatusChip = useMemo(
    () => (detail ? <StatusChip label={biaStatusLabel(detail.status)} tone={tone(detail.status)} /> : undefined),
    [detail],
  )

  const detailDepartmentName = useMemo(() => {
    if (!detail) return null
    return (
      lookups?.orgUnits?.find((u) => u.id === detail.orgUnitId)?.name
      ?? flatten(units).find((u) => u.id === detail.orgUnitId)?.name
      ?? null
    )
  }, [detail, lookups, units])

  const detailPageTitle = detail
    ? detailDepartmentName
      ? `${detailDepartmentName} · ${detail.displayId}`
      : detail.displayId
    : 'BIA'

  useAppShellPage(
    embeddedCycleId
      ? {
          title: detailPageTitle,
          breadcrumbTrail: 'My Tasks',
          actions: detailStatusChip,
          hideContextLabel: true,
          hideRoleChips: true,
          flushContent: true,
        }
      : creatingProject
        ? {
            title: 'New Project',
            breadcrumbTrail: 'New',
            breadcrumbRoot: { label: 'BIA', onClick: () => setCreatingProject(false) },
            hideContextLabel: true,
            hideRoleChips: true,
          }
      : creating || detail
        ? {
            title: creating ? 'New department BIA' : detailPageTitle,
            breadcrumbTrail: creating
              ? 'New'
              : detailDepartmentName
                ? `${detailDepartmentName} · ${detail?.displayId ?? ''}`
                : detail?.displayId,
            breadcrumbRoot: {
              label: selectedProject?.name ?? 'BIA',
              onClick: closeDetail,
            },
            onTitleClick: closeDetail,
            actions: detailStatusChip,
            hideContextLabel: true,
            hideRoleChips: true,
            flushContent: Boolean(detail) && !creating,
          }
        : projectId != null
          ? {
              title: selectedProject?.name ?? 'BIA project',
              breadcrumbTrail: selectedProject ? String(selectedProject.year) : 'Project',
              breadcrumbRoot: { label: 'BIA', onClick: closeProject },
              actions: listActions,
              hideContextLabel: true,
              hideRoleChips: true,
            }
          : {
              title: 'Business Impact Analysis',
              breadcrumbTrail: 'BCMS',
              actions: listActions,
              hideContextLabel: true,
              hideRoleChips: true,
            },
    [
      embeddedCycleId,
      creating,
      creatingProject,
      detail,
      detailPageTitle,
      detailDepartmentName,
      listActions,
      closeDetail,
      closeProject,
      detailStatusChip,
      projectId,
      selectedProject,
    ],
  )

  const flatUnits = useMemo(() => flatten(units), [units])
  const contributors = users.filter((u) => u.roles.includes('CONTRIBUTOR'))
  const selected = detail?.processes.find((p) => p.id === selectedProcessId) ?? null
  const impactPreview = useMemo(() => previewImpactFromRatings(ratings), [ratings])
  const displayImpact = {
    peakScore: impactPreview.peakScore ?? selected?.peakScore ?? null,
    criticalityTier: impactPreview.criticalityTier ?? selected?.criticalityTier ?? null,
    categoryLabel: impactPreview.categoryLabel ?? selected?.categoryLabel ?? null,
    maoHours: impactPreview.maoHours ?? selected?.maoHours ?? null,
    rtoHours: impactPreview.rtoHours ?? selected?.rtoHours ?? null,
    rtoCaption: impactPreview.rtoCaption ?? selected?.rtoCaption ?? null,
    morRequired: impactPreview.peakScore != null
      ? impactPreview.morRequired
      : Boolean(selected?.morRequired),
  }
  const canEdit =
    detail != null &&
    (isAdmin
      ? detail.status === 'DRAFT'
        || detail.status === 'ASSIGNED'
        || detail.status === 'IN_PROGRESS'
        || detail.status === 'RETURNED'
        || detail.status === 'RETURNED_TO_ADMIN'
      : detail.status === 'ASSIGNED' || detail.status === 'IN_PROGRESS' || detail.status === 'RETURNED')
  /** Champion may only delete processes they added; Admin-created rows are Admin-only. */
  const canDeleteSelectedProcess =
    canEdit &&
    selected != null &&
    (isAdmin || (!selected.createdByAdmin && selected.createdByUserId != null))

  const listShownRef = useRef(false)
  const loadList = useCallback(async () => {
    if (!listShownRef.current) setListLoading(true)
    try {
      const lookup = await api.biaLookups().catch(() => null)
      if (lookup) setLookups(lookup)
      const [plist, tree] = await Promise.all([
        api.listBiaProjects().catch(() => [] as BiaProjectRecord[]),
        api.listOrgUnits().catch(() => [] as OrgUnitNode[]),
      ])
      setProjects(plist)
      setUnits(tree)
      if (!isAdmin) {
        const list = await api.listBiaCycles({}).catch(() => [] as BiaCycleRecord[])
        setRows(list)
      } else if (projectId != null) {
        const list = await api.listBiaCycles({ biaProjectId: projectId }).catch(() => [] as BiaCycleRecord[])
        setRows(list)
      } else {
        setRows([])
      }
      if (isAdmin) {
        try {
          setUsers(await api.listUsers())
        } catch {
          setUsers([])
        }
      }
      try {
        setDirectory(await api.listUserDirectory())
      } catch {
        setDirectory([])
      }
    } finally {
      listShownRef.current = true
      setListLoading(false)
    }
  }, [isAdmin, projectId])

  loadListRef.current = loadList

  useEffect(() => {
    void loadList().catch((e) => onError(String(e)))
  }, [loadList])

  useEffect(() => {
    if (isAdmin) {
      setViewerUserId(null)
      return
    }
    void api.authMe().then((u) => setViewerUserId(u.id)).catch(() => setViewerUserId(null))
  }, [isAdmin])

  useEffect(() => {
    const allowed = new Set((processForm.depStaffPeople ?? []).map(biaPersonKey))
    setMor((s) => {
      const next = s.minStaffPeople.filter((p) => allowed.has(biaPersonKey(p)))
      if (next.length === s.minStaffPeople.length) return s
      return { ...s, minStaffPeople: next }
    })
  }, [processForm.depStaffPeople])

  useEffect(() => {
    if (!processScreenOpen) return
    let touched = false
    const mark = (event: Event) => {
      const target = event.target
      if (!(target instanceof Element)) return
      if (!target.closest('#bia-identity, #bia-impact, #bia-mor')) return
      touched = true
    }
    document.addEventListener('input', mark, true)
    document.addEventListener('change', mark, true)
    const timer = window.setTimeout(() => {
      if (!touched) setProcessBaseline(liveSnapRef.current)
    }, 0)
    return () => {
      window.clearTimeout(timer)
      document.removeEventListener('input', mark, true)
      document.removeEventListener('change', mark, true)
    }
  }, [processScreenOpen, selectedProcessId])

  const openCycle = async (id: number) => {
    setDetailLoading(true)
    try {
      const c = await api.getBiaCycle(id)
      setDetail(c)
      setCreating(false)
      setAssigneeId(c.assignedToUserId ?? '')
      setDueDate(c.dueDate ?? plusDays(30))
      setHeadUserId(c.bcHeadUserId ?? '')
      setHeadNotes('')
      setHeadSelectedIds([])
      setSelectedProcessId(null)
      setProcessScreenOpen(false)
      fillProcess(null)
      if (isAdmin && c.biaProjectId && projectId == null) {
        setProjectId(c.biaProjectId)
      }
    } finally {
      setDetailLoading(false)
    }
  }

  useEffect(() => {
    if (embeddedCycleId) {
      void openCycle(embeddedCycleId).catch((e) => onError(String(e)))
    }
  }, [embeddedCycleId])


  const fillProcess = (p: BiaProcessRecord | null) => {
    if (!p) {
      const form = emptyProcess()
      const nextRatings: Record<string, number | ''> = {}
      const nextMor = emptyMor()
      setProcessForm(form)
      setTpInvolved(false)
      setRatings(nextRatings)
      setImpactEditing(true)
      setMor(nextMor)
      setReviewOutcome('')
      setReviewComment('')
      setProcessBaseline(processSnapshot(form, false, nextRatings, nextMor))
      return
    }
    const form = fromProcess(p)
    const tp = inferThirdPartyInvolved(form)
    setProcessForm(form)
    setTpInvolved(tp)
    const next: Record<string, number | ''> = {}
    for (const cell of p.impact) {
      next[`${cell.category}|${cell.band}`] = cell.rating ?? ''
    }
    setRatings(next)
    setImpactEditing(!hasAnyRating(next))
    const nextMor = fromMor(p)
    setMor(nextMor)
    setReviewOutcome(reviewFormOutcome(p))
    setReviewComment(p.reviewComment ?? '')
    setProcessBaseline(processSnapshot(form, tp, next, nextMor))
  }

  const startNewProcess = () => {
    const open = () => {
      setSelectedProcessId(null)
      setProcessScreenOpen(true)
      fillProcess(null)
    }
    if (processScreenOpen && processDirty) {
      requestLeave(open)
      return
    }
    open()
  }

  const openProcessRow = (p: BiaProcessRecord) => {
    const open = () => {
      setSelectedProcessId(p.id)
      setProcessScreenOpen(true)
      fillProcess(p)
    }
    if (processScreenOpen && processDirty && p.id !== selectedProcessId) {
      requestLeave(open)
      return
    }
    if (processScreenOpen && !processDirty && p.id === selectedProcessId) return
    open()
  }

  const closeProcessScreenForced = performCloseProcess

  const refresh = async (cycleId: number, processId?: number | null) => {
    const c = await api.getBiaCycle(cycleId)
    setDetail(c)
    await loadList()
    if (processId === null) {
      setSelectedProcessId(null)
      setProcessScreenOpen(false)
      fillProcess(null)
      return
    }
    const pid = processId !== undefined ? processId : selectedProcessId
    setSelectedProcessId(pid)
    if (pid != null) {
      setProcessScreenOpen(true)
      fillProcess(c.processes.find((p) => p.id === pid) ?? null)
    }
  }

  const handleProcessImported = async (report: BiaProcessImportResult) => {
    if (detail) await refresh(detail.id)
    const ignored = report.ignoredCount ?? 0
    if (report.createdCount > 0 && report.failedCount === 0) {
      onOk(`Added ${report.createdCount} process${report.createdCount === 1 ? '' : 'es'}`)
    } else if (report.createdCount > 0) {
      onOk(`Added ${report.createdCount} process${report.createdCount === 1 ? '' : 'es'}. ${report.failedCount} row(s) could not be imported.`)
    } else if (ignored > 0 && report.failedCount === 0) {
      onOk('Sample row ignored. Add your own process rows to import. You can delete the SAMPLE row or leave it.')
    } else {
      onError(`${report.failedCount} row(s) could not be imported. See the report.`)
    }
  }

  const saveProcessCombined = async (draft: boolean) => {
    if (!detail) return
    const preview = previewImpactFromRatings(ratings)
    const morRequired = preview.peakScore != null ? preview.morRequired : Boolean(selected?.morRequired)
    const validated = draft
      ? validateBiaDraft(processForm, mor)
      : validateBiaFullSave(processForm, ratings, mor, morRequired, tpInvolved, lookups?.assetTypes)
    setFieldErrors(validated.errors)
    setSaveBlockReason(validated.ok ? '' : validated.reason)
    if (!validated.ok) {
      if (validated.firstKey) focusBiaField(validated.firstKey)
      onError(validated.reason || 'Fix the highlighted fields')
      return
    }
    let processId = selectedProcessId
    const body = processSaveBody(processForm, tpInvolved)
    if (processId == null) {
      const created = await api.createBiaProcess(detail.id, body)
      processId = created.id
    } else {
      await api.updateBiaProcess(processId, body)
    }
    const cells = CATEGORIES.flatMap((cat) =>
      BANDS.map((band) => ({
        category: cat,
        band,
        rating: ratings[`${cat}|${band}`] === '' ? null : Number(ratings[`${cat}|${band}`]),
        remarks: null as string | null,
      })),
    )
    if (cells.some((c) => c.rating != null)) {
      await api.saveBiaImpact(processId, cells)
    }
    const lapN = parseWholeNumber(mor.laptops)
    if (morRequired && !draft) {
      await api.saveBiaMor(processId, {
        minStaffPeople: mor.minStaffPeople,
        recoveryStrategies: mor.recoveryStrategies,
        workaround: recoveryOtherComments(mor),
        internetConnectivity: mor.internetConnectivity || null,
        intranetConnectivity: mor.intranetConnectivity || null,
        vpnAccess: mor.vpnAccess || null,
        siteType: mor.siteType || null,
        premises: siteOtherText(mor),
        softwareApplications: mor.softwareApplications.map((s) => s.trim()).filter(Boolean),
        laptops: lapN != null && !Number.isNaN(lapN) ? lapN : null,
        m365: mor.m365 || null,
        thirdPartyContacts: tpInvolved ? mor.thirdPartyContacts : [],
        enforceRequired: true,
      })
    } else if (
      morRequired &&
      draft &&
      (mor.minStaffPeople.length > 0
        || mor.recoveryStrategies.length > 0
        || mor.internetConnectivity
        || mor.siteType
        || mor.siteOther.trim()
        || mor.laptops
        || mor.softwareApplications.some((s) => s.trim()))
    ) {
      await api.saveBiaMor(processId, {
        minStaffPeople: mor.minStaffPeople,
        recoveryStrategies: mor.recoveryStrategies,
        workaround: recoveryOtherComments(mor),
        internetConnectivity: mor.internetConnectivity || null,
        intranetConnectivity: mor.intranetConnectivity || null,
        vpnAccess: mor.vpnAccess || null,
        siteType: mor.siteType || null,
        premises: siteOtherText(mor),
        softwareApplications: mor.softwareApplications.map((s) => s.trim()).filter(Boolean),
        laptops: mor.laptops === '' || Number.isNaN(lapN as number) ? null : lapN,
        m365: mor.m365 || null,
        thirdPartyContacts: tpInvolved ? mor.thirdPartyContacts : [],
      })
    }
    setFieldErrors({})
    setSaveBlockReason('')
    await refresh(detail.id, draft ? processId : null)
    if (!draft) closeProcessScreenForced()
  }

  saveDraftRef.current = async () => {
    if (!processForm.processCode.trim() || !processForm.processName.trim()) {
      onError('Process code and name are required before saving a draft')
      throw new Error('Process code and name are required')
    }
    await saveProcessCombined(true)
  }

  const run = async (fn: () => Promise<unknown>, okMsg: string, action = 'page') => {
    setBusyAction(action)
    try {
      await fn()
      onOk(okMsg)
    } catch (e) {
      onError(String(e))
    } finally {
      setBusyAction(null)
    }
  }

  const ownerOptions = (lookups?.users ?? []).map((u) => ({ value: String(u.id), label: u.name }))
  const frequencyList = lookups?.frequencies?.length ? lookups.frequencies : PACK_FREQUENCIES
  const frequencyOptions = [
    ...frequencyList.map((f) => ({ value: f, label: f })),
    ...(processForm.frequency && !frequencyList.includes(processForm.frequency)
      ? [{ value: processForm.frequency, label: processForm.frequency }]
      : []),
  ]
  const unitName = (id: number) =>
    lookups?.orgUnits?.find((u) => u.id === id)?.name
    ?? flatUnits.find((u) => u.id === id)?.name
    ?? String(id)

  const banner = useMemo(() => {
    let submitted = 0
    let returned = 0
    let closed = 0
    let pendingHead = 0
    let critical = 0
    for (const r of rows) {
      if (r.status === 'SUBMITTED') submitted++
      if (r.status === 'RETURNED' || r.status === 'RETURNED_TO_ADMIN') returned++
      if (r.status === 'PENDING_BC_HEAD') pendingHead++
      if (r.status === 'CLOSED' || r.status === 'APPROVED' || r.status === 'READY_TO_CLOSE') closed++
      critical += r.kpis?.criticalCount ?? 0
    }
    return { total: rows.length, submitted, returned, closed, pendingHead, critical }
  }, [rows])

  const orgFilterOptions = useMemo(() => {
    const seen = new Map<number, string>()
    for (const r of rows) {
      const name =
        lookups?.orgUnits?.find((u) => u.id === r.orgUnitId)?.name
        ?? flatUnits.find((u) => u.id === r.orgUnitId)?.name
        ?? String(r.orgUnitId)
      if (!seen.has(r.orgUnitId)) seen.set(r.orgUnitId, name)
    }
    return [...seen.entries()].map(([id, name]) => ({ value: String(id), label: name }))
  }, [rows, lookups, flatUnits])

  const projectYearOptions = useMemo(() => {
    const years = [...new Set(projects.map((p) => p.year))].sort((a, b) => b - a)
    return [
      { value: 'ALL', label: 'All' },
      ...years.map((y) => ({ value: String(y), label: String(y) })),
    ]
  }, [projects])

  const filteredProjects = useMemo(() => {
    const q = search.trim().toLowerCase()
    return projects.filter((p) => {
      if (projectYearFilter !== 'ALL' && String(p.year) !== projectYearFilter) return false
      if (projectStatusFilter !== 'ALL' && p.status !== projectStatusFilter) return false
      if (!q) return true
      return p.name.toLowerCase().includes(q) || String(p.year).includes(q)
    })
  }, [projects, search, projectYearFilter, projectStatusFilter])

  const filtered = rows.filter((r) => {
    if (filter !== 'ALL' && r.status !== filter) return false
    if (orgFilter !== 'ALL' && r.orgUnitId !== orgFilter) return false
    const q = search.trim().toLowerCase()
    if (q && !r.displayId.toLowerCase().includes(q) && !unitName(r.orgUnitId).toLowerCase().includes(q)) {
      return false
    }
    return true
  })

  if (!detail && !creating && !creatingProject && !embeddedCycleId) {
    if (listLoading) {
      return (
        <PageLoading
          label={projectId == null && isAdmin ? 'Loading BIA projects…' : 'Loading department BIAs…'}
        />
      )
    }
    if (projectId == null && isAdmin) {
      return (
        <Stack spacing={1} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
          <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
            <RegisterToolbar
              search={search}
              searchPlaceholder="Search year or project name"
              onSearchChange={setSearch}
              onClearFilters={() => {
                setSearch('')
                setProjectYearFilter('ALL')
                setProjectStatusFilter('ALL')
              }}
              filters={[
                {
                  id: 'year',
                  label: 'Year',
                  value: projectYearFilter,
                  options: projectYearOptions,
                  onChange: setProjectYearFilter,
                  minWidth: 128,
                },
                {
                  id: 'status',
                  label: 'Status',
                  value: projectStatusFilter,
                  options: [
                    { value: 'ALL', label: 'All' },
                    { value: 'OPEN', label: 'Open' },
                    { value: 'CLOSED', label: 'Closed' },
                  ],
                  onChange: (v) => setProjectStatusFilter(v as 'ALL' | BiaProjectStatus),
                  minWidth: 140,
                },
              ]}
            />
            <TableContainer sx={{ flex: 1, minHeight: 0 }}>
              <Table size="small" stickyHeader sx={registerTableSx}>
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
                          {projects.length === 0
                            ? 'No BIA projects yet. Create a project to add department BIAs.'
                            : 'No projects match these filters.'}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )}
                  {filteredProjects.map((p) => (
                    <TableRow
                      key={p.id}
                      hover
                      sx={{ cursor: 'pointer' }}
                      onClick={() => setProjectId(p.id)}
                    >
                      <TableCell>{p.year}</TableCell>
                      <TableCell>{p.name}</TableCell>
                      <TableCell>{p.departmentCount}</TableCell>
                      <TableCell>
                        <StatusChip label={p.status} tone={p.status === 'OPEN' ? 'info' : 'success'} />
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

    return (
      <Stack spacing={1} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
        <KpiCardGrid columns={5}>
          <KpiCard compact label="Department BIA" value={String(banner.total)} accentColor={brand.gold} />
          <KpiCard
            compact
            label="Awaiting review"
            value={String(banner.submitted)}
            accentColor={banner.submitted ? semantic.risk.high : brand.gold}
            sub="Submitted: Admin process review"
            onClick={() => setFilter(filter === 'SUBMITTED' ? 'ALL' : 'SUBMITTED')}
          />
          <KpiCard
            compact
            label="With Business Continuity Head"
            value={String(banner.pendingHead)}
            accentColor={banner.pendingHead ? semantic.risk.medium : brand.gold}
            onClick={() => setFilter(filter === 'PENDING_BC_HEAD' ? 'ALL' : 'PENDING_BC_HEAD')}
          />
          <KpiCard
            compact
            label="Returned"
            value={String(banner.returned)}
            accentColor={banner.returned ? semantic.risk.medium : brand.gold}
            onClick={() => setFilter(filter === 'RETURNED' ? 'ALL' : 'RETURNED')}
          />
          <KpiCard
            compact
            label="Closed / ready"
            value={String(banner.closed)}
            accentColor={semantic.risk.low}
            onClick={() => setFilter(filter === 'CLOSED' ? 'ALL' : 'CLOSED')}
          />
        </KpiCardGrid>
        <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          <RegisterToolbar
            search={search}
            searchPlaceholder="Search reference or dept"
            onSearchChange={setSearch}
            onClearFilters={() => {
              setSearch('')
              setFilter('ALL')
              setOrgFilter('ALL')
            }}
            columnPicker={biaColumns.columnPicker}
            filters={[
              {
                id: 'status',
                label: 'Status',
                value: filter,
                options: [
                  { value: 'ALL', label: 'All' },
                  ...STATUSES.map((s) => ({
                    value: s,
                    label: `${biaStatusLabel(s)} (${rows.filter((r) => r.status === s).length})`,
                  })),
                ],
                onChange: (v) => setFilter(v as 'ALL' | BiaStatus),
                minWidth: 160,
              },
              {
                id: 'org',
                label: 'Org unit',
                value: orgFilter === 'ALL' ? 'ALL' : String(orgFilter),
                options: [{ value: 'ALL', label: 'All units' }, ...orgFilterOptions],
                onChange: (v) => setOrgFilter(v === 'ALL' ? 'ALL' : Number(v)),
                minWidth: 160,
              },
            ]}
          />
          <TableContainer sx={{ flex: 1, minHeight: 0 }}>
            <Table size="small" stickyHeader sx={registerTableSx}>
              <TableHead>
                <TableRow>
                  {biaColumns.isVisible('reference') && <TableCell>Reference</TableCell>}
                  {biaColumns.isVisible('dept') && <TableCell>Org unit</TableCell>}
                  {biaColumns.isVisible('processes') && <TableCell>In scope</TableCell>}
                  {biaColumns.isVisible('critical') && <TableCell>Critical</TableCell>}
                  {biaColumns.isVisible('status') && (
                    <TableCell sx={{ width: '30%', minWidth: 220, whiteSpace: 'nowrap' }}>Status</TableCell>
                  )}
                </TableRow>
              </TableHead>
              <TableBody>
                {filtered.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5}>
                      <Typography color="text.secondary" sx={{ py: 2 }}>
                        {isAdmin
                          ? 'No department BIAs in this project yet.'
                          : 'No department BIAs assigned to you yet.'}
                      </Typography>
                    </TableCell>
                  </TableRow>
                )}
                {filtered.map((r) => (
                  <TableRow key={r.id} hover sx={{ cursor: 'pointer' }} onClick={() => void openCycle(r.id)}>
                    {biaColumns.isVisible('reference') && <TableCell>{r.displayId}</TableCell>}
                    {biaColumns.isVisible('dept') && <TableCell>{unitName(r.orgUnitId)}</TableCell>}
                    {biaColumns.isVisible('processes') && <TableCell>{r.kpis.inScope}</TableCell>}
                    {biaColumns.isVisible('critical') && <TableCell>{r.kpis.criticalCount}</TableCell>}
                    {biaColumns.isVisible('status') && (
                      <TableCell sx={{ width: '30%', minWidth: 220, whiteSpace: 'nowrap', overflow: 'visible' }}>
                        <StatusChip
                          label={biaStatusLabel(r.status)}
                          tone={tone(r.status)}
                          sx={{
                            maxWidth: 'none',
                            height: 'auto',
                            minHeight: 22,
                            '& .MuiChip-label': {
                              whiteSpace: 'nowrap',
                              overflow: 'visible',
                              textOverflow: 'clip',
                              px: 1,
                            },
                          }}
                        />
                      </TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Card>
      </Stack>
    )
  }

  if (detailLoading && !detail && !creating && !creatingProject) {
    return <PageLoading label="Loading department BIA…" />
  }

  if (creatingProject) {
    const projectNameTaken = projects.some(
      (p) => p.name.trim().toLowerCase() === projectName.trim().toLowerCase(),
    )
    const createWhy = !projectName.trim()
      ? 'Project name is required'
      : projectNameTaken
        ? `A project named "${projectName.trim()}" already exists`
        : ''
    return (
      <SimpleFormLayout
        actions={
          <Stack spacing={0.5} alignItems="flex-end">
            <Stack direction="row" spacing={1}>
            <Button onClick={() => setCreatingProject(false)}>Cancel</Button>
            <Button
              variant="contained"
              disabled={busy || Boolean(createWhy)}
              onClick={() =>
                void run(async () => {
                  const p = await api.createBiaProject({ year: projectYear, name: projectName.trim() })
                  setCreatingProject(false)
                  setProjectId(p.id)
                  await loadList()
                }, 'BIA project created')
              }
            >
              Create
            </Button>
            </Stack>
            {createWhy ? (
              <Typography variant="caption" color="text.secondary">{createWhy}</Typography>
            ) : null}
          </Stack>
        }
      >
        <SettingsCard autoHeight title="Year project">
          <Box sx={formFieldGridSx}>
            <FormSelect
              label="Assessment year"
              required
              value={String(projectYear)}
              options={Array.from({ length: 10 }, (_, i) => {
                const y = String(new Date().getFullYear() + i)
                return { value: y, label: y }
              })}
              onChange={(v) => {
                const next = Number(v)
                setProjectYear(next)
                if (!projectName.trim() || projectName.trim() === defaultBiaProjectName(projectYear, projects) || projectName.trim() === `BIA ${projectYear}`) {
                  setProjectName(defaultBiaProjectName(next, projects))
                }
              }}
              fullWidth
            />
            <CompactTextField
              label="Project name"
              required
              value={projectName}
              onChange={(e) => setProjectName(e.target.value)}
              error={projectNameTaken}
              helperText={projectNameTaken ? `A project named "${projectName.trim()}" already exists` : undefined}
            />
          </Box>
        </SettingsCard>
      </SimpleFormLayout>
    )
  }

  if (creating) {
    const takenOrgUnitIds = new Set(rows.map((r) => r.orgUnitId))
    const availableUnits = flatUnits.filter((u) => !takenOrgUnitIds.has(u.id))
    const createDeptWhy =
      availableUnits.length === 0
        ? 'Every organisation unit already has a department BIA in this project'
        : orgUnitId === ''
          ? 'Department is required'
          : ''
    const selectedDeptName =
      orgUnitId === ''
        ? ''
        : availableUnits.find((u) => u.id === orgUnitId)?.name
          ?? flatUnits.find((u) => u.id === orgUnitId)?.name
          ?? ''
    const autoReference =
      orgUnitId !== '' && selectedProject && selectedDeptName
        ? nextBiaReference(
            selectedProject.year,
            selectedDeptName,
            Number(orgUnitId),
            rows.map((r) => r.displayId),
          )
        : '-'
    return (
      <SimpleFormLayout
        actions={
          <Stack spacing={0.5} alignItems="flex-end">
            <Stack direction="row" spacing={1}>
              <Button onClick={() => setCreating(false)}>Cancel</Button>
              <Button
                variant="contained"
                disabled={busy || Boolean(createDeptWhy) || projectId == null}
                onClick={() =>
                  void run(async () => {
                    const c = await api.createBiaCycle({
                      biaProjectId: projectId!,
                      orgUnitId: Number(orgUnitId),
                      displayId: autoReference === '-' ? null : autoReference,
                    })
                    setRows((prev) => (prev.some((r) => r.id === c.id) ? prev : [c, ...prev]))
                    setProjects((prev) =>
                      prev.map((p) =>
                        p.id === projectId ? { ...p, departmentCount: p.departmentCount + 1 } : p,
                      ),
                    )
                    await openCycle(c.id)
                  }, 'Department BIA created')
                }
              >
                Create
              </Button>
            </Stack>
            {createDeptWhy ? (
              <Typography variant="caption" color="text.secondary">{createDeptWhy}</Typography>
            ) : null}
          </Stack>
        }
      >
        <SettingsCard autoHeight title="Department">
          <Box sx={formFieldGridSx}>
            <FormSelect
              label="Department"
              required
              value={orgUnitId === '' ? '' : String(orgUnitId)}
              options={[
                { value: '', label: availableUnits.length === 0 ? 'No departments left' : 'Select' },
                ...availableUnits.map((u) => ({ value: String(u.id), label: u.name })),
              ]}
              onChange={(v) => setOrgUnitId(v === '' ? '' : Number(v))}
              fullWidth
              disabled={availableUnits.length === 0}
            />
            <ReadOnlyValue label="BIA reference" value={autoReference} />
          </Box>
        </SettingsCard>
      </SimpleFormLayout>
    )
  }

  if (!detail) return null

  const k = detail.kpis
  const dueErr = actionDueDateErrorIfChanged(detail.dueDate, dueDate)
  const reviewOpen =
    isAdmin &&
    (detail.status === 'SUBMITTED' || detail.status === 'RETURNED')
  const canRecordReview = isAdmin && detail.status === 'SUBMITTED'
  const reviewLockedReason = !canRecordReview
    ? 'Review is locked while this BIA is with the Champion'
    : undefined
  const isHeadAssignee =
    detail.status === 'PENDING_BC_HEAD' &&
    (Boolean(embeddedCycleId) || (viewerUserId != null && viewerUserId === detail.bcHeadUserId))
  const headReviewOpen = isHeadAssignee
  /** Process screen is always openable (read-only when not editable). */
  const processFormVisible = processScreenOpen
  const actionsLocked = !canEdit
  const submitBia = () =>
    void run(async () => {
      await api.submitBiaCycle(detail.id)
      await refresh(detail.id, null)
      onSubmitted?.()
    }, 'Submitted for review')
  const canAssign =
    isAdmin &&
    !embeddedCycleId &&
    detail.status !== 'SUBMITTED' &&
    detail.status !== 'PENDING_BC_HEAD' &&
    detail.status !== 'READY_TO_CLOSE' &&
    detail.status !== 'CLOSED' &&
    detail.status !== 'APPROVED'
  const reviewableProcesses = detail.processes.filter((p) => p.reviewOutcome !== 'REJECTED')
  const verifiedProcessCount = reviewableProcesses.filter((p) => specialistDone(p.reviewOutcome)).length
  const headScope = detail.processes.filter((p) => p.reviewOutcome !== 'REJECTED')
  const headAllVerified = headScope.length > 0 && headScope.every((p) => p.headVerified)
  const headMarkIds = headScope.filter((p) => headSelectedIds.includes(p.id) && !p.headVerified).map((p) => p.id)
  const pendingReviewCount = detail.processes.filter(
    (p) => p.completenessOk && p.reviewOutcome === 'PENDING_REVIEW',
  ).length
  const returnedProcessCount = detail.processes.filter(
    (p) => p.reviewOutcome === 'RETURNED_FOR_REVISION',
  ).length
  const rejectedProcessCount = detail.processes.filter((p) => p.reviewOutcome === 'REJECTED').length
  const canSendToHead =
    isAdmin &&
    (detail.status === 'SUBMITTED' || detail.status === 'RETURNED_TO_ADMIN') &&
    reviewableProcesses.length > 0 &&
    reviewableProcesses.every((p) => specialistDone(p.reviewOutcome))
  const showJourney =
    !processFormVisible &&
    (headReviewOpen ||
      (isAdmin &&
        !embeddedCycleId &&
        detail.status !== 'DRAFT'))
  const personById = (id: number | null | undefined, fallbackName?: string | null) => {
    const fromDirectory = id != null ? directory.find((u) => u.id === id) : undefined
    const fromUsers = id != null ? users.find((u) => u.id === id) : undefined
    const label = nameWithEmail(
      fromDirectory?.fullName || fromUsers?.fullName || fallbackName,
      fromDirectory?.email || fromUsers?.email,
    )
    return label === '-' ? '' : label
  }
  const bcHeadName = detail.bcHeadUserId == null ? null : personById(detail.bcHeadUserId) || null
  const championDone =
    detail.assignedToUserId != null &&
    detail.status !== 'DRAFT' &&
    detail.status !== 'ASSIGNED' &&
    detail.status !== 'IN_PROGRESS'
  const championPerson = championDone ? personById(detail.assignedToUserId) : ''
  const adminReviewDone =
    detail.status === 'RETURNED' ||
    detail.status === 'RETURNED_TO_ADMIN' ||
    detail.status === 'PENDING_BC_HEAD' ||
    detail.status === 'READY_TO_CLOSE' ||
    detail.status === 'CLOSED' ||
    detail.status === 'APPROVED' ||
    canSendToHead
  const adminReviewPerson = (() => {
    if (!adminReviewDone) return ''
    const seen = new Set<string>()
    const labels: string[] = []
    for (const p of detail.processes) {
      const counted =
        p.reviewOutcome === 'VERIFIED' ||
        p.reviewOutcome === 'APPROVED' ||
        p.reviewOutcome === 'RETURNED_FOR_REVISION'
      if (!counted) continue
      const label = personById(p.reviewerUserId, p.reviewerName)
      if (!label || seen.has(label)) continue
      seen.add(label)
      labels.push(label)
    }
    return labels.join(', ')
  })()
  const headPerson =
    detail.bcHeadUserId != null &&
    (detail.status === 'PENDING_BC_HEAD' ||
      detail.status === 'RETURNED_TO_ADMIN' ||
      detail.status === 'READY_TO_CLOSE' ||
      detail.status === 'CLOSED' ||
      detail.status === 'APPROVED')
      ? personById(detail.bcHeadUserId)
      : ''
  const reviewTotal = reviewableProcesses.length || detail.processes.length
  const pendingReviewProcess =
    detail.processes.find(
      (p) => !specialistDone(p.reviewOutcome) && p.reviewOutcome !== 'REJECTED',
    ) ?? detail.processes[0] ?? null

  const journeyPhase: 'champion' | 'admin_review' | 'send_head' | 'await_head' | 'close' | 'done' =
    detail.status === 'CLOSED' || detail.status === 'APPROVED'
      ? 'done'
      : detail.status === 'READY_TO_CLOSE'
        ? 'close'
        : detail.status === 'PENDING_BC_HEAD'
          ? 'await_head'
          : canSendToHead
            ? 'send_head'
            : detail.status === 'SUBMITTED' ||
                detail.status === 'RETURNED' ||
                detail.status === 'RETURNED_TO_ADMIN'
              ? 'admin_review'
              : 'champion'

  const journeySteps: BiaJourneyStep[] = (() => {
    const adminHint =
      journeyPhase === 'admin_review'
        ? `${verifiedProcessCount} of ${reviewTotal} verified`
        : journeyPhase === 'send_head' ||
            journeyPhase === 'await_head' ||
            journeyPhase === 'close' ||
            journeyPhase === 'done'
          ? `${verifiedProcessCount} of ${reviewTotal} verified`
          : undefined
    const headHint = journeyPhase === 'send_head' ? 'Ready to send' : undefined
    const stateFor = (index: 0 | 1 | 2 | 3): BiaJourneyStep['state'] => {
      const activeIndex =
        journeyPhase === 'champion'
          ? 0
          : journeyPhase === 'admin_review'
            ? 1
            : journeyPhase === 'send_head' || journeyPhase === 'await_head'
              ? 2
              : 3
      if (journeyPhase === 'done') return 'done'
      if (index < activeIndex) return 'done'
      if (index === activeIndex) return 'active'
      return 'todo'
    }
    return [
      {
        id: 'champion',
        label: 'Champion',
        hint: journeyPhase === 'champion' ? 'Working or not yet submitted' : undefined,
        person: championPerson || undefined,
        state: stateFor(0),
      },
      {
        id: 'admin_review',
        label: 'Admin Review',
        hint: adminHint,
        person: adminReviewPerson || undefined,
        state: stateFor(1),
      },
      {
        id: 'bc_head',
        label: 'Business Continuity Head',
        hint: headHint,
        person: headPerson || undefined,
        state: stateFor(2),
      },
      {
        id: 'close',
        label: 'Close',
        hint: journeyPhase === 'close' ? 'Ready to close' : undefined,
        state: stateFor(3),
      },
    ]
  })()

  const journeyActionTitle =
    headReviewOpen
      ? 'Your decision'
      : journeyPhase === 'champion'
        ? 'Waiting on Champion'
        : journeyPhase === 'admin_review'
          ? 'Review processes'
          : journeyPhase === 'send_head'
            ? 'Send to Business Continuity Head'
            : journeyPhase === 'await_head'
              ? 'Awaiting Business Continuity Head'
              : journeyPhase === 'close'
                ? 'Close Department BIA'
                : 'Workflow complete'

  const readyToSubmit = k.inScope > 0 && k.completeCount === k.inScope

  return (
    <>
    <Box
      sx={{
        flex: 1,
        minHeight: 0,
        display: 'flex',
        flexDirection: 'column',
        overflow: { xs: 'auto', md: 'hidden' },
        px: embeddedCycleId ? 0 : { xs: 2, md: 2.5 },
        pt: embeddedCycleId ? 0 : 1.5,
        boxSizing: 'border-box',
      }}
    >
    {detail.status === 'RETURNED' && (
      <Alert severity="warning" sx={{ mb: 1.5, flexShrink: 0 }}>
        {detail.returnReason ? (
          <>
            {'Returned for review: address this feedback: '}<Box component="span" sx={{ fontWeight: 700, whiteSpace: 'pre-wrap' }}>{detail.returnReason.trim()}</Box>
          </>
        ) : (
          'Returned for review. Open the process Admin sent back and revise, then Submit BIA again.'
        )}
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
            <KpiCard
              compact
              fitText
              label="Department"
              value={detailDepartmentName ?? unitName(detail.orgUnitId)}
              accentColor={brand.gold}
            />
            <KpiCard compact label="Total process" value={String(detail.processes.length)} accentColor={brand.gold} />
            <KpiCard
              compact
              label="Critical (1–3)"
              value={String(k.criticalCount)}
              accentColor={k.criticalCount > 0 ? semantic.risk.high : brand.gold}
            />
            <KpiCard
              compact
              label="Normal (4–5)"
              value={String(k.normalCount)}
              accentColor={brand.gold}
            />
            <KpiCard
              compact
              label="Ready for review"
              value={String(pendingReviewCount)}
              sub={
                returnedProcessCount > 0 || rejectedProcessCount > 0
                  ? [
                      returnedProcessCount > 0 ? `${returnedProcessCount} returned` : null,
                      rejectedProcessCount > 0 ? `${rejectedProcessCount} rejected` : null,
                    ].filter(Boolean).join(' · ')
                  : detail.processes.length === 0
                    ? (isAdmin ? 'Waiting for Champion' : 'Save a process to start the count')
                    : `${pendingReviewCount} of ${detail.processes.length} waiting`
              }
              accentColor={pendingReviewCount > 0 ? semantic.risk.medium : brand.gold}
            />
            <KpiCard
              compact
              label="Verified"
              value={String(verifiedProcessCount)}
              accentColor={verifiedProcessCount > 0 ? semantic.risk.low : brand.gold}
            />
          </Stack>
        </SettingsCard>
      </Box>

      <Stack spacing={1.25} sx={{ minHeight: 0, overflow: { md: 'auto' }, pb: 3 }}>
      {rightHeader}

      {detail.returnReason && (detail.status === 'RETURNED' || detail.status === 'RETURNED_TO_ADMIN') && (
        <Alert severity="warning" sx={{ mb: 1 }}>
          {`${detail.status === 'RETURNED_TO_ADMIN' ? 'Business Continuity Head returned' : 'Returned'} ${detail.returnReason.trim()}`}
        </Alert>
      )}

      {showJourney && (
        <BiaJourneyPanel
          title={headReviewOpen ? 'Business Continuity Head Workflow' : 'Workflow'}
          steps={journeySteps}
          actionTitle={journeyActionTitle}
        >
          {headReviewOpen && (
            <Stack spacing={1.5}>
              <Typography variant="body2" color="text.secondary">
                Approve this{detailDepartmentName ? ` ${detailDepartmentName}` : ''} BIA or return it to Admin
                with notes.
              </Typography>
              <CompactTextField
                label="Head notes"
                multiline
                minRows={2}
                maxRows={NARRATIVE_MAX_ROWS}
                value={headNotes}
                onChange={(e) => setHeadNotes(e.target.value)}
              />
              <Stack direction="row" spacing={1} alignItems="center">
                <BusyButton
                  size="small"
                  variant="contained"
                  busy={busyAction === 'approve'}
                  busyLabel="Approving…"
                  disabled={busy || !headAllVerified}
                  disabledReason={headAllVerified ? undefined : 'Please review each process to Approve.'}
                  onClick={() =>
                    void run(async () => {
                      await api.headApproveBia(detail.id, { notes: headNotes || null })
                      await refresh(detail.id, null)
                      onSubmitted?.()
                    }, 'Approved - ready for Admin close', 'approve')
                  }
                >
                  Approve
                </BusyButton>
                <Button
                  size="small"
                  variant="outlined"
                  color="warning"
                  disabled={busy || !headNotes.trim()}
                  onClick={() =>
                    void run(async () => {
                      await api.headReturnBia(detail.id, { notes: headNotes.trim() })
                      await refresh(detail.id, null)
                      onSubmitted?.()
                    }, 'Returned to Admin')
                  }
                >
                  Return to Admin
                </Button>
              </Stack>
              {!headAllVerified && (
                <Typography variant="caption" color="text.secondary" sx={{ whiteSpace: 'nowrap' }}>
                  Please review each process to Approve.
                </Typography>
              )}
            </Stack>
          )}

          {!headReviewOpen && journeyPhase === 'champion' && (
            <Typography variant="body2" color="text.secondary">
              The assigned Champion is currently completing the BIA. Upon submission, the BIA will
              progress to Admin Review.
            </Typography>
          )}

          {!headReviewOpen && journeyPhase === 'admin_review' && (
            <Stack spacing={1.25}>
              <Typography variant="body2" color="text.secondary">
                {detail.status === 'RETURNED_TO_ADMIN'
                  ? 'Business Continuity Head returned this BIA. Update processes if needed, then verify each process again.'
                  : 'Open each process and use the Specialist review tab to verify or return. When every in-scope process is Verified, the next step (Send to Business Continuity Head) unlocks here.'}
              </Typography>
              {pendingReviewProcess && (
                <Button
                  size="small"
                  variant="contained"
                  disabled={busy}
                  onClick={() => openProcessRow(pendingReviewProcess)}
                  sx={{ alignSelf: 'flex-start' }}
                >
                  {specialistDone(pendingReviewProcess.reviewOutcome)
                    ? 'Open a Process'
                    : `Review ${pendingReviewProcess.processCode}`}
                </Button>
              )}
            </Stack>
          )}

          {!headReviewOpen && journeyPhase === 'send_head' && (
            <Stack spacing={1.25}>
              <Typography variant="body2" color="text.secondary">
                Select a Business Continuity Head and send for final approval.
              </Typography>
              <Stack direction="row" spacing={1.5} alignItems="flex-end" flexWrap="wrap" useFlexGap>
                <Box sx={{ flex: '1 1 240px', minWidth: 200, maxWidth: 360 }}>
                  <FormSelect
                    label="Business Continuity Head"
                    value={headUserId === '' ? '' : String(headUserId)}
                    options={[
                      { value: '', label: 'Select user' },
                      ...users.map((u) => ({ value: String(u.id), label: u.fullName })),
                    ]}
                    onChange={(v) => setHeadUserId(v === '' ? '' : Number(v))}
                    fullWidth
                  />
                </Box>
                <Box sx={{ flex: '1 1 240px', minWidth: 200 }}>
                  <CompactTextField
                    label="Notes to Head"
                    value={headNotes}
                    onChange={(e) => setHeadNotes(e.target.value)}
                  />
                </Box>
                <Button
                  size="small"
                  variant="contained"
                  disabled={busy || headUserId === ''}
                  onClick={() => setConfirmSendHead(true)}
                  sx={{ flexShrink: 0, height: 32 }}
                >
                  {busy ? 'Sending…' : 'Send'}
                </Button>
              </Stack>
            </Stack>
          )}

          {!headReviewOpen && journeyPhase === 'await_head' && (
            <Typography variant="body2" color="text.secondary">
              Waiting for{' '}
              <strong>{bcHeadName ?? 'Business Continuity Head'}</strong> on My Tasks. You can still open processes
              below to view Identity, Impact analysis, and MOR.
            </Typography>
          )}

          {!headReviewOpen && journeyPhase === 'close' && (
            <Stack spacing={1.25}>
              <Typography variant="body2" color="text.secondary">
                The Business Continuity Head has approved this BIA. Select &apos;Close Department BIA&apos; to
                complete the workflow.
              </Typography>
              <Button
                size="small"
                variant="contained"
                disabled={busy}
                onClick={() => setConfirmCloseBia(true)}
                sx={{ alignSelf: 'flex-start' }}
              >
                {busy ? 'Closing…' : 'Close Department BIA'}
              </Button>
            </Stack>
          )}

          {!headReviewOpen && journeyPhase === 'done' && (
            <Typography variant="body2" color="text.secondary">
              This{detailDepartmentName ? ` ${detailDepartmentName}` : ' department'} BIA is closed. Open any
              process below to view the assessment.
            </Typography>
          )}
        </BiaJourneyPanel>
      )}

      {false && (detail?.rankedCritical.length ?? 0) > 0 && (
        <SettingsCard autoHeight title="Recovery order">
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            Critical processes ranked by MAO, then RTO. This order drives recovery planning.
          </Typography>
        </SettingsCard>
      )}

      {!processFormVisible && canAssign && (
        <SettingsCard
          autoHeight
          title={detail.assignedToUserId != null ? 'Champion' : 'Assign Champion'}
        >
          {detail.assignedToUserId != null ? (
            <Stack spacing={1} sx={{ mb: 1.5 }}>
              <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                <StatusChip label="Assigned" tone="warning" />
                <Typography variant="body2">
                  Assigned to{' '}
                  <strong>
                    {contributors.find((u) => u.id === detail.assignedToUserId)?.fullName
                      ?? users.find((u) => u.id === detail.assignedToUserId)?.fullName
                      ?? `User ${detail.assignedToUserId}`}
                  </strong>
                  {detail.dueDate ? (
                    <>
                      {' '}
                      · due {detail.dueDate}
                    </>
                  ) : null}
                </Typography>
              </Stack>
              <Typography variant="body2" color="text.secondary">
                This BIA is currently assigned to the selected Business Continuity Champion. Reassign
                the Champion or update the due date below if responsibilities need to be transferred.
              </Typography>
            </Stack>
          ) : (
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
              Assign this BIA to a Business Continuity Champion responsible for documenting
              processes, completing the Impact Analysis and MOR, and submitting the assessment
              for review by the due date.
            </Typography>
          )}
          <Stack
            direction="row"
            spacing={1.5}
            alignItems="flex-end"
            sx={{ flexWrap: 'nowrap' }}
          >
            <Box sx={{ flex: '1 1 240px', minWidth: 200, maxWidth: 360 }}>
              <FormSelect
                label="Champion"
                value={assigneeId === '' ? '' : String(assigneeId)}
                options={[{ value: '', label: 'Select contributor' }, ...contributors.map((u) => ({ value: String(u.id), label: u.fullName }))]}
                onChange={(v) => setAssigneeId(v === '' ? '' : Number(v))}
                fullWidth
              />
            </Box>
            <Box sx={{ width: 168, flexShrink: 0 }}>
              <CompactTextField
                label="Due date"
                type="date"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
                error={Boolean(dueErr)}
                helperText={dueErr || undefined}
                InputLabelProps={{ shrink: true }}
              />
            </Box>
            <Button
              size="small"
              variant="contained"
              disabled={busy || assigneeId === '' || Boolean(dueErr)}
              onClick={() =>
                void run(async () => {
                  await api.assignBiaCycle(detail.id, { userId: Number(assigneeId), dueDate })
                  await refresh(detail.id)
                }, detail.assignedToUserId != null ? 'Champion reassigned' : 'Assigned to Champion')
              }
              sx={{ flexShrink: 0, height: 32 }}
            >
              {detail.assignedToUserId != null ? 'Reassign' : 'Assign'}
            </Button>
          </Stack>
        </SettingsCard>
      )}

      {!processFormVisible && SHOW_EXCEL_PACK && (
      <SettingsCard autoHeight title="Excel pack">
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
          Optional offline path. Blank template is the pack layout with empty process rows.
          Export writes this BIA (grey system columns such as RTO and MAO are ignored on import).
          Import merges a filled workbook into this cycle.
        </Typography>
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
          <Button size="small" variant="outlined" onClick={() => void api.downloadBiaTemplate().catch((e) => onError(String(e)))}>
            Blank Template
          </Button>
          <Button size="small" variant="outlined" onClick={() => void api.downloadBiaExport(detail.id).catch((e) => onError(String(e)))}>
            Export This BIA
          </Button>
          {canEdit && (
            <Button size="small" variant="outlined" component="label">
              Import Workbook
              <input
                type="file"
                hidden
                accept=".xlsx"
                onChange={(e) => {
                  const f = e.target.files?.[0]
                  e.target.value = ''
                  if (!f) return
                  void run(async () => {
                    await api.importBiaCycle(detail.id, f)
                    await refresh(detail.id)
                  }, 'Imported')
                }}
              />
            </Button>
          )}
        </Stack>
      </SettingsCard>
      )}

      {!processFormVisible && isAdmin && (
        <SettingsCard autoHeight title="Processes">
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
            {canEdit
              ? 'Document all business processes for this department or upload them using the Arbor template. Complete the Identity, Impact Analysis, and Minimum Operating Requirements (MOR) assessments for each process to ensure a comprehensive Business Impact Analysis.'
              : reviewOpen
                ? 'Click a process row to review and record Verified or Returned for Revision.'
                : 'Click a process row to view Identity, Impact analysis, and MOR.'}
          </Typography>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Code</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Tier</TableCell>
                <TableCell>Completeness</TableCell>
                <TableCell>Process Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {detail.processes.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5}>
                    <Typography color="text.secondary" sx={{ py: 1 }}>
                      No processes yet.
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
              {detail.processes.map((p) => (
                <TableRow
                  key={p.id}
                  hover
                  selected={p.id === selectedProcessId}
                  sx={{ cursor: 'pointer' }}
                  onClick={() => openProcessRow(p)}
                >
                  <TableCell>{p.processCode}</TableCell>
                  <TableCell sx={{ minWidth: 0, maxWidth: 280 }}>
                    <Typography sx={{ fontSize: '0.75rem', ...narrativeReadScrollSx }}>{p.processName}</Typography>
                  </TableCell>
                  <TableCell>{p.categoryLabel ?? '-'}</TableCell>
                  <TableCell>{p.completenessLabel}</TableCell>
                  <TableCell>{processStatusLabel(p, detail.status)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          {canEdit && (
            <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap sx={{ mt: 1.5 }}>
              <Button size="small" variant="contained" disabled={busy} onClick={startNewProcess}>
                Add New Process
              </Button>
              <BiaProcessExcelBar
                cycleId={detail.id}
                busy={busy}
                onError={onError}
                onImported={(r) => void handleProcessImported(r)}
              />
            </Stack>
          )}
        </SettingsCard>
      )}

      {!processFormVisible && !isAdmin && (
        <SettingsCard autoHeight title="Processes">
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
            {canEdit
              ? 'Open a process from the list, or add a new one. Download the Arbor template to add several processes at once. Identity, impact, and MOR open on the next screen.'
              : headReviewOpen
                ? 'Select processes and choose Verify, or open a process and use Verify. Approve stays off until every process is Verified by Business Continuity Head.'
                : 'Click a process row to view Identity, Impact analysis, and MOR.'}
          </Typography>
          <Table size="small">
            <TableHead>
              <TableRow>
                {headReviewOpen && (
                  <TableCell padding="checkbox">
                    <Checkbox
                      size="small"
                      checked={headScope.length > 0 && headScope.every((p) => headSelectedIds.includes(p.id))}
                      indeterminate={headSelectedIds.length > 0 && !headScope.every((p) => headSelectedIds.includes(p.id))}
                      onChange={() => {
                        setHeadSelectedIds((prev) => (
                          headScope.every((p) => prev.includes(p.id)) ? [] : headScope.map((p) => p.id)
                        ))
                      }}
                      inputProps={{ 'aria-label': 'Select all processes' }}
                    />
                  </TableCell>
                )}
                <TableCell>Code</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Tier</TableCell>
                <TableCell>Completeness</TableCell>
                <TableCell>Process Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {detail.processes.length === 0 && (
                <TableRow>
                  <TableCell colSpan={headReviewOpen ? 6 : 5}>
                    <Typography color="text.secondary" sx={{ py: 1 }}>No processes yet.</Typography>
                  </TableCell>
                </TableRow>
              )}
              {detail.processes.map((p) => (
                <TableRow
                  key={p.id}
                  hover
                  selected={p.id === selectedProcessId}
                  onClick={() => openProcessRow(p)}
                  sx={{ cursor: 'pointer' }}
                >
                  {headReviewOpen && (
                    <TableCell padding="checkbox" onClick={(e) => e.stopPropagation()}>
                      {p.reviewOutcome !== 'REJECTED' && (
                        <Checkbox
                          size="small"
                          checked={headSelectedIds.includes(p.id)}
                          onChange={() => {
                            setHeadSelectedIds((prev) => (
                              prev.includes(p.id) ? prev.filter((id) => id !== p.id) : [...prev, p.id]
                            ))
                          }}
                          inputProps={{ 'aria-label': `Select ${p.processCode}` }}
                        />
                      )}
                    </TableCell>
                  )}
                  <TableCell>{p.processCode}</TableCell>
                  <TableCell sx={{ minWidth: 0, maxWidth: 280 }}>
                    <Typography sx={{ fontSize: '0.75rem', ...narrativeReadScrollSx }}>{p.processName}</Typography>
                  </TableCell>
                  <TableCell>{p.categoryLabel ?? '-'}</TableCell>
                  <TableCell>{p.completenessLabel}</TableCell>
                  <TableCell>{processStatusLabel(p, detail.status)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          {headReviewOpen && (
            <Stack direction="row" spacing={1} justifyContent="flex-end" sx={{ mt: 1.5 }}>
              <BusyButton
                size="small"
                variant="contained"
                busy={busyAction === 'verify'}
                disabled={busy || headMarkIds.length === 0}
                disabledReason={headSelectedIds.length > 0 && headMarkIds.length === 0 ? 'Already verified' : undefined}
                onClick={() => void run(async () => {
                  if (headMarkIds.length === 0) return
                  await api.headVerifyBia(detail.id, headMarkIds)
                  setHeadSelectedIds([])
                  await refresh(detail.id)
                }, 'Verified', 'verify')}
              >
                {busyAction === 'verify' ? 'Saving…' : 'Verify'}
              </BusyButton>
            </Stack>
          )}
          {canEdit && (
            <Stack spacing={0.75} sx={{ mt: 1.5 }}>
              <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                <Button size="small" variant="contained" disabled={busy} onClick={startNewProcess}>
                  Add New Process
                </Button>
                <BiaProcessExcelBar
                  cycleId={detail.id}
                  busy={busy}
                  onError={onError}
                  onImported={(r) => void handleProcessImported(r)}
                />
                <Button
                  size="small"
                  variant="contained"
                  disabled={busy || !readyToSubmit}
                  onClick={submitBia}
                >
                  {busy ? 'Submitting…' : 'Submit BIA'}
                </Button>
              </Stack>
              {!readyToSubmit && (
                <Typography variant="caption" color="text.secondary">
                  {detail.processes.length === 0
                    ? 'Add at least one complete process before Submit.'
                    : `Submit needs every process Complete. ${
                        detail.processes
                          .filter((p) => !p.completenessOk)
                          .slice(0, 3)
                          .map((p) => `${p.processCode}: ${(p.completenessMissing ?? []).slice(0, 2).join(', ') || p.completenessLabel}`)
                          .join(' · ')
                      }`}
                </Typography>
              )}
            </Stack>
          )}
        </SettingsCard>
      )}

      {processFormVisible && (
      <>
      <Stack
        direction="row"
        spacing={1.5}
        alignItems="center"
        flexWrap="wrap"
        useFlexGap
        sx={{ mb: 1.25, minWidth: 0 }}
      >
        <Button
          size="small"
          variant="outlined"
          disabled={busy}
          onClick={closeProcessScreen}
          sx={controlActionSx}
        >
          Back to Process List
        </Button>
        <Box
          aria-hidden
          sx={{
            display: { xs: 'none', sm: 'block' },
            width: '1px',
            height: 20,
            bgcolor: brand.border,
            flexShrink: 0,
          }}
        />
        {(processForm.processCode.trim() || selected?.processCode) && (
          <StatusChip
            label={`Code ${processForm.processCode.trim() || selected?.processCode}`}
            tone="neutral"
            title={`Process code ${processForm.processCode.trim() || selected?.processCode}`}
          />
        )}
        <Typography
          sx={{
            fontSize: '0.875rem',
            fontWeight: 650,
            lineHeight: 1.3,
            color: brand.ink,
            minWidth: 0,
            flex: '1 1 140px',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
          }}
        >
          {processForm.processName.trim()
            || selected?.processName
            || (selectedProcessId ? 'Process' : 'New process')}
        </Typography>
        {selected && (
          <StatusChip
            label={selected.completenessLabel}
            tone={selected.completenessOk ? 'success' : 'warning'}
          />
        )}
      </Stack>
      <FormSectionShell
        fill={false}
        initialSectionId={
          headReviewOpen ? 'bia-head-review' : canRecordReview ? 'bia-specialist-review' : undefined
        }
        sections={[
          { id: 'bia-identity', label: 'Identity' },
          { id: 'bia-impact', label: 'Impact Analysis' },
          { id: 'bia-mor', label: 'MOR' },
          ...(reviewOpen ? [{ id: 'bia-specialist-review', label: 'Specialist Review' }] : []),
          ...(headReviewOpen ? [{ id: 'bia-head-review', label: 'Head Review' }] : []),
          ...(!isAdmin && !headReviewOpen ? [{ id: 'bia-review', label: 'Completeness' }] : []),
        ]}
      >
        <Box id="bia-identity" sx={{ ...sectionAnchorSx, mt: 1.5 }}>
          <SettingsCard autoHeight title="Process identity">
            {!canEdit && (
              <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
                {reviewOpen
                  ? 'Submitted for Admin review. Record your verdict on the Specialist review tab.'
                  : headReviewOpen
                    ? 'Use Verify on the Head Review tab. Approve stays off until every process is Verified by Business Continuity Head.'
                  : detail.status === 'CLOSED' || detail.status === 'APPROVED'
                    ? 'This BIA is closed. Process details are read-only.'
                    : 'Read-only view. Process details cannot be edited in this status.'}
              </Typography>
            )}
            <Box sx={formFieldGridSx}>
              <CompactTextField
                id="bia-field-processCode"
                wrapLabel
                label="Process Code"
                required
                value={processForm.processCode}
                onChange={(e) => {
                  setProcessForm((s) => ({ ...s, processCode: e.target.value }))
                  setFieldErrors((err) => ({ ...err, processCode: undefined }))
                }}
                disabled={!canEdit}
                error={Boolean(fieldErrors.processCode)}
                helperText={fieldErrors.processCode}
              />
              <CompactTextField
                id="bia-field-processName"
                wrapLabel
                label="Business Process Name"
                required
                value={processForm.processName}
                onChange={(e) => {
                  setProcessForm((s) => ({ ...s, processName: e.target.value }))
                  setFieldErrors((err) => ({ ...err, processName: undefined }))
                }}
                disabled={!canEdit}
                error={Boolean(fieldErrors.processName)}
                helperText={fieldErrors.processName}
              />
              {canEdit ? (
                <FormSelect
                  wrapLabel
                  label="Process owner"
                  required
                  value={processForm.ownerUserId == null ? '' : String(processForm.ownerUserId)}
                  options={[{ value: '', label: 'Select' }, ...ownerOptions]}
                  onChange={(v) => {
                    setProcessForm((s) => ({ ...s, ownerUserId: v === '' ? null : Number(v) }))
                    setFieldErrors((err) => ({ ...err, ownerUserId: undefined }))
                  }}
                  fullWidth
                  error={Boolean(fieldErrors.ownerUserId)}
                  helperText={fieldErrors.ownerUserId}
                />
              ) : (
                <ReadOnlyValue
                  label="Process owner"
                  required
                  value={ownerOptions.find((o) => o.value === String(processForm.ownerUserId ?? ''))?.label ?? '-'}
                />
              )}
              {canEdit ? (
                <FormSelect
                  wrapLabel
                  label="Process Frequency"
                  required
                  value={processForm.frequency ?? ''}
                  options={[{ value: '', label: 'Select' }, ...frequencyOptions]}
                  onChange={(v) => {
                    setProcessForm((s) => ({ ...s, frequency: v }))
                    setFieldErrors((err) => ({ ...err, frequency: undefined }))
                  }}
                  error={Boolean(fieldErrors.frequency)}
                  helperText={fieldErrors.frequency}
                />
              ) : (
                <ReadOnlyValue label="Process Frequency" required value={processForm.frequency ?? '-'} />
              )}
              {canEdit ? (
                <FormSelect
                  wrapLabel
                  label="Third party involved"
                  value={tpInvolved ? 'Yes' : 'None'}
                  options={[
                    { value: 'None', label: 'None' },
                    { value: 'Yes', label: 'Yes' },
                  ]}
                  onChange={(v) => {
                    const yes = v === 'Yes'
                    if (yes === tpInvolved) return
                    setTpInvolved(yes)
                    if (yes) {
                      setProcessForm((s) => ({
                        ...s,
                        thirdPartyActivities: isNoneish(s.thirdPartyActivities) ? '' : s.thirdPartyActivities,
                        supplierName: isNoneish(s.supplierName) ? '' : s.supplierName,
                        depThirdParty: isNoneish(s.depThirdParty) ? '' : s.depThirdParty,
                      }))
                    }
                  }}
                />
              ) : (
                <ReadOnlyValue label="Third party involved" value={tpInvolved ? 'Yes' : 'None'} />
              )}
            </Box>
            {tpInvolved && (
              <Box
                sx={{
                  ...formFieldGridSx,
                  mt: 1.25,
                  gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' },
                }}
              >
                <CompactTextField
                  id="bia-field-thirdPartyActivities"
                  wrapLabel
                  label="Activities Performed by Third Party (if any)"
                  required
                  multiline
                  minRows={2}
                  maxRows={NARRATIVE_MAX_ROWS}
                  value={processForm.thirdPartyActivities ?? ''}
                  onChange={(e) => {
                    setProcessForm((s) => ({ ...s, thirdPartyActivities: e.target.value }))
                    setFieldErrors((err) => ({ ...err, thirdPartyActivities: undefined }))
                  }}
                  disabled={!canEdit}
                  error={Boolean(fieldErrors.thirdPartyActivities)}
                  helperText={fieldErrors.thirdPartyActivities}
                />
                <CompactTextField
                  id="bia-field-supplierName"
                  wrapLabel
                  label="Third Party / Supplier Name"
                  required
                  value={processForm.supplierName ?? ''}
                  onChange={(e) => {
                    setProcessForm((s) => ({ ...s, supplierName: e.target.value }))
                    setFieldErrors((err) => ({ ...err, supplierName: undefined }))
                  }}
                  disabled={!canEdit}
                  error={Boolean(fieldErrors.supplierName)}
                  helperText={fieldErrors.supplierName}
                />
                <CompactTextField
                  wrapLabel
                  label="Dependency - Third Party / Suppliers"
                  multiline
                  minRows={2}
                  maxRows={NARRATIVE_MAX_ROWS}
                  value={processForm.depThirdParty ?? ''}
                  onChange={(e) => setProcessForm((s) => ({ ...s, depThirdParty: e.target.value }))}
                  disabled={!canEdit}
                />
              </Box>
            )}
            <Box
              sx={{
                ...formFieldGridSx,
                mt: 1,
                gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' },
              }}
            >
              <CompactTextField
                id="bia-field-description"
                wrapLabel
                label="Process Description"
                required
                multiline
                minRows={2}
                maxRows={NARRATIVE_MAX_ROWS}
                value={processForm.description ?? ''}
                onChange={(e) => {
                  setProcessForm((s) => ({ ...s, description: e.target.value }))
                  setFieldErrors((err) => ({ ...err, description: undefined }))
                }}
                disabled={!canEdit}
                error={Boolean(fieldErrors.description)}
                helperText={fieldErrors.description}
              />
              <CompactTextField
                id="bia-field-flowTrigger"
                wrapLabel
                label="Process Flow - Trigger / Inflow"
                required
                multiline
                minRows={2}
                maxRows={NARRATIVE_MAX_ROWS}
                value={processForm.flowTrigger ?? ''}
                onChange={(e) => {
                  setProcessForm((s) => ({ ...s, flowTrigger: e.target.value }))
                  setFieldErrors((err) => ({ ...err, flowTrigger: undefined }))
                }}
                disabled={!canEdit}
                error={Boolean(fieldErrors.flowTrigger)}
                helperText={fieldErrors.flowTrigger}
              />
              <CompactTextField
                id="bia-field-flowSteps"
                wrapLabel
                label="Process Flow - Key Steps / Activities Performed by the Organization"
                required
                multiline
                minRows={2}
                maxRows={NARRATIVE_MAX_ROWS}
                value={processForm.flowSteps ?? ''}
                onChange={(e) => {
                  setProcessForm((s) => ({ ...s, flowSteps: e.target.value }))
                  setFieldErrors((err) => ({ ...err, flowSteps: undefined }))
                }}
                disabled={!canEdit}
                error={Boolean(fieldErrors.flowSteps)}
                helperText={fieldErrors.flowSteps}
              />
              <CompactTextField
                id="bia-field-flowOutput"
                wrapLabel
                label="Process Flow - Output / Outflow"
                required
                multiline
                minRows={2}
                maxRows={NARRATIVE_MAX_ROWS}
                value={processForm.flowOutput ?? ''}
                onChange={(e) => {
                  setProcessForm((s) => ({ ...s, flowOutput: e.target.value }))
                  setFieldErrors((err) => ({ ...err, flowOutput: undefined }))
                }}
                disabled={!canEdit}
                error={Boolean(fieldErrors.flowOutput)}
                helperText={fieldErrors.flowOutput}
              />
            </Box>
            <Box
              sx={{
                ...formFieldGridSx,
                mt: 1,
                gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' },
              }}
            >
              <Box id="bia-field-depStaff">
                <BiaPersonPicker
                  id="bia-field-depStaff"
                  label="Dependency - Minimum Staff (roles & numbers)"
                  required
                  value={processForm.depStaffPeople ?? []}
                  onChange={(next) => {
                    setProcessForm((s) => ({ ...s, depStaffPeople: next }))
                    setFieldErrors((err) => ({ ...err, depStaff: undefined }))
                  }}
                  disabled={!canEdit}
                  error={Boolean(fieldErrors.depStaff)}
                  helperText={fieldErrors.depStaff}
                />
              </Box>
              <CompactTextField id="bia-field-depPremises" wrapLabel label="Dependency - Premises / Facilities" required multiline minRows={2} maxRows={NARRATIVE_MAX_ROWS} value={processForm.depPremises ?? ''} onChange={(e) => { setProcessForm((s) => ({ ...s, depPremises: e.target.value })); setFieldErrors((err) => ({ ...err, depPremises: undefined })) }} disabled={!canEdit} error={Boolean(fieldErrors.depPremises)} helperText={fieldErrors.depPremises} />
              <Box id="bia-field-depIt" sx={{ gridColumn: { md: '1 / -1' } }}>
                <BiaAssetTypeRows
                  rows={processForm.depAssets ?? []}
                  types={lookups?.assetTypes}
                  disabled={!canEdit}
                  error={Boolean(fieldErrors.depIt)}
                  helperText={fieldErrors.depIt}
                  onChange={(next) => {
                    setProcessForm((s) => ({ ...s, depAssets: next }))
                    setFieldErrors((err) => ({ ...err, depIt: undefined }))
                  }}
                />
              </Box>
              <CompactTextField id="bia-field-depRecords" wrapLabel label="Dependency - Vital Records / Data" required multiline minRows={2} maxRows={NARRATIVE_MAX_ROWS} value={processForm.depRecords ?? ''} onChange={(e) => { setProcessForm((s) => ({ ...s, depRecords: e.target.value })); setFieldErrors((err) => ({ ...err, depRecords: undefined })) }} disabled={!canEdit} error={Boolean(fieldErrors.depRecords)} helperText={fieldErrors.depRecords} />
            </Box>
          </SettingsCard>
        </Box>

        <Box id="bia-impact" sx={{ ...sectionAnchorSx, mt: 1.5 }}>
          <SettingsCard autoHeight title="Impact Analysis">
            {fieldErrors.impact && (
              <Alert severity="error" sx={{ mb: 1 }}>{fieldErrors.impact}</Alert>
            )}
            <Stack direction="row" spacing={1} alignItems="flex-start" justifyContent="flex-end" flexWrap="wrap" useFlexGap>
              {canEdit && selectedProcessId && !impactEditing && (
                <Button size="small" variant="outlined" onClick={() => setImpactEditing(true)}>
                  Edit
                </Button>
              )}
            </Stack>
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mt: 1.25 }}>
              {([
                ['Tier', displayImpact.categoryLabel ?? '-', tierChipTone(displayImpact.criticalityTier)],
                ['RTO', displayImpact.rtoHours != null ? `${displayImpact.rtoHours} h` : '-', semantic.riskChip.low],
                ['MAO', displayImpact.maoHours != null ? `${displayImpact.maoHours} h` : '-', semantic.riskChip.medium],
              ] as const).map(([label, value, chip]) => (
                <MetricChip key={label} label={label} value={value} tone={chip} />
              ))}
            </Stack>
            {displayImpact.rtoCaption ? (
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.75 }}>
                {displayImpact.rtoCaption}
                {canEdit && impactEditing && impactPreview.rtoCaption
                  ? ' Preview until you Save.'
                  : ''}
              </Typography>
            ) : null}
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.75 }}>
              1 Insignificant · 2 Minor · 3 Medium · 4 Major · 5 Critical
            </Typography>
            <Table size="small" sx={{ mt: 1 }}>
              <TableHead>
                <TableRow>
                  <TableCell>Category</TableCell>
                  {BAND_LABELS.map((l) => (
                    <TableCell key={l}>{l}</TableCell>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {CATEGORIES.map((cat) => (
                  <TableRow key={cat}>
                    <TableCell>{CAT_LABELS[cat]}</TableCell>
                    {BANDS.map((band, bandIdx) => {
                      const value = ratings[`${cat}|${band}`] ?? ''
                      const chip = impactChipTone(value)
                      const editing = canEdit && impactEditing
                      if (!editing) {
                        return (
                          <TableCell key={band}>
                            <Box title={typeof value === 'number' ? `${value} · ${IMPACT_SCALE[value].name}` : 'Not rated'}>
                              <MetricChip value={value === '' ? '-' : String(value)} tone={chip} />
                            </Box>
                          </TableCell>
                        )
                      }
                      const floor = ratingFloor(ratings, cat, bandIdx)
                      return (
                        <TableCell key={band}>
                          <Select
                            size="small"
                            value={String(value)}
                            displayEmpty
                            renderValue={(v) => (v === '' ? '-' : String(v))}
                            onChange={(e) => {
                              const raw = e.target.value
                              setRatings((s) => applyRating(s, cat, band, raw === '' ? '' : Number(raw)))
                            }}
                            sx={{
                              ...compactSelectSx,
                              minWidth: 52,
                              maxWidth: 64,
                              bgcolor: chip.bg,
                              color: chip.text,
                              '& .MuiSelect-icon': { color: chip.text },
                              '& .MuiOutlinedInput-notchedOutline': { borderColor: 'transparent' },
                            }}
                          >
                            {floor <= 1 && <MenuItem value="">-</MenuItem>}
                            {([1, 2, 3, 4, 5] as const)
                              .filter((n) => n >= floor)
                              .map((n) => (
                                <MenuItem key={n} value={String(n)} sx={{ fontSize: '0.75rem' }}>
                                  {n} · {IMPACT_SCALE[n].name}
                                </MenuItem>
                              ))}
                          </Select>
                        </TableCell>
                      )
                    })}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </SettingsCard>
        </Box>

        <Box id="bia-mor" sx={{ ...sectionAnchorSx, mt: 1.5 }}>
          <SettingsCard autoHeight title="Minimum operating requirements">
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1.25 }}>
              Minimum Operating Requirements (MOR) define the essential people, processes, systems, facilities, and resources required to maintain critical business operations during a disruption.
            </Typography>
            {!displayImpact.morRequired && (
              <Typography variant="body2" color="text.secondary">
                {impactPreview.peakScore == null && !selected?.morRequired
                  ? 'Fill impact ratings to see whether MOR is required. Critical (Tier 1-3) processes need MOR.'
                  : 'MOR is not required for Normal (Tier 4-5) processes.'}
              </Typography>
            )}
            {displayImpact.morRequired && (
              <>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                  Mandatory for Critical Process (Tier 1-3)
                </Typography>
                <Box
                  sx={{
                    ...formFieldGridSx,
                    mb: 1.25,
                    gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' },
                    columnGap: { xs: 1, md: 3 },
                    rowGap: 1.5,
                  }}
                >
                  <Box id="bia-field-minStaff">
                    <BiaPersonPicker
                      label="Min staff"
                      required
                      value={mor.minStaffPeople}
                      restrictTo={processForm.depStaffPeople ?? []}
                      onChange={(next) => {
                        setMor((s) => ({ ...s, minStaffPeople: next }))
                        setFieldErrors((err) => ({ ...err, minStaff: undefined }))
                      }}
                      disabled={!canEdit || (processForm.depStaffPeople ?? []).length === 0}
                      error={Boolean(fieldErrors.minStaff)}
                      helperText={
                        fieldErrors.minStaff
                          ?? ((processForm.depStaffPeople ?? []).length === 0
                            ? 'Add Dependency Minimum Staff on Identity first'
                            : undefined)
                      }
                    />
                  </Box>
                  <Box id="bia-field-recoveryStrategy">
                    <BiaCompactMultiSelect
                      label="Recovery strategy"
                      required
                      options={lookups?.recoveryStrategies ?? []}
                      value={mor.recoveryStrategies}
                      onChange={(next) => {
                        setMor((s) => ({ ...s, recoveryStrategies: next }))
                        setFieldErrors((err) => ({
                          ...err,
                          recoveryStrategy: undefined,
                          recoveryComments: next.includes(BIA_RECOVERY_OTHER) ? err.recoveryComments : undefined,
                        }))
                      }}
                      disabled={!canEdit}
                      error={Boolean(fieldErrors.recoveryStrategy)}
                      helperText={fieldErrors.recoveryStrategy}
                      noun={mor.recoveryStrategies.length === 1 ? 'strategy' : 'strategies'}
                    />
                  </Box>
                </Box>
                {mor.recoveryStrategies.includes(BIA_RECOVERY_OTHER) && (
                  <Box sx={{ mb: 1.25 }}>
                    <CompactTextField
                      id="bia-field-recoveryComments"
                      wrapLabel
                      label="Comments"
                      required
                      multiline
                      minRows={2}
                      maxRows={NARRATIVE_MAX_ROWS}
                      value={mor.recoveryComments}
                      onChange={(e) => {
                        setMor((s) => ({ ...s, recoveryComments: e.target.value }))
                        setFieldErrors((err) => ({ ...err, recoveryComments: undefined }))
                      }}
                      disabled={!canEdit}
                      error={Boolean(fieldErrors.recoveryComments)}
                      helperText={
                        fieldErrors.recoveryComments
                          ?? (canEdit && !mor.recoveryComments.trim()
                            ? 'Describe the other recovery strategy.'
                            : undefined)
                      }
                    />
                  </Box>
                )}
                <Typography variant="subtitle2" sx={{ mb: 0.75 }}>Minimum Operating Requirements (Resources)</Typography>
                {fieldErrors.morResources && (
                  <Typography variant="caption" color="error" sx={{ display: 'block', mb: 0.75 }}>
                    {fieldErrors.morResources}
                  </Typography>
                )}
                <Box sx={formFieldGridSx}>
                  <FormSelect
                    label="Internet Connectivity"
                    required
                    value={mor.internetConnectivity}
                    options={[{ value: '', label: 'Select' }, { value: 'Yes', label: 'Yes' }, { value: 'No', label: 'No' }]}
                    onChange={(v) => setMor((s) => ({ ...s, internetConnectivity: v }))}
                    disabled={!canEdit}
                  />
                  <FormSelect
                    label="Intranet Connectivity"
                    required
                    value={mor.intranetConnectivity}
                    options={[{ value: '', label: 'Select' }, { value: 'Yes', label: 'Yes' }, { value: 'No', label: 'No' }]}
                    onChange={(v) => setMor((s) => ({ ...s, intranetConnectivity: v }))}
                    disabled={!canEdit}
                  />
                  <FormSelect
                    label="VPN Access"
                    required
                    value={mor.vpnAccess}
                    options={[{ value: '', label: 'Select' }, { value: 'Yes', label: 'Yes' }, { value: 'No', label: 'No' }]}
                    onChange={(v) => setMor((s) => ({ ...s, vpnAccess: v }))}
                    disabled={!canEdit}
                  />
                  <FormSelect
                    label="Primary / Secondary Site"
                    required
                    value={mor.siteType}
                    options={[
                      { value: '', label: 'Select' },
                      { value: 'Primary Site', label: 'Primary Site' },
                      { value: 'Secondary Site', label: 'Secondary Site' },
                      { value: 'Other', label: 'Other' },
                    ]}
                    onChange={(v) => {
                      setMor((s) => ({ ...s, siteType: v }))
                      setFieldErrors((err) => ({
                        ...err,
                        siteOther: v === BIA_SITE_OTHER ? err.siteOther : undefined,
                      }))
                    }}
                    disabled={!canEdit}
                  />
                  <FormSelect
                    label="M365"
                    required
                    value={mor.m365}
                    options={[{ value: '', label: 'Select' }, { value: 'Yes', label: 'Yes' }, { value: 'No', label: 'No' }]}
                    onChange={(v) => setMor((s) => ({ ...s, m365: v }))}
                    disabled={!canEdit}
                  />
                  <CompactTextField
                    label="Laptops"
                    required
                    value={mor.laptops}
                    onChange={(e) => {
                      const v = e.target.value
                      if (v === '' || /^\d*$/.test(v)) setMor((s) => ({ ...s, laptops: v }))
                    }}
                    disabled={!canEdit}
                  />
                  {mor.siteType === BIA_SITE_OTHER && (
                    <CompactTextField
                      id="bia-field-siteOther"
                      wrapLabel
                      label="Other site"
                      required
                      maxWidth={COMPACT_SELECT_MAX_WIDTH}
                      value={mor.siteOther}
                      onChange={(e) => {
                        setMor((s) => ({ ...s, siteOther: e.target.value }))
                        setFieldErrors((err) => ({ ...err, siteOther: undefined }))
                      }}
                      disabled={!canEdit}
                      error={Boolean(fieldErrors.siteOther)}
                      helperText={
                        fieldErrors.siteOther
                          ?? (canEdit && !mor.siteOther.trim() ? 'Name the site.' : undefined)
                      }
                    />
                  )}
                </Box>
                <Box sx={{ mt: 1.25 }}>
                  <FormFieldLabel required>Software / Application</FormFieldLabel>
                  <Stack spacing={1}>
                    {mor.softwareApplications.map((name, idx) => (
                      <Stack key={idx} direction="row" spacing={1} alignItems="flex-end">
                        <Box sx={{ flex: 1, minWidth: 0 }}>
                          <CompactTextField
                            label={idx === 0 ? 'Application name' : `Application name ${idx + 1}`}
                            value={name}
                            onChange={(e) => {
                              const next = [...mor.softwareApplications]
                              next[idx] = e.target.value
                              setMor((s) => ({ ...s, softwareApplications: next }))
                            }}
                            disabled={!canEdit}
                          />
                        </Box>
                        {canEdit && mor.softwareApplications.length > 1 && (
                          <IconButton
                            size="small"
                            aria-label="Remove application"
                            onClick={() => setMor((s) => ({
                              ...s,
                              softwareApplications: s.softwareApplications.filter((_, i) => i !== idx),
                            }))}
                            sx={{
                              height: 32,
                              width: 32,
                              border: '1px solid',
                              borderColor: 'divider',
                              borderRadius: 1,
                            }}
                          >
                            <DeleteOutlineIcon fontSize="small" />
                          </IconButton>
                        )}
                      </Stack>
                    ))}
                    {canEdit && (
                      <Box sx={{ pt: 0.25 }}>
                        <Button
                          size="small"
                          variant="outlined"
                          startIcon={<AddRoundedIcon sx={{ fontSize: 16 }} />}
                          onClick={() => setMor((s) => ({ ...s, softwareApplications: [...s.softwareApplications, ''] }))}
                          sx={controlActionSx}
                        >
                          Add Application
                        </Button>
                      </Box>
                    )}
                  </Stack>
                </Box>
                <Box sx={{ mt: 1.5 }} id="bia-field-thirdPartyContacts">
                  <FormFieldLabel>Third party support</FormFieldLabel>
                  {!tpInvolved ? (
                    <Typography
                      variant="body2"
                      sx={{
                        minHeight: 32,
                        display: 'flex',
                        alignItems: 'center',
                        color: brand.muted,
                      }}
                    >
                      N/A
                    </Typography>
                  ) : (
                    <Stack spacing={1} sx={{ mt: 0.75 }}>
                      {fieldErrors.thirdPartyContacts && (
                        <Typography variant="caption" color="error">{fieldErrors.thirdPartyContacts}</Typography>
                      )}
                      {(mor.thirdPartyContacts.length === 0 ? [{ name: '', email: '', phone: '' }] : mor.thirdPartyContacts).map((c, idx) => {
                        const setContact = (patch: Partial<BiaThirdPartyContact>) => {
                          const base = mor.thirdPartyContacts.length === 0
                            ? [{ name: '', email: '', phone: '' }]
                            : mor.thirdPartyContacts
                          setMor((s) => ({
                            ...s,
                            thirdPartyContacts: base.map((row, i) => (i === idx ? { ...row, ...patch } : row)),
                          }))
                        }
                        return (
                          <Box
                            key={idx}
                            sx={{
                              ...formFieldGridSx,
                              gridTemplateColumns: { xs: '1fr', md: 'minmax(0, 1fr) minmax(0, 1fr) minmax(0, 1fr) auto' },
                              alignItems: 'end',
                            }}
                          >
                            <CompactTextField
                              wrapLabel
                              label="Name"
                              required
                              value={c.name}
                              onChange={(e) => setContact({ name: e.target.value })}
                              disabled={!canEdit}
                            />
                            <CompactTextField
                              wrapLabel
                              label="Email"
                              required
                              value={c.email}
                              onChange={(e) => setContact({ email: e.target.value })}
                              disabled={!canEdit}
                            />
                            <CompactTextField
                              wrapLabel
                              label="Contact number (Optional)"
                              value={c.phone ?? ''}
                              onChange={(e) => setContact({ phone: e.target.value })}
                              disabled={!canEdit}
                            />
                            {canEdit && (
                              <IconButton
                                size="small"
                                aria-label="Remove contact"
                                onClick={() => setMor((s) => ({
                                  ...s,
                                  thirdPartyContacts: s.thirdPartyContacts.filter((_, i) => i !== idx),
                                }))}
                                sx={{
                                  height: 32,
                                  width: 32,
                                  mb: 0.25,
                                  border: '1px solid',
                                  borderColor: 'divider',
                                  borderRadius: 1,
                                }}
                              >
                                <DeleteOutlineIcon fontSize="small" />
                              </IconButton>
                            )}
                          </Box>
                        )
                      })}
                      {canEdit && (
                        <Box sx={{ pt: 0.25 }}>
                          <Button
                            size="small"
                            variant="outlined"
                            startIcon={<AddRoundedIcon sx={{ fontSize: 16 }} />}
                            onClick={() => setMor((s) => ({
                              ...s,
                              thirdPartyContacts: [...(s.thirdPartyContacts.length ? s.thirdPartyContacts : [{ name: '', email: '', phone: '' }]), { name: '', email: '', phone: '' }],
                            }))}
                            sx={controlActionSx}
                          >
                            Add Contact
                          </Button>
                        </Box>
                      )}
                    </Stack>
                  )}
                </Box>
              </>
            )}
          </SettingsCard>
        </Box>

        {reviewOpen && (
        <Box id="bia-specialist-review" sx={{ ...sectionAnchorSx, mt: 1.5 }}>
          <SettingsCard autoHeight title="Specialist Review">
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              {canRecordReview
                ? 'Record a verdict for this process. Use Identity, Impact analysis, and MOR tabs to inspect details. The BIA can go to Business Continuity Head when every in-scope process is Verified. Switch processes from the list above.'
                : 'This BIA is with the Champion. Outcome and comments are read-only until they resubmit.'}
            </Typography>
            {selectedProcessId ? (
              <Stack spacing={1.25}>
                <Typography variant="body2">
                  {selected?.processCode} · {selected?.completenessLabel}
                  {selected?.reviewOutcomeLabel ? ` · ${selected.reviewOutcomeLabel}` : ''}
                </Typography>
                {canRecordReview ? (
                <Stack spacing={0.75}>
                  <FieldActionRow spacing={1.5} sx={{ flexWrap: 'nowrap' }}>
                    <Box sx={{ width: 200, flexShrink: 0 }}>
                      <FormSelect
                        label="Outcome"
                        required
                        value={reviewOutcome}
                        options={[
                          { value: '', label: 'Select' },
                          ...(lookups?.reviewOutcomes ?? ['Verified', 'Returned for Revision'])
                            .filter((o) => o !== 'Pending Review')
                            .map((o) => ({ value: o, label: o })),
                        ]}
                        onChange={setReviewOutcome}
                        fullWidth
                      />
                    </Box>
                    <Box sx={{ flex: 1, minWidth: 160 }}>
                      <CompactTextField
                        label={
                          reviewOutcome.toLowerCase().includes('return')
                            ? 'Reviewer comments'
                            : 'Reviewer comments (Optional)'
                        }
                        required={reviewOutcome.toLowerCase().includes('return')}
                        value={reviewComment}
                        onChange={(e) => setReviewComment(e.target.value)}
                      />
                    </Box>
                    <BusyButton
                      size="small"
                      variant="contained"
                      disabled={
                        busy ||
                        !reviewOutcome.trim() ||
                        (reviewOutcome.toLowerCase().includes('return') && !reviewComment.trim())
                      }
                      disabledReason={
                        !reviewOutcome.trim()
                          ? 'Select an outcome for this process'
                          : reviewOutcome.toLowerCase().includes('return') && !reviewComment.trim()
                            ? 'Comment is required when returning a process'
                            : undefined
                      }
                      onClick={() =>
                        void run(async () => {
                          await api.reviewBiaProcess(selectedProcessId, {
                            outcome: reviewOutcome,
                            comment: reviewComment,
                          })
                          await refresh(detail.id, null)
                        }, 'Review saved')
                      }
                      sx={controlActionSx}
                    >
                      {busy ? 'Saving…' : 'Save Review'}
                    </BusyButton>
                  </FieldActionRow>
                  {!reviewOutcome.trim() ? (
                    <Typography variant="caption" color="text.secondary">
                      Select an outcome for this process
                    </Typography>
                  ) : reviewOutcome.toLowerCase().includes('return') && !reviewComment.trim() ? (
                    <Typography variant="caption" color="error">
                      Comment is required when returning a process
                    </Typography>
                  ) : null}
                </Stack>
                ) : (
                <Stack spacing={0.75}>
                  <FieldActionRow spacing={1.5} sx={{ flexWrap: 'nowrap' }}>
                    <Box sx={{ width: 200, flexShrink: 0 }}>
                      <ReadOnlyValue
                        label="Outcome"
                        required
                        value={selected?.reviewOutcomeLabel || reviewOutcomeDisplay(selected?.reviewOutcome) || '-'}
                      />
                    </Box>
                    <Box sx={{ flex: 1, minWidth: 160 }}>
                      <ReadOnlyValue
                        label="Reviewer comments"
                        value={selected?.reviewComment || reviewComment || '-'}
                        scroll
                      />
                    </Box>
                    <BusyButton
                      size="small"
                      variant="contained"
                      disabled
                      disabledReason={reviewLockedReason}
                      sx={controlActionSx}
                    >
                      Save Review
                    </BusyButton>
                  </FieldActionRow>
                  <Typography variant="caption" color="text.secondary">
                    {reviewLockedReason}
                  </Typography>
                </Stack>
                )}
              </Stack>
            ) : (
              <Typography variant="body2">Open a process from the list to record a verdict.</Typography>
            )}
          </SettingsCard>
        </Box>
        )}

        {headReviewOpen && (
        <Box id="bia-head-review" sx={{ ...sectionAnchorSx, mt: 1.5 }}>
          <SettingsCard autoHeight title="Head Review">
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              Verify this process. Approve stays off until every process is Verified by Business Continuity Head.
            </Typography>
            {!selectedProcessId && (
              <Typography variant="body2">Open a process from the list to verify it.</Typography>
            )}
            {selected && selected.reviewOutcome === 'REJECTED' && (
              <Typography variant="body2">This process is out of scope.</Typography>
            )}
            {selected && selected.reviewOutcome !== 'REJECTED' && selected.headVerified && (
              <ReadOnlyValue label="Process Status" value="Verified by Business Continuity Head" />
            )}
            {selected && selected.reviewOutcome !== 'REJECTED' && !selected.headVerified && (
              <BusyButton
                size="small"
                variant="contained"
                busy={busyAction === 'verify'}
                disabled={busy}
                onClick={() => void run(async () => {
                  if (selectedProcessId == null) return
                  await api.headVerifyBia(detail.id, [selectedProcessId])
                  await refresh(detail.id, selectedProcessId)
                }, 'Verified', 'verify')}
                sx={controlActionSx}
              >
                {busyAction === 'verify' ? 'Saving…' : 'Verify'}
              </BusyButton>
            )}
          </SettingsCard>
        </Box>
        )}

        {!isAdmin && !headReviewOpen && (
        <Box id="bia-review" sx={{ ...sectionAnchorSx, mt: 1.5 }}>
          <SettingsCard autoHeight title="Completeness">
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              System checklist for the selected process. Submit is blocked until every in-scope process is ready for review.
            </Typography>
            {selected && (
              <Typography variant="body2" sx={{ mb: 1 }}>
                {selected.processCode} · {selected.completenessLabel}
                {selected.completenessMissing.length > 0 ? ` · Missing: ${selected.completenessMissing.join(', ')}` : ''}
              </Typography>
            )}
            {!selected && (
              <Typography variant="body2" color="text.secondary">Select a process to see what is still missing.</Typography>
            )}
          </SettingsCard>
        </Box>
        )}

        {canEdit && (
          <Stack spacing={1} sx={{ mt: 2, pb: 1 }}>
            <Stack direction="row" spacing={1} justifyContent="flex-end" flexWrap="wrap" useFlexGap>
              <Button
                variant="outlined"
                disabled={actionsLocked || busy || !processForm.processCode.trim() || !processForm.processName.trim()}
                onClick={() => void run(() => saveProcessCombined(true), 'Draft saved')}
              >
                {busy ? 'Saving…' : 'Save Draft'}
              </Button>
              <Button
                variant="contained"
                disabled={actionsLocked || busy}
                onClick={() => void run(() => saveProcessCombined(false), 'Process saved')}
              >
                {busy ? 'Saving…' : 'Save'}
              </Button>
              {canDeleteSelectedProcess && selectedProcessId && (
                <Button
                  variant="outlined"
                  color="error"
                  disabled={actionsLocked || busy}
                  onClick={() => setConfirmDeleteProcess(true)}
                >
                  Delete
                </Button>
              )}
              <Button variant="outlined" disabled={busy} onClick={closeProcessScreen}>
                Back to Process List
              </Button>
            </Stack>
            {saveBlockReason ? (
              <Typography variant="caption" color="error" sx={{ textAlign: 'right' }}>
                {saveBlockReason}
              </Typography>
            ) : (
              <Typography variant="caption" color="text.secondary" sx={{ textAlign: 'right' }}>
                Save requires all asterisked fields, impact ratings, and MOR when Critical.
              </Typography>
            )}
          </Stack>
        )}
      </FormSectionShell>
      </>
      )}
      </Stack>
    </Box>
    </Box>

    <ConfirmActionDialog
      open={confirmSendHead}
      title="Send to Business Continuity Head?"
      message={`Send this ${
        detailDepartmentName ? `${detailDepartmentName} ` : ''
      }BIA for final approval?`}
      confirmLabel={busy ? 'Sending…' : 'Send'}
      confirmColor="primary"
      busy={busy}
      onCancel={() => setConfirmSendHead(false)}
      onConfirm={() =>
        void run(async () => {
          await api.sendBiaToHead(detail!.id, {
            bcHeadUserId: Number(headUserId),
            notes: headNotes || null,
          })
          setConfirmSendHead(false)
          await refresh(detail!.id, null)
        }, 'Sent to Business Continuity Head')
      }
    />
    <ConfirmActionDialog
      open={confirmCloseBia}
      title="Close Department BIA?"
      message="This action will finalize the assessment and complete the BIA workflow."
      confirmLabel={busy ? 'Closing…' : 'Close Department BIA'}
      confirmColor="primary"
      busy={busy}
      onCancel={() => setConfirmCloseBia(false)}
      onConfirm={() =>
        void run(async () => {
          await api.closeBiaCycle(detail!.id)
          setConfirmCloseBia(false)
          await refresh(detail!.id, null)
        }, 'Department BIA closed')
      }
    />
    <Dialog
      open={confirmDeleteProcess}
      onClose={() => !busy && setConfirmDeleteProcess(false)}
      maxWidth="xs"
      fullWidth
    >
      <DialogTitle sx={{ pb: 1 }}>Delete process?</DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary">
          Delete{' '}
          <strong>
            {selected?.processCode ?? 'this process'}
            {selected?.processName ? ` (${selected.processName})` : ''}
          </strong>
          ? This cannot be undone.
        </Typography>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={() => setConfirmDeleteProcess(false)} disabled={busy}>
          Cancel
        </Button>
        <Button
          color="error"
          variant="contained"
          disabled={busy || !selectedProcessId}
          onClick={() =>
            void run(async () => {
              if (!selectedProcessId || !detail) return
              await api.deleteBiaProcess(selectedProcessId)
              setConfirmDeleteProcess(false)
              await refresh(detail.id, null)
            }, 'Process deleted')
          }
        >
          Delete
        </Button>
      </DialogActions>
    </Dialog>
    </>
  )
}
