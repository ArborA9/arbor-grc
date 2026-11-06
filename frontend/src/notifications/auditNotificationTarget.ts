import { api, type NotificationItem } from '../api'
import { AUDIT_NAV_REGISTER } from '../components/audit/auditNav'

/** recordId is the audit. Finding logged stores the finding id instead. */
const AUDIT_ID_TRIGGERS = new Set([
  'AUDIT_PLANNED',
  'AUDIT_START_DATE',
  'AUDIT_SUBMITTED',
  'AUDIT_RETURNED',
  'AUDIT_COMPLETED',
  'AUDIT_OVERDUE',
  'AUDIT_REPORT_PENDING',
])

export type AuditNotificationTarget = {
  navId: typeof AUDIT_NAV_REGISTER
  auditId: number
}

function parseId(raw: string | null): number | null {
  if (!raw) return null
  const n = Number(raw)
  return Number.isFinite(n) && n > 0 ? n : null
}

export async function resolveAuditNotificationTarget(
  n: NotificationItem,
): Promise<AuditNotificationTarget | null> {
  const id = parseId(n.recordId)
  if (id == null) return null
  if (AUDIT_ID_TRIGGERS.has(n.triggerType)) {
    return { navId: AUDIT_NAV_REGISTER, auditId: id }
  }
  if (n.triggerType !== 'AUDIT_FINDING_LOGGED') return null
  const rows = await api.listAuditFindingsRegister()
  const row = rows.find((item) => item.findingId === id)
  return row ? { navId: AUDIT_NAV_REGISTER, auditId: row.auditId } : null
}
