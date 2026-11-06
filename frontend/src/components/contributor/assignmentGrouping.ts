import type { WorkTaskSummary } from '../../api'
import {
  cardVisualStatus,
  contributorTab,
  CONTRIBUTOR_TABS,
  daysUntilDue,
  isOverdue,
  type CardVisualStatus,
  type ContributorTab,
} from './contribUtils'

export type { ContributorTab }

export interface ContributorAssignment {
  id: string
  tab: ContributorTab
  /** Primary line: e.g. framework name */
  title: string
  /** Secondary line: assessment / cycle / assigner */
  subtitle: string
  assignerName: string
  tasks: WorkTaskSummary[]
}

function assignerName(task: WorkTaskSummary): string {
  if (task.assignedByName?.trim()) return task.assignedByName.trim()
  if (task.assignedByEmail) return task.assignedByEmail
  return '-'
}

function groupKey(task: WorkTaskSummary, tab: ContributorTab): string {
  if (tab === 'GAP') {
    if (task.projectId != null) return `gap-project:${task.projectId}`
    if (task.detail?.trim()) return `gap:${task.detail.trim()}`
    return `gap:task:${task.id}`
  }
  if (tab === 'RISK_ACCEPTANCE') {
    return `accept:${task.sourceRecordId}`
  }
  if (tab === 'RISK') {
    if (task.projectId != null) return `risk-project:${task.projectId}`
    return `risk:task:${task.id}`
  }
  if (tab === 'ACTIVITY') {
    const head = task.title.split(': ')[0]?.trim() ?? task.title
    return `activity:${task.sourceModule}:${head}`
  }
  const head = task.title.split(': ')[0]?.trim() ?? task.title
  return `audit:${task.sourceModule}:${head}`
}

function parseGapDetail(detail: string): { assessment: string; framework: string } {
  const parts = detail.split('·').map((p) => p.trim())
  if (parts.length >= 2) {
    return { assessment: parts[0]!, framework: parts.slice(1).join(' · ') }
  }
  return { assessment: detail, framework: detail }
}

function buildAssignmentMeta(tab: ContributorTab, tasks: WorkTaskSummary[]): Pick<ContributorAssignment, 'title' | 'subtitle' | 'assignerName'> {
  const first = tasks[0]!
  const assigner = assignerName(first)
  if (tab === 'GAP') {
    const project = first.projectName?.trim()
    if (project) {
      return {
        title: project,
        subtitle: `${tasks.length} control${tasks.length === 1 ? '' : 's'} · Assigned by: ${assigner}`,
        assignerName: assigner,
      }
    }
    if (first.detail) {
      const { assessment, framework } = parseGapDetail(first.detail)
      return {
        title: framework,
        subtitle: `${assessment} · Assigned by: ${assigner}`,
        assignerName: assigner,
      }
    }
  }
  if (tab === 'RISK_ACCEPTANCE') {
    return {
      title: first.title,
      subtitle: first.detail?.trim() || 'Waiting your approval',
      assignerName: assigner,
    }
  }
  if (tab === 'RISK') {
    const project = first.projectName?.trim()
    return {
      title: project || first.title.split(': ')[0] || 'Risk review',
      subtitle: `${tasks.length} risk${tasks.length === 1 ? '' : 's'} · Assigned by: ${assigner}`,
      assignerName: assigner,
    }
  }
  return {
    title: first.title.split(': ')[0] ?? first.title,
    subtitle: `${first.sourceModule} · Assigned by: ${assigner}`,
    assignerName: assigner,
  }
}

export function groupIntoAssignments(tasks: WorkTaskSummary[], tab: ContributorTab): ContributorAssignment[] {
  const filtered = tasks.filter((t) => {
    if (contributorTab(t) !== tab) return false
    if (tab === 'RISK_ACCEPTANCE' && t.status === 'SUBMITTED') {
      const st = t.riskStatus
      if (st !== 'PENDING_ACCEPTANCE' && st !== 'PENDING_ADMIN_REVIEW') return false
    }
    return true
  })
  const buckets = new Map<string, WorkTaskSummary[]>()
  for (const task of filtered) {
    const key = groupKey(task, tab)
    const list = buckets.get(key) ?? []
    list.push(task)
    buckets.set(key, list)
  }

  const assignments: ContributorAssignment[] = []
  for (const [id, groupTasks] of buckets) {
    const sorted = [...groupTasks].sort((a, b) => (a.dueDate ?? '9999').localeCompare(b.dueDate ?? '9999'))
    const meta = buildAssignmentMeta(tab, sorted)
    assignments.push({ id, tab, ...meta, tasks: sorted })
  }

  return assignments.sort((a, b) => {
    const ad = a.tasks[0]?.dueDate ?? '9999-12-31'
    const bd = b.tasks[0]?.dueDate ?? '9999-12-31'
    return ad.localeCompare(bd)
  })
}

export function assignmentVisualStatus(assignment: ContributorAssignment): CardVisualStatus {
  const statuses = assignment.tasks.map((t) => cardVisualStatus(t))
  if (statuses.some((s) => s === 'RETURNED')) return 'RETURNED'
  if (statuses.some((s) => s === 'OVERDUE')) return 'OVERDUE'
  if (statuses.every((s) => s === 'DONE')) return 'DONE'
  if (statuses.some((s) => s === 'IN_PROGRESS') || statuses.some((s) => s === 'DONE')) return 'IN_PROGRESS'
  return 'NOT_STARTED'
}

export function assignmentProgress(assignment: ContributorAssignment): number {
  if (assignment.tasks.length === 0) return 0
  const done = assignment.tasks.filter((t) => t.status === 'SUBMITTED').length
  return Math.round((done / assignment.tasks.length) * 100)
}

export function assignmentDueLabel(assignment: ContributorAssignment): {
  text: string
  tone: 'over' | 'soon' | 'ok'
} {
  const open = assignment.tasks.filter((t) => t.status !== 'SUBMITTED')
  if (open.length === 0) return { text: 'All completed', tone: 'ok' }
  const withDue = open.filter((t) => t.dueDate)
  if (withDue.length === 0) return { text: 'No due date', tone: 'ok' }

  let worst: { days: number; task: WorkTaskSummary } | null = null
  for (const t of withDue) {
    const d = daysUntilDue(t.dueDate)
    if (d == null) continue
    if (!worst || d < worst.days) worst = { days: d, task: t }
  }
  if (!worst) return { text: 'No due date', tone: 'ok' }
  if (worst.days < 0) {
    return {
      text: `Overdue by ${Math.abs(worst.days)} day${Math.abs(worst.days) === 1 ? '' : 's'}`,
      tone: 'over',
    }
  }
  if (worst.days <= 7) {
    return {
      text: `${worst.days} day${worst.days === 1 ? '' : 's'} remaining`,
      tone: worst.days <= 3 ? 'soon' : 'ok',
    }
  }
  return {
    text: `${worst.days} days remaining`,
    tone: 'ok',
  }
}

export function countActiveAssignments(tasks: WorkTaskSummary[]): number {
  const tabs: ContributorTab[] = [...CONTRIBUTOR_TABS]
  let n = 0
  for (const tab of tabs) {
    n += groupIntoAssignments(tasks, tab).filter((a) =>
      a.tasks.some((t) => t.status !== 'SUBMITTED'),
    ).length
  }
  return n
}

/** Label for next-due banner: assignment title, not control title. */
export function nextDueAssignmentLabel(tasks: WorkTaskSummary[]): string | null {
  const open = tasks.filter((t) => t.status !== 'SUBMITTED' && t.dueDate)
  if (open.length === 0) return null
  const next = [...open].sort((a, b) => (a.dueDate ?? '').localeCompare(b.dueDate ?? ''))[0]
  if (!next) return null
  const tab = contributorTab(next)
  const assignment = groupIntoAssignments(tasks, tab).find((a) =>
    a.tasks.some((t) => t.id === next.id),
  )
  return assignment?.title ?? next.title
}

export function isAssignmentOverdue(assignment: ContributorAssignment): boolean {
  return assignment.tasks.some((t) => isOverdue(t))
}
