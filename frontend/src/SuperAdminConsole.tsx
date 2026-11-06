import { useEffect, useMemo, useRef, useState, type MouseEvent } from 'react'
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  Chip,
  CircularProgress,
  Collapse,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControl,
  FormControlLabel,
  IconButton,
  InputAdornment,
  InputLabel,
  LinearProgress,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
  Select,
  Snackbar,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from '@mui/material'
import ApartmentRoundedIcon from '@mui/icons-material/ApartmentRounded'
import HubRoundedIcon from '@mui/icons-material/HubRounded'
import AddRoundedIcon from '@mui/icons-material/AddRounded'
import VpnKeyRoundedIcon from '@mui/icons-material/VpnKeyRounded'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import MoreVertRoundedIcon from '@mui/icons-material/MoreVertRounded'
import ContentCopyRoundedIcon from '@mui/icons-material/ContentCopyRounded'
import TuneRoundedIcon from '@mui/icons-material/TuneRounded'
import ForwardToInboxRoundedIcon from '@mui/icons-material/ForwardToInboxRounded'
import BlockRoundedIcon from '@mui/icons-material/BlockRounded'
import ViewModuleRoundedIcon from '@mui/icons-material/ViewModuleRounded'
import StorageRoundedIcon from '@mui/icons-material/StorageRounded'
import AccountTreeRoundedIcon from '@mui/icons-material/AccountTreeRounded'
import DownloadRoundedIcon from '@mui/icons-material/DownloadRounded'
import UploadFileRoundedIcon from '@mui/icons-material/UploadFileRounded'
import ExpandMoreRoundedIcon from '@mui/icons-material/ExpandMoreRounded'
import ExpandLessRoundedIcon from '@mui/icons-material/ExpandLessRounded'
import PublishRoundedIcon from '@mui/icons-material/PublishRounded'
import DoneAllRoundedIcon from '@mui/icons-material/DoneAllRounded'
import EventRepeatRoundedIcon from '@mui/icons-material/EventRepeatRounded'
import AppShell, { type NavItem } from './components/AppShell'
import FrameworkViewerDialog from './components/FrameworkViewerDialog'
import StatusChip from './components/StatusChip'
import LicenceTermBadge from './components/license/LicenceTermBadge'
import { KpiCard, KpiCardGrid, CompactTextField, FormSelect, BusyButton, registerTableSx, primaryTableCellSx, compactTableCellSx, mediumTableCellSx, lastTableCellSx, compactTableCellSxWidth } from './components/ui'
import { productErrorMessage } from './uiQuality/productError'
import { setArborTitle } from './uiQuality/pageTitle'
import { formatTenantDate } from './uiQuality/tenantLocale'
import {
  expiryInputValue,
  hasLicenceCalendarDate,
  licenceTermView,
  licenseStatusTone,
  tenantLicenseStatusLabel,
  todayYmd,
  ymdPlusYears,
} from './licenseExpiry'
import {
  api,
  MODULE_CODES,
  MODULE_LABELS,
  ssoEntryUrl,
  ssoRedirectUri,
  type FrameworkBundleImportResult,
  type FrameworkBundleStatus,
  type FrameworkDetail,
  type FrameworkImportReportLine,
  type FrameworkQuestion,
  type FrameworkSummary,
  type FrameworkTenantAssignment,
  type FrameworkType,
  type ModuleLicense,
  type ProvisionResponse,
  type Session,
  type Tenant,
} from './api'
import { brand, semantic } from './theme'
import WorkspaceBootScreen from './components/login/WorkspaceBootScreen'

const NAV: NavItem[] = [
  { id: 'tenants', label: 'Tenants', icon: <ApartmentRoundedIcon fontSize="small" /> },
  { id: 'modules', label: 'Modules', icon: <VpnKeyRoundedIcon fontSize="small" /> },
  { id: 'frameworks', label: 'Frameworks', icon: <AccountTreeRoundedIcon fontSize="small" /> },
  { id: 'sso', label: 'SSO / AD', icon: <HubRoundedIcon fontSize="small" /> },
]

function frameworkStatusTone(status: string): 'success' | 'warning' | 'info' | 'neutral' {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'VALIDATED') return 'info'
  if (status === 'DRAFT') return 'warning'
  return 'neutral'
}

const MONO = 'ui-monospace, SFMono-Regular, Menlo, monospace'

const PROVISION_STEPS = [
  'Creating the tenant database',
  'Running database setup',
  'Creating the first administrator',
  'Finishing workspace setup',
]

/** Legacy GRC Checklist / Arbor sample import UI: kept in code, hidden; V2 uses folder packs + single Excel upload. */
const SHOW_LEGACY_CHECKLIST_IMPORT_UI = false

const goldModuleChipSx = {
  fontFamily: MONO,
  fontSize: '0.7rem',
  bgcolor: brand.goldLight,
  color: brand.goldDark,
  borderColor: brand.goldMid,
} as const

const goldRevokePanelSx = {
  p: 1.5,
  borderRadius: 1,
  border: `1px solid ${brand.goldMid}`,
  bgcolor: brand.goldLight,
} as const

function SsoStep({
  n,
  label,
  active,
  done,
}: {
  n: number
  label: string
  active: boolean
  done: boolean
}) {
  return (
    <Stack direction="row" spacing={1} alignItems="center" sx={{ minWidth: 0 }}>
      <Box
        sx={{
          width: 22,
          height: 22,
          borderRadius: '50%',
          display: 'grid',
          placeItems: 'center',
          fontSize: '0.7rem',
          fontWeight: 700,
          flexShrink: 0,
          bgcolor: done || active ? brand.gold : brand.canvas,
          color: done || active ? '#fff' : brand.muted,
          border: done || active ? 'none' : `1px solid ${brand.hairlineStrong}`,
        }}
      >
        {done ? '✓' : n}
      </Box>
      <Typography
        variant="caption"
        sx={{
          fontWeight: active ? 650 : 500,
          color: active ? 'text.primary' : 'text.secondary',
          whiteSpace: 'nowrap',
        }}
      >
        {label}
      </Typography>
    </Stack>
  )
}

function ImportReportLineCard({ line }: { line: FrameworkImportReportLine }) {
  const [errorsOpen, setErrorsOpen] = useState(false)
  const [infoOpen, setInfoOpen] = useState(false)
  const label = line.frameworkCode
    ? `${line.frameworkCode}${line.version ? ` ${line.version}` : ''}`
    : line.sourceFrameworkCode ?? line.sheetName
  const routing = line.routing

  return (
    <Card variant="outlined" sx={{ borderColor: brand.hairlineStrong }}>
      <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
        <Stack spacing={1}>
          <Stack direction="row" alignItems="center" spacing={1} flexWrap="wrap" useFlexGap>
            <Typography variant="subtitle2" sx={{ fontFamily: MONO }}>
              {label}
            </Typography>
            <StatusChip label={line.status} tone={line.errorCount > 0 ? 'warning' : 'info'} />
            {line.errorCount > 0 && (
              <Chip size="small" label={`${line.errorCount} errors`} color="warning" variant="outlined" />
            )}
          </Stack>
          {line.message && (
            <Typography variant="caption" color="text.secondary">
              {line.message}
            </Typography>
          )}
          <Typography variant="caption" color="text.secondary" sx={{ fontFamily: MONO }}>
            {line.contentRows} content rows · {line.nodesCreated} nodes · {line.leafNodes} routed leaves ·{' '}
            {line.questionsImported} questions
          </Typography>
          {routing && (
            <Typography variant="caption" color="text.secondary">
              Routing: SoA {routing.inSoa} · Gap {routing.inGapAssessment} · Audit{' '}
              {routing.inAuditChecklist} · Risk {routing.inRiskRegister}
            </Typography>
          )}
          {line.merge && (
            <Typography variant="caption" color="text.secondary">
              Merge: {line.merge.refRows} refs · {line.merge.exactMatches} exact ·{' '}
              {line.merge.ancestorAttachments} attached · {line.merge.unmatchedRefs} dropped ·{' '}
              {line.merge.questionsReplaced} replaced · {line.merge.questionsKept} kept ·{' '}
              {line.merge.attributesWritten} attributes
            </Typography>
          )}
          {line.errors.length > 0 && (
            <Box>
              <Button
                size="small"
                variant="text"
                endIcon={errorsOpen ? <ExpandLessRoundedIcon /> : <ExpandMoreRoundedIcon />}
                onClick={() => setErrorsOpen((v) => !v)}
                sx={{ px: 0, minWidth: 0 }}
              >
                Row Errors ({line.errors.length})
              </Button>
              <Collapse in={errorsOpen}>
                <Stack spacing={0.5} sx={{ mt: 0.5, pl: 0.5 }}>
                  {line.errors.map((err, i) => (
                    <Typography key={i} variant="caption" color="error.main" display="block">
                      Row {err.row} · {err.field}: {err.message}
                    </Typography>
                  ))}
                </Stack>
              </Collapse>
            </Box>
          )}
          {line.info.length > 0 && (
            <Box>
              <Button
                size="small"
                variant="text"
                endIcon={infoOpen ? <ExpandLessRoundedIcon /> : <ExpandMoreRoundedIcon />}
                onClick={() => setInfoOpen((v) => !v)}
                sx={{ px: 0, minWidth: 0 }}
              >
                Info ({line.info.length})
              </Button>
              <Collapse in={infoOpen}>
                <Stack spacing={0.5} sx={{ mt: 0.5, pl: 0.5 }}>
                  {line.info.map((msg, i) => (
                    <Typography key={i} variant="caption" color="text.secondary" display="block">
                      {msg}
                    </Typography>
                  ))}
                </Stack>
              </Collapse>
            </Box>
          )}
        </Stack>
      </CardContent>
    </Card>
  )
}

export default function SuperAdminConsole({ session, onLogout }: { session: Session; onLogout: () => void }) {
  const [active, setActive] = useState('tenants')
  const [tenants, setTenants] = useState<Tenant[]>([])
  const [workspaceReady, setWorkspaceReady] = useState(false)
  const [query, setQuery] = useState('')
  const [newId, setNewId] = useState('')
  const [newName, setNewName] = useState('')
  const [ownerEmail, setOwnerEmail] = useState('')
  const [ownerName, setOwnerName] = useState('')
  const [provisionSso, setProvisionSso] = useState(false)
  const [provisionIssuer, setProvisionIssuer] = useState('')
  const [provisionClientId, setProvisionClientId] = useState('')
  const [provisionClientSecret, setProvisionClientSecret] = useState('')
  const [provisionOpen, setProvisionOpen] = useState(false)
  const [provisioning, setProvisioning] = useState(false)
  const [provisionWaitIdx, setProvisionWaitIdx] = useState(0)
  const [provisionElapsedSec, setProvisionElapsedSec] = useState(0)
  const [provisioned, setProvisioned] = useState<ProvisionResponse | null>(null)
  const [provisionPasswordVisible, setProvisionPasswordVisible] = useState(false)
  const [error, setErrorRaw] = useState('')
  const setError = (msg: unknown) => setErrorRaw(productErrorMessage(msg))
  const [ok, setOk] = useState('')
  useEffect(() => {
    setArborTitle('Super Admin')
  }, [])
  useEffect(() => {
    if (!provisioning) {
      setProvisionWaitIdx(0)
      setProvisionElapsedSec(0)
      return
    }
    const stepTimer = window.setInterval(() => {
      setProvisionWaitIdx((i) => Math.min(i + 1, PROVISION_STEPS.length - 1))
    }, 8000)
    const clock = window.setInterval(() => {
      setProvisionElapsedSec((s) => s + 1)
    }, 1000)
    return () => {
      window.clearInterval(stepTimer)
      window.clearInterval(clock)
    }
  }, [provisioning])

  const [idpTenant, setIdpTenant] = useState('')
  const [ssoEnabled, setSsoEnabled] = useState(false)
  const [oidcIssuer, setOidcIssuer] = useState('')
  const [oidcClientId, setOidcClientId] = useState('')
  const [oidcClientSecret, setOidcClientSecret] = useState('')
  const [jitEnabled, setJitEnabled] = useState(false)
  const [jitDefaultRole, setJitDefaultRole] = useState<'BLOCK' | 'CONTRIBUTOR'>('BLOCK')

  const [licenseTenant, setLicenseTenant] = useState('')
  const [licenseTenantMenu, setLicenseTenantMenu] = useState<null | HTMLElement>(null)
  const [licenses, setLicenses] = useState<ModuleLicense[]>([])
  const [licenseBusy, setLicenseBusy] = useState(false)
  const [moduleConfirm, setModuleConfirm] = useState<null | {
    code: string
    enabled: boolean
    frameworkIds: number[]
    expiresOn: string
  }>(null)
  const [licenseAllConfirm, setLicenseAllConfirm] = useState(false)
  const [licenseAllExpiry, setLicenseAllExpiry] = useState(ymdPlusYears(1))
  const [revokeAllConfirm, setRevokeAllConfirm] = useState(false)
  const [extendDialog, setExtendDialog] = useState<null | { codes: string[] | 'all'; expiresOn: string }>(null)
  const [selectedModuleCodes, setSelectedModuleCodes] = useState<string[]>([])
  /** Inline framework picks while a module row checkbox is selected (before License selected). */
  const [selectionFrameworkIdsByCode, setSelectionFrameworkIdsByCode] = useState<
    Record<string, number[]>
  >({})
  const [moduleBulkConfirm, setModuleBulkConfirm] = useState<null | {
    mode: 'license' | 'revoke'
    codes: string[]
    /** When licensing: published framework ids entitled per module code. */
    frameworkIdsByCode?: Record<string, number[]>
    expiresOn?: string
  }>(null)
  const [fwLicenseExpiry, setFwLicenseExpiry] = useState(ymdPlusYears(1))
  const [ssoSaveConfirm, setSsoSaveConfirm] = useState(false)
  const [idpBusy, setIdpBusy] = useState(false)

  const [fwTypes, setFwTypes] = useState<FrameworkType[]>([])
  const [fwLibrary, setFwLibrary] = useState<FrameworkSummary[]>([])
  const [fwDetail, setFwDetail] = useState<FrameworkDetail | null>(null)
  const [fwAssignments, setFwAssignments] = useState<FrameworkTenantAssignment[]>([])
  const [fwQuestions, setFwQuestions] = useState<FrameworkQuestion[]>([])
  const [qImportFile, setQImportFile] = useState<File | null>(null)
  const [fwBusy, setFwBusy] = useState(false)
  const [importOpen, setImportOpen] = useState(false)
  const [bundleOpen, setBundleOpen] = useState(false)
  const [bundleStatus, setBundleStatus] = useState<FrameworkBundleStatus | null>(null)
  const [bundleBusy, setBundleBusy] = useState(false)
  const [bundleElapsedSec, setBundleElapsedSec] = useState(0)
  const [bundleResult, setBundleResult] = useState<FrameworkBundleImportResult | null>(null)
  const [bundleFinished, setBundleFinished] = useState<'idle' | 'dry-run' | 'imported'>('idle')
  const bundleClickLock = useRef(false)
  const [importType, setImportType] = useState('')
  const [importVersion, setImportVersion] = useState('')
  const [importFile, setImportFile] = useState<File | null>(null)
  const [importPreview, setImportPreview] = useState<FrameworkImportReportLine[] | null>(null)
  const [importPreviewBusy, setImportPreviewBusy] = useState(false)
  useEffect(() => {
    if (!bundleBusy) {
      setBundleElapsedSec(0)
      return
    }
    const clock = window.setInterval(() => setBundleElapsedSec((s) => s + 1), 1000)
    return () => window.clearInterval(clock)
  }, [bundleBusy])
  const [fwConfirm, setFwConfirm] = useState<null | {
    action: 'validate' | 'publish' | 'delete'
    id: number
    label: string
  }>(null)
  const [fwBulkPublish, setFwBulkPublish] = useState<FrameworkSummary[] | null>(null)
  /** Step 1: review activate/deactivate for a tenant. */
  const [fwAssignDialog, setFwAssignDialog] = useState<null | {
    tenantId: string
    tenantName: string
    enable: boolean
    moduleLicensed: boolean
  }>(null)
  /** When activating and module is off: must explicitly opt in to license it. */
  const [fwLicenseModule, setFwLicenseModule] = useState(false)
  /** Step 2: final confirmation before API call. */
  const [fwAssignConfirm, setFwAssignConfirm] = useState<null | {
    tenantId: string
    tenantName: string
    enable: boolean
    moduleLicensed: boolean
    licenseModule: boolean
  }>(null)

  const [menuEl, setMenuEl] = useState<null | HTMLElement>(null)
  const [menuTenant, setMenuTenant] = useState<Tenant | null>(null)
  const [activationBusy, setActivationBusy] = useState(false)

  const load = () =>
    api
      .listTenants()
      .then(setTenants)
      .catch((e) => setError(String(e)))
      .finally(() => setWorkspaceReady(true))
  const loadFrameworks = () => {
    Promise.all([api.listFrameworkTypes(), api.listAdminFrameworks()])
      .then(([types, library]) => {
        setFwTypes(types)
        setFwLibrary(library)
      })
      .catch((e) => setError(String(e)))
  }
  useEffect(() => { load() }, [])
  useEffect(() => {
    if (active === 'tenants') load()
  }, [active])
  useEffect(() => {
    if (active === 'frameworks' || active === 'modules') loadFrameworks()
  }, [active])

  const stats = useMemo(() => ({
    total: tenants.length,
    active: tenants.filter((t) => t.status === 'ACTIVE').length,
    sso: tenants.filter((t) => t.ssoEnabled).length,
    expired: tenants.filter((t) => t.license?.writeLocked || t.license?.status === 'EXPIRED').length,
  }), [tenants])

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return tenants
    return tenants.filter((t) => {
      if (t.tenantId.toLowerCase().includes(q) || t.name.toLowerCase().includes(q)) return true
      return (t.license?.licensedCodes ?? []).some((code) => code.toLowerCase().includes(q))
    })
  }, [tenants, query])

  /** Platform module catalog: fixed type→module map + imported library versions + tenant licence. */
  const moduleCatalog = useMemo(() => {
    return MODULE_CODES.map((code) => {
      const frameworkTypes = fwTypes.filter((t) => t.moduleCode === code)
      const imported = fwLibrary
        .filter((f) => f.moduleCode === code)
        .sort((a, b) => a.code.localeCompare(b.code) || b.version.localeCompare(a.version))
      const license = licenses.find((m) => m.code === code)
      return {
        code,
        label: MODULE_LABELS[code] ?? code,
        frameworkTypes,
        imported,
        license,
        publishedCount: imported.filter((f) => f.status === 'PUBLISHED').length,
      }
    })
  }, [fwTypes, fwLibrary, licenses])

  const validatedFrameworks = useMemo(
    () => fwLibrary.filter((f) => f.status === 'VALIDATED'),
    [fwLibrary],
  )

  const publishedFrameworkCount = useMemo(
    () => fwLibrary.filter((f) => f.status === 'PUBLISHED').length,
    [fwLibrary],
  )

  const draftFrameworkCount = useMemo(
    () => fwLibrary.filter((f) => f.status === 'DRAFT').length,
    [fwLibrary],
  )

  const unlicensedModuleCount = useMemo(
    () => licenses.filter((m) => !m.enabled).length,
    [licenses],
  )

  const licensedModuleCount = useMemo(
    () => licenses.filter((m) => m.enabled).length,
    [licenses],
  )

  const expiredLicenseCount = useMemo(
    () => licenses.filter((m) => m.enabled && licenceTermView(m).kind === 'expired').length,
    [licenses],
  )

  const licensedWithDateCount = useMemo(
    () => licenses.filter((m) => m.enabled && hasLicenceCalendarDate(m)).length,
    [licenses],
  )

  const selectedToLicenseCodes = useMemo(
    () =>
      selectedModuleCodes.filter(
        (code) => !licenses.find((m) => m.code === code)?.enabled,
      ),
    [selectedModuleCodes, licenses],
  )

  const selectedToRevokeCodes = useMemo(
    () =>
      selectedModuleCodes.filter(
        (code) => Boolean(licenses.find((m) => m.code === code)?.enabled),
      ),
    [selectedModuleCodes, licenses],
  )

  /** Toolbar Select all is complete only when every module and every published framework is ticked. */
  const selectAllModulesAndFrameworks = useMemo(() => {
    if (selectedModuleCodes.length !== MODULE_CODES.length) return false
    return MODULE_CODES.every((code) => {
      const ids = fwLibrary
        .filter((f) => f.moduleCode === code && f.status === 'PUBLISHED')
        .map((f) => f.id)
      const picked = new Set(selectionFrameworkIdsByCode[code] ?? [])
      return ids.every((id) => picked.has(id))
    })
  }, [fwLibrary, selectedModuleCodes, selectionFrameworkIdsByCode])

  const selectAllPartial =
    !selectAllModulesAndFrameworks
    && (
      selectedModuleCodes.length > 0
      || Object.values(selectionFrameworkIdsByCode).some((ids) => ids.length > 0)
    )

  const provision = async () => {
    setProvisioning(true)
    try {
      const res = await api.provisionTenant(newId.trim(), newName.trim(), {
        ownerEmail: ownerEmail.trim(),
        ownerFullName: ownerName.trim(),
        ssoEnabled: provisionSso,
        oidcIssuer: provisionIssuer.trim(),
        oidcClientId: provisionClientId.trim(),
        oidcClientSecret: provisionClientSecret,
      })
      setProvisioned(res)
      setProvisionPasswordVisible(false)
      setNewId('')
      setNewName('')
      setOwnerEmail('')
      setOwnerName('')
      setProvisionSso(false)
      setProvisionIssuer('')
      setProvisionClientId('')
      setProvisionClientSecret('')
      load()
    } catch (e) {
      setError(String(e))
    } finally {
      setProvisioning(false)
    }
  }

  const selectIdpTenant = (tenantId: string) => {
    setIdpTenant(tenantId)
    const t = tenants.find((x) => x.tenantId === tenantId)
    setSsoEnabled(t?.ssoEnabled ?? false)
    setOidcIssuer(t?.oidcIssuer ?? '')
    setOidcClientId(t?.oidcClientId ?? '')
    setOidcClientSecret('')
    setJitEnabled(t?.jitEnabled ?? false)
    setJitDefaultRole(t?.jitDefaultRole === 'CONTRIBUTOR' ? 'CONTRIBUTOR' : 'BLOCK')
  }

  const requestSaveIdp = () => {
    if (!idpTenant) return
    setSsoSaveConfirm(true)
  }

  const runSaveIdp = async () => {
    setIdpBusy(true)
    try {
      await api.configureTenantIdp(idpTenant, {
        ssoEnabled,
        oidcIssuer: oidcIssuer.trim(),
        oidcClientId: oidcClientId.trim(),
        oidcClientSecret,
        jitEnabled,
        jitDefaultRole,
      })
      setOk(`SSO settings saved for ${idpTenant}`)
      setOidcClientSecret('')
      setSsoSaveConfirm(false)
      load()
    } catch (e) {
      setError(String(e))
    } finally {
      setIdpBusy(false)
    }
  }

  const openRowMenu = (e: MouseEvent<HTMLElement>, t: Tenant) => {
    setMenuEl(e.currentTarget)
    setMenuTenant(t)
  }

  const closeRowMenu = () => {
    setMenuEl(null)
    setMenuTenant(null)
  }

  const copyEntryUrl = async (tenantId?: string) => {
    const id = tenantId ?? menuTenant?.tenantId
    if (!id) return
    try {
      await navigator.clipboard.writeText(ssoEntryUrl(id))
      setOk('SSO entry URL copied')
    } catch {
      setError('Could not copy to clipboard')
    }
    closeRowMenu()
  }

  const copyRedirectUri = async () => {
    if (!idpTenant) return
    try {
      await navigator.clipboard.writeText(ssoRedirectUri(idpTenant))
      setOk('Redirect URI copied')
    } catch {
      setError('Could not copy to clipboard')
    }
  }

  const goConfigureSso = () => {
    if (!menuTenant) return
    selectIdpTenant(menuTenant.tenantId)
    setActive('sso')
    closeRowMenu()
  }

  const openTenantModules = (tenantId: string) => {
    selectLicenseTenant(tenantId)
    setActive('modules')
  }

  const resendOwnerActivation = async () => {
    if (!menuTenant || activationBusy) return
    const tid = menuTenant.tenantId
    setActivationBusy(true)
    try {
      const res = await api.resendOwnerActivation(tid)
      setOk(`Activation email sent to ${res.sentTo.join(', ')}. It is valid for 24 hours.`)
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setActivationBusy(false)
      closeRowMenu()
    }
  }

  const goManageModules = () => {
    if (!menuTenant) return
    const tid = menuTenant.tenantId
    closeRowMenu()
    openTenantModules(tid)
  }

  const loadLicenses = async (tenantId: string) => {
    if (!tenantId) {
      setLicenses([])
      return
    }
    setLicenseBusy(true)
    try {
      const res = await api.listTenantModules(tenantId)
      setLicenses(res.modules)
    } catch (e) {
      setError(String(e))
    } finally {
      setLicenseBusy(false)
    }
  }

  const selectLicenseTenant = (tenantId: string) => {
    setLicenseTenant(tenantId)
    setSelectedModuleCodes([])
    setSelectionFrameworkIdsByCode({})
    loadLicenses(tenantId)
  }

  const publishedFrameworksForModule = (code: string) =>
    fwLibrary.filter((f) => f.moduleCode === code && f.status === 'PUBLISHED')

  const defaultFrameworkIdsForModule = (code: string): number[] => {
    const latestByCode = new Map<string, number>()
    for (const f of publishedFrameworksForModule(code)) {
      if (!latestByCode.has(f.code)) latestByCode.set(f.code, f.id)
    }
    return [...latestByCode.values()]
  }

  const toggleModuleSelection = (code: string) => {
    setSelectedModuleCodes((prev) => {
      if (prev.includes(code)) {
        setSelectionFrameworkIdsByCode((fw) => {
          const next = { ...fw }
          delete next[code]
          return next
        })
        return prev.filter((c) => c !== code)
      }
      setSelectionFrameworkIdsByCode((fw) => ({
        ...fw,
        [code]: publishedFrameworksForModule(code).map((f) => f.id),
      }))
      return [...prev, code]
    })
  }

  const toggleFrameworkInSelection = (moduleCode: string, frameworkId: number, checked: boolean) => {
    const id = Number(frameworkId)
    setSelectionFrameworkIdsByCode((prev) => {
      const cur = (prev[moduleCode] ?? []).map(Number)
      const next = checked ? [...cur.filter((x) => x !== id), id] : cur.filter((x) => x !== id)
      return { ...prev, [moduleCode]: next }
    })
  }

  const selectUnlicensedModules = () => {
    const codes = licenses.filter((m) => !m.enabled).map((m) => m.code)
    setSelectedModuleCodes(codes)
    const next: Record<string, number[]> = {}
    for (const code of codes) next[code] = publishedFrameworksForModule(code).map((f) => f.id)
    setSelectionFrameworkIdsByCode(next)
  }

  const selectLicensedModules = () => {
    const codes = licenses.filter((m) => m.enabled).map((m) => m.code)
    setSelectedModuleCodes(codes)
    const next: Record<string, number[]> = {}
    for (const code of codes) next[code] = publishedFrameworksForModule(code).map((f) => f.id)
    setSelectionFrameworkIdsByCode(next)
  }

  const askModuleConfirm = (code: string, enabled: boolean) => {
    if (!licenseTenant) return
    const existing = licenses.find((m) => m.code === code)?.entitledFrameworkIds ?? []
    const fromSelection = selectionFrameworkIdsByCode[code]
    setModuleConfirm({
      code,
      enabled,
      frameworkIds: enabled
        ? (existing.length > 0
            ? [...existing]
            : fromSelection && fromSelection.length > 0
              ? [...fromSelection]
              : [])
        : [],
      expiresOn: expiryInputValue(licenses.find((m) => m.code === code)?.expiresOn
        ?? licenses.find((m) => m.code === code)?.expiresAt),
    })
  }

  /** License selected uses inline framework checkboxes (no second picker for a single module). */
  const openLicenseSelected = () => {
    const codes = selectedToLicenseCodes
    if (codes.length === 0) return
    for (const code of codes) {
      const published = publishedFrameworksForModule(code)
      const picked = selectionFrameworkIdsByCode[code] ?? []
      if (published.length > 0 && picked.length === 0) {
        setError(`Select at least one published framework under ${code}`)
        return
      }
    }
    const frameworkIdsByCode: Record<string, number[]> = {}
    for (const code of codes) {
      frameworkIdsByCode[code] = [...(selectionFrameworkIdsByCode[code] ?? [])]
    }
    setModuleBulkConfirm({ mode: 'license', codes, frameworkIdsByCode, expiresOn: ymdPlusYears(1) })
  }

  const runModuleConfirm = async () => {
    if (!moduleConfirm || !licenseTenant) return
    const { code, enabled, frameworkIds, expiresOn } = moduleConfirm
    const published = fwLibrary.filter((f) => f.moduleCode === code && f.status === 'PUBLISHED')
    if (enabled && published.length > 0 && frameworkIds.length === 0) {
      setError(`Select at least one published framework under ${code}`)
      return
    }
    if (enabled && !expiresOn) {
      setError('Select a licence expiry date')
      return
    }
    setLicenseBusy(true)
    try {
      const res = await api.updateTenantModules(licenseTenant, [
        {
          code,
          enabled,
          frameworkIds: enabled ? (published.length === 0 ? null : frameworkIds) : undefined,
          expiresOn: enabled ? expiresOn : undefined,
        },
      ])
      setLicenses(res.modules)
      setSelectedModuleCodes((prev) => prev.filter((c) => c !== code))
      setSelectionFrameworkIdsByCode((prev) => {
        const next = { ...prev }
        delete next[code]
        return next
      })
      setOk(
        enabled
          ? `Module ${code} licensed for ${licenseTenant}` +
            (frameworkIds.length ? ` · ${frameworkIds.length} framework(s)` : '')
          : `Module ${code} unlicensed for ${licenseTenant}`,
      )
      setModuleConfirm(null)
    } catch (e) {
      setError(String(e))
    } finally {
      setLicenseBusy(false)
    }
  }

  const runLicenseAll = async () => {
    if (!licenseTenant) return
    if (!licenseAllExpiry) {
      setError('Select a licence expiry date for all modules')
      return
    }
    setLicenseBusy(true)
    try {
      const res = await api.updateTenantModules(
        licenseTenant,
        MODULE_CODES.map((code) => {
          const ids = defaultFrameworkIdsForModule(code)
          return {
            code,
            enabled: true,
            frameworkIds: ids.length > 0 ? ids : null,
          }
        }),
        licenseAllExpiry,
      )
      setLicenses(res.modules)
      setSelectedModuleCodes([])
      setSelectionFrameworkIdsByCode({})
      setOk(`All ${MODULE_CODES.length} modules licensed for ${licenseTenant} (full published framework pack)`)
      setLicenseAllConfirm(false)
    } catch (e) {
      setError(String(e))
    } finally {
      setLicenseBusy(false)
    }
  }

  const runRevokeAll = async () => {
    if (!licenseTenant) return
    setLicenseBusy(true)
    try {
      const res = await api.updateTenantModules(
        licenseTenant,
        MODULE_CODES.map((code) => ({ code, enabled: false })),
      )
      setLicenses(res.modules)
      setSelectedModuleCodes([])
      setSelectionFrameworkIdsByCode({})
      setOk(`All module licences revoked for ${licenseTenant}`)
      setRevokeAllConfirm(false)
    } catch (e) {
      setError(String(e))
    } finally {
      setLicenseBusy(false)
    }
  }

  const runModuleBulkConfirm = async () => {
    if (!moduleBulkConfirm || !licenseTenant) return
    const { mode, codes, frameworkIdsByCode, expiresOn } = moduleBulkConfirm
    if (codes.length === 0) return
    const enabled = mode === 'license'
    if (enabled) {
      if (!expiresOn) {
        setError('Select a licence expiry date')
        return
      }
      for (const code of codes) {
        const published = fwLibrary.filter((f) => f.moduleCode === code && f.status === 'PUBLISHED')
        const picked = frameworkIdsByCode?.[code] ?? []
        if (published.length > 0 && picked.length === 0) {
          setError(`Select at least one published framework under ${code}`)
          return
        }
      }
    }
    setLicenseBusy(true)
    try {
      const res = await api.updateTenantModules(
        licenseTenant,
        codes.map((code) => {
          const published = fwLibrary.filter((f) => f.moduleCode === code && f.status === 'PUBLISHED')
          const picked = frameworkIdsByCode?.[code] ?? []
          return {
            code,
            enabled,
            frameworkIds: enabled ? (published.length === 0 ? null : picked) : undefined,
            expiresOn: enabled ? expiresOn : undefined,
          }
        }),
      )
      setLicenses(res.modules)
      setSelectedModuleCodes((prev) => prev.filter((code) => !codes.includes(code)))
      setSelectionFrameworkIdsByCode((prev) => {
        const next = { ...prev }
        for (const code of codes) delete next[code]
        return next
      })
      setOk(
        enabled
          ? codes.length === 1
            ? `Module ${codes[0]} licensed for ${licenseTenant}`
            : `${codes.length} modules licensed for ${licenseTenant}`
          : codes.length === 1
            ? `Module ${codes[0]} licence revoked for ${licenseTenant}`
            : `${codes.length} module licences revoked for ${licenseTenant}`,
      )
      setModuleBulkConfirm(null)
    } catch (e) {
      setError(String(e))
    } finally {
      setLicenseBusy(false)
    }
  }

  const runExtendExpiry = async () => {
    if (!extendDialog || !licenseTenant) return
    if (!extendDialog.expiresOn) {
      setError('Select a licence expiry date')
      return
    }
    if (extendDialog.expiresOn < todayYmd()) {
      setError('Licence expiry date must be today or later')
      return
    }
    setLicenseBusy(true)
    try {
      const res = await api.extendTenantModuleExpiry(
        licenseTenant,
        extendDialog.expiresOn,
        extendDialog.codes === 'all' ? undefined : extendDialog.codes,
      )
      setLicenses(res.modules)
      setOk(
        extendDialog.codes === 'all'
          ? `Expiry set for all licensed modules on ${licenseTenant}`
          : extendDialog.codes.length === 1
            ? `Expiry updated for ${extendDialog.codes[0]}`
            : `Expiry updated for ${extendDialog.codes.length} modules`,
      )
      setExtendDialog(null)
    } catch (e) {
      setError(String(e))
    } finally {
      setLicenseBusy(false)
    }
  }

  const openImport = (type?: FrameworkType) => {
    setImportType(type?.code ?? '')
    setImportVersion(type?.defaultVersion ?? '')
    setImportFile(null)
    setImportPreview(null)
    setImportOpen(true)
  }

  const openBundleImport = async () => {
    bundleClickLock.current = false
    setBundleResult(null)
    setBundleFinished('idle')
    setBundleOpen(true)
    try {
      const st = await api.getFrameworkBundleStatus()
      setBundleStatus(st)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not load shipped framework packs')
    }
  }

  const runBundleDryRun = async () => {
    if (bundleClickLock.current || bundleFinished !== 'idle') return
    bundleClickLock.current = true
    setBundleFinished('dry-run')
    setBundleBusy(true)
    setBundleResult(null)
    try {
      const result = await api.importFrameworkBundle({
        scope: 'ALL',
        dryRun: true,
      })
      setBundleResult(result)
      setOk(`Dry-run: ${result.importedFileCount} file(s) OK path, ${result.errorFileCount} error(s)`)
      bundleClickLock.current = false
    } catch (e) {
      setBundleFinished('idle')
      bundleClickLock.current = false
      setError(e instanceof Error ? e.message : 'Dry-run failed')
    } finally {
      setBundleBusy(false)
    }
  }

  const runBundleImport = async () => {
    if (bundleClickLock.current || bundleFinished === 'imported') return
    bundleClickLock.current = true
    const previous = bundleFinished
    setBundleFinished('imported')
    setBundleBusy(true)
    setBundleResult(null)
    try {
      const result = await api.importFrameworkBundle({
        scope: 'ALL',
        dryRun: false,
      })
      setBundleResult(result)
      setOk(
        `Imported ${result.importedFileCount} file(s)`
          + (result.errorFileCount ? `, ${result.errorFileCount} error(s)` : ''),
      )
      try {
        await loadFrameworks()
      } catch {
        /* Catalog write already succeeded; keep Import all locked. */
      }
    } catch (e) {
      setBundleFinished(previous)
      bundleClickLock.current = false
      setError(e instanceof Error ? e.message : 'Import failed')
    } finally {
      setBundleBusy(false)
    }
  }

  const runImportPreview = async (
    file: File,
    opts?: { typeCode?: string; version?: string },
  ) => {
    setImportPreviewBusy(true)
    setImportPreview(null)
    const typeCode = opts?.typeCode !== undefined ? opts.typeCode : importType || undefined
    const ver =
      opts?.version !== undefined
        ? opts.version
        : importVersion.trim() || undefined
    try {
      const result = await api.importFramework(file, {
        typeCode,
        version: ver,
        dryRun: true,
      })
      setImportPreview(result.report ?? [])
    } catch (e) {
      setError(String(e))
      setImportPreview(null)
    } finally {
      setImportPreviewBusy(false)
    }
  }

  const handleImportFileChange = (file: File | null) => {
    setImportFile(file)
    setImportPreview(null)
    if (file) void runImportPreview(file)
  }

  const runImport = async () => {
    if (!importFile) return
    // Legacy checklist/Arbor Controls: type + version when selected. Pack Excel auto-detect ignores both.
    if (importType && !importVersion.trim()) return
    setFwBusy(true)
    try {
      const result = await api.importFramework(importFile, {
        typeCode: importType || undefined,
        version: importVersion.trim() || undefined,
        dryRun: false,
      })
      const labels = result.imported.map((d) => `${d.framework.code} ${d.framework.version}`).join(', ')
      setOk(`Imported as DRAFT: ${labels}`)
      setImportOpen(false)
      setImportPreview(null)
      if (result.imported.length === 1) {
        const id = result.imported[0].framework.id
        setFwDetail(result.imported[0])
        await loadFwQuestions(id)
        await loadFwAssignments(id, result.imported[0].framework.status)
      } else {
        clearFrameworkSelection()
      }
      loadFrameworks()
    } catch (e) {
      setError(String(e))
    } finally {
      setFwBusy(false)
    }
  }

  const loadFwQuestions = async (id: number) => {
    try {
      setFwQuestions(await api.listFrameworkQuestions(id))
    } catch {
      setFwQuestions([])
    }
  }

  const loadFwAssignments = async (id: number, status?: string) => {
    if (status && status !== 'PUBLISHED') {
      setFwAssignments([])
      return
    }
    try {
      setFwAssignments(await api.listFrameworkTenantAssignments(id))
    } catch (e) {
      setFwAssignments([])
      if (!status || status === 'PUBLISHED') setError(String(e))
    }
  }

  const viewFramework = async (id: number) => {
    setFwBusy(true)
    try {
      const detail = await api.getAdminFramework(id)
      setFwDetail(detail)
      setQImportFile(null)
      await loadFwAssignments(id, detail.framework.status)
      await loadFwQuestions(id)
    } catch (e) {
      setError(String(e))
    } finally {
      setFwBusy(false)
    }
  }

  const clearFrameworkSelection = () => {
    setFwDetail(null)
    setFwAssignments([])
    setFwQuestions([])
    setQImportFile(null)
  }

  const runQuestionsImport = async () => {
    if (!fwDetail || !qImportFile) return
    setFwBusy(true)
    try {
      const result = await api.importFrameworkQuestions(fwDetail.framework.id, qImportFile)
      setOk(
        `Questions v${result.versionNumber}: imported ${result.importedCount}` +
          (result.skippedRows ? `, skipped ${result.skippedRows}` : ''),
      )
      setQImportFile(null)
      setFwQuestions(await api.listFrameworkQuestions(fwDetail.framework.id))
    } catch (e) {
      setError(String(e))
    } finally {
      setFwBusy(false)
    }
  }

  const askFrameworkConfirm = (
    action: 'validate' | 'publish' | 'delete',
    f: { id: number; name: string; code: string; version: string },
  ) => {
    setFwConfirm({
      action,
      id: f.id,
      label: f.name,
    })
  }

  const runFrameworkConfirm = async () => {
    if (!fwConfirm) return
    const { action, id } = fwConfirm
    setFwBusy(true)
    try {
      if (action === 'validate') {
        await api.validateFramework(id)
        setOk('Framework marked VALIDATED')
        loadFrameworks()
        if (fwDetail?.framework.id === id) {
          const detail = await api.getAdminFramework(id)
          setFwDetail(detail)
          await loadFwAssignments(id, detail.framework.status)
          await loadFwQuestions(id)
        }
      } else if (action === 'publish') {
        await api.publishFramework(id)
        setOk('Framework published: enable it for tenants from the viewer')
        loadFrameworks()
        const detail = await api.getAdminFramework(id)
        setFwDetail(detail)
        await loadFwAssignments(id, detail.framework.status)
        await loadFwQuestions(id)
      } else {
        await api.deleteFrameworkDraft(id)
        setOk('Framework deleted. You can import this version again.')
        if (fwDetail?.framework.id === id) {
          setFwDetail(null)
          setFwAssignments([])
          setFwQuestions([])
        }
        loadFrameworks()
      }
      setFwConfirm(null)
    } catch (e) {
      setError(String(e))
    } finally {
      setFwBusy(false)
    }
  }

  const runPublishAllValidated = async () => {
    if (!fwBulkPublish?.length) return
    setFwBusy(true)
    let published = 0
    const failures: string[] = []
    const openDetailId = fwDetail?.framework.id
    try {
      for (const f of fwBulkPublish) {
        try {
          await api.publishFramework(f.id)
          published += 1
        } catch (e) {
          failures.push(`${f.code} ${f.version}: ${String(e)}`)
        }
      }
      loadFrameworks()
      if (openDetailId != null && fwBulkPublish.some((f) => f.id === openDetailId)) {
        const detail = await api.getAdminFramework(openDetailId)
        setFwDetail(detail)
        await loadFwAssignments(openDetailId, detail.framework.status)
      }
      setFwBulkPublish(null)
      if (failures.length > 0) {
        setError(
          `Published ${published} of ${fwBulkPublish.length}. Failed: ${failures.slice(0, 3).join('; ')}${
            failures.length > 3 ? ` (+${failures.length - 3} more)` : ''
          }`,
        )
      } else {
        setOk(
          published === 1
            ? '1 framework published: enable for tenants from the viewer'
            : `${published} frameworks published: enable for tenants from each viewer`,
        )
      }
    } finally {
      setFwBusy(false)
    }
  }

  const openAssignDialog = (a: FrameworkTenantAssignment, enable: boolean) => {
    setFwLicenseModule(false)
    setFwLicenseExpiry(ymdPlusYears(1))
    setFwAssignDialog({
      tenantId: a.tenantId,
      tenantName: a.tenantName,
      enable,
      moduleLicensed: a.moduleLicensed,
    })
  }

  const proceedAssignToConfirm = () => {
    if (!fwAssignDialog) return
    if (fwAssignDialog.enable && !fwAssignDialog.moduleLicensed && !fwLicenseModule) return
    if (fwAssignDialog.enable && !fwAssignDialog.moduleLicensed && fwLicenseModule && !fwLicenseExpiry) return
    setFwAssignConfirm({
      ...fwAssignDialog,
      licenseModule: fwAssignDialog.enable && !fwAssignDialog.moduleLicensed && fwLicenseModule,
    })
    setFwAssignDialog(null)
  }

  const runAssignConfirm = async () => {
    if (!fwAssignConfirm || !fwDetail || fwDetail.framework.status !== 'PUBLISHED') return
    const { tenantId, enable, licenseModule } = fwAssignConfirm
    setFwBusy(true)
    try {
      const rows = await api.updateFrameworkTenantAssignments(fwDetail.framework.id, [
        {
          tenantId,
          assigned: enable,
          licenseModule: enable ? licenseModule : undefined,
          expiresOn: enable && licenseModule ? fwLicenseExpiry : undefined,
        },
      ])
      setFwAssignments(rows)
      setOk(
        enable
          ? licenseModule
            ? `Licensed ${fwDetail.framework.moduleCode} and activated for ${tenantId}`
            : `Activated for ${tenantId}`
          : `Deactivated for ${tenantId}`,
      )
      setFwAssignConfirm(null)
      setFwLicenseModule(false)
    } catch (e) {
      setError(String(e))
    } finally {
      setFwBusy(false)
    }
  }

  const headerTitle = NAV.find((n) => n.id === active)?.label ?? 'Tenants'
  const oidcReady = !!(oidcIssuer.trim() && oidcClientId.trim())
  const ssoStep = !idpTenant ? 1 : !oidcReady && !ssoEnabled ? 2 : 3

  const licenseTenantRow = tenants.find((t) => t.tenantId === licenseTenant)
  const modulesTopbarActions =
    active === 'modules' ? (
      <Stack direction="row" spacing={1.25} alignItems="center" flexWrap="wrap" useFlexGap>
        <Button
          size="small"
          variant="contained"
          color="primary"
          startIcon={<ApartmentRoundedIcon />}
          endIcon={<ExpandMoreRoundedIcon />}
          aria-label="Tenant to license"
          aria-haspopup="listbox"
          aria-expanded={Boolean(licenseTenantMenu)}
          data-testid="sa-modules-tenant"
          onClick={(e) => setLicenseTenantMenu(e.currentTarget)}
          sx={{
            minWidth: { xs: 188, sm: 248 },
            maxWidth: 320,
            justifyContent: 'space-between',
            fontWeight: 700,
            px: 1.35,
            py: 0.7,
            boxShadow: 'none',
          }}
        >
          <Box
            component="span"
            sx={{
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
              textAlign: 'left',
              flex: 1,
            }}
          >
            {licenseTenantRow ? licenseTenantRow.name : 'Choose Tenant'}
          </Box>
        </Button>
        <Menu
          anchorEl={licenseTenantMenu}
          open={Boolean(licenseTenantMenu)}
          onClose={() => setLicenseTenantMenu(null)}
          MenuListProps={{ 'aria-label': 'Tenant to license' }}
          slotProps={{ paper: { sx: { minWidth: 280, maxHeight: 360 } } }}
        >
          {tenants.length === 0 ? (
            <MenuItem disabled>No tenants yet</MenuItem>
          ) : (
            tenants.map((t) => (
              <MenuItem
                key={t.tenantId}
                selected={t.tenantId === licenseTenant}
                onClick={() => {
                  selectLicenseTenant(t.tenantId)
                  setLicenseTenantMenu(null)
                }}
              >
                <ListItemText
                  primary={t.name}
                  secondary={t.tenantId}
                  primaryTypographyProps={{ fontWeight: 600, fontSize: '0.8125rem' }}
                  secondaryTypographyProps={{ fontFamily: MONO, fontSize: '0.6875rem' }}
                />
              </MenuItem>
            ))
          )}
        </Menu>
        {licenseTenant ? (
          <>
            <Typography
              variant="caption"
              sx={{
                fontWeight: 600,
                whiteSpace: 'nowrap',
                display: { xs: 'none', md: 'inline' },
                color: brand.muted,
              }}
            >
              {licensedModuleCount} of {licenses.length || MODULE_CODES.length} licensed
            </Typography>
            {expiredLicenseCount > 0 && (
              <Typography
                variant="caption"
                sx={{
                  fontWeight: 700,
                  whiteSpace: 'nowrap',
                  color: '#9B2C2C',
                }}
              >
                {expiredLicenseCount} expired
              </Typography>
            )}
            {unlicensedModuleCount > 0 && (
              <Button
                size="small"
                variant="contained"
                color="primary"
                startIcon={<DoneAllRoundedIcon />}
                disabled={licenseBusy}
                onClick={() => {
                  setLicenseAllExpiry(ymdPlusYears(1))
                  setLicenseAllConfirm(true)
                }}
              >
                License All ({unlicensedModuleCount})
              </Button>
            )}
            {licensedModuleCount > 0 && (
              <Button
                size="small"
                variant="outlined"
                color="primary"
                startIcon={<EventRepeatRoundedIcon />}
                disabled={licenseBusy}
                onClick={() => setExtendDialog({ codes: 'all', expiresOn: ymdPlusYears(1) })}
              >
                {licensedWithDateCount > 0 ? 'Reset Expiry for All' : 'Set Expiry for All'}
              </Button>
            )}
            {licensedModuleCount > 0 && (
              <Button
                size="small"
                variant="outlined"
                color="primary"
                startIcon={<BlockRoundedIcon />}
                disabled={licenseBusy}
                onClick={() => setRevokeAllConfirm(true)}
              >
                Revoke All
              </Button>
            )}
          </>
        ) : null}
      </Stack>
    ) : undefined

  const shellBreadcrumb =
    active === 'tenants'
      ? 'Platform › Tenants'
      : active === 'sso'
        ? 'Platform › SSO / AD'
        : active === 'frameworks'
          ? 'Platform › Frameworks'
          : active === 'modules'
            ? 'Platform › Modules'
            : undefined

  const shellPageActions =
    active === 'tenants' ? (
      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
        <Typography
          variant="caption"
          sx={{
            fontWeight: 700,
            whiteSpace: 'nowrap',
            display: { xs: 'none', sm: 'inline' },
            color: brand.goldDark,
            bgcolor: brand.goldLight,
            border: `1px solid ${brand.goldMid}`,
            px: 1,
            py: 0.35,
            borderRadius: 1,
          }}
        >
          {filtered.length}/{tenants.length} workspaces
        </Typography>
        <Button
          size="small"
          variant="contained"
          startIcon={<AddRoundedIcon />}
          onClick={() => {
            setProvisioned(null)
            setProvisionPasswordVisible(false)
            setProvisionOpen(true)
          }}
        >
          Provision
        </Button>
      </Stack>
    ) : active === 'frameworks' ? (
      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
        {validatedFrameworks.length > 0 && (
          <Button
            size="small"
            variant="outlined"
            color="primary"
            startIcon={<PublishRoundedIcon />}
            disabled={fwBusy}
            onClick={() => setFwBulkPublish(validatedFrameworks)}
          >
            Publish All ({validatedFrameworks.length})
          </Button>
        )}
        <Button
          size="small"
          variant="contained"
          color="primary"
          startIcon={<StorageRoundedIcon />}
          onClick={() => void openBundleImport()}
          disabled={fwBusy || bundleBusy}
          data-testid="sa-framework-import-bundle-open"
        >
          Import All Frameworks
        </Button>
        <Button
          size="small"
          variant="outlined"
          color="primary"
          startIcon={<UploadFileRoundedIcon />}
          onClick={() => openImport()}
          disabled={fwBusy}
          data-testid="sa-framework-import-open"
        >
          Upload Excel
        </Button>
      </Stack>
    ) : modulesTopbarActions

  if (!workspaceReady) {
    return (
      <WorkspaceBootScreen
        title="Opening the platform console"
        message="Loading client tenants and catalog. This usually takes a few seconds."
      />
    )
  }

  return (
    <AppShell
      productTag="Platform Console"
      nav={NAV}
      active={active}
      onNavChange={setActive}
      email={session.email}
      accountMeta="Platform operator · Local session"
      roleChips={
        <Chip size="small" color="secondary" label="Super Admin" variant="filled" />
      }
      headerTitle={headerTitle}
      headerBreadcrumb={shellBreadcrumb}
      pageActions={shellPageActions}
      workspaceName="Platform"
      onLogout={onLogout}
      canChangePassword
      superAdmin
    >
      {active === 'tenants' && (
        <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
          <KpiCardGrid columns={4}>
            <KpiCard label="Client tenants" value={stats.total} accentColor={brand.gold} />
            <KpiCard label="Active" value={stats.active} accentColor={brand.goldDark} />
            <KpiCard
              label="SSO enabled"
              value={stats.sso}
              accentColor={brand.gold}
              sub="Entra OIDC"
            />
            <KpiCard
              label="Licence expired"
              value={stats.expired}
              accentColor={stats.expired > 0 ? semantic.risk.critical : brand.goldDark}
              sub="View-only workspaces"
            />
          </KpiCardGrid>

          <Card sx={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden' }}>
            <Box
              sx={{
                px: 2,
                py: 1.25,
                borderBottom: `1px solid ${brand.hairline}`,
                flexShrink: 0,
                bgcolor: brand.paper,
              }}
            >
              <CompactTextField
                label="Search tenants"
                placeholder="Tenant id, organisation, or module"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                fullWidth
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchRoundedIcon sx={{ fontSize: 16, color: 'text.secondary' }} />
                    </InputAdornment>
                  ),
                }}
              />
            </Box>

            <TableContainer sx={{ flex: 1, minHeight: 0 }}>
              <Table size="small" stickyHeader sx={registerTableSx}>
                <TableHead>
                  <TableRow>
                    <TableCell sx={compactTableCellSx}>Tenant</TableCell>
                    <TableCell sx={primaryTableCellSx}>Organisation</TableCell>
                    <TableCell sx={compactTableCellSx}>Status</TableCell>
                    <TableCell sx={mediumTableCellSx}>Identity</TableCell>
                    <TableCell sx={compactTableCellSxWidth('9rem')}>Licence</TableCell>
                    <TableCell sx={compactTableCellSxWidth('16rem')}>Expiry</TableCell>
                    <TableCell sx={compactTableCellSxWidth('7rem')}>Created</TableCell>
                    <TableCell sx={{ ...compactTableCellSxWidth('3rem'), ...lastTableCellSx }} align="right" />
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filtered.map((t) => {
                    const oidcConfigured = !!(t.oidcIssuer && t.oidcClientId)
                    const license = t.license
                    const licenseLabel = tenantLicenseStatusLabel(license?.status, license?.writeLocked)
                    const licenseTone = licenseStatusTone(
                      license?.writeLocked ? 'EXPIRED' : license?.status,
                    )
                    const licensedCodes = license?.licensedCodes ?? []
                    const licenseCountLabel = license
                      ? `${license.licensedCount} of ${license.moduleCount}`
                      : '-'
                    const term = license
                      ? licenceTermView({
                          expiresOn: license.earliestExpiresOn,
                          expired: license.writeLocked || license.status === 'EXPIRED',
                          status: license.status,
                          daysRemaining: license.daysRemaining,
                          phrase: 'valid-until',
                        })
                      : null
                    const expiryTerm = term && license && license.licensedCount > 0
                      ? {
                          ...term,
                          detail: license.writeLocked
                            ? (license.mixedExpiry ? 'Has expired module' : 'Module expired')
                            : license.mixedExpiry
                              ? (term.detail ? `${term.detail} · mixed dates` : 'Mixed dates')
                              : term.detail,
                        }
                      : null
                    const modulesTitle = licensedCodes.length
                      ? `Open Modules: ${licensedCodes.join(', ')}`
                      : 'Open Modules for this tenant'
                    return (
                      <TableRow key={t.id} hover>
                        <TableCell>
                          <Typography variant="body2" fontWeight={650} fontFamily={MONO} fontSize="0.78rem">
                            {t.tenantId}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" noWrap>{t.name}</Typography>
                        </TableCell>
                        <TableCell>
                          <StatusChip
                            label={t.status === 'ACTIVE' ? 'Active' : t.status}
                            tone={t.status === 'ACTIVE' ? 'success' : 'neutral'}
                          />
                        </TableCell>
                        <TableCell>
                          <StatusChip
                            label={t.ssoEnabled ? 'SSO on' : oidcConfigured ? 'SSO ready' : 'Local'}
                            tone={t.ssoEnabled ? 'info' : oidcConfigured ? 'warning' : 'neutral'}
                          />
                        </TableCell>
                        <TableCell
                          onClick={() => openTenantModules(t.tenantId)}
                          sx={{ cursor: 'pointer' }}
                          aria-label={modulesTitle}
                        >
                          <Typography
                            noWrap
                            sx={{
                              fontSize: '0.8125rem',
                              fontWeight: 650,
                              color: licenseTone === 'danger' ? '#9B2C2C' : brand.ink,
                            }}
                          >
                            {licenseCountLabel} · {licenseLabel}
                          </Typography>
                        </TableCell>
                        <TableCell
                          onClick={() => openTenantModules(t.tenantId)}
                          sx={{ cursor: 'pointer' }}
                          aria-label={modulesTitle}
                        >
                          {expiryTerm ? (
                            <LicenceTermBadge term={expiryTerm} compact />
                          ) : (
                            <Typography variant="caption" color="text.secondary">-</Typography>
                          )}
                        </TableCell>
                        <TableCell>
                          <Typography variant="caption" color="text.secondary">
                            {formatTenantDate(t.createdAt)}
                          </Typography>
                        </TableCell>
                        <TableCell align="right" sx={lastTableCellSx}>
                          <Tooltip title="Actions">
                            <IconButton size="small" aria-label={`Actions for ${t.tenantId}`} onClick={(e) => openRowMenu(e, t)}>
                              <MoreVertRoundedIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </TableCell>
                      </TableRow>
                    )
                  })}
                  {filtered.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={8} sx={{ border: 0, height: 280, verticalAlign: 'middle' }}>
                        <Box sx={{ textAlign: 'center', px: 2 }}>
                          <Typography variant="subtitle2" gutterBottom>
                            {tenants.length === 0 ? 'No tenants yet' : 'No matching tenants'}
                          </Typography>
                          <Typography variant="body2" color="text.secondary" sx={{ mb: 1.75, maxWidth: 300, mx: 'auto' }}>
                            {tenants.length === 0
                              ? 'Provision a client workspace to create its isolated database and bootstrap admin.'
                              : 'Try a different search.'}
                          </Typography>
                          {tenants.length === 0 && (
                            <Button
                              variant="contained"
                              size="small"
                              startIcon={<AddRoundedIcon />}
                              onClick={() => setProvisionOpen(true)}
                            >
                              Provision Tenant
                            </Button>
                          )}
                        </Box>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </Card>
        </Stack>
      )}

      {active === 'sso' && (
        <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0 }}>
          <Typography variant="body2" color="text.secondary" sx={{ fontSize: '0.8125rem' }}>
            Configure Microsoft Entra OIDC per tenant workspace.
          </Typography>
          <Card sx={{ flexShrink: 0 }}>
            <CardContent sx={{ py: 1.25, px: 2, '&:last-child': { pb: 1.25 } }}>
              <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={{ xs: 1.25, sm: 3 }}
                alignItems={{ sm: 'center' }}
                divider={<Divider orientation="vertical" flexItem sx={{ display: { xs: 'none', sm: 'block' } }} />}
              >
                <SsoStep n={1} label="Select workspace" active={ssoStep === 1} done={!!idpTenant} />
                <SsoStep n={2} label="Configure OIDC" active={ssoStep === 2} done={oidcReady || ssoEnabled} />
                <SsoStep n={3} label="Enable SSO" active={ssoStep === 3} done={ssoEnabled} />
              </Stack>
            </CardContent>
          </Card>

          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', lg: '280px 1fr' },
              gap: 1.25,
              alignItems: 'stretch',
              flex: 1,
              minHeight: 0,
            }}
          >
            <Card sx={{ height: 'fit-content' }}>
              <CardContent sx={{ p: 2 }}>
                <Typography variant="subtitle2" sx={{ mb: 0.35 }}>1 · Workspace</Typography>
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1.25 }}>
                  Choose the tenant to configure.
                </Typography>
                <FormSelect
                  label="Tenant"
                  fullWidth
                  value={idpTenant}
                  onChange={selectIdpTenant}
                  options={tenants.map((t) => ({ value: t.tenantId, label: t.tenantId }))}
                />
                {idpTenant && (
                  <Box
                    sx={{
                      mt: 1.75,
                      p: 1.25,
                      borderRadius: 1,
                      bgcolor: brand.canvas,
                      border: `1px solid ${brand.hairline}`,
                    }}
                  >
                    <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 0.5 }}>
                      <Typography variant="caption">Entry URL</Typography>
                      <Tooltip title="Copy">
                        <IconButton size="small" onClick={() => copyEntryUrl(idpTenant)} aria-label="Copy SSO entry URL">
                          <ContentCopyRoundedIcon sx={{ fontSize: 14 }} />
                        </IconButton>
                      </Tooltip>
                    </Stack>
                    <Typography
                      variant="body2"
                      component="code"
                      sx={{ display: 'block', wordBreak: 'break-all', fontSize: '0.68rem', fontFamily: MONO, color: brand.ink }}
                    >
                      {ssoEntryUrl(idpTenant)}
                    </Typography>
                  </Box>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardContent sx={{ p: 2 }}>
                <Stack direction="row" alignItems="flex-start" justifyContent="space-between" sx={{ mb: 1.25 }}>
                  <Box>
                    <Typography variant="subtitle2">2–3 · OIDC & enable</Typography>
                    <Typography variant="caption" color="text.secondary">
                      Entra authenticates; Arbor governs roles.
                    </Typography>
                  </Box>
                  <FormControlLabel
                    control={<Switch checked={ssoEnabled} onChange={(e) => setSsoEnabled(e.target.checked)} size="small" />}
                    label={<Typography variant="caption" fontWeight={650}>SSO enabled</Typography>}
                    disabled={!idpTenant}
                    sx={{ mr: 0, ml: 1 }}
                  />
                </Stack>
                <Alert severity="info" variant="outlined" sx={{ mb: 1.75 }}>
                  <Stack direction="row" alignItems="flex-start" justifyContent="space-between" spacing={1}>
                    <Box sx={{ minWidth: 0, flex: 1 }}>
                      <Typography variant="caption" display="block" fontWeight={650}>
                        Redirect URI (share with tenant Entra admin)
                      </Typography>
                      <Typography
                        variant="caption"
                        component="code"
                        sx={{ display: 'block', wordBreak: 'break-all', fontFamily: MONO, mt: 0.35 }}
                      >
                        {idpTenant
                          ? ssoRedirectUri(idpTenant)
                          : 'Select a workspace to show the redirect URI'}
                      </Typography>
                      <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
                        Add as a Web redirect URI in Entra. Leave client secret blank to keep the stored value.
                      </Typography>
                    </Box>
                    {idpTenant && (
                      <Tooltip title="Copy redirect URI">
                        <IconButton size="small" onClick={copyRedirectUri} aria-label="Copy redirect URI">
                          <ContentCopyRoundedIcon sx={{ fontSize: 14 }} />
                        </IconButton>
                      </Tooltip>
                    )}
                  </Stack>
                </Alert>
                <Stack spacing={1.35}>
                  <CompactTextField
                    label="OIDC issuer"
                    disabled={!idpTenant}
                    placeholder="https://login.microsoftonline.com/<Directory-tenant-ID>/v2.0"
                    helperText="Directory (tenant) ID: not 'common'"
                    value={oidcIssuer}
                    onChange={(e) => setOidcIssuer(e.target.value)}
                  />
                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.25}>
                    <CompactTextField label="Client ID" disabled={!idpTenant} value={oidcClientId} onChange={(e) => setOidcClientId(e.target.value)} />
                    <CompactTextField
                      label="Client secret"
                      showPasswordToggle
                      disabled={!idpTenant}
                      placeholder="leave blank to keep existing"
                      value={oidcClientSecret}
                      onChange={(e) => setOidcClientSecret(e.target.value)}
                    />
                  </Stack>
                  <Divider sx={{ my: 0.5 }} />
                  <Typography variant="caption" color="text.secondary" display="block">
                    JIT provisioning: if an SSO user has no Arbor account
                  </Typography>
                  <FormControlLabel
                    control={
                      <Switch
                        checked={jitEnabled}
                        onChange={(e) => setJitEnabled(e.target.checked)}
                        size="small"
                        disabled={!idpTenant || !ssoEnabled}
                      />
                    }
                    label={<Typography variant="caption" fontWeight={650}>JIT enabled</Typography>}
                    sx={{ mr: 0, alignSelf: 'flex-start' }}
                  />
                  <FormSelect
                    label="Default for unmatched users"
                    fullWidth
                    disabled={!idpTenant || !ssoEnabled || !jitEnabled}
                    value={jitDefaultRole}
                    onChange={(v) => setJitDefaultRole(v as 'BLOCK' | 'CONTRIBUTOR')}
                    options={[
                      { value: 'BLOCK', label: 'Block (deny + notify Admin)' },
                      { value: 'CONTRIBUTOR', label: 'Create as Contributor' },
                    ]}
                  />
                  <Button variant="contained" onClick={requestSaveIdp} disabled={!idpTenant} sx={{ alignSelf: 'flex-start' }}>
                    Save SSO Settings
                  </Button>
                </Stack>
              </CardContent>
            </Card>
          </Box>
        </Stack>
      )}

      {active === 'frameworks' && (
        <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
          <KpiCardGrid columns={3}>
            <KpiCard
              label="Total frameworks"
              value={fwLibrary.length}
              accentColor={brand.gold}
              sub={draftFrameworkCount > 0 ? `${draftFrameworkCount} draft` : undefined}
            />
            <KpiCard
              label="Validated"
              value={validatedFrameworks.length}
              accentColor={semantic.risk.medium}
              sub={validatedFrameworks.length > 0 ? 'Ready to publish' : undefined}
            />
            <KpiCard
              label="Published"
              value={publishedFrameworkCount}
              accentColor={semantic.risk.low}
            />
          </KpiCardGrid>

          <Card sx={{ display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden', flex: 1 }}>
            <Box
              sx={{
                px: 2,
                py: 1,
                borderBottom: `1px solid ${brand.hairline}`,
                flexShrink: 0,
                display: 'flex',
                alignItems: 'baseline',
                gap: 1,
              }}
            >
              <Typography
                sx={{
                  fontWeight: 650,
                  fontSize: '0.8125rem',
                  color: brand.ink,
                  letterSpacing: '-0.01em',
                }}
              >
                Library
              </Typography>
              <Typography sx={{ fontSize: '0.6875rem', color: brand.muted, fontWeight: 500 }}>
                {fwLibrary.length} framework{fwLibrary.length === 1 ? '' : 's'}
                {validatedFrameworks.length > 0
                  ? ` · ${validatedFrameworks.length} ready to publish`
                  : ''}
              </Typography>
            </Box>
            <TableContainer sx={{ flex: 1, minHeight: 0 }}>
              <Table
                size="small"
                stickyHeader
                sx={{
                  width: '100%',
                  tableLayout: 'fixed',
                  '& .MuiTableCell-root': {
                    borderColor: brand.hairline,
                    py: 0.65,
                    px: 1.25,
                    fontSize: '0.75rem',
                    verticalAlign: 'middle',
                  },
                  '& .MuiTableCell-head, & .MuiTableCell-stickyHeader': {
                    fontWeight: 650,
                    fontSize: '0.6875rem',
                    letterSpacing: '0.06em',
                    textTransform: 'uppercase',
                    color: brand.ink,
                    background: `${brand.sidebar} !important`,
                    backgroundColor: `${brand.sidebar} !important`,
                    bgcolor: brand.sidebar,
                    zIndex: 3,
                    py: 0.55,
                    borderBottom: `1px solid ${brand.hairline}`,
                  },
                  '& .MuiTableBody .MuiTableRow:last-child .MuiTableCell-root': {
                    borderBottom: 0,
                  },
                }}
              >
                <TableHead>
                  <TableRow>
                    <TableCell sx={{ ...primaryTableCellSx, pl: 1.5 }}>Framework</TableCell>
                    <TableCell sx={compactTableCellSx}>Status</TableCell>
                    <TableCell sx={compactTableCellSxWidth('5rem')} align="right">Clauses</TableCell>
                    <TableCell sx={compactTableCellSxWidth('5.5rem')} align="right">Annexures</TableCell>
                    <TableCell sx={compactTableCellSxWidth('5rem')} align="right">Controls</TableCell>
                    <TableCell sx={{ ...mediumTableCellSx, ...lastTableCellSx }} align="right">Next step</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {fwLibrary.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} sx={{ border: 0, height: 240, verticalAlign: 'middle' }}>
                        <Box sx={{ textAlign: 'center', px: 2 }}>
                          <Typography variant="subtitle2" gutterBottom>No frameworks yet</Typography>
                          <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
                            Import every framework shipped with this app (no file pick), or upload one Excel.
                          </Typography>
                          <Stack direction="row" spacing={1} justifyContent="center" flexWrap="wrap" useFlexGap>
                            <Button
                              variant="contained"
                              size="small"
                              startIcon={<StorageRoundedIcon />}
                              onClick={() => void openBundleImport()}
                              disabled={fwBusy || bundleBusy}
                            >
                              Import All Frameworks
                            </Button>
                            <Button
                              variant="outlined"
                              size="small"
                              startIcon={<UploadFileRoundedIcon />}
                              onClick={() => openImport()}
                            >
                              Upload Excel
                            </Button>
                          </Stack>
                        </Box>
                      </TableCell>
                    </TableRow>
                  ) : (
                    fwLibrary.map((f) => (
                      <TableRow
                        key={f.id}
                        hover
                        selected={fwDetail?.framework.id === f.id}
                        onClick={() => viewFramework(f.id)}
                        sx={{ cursor: 'pointer' }}
                      >
                        <TableCell sx={{ pl: 1.5 }}>
                          <Typography
                            sx={{
                              fontWeight: 600,
                              fontSize: '0.8125rem',
                              color: brand.ink,
                              lineHeight: 1.3,
                              letterSpacing: '-0.005em',
                            }}
                          >
                            {f.name}
                          </Typography>
                          {f.moduleCode ? (
                            <Typography
                              sx={{
                                fontSize: '0.6875rem',
                                color: brand.muted,
                                fontWeight: 500,
                                mt: 0.15,
                                letterSpacing: '0.02em',
                              }}
                            >
                              {f.moduleCode}
                            </Typography>
                          ) : null}
                        </TableCell>
                        <TableCell>
                          <StatusChip
                            label={f.status}
                            tone={frameworkStatusTone(f.status)}
                            sx={{ height: 20, fontSize: '0.6875rem', fontWeight: 600, '& .MuiChip-label': { px: 0.7 } }}
                          />
                        </TableCell>
                        <TableCell align="right">
                          <Typography sx={{ fontFamily: MONO, fontSize: '0.75rem', color: brand.inkSoft }}>
                            {f.clauseCount ?? '-'}
                          </Typography>
                        </TableCell>
                        <TableCell align="right">
                          <Typography sx={{ fontFamily: MONO, fontSize: '0.75rem', color: brand.inkSoft }}>
                            {f.annexControlCount ?? '-'}
                          </Typography>
                        </TableCell>
                        <TableCell align="right">
                          <Typography sx={{ fontFamily: MONO, fontSize: '0.75rem', color: brand.inkSoft }}>
                            {f.controlCount}
                          </Typography>
                        </TableCell>
                        <TableCell align="right" sx={lastTableCellSx} onClick={(e) => e.stopPropagation()}>
                          {f.status === 'DRAFT' && (
                            <Button
                              size="small"
                              variant="contained"
                              disabled={fwBusy}
                              onClick={() => askFrameworkConfirm('validate', f)}
                              sx={{ fontSize: '0.6875rem', minHeight: 26, px: 1 }}
                            >
                              Validate
                            </Button>
                          )}
                          {f.status === 'VALIDATED' && (
                            <Button
                              size="small"
                              variant="contained"
                              disabled={fwBusy}
                              onClick={() => askFrameworkConfirm('publish', f)}
                              sx={{ fontSize: '0.6875rem', minHeight: 26, px: 1 }}
                            >
                              Publish
                            </Button>
                          )}
                          {f.status === 'PUBLISHED' && (
                            <Button
                              size="small"
                              variant="text"
                              disabled={fwBusy}
                              onClick={() => viewFramework(f.id)}
                              sx={{ fontSize: '0.6875rem', minHeight: 26, px: 0.75, fontWeight: 600 }}
                            >
                              View / Activate
                            </Button>
                          )}
                          <Button
                            size="small"
                            color="error"
                            disabled={fwBusy}
                            onClick={() => askFrameworkConfirm('delete', f)}
                            data-testid={`sa-framework-delete-${f.id}`}
                            sx={{ fontSize: '0.6875rem', minHeight: 26, px: 0.75, fontWeight: 600 }}
                          >
                            Delete
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </Card>
        </Stack>
      )}

          <FrameworkViewerDialog
            open={Boolean(fwDetail)}
            detail={fwDetail}
            onClose={clearFrameworkSelection}
            editable
            questions={fwQuestions}
            onDetailChange={setFwDetail}
            onEditError={(msg) => setError(msg)}
            headerActions={
              fwDetail ? (
                <Stack direction="row" spacing={0.75}>
                  {fwDetail.framework.status === 'DRAFT' && (
                    <>
                      <Button
                        size="small"
                        variant="contained"
                        disabled={fwBusy}
                        onClick={() => askFrameworkConfirm('validate', fwDetail.framework)}
                      >
                        Validate
                      </Button>
                      <Button
                        size="small"
                        color="error"
                        disabled={fwBusy}
                        onClick={() => askFrameworkConfirm('delete', fwDetail.framework)}
                      >
                        Delete
                      </Button>
                    </>
                  )}
                  {fwDetail.framework.status === 'VALIDATED' && (
                    <Button
                      size="small"
                      variant="contained"
                      disabled={fwBusy}
                      onClick={() => askFrameworkConfirm('publish', fwDetail.framework)}
                    >
                      Publish
                    </Button>
                  )}
                  {fwDetail.framework.status !== 'DRAFT' && (
                    <Button
                      size="small"
                      color="error"
                      disabled={fwBusy}
                      onClick={() => askFrameworkConfirm('delete', fwDetail.framework)}
                    >
                      Delete
                    </Button>
                  )}
                </Stack>
              ) : null
            }
            extraTabs={
              fwDetail
                ? [
                    {
                      id: 'questions',
                      label: `Questions (${fwQuestions.length})`,
                      content: (
                        <Box sx={{ px: 2.5, py: 2 }}>
                          <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
                            Question bank imported with the framework pack (and optional Excel top-up).
                            Open a control in the library tab to read questions next to Evidence.
                          </Typography>
                          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ mb: 2 }}>
                            <Button
                              size="small"
                              startIcon={<DownloadRoundedIcon />}
                              disabled={fwBusy}
                              onClick={() =>
                                void api
                                  .downloadFrameworkQuestionsSample(fwDetail.framework.id)
                                  .catch((e) => setError(String(e)))
                              }
                            >
                              Sample Excel
                            </Button>
                            <Button
                              size="small"
                              component="label"
                              variant="outlined"
                              startIcon={<UploadFileRoundedIcon />}
                              disabled={fwBusy}
                            >
                              {qImportFile ? qImportFile.name : 'Choose .xlsx'}
                              <input
                                type="file"
                                hidden
                                accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                onChange={(e) => setQImportFile(e.target.files?.[0] ?? null)}
                              />
                            </Button>
                            <Button
                              size="small"
                              variant="contained"
                              disabled={fwBusy || !qImportFile}
                              onClick={() => void runQuestionsImport()}
                            >
                              Import Questions
                            </Button>
                          </Stack>
                          {fwQuestions.length === 0 ? (
                            <Typography variant="body2" color="text.secondary">
                              No active questions for this framework.
                            </Typography>
                          ) : (
                            <Table size="small">
                              <TableHead>
                                <TableRow>
                                  <TableCell>Code</TableCell>
                                  <TableCell>Type</TableCell>
                                  <TableCell>Text</TableCell>
                                  <TableCell width={70}>Ver</TableCell>
                                </TableRow>
                              </TableHead>
                              <TableBody>
                                {fwQuestions.slice(0, 100).map((q) => (
                                  <TableRow key={q.id}>
                                    <TableCell sx={{ fontFamily: MONO, fontSize: 12 }}>
                                      {q.questionCode}
                                    </TableCell>
                                    <TableCell>{q.questionType ?? '-'}</TableCell>
                                    <TableCell>
                                      <Typography variant="body2" noWrap title={q.questionText}>
                                        {q.questionText}
                                      </Typography>
                                    </TableCell>
                                    <TableCell>{q.versionNumber}</TableCell>
                                  </TableRow>
                                ))}
                              </TableBody>
                            </Table>
                          )}
                          {fwQuestions.length > 100 && (
                            <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                              Showing first 100 of {fwQuestions.length}
                            </Typography>
                          )}
                        </Box>
                      ),
                    },
                    ...(fwDetail.framework.status === 'PUBLISHED'
                      ? [
                          {
                            id: 'tenants',
                            label: 'Tenants',
                            content: (
                        <>
                          <Box sx={{ px: 2.5, py: 1.25, borderBottom: `1px solid ${brand.hairline}` }}>
                            <Typography variant="body2" color="text.secondary">
                              Activate for a client (same as Owner/Admin). Module{' '}
                              <strong>{fwDetail.framework.moduleCode}</strong> must be licensed: use Modules,
                              or opt in to license it in the activate dialog when it is off.
                            </Typography>
                          </Box>
                          {fwAssignments.length === 0 ? (
                            <Box sx={{ px: 2.5, py: 4 }}>
                              <Typography variant="body2" color="text.secondary">
                                No tenants yet. Provision one under Tenants, then return here.
                              </Typography>
                            </Box>
                          ) : (
                            <Table size="small" stickyHeader>
                              <TableHead>
                                <TableRow>
                                  <TableCell>Tenant</TableCell>
                                  <TableCell width={100}>Status</TableCell>
                                  <TableCell align="right" width={120}>Action</TableCell>
                                </TableRow>
                              </TableHead>
                              <TableBody>
                                {fwAssignments.map((a) => (
                                  <TableRow key={a.tenantId} hover>
                                    <TableCell>
                                      <Typography variant="body2" fontWeight={650}>{a.tenantId}</Typography>
                                      <Typography variant="caption" color="text.secondary">
                                        {a.tenantName}
                                        {!a.moduleLicensed && !a.assigned ? ' · module off' : ''}
                                      </Typography>
                                    </TableCell>
                                    <TableCell>
                                      <StatusChip
                                        label={a.assigned ? 'Activated' : 'Not activated'}
                                        tone={a.assigned ? 'success' : 'neutral'}
                                      />
                                    </TableCell>
                                    <TableCell align="right">
                                      {a.assigned ? (
                                        <Button
                                          size="small"
                                          color="error"
                                          disabled={fwBusy}
                                          onClick={() => openAssignDialog(a, false)}
                                        >
                                          Deactivate
                                        </Button>
                                      ) : (
                                        <Button
                                          size="small"
                                          variant="contained"
                                          disabled={fwBusy}
                                          onClick={() => openAssignDialog(a, true)}
                                        >
                                          Activate
                                        </Button>
                                      )}
                                    </TableCell>
                                  </TableRow>
                                ))}
                              </TableBody>
                            </Table>
                          )}
                        </>
                            ),
                          },
                        ]
                      : []),
                  ]
                : undefined
            }
          />

      {active === 'modules' && (
        <Stack spacing={1} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
          {!licenseTenant ? (
            <Box
              sx={{
                flexShrink: 0,
                px: 1.5,
                py: 1,
                borderRadius: 1,
                border: `1px solid ${brand.hairline}`,
                bgcolor: brand.paper,
              }}
            >
              <Typography sx={{ fontSize: '0.8125rem', color: brand.muted, fontWeight: 500, lineHeight: 1.45 }}>
                Select a tenant to license modules and choose frameworks. The catalog is read-only until then.
              </Typography>
            </Box>
          ) : (
            <Box
              sx={{
                flexShrink: 0,
                py: 0.5,
                px: 1,
                borderRadius: 1,
                border: `1px solid ${brand.hairline}`,
                bgcolor: brand.paper,
                display: 'flex',
                flexWrap: 'wrap',
                gap: 0.75,
                alignItems: 'center',
                justifyContent: 'space-between',
              }}
            >
              <Stack direction="row" spacing={0.25} alignItems="center" flexWrap="wrap" useFlexGap>
                <FormControlLabel
                  sx={{ mr: 0.25, '& .MuiFormControlLabel-label': { fontSize: '0.6875rem', fontWeight: 600 } }}
                  control={
                    <Checkbox
                      size="small"
                      disabled={licenseBusy}
                      indeterminate={selectAllPartial}
                      checked={selectAllModulesAndFrameworks}
                      onClick={(e) => {
                        e.stopPropagation()
                        // Read the checkbox from state. A controlled flip from checked to
                        // indeterminate must not clear the list (that was the first uncheck).
                        if (selectAllModulesAndFrameworks) {
                          setSelectedModuleCodes([])
                          setSelectionFrameworkIdsByCode({})
                          return
                        }
                        setSelectedModuleCodes([...MODULE_CODES])
                        const next: Record<string, number[]> = {}
                        for (const code of MODULE_CODES) {
                          next[code] = publishedFrameworksForModule(code).map((f) => f.id)
                        }
                        setSelectionFrameworkIdsByCode(next)
                      }}
                      inputProps={{ 'aria-label': 'Select all modules and frameworks' }}
                      sx={{ p: 0.35 }}
                    />
                  }
                  label="Select all"
                />
                <Button
                  size="small"
                  variant="text"
                  disabled={licenseBusy || unlicensedModuleCount === 0}
                  onClick={selectUnlicensedModules}
                  sx={{ fontSize: '0.6875rem', minHeight: 28, px: 0.75 }}
                >
                  Unlicensed
                </Button>
                <Button
                  size="small"
                  variant="text"
                  disabled={licenseBusy || licensedModuleCount === 0}
                  onClick={selectLicensedModules}
                  sx={{ fontSize: '0.6875rem', minHeight: 28, px: 0.75 }}
                >
                  Licensed
                </Button>
                {selectedModuleCodes.length > 0 && (
                  <Button
                    size="small"
                    variant="text"
                    disabled={licenseBusy}
                    onClick={() => {
                      setSelectedModuleCodes([])
                      setSelectionFrameworkIdsByCode({})
                    }}
                    sx={{ fontSize: '0.6875rem', minHeight: 28, px: 0.75 }}
                  >
                    Clear
                  </Button>
                )}
              </Stack>
              {selectedModuleCodes.length > 0 && (
                <Stack direction="row" spacing={0.75} alignItems="center" flexWrap="wrap" useFlexGap>
                  <Typography sx={{ fontSize: '0.6875rem', color: brand.muted, fontWeight: 500 }}>
                    {selectedModuleCodes.length} selected
                    {selectedToLicenseCodes.length > 0
                      ? ` · ${selectedToLicenseCodes.length} to license`
                      : ''}
                    {selectedToRevokeCodes.length > 0
                      ? ` · ${selectedToRevokeCodes.length} to revoke`
                      : ''}
                    {selectedToLicenseCodes.length > 0
                      ? ' · framework ticks in each card are entitled (1, some, or all)'
                      : ''}
                  </Typography>
                  <Button
                    size="small"
                    variant="contained"
                    color="primary"
                    disabled={licenseBusy || selectedToLicenseCodes.length === 0}
                    onClick={openLicenseSelected}
                    sx={{ fontSize: '0.6875rem', minHeight: 28 }}
                  >
                    License Selected ({selectedToLicenseCodes.length})
                  </Button>
                  <Button
                    size="small"
                    variant="outlined"
                    color="primary"
                    disabled={licenseBusy || selectedToRevokeCodes.length === 0}
                    onClick={() =>
                      setModuleBulkConfirm({ mode: 'revoke', codes: selectedToRevokeCodes })
                    }
                    sx={{ fontSize: '0.6875rem', minHeight: 28 }}
                  >
                    Revoke Selected ({selectedToRevokeCodes.length})
                  </Button>
                </Stack>
              )}
            </Box>
          )}

          <Box sx={{ flex: 1, minHeight: 0, overflow: 'auto' }}>
            <Stack spacing={0.85}>
              {moduleCatalog.map((mod) => {
                const moduleSelected = selectedModuleCodes.includes(mod.code)
                const pickedIdSet = new Set((selectionFrameworkIdsByCode[mod.code] ?? []).map(Number))
                const term = mod.license?.enabled ? licenceTermView(mod.license) : null
                const expired = term?.kind === 'expired'
                const expiring = term?.kind === 'soon' || term?.kind === 'today'
                const enabled = Boolean(mod.license?.enabled)
                const hasDate = enabled && hasLicenceCalendarDate(mod.license ?? {})
                const accent = expired ? '#DC2626' : expiring ? '#CA8A04' : enabled ? brand.gold : brand.border
                return (
                <Card
                  key={mod.code}
                  sx={{
                    overflow: 'hidden',
                    border: `1px solid ${brand.border}`,
                    borderLeft: `3px solid ${accent}`,
                    boxShadow: 'none',
                    borderRadius: 1.25,
                    bgcolor: brand.paper,
                  }}
                >
                  <Box
                    sx={{
                      px: 1.5,
                      py: 1,
                      borderBottom: `1px solid ${brand.hairline}`,
                      display: 'grid',
                      gridTemplateColumns: {
                        xs: 'auto minmax(0, 1fr)',
                        md: 'auto minmax(0, 1fr) max-content auto',
                      },
                      columnGap: 1.25,
                      rowGap: 0.75,
                      alignItems: 'center',
                      bgcolor: expired
                        ? '#FEF2F2'
                        : expiring
                          ? '#FFFBEB'
                          : enabled
                            ? '#F7FBF7'
                            : brand.paper,
                    }}
                  >
                    {licenseTenant ? (
                      <Checkbox
                        size="small"
                        disabled={licenseBusy}
                        checked={moduleSelected}
                        onChange={() => toggleModuleSelection(mod.code)}
                        inputProps={{ 'aria-label': `Select ${mod.code}` }}
                        sx={{ p: 0.25, flexShrink: 0 }}
                      />
                    ) : null}
                    <Box sx={{ minWidth: 0, flex: 1 }}>
                      <Stack direction="row" alignItems="center" spacing={0.75} flexWrap="wrap" useFlexGap>
                        <Typography
                          sx={{
                            fontWeight: 650,
                            fontSize: '0.8125rem',
                            color: brand.ink,
                            letterSpacing: '-0.01em',
                            lineHeight: 1.25,
                          }}
                        >
                          {mod.label}
                        </Typography>
                        <Typography
                          component="span"
                          sx={{
                            fontFamily: MONO,
                            fontSize: '0.6875rem',
                            fontWeight: 600,
                            color: brand.muted,
                            letterSpacing: '0.02em',
                          }}
                        >
                          {mod.code}
                        </Typography>
                      </Stack>
                      <Typography sx={{ fontSize: '0.6875rem', color: brand.muted, mt: 0.2, fontWeight: 500 }}>
                        {mod.license?.enabled && mod.publishedCount > 0
                          ? `${mod.license.entitledFrameworkIds?.length ?? 0} of ${mod.publishedCount} entitled · `
                          : mod.license?.enabled
                            ? 'Module licensed · '
                            : ''}
                        {mod.frameworkTypes.length} framework
                        {mod.frameworkTypes.length === 1 ? '' : 's'}
                        {mod.publishedCount > 0 ? ` · ${mod.publishedCount} in library` : ''}
                      </Typography>
                    </Box>
                    {licenseTenant && enabled ? (
                      <Box sx={{ gridColumn: { xs: '1 / -1', md: '3' }, justifySelf: { md: 'end' } }}>
                        <LicenceTermBadge term={term ?? licenceTermView({})} />
                      </Box>
                    ) : (
                      <Box sx={{ display: { xs: 'none', md: 'block' } }} />
                    )}
                    {licenseTenant ? (
                      <Stack
                        direction="row"
                        spacing={0.75}
                        alignItems="center"
                        sx={{ gridColumn: { xs: '1 / -1', md: '4' }, justifyContent: { xs: 'flex-start', md: 'flex-end' } }}
                      >
                        {mod.license?.enabled && mod.code !== 'AUDIT' && (
                          <Button
                            size="small"
                            variant="outlined"
                            color="primary"
                            disabled={licenseBusy}
                            onClick={() =>
                              setExtendDialog({
                                codes: [mod.code],
                                expiresOn: expiryInputValue(mod.license?.expiresOn ?? mod.license?.expiresAt),
                              })
                            }
                          >
                            {hasDate ? 'Reset Expiry' : 'Set Expiry'}
                          </Button>
                        )}
                        {mod.license?.enabled && mod.code !== 'AUDIT' && (
                          <Button
                            size="small"
                            variant="contained"
                            color="primary"
                            disabled={licenseBusy}
                            onClick={() => askModuleConfirm(mod.code, true)}
                          >
                            Edit Frameworks
                          </Button>
                        )}
                        <FormControlLabel
                          sx={{
                            mr: 0,
                            '& .MuiFormControlLabel-label': { fontSize: '0.6875rem', fontWeight: 600 },
                          }}
                          control={
                            <Switch
                              size="small"
                              checked={Boolean(mod.license?.enabled)}
                              disabled={licenseBusy || !mod.license || mod.code === 'AUDIT'}
                              onChange={(e) => askModuleConfirm(mod.code, e.target.checked)}
                              inputProps={{ 'aria-label': `License ${mod.code}` }}
                            />
                          }
                          label={mod.code === 'AUDIT' ? 'With any licence' : 'Licensed'}
                        />
                      </Stack>
                    ) : null}
                  </Box>

                  {mod.frameworkTypes.length === 0 ? (
                    <Box sx={{ px: 1.5, py: 1 }}>
                      <Typography sx={{ fontSize: '0.6875rem', color: brand.muted }}>
                        {mod.code === 'AUDIT'
                          ? 'Audit and CAPA come with any licensed module and turn view-only when every licence has expired. No framework library for this module.'
                          : 'No standard frameworks for this module in the current pack set.'}
                      </Typography>
                    </Box>
                  ) : (
                    <Table
                      size="small"
                      sx={{
                        width: '100%',
                        tableLayout: 'fixed',
                        '& .MuiTableCell-root': {
                          borderColor: brand.hairline,
                          py: 0.55,
                          px: 1,
                          fontSize: '0.75rem',
                          verticalAlign: 'middle',
                        },
                        '& .MuiTableCell-head': {
                          fontWeight: 700,
                          fontSize: '0.6875rem',
                          letterSpacing: '0.06em',
                          textTransform: 'uppercase',
                          color: brand.muted,
                          bgcolor: `${brand.paper} !important`,
                          background: `${brand.paper} !important`,
                          py: 0.5,
                          borderBottom: `1px solid ${brand.hairline}`,
                        },
                        '& .MuiTableBody .MuiTableRow:last-child .MuiTableCell-root': {
                          borderBottom: 0,
                        },
                      }}
                    >
                      <TableHead>
                        <TableRow>
                          {moduleSelected ? (
                            <TableCell padding="checkbox" sx={{ width: 40, pl: 1 }} />
                          ) : null}
                          <TableCell sx={{ ...primaryTableCellSx, pl: moduleSelected ? 0 : 1.5 }}>
                            Framework
                          </TableCell>
                          <TableCell sx={compactTableCellSx}>Status</TableCell>
                          <TableCell align="right" sx={compactTableCellSxWidth('5rem')}>Clauses</TableCell>
                          <TableCell align="right" sx={compactTableCellSxWidth('5.5rem')}>Annexures</TableCell>
                          <TableCell align="right" sx={compactTableCellSxWidth('5rem')}>Controls</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {mod.frameworkTypes.flatMap((t) => {
                          const versions = mod.imported.filter((f) => f.code === t.code)
                          const latest = versions[0]
                          const entitledIds = new Set(mod.license?.entitledFrameworkIds ?? [])
                          const pickedIds = pickedIdSet
                          const published = versions.filter((f) => f.status === 'PUBLISHED')
                          const displayRows =
                            published.length > 0 ? published : latest ? [latest] : []

                          const nameCell = (primary: boolean) => (
                            <TableCell sx={{ ...primaryTableCellSx, pl: moduleSelected ? 0 : 1.5 }}>
                              <Typography
                                title={t.code}
                                sx={{
                                  fontWeight: primary ? 600 : 500,
                                  fontSize: '0.75rem',
                                  color: primary ? brand.ink : brand.muted,
                                  lineHeight: 1.3,
                                  letterSpacing: '-0.005em',
                                }}
                              >
                                {primary ? t.name : 'Additional library entry'}
                              </Typography>
                            </TableCell>
                          )

                          const numCell = (v: number | string | null | undefined) => (
                            <TableCell align="right" sx={compactTableCellSx}>
                              <Typography
                                sx={{
                                  fontSize: '0.75rem',
                                  fontWeight: 550,
                                  color: brand.ink,
                                  fontVariantNumeric: 'tabular-nums',
                                  fontFamily: MONO,
                                }}
                              >
                                {v == null || v === '' ? '-' : v}
                              </Typography>
                            </TableCell>
                          )

                          if (displayRows.length === 0) {
                            return [(
                              <TableRow key={t.code} hover>
                                {moduleSelected ? (
                                  <TableCell padding="checkbox" sx={{ pl: 1 }}>
                                    <Checkbox size="small" disabled checked={false} sx={{ p: 0.25 }} />
                                  </TableCell>
                                ) : null}
                                {nameCell(true)}
                                <TableCell>
                                  <Stack direction="row" spacing={0.75} alignItems="center">
                                    <Typography sx={{ fontSize: '0.6875rem', color: brand.muted }}>
                                      Not imported
                                    </Typography>
                                    <Button
                                      size="small"
                                      variant="text"
                                      sx={{ minWidth: 0, px: 0.5, fontSize: '0.6875rem', minHeight: 24 }}
                                      onClick={() => void openBundleImport()}
                                    >
                                      Import
                                    </Button>
                                  </Stack>
                                </TableCell>
                                {numCell(null)}
                                {numCell(null)}
                                {numCell(null)}
                              </TableRow>
                            )]
                          }

                          return displayRows.map((f, idx) => (
                            <TableRow
                              key={`${t.code}-${f.id}`}
                              hover
                              sx={{ cursor: 'pointer' }}
                              onClick={() => void viewFramework(f.id)}
                            >
                              {moduleSelected ? (
                                <TableCell
                                  padding="checkbox"
                                  sx={{ pl: 1 }}
                                  onClick={(e) => e.stopPropagation()}
                                >
                                  {f.status === 'PUBLISHED' ? (
                                    <Checkbox
                                      size="small"
                                      disabled={licenseBusy}
                                      checked={pickedIds.has(f.id)}
                                      onMouseDown={(e) => e.stopPropagation()}
                                      onClick={(e) => e.stopPropagation()}
                                      onChange={(e) => {
                                        e.stopPropagation()
                                        toggleFrameworkInSelection(mod.code, f.id, e.target.checked)
                                      }}
                                      inputProps={{
                                        'aria-label': `Entitle ${t.code} for ${licenseTenant}`,
                                      }}
                                      sx={{ p: 0.25 }}
                                    />
                                  ) : (
                                    <Checkbox size="small" disabled checked={false} sx={{ p: 0.25 }} />
                                  )}
                                </TableCell>
                              ) : null}
                              {nameCell(idx === 0)}
                              <TableCell>
                                <Stack direction="row" spacing={0.4} alignItems="center" flexWrap="wrap" useFlexGap>
                                  <StatusChip
                                    label={f.status === 'PUBLISHED' ? 'Published' : f.status}
                                    tone={frameworkStatusTone(f.status)}
                                    sx={{ height: 18, fontSize: '0.6rem', fontWeight: 600, '& .MuiChip-label': { px: 0.6 } }}
                                  />
                                  {licenseTenant && mod.license?.enabled && f.status === 'PUBLISHED' && (
                                    <StatusChip
                                      label={entitledIds.has(f.id) ? 'Entitled' : 'Not entitled'}
                                      tone={entitledIds.has(f.id) ? 'success' : 'neutral'}
                                      sx={{ height: 18, fontSize: '0.6rem', fontWeight: 600, '& .MuiChip-label': { px: 0.6 } }}
                                    />
                                  )}
                                </Stack>
                              </TableCell>
                              {numCell(f.clauseCount)}
                              {numCell(f.annexControlCount)}
                              {numCell(f.controlCount)}
                            </TableRow>
                          ))
                        })}
                      </TableBody>
                    </Table>
                  )}
                </Card>
              )})}
            </Stack>
          </Box>
        </Stack>
      )}

      <Menu anchorEl={menuEl} open={!!menuEl} onClose={closeRowMenu}>
        <MenuItem onClick={goManageModules}>
          <ListItemIcon><ViewModuleRoundedIcon fontSize="small" /></ListItemIcon>
          <ListItemText>Manage Modules</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => copyEntryUrl()}>
          <ListItemIcon><ContentCopyRoundedIcon fontSize="small" /></ListItemIcon>
          <ListItemText>Copy SSO Entry URL</ListItemText>
        </MenuItem>
        <MenuItem onClick={goConfigureSso}>
          <ListItemIcon><TuneRoundedIcon fontSize="small" /></ListItemIcon>
          <ListItemText>Configure SSO</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => void resendOwnerActivation()} disabled={activationBusy}>
          <ListItemIcon>
            {activationBusy ? <CircularProgress size={16} /> : <ForwardToInboxRoundedIcon fontSize="small" />}
          </ListItemIcon>
          <ListItemText
            primary={activationBusy ? 'Sending…' : 'Resend owner activation'}
            secondary="For an Owner who has not set a password yet"
          />
        </MenuItem>
        <Divider />
        <MenuItem disabled>
          <ListItemIcon><BlockRoundedIcon fontSize="small" /></ListItemIcon>
          <ListItemText primary="Suspend tenant" secondary="Not available yet" />
        </MenuItem>
      </Menu>

      <Dialog
        open={Boolean(fwAssignDialog)}
        onClose={() => !fwBusy && setFwAssignDialog(null)}
        fullWidth
        maxWidth="xs"
      >
        <DialogTitle sx={{ pb: 1 }}>
          {fwAssignDialog?.enable ? 'Activate Framework for Client' : 'Deactivate Framework for Client'}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={1.5} sx={{ pt: 0.5 }}>
            <Typography variant="body2" color="text.secondary">
              {fwAssignDialog?.enable
                ? 'Activates this published framework for the client and entitles this framework only (not every framework under the module).'
                : 'Deactivates this framework for the client. They can activate it again later if the module stays licensed and this framework stays entitled.'}
            </Typography>
            <Box
              sx={{
                p: 1.5,
                borderRadius: 1,
                border: `1px solid ${brand.hairline}`,
                bgcolor: 'rgba(11,18,32,0.03)',
              }}
            >
              <Typography variant="caption" color="text.secondary" display="block">Framework</Typography>
              <Typography variant="body2" fontWeight={650}>
                {fwDetail?.framework.name}
                {fwDetail?.framework.moduleCode ? ` · ${fwDetail.framework.moduleCode}` : ''}
              </Typography>
              <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 1 }}>Client</Typography>
              <Typography variant="body2" fontWeight={650}>
                {fwAssignDialog?.tenantId}: {fwAssignDialog?.tenantName}
              </Typography>
              {fwAssignDialog?.enable && (
                <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 1.25 }}>
                  {fwAssignDialog.moduleLicensed
                    ? `Module ${fwDetail?.framework.moduleCode} is already licensed. Activating entitles this framework only.`
                    : `Module ${fwDetail?.framework.moduleCode} is not licensed: required before activation.`}
                </Typography>
              )}
            </Box>
            {fwAssignDialog?.enable && !fwAssignDialog.moduleLicensed && (
              <FormControlLabel
                control={
                  <Switch
                    size="small"
                    checked={fwLicenseModule}
                    onChange={(e) => setFwLicenseModule(e.target.checked)}
                  />
                }
                label={
                  <Typography variant="body2">
                    Also license module <strong>{fwDetail?.framework.moduleCode}</strong> and entitle
                    this framework only
                  </Typography>
                }
              />
            )}
            {fwAssignDialog?.enable && !fwAssignDialog.moduleLicensed && fwLicenseModule && (
              <CompactTextField
                label="Module licence expiry"
                type="date"
                required
                value={fwLicenseExpiry}
                onChange={(e) => setFwLicenseExpiry(e.target.value)}
                slotProps={{ htmlInput: { min: todayYmd() } }}
              />
            )}
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button variant="text" onClick={() => setFwAssignDialog(null)} disabled={fwBusy}>
            Cancel
          </Button>
          <Button
            variant="contained"
            color={fwAssignDialog?.enable ? 'primary' : 'error'}
            onClick={proceedAssignToConfirm}
            disabled={
              fwBusy
              || Boolean(
                fwAssignDialog?.enable
                && !fwAssignDialog.moduleLicensed
                && !fwLicenseModule,
              )
            }
          >
            Continue
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={Boolean(fwAssignConfirm)}
        onClose={() => !fwBusy && setFwAssignConfirm(null)}
        fullWidth
        maxWidth="xs"
      >
        <DialogTitle sx={{ pb: 1 }}>
          {fwAssignConfirm?.enable ? 'Confirm activate?' : 'Confirm deactivate?'}
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary">
            {fwAssignConfirm?.enable ? (
              <>
                Activate <strong>{fwDetail?.framework.name}</strong> for{' '}
                <strong>{fwAssignConfirm?.tenantId}</strong>
                {fwAssignConfirm.licenseModule
                  ? ` and license module ${fwDetail?.framework.moduleCode} with this framework only`
                  : ''}
                ?
              </>
            ) : (
              <>
                Deactivate <strong>{fwDetail?.framework.name}</strong> for{' '}
                <strong>{fwAssignConfirm?.tenantId}</strong>?
              </>
            )}
          </Typography>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button
            variant="text"
            disabled={fwBusy}
            onClick={() => {
              if (fwAssignConfirm) {
                setFwLicenseModule(fwAssignConfirm.licenseModule)
                setFwAssignDialog({
                  tenantId: fwAssignConfirm.tenantId,
                  tenantName: fwAssignConfirm.tenantName,
                  enable: fwAssignConfirm.enable,
                  moduleLicensed: fwAssignConfirm.moduleLicensed,
                })
              }
              setFwAssignConfirm(null)
            }}
          >
            Back
          </Button>
          <Button
            variant="contained"
            color={fwAssignConfirm?.enable ? 'primary' : 'error'}
            onClick={runAssignConfirm}
            disabled={fwBusy}
          >
            {fwBusy
              ? 'Working…'
              : fwAssignConfirm?.enable
                ? 'Confirm Activate'
                : 'Confirm Deactivate'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={Boolean(fwConfirm)}
        onClose={() => !fwBusy && setFwConfirm(null)}
        fullWidth
        maxWidth="xs"
      >
        <DialogTitle sx={{ pb: 1 }}>
          {fwConfirm?.action === 'validate' && 'Validate framework?'}
          {fwConfirm?.action === 'publish' && 'Publish framework?'}
          {fwConfirm?.action === 'delete' && 'Delete framework?'}
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary">
            {fwConfirm?.action === 'validate' && (
              <>
                Mark <strong>{fwConfirm.label}</strong> as VALIDATED. To replace content from Excel later,
                delete this catalog framework first, then import again (re-import of the same code+version
                is blocked).
              </>
            )}
            {fwConfirm?.action === 'publish' && (
              <>Publish <strong>{fwConfirm.label}</strong> to tenants. After publish, this version becomes immutable.</>
            )}
            {fwConfirm?.action === 'delete' && (
              <>
                Permanently delete <strong>{fwConfirm.label}</strong> and its control tree so you can
                import that version again. This cannot be undone. If a client has activated it, open
                View / activate, choose Tenants, and Deactivate first. Delete is refused while it is activated.
              </>
            )}
          </Typography>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button variant="text" onClick={() => setFwConfirm(null)} disabled={fwBusy}>
            Cancel
          </Button>
          <Button
            variant="contained"
            color={fwConfirm?.action === 'delete' ? 'error' : 'primary'}
            onClick={runFrameworkConfirm}
            disabled={fwBusy}
          >
            {fwBusy
              ? 'Working…'
              : fwConfirm?.action === 'validate'
                ? 'Validate'
                : fwConfirm?.action === 'publish'
                  ? 'Publish'
                  : 'Delete'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={Boolean(fwBulkPublish?.length)}
        onClose={() => !fwBusy && setFwBulkPublish(null)}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle sx={{ pb: 1 }}>Publish all validated frameworks?</DialogTitle>
        <DialogContent>
          <Stack spacing={1.5}>
            <Typography variant="body2" color="text.secondary">
              Publishes every <strong>VALIDATED</strong> version in the library. After publish, each
              version is immutable and can be activated for client tenants.
            </Typography>
            <Box
              sx={{
                maxHeight: 220,
                overflow: 'auto',
                border: `1px solid ${brand.hairline}`,
                borderRadius: 1,
                bgcolor: 'rgba(11,18,32,0.03)',
              }}
            >
              <Stack divider={<Divider flexItem />}>
                {fwBulkPublish?.map((f) => (
                  <Box key={f.id} sx={{ px: 1.5, py: 1 }}>
                    <Typography variant="body2" fontWeight={650}>
                      {f.name}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {f.moduleCode ? `${f.moduleCode} · ` : ''}
                      {f.controlCount} controls
                    </Typography>
                  </Box>
                ))}
              </Stack>
            </Box>
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button variant="text" onClick={() => setFwBulkPublish(null)} disabled={fwBusy}>
            Cancel
          </Button>
          <Button
            variant="contained"
            color="primary"
            startIcon={<PublishRoundedIcon />}
            onClick={() => void runPublishAllValidated()}
            disabled={fwBusy}
          >
            {fwBusy
              ? 'Publishing…'
              : `Publish ${fwBulkPublish?.length ?? 0} framework${fwBulkPublish?.length === 1 ? '' : 's'}`}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={Boolean(moduleConfirm)}
        onClose={() => !licenseBusy && setModuleConfirm(null)}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle sx={{ pb: 1 }}>
          {moduleConfirm?.enabled ? 'License Module & Frameworks' : 'Remove module licence?'}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={1.5}>
            <Typography variant="body2" color="text.secondary">
              {moduleConfirm?.enabled ? (
                <>
                  Grant <strong>{MODULE_LABELS[moduleConfirm.code] ?? moduleConfirm.code}</strong> (
                  <code>{moduleConfirm.code}</code>) to <strong>{licenseTenant}</strong>. Tick the
                  published frameworks this tenant bought: one, some, or all. Nothing is pre-selected
                  on a new licence.
                </>
              ) : (
                <>
                  Remove{' '}
                  <strong>
                    {MODULE_LABELS[moduleConfirm?.code ?? ''] ?? moduleConfirm?.code}
                  </strong>{' '}
                  from <strong>{licenseTenant}</strong>? Framework entitlements for this module are
                  cleared; the tenant loses access until re-licensed.
                </>
              )}
            </Typography>
            {moduleConfirm?.enabled && (
              <CompactTextField
                label="Licence expiry"
                type="date"
                required
                value={moduleConfirm.expiresOn}
                onChange={(e) =>
                  setModuleConfirm((prev) => (prev ? { ...prev, expiresOn: e.target.value } : prev))
                }
                slotProps={{ htmlInput: { min: todayYmd() } }}
                helperText="Required. This module becomes view-only after this date until you extend it."
              />
            )}
            {moduleConfirm?.enabled && (
              <Box
                sx={{
                  border: `1px solid ${brand.border}`,
                  borderRadius: 1,
                  bgcolor: brand.goldLight,
                  p: 1.25,
                }}
              >
                {(() => {
                  const published = fwLibrary.filter(
                    (f) => f.moduleCode === moduleConfirm.code && f.status === 'PUBLISHED',
                  )
                  if (published.length === 0) {
                    return (
                      <Typography variant="body2" color="text.secondary">
                        No published frameworks under this module yet: module licence only.
                      </Typography>
                    )
                  }
                  return (
                    <Stack spacing={0.75}>
                      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
                        <Typography variant="subtitle2" fontWeight={700}>
                          Published frameworks
                        </Typography>
                        <Button
                          size="small"
                          variant="text"
                          onClick={() =>
                            setModuleConfirm((prev) =>
                              prev
                                ? { ...prev, frameworkIds: published.map((f) => f.id) }
                                : prev,
                            )
                          }
                        >
                          Select All
                        </Button>
                        <Button
                          size="small"
                          variant="text"
                          onClick={() =>
                            setModuleConfirm((prev) => (prev ? { ...prev, frameworkIds: [] } : prev))
                          }
                        >
                          Clear
                        </Button>
                      </Stack>
                      <Typography variant="caption" color="text.secondary">
                        Only ticked frameworks appear in the tenant library. Unticked stay hidden until
                        you entitle them.
                      </Typography>
                      {published.map((f) => (
                        <FormControlLabel
                          key={f.id}
                          control={
                            <Checkbox
                              size="small"
                              checked={moduleConfirm.frameworkIds.includes(f.id)}
                              onChange={() =>
                                setModuleConfirm((prev) => {
                                  if (!prev) return prev
                                  const on = prev.frameworkIds.includes(f.id)
                                  return {
                                    ...prev,
                                    frameworkIds: on
                                      ? prev.frameworkIds.filter((id) => id !== f.id)
                                      : [...prev.frameworkIds, f.id],
                                  }
                                })
                              }
                            />
                          }
                          label={
                            <Typography variant="body2">
                              {f.name}
                              {f.controlCount != null ? ` · ${f.controlCount} controls` : ''}
                            </Typography>
                          }
                        />
                      ))}
                    </Stack>
                  )
                })()}
              </Box>
            )}
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button variant="text" onClick={() => setModuleConfirm(null)} disabled={licenseBusy}>
            Cancel
          </Button>
          <Button
            variant="contained"
            color="primary"
            onClick={() => void runModuleConfirm()}
            disabled={
              licenseBusy ||
              Boolean(
                moduleConfirm?.enabled &&
                  (!moduleConfirm.expiresOn ||
                    (fwLibrary.some(
                      (f) => f.moduleCode === moduleConfirm.code && f.status === 'PUBLISHED',
                    ) &&
                      moduleConfirm.frameworkIds.length === 0)),
              )
            }
          >
            {licenseBusy
              ? 'Working…'
              : moduleConfirm?.enabled
                ? 'License'
                : 'Remove Licence'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={licenseAllConfirm}
        onClose={() => !licenseBusy && setLicenseAllConfirm(false)}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle sx={{ pb: 1 }}>License all modules?</DialogTitle>
        <DialogContent>
          <Stack spacing={1.5}>
            <Typography variant="body2" color="text.secondary">
              Grant every commercial module and every latest published framework under those modules
              (the full pack) to <strong>{licenseTenant}</strong>. To sell ISMS with only one or two
              frameworks, license that module and tick those frameworks instead. One expiry date
              applies to every module.
            </Typography>
            <CompactTextField
              label="Licence expiry for all modules"
              type="date"
              required
              value={licenseAllExpiry}
              onChange={(e) => setLicenseAllExpiry(e.target.value)}
              slotProps={{ htmlInput: { min: todayYmd() } }}
              helperText="Required. You can extend this later for one module or for all modules."
            />
            {unlicensedModuleCount < MODULE_CODES.length && (
              <Box sx={goldRevokePanelSx}>
                <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 0.75 }}>
                  Will enable {unlicensedModuleCount} module{unlicensedModuleCount === 1 ? '' : 's'}
                  {licensedModuleCount > 0
                    ? ` (${licensedModuleCount} already licensed)`
                    : ''}
                </Typography>
                <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
                  {MODULE_CODES.filter((code) => !licenses.find((m) => m.code === code)?.enabled).map(
                    (code) => (
                      <Chip key={code} size="small" label={code} variant="outlined" sx={goldModuleChipSx} />
                    ),
                  )}
                </Stack>
              </Box>
            )}
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button variant="text" onClick={() => setLicenseAllConfirm(false)} disabled={licenseBusy}>
            Cancel
          </Button>
          <Button
            variant="contained"
            color="primary"
            startIcon={<DoneAllRoundedIcon />}
            onClick={() => void runLicenseAll()}
            disabled={licenseBusy || !licenseAllExpiry}
          >
            {licenseBusy ? 'Working…' : `License All ${MODULE_CODES.length} modules`}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={Boolean(moduleBulkConfirm)}
        onClose={() => !licenseBusy && setModuleBulkConfirm(null)}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle sx={{ pb: 1 }}>
          {moduleBulkConfirm?.mode === 'revoke'
            ? `Revoke ${moduleBulkConfirm.codes.length} module licence${
                moduleBulkConfirm.codes.length === 1 ? '' : 's'
              }?`
            : `License ${moduleBulkConfirm?.codes.length ?? 0} selected module${
                moduleBulkConfirm?.codes.length === 1 ? '' : 's'
              } & frameworks`}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={1.5}>
            <Typography variant="body2" color="text.secondary">
              {moduleBulkConfirm?.mode === 'revoke' ? (
                <>
                  Remove access to the selected modules for <strong>{licenseTenant}</strong>.
                  Frameworks stay in the platform library but the tenant loses module access until
                  re-licensed.
                </>
              ) : (
                <>
                  Grant the selected modules to <strong>{licenseTenant}</strong>. For each module,
                  choose which published frameworks this tenant may activate (1, some, or all). One
                  expiry date applies to every selected module.
                </>
              )}
            </Typography>
            {moduleBulkConfirm?.mode === 'license' && (
              <CompactTextField
                label="Licence expiry for selected modules"
                type="date"
                required
                value={moduleBulkConfirm.expiresOn ?? ''}
                onChange={(e) =>
                  setModuleBulkConfirm((prev) => (prev ? { ...prev, expiresOn: e.target.value } : prev))
                }
                slotProps={{ htmlInput: { min: todayYmd() } }}
              />
            )}
            {moduleBulkConfirm?.mode === 'revoke' ? (
              <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
                {moduleBulkConfirm.codes.map((code) => (
                  <Chip
                    key={code}
                    size="small"
                    label={code}
                    variant="outlined"
                    sx={goldModuleChipSx}
                  />
                ))}
              </Stack>
            ) : (
              <Stack spacing={1.25}>
                {moduleBulkConfirm?.codes.map((code) => {
                  const published = fwLibrary.filter(
                    (f) => f.moduleCode === code && f.status === 'PUBLISHED',
                  )
                  const picked = moduleBulkConfirm.frameworkIdsByCode?.[code] ?? []
                  return (
                    <Box
                      key={code}
                      sx={{
                        border: `1px solid ${brand.border}`,
                        borderRadius: 1,
                        bgcolor: brand.goldLight,
                        p: 1.25,
                      }}
                    >
                      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap sx={{ mb: 0.75 }}>
                        <Typography variant="subtitle2" fontWeight={700} sx={{ fontFamily: MONO }}>
                          {code}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {MODULE_LABELS[code] ?? code}
                        </Typography>
                        {published.length > 0 && (
                          <>
                            <Button
                              size="small"
                              variant="text"
                              onClick={() =>
                                setModuleBulkConfirm((prev) =>
                                  prev
                                    ? {
                                        ...prev,
                                        frameworkIdsByCode: {
                                          ...prev.frameworkIdsByCode,
                                          [code]: published.map((f) => f.id),
                                        },
                                      }
                                    : prev,
                                )
                              }
                            >
                              Select All
                            </Button>
                            <Button
                              size="small"
                              variant="text"
                              onClick={() =>
                                setModuleBulkConfirm((prev) =>
                                  prev
                                    ? {
                                        ...prev,
                                        frameworkIdsByCode: {
                                          ...prev.frameworkIdsByCode,
                                          [code]: [],
                                        },
                                      }
                                    : prev,
                                )
                              }
                            >
                              Clear
                            </Button>
                          </>
                        )}
                      </Stack>
                      {published.length === 0 ? (
                        <Typography variant="body2" color="text.secondary">
                          No published frameworks: module licence only.
                        </Typography>
                      ) : (
                        <Stack spacing={0.25}>
                          {published.map((f) => (
                            <FormControlLabel
                              key={f.id}
                              sx={{ ml: 0, alignItems: 'flex-start' }}
                              control={
                                <Checkbox
                                  size="small"
                                  checked={picked.includes(f.id)}
                                  onChange={() => {
                                    setModuleBulkConfirm((prev) => {
                                      if (!prev) return prev
                                      const cur = prev.frameworkIdsByCode?.[code] ?? []
                                      const on = cur.includes(f.id)
                                      return {
                                        ...prev,
                                        frameworkIdsByCode: {
                                          ...prev.frameworkIdsByCode,
                                          [code]: on
                                            ? cur.filter((id) => id !== f.id)
                                            : [...cur, f.id],
                                        },
                                      }
                                    })
                                  }}
                                />
                              }
                              label={
                                <Box sx={{ pt: 0.35 }}>
                                  <Typography variant="body2" fontWeight={650}>
                                    {f.name || f.code}
                                  </Typography>
                                  <Typography variant="caption" color="text.secondary">
                                    {f.controlCount} controls
                                  </Typography>
                                </Box>
                              }
                            />
                          ))}
                        </Stack>
                      )}
                    </Box>
                  )
                })}
              </Stack>
            )}
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button variant="text" onClick={() => setModuleBulkConfirm(null)} disabled={licenseBusy}>
            Cancel
          </Button>
          <Button
            variant="contained"
            color="primary"
            startIcon={
              moduleBulkConfirm?.mode === 'revoke' ? <BlockRoundedIcon /> : <DoneAllRoundedIcon />
            }
            onClick={() => void runModuleBulkConfirm()}
            disabled={
              licenseBusy
              || !moduleBulkConfirm?.codes.length
              || (moduleBulkConfirm?.mode === 'license'
                && (!moduleBulkConfirm.expiresOn
                  || (moduleBulkConfirm.codes.some((code) => {
                  const published = fwLibrary.filter(
                    (f) => f.moduleCode === code && f.status === 'PUBLISHED',
                  )
                  const picked = moduleBulkConfirm.frameworkIdsByCode?.[code] ?? []
                  return published.length > 0 && picked.length === 0
                }) ?? false)))
            }
          >
            {licenseBusy
              ? 'Working…'
              : moduleBulkConfirm?.mode === 'revoke'
                ? `Revoke ${moduleBulkConfirm.codes.length} licence${
                    moduleBulkConfirm.codes.length === 1 ? '' : 's'
                  }`
                : `License ${moduleBulkConfirm?.codes.length ?? 0} module${
                    moduleBulkConfirm?.codes.length === 1 ? '' : 's'
                  }`}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={Boolean(extendDialog)}
        onClose={() => !licenseBusy && setExtendDialog(null)}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle sx={{ pb: 1 }}>
          {extendDialog?.codes === 'all'
            ? licensedWithDateCount > 0
              ? 'Reset Expiry for All Licensed Modules'
              : 'Set Expiry for All Licensed Modules'
            : extendDialog?.codes.length === 1
              ? `${
                  hasLicenceCalendarDate(
                    licenses.find((m) => m.code === extendDialog.codes[0]) ?? {},
                  )
                    ? 'Reset'
                    : 'Set'
                } expiry for ${extendDialog.codes[0]}`
              : 'Set Module Expiry'}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={1.5} sx={{ pt: 0.5 }}>
            <Typography variant="body2" color="text.secondary">
              {extendDialog?.codes === 'all'
                ? `Applies the same expiry to every licensed module for ${licenseTenant}, including Audit.`
                : 'Replaces the current commercial term. The tenant keeps access until the new date.'}
            </Typography>
            <CompactTextField
              label="Licence expiry"
              type="date"
              required
              value={extendDialog?.expiresOn ?? ''}
              onChange={(e) =>
                setExtendDialog((prev) => (prev ? { ...prev, expiresOn: e.target.value } : prev))
              }
              slotProps={{ htmlInput: { min: todayYmd() } }}
            />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button variant="text" onClick={() => setExtendDialog(null)} disabled={licenseBusy}>
            Cancel
          </Button>
          <Button
            variant="contained"
            color="primary"
            startIcon={<EventRepeatRoundedIcon />}
            onClick={() => void runExtendExpiry()}
            disabled={licenseBusy || !extendDialog?.expiresOn}
          >
            {licenseBusy ? 'Working…' : 'Save Expiry'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={revokeAllConfirm}
        onClose={() => !licenseBusy && setRevokeAllConfirm(false)}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle sx={{ pb: 1 }}>Revoke all module licences?</DialogTitle>
        <DialogContent>
          <Stack spacing={1.5}>
            <Typography variant="body2" color="text.secondary">
              Remove every commercial module licence from <strong>{licenseTenant}</strong>. The
              tenant loses access to all module features until modules are licensed again.
            </Typography>
            {licensedModuleCount > 0 && (
              <Box sx={goldRevokePanelSx}>
                <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 0.75 }}>
                  Will revoke {licensedModuleCount} module licence{licensedModuleCount === 1 ? '' : 's'}
                </Typography>
                <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
                  {licenses.filter((m) => m.enabled).map((m) => (
                    <Chip
                      key={m.code}
                      size="small"
                      label={m.code}
                      variant="outlined"
                      sx={goldModuleChipSx}
                    />
                  ))}
                </Stack>
              </Box>
            )}
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button variant="text" onClick={() => setRevokeAllConfirm(false)} disabled={licenseBusy}>
            Cancel
          </Button>
          <Button
            variant="outlined"
            color="primary"
            startIcon={<BlockRoundedIcon />}
            onClick={() => void runRevokeAll()}
            disabled={licenseBusy || licensedModuleCount === 0}
          >
            {licenseBusy ? 'Working…' : `Revoke All ${licensedModuleCount} licences`}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={Boolean(ssoSaveConfirm)}
        onClose={() => !idpBusy && setSsoSaveConfirm(false)}
        fullWidth
        maxWidth="xs"
      >
        <DialogTitle sx={{ pb: 1 }}>Save SSO settings?</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary">
            Update Azure AD / Entra ID configuration for <strong>{idpTenant}</strong>
            {ssoEnabled ? ' and enable SSO login' : ' (SSO will stay disabled)'}? Existing users can
            be affected if the issuer or client ID is wrong.
          </Typography>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button variant="text" onClick={() => setSsoSaveConfirm(false)} disabled={idpBusy}>
            Cancel
          </Button>
          <Button variant="contained" onClick={runSaveIdp} disabled={idpBusy}>
            {idpBusy ? 'Saving…' : 'Save SSO Settings'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={bundleOpen}
        onClose={() => !bundleBusy && setBundleOpen(false)}
        fullWidth
        maxWidth="sm"
        disableEscapeKeyDown={bundleBusy}
        data-testid="sa-framework-import-bundle-dialog"
      >
        <DialogTitle sx={{ pb: 1 }}>Import All Frameworks</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
            Imports every library workbook shipped with this app. No file or folder to pick.
            Already-imported code+version pairs are skipped with an error on that file. This can
            take several minutes.
          </Typography>
          {!bundleStatus?.directoryExists && (
            <Alert severity="warning" sx={{ mb: 1.5 }}>
              No shipped packs found in this build. Include the framework workbooks when packaging
              the app, or set ARBOR_FRAMEWORKS_DIR.
            </Alert>
          )}
          <Stack spacing={1.5} sx={{ pt: 0.5 }}>
            {bundleStatus && (
              <Typography variant="caption" color="text.secondary">
                {bundleStatus.packs.length} importable pack(s)
                {bundleStatus.skipped.length
                  ? ` · ${bundleStatus.skipped.length} skipped (BIA/forms/activity)`
                  : ''}
                {bundleStatus.directory ? ` · ${bundleStatus.directory}` : ''}
              </Typography>
            )}
            {bundleBusy && (
              <Box data-testid="sa-framework-import-wait">
                <LinearProgress
                  sx={{
                    mb: 1,
                    height: 8,
                    borderRadius: 1,
                    bgcolor: 'action.hover',
                    '& .MuiLinearProgress-bar': { bgcolor: brand.gold },
                  }}
                />
                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                  Importing shipped frameworks. Elapsed{' '}
                  {bundleElapsedSec >= 60
                    ? `${Math.floor(bundleElapsedSec / 60)}m ${bundleElapsedSec % 60}s`
                    : `${bundleElapsedSec}s`}
                </Typography>
                <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.5 }}>
                  Stay on this page. Do not close the dialog or refresh.
                </Typography>
              </Box>
            )}
            {bundleResult && (
              <Box sx={{ maxHeight: 280, overflow: 'auto', border: `1px solid ${brand.hairline}`, borderRadius: 1, p: 1 }}>
                <Typography variant="subtitle2" sx={{ mb: 0.75 }}>
                  Result: imported {bundleResult.importedFileCount}, errors {bundleResult.errorFileCount},
                  skipped {bundleResult.skippedFileCount}
                </Typography>
                {bundleResult.files.map((f) => (
                  <Typography key={`${f.step}-${f.relativePath}`} variant="caption" display="block" sx={{ mb: 0.35 }}>
                    [{f.status}] {f.relativePath}: {f.message}
                  </Typography>
                ))}
              </Box>
            )}
            {bundleFinished === 'dry-run' && !bundleBusy && (
              <Typography variant="caption" color="text.secondary">
                Dry-run is finished and cannot be run again in this dialog. Import all writes to the
                catalog. Close if you are done.
              </Typography>
            )}
            {bundleFinished === 'imported' && !bundleBusy && (
              <Typography variant="caption" color="text.secondary">
                Import is finished. Close this dialog. Open it again only if you need a new run.
              </Typography>
            )}
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button variant="text" onClick={() => setBundleOpen(false)} disabled={bundleBusy}>
            Close
          </Button>
          <Button
            variant="outlined"
            disabled={
              bundleBusy
              || !bundleStatus?.directoryExists
              || bundleFinished !== 'idle'
            }
            title={
              bundleFinished === 'imported'
                ? 'Import already finished. Close this dialog.'
                : bundleFinished === 'dry-run'
                  ? 'Dry-run already finished. Use Import all or Close.'
                  : undefined
            }
            onClick={() => void runBundleDryRun()}
            data-testid="sa-framework-import-bundle-dry-run"
          >
            {bundleBusy ? 'Working…' : 'Dry-run'}
          </Button>
          <BusyButton
            variant="contained"
            busy={bundleBusy}
            disabled={!bundleStatus?.directoryExists || bundleFinished === 'imported'}
            title={
              bundleFinished === 'imported' ? 'Import already finished. Close this dialog.' : undefined
            }
            onClick={() => void runBundleImport()}
            data-testid="sa-framework-import-bundle-confirm"
          >
            {bundleBusy ? 'Importing…' : bundleFinished === 'imported' ? 'All Imported' : 'Import All'}
          </BusyButton>
        </DialogActions>
      </Dialog>

      <Dialog
        open={importOpen}
        onClose={() => !fwBusy && !importPreviewBusy && setImportOpen(false)}
        fullWidth
        maxWidth="md"
        data-testid="sa-framework-import-dialog"
      >
        <DialogTitle sx={{ pb: 1 }}>Upload Pack Excel</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Secondary path: upload a single client pack workbook (e.g. ISO27001.xlsx). Layout is
            detected from sheet headers. Prefer <strong>Import all frameworks</strong> for the shipped pack set.
            Re-import of an existing code+version is blocked: delete the
            catalog framework first, then import again.
          </Typography>
          <Stack spacing={1.75} sx={{ pt: 0.5 }}>
            {SHOW_LEGACY_CHECKLIST_IMPORT_UI && (
              <>
                <FormSelect
                  label="Framework type (legacy templates)"
                  fullWidth
                  data-testid="sa-framework-import-type"
                  value={importType}
                  onChange={(code) => {
                    setImportType(code)
                    let nextVersion = ''
                    if (code) {
                      const t = fwTypes.find((x) => x.code === code)
                      if (t) nextVersion = t.defaultVersion
                      setImportVersion(nextVersion)
                    } else {
                      setImportVersion('')
                    }
                    setImportPreview(null)
                    if (importFile) {
                      void runImportPreview(importFile, {
                        typeCode: code || undefined,
                        version: code ? nextVersion || undefined : undefined,
                      })
                    }
                  }}
                  options={[
                    { value: '', label: 'Auto-detect / all in GRC Checklist' },
                    ...fwTypes.map((t) => ({
                      value: t.code,
                      label: `${t.name} (${t.moduleCode}${t.dedicatedParserExists ? ' · checklist' : ''})`,
                    })),
                  ]}
                />
                <CompactTextField
                  label="Version (legacy templates)"
                  value={importVersion}
                  onChange={(e) => {
                    const next = e.target.value
                    setImportVersion(next)
                    setImportPreview(null)
                    if (importFile && (!importType || next.trim())) {
                      void runImportPreview(importFile, {
                        typeCode: importType || undefined,
                        version: next.trim() || undefined,
                      })
                    }
                  }}
                  disabled={!importType}
                  inputProps={{ 'data-testid': 'sa-framework-import-version' }}
                  helperText={
                    importType
                      ? 'Required for Arbor Controls; defaults per type for GRC Checklist'
                      : 'Optional: pack Excel version is read from the workbook when auto-detected'
                  }
                />
                <Button
                  variant="text"
                  size="small"
                  startIcon={<DownloadRoundedIcon />}
                  disabled={!importType || fwBusy}
                  onClick={() =>
                    importType && api.downloadFrameworkSample(importType).catch((e) => setError(String(e)))
                  }
                  sx={{ alignSelf: 'flex-start' }}
                >
                  Download Arbor Sample Excel
                </Button>
              </>
            )}
            <Stack
              direction="row"
              alignItems="center"
              spacing={1.25}
              flexWrap="wrap"
              useFlexGap
              sx={{ alignSelf: 'flex-start' }}
            >
              <Button
                variant="outlined"
                size="small"
                component="label"
                startIcon={<UploadFileRoundedIcon />}
                data-testid="sa-framework-import-choose"
                disabled={fwBusy || importPreviewBusy}
                sx={{
                  flex: '0 0 auto',
                  px: 1.5,
                  borderColor: brand.hairlineStrong,
                  color: brand.accentHover,
                  '&:hover': { borderColor: brand.accent, bgcolor: brand.accentMuted },
                }}
              >
                Choose .xlsx
                <input
                  type="file"
                  hidden
                  data-testid="sa-framework-import-file"
                  accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                  onChange={(e) => handleImportFileChange(e.target.files?.[0] ?? null)}
                />
              </Button>
              <Typography
                variant="body2"
                color={importFile ? 'text.primary' : 'text.secondary'}
                sx={{
                  fontFamily: importFile ? MONO : undefined,
                  fontSize: importFile ? '0.75rem' : undefined,
                  maxWidth: 360,
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                }}
                title={importFile?.name}
              >
                {importFile ? importFile.name : 'No file selected'}
              </Typography>
            </Stack>

            {importPreviewBusy && (
              <Typography variant="body2" color="text.secondary" data-testid="sa-framework-import-preview-busy">
                Running dry-run validation…
              </Typography>
            )}

            {importPreview && importPreview.length > 0 && (
              <Stack spacing={1.25} sx={{ mt: 0.5 }} data-testid="sa-framework-import-preview">
                <Typography variant="subtitle2">Preview: nothing written yet</Typography>
                {importPreview.map((line, idx) => (
                  <ImportReportLineCard key={`${line.frameworkCode ?? line.sheetName}-${idx}`} line={line} />
                ))}
              </Stack>
            )}

            {importPreview && importPreview.length === 0 && !importPreviewBusy && (
              <Alert severity="info" sx={{ mt: 0.5 }}>
                Dry-run completed with no report lines: check the upload format.
              </Alert>
            )}
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button
            variant="text"
            onClick={() => setImportOpen(false)}
            disabled={fwBusy || importPreviewBusy}
          >
            Cancel
          </Button>
          <Button
            variant="contained"
            onClick={runImport}
            data-testid="sa-framework-import-confirm"
            disabled={
              fwBusy ||
              importPreviewBusy ||
              !importFile ||
              !importPreview ||
              (!!importType && !importVersion.trim())
            }
          >
            {fwBusy ? 'Importing…' : 'Confirm Import'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={provisionOpen}
        onClose={() => !provisioning && setProvisionOpen(false)}
        fullWidth
        maxWidth="sm"
        disableEscapeKeyDown={provisioning}
      >
        <DialogTitle sx={{ pb: 1 }}>Provision a Client Tenant</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Creates an isolated database and a bootstrap local admin for the workspace.
          </Typography>
          <Stack spacing={1.75} sx={{ pt: 0.5 }}>
            <CompactTextField
              label="Tenant ID"
              autoFocus
              helperText={
                newId.trim()
                  ? `Saved as lowercase: ${newId.trim().toLowerCase()}`
                  : 'Letters, digits, and underscore only. Mixed case is accepted and stored lowercase.'
              }
              value={newId}
              onChange={(e) => setNewId(e.target.value)}
              disabled={provisioning || !!provisioned}
            />
            <CompactTextField
              label="Organisation name"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              disabled={provisioning || !!provisioned}
            />
            <CompactTextField
              label="Owner email"
              type="email"
              required
              value={ownerEmail}
              onChange={(e) => setOwnerEmail(e.target.value)}
              disabled={provisioning || !!provisioned}
            />
            <CompactTextField
              label="Owner name"
              required
              value={ownerName}
              onChange={(e) => setOwnerName(e.target.value)}
              disabled={provisioning || !!provisioned}
            />
            <FormControlLabel
              control={
                <Switch
                  size="small"
                  checked={provisionSso}
                  onChange={(_, checked) => setProvisionSso(checked)}
                  disabled={provisioning || !!provisioned}
                />
              }
              label="Configure SSO now (Owner signs in with Entra first)"
            />
            {provisionSso && (
              <>
                <CompactTextField
                  label="OIDC issuer"
                  required
                  value={provisionIssuer}
                  onChange={(e) => setProvisionIssuer(e.target.value)}
                  disabled={provisioning || !!provisioned}
                />
                <CompactTextField
                  label="OIDC client id"
                  required
                  value={provisionClientId}
                  onChange={(e) => setProvisionClientId(e.target.value)}
                  disabled={provisioning || !!provisioned}
                />
                <CompactTextField
                  label="OIDC client secret"
                  required
                  showPasswordToggle
                  value={provisionClientSecret}
                  onChange={(e) => setProvisionClientSecret(e.target.value)}
                  disabled={provisioning || !!provisioned}
                />
              </>
            )}
            {provisioning && (
              <Box sx={{ pt: 0.5 }} data-testid="sa-provision-wait">
                <LinearProgress
                  variant="determinate"
                  value={Math.min(92, ((provisionWaitIdx + 1) / PROVISION_STEPS.length) * 90)}
                  sx={{
                    mb: 1.5,
                    height: 8,
                    borderRadius: 1,
                    bgcolor: 'action.hover',
                    '& .MuiLinearProgress-bar': { bgcolor: brand.gold },
                  }}
                />
                <Typography variant="body2" sx={{ fontWeight: 600, mb: 0.75 }}>
                  Setting up the workspace. Elapsed{' '}
                  {provisionElapsedSec >= 60
                    ? `${Math.floor(provisionElapsedSec / 60)}m ${provisionElapsedSec % 60}s`
                    : `${provisionElapsedSec}s`}
                </Typography>
                <Stack spacing={0.5} component="ol" sx={{ m: 0, pl: 2.25 }}>
                  {PROVISION_STEPS.map((label, i) => (
                    <Typography
                      key={label}
                      component="li"
                      variant="body2"
                      sx={{
                        fontWeight: i === provisionWaitIdx ? 600 : 400,
                        color: i < provisionWaitIdx ? 'text.secondary' : 'text.primary',
                      }}
                    >
                      {i < provisionWaitIdx ? 'Done: ' : i === provisionWaitIdx ? 'In progress: ' : ''}
                      {label}
                    </Typography>
                  ))}
                </Stack>
                <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 1 }}>
                  Do not close this window or refresh. Creating the database on Azure can take several minutes.
                </Typography>
              </Box>
            )}
            {provisioned && (
              <Alert severity="success">
                <Typography variant="subtitle2">Tenant “{provisioned.tenant.tenantId}” created</Typography>
                <Divider sx={{ my: 1 }} />
                <Typography variant="body2">Owner: <b>{provisioned.initialAdminEmail}</b></Typography>
                {provisioned.ssoConfigured ? (
                  <Typography variant="body2" sx={{ mt: 0.5 }}>
                    First sign-in is SSO at /sso/{provisioned.tenant.tenantId}. No password was created.
                  </Typography>
                ) : provisioned.passwordEmailed || !provisioned.initialAdminPassword ? (
                  <Typography variant="body2" sx={{ mt: 0.5 }}>
                    An activation link was emailed to the Owner. It is valid for 24 hours. Resend it from the tenant row menu if needed.
                  </Typography>
                ) : (
                  <>
                    <Typography variant="body2" sx={{ mt: 0.5 }}>
                      Password:{' '}
                      <Chip
                        size="small"
                        label={
                          provisionPasswordVisible
                            ? provisioned.initialAdminPassword
                            : '••••••••'
                        }
                        onClick={() => setProvisionPasswordVisible((v) => !v)}
                        sx={{ cursor: 'pointer' }}
                      />
                    </Typography>
                    <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 1 }}>
                      Shown once on this screen because email is not live here.
                    </Typography>
                  </>
                )}
              </Alert>
            )}
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button variant="text" onClick={() => setProvisionOpen(false)} disabled={provisioning}>
            {provisioned ? 'Close' : 'Cancel'}
          </Button>
          {!provisioned && (
            <BusyButton
              variant="contained"
              onClick={provision}
              busy={provisioning}
              disabled={
                !newId.trim() ||
                !newName.trim() ||
                !ownerEmail.trim() ||
                !ownerName.trim() ||
                (provisionSso &&
                  (!provisionIssuer.trim() || !provisionClientId.trim() || !provisionClientSecret.trim()))
              }
            >
              {provisioning ? 'Provisioning…' : 'Provision Tenant'}
            </BusyButton>
          )}
        </DialogActions>
      </Dialog>

      <Snackbar open={!!error} autoHideDuration={5000} onClose={() => setError('')} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
        <Alert severity="error" onClose={() => setError('')}>{error}</Alert>
      </Snackbar>
      <Snackbar open={!!ok} autoHideDuration={3000} onClose={() => setOk('')} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
        <Alert severity="success" onClose={() => setOk('')}>{ok}</Alert>
      </Snackbar>
    </AppShell>
  )
}
