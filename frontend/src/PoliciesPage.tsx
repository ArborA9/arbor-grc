import { useCallback, useEffect, useMemo, useState } from 'react'
import { frameworkDisplayName } from './uiQuality/frameworkLabel'
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
  POLICY_CATEGORY_LABELS,
  type FrameworkSummary,
  type OrgUnitNode,
  type PolicyAckScopeType,
  type PolicyAcknowledgement,
  type PolicyCategory,
  type PolicyDetail,
  type PolicyStatus,
  type PolicySummary,
  type Session,
  type UserSummary,
} from './api'
import StatusChip from './components/StatusChip'
import DiscardChangesDialog from './components/DiscardChangesDialog'
import PolicyCreateEditForm, { POLICY_FORM_SECTIONS } from './components/policy/PolicyCreateEditForm'
import { useAppShellPage } from './components/AppShellPageContext'
import { CompactTextField, FormSectionShell, RegisterListFrame, RegisterSortHeader, RegisterToolbar } from './components/ui'
import { useListReady } from './uiQuality/useListReady'
import { usePagedRows } from './uiQuality/usePagedRows'
import { compareLoose, toggleSort, type SortDir } from './uiQuality/sortRows'
import { formatTenantDateTime } from './uiQuality/tenantLocale'
import { POLICY_REGISTER_COLUMNS } from './hooks/registerColumns'
import { useRegisterColumnPicker } from './hooks/useRegisterColumnPicker'
import { brand } from './theme'
import { actionDueDateErrorIfChanged, todayIso } from './validation'

const STATUSES: PolicyStatus[] = ['DRAFT', 'UNDER_REVIEW', 'PENDING_OWNER', 'APPROVED', 'DUE_FOR_REVIEW']
const ACK_SCOPE_LABELS: Record<PolicyAckScopeType, string> = {
  ALL_ACTIVE_USERS: 'All active users',
  ORG_UNITS: 'Selected organisation units',
  SELECTED_USERS: 'Selected users',
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

function tone(s: PolicyStatus): 'success' | 'warning' | 'info' | 'danger' | 'neutral' {
  if (s === 'APPROVED') return 'success'
  if (s === 'PENDING_OWNER') return 'warning'
  if (s === 'UNDER_REVIEW') return 'info'
  if (s === 'DUE_FOR_REVIEW') return 'danger'
  return 'neutral'
}

const emptyForm = {
  title: '',
  category: '' as PolicyCategory | '',
  orgUnitId: '' as number | '',
  bodyText: '',
  reviewDate: '',
  ackRequired: true,
  ackScopeType: 'ALL_ACTIVE_USERS' as PolicyAckScopeType,
  ackDeadline: '',
  ownerUserId: '' as number | '',
  frameworkIds: [] as number[],
  ackOrgUnitIds: [] as number[],
  ackUserIds: [] as number[],
}

export default function PoliciesPage({
  session,
  onError,
  onOk,
}: {
  session: Session
  onError: (msg: string) => void
  onOk: (msg: string) => void
}) {
  const canAdmin = session.roles.some((r) => ['OWNER', 'ADMIN'].includes(r))
  const canApprove = session.roles.includes('OWNER')
  const canViewAcks = session.roles.some((r) => ['OWNER', 'ADMIN', 'GLOBAL_READER', 'INTERNAL_AUDITOR'].includes(r))

  const [rows, setRows] = useState<PolicySummary[]>([])
  const [categories, setCategories] = useState<PolicyCategory[]>([])
  const [units, setUnits] = useState<OrgUnitNode[]>([])
  const [users, setUsers] = useState<UserSummary[]>([])
  const [frameworks, setFrameworks] = useState<FrameworkSummary[]>([])
  const [categoryFilter, setCategoryFilter] = useState<PolicyCategory | ''>('')
  const [statusFilter, setStatusFilter] = useState<PolicyStatus | ''>('')
  const policyColumns = useRegisterColumnPicker(POLICY_REGISTER_COLUMNS)

  const [detail, setDetail] = useState<PolicyDetail | null>(null)
  const [acks, setAcks] = useState<PolicyAcknowledgement[]>([])
  const [busy, setBusy] = useState(false)
  const { listReady, markReady } = useListReady()
  const [sortKey, setSortKey] = useState('title')
  const [sortDir, setSortDir] = useState<SortDir>('asc')

  const [formOpen, setFormOpen] = useState(false)
  const [formId, setFormId] = useState<number | null>(null)
  const [form, setForm] = useState(emptyForm)
  const [baselineForm, setBaselineForm] = useState('')
  const [discardOpen, setDiscardOpen] = useState(false)

  const [rejectOpen, setRejectOpen] = useState(false)
  const [rejectReason, setRejectReason] = useState('')

  const [draftFile, setDraftFile] = useState<File | null>(null)

  const load = useCallback(async () => {
    setBusy(true)
    try {
      const [cats, list] = await Promise.all([
        api.listPolicyCategories(),
        api.listPolicies({
          category: categoryFilter || undefined,
          status: statusFilter || undefined,
        }),
      ])
      setCategories(cats)
      setRows(list)
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
      markReady()
    }
  }, [categoryFilter, statusFilter, onError, markReady])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    api.listOrgUnits().then((tree) => setUnits(flatten(tree).filter((u) => u.active))).catch(() => setUnits([]))
    api.listUsers().then(setUsers).catch(() => setUsers([]))
    api.listActivatedFrameworks().then(setFrameworks).catch(() => setFrameworks([]))
  }, [])

  const openDetail = async (id: number) => {
    try {
      const d = await api.getPolicy(id)
      setDetail(d)
      setDraftFile(null)
      if (canViewAcks) {
        try {
          setAcks(await api.listPolicyAcknowledgements(id))
        } catch {
          setAcks([])
        }
      } else {
        setAcks([])
      }
    } catch (e) {
      onError(String(e))
    }
  }

  const refreshDetail = async (id: number) => {
    await openDetail(id)
    await load()
  }

  const openCreate = () => {
    const next = { ...emptyForm, category: categories[0] ?? '' }
    setForm(next)
    setFormId(null)
    setBaselineForm(JSON.stringify(next))
    setFormOpen(true)
  }

  const openEdit = (d: PolicyDetail) => {
    const next = {
      title: d.policy.title,
      category: d.policy.category,
      orgUnitId: (d.policy.orgUnitId ?? '') as number | '',
      bodyText: d.bodyDraft ?? '',
      reviewDate: d.policy.reviewDate ?? '',
      ackRequired: d.policy.ackRequired,
      ackScopeType: d.policy.ackScopeType,
      ackDeadline: d.policy.ackDeadline ?? '',
      ownerUserId: (d.policy.ownerUserId ?? '') as number | '',
      frameworkIds: d.frameworks.map((f) => f.frameworkId),
      ackOrgUnitIds: d.ackOrgUnitIds,
      ackUserIds: d.ackUserIds,
    }
    setForm(next)
    setFormId(d.policy.id)
    setBaselineForm(JSON.stringify(next))
    setFormOpen(true)
  }

  const isFormDirty = useMemo(() => formOpen && JSON.stringify(form) !== baselineForm, [formOpen, form, baselineForm])

  const baselineAckDeadline = useMemo(() => {
    try {
      return (JSON.parse(baselineForm) as { ackDeadline?: string }).ackDeadline ?? ''
    } catch {
      return ''
    }
  }, [baselineForm])

  const ackDeadlineError = useMemo(() => {
    if (!form.ackRequired) return ''
    return actionDueDateErrorIfChanged(baselineAckDeadline, form.ackDeadline)
  }, [form.ackRequired, form.ackDeadline, baselineAckDeadline])

  const baselineReviewDate = useMemo(() => {
    try {
      return (JSON.parse(baselineForm) as { reviewDate?: string }).reviewDate ?? ''
    } catch {
      return ''
    }
  }, [baselineForm])

  const reviewDateError = useMemo(
    () => actionDueDateErrorIfChanged(baselineReviewDate, form.reviewDate),
    [baselineReviewDate, form.reviewDate],
  )

  const dateMin = todayIso()

  const requestCloseForm = () => {
    if (isFormDirty) {
      setDiscardOpen(true)
      return
    }
    setFormOpen(false)
  }

  const discardForm = () => {
    setDiscardOpen(false)
    setFormOpen(false)
  }

  const saveForm = async () => {
    if (!form.title.trim() || !form.category) return
    if (ackDeadlineError) return
    if (reviewDateError) return
    if (form.ackRequired && form.ackScopeType === 'ORG_UNITS' && form.ackOrgUnitIds.length === 0) {
      onError('Select at least one organisation unit for the acknowledgement scope')
      return
    }
    if (form.ackRequired && form.ackScopeType === 'SELECTED_USERS' && form.ackUserIds.length === 0) {
      onError('Select at least one user for the acknowledgement scope')
      return
    }
    const body = {
      title: form.title.trim(),
      category: form.category as PolicyCategory,
      orgUnitId: form.orgUnitId === '' ? null : form.orgUnitId,
      bodyText: form.bodyText.trim() || null,
      reviewDate: form.reviewDate || null,
      ackRequired: form.ackRequired,
      ackScopeType: form.ackScopeType,
      ackDeadline: form.ackDeadline || null,
      ownerUserId: form.ownerUserId === '' ? null : form.ownerUserId,
      frameworkIds: form.frameworkIds,
      ackOrgUnitIds: form.ackScopeType === 'ORG_UNITS' ? form.ackOrgUnitIds : [],
      ackUserIds: form.ackScopeType === 'SELECTED_USERS' ? form.ackUserIds : [],
    }
    setBusy(true)
    try {
      const d = formId == null ? await api.createPolicy(body) : await api.updatePolicy(formId, body)
      setFormOpen(false)
      onOk(formId == null ? 'Policy created' : 'Policy saved')
      await refreshDetail(d.policy.id)
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const uploadFile = async () => {
    if (!detail || !draftFile) return
    setBusy(true)
    try {
      await api.uploadPolicyFile(detail.policy.id, draftFile)
      setDraftFile(null)
      onOk('File attached to draft')
      await refreshDetail(detail.policy.id)
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const run = async (fn: () => Promise<PolicyDetail>, okMsg: string) => {
    setBusy(true)
    try {
      const d = await fn()
      onOk(okMsg)
      await refreshDetail(d.policy.id)
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const reject = async () => {
    if (!detail || !rejectReason.trim()) return
    setBusy(true)
    try {
      const d = await api.rejectPolicy(detail.policy.id, rejectReason.trim())
      setRejectOpen(false)
      setRejectReason('')
      onOk('Policy sent back to draft')
      await refreshDetail(d.policy.id)
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const userLabel = (id: number | null) => {
    if (id == null) return '-'
    return users.find((u) => u.id === id)?.fullName ?? `User #${id}`
  }
  const unitLabel = (id: number | null) => {
    if (id == null) return '-'
    const u = units.find((x) => x.id === id)
    return u ? `L${u.level} · ${u.name}` : `Unit #${id}`
  }

  const editable = detail != null && canAdmin && (detail.policy.status === 'DRAFT' || detail.policy.status === 'DUE_FOR_REVIEW')

  const ackProgress = useMemo(() => {
    if (!detail) return null
    if (!detail.policy.ackRequired) return null
    return `${detail.policy.ackCount} / ${detail.policy.ackExpected} acknowledged`
  }, [detail])

  const policyDetailPanel = detail ? (
    <Stack spacing={1.5}>
      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
        <Typography variant="subtitle2">{detail.policy.title}</Typography>
        <StatusChip label={detail.policy.status.replace(/_/g, ' ')} tone={tone(detail.policy.status)} />
        <Chip size="small" variant="outlined" label={detail.policy.versionLabel} />
      </Stack>
      <Typography variant="caption" color="text.secondary">
        {POLICY_CATEGORY_LABELS[detail.policy.category]} · Org unit {unitLabel(detail.policy.orgUnitId)} · Owner{' '}
        {userLabel(detail.policy.ownerUserId)} · Review {detail.policy.reviewDate ?? '-'}
      </Typography>

      {detail.rejectReason && (
        <Typography variant="body2" color="error">
          Sent back: {detail.rejectReason}
        </Typography>
      )}

      {detail.frameworks.length > 0 && (
        <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
          {detail.frameworks.map((f) => (
            <Chip
              key={f.frameworkId}
              size="small"
              label={frameworkDisplayName(f.frameworkName, f.frameworkCode)}
            />
          ))}
        </Stack>
      )}

      <Box>
        <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 0.5 }}>
          Body
        </Typography>
        <Card variant="outlined" sx={{ p: 1.5, bgcolor: brand.canvas, whiteSpace: 'pre-wrap' }}>
          <Typography variant="body2">
            {detail.currentVersion?.bodyText ?? detail.bodyDraft ?? 'No body text.'}
          </Typography>
        </Card>
      </Box>

      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
        {(detail.currentVersion?.hasFile || detail.hasDraftFile) && (
          <Button
            size="small"
            variant="outlined"
            onClick={() =>
              void api
                .downloadPolicyFile(
                  detail.policy.id,
                  detail.currentVersion?.originalFilename ?? detail.draftFilename ?? undefined,
                )
                .catch((e) => onError(String(e)))
            }
          >
            Download {detail.currentVersion?.originalFilename ?? detail.draftFilename}
          </Button>
        )}
        {editable && (
          <>
            <Button variant="outlined" component="label" size="small" disabled={busy}>
              Choose Draft File
              <input hidden type="file" onChange={(e) => setDraftFile(e.target.files?.[0] ?? null)} />
            </Button>
            {draftFile && (
              <>
                <Typography variant="caption" color="text.secondary">
                  {draftFile.name}
                </Typography>
                <Button size="small" variant="contained" disabled={busy} onClick={() => void uploadFile()}>
                  Attach File
                </Button>
              </>
            )}
          </>
        )}
      </Stack>

      <Typography variant="caption" color="text.secondary">
        Acknowledgement: {detail.policy.ackRequired ? ACK_SCOPE_LABELS[detail.policy.ackScopeType] : 'Not required'}
        {detail.policy.ackRequired && detail.policy.ackDeadline ? ` · due ${detail.policy.ackDeadline}` : ''}
        {ackProgress ? ` · ${ackProgress}` : ''}
      </Typography>

      {canViewAcks && detail.policy.ackRequired && (
        <Box>
          <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 0.5 }}>
            <Typography variant="caption" color="text.secondary">
              Acknowledged by ({acks.length})
            </Typography>
            {acks.length > 0 && (
              <Button
                size="small"
                disabled={busy}
                onClick={() => void api.exportPolicyAcknowledgementsCsv(detail.policy.id)}
              >
                Export CSV
              </Button>
            )}
          </Stack>
          {acks.length === 0 ? (
            <Typography variant="caption" color="text.secondary">
              No acknowledgements yet
            </Typography>
          ) : (
            <Stack spacing={0.25}>
              {acks.map((a) => (
                <Typography key={a.id} variant="caption">
                  {a.userFullName ?? userLabel(a.userId)}
                  {a.userEmail ? ` · ${a.userEmail}` : ''} · {formatTenantDateTime(a.acknowledgedAt)}
                </Typography>
              ))}
            </Stack>
          )}
        </Box>
      )}

      <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
        {editable && (
          <Button variant="outlined" disabled={busy} onClick={() => openEdit(detail)}>
            Edit
          </Button>
        )}
        {canAdmin && (detail.policy.status === 'DRAFT' || detail.policy.status === 'DUE_FOR_REVIEW') && (
          <Button
            variant="contained"
            disabled={busy}
            onClick={() => void run(() => api.submitPolicy(detail.policy.id), 'Submitted for review')}
          >
            Submit for Review
          </Button>
        )}
        {canAdmin && detail.policy.status === 'UNDER_REVIEW' && (
          <Button
            variant="contained"
            disabled={busy}
            onClick={() => void run(() => api.recommendPolicy(detail.policy.id), 'Recommended to Owner')}
          >
            Recommend to Owner
          </Button>
        )}
        {canAdmin && (detail.policy.status === 'UNDER_REVIEW' || detail.policy.status === 'PENDING_OWNER') && (
          <Button variant="outlined" color="error" disabled={busy} onClick={() => setRejectOpen(true)}>
            Send Back
          </Button>
        )}
        {canApprove && (detail.policy.status === 'PENDING_OWNER' || detail.policy.status === 'UNDER_REVIEW') && (
          <Button
            variant="contained"
            color="success"
            disabled={busy}
            onClick={() => void run(() => api.approvePolicy(detail.policy.id), 'Policy approved')}
          >
            Approve
          </Button>
        )}
        {(detail.policy.status === 'APPROVED' || detail.policy.status === 'DUE_FOR_REVIEW') &&
          detail.policy.ackRequired &&
          !detail.currentUserAcknowledged && (
            <Button
              variant="contained"
              disabled={busy}
              onClick={() => void run(() => api.acknowledgePolicy(detail.policy.id), 'Acknowledged')}
            >
              Acknowledge
            </Button>
          )}
        {detail.currentUserAcknowledged && <StatusChip label="You acknowledged" tone="success" />}
      </Stack>
    </Stack>
  ) : null

  const saveDisabled =
    !form.title.trim() || !form.category || busy || !!ackDeadlineError || !!reviewDateError

  const listActions = useMemo(
    () =>
      canAdmin ? (
        <Button variant="contained" size="small" disabled={busy || categories.length === 0} onClick={openCreate}>
          New Policy
        </Button>
      ) : undefined,
    [canAdmin, busy, categories.length],
  )

  const formTopbarActions = useMemo(
    () => (
      <Stack direction="row" spacing={1}>
        <Button size="small" variant="outlined" onClick={requestCloseForm}>
          Cancel
        </Button>
        <Button size="small" variant="contained" disabled={saveDisabled} onClick={() => void saveForm()}>
          Save
        </Button>
      </Stack>
    ),
    [saveDisabled, requestCloseForm],
  )

  useAppShellPage(
    formOpen
      ? {
          title: formId == null ? 'New Policy' : 'Edit Policy',
          breadcrumbTrail: formId == null ? 'New Policy' : 'Edit Policy',
          breadcrumbRoot: { label: 'Policy Repository', onClick: requestCloseForm },
          actions: formTopbarActions,
          hideContextLabel: true,
          hideRoleChips: true,
        }
      : detail
        ? {
            title: detail.policy.title,
            breadcrumbTrail: detail.policy.title,
            breadcrumbRoot: { label: 'Policy Repository', onClick: () => setDetail(null) },
            hideContextLabel: true,
            hideRoleChips: true,
          }
        : {
            title: 'Policy Repository',
            breadcrumbTrail: 'Policies',
            actions: listActions,
            hideContextLabel: true,
            hideRoleChips: true,
          },
    [formOpen, formId, formTopbarActions, requestCloseForm, detail, listActions],
  )

  const sortedRows = useMemo(
    () => [...rows].sort((a, b) => compareLoose(a.title, b.title, sortDir)),
    [rows, sortDir],
  )
  const paged = usePagedRows(sortedRows)

  const clearPolicyFilters = useCallback(() => {
    setCategoryFilter('')
    setStatusFilter('')
  }, [])

  return (
    <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
      {formOpen ? (
        <FormSectionShell sections={[...POLICY_FORM_SECTIONS]}>
          <PolicyCreateEditForm
            form={form}
            setForm={setForm}
            categories={categories}
            units={units}
            users={users}
            frameworks={frameworks}
            reviewDateError={reviewDateError}
            ackDeadlineError={ackDeadlineError}
            dateMin={dateMin}
          />
        </FormSectionShell>
      ) : !detail ? (
        <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          <RegisterToolbar
            filters={[
              {
                id: 'category',
                label: 'Category',
                value: categoryFilter || 'ALL',
                options: [
                  { value: 'ALL', label: 'All categories' },
                  ...categories.map((c) => ({ value: c, label: POLICY_CATEGORY_LABELS[c] })),
                ],
                onChange: (v) => setCategoryFilter(v === 'ALL' ? '' : (v as PolicyCategory)),
                minWidth: 150,
              },
              {
                id: 'status',
                label: 'Status',
                value: statusFilter || 'ALL',
                options: [
                  { value: 'ALL', label: 'All statuses' },
                  ...STATUSES.map((s) => ({ value: s, label: s.replace(/_/g, ' ') })),
                ],
                onChange: (v) => setStatusFilter(v === 'ALL' ? '' : (v as PolicyStatus)),
                minWidth: 160,
              },
            ]}
            onClearFilters={clearPolicyFilters}
            columnPicker={policyColumns.columnPicker}
          />
          <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
          <RegisterListFrame
            ready={listReady}
            count={paged.count}
            emptyMessage="No policies yet."
            page={paged.page}
            rowsPerPage={paged.rowsPerPage}
            onPageChange={paged.setPage}
            onRowsPerPageChange={paged.setRowsPerPage}
          >
          <TableContainer sx={{ flex: 1 }}>
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  {policyColumns.isVisible('title') && (
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
                  {policyColumns.isVisible('category') && <TableCell>Category</TableCell>}
                  {policyColumns.isVisible('version') && <TableCell>Version</TableCell>}
                  {policyColumns.isVisible('status') && <TableCell>Status</TableCell>}
                  {policyColumns.isVisible('ack') && <TableCell>Ack</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {paged.paged.map((p) => (
                  <TableRow
                    key={p.id}
                    hover
                    sx={{ cursor: 'pointer' }}
                    onClick={() => void openDetail(p.id)}
                  >
                    {policyColumns.isVisible('title') && (
                    <TableCell>
                      <Typography variant="body2" fontWeight={600} noWrap title={p.title}>
                        {p.title}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Reviews {p.reviewDate ?? '-'}
                      </Typography>
                    </TableCell>
                    )}
                    {policyColumns.isVisible('category') && (
                    <TableCell>
                      <Typography variant="caption">{POLICY_CATEGORY_LABELS[p.category]}</Typography>
                    </TableCell>
                    )}
                    {policyColumns.isVisible('version') && (
                    <TableCell>
                      <Typography variant="caption" fontFamily="ui-monospace, SFMono-Regular, Menlo, monospace">
                        {p.versionLabel}
                      </Typography>
                    </TableCell>
                    )}
                    {policyColumns.isVisible('status') && (
                    <TableCell>
                      <StatusChip label={p.status.replace(/_/g, ' ')} tone={tone(p.status)} />
                    </TableCell>
                    )}
                    {policyColumns.isVisible('ack') && (
                    <TableCell>
                      <Typography variant="caption" color="text.secondary">
                        {p.ackRequired ? `${p.ackCount}/${p.ackExpected}` : '-'}
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
        </Card>
      ) : (
        <Card sx={{ flex: 1, minHeight: 0, overflow: 'auto', p: 2 }}>{policyDetailPanel}</Card>
      )}

      <Dialog open={rejectOpen} onClose={() => setRejectOpen(false)} fullWidth maxWidth="xs">
        <DialogTitle>Send Back to Draft</DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 1 }}>
            <CompactTextField
              label="Reason"
              required
              multiline
              minRows={2}
              fieldIdPrefix="policy-reject"
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectOpen(false)}>Cancel</Button>
          <Button variant="contained" color="error" disabled={!rejectReason.trim() || busy} onClick={() => void reject()}>
            Send Back
          </Button>
        </DialogActions>
      </Dialog>

      <DiscardChangesDialog
        open={discardOpen}
        title="Discard unsaved policy changes?"
        message="You have unsaved changes on this policy. If you leave now, your edits will be lost."
        onKeepEditing={() => setDiscardOpen(false)}
        onDiscard={discardForm}
      />
    </Stack>
  )
}
