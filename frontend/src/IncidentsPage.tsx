import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Box,
  Button,
  Card,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
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
  type CreateIncidentBody,
  type GapAssessmentSummary,
  type IncidentPriority,
  type IncidentRecord,
  type IncidentType,
  type ReportIncidentBody,
  type RiskSummary,
  type Session,
  type UserSummary,
} from './api'
import StatusChip from './components/StatusChip'
import { incidentPriorityToBadge } from './components/ui/riskBadgeMappers'
import { BusyButton, CompactTextField, FieldActionRow, FormSelect, RegisterListFrame, RegisterSortHeader, RegisterToolbar, RiskLevelBadge, controlActionSx, apiLevelTone } from './components/ui'
import { useListReady } from './uiQuality/useListReady'
import { usePagedRows } from './uiQuality/usePagedRows'
import { usePersistedState } from './uiQuality/usePersistedState'
import { compareLoose, toggleSort, type SortDir } from './uiQuality/sortRows'
import { INCIDENT_REGISTER_COLUMNS } from './hooks/registerColumns'
import { useRegisterColumnPicker } from './hooks/useRegisterColumnPicker'
import { ModuleDetailShell, ModuleListShell } from './hooks/useModulePageShell'

type IncidentListFilter = 'ALL' | 'OPEN' | 'P1' | 'CLOSED'

const TYPES: IncidentType[] = ['SECURITY', 'OPERATIONAL', 'DATA_BREACH', 'PRIVACY', 'PHYSICAL', 'OTHER']
const PRIORITIES: IncidentPriority[] = ['P1', 'P2', 'P3', 'P4']

function statusTone(s: string): 'success' | 'warning' | 'info' | 'danger' | 'neutral' {
  if (s === 'CLOSED') return 'success'
  if (s === 'RESOLVED') return 'info'
  if (s === 'ASSIGNED') return 'warning'
  return 'neutral'
}

const emptyCreate = (): CreateIncidentBody => ({
  title: '',
  incidentType: 'SECURITY',
  description: '',
  occurredAt: null,
  affectedArea: '',
  priority: 'P2',
  linkedRiskId: null,
  linkedGapAssessmentId: null,
  orgUnitId: null,
  ownerUserId: null,
  rootCause: null,
  containmentActions: null,
})

const emptyReport = (): ReportIncidentBody => ({
  title: '',
  incidentType: 'SECURITY',
  description: '',
  occurredAt: null,
  affectedArea: '',
})

export default function IncidentsPage({
  session,
  onError,
  onOk,
}: {
  session: Session
  onError: (msg: string) => void
  onOk: (msg: string) => void
}) {
  const canAdmin = session.roles.some((r) => ['OWNER', 'ADMIN'].includes(r))
  const canReport = session.roles.some((r) => ['OWNER', 'ADMIN', 'CONTRIBUTOR'].includes(r))
  const [rows, setRows] = useState<IncidentRecord[]>([])
  const [users, setUsers] = useState<UserSummary[]>([])
  const [riskOptions, setRiskOptions] = useState<RiskSummary[]>([])
  const [gapOptions, setGapOptions] = useState<GapAssessmentSummary[]>([])
  const [selected, setSelected] = useState<IncidentRecord | null>(null)
  const incidentColumns = useRegisterColumnPicker(INCIDENT_REGISTER_COLUMNS)
  const [busy, setBusy] = useState(false)

  const [createOpen, setCreateOpen] = useState(false)
  const [reportOpen, setReportOpen] = useState(false)
  const [createForm, setCreateForm] = useState<CreateIncidentBody>(emptyCreate())
  const [reportForm, setReportForm] = useState<ReportIncidentBody>(emptyReport())
  const [assignUserId, setAssignUserId] = useState<number | ''>('')
  const [resolveNotes, setResolveNotes] = useState('')
  const [listFilter, setListFilter] = usePersistedState<IncidentListFilter>('reg-incidents-filter', 'ALL')
  const { listReady, markReady } = useListReady()
  const [sortKey, setSortKey] = useState('title')
  const [sortDir, setSortDir] = useState<SortDir>('asc')

  const filteredRows = useMemo(() => {
    let next = rows
    if (listFilter === 'OPEN') next = rows.filter((r) => r.status !== 'CLOSED' && r.status !== 'RESOLVED')
    else if (listFilter === 'P1') next = rows.filter((r) => r.priority === 'P1')
    else if (listFilter === 'CLOSED') next = rows.filter((r) => r.status === 'CLOSED')
    return [...next].sort((a, b) => compareLoose(a.title, b.title, sortDir))
  }, [rows, listFilter, sortDir])
  const paged = usePagedRows(filteredRows)

  const incidentFilterOptions = useMemo(
    () => [
      { value: 'ALL' as const, label: 'All', count: rows.length },
      {
        value: 'OPEN' as const,
        label: 'Open',
        count: rows.filter((r) => r.status !== 'CLOSED' && r.status !== 'RESOLVED').length,
      },
      { value: 'P1' as const, label: 'P1', count: rows.filter((r) => r.priority === 'P1').length },
      { value: 'CLOSED' as const, label: 'Closed', count: rows.filter((r) => r.status === 'CLOSED').length },
    ],
    [rows],
  )

  const load = useCallback(async () => {
    try {
      setRows(await api.listIncidents())
    } catch (e) {
      onError(String(e))
    } finally {
      markReady()
    }
  }, [onError, markReady])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    if (!canAdmin) return
    void api.listUsers().then(setUsers).catch(() => undefined)
    void Promise.all([
      api.listRisks('IS_RISK').catch(() => [] as RiskSummary[]),
      api.listRisks('ERM').catch(() => [] as RiskSummary[]),
      api.listGapAssessments().catch(() => [] as GapAssessmentSummary[]),
    ]).then(([is, erm, gaps]) => {
      setRiskOptions([...is, ...erm])
      setGapOptions(gaps)
    })
  }, [canAdmin])

  const openDetail = async (id: number) => {
    try {
      setSelected(await api.getIncident(id))
    } catch (e) {
      onError(String(e))
    }
  }

  const create = async () => {
    if (!createForm.title.trim()) return
    setBusy(true)
    try {
      const created = await api.createIncident({
        ...createForm,
        title: createForm.title.trim(),
        description: createForm.description?.trim() || null,
        affectedArea: createForm.affectedArea?.trim() || null,
        occurredAt: createForm.occurredAt || null,
        ownerUserId: createForm.ownerUserId || null,
      })
      setCreateOpen(false)
      setCreateForm(emptyCreate())
      onOk(`Created ${created.displayId}`)
      await load()
      setSelected(created)
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const report = async () => {
    if (!reportForm.title.trim()) return
    setBusy(true)
    try {
      const created = await api.reportIncident({
        ...reportForm,
        title: reportForm.title.trim(),
        description: reportForm.description?.trim() || null,
        affectedArea: reportForm.affectedArea?.trim() || null,
        occurredAt: reportForm.occurredAt || null,
      })
      setReportOpen(false)
      setReportForm(emptyReport())
      onOk(`Reported ${created.displayId}`)
      await load()
      setSelected(created)
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const assign = async () => {
    if (!selected || assignUserId === '') return
    setBusy(true)
    try {
      const updated = await api.assignIncident(selected.id, { userId: Number(assignUserId) })
      onOk(`Assigned ${updated.displayId}`)
      setSelected(updated)
      await load()
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const resolve = async () => {
    if (!selected || !resolveNotes.trim()) return
    setBusy(true)
    try {
      const updated = await api.resolveIncident(selected.id, {
        resolutionNotes: resolveNotes.trim() || null,
      })
      onOk(`Resolved ${updated.displayId}`)
      setResolveNotes('')
      setSelected(updated)
      await load()
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const close = async () => {
    if (!selected) return
    setBusy(true)
    try {
      const updated = await api.closeIncident(selected.id)
      onOk(`Closed ${updated.displayId}`)
      setSelected(updated)
      await load()
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const saveAdminFields = async () => {
    if (!selected || !canAdmin) return
    setBusy(true)
    try {
      const updated = await api.updateIncident(selected.id, {
        title: selected.title,
        description: selected.description,
        affectedArea: selected.affectedArea,
        priority: selected.priority,
        rootCause: selected.rootCause,
        containmentActions: selected.containmentActions,
        resolutionNotes: selected.resolutionNotes,
        linkedRiskId: selected.linkedRiskId,
        linkedGapAssessmentId: selected.linkedGapAssessmentId ?? null,
      })
      onOk('Incident updated')
      setSelected(updated)
      await load()
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const listActions = useMemo(
    () => (
      <Stack direction="row" spacing={1}>
        {canReport && (
          <Button
            variant="outlined"
            onClick={() => {
              setReportForm(emptyReport())
              setReportOpen(true)
            }}
          >
            Report Incident
          </Button>
        )}
        {canAdmin && (
          <Button
            variant="contained"
            onClick={() => {
              setCreateForm(emptyCreate())
              setCreateOpen(true)
            }}
          >
            New (Admin)
          </Button>
        )}
      </Stack>
    ),
    [canReport, canAdmin],
  )

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', minHeight: 0, gap: 1.25 }}>
      {selected ? (
        <ModuleDetailShell
          title={selected.displayId}
          breadcrumbRootLabel="Incidents"
          breadcrumbTrail={`${selected.displayId} · ${selected.title}`}
          onBack={() => setSelected(null)}
        />
      ) : (
        <ModuleListShell title="Incidents" breadcrumbTrail="Incidents" actions={listActions} />
      )}

      {!selected ? (
        <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          <RegisterToolbar
            filters={[
              {
                id: 'status',
                label: 'Status',
                value: listFilter,
                options: incidentFilterOptions.map((o) => ({
                  value: o.value,
                  label: `${o.label} (${o.count})`,
                })),
                onChange: (v) => setListFilter(v as IncidentListFilter),
                minWidth: 140,
              },
            ]}
            onClearFilters={() => setListFilter('ALL')}
            columnPicker={incidentColumns.columnPicker}
          />
          <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
          <RegisterListFrame
            ready={listReady}
            count={paged.count}
            emptyMessage="No incidents yet."
            page={paged.page}
            rowsPerPage={paged.rowsPerPage}
            onPageChange={paged.setPage}
            onRowsPerPageChange={paged.setRowsPerPage}
          >
          <TableContainer sx={{ flex: 1 }}>
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  {incidentColumns.isVisible('id') && <TableCell>ID</TableCell>}
                  {incidentColumns.isVisible('title') && (
                    <RegisterSortHeader
                      label="Title"
                      active={sortKey === 'title'}
                      dir={sortDir}
                      onClick={() => {
                        const next = toggleSort(sortKey, sortDir, 'title')
                        setSortKey(next.key)
                        setSortDir(next.dir)
                      }}
                    />
                  )}
                  {incidentColumns.isVisible('priority') && <TableCell>Priority</TableCell>}
                  {incidentColumns.isVisible('type') && <TableCell>Type</TableCell>}
                  {incidentColumns.isVisible('status') && <TableCell>Status</TableCell>}
                  {incidentColumns.isVisible('responseDue') && <TableCell>Response due</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {paged.paged.map((r) => (
                  <TableRow
                    key={r.id}
                    hover
                    sx={{ cursor: 'pointer' }}
                    onClick={() => void openDetail(r.id)}
                  >
                    {incidentColumns.isVisible('id') && <TableCell>{r.displayId}</TableCell>}
                    {incidentColumns.isVisible('title') && <TableCell>{r.title}</TableCell>}
                    {incidentColumns.isVisible('priority') && (
                    <TableCell>
                      <RiskLevelBadge level={incidentPriorityToBadge(r.priority)} label={r.priority} />
                    </TableCell>
                    )}
                    {incidentColumns.isVisible('type') && <TableCell>{r.incidentType}</TableCell>}
                    {incidentColumns.isVisible('status') && (
                    <TableCell>
                      <StatusChip label={r.status} tone={statusTone(r.status)} />
                    </TableCell>
                    )}
                    {incidentColumns.isVisible('responseDue') && (
                    <TableCell>{r.responseDueAt?.replace('T', ' ').slice(0, 16) ?? '-'}</TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          </RegisterListFrame>
          </Box>
        </Card>
      ) : (
        <Card sx={{ flex: 1, minHeight: 0, overflow: 'auto', p: 2 }}>
          <Stack direction="row" justifyContent="space-between" alignItems="flex-start" sx={{ mb: 1.5 }}>
            <Box>
              <Typography variant="subtitle1" fontWeight={700}>
                {selected.displayId}: {selected.title}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {selected.incidentType}
                {selected.affectedArea ? ` · ${selected.affectedArea}` : ''}
                {' · occurred '}
                {selected.occurredAt?.replace('T', ' ').slice(0, 16)}
              </Typography>
            </Box>
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
              <RiskLevelBadge level={incidentPriorityToBadge(selected.priority)} label={selected.priority} />
              <StatusChip label={selected.status} tone={statusTone(selected.status)} />
            </Stack>
          </Stack>

          {canAdmin && selected.status !== 'CLOSED' && (
            <Stack spacing={1.5} sx={{ mb: 2 }}>
              <FormSelect
                label="Priority"
                value={selected.priority}
                options={PRIORITIES.map((p) => ({ value: p, label: p, tone: apiLevelTone(p) }))}
                onChange={(v) => setSelected({ ...selected, priority: v as IncidentPriority })}
                minWidth={160}
              />
              <CompactTextField
                label="Root cause"
                multiline
                minRows={2}
                fieldIdPrefix="inc"
                value={selected.rootCause ?? ''}
                onChange={(e) => setSelected({ ...selected, rootCause: e.target.value })}
              />
              <CompactTextField
                label="Containment actions"
                multiline
                minRows={2}
                fieldIdPrefix="inc"
                value={selected.containmentActions ?? ''}
                onChange={(e) => setSelected({ ...selected, containmentActions: e.target.value })}
              />
              <FormSelect
                label="Linked risk"
                value={selected.linkedRiskId == null ? '' : String(selected.linkedRiskId)}
                options={[
                  { value: '', label: '- None -' },
                  ...riskOptions.map((r) => ({
                    value: String(r.id),
                    label: `${r.displayId}: ${r.title}`,
                  })),
                ]}
                onChange={(v) =>
                  setSelected({ ...selected, linkedRiskId: v === '' ? null : Number(v) })
                }
                fullWidth
              />
              <FormSelect
                label="Linked Gap assessment"
                value={
                  selected.linkedGapAssessmentId == null ? '' : String(selected.linkedGapAssessmentId)
                }
                options={[
                  { value: '', label: '- None -' },
                  ...gapOptions.map((g) => ({
                    value: String(g.id),
                    label: `${g.name} (${g.frameworkCode})`,
                  })),
                ]}
                onChange={(v) =>
                  setSelected({
                    ...selected,
                    linkedGapAssessmentId: v === '' ? null : Number(v),
                  })
                }
                fullWidth
              />
              <Button size="small" variant="outlined" disabled={busy} onClick={() => void saveAdminFields()}>
                Save Details
              </Button>
            </Stack>
          )}

          {!canAdmin && (
            <Typography variant="body2" sx={{ mb: 2, whiteSpace: 'pre-wrap' }}>
              {selected.description ?? 'No description'}
            </Typography>
          )}

          {canAdmin && selected.description && (
            <Typography variant="body2" sx={{ mb: 2, whiteSpace: 'pre-wrap' }}>
              {selected.description}
            </Typography>
          )}

          <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1 }}>
            Response due {selected.responseDueAt?.replace('T', ' ').slice(0, 19)}
            {selected.resolutionDueAt
              ? ` · Resolution due ${selected.resolutionDueAt.replace('T', ' ').slice(0, 19)}`
              : ''}
          </Typography>

          {canAdmin && selected.status !== 'CLOSED' && selected.status !== 'RESOLVED' && (
            <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1.5 }}>
              <FormSelect
                label="Assign owner"
                value={assignUserId === '' ? '' : String(assignUserId)}
                options={users.map((u) => ({
                  value: String(u.id),
                  label: u.fullName || u.email,
                }))}
                onChange={(v) => setAssignUserId(Number(v))}
                minWidth={220}
              />
              <Button size="small" variant="contained" disabled={busy || assignUserId === ''} onClick={() => void assign()}>
                Assign
              </Button>
            </Stack>
          )}

          {selected.status !== 'CLOSED' && selected.status !== 'RESOLVED' && (
            <Stack spacing={0.5} sx={{ mb: 1.5 }}>
              <FieldActionRow>
                <CompactTextField
                  label="Resolution notes"
                  multiline
                  minRows={2}
                  fieldIdPrefix="inc"
                  value={resolveNotes}
                  onChange={(e) => setResolveNotes(e.target.value)}
                />
                <BusyButton
                  size="small"
                  variant="contained"
                  busy={busy}
                  disabled={!resolveNotes.trim()}
                  onClick={() => void resolve()}
                  sx={controlActionSx}
                >
                  Resolve
                </BusyButton>
              </FieldActionRow>
              <Typography variant="caption" color="text.secondary">
                Add resolution notes to resolve
              </Typography>
            </Stack>
          )}

          {canAdmin && selected.status === 'RESOLVED' && (
            <Button size="small" variant="contained" color="success" disabled={busy} onClick={() => void close()}>
              Close Incident
            </Button>
          )}
        </Card>
      )}

      <Dialog open={reportOpen} onClose={() => setReportOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Report Incident</DialogTitle>
        <DialogContent>
          <Stack spacing={1} sx={{ mt: 1 }}>
            <CompactTextField
              label="Title"
              required
              fieldIdPrefix="inc-report"
              value={reportForm.title}
              onChange={(e) => setReportForm({ ...reportForm, title: e.target.value })}
            />
            <FormSelect
              label="Type"
              value={reportForm.incidentType}
              options={TYPES.map((t) => ({ value: t, label: t }))}
              onChange={(v) => setReportForm({ ...reportForm, incidentType: v as IncidentType })}
              fullWidth
            />
            <CompactTextField
              label="Description"
              multiline
              minRows={3}
              fieldIdPrefix="inc-report"
              value={reportForm.description ?? ''}
              onChange={(e) => setReportForm({ ...reportForm, description: e.target.value })}
            />
            <CompactTextField
              label="Date / time occurred"
              type="datetime-local"
              fieldIdPrefix="inc-report"
              value={reportForm.occurredAt ? reportForm.occurredAt.slice(0, 16) : ''}
              onChange={(e) =>
                setReportForm({
                  ...reportForm,
                  occurredAt: e.target.value ? new Date(e.target.value).toISOString() : null,
                })
              }
            />
            <CompactTextField
              label="Affected area"
              fieldIdPrefix="inc-report"
              value={reportForm.affectedArea ?? ''}
              onChange={(e) => setReportForm({ ...reportForm, affectedArea: e.target.value })}
            />
            <Typography variant="caption" color="text.secondary">
              Priority defaults to P3; Admin will complete the full record.
            </Typography>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setReportOpen(false)}>Cancel</Button>
          <Button variant="contained" disabled={busy || !reportForm.title.trim()} onClick={() => void report()}>
            Submit Report
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>New Incident (Admin)</DialogTitle>
        <DialogContent>
          <Stack spacing={1} sx={{ mt: 1 }}>
            <CompactTextField
              label="Title"
              required
              fieldIdPrefix="inc-create"
              value={createForm.title}
              onChange={(e) => setCreateForm({ ...createForm, title: e.target.value })}
            />
            <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 1 }}>
              <FormSelect
                label="Type"
                value={createForm.incidentType}
                options={TYPES.map((t) => ({ value: t, label: t }))}
                onChange={(v) => setCreateForm({ ...createForm, incidentType: v as IncidentType })}
                fullWidth
              />
              <FormSelect
                label="Priority"
                value={createForm.priority}
                options={PRIORITIES.map((p) => ({ value: p, label: p, tone: apiLevelTone(p) }))}
                onChange={(v) => setCreateForm({ ...createForm, priority: v as IncidentPriority })}
                fullWidth
              />
            </Box>
            <CompactTextField
              label="Description"
              multiline
              minRows={3}
              fieldIdPrefix="inc-create"
              value={createForm.description ?? ''}
              onChange={(e) => setCreateForm({ ...createForm, description: e.target.value })}
            />
            <CompactTextField
              label="Affected area"
              fieldIdPrefix="inc-create"
              value={createForm.affectedArea ?? ''}
              onChange={(e) => setCreateForm({ ...createForm, affectedArea: e.target.value })}
            />
            <FormSelect
              label="Owner (optional)"
              value={createForm.ownerUserId == null ? '' : String(createForm.ownerUserId)}
              options={[
                { value: '', label: 'Unassigned' },
                ...users.map((u) => ({ value: String(u.id), label: u.fullName || u.email })),
              ]}
              onChange={(v) =>
                setCreateForm({ ...createForm, ownerUserId: v === '' ? null : Number(v) })
              }
              fullWidth
            />
            <FormSelect
              label="Linked risk"
              value={createForm.linkedRiskId == null ? '' : String(createForm.linkedRiskId)}
              options={[
                { value: '', label: '- None -' },
                ...riskOptions.map((r) => ({
                  value: String(r.id),
                  label: `${r.displayId}: ${r.title}`,
                })),
              ]}
              onChange={(v) =>
                setCreateForm({ ...createForm, linkedRiskId: v === '' ? null : Number(v) })
              }
              fullWidth
            />
            <FormSelect
              label="Linked Gap assessment"
              value={
                createForm.linkedGapAssessmentId == null
                  ? ''
                  : String(createForm.linkedGapAssessmentId)
              }
              options={[
                { value: '', label: '- None -' },
                ...gapOptions.map((g) => ({
                  value: String(g.id),
                  label: `${g.name} (${g.frameworkCode})`,
                })),
              ]}
              onChange={(v) =>
                setCreateForm({
                  ...createForm,
                  linkedGapAssessmentId: v === '' ? null : Number(v),
                })
              }
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
          <Button variant="contained" disabled={busy || !createForm.title.trim()} onClick={() => void create()}>
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
