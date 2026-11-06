import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import {
  Box,
  Button,
  Card,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Menu,
  MenuItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import {
  api,
  ApiError,
  type AuditExternalInvite,
  type CapaAvailableFinding,
  type CapaRecord,
  type CapaRegisterRecord,
  type AuditFinding,
  type FindingSeverity,
  type FrameworkSummary,
  type GrcAuditDetail,
  type GrcAuditStatus,
  type GrcAuditSummary,
  type GrcAuditType,
  type GapAssessmentSummary,
  type FindingRegisterItem,
  type OrgUnitNode,
  type Session,
  type UserSummary,
} from './api'
import StatusChip from './components/StatusChip'
import AuditEngagementDialog from './components/audit/AuditEngagementDialog'
import AuditEngagementWorkspace from './components/audit/AuditEngagementWorkspace'
import AuditEngagementCreateView from './components/audit/AuditEngagementCreateView'
import IaFieldworkShell from './components/audit/IaFieldworkShell'
import { auditNavIdToSection, type AuditModuleSection } from './components/audit/auditNav'
import { isAuditorConsole } from './components/audit/auditRoles'
import AuditCapaRegisterPanel from './components/audit/AuditCapaRegisterPanel'
import AuditCapaDetailPanel from './components/audit/AuditCapaDetailPanel'
import AuditCapaCreateView from './components/audit/AuditCapaCreateView'
import AuditCapaRegisterCreateView from './components/audit/AuditCapaRegisterCreateView'
import AuditCapaRegisterList from './components/audit/AuditCapaRegisterList'
import AuditEngagementHeaderMeta from './components/audit/AuditEngagementHeaderMeta'
import AuditAccessGrantsPanel from './components/audit/AuditAccessGrantsPanel'
import AuditAccessViewLogPanel from './components/audit/AuditAccessViewLogPanel'
import {
  CompactTextField,
  DomainBadge,
  FormSelect,
  BusyButton,
  KpiCard,
  KpiCardGrid,
  RegisterListFrame,
  RegisterSortHeader,
  RegisterToolbar,
  compactTableCellSxWidth,
  controlActionSx,
  mediumTableCellSx,
  primaryRegisterNameSx,
  primaryTableCellSx,
  registerTableSx,
} from './components/ui'
import { useListReady } from './uiQuality/useListReady'
import { usePagedRows } from './uiQuality/usePagedRows'
import { usePersistedState } from './uiQuality/usePersistedState'
import { compareLoose, toggleSort, type SortDir } from './uiQuality/sortRows'
import { AUDIT_REGISTER_COLUMNS } from './hooks/registerColumns'
import { useRegisterColumnPicker } from './hooks/useRegisterColumnPicker'
import type { AppShellPageConfig } from './components/AppShellPageContext'
import {
  auditControlRefSx,
  auditTableContainerSx,
  auditTableHeadCellSx,
  findingSeverityRiskLevel,
} from './components/audit/auditKit'
import {
  auditAssignedLabel,
  auditFrameworkLabel,
  auditModulesLabel,
  auditPeriodLabel,
  auditStatusLabel,
  auditStatusTone,
  findingSeverityShort,
} from './components/audit/auditEngagementUtils'
import AuditFrameworkLabel from './components/audit/AuditFrameworkLabel'
import { capaRef } from './components/audit/capaRegisterUtils'
import { brand } from './theme'
import { actionDueDateError, todayIso, useFormFields } from './validation'
import { useAppShellPage } from './components/AppShellPageContext'
import { useOptionalUnsavedChangesGuardApi } from './uiQuality/UnsavedChangesGuard'
import DiscardChangesDialog from './components/DiscardChangesDialog'

const SEVERITIES: { value: FindingSeverity; label: string }[] = [
  { value: 'MAJOR_NC', label: 'Major NC' },
  { value: 'MINOR_NC', label: 'Minor NC' },
  { value: 'OBSERVATION', label: 'Observation' },
  { value: 'OFI', label: 'OFI' },
]
const ENGAGEMENT_STATUSES: GrcAuditStatus[] = [
  'DRAFT',
  'ACCESS_GRANTED',
  'IN_PROGRESS',
  'REPORT_ISSUED',
  'CLOSED',
  'CANCELLED',
]

type EngagementStatusFilter = 'ALL' | GrcAuditStatus
type EngagementTypeFilter = 'ALL' | GrcAuditType
type FindingSeverityFilter = 'ALL' | FindingSeverity

function tone(s: string): 'success' | 'warning' | 'info' | 'danger' | 'neutral' {
  if (s === 'CLOSED' || s === 'COMPLETED') return 'success'
  if (s === 'REPORT_ISSUED' || s === 'PENDING_ADMIN' || s === 'PENDING_VERIFICATION') return 'warning'
  if (s === 'IN_PROGRESS' || s === 'ACCESS_GRANTED' || s === 'OPEN') return 'info'
  if (s === 'CANCELLED' || s === 'RETURNED' || s === 'OVERDUE') return 'danger'
  return 'neutral'
}

function flattenOrgUnits(nodes: OrgUnitNode[]): OrgUnitNode[] {
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

const AUDIT_SECTION_LABELS: Record<AuditModuleSection, string> = {
  register: 'Audit Projects',
  checklists: 'Audit Checklist',
  capa: 'CAPA',
  grants: 'Access Grants',
  accessLog: 'Access View Log',
  findings: 'Findings',
}

type AuditsPageMode = 'list' | 'create' | 'engagement' | 'capa-register-create' | 'capa-create'

export default function AuditsPage({
  session,
  navId,
  onNavChange,
  onError,
  onOk,
  openFromNotification = null,
  openCapaFromNotification = null,
}: {
  session: Session
  navId: string
  onNavChange: (id: string) => void
  onError: (msg: string) => void
  onOk: (msg: string) => void
  openFromNotification?: { auditId: number; epoch: number } | null
  openCapaFromNotification?: { capaId: number; epoch: number } | null
}) {
  const canPlan = session.roles.some((r) => ['OWNER', 'ADMIN'].includes(r))
  const canAdmin = session.roles.some((r) => ['OWNER', 'ADMIN'].includes(r))
  const auditorConsole = isAuditorConsole(session.roles)
  const contributorOnly =
    session.roles.includes('CONTRIBUTOR') &&
    !session.roles.some((r) =>
      ['OWNER', 'ADMIN', 'GLOBAL_READER', 'INTERNAL_AUDITOR', 'EXTERNAL_AUDITOR', 'SUPER_ADMIN'].includes(r),
    )
  const iaFieldwork = auditorConsole
  const iaFieldworkShell = false
  const moduleSection: AuditModuleSection = auditNavIdToSection(navId)
  const [pageMode, setPageMode] = useState<AuditsPageMode>('list')
  const auditColumns = useRegisterColumnPicker(AUDIT_REGISTER_COLUMNS)
  const [audits, setAudits] = useState<GrcAuditSummary[]>([])
  const [detail, setDetail] = useState<GrcAuditDetail | null>(null)
  const [capasInternal, setCapasInternal] = useState<CapaRecord[]>([])
  const [capasExternal, setCapasExternal] = useState<CapaRecord[]>([])
  const [capaRegisters, setCapaRegisters] = useState<CapaRegisterRecord[]>([])
  const [selectedRegister, setSelectedRegister] = useState<CapaRegisterRecord | null>(null)
  const [capaChrome, setCapaChrome] = useState<ReactNode>(null)
  const [registerFindings, setRegisterFindings] = useState<CapaAvailableFinding[]>([])
  const [users, setUsers] = useState<UserSummary[]>([])
  const [myUserId, setMyUserId] = useState<number | undefined>()
  const [frameworks, setFrameworks] = useState<FrameworkSummary[]>([])
  const [engagementEditOpen, setEngagementEditOpen] = useState(false)
  const [findingOpen, setFindingOpen] = useState(false)
  const [findingLeaveAsk, setFindingLeaveAsk] = useState(false)
  const [paperDirty, setPaperDirty] = useState(false)
  const closeAuditRef = useRef<() => void>(() => {})
  const submitReportRef = useRef<() => void>(() => {})
  const [reportBusy, setReportBusy] = useState(false)
  const [findingBusy, setFindingBusy] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [exportPos, setExportPos] = useState<{ top: number; left: number } | null>(null)
  const [orgUnitsFlat, setOrgUnitsFlat] = useState<OrgUnitNode[]>([])
  const [externalInvites, setExternalInvites] = useState<AuditExternalInvite[]>([])
  const [inviteName, setInviteName] = useState('')
  const [inviteEmail, setInviteEmail] = useState('')
  const [gaps, setGaps] = useState<GapAssessmentSummary[]>([])
  const [findingsRegister, setFindingsRegister] = useState<FindingRegisterItem[]>([])
  const [engagementSearch, setEngagementSearch] = usePersistedState('reg-audits-q', '')
  const [engagementStatusFilter, setEngagementStatusFilter] = useState<EngagementStatusFilter>('ALL')
  const [engagementTypeFilter, setEngagementTypeFilter] = useState<EngagementTypeFilter>('ALL')
  const [findingSearch, setFindingSearch] = usePersistedState('reg-findings-q', '')
  const { listReady, markReady } = useListReady()
  const { listReady: findingsReady, markReady: markFindingsReady } = useListReady()
  const { listReady: capaRegistersReady, markReady: markCapaRegistersReady } = useListReady()
  const { listReady: capaRowsReady, markReady: markCapaRowsReady } = useListReady()
  const [auditSortKey, setAuditSortKey] = useState('name')
  const [auditSortDir, setAuditSortDir] = useState<SortDir>('asc')
  const [findingSortKey, setFindingSortKey] = useState('title')
  const [findingSortDir, setFindingSortDir] = useState<SortDir>('asc')
  const [findingSeverityFilter, setFindingSeverityFilter] = useState<FindingSeverityFilter>('ALL')
  const [findingAuditFilter, setFindingAuditFilter] = useState<number | 'ALL'>('ALL')
  const [domainNarratives, setDomainNarratives] = useState<Record<string, string>>({})
  const [summary, setSummary] = useState('')
  const [fTitle, setFTitle] = useState('')
  const [fDesc, setFDesc] = useState('')
  const [fSev, setFSev] = useState<FindingSeverity>('MINOR_NC')
  const [fControl, setFControl] = useState('')
  const [fItemId, setFItemId] = useState<number | ''>('')
  const [assignCapaId, setAssignCapaId] = useState<number | null>(null)
  const [selectedCapa, setSelectedCapa] = useState<CapaRecord | null>(null)
  const [assignOwner, setAssignOwner] = useState<number | ''>('')
  const [assignTargetDate, setAssignTargetDate] = useState('')
  const assignFields = useFormFields<'targetDate'>()
  const [returnReason, setReturnReason] = useState('')
  const dateMin = todayIso()
  const assignTargetDateError = actionDueDateError(assignTargetDate)

  const loadAudits = useCallback(async () => {
    if (contributorOnly) {
      markReady()
      return
    }
    try {
      setAudits(await api.listAudits())
    } catch (e) {
      onError(String(e))
    } finally {
      markReady()
    }
  }, [contributorOnly, onError, markReady])

  const loadCapasInternal = useCallback(async () => {
    try {
      const list = await api.listCapa('INTERNAL_AUDIT')
      setCapasInternal(list)
      setSelectedCapa((prev) => {
        if (prev?.registerCode !== 'INTERNAL_AUDIT') return prev
        const found = list.find((c) => c.id === prev.id)
        if (!found) return null
        if (prev.status === 'CLOSED' && found.status !== 'CLOSED') return prev
        return found
      })
    } catch (e) {
      onError(String(e))
    }
  }, [onError])

  const loadCapasExternal = useCallback(async () => {
    try {
      const list = await api.listCapa('EXTERNAL_AUDIT')
      setCapasExternal(list)
      setSelectedCapa((prev) => {
        if (prev?.registerCode !== 'EXTERNAL_AUDIT') return prev
        const found = list.find((c) => c.id === prev.id)
        if (!found) return null
        if (prev.status === 'CLOSED' && found.status !== 'CLOSED') return prev
        return found
      })
    } catch (e) {
      onError(String(e))
    }
  }, [onError])

  const loadCapaRegisters = useCallback(async () => {
    try {
      const list = await api.listCapaRegisters()
      setCapaRegisters(list)
      setSelectedRegister((prev) => (prev ? list.find((r) => r.id === prev.id) ?? null : prev))
    } catch (e) {
      onError(String(e))
    } finally {
      markCapaRegistersReady()
    }
  }, [onError, markCapaRegistersReady])

  const loadRegisterCapas = useCallback(async () => {
    if (!selectedRegister) return
    try {
      const list = await api.listCapa(undefined, selectedRegister.id)
      setCapasInternal(list)
      setCapasExternal([])
      setSelectedCapa((prev) => {
        if (!prev) return prev
        const found = list.find((c) => c.id === prev.id)
        if (!found) return null
        if (prev.status === 'CLOSED' && found.status !== 'CLOSED') return prev
        return found
      })
    } catch (e) {
      onError(String(e))
    } finally {
      markCapaRowsReady()
    }
  }, [onError, selectedRegister, markCapaRowsReady])

  const loadCapas = useCallback(async () => {
    try {
      if (contributorOnly) {
        const list = await api.listCapa()
        setCapasInternal(list)
        setCapasExternal([])
        setSelectedCapa((prev) => {
          if (!prev) return prev
          const found = list.find((c) => c.id === prev.id)
          if (!found) return null
          if (prev.status === 'CLOSED' && found.status !== 'CLOSED') return prev
          return found
        })
        return
      }
      await Promise.all([loadCapasInternal(), loadCapasExternal()])
    } catch (e) {
      onError(String(e))
    } finally {
      markCapaRowsReady()
    }
  }, [contributorOnly, loadCapasInternal, loadCapasExternal, onError, markCapaRowsReady])

  const publishCapa = useCallback(
    (next: CapaRecord) => {
      setSelectedCapa(next)
      if (selectedRegister) void loadRegisterCapas()
      else void loadCapas()
    },
    [selectedRegister, loadRegisterCapas, loadCapas],
  )

  useEffect(() => {
    loadAudits()
    if (canPlan) void loadCapas()
    api.authMe().then((u) => setMyUserId(u.id)).catch(() => undefined)
    if (canPlan) {
      api.listUsers().then(setUsers).catch(() => undefined)
      api.listActivatedFrameworks().then(setFrameworks).catch(() => undefined)
      api.listGapAssessments().then(setGaps).catch(() => setGaps([]))
      api
        .listOrgUnits()
        .then((tree) => setOrgUnitsFlat(flattenOrgUnits(tree)))
        .catch(() => setOrgUnitsFlat([]))
    } else {
      api
        .listUserDirectory()
        .then((rows) =>
          setUsers(
            rows.map((r) => ({
              id: r.id,
              email: r.email,
              fullName: r.fullName,
              authType: '',
              status: 'ACTIVE',
              roles: [],
              createdAt: '',
            })),
          ),
        )
        .catch(() => undefined)
    }
  }, [loadAudits, loadCapas, canPlan])

  const loadFindingsRegister = useCallback(
    (severity?: FindingSeverity) => {
      api
        .listAuditFindingsRegister(severity)
        .then(setFindingsRegister)
        .catch((e) => onError(String(e)))
        .finally(markFindingsReady)
    },
    [onError, markFindingsReady],
  )

  useEffect(() => {
    if (pageMode !== 'list') return
    if (moduleSection === 'findings') {
      if (contributorOnly) {
        markFindingsReady()
      } else {
        loadFindingsRegister(findingSeverityFilter === 'ALL' ? undefined : findingSeverityFilter)
      }
    }
    if (moduleSection === 'capa') {
      if (contributorOnly) void loadCapas()
      else if (selectedRegister) void loadRegisterCapas()
      else void loadCapaRegisters()
    }
  }, [
    moduleSection,
    pageMode,
    findingSeverityFilter,
    loadFindingsRegister,
    loadCapas,
    loadCapaRegisters,
    loadRegisterCapas,
    selectedRegister,
    contributorOnly,
    markFindingsReady,
  ])

  // Reset drill-in when switching audit nav item (admin console only: IA stays in engagement workspace).
  useEffect(() => {
    if (iaFieldworkShell) return
    setDetail(null)
    setSelectedCapa(null)
    setSelectedRegister(null)
    setPageMode('list')
  }, [navId, iaFieldworkShell])

  const loadExternalInvites = useCallback(
    async (auditId: number) => {
      try {
        setExternalInvites(await api.listAuditExternalInvites(auditId))
      } catch (e) {
        onError(String(e))
      }
    },
    [onError],
  )

  const openAudit = useCallback(
    async (id: number) => {
      try {
        const d = await api.getAudit(id)
        setDetail(d)
        setSummary(d.executiveSummary ?? '')
        const narr: Record<string, string> = {}
        for (const n of d.domainNarratives ?? []) {
          narr[n.code] = n.narrative ?? ''
        }
        setDomainNarratives(narr)
        setPageMode('engagement')
        if (canPlan && d.audit.auditType === 'EXTERNAL') {
          void loadExternalInvites(id)
        } else {
          setExternalInvites([])
        }
        if (canPlan) void loadCapas()
      } catch (e) {
        onError(String(e))
      }
    },
    [canPlan, loadExternalInvites, loadCapas, onError],
  )

  const iaAutoOpened = useRef(false)
  const auditIdFromQuery = useMemo(() => {
    const q = new URLSearchParams(window.location.search).get('auditId')
    if (!q) return null
    const n = Number(q)
    return Number.isFinite(n) ? n : null
  }, [])

  useEffect(() => {
    if (!iaFieldwork || iaAutoOpened.current) return
    if (auditIdFromQuery != null) {
      iaAutoOpened.current = true
      void openAudit(auditIdFromQuery)
    }
  }, [iaFieldwork, auditIdFromQuery, openAudit])

  const notifyEpochSeen = useRef<number | null>(null)
  useEffect(() => {
    if (!openFromNotification) return
    if (notifyEpochSeen.current === openFromNotification.epoch) return
    notifyEpochSeen.current = openFromNotification.epoch
    void openAudit(openFromNotification.auditId)
  }, [openFromNotification, openAudit])

  const capaNotifySeen = useRef<number | null>(null)
  useEffect(() => {
    if (!openCapaFromNotification) return
    if (capaNotifySeen.current === openCapaFromNotification.epoch) return
    capaNotifySeen.current = openCapaFromNotification.epoch
    const capaId = openCapaFromNotification.capaId
    void (async () => {
      try {
        const c = await api.getCapa(capaId)
        setSelectedRegister(null)
        setSelectedCapa(c)
        setPageMode('list')
      } catch (e) {
        onError(String(e))
      }
    })()
  }, [openCapaFromNotification, onError])

  const capasById = useMemo(() => {
    const map = new Map<number, CapaRecord>()
    for (const c of [...capasInternal, ...capasExternal]) {
      map.set(c.id, c)
    }
    return map
  }, [capasInternal, capasExternal])

  const engagementCapas = useMemo(() => {
    if (!detail) return []
    const source = detail.audit.auditType === 'EXTERNAL' ? capasExternal : capasInternal
    return source.filter((c) => c.sourceAuditId === detail.audit.id)
  }, [detail, capasInternal, capasExternal])

  const contributors = users.filter((u) => u.roles.includes('CONTRIBUTOR'))

  const filteredAudits = useMemo(() => {
    const q = engagementSearch.trim().toLowerCase()
    const next = audits.filter((a) => {
      if (engagementStatusFilter !== 'ALL' && a.status !== engagementStatusFilter) return false
      if (engagementTypeFilter !== 'ALL' && a.auditType !== engagementTypeFilter) return false
      if (!q) return true
      const assigned = auditAssignedLabel(a).toLowerCase()
      const modules = auditModulesLabel(a.scopeModules).toLowerCase()
      return (
        a.name.toLowerCase().includes(q) ||
        auditFrameworkLabel(a).toLowerCase().includes(q) ||
        a.frameworkCode.toLowerCase().includes(q) ||
        (a.frameworkName?.toLowerCase().includes(q) ?? false) ||
        assigned.includes(q) ||
        modules.includes(q) ||
        (a.displayId?.toLowerCase().includes(q) ?? false)
      )
    })
    const valueOf = (a: GrcAuditSummary) => {
      switch (auditSortKey) {
        case 'assigned':
          return auditAssignedLabel(a)
        case 'modules':
          return auditModulesLabel(a.scopeModules)
        case 'dates':
          return `${a.startDate ?? ''}:${a.endDate ?? ''}`
        case 'status':
          return a.status
        case 'type':
          return a.auditType
        default:
          return a.name
      }
    }
    return [...next].sort((a, b) => compareLoose(valueOf(a), valueOf(b), auditSortDir))
  }, [audits, engagementSearch, engagementStatusFilter, engagementTypeFilter, auditSortDir, auditSortKey])
  const auditsPaged = usePagedRows(filteredAudits)

  const engagementStatusOptions = useMemo(
    () => [
      { value: 'ALL' as const, label: 'All', count: audits.length },
      ...ENGAGEMENT_STATUSES.map((status) => ({
        value: status,
        label: status.replace(/_/g, ' '),
        count: audits.filter((a) => a.status === status).length,
      })),
    ],
    [audits],
  )

  const engagementTypeOptions = useMemo(
    () => [
      { value: 'ALL' as const, label: 'All types', count: audits.length },
      {
        value: 'INTERNAL' as const,
        label: 'Internal',
        count: audits.filter((a) => a.auditType === 'INTERNAL').length,
      },
      {
        value: 'EXTERNAL' as const,
        label: 'External',
        count: audits.filter((a) => a.auditType === 'EXTERNAL').length,
      },
    ],
    [audits],
  )

  const clearEngagementFilters = useCallback(() => {
    setEngagementSearch('')
    setEngagementStatusFilter('ALL')
    setEngagementTypeFilter('ALL')
  }, [])

  const filteredFindings = useMemo(() => {
    const q = findingSearch.trim().toLowerCase()
    const next = findingsRegister.filter((f) => {
      if (findingAuditFilter !== 'ALL' && f.auditId !== findingAuditFilter) return false
      if (!q) return true
      return (
        f.title.toLowerCase().includes(q) ||
        f.displayCode.toLowerCase().includes(q) ||
        f.auditName.toLowerCase().includes(q) ||
        (f.controlRef?.toLowerCase().includes(q) ?? false)
      )
    })
    return [...next].sort((a, b) => compareLoose(a.title, b.title, findingSortDir))
  }, [findingsRegister, findingSearch, findingAuditFilter, findingSortDir])
  const findingsPaged = usePagedRows(filteredFindings)

  const findingSeverityOptions = useMemo(
    () => [
      { value: 'ALL' as const, label: 'All severities', count: findingsRegister.length },
      ...SEVERITIES.map((severity) => ({
        value: severity.value,
        label: findingSeverityShort(severity.value),
        count: findingsRegister.filter((f) => f.severity === severity.value).length,
      })),
    ],
    [findingsRegister],
  )

  const findingAuditOptions = useMemo(() => {
    const byId = new Map<number, string>()
    for (const f of findingsRegister) {
      byId.set(f.auditId, f.auditName)
    }
    for (const a of audits) {
      byId.set(a.id, a.name)
    }
    return [
      { value: 'ALL' as const, label: 'All engagements', count: findingsRegister.length },
      ...[...byId.entries()]
        .sort((a, b) => a[1].localeCompare(b[1]))
        .map(([id, name]) => ({
          value: id,
          label: name,
          count: findingsRegister.filter((f) => f.auditId === id).length,
        })),
    ]
  }, [findingsRegister, audits])

  const clearFindingFilters = useCallback(() => {
    setFindingSearch('')
    setFindingSeverityFilter('ALL')
    setFindingAuditFilter('ALL')
  }, [])

  const handleEngagementSaved = useCallback(
    (d: GrcAuditDetail) => {
      onOk('Audit engagement created')
      void loadAudits()
      setDetail(d)
      setSummary(d.executiveSummary ?? '')
      setPageMode('engagement')
    },
    [loadAudits, onOk],
  )

  const { requestNavigation } = useOptionalUnsavedChangesGuardApi()

  const closeEngagement = useCallback(() => {
    requestNavigation(() => {
      setDetail(null)
      setPageMode('list')
    })
  }, [requestNavigation])

  const openCreate = useCallback(() => setPageMode('create'), [])

  const closeCapaDetail = useCallback(() => {
    requestNavigation(() => setSelectedCapa(null))
  }, [requestNavigation])

  const closeRegister = useCallback(() => {
    requestNavigation(() => {
      setSelectedRegister(null)
      setSelectedCapa(null)
      setRegisterFindings([])
      setPageMode('list')
      void loadCapaRegisters()
    })
  }, [loadCapaRegisters, requestNavigation])

  const openRegister = useCallback(
    async (r: CapaRegisterRecord) => {
      try {
        const [fresh, findings] = await Promise.all([
          api.getCapaRegister(r.id),
          api.listCapaAvailableFindings(),
        ])
        setSelectedRegister(fresh)
        setRegisterFindings(findings)
        const list = await api.listCapa(undefined, r.id)
        setCapasInternal(list)
        setCapasExternal([])
        setSelectedCapa(null)
        setPageMode('list')
      } catch (e) {
        onError(e instanceof ApiError ? e.message : String(e))
      }
    },
    [onError],
  )

  const backToList = useCallback(() => {
    requestNavigation(() => setPageMode('list'))
  }, [requestNavigation])

  const newEngagementButton = useMemo(
    () =>
      canPlan ? (
        <Button variant="contained" size="small" onClick={openCreate}>
          New Audit
        </Button>
      ) : null,
    [canPlan, openCreate],
  )

  const newCapaButton = useMemo(() => {
    if (!canAdmin) return null
    if (selectedRegister) {
      return (
        <Button
          variant="contained"
          size="small"
          sx={controlActionSx}
          onClick={() => {
            void api
              .listCapaAvailableFindings()
              .then(setRegisterFindings)
              .catch((e) => onError(e instanceof ApiError ? e.message : String(e)))
              .finally(() => setPageMode('capa-create'))
          }}
        >
          Add CAPA
        </Button>
      )
    }
    return (
      <Button
        variant="contained"
        size="small"
        sx={controlActionSx}
        onClick={() => setPageMode('capa-register-create')}
      >
        Add CAPA Register
      </Button>
    )
  }, [canAdmin, selectedRegister, onError])

  const auditShell = useMemo((): AppShellPageConfig | null => {
    const chrome = { hideContextLabel: true as const, hideRoleChips: true as const }
    const iaChrome = { hideContextLabel: true as const, hideRoleChips: false as const }

    if (iaFieldworkShell) {
      if (pageMode === 'create') {
        return {
          ...iaChrome,
          title: 'New Audit Engagement',
          breadcrumbTrail: 'Create',
          breadcrumbRoot: { label: 'My Engagements', onClick: backToList },
          hideSidebar: true,
          flushContent: true,
        }
      }
      return {
        ...iaChrome,
        title: detail?.audit.name ?? 'Audit fieldwork',
        breadcrumbTrail: detail ? detail.audit.name : 'My Engagements',
        hideSidebar: true,
        flushContent: true,
      }
    }

    if (pageMode === 'create') {
      return {
        ...chrome,
        title: 'New Audit',
        breadcrumbTrail: 'Create',
        breadcrumbRoot: { label: 'Audit Projects', onClick: backToList },
      }
    }

    if (pageMode === 'engagement' && detail) {
      return {
        ...chrome,
        title: detail.audit.name,
        breadcrumbTrail: detail.audit.displayId?.trim() || 'Engagement',
        breadcrumbRoot: { label: 'Audit Projects', onClick: closeEngagement },
      }
    }

    if (pageMode === 'capa-register-create') {
      return {
        ...chrome,
        title: 'Add CAPA Register',
        breadcrumbTrail: 'Create',
        breadcrumbRoot: { label: 'CAPA', onClick: backToList },
        actions: capaChrome ?? undefined,
      }
    }

    if (pageMode === 'capa-create' && selectedRegister) {
      return {
        ...chrome,
        title: 'Add CAPA',
        breadcrumbTrail: 'Create',
        breadcrumbRoot: {
          label: selectedRegister.name,
          onClick: backToList,
        },
        actions: capaChrome ?? undefined,
      }
    }

    if (selectedCapa) {
      return {
        ...chrome,
        title: capaRef(selectedCapa.id),
        breadcrumbTrail: selectedCapa.title || capaRef(selectedCapa.id),
        breadcrumbRoot: {
          label: selectedRegister?.name ?? 'CAPA',
          onClick: closeCapaDetail,
        },
        actions: capaChrome ?? undefined,
      }
    }

    if (selectedRegister && moduleSection === 'capa') {
      return {
        ...chrome,
        title: selectedRegister.name,
        breadcrumbTrail: selectedRegister.name,
        breadcrumbRoot: { label: 'CAPA', onClick: closeRegister },
        actions: newCapaButton ?? undefined,
      }
    }

    return {
      ...chrome,
      title: AUDIT_SECTION_LABELS[moduleSection],
      breadcrumbTrail: `Audit › ${AUDIT_SECTION_LABELS[moduleSection]}`,
      actions:
        moduleSection === 'register'
          ? newEngagementButton ?? undefined
          : moduleSection === 'capa'
            ? newCapaButton ?? undefined
            : undefined,
    }
  }, [
    iaFieldworkShell,
    iaFieldwork,
    detail,
    pageMode,
    selectedCapa,
    selectedRegister,
    moduleSection,
    newEngagementButton,
    newCapaButton,
    capaChrome,
    backToList,
    closeEngagement,
    closeCapaDetail,
    closeRegister,
    pageMode,
  ])

  const meId =
    myUserId ?? users.find((u) => u.email.toLowerCase() === session.email.toLowerCase())?.id
  const isTeamMember =
    !!detail &&
    (canAdmin ||
      detail.audit.leadAuditorUserId === meId ||
      (detail.accessGrants ?? []).some((g) => g.userId === meId && !g.revoked) ||
      (detail.teamMembers ?? []).some((m) => m.userId === meId))
  const isLeadAuditor =
    !!detail &&
    ((detail.teamMembers ?? []).some((m) => m.userId === meId && m.teamRole === 'LEAD') ||
      (detail.teamMembers?.length === 0 && detail.audit.leadAuditorUserId === meId))
  const canRaiseFinding =
    !!detail &&
    (isTeamMember || canAdmin) &&
    !detail.audit.locked &&
    !['CLOSED', 'CANCELLED'].includes(detail.audit.status) &&
    (canAdmin || detail.audit.status !== 'REPORT_ISSUED') &&
    detail.audit.auditMode !== 'ASSESSMENT'

  const showAddFinding =
    !!detail &&
    (isTeamMember || canAdmin) &&
    detail.audit.auditMode !== 'ASSESSMENT' &&
    !['CLOSED', 'CANCELLED'].includes(detail.audit.status)

  const addFindingWhy = !detail
    ? undefined
    : detail.audit.locked
      ? 'Audit is locked'
      : detail.audit.status === 'REPORT_ISSUED' && !canAdmin
        ? 'Report submitted - findings are final for the auditor'
        : undefined

  const openFindingDialog = (prefill?: { checklistItemId?: number; controlRef?: string }) => {
    setFItemId(prefill?.checklistItemId ?? '')
    setFControl(prefill?.controlRef ?? '')
    setFTitle('')
    setFDesc('')
    setFSev('MINOR_NC')
    setFindingOpen(true)
  }

  const userLabel = (id: number) => users.find((u) => u.id === id)?.fullName ?? `#${id}`

  const canExportReport = session.roles.some((r) =>
    ['OWNER', 'ADMIN', 'INTERNAL_AUDITOR', 'EXTERNAL_AUDITOR'].includes(r),
  )

  const exportAudit = async (format: 'pdf' | 'xlsx') => {
    if (!detail) return
    setExportPos(null)
    setExporting(true)
    try {
      await api.downloadAuditReport(detail.audit.id, format)
    } catch (e) {
      onError(e instanceof ApiError ? e.message : 'Could not export the audit report')
    } finally {
      setExporting(false)
    }
  }

  const canCloseAudit =
    !!detail &&
    canAdmin &&
    !detail.audit.locked &&
    !['CLOSED', 'CANCELLED'].includes(detail.audit.status)

  const canSubmitReport =
    !!detail &&
    !!isLeadAuditor &&
    !detail.audit.locked &&
    detail.audit.status === 'IN_PROGRESS'

  const engagementHeader = useMemo(() => {
    if (pageMode !== 'engagement' || !detail) {
      return {}
    }
    const assigned = detail.audit.leadAuditorName?.trim() || userLabel(detail.audit.leadAuditorUserId)
    const framework = auditFrameworkLabel(detail.audit)
    const period =
      detail.audit.startDate || detail.audit.endDate
        ? auditPeriodLabel(detail.audit.startDate, detail.audit.endDate)
        : 'Dates not set'
    return {
      headerMeta: (
        <AuditEngagementHeaderMeta
          status={detail.audit.status}
          findingCount={detail.findings.length}
          framework={framework}
          period={period}
          assignee={assigned}
        />
      ),
      actions: (
        <Stack direction="row" spacing={0.75} flexWrap="nowrap" useFlexGap>
          {canExportReport && (
            <>
              <BusyButton
                size="small"
                variant="outlined"
                sx={controlActionSx}
                busy={exporting}
                onClick={(e) => {
                  const rect = e.currentTarget.getBoundingClientRect()
                  setExportPos({ top: rect.bottom, left: rect.left })
                }}
              >
                {exporting ? 'Exporting...' : 'Export'}
              </BusyButton>
              <Menu
                anchorReference="anchorPosition"
                anchorPosition={exportPos ?? { top: 0, left: 0 }}
                open={Boolean(exportPos)}
                onClose={() => setExportPos(null)}
              >
                <MenuItem onClick={() => void exportAudit('xlsx')}>Excel</MenuItem>
                <MenuItem onClick={() => void exportAudit('pdf')}>PDF</MenuItem>
              </Menu>
            </>
          )}
          {canAdmin && detail.audit.status === 'DRAFT' && (
            <Button
              size="small"
              variant="outlined"
              sx={controlActionSx}
              onClick={() => setEngagementEditOpen(true)}
            >
              Edit Audit
            </Button>
          )}
          {showAddFinding && (
            <BusyButton
              size="small"
              variant="outlined"
              sx={controlActionSx}
              disabled={!canRaiseFinding}
              disabledReason={addFindingWhy}
              onClick={() => openFindingDialog()}
            >
              Add Finding
            </BusyButton>
          )}
          {canSubmitReport && (
            <BusyButton
              size="small"
              variant="contained"
              sx={controlActionSx}
              busy={reportBusy}
              busyLabel="Submitting..."
              disabled={paperDirty || reportBusy}
              disabledReason={
                paperDirty ? 'Save progress before submitting' : reportBusy ? 'Submitting the report' : undefined
              }
              onClick={() => submitReportRef.current()}
            >
              Submit Report
            </BusyButton>
          )}
          {canCloseAudit && (
            <BusyButton
              size="small"
              variant="contained"
              sx={controlActionSx}
              disabled={detail.audit.status !== 'REPORT_ISSUED' || paperDirty}
              disabledReason={
                detail.audit.status !== 'REPORT_ISSUED'
                  ? 'The auditor must submit the report first'
                  : paperDirty
                    ? 'Save progress before closing'
                    : undefined
              }
              onClick={() => closeAuditRef.current()}
            >
              Close Audit
            </BusyButton>
          )}
        </Stack>
      ),
    }
  }, [pageMode, detail, canAdmin, canExportReport, exporting, exportPos, canRaiseFinding, showAddFinding, addFindingWhy, canCloseAudit, canSubmitReport, paperDirty, reportBusy, summary, users])

  useAppShellPage(auditShell ? { ...auditShell, ...engagementHeader } : null, [
    auditShell,
    engagementHeader,
  ])

  useEffect(() => {
    if (pageMode !== 'engagement') setPaperDirty(false)
  }, [pageMode, detail?.audit.id])

  const refreshDetail = async () => {
    if (!detail) return
    await openAudit(detail.audit.id)
  }

  const sendExternalInvite = () => {
    if (!detail) return Promise.resolve()
    return api
      .createAuditExternalInvite(detail.audit.id, {
        name: inviteName.trim(),
        email: inviteEmail.trim(),
      })
      .then(() => {
        setInviteName('')
        setInviteEmail('')
        onOk('Invite sent')
        void loadExternalInvites(detail.audit.id)
      })
      .catch((e) => onError(String(e)))
  }

  const addFinding = async () => {
    if (!detail || !fTitle.trim() || !fDesc.trim() || findingBusy) return
    setFindingBusy(true)
    try {
      await api.addAuditFinding(detail.audit.id, {
        checklistItemId: fItemId === '' ? null : Number(fItemId),
        severity: fSev,
        title: fTitle.trim(),
        description: fDesc.trim(),
        controlRef: fControl.trim() || null,
      })
      setFindingOpen(false)
      setFTitle('')
      setFDesc('')
      onOk('Finding added')
      await openAudit(detail.audit.id)
    } catch (e) {
      onError(String(e))
    } finally {
      setFindingBusy(false)
    }
  }

  return (
    <Stack
      spacing={iaFieldworkShell ? 0 : 1.25}
      sx={{ flex: 1, minHeight: 0, height: '100%' }}
    >
      {iaFieldworkShell ? (
        <Box sx={{ flex: 1, minHeight: 0, overflow: 'hidden', display: 'flex' }}>
          {pageMode === 'create' ? (
            <AuditEngagementCreateView
              users={users}
              frameworks={frameworks}
              gaps={gaps}
              orgUnits={orgUnitsFlat.map((u) => ({ id: u.id, name: u.name, level: u.level }))}
              onCancel={backToList}
              onSaved={handleEngagementSaved}
              onError={onError}
              internalPlanner
              defaultLeadUserId={myUserId}
            />
          ) : (
          <IaFieldworkShell
            detail={detail}
            audits={audits}
            users={users}
            isTeamMember={!!isTeamMember}
            isLeadAuditor={!!isLeadAuditor}
            canRaiseFinding={canRaiseFinding}
            engagementCapas={engagementCapas}
            capasById={capasById}
            summary={summary}
            onSummaryChange={setSummary}
            domainNarratives={domainNarratives}
            onDomainNarrativesChange={setDomainNarratives}
            returnReason={returnReason}
            onReturnReasonChange={setReturnReason}
            externalInvites={externalInvites}
            inviteName={inviteName}
            inviteEmail={inviteEmail}
            onInviteNameChange={setInviteName}
            onInviteEmailChange={setInviteEmail}
            onSendInvite={sendExternalInvite}
            onRefresh={refreshDetail}
            onEditEngagement={() => setEngagementEditOpen(true)}
            onLogFinding={openFindingDialog}
            onError={onError}
            onOk={onOk}
            onLoadAudits={loadAudits}
            onLoadCapas={loadCapas}
            onOpenEngagement={(id) => openAudit(id)}
            onCreateEngagement={canPlan ? openCreate : undefined}
            canCreateEngagement={canPlan}
            engagementSearch={engagementSearch}
            engagementStatusFilter={engagementStatusFilter}
            engagementTypeFilter={engagementTypeFilter}
            onEngagementSearchChange={setEngagementSearch}
            onEngagementStatusFilterChange={setEngagementStatusFilter}
            onEngagementTypeFilterChange={setEngagementTypeFilter}
            onClearEngagementFilters={clearEngagementFilters}
          />
          )}
        </Box>
      ) : (
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
        {pageMode === 'create' && (
            <AuditEngagementCreateView
              users={users}
              frameworks={frameworks}
              gaps={gaps}
              orgUnits={orgUnitsFlat.map((u) => ({ id: u.id, name: u.name, level: u.level }))}
              onCancel={backToList}
              onSaved={handleEngagementSaved}
              onError={onError}
            />
          )}

          {pageMode === 'engagement' && detail && (
            <Box sx={{ flex: 1, minHeight: 0, overflow: 'hidden', display: 'flex' }}>
              <AuditEngagementWorkspace
                detail={detail}
                users={users}
                canAdmin={canAdmin}
                isTeamMember={!!isTeamMember}
                isLeadAuditor={!!isLeadAuditor}
                iaFieldwork={iaFieldwork}
                canRaiseFinding={canRaiseFinding}
                engagementCapas={engagementCapas}
                capasById={capasById}
                summary={summary}
                onSummaryChange={setSummary}
                domainNarratives={domainNarratives}
                onDomainNarrativesChange={setDomainNarratives}
                returnReason={returnReason}
                onReturnReasonChange={setReturnReason}
                externalInvites={externalInvites}
                inviteName={inviteName}
                inviteEmail={inviteEmail}
                onInviteNameChange={setInviteName}
                onInviteEmailChange={setInviteEmail}
                onSendInvite={sendExternalInvite}
                onRefresh={refreshDetail}
                onEditEngagement={() => setEngagementEditOpen(true)}
                onLogFinding={openFindingDialog}
                onError={onError}
                onOk={onOk}
                onLoadAudits={loadAudits}
                onLoadCapas={loadCapas}
                onBackToList={closeEngagement}
                onPaperDirtyChange={setPaperDirty}
                onBindCloseAudit={(open) => {
                  closeAuditRef.current = open
                }}
                onBindSubmitReport={(submit) => {
                  submitReportRef.current = submit
                }}
                onReportBusyChange={setReportBusy}
              />
            </Box>
          )}

          {pageMode === 'list' && moduleSection === 'register' && !detail && (
            <Box sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column', p: 2 }}>
              <RegisterToolbar
                search={engagementSearch}
                searchPlaceholder="Search audits"
                onSearchChange={setEngagementSearch}
                onClearFilters={clearEngagementFilters}
                filters={[
                  {
                    id: 'status',
                    label: 'Status',
                    value: engagementStatusFilter,
                    options: engagementStatusOptions.map((o) => ({
                      value: o.value,
                      label: `${o.label} (${o.count})`,
                    })),
                    onChange: (v) => setEngagementStatusFilter(v as EngagementStatusFilter),
                    minWidth: 160,
                  },
                  {
                    id: 'type',
                    label: 'Type',
                    value: engagementTypeFilter,
                    options: engagementTypeOptions.map((o) => ({
                      value: o.value,
                      label: `${o.label} (${o.count})`,
                    })),
                    onChange: (v) => setEngagementTypeFilter(v as EngagementTypeFilter),
                    minWidth: 140,
                  },
                ]}
                columnPicker={auditColumns.columnPicker}
              />
              <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
              <RegisterListFrame
                ready={listReady}
                count={auditsPaged.count}
                emptyMessage={
                  audits.length === 0
                    ? 'No audits yet. Create an audit, set scope, and assign an auditor.'
                    : 'No audits match the current filters.'
                }
                emptyAction={
                  audits.length > 0 ? (
                    <Button variant="outlined" size="small" onClick={clearEngagementFilters}>
                      Clear Filters
                    </Button>
                  ) : undefined
                }
                page={auditsPaged.page}
                rowsPerPage={auditsPaged.rowsPerPage}
                onPageChange={auditsPaged.setPage}
                onRowsPerPageChange={auditsPaged.setRowsPerPage}
              >
              <TableContainer sx={{ ...auditTableContainerSx, border: 'none', boxShadow: 'none', borderRadius: 0, flex: 1 }}>
                <Table size="small" stickyHeader sx={registerTableSx}>
                  <TableHead>
                    <TableRow>
                      {auditColumns.isVisible('name') && (
                        <RegisterSortHeader
                          label="Name"
                          active={auditSortKey === 'name'}
                          dir={auditSortDir}
                          width="18%"
                          onClick={() => {
                            const next = toggleSort(auditSortKey, auditSortDir, 'name')
                            setAuditSortKey(next.key)
                            setAuditSortDir(next.dir)
                          }}
                        />
                      )}
                      {auditColumns.isVisible('type') && (
                        <TableCell sx={auditTableHeadCellSx} width="10%">
                          Type
                        </TableCell>
                      )}
                      {auditColumns.isVisible('assigned') && (
                        <RegisterSortHeader
                          label="Assigned to"
                          active={auditSortKey === 'assigned'}
                          dir={auditSortDir}
                          width="14%"
                          onClick={() => {
                            const next = toggleSort(auditSortKey, auditSortDir, 'assigned')
                            setAuditSortKey(next.key)
                            setAuditSortDir(next.dir)
                          }}
                        />
                      )}
                      {auditColumns.isVisible('modules') && (
                        <RegisterSortHeader
                          label="Modules in scope"
                          active={auditSortKey === 'modules'}
                          dir={auditSortDir}
                          width="20%"
                          onClick={() => {
                            const next = toggleSort(auditSortKey, auditSortDir, 'modules')
                            setAuditSortKey(next.key)
                            setAuditSortDir(next.dir)
                          }}
                        />
                      )}
                      {auditColumns.isVisible('dates') && (
                        <RegisterSortHeader
                          label="Dates"
                          active={auditSortKey === 'dates'}
                          dir={auditSortDir}
                          width="16%"
                          onClick={() => {
                            const next = toggleSort(auditSortKey, auditSortDir, 'dates')
                            setAuditSortKey(next.key)
                            setAuditSortDir(next.dir)
                          }}
                        />
                      )}
                      {auditColumns.isVisible('framework') && (
                        <TableCell sx={auditTableHeadCellSx} width="10%">
                          Framework
                        </TableCell>
                      )}
                      {auditColumns.isVisible('status') && (
                        <TableCell sx={auditTableHeadCellSx} width="16%">
                          Status
                        </TableCell>
                      )}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {auditsPaged.paged.map((a) => (
                      <TableRow
                        key={a.id}
                        hover
                        sx={{ cursor: 'pointer' }}
                        onClick={() => void openAudit(a.id)}
                      >
                        {auditColumns.isVisible('name') && (
                          <TableCell sx={{ ...primaryTableCellSx, width: '18%', whiteSpace: 'normal' }}>
                            <Box sx={primaryRegisterNameSx}>{a.name}</Box>
                            {a.displayId ? (
                              <Typography sx={{ fontSize: '0.6875rem', color: brand.muted, mt: 0.15 }}>
                                {a.displayId}
                              </Typography>
                            ) : null}
                          </TableCell>
                        )}
                        {auditColumns.isVisible('type') && (
                          <TableCell sx={compactTableCellSxWidth('10%')}>{a.auditType}</TableCell>
                        )}
                        {auditColumns.isVisible('assigned') && (
                          <TableCell sx={{ ...mediumTableCellSx, width: '14%', whiteSpace: 'normal' }}>
                            {auditAssignedLabel(a, userLabel)}
                          </TableCell>
                        )}
                        {auditColumns.isVisible('modules') && (
                          <TableCell sx={{ ...mediumTableCellSx, width: '20%', whiteSpace: 'normal' }}>
                            {auditModulesLabel(a.scopeModules)}
                          </TableCell>
                        )}
                        {auditColumns.isVisible('dates') && (
                          <TableCell sx={compactTableCellSxWidth('16%')}>
                            {auditPeriodLabel(a.startDate, a.endDate)}
                          </TableCell>
                        )}
                        {auditColumns.isVisible('framework') && (
                          <TableCell sx={compactTableCellSxWidth('12%')}>
                            <AuditFrameworkLabel audit={a} />
                          </TableCell>
                        )}
                        {auditColumns.isVisible('status') && (
                          <TableCell sx={{ ...compactTableCellSxWidth('16%'), whiteSpace: 'normal' }}>
                            <StatusChip
                              label={auditStatusLabel(a.status, a.findingCount ?? 0)}
                              tone={tone(a.status)}
                            />
                          </TableCell>
                        )}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
              </RegisterListFrame>
              </Box>
            </Box>
          )}

          {pageMode === 'capa-register-create' && (
            <AuditCapaRegisterCreateView
              existingYears={capaRegisters.map((r) => r.year)}
              onChrome={setCapaChrome}
              onCancel={backToList}
              onSaved={(r) => {
                onOk('CAPA register created')
                void openRegister(r)
              }}
              onError={onError}
            />
          )}

          {pageMode === 'capa-create' && selectedRegister && (
            <AuditCapaCreateView
              register={selectedRegister}
              findings={registerFindings}
              users={users}
              orgUnits={orgUnitsFlat.map((u) => ({ id: u.id, name: u.name }))}
              onChrome={setCapaChrome}
              onCancel={backToList}
              onSaved={(c) => {
                onOk('CAPA created')
                void loadRegisterCapas()
                void api.listCapaAvailableFindings().then(setRegisterFindings)
                setSelectedCapa(c)
                setPageMode('list')
              }}
              onError={onError}
            />
          )}

          {pageMode === 'list' && moduleSection === 'capa' && !selectedCapa && !selectedRegister && !contributorOnly && (
            <AuditCapaRegisterList ready={capaRegistersReady} registers={capaRegisters} onSelect={(r) => void openRegister(r)} />
          )}

          {pageMode === 'list' && moduleSection === 'capa' && !selectedCapa && (selectedRegister || contributorOnly) && (
            <Box sx={{ flex: 1, minHeight: 0, overflow: 'hidden', display: 'flex', flexDirection: 'column', p: 1.5 }}>
              <AuditCapaRegisterPanel
                ready={capaRowsReady}
                capas={[...capasInternal, ...capasExternal]}
                users={users}
                audits={audits}
                onSelect={setSelectedCapa}
              />
            </Box>
          )}

          {pageMode === 'list' && moduleSection === 'grants' && canAdmin && (
            <Box sx={{ flex: 1, minHeight: 0, overflow: 'auto', p: 2 }}>
              <AuditAccessGrantsPanel
                audits={audits}
                users={users}
                onOpenAudit={(id) => void openAudit(id)}
                onError={onError}
              />
            </Box>
          )}

          {pageMode === 'list' && moduleSection === 'accessLog' && canAdmin && (
            <Box sx={{ flex: 1, minHeight: 0, overflow: 'auto', p: 2 }}>
              <AuditAccessViewLogPanel onError={onError} />
            </Box>
          )}

          {pageMode === 'list' && moduleSection === 'findings' && !selectedCapa && (
            <Box sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column', p: 2 }}>
              <RegisterToolbar
                search={findingSearch}
                searchPlaceholder="Search findings…"
                onSearchChange={setFindingSearch}
                onClearFilters={clearFindingFilters}
                filters={[
                  {
                    id: 'severity',
                    label: 'Severity',
                    value: findingSeverityFilter,
                    options: findingSeverityOptions.map((o) => ({
                      value: o.value,
                      label: `${o.label} (${o.count})`,
                    })),
                    onChange: (v) => setFindingSeverityFilter(v as FindingSeverityFilter),
                    minWidth: 150,
                  },
                  {
                    id: 'engagement',
                    label: 'Engagement',
                    value: findingAuditFilter === 'ALL' ? 'ALL' : String(findingAuditFilter),
                    options: findingAuditOptions.map((o) => ({
                      value: o.value === 'ALL' ? 'ALL' : String(o.value),
                      label: `${o.label} (${o.count})`,
                    })),
                    onChange: (v) =>
                      setFindingAuditFilter(v === 'ALL' ? 'ALL' : Number(v)),
                    minWidth: 180,
                  },
                ]}
              />
              <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
              <RegisterListFrame
                ready={findingsReady}
                count={findingsPaged.count}
                emptyMessage={
                  findingsRegister.length === 0
                    ? 'No open findings on in-progress audits.'
                    : 'No findings match the current filters.'
                }
                page={findingsPaged.page}
                rowsPerPage={findingsPaged.rowsPerPage}
                onPageChange={findingsPaged.setPage}
                onRowsPerPageChange={findingsPaged.setRowsPerPage}
              >
              <TableContainer sx={{ ...auditTableContainerSx, flex: 1 }}>
                <Table size="small" stickyHeader>
                  <TableHead>
                    <TableRow>
                      <TableCell sx={auditTableHeadCellSx}>Code</TableCell>
                      <TableCell sx={auditTableHeadCellSx}>Severity</TableCell>
                      <RegisterSortHeader
                        label="Title"
                        active={findingSortKey === 'title'}
                        dir={findingSortDir}
                        onClick={() => {
                          const next = toggleSort(findingSortKey, findingSortDir, 'title')
                          setFindingSortKey(next.key)
                          setFindingSortDir(next.dir)
                        }}
                      />
                      <TableCell sx={auditTableHeadCellSx}>Audit</TableCell>
                      <TableCell sx={auditTableHeadCellSx}>Control</TableCell>
                      <TableCell sx={auditTableHeadCellSx}>CAPA</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {findingsPaged.paged.map((f) => (
                      <TableRow
                        key={f.findingId}
                        hover
                        sx={{ cursor: 'pointer' }}
                        onClick={() => void openAudit(f.auditId)}
                      >
                        <TableCell sx={auditControlRefSx}>{f.displayCode}</TableCell>
                        <TableCell>
                          <DomainBadge
                            variant="risk"
                            level={findingSeverityRiskLevel(f.severity)}
                            label={findingSeverityShort(f.severity)}
                          />
                        </TableCell>
                        <TableCell>{f.title}</TableCell>
                        <TableCell>{f.auditName}</TableCell>
                        <TableCell>{f.controlRef ?? '-'}</TableCell>
                        <TableCell>{f.capaId ?? '-'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
              </RegisterListFrame>
              </Box>
            </Box>
          )}

          {pageMode === 'list' && moduleSection === 'capa' && selectedCapa && (
            <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', overflow: 'hidden', p: 1.5 }}>
              <AuditCapaDetailPanel
                capa={selectedCapa}
                users={users}
                canAdmin={canAdmin}
                currentUserId={meId}
                onChrome={setCapaChrome}
                onAssign={() => setAssignCapaId(selectedCapa.id)}
                onClose={() =>
                  api.closeCapa(selectedCapa.id).then((closed) => {
                    onOk('CAPA closed')
                    publishCapa(closed)
                  })
                }
                onReject={(reason) =>
                  api.rejectCapa(selectedCapa.id, reason).then((next) => {
                    onOk('CAPA rejected')
                    publishCapa(next)
                  })
                }
                onSaved={(next, notice) => {
                  onOk(notice ?? 'CAPA tracker saved')
                  publishCapa(next)
                }}
                onError={(msg) => onError(msg)}
              />
            </Box>
          )}
      </Card>
      )}

      <AuditEngagementDialog
        open={engagementEditOpen}
        mode="edit"
        detail={detail}
        users={users}
        frameworks={frameworks}
        gaps={gaps}
        orgUnits={orgUnitsFlat.map((u) => ({ id: u.id, name: u.name, level: u.level }))}
        onClose={() => setEngagementEditOpen(false)}
        onSaved={(d) => {
          onOk('Engagement updated')
          setDetail(d)
          void loadAudits()
        }}
        onError={onError}
      />

      <Dialog
        open={findingOpen}
        onClose={() => {
          if (fTitle.trim() || fDesc.trim() || fControl.trim() || fItemId !== '') {
            setFindingLeaveAsk(true)
            return
          }
          setFindingOpen(false)
        }}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle>Add Finding</DialogTitle>
        <DialogContent>
          <Stack spacing={1.5} sx={{ mt: 1 }}>
            <FormSelect
              label="Classification"
              required
              value={fSev}
              options={SEVERITIES.map((s) => ({ value: s.value, label: s.label }))}
              onChange={(v) => setFSev(v as FindingSeverity)}
            />
            <FormSelect
              label="Checklist line (optional)"
              value={fItemId === '' ? '' : String(fItemId)}
              options={[
                { value: '', label: '-' },
                ...(detail?.checklists?.length
                  ? detail.checklists.flatMap((s) => s.items)
                  : (detail?.checklist ?? [])
                ).map((c) => ({
                  value: String(c.id),
                  label: `${c.nodeCode}: ${c.title}`,
                })),
              ]}
              onChange={(v) => setFItemId(v === '' ? '' : Number(v))}
              minWidth={240}
            />
            <CompactTextField
              label="Title"
              fieldIdPrefix="add-fnd"
              required
              value={fTitle}
              onChange={(e) => setFTitle(e.target.value)}
              maxLength={512}
            />
            <CompactTextField
              label="Description"
              fieldIdPrefix="add-fnd"
              required
              value={fDesc}
              onChange={(e) => setFDesc(e.target.value)}
              multiline
              minRows={3}
            />
            <CompactTextField
              label="Control ref"
              fieldIdPrefix="add-fnd"
              value={fControl}
              onChange={(e) => setFControl(e.target.value)}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => {
              if (fTitle.trim() || fDesc.trim() || fControl.trim() || fItemId !== '') {
                setFindingLeaveAsk(true)
                return
              }
              setFindingOpen(false)
            }}
          >
            Cancel
          </Button>
          <BusyButton
            variant="contained"
            busy={findingBusy}
            busyLabel="Adding..."
            disabled={!fTitle.trim() || !fDesc.trim()}
            disabledReason={
              !fTitle.trim() ? 'Enter a title' : !fDesc.trim() ? 'Enter a description' : undefined
            }
            onClick={() => void addFinding()}
          >
            Add Finding
          </BusyButton>
        </DialogActions>
      </Dialog>
      <DiscardChangesDialog
        open={findingLeaveAsk}
        title="Leave Add finding?"
        message="You have unsaved changes on this finding. Discard them to close."
        onKeepEditing={() => setFindingLeaveAsk(false)}
        onDiscard={() => {
          setFindingLeaveAsk(false)
          setFindingOpen(false)
        }}
      />

      <Dialog
        open={assignCapaId != null}
        onClose={() => {
          setAssignCapaId(null)
          setAssignTargetDate('')
          assignFields.reset()
        }}
        fullWidth
        maxWidth="xs"
      >
        <DialogTitle>Assign CAPA Owner</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <FormSelect
              label="Contributor"
              value={assignOwner === '' ? '' : String(assignOwner)}
              options={contributors.map((u) => ({ value: String(u.id), label: u.fullName }))}
              onChange={(v) => setAssignOwner(Number(v))}
              fullWidth
            />
            <CompactTextField
              label="Target date (optional)"
              type="date"
              fieldIdPrefix="audit"
              InputLabelProps={{ shrink: true }}
              inputProps={{ min: dateMin }}
              value={assignTargetDate}
              onChange={(e) => setAssignTargetDate(e.target.value)}
              onBlur={() => assignFields.touch('targetDate')}
              error={!!assignFields.fieldError('targetDate', assignTargetDateError)}
              helperText={assignFields.fieldError('targetDate', assignTargetDateError)}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => {
              setAssignCapaId(null)
              setAssignTargetDate('')
              assignFields.reset()
            }}
          >
            Cancel
          </Button>
          <Button
            variant="contained"
            disabled={!assignOwner || assignCapaId == null || !!assignTargetDateError}
            onClick={() => {
              if (assignCapaId == null || !assignOwner || assignTargetDateError) return
              assignFields.touchAll()
              const owner = contributors.find((u) => u.id === Number(assignOwner))
              const dept =
                owner?.orgUnitIds
                  ?.map((id) => orgUnitsFlat.find((u) => u.id === id)?.name?.trim())
                  .filter((n): n is string => Boolean(n))
                  .join(', ') || null
              api
                .assignCapa(assignCapaId, {
                  ownerUserId: Number(assignOwner),
                  targetDate: assignTargetDate.trim() || null,
                  contributorDepartment: dept,
                })
                .then(() => {
                  setAssignCapaId(null)
                  setAssignTargetDate('')
                  assignFields.reset()
                  onOk('CAPA assigned')
                  loadCapas()
                })
                .catch((e) => {
                  if (!assignFields.applyApiError(e)) {
                    onError(e instanceof ApiError ? e.message : String(e))
                  }
                })
            }}
          >
            Assign
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}
