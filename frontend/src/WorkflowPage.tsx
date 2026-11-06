import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Box,
  Button,
  Card,
  FormControl,
  InputLabel,
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
import { workflowActionLabel } from './uiQuality/workflowActionLabel'
import { formatTenantDateTime } from './uiQuality/tenantLocale'
import {
  api,
  type Session,
  type WorkflowDefinition,
  type WorkflowInstanceDetail,
} from './api'
import StatusChip from './components/StatusChip'
import { CompactTextField, FormSelect, RegisterListFrame, RegisterSortHeader, RegisterToolbar } from './components/ui'
import { useListReady } from './uiQuality/useListReady'
import { usePagedRows } from './uiQuality/usePagedRows'
import { usePersistedState } from './uiQuality/usePersistedState'
import { compareLoose, toggleSort, type SortDir } from './uiQuality/sortRows'
import { WORKFLOW_REGISTER_COLUMNS } from './hooks/registerColumns'
import { useRegisterColumnPicker } from './hooks/useRegisterColumnPicker'
import { ModuleDetailShell, ModuleListShell } from './hooks/useModulePageShell'
import { brand } from './theme'

const MONO = 'ui-monospace, SFMono-Regular, Menlo, monospace'

export default function WorkflowPage({
  session,
  onError,
  onOk,
}: {
  session: Session
  onError: (msg: string) => void
  onOk: (msg: string) => void
}) {
  const canStart = session.roles.some((r) => ['OWNER', 'ADMIN', 'CONTRIBUTOR'].includes(r))

  const [defs, setDefs] = useState<WorkflowDefinition[]>([])
  const [rows, setRows] = useState<WorkflowInstanceDetail[]>([])
  const [total, setTotal] = useState(0)
  const [statusFilter, setStatusFilter] = usePersistedState('reg-workflow-status', 'OPEN')
  const workflowColumns = useRegisterColumnPicker(WORKFLOW_REGISTER_COLUMNS)
  const [busy, setBusy] = useState(false)
  const { listReady, markReady } = useListReady()
  const [sortKey, setSortKey] = useState('record')
  const [sortDir, setSortDir] = useState<SortDir>('asc')
  const [detail, setDetail] = useState<WorkflowInstanceDetail | null>(null)
  const [startOpen, setStartOpen] = useState(false)
  const [recordId, setRecordId] = useState('')
  const [recordName, setRecordName] = useState('')
  const [defCode, setDefCode] = useState('SIMPLE_APPROVAL')
  const [rejectComment, setRejectComment] = useState('')
  const [pendingAction, setPendingAction] = useState<string | null>(null)

  const load = useCallback(async () => {
    setBusy(true)
    try {
      const [d, page] = await Promise.all([
        api.listWorkflowDefinitions(),
        api.listWorkflowInstances(statusFilter || undefined),
      ])
      setDefs(d)
      setRows(page.content)
      setTotal(page.totalElements)
      if (d.length && !d.find((x) => x.code === defCode)) {
        setDefCode(d[0].code)
      }
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
      markReady()
    }
  }, [statusFilter, onError, defCode, markReady])

  useEffect(() => {
    void load()
  }, [load])

  const openDetail = async (id: number) => {
    try {
      setDetail(await api.getWorkflowInstance(id))
      setRejectComment('')
      setPendingAction(null)
    } catch (e) {
      onError(String(e))
    }
  }

  const runStart = async () => {
    if (!recordId.trim()) {
      onError('Record id is required')
      return
    }
    setBusy(true)
    try {
      const inst = await api.startWorkflow({
        definitionCode: defCode,
        recordId: recordId.trim(),
        recordName: recordName.trim() || undefined,
      })
      setStartOpen(false)
      setRecordId('')
      setRecordName('')
      onOk(`Started ${inst.definitionCode} → ${inst.currentState}`)
      setDetail(inst)
      await load()
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const runTransition = async (action: string, requiresComment: boolean) => {
    if (!detail) return
    if (requiresComment && !rejectComment.trim()) {
      setPendingAction(action)
      return
    }
    setBusy(true)
    try {
      const next = await api.transitionWorkflow(detail.id, {
        action,
        comment: rejectComment.trim() || undefined,
      })
      setDetail(next)
      setRejectComment('')
      setPendingAction(null)
      onOk(`${action} → ${next.currentState}`)
      await load()
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const listActions = useMemo(
    () =>
      canStart ? (
        <Button variant="contained" onClick={() => setStartOpen(true)} disabled={busy}>
          Start Demo
        </Button>
      ) : undefined,
    [canStart, busy],
  )

  const sortedRows = useMemo(
    () => [...rows].sort((a, b) => compareLoose(a.recordName ?? a.recordId, b.recordName ?? b.recordId, sortDir)),
    [rows, sortDir],
  )
  const paged = usePagedRows(sortedRows)

  const requestClose = useCallback(() => {
    setDetail(null)
    setStartOpen(false)
    setRejectComment('')
    setPendingAction(null)
  }, [])

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', minHeight: 0, gap: 1.25 }}>
      {detail || startOpen ? (
        <ModuleDetailShell
          title={detail ? `Workflow #${detail.id}` : 'Start workflow'}
          breadcrumbRootLabel="Workflows"
          breadcrumbTrail={detail ? `${detail.recordType} #${detail.recordId}` : 'Start Demo'}
          onBack={requestClose}
        />
      ) : (
        <ModuleListShell title="Workflows" breadcrumbTrail="Workflows" actions={listActions} />
      )}

      <Card sx={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden' }}>
        {!detail && !startOpen ? (
          <RegisterToolbar
            filters={[
              {
                id: 'status',
                label: 'Status',
                value: statusFilter,
                options: [
                  { value: '', label: 'All' },
                  { value: 'OPEN', label: 'Open' },
                  { value: 'COMPLETED', label: 'Completed' },
                ],
                onChange: setStatusFilter,
                minWidth: 120,
              },
            ]}
            onClearFilters={() => setStatusFilter('')}
            columnPicker={workflowColumns.columnPicker}
          />
        ) : null}

        {!detail && !startOpen ? (
          <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
          <RegisterListFrame
            ready={listReady}
            count={paged.count}
            emptyMessage="No workflow instances. Start a SIMPLE_APPROVAL demo to exercise submit / approve / reject."
            page={paged.page}
            rowsPerPage={paged.rowsPerPage}
            onPageChange={paged.setPage}
            onRowsPerPageChange={paged.setRowsPerPage}
          >
          <TableContainer sx={{ flex: 1, minHeight: 0 }}>
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  {workflowColumns.isVisible('record') && (
                    <RegisterSortHeader
                      label="Record"
                      active={sortKey === 'record'}
                      dir={sortDir}
                      onClick={() => {
                        const next = toggleSort(sortKey, sortDir, 'record')
                        setSortKey(next.key)
                        setSortDir(next.dir)
                      }}
                    />
                  )}
                  {workflowColumns.isVisible('definition') && <TableCell width={140}>Definition</TableCell>}
                  {workflowColumns.isVisible('state') && <TableCell width={130}>State</TableCell>}
                  {workflowColumns.isVisible('status') && <TableCell width={100}>Status</TableCell>}
                  {workflowColumns.isVisible('started') && <TableCell width={160}>Started</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {paged.paged.map((row) => (
                  <TableRow
                    key={row.id}
                    hover
                    sx={{ cursor: 'pointer' }}
                    onClick={() => void openDetail(row.id)}
                  >
                    {workflowColumns.isVisible('record') && (
                    <TableCell>
                      <Typography variant="body2" fontWeight={600}>
                        {row.recordName ?? row.recordId}
                      </Typography>
                      <Typography variant="caption" color="text.secondary" fontFamily={MONO}>
                        {row.moduleCode} · {row.recordType} · {row.recordId}
                      </Typography>
                    </TableCell>
                    )}
                    {workflowColumns.isVisible('definition') && (
                    <TableCell>
                      <Typography variant="caption" fontFamily={MONO}>
                        {row.definitionCode}
                      </Typography>
                    </TableCell>
                    )}
                    {workflowColumns.isVisible('state') && (
                    <TableCell>
                      <StatusChip label={row.currentState} tone="info" />
                    </TableCell>
                    )}
                    {workflowColumns.isVisible('status') && (
                    <TableCell>
                      <StatusChip
                        label={row.status}
                        tone={row.status === 'COMPLETED' ? 'success' : 'neutral'}
                      />
                    </TableCell>
                    )}
                    {workflowColumns.isVisible('started') && (
                    <TableCell>
                      <Typography variant="caption" color="text.secondary">
                        {formatTenantDateTime(row.startedAt)}
                      </Typography>
                    </TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          </RegisterListFrame>
          </Box>
        ) : startOpen ? (
          <Box sx={{ flex: 1, minHeight: 0, overflow: 'auto', p: 2 }}>
            <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 2 }}>
              Start workflow
            </Typography>
            <Stack spacing={1.75} sx={{ maxWidth: 480 }}>
              <FormSelect
                label="Definition"
                fullWidth
                value={defCode}
                onChange={setDefCode}
                options={defs.map((d) => ({ value: d.code, label: `${d.name} (${d.code})` }))}
              />
              <CompactTextField
                label="Record id"
                value={recordId}
                onChange={(e) => setRecordId(e.target.value)}
                placeholder="demo-001"
                fullWidth
                required
              />
              <CompactTextField
                label="Record name (optional)"
                value={recordName}
                onChange={(e) => setRecordName(e.target.value)}
                fullWidth
              />
              <Stack direction="row" spacing={1}>
                <Button onClick={() => setStartOpen(false)} disabled={busy}>
                  Cancel
                </Button>
                <Button variant="contained" onClick={() => void runStart()} disabled={busy}>
                  Start
                </Button>
              </Stack>
            </Stack>
          </Box>
        ) : detail ? (
          <Box sx={{ flex: 1, minHeight: 0, overflow: 'auto', p: 2 }}>
            <Stack spacing={2}>
              <Box>
                <Typography variant="h6" fontWeight={700}>
                  {detail.recordName ?? detail.recordId}
                </Typography>
                <Typography variant="caption" color="text.secondary" fontFamily={MONO} display="block" sx={{ mt: 0.5 }}>
                  {detail.moduleCode} · {detail.recordType} · {detail.recordId}
                </Typography>
                <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mt: 1 }}>
                  <StatusChip label={detail.definitionCode} tone="neutral" />
                  <StatusChip label={detail.currentState} tone="info" />
                  <StatusChip
                    label={detail.status}
                    tone={detail.status === 'COMPLETED' ? 'success' : 'neutral'}
                  />
                </Stack>
              </Box>

              {detail.availableActions.length > 0 && (
                <Box>
                  <Typography variant="subtitle2" sx={{ mb: 1 }}>
                    Available actions
                  </Typography>
                  <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                    {detail.availableActions.map((a) => (
                      <Button
                        key={a.actionCode}
                        variant="contained"
                        color={a.actionCode === 'reject' ? 'warning' : 'primary'}
                        size="small"
                        disabled={busy}
                        onClick={() => void runTransition(a.actionCode, a.requiresComment)}
                      >
                        {workflowActionLabel(a.actionCode)}
                      </Button>
                    ))}
                  </Stack>
                  {(pendingAction || detail.availableActions.some((a) => a.requiresComment)) && (
                    <CompactTextField
                      label="Comment (required for reject)"
                      value={rejectComment}
                      onChange={(e) => setRejectComment(e.target.value)}
                      multiline
                      minRows={2}
                    />
                  )}
                  {pendingAction && (
                    <Button
                      sx={{ mt: 1 }}
                      variant="outlined"
                      disabled={busy || !rejectComment.trim()}
                      onClick={() => void runTransition(pendingAction, true)}
                    >
                      Confirm {pendingAction}
                    </Button>
                  )}
                </Box>
              )}

              <Box>
                <Typography variant="subtitle2" sx={{ mb: 0.75 }}>
                  History
                </Typography>
                {detail.history.length === 0 ? (
                  <Typography variant="body2" color="text.secondary">
                    No transitions yet.
                  </Typography>
                ) : (
                  detail.history.map((h) => (
                    <Typography key={h.id} variant="body2" sx={{ mb: 0.75 }}>
                      <Typography component="span" variant="caption" fontFamily={MONO}>
                        {workflowActionLabel(h.actionCode)}
                      </Typography>{' '}
                      {h.fromState} → {h.toState}
                      {h.commentText ? `: ${h.commentText}` : ''}
                      <Typography component="span" variant="caption" color="text.secondary" display="block">
                        {formatTenantDateTime(h.createdAt)}
                      </Typography>
                    </Typography>
                  ))
                )}
              </Box>
            </Stack>
          </Box>
        ) : null}
      </Card>
    </Box>
  )
}
