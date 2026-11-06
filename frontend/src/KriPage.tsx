import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Box,
  Button,
  Card,
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
  type CreateKriBody,
  type KriDirection,
  type KriFrequency,
  type KriRecord,
  type RiskSummary,
  type Session,
  type UserSummary,
} from './api'
import { useAppShellPage } from './components/AppShellPageContext'
import StatusChip from './components/StatusChip'
import {
  CompactTextField,
  FieldActionRow,
  INPUT_LIMIT,
  controlActionSx,
  RegisterListFrame,
  RegisterSortHeader,
  RegisterToolbar,
  RiskLevelBadge,
  SettingsCard,
  SimpleFormLayout,
} from './components/ui'
import { useListReady } from './uiQuality/useListReady'
import { usePagedRows } from './uiQuality/usePagedRows'
import { usePersistedState } from './uiQuality/usePersistedState'
import { compareLoose, toggleSort, type SortDir } from './uiQuality/sortRows'
import FormSelect from './components/ui/FormSelect'
import { kriRagToBadge } from './components/ui/riskBadgeMappers'
import { KRI_REGISTER_COLUMNS } from './hooks/registerColumns'
import { useRegisterColumnPicker } from './hooks/useRegisterColumnPicker'
import { actionDueDateError, kriThresholdsError, todayIso } from './validation'

type KriListFilter = 'ALL' | 'RED' | 'AMBER' | 'GREEN' | 'ACTIVE'

const FREQUENCIES: KriFrequency[] = ['DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'ANNUAL']
const DIRECTIONS: { value: KriDirection; label: string }[] = [
  { value: 'HIGHER_IS_WORSE', label: 'Higher is worse' },
  { value: 'LOWER_IS_WORSE', label: 'Lower is worse' },
]

function trendLabel(t: string | null | undefined): string {
  if (!t) return '-'
  return t.charAt(0) + t.slice(1).toLowerCase()
}

const emptyForm = (): CreateKriBody => ({
  name: '',
  linkedRiskId: null,
  metricDescription: '',
  unit: '',
  frequency: 'MONTHLY',
  ownerUserId: 0,
  direction: 'HIGHER_IS_WORSE',
  greenThreshold: 1,
  amberThreshold: 5,
  redThreshold: 10,
  nextDueDate: null,
})

export default function KriPage({
  session,
  onError,
  onOk,
}: {
  session: Session
  onError: (msg: string) => void
  onOk: (msg: string) => void
}) {
  const canAdmin = session.roles.some((r) => ['OWNER', 'ADMIN'].includes(r))
  const [rows, setRows] = useState<KriRecord[]>([])
  const [users, setUsers] = useState<UserSummary[]>([])
  const [risks, setRisks] = useState<RiskSummary[]>([])
  const [selected, setSelected] = useState<KriRecord | null>(null)
  const [busy, setBusy] = useState(false)

  const [createOpen, setCreateOpen] = useState(false)
  const [form, setForm] = useState<CreateKriBody>(emptyForm())
  const [listFilter, setListFilter] = usePersistedState<KriListFilter>('reg-kri-filter', 'ALL')
  const kriColumns = useRegisterColumnPicker(KRI_REGISTER_COLUMNS)
  const { listReady, markReady } = useListReady()
  const [sortKey, setSortKey] = useState('name')
  const [sortDir, setSortDir] = useState<SortDir>('asc')

  const [valueStr, setValueStr] = useState('')
  const [valueNotes, setValueNotes] = useState('')
  const [showRecordValue, setShowRecordValue] = useState(false)

  const load = useCallback(async () => {
    try {
      setRows(await api.listKris())
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
    void (async () => {
      try {
        setUsers(await api.listUsers())
        setRisks(await api.listRisks('ERM'))
      } catch {
        /* ignore: create dialog still usable with empty lists */
      }
    })()
  }, [canAdmin])

  const openDetail = async (id: number) => {
    try {
      setSelected(await api.getKri(id))
    } catch (e) {
      onError(String(e))
    }
  }

  const create = async () => {
    if (!form.name.trim() || !form.ownerUserId || nextDueDateError || kriThresholdError) return
    setBusy(true)
    try {
      const created = await api.createKri({
        ...form,
        name: form.name.trim(),
        metricDescription: form.metricDescription?.trim() || null,
        unit: form.unit?.trim() || null,
        linkedRiskId: form.linkedRiskId || null,
        nextDueDate: form.nextDueDate || null,
      })
      setCreateOpen(false)
      setForm(emptyForm())
      onOk(`Created ${created.displayId}`)
      await load()
      setSelected(created)
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const recordValue = async () => {
    if (!selected || valueStr.trim() === '') return
    setBusy(true)
    try {
      const updated = await api.recordKriValue(selected.id, {
        value: Number(valueStr),
        notes: valueNotes.trim() || null,
      })
      setShowRecordValue(false)
      setValueStr('')
      setValueNotes('')
      onOk(`Recorded value: RAG ${updated.rag ?? 'n/a'}`)
      setSelected(updated)
      await load()
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const acknowledgeRed = async () => {
    if (!selected) return
    setBusy(true)
    try {
      const updated = await api.acknowledgeKriRed(selected.id)
      onOk(`Red acknowledged for ${updated.displayId}`)
      setSelected(updated)
      await load()
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const canEnterValue = selected != null && selected.status === 'ACTIVE'

  const dateMin = todayIso()
  const nextDueDateError = useMemo(
    () => actionDueDateError(form.nextDueDate ?? ''),
    [form.nextDueDate],
  )
  const kriThresholdError = useMemo(
    () =>
      kriThresholdsError(
        form.direction,
        Number(form.greenThreshold),
        Number(form.amberThreshold),
        Number(form.redThreshold),
      ),
    [form.direction, form.greenThreshold, form.amberThreshold, form.redThreshold],
  )

  const filteredRows = useMemo(() => {
    let next = rows
    if (listFilter === 'ACTIVE') next = rows.filter((k) => k.status === 'ACTIVE')
    else if (listFilter !== 'ALL') next = rows.filter((k) => k.rag === listFilter)
    return [...next].sort((a, b) => compareLoose(a.name, b.name, sortDir))
  }, [rows, listFilter, sortDir])
  const paged = usePagedRows(filteredRows)

  const kriFilterOptions = useMemo(
    () => [
      { value: 'ALL' as const, label: 'All', count: rows.length },
      { value: 'RED' as const, label: 'Red', count: rows.filter((k) => k.rag === 'RED').length },
      { value: 'AMBER' as const, label: 'Amber', count: rows.filter((k) => k.rag === 'AMBER').length },
      { value: 'GREEN' as const, label: 'Green', count: rows.filter((k) => k.rag === 'GREEN').length },
      { value: 'ACTIVE' as const, label: 'Active', count: rows.filter((k) => k.status === 'ACTIVE').length },
    ],
    [rows],
  )

  const listActions = useMemo(
    () =>
      canAdmin ? (
        <Button
          variant="contained"
          size="small"
          onClick={() => {
            const me = users.find((u) => u.email === session.email)
            setForm({
              ...emptyForm(),
              ownerUserId: me?.id ?? users[0]?.id ?? 0,
            })
            setCreateOpen(true)
          }}
        >
          New KRI
        </Button>
      ) : undefined,
    [canAdmin, users, session.email],
  )

  const createDisabled =
    busy || !form.name.trim() || !form.ownerUserId || !!nextDueDateError || !!kriThresholdError

  const createTopbarActions = useMemo(
    () => (
      <>
        <Button
          size="small"
          variant="outlined"
          onClick={() => {
            setCreateOpen(false)
            setForm(emptyForm())
          }}
        >
          Cancel
        </Button>
        <Button size="small" variant="contained" disabled={createDisabled} onClick={() => void create()}>
          Create KRI
        </Button>
      </>
    ),
    [createDisabled],
  )

  useAppShellPage(
    createOpen
      ? {
          title: 'New KRI',
          breadcrumbTrail: 'New KRI',
          breadcrumbRoot: {
            label: 'Key Risk Indicators',
            onClick: () => {
              setCreateOpen(false)
              setForm(emptyForm())
            },
          },
          actions: createTopbarActions,
          hideContextLabel: true,
          hideRoleChips: true,
        }
      : selected
        ? {
            title: selected.name,
            breadcrumbTrail: `${selected.displayId} · ${selected.name}`,
            breadcrumbRoot: { label: 'Key Risk Indicators', onClick: () => setSelected(null) },
            hideContextLabel: true,
            hideRoleChips: true,
          }
        : {
            title: 'Key Risk Indicators',
            breadcrumbTrail: 'KRIs',
            actions: listActions,
            hideContextLabel: true,
            hideRoleChips: true,
          },
    [createOpen, createTopbarActions, selected, listActions],
  )

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', minHeight: 0, gap: 1.25 }}>
      {createOpen ? (
        <SimpleFormLayout>
          <SettingsCard compact title="KRI details" subtitle="Thresholds, owner, and reporting frequency">
            <Stack spacing={1}>
              <CompactTextField
                label="Name"
                required
                fieldIdPrefix="kri"
                maxLength={INPUT_LIMIT.title}
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
              <CompactTextField
                label="Metric description"
                multiline
                minRows={2}
                fieldIdPrefix="kri"
                value={form.metricDescription ?? ''}
                onChange={(e) => setForm({ ...form, metricDescription: e.target.value })}
              />
              <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 1 }}>
                <CompactTextField
                  label="Unit"
                  fieldIdPrefix="kri"
                  value={form.unit ?? ''}
                  onChange={(e) => setForm({ ...form, unit: e.target.value })}
                />
                <FormSelect
                  label="Frequency"
                  value={form.frequency}
                  options={FREQUENCIES.map((f) => ({ value: f, label: f }))}
                  onChange={(v) => setForm({ ...form, frequency: v as KriFrequency })}
                  fullWidth
                />
              </Box>
              <FormSelect
                label="Direction"
                value={form.direction}
                options={DIRECTIONS.map((d) => ({ value: d.value, label: d.label }))}
                onChange={(v) => {
                  const direction = v as KriDirection
                  if (direction === 'HIGHER_IS_WORSE') {
                    setForm({ ...form, direction, greenThreshold: 1, amberThreshold: 5, redThreshold: 10 })
                  } else {
                    setForm({ ...form, direction, redThreshold: 50, amberThreshold: 70, greenThreshold: 90 })
                  }
                }}
                fullWidth
              />
              <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 1 }}>
                <CompactTextField
                  label="Green"
                  type="number"
                  fieldIdPrefix="kri"
                  value={form.greenThreshold}
                  onChange={(e) => setForm({ ...form, greenThreshold: Number(e.target.value) })}
                  error={!!kriThresholdError}
                />
                <CompactTextField
                  label="Amber"
                  type="number"
                  fieldIdPrefix="kri"
                  value={form.amberThreshold}
                  onChange={(e) => setForm({ ...form, amberThreshold: Number(e.target.value) })}
                  error={!!kriThresholdError}
                />
                <CompactTextField
                  label="Red"
                  type="number"
                  fieldIdPrefix="kri"
                  value={form.redThreshold}
                  onChange={(e) => setForm({ ...form, redThreshold: Number(e.target.value) })}
                  error={!!kriThresholdError}
                  helperText={kriThresholdError || undefined}
                />
              </Box>
              <FormSelect
                label="Owner"
                required
                value={form.ownerUserId ? String(form.ownerUserId) : ''}
                options={users.map((u) => ({
                  value: String(u.id),
                  label: `${u.fullName || u.email} (${u.email})`,
                }))}
                onChange={(v) => setForm({ ...form, ownerUserId: Number(v) })}
                fullWidth
              />
              <FormSelect
                label="Linked ERM risk (optional)"
                value={form.linkedRiskId == null ? '' : String(form.linkedRiskId)}
                options={[
                  { value: '', label: 'None' },
                  ...risks.map((r) => ({ value: String(r.id), label: `${r.displayId}: ${r.title}` })),
                ]}
                onChange={(v) => setForm({ ...form, linkedRiskId: v === '' ? null : Number(v) })}
                fullWidth
              />
              <CompactTextField
                label="Next due date"
                type="date"
                fieldIdPrefix="kri"
                value={form.nextDueDate ?? ''}
                onChange={(e) => setForm({ ...form, nextDueDate: e.target.value || null })}
                error={!!nextDueDateError}
                helperText={nextDueDateError || 'Leave blank to set from frequency (from today)'}
                inputProps={{ min: dateMin }}
              />
            </Stack>
          </SettingsCard>
        </SimpleFormLayout>
      ) : !selected ? (
        <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          <RegisterToolbar
            filters={[
              {
                id: 'rag',
                label: 'View',
                value: listFilter,
                options: kriFilterOptions.map((o) => ({
                  value: o.value,
                  label: `${o.label} (${o.count})`,
                })),
                onChange: (v) => setListFilter(v as KriListFilter),
                minWidth: 140,
              },
            ]}
            onClearFilters={() => setListFilter('ALL')}
            columnPicker={kriColumns.columnPicker}
          />
          <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
          <RegisterListFrame
            ready={listReady}
            count={paged.count}
            emptyMessage="No KRIs yet."
            page={paged.page}
            rowsPerPage={paged.rowsPerPage}
            onPageChange={paged.setPage}
            onRowsPerPageChange={paged.setRowsPerPage}
          >
          <TableContainer sx={{ flex: 1 }}>
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  {kriColumns.isVisible('id') && <TableCell>ID</TableCell>}
                  {kriColumns.isVisible('name') && (
                    <RegisterSortHeader
                      label="Name"
                      active={sortKey === 'name'}
                      dir={sortDir}
                      onClick={() => {
                        const next = toggleSort(sortKey, sortDir, 'name')
                        setSortKey(next.key)
                        setSortDir(next.dir)
                      }}
                    />
                  )}
                  {kriColumns.isVisible('rag') && <TableCell>RAG</TableCell>}
                  {kriColumns.isVisible('value') && <TableCell>Value</TableCell>}
                  {kriColumns.isVisible('trend') && <TableCell>Trend</TableCell>}
                  {kriColumns.isVisible('due') && <TableCell>Next due</TableCell>}
                  {kriColumns.isVisible('status') && <TableCell>Status</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {paged.paged.map((k) => (
                  <TableRow
                    key={k.id}
                    hover
                    sx={{ cursor: 'pointer' }}
                    onClick={() => void openDetail(k.id)}
                  >
                    {kriColumns.isVisible('id') && <TableCell>{k.displayId}</TableCell>}
                    {kriColumns.isVisible('name') && <TableCell>{k.name}</TableCell>}
                    {kriColumns.isVisible('rag') && (
                    <TableCell>
                      {k.rag ? <RiskLevelBadge level={kriRagToBadge(k.rag)} label={k.rag} /> : '-'}
                    </TableCell>
                    )}
                    {kriColumns.isVisible('value') && (
                    <TableCell>
                      {k.currentValue != null
                        ? `${k.currentValue}${k.unit ? ` ${k.unit}` : ''}`
                        : '-'}
                    </TableCell>
                    )}
                    {kriColumns.isVisible('trend') && <TableCell>{trendLabel(k.trend)}</TableCell>}
                    {kriColumns.isVisible('due') && <TableCell>{k.nextDueDate ?? '-'}</TableCell>}
                    {kriColumns.isVisible('status') && <TableCell>{k.status}</TableCell>}
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
                {selected.displayId}: {selected.name}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {selected.direction === 'HIGHER_IS_WORSE' ? 'Higher is worse' : 'Lower is worse'}
                {' · '}
                {selected.frequency}
                {selected.unit ? ` · ${selected.unit}` : ''}
                {selected.linkedRiskId ? ` · linked risk #${selected.linkedRiskId}` : ''}
              </Typography>
            </Box>
            <Stack direction="row" spacing={1}>
              {canEnterValue && (
                <Button
                  size="small"
                  variant={showRecordValue ? 'outlined' : 'contained'}
                  onClick={() => {
                    if (showRecordValue) {
                      setShowRecordValue(false)
                      setValueStr('')
                      setValueNotes('')
                    } else {
                      setValueStr('')
                      setValueNotes('')
                      setShowRecordValue(true)
                    }
                  }}
                >
                  {showRecordValue ? 'Cancel' : 'Record Value'}
                </Button>
              )}
              {canAdmin && selected.rag === 'RED' && !selected.redAcknowledgedAt && (
                <Button size="small" color="warning" variant="outlined" disabled={busy} onClick={() => void acknowledgeRed()}>
                  Acknowledge Red
                </Button>
              )}
            </Stack>
          </Stack>

          <Stack direction="row" spacing={2} flexWrap="wrap" useFlexGap sx={{ mb: 2 }}>
            {selected.rag ? (
              <RiskLevelBadge level={kriRagToBadge(selected.rag)} label={selected.rag} />
            ) : (
              <Typography variant="caption" color="text.secondary">
                No RAG
              </Typography>
            )}
            <Typography variant="body2">
              Thresholds G {selected.greenThreshold} / A {selected.amberThreshold} / R{' '}
              {selected.redThreshold}
            </Typography>
            <Typography variant="body2">Trend: {trendLabel(selected.trend)}</Typography>
            <Typography variant="body2">Due: {selected.nextDueDate ?? '-'}</Typography>
            {selected.redAcknowledgedAt && (
              <Typography variant="body2" color="warning.main">
                Red acknowledged {selected.redAcknowledgedAt.slice(0, 10)}
              </Typography>
            )}
          </Stack>

          {selected.metricDescription && (
            <Typography variant="body2" sx={{ mb: 2, whiteSpace: 'pre-wrap' }}>
              {selected.metricDescription}
            </Typography>
          )}

          {showRecordValue && (
            <Card variant="outlined" sx={{ p: 2, mb: 2 }}>
              <Typography variant="subtitle2" sx={{ mb: 1.5 }}>
                Record new value
              </Typography>
              <FieldActionRow spacing={1.5}>
                <Box sx={{ width: { xs: '100%', sm: 140 } }}>
                  <CompactTextField
                    label="Value"
                    type="number"
                    required
                    fieldIdPrefix="kri-val"
                    value={valueStr}
                    onChange={(e) => setValueStr(e.target.value)}
                  />
                </Box>
                <Box sx={{ flex: 1, minWidth: 0 }}>
                  <CompactTextField
                    label="Notes"
                    multiline
                    minRows={1}
                    fieldIdPrefix="kri-val"
                    value={valueNotes}
                    onChange={(e) => setValueNotes(e.target.value)}
                  />
                </Box>
                <Button
                  variant="contained"
                  disabled={busy || valueStr.trim() === ''}
                  onClick={() => void recordValue()}
                  sx={controlActionSx}
                >
                  Save
                </Button>
              </FieldActionRow>
            </Card>
          )}

          <Typography variant="subtitle2" sx={{ mb: 1 }}>
            Recent readings
          </Typography>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>When</TableCell>
                <TableCell>Value</TableCell>
                <TableCell>RAG</TableCell>
                <TableCell>Notes</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(selected.recentReadings ?? []).length === 0 && (
                <TableRow>
                  <TableCell colSpan={4}>
                    <Typography variant="body2" color="text.secondary">
                      No readings yet.
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
              {(selected.recentReadings ?? []).map((r) => (
                <TableRow key={r.id}>
                  <TableCell>{r.recordedAt?.replace('T', ' ').slice(0, 19)}</TableCell>
                  <TableCell>{r.value}</TableCell>
                  <TableCell>
                    <RiskLevelBadge level={kriRagToBadge(r.rag)} label={r.rag} />
                  </TableCell>
                  <TableCell>{r.notes ?? '-'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Card>
      )}
    </Box>
  )
}
