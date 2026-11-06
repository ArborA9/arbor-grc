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
  LinearProgress,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import ChevronRightRoundedIcon from '@mui/icons-material/ChevronRightRounded'
import {
  api,
  ApiError,
  type ContactRole,
  type Session,
  type TprmAssessment,
  type TprmAssessmentItem,
  type TprmCertType,
  type TprmCompliance,
  type TprmFinding,
  type TprmKpis,
  type TprmLookups,
  type TprmVendor,
  type TprmVerdict,
  type UpsertVendorBody,
  type VendorLifecycleState,
  type VendorType,
} from './api'
import { useAppShellPage } from './components/AppShellPageContext'
import StatusChip from './components/StatusChip'
import TprmReviewDrawer from './components/tprm/TprmReviewDrawer'
import {
  BusyButton,
  CompactTextField,
  ConfirmActionDialog,
  FieldActionRow,
  INPUT_LIMIT,
  FormSectionShell,
  useFormSectionShell,
  KpiCard,
  KpiCardGrid,
  RegisterListFrame,
  RegisterSortHeader,
  RegisterToolbar,
  RiskLevelBadge,
  SettingsCard,
  complexFormContainerSx,
  controlActionSx,
  formFieldGridSx,
  twoColFieldSx,
  NARRATIVE_FIELD_MAX_WIDTH,
  NARRATIVE_MAX_ROWS,
  lastTableCellSx,
  primaryRegisterNameSx,
  primaryTableCellSx,
  compactTableCellSx,
  mediumTableCellSx,
  registerTableSx,
  sectionAnchorSx,
} from './components/ui'
import { useListReady } from './uiQuality/useListReady'
import { usePagedRows } from './uiQuality/usePagedRows'
import { usePersistedState } from './uiQuality/usePersistedState'
import { compareLoose, toggleSort, type SortDir } from './uiQuality/sortRows'
import { useOptionalUnsavedChangesGuardApi, useRegisterUnsavedChanges } from './uiQuality/UnsavedChangesGuard'
import FormSelect from './components/ui/FormSelect'
import { vendorCriticalityToBadge } from './components/ui/riskBadgeMappers'
import { RATING_TONE_COLOR, type RatingTone } from './components/ui/ratingTone'
import { TPRM_REGISTER_COLUMNS } from './hooks/registerColumns'
import { useRegisterColumnPicker } from './hooks/useRegisterColumnPicker'
import { brand, semantic } from './theme'
import {
  emailError,
  formatMobile,
  parseStoredMobile,
  storedMobileForApi,
  phoneError,
  phoneDigitHint,
  nationalLength,
  nationalPlaceholder,
  PHONE_COUNTRY_OPTIONS,
  useFormFields,
  type PhoneDial,
} from './validation'

const FACTOR_KEYS = ['f1', 'f2', 'f3', 'f4', 'f5', 'f6'] as const
type FactorKey = (typeof FACTOR_KEYS)[number]

const FACTOR_LABEL: Record<FactorKey, string> = {
  f1: 'Information exposure',
  f2: 'Access to Organization systems',
  f3: 'Business criticality',
  f4: 'Delivery environment',
  f5: 'Personal data',
  f6: 'Concentration and substitutability',
}

/** Excel Calculation Reference / Lookups scores (1-4). Score is calculated, not typed. */
const FACTOR_SCORE_TABLE: Record<FactorKey, Record<string, number>> = {
  f1: { Secret: 4, Sensitive: 3, Confidential: 2, Open: 1 },
  f2: {
    'Privileged or administrative access': 4,
    'Standard named user access': 3,
    'Read-only or view access, or physical access to a critical area': 2,
    'No access of any kind': 1,
  },
  f3: {
    'A critical service stops, with no workaround': 4,
    'A critical service degrades, a manual workaround exists': 3,
    'An important but non-critical service is affected': 2,
    'Minimal operational effect': 1,
  },
  f4: {
    'Vendor-owned infrastructure, worked remotely / off site': 4,
    'Vendor-owned laptops or devices, worked on Organization premises': 3,
    'Organization-provided assets, worked remotely / off site': 2,
    'Organization-provided assets, worked on Organization premises': 1,
  },
  f5: {
    'Large volumes, or special-category data': 4,
    'Routine personal data as part of the service': 3,
    'Incidental exposure only': 2,
    'No personal data': 1,
  },
  f6: {
    'Sole source, replacement would take more than six months': 4,
    'Few alternatives, replacement would take three to six months': 3,
    'Several alternatives, replacement within three months': 2,
    'Readily replaceable, commodity service': 1,
  },
}

function factorScoreValue(key: FactorKey, option: string | null | undefined, saved: number | null | undefined): number | null {
  if (option) {
    const live = FACTOR_SCORE_TABLE[key][option]
    if (live != null) return live
  }
  if (saved != null) return saved
  return null
}

/** Higher factor score = higher third-party risk (Excel 1-4). */
function factorScoreTone(score: number | null): RatingTone | null {
  if (score == null) return null
  if (score >= 4) return 'critical'
  if (score >= 3) return 'high'
  if (score >= 2) return 'medium'
  return 'low'
}

function nextTierUp(tier: string): string {
  if (tier === 'TIER_4') return 'TIER_3'
  if (tier === 'TIER_3') return 'TIER_2'
  return 'TIER_1'
}

const FACTOR_SCORE_KEY: Record<FactorKey,
  'f1Score' | 'f2Score' | 'f3Score' | 'f4Score' | 'f5Score' | 'f6Score'> = {
  f1: 'f1Score',
  f2: 'f2Score',
  f3: 'f3Score',
  f4: 'f4Score',
  f5: 'f5Score',
  f6: 'f6Score',
}

const CREATE_SECTIONS = [
  { id: 'tprm-identity', label: 'Identity' },
  { id: 'tprm-engagement', label: 'Engagement' },
  { id: 'tprm-contacts', label: 'Contacts' },
  { id: 'tprm-categorisation', label: 'Risk Profile' },
  { id: 'tprm-certs', label: 'Assurance' },
]

/** Percent tracks ~100%. Short headers use minWidth so labels like Inherent / Next review stay fully visible. */
const tprmCell = {
  name: { ...primaryTableCellSx, width: '14%', minWidth: 120 },
  type: { ...mediumTableCellSx, width: '12%', minWidth: 108 },
  inherent: { ...compactTableCellSx, width: '8%', minWidth: 72, whiteSpace: 'nowrap' as const, overflow: 'visible' },
  tier: { ...compactTableCellSx, width: '5%', minWidth: 44, whiteSpace: 'nowrap' as const, overflow: 'visible' },
  criticality: { ...compactTableCellSx, width: '11%', minWidth: 100, overflow: 'visible', whiteSpace: 'nowrap' as const },
  posture: { ...compactTableCellSx, width: '7%', minWidth: 64, whiteSpace: 'nowrap' as const, overflow: 'visible' },
  compliance: { ...compactTableCellSx, width: '13%', minWidth: 112, overflow: 'visible', whiteSpace: 'nowrap' as const },
  state: { ...compactTableCellSx, width: '12%', minWidth: 100, overflow: 'visible', whiteSpace: 'nowrap' as const },
  nextReview: { ...lastTableCellSx, width: '18%', minWidth: 108, whiteSpace: 'nowrap' as const, overflow: 'visible' },
}

const blankContact = (role: ContactRole) => ({ role, name: '', email: '', mobile: '' })

function pct(v: number | null | undefined) {
  if (v == null) return '-'
  return `${Math.round(v * 1000) / 10}%`
}

function tierLabel(tier: string | null | undefined) {
  if (!tier) return '-'
  return tier.replace('TIER_', 'T')
}

function pretty(value: string | null | undefined) {
  if (!value) return '-'
  return value.replace(/_/g, ' ')
}

type TprmReviewFieldSnap = { verdict: TprmVerdict | null; comment: string | null }

function buildTprmReviewSnap(items: TprmAssessmentItem[]): Record<number, TprmReviewFieldSnap> {
  const out: Record<number, TprmReviewFieldSnap> = {}
  for (const it of items) {
    out[it.id] = { verdict: it.reviewVerdict ?? null, comment: it.reviewComment ?? null }
  }
  return out
}

function isTprmReviewDirty(
  items: TprmAssessmentItem[],
  snap: Record<number, TprmReviewFieldSnap> | null,
): boolean {
  if (!snap) return false
  return items.some((it) => {
    const s = snap[it.id]
    const verdict = it.reviewVerdict ?? null
    const comment = it.reviewComment ?? null
    if (!s) return verdict != null || (comment ?? '') !== ''
    return verdict !== s.verdict || (comment ?? '') !== (s.comment ?? '')
  })
}

const COMPLIANCE_LABEL: Record<TprmCompliance, string> = {
  UNKNOWN: 'Not reviewed',
  COMPLIANT: 'Compliant',
  NOT_COMPLIANT: 'Not compliant',
}

const LIFECYCLE_LABEL: Record<VendorLifecycleState, string> = {
  PROFILE_INCOMPLETE: 'Incomplete',
  CATEGORISED: 'Categorised',
  QUESTIONNAIRE_READY: 'Ready to issue',
  ISSUED: 'Issued',
  SUBMITTED: 'Submitted',
  IN_REVIEW: 'In review',
  RETURNED: 'Returned',
  COMPLETE: 'Complete',
}

function lifecycleTone(state: VendorLifecycleState): 'success' | 'warning' | 'info' | 'danger' | 'neutral' {
  if (state === 'COMPLETE') return 'success'
  if (state === 'SUBMITTED' || state === 'IN_REVIEW') return 'info'
  if (state === 'ISSUED' || state === 'RETURNED' || state === 'QUESTIONNAIRE_READY') return 'warning'
  if (state === 'PROFILE_INCOMPLETE') return 'danger'
  return 'neutral'
}

function assessmentTone(status: string): 'success' | 'warning' | 'info' | 'danger' | 'neutral' {
  if (status === 'COMPLETE') return 'success'
  if (status === 'SUBMITTED' || status === 'IN_REVIEW') return 'info'
  if (status === 'ISSUED' || status === 'IN_PROGRESS' || status === 'RETURNED' || status === 'DRAFT') return 'warning'
  return 'neutral'
}

function reviewCompleteMissing(assessment: TprmAssessment): string | null {
  const missing = assessment.items.filter((it) => !it.reviewVerdict)
  if (missing.length > 0) {
    return `Set a verdict on every control before completing (${missing.length} remaining).`
  }
  if (assessment.items.some((it) => it.reviewVerdict === 'ACCEPTED_WITH_OBSERVATION' && !(it.reviewComment ?? '').trim())) {
    return 'Add a review comment on every Accepted with observation control'
  }
  return null
}

function emptyToNull(value: string | null | undefined): string | null {
  const t = value?.trim()
  return t ? t : null
}

function contactOf(form: UpsertVendorBody, role: ContactRole) {
  return form.contacts?.find((c) => c.role === role) ?? blankContact(role)
}

function setContact(form: UpsertVendorBody, role: ContactRole, patch: Partial<{ name: string; email: string; mobile: string }>): UpsertVendorBody {
  const prev = contactOf(form, role)
  const rest = (form.contacts ?? []).filter((c) => c.role !== role)
  const next = { ...prev, ...patch, role }
  const ordered = role === 'PRIMARY_SPOC' ? [next, ...rest] : [...rest.filter((c) => c.role === 'PRIMARY_SPOC'), next, ...rest.filter((c) => c.role !== 'PRIMARY_SPOC')]
  return { ...form, contacts: ordered }
}

/** Excel Vendor Profile mandatory blanks for the current form (client-side gate). */
function formMissingProfileFields(form: UpsertVendorBody): string[] {
  const missing: string[] = []
  const spoc = contactOf(form, 'PRIMARY_SPOC')
  if (!form.name.trim()) missing.push('Vendor legal name')
  if (!form.vendorType) missing.push('Vendor type')
  if (!spoc.name?.trim()) missing.push('Vendor primary SPOC - name')
  if (emailError(spoc.email, true)) missing.push('Vendor primary SPOC - email')
  if (!parseStoredMobile(spoc.mobile).local) missing.push('Vendor primary SPOC - mobile')
  else if (phoneError(spoc.mobile, true)) missing.push('Vendor primary SPOC - mobile')
  if (!form.servicesInScope?.trim()) missing.push('Services in scope')
  if (!form.projectDescription?.trim()) missing.push('Project / service description')
  if (!form.orgOwner?.trim()) missing.push('Organization Project Owner')
  if (!form.contractCode?.trim()) missing.push('Contract reference / Contract Code')
  if (!form.contractStart || !form.contractEnd) missing.push('Contract start and end date')
  if (!form.f1) missing.push('Highest Organization information classification exposed')
  if (!form.f2) missing.push('Level of access to Organization systems')
  if (!form.f3) missing.push('Business criticality of the service')
  if (!form.f4) missing.push('Delivery environment - whose assets, and where')
  if (!form.f5) missing.push('Personal data involved')
  if (!form.f6) missing.push('Concentration and substitutability')
  const certs = (form.certs ?? []).filter((c) => c.certType)
  if (certs.length === 0) {
    missing.push('Independent certification held')
  } else if (certs.some((c) => c.certType !== 'NONE' && !c.expiryDate)) {
    missing.push('Certification expiry date')
  } else if (certs.some((c) => c.certType === 'OTHER' && !(c.otherLabel ?? '').trim())) {
    missing.push('Others certification name')
  }
  return missing
}

function fieldErrorFor(label: string, missing: string[], show: boolean): string {
  if (!show) return ''
  return missing.includes(label) ? `${label} is required` : ''
}

function emptyBody(): UpsertVendorBody {
  return {
    name: '',
    vendorType: 'CLOUD_SERVICE_PROVIDER',
    website: '',
    servicesInScope: '',
    projectDescription: '',
    orgOwner: '',
    contractCode: '',
    contractStart: '',
    contractEnd: '',
    f1: '',
    f2: '',
    f3: '',
    f4: '',
    f5: '',
    f6: '',
    overrideTier: '',
    overrideJustification: '',
    contacts: [blankContact('PRIMARY_SPOC')],
    certs: [{ certType: 'NONE', expiryDate: null }],
  }
}

function vendorToBody(v: TprmVendor): UpsertVendorBody {
  return {
    name: v.name,
    vendorType: v.vendorType,
    website: v.website ?? '',
    servicesInScope: v.servicesInScope ?? '',
    projectDescription: v.projectDescription ?? '',
    orgOwner: v.orgOwner ?? '',
    contractCode: v.contractCode ?? '',
    contractStart: v.contractStart ?? '',
    contractEnd: v.contractEnd ?? '',
    f1: v.f1 ?? '',
    f2: v.f2 ?? '',
    f3: v.f3 ?? '',
    f4: v.f4 ?? '',
    f5: v.f5 ?? '',
    f6: v.f6 ?? '',
    overrideTier: v.overrideTier ?? '',
    overrideJustification: v.overrideJustification ?? '',
    contacts: v.contacts.length
      ? v.contacts.map((c) => ({ role: c.role, name: c.name ?? '', email: c.email, mobile: c.mobile ?? '' }))
      : [blankContact('PRIMARY_SPOC')],
    certs: v.certs.length
      ? v.certs.map((c) => ({ certType: c.certType, expiryDate: c.expiryDate, otherLabel: c.otherLabel ?? '' }))
      : [{ certType: 'NONE', expiryDate: null }],
  }
}

function Section({ id, title, subtitle, action, heading, children }: {
  id: string
  title: string
  subtitle?: string
  action?: ReactNode
  heading?: boolean
  children: ReactNode
}) {
  const { mode, activeSection } = useFormSectionShell()
  if (mode === 'panels' && activeSection && activeSection !== id) {
    return null
  }
  return (
    <Box id={id} sx={{ ...sectionAnchorSx, mb: mode === 'panels' ? 0 : 1.25 }}>
      <SettingsCard compact heading={heading} title={title} subtitle={subtitle} action={action}>
        {children}
      </SettingsCard>
    </Box>
  )
}

export default function TprmPage({
  session,
  onError,
  onOk,
  onNavigate: _onNavigate,
}: {
  session: Session
  onError: (msg: string) => void
  onOk: (msg: string) => void
  /** Optional left-nav jump (kept for callers; Reports stay under left nav). */
  onNavigate?: (navId: string) => void
}) {
  const canAdmin = session.roles.some((r) => ['OWNER', 'ADMIN'].includes(r))
  const [lookups, setLookups] = useState<TprmLookups | null>(null)
  const [ownerUsers, setOwnerUsers] = useState<{ id: number; email: string; fullName: string }[]>([])
  const [kpis, setKpis] = useState<TprmKpis | null>(null)
  const [vendors, setVendors] = useState<TprmVendor[]>([])
  const [selected, setSelected] = useState<TprmVendor | null>(null)
  const [createOpen, setCreateOpen] = useState(false)
  const [form, setForm] = useState<UpsertVendorBody>(emptyBody())
  const [assessment, setAssessment] = useState<TprmAssessment | null>(null)
  const [history, setHistory] = useState<TprmAssessment[]>([])
  const [findings, setFindings] = useState<TprmFinding[]>([])
  const [busy, setBusy] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [issueOpen, setIssueOpen] = useState(false)
  const [issueEmail, setIssueEmail] = useState('')
  const [issueDue, setIssueDue] = useState('')
  const [lastIssued, setLastIssued] = useState<TprmAssessment | null>(null)
  const [showAdvanceErrors, setShowAdvanceErrors] = useState(false)
  const [confirmIssue, setConfirmIssue] = useState(false)
  const [confirmCompleteReview, setConfirmCompleteReview] = useState(false)
  const [confirmReturn, setConfirmReturn] = useState(false)
  const [confirmCloseFindingId, setConfirmCloseFindingId] = useState<number | null>(null)
  const [confirmCertIdx, setConfirmCertIdx] = useState<number | null>(null)
  const [categoriseAcknowledged, setCategoriseAcknowledged] = useState(false)
  const [reviewItemId, setReviewItemId] = useState<number | null>(null)
  const [reviewSnap, setReviewSnap] = useState<Record<number, TprmReviewFieldSnap> | null>(null)
  const [formBaseline, setFormBaseline] = useState('')
  const [vendorEditing, setVendorEditing] = useState(false)
  const [search, setSearch] = usePersistedState('reg-tprm-q', '')
  const { listReady, markReady } = useListReady()
  const [sortKey, setSortKey] = useState('name')
  const [sortDir, setSortDir] = useState<SortDir>('asc')
  const [typeFilter, setTypeFilter] = useState('')
  const [tierFilter, setTierFilter] = useState('')
  const [critFilter, setCritFilter] = useState('')
  const [compFilter, setCompFilter] = useState('')
  const [stateFilter, setStateFilter] = useState('')
  const vendorColumns = useRegisterColumnPicker(TPRM_REGISTER_COLUMNS)
  const fields = useFormFields<'spocEmail'>()
  const spoc = contactOf(form, 'PRIMARY_SPOC')
  const contributor = contactOf(form, 'CONTRIBUTOR')
  const spocErr = emailError(spoc.email, true)
  const spocMobileParts = parseStoredMobile(spoc.mobile)
  const spocMobileErr = phoneError(spoc.mobile, true)
  const creating = createOpen
  const { requestNavigation } = useOptionalUnsavedChangesGuardApi()
  const saveVendorRef = useRef<() => Promise<void>>(async () => {})

  const formMissing = useMemo(() => formMissingProfileFields(form), [form])
  const savedMissing = selected?.missingProfileFields ?? []
  const showMissingChecklist = !creating && selected != null && (formMissing.length > 0 || savedMissing.length > 0)
  const checklistItems = formMissing.length > 0 ? formMissing : savedMissing

  const formDirty =
    (createOpen || selected != null) &&
    formBaseline !== '' &&
    JSON.stringify(form) !== formBaseline

  useRegisterUnsavedChanges(
    createOpen || selected
      ? {
          dirty: formDirty,
          title: 'Leave without saving?',
          message:
            'You have unsaved vendor edits. Save to keep your work, or discard to leave without saving. Left-nav and Back use the same prompt.',
          onSaveDraft: () => saveVendorRef.current(),
          saveDraftLabel: creating ? 'Draft Vendor' : 'Save Changes',
        }
      : null,
  )

  const load = useCallback(async () => {
    try {
      const [l, k, list, directory] = await Promise.all([
        api.listTprmLookups(),
        api.listTprmKpis(),
        api.listTprmVendors(),
        api.listUserDirectory().catch(() => [] as { id: number; email: string; fullName: string }[]),
      ])
      setLookups(l)
      setKpis(k)
      setVendors(list)
      setOwnerUsers(directory)
    } catch (e) {
      onError(String(e))
    } finally {
      markReady()
    }
  }, [onError, markReady])

  useEffect(() => {
    void load()
  }, [load])

  const openVendor = async (v: TprmVendor) => {
    setCreateOpen(false)
    setSelected(v)
    const body = vendorToBody(v)
    setForm(body)
    setFormBaseline(JSON.stringify(body))
    setShowAdvanceErrors(false)
    setCategoriseAcknowledged(false)
    fields.reset()
    setVendorEditing(false)
    setDetailLoading(true)
    try {
      const [a, h, f] = await Promise.all([
        v.currentAssessmentId ? api.getTprmAssessment(v.currentAssessmentId) : Promise.resolve(null),
        api.listTprmAssessments(v.id),
        api.listTprmFindings(v.id),
      ])
      setAssessment(a)
      setReviewSnap(a ? buildTprmReviewSnap(a.items) : null)
      setHistory(h)
      setFindings(f)
      if (a?.accessToken && a?.otpCode) {
        setLastIssued(a)
      } else if (a?.portalInvites && a.portalInvites.length > 0) {
        const primary = a.portalInvites[0]
        setLastIssued({ ...a, accessToken: primary.accessToken, otpCode: primary.otpCode })
      } else {
        setLastIssued(null)
      }
    } catch (e) {
      onError(String(e))
    } finally {
      setDetailLoading(false)
    }
  }

  const performCloseVendor = useCallback(() => {
    setCreateOpen(false)
    setSelected(null)
    setAssessment(null)
    setReviewSnap(null)
    setHistory([])
    setFindings([])
    setForm(emptyBody())
    setFormBaseline('')
    setShowAdvanceErrors(false)
    fields.reset()
    setVendorEditing(false)
    void load()
  }, [fields.reset, load])

  const closeVendor = useCallback(() => {
    requestNavigation(performCloseVendor)
  }, [requestNavigation, performCloseVendor])

  const startCreate = useCallback(() => {
    const body = emptyBody()
    setSelected(null)
    setCreateOpen(true)
    setForm(body)
    setFormBaseline(JSON.stringify(body))
    setShowAdvanceErrors(false)
    setAssessment(null)
    setReviewSnap(null)
    setHistory([])
    setFindings([])
    setCategoriseAcknowledged(false)
    fields.reset()
    setVendorEditing(true)
  }, [fields.reset])

  const saveVendor = async () => {
    fields.touchAll()
    if (!form.name.trim()) {
      onError('Enter the vendor legal name.')
      document.getElementById('tprm-identity')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
      throw new Error('Vendor legal name is required')
    }
    if (spoc.email.trim() && spocErr) {
      onError(spocErr)
      document.getElementById('tprm-contacts')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
      throw new Error(spocErr)
    }
    if (parseStoredMobile(spoc.mobile).local && spocMobileErr) {
      onError(spocMobileErr)
      document.getElementById('tprm-contacts')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
      throw new Error(spocMobileErr)
    }
    setBusy(true)
    try {
      const body: UpsertVendorBody = {
        ...form,
        name: form.name.trim(),
        vendorType: form.vendorType || null,
        website: emptyToNull(form.website),
        servicesInScope: emptyToNull(form.servicesInScope),
        projectDescription: emptyToNull(form.projectDescription),
        orgOwner: emptyToNull(form.orgOwner),
        contractCode: emptyToNull(form.contractCode),
        contractStart: emptyToNull(form.contractStart),
        contractEnd: emptyToNull(form.contractEnd),
        f1: emptyToNull(form.f1),
        f2: emptyToNull(form.f2),
        f3: emptyToNull(form.f3),
        f4: emptyToNull(form.f4),
        f5: emptyToNull(form.f5),
        f6: emptyToNull(form.f6),
        overrideTier: emptyToNull(form.overrideTier),
        overrideJustification: emptyToNull(form.overrideJustification),
        contacts: (form.contacts ?? [])
          .filter((c) => c.email.trim())
          .map((c) => ({
            ...c,
            email: c.email.trim(),
            name: emptyToNull(c.name),
            mobile: storedMobileForApi(c.mobile),
          })),
        certs: (form.certs ?? [])
          .filter((c) => c.certType)
          .map((c) => ({
            certType: c.certType,
            expiryDate: emptyToNull(c.expiryDate),
            otherLabel: c.certType === 'OTHER' ? (c.otherLabel ?? null) : null,
          })),
      }
      if (selected?.id && selected.categorisationFrozen) {
        onError('Vendor details are locked after the project is assigned or closed.')
        return
      }
      const saved = selected?.id ? await api.updateTprmVendor(selected.id, body) : await api.createTprmVendor(body)
      onOk(selected?.id ? 'Vendor updated' : `Vendor "${saved.name}" created`)
      setFormBaseline(JSON.stringify(vendorToBody(saved)))
      setShowAdvanceErrors(false)
      setVendorEditing(false)
      await load()
      await openVendor(saved)
    } catch (e) {
      if (!fields.applyApiError(e)) onError(e instanceof ApiError ? e.message : String(e))
      throw e instanceof Error ? e : new Error(String(e))
    } finally {
      setBusy(false)
    }
  }
  saveVendorRef.current = () => saveVendor()

  const generate = async () => {
    if (!selected?.id) return
    if (assessment) {
      onError('Questionnaire already generated. Use Assign / Issue next.')
      return
    }
    const missing = formMissingProfileFields(form)
    if (missing.length > 0 || formDirty) {
      setShowAdvanceErrors(true)
      if (missing.length > 0) {
        onError(`Complete the vendor profile before generating (${missing.length} field${missing.length === 1 ? '' : 's'} remaining).`)
        document.getElementById('tprm-identity')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
        return
      }
      if (formDirty) {
        onError('Save vendor changes before generating the questionnaire.')
        return
      }
    }
    setBusy(true)
    try {
      const a = await api.generateTprmQuestionnaire(selected.id)
      setAssessment(a)
      setReviewSnap(buildTprmReviewSnap(a.items))
      onOk(`Questionnaire generated (${a.items.length} controls)`)
      await openVendor(await api.getTprmVendor(selected.id))
    } catch (e) {
      onError(e instanceof ApiError ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  const issue = async () => {
    if (!selected?.id) return
    setBusy(true)
    try {
      const a = await api.issueTprmAssessment(selected.id, {
        spocEmail: issueEmail || null,
        dueDate: issueDue || null,
        expiryDays: 14,
      })
      setLastIssued(a)
      setIssueOpen(false)
      setConfirmIssue(false)
      setAssessment(a)
      setReviewSnap(buildTprmReviewSnap(a.items))
      onOk(`Issued ${a.displayId}`)
      await openVendor(await api.getTprmVendor(selected.id))
    } catch (e) {
      onError(e instanceof ApiError ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  const completeReview = async (complete: boolean, returnToVendor = false): Promise<boolean> => {
    if (!assessment) return false
    if (complete && returnToVendor) {
      onError('Choose either Return to vendor or Close project, not both')
      return false
    }
    if (complete) {
      const blocker = reviewCompleteMissing(assessment)
      if (blocker) {
        onError(blocker)
        return false
      }
    }
    if (returnToVendor && !assessment.items.some((it) => it.reviewVerdict === 'RETURNED')) {
      onError('Mark at least one control as Returned before returning to the vendor')
      return false
    }
    const reviewedItems = assessment.items
      .filter((it) => it.reviewVerdict != null)
      .map((it) => ({
        itemId: it.id,
        verdict: it.reviewVerdict as TprmVerdict,
        comment: it.reviewComment,
      }))
    if (reviewedItems.length === 0) {
      onError('Set a verdict on at least one control before saving')
      return false
    }
    if (reviewedItems.some((it) => it.verdict === 'DOWNGRADED')) {
      onError('Downgraded is no longer a review verdict')
      return false
    }
    if (reviewedItems.some((it) => it.verdict === 'ACCEPTED_WITH_OBSERVATION' && !(it.comment ?? '').trim())) {
      onError('Review comment is required for Accepted with observation')
      return false
    }
    setBusy(true)
    try {
      const a = await api.reviewTprmAssessment(assessment.id, {
        complete,
        returnToVendor,
        items: reviewedItems,
      })
      setAssessment(a)
      setReviewSnap(buildTprmReviewSnap(a.items))
      setConfirmCompleteReview(false)
      setConfirmReturn(false)
      onOk(
        complete
          ? 'Review complete'
          : returnToVendor
            ? 'Returned controls sent to the vendor'
            : 'Review saved',
      )
      if (selected?.id) await openVendor(await api.getTprmVendor(selected.id))
      return true
    } catch (e) {
      onError(e instanceof ApiError ? e.message : String(e))
      return false
    } finally {
      setBusy(false)
    }
  }

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    const next = vendors.filter((v) => {
      if (q && !`${v.name} ${v.vendorType ?? ''}`.toLowerCase().includes(q)) return false
      if (typeFilter && v.vendorType !== typeFilter) return false
      if (tierFilter && v.tier !== tierFilter) return false
      if (critFilter && v.criticality !== critFilter) return false
      if (compFilter && v.complianceStatus !== compFilter) return false
      if (stateFilter && v.lifecycleState !== stateFilter) return false
      return true
    })
    return [...next].sort((a, b) => compareLoose(a.name, b.name, sortDir))
  }, [vendors, search, typeFilter, tierFilter, critFilter, compFilter, stateFilter, sortDir])
  const paged = usePagedRows(filtered)

  const profileEditable = creating || vendorEditing
  const saveWhy = !form.name.trim()
    ? 'Enter the vendor legal name'
    : formMissing.length > 0
      ? `${formMissing[0]} is required`
      : undefined

  const formActions = useMemo(
    () =>
      canAdmin ? (
        <>
          <Button variant="outlined" size="small" disabled={busy} onClick={closeVendor}>
            {profileEditable ? 'Cancel' : 'Back'}
          </Button>
          {!creating && selected && canAdmin && !vendorEditing && (
            <BusyButton
              size="small"
              variant="outlined"
              disabled={busy || Boolean(selected.categorisationFrozen)}
              disabledReason={
                selected.categorisationFrozen
                  ? 'Locked after the project is assigned or closed'
                  : undefined
              }
              onClick={() => setVendorEditing(true)}
            >
              Edit
            </BusyButton>
          )}
          {profileEditable && (
            <BusyButton
              size="small"
              variant="contained"
              busy={busy}
              disabled={Boolean(saveWhy)}
              disabledReason={saveWhy}
              onClick={() => { void saveVendorRef.current().catch(() => undefined) }}
            >
              {creating ? 'Create Vendor' : 'Save Changes'}
            </BusyButton>
          )}
        </>
      ) : (
        <Button variant="outlined" size="small" onClick={closeVendor}>
          Back
        </Button>
      ),
    [busy, canAdmin, closeVendor, creating, form.name, profileEditable, vendorEditing, selected, saveWhy],
  )

  const listActions = useMemo(
    () =>
      canAdmin ? (
        <Button variant="contained" size="small" onClick={() => requestNavigation(startCreate)}>
          New Vendor
        </Button>
      ) : undefined,
    [canAdmin, startCreate, requestNavigation],
  )

  useAppShellPage(
    (createOpen || selected)
      ? {
          title: creating ? 'New Vendor' : selected!.name,
          breadcrumbTrail: creating ? 'New Vendor' : selected!.name,
          breadcrumbRoot: { label: 'Third-Party Vendor Inventory', onClick: closeVendor },
          actions: formActions,
          hideContextLabel: true,
          hideRoleChips: true,
        }
      : {
          title: 'Third-Party Vendor Inventory',
          breadcrumbTrail: 'TPRM',
          actions: listActions,
          hideContextLabel: true,
          hideRoleChips: true,
        },
    [createOpen, selected, creating, formActions, listActions, closeVendor],
  )

  const typeOptions = (lookups?.vendorTypes ?? []).map((t) => ({ value: t.value, label: t.label }))
  const orgOwnerOptions = useMemo(() => {
    const names = ownerUsers.map((u) => u.fullName)
    const current = form.orgOwner?.trim() ?? ''
    const extra = current && !names.some((n) => n.toLowerCase() === current.toLowerCase())
      && !ownerUsers.some((u) => u.email.toLowerCase() === current.toLowerCase())
      ? [current]
      : []
    return [
      { value: '', label: ownerUsers.length === 0 ? 'No users' : 'Select' },
      ...ownerUsers.map((u) => ({ value: u.fullName, label: u.fullName })),
      ...extra.map((name) => ({ value: name, label: name })),
    ]
  }, [ownerUsers, form.orgOwner])
  const factorOpts = (key: FactorKey) => {
    const raw = lookups?.factors?.[key] ?? Object.keys(FACTOR_SCORE_TABLE[key])
    const ordered = [...raw].sort((a, b) => (FACTOR_SCORE_TABLE[key][b] ?? 0) - (FACTOR_SCORE_TABLE[key][a] ?? 0))
    return [{ value: '', label: 'Select' }, ...ordered.map((v) => ({ value: v, label: v }))]
  }
  const certOptions = (lookups?.certs ?? []).map((c) => ({ value: c.value, label: c.label }))

  const count = (pred: (v: TprmVendor) => boolean) => vendors.filter(pred).length

  const issueQuestionnaire = () => {
    const missing = formMissingProfileFields(form)
    if (missing.length > 0) {
      setShowAdvanceErrors(true)
      onError(`Complete the vendor profile before issuing (${missing.length} field${missing.length === 1 ? '' : 's'} remaining).`)
      return
    }
    if (formDirty) {
      onError('Save vendor changes before issuing the questionnaire.')
      return
    }
    setIssueEmail(spoc.email)
    setIssueOpen(true)
  }
  const setItem = (itemId: number, patch: Partial<TprmAssessmentItem>) => {
    if (!assessment) return
    setAssessment({
      ...assessment,
      items: assessment.items.map((row) => (row.id === itemId ? { ...row, ...patch } : row)),
    })
  }

  const discardReviewEdits = () => {
    if (!assessment || !reviewSnap) return
    setAssessment({
      ...assessment,
      items: assessment.items.map((it) => {
        const s = reviewSnap[it.id]
        if (!s) return it
        return { ...it, reviewVerdict: s.verdict, reviewComment: s.comment }
      }),
    })
  }

  const reviewItem = assessment?.items.find((it) => it.id === reviewItemId) ?? null
  const reviewItemIndex = reviewItem && assessment
    ? assessment.items.findIndex((it) => it.id === reviewItem.id)
    : -1
  const reviewEditable =
    canAdmin && assessment != null && (assessment.status === 'SUBMITTED' || assessment.status === 'IN_REVIEW')
  const reviewDirty = Boolean(reviewEditable && assessment && isTprmReviewDirty(assessment.items, reviewSnap))
  const categorisationFrozen = Boolean(selected?.categorisationFrozen)
  const categorisationFrozenWhy = 'Locked after the project is assigned or closed'

  const register = (
    <Stack spacing={1} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
      {!listReady && (
        <LinearProgress
          sx={{
            flexShrink: 0,
            height: 3,
            borderRadius: 0,
            bgcolor: brand.accentMuted,
            '& .MuiLinearProgress-bar': { bgcolor: brand.gold },
          }}
        />
      )}
      {kpis && (
        <KpiCardGrid columns={5}>
          <KpiCard
            compact
            label="Total vendors"
            value={String(kpis.total)}
            accentColor={brand.gold}
          />
          <KpiCard
            compact
            label="Tier 1"
            value={String(kpis.tier1 ?? count((v) => v.tier === 'TIER_1'))}
            accentColor={semantic.risk.critical}
            onClick={() => setTierFilter(tierFilter === 'TIER_1' ? '' : 'TIER_1')}
          />
          <KpiCard
            compact
            label="Tier 2"
            value={String(kpis.tier2 ?? count((v) => v.tier === 'TIER_2'))}
            accentColor={semantic.risk.high}
            onClick={() => setTierFilter(tierFilter === 'TIER_2' ? '' : 'TIER_2')}
          />
          <KpiCard
            compact
            label="Tier 3"
            value={String(kpis.tier3 ?? count((v) => v.tier === 'TIER_3'))}
            accentColor={brand.gold}
            onClick={() => setTierFilter(tierFilter === 'TIER_3' ? '' : 'TIER_3')}
          />
          <KpiCard
            compact
            label="Tier 4"
            value={String(kpis.tier4 ?? count((v) => v.tier === 'TIER_4'))}
            accentColor={semantic.risk.low}
            onClick={() => setTierFilter(tierFilter === 'TIER_4' ? '' : 'TIER_4')}
          />
        </KpiCardGrid>
      )}
      <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
        <RegisterToolbar
          search={search}
          searchPlaceholder="Search vendors"
          onSearchChange={setSearch}
          filters={[
            {
              id: 'type',
              label: 'Type',
              value: typeFilter,
              options: [{ value: '', label: `All types (${vendors.length})` }, ...typeOptions],
              onChange: setTypeFilter,
              minWidth: 118,
            },
            {
              id: 'tier',
              label: 'Tier',
              value: tierFilter,
              options: [
                { value: '', label: 'All tiers' },
                { value: 'TIER_1', label: `T1 (${count((v) => v.tier === 'TIER_1')})` },
                { value: 'TIER_2', label: `T2 (${count((v) => v.tier === 'TIER_2')})` },
                { value: 'TIER_3', label: `T3 (${count((v) => v.tier === 'TIER_3')})` },
                { value: 'TIER_4', label: `T4 (${count((v) => v.tier === 'TIER_4')})` },
              ],
              onChange: setTierFilter,
              minWidth: 100,
            },
            {
              id: 'crit',
              label: 'Criticality',
              value: critFilter,
              options: [
                { value: '', label: 'All' },
                { value: 'CRITICAL', label: `Critical (${count((v) => v.criticality === 'CRITICAL')})` },
                { value: 'NON_CRITICAL', label: `Non-critical (${count((v) => v.criticality === 'NON_CRITICAL')})` },
              ],
              onChange: setCritFilter,
              minWidth: 118,
            },
            {
              id: 'comp',
              label: 'Compliance',
              value: compFilter,
              options: [
                { value: '', label: 'All' },
                { value: 'COMPLIANT', label: `Compliant (${count((v) => v.complianceStatus === 'COMPLIANT')})` },
                { value: 'NOT_COMPLIANT', label: `Not compliant (${count((v) => v.complianceStatus === 'NOT_COMPLIANT')})` },
                { value: 'UNKNOWN', label: `Not reviewed (${count((v) => v.complianceStatus === 'UNKNOWN')})` },
              ],
              onChange: setCompFilter,
              minWidth: 118,
            },
            {
              id: 'state',
              label: 'State',
              value: stateFilter,
              options: [
                { value: '', label: 'All states' },
                { value: 'ISSUED', label: 'Issued' },
                { value: 'SUBMITTED', label: 'Submitted' },
                { value: 'IN_REVIEW', label: 'In review' },
                { value: 'COMPLETE', label: 'Complete' },
              ],
              onChange: setStateFilter,
              minWidth: 110,
            },
          ]}
          onClearFilters={() => {
            setSearch('')
            setTypeFilter('')
            setTierFilter('')
            setCritFilter('')
            setCompFilter('')
            setStateFilter('')
          }}
          columnPicker={vendorColumns.columnPicker}
        />
        <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
        <RegisterListFrame
          ready={listReady}
          count={paged.count}
          emptyMessage={
            vendors.length === 0
              ? 'No vendors yet. Create a vendor to start third-party risk.'
              : 'No vendors match these filters.'
          }
          emptyAction={
            vendors.length === 0 && canAdmin ? (
              <Button size="small" variant="contained" onClick={() => requestNavigation(startCreate)}>
                New Vendor
              </Button>
            ) : undefined
          }
          page={paged.page}
          rowsPerPage={paged.rowsPerPage}
          onPageChange={paged.setPage}
          onRowsPerPageChange={paged.setRowsPerPage}
        >
        <TableContainer sx={{ flex: 1, minWidth: 0, overflowX: 'auto' }}>
          <Table size="small" stickyHeader sx={{ ...registerTableSx, minWidth: 920, tableLayout: 'fixed' }}>
            <TableHead>
              <TableRow>
                {vendorColumns.isVisible('name') && (
                  <RegisterSortHeader
                    label="Vendor"
                    active={sortKey === 'name'}
                    dir={sortDir}
                    width={tprmCell.name.width}
                    onClick={() => {
                      const next = toggleSort(sortKey, sortDir, 'name')
                      setSortKey(next.key)
                      setSortDir(next.dir)
                    }}
                  />
                )}
                {vendorColumns.isVisible('type') && <TableCell sx={tprmCell.type}>Type</TableCell>}
                {vendorColumns.isVisible('inherent') && <TableCell sx={tprmCell.inherent}>Inherent</TableCell>}
                {vendorColumns.isVisible('tier') && <TableCell sx={tprmCell.tier}>Tier</TableCell>}
                {vendorColumns.isVisible('criticality') && <TableCell sx={tprmCell.criticality}>Criticality</TableCell>}
                {vendorColumns.isVisible('posture') && <TableCell sx={tprmCell.posture}>Posture</TableCell>}
                {vendorColumns.isVisible('compliance') && <TableCell sx={tprmCell.compliance}>Compliance</TableCell>}
                {vendorColumns.isVisible('state') && <TableCell sx={tprmCell.state}>State</TableCell>}
                {vendorColumns.isVisible('nextReview') && <TableCell sx={tprmCell.nextReview}>Next review</TableCell>}
              </TableRow>
            </TableHead>
            <TableBody>
              {paged.paged.map((v) => (
                <TableRow key={v.id} hover onClick={() => void openVendor(v)} sx={{ cursor: 'pointer' }}>
                  {vendorColumns.isVisible('name') && (
                    <TableCell sx={tprmCell.name}>
                      <Typography noWrap sx={primaryRegisterNameSx}>{v.name}</Typography>
                    </TableCell>
                  )}
                  {vendorColumns.isVisible('type') && (
                    <TableCell sx={tprmCell.type}>
                      <Typography variant="caption" noWrap>
                        {lookups?.vendorTypes.find((t) => t.value === v.vendorType)?.label ?? v.vendorType ?? '-'}
                      </Typography>
                    </TableCell>
                  )}
                  {vendorColumns.isVisible('inherent') && (
                    <TableCell sx={tprmCell.inherent}>{v.inherentScore ?? '-'}</TableCell>
                  )}
                  {vendorColumns.isVisible('tier') && (
                    <TableCell sx={tprmCell.tier}>{tierLabel(v.tier)}</TableCell>
                  )}
                  {vendorColumns.isVisible('criticality') && (
                    <TableCell sx={tprmCell.criticality}>
                      {v.criticality ? (
                        <RiskLevelBadge
                          level={vendorCriticalityToBadge(v.criticality)}
                          label={v.criticality === 'CRITICAL' ? 'Critical' : 'Non-critical'}
                        />
                      ) : '-'}
                    </TableCell>
                  )}
                  {vendorColumns.isVisible('posture') && (
                    <TableCell sx={tprmCell.posture}>{pct(v.posturePercent)}</TableCell>
                  )}
                  {vendorColumns.isVisible('compliance') && (
                    <TableCell sx={tprmCell.compliance}>
                      <StatusChip
                        label={COMPLIANCE_LABEL[v.complianceStatus]}
                        tone={v.complianceStatus === 'COMPLIANT' ? 'success' : v.complianceStatus === 'NOT_COMPLIANT' ? 'danger' : 'neutral'}
                        title={COMPLIANCE_LABEL[v.complianceStatus]}
                        sx={{ maxWidth: 'none', '& .MuiChip-label': { overflow: 'visible', textOverflow: 'clip' } }}
                      />
                    </TableCell>
                  )}
                  {vendorColumns.isVisible('state') && (
                    <TableCell sx={tprmCell.state}>
                      <StatusChip
                        label={LIFECYCLE_LABEL[v.lifecycleState] ?? pretty(v.lifecycleState)}
                        tone={lifecycleTone(v.lifecycleState)}
                      />
                    </TableCell>
                  )}
                  {vendorColumns.isVisible('nextReview') && (
                    <TableCell sx={tprmCell.nextReview}>{v.nextReviewDate ?? '-'}</TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
        </RegisterListFrame>
        </Box>
      </Card>
    </Stack>
  )

  const workspace = (
    <Stack sx={{ flex: 1, minHeight: 0, height: '100%' }}>
    {(busy || detailLoading) && (
      <LinearProgress
        sx={{
          flexShrink: 0,
          height: 3,
          borderRadius: 0,
          bgcolor: brand.accentMuted,
          '& .MuiLinearProgress-bar': { bgcolor: brand.gold },
        }}
      />
    )}
    <Stack
      direction={{ xs: 'column', md: 'row' }}
      spacing={1.25}
      sx={{ flex: 1, minHeight: 0, alignItems: 'stretch' }}
    >
      <Stack sx={{ flex: 1, minWidth: 0, minHeight: 0 }}>
    <FormSectionShell
      sections={CREATE_SECTIONS}
      mode="scroll"
    >
      <Box sx={{
        ...complexFormContainerSx,
        ...(!profileEditable ? { pointerEvents: 'none' } : {}),
      }}>
        {categorisationFrozen && !creating && (
          <Alert severity="info" sx={{ mb: 1.25 }}>
            Vendor details are locked after the project is assigned or closed.
          </Alert>
        )}
        {(showMissingChecklist || (showAdvanceErrors && formMissing.length > 0)) && (
          <Alert severity="warning" sx={{ mb: 1.25 }}>
            Profile incomplete. Still needed: {(showAdvanceErrors ? formMissing : checklistItems).join('; ')}.
          </Alert>
        )}
        <Section id="tprm-identity" title="Identity" subtitle="Who the third party is on the contract">
          <Stack spacing={1}>
          <Box sx={formFieldGridSx}>
            <CompactTextField
              label="Vendor legal name"
              required
              fieldIdPrefix="tprm"
              maxLength={INPUT_LIMIT.orgName}
              value={form.name}
              error={Boolean(fieldErrorFor('Vendor legal name', formMissing, showAdvanceErrors))}
              helperText={fieldErrorFor('Vendor legal name', formMissing, showAdvanceErrors)}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
            />
            <FormSelect
              label="Vendor type"
              required
              value={form.vendorType ?? ''}
              options={typeOptions}
              onChange={(v) => setForm({ ...form, vendorType: v as VendorType })}
              minWidth={220}
              disabled={categorisationFrozen}
              error={Boolean(fieldErrorFor('Vendor type', formMissing, showAdvanceErrors))}
              helperText={
                categorisationFrozen
                  ? categorisationFrozenWhy
                  : fieldErrorFor('Vendor type', formMissing, showAdvanceErrors)
              }
            />
            <CompactTextField
              label="Website (optional)"
              fieldIdPrefix="tprm"
              value={form.website ?? ''}
              onChange={(e) => setForm({ ...form, website: e.target.value })}
            />
          </Box>
          </Stack>
        </Section>

        <Section id="tprm-engagement" title="Engagement" subtitle="Services, internal owner, and contract dates">
          <Stack spacing={1}>
          <Box sx={twoColFieldSx}>
            <CompactTextField
              label="Services in scope"
              required
              fieldIdPrefix="tprm"
              maxLength={1000}
              value={form.servicesInScope ?? ''}
              error={Boolean(fieldErrorFor('Services in scope', formMissing, showAdvanceErrors))}
              helperText={fieldErrorFor('Services in scope', formMissing, showAdvanceErrors)}
              onChange={(e) => setForm({ ...form, servicesInScope: e.target.value })}
              multiline
              minRows={2}
              maxRows={NARRATIVE_MAX_ROWS}
              maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
            />
            <CompactTextField
              label="Project / service description"
              required
              fieldIdPrefix="tprm"
              maxLength={2000}
              value={form.projectDescription ?? ''}
              error={Boolean(fieldErrorFor('Project / service description', formMissing, showAdvanceErrors))}
              helperText={fieldErrorFor('Project / service description', formMissing, showAdvanceErrors)}
              onChange={(e) => setForm({ ...form, projectDescription: e.target.value })}
              multiline
              minRows={2}
              maxRows={NARRATIVE_MAX_ROWS}
              maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
            />
          </Box>
          <FieldActionRow sx={{ alignItems: { xs: 'stretch', sm: 'flex-start' } }}>
            <Box sx={{ flex: '1.6 1 0', minWidth: 0 }}>
              <FormSelect
                label="Organization Project Owner"
                required
                value={form.orgOwner ?? ''}
                options={orgOwnerOptions}
                onChange={(v) => setForm({ ...form, orgOwner: v })}
                minWidth={220}
                error={Boolean(fieldErrorFor('Organization Project Owner', formMissing, showAdvanceErrors))}
                helperText={
                  ownerUsers.length === 0
                    ? 'Add a user first'
                    : fieldErrorFor('Organization Project Owner', formMissing, showAdvanceErrors)
                }
              />
            </Box>
            <Box sx={{ flex: '1 1 0', minWidth: 0 }}>
              <CompactTextField
                label="Contract reference / Contract Code"
                required
                wrapLabel
                fieldIdPrefix="tprm"
                maxLength={60}
                value={form.contractCode ?? ''}
                maxWidth="100%"
                error={Boolean(fieldErrorFor('Contract reference / Contract Code', formMissing, showAdvanceErrors))}
                helperText={fieldErrorFor('Contract reference / Contract Code', formMissing, showAdvanceErrors)}
                onChange={(e) => setForm({ ...form, contractCode: e.target.value })}
              />
            </Box>
            <CompactTextField
              label="Contract start"
              required
              type="date"
              fieldIdPrefix="tprm"
              value={form.contractStart ?? ''}
              error={Boolean(fieldErrorFor('Contract start and end date', formMissing, showAdvanceErrors) && !form.contractStart)}
              helperText={fieldErrorFor('Contract start and end date', formMissing, showAdvanceErrors) && !form.contractStart ? 'Contract start is required' : '\u00a0'}
              onChange={(e) => setForm({ ...form, contractStart: e.target.value })}
              InputLabelProps={{ shrink: true }}
            />
            <CompactTextField
              label="Contract end"
              required
              type="date"
              fieldIdPrefix="tprm"
              value={form.contractEnd ?? ''}
              error={Boolean(fieldErrorFor('Contract start and end date', formMissing, showAdvanceErrors) && !form.contractEnd)}
              helperText={fieldErrorFor('Contract start and end date', formMissing, showAdvanceErrors) && !form.contractEnd ? 'Contract end is required' : '\u00a0'}
              onChange={(e) => setForm({ ...form, contractEnd: e.target.value })}
              InputLabelProps={{ shrink: true }}
            />
          </FieldActionRow>
          </Stack>
        </Section>

        <Section id="tprm-contacts" title="Contact" subtitle="Excel Vendor Profile. Vendor primary SPOC is mandatory. One Additional vendor contributor is optional.">
          <Box sx={formFieldGridSx}>
            <CompactTextField
              label="Vendor primary SPOC - name"
              required
              fieldIdPrefix="tprm"
              value={spoc.name ?? ''}
              error={Boolean(fieldErrorFor('Vendor primary SPOC - name', formMissing, showAdvanceErrors))}
              helperText={fieldErrorFor('Vendor primary SPOC - name', formMissing, showAdvanceErrors)}
              onChange={(e) => setForm(setContact(form, 'PRIMARY_SPOC', { name: e.target.value }))}
            />
            <CompactTextField
              label="Vendor primary SPOC - email"
              type="email"
              required
              fieldIdPrefix="tprm"
              value={spoc.email}
              error={Boolean((spocErr && fields.submitted) || fieldErrorFor('Vendor primary SPOC - email', formMissing, showAdvanceErrors))}
              helperText={fields.submitted && spocErr ? spocErr : fieldErrorFor('Vendor primary SPOC - email', formMissing, showAdvanceErrors)}
              onChange={(e) => setForm(setContact(form, 'PRIMARY_SPOC', { email: e.target.value }))}
              onBlur={() => fields.touch('spocEmail')}
            />
            <Box sx={{ gridColumn: '1 / -1' }}>
              <FieldActionRow sx={{ alignItems: { xs: 'stretch', sm: 'flex-start' } }}>
                <FormSelect
                  label="Country code"
                  required
                  value={spocMobileParts.dial}
                  options={PHONE_COUNTRY_OPTIONS.map((o) => ({ value: o.dial, label: o.label }))}
                  minWidth={200}
                  maxWidth={248}
                  error={Boolean(
                    (parseStoredMobile(spoc.mobile).local && spocMobileErr)
                    || fieldErrorFor('Vendor primary SPOC - mobile', formMissing, showAdvanceErrors),
                  )}
                  helperText=" "
                  onChange={(dial) => {
                    setForm(setContact(form, 'PRIMARY_SPOC', {
                      mobile: formatMobile(dial as PhoneDial, spocMobileParts.local),
                    }))
                  }}
                />
                <CompactTextField
                  label="Vendor primary SPOC - mobile"
                  required
                  fieldIdPrefix="tprm"
                  value={spocMobileParts.local}
                  placeholder={nationalPlaceholder(spocMobileParts.dial)}
                  error={Boolean(
                    (parseStoredMobile(spoc.mobile).local && spocMobileErr)
                    || fieldErrorFor('Vendor primary SPOC - mobile', formMissing, showAdvanceErrors),
                  )}
                  helperText={
                    parseStoredMobile(spoc.mobile).local && spocMobileErr
                      ? spocMobileErr
                      : fieldErrorFor('Vendor primary SPOC - mobile', formMissing, showAdvanceErrors)
                        || phoneDigitHint(spocMobileParts.dial)
                  }
                  onChange={(e) => {
                    const local = e.target.value.replace(/\D/g, '').slice(0, nationalLength(spocMobileParts.dial))
                    setForm(setContact(form, 'PRIMARY_SPOC', {
                      mobile: formatMobile(spocMobileParts.dial, local),
                    }))
                  }}
                  slotProps={{
                    htmlInput: {
                      inputMode: 'numeric',
                      autoComplete: 'tel-national',
                      maxLength: nationalLength(spocMobileParts.dial),
                      pattern: '[0-9]*',
                      'aria-label': `Vendor primary SPOC mobile number, ${nationalLength(spocMobileParts.dial)} digits`,
                    },
                  }}
                  sx={{ minWidth: 180, maxWidth: 220 }}
                />
              </FieldActionRow>
            </Box>
            <CompactTextField
              label="Additional vendor contributors - name"
              fieldIdPrefix="tprm"
              value={contributor.name ?? ''}
              onChange={(e) => setForm(setContact(form, 'CONTRIBUTOR', { name: e.target.value }))}
            />
            <CompactTextField
              label="Additional vendor contributors - email"
              type="email"
              fieldIdPrefix="tprm"
              value={contributor.email}
              onChange={(e) => setForm(setContact(form, 'CONTRIBUTOR', { email: e.target.value }))}
            />
          </Box>
        </Section>

        <Section
          id="tprm-categorisation"
          title="Risk Profile"
          subtitle={
            categorisationFrozen
              ? `Six Excel factors. ${categorisationFrozenWhy}.`
              : 'Six Excel factors. Score is calculated from the Lookups sheet (1-4), not typed. The vendor never sees scores, tier, or criticality.'
          }
        >
          {selected?.id && selected.lifecycleState === 'CATEGORISED' && (
            <Alert severity="success" sx={{ mb: 1.5 }}>
              Categorisation is complete
              {selected.tier ? `: ${tierLabel(selected.tier)}` : ''}
              {selected.criticality ? ` · ${pretty(selected.criticality)}` : ''}
              {selected.adjustedScore != null ? ` · adjusted score ${selected.adjustedScore}` : ''}
              .
            </Alert>
          )}
          {selected?.id && selected.elevationFlag && (
            <Alert severity="warning" sx={{ mb: 1.5 }}>
              Information exposure or system access scored 4 while the calculated tier is T3 or T4.
              A documented one-level raise is allowed.
            </Alert>
          )}
          {selected?.id && (
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: { xs: '1fr 1fr', md: 'repeat(4, minmax(0, 1fr))' },
                gap: 1,
                mb: 1.5,
              }}
            >
              <KpiCard
                compact
                label="Inherent"
                value={String(selected.inherentScore ?? '-')}
                sub="Sum of six factors (6-24)"
                accentColor={brand.ink}
              />
              <KpiCard
                compact
                label="Adjusted"
                value={String(selected.adjustedScore ?? '-')}
                sub={selected.assuranceCredit
                  ? `Assurance credit -${selected.assuranceCredit}`
                  : 'No assurance credit'}
                accentColor={brand.ink}
              />
              <KpiCard
                compact
                label="Tier"
                value={tierLabel(selected.tier)}
                sub={selected.criticality === 'CRITICAL' ? 'Critical' : selected.criticality === 'NON_CRITICAL' ? 'Non-critical' : undefined}
                accentColor={selected.criticality === 'CRITICAL' ? semantic.risk.critical : semantic.risk.low}
              />
              <KpiCard
                compact
                label="Posture target"
                value={pct(selected.postureTarget)}
                sub={selected.reassessmentMonths ? `Reassess every ${selected.reassessmentMonths} months` : undefined}
                accentColor={brand.gold}
              />
            </Box>
          )}
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))', lg: 'repeat(3, minmax(0, 1fr))' },
              gap: 1,
            }}
          >
            {FACTOR_KEYS.map((key) => {
              const excelLabel =
                key === 'f1' ? 'Highest Organization information classification exposed'
                  : key === 'f2' ? 'Level of access to Organization systems'
                    : key === 'f3' ? 'Business criticality of the service'
                      : key === 'f4' ? 'Delivery environment - whose assets, and where'
                        : key === 'f5' ? 'Personal data involved'
                          : 'Concentration and substitutability'
              const factorValue = (form[key] as string) ?? ''
              const score = factorScoreValue(key, factorValue, selected?.[FACTOR_SCORE_KEY[key]] ?? null)
              const tone = factorScoreTone(score)
              const toneColor = tone ? RATING_TONE_COLOR[tone] : brand.muted
              return (
              <FieldActionRow key={key} direction="row" spacing={0.5} sx={{ minWidth: 0 }}>
                <Box sx={{ flex: 1, minWidth: 0 }} title={factorValue || undefined}>
                  <FormSelect
                    label={FACTOR_LABEL[key]}
                    required
                    wrapLabel
                    fullWidth
                    value={factorValue}
                    options={factorOpts(key)}
                    onChange={(v) => setForm({ ...form, [key]: v })}
                    disabled={categorisationFrozen}
                    error={Boolean(fieldErrorFor(excelLabel, formMissing, showAdvanceErrors))}
                    helperText={fieldErrorFor(excelLabel, formMissing, showAdvanceErrors)}
                  />
                </Box>
                <Box sx={{ width: 36, flexShrink: 0 }}>
                  <Typography
                    component="div"
                    sx={{
                      fontSize: '0.625rem',
                      fontWeight: 650,
                      color: brand.muted,
                      mb: 0.35,
                      letterSpacing: '0.04em',
                      textAlign: 'center',
                    }}
                  >
                    SCORE
                  </Typography>
                  <Box
                    title={
                      score != null
                        ? `Lookups score ${score} (1-4). Higher means more risk.`
                        : 'From Excel Lookups: option maps to 1-4'
                    }
                    sx={{
                      height: 32,
                      minHeight: 32,
                      width: 36,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      border: `1px solid ${tone ? `${toneColor}55` : brand.hairlineStrong}`,
                      borderRadius: 1,
                      bgcolor: tone ? `${toneColor}18` : brand.paper,
                      fontSize: '0.8125rem',
                      fontWeight: 700,
                      color: tone ? toneColor : brand.inkSoft,
                    }}
                  >
                    {score != null ? String(score) : '-'}
                  </Box>
                </Box>
              </FieldActionRow>
              )
            })}
          </Box>
          {canAdmin && selected?.id && selected.elevationFlag && selected.calculatedTier && (
            <Box sx={{ ...twoColFieldSx, mt: 1.5 }}>
              <FormSelect
                label="One-level override"
                value={form.overrideTier ?? ''}
                options={[
                  { value: '', label: 'None' },
                  {
                    value: nextTierUp(selected.calculatedTier),
                    label: `Raise one level to ${tierLabel(nextTierUp(selected.calculatedTier))}`,
                  },
                ]}
                onChange={(v) => setForm({ ...form, overrideTier: v })}
                minWidth={220}
                disabled={categorisationFrozen}
              />
              <CompactTextField
                label="Override justification"
                fieldIdPrefix="tprm"
                value={form.overrideJustification ?? ''}
                onChange={(e) => setForm({ ...form, overrideJustification: e.target.value })}
                multiline
                minRows={2}
                maxRows={NARRATIVE_MAX_ROWS}
                maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
                disabled={categorisationFrozen}
              />
            </Box>
          )}
        </Section>

        <Section
          id="tprm-certs"
          title="Assurance"
          subtitle={
            categorisationFrozen
              ? `Independent certification held. ${categorisationFrozenWhy}.`
              : 'Independent certification held. Distinct certs other than None count toward assurance credit (capped at 3).'
          }
        >
          <Stack spacing={1}>
            {(form.certs ?? []).length === 0 && showAdvanceErrors && (
              <Typography variant="caption" color="error">Independent certification held is required</Typography>
            )}
            {(form.certs ?? []).map((cert, idx) => (
              <FieldActionRow key={`${cert.certType}-${idx}`}>
                <FormSelect
                  label="Independent certification held"
                  required
                  wrapLabel
                  value={cert.certType}
                  options={certOptions}
                  onChange={(v) => {
                    const certs = [...(form.certs ?? [])]
                    const nextType = v as TprmCertType
                    certs[idx] = {
                      ...certs[idx],
                      certType: nextType,
                      expiryDate: nextType === 'NONE' ? null : certs[idx].expiryDate,
                      otherLabel: nextType === 'OTHER' ? (certs[idx].otherLabel ?? '') : '',
                    }
                    setForm({ ...form, certs: nextType === 'NONE' ? [{ certType: 'NONE' }] : certs.filter((c) => c.certType !== 'NONE') })
                  }}
                  minWidth={240}
                  disabled={categorisationFrozen}
                />
                {cert.certType === 'OTHER' && (
                  <CompactTextField
                    label="Others: certification name"
                    required
                    fieldIdPrefix={`tprm-cert-other-${idx}`}
                    value={cert.otherLabel ?? ''}
                    error={Boolean(!(cert.otherLabel ?? '').trim() && showAdvanceErrors)}
                    helperText={!(cert.otherLabel ?? '').trim() && showAdvanceErrors ? 'Others certification name is required' : ''}
                    onChange={(e) => {
                      const certs = [...(form.certs ?? [])]
                      certs[idx] = { ...certs[idx], otherLabel: e.target.value }
                      setForm({ ...form, certs })
                    }}
                    disabled={categorisationFrozen}
                    sx={{ minWidth: 180, maxWidth: 240 }}
                  />
                )}
                <CompactTextField
                  label="Expiry"
                  type="date"
                  required={cert.certType !== 'NONE'}
                  fieldIdPrefix={`tprm-cert-${idx}`}
                  value={cert.expiryDate ?? ''}
                  error={Boolean(cert.certType !== 'NONE' && !cert.expiryDate && showAdvanceErrors)}
                  helperText={cert.certType !== 'NONE' && !cert.expiryDate && showAdvanceErrors ? 'Certification expiry date is required' : ''}
                  onChange={(e) => {
                    const certs = [...(form.certs ?? [])]
                    certs[idx] = { ...certs[idx], expiryDate: e.target.value || null }
                    setForm({ ...form, certs })
                  }}
                  InputLabelProps={{ shrink: true }}
                  disabled={categorisationFrozen || cert.certType === 'NONE'}
                />
                {canAdmin ? (
                  <Button
                    size="small"
                    variant="outlined"
                    sx={controlActionSx}
                    disabled={!profileEditable || categorisationFrozen}
                    title={
                      !profileEditable
                        ? 'Open Edit to change certifications'
                        : categorisationFrozen
                          ? categorisationFrozenWhy
                          : undefined
                    }
                    onClick={() => setConfirmCertIdx(idx)}
                  >
                    Remove
                  </Button>
                ) : null}
              </FieldActionRow>
            ))}
            {canAdmin && (
              <Button
                size="small"
                variant="outlined"
                sx={{ alignSelf: 'flex-start', ...controlActionSx }}
                disabled={!profileEditable || categorisationFrozen}
                title={
                  !profileEditable
                    ? 'Open Edit to change certifications'
                    : categorisationFrozen
                      ? categorisationFrozenWhy
                      : undefined
                }
                onClick={() => setForm({
                  ...form,
                  certs: [...(form.certs ?? []).filter((c) => c.certType !== 'NONE'), { certType: 'ISO_27001', expiryDate: null }],
                })}
              >
                Add Certification
              </Button>
            )}
          </Stack>
        </Section>

      </Box>
    </FormSectionShell>
      </Stack>
    </Stack>
    </Stack>
  )

  return (
    <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
      {createOpen || selected ? workspace : register}
      {reviewItem && assessment && (
        <TprmReviewDrawer
          open
          item={reviewItem}
          index={reviewItemIndex}
          total={assessment.items.length}
          reviewable={
            assessment.status === 'SUBMITTED'
            || assessment.status === 'IN_REVIEW'
            || assessment.status === 'IN_PROGRESS'
          }
          reviewEditable={reviewEditable}
          progressHint={
            assessment.status === 'IN_PROGRESS'
              ? 'This is saved progress. Assign a verdict after the vendor submits.'
              : null
          }
          busy={busy}
          dirty={reviewDirty}
          onClose={() => setReviewItemId(null)}
          onPrev={() => {
            const prev = assessment.items[reviewItemIndex - 1]
            if (prev) setReviewItemId(prev.id)
          }}
          onNext={() => {
            const next = assessment.items[reviewItemIndex + 1]
            if (next) setReviewItemId(next.id)
          }}
          onPatch={(patch) => setItem(reviewItem.id, patch)}
          onSave={() => completeReview(false)}
          onDiscard={discardReviewEdits}
          onError={onError}
        />
      )}
      <Dialog open={issueOpen} onClose={() => setIssueOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Assign to Vendor</DialogTitle>
        <DialogContent>
          <Stack spacing={1} sx={{ mt: 0.5 }}>
            <Typography variant="body2" color="text.secondary">
              Issues the questionnaire to the Vendor primary SPOC. If set, Additional vendor contributors also receive a portal link. Until email is live, links and OTP 1111 stay on this page.
            </Typography>
            <CompactTextField
              label="Vendor primary SPOC - email"
              required
              fieldIdPrefix="tprm-issue"
              value={issueEmail}
              onChange={(e) => setIssueEmail(e.target.value)}
            />
            {contributor.email.trim() ? (
              <Typography variant="body2" color="text.secondary">
                Additional vendor contributors: {contributor.name?.trim() || contributor.email} ({contributor.email})
              </Typography>
            ) : (
              <Typography variant="body2" color="text.secondary">
                No Additional vendor contributors on this profile (optional).
              </Typography>
            )}
            <CompactTextField
              label="Due date"
              type="date"
              fieldIdPrefix="tprm-issue"
              value={issueDue}
              onChange={(e) => setIssueDue(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setIssueOpen(false)}>Cancel</Button>
          <BusyButton
            variant="contained"
            busy={busy}
            disabled={!issueEmail.trim()}
            disabledReason={!issueEmail.trim() ? 'Enter the vendor email' : undefined}
            onClick={() => setConfirmIssue(true)}
          >
            Assign / Issue
          </BusyButton>
        </DialogActions>
      </Dialog>
      <ConfirmActionDialog
        open={confirmIssue}
        title="Assign questionnaire to vendor?"
        message="Vendor primary SPOC can submit. Additional vendor contributors can draft only. Portal link and OTP will show on this page."
        confirmLabel={busy ? 'Issuing…' : 'Assign / Issue'}
        confirmColor="primary"
        busy={busy}
        onCancel={() => setConfirmIssue(false)}
        onConfirm={() => void issue()}
      />
      <ConfirmActionDialog
        open={confirmCompleteReview}
        title="Complete review?"
        message="This finishes Admin review, sets compliance posture, and schedules the next reassessment."
        confirmLabel={busy ? 'Completing…' : 'Complete Review'}
        confirmColor="primary"
        busy={busy}
        onCancel={() => setConfirmCompleteReview(false)}
        onConfirm={() => void completeReview(true)}
      />
      <ConfirmActionDialog
        open={confirmReturn}
        title="Return controls to vendor?"
        message="Controls marked Returned go back to the SPOC on the same portal link. Finish other verdicts before this step if you still need to review them."
        confirmLabel={busy ? 'Returning…' : 'Return to Vendor'}
        confirmColor="warning"
        busy={busy}
        onCancel={() => setConfirmReturn(false)}
        onConfirm={() => void completeReview(false, true)}
      />
      <ConfirmActionDialog
        open={confirmCertIdx != null}
        title="Remove this certification?"
        message="This removes the certification from the vendor form. Save the vendor to keep the change."
        confirmLabel="Remove"
        confirmColor="error"
        onCancel={() => setConfirmCertIdx(null)}
        onConfirm={() => {
          if (confirmCertIdx == null) return
          const next = (form.certs ?? []).filter((_, i) => i !== confirmCertIdx)
          setForm({ ...form, certs: next.length ? next : [{ certType: 'NONE', expiryDate: null }] })
          setConfirmCertIdx(null)
        }}
      />
      <ConfirmActionDialog
        open={confirmCloseFindingId != null}
        title="Close finding?"
        message="Mark this finding closed. This cannot be undone from the register."
        confirmLabel={busy ? 'Closing…' : 'Close Finding'}
        confirmColor="error"
        busy={busy}
        onCancel={() => setConfirmCloseFindingId(null)}
        onConfirm={() => {
          if (confirmCloseFindingId == null) return
          setBusy(true)
          void api.updateTprmFinding(confirmCloseFindingId, 'CLOSED')
            .then(() => {
              setConfirmCloseFindingId(null)
              onOk('Finding closed')
              if (selected) return openVendor(selected)
            })
            .catch((e) => onError(String(e)))
            .finally(() => setBusy(false))
        }}
      />
    </Stack>
  )
}
