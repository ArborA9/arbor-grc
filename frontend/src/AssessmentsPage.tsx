import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import {
  Box,
  Button,
  Card,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  FormGroup,
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
import {
  api,
  ASSESSMENT_MODULES,
  GAP_ONLY_MODULES,
  SOA_MODULES,
  type ComplianceProjectDetail,
  type ComplianceProjectStatus,
  type ComplianceProjectSummary,
  type FrameworkSummary,
  type GapAssessmentSummary,
  type GapAssessmentType,
  type Session,
  type SoaSummary,
  type UserSummary,
} from './api'
import { isGapAssessor } from './gapAssessor'
import GapAssessmentPage from './GapAssessmentPage'
import SoaPage from './SoaPage'
import StatusChip from './components/StatusChip'
import DiscardChangesDialog from './components/DiscardChangesDialog'
import { BusyButton, CompactTextField, FieldActionRow, FormSelect, RegisterListFrame, RegisterToolbar, registerTableSx, primaryTableCellSx, primaryRegisterNameSx, compactTableCellSx, mediumTableCellSx, lastTableCellSx, compactTableCellSxWidth } from './components/ui'
import { NARRATIVE_MAX_ROWS } from './components/ui/formLayout'
import { resolveGapScoringProfile } from './components/gap/gapUi'
import { actionDueDateError, todayIso, useFormFields } from './validation'
import { useRegisterUnsavedChanges } from './uiQuality/UnsavedChangesGuard'
import { useListReady } from './uiQuality/useListReady'
import { usePagedRows } from './uiQuality/usePagedRows'
import { usePersistedState } from './uiQuality/usePersistedState'
import { useAppShellPage } from './components/AppShellPageContext'
import { ASSESSMENT_PROJECT_COLUMNS, GAP_HOME_COLUMNS, SOA_HOME_COLUMNS } from './hooks/registerColumns'
import { soaCompletionPct } from './components/soa/soaUi'
import { frameworkDisplayName, frameworkLabel } from './uiQuality/frameworkLabel'
import { useRegisterColumnPicker } from './hooks/useRegisterColumnPicker'
import { brand } from './theme'

type View = 'list' | 'detail'
type YearFilter = 'ALL' | string
type StatusFilter = 'ALL' | ComplianceProjectStatus
type FrameworkFilter = 'ALL' | string

function prettyStatus(status: string): string {
  return status
    .replace(/_/g, ' ')
    .toLowerCase()
    .replace(/\b\w/g, (c) => c.toUpperCase())
}

const ASSESSMENT_MODULE_SET = new Set<string>(ASSESSMENT_MODULES)
const GAP_ONLY_MODULE_SET = new Set<string>(GAP_ONLY_MODULES)
const SOA_MODULE_SET = new Set<string>(SOA_MODULES)

function isGapOnlyModule(code: string | null | undefined): boolean {
  return !!code && GAP_ONLY_MODULE_SET.has(code)
}

function isSoaModule(code: string | null | undefined): boolean {
  return !!code && SOA_MODULE_SET.has(code)
}

function isBcmsProject(p: ComplianceProjectSummary, activated: FrameworkSummary[]): boolean {
  if (p.frameworks.length === 0) return false
  return p.frameworks.every((pf) => {
    const fw = activated.find((a) => a.id === pf.frameworkId)
    if (fw) return fw.moduleCode === 'BCMS'
    const code = (pf.frameworkCode || '').toUpperCase()
    return code.includes('NCEMA') || code.includes('22301') || code.includes('ISO_22301')
  })
}

function projectFrameworkIsSoa(
  pf: ComplianceProjectSummary['frameworks'][number],
  activated: FrameworkSummary[],
): boolean {
  const fw = activated.find((a) => a.id === pf.frameworkId)
  if (fw) return isSoaModule(fw.moduleCode)
  const code = (pf.frameworkCode || '').toUpperCase()
  return (
    code.includes('27001') ||
    code.includes('27017') ||
    code.includes('27701') ||
    code.includes('9001') ||
    code.includes('45001') ||
    code.includes('SAMA') ||
    code.includes('UAE_IA') ||
    code.includes('UAEIA')
  )
}

export default function AssessmentsPage({
  session,
  onError,
  onOk,
  focus = 'gap',
  onOpenAssignedGap,
  openFromNotification,
}: {
  session: Session
  onError: (msg: string) => void
  onOk: (msg: string) => void
  /** Req §3.6: Projects vs SoA vs Gap vs Business Continuity → NCEMA. */
  focus?: 'projects' | 'soa' | 'gap' | 'ncema'
  /** Contributor: open the assigned-control questionnaire instead of the Admin Gap workspace. */
  onOpenAssignedGap?: (projectId: number, gapAssessmentId: number | null) => void
  /** Bell / inbox: open this Gap (and control) after the register loads. */
  openFromNotification?: {
    projectId: number
    gapId: number
    itemId: number | null
    epoch: number
  } | null
}) {
  const canEdit = session.roles.some((r) => ['OWNER', 'ADMIN'].includes(r))
  const contributorOnly =
    session.roles.includes('CONTRIBUTOR') &&
    !session.roles.some((r) => ['OWNER', 'ADMIN', 'GLOBAL_READER'].includes(r))
  const [view, setView] = useState<View>('list')
  const projectListMode = contributorOnly || focus === 'projects'
  const workRegister = !contributorOnly && (focus === 'soa' || focus === 'gap' || focus === 'ncema')
  const projectColumns = useRegisterColumnPicker(
    contributorOnly
      ? ASSESSMENT_PROJECT_COLUMNS.filter((c) => c.id !== 'assessments')
      : focus === 'soa'
        ? SOA_HOME_COLUMNS
        : workRegister
          ? GAP_HOME_COLUMNS
          : ASSESSMENT_PROJECT_COLUMNS,
  )
  const [rows, setRows] = useState<ComplianceProjectSummary[]>([])
  const [soaRecords, setSoaRecords] = useState<SoaSummary[]>([])
  const [gapRecords, setGapRecords] = useState<GapAssessmentSummary[]>([])
  const [openSoaId, setOpenSoaId] = useState<number | null>(null)
  const [openGapId, setOpenGapId] = useState<number | null>(null)
  const [openGapItemId, setOpenGapItemId] = useState<number | null>(null)
  const [startCreate, setStartCreate] = useState(false)
  const [createSoaOpen, setCreateSoaOpen] = useState(false)
  const [createSoaProjectId, setCreateSoaProjectId] = useState<number | ''>('')
  const [createSoaFrameworkId, setCreateSoaFrameworkId] = useState<number | ''>('')
  const [creatingSoa, setCreatingSoa] = useState(false)
  const [createGapOpen, setCreateGapOpen] = useState(false)
  const [createGapProjectId, setCreateGapProjectId] = useState<number | ''>('')
  const [createGapFrameworkId, setCreateGapFrameworkId] = useState<number | ''>('')
  const [createGapName, setCreateGapName] = useState('')
  const [createGapType, setCreateGapType] = useState<GapAssessmentType>('INTERNAL')
  const [createGapScope, setCreateGapScope] = useState('')
  const [createGapTargetDate, setCreateGapTargetDate] = useState('')
  const [createGapAssessorId, setCreateGapAssessorId] = useState<number | ''>('')
  const [createGapDepth, setCreateGapDepth] = useState<3 | 4>(4)
  const [creatingGap, setCreatingGap] = useState(false)
  const [createGapUsers, setCreateGapUsers] = useState<UserSummary[]>([])
  const createGapFields = useFormFields<'targetDate'>()
  const [recordStatusFilter, setRecordStatusFilter] = useState('ALL')
  const [detail, setDetail] = useState<ComplianceProjectDetail | null>(null)
  const [search, setSearch] = usePersistedState(`reg-projects-${focus ?? 'all'}-q`, '')
  const [yearFilter, setYearFilter] = useState<YearFilter>('ALL')
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL')
  const [frameworkFilter, setFrameworkFilter] = useState<FrameworkFilter>('ALL')
  const [frameworks, setFrameworks] = useState<FrameworkSummary[]>([])
  const [busy, setBusy] = useState(false)
  const { listReady, markReady } = useListReady()
  const [createOpen, setCreateOpen] = useState(false)
  const [discardCreate, setDiscardCreate] = useState(false)
  const [name, setName] = useState('')
  const defaultYear = new Date().getFullYear()
  const [year, setYear] = useState(defaultYear)
  const yearOptions = useMemo(() => {
    const y = new Date().getFullYear()
    return [y - 1, y, y + 1]
  }, [])
  const [selectedFw, setSelectedFw] = useState<number[]>([])
  const [includeCloud, setIncludeCloud] = useState(false)
  /** When true, do not overwrite includeCloud from framework toggles (G-02). */
  const includeCloudTouched = useRef(false)

  const resetCreateForm = useCallback(() => {
    setName('')
    setYear(new Date().getFullYear())
    setSelectedFw([])
    setIncludeCloud(false)
    includeCloudTouched.current = false
    setDiscardCreate(false)
  }, [])

  const createDirty =
    name.trim() !== '' ||
    selectedFw.length > 0 ||
    year !== new Date().getFullYear() ||
    includeCloudTouched.current

  const requestCloseCreate = () => {
    if (busy) return
    if (!createDirty) {
      resetCreateForm()
      setCreateOpen(false)
      return
    }
    setDiscardCreate(true)
  }

  const discardCreateForm = () => {
    resetCreateForm()
    setCreateOpen(false)
  }
  const [tab, setTab] = useState<'soa' | 'gap'>('soa')
  const [shellChildActions, setShellChildActions] = useState<ReactNode>(null)
  const [shellTrailMeta, setShellTrailMeta] = useState<{
    title: string
    breadcrumbTrail: string
  } | null>(null)

  const selectableFrameworks = useMemo(() => {
    if (focus === 'ncema') return frameworks.filter((f) => f.moduleCode === 'BCMS')
    if (focus === 'soa') return frameworks.filter((f) => isSoaModule(f.moduleCode))
    return frameworks
  }, [frameworks, focus])

  const visibleRows = useMemo(() => {
    if (focus === 'soa') {
      return rows.filter((r) => r.frameworks.some((pf) => projectFrameworkIsSoa(pf, frameworks)))
    }
    if (focus !== 'ncema') return rows
    return rows.filter((r) => isBcmsProject(r, frameworks))
  }, [rows, focus, frameworks])

  const projectById = useMemo(() => {
    const map = new Map<number, ComplianceProjectSummary>()
    for (const p of rows) map.set(p.id, p)
    return map
  }, [rows])

  const soaHomeRows = useMemo(
    () =>
      soaRecords
        .map((soa) => ({ soa, project: projectById.get(soa.projectId) ?? null }))
        .filter((r): r is { soa: SoaSummary; project: ComplianceProjectSummary } => r.project != null),
    [soaRecords, projectById],
  )

  const gapHomeRows = useMemo(
    () =>
      gapRecords
        .map((gap) => ({ gap, project: projectById.get(gap.projectId) ?? null }))
        .filter((r): r is { gap: GapAssessmentSummary; project: ComplianceProjectSummary } => {
          if (!r.project) return false
          if (focus === 'ncema') return isBcmsProject(r.project, frameworks)
          return true
        }),
    [gapRecords, projectById, focus, frameworks],
  )

  const yearFilterOptions = useMemo(() => {
    const years = projectListMode
      ? visibleRows.map((r) => r.year)
      : focus === 'soa'
        ? soaHomeRows.map((r) => r.project.year)
        : gapHomeRows.map((r) => r.project.year)
    const unique = Array.from(new Set(years)).sort((a, b) => b - a)
    const countFor = (y: number) =>
      projectListMode
        ? visibleRows.filter((r) => r.year === y).length
        : focus === 'soa'
          ? soaHomeRows.filter((r) => r.project.year === y).length
          : gapHomeRows.filter((r) => r.project.year === y).length
    const total = projectListMode
      ? visibleRows.length
      : focus === 'soa'
        ? soaHomeRows.length
        : gapHomeRows.length
    return [
      { value: 'ALL' as const, label: `All (${total})` },
      ...unique.map((y) => ({
        value: String(y),
        label: `${y} (${countFor(y)})`,
      })),
    ]
  }, [projectListMode, visibleRows, focus, soaHomeRows, gapHomeRows])

  const statusFilterOptions = useMemo(() => {
    if (projectListMode) {
      const counts: Record<StatusFilter, number> = {
        ALL: visibleRows.length,
        ACTIVE: 0,
        COMPLETE: 0,
        ARCHIVED: 0,
      }
      for (const r of visibleRows) counts[r.status] += 1
      return (['ALL', 'ACTIVE', 'COMPLETE', 'ARCHIVED'] as const).map((v) => ({
        value: v,
        label: `${v === 'ALL' ? 'All' : prettyStatus(v)} (${counts[v]})`,
      }))
    }
    const statuses =
      focus === 'soa'
        ? soaHomeRows.map((r) => r.soa.status)
        : gapHomeRows.map((r) => r.gap.status)
    const unique = Array.from(new Set(statuses))
    return [
      { value: 'ALL', label: `All (${statuses.length})` },
      ...unique.map((s) => ({
        value: s,
        label: `${prettyStatus(s)} (${statuses.filter((x) => x === s).length})`,
      })),
    ]
  }, [projectListMode, visibleRows, focus, soaHomeRows, gapHomeRows])

  const frameworkFilterOptions = useMemo(() => {
    const codes = projectListMode
      ? visibleRows.flatMap((r) => r.frameworks.map((f) => f.frameworkCode))
      : focus === 'soa'
        ? soaHomeRows.map((r) => r.soa.frameworkCode)
        : gapHomeRows.map((r) => r.gap.frameworkCode)
    const unique = Array.from(new Set(codes.filter(Boolean))).sort()
    const countFor = (code: string) =>
      projectListMode
        ? visibleRows.filter((r) => r.frameworks.some((f) => f.frameworkCode === code)).length
        : focus === 'soa'
          ? soaHomeRows.filter((r) => r.soa.frameworkCode === code).length
          : gapHomeRows.filter((r) => r.gap.frameworkCode === code).length
    return [
      { value: 'ALL' as const, label: 'All' },
      ...unique.map((code) => ({
        value: code,
        label: `${frameworkDisplayName(undefined, code)} (${countFor(code)})`,
      })),
    ]
  }, [projectListMode, visibleRows, focus, soaHomeRows, gapHomeRows])

  const filteredRows = useMemo(() => {
    const q = search.trim().toLowerCase()
    return visibleRows.filter((r) => {
      if (q) {
        const inName = r.name.toLowerCase().includes(q)
        const inFw = r.frameworks.some((f) => f.frameworkCode.toLowerCase().includes(q))
        if (!inName && !inFw) return false
      }
      if (yearFilter !== 'ALL' && String(r.year) !== yearFilter) return false
      if (statusFilter !== 'ALL' && r.status !== statusFilter) return false
      if (
        frameworkFilter !== 'ALL' &&
        !r.frameworks.some((f) => f.frameworkCode === frameworkFilter)
      ) {
        return false
      }
      return true
    })
  }, [visibleRows, search, yearFilter, statusFilter, frameworkFilter])
  const projectPaged = usePagedRows(filteredRows)

  const filteredSoaRows = useMemo(() => {
    const q = search.trim().toLowerCase()
    return soaHomeRows.filter((r) => {
      if (q) {
        const hay = `${r.project.name} ${r.soa.frameworkCode} ${r.soa.frameworkName}`.toLowerCase()
        if (!hay.includes(q)) return false
      }
      if (yearFilter !== 'ALL' && String(r.project.year) !== yearFilter) return false
      if (recordStatusFilter !== 'ALL' && r.soa.status !== recordStatusFilter) return false
      if (frameworkFilter !== 'ALL' && r.soa.frameworkCode !== frameworkFilter) return false
      return true
    })
  }, [soaHomeRows, search, yearFilter, recordStatusFilter, frameworkFilter])
  const soaPaged = usePagedRows(filteredSoaRows)

  const filteredGapRows = useMemo(() => {
    const q = search.trim().toLowerCase()
    return gapHomeRows.filter((r) => {
      if (q) {
        const hay = `${r.gap.name} ${r.project.name} ${r.gap.frameworkCode} ${r.gap.frameworkName}`.toLowerCase()
        if (!hay.includes(q)) return false
      }
      if (yearFilter !== 'ALL' && String(r.project.year) !== yearFilter) return false
      if (recordStatusFilter !== 'ALL' && r.gap.status !== recordStatusFilter) return false
      if (frameworkFilter !== 'ALL' && r.gap.frameworkCode !== frameworkFilter) return false
      return true
    })
  }, [gapHomeRows, search, yearFilter, recordStatusFilter, frameworkFilter])
  const gapPaged = usePagedRows(filteredGapRows)

  const clearListFilters = () => {
    setSearch('')
    setYearFilter('ALL')
    setStatusFilter('ALL')
    setRecordStatusFilter('ALL')
    setFrameworkFilter('ALL')
  }

  const pageTitle =
    focus === 'soa'
      ? 'Statement of Applicability'
      : focus === 'ncema'
        ? 'NCEMA'
        : focus === 'projects'
          ? 'Projects'
          : 'Gap Assessments'

  const projectModule = useMemo(() => {
    if (!detail) return null
    const codes = detail.project.frameworks
      .map((f) => frameworks.find((a) => a.id === f.frameworkId)?.moduleCode)
      .filter(Boolean) as string[]
    if (codes.some((c) => isGapOnlyModule(c)) && !codes.includes('ISMS')) {
      return codes.find((c) => isGapOnlyModule(c)) ?? codes[0] ?? null
    }
    if (codes.includes('ISMS')) return 'ISMS'
    // Fallback: treat unknown activated codes via library when list not loaded
    return frameworks.find((a) => detail.project.frameworks.some((f) => f.frameworkId === a.id))
      ?.moduleCode ?? 'ISMS'
  }, [detail, frameworks])

  const isGapOnlyProject = isGapOnlyModule(projectModule) || contributorOnly
  const showProjectWorkspaceTabs =
    focus === 'projects' && !isGapOnlyProject && !contributorOnly

  const loadList = useCallback(async () => {
    setBusy(true)
    try {
      const projects = await api.listComplianceProjects()
      setRows(projects)
      try {
        const activated = await api.listActivatedFrameworks()
        setFrameworks(activated.filter((f) => ASSESSMENT_MODULE_SET.has(f.moduleCode)))
      } catch {
        setFrameworks([])
      }
      if (!contributorOnly) {
        try {
          setSoaRecords(await api.listSoas())
        } catch {
          setSoaRecords([])
        }
        try {
          setGapRecords(await api.listGapAssessments())
        } catch {
          setGapRecords([])
        }
      }
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
      markReady()
    }
  }, [onError, markReady, contributorOnly])

  useEffect(() => {
    void loadList()
  }, [loadList])

  useEffect(() => {
    setView('list')
    setDetail(null)
    setSelectedFw([])
    setSearch('')
    setYearFilter('ALL')
    setStatusFilter('ALL')
    setFrameworkFilter('ALL')
    setRecordStatusFilter('ALL')
    setOpenSoaId(null)
    setOpenGapId(null)
    setOpenGapItemId(null)
    setStartCreate(false)
  }, [focus])

  const openProject = async (
    id: number,
    preferTab?: 'soa' | 'gap',
    opts?: { gapAssessmentId?: number; soaId?: number; startCreate?: boolean; itemId?: number },
  ) => {
    if (onOpenAssignedGap && preferTab === 'gap') {
      onOpenAssignedGap(id, opts?.gapAssessmentId ?? null)
      return
    }
    setBusy(true)
    try {
      const d = await api.getComplianceProject(id)
      setOpenSoaId(opts?.soaId ?? null)
      setOpenGapId(opts?.gapAssessmentId ?? null)
      setOpenGapItemId(opts?.itemId ?? null)
      setStartCreate(Boolean(opts?.startCreate))
      setDetail(d)
      setView('detail')
      const mod = d.project.frameworks
        .map((f) => frameworks.find((a) => a.id === f.frameworkId)?.moduleCode)
        .find(Boolean)
      if (preferTab) {
        setTab(preferTab)
      } else if (focus === 'ncema' || isGapOnlyModule(mod)) {
        setTab('gap')
      } else {
        setTab(focus === 'gap' ? 'gap' : 'soa')
      }
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  useEffect(() => {
    if (!openFromNotification) return
    void openProject(openFromNotification.projectId, 'gap', {
      gapAssessmentId: openFromNotification.gapId,
      itemId: openFromNotification.itemId ?? undefined,
    })
    // epoch is the click identity.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [openFromNotification?.epoch])

  const selectableHasCloud = useMemo(
    () => frameworks.some((f) => selectedFw.includes(f.id) && f.hasCloud),
    [frameworks, selectedFw],
  )

  useEffect(() => {
    if (!selectableHasCloud) {
      setIncludeCloud(false)
      includeCloudTouched.current = false
      return
    }
    // Only auto-default when user has not manually toggled the cloud checkbox
    if (!includeCloudTouched.current) {
      const preferOn = frameworks.some((f) => selectedFw.includes(f.id) && f.includeCloud)
      setIncludeCloud(preferOn)
    }
  }, [selectableHasCloud, selectedFw, frameworks])

  const projectNameTaken = rows.some(
    (r) => r.name.trim().toLowerCase() === name.trim().toLowerCase(),
  )

  const create = async (): Promise<boolean> => {
    if (!name.trim() || selectedFw.length === 0) return false
    if (projectNameTaken) {
      onError(`A project named "${name.trim()}" already exists`)
      return false
    }
    const selectedMods = new Set(
      frameworks.filter((f) => selectedFw.includes(f.id)).map((f) => f.moduleCode),
    )
    if (selectedMods.size > 1) {
      onError('Select frameworks from one module only (ISMS, BCMS, QMS, OHSMS, SAMA, ITSM, AIMS, or PRIVACY)')
      return false
    }
    setBusy(true)
    try {
      const d = await api.createComplianceProject({
        name: name.trim(),
        year,
        frameworkIds: selectedFw,
        includeCloud: selectableHasCloud ? includeCloud : false,
      })
      setCreateOpen(false)
      resetCreateForm()
      onOk(
        d.seededIsRiskCount && d.seededIsRiskCount > 0
          ? `Project “${d.project.name}” created: ${d.seededIsRiskCount} Information Security Risk(s) seeded from curated library (review in Risk Management)`
          : `Project “${d.project.name}” created`,
      )
      await loadList()
      setOpenSoaId(null)
      setOpenGapId(null)
      setOpenGapItemId(null)
      setStartCreate(false)
      setDetail(d)
      setView('detail')
      setTab(
        [...selectedMods].some((m) => isGapOnlyModule(m)) || focus === 'ncema' || focus === 'gap'
          ? 'gap'
          : 'soa',
      )
      return true
    } catch (e) {
      onError(String(e))
      return false
    } finally {
      setBusy(false)
    }
  }

  useRegisterUnsavedChanges(
    createOpen && createDirty && !busy
      ? {
          dirty: true,
          title: 'Leave without creating?',
          message: 'You have started a new project. Create it, discard it, or keep editing.',
          saveDraftLabel: 'Create Project',
          onSaveDraft: async () => {
            const ok = await create()
            if (!ok) throw new Error('Create blocked')
          },
        }
      : null,
  )

  useEffect(() => {
    if (view !== 'detail' || !detail) return
    const mod = detail.project.frameworks
      .map((f) => frameworks.find((a) => a.id === f.frameworkId)?.moduleCode)
      .find(Boolean)
    if (focus === 'projects') {
      if (contributorOnly || isGapOnlyModule(mod)) setTab('gap')
      return
    }
    if (focus === 'soa') {
      setTab('soa')
      return
    }
    setTab('gap')
  }, [focus, view, detail, frameworks, contributorOnly])

  const projectFrameworks: FrameworkSummary[] = useMemo(() => {
    if (!detail) return []
    return detail.project.frameworks.map((f) => {
      const activated = frameworks.find((a) => a.id === f.frameworkId)
      return {
        id: f.frameworkId,
        code: f.frameworkCode,
        name: f.frameworkName,
        version: f.frameworkVersion,
        moduleCode: activated?.moduleCode ?? projectModule ?? 'ISMS',
        status: 'PUBLISHED' as const,
        controlCount: activated?.controlCount ?? 0,
        importedBy: null,
        importedAt: null,
        validatedBy: null,
        validatedAt: null,
        publishedBy: null,
        publishedAt: null,
        sourceFilename: null,
        activated: true,
      }
    })
  }, [detail, frameworks, projectModule])

  const backToProjects = useCallback(() => {
    setView('list')
    setDetail(null)
    setOpenSoaId(null)
    setOpenGapId(null)
    setOpenGapItemId(null)
    setStartCreate(false)
    setShellTrailMeta(null)
    setShellChildActions(null)
    void loadList()
  }, [loadList])

  const soaCreateProjects = useMemo(
    () =>
      visibleRows.filter((p) => {
        const used = new Set(soaRecords.filter((s) => s.projectId === p.id).map((s) => s.frameworkId))
        return p.frameworks.some((pf) => !used.has(pf.frameworkId) && projectFrameworkIsSoa(pf, frameworks))
      }),
    [visibleRows, soaRecords, frameworks],
  )

  const soaCreateFrameworks = useMemo(() => {
    if (createSoaProjectId === '') return []
    const p = rows.find((r) => r.id === createSoaProjectId)
    if (!p) return []
    const used = new Set(soaRecords.filter((s) => s.projectId === p.id).map((s) => s.frameworkId))
    return p.frameworks.filter((pf) => !used.has(pf.frameworkId) && projectFrameworkIsSoa(pf, frameworks))
  }, [createSoaProjectId, rows, soaRecords, frameworks])

  const gapCreateProjects = useMemo(() => {
    const pool = focus === 'ncema' ? visibleRows : rows
    return pool.filter((p) => p.frameworks.length > 0)
  }, [focus, visibleRows, rows])

  const createGapFrameworks = useMemo(() => {
    if (createGapProjectId === '') return []
    const p = rows.find((r) => r.id === createGapProjectId)
    return p?.frameworks ?? []
  }, [createGapProjectId, rows])

  const createGapExistingFw = useMemo(() => {
    if (createGapProjectId === '') return new Set<number>()
    return new Set(
      gapRecords.filter((g) => g.projectId === createGapProjectId).map((g) => g.frameworkId),
    )
  }, [createGapProjectId, gapRecords])

  const createGapFw = createGapFrameworkId === ''
    ? null
    : createGapFrameworks.find((f) => f.frameworkId === createGapFrameworkId) ?? null
  const createGapUaeIa =
    resolveGapScoringProfile(createGapFw?.frameworkCode, createGapFw?.frameworkName) === 'UAE_IA'
  const createGapDateMin = todayIso()
  const createGapTargetDateError = useMemo(
    () => actionDueDateError(createGapTargetDate),
    [createGapTargetDate],
  )

  const resetCreateGapForm = useCallback((projectId: number | '' = '') => {
    setCreateGapProjectId(projectId)
    const p = projectId === '' ? undefined : rows.find((r) => r.id === projectId)
    setCreateGapFrameworkId(p && p.frameworks.length === 1 ? p.frameworks[0].frameworkId : '')
    setCreateGapName('')
    setCreateGapType('INTERNAL')
    setCreateGapScope('')
    setCreateGapTargetDate('')
    setCreateGapAssessorId('')
    setCreateGapDepth(4)
    createGapFields.reset()
  }, [rows, createGapFields.reset])

  const createSoaFromHome = async () => {
    if (createSoaProjectId === '' || createSoaFrameworkId === '' || creatingSoa) return
    setCreatingSoa(true)
    try {
      const d = await api.createSoa(createSoaProjectId, createSoaFrameworkId)
      setCreateSoaOpen(false)
      setCreateSoaProjectId('')
      setCreateSoaFrameworkId('')
      onOk(
        `SoA created for ${frameworkLabel({
          name: d.soa.frameworkName,
          code: d.soa.frameworkCode,
          version: d.soa.frameworkVersion,
        })}`,
      )
      await loadList()
      await openProject(d.soa.projectId, 'soa', { soaId: d.soa.id })
    } catch (e) {
      onError(String(e))
    } finally {
      setCreatingSoa(false)
    }
  }

  const openNewGapFromHome = useCallback(() => {
    resetCreateGapForm(gapCreateProjects.length === 1 ? gapCreateProjects[0].id : '')
    setCreateGapOpen(true)
  }, [gapCreateProjects, resetCreateGapForm])

  useEffect(() => {
    if (!createGapOpen || !canEdit) return
    void api.listUsers().then(setCreateGapUsers).catch(() => setCreateGapUsers([]))
  }, [createGapOpen, canEdit])

  const createGapFromHome = async () => {
    if (
      createGapProjectId === '' ||
      createGapFrameworkId === '' ||
      !createGapName.trim() ||
      !createGapScope.trim() ||
      createGapAssessorId === '' ||
      creatingGap ||
      createGapTargetDateError
    ) {
      return
    }
    setCreatingGap(true)
    try {
      const d = await api.createGapAssessment({
        projectId: createGapProjectId,
        frameworkId: createGapFrameworkId,
        name: createGapName.trim(),
        assessmentType: createGapType,
        targetDate: createGapTargetDate.trim() || null,
        scope: createGapScope.trim(),
        assessorUserId: createGapAssessorId,
        ...(createGapUaeIa ? { assessmentDepth: createGapDepth } : {}),
      })
      setCreateGapOpen(false)
      resetCreateGapForm()
      onOk(
        d.assessment.soaId != null
          ? 'Gap assessment created (applicability from complete SoA)'
          : 'Gap assessment created (all controls Applicable: no SoA required)',
      )
      await loadList()
      await openProject(d.assessment.projectId, 'gap', { gapAssessmentId: d.assessment.id })
    } catch (e) {
      onError(String(e))
    } finally {
      setCreatingGap(false)
    }
  }

  const workspaceTabs = useMemo(() => {
    if (!showProjectWorkspaceTabs) return undefined
    return (
      <Stack direction="row" spacing={0.5}>
        <Button
          size="small"
          variant={tab === 'soa' ? 'contained' : 'outlined'}
          color="primary"
          onClick={() => setTab('soa')}
        >
          SoA
        </Button>
        <Button
          size="small"
          variant={tab === 'gap' ? 'contained' : 'outlined'}
          color="primary"
          onClick={() => setTab('gap')}
        >
          Gap Assessment
        </Button>
      </Stack>
    )
  }, [tab, showProjectWorkspaceTabs])

  const workspaceShell = useMemo(() => {
    if (view !== 'detail' || !detail || (tab !== 'gap' && tab !== 'soa')) {
      return null
    }
    const p = detail.project
    const isGap = tab === 'gap'
    const fallbackTrail =
      projectFrameworks.map((f) => f.name || f.code).join(' · ') ||
      p.frameworks.map((f) => f.frameworkCode).join(' · ') ||
      `${p.name} · ${p.year}`
    return {
      title:
        shellTrailMeta?.title ??
        (isGap ? 'Gap Assessment' : 'Statement of Applicability'),
      breadcrumbTrail: shellTrailMeta?.breadcrumbTrail || fallbackTrail,
      breadcrumbRoot: { label: pageTitle, onClick: backToProjects },
      tabs: workspaceTabs,
      actions: shellChildActions ?? undefined,
      hideContextLabel: true,
      hideRoleChips: true,
      flushContent: true,
    }
  }, [
    view,
    detail,
    tab,
    pageTitle,
    shellTrailMeta,
    backToProjects,
    projectFrameworks,
    workspaceTabs,
    shellChildActions,
  ])

  const listShell = useMemo(() => {
    if (view !== 'list') return null
    const actions: ReactNode[] = []
    const newProjectWhy =
      selectableFrameworks.length === 0 ? 'Activate a framework first' : undefined
    const newSoaWhy =
      soaCreateProjects.length === 0
        ? visibleRows.length === 0
          ? 'Create a compliance project under Projects first'
          : 'Every SoA-capable framework already has a SoA'
        : undefined
    const newGapWhy =
      gapCreateProjects.length === 0
        ? (focus === 'ncema' ? visibleRows : rows).length === 0
          ? 'Create a project under Projects first'
          : 'No project has a framework yet'
        : undefined
    if (canEdit && focus === 'projects') {
      actions.push(
        <BusyButton
          key="project"
          variant="contained"
          size="small"
          disabled={busy || selectableFrameworks.length === 0}
          disabledReason={newProjectWhy}
          onClick={() => {
            resetCreateForm()
            setCreateOpen(true)
          }}
        >
          New Project
        </BusyButton>,
      )
    }
    if (canEdit && focus === 'soa') {
      actions.push(
        <BusyButton
          key="soa"
          variant="contained"
          size="small"
          disabled={busy || soaCreateProjects.length === 0}
          disabledReason={newSoaWhy}
          onClick={() => {
            setCreateSoaProjectId(soaCreateProjects.length === 1 ? soaCreateProjects[0].id : '')
            setCreateSoaFrameworkId('')
            setCreateSoaOpen(true)
          }}
        >
          New SoA
        </BusyButton>,
      )
    }
    if (canEdit && (focus === 'gap' || focus === 'ncema')) {
      actions.push(
        <BusyButton
          key="gap"
          variant="contained"
          size="small"
          busy={creatingGap}
          disabled={busy || gapCreateProjects.length === 0}
          disabledReason={newGapWhy}
          onClick={() => openNewGapFromHome()}
        >
          New Gap Assessment
        </BusyButton>,
      )
    }
    return {
      title: pageTitle,
      breadcrumbTrail: contributorOnly
        ? ''
        : focus === 'soa'
          ? 'SoA Register'
          : focus === 'projects'
            ? 'Compliance Projects'
            : 'Gap register',
      actions: actions.length > 0 ? <Stack direction="row" spacing={0.75}>{actions}</Stack> : undefined,
      hideContextLabel: true,
      hideRoleChips: true,
    }
  }, [
    view,
    pageTitle,
    canEdit,
    busy,
    focus,
    contributorOnly,
    selectableFrameworks.length,
    resetCreateForm,
    soaCreateProjects,
    gapCreateProjects.length,
    visibleRows.length,
    rows.length,
    openNewGapFromHome,
    creatingGap,
  ])

  useEffect(() => {
    if (view !== 'detail' || (tab !== 'gap' && tab !== 'soa')) {
      setShellTrailMeta(null)
      setShellChildActions(null)
    }
  }, [view, tab])

  useEffect(() => {
    if (view === 'detail' && startCreate) {
      setStartCreate(false)
    }
  }, [view, startCreate])

  useAppShellPage(workspaceShell ?? listShell, [workspaceShell, listShell])

  const toggleFw = (id: number) => {
    setSelectedFw((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]))
  }

  if (view === 'detail' && detail) {
    const p = detail.project
    const onSoaTab = focus === 'soa' || (tab === 'soa' && !isGapOnlyProject)
    const queueRecordOnly = focus === 'soa' || focus === 'gap' || focus === 'ncema'

    return (
      <Stack sx={{ flex: 1, minHeight: 0, height: '100%', overflow: 'hidden' }}>
        {onSoaTab ? (
          <SoaPage
            embedded
            session={session}
            projectId={p.id}
            projectLabel={`${p.name} · ${p.year}`}
            projectName={p.name}
            projectFrameworks={projectFrameworks}
            onError={onError}
            onOk={onOk}
            onPageActionsChange={setShellChildActions}
            onHeaderMetaChange={setShellTrailMeta}
            initialSoaId={openSoaId}
            startCreate={startCreate && tab === 'soa'}
            recordOnly={queueRecordOnly}
            onLeaveRecord={backToProjects}
          />
        ) : (
          <GapAssessmentPage
            key={`gap-${openGapId ?? 'none'}-${openGapItemId ?? 'none'}-${openFromNotification?.epoch ?? 0}`}
            embedded
            session={session}
            projectId={p.id}
            projectLabel={`${p.name} · ${p.year}`}
            projectName={p.name}
            projectFrameworks={projectFrameworks}
            onError={onError}
            onOk={onOk}
            onOpenSoa={
              isGapOnlyProject
                ? undefined
                : (soaId) => {
                    setOpenSoaId(soaId)
                    setTab('soa')
                  }
            }
            soaApplicable={!isGapOnlyProject}
            onPageActionsChange={setShellChildActions}
            onHeaderMetaChange={setShellTrailMeta}
            initialGapId={openGapId}
            initialItemId={openGapItemId}
            startCreate={startCreate && tab === 'gap'}
            recordOnly={queueRecordOnly}
            onLeaveRecord={backToProjects}
          />
        )}
      </Stack>
    )
  }

  return (
    <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
      <Card sx={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden' }}>
        <RegisterToolbar
          search={search}
          searchPlaceholder={
            projectListMode
              ? 'Search projects…'
              : focus === 'soa'
                ? 'Search SoA…'
                : 'Search assessments…'
          }
          onSearchChange={setSearch}
          onClearFilters={clearListFilters}
          filters={[
            {
              id: 'year',
              label: 'Year',
              value: yearFilter,
              options: yearFilterOptions,
              onChange: (v) => setYearFilter(v as YearFilter),
              minWidth: 128,
            },
            {
              id: 'status',
              label: 'Status',
              value: projectListMode ? statusFilter : recordStatusFilter,
              options: statusFilterOptions,
              onChange: (v) => {
                if (projectListMode) setStatusFilter(v as StatusFilter)
                else setRecordStatusFilter(v)
              },
              minWidth: 140,
            },
            {
              id: 'framework',
              label: 'Framework',
              value: frameworkFilter,
              options: frameworkFilterOptions,
              onChange: (v) => setFrameworkFilter(v as FrameworkFilter),
              minWidth: 168,
            },
          ]}
          columnPicker={projectColumns.columnPicker}
        />
        <RegisterListFrame
          ready={listReady}
          count={
            projectListMode
              ? projectPaged.count
              : focus === 'soa'
                ? soaPaged.count
                : gapPaged.count
          }
          emptyMessage={
            contributorOnly
              ? visibleRows.length > 0
                ? 'No projects match the current search or filters.'
                : 'No Gap controls are assigned to you yet.'
              : focus === 'projects'
                ? visibleRows.length > 0
                  ? 'No projects match the current search or filters.'
                  : 'No compliance projects yet. Use New Project to create one.'
                : focus === 'soa'
                ? soaHomeRows.length > 0
                  ? 'No SoA matches the current search or filters.'
                  : visibleRows.length === 0
                    ? 'No SoA yet. Create a compliance project under Projects first.'
                    : 'No SoA yet. Use New SoA to add one for a project framework.'
                : gapHomeRows.length > 0
                  ? 'No assessments match the current search or filters.'
                  : 'No Gap Assessments Yet. Create a project under Projects, then New Gap Assessment.'
          }
          page={
            projectListMode ? projectPaged.page : focus === 'soa' ? soaPaged.page : gapPaged.page
          }
          rowsPerPage={
            projectListMode
              ? projectPaged.rowsPerPage
              : focus === 'soa'
                ? soaPaged.rowsPerPage
                : gapPaged.rowsPerPage
          }
          onPageChange={
            projectListMode
              ? projectPaged.setPage
              : focus === 'soa'
                ? soaPaged.setPage
                : gapPaged.setPage
          }
          onRowsPerPageChange={
            projectListMode
              ? projectPaged.setRowsPerPage
              : focus === 'soa'
                ? soaPaged.setRowsPerPage
                : gapPaged.setRowsPerPage
          }
        >
        <TableContainer sx={{ flex: 1 }}>
          <Table size="small" stickyHeader sx={registerTableSx}>
            <TableHead>
              <TableRow>
                {contributorOnly ? (
                  <>
                    {projectColumns.isVisible('project') && (
                      <TableCell sx={{ ...primaryTableCellSx, width: '24%', pl: 1.5 }}>Project</TableCell>
                    )}
                    {projectColumns.isVisible('year') && (
                      <TableCell sx={{ ...compactTableCellSx, width: '10%' }}>Year</TableCell>
                    )}
                    {projectColumns.isVisible('frameworks') && (
                      <TableCell sx={{ ...mediumTableCellSx, width: '22%' }}>Frameworks</TableCell>
                    )}
                    <TableCell sx={{ ...compactTableCellSxWidth('22%'), width: '22%' }} align="right">
                      Assigned to you
                    </TableCell>
                    <TableCell sx={{ ...compactTableCellSx, width: '14%' }} align="right">
                      Needs action
                    </TableCell>
                    {projectColumns.isVisible('status') && (
                      <TableCell sx={{ ...compactTableCellSx, ...lastTableCellSx, width: '20%' }}>Status</TableCell>
                    )}
                  </>
                ) : focus === 'projects' ? (
                  <>
                    {projectColumns.isVisible('project') && (
                      <TableCell sx={{ ...primaryTableCellSx, width: '24%', pl: 1.5 }}>Project</TableCell>
                    )}
                    {projectColumns.isVisible('year') && (
                      <TableCell sx={{ ...compactTableCellSx, width: '10%' }}>Year</TableCell>
                    )}
                    {projectColumns.isVisible('frameworks') && (
                      <TableCell sx={{ ...mediumTableCellSx, width: '22%' }}>Frameworks</TableCell>
                    )}
                    {projectColumns.isVisible('assessments') && (
                      <TableCell sx={{ ...compactTableCellSxWidth('24%'), width: '24%' }}>Assessments</TableCell>
                    )}
                    {projectColumns.isVisible('status') && (
                      <TableCell sx={{ ...compactTableCellSx, ...lastTableCellSx, width: '20%' }}>Status</TableCell>
                    )}
                  </>
                ) : focus === 'soa' ? (
                  <>
                    {projectColumns.isVisible('project') && (
                      <TableCell sx={{ ...primaryTableCellSx, width: '28%', pl: 1.5 }}>Project</TableCell>
                    )}
                    {projectColumns.isVisible('year') && (
                      <TableCell sx={{ ...compactTableCellSx, width: '10%' }}>Year</TableCell>
                    )}
                    {projectColumns.isVisible('framework') && (
                      <TableCell sx={{ ...mediumTableCellSx, width: '28%' }}>Framework</TableCell>
                    )}
                    {projectColumns.isVisible('status') && (
                      <TableCell sx={{ ...compactTableCellSx, width: '16%' }}>Status</TableCell>
                    )}
                    {projectColumns.isVisible('completion') && (
                      <TableCell sx={{ ...compactTableCellSx, ...lastTableCellSx, width: '18%' }} align="right">
                        Completion
                      </TableCell>
                    )}
                  </>
                ) : (
                  <>
                    {projectColumns.isVisible('assessment') && (
                      <TableCell sx={{ ...primaryTableCellSx, width: '24%', pl: 1.5 }}>Assessment</TableCell>
                    )}
                    {projectColumns.isVisible('project') && (
                      <TableCell sx={{ ...mediumTableCellSx, width: '18%' }}>Project</TableCell>
                    )}
                    {projectColumns.isVisible('year') && (
                      <TableCell sx={{ ...compactTableCellSx, width: '8%' }}>Year</TableCell>
                    )}
                    {projectColumns.isVisible('framework') && (
                      <TableCell sx={{ ...compactTableCellSx, width: '12%' }}>Framework</TableCell>
                    )}
                    {projectColumns.isVisible('type') && (
                      <TableCell sx={{ ...compactTableCellSx, width: '10%' }}>Type</TableCell>
                    )}
                    {projectColumns.isVisible('status') && (
                      <TableCell sx={{ ...compactTableCellSx, width: '12%' }}>Status</TableCell>
                    )}
                    {projectColumns.isVisible('score') && (
                      <TableCell sx={{ ...compactTableCellSx, width: '8%' }} align="right">
                        Score
                      </TableCell>
                    )}
                    {projectColumns.isVisible('due') && (
                      <TableCell sx={{ ...compactTableCellSx, ...lastTableCellSx, width: '8%' }}>Due</TableCell>
                    )}
                  </>
                )}
              </TableRow>
            </TableHead>
            <TableBody>
              {contributorOnly
                ? projectPaged.paged.map((r) => (
                    <TableRow
                      key={r.id}
                      hover
                      sx={{ cursor: 'pointer' }}
                      onClick={() => void openProject(r.id, 'gap')}
                    >
                      {projectColumns.isVisible('project') && (
                        <TableCell sx={{ ...primaryTableCellSx, width: '24%', pl: 1.5 }}>
                          <Typography noWrap sx={primaryRegisterNameSx}>
                            {r.name}
                          </Typography>
                        </TableCell>
                      )}
                      {projectColumns.isVisible('year') && (
                        <TableCell sx={{ ...compactTableCellSx, width: '10%' }}>{r.year}</TableCell>
                      )}
                      {projectColumns.isVisible('frameworks') && (
                        <TableCell sx={{ ...mediumTableCellSx, width: '22%' }}>
                          <Typography variant="caption" color="text.secondary" noWrap>
                            {r.frameworks.map((f) => frameworkDisplayName(f.frameworkName, f.frameworkCode)).join(', ') || '-'}
                          </Typography>
                        </TableCell>
                      )}
                      <TableCell sx={{ ...compactTableCellSxWidth('22%'), width: '22%' }} align="right">
                        <Typography variant="caption" color="text.secondary" noWrap>
                          {r.assignedControlCount ?? 0} Gap control
                          {(r.assignedControlCount ?? 0) === 1 ? '' : 's'}
                        </Typography>
                      </TableCell>
                      <TableCell sx={{ ...compactTableCellSx, width: '14%' }} align="right">
                        {(r.needsActionCount ?? 0) > 0 ? (
                          <StatusChip label={String(r.needsActionCount)} tone="warning" />
                        ) : (
                          '0'
                        )}
                      </TableCell>
                      {projectColumns.isVisible('status') && (
                        <TableCell sx={{ ...compactTableCellSx, ...lastTableCellSx, width: '20%' }}>
                          <StatusChip
                            label={r.status}
                            tone={r.status === 'ACTIVE' ? 'success' : r.status === 'COMPLETE' ? 'info' : 'neutral'}
                          />
                        </TableCell>
                      )}
                    </TableRow>
                  ))
                : focus === 'projects'
                  ? projectPaged.paged.map((r) => (
                      <TableRow
                        key={r.id}
                        hover
                        sx={{ cursor: 'pointer' }}
                        onClick={() => void openProject(r.id)}
                      >
                        {projectColumns.isVisible('project') && (
                          <TableCell sx={{ ...primaryTableCellSx, width: '24%', pl: 1.5 }}>
                            <Typography noWrap sx={primaryRegisterNameSx}>
                              {r.name}
                            </Typography>
                          </TableCell>
                        )}
                        {projectColumns.isVisible('year') && (
                          <TableCell sx={{ ...compactTableCellSx, width: '10%' }}>{r.year}</TableCell>
                        )}
                        {projectColumns.isVisible('frameworks') && (
                          <TableCell sx={{ ...mediumTableCellSx, width: '22%' }}>
                            <Typography variant="caption" color="text.secondary" noWrap>
                              {r.frameworks.map((f) => frameworkDisplayName(f.frameworkName, f.frameworkCode)).join(', ') || '-'}
                            </Typography>
                          </TableCell>
                        )}
                        {projectColumns.isVisible('assessments') && (
                          <TableCell sx={{ ...compactTableCellSxWidth('24%'), width: '24%' }}>
                            <Stack direction="row" spacing={1.25} sx={{ minWidth: 0 }}>
                              <Typography variant="caption" color="text.secondary" noWrap>
                                <Box component="span" sx={{ fontWeight: 650, color: brand.ink }}>
                                  {r.soaCount ?? 0}
                                </Box>
                                {' '}SoA
                              </Typography>
                              <Typography variant="caption" color="text.secondary" noWrap>
                                <Box component="span" sx={{ fontWeight: 650, color: brand.ink }}>
                                  {r.gapAssessmentCount ?? 0}
                                </Box>
                                {' '}Gap
                              </Typography>
                            </Stack>
                          </TableCell>
                        )}
                        {projectColumns.isVisible('status') && (
                          <TableCell sx={{ ...compactTableCellSx, ...lastTableCellSx, width: '20%' }}>
                            <StatusChip
                              label={r.status}
                              tone={r.status === 'ACTIVE' ? 'success' : r.status === 'COMPLETE' ? 'info' : 'neutral'}
                            />
                          </TableCell>
                        )}
                      </TableRow>
                    ))
                : focus === 'soa'
                  ? soaPaged.paged.map((r) => (
                      <TableRow
                        key={r.soa.id}
                        hover
                        sx={{ cursor: 'pointer' }}
                        onClick={() => void openProject(r.project.id, 'soa', { soaId: r.soa.id })}
                      >
                        {projectColumns.isVisible('project') && (
                          <TableCell sx={{ ...primaryTableCellSx, width: '28%', pl: 1.5 }}>
                            <Typography noWrap sx={primaryRegisterNameSx}>
                              {r.project.name}
                            </Typography>
                          </TableCell>
                        )}
                        {projectColumns.isVisible('year') && (
                          <TableCell sx={{ ...compactTableCellSx, width: '10%' }}>{r.project.year}</TableCell>
                        )}
                        {projectColumns.isVisible('framework') && (
                          <TableCell sx={{ ...mediumTableCellSx, width: '28%' }}>
                            {frameworkDisplayName(r.soa.frameworkName, r.soa.frameworkCode)}
                          </TableCell>
                        )}
                        {projectColumns.isVisible('status') && (
                          <TableCell sx={{ ...compactTableCellSx, width: '16%' }}>
                            <StatusChip
                              label={r.soa.status === 'COMPLETE' ? 'Complete' : 'Draft'}
                              tone={r.soa.status === 'COMPLETE' ? 'success' : 'warning'}
                            />
                          </TableCell>
                        )}
                        {projectColumns.isVisible('completion') && (
                          <TableCell sx={{ ...compactTableCellSx, ...lastTableCellSx, width: '18%' }} align="right">
                            {soaCompletionPct(r.soa)}%
                          </TableCell>
                        )}
                      </TableRow>
                    ))
                  : gapPaged.paged.map((r) => (
                      <TableRow
                        key={r.gap.id}
                        hover
                        sx={{ cursor: 'pointer' }}
                        onClick={() =>
                          void openProject(r.project.id, 'gap', { gapAssessmentId: r.gap.id })
                        }
                      >
                        {projectColumns.isVisible('assessment') && (
                          <TableCell sx={{ ...primaryTableCellSx, width: '24%', pl: 1.5 }}>
                            <Typography noWrap sx={primaryRegisterNameSx}>
                              {r.gap.name}
                            </Typography>
                          </TableCell>
                        )}
                        {projectColumns.isVisible('project') && (
                          <TableCell sx={{ ...mediumTableCellSx, width: '18%' }}>{r.project.name}</TableCell>
                        )}
                        {projectColumns.isVisible('year') && (
                          <TableCell sx={{ ...compactTableCellSx, width: '8%' }}>{r.project.year}</TableCell>
                        )}
                        {projectColumns.isVisible('framework') && (
                          <TableCell sx={{ ...compactTableCellSx, width: '12%' }}>{frameworkDisplayName(r.gap.frameworkName, r.gap.frameworkCode)}</TableCell>
                        )}
                        {projectColumns.isVisible('type') && (
                          <TableCell sx={{ ...compactTableCellSx, width: '10%' }}>
                            {prettyStatus(r.gap.assessmentType)}
                          </TableCell>
                        )}
                        {projectColumns.isVisible('status') && (
                          <TableCell sx={{ ...compactTableCellSx, width: '12%' }}>
                            <StatusChip
                              label={prettyStatus(r.gap.status)}
                              tone={
                                r.gap.status === 'COMPLETED'
                                  ? 'success'
                                  : r.gap.status === 'IN_PROGRESS'
                                    ? 'info'
                                    : 'warning'
                              }
                            />
                          </TableCell>
                        )}
                        {projectColumns.isVisible('score') && (
                          <TableCell sx={{ ...compactTableCellSx, width: '8%' }} align="right">
                            {r.gap.overallScore != null
                              ? `${r.gap.overallScore}%`
                              : `${r.gap.scoredCount}/${r.gap.applicableCount}`}
                          </TableCell>
                        )}
                        {projectColumns.isVisible('due') && (
                          <TableCell sx={{ ...compactTableCellSx, ...lastTableCellSx, width: '8%' }}>
                            {r.gap.targetDate || '-'}
                          </TableCell>
                        )}
                      </TableRow>
                    ))}
            </TableBody>
          </Table>
        </TableContainer>
        </RegisterListFrame>
      </Card>

      <Dialog
        open={createSoaOpen}
        onClose={() => {
          if (!creatingSoa) setCreateSoaOpen(false)
        }}
        disableEscapeKeyDown={creatingSoa}
        fullWidth
        maxWidth="xs"
      >
        <DialogTitle>New SoA</DialogTitle>
        <DialogContent>
          <Stack spacing={1.5} sx={{ mt: 1 }}>
            <FormSelect
              label="Project"
              required
              fullWidth
              value={createSoaProjectId === '' ? '' : String(createSoaProjectId)}
              onChange={(v) => {
                setCreateSoaProjectId(v === '' ? '' : Number(v))
                setCreateSoaFrameworkId('')
              }}
              disabled={creatingSoa}
              options={[
                { value: '', label: 'Select a project' },
                ...soaCreateProjects.map((p) => ({
                  value: String(p.id),
                  label: `${p.name} · ${p.year}`,
                })),
              ]}
            />
            <FormSelect
              label="Framework"
              required
              fullWidth
              value={createSoaFrameworkId === '' ? '' : String(createSoaFrameworkId)}
              onChange={(v) => setCreateSoaFrameworkId(v === '' ? '' : Number(v))}
              disabled={creatingSoa || createSoaProjectId === ''}
              options={[
                { value: '', label: createSoaProjectId === '' ? 'Select a project first' : 'Select a framework' },
                ...soaCreateFrameworks.map((f) => ({
                  value: String(f.frameworkId),
                  label: frameworkDisplayName(f.frameworkName, f.frameworkCode),
                })),
              ]}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateSoaOpen(false)} disabled={creatingSoa}>
            Cancel
          </Button>
          <BusyButton
            variant="contained"
            busy={creatingSoa}
            disabled={createSoaProjectId === '' || createSoaFrameworkId === '' || creatingSoa}
            disabledReason={
              createSoaProjectId === ''
                ? 'Select a project'
                : createSoaFrameworkId === ''
                  ? 'Select a framework'
                  : undefined
            }
            onClick={() => void createSoaFromHome()}
          >
            Create
          </BusyButton>
        </DialogActions>
      </Dialog>

      <Dialog
        open={createGapOpen}
        onClose={() => {
          if (!creatingGap) {
            setCreateGapOpen(false)
            resetCreateGapForm()
          }
        }}
        disableEscapeKeyDown={creatingGap}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle>New Gap Assessment</DialogTitle>
        <DialogContent>
          <Stack spacing={1.5} sx={{ mt: 1 }}>
            <FormSelect
              label="Project"
              required
              fullWidth
              value={createGapProjectId === '' ? '' : String(createGapProjectId)}
              onChange={(v) => {
                const id = v === '' ? '' : Number(v)
                setCreateGapProjectId(id)
                const p = id === '' ? undefined : rows.find((r) => r.id === id)
                setCreateGapFrameworkId(p && p.frameworks.length === 1 ? p.frameworks[0].frameworkId : '')
                setCreateGapDepth(4)
              }}
              disabled={creatingGap}
              options={[
                { value: '', label: 'Select a project' },
                ...gapCreateProjects.map((p) => ({
                  value: String(p.id),
                  label: `${p.name} · ${p.year}`,
                })),
              ]}
            />
            <FormSelect
              label="Framework"
              required
              fullWidth
              value={createGapFrameworkId === '' ? '' : String(createGapFrameworkId)}
              onChange={(v) => {
                setCreateGapFrameworkId(v === '' ? '' : Number(v))
                setCreateGapDepth(4)
              }}
              disabled={creatingGap || createGapProjectId === ''}
              options={[
                {
                  value: '',
                  label: createGapProjectId === '' ? 'Select a project first' : 'Select a framework',
                },
                ...createGapFrameworks.map((f) => ({
                  value: String(f.frameworkId),
                  label: frameworkDisplayName(f.frameworkName, f.frameworkCode),
                })),
              ]}
            />
            {(() => {
              const lines = [
                createGapUaeIa
                  ? ''
                  : 'SoA is optional. If a complete SoA exists for the framework, applicability is copied; otherwise every control starts as Applicable and you set N/A on the Gap.',
                createGapFrameworkId !== '' && createGapExistingFw.has(createGapFrameworkId)
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
                  value={createGapName}
                  onChange={(e) => setCreateGapName(e.target.value)}
                  placeholder="e.g. Jul 2026 self-assessment"
                  disabled={creatingGap}
                />
              </Box>
              <FormSelect
                label="Type"
                value={createGapType}
                onChange={(v) => setCreateGapType(v as GapAssessmentType)}
                disabled={creatingGap}
                options={[
                  { value: 'INTERNAL', label: 'Internal' },
                  { value: 'EXTERNAL', label: 'External' },
                ]}
              />
            </FieldActionRow>
            {createGapUaeIa ? (
              <>
                <FormSelect
                  label="Assessment depth"
                  value={String(createGapDepth)}
                  onChange={(v) => setCreateGapDepth(v === '3' ? 3 : 4)}
                  disabled={creatingGap}
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
            ) : null}
            <CompactTextField
              label="Scope"
              fullWidth
              required
              multiline
              minRows={2}
              maxRows={NARRATIVE_MAX_ROWS}
              value={createGapScope}
              onChange={(e) => setCreateGapScope(e.target.value)}
              placeholder="Org units, systems, or locations in scope"
              disabled={creatingGap}
            />
            <FieldActionRow>
              <Box sx={{ flex: 1, minWidth: 0 }}>
                <FormSelect
                  label="Assessor"
                  fullWidth
                  required
                  value={createGapAssessorId === '' ? '' : String(createGapAssessorId)}
                  onChange={(v) => setCreateGapAssessorId(v === '' ? '' : Number(v))}
                  disabled={creatingGap}
                  hint="Admin or Owner"
                  options={[
                    { value: '', label: '-' },
                    ...createGapUsers.filter(isGapAssessor).map((u) => ({
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
                inputProps={{ min: createGapDateMin }}
                value={createGapTargetDate}
                onChange={(e) => setCreateGapTargetDate(e.target.value)}
                error={Boolean(createGapFields.fieldError('targetDate', createGapTargetDateError))}
                helperText={createGapFields.fieldError('targetDate', createGapTargetDateError) || undefined}
                onBlur={() => createGapFields.touch('targetDate')}
                disabled={creatingGap}
              />
            </FieldActionRow>
            {creatingGap && (
              <Box role="status" aria-live="polite" aria-busy="true">
                <LinearProgress aria-label="Creating gap assessment" />
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.75 }}>
                  Adding one Gap row for each control in{' '}
                  {frameworkDisplayName(createGapFw?.frameworkName, createGapFw?.frameworkCode) || 'the selected framework'}.
                  Large frameworks can take a minute or two. Stay on this page.
                </Typography>
              </Box>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => {
              setCreateGapOpen(false)
              resetCreateGapForm()
            }}
            disabled={creatingGap}
          >
            Cancel
          </Button>
          <BusyButton
            variant="contained"
            busy={creatingGap}
            disabled={
              createGapProjectId === '' ||
              createGapFrameworkId === '' ||
              !createGapName.trim() ||
              !createGapScope.trim() ||
              createGapAssessorId === '' ||
              creatingGap ||
              Boolean(createGapTargetDateError)
            }
            disabledReason={
              createGapProjectId === ''
                ? 'Select a project'
                : createGapFrameworkId === ''
                  ? 'Select a framework'
                  : !createGapName.trim()
                    ? 'Enter an assessment name'
                    : !createGapScope.trim()
                      ? 'Enter a scope'
                      : createGapAssessorId === ''
                        ? 'Select an assessor'
                        : createGapTargetDateError || undefined
            }
            onClick={() => void createGapFromHome()}
          >
            {creatingGap ? 'Creating…' : 'Create'}
          </BusyButton>
        </DialogActions>
      </Dialog>

      <Dialog
        open={createOpen}
        onClose={() => requestCloseCreate()}
        disableEscapeKeyDown={busy}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle>New Compliance Project</DialogTitle>
        <DialogContent>
          <Stack spacing={1.5} sx={{ mt: 1 }}>
            <CompactTextField
              label="Name"
              required
              fullWidth
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder={focus === 'ncema' ? 'NCEMA 2026' : 'ISMS 2026'}
              error={projectNameTaken}
              helperText={
                projectNameTaken ? `A project named "${name.trim()}" already exists` : undefined
              }
            />
            <FormSelect
              label="Year"
              fullWidth
              value={String(year)}
              onChange={(v) => setYear(Number(v))}
              options={yearOptions.map((y) => ({ value: String(y), label: String(y) }))}
            />
            <Box>
              <Typography variant="caption" color="text.secondary" sx={{ mb: 0.5, display: 'block' }}>
                {focus === 'ncema'
                  ? 'Activated BCMS frameworks (NCEMA V3 / ISO 22301)'
                  : focus === 'soa'
                    ? 'Activated SoA frameworks (ISMS, QMS, OHSMS, SAMA, ITSM, AIMS, PRIVACY)'
                    : 'Activated frameworks (one module per project)'}
              </Typography>
              <FormGroup>
                {selectableFrameworks.map((f) => (
                  <Box key={f.id}>
                    <FormControlLabel
                      control={
                        <Checkbox
                          checked={selectedFw.includes(f.id)}
                          onChange={() => toggleFw(f.id)}
                          size="small"
                        />
                      }
                      label={frameworkDisplayName(f.name, f.code)}
                    />
                    {f.hasCloud && selectedFw.includes(f.id) ? (
                      <FormControlLabel
                        sx={{ ml: 3, alignItems: 'flex-start' }}
                        control={
                          <Checkbox
                            size="small"
                            checked={includeCloud}
                            onChange={(e) => {
                              includeCloudTouched.current = true
                              setIncludeCloud(e.target.checked)
                            }}
                          />
                        }
                        label="Include cloud controls (ISO 27017 sheet) in SoA and Gap"
                      />
                    ) : null}
                  </Box>
                ))}
              </FormGroup>
            </Box>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={requestCloseCreate} disabled={busy}>
            Cancel
          </Button>
          <BusyButton
            variant="contained"
            busy={busy}
            disabled={!name.trim() || selectedFw.length === 0 || busy || projectNameTaken}
            disabledReason={
              !name.trim()
                ? 'Enter a project name'
                : selectedFw.length === 0
                  ? 'Select at least one framework'
                  : projectNameTaken
                    ? `A project named "${name.trim()}" already exists`
                    : undefined
            }
            onClick={() => void create()}
          >
            Create
          </BusyButton>
        </DialogActions>
      </Dialog>
      <DiscardChangesDialog
        open={discardCreate}
        title="Discard this project?"
        message="You have started a new project. Create it, discard it, or keep editing."
        saveDraftLabel="Create Project"
        onSaveDraft={() => {
          void (async () => {
            const ok = await create()
            if (ok) setDiscardCreate(false)
          })()
        }}
        onKeepEditing={() => setDiscardCreate(false)}
        onDiscard={discardCreateForm}
      />
    </Stack>
  )
}
