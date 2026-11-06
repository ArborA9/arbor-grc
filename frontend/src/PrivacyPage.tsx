import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Box,
  Button,
  Card,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
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
import {
  api,
  ApiError,
  DSR_REQUEST_TYPES,
  type CreatePrivacyDsrBody,
  type CreatePrivacyInventoryBody,
  type DsrRequestType,
  type OrgUnitNode,
  type PrivacyDsr,
  type PrivacyInventory,
  type Session,
  type UserSummary,
} from './api'
import { BreachPanel, ConsentPanel } from './PrivacyF4Panels'
import PrivacyDpiaJourneyPanel from './PrivacyDpiaJourneyPanel'
import PrivacyDpiaQuestionnairePanel from './PrivacyDpiaQuestionnairePanel'
import PrivacyRopaJourneyPanel from './PrivacyRopaJourneyPanel'
import StatusChip from './components/StatusChip'
import ValidatedTextField from './components/ValidatedTextField'
import { CompactTextField, FormSelect, INPUT_LIMIT, RegisterListFrame, RegisterSortHeader, RegisterToolbar, SettingsCard, SimpleFormLayout } from './components/ui'
import { useListReady } from './uiQuality/useListReady'
import { usePagedRows } from './uiQuality/usePagedRows'
import { usePersistedState } from './uiQuality/usePersistedState'
import { compareLoose, toggleSort, type SortDir } from './uiQuality/sortRows'
import {
  PRIVACY_DSR_COLUMNS,
  PRIVACY_INVENTORY_COLUMNS,
} from './hooks/registerColumns'
import { useRegisterColumnPicker } from './hooks/useRegisterColumnPicker'
import { useAppShellPage } from './components/AppShellPageContext'
import { brand } from './theme'
import { emailError, useFormFields } from './validation'

export type PrivacySection =
  | 'inventory'
  | 'ropa'
  | 'dsr'
  | 'dpia'
  | 'dpia-projects'
  | 'dpia-questionnaires'
  | 'consent'
  | 'breach'

function flattenOrg(nodes: OrgUnitNode[]): OrgUnitNode[] {
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

function toneForStatus(status: string): 'success' | 'warning' | 'danger' | 'neutral' | 'info' {
  switch (status) {
    case 'ACTIVE':
    case 'APPROVED':
    case 'COMPLETE':
      return 'success'
    case 'UNDER_REVIEW':
    case 'IN_PROGRESS':
    case 'RECEIVED':
      return 'info'
    case 'DRAFT':
      return 'neutral'
    case 'REJECTED':
    case 'OVERDUE':
    case 'ARCHIVED':
      return 'danger'
    default:
      return 'neutral'
  }
}

export default function PrivacyPage({
  session,
  section,
  onError,
  onOk,
}: {
  session: Session
  section: PrivacySection
  onError: (msg: string) => void
  onOk: (msg: string) => void
}) {
  const canWrite = session.roles.some((r) => r === 'OWNER' || r === 'ADMIN')

  if (section === 'inventory') {
    return <InventoryPanel canWrite={canWrite} onError={onError} onOk={onOk} />
  }
  if (section === 'ropa') {
    return <PrivacyRopaJourneyPanel canWrite={canWrite} onError={onError} onOk={onOk} />
  }
  if (section === 'dpia' || section === 'dpia-projects') {
    return <PrivacyDpiaJourneyPanel session={session} onError={onError} onOk={onOk} />
  }
  if (section === 'dpia-questionnaires') {
    return <PrivacyDpiaQuestionnairePanel session={session} onError={onError} onOk={onOk} />
  }
  if (section === 'consent') {
    return <ConsentPanel canWrite={canWrite} onError={onError} onOk={onOk} />
  }
  if (section === 'breach') {
    return <BreachPanel canWrite={canWrite} onError={onError} onOk={onOk} />
  }
  return <DsrPanel canWrite={canWrite} session={session} onError={onError} onOk={onOk} />
}

function InventoryPanel({
  canWrite,
  onError,
  onOk,
}: {
  canWrite: boolean
  onError: (m: string) => void
  onOk: (m: string) => void
}) {
  const [rows, setRows] = useState<PrivacyInventory[]>([])
  const [orgUnits, setOrgUnits] = useState<OrgUnitNode[]>([])
  const [open, setOpen] = useState(false)
  const [selected, setSelected] = useState<PrivacyInventory | null>(null)
  const [listFilter, setListFilter] = usePersistedState<'ALL' | 'ACTIVE' | 'ARCHIVED'>('reg-privacy-inv-filter', 'ALL')
  const [form, setForm] = useState<CreatePrivacyInventoryBody>({ dataType: '' })
  const inventoryColumns = useRegisterColumnPicker(PRIVACY_INVENTORY_COLUMNS)
  const { listReady, markReady } = useListReady()
  const [sortKey, setSortKey] = useState('dataType')
  const [sortDir, setSortDir] = useState<SortDir>('asc')

  const load = useCallback(async () => {
    try {
      const [inv, org] = await Promise.all([api.listPrivacyInventory(), api.listOrgUnits()])
      setRows(inv)
      setOrgUnits(flattenOrg(org))
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Failed to load inventory')
    } finally {
      markReady()
    }
  }, [onError, markReady])

  useEffect(() => {
    void load()
  }, [load])

  const filteredRows = useMemo(() => {
    const next = listFilter === 'ALL' ? rows : rows.filter((r) => r.status === listFilter)
    return [...next].sort((a, b) => compareLoose(a.dataType, b.dataType, sortDir))
  }, [rows, listFilter, sortDir])
  const paged = usePagedRows(filteredRows)

  const inventoryFilterOptions = useMemo(
    () => [
      { value: 'ALL' as const, label: 'All', count: rows.length },
      { value: 'ACTIVE' as const, label: 'Active', count: rows.filter((r) => r.status === 'ACTIVE').length },
      { value: 'ARCHIVED' as const, label: 'Archived', count: rows.filter((r) => r.status === 'ARCHIVED').length },
    ],
    [rows],
  )

  const save = async () => {
    try {
      await api.createPrivacyInventory(form)
      setOpen(false)
      setForm({ dataType: '' })
      onOk('Inventory record created')
      await load()
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Create failed')
    }
  }

  const archive = async (id: number) => {
    try {
      await api.archivePrivacyInventory(id)
      onOk('Archived')
      setSelected(null)
      await load()
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Archive failed')
    }
  }

  const orgName = (id: number | null) =>
    id == null ? '-' : orgUnits.find((u) => u.id === id)?.name ?? `#${id}`

  const listActions = useMemo(
    () =>
      canWrite ? (
        <Button variant="contained" onClick={() => setOpen(true)}>
          Add Record
        </Button>
      ) : undefined,
    [canWrite],
  )

  const requestBack = useCallback(() => {
    setOpen(false)
    setSelected(null)
  }, [])

  const closeCreate = useCallback(() => {
    setOpen(false)
    setForm({ dataType: '' })
  }, [])

  const createActions = useMemo(
    () => (
      <>
        <Button variant="outlined" size="small" onClick={closeCreate}>
          Cancel
        </Button>
        <Button
          variant="contained"
          size="small"
          disabled={!form.dataType.trim()}
          onClick={() => void save()}
        >
          Create Record
        </Button>
      </>
    ),
    [form.dataType, closeCreate],
  )

  useAppShellPage(
    open
      ? {
          title: 'New Inventory Record',
          breadcrumbTrail: 'New',
          breadcrumbRoot: { label: 'Data Inventory', onClick: closeCreate },
          actions: canWrite ? createActions : undefined,
          hideContextLabel: true,
          hideRoleChips: true,
        }
      : selected
        ? {
            title: selected.dataType,
            breadcrumbTrail: `${selected.displayId} · ${selected.dataType}`,
            breadcrumbRoot: { label: 'Data Inventory', onClick: requestBack },
            hideContextLabel: true,
            hideRoleChips: true,
          }
        : {
            title: 'Data Inventory',
            breadcrumbTrail: 'Privacy',
            actions: listActions,
            hideContextLabel: true,
            hideRoleChips: true,
          },
    [open, selected, createActions, listActions, canWrite, closeCreate, requestBack],
  )

  return (
    <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
      {!open && !selected ? (
        <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          <RegisterToolbar
            filters={[
              {
                id: 'status',
                label: 'Status',
                value: listFilter,
                options: inventoryFilterOptions.map((o) => ({
                  value: o.value,
                  label: `${o.label} (${o.count})`,
                })),
                onChange: (v) => setListFilter(v as 'ALL' | 'ACTIVE' | 'ARCHIVED'),
                minWidth: 140,
              },
            ]}
            onClearFilters={() => setListFilter('ALL')}
            columnPicker={inventoryColumns.columnPicker}
          />
          <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
          <RegisterListFrame
            ready={listReady}
            count={paged.count}
            emptyMessage="No inventory records yet."
            page={paged.page}
            rowsPerPage={paged.rowsPerPage}
            onPageChange={paged.setPage}
            onRowsPerPageChange={paged.setRowsPerPage}
          >
          <TableContainer sx={{ flex: 1 }}>
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  {inventoryColumns.isVisible('id') && <TableCell>ID</TableCell>}
                  {inventoryColumns.isVisible('dataType') && (
                    <RegisterSortHeader
                      label="Data type"
                      active={sortKey === 'dataType'}
                      dir={sortDir}
                      onClick={() => {
                        const next = toggleSort(sortKey, sortDir, 'dataType')
                        setSortKey(next.key)
                        setSortDir(next.dir)
                      }}
                    />
                  )}
                  {inventoryColumns.isVisible('subjects') && <TableCell>Subjects</TableCell>}
                  {inventoryColumns.isVisible('location') && <TableCell>Location</TableCell>}
                  {inventoryColumns.isVisible('country') && <TableCell>Country</TableCell>}
                  {inventoryColumns.isVisible('retention') && <TableCell>Retention</TableCell>}
                  {inventoryColumns.isVisible('status') && <TableCell>Status</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {paged.paged.map((r) => (
                  <TableRow
                    key={r.id}
                    hover
                    sx={{ cursor: 'pointer' }}
                    onClick={() => setSelected(r)}
                  >
                    {inventoryColumns.isVisible('id') && (
                    <TableCell sx={{ fontFamily: 'ui-monospace, Menlo, monospace' }}>{r.displayId}</TableCell>
                    )}
                    {inventoryColumns.isVisible('dataType') && <TableCell>{r.dataType}</TableCell>}
                    {inventoryColumns.isVisible('subjects') && <TableCell>{r.dataSubjects ?? '-'}</TableCell>}
                    {inventoryColumns.isVisible('location') && (
                    <TableCell>{r.storageLocation ?? (r.storageAssetId ? `Asset #${r.storageAssetId}` : '-')}</TableCell>
                    )}
                    {inventoryColumns.isVisible('country') && <TableCell>{r.storageCountry ?? '-'}</TableCell>}
                    {inventoryColumns.isVisible('retention') && <TableCell>{r.retentionPeriod ?? '-'}</TableCell>}
                    {inventoryColumns.isVisible('status') && (
                    <TableCell>
                      <StatusChip label={r.status} tone={toneForStatus(r.status)} />
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
        <SimpleFormLayout>
          <SettingsCard compact title="Inventory details" subtitle="Data inventory for privacy compliance">
            <Stack spacing={1}>
              <CompactTextField
                label="Data type"
                required
                fieldIdPrefix="privacy-inv"
                value={form.dataType}
                onChange={(e) => setForm({ ...form, dataType: e.target.value })}
              />
              <CompactTextField
                label="Data subjects"
                fieldIdPrefix="privacy-inv"
                maxLength={INPUT_LIMIT.dataSubjects}
                value={form.dataSubjects ?? ''}
                onChange={(e) => setForm({ ...form, dataSubjects: e.target.value })}
              />
              <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 1 }}>
                <CompactTextField
                  label="Volume"
                  fieldIdPrefix="privacy-inv"
                  value={form.volume ?? ''}
                  onChange={(e) => setForm({ ...form, volume: e.target.value })}
                />
                <CompactTextField
                  label="Format"
                  fieldIdPrefix="privacy-inv"
                  value={form.format ?? ''}
                  onChange={(e) => setForm({ ...form, format: e.target.value })}
                />
              </Box>
              <CompactTextField
                label="Storage location"
                fieldIdPrefix="privacy-inv"
                maxLength={INPUT_LIMIT.website}
                value={form.storageLocation ?? ''}
                onChange={(e) => setForm({ ...form, storageLocation: e.target.value })}
                helperText="Optional free text; Asset Register link can be added later"
              />
              <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 1 }}>
                <CompactTextField
                  label="Storage country"
                  fieldIdPrefix="privacy-inv"
                  value={form.storageCountry ?? ''}
                  onChange={(e) => setForm({ ...form, storageCountry: e.target.value })}
                />
                <CompactTextField
                  label="Retention period"
                  fieldIdPrefix="privacy-inv"
                  value={form.retentionPeriod ?? ''}
                  onChange={(e) => setForm({ ...form, retentionPeriod: e.target.value })}
                />
              </Box>
              <CompactTextField
                label="Deletion method"
                fieldIdPrefix="privacy-inv"
                value={form.deletionMethod ?? ''}
                onChange={(e) => setForm({ ...form, deletionMethod: e.target.value })}
              />
              <FormSelect
                label="Department"
                value={form.orgUnitId == null ? '' : String(form.orgUnitId)}
                options={[
                  { value: '', label: '-' },
                  ...orgUnits.map((u) => ({ value: String(u.id), label: u.name })),
                ]}
                onChange={(v) =>
                  setForm({ ...form, orgUnitId: v === '' ? null : Number(v) })
                }
                fullWidth
              />
              <CompactTextField
                label="Notes"
                multiline
                minRows={2}
                fieldIdPrefix="privacy-inv"
                maxLength={INPUT_LIMIT.reason}
                value={form.notes ?? ''}
                onChange={(e) => setForm({ ...form, notes: e.target.value })}
              />
            </Stack>
          </SettingsCard>
        </SimpleFormLayout>
      ) : selected ? (
        <Card sx={{ flex: 1, minHeight: 0, overflow: 'auto', p: 2 }}>
          <Stack spacing={2}>
            <Box>
              <Typography variant="h6" fontWeight={700}>
                {selected.displayId}: {selected.dataType}
              </Typography>
              <StatusChip label={selected.status} tone={toneForStatus(selected.status)} sx={{ mt: 1 }} />
            </Box>
            {[
              ['Data subjects', selected.dataSubjects],
              ['Volume', selected.volume],
              ['Format', selected.format],
              ['Storage location', selected.storageLocation ?? (selected.storageAssetId ? `Asset #${selected.storageAssetId}` : null)],
              ['Storage country', selected.storageCountry],
              ['Retention period', selected.retentionPeriod],
              ['Deletion method', selected.deletionMethod],
              ['Department', orgName(selected.orgUnitId)],
              ['Notes', selected.notes],
            ].map(([label, val]) =>
              val ? (
                <Box key={String(label)}>
                  <Typography variant="subtitle2">{label}</Typography>
                  <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                    {val}
                  </Typography>
                </Box>
              ) : null,
            )}
            {canWrite && selected.status === 'ACTIVE' && (
              <Button color="warning" variant="outlined" onClick={() => void archive(selected.id)}>
                Archive Record
              </Button>
            )}
          </Stack>
        </Card>
      ) : null}
    </Stack>
  )
}

function DsrPanel({
  canWrite,
  session,
  onError,
  onOk,
}: {
  canWrite: boolean
  session: Session
  onError: (m: string) => void
  onOk: (m: string) => void
}) {
  const [rows, setRows] = useState<PrivacyDsr[]>([])
  const [users, setUsers] = useState<UserSummary[]>([])
  const [open, setOpen] = useState(false)
  const [selected, setSelected] = useState<PrivacyDsr | null>(null)
  const [listFilter, setListFilter] = usePersistedState<'ALL' | 'OPEN' | 'OVERDUE' | 'COMPLETE'>('reg-privacy-dsr-filter', 'ALL')
  const { listReady, markReady } = useListReady()
  const [sortKey, setSortKey] = useState('subject')
  const [sortDir, setSortDir] = useState<SortDir>('asc')
  const [rejectId, setRejectId] = useState<number | null>(null)
  const [rejectReason, setRejectReason] = useState('')
  const [form, setForm] = useState<CreatePrivacyDsrBody>({
    requestType: 'ACCESS',
    subjectName: '',
    assignedUserId: null,
  })
  const dsrFields = useFormFields<'subjectEmail'>()
  const dsrColumns = useRegisterColumnPicker(PRIVACY_DSR_COLUMNS)

  const load = useCallback(async () => {
    try {
      const [dsrs, us] = await Promise.all([
        api.listPrivacyDsr(),
        canWrite ? api.listUsers() : Promise.resolve([] as UserSummary[]),
      ])
      setRows(dsrs)
      setUsers(us)
      const me = us.find((u) => u.email === session.email)
      setForm((prev) =>
        prev.assignedUserId == null && me ? { ...prev, assignedUserId: me.id } : prev,
      )
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Failed to load DSRs')
    } finally {
      markReady()
    }
  }, [canWrite, onError, session.email, markReady])

  useEffect(() => {
    void load()
  }, [load])

  const openStatuses = new Set(['RECEIVED', 'IN_PROGRESS', 'OVERDUE'])

  const filteredRows = useMemo(() => {
    let next = rows
    if (listFilter === 'OPEN') next = rows.filter((r) => openStatuses.has(r.status))
    else if (listFilter !== 'ALL') next = rows.filter((r) => r.status === listFilter)
    return [...next].sort((a, b) => compareLoose(a.subjectName, b.subjectName, sortDir))
  }, [rows, listFilter, sortDir])
  const paged = usePagedRows(filteredRows)

  const dsrFilterOptions = useMemo(
    () => [
      { value: 'ALL' as const, label: 'All', count: rows.length },
      { value: 'OPEN' as const, label: 'Open', count: rows.filter((r) => openStatuses.has(r.status)).length },
      { value: 'OVERDUE' as const, label: 'Overdue', count: rows.filter((r) => r.status === 'OVERDUE').length },
      { value: 'COMPLETE' as const, label: 'Complete', count: rows.filter((r) => r.status === 'COMPLETE').length },
    ],
    [rows],
  )

  const dsrEmailError = emailError(form.subjectEmail)

  const save = async () => {
    dsrFields.touchAll()
    if (dsrEmailError) return
    try {
      await api.createPrivacyDsr(form)
      setOpen(false)
      dsrFields.reset()
      const me = users.find((u) => u.email === session.email)
      setForm({
        requestType: 'ACCESS',
        subjectName: '',
        assignedUserId: me?.id ?? null,
      })
      onOk('DSR logged: 30-day PDPL clock started')
      await load()
    } catch (e) {
      if (!dsrFields.applyApiError(e)) {
        onError(e instanceof ApiError ? e.message : 'Create failed')
      }
    }
  }

  const act = async (fn: () => Promise<unknown>, ok: string) => {
    try {
      const selId = selected?.id
      await fn()
      onOk(ok)
      const next = await api.listPrivacyDsr()
      setRows(next)
      if (selId != null) setSelected(next.find((r) => r.id === selId) ?? null)
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Action failed')
    }
  }

  const assigneeName = (id: number | null) =>
    id == null ? 'Unassigned' : users.find((u) => u.id === id)?.fullName ?? `#${id}`

  const listActions = useMemo(
    () =>
      canWrite ? (
        <Button variant="contained" onClick={() => setOpen(true)}>
          Log DSR
        </Button>
      ) : undefined,
    [canWrite],
  )

  const requestBack = useCallback(() => {
    setOpen(false)
    setSelected(null)
  }, [])

  const closeCreate = useCallback(() => {
    setOpen(false)
    dsrFields.reset()
    const me = users.find((u) => u.email === session.email)
    setForm({
      requestType: 'ACCESS',
      subjectName: '',
      assignedUserId: me?.id ?? null,
    })
  }, [dsrFields, session.email, users])

  const createActions = useMemo(
    () => (
      <>
        <Button variant="outlined" size="small" onClick={closeCreate}>
          Cancel
        </Button>
        <Button
          variant="contained"
          size="small"
          disabled={!form.subjectName.trim() || !!dsrEmailError}
          onClick={() => void save()}
        >
          Log DSR
        </Button>
      </>
    ),
    [form.subjectName, dsrEmailError, closeCreate],
  )

  useAppShellPage(
    open
      ? {
          title: 'Log DSR',
          breadcrumbTrail: 'New',
          breadcrumbRoot: { label: 'Data Subject Requests', onClick: closeCreate },
          actions: canWrite ? createActions : undefined,
          hideContextLabel: true,
          hideRoleChips: true,
        }
      : selected
        ? {
            title: selected.displayId,
            breadcrumbTrail: `${selected.displayId} · ${selected.requestType}`,
            breadcrumbRoot: { label: 'Data Subject Requests', onClick: requestBack },
            hideContextLabel: true,
            hideRoleChips: true,
          }
        : {
            title: 'Data Subject Requests',
            breadcrumbTrail: 'Privacy',
            actions: listActions,
            hideContextLabel: true,
            hideRoleChips: true,
          },
    [open, selected, createActions, listActions, canWrite, closeCreate, requestBack],
  )

  return (
    <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
      {!open && !selected ? (
        <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          <RegisterToolbar
            filters={[
              {
                id: 'status',
                label: 'Status',
                value: listFilter,
                options: dsrFilterOptions.map((o) => ({
                  value: o.value,
                  label: `${o.label} (${o.count})`,
                })),
                onChange: (v) => setListFilter(v as 'ALL' | 'OPEN' | 'OVERDUE' | 'COMPLETE'),
                minWidth: 140,
              },
            ]}
            onClearFilters={() => setListFilter('ALL')}
            columnPicker={dsrColumns.columnPicker}
          />
          <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
          <RegisterListFrame
            ready={listReady}
            count={paged.count}
            emptyMessage="No DSRs yet."
            page={paged.page}
            rowsPerPage={paged.rowsPerPage}
            onPageChange={paged.setPage}
            onRowsPerPageChange={paged.setRowsPerPage}
          >
          <TableContainer sx={{ flex: 1 }}>
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  {dsrColumns.isVisible('id') && <TableCell>ID</TableCell>}
                  {dsrColumns.isVisible('type') && <TableCell>Type</TableCell>}
                  {dsrColumns.isVisible('subject') && (
                    <RegisterSortHeader
                      label="Subject"
                      active={sortKey === 'subject'}
                      dir={sortDir}
                      onClick={() => {
                        const next = toggleSort(sortKey, sortDir, 'subject')
                        setSortKey(next.key)
                        setSortDir(next.dir)
                      }}
                    />
                  )}
                  {dsrColumns.isVisible('due') && <TableCell>Due</TableCell>}
                  {dsrColumns.isVisible('status') && <TableCell>Status</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {paged.paged.map((r) => (
                  <TableRow
                    key={r.id}
                    hover
                    sx={{ cursor: 'pointer' }}
                    onClick={() => setSelected(r)}
                  >
                    {dsrColumns.isVisible('id') && (
                    <TableCell sx={{ fontFamily: 'ui-monospace, Menlo, monospace' }}>{r.displayId}</TableCell>
                    )}
                    {dsrColumns.isVisible('type') && <TableCell>{r.requestType}</TableCell>}
                    {dsrColumns.isVisible('subject') && (
                    <TableCell>
                      <Typography variant="body2">{r.subjectName}</Typography>
                      {r.subjectEmail && (
                        <Typography variant="caption" color="text.secondary" display="block">
                          {r.subjectEmail}
                        </Typography>
                      )}
                    </TableCell>
                    )}
                    {dsrColumns.isVisible('due') && (
                    <TableCell sx={{ fontFamily: 'ui-monospace, Menlo, monospace' }}>{r.dueAt}</TableCell>
                    )}
                    {dsrColumns.isVisible('status') && (
                    <TableCell>
                      <StatusChip label={r.status} tone={toneForStatus(r.status)} />
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
        <SimpleFormLayout>
          <SettingsCard compact title="DSR intake" subtitle="DSR intake and assignment">
            <Stack spacing={1}>
              <FormSelect
                label="Request type"
                value={form.requestType}
                options={DSR_REQUEST_TYPES.map((t) => ({ value: t, label: t }))}
                onChange={(v) => setForm({ ...form, requestType: v as DsrRequestType })}
                fullWidth
              />
              <CompactTextField
                label="Subject name"
                required
                fieldIdPrefix="privacy-dsr"
                value={form.subjectName}
                onChange={(e) => setForm({ ...form, subjectName: e.target.value })}
              />
              <CompactTextField
                label="Subject email"
                type="email"
                fieldIdPrefix="privacy-dsr"
                value={form.subjectEmail ?? ''}
                onChange={(e) => setForm({ ...form, subjectEmail: e.target.value })}
                error={!!dsrFields.fieldError('subjectEmail', dsrEmailError)}
                helperText={dsrFields.fieldError('subjectEmail', dsrEmailError) || undefined}
                onBlur={() => dsrFields.touch('subjectEmail')}
              />
              <CompactTextField
                label="Subject reference"
                fieldIdPrefix="privacy-dsr"
                value={form.subjectReference ?? ''}
                onChange={(e) => setForm({ ...form, subjectReference: e.target.value })}
              />
              <CompactTextField
                label="Description"
                multiline
                minRows={2}
                fieldIdPrefix="privacy-dsr"
                value={form.description ?? ''}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
              <FormSelect
                label="Assignee"
                value={form.assignedUserId == null ? '' : String(form.assignedUserId)}
                options={[
                  { value: '', label: 'Unassigned' },
                  ...users.map((u) => ({ value: String(u.id), label: u.fullName || u.email })),
                ]}
                onChange={(v) =>
                  setForm({ ...form, assignedUserId: v === '' ? null : Number(v) })
                }
                fullWidth
              />
            </Stack>
          </SettingsCard>
        </SimpleFormLayout>
      ) : selected ? (
        <Card sx={{ flex: 1, minHeight: 0, overflow: 'auto', p: 2 }}>
          <Stack spacing={2}>
            <Box>
              <Typography variant="h6" fontWeight={700}>
                {selected.displayId}: {selected.subjectName}
              </Typography>
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mt: 1 }}>
                <StatusChip label={selected.status} tone={toneForStatus(selected.status)} />
                <Typography variant="caption" color="text.secondary" sx={{ alignSelf: 'center' }}>
                  {selected.requestType} · Due {selected.dueAt} · Assignee {assigneeName(selected.assignedUserId)}
                </Typography>
              </Stack>
            </Box>
            {selected.subjectEmail && (
              <Typography variant="body2" color="text.secondary">
                {selected.subjectEmail}
              </Typography>
            )}
            {selected.description && (
              <Box>
                <Typography variant="subtitle2">Description</Typography>
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                  {selected.description}
                </Typography>
              </Box>
            )}
            {selected.rejectionReason && (
              <Typography variant="body2" color="error.main">
                Rejected: {selected.rejectionReason}
              </Typography>
            )}
            {canWrite && (
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                {selected.status === 'RECEIVED' && (
                  <Button variant="contained" onClick={() => void act(() => api.startPrivacyDsr(selected.id), 'In progress')}>
                    Start
                  </Button>
                )}
                {openStatuses.has(selected.status) && (
                  <>
                    <Button variant="contained" onClick={() => void act(() => api.completePrivacyDsr(selected.id), 'Completed')}>
                      Complete
                    </Button>
                    <Button color="error" variant="outlined" onClick={() => setRejectId(selected.id)}>
                      Reject
                    </Button>
                  </>
                )}
              </Stack>
            )}
          </Stack>
        </Card>
      ) : null}

      <Dialog open={rejectId != null} onClose={() => setRejectId(null)} fullWidth maxWidth="xs">
        <DialogTitle>Reject DSR</DialogTitle>
        <DialogContent>
          <CompactTextField
            label="Legal reason"
            required
            multiline
            minRows={2}
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectId(null)}>Cancel</Button>
          <Button
            color="error"
            variant="contained"
            disabled={rejectId == null || !rejectReason.trim()}
            onClick={() => {
              if (rejectId == null) return
              void act(() => api.rejectPrivacyDsr(rejectId, rejectReason.trim()), 'Rejected').then(
                () => {
                  setRejectId(null)
                  setRejectReason('')
                },
              )
            }}
          >
            Reject
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}
