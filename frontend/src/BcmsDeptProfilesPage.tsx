import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Box,
  Button,
  Card,
  Checkbox,
  FormControlLabel,
  FormGroup,
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
  type AssetSummary,
  type BcmsDeptProfile,
  type OrgUnitNode,
  type Session,
} from './api'
import { CompactTextField, FormSelect, RegisterListFrame, RegisterSortHeader, RegisterToolbar, SettingsCard, SimpleFormLayout } from './components/ui'
import { useListReady } from './uiQuality/useListReady'
import { usePagedRows } from './uiQuality/usePagedRows'
import { compareLoose, toggleSort, type SortDir } from './uiQuality/sortRows'
import { formatTenantDate } from './uiQuality/tenantLocale'
import { useAppShellPage } from './components/AppShellPageContext'
import { BCMS_PROFILE_COLUMNS } from './hooks/registerColumns'
import { useRegisterColumnPicker } from './hooks/useRegisterColumnPicker'
import { brand } from './theme'

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

export default function BcmsDeptProfilesPage({
  session,
  onError,
  onOk,
}: {
  session: Session
  onError: (msg: string) => void
  onOk: (msg: string) => void
}) {
  const canEdit = session.roles.some((r) => ['OWNER', 'ADMIN'].includes(r))
  const [rows, setRows] = useState<BcmsDeptProfile[]>([])
  const [units, setUnits] = useState<OrgUnitNode[]>([])
  const [assets, setAssets] = useState<AssetSummary[]>([])
  const [busy, setBusy] = useState(false)
  const [open, setOpen] = useState(false)
  const profileColumns = useRegisterColumnPicker(BCMS_PROFILE_COLUMNS)
  const [editId, setEditId] = useState<number | null>(null)

  const [orgUnitId, setOrgUnitId] = useState<number | ''>('')
  const [totalStaff, setTotalStaff] = useState('')
  const [criticalStaffRoles, setCriticalStaffRoles] = useState('')
  const [primaryFunctions, setPrimaryFunctions] = useState('')
  const [internalDependencies, setInternalDependencies] = useState('')
  const [externalDependencies, setExternalDependencies] = useState('')
  const [physicalLocation, setPhysicalLocation] = useState('')
  const [alternateWorkLocation, setAlternateWorkLocation] = useState('')
  const [assetIds, setAssetIds] = useState<number[]>([])
  const { listReady, markReady } = useListReady()
  const [sortKey, setSortKey] = useState('dept')
  const [sortDir, setSortDir] = useState<SortDir>('asc')

  const load = useCallback(async () => {
    setBusy(true)
    try {
      const [profiles, tree] = await Promise.all([api.listDeptProfiles(), api.listOrgUnits()])
      setRows(profiles)
      setUnits(flatten(tree).filter((u) => u.active))
      try {
        setAssets(await api.listAssets())
      } catch {
        setAssets([])
      }
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
    setOrgUnitId('')
    setTotalStaff('')
    setCriticalStaffRoles('')
    setPrimaryFunctions('')
    setInternalDependencies('')
    setExternalDependencies('')
    setPhysicalLocation('')
    setAlternateWorkLocation('')
    setAssetIds([])
  }

  const openCreate = () => {
    resetForm()
    setOpen(true)
  }

  const openEdit = (row: BcmsDeptProfile) => {
    setEditId(row.id)
    setOrgUnitId(row.orgUnitId)
    setTotalStaff(row.totalStaff != null ? String(row.totalStaff) : '')
    setCriticalStaffRoles(row.criticalStaffRoles ?? '')
    setPrimaryFunctions(row.primaryFunctions ?? '')
    setInternalDependencies(row.internalDependencies ?? '')
    setExternalDependencies(row.externalDependencies ?? '')
    setPhysicalLocation(row.physicalLocation ?? '')
    setAlternateWorkLocation(row.alternateWorkLocation ?? '')
    setAssetIds(row.assetIds ?? [])
    setOpen(true)
  }

  const save = async () => {
    if (orgUnitId === '') return
    setBusy(true)
    try {
      await api.upsertDeptProfile({
        orgUnitId,
        totalStaff: totalStaff.trim() === '' ? null : Number(totalStaff),
        criticalStaffRoles: criticalStaffRoles.trim() || null,
        primaryFunctions: primaryFunctions.trim() || null,
        internalDependencies: internalDependencies.trim() || null,
        externalDependencies: externalDependencies.trim() || null,
        physicalLocation: physicalLocation.trim() || null,
        alternateWorkLocation: alternateWorkLocation.trim() || null,
        assetIds,
      })
      onOk(editId == null ? 'Department profile created' : 'Department profile updated')
      setOpen(false)
      await load()
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const toggleAsset = (id: number) =>
    setAssetIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]))

  const unitLabel = (id: number) => {
    const u = units.find((x) => x.id === id)
    return u ? `L${u.level} · ${u.name}` : `Unit #${id}`
  }

  const sortedRows = useMemo(
    () => [...rows].sort((a, b) => compareLoose(unitLabel(a.orgUnitId), unitLabel(b.orgUnitId), sortDir)),
    [rows, sortDir, units],
  )
  const paged = usePagedRows(sortedRows)

  const unitsWithoutProfile = units.filter(
    (u) => editId != null || !rows.some((r) => r.orgUnitId === u.id),
  )

  const listActions = useMemo(
    () =>
      canEdit ? (
        <Button variant="contained" size="small" disabled={busy} onClick={openCreate}>
          New Profile
        </Button>
      ) : undefined,
    [canEdit, busy],
  )

  const closeForm = useCallback(() => setOpen(false), [])

  const formTitle = editId == null ? 'New Profile' : canEdit ? 'Edit Profile' : 'View Profile'

  const formActions = useMemo(
    () => (
      <>
        <Button variant="outlined" size="small" disabled={busy} onClick={closeForm}>
          Cancel
        </Button>
        {canEdit ? (
        <Button
          variant="contained"
          size="small"
          disabled={orgUnitId === '' || busy}
          onClick={() => void save()}
        >
          {editId == null ? 'Create Profile' : 'Save Changes'}
        </Button>
        ) : null}
      </>
    ),
    [busy, closeForm, orgUnitId, editId, canEdit],
  )

  useAppShellPage(
    open
      ? {
          title: formTitle,
          breadcrumbTrail:
            editId == null ? 'New' : orgUnitId !== '' ? unitLabel(orgUnitId) : formTitle,
          breadcrumbRoot: { label: 'Department Profiles', onClick: closeForm },
          actions: formActions,
          hideContextLabel: true,
          hideRoleChips: true,
        }
      : {
          title: 'Department Profiles',
          breadcrumbTrail: 'BCMS',
          actions: listActions,
          hideContextLabel: true,
          hideRoleChips: true,
        },
    [open, formTitle, editId, orgUnitId, formActions, listActions, canEdit, closeForm],
  )

  return (
    <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
      {!open ? (
        <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          <RegisterToolbar columnPicker={profileColumns.columnPicker} />
          <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
          <RegisterListFrame
            ready={listReady}
            count={paged.count}
            emptyMessage="No department profiles yet."
            page={paged.page}
            rowsPerPage={paged.rowsPerPage}
            onPageChange={paged.setPage}
            onRowsPerPageChange={paged.setRowsPerPage}
          >
          <TableContainer sx={{ flex: 1 }}>
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  {profileColumns.isVisible('dept') && (
                    <RegisterSortHeader
                      label="Org unit"
                      active={sortKey === 'dept'}
                      dir={sortDir}
                      onClick={() => {
                        const next = toggleSort(sortKey, sortDir, 'dept')
                        setSortKey(next.key)
                        setSortDir(next.dir)
                      }}
                    />
                  )}
                  {profileColumns.isVisible('staff') && <TableCell>Total staff</TableCell>}
                  {profileColumns.isVisible('physical') && <TableCell>Physical location</TableCell>}
                  {profileColumns.isVisible('alternate') && <TableCell>Alternate location</TableCell>}
                  {profileColumns.isVisible('assets') && <TableCell>Linked assets</TableCell>}
                  {profileColumns.isVisible('updated') && <TableCell>Updated</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {paged.paged.map((r) => (
                  <TableRow
                    key={r.id}
                    hover
                    sx={{ cursor: 'pointer' }}
                    onClick={() => openEdit(r)}
                  >
                    {profileColumns.isVisible('dept') && (
                    <TableCell>
                      <Typography variant="body2" fontWeight={600}>
                        {unitLabel(r.orgUnitId)}
                      </Typography>
                    </TableCell>
                    )}
                    {profileColumns.isVisible('staff') && (
                    <TableCell>
                      <Typography variant="caption">{r.totalStaff ?? '-'}</Typography>
                    </TableCell>
                    )}
                    {profileColumns.isVisible('physical') && (
                    <TableCell>
                      <Typography variant="caption">{r.physicalLocation ?? '-'}</Typography>
                    </TableCell>
                    )}
                    {profileColumns.isVisible('alternate') && (
                    <TableCell>
                      <Typography variant="caption">{r.alternateWorkLocation ?? '-'}</Typography>
                    </TableCell>
                    )}
                    {profileColumns.isVisible('assets') && (
                    <TableCell>
                      <Typography variant="caption">{r.assetIds.length}</Typography>
                    </TableCell>
                    )}
                    {profileColumns.isVisible('updated') && (
                    <TableCell>
                      <Typography variant="caption" color="text.secondary">
                        {formatTenantDate(r.updatedAt)}
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
        <SimpleFormLayout>
          <SettingsCard compact title="Profile details" subtitle="Staff, locations, and linked assets">
            <Stack spacing={1} component="fieldset" disabled={!canEdit} sx={{ border: 0, m: 0, p: 0, minWidth: 0 }}>
              <FormSelect
                label="Organisation unit"
                value={orgUnitId === '' ? '' : String(orgUnitId)}
                options={unitsWithoutProfile.map((u) => ({
                  value: String(u.id),
                  label: `L${u.level} · ${u.name}`,
                }))}
                onChange={(v) => setOrgUnitId(Number(v))}
                fullWidth
                disabled={editId != null}
              />
              <CompactTextField
                label="Total staff"
                type="number"
                fieldIdPrefix="bcms-dept"
                value={totalStaff}
                onChange={(e) => setTotalStaff(e.target.value)}
              />
              <CompactTextField
                label="Critical staff roles"
                multiline
                minRows={2}
                fieldIdPrefix="bcms-dept"
                value={criticalStaffRoles}
                onChange={(e) => setCriticalStaffRoles(e.target.value)}
              />
              <CompactTextField
                label="Primary functions"
                multiline
                minRows={2}
                fieldIdPrefix="bcms-dept"
                value={primaryFunctions}
                onChange={(e) => setPrimaryFunctions(e.target.value)}
              />
              <CompactTextField
                label="Internal dependencies"
                multiline
                minRows={2}
                fieldIdPrefix="bcms-dept"
                value={internalDependencies}
                onChange={(e) => setInternalDependencies(e.target.value)}
              />
              <CompactTextField
                label="External dependencies"
                multiline
                minRows={2}
                fieldIdPrefix="bcms-dept"
                value={externalDependencies}
                onChange={(e) => setExternalDependencies(e.target.value)}
              />
              <CompactTextField
                label="Physical location"
                fieldIdPrefix="bcms-dept"
                value={physicalLocation}
                onChange={(e) => setPhysicalLocation(e.target.value)}
              />
              <CompactTextField
                label="Alternate work location"
                fieldIdPrefix="bcms-dept"
                value={alternateWorkLocation}
                onChange={(e) => setAlternateWorkLocation(e.target.value)}
              />
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Linked assets {assets.length === 0 ? '(none available: ISMS may not be licensed)' : ''}
                </Typography>
                <FormGroup sx={{ maxHeight: 160, overflow: 'auto' }}>
                  {assets.map((a) => (
                    <FormControlLabel
                      key={a.id}
                      control={
                        <Checkbox
                          size="small"
                          checked={assetIds.includes(a.id)}
                          onChange={() => toggleAsset(a.id)}
                        />
                      }
                      label={<Typography variant="caption">{a.name}</Typography>}
                    />
                  ))}
                </FormGroup>
              </Box>
            </Stack>
          </SettingsCard>
        </SimpleFormLayout>
      )}
    </Stack>
  )
}
