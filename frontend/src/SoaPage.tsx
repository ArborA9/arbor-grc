import { useCallback, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import {
  Box,
  Button,
  Card,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputLabel,
  LinearProgress,
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
  type FrameworkSummary,
  type Session,
  type SoaApplicability,
  type SoaDetail,
  type SoaItem,
  type SoaSummary,
} from './api'
import StatusChip from './components/StatusChip'
import SoaControlTable from './components/soa/SoaControlTable'
import SoaItemDrawer from './components/soa/SoaItemDrawer'
import {
  filterSoaItems,
  filterSoaItemsBySection,
  soaCompletionPct,
  soaSectionCounts,
  type SoaApplicabilityFilter,
  type SoaSectionTab,
} from './components/soa/soaUi'
import {
  AttentionBanner,
  BusyButton,
  CompactTextField,
  FilterPillRow,
  FormSelect,
  PageLoading,
  RegisterListFrame,
  RegisterToolbar,
  WorkspaceEmptyPanel,
  NARRATIVE_MAX_ROWS,
  registerTableSx,
  primaryTableCellSx,
  primaryRegisterNameSx,
  compactTableCellSx,
  lastTableCellSx,
} from './components/ui'
import { SOA_REGISTER_COLUMNS } from './components/soa/soaRegisterColumns'
import { useRegisterColumnPicker } from './hooks/useRegisterColumnPicker'
import { brand, semantic } from './theme'
import { frameworkDisplayName, frameworkLabel } from './uiQuality/frameworkLabel'
import { useListReady } from './uiQuality/useListReady'
import { usePagedRows } from './uiQuality/usePagedRows'

function naBlockedReason(frameworkCode: string, items: SoaItem[], targets: SoaItem[]): string | null {
  const frameworkHasNoNa =
    frameworkCode === 'ISO_20000' && items.length > 0 && items.every((item) => item.applicabilityLocked)
  const targetsHaveNoNa = targets.length > 0 && targets.every((item) => item.applicabilityLocked)
  if (!frameworkHasNoNa && !targetsHaveNoNa) return null
  if (frameworkCode === 'ISO_20000') {
    return 'N/A is not allowed for this framework. These controls are always applicable.'
  }
  return 'N/A is not allowed for these controls. They are always applicable.'
}

function ReadOnlyValue({ label, value }: { label: string; value: string }) {
  const text = value.trim() || '-'
  return (
    <Box sx={{ minWidth: 0 }}>
      <Typography sx={{ fontSize: '0.6875rem', fontWeight: 600, color: brand.muted, mb: 0.5, lineHeight: 1.2 }}>
        {label}
      </Typography>
      <Box
        role="textbox"
        aria-readonly="true"
        aria-label={label}
        sx={{
          width: '100%',
          fontSize: '0.75rem',
          lineHeight: 1.4,
          color: text === '-' ? brand.muted : brand.ink,
          bgcolor: brand.hover,
          border: `1px solid ${brand.border}`,
          borderRadius: '6px',
          px: 1,
          minHeight: 32,
          display: 'flex',
          alignItems: 'center',
          py: 0.5,
        }}
      >
        {text}
      </Box>
    </Box>
  )
}

export default function SoaPage({
  session,
  projectId,
  projectFrameworks,
  projectLabel,
  projectName,
  embedded,
  onError,
  onOk,
  onPageActionsChange,
  onHeaderMetaChange,
  initialSoaId,
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
  onPageActionsChange?: (actions: ReactNode | null) => void
  onHeaderMetaChange?: (meta: { title: string; breadcrumbTrail: string } | null) => void
  initialSoaId?: number | null
  startCreate?: boolean
  /** Queue open: this SoA only. No list, no New. Back leaves the queue. */
  recordOnly?: boolean
  onLeaveRecord?: () => void
}) {
  const canEdit = session.roles.some((r) => ['OWNER', 'ADMIN'].includes(r))
  const canExport = session.roles.some((r) => ['OWNER', 'ADMIN'].includes(r))
  const [rows, setRows] = useState<SoaSummary[]>([])
  const [detail, setDetail] = useState<SoaDetail | null>(null)
  const [busy, setBusy] = useState(false)
  const { listReady, markReady } = useListReady()
  const [createOpen, setCreateOpen] = useState(false)
  const [creating, setCreating] = useState(false)
  const [openingDetail, setOpeningDetail] = useState(false)
  const [frameworkId, setFrameworkId] = useState<number | ''>('')
  const [editItem, setEditItem] = useState<SoaItem | null>(null)
  const [applicability, setApplicability] = useState<SoaApplicability>('APPLICABLE')
  const [justification, setJustification] = useState('')
  const [remarks, setRemarks] = useState('')
  const [sectionTab, setSectionTab] = useState<SoaSectionTab>('CLAUSE')
  const [bulkOpen, setBulkOpen] = useState<null | SoaApplicability>(null)
  const [bulkJustification, setBulkJustification] = useState('')
  const [bulkScope, setBulkScope] = useState<'UNANSWERED' | 'ALL' | 'SELECTED'>('UNANSWERED')
  const [selectedIds, setSelectedIds] = useState<Set<number>>(() => new Set())
  const [applicabilityFilter, setApplicabilityFilter] = useState<SoaApplicabilityFilter>('ALL')
  const [soaDiscard, setSoaDiscard] = useState(false)
  const pendingSoaItem = useRef<SoaItem | null>(null)
  const openedInitial = useRef(false)
  const startedCreate = useRef(false)
  const [search, setSearch] = useState('')

  const load = useCallback(async () => {
    setBusy(true)
    try {
      setRows(await api.listSoas(projectId))
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
      markReady()
    }
  }, [onError, projectId, markReady])

  // Any framework whose controls carry a priority gets the column. The name-matching fallback this
  // replaces showed an empty column for UAE IA imports that had no priority data, and hid it for
  // every other framework that did.
  const showPriority = !!detail && detail.items.some((i) => !!i.priority)

  const soaColumnDefs = useMemo(
    () =>
      showPriority
        ? SOA_REGISTER_COLUMNS
        : SOA_REGISTER_COLUMNS.filter((c) => c.id !== 'priority'),
    [showPriority],
  )
  const soaColumns = useRegisterColumnPicker(soaColumnDefs)

  const openDetail = useCallback(async (id: number) => {
    setOpeningDetail(true)
    try {
      const d = await api.getSoa(id)
      setDetail(d)
      const counts = soaSectionCounts(d.items)
      setSectionTab(counts.clause > 0 ? 'CLAUSE' : counts.annex > 0 ? 'ANNEX' : 'CLOUD')
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
    if (recordOnly && !detail) onLeaveRecord?.()
  }

  useEffect(() => {
    void load()
  }, [load])

  const create = async () => {
    if (frameworkId === '' || creating) return
    setCreating(true)
    setBusy(true)
    try {
      const d = await api.createSoa(projectId, frameworkId)
      setDetail(d)
      await load()
      onOk(
        `SoA created for ${frameworkLabel({
          name: d.soa.frameworkName,
          code: d.soa.frameworkCode,
          version: d.soa.frameworkVersion,
        })}`,
      )
      setCreateOpen(false)
      setFrameworkId('')
    } catch (e) {
      onError(String(e))
    } finally {
      setCreating(false)
      setBusy(false)
    }
  }

  const applyEditItem = (item: SoaItem) => {
    setEditItem(item)
    setApplicability(item.applicability ?? 'APPLICABLE')
    setJustification(item.justification ?? '')
    setRemarks(item.remarks ?? '')
  }

  const soaEditDirty = Boolean(
    editItem &&
      (applicability !== (editItem.applicability ?? 'APPLICABLE') ||
        justification !== (editItem.justification ?? '') ||
        remarks !== (editItem.remarks ?? '')),
  )

  const closeSoaDrawer = () => {
    if (busy) return
    if (!soaEditDirty) {
      setEditItem(null)
      pendingSoaItem.current = null
      return
    }
    setSoaDiscard(true)
  }

  const openEdit = (item: SoaItem) => {
    if (editItem && soaEditDirty && editItem.id !== item.id) {
      pendingSoaItem.current = item
      setSoaDiscard(true)
      return
    }
    pendingSoaItem.current = null
    applyEditItem(item)
  }

  const saveItem = async () => {
    if (!detail || !editItem) return
    if (applicability === 'NOT_APPLICABLE' && !justification.trim()) return
    setBusy(true)
    try {
      const latest = await api.updateSoaItem(detail.soa.id, editItem.id, {
        applicability,
        justification: justification.trim(),
        remarks: remarks.trim() || null,
      })
      const next = pendingSoaItem.current
      pendingSoaItem.current = null
      setSoaDiscard(false)
      if (next) applyEditItem(next)
      else setEditItem(null)
      setDetail({
        ...detail,
        items: detail.items.map((i) => (i.id === latest.id ? latest : i)),
      })
      await load()
      onOk('Control updated')
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const complete = async () => {
    if (!detail) return
    setBusy(true)
    try {
      const d = await api.completeSoa(detail.soa.id)
      setDetail(d)
      await load()
      onOk('SoA marked complete')
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
      const d = await api.reopenSoa(detail.soa.id)
      setDetail(d)
      await load()
      onOk('SoA reopened for editing')
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const openBulk = useCallback(
    (next: SoaApplicability, scope: 'UNANSWERED' | 'ALL' | 'SELECTED' = 'UNANSWERED') => {
      setBulkOpen(next)
      setBulkScope(scope)
      setBulkJustification(
        next === 'APPLICABLE' ? 'Bulk marked Applicable' : 'Bulk marked Not applicable',
      )
    },
    [],
  )

  const targetsForScope = (scope: 'UNANSWERED' | 'ALL' | 'SELECTED') => {
    if (!detail) return [] as SoaItem[]
    if (scope === 'SELECTED') return detail.items.filter((item) => selectedIds.has(item.id))
    if (scope === 'UNANSWERED') return detail.items.filter((item) => item.applicability == null)
    return detail.items
  }

  const requestNotApplicable = (scope: 'UNANSWERED' | 'ALL' | 'SELECTED') => {
    if (!detail) return
    const reason = naBlockedReason(detail.soa.frameworkCode, detail.items, targetsForScope(scope))
    if (reason) {
      onError(reason)
      return
    }
    openBulk('NOT_APPLICABLE', scope)
  }

  const runBulk = async () => {
    if (!detail || !bulkOpen) return
    if (bulkOpen === 'NOT_APPLICABLE' && !bulkJustification.trim()) return
    if (bulkScope === 'SELECTED' && selectedIds.size === 0) {
      onError('Select at least one control')
      return
    }
    const targets = targetsForScope(bulkScope)
    if (bulkOpen === 'NOT_APPLICABLE') {
      const reason = naBlockedReason(detail.soa.frameworkCode, detail.items, targets)
      if (reason) {
        onError(reason)
        return
      }
    }
    const itemIds =
      bulkScope === 'SELECTED'
        ? bulkOpen === 'NOT_APPLICABLE'
          ? targets.filter((item) => !item.applicabilityLocked).map((item) => item.id)
          : Array.from(selectedIds)
        : undefined
    if (bulkScope === 'SELECTED' && (!itemIds || itemIds.length === 0)) {
      onError('Select at least one control')
      return
    }
    setBusy(true)
    try {
      const d = await api.bulkUpdateSoaItems(detail.soa.id, {
        applicability: bulkOpen,
        justification: bulkJustification.trim(),
        scope: bulkScope,
        itemIds,
      })
      const selectedCount = itemIds?.length ?? selectedIds.size
      setDetail(d)
      setBulkOpen(null)
      setSelectedIds(new Set())
      await load()
      const label = bulkOpen === 'APPLICABLE' ? 'Applicable' : 'Not applicable'
      onOk(
        bulkScope === 'SELECTED'
          ? `Marked ${selectedCount} selected control${selectedCount === 1 ? '' : 's'} as ${label}`
          : bulkScope === 'UNANSWERED'
            ? `Marked undecided controls as ${label}`
            : `Marked all controls as ${label}`,
      )
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const existingFw = new Set(rows.map((r) => r.frameworkId))
  const createOptions = projectFrameworks.filter((f) => !existingFw.has(f.id))
  const creatingFramework =
    frameworkId === '' ? null : (createOptions.find((f) => f.id === frameworkId) ?? null)
  const creatingFrameworkName = creatingFramework
    ? frameworkLabel(creatingFramework)
    : 'the selected framework'
  const visibleRows = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return rows
    return rows.filter(
      (r) =>
        r.frameworkCode.toLowerCase().includes(q) ||
        r.frameworkName.toLowerCase().includes(q),
    )
  }, [rows, search])
  const soaListPaged = usePagedRows(visibleRows)

  const sectionCounts = useMemo(
    () => (detail ? soaSectionCounts(detail.items) : { clause: 0, annex: 0, cloud: 0 }),
    [detail],
  )

  const sectionItems = useMemo(
    () => (detail ? filterSoaItemsBySection(detail.items, sectionTab) : []),
    [detail, sectionTab],
  )

  const filteredItems = useMemo(
    () => filterSoaItems(sectionItems, applicabilityFilter),
    [sectionItems, applicabilityFilter],
  )

  const toggleApplicabilityFilter = (next: SoaApplicabilityFilter) => {
    setApplicabilityFilter((prev) => (prev === next ? 'ALL' : next))
  }

  useEffect(() => {
    setSelectedIds(new Set())
  }, [detail?.soa.id, sectionTab, applicabilityFilter])

  const toggleSelect = useCallback((itemId: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (next.has(itemId)) next.delete(itemId)
      else next.add(itemId)
      return next
    })
  }, [])

  const toggleSelectAllVisible = useCallback(() => {
    setSelectedIds((prev) => {
      const visibleIds = filteredItems.map((i) => i.id)
      const allOn = visibleIds.length > 0 && visibleIds.every((id) => prev.has(id))
      if (allOn) return new Set()
      return new Set(visibleIds)
    })
  }, [filteredItems])

  const completionPct = detail ? soaCompletionPct(detail.soa) : 0
  const rowClickable = !!(canEdit && detail?.soa.status === 'DRAFT')
  const shellLifted = Boolean(embedded && onPageActionsChange)

  const exportSoa = useCallback(() => {
    if (!detail) return
    void api.downloadSoaExport(detail.soa.id).catch((e) => onError(String(e)))
  }, [detail, onError])

  const openCreate = useCallback((nextFrameworkId?: number) => {
    if (nextFrameworkId != null) setFrameworkId(nextFrameworkId)
    setCreateOpen(true)
  }, [])

  useEffect(() => {
    if (openedInitial.current || initialSoaId == null || !listReady) return
    openedInitial.current = true
    void openDetail(initialSoaId)
  }, [initialSoaId, listReady, openDetail])

  useEffect(() => {
    if (startedCreate.current || !startCreate) return
    startedCreate.current = true
    openCreate()
  }, [startCreate, openCreate])

  useEffect(() => {
    if (!onHeaderMetaChange || !embedded) return undefined
    if (!detail) {
      onHeaderMetaChange({
        title: 'Statement of Applicability',
        breadcrumbTrail: projectLabel ?? '',
      })
    } else {
      const fw = frameworkDisplayName(detail.soa.frameworkName, detail.soa.frameworkCode)
      onHeaderMetaChange({
        title: 'Statement of Applicability',
        breadcrumbTrail: `${fw} · ${detail.soa.totalControls} controls`,
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
    if (canEdit && !recordOnly && createOptions.length > 0) {
      actions.push(
        <Button key="new" variant="outlined" size="small" disabled={busy} onClick={() => openCreate()}>
          New SoA
        </Button>,
      )
    }
    if (detail) {
      if (canExport && detail.soa.status === 'COMPLETE') {
        actions.push(
          <Button key="export" variant="outlined" color="primary" size="small" disabled={busy} onClick={exportSoa}>
            Export SoA
          </Button>,
        )
      }
      if (canEdit && detail.soa.status === 'DRAFT') {
        const completeWhy =
          detail.soa.unansweredCount > 0
            ? `${detail.soa.unansweredCount} control${detail.soa.unansweredCount === 1 ? '' : 's'} not decided`
            : undefined
        actions.push(
          <BusyButton
            key="complete"
            variant="contained"
            size="small"
            busy={busy}
            disabled={busy || Boolean(completeWhy)}
            disabledReason={completeWhy}
            onClick={() => void complete()}
          >
            Complete SoA
          </BusyButton>,
        )
      }
      if (canEdit && detail.soa.status === 'COMPLETE') {
        actions.push(
          <Button
            key="reopen"
            variant="outlined"
            size="small"
            disabled={busy}
            onClick={() => void reopen()}
          >
            Reopen SoA
          </Button>,
        )
      }
    }
    onPageActionsChange(actions.length > 0 ? <>{actions}</> : null)
    return () => onPageActionsChange(null)
  }, [
    onPageActionsChange,
    embedded,
    detail,
    recordOnly,
    canEdit,
    canExport,
    createOptions.length,
    busy,
    closeDetail,
    openCreate,
    exportSoa,
    complete,
    reopen,
  ])

  const selectedNaEligible = detail
    ? detail.items.filter((item) => selectedIds.has(item.id) && !item.applicabilityLocked).length
    : 0
  const selectedNaSkipped = Math.max(0, selectedIds.size - selectedNaEligible)

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
              borderTop: `1px solid ${brand.border}`,
              flexShrink: 0,
            }
          : undefined
      }
    >
      <Box sx={{ flex: 1, minWidth: 0 }}>
        <Typography variant="subtitle2" sx={{ fontWeight: 700 }}>
          SoA
        </Typography>
        <Typography variant="caption" color="text.secondary" display="block">
          One SoA per framework · Applicable / N/A + justification · complete when every row is filled
        </Typography>
      </Box>
      {canEdit && !recordOnly && (
        <Button
          size="small"
          variant="contained"
          disabled={busy || createOptions.length === 0}
          onClick={() => setCreateOpen(true)}
        >
          New SoA
        </Button>
      )}
    </Stack>
  )

  const controlsPanel = (
    <Card
      sx={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        minHeight: 0,
        minWidth: 0,
        overflow: 'hidden',
      }}
    >
      {!detail ? (
        recordOnly || !listReady || openingDetail ? (
          <Box sx={{ p: 3, flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <PageLoading label="Loading SoA…" />
          </Box>
        ) : rows.length === 0 ? (
          <WorkspaceEmptyPanel
            title="No SoA yet"
            message={
              projectFrameworks.length === 0
                ? 'Add a framework to this project first. Then create one SoA per framework.'
                : 'Create one SoA per project framework. You will mark each control Applicable or N/A.'
            }
            steps={
              createOptions.length > 0
                ? [
                    { title: 'Create', detail: 'Use New SoA in the header. One SoA per framework.' },
                    { title: 'Decide', detail: 'Mark each control Applicable or N/A and add justification.' },
                    { title: 'Complete', detail: 'Finish every row, then complete the SoA.' },
                  ]
                : undefined
            }
          />
        ) : (
          <>
            <RegisterToolbar
              search={search}
              searchPlaceholder="Search SoA…"
              onSearchChange={setSearch}
              onClearFilters={search ? () => setSearch('') : undefined}
            />
            <RegisterListFrame
              ready={listReady}
              count={soaListPaged.count}
              emptyMessage="No SoA matches this search."
              page={soaListPaged.page}
              rowsPerPage={soaListPaged.rowsPerPage}
              onPageChange={soaListPaged.setPage}
              onRowsPerPageChange={soaListPaged.setRowsPerPage}
            >
              <TableContainer sx={{ flex: 1 }}>
                <Table size="small" stickyHeader sx={registerTableSx}>
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ ...primaryTableCellSx, width: '24%', pl: 1.5 }}>Framework</TableCell>
                      <TableCell sx={{ ...compactTableCellSx, width: '14%' }}>Status</TableCell>
                      <TableCell sx={{ ...compactTableCellSx, width: '12%' }} align="right">Controls</TableCell>
                      <TableCell sx={{ ...compactTableCellSx, width: '14%' }} align="right">Applicable</TableCell>
                      <TableCell sx={{ ...compactTableCellSx, width: '18%' }} align="right">Not applicable</TableCell>
                      <TableCell sx={{ ...compactTableCellSx, ...lastTableCellSx, width: '16%' }} align="right">
                        Completion
                      </TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {soaListPaged.paged.map((r) => (
                      <TableRow
                        key={r.id}
                        hover
                        sx={{ cursor: 'pointer' }}
                        onClick={() => void openDetail(r.id)}
                      >
                        <TableCell sx={{ ...primaryTableCellSx, width: '28%', pl: 1.5 }}>
                          <Box sx={primaryRegisterNameSx}>{frameworkDisplayName(r.frameworkName, r.frameworkCode)}</Box>
                          <Typography variant="caption" color="text.secondary" display="block" noWrap>
                            {r.frameworkName}
                          </Typography>
                        </TableCell>
                        <TableCell sx={{ ...compactTableCellSx, width: '14%' }}>
                          <StatusChip
                            label={r.status === 'COMPLETE' ? 'Complete' : 'Draft'}
                            tone={r.status === 'COMPLETE' ? 'success' : 'warning'}
                          />
                        </TableCell>
                        <TableCell sx={{ ...compactTableCellSx, width: '14%' }} align="right">
                          {r.totalControls}
                        </TableCell>
                        <TableCell sx={{ ...compactTableCellSx, width: '14%' }} align="right">
                          {r.applicableCount}
                        </TableCell>
                        <TableCell sx={{ ...compactTableCellSx, width: '14%' }} align="right">
                          {r.notApplicableCount}
                        </TableCell>
                        <TableCell sx={{ ...compactTableCellSx, ...lastTableCellSx, width: '16%' }} align="right">
                          {soaCompletionPct(r)}%
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
        <Box sx={{ px: 1.25, pt: 1.25, pb: 1, flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
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
                    sm: 'repeat(3, minmax(0, 1fr))',
                  },
                  gap: 1,
                }}
              >
                <ReadOnlyValue label="Project" value={projectName || projectLabel || ''} />
                <ReadOnlyValue
                  label="Framework"
                  value={frameworkDisplayName(detail.soa.frameworkName, detail.soa.frameworkCode)}
                />
                <ReadOnlyValue label="Controls" value={String(detail.soa.totalControls)} />
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
                  label={detail.soa.status === 'COMPLETE' ? 'Complete' : 'Draft'}
                  tone={detail.soa.status === 'COMPLETE' ? 'success' : 'warning'}
                />
                {!shellLifted && canExport && detail.soa.status === 'COMPLETE' && (
                  <Button size="small" variant="outlined" color="primary" disabled={busy} onClick={exportSoa}>
                    Export SoA
                  </Button>
                )}
                {!shellLifted && canEdit && detail.soa.status === 'DRAFT' && (
                  <BusyButton
                    size="small"
                    variant="contained"
                    busy={busy}
                    disabled={busy || detail.soa.unansweredCount > 0}
                    disabledReason={
                      detail.soa.unansweredCount > 0
                        ? `${detail.soa.unansweredCount} control${detail.soa.unansweredCount === 1 ? '' : 's'} not decided`
                        : undefined
                    }
                    onClick={() => void complete()}
                  >
                    Complete SoA
                  </BusyButton>
                )}
                {!shellLifted && canEdit && detail.soa.status === 'COMPLETE' && (
                  <Button size="small" variant="outlined" disabled={busy} onClick={() => void reopen()}>
                    Reopen SoA
                  </Button>
                )}
              </Stack>
            </Box>
            <Box sx={{ display: 'flex', alignItems: 'flex-end', gap: 3, mt: 1.5, flexWrap: 'wrap' }}>
              <Box sx={{ minWidth: 220, flex: '1 1 280px' }}>
                <Typography sx={{ fontSize: '1.25rem', fontWeight: 700, lineHeight: 1, letterSpacing: '-0.02em' }}>
                  {completionPct}%
                </Typography>
                <Typography sx={{ fontSize: '0.75rem', color: brand.muted, mt: 0.35, mb: 0.75 }}>
                  SoA completion
                  {detail.soa.totalControls > 0
                    ? ` · ${detail.soa.totalControls - detail.soa.unansweredCount} of ${detail.soa.totalControls} decided`
                    : ''}
                </Typography>
                <LinearProgress
                  variant="determinate"
                  value={Math.min(100, Math.max(0, completionPct))}
                  sx={{ height: 8, borderRadius: 4, bgcolor: brand.border, '& .MuiLinearProgress-bar': { bgcolor: brand.gold } }}
                />
              </Box>
              <Stack direction="row" spacing={2.5} flexWrap="wrap" useFlexGap>
                {(
                  [
                    ['Decided', detail.soa.totalControls - detail.soa.unansweredCount, brand.goldDark, 'DECIDED'],
                    ['Applicable', detail.soa.applicableCount, semantic.soa.yes.text, 'APPLICABLE'],
                    ['Not applicable', detail.soa.notApplicableCount, semantic.soa.na.text, 'NOT_APPLICABLE'],
                    ['Not decided', detail.soa.unansweredCount, brand.muted, 'UNANSWERED'],
                  ] as const
                ).map(([label, value, color, filter]) => (
                  <Box
                    key={label}
                    component="button"
                    type="button"
                    onClick={() => toggleApplicabilityFilter(filter)}
                    aria-pressed={applicabilityFilter === filter}
                    sx={{
                      border: 0,
                      bgcolor: 'transparent',
                      p: 0,
                      textAlign: 'left',
                      cursor: 'pointer',
                      font: 'inherit',
                    }}
                  >
                    <Typography sx={{ fontSize: '0.9375rem', fontWeight: 700, color, lineHeight: 1 }}>{value}</Typography>
                    <Typography
                      sx={{
                        fontSize: '0.6875rem',
                        color: applicabilityFilter === filter ? brand.ink : brand.muted,
                        fontWeight: applicabilityFilter === filter ? 700 : 500,
                        mt: 0.35,
                      }}
                    >
                      {label}
                    </Typography>
                  </Box>
                ))}
              </Stack>
            </Box>
          </Box>

          {detail.soa.status === 'COMPLETE' && (
            <AttentionBanner
              dense
              tone="success"
              emphasis="Complete"
              message="This SoA is closed. Reopen if you need to change applicability."
            />
          )}

          <Box
            sx={{
              display: 'flex',
              flexWrap: 'nowrap',
              alignItems: 'center',
              gap: 1.25,
              mt: 1,
              mb: 1,
              flexShrink: 0,
              minWidth: 0,
              width: '100%',
              overflowX: 'auto',
              overflowY: 'hidden',
              WebkitOverflowScrolling: 'touch',
              scrollbarWidth: 'thin',
            }}
          >
            <Box
              sx={{
                display: 'flex',
                flexWrap: 'nowrap',
                alignItems: 'center',
                gap: 0.75,
                flexShrink: 0,
              }}
            >
            {(
              [
                { id: 'CLAUSE' as const, label: 'Clauses', count: sectionCounts.clause },
                { id: 'ANNEX' as const, label: 'Annex', count: sectionCounts.annex },
                ...(sectionCounts.cloud > 0
                  ? [{ id: 'CLOUD' as const, label: 'Cloud', count: sectionCounts.cloud }]
                  : []),
              ] as { id: SoaSectionTab; label: string; count: number }[]
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
                  flexShrink: 0,
                }}
              >
                {t.label} ({t.count})
              </Box>
            ))}
            </Box>
            <Box sx={{ flexShrink: 0, display: 'flex', flexWrap: 'nowrap' }}>
              <FilterPillRow
                value={applicabilityFilter}
                dense
                nowrap
                onChange={setApplicabilityFilter}
                options={[
                  { value: 'ALL', label: 'All', count: sectionItems.length },
                  {
                    value: 'DECIDED',
                    label: 'Decided',
                    count: sectionItems.filter((i) => !!i.applicability).length,
                  },
                  {
                    value: 'UNANSWERED',
                    label: 'Not decided',
                    count: sectionItems.filter((i) => !i.applicability).length,
                    disabled: sectionItems.every((i) => i.applicability),
                  },
                  {
                    value: 'APPLICABLE',
                    label: 'Applicable',
                    count: sectionItems.filter((i) => i.applicability === 'APPLICABLE').length,
                  },
                  {
                    value: 'NOT_APPLICABLE',
                    label: 'N/A',
                    count: sectionItems.filter((i) => i.applicability === 'NOT_APPLICABLE').length,
                  },
                ]}
              />
            </Box>
            <Box
              sx={{
                display: 'flex',
                flexWrap: 'nowrap',
                alignItems: 'center',
                gap: 0.75,
                flexShrink: 0,
                ml: 'auto',
                '& .MuiButton-root': { whiteSpace: 'nowrap', flexShrink: 0 },
              }}
            >
              {soaColumns.columnPicker}
              {canEdit && detail.soa.status === 'DRAFT' ? (
                selectedIds.size > 0 ? (
                  <>
                    <Typography
                      variant="caption"
                      color="text.secondary"
                      sx={{ whiteSpace: 'nowrap' }}
                    >
                      {selectedIds.size} selected
                    </Typography>
                    <Button
                      size="small"
                      variant="outlined"
                      disabled={busy}
                      onClick={() => openBulk('APPLICABLE', 'SELECTED')}
                    >
                      Mark Applicable
                    </Button>
                    <Button
                      size="small"
                      variant="outlined"
                      disabled={busy}
                      onClick={() => requestNotApplicable('SELECTED')}
                    >
                      Mark N/A
                    </Button>
                    <Button
                      size="small"
                      variant="text"
                      disabled={busy}
                      onClick={() => setSelectedIds(new Set())}
                    >
                      Clear
                    </Button>
                  </>
                ) : (
                  <>
                    <Button
                      size="small"
                      variant="outlined"
                      disabled={busy}
                      onClick={() => openBulk('APPLICABLE')}
                    >
                      All Applicable
                    </Button>
                    <Button
                      size="small"
                      variant="outlined"
                      disabled={busy}
                      onClick={() => requestNotApplicable('UNANSWERED')}
                    >
                      All N/A
                    </Button>
                  </>
                )
              ) : null}
            </Box>
          </Box>

          <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>

          <Box sx={{ flex: 1, minHeight: 0, overflow: 'auto' }}>
          <SoaControlTable
            items={filteredItems}
            showPriority={showPriority}
            clickable
            onRowClick={openEdit}
            showApplicabilityHint={detail.soa.frameworkCode !== 'ISO_20000'}
            isVisible={soaColumns.isVisible}
            gridTemplateColumns={soaColumns.gridTemplateColumns}
            selectable={rowClickable}
            selectedIds={selectedIds}
            onToggleSelect={toggleSelect}
            onToggleSelectAll={toggleSelectAllVisible}
          />
          </Box>
          </Box>
        </Box>
      )}
    </Card>
  )

  return (
    <Stack spacing={embedded ? 0 : 1} sx={{ flex: 1, minHeight: 0, height: embedded ? '100%' : undefined }}>
      {!shellLifted && (embedded ? toolbar : <Card sx={{ px: 2, py: 1.25, flexShrink: 0 }}>{toolbar}</Card>)}

      <Box sx={{ flex: 1, minHeight: 0, px: embedded ? 0.75 : 0, pb: embedded ? 0.75 : 0, pt: embedded ? 0.5 : 0, display: 'flex' }}>
        {controlsPanel}
      </Box>

      <Dialog
        open={createOpen}
        onClose={() => dismissCreate()}
        disableEscapeKeyDown={creating}
        fullWidth
        maxWidth="xs"
      >
        <DialogTitle>Create SoA</DialogTitle>
        <DialogContent sx={{ pt: 1 }}>
          <Stack spacing={1.5}>
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
            {creating && (
              <Box role="status" aria-live="polite" aria-busy="true">
                <LinearProgress aria-label="Creating SoA" />
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.75 }}>
                  Adding one SoA row for each control in {creatingFrameworkName}. Large frameworks can
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
            disabled={frameworkId === '' || creating}
            disabledReason={frameworkId === '' ? 'Select a project framework' : undefined}
            onClick={() => void create()}
          >
            {creating ? 'Creating…' : 'Create'}
          </BusyButton>
        </DialogActions>
      </Dialog>

      <SoaItemDrawer
        item={editItem}
        frameworkCode={detail?.soa.frameworkCode}
        items={filteredItems}
        applicability={applicability}
        justification={justification}
        remarks={remarks}
        editable={Boolean(canEdit && detail?.soa.status === 'DRAFT')}
        busy={busy}
        dirty={soaEditDirty}
        discardOpen={soaDiscard}
        onApplicability={setApplicability}
        onJustification={setJustification}
        onRemarks={setRemarks}
        onClose={closeSoaDrawer}
        onSave={() => void saveItem()}
        onOpenItem={openEdit}
        onKeepEditing={() => setSoaDiscard(false)}
        onDiscard={() => {
          const next = pendingSoaItem.current
          pendingSoaItem.current = null
          setSoaDiscard(false)
          if (next) applyEditItem(next)
          else setEditItem(null)
        }}
      />

      <Dialog open={bulkOpen != null} onClose={() => !busy && setBulkOpen(null)} fullWidth maxWidth="xs">
        <DialogTitle>
          {bulkOpen === 'APPLICABLE' ? 'Mark Applicable' : 'Mark Not Applicable'}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={1.5} sx={{ mt: 1 }}>
            <Typography variant="body2" color="text.secondary">
              {bulkScope === 'SELECTED'
                ? bulkOpen === 'NOT_APPLICABLE' && selectedNaSkipped > 0
                  ? `Applies the same justification to ${selectedNaEligible} selected control${
                      selectedNaEligible === 1 ? '' : 's'
                    }. Always-applicable controls in the selection stay Applicable.`
                  : `Applies the same justification to ${selectedIds.size} selected control${
                      selectedIds.size === 1 ? '' : 's'
                    }. You can still edit individual rows afterward.`
                : 'Applies the same justification to every matching control. You can still edit individual rows afterward.'}
            </Typography>
            {bulkScope !== 'SELECTED' && (
              <FormSelect
                label="Which controls"
                fullWidth
                value={bulkScope}
                onChange={(v) => setBulkScope(v as 'UNANSWERED' | 'ALL')}
                options={[
                  {
                    value: 'UNANSWERED',
                    label: `Not decided only (${detail?.soa.unansweredCount ?? 0})`,
                  },
                  {
                    value: 'ALL',
                    label: `All controls (${detail?.soa.totalControls ?? 0}): overwrites existing`,
                  },
                ]}
              />
            )}
            <CompactTextField
              label={
                bulkOpen === 'NOT_APPLICABLE' ? 'Justification' : 'Justification (optional)'
              }
              required={bulkOpen === 'NOT_APPLICABLE'}
              multiline
              minRows={2}
              maxRows={NARRATIVE_MAX_ROWS}
              fullWidth
              value={bulkJustification}
              onChange={(e) => setBulkJustification(e.target.value)}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setBulkOpen(null)} disabled={busy}>
            Cancel
          </Button>
          <Button
            variant="contained"
            disabled={
              busy ||
              (bulkOpen === 'NOT_APPLICABLE' && !bulkJustification.trim()) ||
              (bulkScope === 'SELECTED' && selectedIds.size === 0)
            }
            onClick={() => void runBulk()}
          >
            Apply
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}
