import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Box,
  Button,
  Card,
  Checkbox,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  ListItemText,
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
} from '@mui/material'
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded'
import AddRoundedIcon from '@mui/icons-material/AddRounded'
import {
  api,
  type BcmsExercise,
  type ExerciseParticipantBody,
  type ExerciseStatus,
  type ExerciseType,
  type RecoveryPlan,
  type Session,
  type UserSummary,
} from './api'
import StatusChip from './components/StatusChip'
import { CompactTextField, FormFieldLabel, FormSelect, RegisterListFrame, RegisterSortHeader, RegisterToolbar, compactSelectSx } from './components/ui'
import { useListReady } from './uiQuality/useListReady'
import { usePagedRows } from './uiQuality/usePagedRows'
import { compareLoose, toggleSort, type SortDir } from './uiQuality/sortRows'
import { BCMS_EXERCISE_COLUMNS } from './hooks/registerColumns'
import { useRegisterColumnPicker } from './hooks/useRegisterColumnPicker'
import { ModuleDetailShell, ModuleListShell } from './hooks/useModulePageShell'
import { brand } from './theme'
import { actionDueDateError, actionDueDateErrorIfChanged, todayIso } from './validation'

const EXERCISE_TYPES: ExerciseType[] = ['TABLETOP', 'WALKTHROUGH', 'SIMULATION', 'FULL_TEST']

function tone(s: ExerciseStatus): 'success' | 'warning' | 'info' | 'danger' | 'neutral' {
  if (s === 'COMPLETED') return 'success'
  if (s === 'CANCELLED') return 'danger'
  return 'info'
}

const emptyForm = {
  name: '',
  exerciseType: 'TABLETOP' as ExerciseType,
  scope: '',
  facilitatorUserId: '' as number | '',
  objectives: '',
  scheduledDate: '',
  planIds: [] as number[],
}

const emptyComplete = {
  scenariosTested: '',
  results: '',
  gapsIdentified: '',
  lessonsLearned: '',
}

export default function BcmsExercisesPage({
  session,
  onError,
  onOk,
}: {
  session: Session
  onError: (msg: string) => void
  onOk: (msg: string) => void
}) {
  const canAdmin = session.roles.some((r) => ['OWNER', 'ADMIN'].includes(r))
  const [rows, setRows] = useState<BcmsExercise[]>([])
  const [plans, setPlans] = useState<RecoveryPlan[]>([])
  const [users, setUsers] = useState<UserSummary[]>([])
  const [busy, setBusy] = useState(false)
  const [open, setOpen] = useState(false)
  const exerciseColumns = useRegisterColumnPicker(BCMS_EXERCISE_COLUMNS)
  const [selected, setSelected] = useState<BcmsExercise | null>(null)
  const [editId, setEditId] = useState<number | null>(null)
  const [baselineScheduledDate, setBaselineScheduledDate] = useState('')
  const [form, setForm] = useState(emptyForm)
  const [participants, setParticipants] = useState<ExerciseParticipantBody[]>([])
  const [completeId, setCompleteId] = useState<number | null>(null)
  const [completeForm, setCompleteForm] = useState(emptyComplete)
  const { listReady, markReady } = useListReady()
  const [sortKey, setSortKey] = useState('name')
  const [sortDir, setSortDir] = useState<SortDir>('asc')

  const load = useCallback(async () => {
    setBusy(true)
    try {
      const [exercises, planList] = await Promise.all([api.listExercises(), api.listRecoveryPlans()])
      setRows(exercises)
      setPlans(planList)
      setUsers(await api.listUsers().catch(() => []))
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
      markReady()
    }
  }, [onError, markReady])

  useEffect(() => {
    void load()
  }, [load])

  const resetForm = () => {
    setEditId(null)
    setBaselineScheduledDate('')
    setForm(emptyForm)
    setParticipants([])
  }

  const openCreate = () => {
    resetForm()
    setSelected(null)
    setOpen(true)
  }

  const openEdit = (e: BcmsExercise) => {
    setEditId(e.id)
    setBaselineScheduledDate(e.scheduledDate ?? '')
    setForm({
      name: e.name,
      exerciseType: e.exerciseType,
      scope: e.scope ?? '',
      facilitatorUserId: e.facilitatorUserId ?? '',
      objectives: e.objectives ?? '',
      scheduledDate: e.scheduledDate ?? '',
      planIds: e.planIds,
    })
    setParticipants(e.participants.map((p) => ({ userId: p.userId, participantName: p.participantName ?? '' })))
    setSelected(null)
    setOpen(true)
  }

  const addParticipant = () => setParticipants((prev) => [...prev, { userId: null, participantName: '' }])
  const updateParticipant = (idx: number, patch: Partial<ExerciseParticipantBody>) =>
    setParticipants((prev) => prev.map((p, i) => (i === idx ? { ...p, ...patch } : p)))
  const removeParticipant = (idx: number) => setParticipants((prev) => prev.filter((_, i) => i !== idx))

  const dateMin = todayIso()
  const scheduledDateError = useMemo(
    () =>
      editId == null
        ? actionDueDateError(form.scheduledDate)
        : actionDueDateErrorIfChanged(baselineScheduledDate, form.scheduledDate),
    [editId, baselineScheduledDate, form.scheduledDate],
  )

  const save = async () => {
    if (!form.name.trim() || scheduledDateError) return
    setBusy(true)
    try {
      const body = {
        name: form.name.trim(),
        exerciseType: form.exerciseType,
        scope: form.scope.trim() || null,
        facilitatorUserId: form.facilitatorUserId === '' ? null : form.facilitatorUserId,
        objectives: form.objectives.trim() || null,
        scheduledDate: form.scheduledDate || null,
        planIds: form.planIds,
        participants: participants.filter((p) => p.userId != null || (p.participantName ?? '').trim()),
      }
      if (editId == null) {
        await api.createExercise(body)
        onOk('Exercise planned')
      } else {
        await api.updateExercise(editId, body)
        onOk('Exercise updated')
      }
      setOpen(false)
      setSelected(null)
      await load()
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const openComplete = (id: number) => {
    setCompleteId(id)
    setCompleteForm(emptyComplete)
  }

  const complete = async () => {
    if (completeId == null) return
    setBusy(true)
    try {
      const e = await api.completeExercise(completeId, {
        scenariosTested: completeForm.scenariosTested.trim() || null,
        results: completeForm.results.trim() || null,
        gapsIdentified: completeForm.gapsIdentified.trim() || null,
        lessonsLearned: completeForm.lessonsLearned.trim() || null,
      })
      setCompleteId(null)
      onOk(e.gapsIdentified ? 'Exercise completed: CAPA raised for gaps' : 'Exercise completed')
      await load()
      setSelected(e)
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const cancel = async (id: number) => {
    setBusy(true)
    try {
      await api.cancelExercise(id)
      onOk('Exercise cancelled')
      setSelected(null)
      await load()
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const userLabel = (id: number | null) => (id == null ? '-' : users.find((u) => u.id === id)?.fullName ?? `#${id}`)

  const sortedRows = useMemo(
    () => [...rows].sort((a, b) => compareLoose(a.name, b.name, sortDir)),
    [rows, sortDir],
  )
  const paged = usePagedRows(sortedRows)

  const listActions = useMemo(
    () =>
      canAdmin ? (
        <Button variant="contained" disabled={busy} onClick={openCreate}>
          New Exercise
        </Button>
      ) : undefined,
    [canAdmin, busy],
  )

  const requestBack = useCallback(() => {
    if (open) {
      setOpen(false)
      if (editId != null) {
        const ex = rows.find((r) => r.id === editId)
        if (ex) setSelected(ex)
      }
    } else {
      setSelected(null)
    }
  }, [open, editId, rows])

  const shellTitle = open
    ? editId == null
      ? 'New Exercise'
      : 'Edit Exercise'
    : selected?.name ?? 'Exercise'

  return (
    <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
      {open ? (
        <ModuleDetailShell
          title={shellTitle}
          breadcrumbRootLabel="Exercises"
          breadcrumbTrail={shellTitle}
          onBack={requestBack}
        />
      ) : selected ? (
        <ModuleDetailShell
          title={selected.name}
          breadcrumbRootLabel="Exercises"
          breadcrumbTrail={selected.name}
          onBack={requestBack}
        />
      ) : (
        <ModuleListShell title="Exercises" breadcrumbTrail="BCMS" actions={listActions} />
      )}

      {!open && !selected ? (
        <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          <RegisterToolbar columnPicker={exerciseColumns.columnPicker} />
          <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
          <RegisterListFrame
            ready={listReady}
            count={paged.count}
            emptyMessage="No exercises yet."
            page={paged.page}
            rowsPerPage={paged.rowsPerPage}
            onPageChange={paged.setPage}
            onRowsPerPageChange={paged.setRowsPerPage}
          >
          <TableContainer sx={{ flex: 1 }}>
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  {exerciseColumns.isVisible('name') && (
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
                  {exerciseColumns.isVisible('type') && <TableCell>Type</TableCell>}
                  {exerciseColumns.isVisible('scheduled') && <TableCell>Scheduled</TableCell>}
                  {exerciseColumns.isVisible('facilitator') && <TableCell>Facilitator</TableCell>}
                  {exerciseColumns.isVisible('status') && <TableCell>Status</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {paged.paged.map((e) => (
                  <TableRow
                    key={e.id}
                    hover
                    sx={{ cursor: 'pointer' }}
                    onClick={() => setSelected(e)}
                  >
                    {exerciseColumns.isVisible('name') && (
                    <TableCell>
                      <Typography variant="body2" fontWeight={600}>
                        {e.name}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {e.planIds.length} plan{e.planIds.length === 1 ? '' : 's'} ·{' '}
                        {e.participants.length} participant{e.participants.length === 1 ? '' : 's'}
                      </Typography>
                    </TableCell>
                    )}
                    {exerciseColumns.isVisible('type') && (
                    <TableCell>
                      <Typography variant="caption">{e.exerciseType.replace(/_/g, ' ')}</Typography>
                    </TableCell>
                    )}
                    {exerciseColumns.isVisible('scheduled') && (
                    <TableCell>
                      <Typography variant="caption">{e.scheduledDate ?? '-'}</Typography>
                    </TableCell>
                    )}
                    {exerciseColumns.isVisible('facilitator') && (
                    <TableCell>
                      <Typography variant="caption">{userLabel(e.facilitatorUserId)}</Typography>
                    </TableCell>
                    )}
                    {exerciseColumns.isVisible('status') && (
                    <TableCell>
                      <StatusChip label={e.status} tone={tone(e.status)} />
                    </TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          </RegisterListFrame>
          </Box>
        </Card>
      ) : open ? (
        <Card sx={{ flex: 1, minHeight: 0, overflow: 'auto', p: 2 }}>
          <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 2 }}>
            {editId == null ? 'New Exercise' : 'Edit Exercise'}
          </Typography>
          <Stack spacing={1.5}>
            <Stack direction="row" spacing={1}>
              <CompactTextField
                label="Name"
                required
                fullWidth
                value={form.name}
                onChange={(e) => setForm((s) => ({ ...s, name: e.target.value }))}
              />
              <FormSelect
                label="Type"
                fullWidth
                value={form.exerciseType}
                onChange={(v) => setForm((s) => ({ ...s, exerciseType: v as ExerciseType }))}
                options={EXERCISE_TYPES.map((t) => ({
                  value: t,
                  label: t.replace(/_/g, ' '),
                }))}
              />
            </Stack>
            <Stack direction="row" spacing={1}>
              <FormSelect
                label="Facilitator"
                fullWidth
                value={form.facilitatorUserId === '' ? '' : String(form.facilitatorUserId)}
                onChange={(v) =>
                  setForm((s) => ({ ...s, facilitatorUserId: v === '' ? '' : Number(v) }))
                }
                options={[
                  { value: '', label: '-' },
                  ...users.map((u) => ({ value: String(u.id), label: u.fullName })),
                ]}
              />
              <CompactTextField
                label="Scheduled date"
                type="date"
                fullWidth
                inputProps={{ min: dateMin }}
                value={form.scheduledDate}
                onChange={(e) => setForm((s) => ({ ...s, scheduledDate: e.target.value }))}
                error={!!scheduledDateError}
                helperText={scheduledDateError || undefined}
              />
            </Stack>
            <CompactTextField
              label="Scope"
              multiline
              minRows={2}
              fullWidth
              value={form.scope}
              onChange={(e) => setForm((s) => ({ ...s, scope: e.target.value }))}
            />
            <CompactTextField
              label="Objectives"
              multiline
              minRows={2}
              fullWidth
              value={form.objectives}
              onChange={(e) => setForm((s) => ({ ...s, objectives: e.target.value }))}
            />

            <Box>
              <FormFieldLabel htmlFor="exercise-plan-ids">Recovery plans exercised</FormFieldLabel>
              <Select
                id="exercise-plan-ids"
                multiple
                size="small"
                fullWidth
                value={form.planIds}
                onChange={(e) => setForm((s) => ({ ...s, planIds: e.target.value as number[] }))}
                renderValue={(selected) => (
                  <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
                    {(selected as number[]).map((id) => (
                      <Chip key={id} size="small" label={plans.find((p) => p.id === id)?.name ?? id} />
                    ))}
                  </Stack>
                )}
                sx={compactSelectSx}
              >
                {plans.map((p) => (
                  <MenuItem key={p.id} value={p.id}>
                    <Checkbox size="small" checked={form.planIds.includes(p.id)} />
                    <ListItemText primary={p.name} secondary={p.planType} />
                  </MenuItem>
                ))}
              </Select>
            </Box>

            <Box>
              <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 0.5 }}>
                <Typography variant="caption" color="text.secondary">
                  Participants
                </Typography>
                <Button size="small" startIcon={<AddRoundedIcon />} onClick={addParticipant}>
                  Add Participant
                </Button>
              </Stack>
              <Stack spacing={1}>
                {participants.map((p, idx) => (
                  <Stack key={idx} direction="row" spacing={1} alignItems="flex-end">
                    <Box sx={{ flex: 1 }}>
                      <FormSelect
                        label="User (optional)"
                        fullWidth
                        value={p.userId == null ? '' : String(p.userId)}
                        onChange={(v) =>
                          updateParticipant(idx, { userId: v === '' ? null : Number(v) })
                        }
                        options={[
                          { value: '', label: '-' },
                          ...users.map((u) => ({ value: String(u.id), label: u.fullName })),
                        ]}
                      />
                    </Box>
                    <CompactTextField
                      label="Participant name"
                      value={p.participantName ?? ''}
                      onChange={(e) => updateParticipant(idx, { participantName: e.target.value })}
                      sx={{ flex: 1 }}
                    />
                    <Button size="small" color="error" onClick={() => removeParticipant(idx)}>
                      <DeleteOutlineRoundedIcon fontSize="small" />
                    </Button>
                  </Stack>
                ))}
                {participants.length === 0 && (
                  <Typography variant="caption" color="text.secondary">
                    No participants added yet.
                  </Typography>
                )}
              </Stack>
            </Box>
            <Stack direction="row" spacing={1}>
              <Button onClick={() => setOpen(false)}>Cancel</Button>
              <Button variant="contained" disabled={!form.name.trim() || busy || !!scheduledDateError} onClick={() => void save()}>
                Save
              </Button>
            </Stack>
          </Stack>
        </Card>
      ) : selected ? (
        <Card sx={{ flex: 1, minHeight: 0, overflow: 'auto', p: 2 }}>
          <Stack spacing={2}>
            <Box>
              <Typography variant="h6" fontWeight={700}>
                {selected.name}
              </Typography>
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mt: 1 }}>
                <StatusChip label={selected.status} tone={tone(selected.status)} />
                <Typography variant="caption" color="text.secondary" sx={{ alignSelf: 'center' }}>
                  {selected.exerciseType.replace(/_/g, ' ')}
                  {selected.scheduledDate ? ` · ${selected.scheduledDate}` : ''}
                  {' · Facilitator '}
                  {userLabel(selected.facilitatorUserId)}
                </Typography>
              </Stack>
            </Box>
            {selected.scope && (
              <Box>
                <Typography variant="subtitle2">Scope</Typography>
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                  {selected.scope}
                </Typography>
              </Box>
            )}
            {selected.objectives && (
              <Box>
                <Typography variant="subtitle2">Objectives</Typography>
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                  {selected.objectives}
                </Typography>
              </Box>
            )}
            {selected.status === 'COMPLETED' && (
              <>
                {selected.results && (
                  <Box>
                    <Typography variant="subtitle2">Results</Typography>
                    <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                      {selected.results}
                    </Typography>
                  </Box>
                )}
                {selected.gapsIdentified && (
                  <Typography variant="body2" color="warning.main">
                    Gaps identified: CAPA raised
                  </Typography>
                )}
              </>
            )}
            {canAdmin && selected.status === 'PLANNED' && (
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                <Button variant="outlined" onClick={() => openEdit(selected)}>
                  Edit
                </Button>
                <Button variant="contained" onClick={() => openComplete(selected.id)}>
                  Complete
                </Button>
                <Button color="warning" variant="outlined" onClick={() => void cancel(selected.id)}>
                  Cancel Exercise
                </Button>
              </Stack>
            )}
          </Stack>
        </Card>
      ) : null}

      <Dialog open={completeId != null} onClose={() => setCompleteId(null)} fullWidth maxWidth="sm">
        <DialogTitle>Complete Exercise</DialogTitle>
        <DialogContent>
          <Stack spacing={1.5} sx={{ mt: 1 }}>
            <CompactTextField
              label="Scenarios tested"
              multiline
              minRows={2}
              fieldIdPrefix="bcms-ex"
              value={completeForm.scenariosTested}
              onChange={(e) => setCompleteForm((s) => ({ ...s, scenariosTested: e.target.value }))}
            />
            <CompactTextField
              label="Results"
              multiline
              minRows={2}
              fieldIdPrefix="bcms-ex"
              value={completeForm.results}
              onChange={(e) => setCompleteForm((s) => ({ ...s, results: e.target.value }))}
            />
            <CompactTextField
              label="Gaps identified (raises CAPA if filled)"
              multiline
              minRows={2}
              fieldIdPrefix="bcms-ex"
              value={completeForm.gapsIdentified}
              onChange={(e) => setCompleteForm((s) => ({ ...s, gapsIdentified: e.target.value }))}
            />
            <CompactTextField
              label="Lessons learned"
              multiline
              minRows={2}
              fieldIdPrefix="bcms-ex"
              value={completeForm.lessonsLearned}
              onChange={(e) => setCompleteForm((s) => ({ ...s, lessonsLearned: e.target.value }))}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCompleteId(null)}>Cancel</Button>
          <Button variant="contained" disabled={busy} onClick={() => void complete()}>
            Complete
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}
