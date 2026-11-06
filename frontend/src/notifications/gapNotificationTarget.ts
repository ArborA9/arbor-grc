import { api, type NotificationItem } from '../api'

const GAP_ITEM_TRIGGERS = new Set([
  'GAP_CONTROL_ASSIGNED',
  'GAP_CONTROL_SUBMITTED',
  'GAP_CONTROL_SENT_BACK',
  'GAP_CONTROL_FINALIZED',
  'GAP_NOT_RESPONDED',
])

const GAP_ASSESSMENT_TRIGGERS = new Set([
  'GAP_ASSESSMENT_CREATED',
  'GAP_ASSESSMENT_COMPLETED',
  'GAP_ASSESSMENT_COMPLETED_ASSESSOR',
])

export type GapNotificationTarget = {
  projectId: number
  gapId: number
  itemId: number | null
}

function parseId(raw: string | null): number | null {
  if (!raw) return null
  const n = Number(raw)
  return Number.isFinite(n) && n > 0 ? n : null
}

/** Resolve a Gap notification to the project / assessment (and control, when present). */
export async function resolveGapNotificationTarget(
  n: NotificationItem,
): Promise<GapNotificationTarget | null> {
  const recordId = parseId(n.recordId)
  if (recordId == null) return null
  if (GAP_ASSESSMENT_TRIGGERS.has(n.triggerType)) {
    const d = await api.getGapAssessment(recordId)
    return { projectId: d.assessment.projectId, gapId: d.assessment.id, itemId: null }
  }
  if (GAP_ITEM_TRIGGERS.has(n.triggerType)) {
    const loc = await api.locateGapItem(recordId)
    return { projectId: loc.projectId, gapId: loc.assessmentId, itemId: loc.itemId }
  }
  return null
}
