import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Box,
  Button,
  Card,
  Checkbox,
  Chip,
  FormControl,
  InputLabel,
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
  type BiaProcessRecord,
  type RecoveryPlan,
  type RecoveryPlanProcedureBody,
  type RecoveryPlanStatus,
  type RecoveryPlanType,
  type Session,
  type UserSummary,
} from './api'
import StatusChip from './components/StatusChip'
import { CompactTextField, FieldActionRow, FormFieldLabel, FormSelect, RegisterListFrame, RegisterSortHeader, RegisterToolbar, compactSelectSx, controlActionSx } from './components/ui'
import { useListReady } from './uiQuality/useListReady'
import { usePagedRows } from './uiQuality/usePagedRows'
import { compareLoose, toggleSort, type SortDir } from './uiQuality/sortRows'
import { BCMS_PLAN_COLUMNS } from './hooks/registerColumns'
import { useRegisterColumnPicker } from './hooks/useRegisterColumnPicker'
import { ModuleDetailShell, ModuleListShell } from './hooks/useModulePageShell'
import { brand } from './theme'
import { actionDueDateError, actionDueDateErrorIfChanged, todayIso } from './validation'

const PLAN_TYPES: RecoveryPlanType[] = ['BCP', 'DRP', 'IT_RECOVERY', 'CRISIS_COMMS']

function tone(s: RecoveryPlanStatus): 'success' | 'warning' | 'info' | 'danger' | 'neutral' {
  if (s === 'APPROVED') return 'success'
  if (s === 'PENDING_OWNER') return 'warning'
  if (s === 'EXPIRED') return 'danger'
  return 'neutral'
}

const emptyForm = {
  name: '',
  planType: 'BCP' as RecoveryPlanType,
  scope: '',
  crisisTeam: '',
  escalationContacts: '',
  communicationTree: '',
  resourceRequirements: '',
  alternateSiteDetails: '',
  ownerUserId: '' as number | '',
  reviewDate: '',
  biaIds: [] as number[],
}

export default function BcmsRecoveryPlansPage({
  session,
  onError,
  onOk,
}: {
  session: Session
  onError: (msg: string) => void
  onOk: (msg: string) => void
}) {
  const canAdmin = session.roles.some((r) => ['OWNER', 'ADMIN'].includes(r))
  const [rows, setRows] = useState<RecoveryPlan[]>([])
  const [bias, setBias] = useState<BiaProcessRecord[]>([])
  const [users, setUsers] = useState<UserSummary[]>([])
  const [busy, setBusy] = useState(false)
  const [open, setOpen] = useState(false)
  const planColumns = useRegisterColumnPicker(BCMS_PLAN_COLUMNS)
  const [selected, setSelected] = useState<RecoveryPlan | null>(null)
  const [editId, setEditId] = useState<number | null>(null)
  const [baselineReviewDate, setBaselineReviewDate] = useState('')
  const [form, setForm] = useState(emptyForm)
  const [procedures, setProcedures] = useState<RecoveryPlanProcedureBody[]>([])
  const { listReady, markReady } = useListReady()
  const [sortKey, setSortKey] = useState('name')
  const [sortDir, setSortDir] = useState<SortDir>('asc')

  const me = users.find((u) => u.email.toLowerCase() === session.email.toLowerCase())
  const approvedBias = bias

  const load = useCallback(async () => {
    setBusy(true)
    try {
      const [plans, biaList] = await Promise.all([api.listRecoveryPlans(), api.listApprovedBiaProcesses()])
      setRows(plans)
      setBias(biaList)
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
    setBaselineReviewDate('')
    setForm(emptyForm)
    setProcedures([])
  }

  const openCreate = () => {
    resetForm()
    setSelected(null)
    setOpen(true)
  }

  const openEdit = (p: RecoveryPlan) => {
    setEditId(p.id)
    setBaselineReviewDate(p.reviewDate ?? '')
    setForm({
      name: p.name,
      planType: p.planType,
      scope: p.scope ?? '',
      crisisTeam: p.crisisTeam ?? '',
      escalationContacts: p.escalationContacts ?? '',
      communicationTree: p.communicationTree ?? '',
      resourceRequirements: p.resourceRequirements ?? '',
      alternateSiteDetails: p.alternateSiteDetails ?? '',
      ownerUserId: p.ownerUserId ?? '',
      reviewDate: p.reviewDate ?? '',
      biaIds: p.biaIds,
    })
    setProcedures(p.procedures.map((pr) => ({ biaId: pr.biaId, processName: pr.processName, procedureText: pr.procedureText ?? '', sortOrder: pr.sortOrder })))
    setSelected(null)
    setOpen(true)
  }

  const addProcedure = () =>
    setProcedures((prev) => [...prev, { biaId: null, processName: '', procedureText: '', sortOrder: prev.length }])

  const updateProcedure = (idx: number, patch: Partial<RecoveryPlanProcedureBody>) =>
    setProcedures((prev) => prev.map((p, i) => (i === idx ? { ...p, ...patch } : p)))

  const removeProcedure = (idx: number) =>
    setProcedures((prev) => prev.filter((_, i) => i !== idx).map((p, i) => ({ ...p, sortOrder: i })))

  const dateMin = todayIso()
  const reviewDateError = useMemo(
    () =>
      editId == null
        ? actionDueDateError(form.reviewDate)
        : actionDueDateErrorIfChanged(baselineReviewDate, form.reviewDate),
    [editId, baselineReviewDate, form.reviewDate],
  )

  const save = async () => {
    if (!form.name.trim() || reviewDateError) return
    setBusy(true)
    try {
      const body = {
        name: form.name.trim(),
        planType: form.planType,
        scope: form.scope.trim() || null,
        crisisTeam: form.crisisTeam.trim() || null,
        escalationContacts: form.escalationContacts.trim() || null,
        communicationTree: form.communicationTree.trim() || null,
        resourceRequirements: form.resourceRequirements.trim() || null,
        alternateSiteDetails: form.alternateSiteDetails.trim() || null,
        ownerUserId: form.ownerUserId === '' ? null : form.ownerUserId,
        biaIds: form.biaIds,
        procedures: procedures
          .filter((p) => p.processName.trim())
          .map((p, i) => ({ ...p, processName: p.processName.trim(), sortOrder: i })),
      }
      if (editId == null) {
        await api.createRecoveryPlan(body)
        onOk('Recovery plan created')
      } else {
        await api.updateRecoveryPlan(editId, { ...body, reviewDate: form.reviewDate || null })
        onOk('Recovery plan updated')
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

  const run = async (fn: () => Promise<RecoveryPlan>, okMsg: string) => {
    setBusy(true)
    try {
      const updated = await fn()
      await load()
      setSelected(updated)
      onOk(okMsg)
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

  const isOwner = selected != null && me != null && selected.ownerUserId === me.id

  const listActions = useMemo(
    () =>
      canAdmin ? (
        <Button variant="contained" disabled={busy} onClick={openCreate}>
          New Plan
        </Button>
      ) : undefined,
    [canAdmin, busy],
  )

  const requestBack = useCallback(() => {
    if (open) {
      setOpen(false)
      if (editId != null) {
        const plan = rows.find((r) => r.id === editId)
        if (plan) setSelected(plan)
      }
    } else {
      setSelected(null)
    }
  }, [open, editId, rows])

  const shellTitle = open
    ? editId == null
      ? 'New recovery plan'
      : 'Edit recovery plan'
    : selected?.name ?? 'Recovery plan'

  return (
    <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
      {open ? (
        <ModuleDetailShell
          title={shellTitle}
          breadcrumbRootLabel="Recovery Plans"
          breadcrumbTrail={shellTitle}
          onBack={requestBack}
        />
      ) : selected ? (
        <ModuleDetailShell
          title={selected.name}
          breadcrumbRootLabel="Recovery Plans"
          breadcrumbTrail={selected.name}
          onBack={requestBack}
        />
      ) : (
        <ModuleListShell title="Recovery Plans" breadcrumbTrail="BCMS" actions={listActions} />
      )}

      {!open && !selected ? (
        <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          <RegisterToolbar columnPicker={planColumns.columnPicker} />
          <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
          <RegisterListFrame
            ready={listReady}
            count={paged.count}
            emptyMessage="No recovery plans yet."
            page={paged.page}
            rowsPerPage={paged.rowsPerPage}
            onPageChange={paged.setPage}
            onRowsPerPageChange={paged.setRowsPerPage}
          >
          <TableContainer sx={{ flex: 1 }}>
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  {planColumns.isVisible('name') && (
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
                  {planColumns.isVisible('type') && <TableCell>Type</TableCell>}
                  {planColumns.isVisible('version') && <TableCell>Version</TableCell>}
                  {planColumns.isVisible('status') && <TableCell>Status</TableCell>}
                  {planColumns.isVisible('owner') && <TableCell>Owner</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {paged.paged.map((p) => (
                  <TableRow
                    key={p.id}
                    hover
                    sx={{ cursor: 'pointer' }}
                    onClick={() => setSelected(p)}
                  >
                    {planColumns.isVisible('name') && (
                    <TableCell>
                      <Typography variant="body2" fontWeight={600}>
                        {p.name}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {p.biaIds.length} linked BIA{p.biaIds.length === 1 ? '' : 's'} ·{' '}
                        {p.procedures.length} procedure{p.procedures.length === 1 ? '' : 's'}
                      </Typography>
                    </TableCell>
                    )}
                    {planColumns.isVisible('type') && (
                    <TableCell>
                      <Typography variant="caption">{p.planType.replace(/_/g, ' ')}</Typography>
                    </TableCell>
                    )}
                    {planColumns.isVisible('version') && (
                    <TableCell>
                      <Typography variant="caption">v{p.versionNum}</Typography>
                    </TableCell>
                    )}
                    {planColumns.isVisible('status') && (
                    <TableCell>
                      <StatusChip label={p.status} tone={tone(p.status)} />
                    </TableCell>
                    )}
                    {planColumns.isVisible('owner') && (
                    <TableCell>
                      <Typography variant="caption">{userLabel(p.ownerUserId)}</Typography>
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
            {editId == null ? 'New recovery plan' : 'Edit recovery plan'}
          </Typography>
          <Stack spacing={1.5}>
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 1 }}>
              <CompactTextField
                label="Name"
                required
                fieldIdPrefix="bcms-plan"
                value={form.name}
                onChange={(e) => setForm((s) => ({ ...s, name: e.target.value }))}
              />
              <FormSelect
                label="Type"
                value={form.planType}
                options={PLAN_TYPES.map((t) => ({ value: t, label: t.replace(/_/g, ' ') }))}
                onChange={(v) => setForm((s) => ({ ...s, planType: v as RecoveryPlanType }))}
                fullWidth
              />
            </Box>
            <CompactTextField
              label="Scope"
              multiline
              minRows={2}
              fieldIdPrefix="bcms-plan"
              value={form.scope}
              onChange={(e) => setForm((s) => ({ ...s, scope: e.target.value }))}
            />
            <FormSelect
              label="Owner"
              value={form.ownerUserId === '' ? '' : String(form.ownerUserId)}
              options={[
                { value: '', label: '-' },
                ...users.map((u) => ({ value: String(u.id), label: u.fullName })),
              ]}
              onChange={(v) => setForm((s) => ({ ...s, ownerUserId: v === '' ? '' : Number(v) }))}
              fullWidth
            />
            {editId != null && (
              <CompactTextField
                label="Review date"
                type="date"
                fieldIdPrefix="bcms-plan"
                InputLabelProps={{ shrink: true }}
                inputProps={{ min: dateMin }}
                value={form.reviewDate}
                onChange={(e) => setForm((s) => ({ ...s, reviewDate: e.target.value }))}
                error={!!reviewDateError}
                helperText={reviewDateError}
              />
            )}
            <CompactTextField
              label="Crisis team"
              multiline
              minRows={2}
              fieldIdPrefix="bcms-plan"
              value={form.crisisTeam}
              onChange={(e) => setForm((s) => ({ ...s, crisisTeam: e.target.value }))}
            />
            <CompactTextField
              label="Escalation contacts"
              multiline
              minRows={2}
              fieldIdPrefix="bcms-plan"
              value={form.escalationContacts}
              onChange={(e) => setForm((s) => ({ ...s, escalationContacts: e.target.value }))}
            />
            <CompactTextField
              label="Communication tree"
              multiline
              minRows={2}
              fieldIdPrefix="bcms-plan"
              value={form.communicationTree}
              onChange={(e) => setForm((s) => ({ ...s, communicationTree: e.target.value }))}
            />
            <CompactTextField
              label="Resource requirements"
              multiline
              minRows={2}
              fieldIdPrefix="bcms-plan"
              value={form.resourceRequirements}
              onChange={(e) => setForm((s) => ({ ...s, resourceRequirements: e.target.value }))}
            />
            <CompactTextField
              label="Alternate site details"
              multiline
              minRows={2}
              fieldIdPrefix="bcms-plan"
              value={form.alternateSiteDetails}
              onChange={(e) => setForm((s) => ({ ...s, alternateSiteDetails: e.target.value }))}
            />

            <Box>
              <FormFieldLabel htmlFor="bcms-plan-bias">Linked BIAs (approved only)</FormFieldLabel>
              <Select
                id="bcms-plan-bias"
                multiple
                size="small"
                fullWidth
                value={form.biaIds}
                onChange={(e) => setForm((s) => ({ ...s, biaIds: e.target.value as number[] }))}
                sx={compactSelectSx}
                displayEmpty
                renderValue={(selected) => (
                  <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
                    {(selected as number[]).map((id) => (
                      <Chip key={id} size="small" label={bias.find((b) => b.id === id)?.processName ?? id} />
                    ))}
                  </Stack>
                )}
              >
                {approvedBias.map((b) => (
                  <MenuItem key={b.id} value={b.id}>
                    <Checkbox size="small" checked={form.biaIds.includes(b.id)} />
                    <ListItemText primary={b.processName} secondary={b.categoryLabel ?? ''} />
                  </MenuItem>
                ))}
              </Select>
            </Box>

            <Box>
              <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 0.5 }}>
                <Typography variant="caption" color="text.secondary">
                  Recovery procedures
                </Typography>
                <Button size="small" startIcon={<AddRoundedIcon />} onClick={addProcedure}>
                  Add Step
                </Button>
              </Stack>
              <Stack spacing={1}>
                {procedures.map((proc, idx) => (
                  <FieldActionRow key={idx}>
                    <CompactTextField
                      label="Process"
                      fieldIdPrefix="bcms-plan"
                      value={proc.processName}
                      onChange={(e) => updateProcedure(idx, { processName: e.target.value })}
                      sx={{ flex: 1 }}
                    />
                    <Box sx={{ minWidth: 150 }}>
                      <FormSelect
                        label="Linked BIA"
                        value={proc.biaId == null ? '' : String(proc.biaId)}
                        options={[
                          { value: '', label: '-' },
                          ...approvedBias.map((b) => ({ value: String(b.id), label: b.processName })),
                        ]}
                        onChange={(v) =>
                          updateProcedure(idx, { biaId: v === '' ? null : Number(v) })
                        }
                        fullWidth
                      />
                    </Box>
                    <CompactTextField
                      label="Procedure text"
                      multiline
                      minRows={1}
                      fieldIdPrefix="bcms-plan"
                      value={proc.procedureText ?? ''}
                      onChange={(e) => updateProcedure(idx, { procedureText: e.target.value })}
                      sx={{ flex: 2 }}
                    />
                    <Button size="small" color="error" onClick={() => removeProcedure(idx)} sx={controlActionSx}>
                      <DeleteOutlineRoundedIcon fontSize="small" />
                    </Button>
                  </FieldActionRow>
                ))}
                {procedures.length === 0 && (
                  <Typography variant="caption" color="text.secondary">
                    No steps added yet.
                  </Typography>
                )}
              </Stack>
            </Box>
            <Stack direction="row" spacing={1}>
              <Button onClick={() => setOpen(false)}>Cancel</Button>
              <Button variant="contained" disabled={!form.name.trim() || busy || !!reviewDateError} onClick={() => void save()}>
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
                  {selected.planType.replace(/_/g, ' ')} · v{selected.versionNum} · Owner{' '}
                  {userLabel(selected.ownerUserId)}
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
            {selected.procedures.length > 0 && (
              <Box>
                <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
                  Procedures ({selected.procedures.length})
                </Typography>
                {selected.procedures.map((pr) => (
                  <Typography key={pr.id} variant="body2" sx={{ mb: 0.5 }}>
                    {pr.processName}
                    {pr.procedureText ? `: ${pr.procedureText}` : ''}
                  </Typography>
                ))}
              </Box>
            )}
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
              <Button
                variant="outlined"
                onClick={() => void api.downloadRecoveryPlanPdf(selected.id).catch((e) => onError(String(e)))}
              >
                Download PDF
              </Button>
              {canAdmin && selected.status !== 'EXPIRED' && (
                <Button variant="outlined" onClick={() => openEdit(selected)}>
                  Edit
                </Button>
              )}
              {canAdmin && selected.status === 'DRAFT' && (
                <Button
                  variant="contained"
                  disabled={busy}
                  onClick={() => void run(() => api.submitRecoveryPlan(selected.id), 'Submitted for Owner approval')}
                >
                  Submit for Approval
                </Button>
              )}
              {(canAdmin || isOwner) && selected.status === 'PENDING_OWNER' && (
                <Button
                  variant="contained"
                  disabled={busy}
                  onClick={() => void run(() => api.approveRecoveryPlan(selected.id), 'Plan approved')}
                >
                  Approve
                </Button>
              )}
            </Stack>
          </Stack>
        </Card>
      ) : null}
    </Stack>
  )
}
