import type { AuditChecklistItem, AuditFinding, GrcAuditDetail } from '../../api'
import { formatTenantDate } from '../../uiQuality/tenantLocale'
import { isCloudControlCode } from '../gap/gapUi'

export type EngagementWorkspaceTab = 'overview' | 'controls' | 'findings'

export function auditStatusTone(s: string): 'success' | 'warning' | 'info' | 'danger' | 'neutral' {
  if (s === 'CLOSED' || s === 'COMPLETED') return 'success'
  if (s === 'REPORT_ISSUED' || s === 'PENDING_ADMIN' || s === 'PENDING_VERIFICATION') return 'warning'
  if (s === 'IN_PROGRESS' || s === 'ACCESS_GRANTED' || s === 'OPEN') return 'info'
  if (s === 'CANCELLED' || s === 'RETURNED' || s === 'OVERDUE') return 'danger'
  return 'neutral'
}

export function auditStatusLabel(status: string, findingCount?: number): string {
  if (status === 'CLOSED') {
    const n = findingCount ?? 0
    return n === 0 ? 'Closed with no findings' : `Closed with ${n} finding${n === 1 ? '' : 's'}`
  }
  switch (status) {
    case 'DRAFT':
      return 'Draft'
    case 'ACCESS_GRANTED':
      return 'Access granted'
    case 'IN_PROGRESS':
      return 'In progress'
    case 'REPORT_ISSUED':
      return 'Report issued'
    case 'CANCELLED':
      return 'Cancelled'
    default:
      return status.replace(/_/g, ' ')
  }
}

export function engagementDaysRemaining(endDate: string | null | undefined): number | null {
  if (!endDate) return null
  const end = new Date(`${endDate}T23:59:59`)
  if (Number.isNaN(end.getTime())) return null
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.ceil((end.getTime() - today.getTime()) / 86_400_000)
}

function isConcluded(item: AuditChecklistItem): boolean {
  return Boolean(item.conclusion && item.conclusion !== 'NOT_TESTED')
}

export function engagementKpis(detail: GrcAuditDetail) {
  const inScope = detail.checklist.filter((c) => c.inScope !== false)
  const total = inScope.length
  const assessmentMode = detail.audit.auditMode === 'ASSESSMENT'
  const reviewed = inScope.filter((c) =>
    assessmentMode ? c.assessment != null : isConcluded(c),
  ).length
  const findings = detail.findings.length
  const daysRemaining = engagementDaysRemaining(detail.audit.endDate)
  const progressPct = total > 0 ? Math.round((reviewed / total) * 100) : 0
  const compliant = inScope.filter((c) => c.assessment === 'YES').length
  const partial = inScope.filter((c) => c.assessment === 'PARTIAL').length
  const nonCompliant = inScope.filter((c) => c.assessment === 'NO').length
  return { total, reviewed, findings, daysRemaining, progressPct, compliant, partial, nonCompliant }
}

export function selfAssessmentLabel(value: string | null | undefined): string {
  switch (value) {
    case 'YES':
      return 'Compliant'
    case 'PARTIAL':
      return 'Partial'
    case 'NO':
      return 'Non-compliant'
    default:
      return '-'
  }
}

export function selfAssessmentTone(
  value: string | null | undefined,
): 'success' | 'warning' | 'danger' | 'neutral' {
  switch (value) {
    case 'YES':
      return 'success'
    case 'PARTIAL':
      return 'warning'
    case 'NO':
      return 'danger'
    default:
      return 'neutral'
  }
}

export function controlReviewStatus(
  item: AuditChecklistItem,
): 'NOT_STARTED' | 'IN_REVIEW' | 'REVIEWED' {
  if (!item.assessment) return 'NOT_STARTED'
  if (item.notes?.trim()) return 'REVIEWED'
  return 'IN_REVIEW'
}

export function controlReviewLabel(status: ReturnType<typeof controlReviewStatus>): string {
  switch (status) {
    case 'NOT_STARTED':
      return 'Not started'
    case 'IN_REVIEW':
      return 'In review'
    case 'REVIEWED':
      return 'Reviewed'
  }
}

export function controlReviewTone(
  status: ReturnType<typeof controlReviewStatus>,
): 'neutral' | 'info' | 'success' {
  switch (status) {
    case 'NOT_STARTED':
      return 'neutral'
    case 'IN_REVIEW':
      return 'info'
    case 'REVIEWED':
      return 'success'
  }
}

export function scopeModuleLabel(m: string): string {
  switch (m) {
    case 'DASHBOARD':
      return 'Dashboard'
    case 'GAP':
      return 'Gap'
    case 'SOA':
      return 'SoA'
    case 'RISK':
      return 'Risk'
    case 'ERM':
      return 'ERM'
    case 'EVIDENCE':
      return 'Evidence'
    case 'BCMS':
      return 'BCMS'
    case 'PRIVACY':
      return 'Privacy'
    case 'ASSET':
      return 'Assets'
    default:
      return m
  }
}

export function auditAssignedLabel(
  a: { leadAuditorName?: string | null; leadAuditorUserId: number },
  fallback?: (id: number) => string,
): string {
  const named = a.leadAuditorName?.trim()
  if (named) return named
  if (fallback) return fallback(a.leadAuditorUserId)
  return a.leadAuditorUserId ? `#${a.leadAuditorUserId}` : '-'
}

export function auditPeriodLabel(
  startDate: string | null | undefined,
  endDate: string | null | undefined,
): string {
  if (!startDate && !endDate) return '-'
  const start = startDate ? formatTenantDate(startDate) : '-'
  const end = endDate ? formatTenantDate(endDate) : '-'
  return `${start} - ${end}`
}

export function auditModulesLabel(modules: string[] | null | undefined): string {
  if (!modules?.length) return '-'
  return modules.map(scopeModuleLabel).join(', ')
}

function frameworkTokenLabel(token: string): string {
  switch (token) {
    case 'ISO_27001':
      return 'ISO 27001'
    case 'ISO_20000':
      return 'ISO 20000'
    case 'UAE_IA':
      return 'UAE IA'
    case 'ISO_27701':
      return 'ISO 27701'
    default:
      return token
  }
}

/** Framework column. Multiple stays the word Multiple, not the checklist names. */
export function auditFrameworkNames(a: {
  auditStandard?: string | null
  frameworkCode?: string | null
  frameworkName?: string | null
}): string[] {
  if (a.auditStandard === 'MULTIPLE' || a.frameworkCode === 'MULTIPLE') return ['Multiple']
  const code = a.frameworkCode?.trim()
  const raw = code && code !== '-' ? code : (a.frameworkName?.trim() ?? '')
  if (!raw || raw === '-') return []
  return raw
    .split(',')
    .map((part) => frameworkTokenLabel(part.trim()))
    .filter(Boolean)
}

/** Single standard shows its name. Multiple shows Multiple. */
export function auditFrameworkLabel(a: {
  auditStandard?: string | null
  frameworkCode?: string | null
  frameworkName?: string | null
}): string {
  const names = auditFrameworkNames(a)
  return names.length ? names.join(', ') : '-'
}

export function findingSeverityTone(sev: string): 'danger' | 'warning' | 'info' | 'neutral' {
  if (sev === 'MAJOR_NC') return 'danger'
  if (sev === 'MINOR_NC') return 'warning'
  if (sev === 'OBSERVATION') return 'info'
  return 'neutral'
}

export function findingSeverityShort(sev: string): string {
  switch (sev) {
    case 'MAJOR_NC':
      return 'Major NC'
    case 'MINOR_NC':
      return 'Minor NC'
    case 'OBSERVATION':
      return 'Observation'
    case 'OFI':
      return 'OFI'
    default:
      return sev
  }
}

/** Mock Screen 2: finding column / card severity label. */
export function findingTableLabel(sev: string): string {
  switch (sev) {
    case 'MAJOR_NC':
      return 'Major Finding'
    case 'MINOR_NC':
      return 'Minor Finding'
    case 'OBSERVATION':
      return 'Observation'
    case 'OFI':
      return 'OFI'
    default:
      return sev
  }
}

export function formatEngagementDate(iso: string | null | undefined): string {
  if (!iso) return '-'
  return formatTenantDate(iso)
}

export function findingWorkflowStatus(
  finding: AuditFinding,
  capa?: { status: string; id: number } | null,
): { label: string; tone: 'warning' | 'info' | 'success' | 'neutral' } {
  if (!finding.capaId) {
    return { label: 'Awaiting CAPA', tone: 'warning' }
  }
  if (!capa) {
    return { label: 'CAPA in progress', tone: 'info' }
  }
  if (capa.status === 'CLOSED' || capa.status === 'VERIFIED') {
    return { label: 'Closed', tone: 'success' }
  }
  return { label: 'CAPA in progress', tone: 'info' }
}

export function findingWorkflowMeta(
  finding: AuditFinding,
  capa?: { id: number; ownerUserId: number | null; status: string } | null,
  ownerName?: string,
): string {
  const raised = `Raised ${formatEngagementDate(finding.createdAt)}`
  const severity = `Severity: ${findingSeverityShort(finding.severity).replace(' NC', '')}`
  if (!finding.capaId) {
    return `${raised} · ${severity} · Assigned to Admin for CAPA`
  }
  if (capa) {
  const owner = ownerName ? ` assigned to ${ownerName}` : ''
    return `${raised} · ${severity} · CAPA-${String(capa.id).padStart(3, '0')}${owner}`
  }
  return `${raised} · ${severity}`
}

export type DomainGroup = {
  code: string
  name: string
  narrative: string
  scoreLabel: string | null
  controls: AuditChecklistItem[]
  findings: AuditFinding[]
}

export function groupByDomain(detail: GrcAuditDetail): DomainGroup[] {
  const narratives = detail.domainNarratives ?? []
  const domainsFromGap = detail.gapLayerScores?.domains ?? []
  const domainDefs =
    narratives.length > 0
      ? narratives.map((n) => ({
          code: n.code,
          name: n.name,
          narrative: n.narrative ?? '',
          score: domainsFromGap.find((d) => d.code === n.code) ?? null,
        }))
      : domainsFromGap.map((d) => ({
          code: d.code,
          name: d.name,
          narrative: '',
          score: d,
        }))

  const groups: DomainGroup[] = domainDefs.map((d) => ({
    code: d.code,
    name: d.name,
    narrative: d.narrative,
    scoreLabel:
      d.score?.score != null ? `${d.score.score}%${d.score.rag ? ` (${d.score.rag})` : ''}` : null,
    controls: [],
    findings: [],
  }))

  const other: DomainGroup = {
    code: '_other',
    name: 'Other controls',
    narrative: '',
    scoreLabel: null,
    controls: [],
    findings: [],
  }

  const cloud: DomainGroup = {
    code: 'CLOUD',
    name: 'Cloud (ISO 27017)',
    narrative: '',
    scoreLabel: null,
    controls: [],
    findings: [],
  }

  const bucketForCode = (ref: string | null | undefined): DomainGroup => {
    if (!ref) return other
    if (isCloudControlCode(ref)) return cloud
    const upper = ref.toUpperCase()
    const match = groups.find(
      (g) => upper === g.code.toUpperCase() || upper.startsWith(`${g.code.toUpperCase()}.`),
    )
    return match ?? other
  }

  for (const item of detail.checklist) {
    bucketForCode(item.nodeCode).controls.push(item)
  }
  for (const f of detail.findings) {
    bucketForCode(f.controlRef).findings.push(f)
  }

  const out = groups.filter((g) => g.controls.length > 0 || g.findings.length > 0 || g.narrative)
  if (cloud.controls.length > 0 || cloud.findings.length > 0) out.push(cloud)
  if (other.controls.length > 0 || other.findings.length > 0) out.push(other)
  if (out.length === 0 && detail.checklist.length > 0) {
    return [
      {
        code: 'all',
        name: 'Controls',
        narrative: '',
        scoreLabel: null,
        controls: [...detail.checklist],
        findings: [...detail.findings],
      },
    ]
  }
  return out
}
