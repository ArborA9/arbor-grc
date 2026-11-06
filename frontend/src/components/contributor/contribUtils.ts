import type { WorkTaskSummary, WorkTaskStatus } from '../../api'
import { brand, semantic } from '../../theme'

export type ContributorTab = 'GAP' | 'RISK' | 'RISK_ACCEPTANCE' | 'AUDIT' | 'ACTIVITY'

export const CONTRIBUTOR_TABS: ContributorTab[] = ['GAP', 'RISK', 'RISK_ACCEPTANCE', 'AUDIT', 'ACTIVITY']

const ACTIVITY_MODULES = new Set(['BCMS', 'PRIVACY', 'POLICY', 'KRI', 'INCIDENT'])

export function contributorTab(task: WorkTaskSummary): ContributorTab {
  if (task.sourceModule === 'GAP') return 'GAP'
  if (task.sourceModule === 'RISK' && task.sourceRecordType === 'RISK_ACCEPTANCE') return 'RISK_ACCEPTANCE'
  if (task.sourceModule === 'RISK') return 'RISK'
  if (task.sourceModule === 'CAPA' || task.sourceModule === 'AUDIT') return 'AUDIT'
  if (ACTIVITY_MODULES.has(task.sourceModule)) return 'ACTIVITY'
  return 'AUDIT'
}

export function tabLabel(tab: ContributorTab): string {
  switch (tab) {
    case 'GAP':
      return 'Gap Assessments'
    case 'RISK':
      return 'Risk Reviews'
    case 'RISK_ACCEPTANCE':
      return 'Risk Acceptance'
    case 'ACTIVITY':
      return 'Activity'
    default:
      return 'Audit'
  }
}

export type CardVisualStatus = 'OVERDUE' | 'RETURNED' | 'IN_PROGRESS' | 'NOT_STARTED' | 'DONE'

export function isReturned(task: WorkTaskSummary): boolean {
  return Boolean(task.returned) && task.status !== 'SUBMITTED'
}

export function isOverdue(task: WorkTaskSummary): boolean {
  if (task.status === 'SUBMITTED' || !task.dueDate) return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const due = new Date(`${task.dueDate}T00:00:00`)
  return due < today
}

export function cardVisualStatus(task: WorkTaskSummary): CardVisualStatus {
  if (task.status === 'SUBMITTED') return 'DONE'
  if (isReturned(task)) return 'RETURNED'
  if (isOverdue(task)) return 'OVERDUE'
  if (task.status === 'IN_PROGRESS') return 'IN_PROGRESS'
  return 'NOT_STARTED'
}

export function daysUntilDue(dueDate: string | null): number | null {
  if (!dueDate) return null
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const due = new Date(`${dueDate}T00:00:00`)
  return Math.round((due.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
}

export function dueLabel(task: WorkTaskSummary): { text: string; tone: 'over' | 'soon' | 'ok' } {
  if (task.status === 'SUBMITTED') {
    return { text: 'Completed', tone: 'ok' }
  }
  const days = daysUntilDue(task.dueDate)
  if (days == null) return { text: 'No due date', tone: 'ok' }
  if (days < 0) return { text: `Overdue by ${Math.abs(days)} day${Math.abs(days) === 1 ? '' : 's'}`, tone: 'over' }
  if (days <= 7) return { text: `${days} day${days === 1 ? '' : 's'} remaining`, tone: days <= 3 ? 'soon' : 'ok' }
  return { text: `${days} days remaining`, tone: 'ok' }
}

export function progressPct(task: WorkTaskSummary): number {
  if (task.status === 'SUBMITTED') return 100
  if (task.status === 'IN_PROGRESS') return 50
  return 0
}

export function statusBadge(status: CardVisualStatus): { label: string; bg: string; color: string } {
  switch (status) {
    case 'OVERDUE':
      return { label: 'OVERDUE', bg: '#FEE2E2', color: '#991B1B' }
    case 'RETURNED':
      return { label: 'RETURNED FOR REVIEW', bg: semantic.riskChip.medium.bg, color: semantic.riskChip.medium.text }
    case 'IN_PROGRESS':
      return { label: 'IN PROGRESS', bg: brand.goldLight, color: brand.goldDark }
    case 'DONE':
      return { label: 'COMPLETED', bg: semantic.gap.compliant.bg, color: semantic.gap.compliant.text }
    default:
      return { label: 'NOT STARTED', bg: '#F3F4F6', color: '#6B7280' }
  }
}

export function greetingName(email: string): string {
  const local = email.split('@')[0] ?? email
  const parts = local.replace(/[._-]+/g, ' ').split(' ').filter(Boolean)
  if (parts.length === 0) return email
  return parts.map((p) => p.charAt(0).toUpperCase() + p.slice(1)).join(' ')
}

export function greetingTime(): string {
  const h = new Date().getHours()
  if (h < 12) return 'Good morning'
  if (h < 17) return 'Good afternoon'
  return 'Good evening'
}

export function initials(email: string): string {
  const name = greetingName(email)
  const bits = name.split(' ').filter(Boolean)
  if (bits.length >= 2) return `${bits[0]![0]}${bits[1]![0]}`.toUpperCase()
  return name.slice(0, 2).toUpperCase()
}

export function sortTasks(tasks: WorkTaskSummary[], asc: boolean): WorkTaskSummary[] {
  return [...tasks].sort((a, b) => {
    const ad = a.dueDate ?? '9999-12-31'
    const bd = b.dueDate ?? '9999-12-31'
    return asc ? ad.localeCompare(bd) : bd.localeCompare(ad)
  })
}
