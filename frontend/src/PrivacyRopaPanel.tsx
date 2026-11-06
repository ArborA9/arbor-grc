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
  FormControlLabel,
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
  ROPA_DESIGNATIONS,
  ROPA_DPIA_STATUSES,
  ROPA_FREQUENCIES,
  ROPA_LEGAL_BASES,
  ROPA_VOLUME_BUCKETS,
  type CreatePrivacyRopaBody,
  type PrivacyRopa,
  type RopaDesignation,
  type RopaDpiaStatus,
  type RopaIntakeMode,
  type RopaLegalBasis,
  type UserSummary,
} from './api'
import StatusChip from './components/StatusChip'
import { CompactTextField, FormSelect, RegisterToolbar, SettingsCard, complexFormContainerSx } from './components/ui'
import { PRIVACY_ROPA_COLUMNS } from './hooks/registerColumns'
import { useRegisterColumnPicker } from './hooks/useRegisterColumnPicker'
import { useAppShellPage } from './components/AppShellPageContext'
import { brand } from './theme'

function toneForStatus(status: string): 'success' | 'warning' | 'danger' | 'neutral' | 'info' {
  switch (status) {
    case 'APPROVED':
      return 'success'
    case 'UNDER_REVIEW':
    case 'ASSIGNED':
      return 'info'
    case 'DRAFT':
      return 'neutral'
    case 'REJECTED':
      return 'danger'
    default:
      return 'neutral'
  }
}

const emptyForm = (): CreatePrivacyRopaBody => ({
  activityName: '',
  legalBasis: 'CONSENT',
  designation: 'CONTROLLER',
  thirdCountryTransfers: false,
  intakeMode: 'FORM',
  dpiaStatus: 'NOT_REQUIRED',
  shareInternal: false,
  shareOperator: false,
  shareExternal: false,
})

export default function PrivacyRopaPanel({
  canWrite,
  onError,
  onOk,
}: {
  canWrite: boolean
  onError: (m: string) => void
  onOk: (m: string) => void
}) {
  const [rows, setRows] = useState<PrivacyRopa[]>([])
  const [users, setUsers] = useState<UserSummary[]>([])
  const [open, setOpen] = useState(false)
  const [selected, setSelected] = useState<PrivacyRopa | null>(null)
  const [listFilter, setListFilter] = useState<'ALL' | 'DRAFT' | 'ASSIGNED' | 'UNDER_REVIEW' | 'APPROVED'>('ALL')
  const [rejectId, setRejectId] = useState<number | null>(null)
  const [rejectNotes, setRejectNotes] = useState('')
  const [assignUserId, setAssignUserId] = useState<number | ''>('')
  const [assignDue, setAssignDue] = useState('')
  const [form, setForm] = useState<CreatePrivacyRopaBody>(emptyForm)
  const ropaColumns = useRegisterColumnPicker(PRIVACY_ROPA_COLUMNS)
  const workbookMode = form.intakeMode === 'WORKBOOK'

  const load = useCallback(async () => {
    try {
      const [ropa, userList] = await Promise.all([
        api.listPrivacyRopa(),
        canWrite ? api.listUsers() : Promise.resolve([] as UserSummary[]),
      ])
      setRows(ropa)
      setUsers(userList.filter((u) => u.roles.includes('CONTRIBUTOR')))
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Failed to load ROPA')
    }
  }, [canWrite, onError])

  useEffect(() => {
    void load()
  }, [load])

  const filteredRows = useMemo(() => {
    if (listFilter === 'ALL') return rows
    return rows.filter((r) => r.status === listFilter)
  }, [rows, listFilter])

  const ropaFilterOptions = useMemo(
    () => [
      { value: 'ALL' as const, label: 'All', count: rows.length },
      { value: 'DRAFT' as const, label: 'Draft', count: rows.filter((r) => r.status === 'DRAFT').length },
      { value: 'ASSIGNED' as const, label: 'Assigned', count: rows.filter((r) => r.status === 'ASSIGNED').length },
      {
        value: 'UNDER_REVIEW' as const,
        label: 'In review',
        count: rows.filter((r) => r.status === 'UNDER_REVIEW').length,
      },
      { value: 'APPROVED' as const, label: 'Approved', count: rows.filter((r) => r.status === 'APPROVED').length },
    ],
    [rows],
  )

  const save = async () => {
    try {
      await api.createPrivacyRopa(form)
      setOpen(false)
      setForm(emptyForm())
      onOk(workbookMode ? 'RoPA workbook shell created: assign a Contributor' : 'ROPA activity created')
      await load()
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Create failed')
    }
  }

  const act = async (fn: () => Promise<unknown>, ok: string) => {
    try {
      const selId = selected?.id
      await fn()
      onOk(ok)
      const next = await api.listPrivacyRopa()
      setRows(next)
      if (selId != null) setSelected(next.find((r) => r.id === selId) ?? null)
    } catch (e) {
      onError(e instanceof Error ? e.message : 'Action failed')
    }
  }

  const listActions = useMemo(
    () =>
      canWrite ? (
        <Stack direction="row" spacing={1}>
          <Button
            variant="outlined"
            size="small"
            onClick={() =>
              void api.downloadPrivacyRopaTemplate().catch((e) => onError(e instanceof Error ? e.message : 'Download failed'))
            }
          >
            Download Template
          </Button>
          <Button variant="contained" onClick={() => setOpen(true)}>
            New Activity
          </Button>
        </Stack>
      ) : undefined,
    [canWrite, onError],
  )

  const requestBack = useCallback(() => {
    setOpen(false)
    setSelected(null)
  }, [])

  const closeCreate = useCallback(() => {
    setOpen(false)
    setForm(emptyForm())
  }, [])

  const createActions = useMemo(
    () => (
      <>
        <Button variant="outlined" size="small" onClick={closeCreate}>
          Cancel
        </Button>
        <Button variant="contained" size="small" disabled={!form.activityName.trim()} onClick={() => void save()}>
          Create Activity
        </Button>
      </>
    ),
    [form.activityName, closeCreate],
  )

  useAppShellPage(
    open
      ? {
          title: 'New ROPA Activity',
          breadcrumbTrail: 'New',
          breadcrumbRoot: { label: 'ROPA', onClick: closeCreate },
          actions: canWrite ? createActions : undefined,
          hideContextLabel: true,
          hideRoleChips: true,
        }
      : selected
        ? {
            title: selected.activityName,
            breadcrumbTrail: `${selected.displayId} · ${selected.activityName}`,
            breadcrumbRoot: { label: 'ROPA', onClick: requestBack },
            hideContextLabel: true,
            hideRoleChips: true,
          }
        : {
            title: 'ROPA',
            breadcrumbTrail: 'Privacy',
            actions: listActions,
            hideContextLabel: true,
            hideRoleChips: true,
          },
    [open, selected, createActions, listActions, canWrite, closeCreate, requestBack],
  )

  const sectionTitle = (t: string) => (
    <Typography variant="subtitle2" sx={{ mt: 1, mb: 0.5, fontWeight: 700 }}>
      {t}
    </Typography>
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
                options: ropaFilterOptions.map((o) => ({
                  value: o.value,
                  label: `${o.label} (${o.count})`,
                })),
                onChange: (v) =>
                  setListFilter(v as 'ALL' | 'DRAFT' | 'ASSIGNED' | 'UNDER_REVIEW' | 'APPROVED'),
                minWidth: 140,
              },
            ]}
            onClearFilters={() => setListFilter('ALL')}
            columnPicker={ropaColumns.columnPicker}
          />
          <TableContainer sx={{ flex: 1 }}>
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  {ropaColumns.isVisible('id') && <TableCell>ID</TableCell>}
                  {ropaColumns.isVisible('activity') && <TableCell>Activity</TableCell>}
                  {ropaColumns.isVisible('basis') && <TableCell>Legal basis</TableCell>}
                  {ropaColumns.isVisible('designation') && <TableCell>Designation</TableCell>}
                  {ropaColumns.isVisible('transfers') && <TableCell>Transfers</TableCell>}
                  {ropaColumns.isVisible('status') && <TableCell>Status</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {filteredRows.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={ropaColumns.visibleCount}>
                      <Typography variant="body2" color="text.secondary">
                        No ROPA activities yet.
                      </Typography>
                    </TableCell>
                  </TableRow>
                )}
                {filteredRows.map((r) => (
                  <TableRow key={r.id} hover sx={{ cursor: 'pointer' }} onClick={() => setSelected(r)}>
                    {ropaColumns.isVisible('id') && (
                      <TableCell sx={{ fontFamily: 'ui-monospace, Menlo, monospace' }}>{r.displayId}</TableCell>
                    )}
                    {ropaColumns.isVisible('activity') && <TableCell>{r.activityName}</TableCell>}
                    {ropaColumns.isVisible('basis') && (
                      <TableCell>{r.legalBasis.replace(/_/g, ' ')}</TableCell>
                    )}
                    {ropaColumns.isVisible('designation') && (
                      <TableCell>{r.designation.replace(/_/g, ' ')}</TableCell>
                    )}
                    {ropaColumns.isVisible('transfers') && (
                      <TableCell>{r.thirdCountryTransfers ? 'Yes' : 'No'}</TableCell>
                    )}
                    {ropaColumns.isVisible('status') && (
                      <TableCell>
                        <StatusChip label={r.status} tone={toneForStatus(r.status)} />
                      </TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Card>
      ) : open ? (
        <Card sx={{ flex: 1, minHeight: 0, overflow: 'auto', p: { xs: 1.25, sm: 1.5 }, bgcolor: brand.canvas }}>
          <Box sx={complexFormContainerSx}>
            <SettingsCard compact title="ROPA details" subtitle="PIMS Record of Processing Activity (8 sections)">
              <Stack spacing={1}>
                <FormSelect
                  label="Intake mode"
                  value={form.intakeMode ?? 'FORM'}
                  options={[
                    { value: 'FORM', label: 'Structured form (Mode A)' },
                    { value: 'WORKBOOK', label: 'Workbook assignment (Mode B)' },
                  ]}
                  onChange={(v) => setForm({ ...form, intakeMode: v as RopaIntakeMode })}
                  fullWidth
                />
                {workbookMode && (
                  <Typography variant="body2" color="text.secondary">
                    Creates a shell record. Download the Arbor template, assign a Contributor, then they return the
                    filled workbook plus approval evidence via My Tasks.
                  </Typography>
                )}
                {sectionTitle('1. Identification')}
                <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '1fr 1fr' }, gap: 1 }}>
                  <CompactTextField
                    label="Activity name"
                    required
                    fieldIdPrefix="privacy-ropa"
                    value={form.activityName}
                    onChange={(e) => setForm({ ...form, activityName: e.target.value })}
                  />
                  <FormSelect
                    label="Designation"
                    value={form.designation}
                    options={ROPA_DESIGNATIONS.map((d) => ({ value: d, label: d.replace(/_/g, ' ') }))}
                    onChange={(v) => setForm({ ...form, designation: v as RopaDesignation })}
                    fullWidth
                  />
                  <CompactTextField
                    label="Entity"
                    fieldIdPrefix="privacy-ropa"
                    value={form.entityName ?? ''}
                    onChange={(e) => setForm({ ...form, entityName: e.target.value })}
                  />
                  <CompactTextField
                    label="Sector"
                    fieldIdPrefix="privacy-ropa"
                    value={form.sectorName ?? ''}
                    onChange={(e) => setForm({ ...form, sectorName: e.target.value })}
                  />
                  <CompactTextField
                    label="Division"
                    fieldIdPrefix="privacy-ropa"
                    value={form.divisionName ?? ''}
                    onChange={(e) => setForm({ ...form, divisionName: e.target.value })}
                  />
                  <CompactTextField
                    label="Section"
                    fieldIdPrefix="privacy-ropa"
                    value={form.sectionName ?? ''}
                    onChange={(e) => setForm({ ...form, sectionName: e.target.value })}
                  />
                </Box>

                {!workbookMode && (
                  <>
                    {sectionTitle('2. Processing details')}
                    <CompactTextField
                      label="Processing description"
                      multiline
                      minRows={2}
                      fieldIdPrefix="privacy-ropa"
                      value={form.processingDescription ?? ''}
                      onChange={(e) => setForm({ ...form, processingDescription: e.target.value })}
                    />
                    <CompactTextField
                      label="Purpose"
                      multiline
                      minRows={2}
                      fieldIdPrefix="privacy-ropa"
                      value={form.purpose ?? ''}
                      onChange={(e) => setForm({ ...form, purpose: e.target.value })}
                    />
                    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '1fr 1fr' }, gap: 1 }}>
                      <CompactTextField
                        label="Data categories"
                        fieldIdPrefix="privacy-ropa"
                        value={form.dataCategories ?? ''}
                        onChange={(e) => setForm({ ...form, dataCategories: e.target.value })}
                      />
                      <CompactTextField
                        label="Sensitive data categories"
                        fieldIdPrefix="privacy-ropa"
                        value={form.sensitiveDataCategories ?? ''}
                        onChange={(e) => setForm({ ...form, sensitiveDataCategories: e.target.value })}
                      />
                      <CompactTextField
                        label="Data subjects"
                        fieldIdPrefix="privacy-ropa"
                        value={form.dataSubjects ?? ''}
                        onChange={(e) => setForm({ ...form, dataSubjects: e.target.value })}
                      />
                      <FormSelect
                        label="Data subject volume"
                        value={form.dataSubjectVolume ?? ''}
                        options={[
                          { value: '', label: '-' },
                          ...ROPA_VOLUME_BUCKETS.map((v) => ({ value: v, label: v })),
                        ]}
                        onChange={(v) => setForm({ ...form, dataSubjectVolume: v || null })}
                        fullWidth
                      />
                      <CompactTextField
                        label="Volume note"
                        fieldIdPrefix="privacy-ropa"
                        value={form.dataSubjectVolumeNote ?? ''}
                        onChange={(e) => setForm({ ...form, dataSubjectVolumeNote: e.target.value })}
                      />
                      <FormSelect
                        label="Processing frequency"
                        value={form.processingFrequency ?? ''}
                        options={[
                          { value: '', label: '-' },
                          ...ROPA_FREQUENCIES.map((v) => ({ value: v, label: v })),
                        ]}
                        onChange={(v) => setForm({ ...form, processingFrequency: v || null })}
                        fullWidth
                      />
                      <CompactTextField
                        label="Frequency note"
                        fieldIdPrefix="privacy-ropa"
                        value={form.processingFrequencyNote ?? ''}
                        onChange={(e) => setForm({ ...form, processingFrequencyNote: e.target.value })}
                      />
                      <CompactTextField
                        label="Source of personal data"
                        fieldIdPrefix="privacy-ropa"
                        value={form.sourceOfPersonalData ?? ''}
                        onChange={(e) => setForm({ ...form, sourceOfPersonalData: e.target.value })}
                      />
                      <CompactTextField
                        label="Method of receiving"
                        fieldIdPrefix="privacy-ropa"
                        value={form.methodOfReceiving ?? ''}
                        onChange={(e) => setForm({ ...form, methodOfReceiving: e.target.value })}
                      />
                    </Box>

                    {sectionTitle('3. Legal basis')}
                    <FormSelect
                      label="Legal basis (UAE / PIMS)"
                      value={form.legalBasis ?? 'CONSENT'}
                      options={ROPA_LEGAL_BASES.map((b) => ({ value: b, label: b.replace(/_/g, ' ') }))}
                      onChange={(v) => setForm({ ...form, legalBasis: v as RopaLegalBasis })}
                      fullWidth
                    />

                    {sectionTitle('4. Transfers and disclosures')}
                    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '1fr 1fr' }, gap: 1 }}>
                      <FormControlLabel
                        control={
                          <Checkbox
                            size="small"
                            checked={!!form.shareInternal}
                            onChange={(e) => setForm({ ...form, shareInternal: e.target.checked })}
                          />
                        }
                        label={<Typography sx={{ fontSize: '0.6875rem' }}>Shared with internal department</Typography>}
                      />
                      <CompactTextField
                        label="Internal details"
                        fieldIdPrefix="privacy-ropa"
                        value={form.shareInternalDetails ?? ''}
                        onChange={(e) => setForm({ ...form, shareInternalDetails: e.target.value })}
                      />
                      <FormControlLabel
                        control={
                          <Checkbox
                            size="small"
                            checked={!!form.shareOperator}
                            onChange={(e) => setForm({ ...form, shareOperator: e.target.checked })}
                          />
                        }
                        label={<Typography sx={{ fontSize: '0.6875rem' }}>Shared with operator</Typography>}
                      />
                      <CompactTextField
                        label="Operator details"
                        fieldIdPrefix="privacy-ropa"
                        value={form.shareOperatorDetails ?? ''}
                        onChange={(e) => setForm({ ...form, shareOperatorDetails: e.target.value })}
                      />
                      <FormControlLabel
                        control={
                          <Checkbox
                            size="small"
                            checked={!!form.shareExternal}
                            onChange={(e) => setForm({ ...form, shareExternal: e.target.checked })}
                          />
                        }
                        label={<Typography sx={{ fontSize: '0.6875rem' }}>Shared with external entities</Typography>}
                      />
                      <CompactTextField
                        label="External details"
                        fieldIdPrefix="privacy-ropa"
                        value={form.shareExternalDetails ?? ''}
                        onChange={(e) => setForm({ ...form, shareExternalDetails: e.target.value })}
                      />
                      <CompactTextField
                        label="External entity location"
                        fieldIdPrefix="privacy-ropa"
                        value={form.externalEntityLocation ?? ''}
                        onChange={(e) => setForm({ ...form, externalEntityLocation: e.target.value })}
                      />
                      <FormControlLabel
                        control={
                          <Checkbox
                            size="small"
                            checked={!!form.thirdCountryTransfers}
                            onChange={(e) => setForm({ ...form, thirdCountryTransfers: e.target.checked })}
                          />
                        }
                        label={<Typography sx={{ fontSize: '0.6875rem' }}>Third-country transfers</Typography>}
                      />
                      <CompactTextField
                        label="Transfer details"
                        fieldIdPrefix="privacy-ropa"
                        value={form.thirdCountryTransferDetails ?? ''}
                        onChange={(e) => setForm({ ...form, thirdCountryTransferDetails: e.target.value })}
                      />
                      <CompactTextField
                        label="Cross-border transfer basis"
                        fieldIdPrefix="privacy-ropa"
                        value={form.crossBorderTransferBasis ?? ''}
                        onChange={(e) => setForm({ ...form, crossBorderTransferBasis: e.target.value })}
                      />
                      <CompactTextField
                        label="Safeguards implemented"
                        fieldIdPrefix="privacy-ropa"
                        value={form.safeguardsImplemented ?? ''}
                        onChange={(e) => setForm({ ...form, safeguardsImplemented: e.target.value })}
                      />
                    </Box>

                    {sectionTitle('5. Storage and security')}
                    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '1fr 1fr' }, gap: 1 }}>
                      <CompactTextField
                        label="Storage systems"
                        fieldIdPrefix="privacy-ropa"
                        value={form.storageSystems ?? ''}
                        onChange={(e) => setForm({ ...form, storageSystems: e.target.value })}
                      />
                      <CompactTextField
                        label="Retention period"
                        fieldIdPrefix="privacy-ropa"
                        value={form.retentionPeriod ?? ''}
                        onChange={(e) => setForm({ ...form, retentionPeriod: e.target.value })}
                      />
                      <CompactTextField
                        label="Security measures"
                        multiline
                        minRows={2}
                        fieldIdPrefix="privacy-ropa"
                        value={form.securityMeasures ?? ''}
                        onChange={(e) => setForm({ ...form, securityMeasures: e.target.value })}
                      />
                    </Box>

                    {sectionTitle('6. DPIA flag')}
                    <FormSelect
                      label="DPIA status"
                      value={form.dpiaStatus ?? 'NOT_REQUIRED'}
                      options={ROPA_DPIA_STATUSES.map((s) => ({ value: s, label: s.replace(/_/g, ' ') }))}
                      onChange={(v) => setForm({ ...form, dpiaStatus: v as RopaDpiaStatus })}
                      fullWidth
                    />

                    {sectionTitle('7. Representative and comments')}
                    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '1fr 1fr' }, gap: 1 }}>
                      <CompactTextField
                        label="Representative name"
                        fieldIdPrefix="privacy-ropa"
                        value={form.representativeName ?? ''}
                        onChange={(e) => setForm({ ...form, representativeName: e.target.value })}
                      />
                      <CompactTextField
                        label="Representative employee ID"
                        fieldIdPrefix="privacy-ropa"
                        value={form.representativeEmployeeId ?? ''}
                        onChange={(e) => setForm({ ...form, representativeEmployeeId: e.target.value })}
                      />
                    </Box>
                    <CompactTextField
                      label="Comments"
                      multiline
                      minRows={2}
                      fieldIdPrefix="privacy-ropa"
                      value={form.comments ?? ''}
                      onChange={(e) => setForm({ ...form, comments: e.target.value })}
                    />
                  </>
                )}
              </Stack>
            </SettingsCard>
          </Box>
        </Card>
      ) : selected ? (
        <Card sx={{ flex: 1, minHeight: 0, overflow: 'auto', p: 2 }}>
          <Stack spacing={2}>
            <Box>
              <Typography variant="h6" fontWeight={700}>
                {selected.displayId}: {selected.activityName}
              </Typography>
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mt: 1 }}>
                <StatusChip label={selected.status} tone={toneForStatus(selected.status)} />
                <StatusChip label={selected.intakeMode} tone="neutral" />
                {(selected.dpiaRequired || selected.dpiaStatus === 'REQUIRED') && (
                  <StatusChip label="DPIA required" tone="warning" />
                )}
                {selected.dpiaStatus === 'RECOMMENDED' && (
                  <StatusChip label="DPIA recommended" tone="info" />
                )}
                <Typography variant="caption" color="text.secondary" sx={{ alignSelf: 'center' }}>
                  {selected.legalBasis.replace(/_/g, ' ')} · {selected.designation.replace(/_/g, ' ')}
                </Typography>
              </Stack>
            </Box>
            {[
              ['Entity', selected.entityName],
              ['Sector', selected.sectorName],
              ['Division', selected.divisionName],
              ['Section', selected.sectionName],
              ['Processing description', selected.processingDescription],
              ['Purpose', selected.purpose],
              ['Data categories', selected.dataCategories],
              ['Sensitive data categories', selected.sensitiveDataCategories],
              ['Data subjects', selected.dataSubjects],
              ['Volume', selected.dataSubjectVolume],
              ['Volume note', selected.dataSubjectVolumeNote],
              ['Frequency', selected.processingFrequency],
              ['Frequency note', selected.processingFrequencyNote],
              ['Source of personal data', selected.sourceOfPersonalData],
              ['Method of receiving', selected.methodOfReceiving],
              ['Share internal', selected.shareInternal ? selected.shareInternalDetails || 'Yes' : null],
              ['Share operator', selected.shareOperator ? selected.shareOperatorDetails || 'Yes' : null],
              ['Share external', selected.shareExternal ? selected.shareExternalDetails || 'Yes' : null],
              ['External location', selected.externalEntityLocation],
              ['Retention period', selected.retentionPeriod],
              ['Storage systems', selected.storageSystems],
              ['Security measures', selected.securityMeasures],
              ['Representative', selected.representativeName],
              ['Comments', selected.comments],
              [
                'Transfer details',
                selected.thirdCountryTransfers ? selected.thirdCountryTransferDetails : null,
              ],
              ['Cross-border basis', selected.crossBorderTransferBasis],
              ['Safeguards', selected.safeguardsImplemented],
              ['Review notes', selected.reviewNotes],
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

            {canWrite && selected.intakeMode === 'WORKBOOK' && (selected.status === 'DRAFT' || selected.status === 'REJECTED' || selected.status === 'ASSIGNED') && (
              <Stack spacing={1} sx={{ p: 1.5, border: '1px solid', borderColor: 'divider', borderRadius: 1 }}>
                <Typography variant="subtitle2">Assign Contributor (Mode B)</Typography>
                <FormSelect
                  label="Contributor"
                  value={assignUserId === '' ? '' : String(assignUserId)}
                  options={[
                    { value: '', label: '-' },
                    ...users.map((u) => ({ value: String(u.id), label: `${u.fullName} (${u.email})` })),
                  ]}
                  onChange={(v) => setAssignUserId(v ? Number(v) : '')}
                  fullWidth
                />
                <CompactTextField
                  label="Due date (YYYY-MM-DD)"
                  fieldIdPrefix="privacy-ropa-assign"
                  value={assignDue}
                  onChange={(e) => setAssignDue(e.target.value)}
                  placeholder="2026-12-31"
                />
                <Button
                  variant="contained"
                  disabled={assignUserId === ''}
                  onClick={() =>
                    void act(
                      () =>
                        api.assignPrivacyRopa(selected.id, {
                          userId: Number(assignUserId),
                          dueDate: assignDue || null,
                        }),
                      'Assigned to Contributor',
                    )
                  }
                >
                  Assign
                </Button>
              </Stack>
            )}

            {canWrite && (
              <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                {selected.intakeMode === 'FORM' && (selected.status === 'DRAFT' || selected.status === 'REJECTED') && (
                  <Button
                    variant="contained"
                    onClick={() => void act(() => api.submitPrivacyRopaReview(selected.id), 'Submitted for review')}
                  >
                    Submit for Review
                  </Button>
                )}
                {selected.status === 'UNDER_REVIEW' && (
                  <>
                    <Button
                      variant="contained"
                      onClick={() => void act(() => api.approvePrivacyRopa(selected.id), 'Approved')}
                    >
                      Approve
                    </Button>
                    <Button color="error" variant="outlined" onClick={() => setRejectId(selected.id)}>
                      Reject / Send Back
                    </Button>
                  </>
                )}
              </Stack>
            )}
          </Stack>
        </Card>
      ) : null}

      <Dialog open={rejectId != null} onClose={() => setRejectId(null)} fullWidth maxWidth="xs">
        <DialogTitle>Reject ROPA</DialogTitle>
        <DialogContent>
          <TextField
            label="Review notes"
            fullWidth
            multiline
            minRows={2}
            value={rejectNotes}
            onChange={(e) => setRejectNotes(e.target.value)}
            sx={{ mt: 0.5 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectId(null)}>Cancel</Button>
          <Button
            color="error"
            variant="contained"
            disabled={rejectId == null}
            onClick={() => {
              if (rejectId == null) return
              void act(() => api.rejectPrivacyRopa(rejectId, rejectNotes), 'Rejected').then(() => {
                setRejectId(null)
                setRejectNotes('')
              })
            }}
          >
            Reject
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}
