import { useCallback, useEffect, useMemo, useState, type MouseEvent, type ReactNode } from 'react'
import {
  Alert,
  Avatar,
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  FormGroup,
  IconButton,
  Menu,
  MenuItem,
  Snackbar,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import SpaceDashboardRoundedIcon from '@mui/icons-material/SpaceDashboardRounded'
import GroupRoundedIcon from '@mui/icons-material/GroupRounded'
import FactCheckRoundedIcon from '@mui/icons-material/FactCheckRounded'
import AssignmentTurnedInRoundedIcon from '@mui/icons-material/AssignmentTurnedInRounded'
import SecurityRoundedIcon from '@mui/icons-material/SecurityRounded'
import HowToRegRoundedIcon from '@mui/icons-material/HowToRegRounded'
import WarningAmberRoundedIcon from '@mui/icons-material/WarningAmberRounded'
import BusinessCenterRoundedIcon from '@mui/icons-material/BusinessCenterRounded'
import HandshakeRoundedIcon from '@mui/icons-material/HandshakeRounded'
import GavelRoundedIcon from '@mui/icons-material/GavelRounded'
import HealthAndSafetyRoundedIcon from '@mui/icons-material/HealthAndSafetyRounded'
import PrivacyTipRoundedIcon from '@mui/icons-material/PrivacyTipRounded'
import DatasetRoundedIcon from '@mui/icons-material/DatasetRounded'
import ListAltRoundedIcon from '@mui/icons-material/ListAltRounded'
import PersonSearchRoundedIcon from '@mui/icons-material/PersonSearchRounded'
import Inventory2RoundedIcon from '@mui/icons-material/Inventory2Rounded'
import FolderCopyRoundedIcon from '@mui/icons-material/FolderCopyRounded'
import ApartmentRoundedIcon from '@mui/icons-material/ApartmentRounded'
import MoreVertRoundedIcon from '@mui/icons-material/MoreVertRounded'
import HistoryRoundedIcon from '@mui/icons-material/HistoryRounded'
import AccountTreeRoundedIcon from '@mui/icons-material/AccountTreeRounded'
import ArticleRoundedIcon from '@mui/icons-material/ArticleRounded'
import AssessmentRoundedIcon from '@mui/icons-material/AssessmentRounded'
import SettingsRoundedIcon from '@mui/icons-material/SettingsRounded'
import NotificationsActiveRoundedIcon from '@mui/icons-material/NotificationsActiveRounded'
import SchemaRoundedIcon from '@mui/icons-material/SchemaRounded'
import PolicyRoundedIcon from '@mui/icons-material/PolicyRounded'
import FitnessCenterRoundedIcon from '@mui/icons-material/FitnessCenterRounded'
import TimelineRoundedIcon from '@mui/icons-material/TimelineRounded'
import FlagRoundedIcon from '@mui/icons-material/FlagRounded'
import SpeedRoundedIcon from '@mui/icons-material/SpeedRounded'
import ReportProblemRoundedIcon from '@mui/icons-material/ReportProblemRounded'
import AssignmentRoundedIcon from '@mui/icons-material/AssignmentRounded'
import TaskAltRoundedIcon from '@mui/icons-material/TaskAltRounded'
import AppShell, { type NavItem } from './components/AppShell'
import {
  AppShellPageProvider,
  type AppShellPageConfig,
} from './components/AppShellPageContext'
import { ModuleListShell } from './hooks/useModulePageShell'
import FrameworkViewerDialog from './components/FrameworkViewerDialog'
import NotificationBell from './components/NotificationBell'
import StatusChip from './components/StatusChip'
import LicenceTermBadge from './components/license/LicenceTermBadge'
import EvidencePage from './EvidencePage'
import AssessmentsPage from './AssessmentsPage'
import AssetsPage from './AssetsPage'
import RiskPage from './RiskPage'
import PrivacyRiskPage from './PrivacyRiskPage'
import type { RiskHeatMapDrill } from './RiskHeatMap'
import type { RiskType } from './api'
import TprmPage from './TprmPage'
import TprmProjectsPage from './TprmProjectsPage'
import TprmQuestionnairesPage from './TprmQuestionnairesPage'
import AuditChecklistsPage from './AuditChecklistsPage'
import KriPage from './KriPage'
import IncidentsPage from './IncidentsPage'
import PrivacyPage from './PrivacyPage'
import AuditsPage from './AuditsPage'
import {
  AUDIT_NAV_CAPA,
  AUDIT_NAV_REGISTER,
  isAuditNavId,
} from './components/audit/auditNav'
import { isBcmsNavPending } from './components/bcms/bcmsNav'
import { isPrivacyNavPending } from './components/privacy/privacyNav'
import { isRiskNavHidden } from './components/risk/riskNav'
import {
  defaultAuditHomeNav,
  filterNavForAuditor,
  isAuditorConsole,
} from './components/audit/auditRoles'
import BcmsDeptProfilesPage from './BcmsDeptProfilesPage'
import BcmsBiaPage from './BcmsBiaPage'
import BcmsRecoveryPlansPage from './BcmsRecoveryPlansPage'
import BcmsExercisesPage from './BcmsExercisesPage'
import PoliciesPage from './PoliciesPage'
import OverviewDashboard from './OverviewDashboard'
import WorkspaceBootScreen from './components/login/WorkspaceBootScreen'
import ReportsPage from './ReportsPage'
import LicensedModulePage, {
  parseLicensedModuleActive,
} from './LicensedModulePage'
import GuidedSetupWizard from './GuidedSetupWizard'
import { frameworkDisplayName } from './uiQuality/frameworkLabel'
import OrganisationPage from './OrganisationPage'
import MfaSettingsCard from './components/MfaSettingsCard'
import WorkflowPage from './WorkflowPage'
import {
  api,
  ApiError,
  ASSIGNABLE_ROLES,
  ASSESSMENT_MODULES,
  FRAMEWORK_MODULES,
  SOA_MODULES,
  MODULE_LABELS,
  type AuditLogEntry,
  type FrameworkDetail,
  type FrameworkSummary,
  type ModuleLicense,
  type LicenseBanner,
  type EmployeeDirectoryEntry,
  type OrgUnitNode,
  type Session,
  type UserSummary,
  type DashboardSummary,
  type NotificationItem,
} from './api'
import { resolveAuditNotificationTarget } from './notifications/auditNotificationTarget'
import { resolveCapaNotificationTarget } from './notifications/capaNotificationTarget'
import { resolveGapNotificationTarget } from './notifications/gapNotificationTarget'
import { resolveRiskNotificationTarget } from './notifications/riskNotificationTarget'
import { brand } from './theme'
import { emailError, useFormFields } from './validation'
import { productErrorMessage } from './uiQuality/productError'
import { roleLabel, roleLabels } from './uiQuality/roleLabels'
import AdminModulePicker from './components/users/AdminModulePicker'
import UserAccessCell from './components/users/UserAccessCell'
import DepartmentPicker from './components/users/DepartmentPicker'
import { navIdToTitle, setArborTitle } from './uiQuality/pageTitle'
import { readNavParam, writeNavParam } from './uiQuality/navUrl'
import { licenceTermView, moduleWriteLockReason } from './licenseExpiry'
import { formatTenantDateTime } from './uiQuality/tenantLocale'
import { usePagedRows } from './uiQuality/usePagedRows'
import { useListReady } from './uiQuality/useListReady'
import { useTenantLocale } from './uiQuality/TenantLocaleContext'
import {
  UnsavedChangesGuardProvider,
  useUnsavedChangesGuardApi,
} from './uiQuality/UnsavedChangesGuard'
import SsoInviteEmailField from './components/iam/SsoInviteEmailField'
import {
  BusyButton,
  CompactTextField,
  FormSelect,
  RegisterListFrame,
  RegisterToolbar,
  twoColFieldSx,
  AttentionBanner,
  registerTableSx,
  primaryTableCellSx,
  primaryRegisterNameSx,
  compactTableCellSx,
  lastTableCellSx,
  compactTableCellSxWidth,
} from './components/ui'

/** module / anyModules = commercial licence required; omit for Platform always-on. soon = not built yet.
 * Order: Dashboard → ISMS core → risk/audit/BC/privacy → platform docs → admin (Settings last). */
/** Generic workflow engine UI: hidden from sidebar until wired to real modules. */
const SHOW_WORKFLOWS_IN_NAV = false
const WORKFLOWS_NAV_ID = 'workflows'

const NAV: NavItem[] = [
  { id: 'overview', label: 'Dashboard', icon: <SpaceDashboardRoundedIcon fontSize="small" /> },
  {
    id: 'frameworks',
    label: 'Frameworks',
    icon: <AccountTreeRoundedIcon fontSize="small" />,
    anyModules: [...FRAMEWORK_MODULES],
  },
  {
    id: 'compliance',
    label: 'Compliance Assessment',
    icon: <AssignmentTurnedInRoundedIcon fontSize="small" />,
    anyModules: [...ASSESSMENT_MODULES],
    children: [
      {
        id: 'assessments-projects',
        label: 'Projects',
        icon: <AssignmentRoundedIcon fontSize="small" />,
        anyModules: [...ASSESSMENT_MODULES],
      },
      {
        id: 'assessments',
        label: 'Gap Assessments',
        icon: <AssignmentTurnedInRoundedIcon fontSize="small" />,
        anyModules: [...ASSESSMENT_MODULES],
      },
      {
        id: 'soa',
        label: 'Statement of Applicability',
        icon: <FactCheckRoundedIcon fontSize="small" />,
        anyModules: [...SOA_MODULES],
      },
    ],
  },
  { id: 'assets', label: 'Assets', icon: <Inventory2RoundedIcon fontSize="small" />, module: 'ISMS' },
  {
    id: 'risk-manager',
    label: 'Risk Management',
    icon: <SecurityRoundedIcon fontSize="small" />,
    anyModules: ['ISMS', 'ERM'],
    children: [
      {
        id: 'is-risk-nav',
        label: 'Information Security Risk',
        icon: <WarningAmberRoundedIcon fontSize="small" />,
        module: 'ISMS',
        children: [
          {
            id: 'risk',
            label: 'Projects',
            icon: <AssignmentRoundedIcon fontSize="small" />,
            module: 'ISMS',
          },
          {
            id: 'risk-acceptance',
            label: 'Risk Acceptance',
            icon: <HowToRegRoundedIcon fontSize="small" />,
            module: 'ISMS',
          },
        ],
      },
      {
        id: 'erm-risk-nav',
        label: 'Enterprise Risk',
        icon: <BusinessCenterRoundedIcon fontSize="small" />,
        module: 'ERM',
        children: [
          {
            id: 'erm-risk',
            label: 'Projects',
            icon: <AssignmentRoundedIcon fontSize="small" />,
            module: 'ERM',
          },
          {
            id: 'erm-risk-acceptance',
            label: 'Risk Acceptance',
            icon: <HowToRegRoundedIcon fontSize="small" />,
            module: 'ERM',
          },
        ],
      },
      { id: 'tprm', label: 'Third-Party Risk', icon: <HandshakeRoundedIcon fontSize="small" />, module: 'ERM',
        children: [
          { id: 'tprm-projects', label: 'Projects', icon: <AssignmentRoundedIcon fontSize="small" />, module: 'ERM' },
          { id: 'tprm-vendors', label: 'Vendors', icon: <HandshakeRoundedIcon fontSize="small" />, module: 'ERM' },
          { id: 'tprm-questionnaires', label: 'Questionnaires', icon: <ListAltRoundedIcon fontSize="small" />, module: 'ERM' },
        ],
      },
      { id: 'kri', label: 'KRI', icon: <SpeedRoundedIcon fontSize="small" />, module: 'ERM' },
      {
        id: 'incidents',
        label: 'Incidents',
        icon: <ReportProblemRoundedIcon fontSize="small" />,
        module: 'ERM',
      },
    ].filter((child) => !isRiskNavHidden(child.id)),
  },
  {
    id: 'audit',
    label: 'Audit',
    icon: <GavelRoundedIcon fontSize="small" />,
    module: 'AUDIT',
    children: [
      {
        id: 'audit-register',
        label: 'Audit Projects',
        icon: <AssignmentRoundedIcon fontSize="small" />,
        module: 'AUDIT',
      },
      {
        id: 'audit-checklists',
        label: 'Audit Checklist',
        icon: <ListAltRoundedIcon fontSize="small" />,
        module: 'AUDIT',
      },
      {
        id: 'audit-capa',
        label: 'CAPA',
        icon: <TaskAltRoundedIcon fontSize="small" />,
        module: 'AUDIT',
      },
    ],
  },
  {
    id: 'business-continuity',
    label: 'Business Continuity',
    icon: <HealthAndSafetyRoundedIcon fontSize="small" />,
    module: 'BCMS',
    children: [
      { id: 'bcms-profiles', label: 'Dept Profiles', icon: <ApartmentRoundedIcon fontSize="small" />, module: 'BCMS' },
      { id: 'bcms-bia', label: 'BIA', icon: <TimelineRoundedIcon fontSize="small" />, module: 'BCMS' },
      { id: 'bcms-plans', label: 'Recovery Plans', icon: <PolicyRoundedIcon fontSize="small" />, module: 'BCMS' },
      {
        id: 'bcms-exercises',
        label: 'Exercises',
        icon: <FitnessCenterRoundedIcon fontSize="small" />,
        module: 'BCMS',
      },
      { id: 'bcms-ncema', label: 'NCEMA', icon: <FlagRoundedIcon fontSize="small" />, module: 'BCMS' },
    ].filter((child) => !isBcmsNavPending(child.id)),
  },
  {
    id: 'privacy',
    label: 'Privacy',
    icon: <PrivacyTipRoundedIcon fontSize="small" />,
    module: 'PRIVACY',
    children: [
      {
        id: 'privacy-inventory',
        label: 'Data Inventory',
        icon: <DatasetRoundedIcon fontSize="small" />,
        module: 'PRIVACY',
      },
      {
        id: 'privacy-ropa',
        label: 'ROPA',
        icon: <ListAltRoundedIcon fontSize="small" />,
        module: 'PRIVACY',
      },
      {
        id: 'privacy-dsr',
        label: 'DSR',
        icon: <PersonSearchRoundedIcon fontSize="small" />,
        module: 'PRIVACY',
      },
      {
        id: 'privacy-dpia',
        label: 'DPIA',
        icon: <FactCheckRoundedIcon fontSize="small" />,
        module: 'PRIVACY',
        children: [
          {
            id: 'privacy-dpia-projects',
            label: 'Projects',
            icon: <AssignmentRoundedIcon fontSize="small" />,
            module: 'PRIVACY',
          },
          {
            id: 'privacy-dpia-questionnaires',
            label: 'DPIA Questionnaire',
            icon: <ListAltRoundedIcon fontSize="small" />,
            module: 'PRIVACY',
          },
        ],
      },
      {
        id: 'privacy-risk',
        label: 'Privacy Risk',
        icon: <FactCheckRoundedIcon fontSize="small" />,
        module: 'PRIVACY',
      },
      {
        id: 'privacy-consent',
        label: 'Consent',
        icon: <AssignmentTurnedInRoundedIcon fontSize="small" />,
        module: 'PRIVACY',
      },
      {
        id: 'privacy-breach',
        label: 'Breaches',
        icon: <WarningAmberRoundedIcon fontSize="small" />,
        module: 'PRIVACY',
      },
    ].filter((child) => !isPrivacyNavPending(child.id)),
  },
  { id: 'evidence', label: 'Evidence', icon: <FolderCopyRoundedIcon fontSize="small" /> },
  { id: 'policies', label: 'Policies', icon: <ArticleRoundedIcon fontSize="small" /> },
  { id: 'reports', label: 'Reports', icon: <AssessmentRoundedIcon fontSize="small" /> },
  {
    id: 'settings',
    label: 'Settings',
    icon: <SettingsRoundedIcon fontSize="small" />,
    children: [
      { id: 'users', label: 'Users & Access', icon: <GroupRoundedIcon fontSize="small" /> },
      { id: 'organisation', label: 'Organisation', icon: <ApartmentRoundedIcon fontSize="small" /> },
      {
        id: 'notifications',
        label: 'Notifications',
        icon: <NotificationsActiveRoundedIcon fontSize="small" />,
      },
    ],
  },
  { id: 'audit-trail', label: 'Audit Trail', icon: <HistoryRoundedIcon fontSize="small" /> },
  { id: WORKFLOWS_NAV_ID, label: 'Workflows', icon: <SchemaRoundedIcon fontSize="small" /> },
]

function navItemVisible(item: NavItem, enabledModules: string[]): boolean {
  if (item.anyModules?.length) {
    return item.anyModules.some((m) => enabledModules.includes(m))
  }
  return !item.module || enabledModules.includes(item.module)
}

function filterNav(items: NavItem[], enabledModules: string[]): NavItem[] {
  return items
    .map((item) => {
      if (item.children?.length) {
        const children = filterNav(item.children, enabledModules)
        if (!children.length || !navItemVisible(item, enabledModules)) return null
        return { ...item, children }
      }
      return navItemVisible(item, enabledModules) ? item : null
    })
    .filter((item): item is NavItem => item != null)
}

function findNavItem(items: NavItem[], id: string): NavItem | undefined {
  for (const item of items) {
    if (item.id === id) return item
    if (item.children?.length) {
      const nested = findNavItem(item.children, id)
      if (nested) return nested
    }
  }
  return undefined
}

function findNavLabel(items: NavItem[], id: string): string | undefined {
  return findNavItem(items, id)?.label
}

/** True if id is a top-level or nested (§3.6 parent-group) nav item. */
function navContainsId(items: NavItem[], id: string): boolean {
  return items.some((n) => n.id === id || (n.children != null && navContainsId(n.children, id)))
}

const MONO = 'ui-monospace, SFMono-Regular, Menlo, monospace'

/** Gold fill that matches the left-nav selected item (not the brighter amber chips). */
const NAV_GOLD_FILL = brand.sidebarSelectedFill

/** Tenant Frameworks library only: column titles on paper like data rows, bold labels. */
const fwLibraryHeadSx = {
  bgcolor: `${brand.paper} !important`,
  backgroundColor: `${brand.paper} !important`,
  background: `${brand.paper} !important`,
  color: brand.ink,
  fontWeight: 700,
  fontSize: '0.75rem',
  letterSpacing: 0,
  textTransform: 'none' as const,
  borderBottom: `1px solid ${brand.border}`,
}

function flattenOrgUnits(nodes: OrgUnitNode[]): OrgUnitNode[] {
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

function avatarColor(seed: string) {
  const colors = [brand.accent, brand.accentSoft, brand.inkSoft, brand.gold, '#C53030', '#0D9488']
  let h = 0
  for (let i = 0; i < seed.length; i++) h = seed.charCodeAt(i) + ((h << 5) - h)
  return colors[Math.abs(h) % colors.length]
}

export default function Dashboard(props: { session: Session; onLogout: () => void }) {
  return (
    <UnsavedChangesGuardProvider>
      <DashboardBody {...props} />
    </UnsavedChangesGuardProvider>
  )
}

function DashboardBody({ session, onLogout }: { session: Session; onLogout: () => void }) {
  const { requestNavigation } = useUnsavedChangesGuardApi()
  const isAuditorConsoleUser = isAuditorConsole(session.roles)
  const [auditorGrantModules, setAuditorGrantModules] = useState<string[]>([])
  const [active, setActive] = useState(
    () => readNavParam() || defaultAuditHomeNav(session.roles) || 'overview',
  )
  const [riskDrill, setRiskDrill] = useState<(RiskHeatMapDrill & { riskType: RiskType }) | null>(null)
  const [riskHomeNonce, setRiskHomeNonce] = useState(0)
  const [biaHomeNonce, setBiaHomeNonce] = useState(0)
  const [auditNavEpoch, setAuditNavEpoch] = useState(0)
  const [appShellPage, setAppShellPage] = useState<AppShellPageConfig | null>(null)
  const [users, setUsers] = useState<UserSummary[]>([])
  const [userQuery, setUserQuery] = useState('')
  const [signInFilter, setSignInFilter] = useState<'' | 'SSO' | 'LOCAL'>('')
  const [accountFilter, setAccountFilter] = useState('')
  const [departmentFilter, setDepartmentFilter] = useState('')
  const [orgUnitNameById, setOrgUnitNameById] = useState<Map<number, string>>(new Map())
  const [addUserOpen, setAddUserOpen] = useState(false)
  const [mfaOpen, setMfaOpen] = useState(false)
  const { listReady: usersListReady, markReady: markUsersReady } = useListReady()
  const [enabledModules, setEnabledModules] = useState<string[]>([])
  const [workspaceReady, setWorkspaceReady] = useState(false)
  const [bootSummary, setBootSummary] = useState<DashboardSummary | null>(null)
  const [moduleLicenses, setModuleLicenses] = useState<ModuleLicense[]>([])
  const [licenseBanner, setLicenseBanner] = useState<LicenseBanner | null>(null)
  const [licenseExpiredCodes, setLicenseExpiredCodes] = useState<string[]>([])
  const [auditRows, setAuditRows] = useState<AuditLogEntry[]>([])
  const [auditTotal, setAuditTotal] = useState(0)
  const [fwLibrary, setFwLibrary] = useState<FrameworkSummary[]>([])
  const [fwDetail, setFwDetail] = useState<FrameworkDetail | null>(null)
  const [fwBusy, setFwBusy] = useState(false)
  const [fwConfirm, setFwConfirm] = useState<null | {
    action: 'activate' | 'deactivate'
    framework: FrameworkSummary
  }>(null)
  const [fwBulkActivate, setFwBulkActivate] = useState<FrameworkSummary[] | null>(null)
  const [selectedFwIds, setSelectedFwIds] = useState<number[]>([])
  const [activateIncludeCloud, setActivateIncludeCloud] = useState(false)
  const [error, setErrorRaw] = useState('')
  const setError = useCallback((msg: unknown) => setErrorRaw(productErrorMessage(msg)), [])
  const [ok, setOk] = useState('')
  const { locale } = useTenantLocale()
  const userDepartments = useCallback(
    (u: UserSummary) =>
      (u.orgUnitIds ?? [])
        .map((id) => orgUnitNameById.get(id))
        .filter((name): name is string => Boolean(name))
        .sort((a, b) => a.localeCompare(b)),
    [orgUnitNameById],
  )
  const filteredUsers = useMemo(() => {
    const q = userQuery.trim().toLowerCase()
    return users.filter((u) => {
      if (q && !`${u.fullName} ${u.email} ${userDepartments(u).join(' ')}`.toLowerCase().includes(q)) return false
      const local = Boolean(u.localLoginEnabled) || u.authType === 'LOCAL'
      if (signInFilter === 'SSO' && u.authType !== 'SSO') return false
      if (signInFilter === 'LOCAL' && !local) return false
      if (accountFilter && u.status !== accountFilter) return false
      const unitIds = u.orgUnitIds ?? []
      if (departmentFilter === 'NONE' && unitIds.length > 0) return false
      if (departmentFilter && departmentFilter !== 'NONE' && !unitIds.includes(Number(departmentFilter))) return false
      return true
    })
  }, [users, userQuery, signInFilter, accountFilter, departmentFilter, userDepartments])
  const usersPaged = usePagedRows(filteredUsers)
  const [gapNotifyOpen, setGapNotifyOpen] = useState<{
    projectId: number
    gapId: number
    itemId: number | null
    epoch: number
  } | null>(null)
  const [riskNotifyOpen, setRiskNotifyOpen] = useState<{
    riskId: number
    riskType: 'IS_RISK' | 'ERM' | 'PRIVACY_RISK'
    epoch: number
  } | null>(null)
  const [auditNotifyOpen, setAuditNotifyOpen] = useState<{ auditId: number; epoch: number } | null>(null)
  const [capaNotifyOpen, setCapaNotifyOpen] = useState<{ capaId: number; epoch: number } | null>(null)
  const goNav = useCallback((id: string) => {
    requestNavigation(() => {
      const reenterRiskHome = id === active && (id === 'risk' || id === 'erm-risk' || id === 'privacy-risk')
      const reenterBiaHome = id === active && id === 'bcms-bia'
      writeNavParam(id, 'push')
      setActive(id)
      setRiskDrill(null)
      setAppShellPage(null)
      if (id !== 'assessments') setGapNotifyOpen(null)
      if (id !== 'risk' && id !== 'erm-risk' && id !== 'privacy-risk') setRiskNotifyOpen(null)
      setAuditNotifyOpen(null)
      setCapaNotifyOpen(null)
      if (reenterRiskHome) setRiskHomeNonce((n) => n + 1)
      if (reenterBiaHome) setBiaHomeNonce((n) => n + 1)
      if (isAuditNavId(id)) setAuditNavEpoch((n) => n + 1)
    })
  }, [active, requestNavigation])
  const openNotification = useCallback(
    (n: NotificationItem) => {
      void (async () => {
        try {
          const target = await resolveGapNotificationTarget(n)
          if (target) {
            requestNavigation(() => {
              writeNavParam('assessments', 'push')
              setActive('assessments')
              setRiskDrill(null)
              setAppShellPage(null)
              setGapNotifyOpen({ ...target, epoch: Date.now() })
            })
            return
          }
        } catch (e) {
          setError(e)
        }
        try {
          const target = await resolveRiskNotificationTarget(n)
          if (target) {
            requestNavigation(() => {
              writeNavParam(target.navId, 'push')
              setActive(target.navId)
              setRiskDrill(null)
              setAppShellPage(null)
              setGapNotifyOpen(null)
              setRiskNotifyOpen({
                riskId: target.riskId,
                riskType: target.riskType,
                epoch: Date.now(),
              })
              if (target.navId === active) setRiskHomeNonce((v) => v + 1)
            })
            return
          }
        } catch (e) {
          setError(e)
        }
        try {
          const capaTarget = resolveCapaNotificationTarget(n)
          if (capaTarget) {
            requestNavigation(() => {
              writeNavParam(AUDIT_NAV_CAPA, 'push')
              setActive(AUDIT_NAV_CAPA)
              setRiskDrill(null)
              setAppShellPage(null)
              setGapNotifyOpen(null)
              setRiskNotifyOpen(null)
              setAuditNotifyOpen(null)
              setCapaNotifyOpen({ capaId: capaTarget.capaId, epoch: Date.now() })
              setAuditNavEpoch((v) => v + 1)
            })
            return
          }
        } catch (e) {
          setError(e)
        }
        try {
          const target = await resolveAuditNotificationTarget(n)
          if (target) {
            requestNavigation(() => {
              writeNavParam(AUDIT_NAV_REGISTER, 'push')
              setActive(AUDIT_NAV_REGISTER)
              setRiskDrill(null)
              setAppShellPage(null)
              setGapNotifyOpen(null)
              setRiskNotifyOpen(null)
              setAuditNotifyOpen({ auditId: target.auditId, epoch: Date.now() })
              setAuditNavEpoch((v) => v + 1)
            })
            return
          }
        } catch (e) {
          setError(e)
        }
        if (n.link && n.link !== 'audits') goNav(n.link)
      })()
    },
    [active, goNav, requestNavigation, setError],
  )
  const [wizardOpen, setWizardOpen] = useState(false)
  const [wizardChecked, setWizardChecked] = useState(false)

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fullName, setFullName] = useState('')
  const [inviteDirectoryDepartment, setInviteDirectoryDepartment] = useState<string | null>(null)
  const [roles, setRoles] = useState<string[]>(['CONTRIBUTOR'])
  const [inviteAdminModules, setInviteAdminModules] = useState<string[]>([])
  const [authType, setAuthType] = useState<'SSO' | 'LOCAL'>('SSO')
  const [orgUnitsFlat, setOrgUnitsFlat] = useState<OrgUnitNode[]>([])
  const [inviteOrgUnitIds, setInviteOrgUnitIds] = useState<number[]>([])
  const [creatingUser, setCreatingUser] = useState(false)
  const inviteFields = useFormFields<'email'>()
  const [userMenuAnchor, setUserMenuAnchor] = useState<null | HTMLElement>(null)
  const [userMenuTarget, setUserMenuTarget] = useState<UserSummary | null>(null)
  const [rolesDialogUser, setRolesDialogUser] = useState<UserSummary | null>(null)
  const [editRoles, setEditRoles] = useState<string[]>([])
  const [editAdminModules, setEditAdminModules] = useState<string[]>([])
  const [editOrgUnitIds, setEditOrgUnitIds] = useState<number[]>([])
  const [savingRoles, setSavingRoles] = useState(false)
  const [passwordDialogUser, setPasswordDialogUser] = useState<UserSummary | null>(null)
  const [resetPassword, setResetPassword] = useState('')
  const [localPolicy, setLocalPolicy] = useState<{ scope: string; ssoEnabled: boolean; ownerOnly: boolean } | null>(null)
  const [userConfirm, setUserConfirm] = useState<null | {
    action: 'deactivate' | 'disable-local' | 'enable-local'
    user: UserSummary
  }>(null)
  const [armLocalPassword, setArmLocalPassword] = useState('')

  const canManageUsers = session.roles.includes('OWNER') || session.roles.includes('ADMIN')
  const usersListActions = useMemo(() => {
    if (!canManageUsers) return undefined
    return (
      <Stack direction="row" spacing={1}>
        {!session.superAdmin && (
          <Button size="small" variant="outlined" onClick={() => setMfaOpen(true)}>
            Set Up Authenticator
          </Button>
        )}
        <Button size="small" variant="contained" onClick={() => setAddUserOpen(true)}>
          Add User
        </Button>
      </Stack>
    )
  }, [canManageUsers, session.superAdmin])
  const showLicenseBanner =
    Boolean(licenseBanner) && (canManageUsers || licenseBanner?.tone === 'error')
  const canExportAudit = canManageUsers
  const canManageFrameworks = canManageUsers
  const me = users.find((u) => u.email.toLowerCase() === session.email.toLowerCase())
  const sessionAuth = me?.authType === 'SSO' ? 'Entra SSO' : 'Local session'
  const needsOrgUnit = roles.includes('CONTRIBUTOR')

  const nav = useMemo(() => {
    let base = filterNav(NAV, enabledModules)
    if (!SHOW_WORKFLOWS_IN_NAV) {
      base = base.filter((item) => item.id !== WORKFLOWS_NAV_ID)
    }
    if (isAuditorConsoleUser) {
      return filterNavForAuditor(base, auditorGrantModules)
    }
    return base
  }, [enabledModules, isAuditorConsoleUser, auditorGrantModules])

  const licensedFrameworkModules = useMemo(
    () => FRAMEWORK_MODULES.filter((m) => enabledModules.includes(m)),
    [enabledModules],
  )

  const fwByModule = useMemo(() => {
    const order = [...FRAMEWORK_MODULES]
    const groups: { moduleCode: string; rows: FrameworkSummary[] }[] = []
    for (const code of order) {
      const rows = fwLibrary.filter((f) => f.moduleCode === code)
      if (rows.length) groups.push({ moduleCode: code, rows })
    }
    const known = new Set(order as readonly string[])
    const other = fwLibrary.filter((f) => !known.has(f.moduleCode))
    if (other.length) {
      const by = new Map<string, FrameworkSummary[]>()
      for (const f of other) {
        const list = by.get(f.moduleCode) ?? []
        list.push(f)
        by.set(f.moduleCode, list)
      }
      for (const [moduleCode, rows] of by) groups.push({ moduleCode, rows })
    }
    return groups
  }, [fwLibrary])

  const loadUsers = useCallback(async () => {
    if (isAuditorConsoleUser) return
    try {
      setUsers(await api.listUsers())
      if (session.roles.includes('OWNER') || session.roles.includes('ADMIN')) {
        api.localLoginPolicy().then(setLocalPolicy).catch(() => setLocalPolicy(null))
      }
    } catch (e) {
      setError(String(e))
    } finally {
      markUsersReady()
    }
  }, [isAuditorConsoleUser, session.roles, markUsersReady])
  const loadOrgUnits = useCallback(() => {
    api
      .listOrgUnits()
      .then((tree) => {
        const all = flattenOrgUnits(tree)
        setOrgUnitNameById(new Map(all.map((u) => [u.id, u.name])))
        setOrgUnitsFlat(all.filter((u) => u.active))
      })
      .catch(() => setOrgUnitsFlat([]))
  }, [])
  const loadAudit = useCallback(() => {
    api.listAuditTrail(0, 50)
      .then((r) => {
        setAuditRows(r.content ?? [])
        setAuditTotal(r.totalElements ?? 0)
      })
      .catch((e) => setError(String(e)))
  }, [])

  const loadFrameworks = useCallback(() => {
    // One catalog list: published + licensed, with activated flag (single-table UX).
    api
      .listAvailableFrameworks()
      .then((rows) => {
        setFwLibrary(
          [...rows].sort((a, b) => {
            const aOn = Boolean(a.activated)
            const bOn = Boolean(b.activated)
            if (aOn !== bOn) return aOn ? -1 : 1
            return a.name.localeCompare(b.name) || a.version.localeCompare(b.version)
          }),
        )
        // Drop selections that are no longer available (activated / removed).
        setSelectedFwIds((prev) =>
          prev.filter((id) => rows.some((r) => r.id === id && !r.activated)),
        )
      })
      .catch((e) => setError(String(e)))
  }, [])

  useEffect(() => {
    let cancelled = false
    if (!isAuditorConsoleUser) {
      loadUsers()
      setAuditorGrantModules([])
    }
    loadOrgUnits()

    const landing = readNavParam() || defaultAuditHomeNav(session.roles) || 'overview'
    const waitForOverview = !isAuditorConsoleUser && landing === 'overview'
    const modules = api
      .listMyModules()
      .then((r) => {
        setEnabledModules(r.enabled ?? [])
        setModuleLicenses(r.modules ?? [])
        setLicenseExpiredCodes(r.expiredCodes ?? r.modules?.filter((m) => m.enabled && m.expired).map((m) => m.code) ?? [])
        const banner = r.banner ?? null
        setLicenseBanner(banner && !banner.dismissed ? banner : null)
      })
      .catch((e) => setError(String(e)))
    const extra = isAuditorConsoleUser
      ? api
          .getMyAuditAccess()
          .then((r) => setAuditorGrantModules((r.modules ?? []).map(String)))
          .catch(() => setAuditorGrantModules([]))
      : waitForOverview
        ? api
            .getDashboardSummary()
            .then((s) => {
              setBootSummary(s)
              return s
            })
            .catch(() => null)
        : Promise.resolve()

    void Promise.all([modules, extra]).finally(() => {
      if (!cancelled) setWorkspaceReady(true)
    })
    return () => {
      cancelled = true
    }
  }, [isAuditorConsoleUser, loadUsers, loadOrgUnits, session.roles])

  useEffect(() => {
    if (active === 'users') {
      loadUsers()
      loadOrgUnits()
    }
  }, [active, loadUsers, loadOrgUnits])

  useEffect(() => {
    if (active === 'audit-trail') loadAudit()
  }, [active, loadAudit])

  useEffect(() => {
    if (active === 'frameworks') loadFrameworks()
  }, [active, loadFrameworks])

  const licensedModuleNav = useMemo(() => {
    let codes = enabledModules
    if (isAuditorConsoleUser) {
      const allow = new Set<string>(['AUDIT'])
      if (
        auditorGrantModules.includes('GAP') ||
        auditorGrantModules.includes('SOA') ||
        auditorGrantModules.includes('RISK') ||
        auditorGrantModules.includes('EVIDENCE') ||
        auditorGrantModules.includes('ASSET')
      ) {
        allow.add('ISMS')
      }
      if (auditorGrantModules.includes('ERM')) allow.add('ERM')
      if (auditorGrantModules.includes('BCMS')) allow.add('BCMS')
      if (auditorGrantModules.includes('PRIVACY')) allow.add('PRIVACY')
      codes = enabledModules.filter((c) => allow.has(c))
    }
    return codes.filter((code) => code !== 'AUDIT').map((code) => ({ code, label: code }))
  }, [enabledModules, isAuditorConsoleUser, auditorGrantModules])

  const viewingModuleCode = parseLicensedModuleActive(active)

  useEffect(() => {
    if (viewingModuleCode) {
      if (!enabledModules.includes(viewingModuleCode)) setActive('overview')
      return
    }
    if (isAuditorConsoleUser && active === 'overview' && !auditorGrantModules.includes('DASHBOARD')) {
      setActive('audit-register')
      return
    }
    if (active === 'tprm') {
      writeNavParam('tprm-projects', 'replace')
      setActive('tprm-projects')
      return
    }
    if (active === 'audit') {
      writeNavParam('audit-register', 'replace')
      setActive('audit-register')
      return
    }
    if (active === 'compliance') {
      writeNavParam('assessments-projects', 'replace')
      setActive('assessments-projects')
      return
    }
    if (!navContainsId(nav, active)) {
      if (active === WORKFLOWS_NAV_ID && !SHOW_WORKFLOWS_IN_NAV) {
        return
      }
      const deepNav = readNavParam()
      if (deepNav && navContainsId(nav, deepNav)) {
        if (deepNav !== active) setActive(deepNav)
      } else {
        const fallback = isAuditorConsoleUser ? 'audit-register' : 'overview'
        if (fallback !== active) setActive(fallback)
      }
    }
  }, [nav, active, viewingModuleCode, enabledModules, isAuditorConsoleUser, auditorGrantModules])

  /** Deep-link: path (`/frameworks`) or legacy `?nav=` (E2E / bookmarks). */
  useEffect(() => {
    const q = readNavParam()
    if (!q || enabledModules.length === 0) return
    if (navContainsId(nav, q) || (q === WORKFLOWS_NAV_ID && !SHOW_WORKFLOWS_IN_NAV)) setActive(q)
  }, [nav, enabledModules])

  useEffect(() => {
    const onPop = () => {
      setActive(readNavParam() || defaultAuditHomeNav(session.roles) || 'overview')
    }
    window.addEventListener('popstate', onPop)
    return () => window.removeEventListener('popstate', onPop)
  }, [session.roles])

  useEffect(() => {
    setArborTitle(navIdToTitle(active))
  }, [active])

  useEffect(() => {
    if (wizardChecked) return
    const canSetup = session.roles.some((r) => ['OWNER', 'ADMIN'].includes(r))
    if (!canSetup) {
      setWizardChecked(true)
      return
    }
    let cancelled = false
    api
      .getOrgProfile()
      .then((p) => {
        if (!cancelled && !p.wizardCompleted) setWizardOpen(true)
      })
      .catch(() => {
        /* profile may 404 before V8 migration / restart: ignore */
      })
      .finally(() => {
        if (!cancelled) setWizardChecked(true)
      })
    return () => {
      cancelled = true
    }
  }, [session.roles, wizardChecked])

  const toggleRole = (role: string) =>
    setRoles((prev) => (prev.includes(role) ? prev.filter((r) => r !== role) : [...prev, role]))

  const inviteEmailError = emailError(email, true)
  const inviteEmailMsg = inviteFields.fieldError('email', inviteEmailError)
  const needsAdminModules = roles.includes('ADMIN') && !roles.includes('OWNER')
  const createDisabled =
    !email.trim() ||
    !!inviteEmailError ||
    !fullName.trim() ||
    roles.length === 0 ||
    (!localPolicy?.ownerOnly && authType === 'LOCAL' && password.length < 8) ||
    (needsOrgUnit && inviteOrgUnitIds.length === 0) ||
    (needsAdminModules && (inviteAdminModules.length === 0 || enabledModules.length === 0))

  const createDisabledReason = !email.trim()
    ? 'Enter an email'
    : inviteEmailError
      ? inviteEmailError
      : !fullName.trim()
        ? 'Enter a full name'
        : roles.length === 0
          ? 'Select at least one role'
          : !localPolicy?.ownerOnly && authType === 'LOCAL' && password.length < 8
            ? 'Password must be at least 8 characters'
              : needsOrgUnit && inviteOrgUnitIds.length === 0
              ? 'Choose a department'
              : needsAdminModules && enabledModules.length === 0
                ? 'No licensed modules are available to assign'
                : needsAdminModules && inviteAdminModules.length === 0
                  ? 'Select at least one module'
                  : undefined

  const inviteSso = Boolean(localPolicy?.ownerOnly) || authType === 'SSO'
  const roleHelp: Record<string, string> = {
    OWNER: 'Runs this workspace, including people and settings.',
    ADMIN: 'Manages people and day-to-day settings.',
    CONTRIBUTOR: 'Works on records in their department.',
    INTERNAL_AUDITOR: 'Reviews work. Cannot change records.',
    GLOBAL_READER: 'Can view every department.',
  }
  const directoryOrgMatch = inviteDirectoryDepartment
    ? orgUnitsFlat.find(
        (u) => u.name.trim().toLowerCase() === inviteDirectoryDepartment.trim().toLowerCase(),
      )
    : undefined
  const ssoDirectoryHint = !inviteSso
    ? undefined
    : inviteDirectoryDepartment
      ? directoryOrgMatch
        ? `Department: ${inviteDirectoryDepartment}`
        : `Department: ${inviteDirectoryDepartment}. Add it under Organisation if this person needs that unit.`
      : undefined

  const applyDirectoryPerson = (person: EmployeeDirectoryEntry) => {
    const mail = (person.email ?? '').trim()
    if (mail) setEmail(mail)
    const name = person.displayName?.trim()
    if (name) setFullName(name)
    const dept = person.department?.trim() || null
    setInviteDirectoryDepartment(dept)
    if (!dept) return
    const key = dept.toLowerCase()
    const hits = orgUnitsFlat.filter((u) => u.name.trim().toLowerCase() === key)
    const unit = hits.find((u) => u.level === 1) ?? hits[0]
    if (unit) {
      setInviteOrgUnitIds((prev) => (prev.includes(unit.id) ? prev : [...prev, unit.id]))
    }
  }

  const createUser = async () => {
    inviteFields.touchAll()
    if (inviteEmailError || creatingUser || createDisabled) return
    const chosenAuth = localPolicy?.ownerOnly ? 'SSO' : authType
    setCreatingUser(true)
    try {
      const created = await api.createUser(
        email.trim(),
        fullName.trim(),
        roles,
        chosenAuth,
        chosenAuth === 'LOCAL' ? password : undefined,
        inviteOrgUnitIds,
        needsAdminModules ? inviteAdminModules : [],
      )
      setUsers((prev) => (prev.some((u) => u.id === created.id) ? prev : [created, ...prev]))
      await loadUsers()
      setOk(authType === 'SSO' ? `Added ${email}. They sign in with SSO.` : `Added ${email}. They sign in with a password.`)
      setEmail('')
      setPassword('')
      setFullName('')
      setInviteDirectoryDepartment(null)
      setRoles(['CONTRIBUTOR'])
      setInviteAdminModules([])
      setInviteOrgUnitIds([])
      inviteFields.reset()
      setAddUserOpen(false)
    } catch (e) {
      inviteFields.applyApiError(e)
      setError(e instanceof ApiError ? e.message : String(e))
    } finally {
      setCreatingUser(false)
    }
  }

  const openUserMenu = (event: MouseEvent<HTMLElement>, user: UserSummary) => {
    setUserMenuAnchor(event.currentTarget)
    setUserMenuTarget(user)
  }

  const closeUserMenu = () => {
    setUserMenuAnchor(null)
    setUserMenuTarget(null)
  }

  const canManageTarget = (target: UserSummary) => {
    if (target.email.toLowerCase() === session.email.toLowerCase()) return false
    if (session.roles.includes('OWNER')) return true
    return !target.roles.includes('OWNER')
  }

  const runDisableUser = async (user: UserSummary) => {
    try {
      await api.disableUser(user.id)
      setOk(`Deactivated ${user.email}`)
      loadUsers()
    } catch (e) {
      setError(String(e))
    }
  }

  const runDisableLocal = async (user: UserSummary) => {
    try {
      await api.disableLocalLogin(user.id)
      setOk(`Local login off for ${user.email}`)
      loadUsers()
    } catch (e) {
      setError(String(e))
    }
  }

  const runEnableLocal = async (user: UserSummary) => {
    try {
      await api.enableLocalLogin(user.id, armLocalPassword.trim() || undefined)
      setOk(`Local login on for ${user.email}`)
      setArmLocalPassword('')
      loadUsers()
    } catch (e) {
      setError(String(e))
    }
  }

  const localOn = (user: UserSummary) => Boolean(user.localLoginEnabled) || user.authType === 'LOCAL'
  const canArmLocal = (user: UserSummary) => {
    if (!localPolicy?.ownerOnly) return true
    return session.roles.includes('OWNER') && user.roles.includes('OWNER')
  }

  const runEnableUser = async (user: UserSummary) => {
    closeUserMenu()
    try {
      await api.enableUser(user.id)
      setOk(`Reactivated ${user.email}`)
      loadUsers()
    } catch (e) {
      setError(String(e))
    }
  }

  const openRolesDialog = (user: UserSummary) => {
    closeUserMenu()
    setRolesDialogUser(user)
    setEditRoles([...user.roles])
    setEditAdminModules([...(user.adminModules ?? [])])
    setEditOrgUnitIds([...(user.orgUnitIds ?? [])])
  }

  const editNeedsAdminModules = editRoles.includes('ADMIN') && !editRoles.includes('OWNER')
  const editNeedsOrgUnit = editRoles.includes('CONTRIBUTOR')
  const sameSet = <T,>(a: T[], b: T[]) => a.length === b.length && a.every((x) => b.includes(x))
  const rolesDialogDirty =
    rolesDialogUser != null &&
    (!sameSet(editRoles, rolesDialogUser.roles) ||
      !sameSet(editAdminModules, rolesDialogUser.adminModules ?? []) ||
      (editNeedsOrgUnit && !sameSet(editOrgUnitIds, rolesDialogUser.orgUnitIds ?? [])))
  const rolesSaveDisabledReason =
    editRoles.length === 0
      ? 'Select at least one role'
      : editNeedsOrgUnit && editOrgUnitIds.length === 0
        ? 'Choose a department'
        : editNeedsAdminModules && enabledModules.length === 0
          ? 'No licensed modules are available to assign'
          : editNeedsAdminModules && editAdminModules.length === 0
            ? 'Select at least one module'
            : !rolesDialogDirty
              ? 'No changes to save'
              : undefined
  const saveRolesDialog = async () => {
    if (!rolesDialogUser || rolesSaveDisabledReason || savingRoles) return
    setSavingRoles(true)
    try {
      await api.updateUserRoles(
        rolesDialogUser.id,
        editRoles,
        editNeedsAdminModules ? editAdminModules : [],
        editNeedsOrgUnit ? editOrgUnitIds : undefined,
      )
      setOk(`Updated ${rolesDialogUser.email}`)
      setRolesDialogUser(null)
      loadUsers()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e))
    } finally {
      setSavingRoles(false)
    }
  }

  const openPasswordDialog = (user: UserSummary) => {
    closeUserMenu()
    setPasswordDialogUser(user)
    setResetPassword('')
  }

  const savePasswordDialog = async () => {
    if (!passwordDialogUser || resetPassword.length < 8) return
    try {
      await api.resetUserPassword(passwordDialogUser.id, resetPassword)
      setOk(`Password reset for ${passwordDialogUser.email}`)
      setPasswordDialogUser(null)
      setResetPassword('')
    } catch (e) {
      setError(String(e))
    }
  }

  const viewTenantFramework = async (id: number) => {
    setFwBusy(true)
    try {
      setFwDetail(await api.getTenantFramework(id))
    } catch (e) {
      setError(String(e))
    } finally {
      setFwBusy(false)
    }
  }

  const runFwConfirm = async () => {
    if (!fwConfirm) return
    const { action, framework } = fwConfirm
    setFwBusy(true)
    try {
      if (action === 'activate') {
        await api.activateFramework(framework.id, { includeCloud: activateIncludeCloud })
        setOk(`${framework.name} activated for this workspace`)
        loadFrameworks()
        setFwDetail(await api.getTenantFramework(framework.id))
        setSelectedFwIds((ids) => ids.filter((id) => id !== framework.id))
      } else {
        await api.deactivateFramework(framework.id)
        setOk(`${framework.name} deactivated`)
        if (fwDetail?.framework.id === framework.id) setFwDetail(null)
        loadFrameworks()
      }
      setFwConfirm(null)
      setActivateIncludeCloud(false)
    } catch (e) {
      setError(String(e))
    } finally {
      setFwBusy(false)
    }
  }

  const runFwBulkActivate = async () => {
    if (!fwBulkActivate?.length) return
    setFwBusy(true)
    try {
      let done = 0
      for (const framework of fwBulkActivate) {
        await api.activateFramework(framework.id, {
          includeCloud: activateIncludeCloud && Boolean(framework.hasCloud),
        })
        done += 1
      }
      setOk(
        done === 1
          ? `${fwBulkActivate[0].name} activated for this workspace`
          : `${done} frameworks activated for this workspace`,
      )
      setFwBulkActivate(null)
      setSelectedFwIds([])
      setActivateIncludeCloud(false)
      loadFrameworks()
    } catch (e) {
      setError(String(e))
      loadFrameworks()
    } finally {
      setFwBusy(false)
    }
  }

  const activatedCount = useMemo(
    () => fwLibrary.filter((f) => f.activated).length,
    [fwLibrary],
  )

  const availableFrameworks = useMemo(
    () => fwLibrary.filter((f) => !f.activated),
    [fwLibrary],
  )

  const selectedAvailable = useMemo(
    () =>
      availableFrameworks.filter(
        (f) => selectedFwIds.includes(f.id) && !licenseExpiredCodes.includes(f.moduleCode),
      ),
    [availableFrameworks, selectedFwIds, licenseExpiredCodes],
  )

  const allAvailableSelected =
    availableFrameworks.length > 0 && selectedAvailable.length === availableFrameworks.length

  const toggleFwSelected = (id: number, checked: boolean) => {
    setSelectedFwIds((prev) => {
      if (checked) return prev.includes(id) ? prev : [...prev, id]
      return prev.filter((x) => x !== id)
    })
  }

  const toggleSelectAllAvailable = (checked: boolean) => {
    setSelectedFwIds(checked ? availableFrameworks.map((f) => f.id) : [])
  }

  const iaAuditFieldwork = false

  const headerTitle =
    appShellPage?.title ??
    (findNavLabel(nav, active) ?? 'Dashboard')

  const headerBreadcrumb = appShellPage?.breadcrumbTrail
  const headerBreadcrumbRoot = appShellPage?.breadcrumbRoot
  const headerTabs = appShellPage?.tabs
  const showRoleChips = !appShellPage?.hideRoleChips

  const roleChips = (
    <Stack direction="row" spacing={0.5} alignItems="center">
      {session.roles.slice(0, 2).map((r) => (
        <Chip key={r} size="small" variant="outlined" label={roleLabel(r)} />
      ))}
      {session.roles.length > 2 && (
        <Chip size="small" variant="outlined" label={`+${session.roles.length - 2}`} />
      )}
    </Stack>
  )

  if (!workspaceReady) {
    return (
      <WorkspaceBootScreen
        title="Opening your workspace"
        message="Loading your organisation, modules, and dashboard. This usually takes a few seconds."
      />
    )
  }

  return (
    <AppShellPageProvider onPageChange={setAppShellPage}>
    <AppShell
      nav={nav}
      active={active}
      onNavChange={goNav}
      showLicensedModulesNav={!isAuditorConsoleUser}
      licensedModules={licensedModuleNav}
      email={session.email}
      accountMeta={`${sessionAuth} · ${roleLabels(session.roles)}`}
      roleChips={showRoleChips ? roleChips : undefined}
      headerTitle={headerTitle}
      onHeaderTitleClick={appShellPage?.onTitleClick}
      headerBreadcrumb={headerBreadcrumb}
      headerBreadcrumbRoot={headerBreadcrumbRoot}
      headerTabs={headerTabs}
      headerMeta={appShellPage?.headerMeta}
      workspaceName={locale.displayName || session.tenantId || 'Workspace'}
      hideSidebar={appShellPage?.hideSidebar ?? iaAuditFieldwork}
      flushContent={appShellPage?.flushContent ?? iaAuditFieldwork}
      pageActions={appShellPage?.actions}
      actionsLockedReason={moduleWriteLockReason(
        viewingModuleCode ?? findNavItem(NAV, active)?.module,
        licenseExpiredCodes,
      )}
      notice={
        showLicenseBanner && licenseBanner ? (
          <AttentionBanner
            dense
            tone={licenseBanner.tone === 'error' ? 'error' : 'warning'}
            emphasis={licenseBanner.title}
            message={licenseBanner.message}
            onDismiss={() => {
              const key = licenseBanner.key
              setLicenseBanner(null)
              void api.dismissLicenseBanner(key).catch(() => {
                /* keep closed locally even if persist fails */
              })
            }}
          />
        ) : undefined
      }
      actions={
        <NotificationBell
          onNavigate={goNav}
          onOpenNotification={openNotification}
          onViewAll={() => goNav('notifications')}
        />
      }
      onLogout={onLogout}
      canChangePassword={session.superAdmin || session.authType !== 'SSO'}
      superAdmin={session.superAdmin}
    >
      {viewingModuleCode && (
        <LicensedModulePage
          code={viewingModuleCode}
          license={moduleLicenses.find((m) => m.code === viewingModuleCode) ?? null}
          onOpenNav={goNav}
        />
      )}

      {active === 'overview' && (
        <OverviewDashboard
          session={session}
          initialSummary={bootSummary}
          onError={setError}
          onNavigate={(id, drill) => {
            goNav(id)
            setRiskDrill(drill ?? null)
          }}
        />
      )}

      {active === 'reports' && (
        <ReportsPage session={session} onError={setError} onOk={setOk} />
      )}

      {active === 'frameworks' && (
        <>
          <ModuleListShell title="Frameworks" breadcrumbTrail="Settings › Frameworks" />
        <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
          <Card sx={{ flexShrink: 0 }}>
            <Box sx={{ px: 2, py: 1.25 }}>
              <Typography variant="subtitle2" sx={{ mb: 0.75 }}>
                Licensed modules
              </Typography>
              {licensedFrameworkModules.length === 0 ? (
                <Typography variant="body2" color="text.secondary">
                  No framework modules are licensed yet. Ask Arbor (Super Admin) to enable ISMS or another
                  module under Licensing: then Frameworks will appear with that module&apos;s standards.
                </Typography>
              ) : (
                <Stack direction="row" spacing={0.75} flexWrap="wrap" useFlexGap>
                  {licensedFrameworkModules.map((code) => (
                    <Chip
                      key={code}
                      size="small"
                      label={code}
                      title={MODULE_LABELS[code] ?? code}
                      sx={{
                        fontWeight: 700,
                        fontFamily: MONO,
                        bgcolor: NAV_GOLD_FILL,
                        color: brand.accentHover,
                        border: `1px solid ${brand.accent}47`,
                      }}
                    />
                  ))}
                  <Typography variant="caption" color="text.secondary" sx={{ alignSelf: 'center', ml: 0.5 }}>
                    {fwLibrary.length} framework{fwLibrary.length === 1 ? '' : 's'} · {activatedCount} activated
                  </Typography>
                </Stack>
              )}
            </Box>
          </Card>

          <Box sx={{ flex: 1, minHeight: 0, overflow: 'auto' }}>
            {fwLibrary.length === 0 ? (
              <Card>
                <Box sx={{ py: 5, px: 2, textAlign: 'center' }}>
                  <Typography variant="subtitle2" gutterBottom>
                    No frameworks available
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 420, mx: 'auto' }}>
                    {licensedFrameworkModules.length === 0
                      ? 'Super Admin must license a module (e.g. ISMS) for this workspace first.'
                      : 'Super Admin must import and publish frameworks for your licensed modules (Platform → Frameworks).'}
                  </Typography>
                </Box>
              </Card>
            ) : (
              <Stack spacing={1.25}>
                {canManageFrameworks && availableFrameworks.length > 0 && (
                  <Card sx={{ flexShrink: 0 }}>
                    <Stack
                      direction="row"
                      alignItems="center"
                      spacing={1.25}
                      flexWrap="wrap"
                      useFlexGap
                      sx={{ px: 2, py: 1 }}
                    >
                      <FormControlLabel
                        sx={{ mr: 0.5 }}
                        control={
                          <Checkbox
                            size="small"
                            checked={allAvailableSelected}
                            indeterminate={
                              selectedAvailable.length > 0 && !allAvailableSelected
                            }
                            onChange={(e) => toggleSelectAllAvailable(e.target.checked)}
                            disabled={fwBusy}
                            inputProps={{ 'aria-label': 'Select all available frameworks' }}
                          />
                        }
                        label={
                          <Typography variant="body2">
                            Select all available ({availableFrameworks.length})
                          </Typography>
                        }
                      />
                      <Box sx={{ flex: 1 }} />
                      <Button
                        size="small"
                        variant="contained"
                        disabled={fwBusy || selectedAvailable.length === 0}
                        data-testid="tenant-framework-bulk-activate"
                        onClick={() => {
                          setActivateIncludeCloud(false)
                          setFwBulkActivate(selectedAvailable)
                        }}
                      >
                        Activate Selected ({selectedAvailable.length})
                      </Button>
                    </Stack>
                  </Card>
                )}
                {fwByModule.map((group) => {
                  const moduleLicense = moduleLicenses.find((m) => m.code === group.moduleCode)
                  const term = moduleLicense?.enabled
                    ? licenceTermView({ ...moduleLicense, phrase: 'valid-until' })
                    : null
                  const moduleExpired = licenseExpiredCodes.includes(group.moduleCode)
                  const moduleLockWhy = moduleWriteLockReason(group.moduleCode, licenseExpiredCodes)
                  return (
                  <Card key={group.moduleCode} sx={{ overflow: 'hidden' }}>
                    <Box
                      sx={{
                        px: 2,
                        py: 1,
                        bgcolor: NAV_GOLD_FILL,
                        borderBottom: `1px solid ${brand.accent}47`,
                      }}
                    >
                      <Stack direction="row" alignItems="center" spacing={1} flexWrap="wrap" useFlexGap>
                        <Chip
                          size="small"
                          label={group.moduleCode}
                          sx={{
                            fontWeight: 700,
                            fontFamily: MONO,
                            height: 22,
                            bgcolor: NAV_GOLD_FILL,
                            color: brand.accentHover,
                            border: `1px solid ${brand.accent}47`,
                          }}
                        />
                        <Typography variant="caption" sx={{ color: brand.goldDark, fontWeight: 600 }}>
                          {MODULE_LABELS[group.moduleCode] ?? 'Module'}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {group.rows.length} framework{group.rows.length === 1 ? '' : 's'} ·{' '}
                          {group.rows.filter((r) => r.activated).length} activated
                        </Typography>
                        {term ? (
                          <Box sx={{ ml: 'auto' }}>
                            <LicenceTermBadge term={term} compact />
                          </Box>
                        ) : null}
                      </Stack>
                    </Box>
                    <Table
                      size="small"
                      sx={{
                        ...registerTableSx,
                        '& .MuiTableCell-root': {
                          py: 0.85,
                          px: 1.5,
                          fontSize: '0.75rem',
                          verticalAlign: 'middle',
                          borderColor: brand.hairline,
                        },
                        '& .MuiTableCell-head': {
                          ...fwLibraryHeadSx,
                          py: 0.7,
                          px: 1.5,
                        },
                        '& .MuiTableBody .MuiTableRow-root': {
                          bgcolor: brand.paper,
                        },
                        '& .MuiTableBody .MuiTableRow-hover:hover': {
                          bgcolor: brand.hover,
                        },
                        '& .MuiTableBody .MuiTableRow:last-child .MuiTableCell-root': {
                          borderBottom: 0,
                        },
                      }}
                    >
                      <TableHead>
                        <TableRow>
                          {canManageFrameworks && (
                            <TableCell padding="checkbox" sx={{ width: '4%', px: 1 }} />
                          )}
                          <TableCell sx={{ ...primaryTableCellSx, width: canManageFrameworks ? '32%' : '36%' }}>
                            Framework
                          </TableCell>
                          <TableCell align="right" sx={compactTableCellSx}>
                            Clauses
                          </TableCell>
                          <TableCell align="right" sx={compactTableCellSxWidth('13%')}>
                            Annexures
                          </TableCell>
                          <TableCell align="right" sx={compactTableCellSx}>
                            Controls
                          </TableCell>
                          <TableCell sx={compactTableCellSxWidth('14%')}>Status</TableCell>
                          {canManageFrameworks && (
                            <TableCell align="right" sx={{ ...compactTableCellSxWidth('15%'), ...lastTableCellSx }}>
                              Actions
                            </TableCell>
                          )}
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {group.rows.map((f) => {
                          const on = Boolean(f.activated)
                          const checked = selectedFwIds.includes(f.id)
                          return (
                            <TableRow
                              key={f.id}
                              hover
                              selected={fwDetail?.framework.id === f.id || checked}
                              onClick={() => void viewTenantFramework(f.id)}
                              sx={{ cursor: 'pointer' }}
                            >
                              {canManageFrameworks && (
                                <TableCell padding="checkbox" sx={{ px: 1 }} onClick={(e) => e.stopPropagation()}>
                                  {!on ? (
                                    <Checkbox
                                      size="small"
                                      checked={checked}
                                      disabled={fwBusy}
                                      onChange={(e) => toggleFwSelected(f.id, e.target.checked)}
                                      inputProps={{ 'aria-label': `Select ${f.code} ${f.version}` }}
                                    />
                                  ) : null}
                                </TableCell>
                              )}
                              <TableCell
                                sx={{
                                  ...primaryTableCellSx,
                                  width: canManageFrameworks ? '32%' : '36%',
                                }}
                              >
                                <Typography sx={primaryRegisterNameSx} noWrap title={f.name}>
                                  {f.name}
                                </Typography>
                              </TableCell>
                              <TableCell align="right" sx={{ ...compactTableCellSx, fontFamily: MONO }}>
                                {f.clauseCount ?? '-'}
                              </TableCell>
                              <TableCell align="right" sx={{ ...compactTableCellSxWidth('13%'), fontFamily: MONO }}>
                                {f.annexControlCount ?? '-'}
                              </TableCell>
                              <TableCell align="right" sx={{ ...compactTableCellSx, fontFamily: MONO }}>
                                {f.controlCount}
                              </TableCell>
                              <TableCell sx={compactTableCellSxWidth('14%')}>
                                <StatusChip
                                  label={on ? 'Activated' : 'Available'}
                                  tone={on ? 'success' : 'neutral'}
                                />
                              </TableCell>
                              {canManageFrameworks && (
                                <TableCell
                                  align="right"
                                  sx={{ ...compactTableCellSxWidth('15%'), ...lastTableCellSx }}
                                  onClick={(e) => e.stopPropagation()}
                                >
                                  {on ? (
                                    <Button
                                      size="small"
                                      variant="outlined"
                                      color="primary"
                                      disabled={fwBusy || moduleExpired}
                                      title={moduleExpired ? moduleLockWhy : undefined}
                                      onClick={() => setFwConfirm({ action: 'deactivate', framework: f })}
                                    >
                                      Deactivate
                                    </Button>
                                  ) : (
                                    <Button
                                      size="small"
                                      variant="contained"
                                      color="primary"
                                      disabled={fwBusy || moduleExpired}
                                      title={moduleExpired ? moduleLockWhy : undefined}
                                      onClick={() => {
                                        setActivateIncludeCloud(false)
                                        setFwConfirm({ action: 'activate', framework: f })
                                      }}
                                    >
                                      Activate
                                    </Button>
                                  )}
                                </TableCell>
                              )}
                            </TableRow>
                          )
                        })}
                      </TableBody>
                    </Table>
                  </Card>
                  )
                })}
              </Stack>
            )}
          </Box>

          <Dialog
            open={Boolean(fwConfirm)}
            onClose={() => !fwBusy && setFwConfirm(null)}
            fullWidth
            maxWidth="xs"
          >
            <DialogTitle sx={{ pb: 1 }}>
              {fwConfirm?.action === 'activate' ? 'Activate framework?' : 'Deactivate framework?'}
            </DialogTitle>
            <DialogContent>
              <Typography variant="body2" color="text.secondary">
                {fwConfirm?.action === 'activate' ? (
                  <>
                    Activate <strong>{frameworkDisplayName(fwConfirm.framework.name, fwConfirm.framework.code)}</strong> ({frameworkDisplayName(undefined, fwConfirm.framework.code)} ·{' '}
                    {fwConfirm.framework.version}) under module{' '}
                    <strong>{fwConfirm.framework.moduleCode}</strong> for this workspace?
                  </>
                ) : (
                  <>
                    Deactivate <strong>{fwConfirm?.framework.name}</strong> for this workspace? You can
                    activate it again later from this library.
                  </>
                )}
              </Typography>
              {fwConfirm?.action === 'activate' && fwConfirm.framework.hasCloud && (
                <FormControlLabel
                  sx={{ mt: 1.5, alignItems: 'flex-start' }}
                  control={
                    <Checkbox
                      size="small"
                      checked={activateIncludeCloud}
                      onChange={(e) => setActivateIncludeCloud(e.target.checked)}
                      disabled={fwBusy}
                    />
                  }
                  label={
                    <Box>
                      <Typography variant="body2">Include cloud controls?</Typography>
                      <Typography variant="caption" color="text.secondary" display="block">
                        ISO 27017 cloud sheet nodes for SoA and Gap. Leave off if cloud is out of
                        scope.
                      </Typography>
                    </Box>
                  }
                />
              )}
            </DialogContent>
            <DialogActions sx={{ px: 3, pb: 2 }}>
              <Button variant="text" disabled={fwBusy} onClick={() => setFwConfirm(null)}>
                Cancel
              </Button>
              <Button
                variant="contained"
                color={fwConfirm?.action === 'deactivate' ? 'error' : 'primary'}
                disabled={fwBusy}
                onClick={runFwConfirm}
              >
                {fwBusy
                  ? 'Working…'
                  : fwConfirm?.action === 'activate'
                    ? 'Activate'
                    : 'Deactivate'}
              </Button>
            </DialogActions>
          </Dialog>

          <Dialog
            open={Boolean(fwBulkActivate?.length)}
            onClose={() => !fwBusy && setFwBulkActivate(null)}
            fullWidth
            maxWidth="sm"
          >
            <DialogTitle sx={{ pb: 1 }}>
              Activate {fwBulkActivate?.length ?? 0} framework
              {(fwBulkActivate?.length ?? 0) === 1 ? '' : 's'}?
            </DialogTitle>
            <DialogContent>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                Activate the selected published frameworks for this workspace. Each stays available for
                SoA / Gap under its module.
              </Typography>
              <Box
                sx={{
                  maxHeight: 200,
                  overflow: 'auto',
                  border: `1px solid ${brand.hairline}`,
                  borderRadius: 1,
                  px: 1.25,
                  py: 0.75,
                }}
              >
                {(fwBulkActivate ?? []).map((f) => (
                  <Typography key={f.id} variant="body2" sx={{ fontFamily: MONO, py: 0.25 }}>
                    {frameworkDisplayName(f.name, f.code)} · {f.version}
                    <Typography component="span" variant="caption" color="text.secondary" sx={{ ml: 1 }}>
                      {f.name}
                    </Typography>
                  </Typography>
                ))}
              </Box>
              {(fwBulkActivate ?? []).some((f) => f.hasCloud) && (
                <FormControlLabel
                  sx={{ mt: 1.5, alignItems: 'flex-start' }}
                  control={
                    <Checkbox
                      size="small"
                      checked={activateIncludeCloud}
                      onChange={(e) => setActivateIncludeCloud(e.target.checked)}
                      disabled={fwBusy}
                    />
                  }
                  label={
                    <Box>
                      <Typography variant="body2">Include cloud controls?</Typography>
                      <Typography variant="caption" color="text.secondary" display="block">
                        Applies to selected frameworks that have a cloud sheet (e.g. ISO 27017).
                      </Typography>
                    </Box>
                  }
                />
              )}
            </DialogContent>
            <DialogActions sx={{ px: 3, pb: 2 }}>
              <Button variant="text" disabled={fwBusy} onClick={() => setFwBulkActivate(null)}>
                Cancel
              </Button>
              <Button
                variant="contained"
                disabled={fwBusy || !fwBulkActivate?.length}
                onClick={() => void runFwBulkActivate()}
                data-testid="tenant-framework-bulk-activate-confirm"
              >
                {fwBusy ? 'Working…' : `Activate ${fwBulkActivate?.length ?? 0}`}
              </Button>
            </DialogActions>
          </Dialog>

          <FrameworkViewerDialog
            open={Boolean(fwDetail)}
            detail={fwDetail}
            onClose={() => setFwDetail(null)}
          />
        </Stack>
        </>
      )}

      {active === 'audit-trail' && (
        <>
          <ModuleListShell
            title="Audit Trail"
            breadcrumbTrail="Settings › Audit Trail"
            actions={
              canExportAudit ? (
                <Button
                  size="small"
                  variant="outlined"
                  onClick={() => {
                    void api.downloadAuditTrailCsv().catch((e) => setError(String(e)))
                  }}
                >
                  Export CSV
                </Button>
              ) : undefined
            }
          />
        <Stack spacing={1.25} sx={{ flex: 1, minHeight: 0, height: '100%' }}>
        <Card sx={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden' }}>
          <TableContainer sx={{ flex: 1, minHeight: 0 }}>
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  <TableCell width={160}>When</TableCell>
                  <TableCell width={90}>Action</TableCell>
                  <TableCell width={70}>Module</TableCell>
                  <TableCell>Record</TableCell>
                  <TableCell width={120}>Actor roles</TableCell>
                  <TableCell width={70}>Auth</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {auditRows.map((row) => (
                  <TableRow key={row.id} hover>
                    <TableCell>
                      <Typography variant="caption" color="text.secondary">
                        {formatTenantDateTime(row.createdAt)}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <StatusChip
                        label={row.actionType.toLowerCase()}
                        tone={row.actionType === 'LOGIN' ? 'info' : row.actionType === 'CREATED' ? 'success' : 'neutral'}
                      />
                    </TableCell>
                    <TableCell>
                      <Typography variant="caption" fontFamily={MONO}>{row.module}</Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" fontWeight={600} noWrap>
                        {row.recordName ?? row.recordType ?? '-'}
                      </Typography>
                      <Typography variant="caption" color="text.secondary" noWrap display="block">
                        {[row.recordType, row.recordId].filter(Boolean).join(' · ') || '-'}
                        {row.fieldChanged ? ` · ${row.fieldChanged}` : ''}
                        {row.newValue ? ` → ${row.newValue}` : ''}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="caption" noWrap>{row.userRole ?? '-'}</Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="caption">{row.authMethod ?? '-'}</Typography>
                    </TableCell>
                  </TableRow>
                ))}
                {auditRows.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={6} sx={{ border: 0, height: 200, verticalAlign: 'middle' }}>
                      <Typography variant="body2" color="text.secondary" textAlign="center">
                        No audit events yet. Logins and user changes appear here.
                      </Typography>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </Card>
        </Stack>
        </>
      )}

      {active === 'evidence' && (
        <EvidencePage session={session} onError={setError} onOk={setOk} />
      )}

      {active === 'policies' && (
        <PoliciesPage session={session} onError={setError} onOk={setOk} />
      )}

      {active === WORKFLOWS_NAV_ID && (
        <WorkflowPage session={session} onError={setError} onOk={setOk} />
      )}

      {active === 'organisation' && (
        <OrganisationPage
          session={session}
          onError={setError}
          onOk={setOk}
          section="org"
          onOpenWizard={() => setWizardOpen(true)}
        />
      )}

      {active === 'notifications' && (
        <OrganisationPage
          session={session}
          onError={setError}
          onOk={setOk}
          section="notifications"
          onNavigate={goNav}
          onOpenNotification={openNotification}
        />
      )}

      {active === 'assessments-projects' && (
        <AssessmentsPage session={session} onError={setError} onOk={setOk} focus="projects" />
      )}

      {active === 'soa' && (
        <AssessmentsPage session={session} onError={setError} onOk={setOk} focus="soa" />
      )}

      {active === 'assessments' && (
        <AssessmentsPage
          session={session}
          onError={setError}
          onOk={setOk}
          focus="gap"
          openFromNotification={gapNotifyOpen}
        />
      )}

      {active === 'bcms-ncema' && (
        <AssessmentsPage session={session} onError={setError} onOk={setOk} focus="ncema" />
      )}

      {active === 'assets' && (
        <AssetsPage session={session} onError={setError} onOk={setOk} />
      )}

      {active === 'risk' && (
        <RiskPage
          key={`is-risk-${riskHomeNonce}`}
          session={session}
          riskType="IS_RISK"
          onError={setError}
          onOk={setOk}
          initialDrill={riskDrill?.riskType === 'IS_RISK' ? riskDrill : null}
          onClearDrill={() => setRiskDrill(null)}
          openFromNotification={
            riskNotifyOpen?.riskType === 'IS_RISK' ? riskNotifyOpen : null
          }
        />
      )}

      {active === 'erm-risk' && (
        <RiskPage
          key={`erm-risk-${riskHomeNonce}`}
          session={session}
          riskType="ERM"
          onError={setError}
          onOk={setOk}
          initialDrill={riskDrill?.riskType === 'ERM' ? riskDrill : null}
          onClearDrill={() => setRiskDrill(null)}
          openFromNotification={
            riskNotifyOpen?.riskType === 'ERM' ? riskNotifyOpen : null
          }
        />
      )}

      {active === 'risk-acceptance' && (
        <RiskPage
          key="is-risk-acceptance"
          session={session}
          riskType="IS_RISK"
          startOnAcceptance
          onError={setError}
          onOk={setOk}
        />
      )}

      {active === 'erm-risk-acceptance' && (
        <RiskPage
          key="erm-risk-acceptance"
          session={session}
          riskType="ERM"
          startOnAcceptance
          onError={setError}
          onOk={setOk}
        />
      )}

      {active === 'tprm-projects' && (
        <TprmProjectsPage session={session} onError={setError} onOk={setOk} />
      )}
      {active === 'tprm-vendors' && (
        <TprmPage session={session} onError={setError} onOk={setOk} onNavigate={goNav} />
      )}
      {active === 'tprm-questionnaires' && (
        <TprmQuestionnairesPage session={session} onError={setError} onOk={setOk} />
      )}

      {active === 'kri' && (
        <KriPage session={session} onError={setError} onOk={setOk} />
      )}

      {active === 'incidents' && (
        <IncidentsPage session={session} onError={setError} onOk={setOk} />
      )}

      {active === 'privacy-inventory' && (
        <PrivacyPage session={session} section="inventory" onError={setError} onOk={setOk} />
      )}

      {active === 'privacy-ropa' && (
        <PrivacyPage session={session} section="ropa" onError={setError} onOk={setOk} />
      )}

      {active === 'privacy-dsr' && (
        <PrivacyPage session={session} section="dsr" onError={setError} onOk={setOk} />
      )}

      {active === 'privacy-dpia' && (
        <PrivacyPage session={session} section="dpia-projects" onError={setError} onOk={setOk} />
      )}

      {active === 'privacy-dpia-projects' && (
        <PrivacyPage session={session} section="dpia-projects" onError={setError} onOk={setOk} />
      )}

      {active === 'privacy-dpia-questionnaires' && (
        <PrivacyPage session={session} section="dpia-questionnaires" onError={setError} onOk={setOk} />
      )}

      {active === 'privacy-risk' && (
        <PrivacyRiskPage
          key={`privacy-risk-${riskHomeNonce}`}
          session={session}
          onError={setError}
          onOk={setOk}
          openFromNotification={
            riskNotifyOpen?.riskType === 'PRIVACY_RISK' ? riskNotifyOpen : null
          }
        />
      )}

      {active === 'privacy-consent' && (
        <PrivacyPage session={session} section="consent" onError={setError} onOk={setOk} />
      )}

      {active === 'privacy-breach' && (
        <PrivacyPage session={session} section="breach" onError={setError} onOk={setOk} />
      )}

      {active === 'audit-checklists' && (
        <AuditChecklistsPage session={session} onError={setError} onOk={setOk} />
      )}

      {isAuditNavId(active) && active !== 'audit-checklists' && (
        <AuditsPage
          key={`${active}-${auditNavEpoch}`}
          session={session}
          navId={active}
          onNavChange={goNav}
          onError={setError}
          onOk={setOk}
          openFromNotification={active === AUDIT_NAV_REGISTER ? auditNotifyOpen : null}
          openCapaFromNotification={active === AUDIT_NAV_CAPA ? capaNotifyOpen : null}
        />
      )}

      {active === 'bcms-profiles' && (
        <BcmsDeptProfilesPage session={session} onError={setError} onOk={setOk} />
      )}

      {active === 'bcms-bia' && (
        <BcmsBiaPage key={`bia-${biaHomeNonce}`} session={session} onError={setError} onOk={setOk} />
      )}

      {active === 'bcms-plans' && (
        <BcmsRecoveryPlansPage session={session} onError={setError} onOk={setOk} />
      )}

      {active === 'bcms-exercises' && (
        <BcmsExercisesPage session={session} onError={setError} onOk={setOk} />
      )}

      {active === 'users' && (
        <>
          <ModuleListShell
            title="Users & Access"
            breadcrumbTrail="Settings › Users & Access"
            actions={usersListActions}
          />
          <Card sx={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden' }}>
            <RegisterToolbar
              search={userQuery}
              searchPlaceholder="Search name, email or department"
              searchWidth={260}
              onSearchChange={setUserQuery}
              onClearFilters={
                userQuery || signInFilter || accountFilter || departmentFilter
                  ? () => {
                      setUserQuery('')
                      setSignInFilter('')
                      setAccountFilter('')
                      setDepartmentFilter('')
                    }
                  : undefined
              }
              filters={[
                {
                  id: 'sign-in',
                  label: 'Sign-in',
                  value: signInFilter,
                  minWidth: 120,
                  options: [
                    { value: '', label: 'All' },
                    { value: 'SSO', label: 'SSO' },
                    { value: 'LOCAL', label: 'Local' },
                  ],
                  onChange: (v) => setSignInFilter(v as '' | 'SSO' | 'LOCAL'),
                },
                {
                  id: 'account',
                  label: 'Account',
                  value: accountFilter,
                  minWidth: 130,
                  options: [
                    { value: '', label: 'All' },
                    { value: 'ACTIVE', label: 'Active' },
                    { value: 'INVITED', label: 'Invited' },
                    { value: 'DISABLED', label: 'Disabled' },
                  ],
                  onChange: setAccountFilter,
                },
                {
                  id: 'department',
                  label: 'Department',
                  value: departmentFilter,
                  minWidth: 150,
                  options: [
                    { value: '', label: 'All' },
                    ...[...orgUnitsFlat]
                      .sort((a, b) => a.name.localeCompare(b.name))
                      .map((u) => ({ value: String(u.id), label: u.name })),
                    { value: 'NONE', label: 'No department' },
                  ],
                  onChange: setDepartmentFilter,
                },
              ]}
            />
            <RegisterListFrame
              ready={usersListReady}
              count={usersPaged.count}
              emptyMessage={users.length === 0 ? 'No users yet.' : 'No users match this search.'}
              emptyAction={
                users.length === 0 && canManageUsers ? (
                  <Button size="small" variant="contained" onClick={() => setAddUserOpen(true)}>
                    Add User
                  </Button>
                ) : undefined
              }
              page={usersPaged.page}
              rowsPerPage={usersPaged.rowsPerPage}
              onPageChange={usersPaged.setPage}
              onRowsPerPageChange={usersPaged.setRowsPerPage}
            >
            <TableContainer sx={{ flex: 1, minHeight: 0 }}>
              <Table size="small" stickyHeader>
                <TableHead>
                  <TableRow>
                    <TableCell sx={{ pl: 2 }}>User</TableCell>
                    <TableCell width={130}>Sign-in</TableCell>
                    <TableCell width={100}>Account</TableCell>
                    <TableCell width={180}>Department</TableCell>
                    <TableCell>Access</TableCell>
                    {canManageUsers && <TableCell width={56} sx={{ pr: 2 }} />}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {usersPaged.paged.map((u) => (
                    <TableRow key={u.id} hover>
                      <TableCell sx={{ pl: 2 }}>
                        <Stack direction="row" spacing={1.15} alignItems="center">
                          <Avatar sx={{ width: 28, height: 28, fontSize: '0.68rem', fontWeight: 650, bgcolor: avatarColor(u.email) }}>
                            {u.fullName.slice(0, 1).toUpperCase()}
                          </Avatar>
                          <Box sx={{ minWidth: 0 }}>
                            <Typography variant="body2" fontWeight={600} noWrap>{u.fullName}</Typography>
                            <Typography variant="caption" color="text.secondary" noWrap display="block">{u.email}</Typography>
                          </Box>
                        </Stack>
                      </TableCell>
                      <TableCell>
                        <Stack direction="row" spacing={0.4}>
                          {(u.authType === 'SSO' || (localPolicy?.ssoEnabled && localOn(u))) && (
                            <StatusChip label="SSO" tone="info" />
                          )}
                          {localOn(u) && <StatusChip label="Local" tone="neutral" />}
                        </Stack>
                      </TableCell>
                      <TableCell>
                        <StatusChip
                          label={
                            u.status === 'ACTIVE'
                              ? 'Active'
                              : u.status === 'INVITED'
                                ? 'Invited'
                                : u.status === 'DISABLED'
                                  ? 'Disabled'
                                  : u.status.charAt(0) + u.status.slice(1).toLowerCase()
                          }
                          tone={u.status === 'ACTIVE' ? 'success' : u.status === 'INVITED' ? 'warning' : 'neutral'}
                        />
                      </TableCell>
                      <TableCell sx={{ maxWidth: 180 }}>
                        {(() => {
                          const depts = userDepartments(u)
                          return depts.length ? (
                            <Typography variant="body2" noWrap title={depts.join(', ')}>
                              {depts.join(', ')}
                            </Typography>
                          ) : (
                            <Typography variant="body2" color="text.secondary">-</Typography>
                          )
                        })()}
                      </TableCell>
                      <TableCell>
                        <UserAccessCell
                          roles={u.roles}
                          adminModules={u.adminModules ?? []}
                          licensedModules={enabledModules}
                        />
                      </TableCell>
                      {canManageUsers && (
                        <TableCell sx={{ pr: 2 }}>
                          {canManageTarget(u) && (
                            <IconButton size="small" onClick={(e) => openUserMenu(e, u)} aria-label="User actions">
                              <MoreVertRoundedIcon fontSize="small" />
                            </IconButton>
                          )}
                        </TableCell>
                      )}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
            </RegisterListFrame>
          </Card>
          {canManageUsers && (
            <Dialog
              open={addUserOpen}
              onClose={() => {
                if (!creatingUser) setAddUserOpen(false)
              }}
              maxWidth="sm"
              fullWidth
            >
              <DialogTitle>Add User</DialogTitle>
              <DialogContent sx={{ '&.MuiDialogContent-root': { paddingTop: 2 } }}>
                <Stack spacing={1.75} component="fieldset" disabled={creatingUser} sx={{ border: 0, m: 0, p: 0, minWidth: 0, pt: 0.5 }}>
                  <Box>
                    <FormSelect
                      label="Sign-in"
                      fullWidth
                      required
                      value={localPolicy?.ownerOnly ? 'SSO' : authType}
                      disabled={Boolean(localPolicy?.ownerOnly) || creatingUser}
                      onChange={(v) => setAuthType(v as 'SSO' | 'LOCAL')}
                      options={
                        localPolicy?.ownerOnly
                          ? [{ value: 'SSO', label: 'SSO user' }]
                          : [
                              { value: 'SSO', label: 'SSO user' },
                              { value: 'LOCAL', label: 'Local user' },
                            ]
                      }
                    />
                    <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 0.75 }}>
                      {inviteSso
                        ? 'They sign in with their work account. Search by name or email. No password is stored here.'
                        : 'Password sign-in. Use this only when they cannot use SSO.'}
                    </Typography>
                  </Box>
                  <Box sx={twoColFieldSx}>
                    {inviteSso ? (
                      <SsoInviteEmailField
                        value={email}
                        disabled={creatingUser}
                        error={Boolean(inviteEmailMsg)}
                        helperText={inviteEmailMsg || ssoDirectoryHint}
                        onBlur={() => inviteFields.touch('email')}
                        onChange={(next) => {
                          setEmail(next)
                          setInviteDirectoryDepartment(null)
                        }}
                        onPick={applyDirectoryPerson}
                      />
                    ) : (
                      <CompactTextField
                        label="Email"
                        type="email"
                        required
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        error={Boolean(inviteEmailMsg)}
                        helperText={inviteEmailMsg || undefined}
                        onBlur={() => inviteFields.touch('email')}
                      />
                    )}
                    <CompactTextField
                      label="Full name"
                      required
                      value={fullName}
                      onChange={(e) => setFullName(e.target.value)}
                    />
                  </Box>
                  {!localPolicy?.ownerOnly && authType === 'LOCAL' && (
                    <CompactTextField
                      label="Password"
                      showPasswordToggle
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      helperText="Minimum 8 characters"
                    />
                  )}
                  <Box>
                    <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 0.5 }}>
                      What they can do
                    </Typography>
                    <FormGroup>
                      {ASSIGNABLE_ROLES.map((r) => (
                        <FormControlLabel
                          key={r}
                          sx={{ alignItems: 'flex-start', mx: 0, my: 0.25 }}
                          control={<Checkbox size="small" checked={roles.includes(r)} onChange={() => toggleRole(r)} sx={{ pt: 0.25 }} />}
                          label={
                            <Box>
                              <Typography variant="body2" fontWeight={600} sx={{ lineHeight: 1.3 }}>{roleLabel(r)}</Typography>
                              <Typography variant="caption" color="text.secondary" display="block">
                                {roleHelp[r]}
                              </Typography>
                            </Box>
                          }
                        />
                      ))}
                    </FormGroup>
                  </Box>
                  {needsAdminModules && (
                    <AdminModulePicker
                      modules={enabledModules}
                      selected={inviteAdminModules}
                      disabled={creatingUser}
                      onChange={setInviteAdminModules}
                    />
                  )}
                  {needsOrgUnit && (
                    <DepartmentPicker
                      units={orgUnitsFlat}
                      selected={inviteOrgUnitIds}
                      disabled={creatingUser}
                      hint="Where this contributor works. You can change the department Entra suggested."
                      onChange={setInviteOrgUnitIds}
                    />
                  )}
                </Stack>
              </DialogContent>
              <DialogActions>
                <Button onClick={() => setAddUserOpen(false)} disabled={creatingUser}>
                  Cancel
                </Button>
                <BusyButton
                  variant="contained"
                  busy={creatingUser}
                  disabled={createDisabled || creatingUser}
                  disabledReason={createDisabledReason}
                  onClick={() => void createUser()}
                >
                  {creatingUser ? 'Adding…' : 'Add User'}
                </BusyButton>
              </DialogActions>
            </Dialog>
          )}
          <MfaSettingsCard
            session={session}
            open={mfaOpen}
            onClose={() => setMfaOpen(false)}
            onError={setError}
            onOk={setOk}
          />
        </>
      )}

      <GuidedSetupWizard
        open={wizardOpen}
        session={session}
        onClose={() => setWizardOpen(false)}
        onError={setError}
        onOk={setOk}
      />

      <Menu anchorEl={userMenuAnchor} open={Boolean(userMenuAnchor)} onClose={closeUserMenu}>
        {userMenuTarget && (
          <>
            {!userMenuTarget.roles.includes('VENDOR') && (
              <MenuItem onClick={() => openRolesDialog(userMenuTarget)}>Edit Roles and Department</MenuItem>
            )}
            {!userMenuTarget.roles.includes('VENDOR') && localOn(userMenuTarget) && (
              <MenuItem onClick={() => openPasswordDialog(userMenuTarget)}>Reset Password</MenuItem>
            )}
            {localOn(userMenuTarget) && (
              <MenuItem onClick={() => { setUserConfirm({ action: 'disable-local', user: userMenuTarget }); closeUserMenu() }}>
                Deactivate Local Login
              </MenuItem>
            )}
            {!localOn(userMenuTarget) && canArmLocal(userMenuTarget) && (
              <MenuItem onClick={() => { setArmLocalPassword(''); setUserConfirm({ action: 'enable-local', user: userMenuTarget }); closeUserMenu() }}>
                Activate Local Login
              </MenuItem>
            )}
            {userMenuTarget.status === 'DISABLED' ? (
              <MenuItem onClick={() => void runEnableUser(userMenuTarget)}>Enable Account</MenuItem>
            ) : (
              <MenuItem onClick={() => { setUserConfirm({ action: 'deactivate', user: userMenuTarget }); closeUserMenu() }}>
                Deactivate Account
              </MenuItem>
            )}
          </>
        )}
      </Menu>

      <Dialog
        open={rolesDialogUser != null}
        onClose={() => {
          if (!savingRoles && !rolesDialogDirty) setRolesDialogUser(null)
        }}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Edit Roles and Department: {rolesDialogUser?.email}</DialogTitle>
        <DialogContent>
          <FormGroup>
            {ASSIGNABLE_ROLES.map((r) => (
              <FormControlLabel
                key={r}
                control={
                  <Checkbox
                    size="small"
                    disabled={savingRoles}
                    checked={editRoles.includes(r)}
                    onChange={() =>
                      setEditRoles((prev) =>
                        prev.includes(r) ? prev.filter((x) => x !== r) : [...prev, r],
                      )
                    }
                  />
                }
                label={roleLabel(r)}
              />
            ))}
          </FormGroup>
          {editNeedsAdminModules && (
            <Box sx={{ mt: 1.5 }}>
              <AdminModulePicker
                modules={enabledModules}
                selected={editAdminModules}
                disabled={savingRoles}
                onChange={setEditAdminModules}
              />
            </Box>
          )}
          {editNeedsOrgUnit && (
            <Box sx={{ mt: 1.5 }}>
              <DepartmentPicker
                units={orgUnitsFlat}
                selected={editOrgUnitIds}
                disabled={savingRoles}
                hint="Where this contributor works. Only contributors in a department can be picked for its tasks."
                onChange={setEditOrgUnitIds}
              />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRolesDialogUser(null)} disabled={savingRoles}>
            Cancel
          </Button>
          <BusyButton
            variant="contained"
            busy={savingRoles}
            disabled={Boolean(rolesSaveDisabledReason) || savingRoles}
            disabledReason={rolesSaveDisabledReason}
            onClick={() => void saveRolesDialog()}
          >
            {savingRoles ? 'Saving…' : 'Save'}
          </BusyButton>
        </DialogActions>
      </Dialog>

      <Dialog open={userConfirm != null} onClose={() => setUserConfirm(null)} maxWidth="xs" fullWidth>
        <DialogTitle>
          {userConfirm?.action === 'deactivate'
            ? 'Deactivate account?'
            : userConfirm?.action === 'disable-local'
              ? 'Deactivate local login?'
              : 'Activate local login?'}
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 1 }}>
            {userConfirm?.user.email}
          </Typography>
          {userConfirm?.action === 'enable-local' && (
            <CompactTextField
              label="Password"
              showPasswordToggle
              value={armLocalPassword}
              onChange={(e) => setArmLocalPassword(e.target.value)}
              helperText="Required on this screen when email is not live. Otherwise a set-password link is emailed."
            />
          )}
          {userConfirm?.action === 'disable-local' && (
            <Typography variant="body2" color="text.secondary">
              SSO sign-in stays available. This does not end an active SSO session.
            </Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setUserConfirm(null)}>Cancel</Button>
          <Button
            variant="contained"
            color={userConfirm?.action === 'enable-local' ? 'primary' : 'error'}
            onClick={() => {
              const pending = userConfirm
              setUserConfirm(null)
              if (!pending) return
              if (pending.action === 'deactivate') void runDisableUser(pending.user)
              else if (pending.action === 'disable-local') void runDisableLocal(pending.user)
              else void runEnableLocal(pending.user)
            }}
          >
            Confirm
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={passwordDialogUser != null} onClose={() => setPasswordDialogUser(null)} maxWidth="xs" fullWidth>
        <DialogTitle>Reset Password: {passwordDialogUser?.email}</DialogTitle>
        <DialogContent>
          <CompactTextField
            label="New password"
            showPasswordToggle
            value={resetPassword}
            onChange={(e) => setResetPassword(e.target.value)}
            helperText="Minimum 8 characters"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPasswordDialogUser(null)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={resetPassword.length < 8}
            onClick={() => void savePasswordDialog()}
          >
            Reset
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={!!error} autoHideDuration={5000} onClose={() => setError('')} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
        <Alert severity="error" onClose={() => setError('')}>{error}</Alert>
      </Snackbar>
      <Snackbar open={!!ok} autoHideDuration={3000} onClose={() => setOk('')} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
        <Alert severity="success" onClose={() => setOk('')}>{ok}</Alert>
      </Snackbar>
    </AppShell>
    </AppShellPageProvider>
  )
}

function ComingSoon({ title }: { title: string }) {
  return (
    <Card sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: 320 }}>
      <CardContent sx={{ textAlign: 'center', px: 3, maxWidth: 400 }}>
        <Chip label="On the roadmap" color="primary" variant="outlined" sx={{ mb: 1.75 }} />
        <Typography variant="h5" sx={{ mb: 0.75 }}>{title}</Typography>
        <Typography variant="body2" color="text.secondary">
          This module is not available yet. Identity and tenancy are live: feature modules ship next on the roadmap.
        </Typography>
      </CardContent>
    </Card>
  )
}
