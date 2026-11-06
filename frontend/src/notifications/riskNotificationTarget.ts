import { api, type NotificationItem, type RiskType } from '../api'

/** Triggers whose recordId is a risk row (not KRI / matrix-only). */
const RISK_RECORD_TRIGGERS = new Set([
  'RISK_ASSIGNED',
  'RISK_SUBMITTED',
  'RISK_RETURNED',
  'RISK_VALIDATION_REJECTED',
  'RISK_PENDING_ADMIN',
  'RISK_PENDING_OWNER',
  'RISK_TOLERATED',
  'RISK_MITIGATING',
  'TREATMENT_OVERDUE',
  'RISK_TOLERANCE_REVIEW_DUE',
  'RISK_TOLERANCE_RETURNED',
  'RISK_REVIEW_DUE',
  'RISK_NOT_RESPONDED',
  'RISK_OWNER_NOT_RESPONDED',
  'RISK_OWNER_REJECTED',
  'ADMIN_FINAL_DECISION',
  'RISK_RE_OPENED',
  'RISK_OVERDUE_ESCALATION',
  'RISK_AUTO_CLOSED',
])

export type RiskNotificationTarget = {
  navId: 'risk' | 'erm-risk' | 'privacy-risk'
  riskId: number
  riskType: RiskType
  projectId: number | null
}

export function riskNavIdForType(riskType: RiskType): 'risk' | 'erm-risk' | 'privacy-risk' {
  if (riskType === 'ERM') return 'erm-risk'
  if (riskType === 'PRIVACY_RISK') return 'privacy-risk'
  return 'risk'
}

function parseId(raw: string | null): number | null {
  if (!raw) return null
  const n = Number(raw)
  return Number.isFinite(n) && n > 0 ? n : null
}

export function riskNavId(riskType: RiskType): 'risk' | 'erm-risk' | 'privacy-risk' {
  return riskNavIdForType(riskType)
}

/** Stored links are "risk" for both IS and ERM. Resolve the register from the row. */
export async function resolveRiskNotificationTarget(
  n: NotificationItem,
): Promise<RiskNotificationTarget | null> {
  if (!RISK_RECORD_TRIGGERS.has(n.triggerType)) return null
  const riskId = parseId(n.recordId)
  if (riskId == null) return null
  const risk = await api.getRisk(riskId)
  const riskType = risk.riskType
  return {
    navId: riskNavId(riskType),
    riskId: risk.id,
    riskType,
    projectId: risk.riskProjectId ?? null,
  }
}
