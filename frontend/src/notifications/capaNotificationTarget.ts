import type { NotificationItem } from '../api'
import { AUDIT_NAV_CAPA } from '../components/audit/auditNav'

/** recordId is the CAPA id for every CAPA trigger. */
const CAPA_ID_TRIGGERS = new Set([
  'CAPA_CREATED',
  'CAPA_ASSIGNED',
  'CAPA_SUBMITTED',
  'CAPA_CLOSED',
  'CAPA_REJECTED',
  'CAPA_DUE_SOON',
  'CAPA_OVERDUE',
  'CAPA_OVERDUE_ESCALATION',
])

export type CapaNotificationTarget = {
  navId: typeof AUDIT_NAV_CAPA
  capaId: number
}

export function resolveCapaNotificationTarget(n: NotificationItem): CapaNotificationTarget | null {
  if (!CAPA_ID_TRIGGERS.has(n.triggerType)) return null
  const capaId = Number(n.recordId)
  if (!Number.isFinite(capaId) || capaId <= 0) return null
  return { navId: AUDIT_NAV_CAPA, capaId }
}
