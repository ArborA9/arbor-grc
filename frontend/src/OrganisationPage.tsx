import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Box,
  Button,
  Card,
  Collapse,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  IconButton,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Tab,
  Tabs,
  Typography,
} from '@mui/material'
import AddRoundedIcon from '@mui/icons-material/AddRounded'
import ExpandMoreRoundedIcon from '@mui/icons-material/ExpandMoreRounded'
import ChevronRightRoundedIcon from '@mui/icons-material/ChevronRightRounded'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import {
  api,
  ORG_DATE_FORMATS,
  ORG_SIZE_BANDS,
  ORG_TIMEZONES,
  type ModuleLicense,
  type OrgLevel,
  type OrgProfile,
  type OrgUnitNode,
  type Session,
  type SsoGroupRoleMapping,
  type NotificationTemplateSummary,
  type NotificationItem,
  type TenantSsoStatus,
  type RiskLevel,
} from './api'
import { appetiteMaxResidual, parseGridSize } from './components/risk/riskMatrix'
import StatusChip from './components/StatusChip'
import ActiveModulesList from './components/org/ActiveModulesList'
import OrgLevelPreview from './components/org/OrgLevelPreview'
import {
  CompactTextField,
  INPUT_LIMIT,
  FieldActionRow,
  FormSelect,
  controlActionSx,
  SectionHeader,
  SettingsCard,
  SettingsField,
  SettingsReadonlyValue,
} from './components/ui'
import { formatTenantDateTime } from './uiQuality/tenantLocale'
import { roleLabel } from './uiQuality/roleLabels'
import { ModuleListShell } from './hooks/useModulePageShell'
import { brand } from './theme'

/** Level-name editor on Structure. Turn back on when clients need to rename the four levels. */
const SHOW_LEVEL_NAMES = false

const SSO_MAPPABLE_ROLES = [
  'OWNER',
  'ADMIN',
  'CONTRIBUTOR',
  'INTERNAL_AUDITOR',
  'GLOBAL_READER',
] as const

function UnitRow({
  unit,
  depth,
  levelLabel,
  canEdit,
  onAddChild,
  onEdit,
  onDeactivate,
  onReactivate,
  onDelete,
}: {
  unit: OrgUnitNode
  depth: number
  levelLabel: (level: number) => string
  canEdit: boolean
  onAddChild: (parent: OrgUnitNode) => void
  onEdit: (unit: OrgUnitNode) => void
  onDeactivate: (unit: OrgUnitNode) => void
  onReactivate: (unit: OrgUnitNode) => void
  onDelete: (unit: OrgUnitNode) => void
}) {
  const [open, setOpen] = useState(true)
  const hasKids = (unit.children?.length ?? 0) > 0

  return (
    <Box>
      <Stack
        direction="row"
        alignItems="center"
        spacing={0.75}
        sx={{
          pl: depth * 2.5,
          py: 0.75,
          pr: 1,
          borderBottom: `1px solid ${brand.hairline}`,
          opacity: unit.active ? 1 : 0.55,
          '&:hover': { bgcolor: 'rgba(15, 23, 42, 0.02)' },
        }}
      >
        <IconButton size="small" disabled={!hasKids} onClick={() => setOpen((v) => !v)} sx={{ p: 0.25 }}>
          {hasKids ? (
            open ? (
              <ExpandMoreRoundedIcon fontSize="small" />
            ) : (
              <ChevronRightRoundedIcon fontSize="small" />
            )
          ) : (
            <Box sx={{ width: 20 }} />
          )}
        </IconButton>
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography variant="body2" fontWeight={600} noWrap>
            {unit.name}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {levelLabel(unit.level)}
          </Typography>
        </Box>
        {unit.source === 'ENTRA' && <StatusChip label="Entra" tone="info" />}
        {!unit.active && <StatusChip label="Inactive" tone="neutral" />}
        {canEdit && (
          <Stack direction="row" spacing={0.5}>
            {unit.active && (
              <Button size="small" startIcon={<AddRoundedIcon />} onClick={() => onAddChild(unit)}>
                Child
              </Button>
            )}
            <IconButton size="small" onClick={() => onEdit(unit)} aria-label="Edit unit">
              <EditOutlinedIcon fontSize="small" />
            </IconButton>
            {unit.active ? (
              <Button size="small" color="warning" onClick={() => onDeactivate(unit)}>
                Deactivate
              </Button>
            ) : (
              <Button size="small" onClick={() => onReactivate(unit)}>
                Reactivate
              </Button>
            )}
            {!hasKids && (
              <Button size="small" color="error" onClick={() => onDelete(unit)}>
                Delete
              </Button>
            )}
          </Stack>
        )}
      </Stack>
      {hasKids && (
        <Collapse in={open}>
          {unit.children.map((c) => (
            <UnitRow
              key={c.id}
              unit={c}
              depth={depth + 1}
              levelLabel={levelLabel}
              canEdit={canEdit}
              onAddChild={onAddChild}
              onEdit={onEdit}
              onDeactivate={onDeactivate}
              onReactivate={onReactivate}
              onDelete={onDelete}
            />
          ))}
        </Collapse>
      )}
    </Box>
  )
}

export default function OrganisationPage({
  session,
  onError,
  onOk,
  onOpenWizard,
  onNavigate,
  onOpenNotification,
  section = 'org',
}: {
  session: Session
  onError: (msg: string) => void
  onOk: (msg: string) => void
  onOpenWizard?: () => void
  onNavigate?: (navId: string) => void
  onOpenNotification?: (n: NotificationItem) => void
  /** Req §3.6 Settings → Org Structure vs Notifications. */
  section?: 'org' | 'notifications'
}) {
  const canEdit = session.roles.some((r) => ['OWNER', 'ADMIN'].includes(r))
  const showOrg = section === 'org'
  const showNotifications = section === 'notifications'

  const [profile, setProfile] = useState<OrgProfile | null>(null)
  const [levels, setLevels] = useState<OrgLevel[]>([])
  const [tree, setTree] = useState<OrgUnitNode[]>([])
  const [moduleLicenses, setModuleLicenses] = useState<ModuleLicense[]>([])
  const [ssoStatus, setSsoStatus] = useState<TenantSsoStatus | null>(null)
  const [showInactive, setShowInactive] = useState(false)
  const [busy, setBusy] = useState(false)

  const [displayName, setDisplayName] = useState('')
  const [industry, setIndustry] = useState('')
  const [sizeBand, setSizeBand] = useState('')
  const [timezone, setTimezone] = useState('Asia/Dubai')
  const [timezoneOther, setTimezoneOther] = useState(false)
  const [dateFormat, setDateFormat] = useState('DD/MM/YYYY')
  const [idleTimeoutMinutes, setIdleTimeoutMinutes] = useState(5)
  const [orgSection, setOrgSection] = useState<'profile' | 'structure' | 'access'>('profile')
  const [deptName, setDeptName] = useState('')
  const [syncMessage, setSyncMessage] = useState('')
  const [riskMatrixGridSize, setRiskMatrixGridSize] = useState('5')
  const [riskAppetiteRating, setRiskAppetiteRating] = useState('LOW')
  const [levelDrafts, setLevelDrafts] = useState<Record<number, string>>({})

  const [unitDialog, setUnitDialog] = useState<null | {
    mode: 'create' | 'edit'
    parent?: OrgUnitNode | null
    unit?: OrgUnitNode
  }>(null)
  const [unitName, setUnitName] = useState('')
  const [confirm, setConfirm] = useState<null | {
    action: 'deactivate' | 'delete'
    unit: OrgUnitNode
  }>(null)

  const [ssoMappings, setSsoMappings] = useState<SsoGroupRoleMapping[]>([])
  const [newGroupName, setNewGroupName] = useState('')
  const [newGroupRole, setNewGroupRole] = useState<string>('CONTRIBUTOR')
  const [notifyTemplates, setNotifyTemplates] = useState<NotificationTemplateSummary[]>([])
  const [notifyHistory, setNotifyHistory] = useState<NotificationItem[]>([])
  const [notifyHistoryPage, setNotifyHistoryPage] = useState(0)
  const [notifyHistoryPages, setNotifyHistoryPages] = useState(0)
  const [notifyHistoryTotal, setNotifyHistoryTotal] = useState(0)

  const load = useCallback(async () => {
    setBusy(true)
    try {
      const [p, lv, units, mappings, templates, modules, sso] = await Promise.all([
        api.getOrgProfile(),
        api.listOrgLevels(),
        api.listOrgUnits(showInactive),
        canEdit
          ? api.listSsoGroupRoleMappings().catch(() => [] as SsoGroupRoleMapping[])
          : Promise.resolve([] as SsoGroupRoleMapping[]),
        canEdit
          ? api.listNotificationTemplates().catch(() => [] as NotificationTemplateSummary[])
          : Promise.resolve([] as NotificationTemplateSummary[]),
        api.listMyModules().catch(() => ({ modules: [] as ModuleLicense[], enabled: [] as string[] })),
        session.tenantId
          ? api.tenantSsoStatus(session.tenantId).catch(() => null)
          : Promise.resolve(null),
      ])
      setProfile(p)
      setLevels(lv)
      setTree(units)
      setSsoMappings(mappings)
      setNotifyTemplates(templates)
      setModuleLicenses(modules.modules ?? [])
      setSsoStatus(sso)
      setDisplayName(p.displayName ?? '')
      setIndustry(p.industry ?? '')
      setSizeBand(p.sizeBand ?? '')
      setTimezone(p.timezone)
      setTimezoneOther(!ORG_TIMEZONES.some((z) => z.value === p.timezone))
      setDateFormat(p.dateFormat)
      setIdleTimeoutMinutes(p.idleTimeoutMinutes ?? 5)
      setRiskMatrixGridSize(String(p.riskMatrixGridSize ?? 5))
      setRiskAppetiteRating(p.riskAppetiteRating ?? 'LOW')
      const drafts: Record<number, string> = {}
      lv.forEach((l) => {
        drafts[l.level] = l.label
      })
      setLevelDrafts(drafts)
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }, [onError, showInactive, canEdit, session.tenantId])

  const loadNotifyHistory = useCallback(async (page = 0) => {
    try {
      const result = await api.listNotificationsPage(page, 25)
      setNotifyHistory(result.content ?? [])
      setNotifyHistoryPage(result.number ?? page)
      setNotifyHistoryPages(result.totalPages ?? 0)
      setNotifyHistoryTotal(result.totalElements ?? 0)
    } catch (e) {
      onError(String(e))
    }
  }, [onError])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    if (showNotifications) {
      void loadNotifyHistory(0)
    }
  }, [showNotifications, loadNotifyHistory])

  const openNotification = async (n: NotificationItem) => {
    if (n.unread) {
      try {
        await api.markNotificationRead(n.id)
        await loadNotifyHistory(notifyHistoryPage)
      } catch (e) {
        onError(String(e))
        return
      }
    }
    if (onOpenNotification) {
      onOpenNotification(n)
    } else if (n.link && onNavigate) {
      onNavigate(n.link)
    }
  }

  const markAllNotificationsRead = async () => {
    setBusy(true)
    try {
      await api.markAllNotificationsRead()
      await loadNotifyHistory(notifyHistoryPage)
      onOk('All notifications marked read')
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const levelLabel = useMemo(() => {
    const map = new Map(levels.map((l) => [l.level, l.label]))
    return (level: number) =>
      map.get(level) ?? map.get(Math.min(level, 4)) ?? `Level ${level}`
  }, [levels])

  const registryName = ssoStatus?.name ?? session.tenantId ?? '-'

  const saveProfile = async () => {
    setBusy(true)
    try {
      const p = await api.updateOrgProfile({
        displayName: displayName.trim() || null,
        industry: industry.trim() || null,
        sizeBand: sizeBand || null,
        timezone,
        dateFormat,
        idleTimeoutMinutes,
        riskMatrixGridSize: profile?.riskMatrixLocked ? undefined : Number(riskMatrixGridSize) || 5,
        riskAppetiteRating: profile?.riskMatrixLocked ? undefined : riskAppetiteRating,
      })
      setProfile(p)
      setIdleTimeoutMinutes(p.idleTimeoutMinutes ?? idleTimeoutMinutes)
      onOk('Organisation profile saved')
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const saveLevels = async () => {
    setBusy(true)
    try {
      const body = [1, 2, 3, 4].map((level) => ({
        level,
        label: (levelDrafts[level] ?? '').trim(),
      }))
      if (body.some((b) => !b.label)) {
        onError('Each level needs a label')
        return
      }
      const lv = await api.updateOrgLevels(body)
      setLevels(lv)
      onOk('Level labels updated')
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const openCreate = (parent?: OrgUnitNode | null) => {
    setUnitName('')
    setUnitDialog({ mode: 'create', parent: parent ?? null })
  }

  const openEdit = (unit: OrgUnitNode) => {
    setUnitName(unit.name)
    setUnitDialog({ mode: 'edit', unit })
  }

  const saveUnit = async () => {
    if (!unitDialog || !unitName.trim()) return
    setBusy(true)
    try {
      if (unitDialog.mode === 'create') {
        await api.createOrgUnit({
          name: unitName.trim(),
          parentId: unitDialog.parent?.id ?? null,
        })
        onOk('Unit added')
      } else if (unitDialog.unit) {
        await api.updateOrgUnit(unitDialog.unit.id, { name: unitName.trim() })
        onOk('Unit updated')
      }
      setUnitDialog(null)
      await load()
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const runConfirm = async () => {
    if (!confirm) return
    setBusy(true)
    try {
      if (confirm.action === 'deactivate') {
        await api.deactivateOrgUnit(confirm.unit.id)
        onOk('Unit deactivated')
      } else {
        await api.deleteOrgUnit(confirm.unit.id)
        onOk('Unit deleted')
      }
      setConfirm(null)
      await load()
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const addSsoMapping = async () => {
    if (!newGroupName.trim()) return
    setBusy(true)
    try {
      await api.createSsoGroupRoleMapping({
        idpGroupName: newGroupName.trim(),
        role: newGroupRole,
      })
      setNewGroupName('')
      setNewGroupRole('CONTRIBUTOR')
      setSsoMappings(await api.listSsoGroupRoleMappings())
      onOk('SSO group mapping added')
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const removeSsoMapping = async (id: number) => {
    setBusy(true)
    try {
      await api.deleteSsoGroupRoleMapping(id)
      setSsoMappings(await api.listSsoGroupRoleMappings())
      onOk('SSO group mapping removed')
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const toggleTemplate = async (id: number, active: boolean) => {
    setBusy(true)
    try {
      await api.setNotificationTemplateActive(id, active)
      setNotifyTemplates(await api.listNotificationTemplates())
      onOk(active ? 'Notification trigger enabled' : 'Notification trigger disabled')
    } catch (e) {
      onError(String(e))
    } finally {
      setBusy(false)
    }
  }

  const orgActions = useMemo(
    () =>
      canEdit && onOpenWizard ? (
        <Button variant="outlined" onClick={onOpenWizard} disabled={busy}>
          {profile?.wizardCompleted ? 'Re-run Guided Setup' : 'Guided Setup'}
        </Button>
      ) : undefined,
    [busy, canEdit, onOpenWizard, profile?.wizardCompleted],
  )

  return (
    <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
      <ModuleListShell
        title={showOrg ? 'Organisation Settings' : 'Notifications'}
        breadcrumbTrail={showOrg ? 'Settings › Organisation' : 'Settings › Notifications'}
        actions={showOrg ? orgActions : undefined}
      />

      {showOrg && (
        <Box sx={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
          <Tabs
            value={orgSection}
            onChange={(_, value) => setOrgSection(value)}
            sx={{
              minHeight: 30,
              position: 'relative',
              zIndex: 1,
              '& .MuiTabs-indicator': { display: 'none' },
              '& .MuiTabs-flexContainer': { gap: 0.5, alignItems: 'flex-end' },
              '& .MuiTab-root': { minHeight: 30 },
            }}
          >
            {([
              ['profile', 'Profile'],
              ['structure', 'Structure'],
              ['access', 'Access'],
            ] as const).map(([value, label]) => (
              <Tab
                key={value}
                value={value}
                label={label}
                sx={{
                  minHeight: 30,
                  minWidth: 72,
                  px: 1.5,
                  py: 0.25,
                  textTransform: 'none',
                  fontWeight: 650,
                  fontSize: '0.8125rem',
                  lineHeight: 1.2,
                  color: brand.ink,
                  bgcolor: brand.canvas,
                  border: `1px solid ${brand.border}`,
                  borderRadius: '6px 6px 0 0',
                  mb: '-1px',
                  '&.Mui-selected': {
                    color: '#fff',
                    bgcolor: brand.gold,
                    borderColor: brand.gold,
                    zIndex: 1,
                  },
                  '&:hover': {
                    bgcolor: brand.goldLight,
                    borderColor: brand.gold,
                    color: brand.gold,
                  },
                  '&.Mui-selected:hover': {
                    color: '#fff',
                    bgcolor: brand.gold,
                    borderColor: brand.gold,
                  },
                }}
              />
            ))}
          </Tabs>
          <Box
            sx={{
              flex: 1,
              minHeight: 0,
              display: 'flex',
              flexDirection: 'column',
              gap: 1.25,
              overflow: 'auto',
              p: 1.5,
              bgcolor: brand.paper,
              border: `1px solid ${brand.border}`,
              borderRadius: orgSection === 'profile' ? '0 8px 8px 8px' : '8px',
            }}
          >
          <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={2} sx={{ minHeight: 32 }}>
            <Typography variant="body2" color="text.secondary">
              {orgSection === 'profile'
                ? 'Organisation identity, regional settings, and risk defaults.'
                : orgSection === 'structure'
                  ? 'Departments for this organisation. Sync names from Entra, or add any that are not in the directory.'
                  : 'Sign-in, session timeout, and Entra group to role mappings.'}
            </Typography>
            {orgSection === 'profile' && canEdit && (
              <Button variant="contained" onClick={() => void saveProfile()} disabled={busy} sx={{ flexShrink: 0 }}>
                Save Profile
              </Button>
            )}
          </Stack>
          {(orgSection === 'profile' || orgSection === 'structure' || orgSection === 'access') && (
          <>
          {orgSection === 'profile' && (
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: { xs: '1fr', lg: 'minmax(0, 1.4fr) minmax(280px, 0.8fr)' },
                gap: 1.25,
                alignItems: 'start',
              }}
            >
              <Stack spacing={1.25}>
                <SettingsCard title="Identity" subtitle="How this organisation is named in Arbor." autoHeight>
                  <Stack spacing={1.25}>
                    <CompactTextField
                      label="Organisation name"
                      fullWidth
                      value={displayName}
                      onChange={(e) => setDisplayName(e.target.value)}
                      disabled={!canEdit}
                      placeholder={registryName}
                      helperText="Shown in the product. The provisioned registry name is unchanged."
                    />
                    <CompactTextField
                      label="Sector / industry"
                      fullWidth
                      value={industry}
                      onChange={(e) => setIndustry(e.target.value)}
                      disabled={!canEdit}
                    />
                    <FormSelect
                      label="Organisation size"
                      disabled={!canEdit}
                      value={sizeBand}
                      onChange={setSizeBand}
                      options={[
                        { value: '', label: 'Not set' },
                        ...ORG_SIZE_BANDS.map((b) => ({ value: b, label: b })),
                      ]}
                    />
                  </Stack>
                </SettingsCard>
                <SettingsCard title="Region and calendar" subtitle="Timestamps and register dates." autoHeight>
                  <Stack spacing={1.25}>
                    <FormSelect
                      label="Timezone"
                      fullWidth
                      disabled={!canEdit}
                      value={timezoneOther ? '__OTHER__' : timezone}
                      onChange={(v) => {
                        if (v === '__OTHER__') {
                          setTimezoneOther(true)
                          if (ORG_TIMEZONES.some((z) => z.value === timezone)) setTimezone('')
                        } else {
                          setTimezoneOther(false)
                          setTimezone(v)
                        }
                      }}
                      options={[
                        ...ORG_TIMEZONES.map((z) => ({ value: z.value, label: z.label })),
                        { value: '__OTHER__', label: 'Other (IANA id)' },
                      ]}
                    />
                    {timezoneOther && (
                      <CompactTextField
                        label="IANA timezone"
                        fullWidth
                        value={timezone}
                        onChange={(e) => setTimezone(e.target.value)}
                        disabled={!canEdit}
                        helperText="Example: Asia/Tokyo"
                      />
                    )}
                    <FormSelect
                      label="Date format"
                      disabled={!canEdit}
                      value={dateFormat}
                      onChange={setDateFormat}
                      options={ORG_DATE_FORMATS.map((f) => ({ value: f, label: f }))}
                    />
                    <Typography variant="caption" color="text.secondary">
                      Registers use this format in {timezone.replace(/_/g, ' ') || 'the selected timezone'}. Calendar pickers follow the browser.
                    </Typography>
                  </Stack>
                </SettingsCard>
                <SettingsCard title="Risk defaults" subtitle="Used by appetite and scoring." autoHeight>
                  <Stack spacing={1.25}>
                    <FormSelect
                      label="Risk matrix size"
                      disabled={!canEdit || Boolean(profile?.riskMatrixLocked)}
                      value={riskMatrixGridSize}
                      onChange={setRiskMatrixGridSize}
                      options={[
                        { value: '3', label: '3 x 3' },
                        { value: '4', label: '4 x 4' },
                        { value: '5', label: '5 x 5 (default)' },
                      ]}
                    />
                    <FormSelect
                      label="Risk appetite"
                      disabled={!canEdit || Boolean(profile?.riskMatrixLocked)}
                      value={riskAppetiteRating}
                      onChange={setRiskAppetiteRating}
                      options={[
                        { value: 'VERY_LOW', label: 'Very low' },
                        { value: 'LOW', label: 'Low (default)' },
                        { value: 'MEDIUM', label: 'Medium' },
                        { value: 'HIGH', label: 'High' },
                        { value: 'VERY_HIGH', label: 'Very high' },
                      ]}
                    />
                    <Typography variant="caption" color="text.secondary">
                      Maximum residual within appetite: {appetiteMaxResidual(
                        parseGridSize(Number(riskMatrixGridSize)),
                        (riskAppetiteRating as RiskLevel) || 'LOW',
                      )}
                    </Typography>
                    {profile?.riskMatrixLocked && (
                      <Typography variant="caption" color="text.secondary">
                        Matrix size is locked after guided setup. Appetite and scoring still use this grid.
                      </Typography>
                    )}
                  </Stack>
                </SettingsCard>
              </Stack>
              <Stack spacing={1.25}>
                <SettingsCard title="Workspace" subtitle="Read only. Set when the workspace was provisioned." autoHeight>
                  <Stack spacing={1.25}>
                    <SettingsField label="Tenant ID" mono>
                      <SettingsReadonlyValue>{session.tenantId ?? '-'}</SettingsReadonlyValue>
                    </SettingsField>
                    <SettingsField label="Registry name">
                      <SettingsReadonlyValue>{registryName || '-'}</SettingsReadonlyValue>
                    </SettingsField>
                    <SettingsField label="Data region">
                      <SettingsReadonlyValue>Azure UAE North</SettingsReadonlyValue>
                    </SettingsField>
                    <SettingsField label="Sign-in">
                      <SettingsReadonlyValue>
                        {ssoStatus?.ssoEnabled ? 'Microsoft Entra ID' : 'Password only'}
                      </SettingsReadonlyValue>
                    </SettingsField>
                  </Stack>
                </SettingsCard>
                <SettingsCard title="Licensed modules" subtitle="What this workspace can open. Super Admin changes licences." autoHeight>
                  <ActiveModulesList modules={moduleLicenses} layout="compact" />
                </SettingsCard>
              </Stack>
            </Box>
          )}

          {orgSection === 'structure' && (
            <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0 }}>
              {SHOW_LEVEL_NAMES && (
                <SettingsCard title="Level names" subtitle="Four labels. The units below use these names." autoHeight>
                  <OrgLevelPreview
                    drafts={levelDrafts}
                    disabled={!canEdit}
                    onChange={(level, label) => setLevelDrafts((d) => ({ ...d, [level]: label }))}
                  />
                  {canEdit && (
                    <Button variant="contained" size="small" sx={{ mt: 1.5 }} onClick={() => void saveLevels()} disabled={busy}>
                      Save Labels
                    </Button>
                  )}
                </SettingsCard>
              )}
              <Card id="org-unit-tree" sx={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 280, overflow: 'hidden' }}>
                <Box sx={{ px: 2, py: 1.25, borderBottom: `1px solid ${brand.hairline}` }}>
                  <SectionHeader
                    title="Departments"
                    hint="Add a department here, or sync names from Entra."
                  />
                  {canEdit && (
                    <FieldActionRow sx={{ mt: 1.25, flexWrap: 'nowrap' }}>
                      <Box sx={{ flex: '1 1 auto', minWidth: 0 }}>
                        <CompactTextField
                          label={`${levelLabel(1)} name`}
                          value={deptName}
                          onChange={(e) => setDeptName(e.target.value)}
                          disabled={busy}
                        />
                      </Box>
                      <Button
                        variant="contained"
                        disabled={busy || !deptName.trim()}
                        sx={controlActionSx}
                        onClick={() => {
                          void (async () => {
                            setBusy(true)
                            try {
                              await api.createOrgUnit({ name: deptName.trim() })
                              setDeptName('')
                              onOk(`${levelLabel(1)} added`)
                              await load()
                            } catch (e) {
                              onError(String(e))
                            } finally {
                              setBusy(false)
                            }
                          })()
                        }}
                      >
                        Add
                      </Button>
                      <Button
                        variant="outlined"
                        disabled={busy}
                        sx={{ ...controlActionSx, whiteSpace: 'nowrap' }}
                        onClick={() => {
                          void (async () => {
                            setBusy(true)
                            setSyncMessage('')
                            try {
                              const result = await api.syncDepartments()
                              if (!result.available) {
                                const why =
                                  result.reason === 'sso_disabled' || result.reason === 'not_configured'
                                    ? 'SSO is not configured for this workspace. Add departments manually.'
                                    : result.reason === 'not_entra'
                                      ? 'This SSO issuer is not Microsoft Entra. Add departments manually.'
                                      : 'Entra could not be read. Check the app can read users, or add departments manually.'
                                setSyncMessage(why)
                              } else {
                                setSyncMessage(
                                  result.added === 0
                                    ? 'No new departments. Existing names were left as they are.'
                                    : `Added ${result.added} department${result.added === 1 ? '' : 's'} from Entra.`,
                                )
                                onOk('Department sync finished')
                                await load()
                              }
                            } catch (e) {
                              onError(String(e))
                            } finally {
                              setBusy(false)
                            }
                          })()
                        }}
                      >
                        Sync from Entra
                      </Button>
                    </FieldActionRow>
                  )}
                  <Button
                    variant={showInactive ? 'contained' : 'outlined'}
                    disabled={busy}
                    onClick={() => setShowInactive((v) => !v)}
                    sx={{ mt: canEdit ? 1 : 1.25 }}
                  >
                    {showInactive ? 'Hide Inactive' : 'Show Inactive'}
                  </Button>
                  {syncMessage && (
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>{syncMessage}</Typography>
                  )}
                </Box>
                <Box sx={{ flex: 1, overflow: 'auto' }}>
                  {tree.length === 0 ? (
                    <Box sx={{ p: 3 }}>
                      <Typography color="text.secondary">
                        No units yet. Add a {levelLabel(1)} to tag risks, controls, and evidence.
                      </Typography>
                    </Box>
                  ) : (
                    tree.map((u) => (
                      <UnitRow
                        key={u.id}
                        unit={u}
                        depth={0}
                        levelLabel={levelLabel}
                        canEdit={canEdit}
                        onAddChild={(parent) => openCreate(parent)}
                        onEdit={openEdit}
                        onDeactivate={(unit) => setConfirm({ action: 'deactivate', unit })}
                        onReactivate={async (unit) => {
                          try {
                            await api.reactivateOrgUnit(unit.id)
                            onOk('Unit reactivated')
                            await load()
                          } catch (e) {
                            onError(String(e))
                          }
                        }}
                        onDelete={(unit) => setConfirm({ action: 'delete', unit })}
                      />
                    ))
                  )}
                </Box>
              </Card>
            </Stack>
          )}

          {orgSection === 'access' && (
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: { xs: '1fr', lg: 'minmax(0, 0.9fr) minmax(0, 1.1fr)' },
                gap: 1.25,
                alignItems: 'start',
              }}
            >
              <Stack spacing={1.25}>
                <SettingsCard title="Sign-in" subtitle="Identity is configured by Super Admin." autoHeight>
                  <SettingsReadonlyValue accent={ssoStatus?.ssoEnabled}>
                    <Box
                      component="span"
                      sx={{
                        width: 8,
                        height: 8,
                        borderRadius: '50%',
                        bgcolor: ssoStatus?.ssoEnabled ? brand.gold : brand.mutedLight,
                        flexShrink: 0,
                      }}
                    />
                    {ssoStatus?.ssoEnabled
                      ? 'Microsoft Entra ID is connected. People sign in with their work account.'
                      : 'SSO is off. People sign in with an Arbor password.'}
                  </SettingsReadonlyValue>
                </SettingsCard>
                <SettingsCard title="Session" subtitle="Idle sign-out for this workspace." autoHeight>
                  <Stack spacing={1.25}>
                    <CompactTextField
                      label="Idle timeout (minutes)"
                      type="number"
                      value={idleTimeoutMinutes}
                      onChange={(e) => {
                        const n = Number(e.target.value)
                        if (Number.isFinite(n)) setIdleTimeoutMinutes(n)
                      }}
                      disabled={!canEdit}
                      inputProps={{ min: 1, max: 480 }}
                      helperText="Default 5. Maximum 480. A warning appears about 60 seconds before sign-out."
                    />
                    {canEdit && (
                      <Button variant="contained" onClick={() => void saveProfile()} disabled={busy} sx={{ alignSelf: 'flex-start' }}>
                        Save Session
                      </Button>
                    )}
                  </Stack>
                </SettingsCard>
              </Stack>
              <SettingsCard
                title="Entra group mappings"
                subtitle="On each SSO sign-in, mapped groups add Arbor roles. Roles assigned by hand stay in place."
                autoHeight
              >
                {canEdit ? (
                  <Stack spacing={1.25}>
                    <FieldActionRow>
                      <CompactTextField
                        label="Entra group name or object id"
                        fullWidth
                        value={newGroupName}
                        onChange={(e) => setNewGroupName(e.target.value)}
                      />
                      <FormSelect
                        label="Arbor role"
                        minWidth={180}
                        value={newGroupRole}
                        onChange={setNewGroupRole}
                        options={SSO_MAPPABLE_ROLES.map((r) => ({ value: r, label: roleLabel(r) }))}
                      />
                      <Button
                        variant="contained"
                        disabled={busy || !newGroupName.trim()}
                        onClick={() => void addSsoMapping()}
                        sx={controlActionSx}
                      >
                        Add
                      </Button>
                    </FieldActionRow>
                    {ssoMappings.length === 0 ? (
                      <Typography variant="body2" color="text.secondary">No mappings yet.</Typography>
                    ) : (
                      <Table size="small">
                        <TableHead>
                          <TableRow>
                            <TableCell>Entra group</TableCell>
                            <TableCell width={160}>Role</TableCell>
                            <TableCell width={90} align="right">Action</TableCell>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {ssoMappings.map((m) => (
                            <TableRow key={m.id}>
                              <TableCell>
                                <Typography variant="body2" sx={{ fontFamily: 'ui-monospace, Menlo, monospace', fontSize: 12 }}>
                                  {m.idpGroupName}
                                </Typography>
                              </TableCell>
                              <TableCell>{roleLabel(m.role)}</TableCell>
                              <TableCell align="right">
                                <Button size="small" color="error" disabled={busy} onClick={() => void removeSsoMapping(m.id)}>
                                  Remove
                                </Button>
                              </TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    )}
                  </Stack>
                ) : (
                  <Typography variant="body2" color="text.secondary">
                    Only an Owner or Admin can change group mappings.
                  </Typography>
                )}
              </SettingsCard>
            </Box>
          )}
          </>
          )}
          </Box>
        </Box>
      )}

      {showNotifications && (
        <SettingsCard
          title="Your notifications"
          subtitle="Full in-app history for your account."
          action={
            <Button size="small" disabled={busy} onClick={() => void markAllNotificationsRead()}>
              Mark All Read
            </Button>
          }
        >
          {notifyHistory.length === 0 ? (
            <Typography variant="body2" color="text.secondary">
              No notifications yet.
            </Typography>
          ) : (
            <>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>When</TableCell>
                    <TableCell>Title</TableCell>
                    <TableCell>Trigger</TableCell>
                    <TableCell width={72} align="right">
                      Status
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {notifyHistory.map((n) => (
                    <TableRow
                      key={n.id}
                      hover
                      sx={{ cursor: n.link ? 'pointer' : 'default' }}
                      onClick={() => void openNotification(n)}
                    >
                      <TableCell sx={{ whiteSpace: 'nowrap', fontSize: 12 }}>
                        {formatTenantDateTime(n.createdAt)}
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" fontWeight={n.unread ? 650 : 500}>
                          {n.title}
                        </Typography>
                        {n.body && (
                          <Typography variant="caption" color="text.secondary" display="block">
                            {n.body}
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        <Typography
                          variant="caption"
                          sx={{ fontFamily: 'ui-monospace, Menlo, monospace' }}
                        >
                          {n.triggerType}
                        </Typography>
                      </TableCell>
                      <TableCell align="right">
                        {n.unread ? (
                          <StatusChip label="Unread" tone="warning" />
                        ) : (
                          <StatusChip label="Read" tone="neutral" />
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              {notifyHistoryPages > 1 && (
                <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mt: 1.5 }}>
                  <Typography variant="caption" color="text.secondary">
                    Page {notifyHistoryPage + 1} of {notifyHistoryPages} ({notifyHistoryTotal} total)
                  </Typography>
                  <Stack direction="row" spacing={1}>
                    <Button
                      size="small"
                      disabled={notifyHistoryPage <= 0}
                      onClick={() => void loadNotifyHistory(notifyHistoryPage - 1)}
                    >
                      Previous
                    </Button>
                    <Button
                      size="small"
                      disabled={notifyHistoryPage + 1 >= notifyHistoryPages}
                      onClick={() => void loadNotifyHistory(notifyHistoryPage + 1)}
                    >
                      Next
                    </Button>
                  </Stack>
                </Stack>
              )}
            </>
          )}
        </SettingsCard>
      )}

      {showNotifications && !canEdit && (
        <SettingsCard title="Notification triggers">
          <Typography variant="body2" color="text.secondary">
            Notification templates are managed by Owner or Admin.
          </Typography>
        </SettingsCard>
      )}

      {showNotifications && canEdit && (
        <SettingsCard
          title="Notification triggers"
          subtitle="Enable/disable templates for triggers already fired by Gap, Risk, Audit, CAPA, SSO."
        >
          <Box sx={{ maxHeight: 360, overflow: 'auto', mx: -0.5 }}>
            {notifyTemplates.length === 0 ? (
              <Typography variant="body2" color="text.secondary">
                No templates loaded yet (restart backend after migration V28).
              </Typography>
            ) : (
              <Table size="small" stickyHeader>
                <TableHead>
                  <TableRow>
                    <TableCell>Trigger</TableCell>
                    <TableCell>Subject</TableCell>
                    <TableCell width={100} align="right">
                      Active
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {notifyTemplates.map((t) => (
                    <TableRow key={t.id} hover>
                      <TableCell>
                        <Typography
                          variant="body2"
                          sx={{ fontFamily: 'ui-monospace, Menlo, monospace', fontSize: 12 }}
                        >
                          {t.triggerType}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" noWrap title={t.subjectTemplate}>
                          {t.subjectTemplate}
                        </Typography>
                      </TableCell>
                      <TableCell align="right">
                        <FormControlLabel
                          control={
                            <Switch
                              size="small"
                              checked={t.active}
                              disabled={busy}
                              onChange={(e) => void toggleTemplate(t.id, e.target.checked)}
                            />
                          }
                          label=""
                          sx={{ mr: 0 }}
                        />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </Box>
        </SettingsCard>
      )}

      <Dialog open={!!unitDialog} onClose={() => setUnitDialog(null)} fullWidth maxWidth="xs">
        <DialogTitle>
          {unitDialog?.mode === 'edit'
            ? 'Rename Unit'
            : unitDialog?.parent
              ? `Add Under ${unitDialog.parent.name}`
              : `Add ${levelLabel(1)}`}
        </DialogTitle>
        <DialogContent>
          <CompactTextField
            label="Name"
            fullWidth
            autoFocus
            maxLength={INPUT_LIMIT.orgName}
            value={unitName}
            onChange={(e) => setUnitName(e.target.value)}
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setUnitDialog(null)}>Cancel</Button>
          <Button variant="contained" disabled={!unitName.trim() || busy} onClick={() => void saveUnit()}>
            Save
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={!!confirm} onClose={() => setConfirm(null)} fullWidth maxWidth="xs">
        <DialogTitle>
          {confirm?.action === 'delete' ? 'Delete unit?' : 'Deactivate unit?'}
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2">
            {confirm?.action === 'delete'
              ? `Permanently delete “${confirm.unit.name}”? Only allowed when there are no child units.`
              : `Deactivate “${confirm?.unit.name}” and all descendants? Reactivate later from Show inactive.`}
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirm(null)}>Cancel</Button>
          <Button
            color={confirm?.action === 'delete' ? 'error' : 'warning'}
            variant="contained"
            disabled={busy}
            onClick={() => void runConfirm()}
          >
            {confirm?.action === 'delete' ? 'Delete' : 'Deactivate'}
          </Button>
        </DialogActions>
      </Dialog>
    </Stack>
  )
}
