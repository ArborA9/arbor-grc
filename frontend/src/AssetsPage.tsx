import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
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
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import {
  api,
  type AssetKpis,
  type AssetMeta,
  type AssetSummary,
  type AssetStatus,
  type AssetType,
  type OrgUnitNode,
  type Session,
  type UserSummary,
} from './api'
import StatusChip from './components/StatusChip'
import { useAppShellPage } from './components/AppShellPageContext'
import {
  CIA_SCALE,
  ASSET_VALUE_TIER_MAP,
  DERIVED_CLASSIFICATION_COPY,
  assetValue,
  classificationFromC,
  criticalityFromValue,
  derivedClassificationLabel,
  weightedScore,
} from './components/asset/assetCalc'
import {
  focusAssetField,
  isCiaRating,
  isOtherDataResidency,
  validateAssetDraft,
  validateAssetSave,
  type AssetFieldErrors,
} from './assetRegisterValidation'
import { CompactTextField, ConfirmActionDialog, FormFieldLabel, KpiCard, KpiCardGrid, RegisterListFrame, RegisterSortHeader, RegisterToolbar, RiskLevelBadge, SettingsCard, complexFormContainerSx, formFieldGridSx, DATE_FIELD_WIDTH, NARRATIVE_FIELD_MAX_WIDTH, registerTableSx, primaryTableCellSx, primaryRegisterNameSx, compactTableCellSx, mediumTableCellSx, lastTableCellSx, compactTableCellSxWidth, RATING_TONE_COLOR, scale1to5Tone } from './components/ui'
import { usePagedRows } from './uiQuality/usePagedRows'
import { useListReady } from './uiQuality/useListReady'
import { usePersistedState } from './uiQuality/usePersistedState'
import { compareLoose, toggleSort, type SortDir } from './uiQuality/sortRows'
import {
  useOptionalUnsavedChangesGuardApi,
  useRegisterUnsavedChanges,
} from './uiQuality/UnsavedChangesGuard'
import { roleLabels } from './uiQuality/roleLabels'
import FormSelect from './components/ui/FormSelect'
import { assetCriticalityToBadge } from './components/ui/riskBadgeMappers'
import { brand } from './theme'
import { ASSET_REGISTER_COLUMNS } from './hooks/registerColumns'
import { useRegisterColumnPicker } from './hooks/useRegisterColumnPicker'

type AssetListFilter = 'ALL' | 'DRAFT' | 'ACTIVE' | 'DISPOSED'

function assetStatusTone(status: AssetStatus): 'warning' | 'success' | 'neutral' {
  if (status === 'DRAFT') return 'warning'
  if (status === 'ACTIVE') return 'success'
  return 'neutral'
}

function assetStatusLabel(status: AssetStatus): string {
  if (status === 'DRAFT') return 'Draft'
  if (status === 'ACTIVE') return 'Active'
  return 'Disposed'
}

function flattenOrgUnits(nodes: OrgUnitNode[], acc: OrgUnitNode[] = []): OrgUnitNode[] {
  for (const n of nodes) {
    acc.push(n)
    if (n.children?.length) flattenOrgUnits(n.children, acc)
  }
  return acc
}

function ownerRoleTitle(user: UserSummary | undefined): string {
  if (!user?.roles?.length) return '-'
  return roleLabels(user.roles)
}

function ownerDepartmentName(
  user: UserSummary | undefined,
  orgById: Map<number, string>,
): string {
  const id = user?.orgUnitIds?.[0]
  if (id == null) return ''
  return orgById.get(id) ?? ''
}

type FormState = {
  name: string
  assetType: AssetType
  assetSubType: string
  environment: string
  description: string
  ownerUserId: number | ''
  ownerJobTitle: string
  ownerDepartment: string
  custodianUserId: number | ''
  custodianName: string
  siteBuilding: string
  hostingModel: string
  cloudProviderRegion: string
  dataResidency: string
  dataResidencyOther: string
  hostname: string
  ipAddress: string
  assetTagSerial: string
  platformOsVersion: string
  c: number | ''
  cJust: string
  i: number | ''
  iJust: string
  a: number | ''
  aJust: string
  endOfLifeDate: string
}

const emptyForm = (): FormState => ({
  name: '',
  assetType: 'APPLICATION',
  assetSubType: '',
  environment: 'Production',
  description: '',
  ownerUserId: '',
  ownerJobTitle: '',
  ownerDepartment: '',
  custodianUserId: '',
  custodianName: '',
  siteBuilding: '',
  hostingModel: '',
  cloudProviderRegion: '',
  dataResidency: '',
  dataResidencyOther: '',
  hostname: '',
  ipAddress: '',
  assetTagSerial: '',
  platformOsVersion: '',
  c: '',
  cJust: '',
  i: '',
  iJust: '',
  a: '',
  aJust: '',
  endOfLifeDate: '',
})

const CIA_RATING_OPTIONS = [{ value: '', label: 'Select rating' }, ...CIA_SCALE]

const ciaPairRowSx = {
  gridColumn: '1 / -1' as const,
  display: 'grid',
  gridTemplateColumns: { xs: '1fr', sm: 'minmax(220px, 280px) minmax(0, 1fr)' },
  gap: 1,
  alignItems: 'start' as const,
}

function storedCiaRating(n: number | null | undefined): number | '' {
  return n != null && n >= 1 && n <= 5 ? n : ''
}

function CalcStat({
  label,
  hint,
  children,
}: {
  label: string
  hint: string
  children: React.ReactNode
}) {
  return (
    <Box
      sx={{
        minWidth: 0,
        px: 1.5,
        py: 1.25,
        border: `1px solid ${brand.border}`,
        borderRadius: 1,
        bgcolor: brand.canvas,
      }}
    >
      <Typography
        sx={{
          fontSize: '0.6875rem',
          fontWeight: 600,
          color: brand.muted,
          letterSpacing: '0.04em',
          textTransform: 'uppercase',
          lineHeight: 1.3,
        }}
      >
        {label}
      </Typography>
      <Box sx={{ mt: 0.75, minHeight: 32, display: 'flex', alignItems: 'center' }}>{children}</Box>
      <Typography sx={{ mt: 0.5, fontSize: '0.6875rem', color: brand.mutedLight, lineHeight: 1.35 }}>
        {hint}
      </Typography>
    </Box>
  )
}

function Section({ title, children, defaultExpanded = false }: { title: string; children: React.ReactNode; defaultExpanded?: boolean }) {
  return (
    <Accordion defaultExpanded={defaultExpanded} disableGutters elevation={0} sx={{ border: `1px solid ${brand.border}`, mb: 1, '&:before': { display: 'none' } }}>
      <AccordionSummary expandIcon={<ExpandMoreIcon />}>
        <Typography variant="subtitle2" fontWeight={700}>
          {title}
        </Typography>
      </AccordionSummary>
      <AccordionDetails sx={{ pt: 0 }}>
        <Box sx={formFieldGridSx}>
          {children}
        </Box>
      </AccordionDetails>
    </Accordion>
  )
}

export default function AssetsPage({
  session,
  onError,
  onOk,
}: {
  session: Session
  onError: (msg: string) => void
  onOk: (msg: string) => void
}) {
  const canEdit = session.roles.some((r) => ['OWNER', 'ADMIN'].includes(r))
  const [rows, setRows] = useState<AssetSummary[]>([])
  const [kpis, setKpis] = useState<AssetKpis | null>(null)
  const [meta, setMeta] = useState<AssetMeta | null>(null)
  const [users, setUsers] = useState<UserSummary[]>([])
  const [orgUnitsFlat, setOrgUnitsFlat] = useState<OrgUnitNode[]>([])
  const [busy, setBusy] = useState(false)
  const [open, setOpen] = useState(false)
  const [editId, setEditId] = useState<number | null>(null)
  const [editStatus, setEditStatus] = useState<AssetStatus | null>(null)
  const [displayId, setDisplayId] = useState<string | null>(null)
  const [form, setForm] = useState<FormState>(emptyForm)
  const [fieldErrors, setFieldErrors] = useState<AssetFieldErrors>({})
  const [saveBlockReason, setSaveBlockReason] = useState('')
  const [listFilter, setListFilter] = usePersistedState<AssetListFilter>('reg-assets-filter', 'ALL')
  const [confirmDispose, setConfirmDispose] = useState(false)
  const [baselineForm, setBaselineForm] = useState(() => JSON.stringify(emptyForm()))
  const { listReady, markReady } = useListReady()
  const [sortKey, setSortKey] = useState('name')
  const [sortDir, setSortDir] = useState<SortDir>('asc')
  const assetColumns = useRegisterColumnPicker(ASSET_REGISTER_COLUMNS)
  const { requestNavigation } = useOptionalUnsavedChangesGuardApi()
  const saveAssetRef = useRef<() => Promise<void>>(async () => {})

  const setField = <K extends keyof FormState>(key: K, value: FormState[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  const ciaReady = isCiaRating(form.c) && isCiaRating(form.i) && isCiaRating(form.a)
  const previewValue = isCiaRating(form.c) && isCiaRating(form.i) && isCiaRating(form.a)
    ? assetValue(form.c, form.i, form.a)
    : null
  const previewCrit = previewValue != null ? criticalityFromValue(previewValue) : null
  const derivedClass = isCiaRating(form.c) ? classificationFromC(form.c) : null
  const previewWeighted = isCiaRating(form.c) && isCiaRating(form.i) && isCiaRating(form.a)
    ? weightedScore(form.c, form.i, form.a)
    : null
  const handling = isCiaRating(form.c)
    ? meta?.handlingByConfidentiality.find((h) => h.confidentiality === form.c)
    : undefined
  const saveGate = useMemo(() => validateAssetSave(form), [form])
  const saveDisabled = busy || !saveGate.ok

  const orgById = useMemo(() => {
    const m = new Map<number, string>()
    for (const u of orgUnitsFlat) m.set(u.id, u.name)
    return m
  }, [orgUnitsFlat])

  const load = useCallback(async () => {
    setBusy(true)
    try {
      const [list, m, k, orgTree] = await Promise.all([
        api.listAssets(),
        api.getAssetMeta(),
        api.getAssetKpis(),
        api.listOrgUnits(),
      ])
      setRows(list)
      setMeta(m)
      setKpis(k)
      setOrgUnitsFlat(flattenOrgUnits(orgTree).filter((u) => u.active))
      if (canEdit) {
        setUsers(await api.listUsers())
      } else {
        const dir = await api.listUserDirectory()
        setUsers(
          dir.map((u) => ({
            id: u.id,
            email: u.email,
            fullName: u.fullName,
            roles: [],
            status: 'ACTIVE',
            authType: 'LOCAL',
            orgUnitIds: [],
            createdAt: '',
          })),
        )
      }
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
      markReady()
    }
  }, [canEdit, onError, markReady])

  useEffect(() => {
    void load()
  }, [load])

  const resetForm = () => {
    setEditId(null)
    setEditStatus(null)
    setDisplayId(null)
    const next = emptyForm()
    setForm(next)
    setBaselineForm(JSON.stringify(next))
    setFieldErrors({})
    setSaveBlockReason('')
  }

  const openCreate = () => {
    resetForm()
    setOpen(true)
  }

  const applyOwnerFromUser = useCallback(
    (ownerId: number | '') => {
      if (ownerId === '') {
        setForm((prev) => ({
          ...prev,
          ownerUserId: '',
          ownerJobTitle: '',
          ownerDepartment: '',
        }))
        return
      }
      const u = users.find((x) => x.id === ownerId)
      setForm((prev) => ({
        ...prev,
        ownerUserId: ownerId,
        ownerJobTitle: ownerRoleTitle(u),
        ownerDepartment: ownerDepartmentName(u, orgById),
      }))
    },
    [users, orgById],
  )

  const applyAssetToForm = useCallback(
    (asset: AssetSummary) => {
      setEditId(asset.id)
      setDisplayId(asset.displayId)
      setEditStatus(asset.status)
      const owner = asset.ownerUserId != null ? users.find((u) => u.id === asset.ownerUserId) : undefined
      const next: FormState = {
        name: asset.name,
        assetType: asset.assetType,
        assetSubType: asset.assetSubType ?? '',
        environment: asset.environment ?? '',
        description: asset.description ?? '',
        ownerUserId: asset.ownerUserId ?? '',
        ownerJobTitle: owner ? ownerRoleTitle(owner) : asset.ownerJobTitle ?? '',
        ownerDepartment:
          asset.ownerDepartment?.trim() ||
          (owner ? ownerDepartmentName(owner, orgById) : ''),
        custodianUserId: asset.custodianUserId ?? '',
        custodianName: asset.custodianName ?? '',
        siteBuilding: asset.siteBuilding ?? '',
        hostingModel: asset.hostingModel ?? '',
        cloudProviderRegion: asset.cloudProviderRegion ?? '',
        dataResidency: asset.dataResidency ?? '',
        dataResidencyOther: asset.dataResidencyOther ?? '',
        hostname: asset.hostname ?? '',
        ipAddress: asset.ipAddress ?? '',
        assetTagSerial: asset.assetTagSerial ?? '',
        platformOsVersion: asset.platformOsVersion ?? '',
        c: storedCiaRating(asset.confidentiality),
        cJust: asset.confidentialityJustification ?? '',
        i: storedCiaRating(asset.integrityRating),
        iJust: asset.integrityJustification ?? '',
        a: storedCiaRating(asset.availability),
        aJust: asset.availabilityJustification ?? '',
        endOfLifeDate: asset.endOfLifeDate ?? '',
      }
      setForm(next)
      setBaselineForm(JSON.stringify(next))
      setFieldErrors({})
      setSaveBlockReason('')
    },
    [users, orgById],
  )

  const openEdit = async (row: AssetSummary) => {
    setOpen(true)
    setBusy(true)
    try {
      const detail = await api.getAsset(row.id)
      applyAssetToForm(detail)
    } catch (e) {
      setOpen(false)
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const buildBody = useCallback(
    (asDraft: boolean) => ({
      name: form.name.trim(),
      assetType: form.assetType,
      assetSubType: form.assetSubType.trim() || null,
      environment: form.environment.trim() || null,
      description: form.description.trim() || null,
      ownerUserId: form.ownerUserId === '' ? null : form.ownerUserId,
      ownerJobTitle: form.ownerJobTitle.trim() || null,
      ownerDepartment: form.ownerDepartment.trim() || null,
      custodianUserId: form.custodianUserId === '' ? null : form.custodianUserId,
      custodianName: form.custodianName.trim() || null,
      siteBuilding: form.siteBuilding.trim() || null,
      hostingModel: form.hostingModel.trim() || null,
      cloudProviderRegion: form.cloudProviderRegion.trim() || null,
      dataResidency: form.dataResidency.trim() || null,
      dataResidencyOther: isOtherDataResidency(form.dataResidency)
        ? form.dataResidencyOther.trim() || null
        : null,
      hostname: form.hostname.trim() || null,
      ipAddress: form.ipAddress.trim() || null,
      assetTagSerial: form.assetTagSerial.trim() || null,
      platformOsVersion: form.platformOsVersion.trim() || null,
      confidentiality: form.c === '' ? null : form.c,
      confidentialityJustification: form.cJust.trim() || null,
      integrityRating: form.i === '' ? null : form.i,
      integrityJustification: form.iJust.trim() || null,
      availability: form.a === '' ? null : form.a,
      availabilityJustification: form.aJust.trim() || null,
      endOfLifeDate: form.endOfLifeDate.trim() || null,
      asDraft,
    }),
    [form],
  )

  const saveDraft = useCallback(
    async (opts?: { throwOnInvalid?: boolean }) => {
      const validated = validateAssetDraft(form)
      setFieldErrors(validated.errors)
      setSaveBlockReason(validated.ok ? '' : validated.reason)
      if (!validated.ok) {
        if (validated.firstKey) focusAssetField(validated.firstKey)
        onError(validated.reason || 'Asset name is required to save a draft')
        if (opts?.throwOnInvalid) {
          throw new Error(validated.reason || 'Asset name is required to save a draft')
        }
        return
      }
      const body = buildBody(true)
      setBusy(true)
      try {
        const saved =
          editId == null ? await api.createAsset(body) : await api.updateAsset(editId, body)
        onOk(editId == null ? 'Draft asset saved' : 'Draft updated')
        applyAssetToForm(saved)
        await load()
      } catch (e) {
        onError(String(e))
        if (opts?.throwOnInvalid) {
          throw e instanceof Error ? e : new Error(String(e))
        }
      } finally {
        setBusy(false)
      }
    },
    [applyAssetToForm, buildBody, editId, form, load, onError, onOk],
  )

  const save = useCallback(
    async (opts?: { throwOnInvalid?: boolean }) => {
      const validated = validateAssetSave(form)
      setFieldErrors(validated.errors)
      setSaveBlockReason(validated.ok ? '' : validated.reason)
      if (!validated.ok) {
        if (validated.firstKey) focusAssetField(validated.firstKey)
        onError(validated.reason || 'Fix the highlighted fields')
        if (opts?.throwOnInvalid) {
          throw new Error(validated.reason || 'Fix the highlighted fields')
        }
        return
      }
      const body = buildBody(false)
      setBusy(true)
      try {
        if (editId == null) {
          await api.createAsset(body)
          onOk('Asset created')
        } else {
          await api.updateAsset(editId, body)
          onOk('Asset updated')
        }
        setOpen(false)
        await load()
      } catch (e) {
        onError(String(e))
        if (opts?.throwOnInvalid) {
          throw e instanceof Error ? e : new Error(String(e))
        }
      } finally {
        setBusy(false)
      }
    },
    [buildBody, editId, form, load, onError, onOk],
  )

  const dispose = async (id: number) => {
    setBusy(true)
    try {
      await api.disposeAsset(id)
      onOk('Asset disposed')
      setOpen(false)
      await load()
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const filteredRows = useMemo(() => {
    const next = listFilter === 'ALL' ? rows : rows.filter((r) => r.status === listFilter)
    return [...next].sort((a, b) => compareLoose(a.name, b.name, sortDir))
  }, [rows, listFilter, sortDir])
  const assetsPaged = usePagedRows(filteredRows)

  const isFormDirty = open && JSON.stringify(form) !== baselineForm

  useRegisterUnsavedChanges(
    open
      ? {
          dirty: isFormDirty,
          title: 'Leave asset without saving?',
          message:
            'You have unsaved asset edits. Save a draft (Asset Name required) to keep your work, or discard to leave without saving.',
          onSaveDraft: () => saveAssetRef.current(),
          saveDraftLabel: 'Save Draft',
        }
      : null,
  )

  saveAssetRef.current = () => saveDraft({ throwOnInvalid: true })

  const draftGate = useMemo(() => validateAssetDraft(form), [form])
  const draftDisabled = busy || !draftGate.ok

  const assetFilterOptions = useMemo(
    () => [
      { value: 'ALL' as const, label: 'All', count: rows.length },
      { value: 'DRAFT' as const, label: 'Draft', count: rows.filter((r) => r.status === 'DRAFT').length },
      { value: 'ACTIVE' as const, label: 'Active', count: rows.filter((r) => r.status === 'ACTIVE').length },
      { value: 'DISPOSED' as const, label: 'Disposed', count: rows.filter((r) => r.status === 'DISPOSED').length },
    ],
    [rows],
  )

  const typeOptions = useMemo(() => {
    if (meta?.assetTypes?.length) {
      return meta.assetTypes.map((t) => ({ value: t.value, label: t.label }))
    }
    return [{ value: 'APPLICATION', label: 'Software / Application' }]
  }, [meta])

  const listActions = useMemo(
    () =>
      canEdit ? (
        <Button variant="contained" size="small" disabled={busy} onClick={openCreate}>
          New Asset
        </Button>
      ) : undefined,
    [canEdit, busy],
  )

  const performCloseForm = useCallback(() => {
    setOpen(false)
  }, [])

  const closeForm = useCallback(() => {
    requestNavigation(performCloseForm)
  }, [requestNavigation, performCloseForm])

  const formTitle =
    editId == null ? 'New Asset' : displayId ? `${canEdit ? 'Edit' : 'View'} ${displayId}` : canEdit ? 'Edit Asset' : 'View Asset'

  const formActions = useMemo(
    () => (
      <>
        <Button size="small" variant="outlined" onClick={closeForm} disabled={busy}>
          Cancel
        </Button>
        {canEdit ? (
          <>
            <Button
              size="small"
              variant="outlined"
              disabled={draftDisabled}
              title={draftGate.ok ? undefined : draftGate.reason}
              onClick={() => void saveDraft()}
            >
              {busy ? 'Saving…' : 'Save Draft'}
            </Button>
            <Button
              size="small"
              variant="contained"
              disabled={saveDisabled}
              title={saveGate.ok ? undefined : saveGate.reason}
              onClick={() => void save()}
            >
              {busy ? 'Saving…' : editId == null ? 'Create Asset' : 'Save Changes'}
            </Button>
            {editId != null && editStatus !== 'DISPOSED' && (
              <Button
                size="small"
                variant="outlined"
                color="error"
                disabled={busy}
                onClick={() => setConfirmDispose(true)}
              >
                Dispose
              </Button>
            )}
          </>
        ) : null}
      </>
    ),
    [
      busy,
      closeForm,
      draftDisabled,
      draftGate.ok,
      draftGate.reason,
      editId,
      editStatus,
      save,
      saveDisabled,
      saveDraft,
      saveGate.ok,
      saveGate.reason,
      canEdit,
    ],
  )

  useAppShellPage(
    open
      ? {
          title: formTitle,
          breadcrumbTrail: editId == null ? 'New' : form.name.trim() || formTitle,
          breadcrumbRoot: { label: 'Asset Register', onClick: closeForm },
          actions: formActions,
          hideContextLabel: true,
          hideRoleChips: true,
        }
      : {
          title: 'Asset Register',
          breadcrumbTrail: 'Assets',
          actions: listActions,
          hideContextLabel: true,
          hideRoleChips: true,
        },
    [open, formTitle, editId, form.name, formActions, listActions, canEdit, closeForm],
  )

  const opt = (list: string[] | undefined, includeBlank = true) => [
    ...(includeBlank ? [{ value: '', label: '-' }] : []),
    ...(list ?? []).map((v) => ({ value: v, label: v })),
  ]

  return (
    <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
      {!open ? (
        <>
          {kpis && (
            <KpiCardGrid columns={5}>
              <KpiCard label="Total" value={String(kpis.total)} />
              <KpiCard label="Active" value={String(kpis.active)} />
              <KpiCard label="Disposed" value={String(kpis.disposed)} />
              <KpiCard label="Tier 1–2" value={String(kpis.tier1Or2)} />
              <KpiCard label="Secret / Sensitive" value={String(kpis.secretOrSensitive)} />
            </KpiCardGrid>
          )}
          <Card sx={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
            <RegisterToolbar
              filters={[
                {
                  id: 'status',
                  label: 'Status',
                  value: listFilter,
                  options: assetFilterOptions.map((o) => ({
                    value: o.value,
                    label: `${o.label} (${o.count})`,
                  })),
                  onChange: (v) => setListFilter(v as AssetListFilter),
                  minWidth: 140,
                },
              ]}
              onClearFilters={() => setListFilter('ALL')}
              columnPicker={assetColumns.columnPicker}
            />
            <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
            <RegisterListFrame
              ready={listReady}
              count={assetsPaged.count}
              emptyMessage="No assets yet. Use New asset to add the first record."
              page={assetsPaged.page}
              rowsPerPage={assetsPaged.rowsPerPage}
              onPageChange={assetsPaged.setPage}
              onRowsPerPageChange={assetsPaged.setRowsPerPage}
            >
            <TableContainer sx={{ flex: 1 }}>
              <Table size="small" stickyHeader sx={registerTableSx}>
                <TableHead>
                  <TableRow>
                    {assetColumns.isVisible('displayId') && (
                      <TableCell sx={compactTableCellSxWidth('6rem')}>ID</TableCell>
                    )}
                    {assetColumns.isVisible('name') && (
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
                    {assetColumns.isVisible('type') && (
                      <TableCell sx={mediumTableCellSx}>Type</TableCell>
                    )}
                    {assetColumns.isVisible('cia') && (
                      <TableCell sx={compactTableCellSxWidth('7.5rem')}>CIA</TableCell>
                    )}
                    {assetColumns.isVisible('criticality') && (
                      <TableCell sx={compactTableCellSx}>Criticality</TableCell>
                    )}
                    {assetColumns.isVisible('classification') && (
                      <TableCell sx={mediumTableCellSx}>Class</TableCell>
                    )}
                    {assetColumns.isVisible('status') && (
                      <TableCell sx={{ ...compactTableCellSx, ...lastTableCellSx }}>Status</TableCell>
                    )}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {assetsPaged.paged.map((r) => (
                    <TableRow
                      key={r.id}
                      hover
                      sx={{ cursor: 'pointer' }}
                      onClick={() => void openEdit(r)}
                    >
                      {assetColumns.isVisible('displayId') && (
                        <TableCell sx={compactTableCellSxWidth('6rem')}>
                          <Typography variant="caption">{r.displayId ?? `#${r.id}`}</Typography>
                        </TableCell>
                      )}
                      {assetColumns.isVisible('name') && (
                        <TableCell sx={primaryTableCellSx}>
                          <Typography noWrap sx={primaryRegisterNameSx}>
                            {r.name}
                          </Typography>
                        </TableCell>
                      )}
                      {assetColumns.isVisible('type') && (
                        <TableCell sx={mediumTableCellSx}>
                          <Typography variant="caption" noWrap>
                            {r.assetTypeLabel ?? r.assetType}
                            {r.assetSubType ? ` · ${r.assetSubType}` : ''}
                          </Typography>
                        </TableCell>
                      )}
                      {assetColumns.isVisible('cia') && (
                        <TableCell sx={compactTableCellSxWidth('7.5rem')}>
                          <Typography variant="caption" noWrap component="span" sx={{ fontWeight: 600 }}>
                            {!isCiaRating(r.confidentiality) && !isCiaRating(r.integrityRating) && !isCiaRating(r.availability) ? (
                              '-'
                            ) : (
                              <>
                                <Box
                                  component="span"
                                  sx={{
                                    color: isCiaRating(r.confidentiality)
                                      ? RATING_TONE_COLOR[scale1to5Tone(r.confidentiality)]
                                      : brand.muted,
                                  }}
                                >
                                  {isCiaRating(r.confidentiality) ? `C${r.confidentiality}` : 'C-'}
                                </Box>
                                /
                                <Box
                                  component="span"
                                  sx={{
                                    color: isCiaRating(r.integrityRating)
                                      ? RATING_TONE_COLOR[scale1to5Tone(r.integrityRating)]
                                      : brand.muted,
                                  }}
                                >
                                  {isCiaRating(r.integrityRating) ? `I${r.integrityRating}` : 'I-'}
                                </Box>
                                /
                                <Box
                                  component="span"
                                  sx={{
                                    color: isCiaRating(r.availability)
                                      ? RATING_TONE_COLOR[scale1to5Tone(r.availability)]
                                      : brand.muted,
                                  }}
                                >
                                  {isCiaRating(r.availability) ? `A${r.availability}` : 'A-'}
                                </Box>
                                {isCiaRating(r.confidentiality) && isCiaRating(r.integrityRating) && isCiaRating(r.availability) && (
                                  <Box component="span" sx={{ color: brand.muted, fontWeight: 500 }}>
                                    {' '}
                                    · {r.ciaScore}
                                  </Box>
                                )}
                              </>
                            )}
                          </Typography>
                        </TableCell>
                      )}
                      {assetColumns.isVisible('criticality') && (
                        <TableCell sx={compactTableCellSx}>
                          {isCiaRating(r.confidentiality) && isCiaRating(r.integrityRating) && isCiaRating(r.availability) && r.criticality ? (
                            <RiskLevelBadge
                              level={assetCriticalityToBadge(r.criticality)}
                              label={r.criticalityLabel ?? criticalityFromValue(Number(r.ciaScore)).label}
                            />
                          ) : (
                            <Typography variant="caption" color="text.secondary">-</Typography>
                          )}
                        </TableCell>
                      )}
                      {assetColumns.isVisible('classification') && (
                        <TableCell sx={mediumTableCellSx}>
                          <Typography variant="caption" noWrap>
                            {isCiaRating(r.confidentiality) && r.classification ? r.classification : '-'}
                          </Typography>
                        </TableCell>
                      )}
                      {assetColumns.isVisible('status') && (
                        <TableCell sx={{ ...compactTableCellSx, ...lastTableCellSx }}>
                          <StatusChip
                            label={assetStatusLabel(r.status)}
                            tone={assetStatusTone(r.status)}
                          />
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
        </>
      ) : (
        <Card
          sx={{
            flex: 1,
            minHeight: 0,
            overflow: 'auto',
            p: { xs: 1.25, sm: 1.5 },
            bgcolor: brand.canvas,
          }}
        >
          <Box sx={complexFormContainerSx} component="fieldset" disabled={!canEdit} style={{ border: 0, margin: 0, padding: 0, minWidth: 0 }}>
            {!canEdit ? (
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
                View only. Assigned access cannot change this record.
              </Typography>
            ) : !saveGate.ok ? (
              <Typography variant="caption" color="error" sx={{ display: 'block', mb: 1 }}>
                {saveGate.reason}
                {draftGate.ok
                  ? ' You can Save draft with Asset Name only, then finish later.'
                  : ''}
              </Typography>
            ) : saveBlockReason ? (
              <Typography variant="caption" color="error" sx={{ display: 'block', mb: 1 }}>
                {saveBlockReason}
              </Typography>
            ) : (
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
                Fill identification, ownership, location, and CIA ratings. Save draft keeps incomplete work
                (name required). Create / Save changes requires all asterisked fields and sets status Active.
              </Typography>
            )}
            {(editStatus === 'DRAFT' || (editId == null && form.name.trim())) && (
              <Box sx={{ mb: 1 }}>
                <StatusChip
                  label={editStatus === 'DRAFT' ? 'Draft' : 'Not saved yet'}
                  tone="warning"
                />
              </Box>
            )}
            <Section title="Identification" defaultExpanded>
              <CompactTextField
                label="Asset ID"
                fieldIdPrefix="asset"
                value={displayId ?? (editId == null ? 'Assigned on save' : '-')}
                disabled
              />
              <CompactTextField
                label="Asset Name"
                required
                fieldIdPrefix="asset"
                value={form.name}
                onChange={(e) => {
                  setField('name', e.target.value)
                  setFieldErrors((err) => ({ ...err, name: undefined }))
                }}
                error={Boolean(fieldErrors.name)}
                helperText={fieldErrors.name}
              />
              <FormSelect
                label="Asset Type"
                required
                value={form.assetType}
                options={typeOptions}
                onChange={(v) => {
                  setField('assetType', v as AssetType)
                  setFieldErrors((err) => ({ ...err, assetType: undefined, assetSubType: undefined }))
                }}
                error={Boolean(fieldErrors.assetType)}
                helperText={fieldErrors.assetType}
              />
              <FormSelect
                label="Asset Sub-Type"
                required={form.assetType === 'OTHER'}
                value={form.assetSubType}
                options={opt(meta?.assetSubTypes)}
                onChange={(v) => {
                  setField('assetSubType', v)
                  setFieldErrors((err) => ({ ...err, assetSubType: undefined }))
                }}
                maxWidth={280}
                error={Boolean(fieldErrors.assetSubType)}
                helperText={fieldErrors.assetSubType}
              />
              <FormSelect
                label="Environment"
                required
                value={form.environment}
                options={opt(meta?.environments, false)}
                onChange={(v) => {
                  setField('environment', v)
                  setFieldErrors((err) => ({ ...err, environment: undefined }))
                }}
                error={Boolean(fieldErrors.environment)}
                helperText={fieldErrors.environment}
              />
              <CompactTextField
                label="Asset Description"
                required
                multiline
                minRows={2}
                fieldIdPrefix="asset"
                value={form.description}
                onChange={(e) => {
                  setField('description', e.target.value)
                  setFieldErrors((err) => ({ ...err, description: undefined }))
                }}
                error={Boolean(fieldErrors.description)}
                helperText={fieldErrors.description || 'What it is and what it does'}
                sx={{ gridColumn: '1 / -1' }}
              />
            </Section>

            <Section title="Ownership" defaultExpanded>
              <FormSelect
                label="Asset Owner"
                required
                value={form.ownerUserId === '' ? '' : String(form.ownerUserId)}
                options={[
                  { value: '', label: 'Select' },
                  ...users.map((u) => ({ value: String(u.id), label: u.fullName })),
                ]}
                onChange={(v) => {
                  applyOwnerFromUser(v === '' ? '' : Number(v))
                  setFieldErrors((err) => ({ ...err, ownerUserId: undefined, ownerDepartment: undefined }))
                }}
                minWidth={200}
                maxWidth={280}
                error={Boolean(fieldErrors.ownerUserId)}
                helperText={fieldErrors.ownerUserId || 'Accountable owner'}
              />
              <CompactTextField
                label="Owner Job Title"
                fieldIdPrefix="asset"
                value={
                  form.ownerUserId === ''
                    ? '-'
                    : form.ownerJobTitle.trim() ||
                      ownerRoleTitle(users.find((u) => u.id === form.ownerUserId))
                }
                disabled
                helperText="From the owner's system role"
              />
              <CompactTextField
                label="Owner Department"
                required
                fieldIdPrefix="asset"
                value={form.ownerDepartment}
                onChange={(e) => {
                  setField('ownerDepartment', e.target.value)
                  setFieldErrors((err) => ({ ...err, ownerDepartment: undefined }))
                }}
                error={Boolean(fieldErrors.ownerDepartment)}
                helperText={fieldErrors.ownerDepartment || 'Filled from the owner org unit; you can edit'}
              />
              <FormSelect
                label="Asset Custodian"
                required
                value={form.custodianUserId === '' ? '' : String(form.custodianUserId)}
                options={[
                  { value: '', label: 'Select' },
                  ...users.map((u) => ({ value: String(u.id), label: u.fullName })),
                ]}
                onChange={(v) => {
                  const id = v === '' ? '' : Number(v)
                  setField('custodianUserId', id)
                  if (id !== '') {
                    const u = users.find((x) => x.id === id)
                    if (u) setField('custodianName', u.fullName)
                  }
                  setFieldErrors((err) => ({ ...err, custodian: undefined }))
                }}
                minWidth={200}
                maxWidth={280}
                error={Boolean(fieldErrors.custodian)}
                helperText={fieldErrors.custodian || 'Day-to-day custodian'}
              />
            </Section>

            <Section title="Location & technical">
              <CompactTextField
                label="Site / Building"
                fieldIdPrefix="asset"
                value={form.siteBuilding}
                onChange={(e) => setField('siteBuilding', e.target.value)}
              />
              <FormSelect
                label="Hosting Model"
                value={form.hostingModel}
                options={opt(meta?.hostingModels)}
                onChange={(v) => setField('hostingModel', v)}
              />
              <CompactTextField
                label="Cloud Provider & Region"
                fieldIdPrefix="asset"
                value={form.cloudProviderRegion}
                onChange={(e) => setField('cloudProviderRegion', e.target.value)}
              />
              <FormSelect
                label="Data Residency"
                value={form.dataResidency}
                options={opt(meta?.dataResidencies)}
                onChange={(v) => {
                  setField('dataResidency', v)
                  if (!isOtherDataResidency(v)) {
                    setField('dataResidencyOther', '')
                    setFieldErrors((err) => ({ ...err, dataResidencyOther: undefined }))
                  }
                }}
              />
              {isOtherDataResidency(form.dataResidency) && (
                <CompactTextField
                  label="Residency location"
                  required
                  fieldIdPrefix="asset"
                  value={form.dataResidencyOther}
                  onChange={(e) => {
                    setField('dataResidencyOther', e.target.value)
                    setFieldErrors((err) => ({ ...err, dataResidencyOther: undefined }))
                  }}
                  error={Boolean(fieldErrors.dataResidencyOther)}
                  helperText={fieldErrors.dataResidencyOther || 'Country, city, or data centre'}
                  maxLength={255}
                />
              )}
              <CompactTextField
                label="Hostname / FQDN"
                fieldIdPrefix="asset"
                value={form.hostname}
                onChange={(e) => setField('hostname', e.target.value)}
              />
              <CompactTextField
                label="IP Address (primary)"
                fieldIdPrefix="asset"
                value={form.ipAddress}
                onChange={(e) => setField('ipAddress', e.target.value)}
                wrapLabel
              />
              <CompactTextField
                label="Asset Tag / Serial Number"
                fieldIdPrefix="asset"
                value={form.assetTagSerial}
                onChange={(e) => setField('assetTagSerial', e.target.value)}
              />
              <CompactTextField
                label="Platform / OS / Version"
                fieldIdPrefix="asset"
                value={form.platformOsVersion}
                onChange={(e) => setField('platformOsVersion', e.target.value)}
              />
            </Section>

            <Section title="CIA valuation" defaultExpanded>
              <Box sx={{ gridColumn: '1 / -1', display: 'flex', flexDirection: 'column', gap: 1.5 }}>
                <Box sx={ciaPairRowSx}>
                  <FormSelect
                    label="Confidentiality Rating"
                    required
                    wrapLabel
                    value={form.c === '' ? '' : String(form.c)}
                    options={CIA_RATING_OPTIONS}
                    onChange={(v) => {
                      setField('c', v === '' ? '' : Number(v))
                      setFieldErrors((err) => ({ ...err, c: undefined }))
                    }}
                    minWidth={220}
                    maxWidth={280}
                    error={Boolean(fieldErrors.c)}
                    helperText={fieldErrors.c}
                  />
                  {isCiaRating(form.c) && (
                    <CompactTextField
                      label="Confidentiality Justification"
                      required
                      multiline
                      minRows={2}
                      fieldIdPrefix="asset"
                      value={form.cJust}
                      onChange={(e) => {
                        setField('cJust', e.target.value)
                        setFieldErrors((err) => ({ ...err, cJust: undefined }))
                      }}
                      error={Boolean(fieldErrors.cJust)}
                      helperText={fieldErrors.cJust}
                      maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
                    />
                  )}
                </Box>
                <Box sx={ciaPairRowSx}>
                  <FormSelect
                    label="Integrity Rating"
                    required
                    wrapLabel
                    value={form.i === '' ? '' : String(form.i)}
                    options={CIA_RATING_OPTIONS}
                    onChange={(v) => {
                      setField('i', v === '' ? '' : Number(v))
                      setFieldErrors((err) => ({ ...err, i: undefined }))
                    }}
                    minWidth={220}
                    maxWidth={280}
                    error={Boolean(fieldErrors.i)}
                    helperText={fieldErrors.i}
                  />
                  {isCiaRating(form.i) && (
                    <CompactTextField
                      label="Integrity Justification"
                      required
                      multiline
                      minRows={2}
                      fieldIdPrefix="asset"
                      value={form.iJust}
                      onChange={(e) => {
                        setField('iJust', e.target.value)
                        setFieldErrors((err) => ({ ...err, iJust: undefined }))
                      }}
                      error={Boolean(fieldErrors.iJust)}
                      helperText={fieldErrors.iJust}
                      maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
                    />
                  )}
                </Box>
                <Box sx={ciaPairRowSx}>
                  <FormSelect
                    label="Availability Rating"
                    required
                    wrapLabel
                    value={form.a === '' ? '' : String(form.a)}
                    options={CIA_RATING_OPTIONS}
                    onChange={(v) => {
                      setField('a', v === '' ? '' : Number(v))
                      setFieldErrors((err) => ({ ...err, a: undefined }))
                    }}
                    minWidth={220}
                    maxWidth={280}
                    error={Boolean(fieldErrors.a)}
                    helperText={fieldErrors.a}
                  />
                  {isCiaRating(form.a) && (
                    <CompactTextField
                      label="Availability Justification"
                      required
                      multiline
                      minRows={2}
                      fieldIdPrefix="asset"
                      value={form.aJust}
                      onChange={(e) => {
                        setField('aJust', e.target.value)
                        setFieldErrors((err) => ({ ...err, aJust: undefined }))
                      }}
                      error={Boolean(fieldErrors.aJust)}
                      helperText={fieldErrors.aJust}
                      maxWidth={NARRATIVE_FIELD_MAX_WIDTH}
                    />
                  )}
                </Box>
              </Box>
            </Section>

            <SettingsCard
              autoHeight
              heading
              title="Calculation"
              subtitle="Results update as you set Confidentiality, Integrity, and Availability."
            >
              <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))', md: 'repeat(4, minmax(0, 1fr))' },
                  gap: 1,
                }}
              >
                <CalcStat label="Asset Value" hint="MAX(C, I, A)">
                  <Typography
                    sx={{
                      fontSize: '1.25rem',
                      fontWeight: 700,
                      lineHeight: 1,
                      color: previewValue != null ? RATING_TONE_COLOR[scale1to5Tone(previewValue)] : brand.muted,
                    }}
                  >
                    {previewValue ?? '-'}
                  </Typography>
                </CalcStat>
                <CalcStat label="Weighted Score" hint="Equal weights (Wc = Wi = Wa = 1)">
                  <Typography sx={{ fontSize: '1.25rem', fontWeight: 700, lineHeight: 1, color: brand.ink }}>
                    {previewWeighted ?? '-'}
                  </Typography>
                </CalcStat>
                <CalcStat label="Criticality" hint="Follows Asset Value">
                  {previewCrit ? (
                    <RiskLevelBadge
                      level={assetCriticalityToBadge(previewCrit.level)}
                      label={previewCrit.label}
                    />
                  ) : (
                    <Typography sx={{ fontSize: '0.875rem', color: brand.muted }}>-</Typography>
                  )}
                </CalcStat>
                <CalcStat label="Classification" hint="Follows Confidentiality">
                  <Typography
                    sx={{
                      fontSize: '1.125rem',
                      fontWeight: 700,
                      lineHeight: 1.2,
                      color: derivedClass && isCiaRating(form.c)
                        ? RATING_TONE_COLOR[scale1to5Tone(form.c)]
                        : brand.muted,
                    }}
                  >
                    {derivedClass ? derivedClassificationLabel(derivedClass) : '-'}
                  </Typography>
                </CalcStat>
              </Box>

              {derivedClass && (
                <Box
                  sx={{
                    mt: 1.5,
                    px: 1.5,
                    py: 1.25,
                    border: `1px solid ${brand.border}`,
                    borderLeft: `3px solid ${isCiaRating(form.c) ? RATING_TONE_COLOR[scale1to5Tone(form.c)] : brand.gold}`,
                    borderRadius: 1,
                    bgcolor: brand.paper,
                  }}
                >
                  <Typography sx={{ fontSize: '0.8125rem', fontWeight: 700, color: brand.ink }}>
                    {derivedClassificationLabel(derivedClass)}
                  </Typography>
                  <Typography sx={{ mt: 0.5, fontSize: '0.75rem', color: brand.muted, lineHeight: 1.5, maxWidth: 640 }}>
                    {DERIVED_CLASSIFICATION_COPY[derivedClass]}
                  </Typography>
                  {handling && (
                    <Box sx={{ mt: 1 }}>
                      <Typography
                        sx={{
                          fontSize: '0.6875rem',
                          fontWeight: 600,
                          color: brand.muted,
                          letterSpacing: '0.04em',
                          textTransform: 'uppercase',
                        }}
                      >
                        Labelling
                      </Typography>
                      <Typography sx={{ mt: 0.35, fontSize: '0.75rem', color: brand.inkSoft, lineHeight: 1.5, maxWidth: 640 }}>
                        {handling.labelling}
                      </Typography>
                    </Box>
                  )}
                </Box>
              )}

              <Box
                sx={{
                  mt: 2,
                  pt: 1.5,
                  borderTop: `1px solid ${brand.border}`,
                  display: 'grid',
                  gridTemplateColumns: { xs: '1fr', md: 'minmax(0, 1fr) minmax(0, 1.1fr)' },
                  gap: { xs: 1.5, md: 3 },
                }}
              >
                <Box>
                  <Typography
                    sx={{
                      fontSize: '0.6875rem',
                      fontWeight: 700,
                      color: brand.muted,
                      letterSpacing: '0.04em',
                      textTransform: 'uppercase',
                      mb: 1,
                    }}
                  >
                    Formulas
                  </Typography>
                  <Typography sx={{ fontSize: '0.75rem', color: brand.inkSoft, lineHeight: 1.55 }}>
                    Asset Value = MAX(Confidentiality, Integrity, Availability)
                  </Typography>
                  <Typography sx={{ mt: 0.75, fontSize: '0.75rem', color: brand.inkSoft, lineHeight: 1.55 }}>
                    Weighted Score = (C.Wc + I.Wi + A.Wa) / (Wc + Wi + Wa)
                  </Typography>
                </Box>
                <Box>
                  <Typography
                    sx={{
                      fontSize: '0.6875rem',
                      fontWeight: 700,
                      color: brand.muted,
                      letterSpacing: '0.04em',
                      textTransform: 'uppercase',
                      mb: 1,
                    }}
                  >
                    Value map
                  </Typography>
                  <Typography sx={{ fontSize: '0.6875rem', color: brand.mutedLight, mb: 0.75, lineHeight: 1.4 }}>
                    Criticality follows Asset Value. Classification in this map applies when Confidentiality equals Asset Value.
                  </Typography>
                  <Box
                    sx={{
                      display: 'grid',
                      gridTemplateColumns: '48px 1fr 1fr',
                      columnGap: 1,
                      px: 1,
                      pb: 0.5,
                    }}
                  >
                    {['Value', 'Criticality', 'Classification'].map((h) => (
                      <Typography
                        key={h}
                        sx={{
                          fontSize: '0.625rem',
                          fontWeight: 700,
                          color: brand.mutedLight,
                          letterSpacing: '0.04em',
                          textTransform: 'uppercase',
                        }}
                      >
                        {h}
                      </Typography>
                    ))}
                  </Box>
                  {ASSET_VALUE_TIER_MAP.map((row) => {
                    const active = previewValue != null && row.value === previewValue
                    return (
                      <Box
                        key={row.value}
                        sx={{
                          display: 'grid',
                          gridTemplateColumns: '48px 1fr 1fr',
                          columnGap: 1,
                          px: 1,
                          py: 0.55,
                          borderRadius: 0.5,
                          bgcolor: active ? brand.sidebarSelectedFill : 'transparent',
                          boxShadow: active ? `inset 3px 0 0 ${brand.gold}` : undefined,
                        }}
                      >
                        <Typography sx={{ fontSize: '0.75rem', fontWeight: active ? 700 : 600, color: brand.ink }}>
                          {row.value}
                        </Typography>
                        <Typography sx={{ fontSize: '0.75rem', fontWeight: active ? 700 : 500, color: brand.ink }}>
                          {row.tier}
                        </Typography>
                        <Typography sx={{ fontSize: '0.75rem', fontWeight: active ? 700 : 500, color: brand.ink }}>
                          {row.classification}
                        </Typography>
                      </Box>
                    )
                  })}
                </Box>
              </Box>
            </SettingsCard>

            <Section title="Lifecycle">
              <CompactTextField
                label="End of Life / End of Support"
                type="date"
                fieldIdPrefix="asset"
                value={form.endOfLifeDate}
                onChange={(e) => setField('endOfLifeDate', e.target.value)}
                InputLabelProps={{ shrink: true }}
                wrapLabel
                sx={{ maxWidth: DATE_FIELD_WIDTH + 80, minWidth: DATE_FIELD_WIDTH }}
              />
            </Section>
          </Box>
        </Card>
      )}
    <ConfirmActionDialog
      open={confirmDispose}
      title="Dispose this asset?"
      message="The asset stays on the register as Disposed. You can cancel if you clicked by mistake."
      confirmLabel="Dispose"
      confirmColor="error"
      busy={busy}
      onCancel={() => setConfirmDispose(false)}
      onConfirm={() => {
        if (editId == null) return
        setConfirmDispose(false)
        void dispose(editId)
      }}
    />
    </Stack>
  )
}
