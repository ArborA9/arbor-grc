import type { PriorityLevel, RiskLevel, RiskStatus, RiskSummary } from '../../api'
import type { RiskLevel as BadgeRiskLevel, LifecycleStatus } from '../ui/DomainBadge'
import {
  apiRiskLevelToBadge,
  priorityLevelToBadge,
} from '../ui/riskBadgeMappers'
import {
  parseGridSize,
  residualBandForGrid,
  type MatrixGridSize,
} from './riskMatrix'

export type { BadgeRiskLevel }

const TERMINAL: RiskStatus[] = [
  'CLOSED',
  'AUTO_CLOSED',
  'AVOIDED',
  'TRANSFERRED',
  'TOLERATED',
  'ACCEPTED',
  'CANCELLED',
]

export type RiskPriorityFilter = 'ALL' | RiskLevel
export type RiskStatusFilter =
  | 'ALL'
  | 'DRAFT'
  | 'OPEN'
  | 'MITIGATING'
  | 'CLOSED'
  | 'TOLERATED'
  | 'AVOIDED'
  | 'TRANSFERRED'
  | 'AUTO_CLOSED'
  | 'CANCELLED'

export function isOpenRisk(status: RiskStatus): boolean {
  return !TERMINAL.includes(status)
}

export function waitingMyApproval(
  risk: RiskSummary,
  meId: number | undefined,
  isAdmin: boolean,
): boolean {
  const form = risk.acceptanceForm
  if (form) {
    if (form.status === 'PENDING_RISK_OWNER' && meId != null && meId === form.approver1UserId) return true
    if (form.status === 'PENDING_APPROVER_2' && meId != null && meId === form.approver2UserId) return true
    if (form.status === 'PENDING_APPROVER_3' && meId != null && meId === form.approver3UserId) return true
  }
  return isAdmin && risk.status === 'PENDING_ADMIN_REVIEW'
}

export function computeRiskKpis(rows: RiskSummary[], gridSize: MatrixGridSize = 5) {
  const open = rows.filter((r) => isOpenRisk(r.status))
  const bandOf = (r: RiskSummary) => residualBandForGrid(r.residualRisk, r.residualRating, gridSize)
  return {
    veryHigh: open.filter((r) => bandOf(r) === 'VERY_HIGH').length,
    high: open.filter((r) => bandOf(r) === 'HIGH').length,
    medium: open.filter((r) => bandOf(r) === 'MEDIUM').length,
    low: open.filter((r) => bandOf(r) === 'LOW').length,
    veryLow: open.filter((r) => bandOf(r) === 'VERY_LOW').length,
    totalOpen: open.length,
  }
}

export function kpiCountForBand(
  kpis: {
    veryHigh: number
    high: number
    medium: number
    low: number
    veryLow: number
  },
  band: RiskLevel,
): number {
  switch (band) {
    case 'VERY_HIGH':
      return kpis.veryHigh
    case 'HIGH':
      return kpis.high
    case 'MEDIUM':
      return kpis.medium
    case 'LOW':
      return kpis.low
    case 'VERY_LOW':
      return kpis.veryLow
  }
}

export function priorityToBadgeLevel(p: PriorityLevel): BadgeRiskLevel {
  return priorityLevelToBadge(p)
}

export function riskLevelToBadgeLevel(level: RiskLevel): BadgeRiskLevel {
  return apiRiskLevelToBadge(level)
}

export function riskStatusBadge(status: RiskStatus): { status: LifecycleStatus; label: string } {
  if (status === 'MITIGATING') return { status: 'treat', label: 'IN TREATMENT' }
  if (status === 'DRAFT' || status === 'RE_OPENED') {
    return { status: 'draft', label: status.replace(/_/g, ' ') }
  }
  if (TERMINAL.includes(status)) {
    return { status: 'closed', label: status.replace(/_/g, ' ') }
  }
  return { status: 'open', label: status.replace(/_/g, ' ') }
}

export function matchesStatusFilter(status: RiskStatus, filter: RiskStatusFilter): boolean {
  if (filter === 'ALL') return true
  if (filter === 'DRAFT') return status === 'DRAFT' || status === 'RE_OPENED'
  if (filter === 'MITIGATING') return status === 'MITIGATING'
  if (filter === 'CLOSED') return status === 'CLOSED'
  if (filter === 'TOLERATED') return status === 'TOLERATED'
  if (filter === 'AVOIDED') return status === 'AVOIDED'
  if (filter === 'TRANSFERRED') return status === 'TRANSFERRED'
  if (filter === 'AUTO_CLOSED') return status === 'AUTO_CLOSED'
  if (filter === 'CANCELLED') return status === 'CANCELLED'
  if (filter === 'OPEN') {
    return isOpenRisk(status) && status !== 'MITIGATING' && status !== 'DRAFT' && status !== 'RE_OPENED'
  }
  return true
}

export function filterRiskRows(
  rows: RiskSummary[],
  opts: {
    search: string
    statusFilter: RiskStatusFilter
    priorityFilter: RiskPriorityFilter
    gridSize?: MatrixGridSize
    waitingMyApproval?: boolean
    meId?: number
    isAdmin?: boolean
  },
): RiskSummary[] {
  const q = opts.search.trim().toLowerCase()
  const grid = parseGridSize(opts.gridSize)
  return rows.filter((r) => {
    if (opts.waitingMyApproval && !waitingMyApproval(r, opts.meId, !!opts.isAdmin)) return false
    if (!matchesStatusFilter(r.status, opts.statusFilter)) return false
    if (
      opts.priorityFilter !== 'ALL' &&
      residualBandForGrid(r.residualRisk, r.residualRating, grid) !== opts.priorityFilter
    ) {
      return false
    }
    if (!q) return true
    return (
      r.displayId.toLowerCase().includes(q) ||
      r.title.toLowerCase().includes(q) ||
      r.category.toLowerCase().includes(q)
    )
  })
}

export function riskFilterCounts(rows: RiskSummary[]) {
  return {
    all: rows.length,
    draft: rows.filter((r) => matchesStatusFilter(r.status, 'DRAFT')).length,
    open: rows.filter((r) => matchesStatusFilter(r.status, 'OPEN')).length,
    mitigating: rows.filter((r) => matchesStatusFilter(r.status, 'MITIGATING')).length,
    closed: rows.filter((r) => matchesStatusFilter(r.status, 'CLOSED')).length,
    tolerated: rows.filter((r) => matchesStatusFilter(r.status, 'TOLERATED')).length,
    avoided: rows.filter((r) => matchesStatusFilter(r.status, 'AVOIDED')).length,
    transferred: rows.filter((r) => matchesStatusFilter(r.status, 'TRANSFERRED')).length,
    autoClosed: rows.filter((r) => matchesStatusFilter(r.status, 'AUTO_CLOSED')).length,
    cancelled: rows.filter((r) => matchesStatusFilter(r.status, 'CANCELLED')).length,
    veryHigh: rows.filter((r) => residualBandForGrid(r.residualRisk, r.residualRating, 5) === 'VERY_HIGH').length,
    high: rows.filter((r) => residualBandForGrid(r.residualRisk, r.residualRating, 5) === 'HIGH').length,
    medium: rows.filter((r) => residualBandForGrid(r.residualRisk, r.residualRating, 5) === 'MEDIUM').length,
    low: rows.filter((r) => residualBandForGrid(r.residualRisk, r.residualRating, 5) === 'LOW').length,
    veryLow: rows.filter((r) => residualBandForGrid(r.residualRisk, r.residualRating, 5) === 'VERY_LOW').length,
  }
}

export function treatmentLabel(option: RiskSummary['treatmentOption']): string {
  if (!option) return '-'
  return option
}

const LEVEL_RANK: Record<RiskLevel, number> = {
  VERY_LOW: 0,
  LOW: 1,
  MEDIUM: 2,
  HIGH: 3,
  VERY_HIGH: 4,
}

export type RiskSortKey = 'id' | 'inherent' | 'residual'
export type RiskSortDir = 'asc' | 'desc'

export function sortRiskRows(
  rows: RiskSummary[],
  key: RiskSortKey,
  dir: RiskSortDir,
): RiskSummary[] {
  const sorted = [...rows]
  sorted.sort((a, b) => {
    let cmp = 0
    if (key === 'id') cmp = a.displayId.localeCompare(b.displayId, undefined, { numeric: true })
    else if (key === 'inherent') cmp = LEVEL_RANK[a.inherentRisk] - LEVEL_RANK[b.inherentRisk]
    else cmp = LEVEL_RANK[a.residualRisk] - LEVEL_RANK[b.residualRisk]
    return dir === 'asc' ? cmp : -cmp
  })
  return sorted
}

export function hasActiveRiskFilters(opts: {
  search: string
  statusFilter: RiskStatusFilter
  priorityFilter: RiskPriorityFilter
  waitingMyApproval?: boolean
}): boolean {
  return (
    opts.search.trim().length > 0 ||
    opts.statusFilter !== 'ALL' ||
    opts.priorityFilter !== 'ALL' ||
    Boolean(opts.waitingMyApproval)
  )
}
