import type { CapaRecord } from '../../api'

export type AuditCapaRegister = 'INTERNAL_AUDIT' | 'EXTERNAL_AUDIT'

export const REGISTER_META: Record<
  AuditCapaRegister,
  { tabLabel: string; title: string; subtitle: string; registerNumber: 2 | 3 }
> = {
  INTERNAL_AUDIT: {
    tabLabel: 'Register 2: Internal',
    title: 'CAPA Register 2',
    subtitle: 'Internal audit findings: corrective actions from internal engagements',
    registerNumber: 2,
  },
  EXTERNAL_AUDIT: {
    tabLabel: 'Register 3: External',
    title: 'CAPA Register 3',
    subtitle: 'External audit findings: certification and third-party engagement CAPAs',
    registerNumber: 3,
  },
}

export function capaRef(id: number): string {
  return `CAPA-${String(id).padStart(3, '0')}`
}

export function capaRegisterStats(capas: CapaRecord[]) {
  const total = capas.length
  const overdue = capas.filter((c) => c.status === 'OVERDUE').length
  const inProgress = capas.filter((c) =>
    ['IN_PROGRESS', 'OPEN', 'PENDING_VERIFICATION'].includes(c.status),
  ).length
  const closed = capas.filter((c) => c.status === 'CLOSED').length
  return { total, overdue, inProgress, closed }
}

export function capaSourceLabel(c: CapaRecord): string {
  if (c.registerCode === 'EXTERNAL_AUDIT') return 'External'
  if (c.registerCode === 'OTHER') return c.sourceOther?.trim() || 'Other'
  if (c.registerCode === 'INTERNAL_AUDIT') return 'Internal'
  return c.registerCode
}

export function capaSourceLineage(c: CapaRecord): string {
  if (c.sourceType === 'AUDIT_FINDING' && c.sourceFindingDisplayCode && c.sourceAuditName) {
    return `${c.sourceFindingDisplayCode} · ${c.sourceAuditName}`
  }
  if (c.sourceType === 'MANUAL') return c.sourceFindingDisplayCode || 'Manual CAPA'
  return c.sourceType.replace(/_/g, ' ')
}
