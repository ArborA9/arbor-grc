import SpaceDashboardRoundedIcon from '@mui/icons-material/SpaceDashboardRounded'
import AssignmentTurnedInRoundedIcon from '@mui/icons-material/AssignmentTurnedInRounded'
import FactCheckRoundedIcon from '@mui/icons-material/FactCheckRounded'
import GavelRoundedIcon from '@mui/icons-material/GavelRounded'
import HealthAndSafetyRoundedIcon from '@mui/icons-material/HealthAndSafetyRounded'
import ListAltRoundedIcon from '@mui/icons-material/ListAltRounded'
import PrivacyTipRoundedIcon from '@mui/icons-material/PrivacyTipRounded'
import ReportProblemRoundedIcon from '@mui/icons-material/ReportProblemRounded'
import SecurityRoundedIcon from '@mui/icons-material/SecurityRounded'
import SpeedRoundedIcon from '@mui/icons-material/SpeedRounded'
import TaskAltRoundedIcon from '@mui/icons-material/TaskAltRounded'
import TimelineRoundedIcon from '@mui/icons-material/TimelineRounded'
import WarningAmberRoundedIcon from '@mui/icons-material/WarningAmberRounded'
import ArticleRoundedIcon from '@mui/icons-material/ArticleRounded'
import BusinessCenterRoundedIcon from '@mui/icons-material/BusinessCenterRounded'
import type { WorkTaskSummary } from '../../api'
import type { NavItem } from '../AppShell'
import { isRiskNavHidden } from '../risk/riskNav'
import { contributorTab } from './contribUtils'

/** Admin-shaped nav. Children appear only when the Contributor has assigned work there. */
export const CONTRIBUTOR_NAV_CATALOG: NavItem[] = [
  {
    id: 'contributor-dashboard',
    label: 'Dashboard',
    icon: <SpaceDashboardRoundedIcon fontSize="small" />,
  },
  {
    id: 'assessments',
    label: 'Assessments',
    icon: <AssignmentTurnedInRoundedIcon fontSize="small" />,
  },
  {
    id: 'risk-manager',
    label: 'Risk Management',
    icon: <SecurityRoundedIcon fontSize="small" />,
    children: [
      { id: 'risk', label: 'Information Security Risk', icon: <WarningAmberRoundedIcon fontSize="small" /> },
      { id: 'erm-risk', label: 'Enterprise Risk', icon: <BusinessCenterRoundedIcon fontSize="small" /> },
      { id: 'kri', label: 'KRI', icon: <SpeedRoundedIcon fontSize="small" /> },
      { id: 'incidents', label: 'Incidents', icon: <ReportProblemRoundedIcon fontSize="small" /> },
    ].filter((child) => !isRiskNavHidden(child.id)),
  },
  {
    id: 'audit',
    label: 'Audit',
    icon: <GavelRoundedIcon fontSize="small" />,
    children: [
                      { id: 'audit-capa', label: 'CAPA Tracker', icon: <TaskAltRoundedIcon fontSize="small" /> },
    ],
  },
  {
    id: 'business-continuity',
    label: 'Business Continuity',
    icon: <HealthAndSafetyRoundedIcon fontSize="small" />,
    children: [
      { id: 'bcms-bia', label: 'BIA', icon: <TimelineRoundedIcon fontSize="small" /> },
    ],
  },
  {
    id: 'privacy',
    label: 'Privacy',
    icon: <PrivacyTipRoundedIcon fontSize="small" />,
    children: [
      { id: 'privacy-ropa', label: 'ROPA', icon: <ListAltRoundedIcon fontSize="small" /> },
      { id: 'privacy-dpia-projects', label: 'DPIA', icon: <FactCheckRoundedIcon fontSize="small" /> },
      { id: 'privacy-risk', label: 'Privacy Risk', icon: <FactCheckRoundedIcon fontSize="small" /> },
    ],
  },
  { id: 'policies', label: 'Policies', icon: <ArticleRoundedIcon fontSize="small" /> },
]

export function contributorNavIdForTask(task: WorkTaskSummary): string {
  const tab = contributorTab(task)
  if (tab === 'GAP') return 'assessments'
  if (tab === 'AUDIT') return 'audit-capa'
  if (tab === 'RISK' || tab === 'RISK_ACCEPTANCE') {
    if (task.riskType === 'ERM') return 'erm-risk'
    if (task.riskType === 'PRIVACY_RISK') return 'privacy-risk'
    return 'risk'
  }
  if (task.sourceModule === 'BCMS') return 'bcms-bia'
  if (task.sourceModule === 'PRIVACY' && task.sourceRecordType === 'PRIVACY_DPIA') return 'privacy-dpia-projects'
  if (task.sourceModule === 'PRIVACY' && (task.sourceRecordType === 'DPIA_DEPARTMENT' || task.sourceRecordType === 'DPIA_HEAD')) {
    return 'privacy-dpia-projects'
  }
  if (task.sourceModule === 'PRIVACY') return 'privacy-ropa'
  if (task.sourceModule === 'POLICY') return 'policies'
  if (task.sourceModule === 'KRI') return 'kri'
  if (task.sourceModule === 'INCIDENT') return 'incidents'
  return 'assessments'
}

export function taskMatchesContributorNav(task: WorkTaskSummary, navId: string): boolean {
  if (navId === 'overview' || navId === 'contributor-dashboard') return false
  return contributorNavIdForTask(task) === navId
}

export function contributorNavIdsFromTasks(tasks: WorkTaskSummary[]): Set<string> {
  const ids = new Set<string>(['contributor-dashboard'])
  for (const task of tasks) {
    const id = contributorNavIdForTask(task)
    if (isRiskNavHidden(id)) continue
    ids.add(id)
    if (id === 'risk' || id === 'erm-risk') {
      ids.add('risk-manager')
    }
    if (id === 'audit-capa') ids.add('audit')
    if (id === 'bcms-bia') ids.add('business-continuity')
    if (id === 'privacy-ropa' || id === 'privacy-dpia' || id === 'privacy-dpia-projects' || id === 'privacy-risk') {
      ids.add('privacy')
    }
  }
  return ids
}

export function filterNavForContributor<T extends { id: string; children?: T[] }>(
  items: T[],
  tasks: WorkTaskSummary[],
  extraIds: string[] = [],
): T[] {
  const allow = contributorNavIdsFromTasks(tasks)
  for (const id of extraIds) {
    allow.add(id)
    if (id === 'risk' || id === 'erm-risk') allow.add('risk-manager')
  }
  return items
    .map((item) => {
      if (!allow.has(item.id)) return null
      if (!item.children?.length) return item
      const children = item.children.filter((c) => allow.has(c.id))
      if (!children.length) return null
      return { ...item, children }
    })
    .filter((item): item is T => item != null)
}

export function firstContributorNavId<T extends { id: string; children?: T[] }>(items: T[]): string | null {
  for (const item of items) {
    if (item.children?.length) return item.children[0].id
    return item.id
  }
  return null
}

export function navContainsId<T extends { id: string; children?: T[] }>(items: T[], id: string): boolean {
  return items.some((item) => item.id === id || (item.children != null && navContainsId(item.children, id)))
}

export function contributorNavTitle(navId: string): string {
  switch (navId) {
    case 'contributor-dashboard':
      return 'Dashboard'
    case 'assessments':
      return 'Assessments'
    case 'risk':
      return 'Information Security Risk'
    case 'erm-risk':
      return 'Enterprise Risk'
    case 'kri':
      return 'KRI'
    case 'incidents':
      return 'Incidents'
    case 'audit-capa':
      return 'CAPA Tracker'
    case 'bcms-bia':
      return 'BIA'
    case 'privacy-ropa':
      return 'ROPA'
    case 'privacy-dpia':
    case 'privacy-dpia-projects':
      return 'DPIA'
    case 'privacy-risk':
      return 'Privacy Risk'
    case 'policies':
      return 'Policies'
    default:
      return 'Assessments'
  }
}

export function contributorWorkBackLabel(navId: string): string {
  if (navId === 'contributor-dashboard') return 'Dashboard'
  if (navId === 'risk') return 'Information Security Risk'
  if (navId === 'erm-risk') return 'Enterprise Risk'
  if (navId === 'assessments') return 'Assessments'
  return contributorNavTitle(navId)
}

export function contributorNavCaption(navId: string): string {
  if (navId === 'contributor-dashboard') {
    return ''
  }
  return `Assigned ${contributorNavTitle(navId)} work only. You cannot create or manage records you were not assigned.`
}
