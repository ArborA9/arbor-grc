import type { EvidenceSummary, TprmAnswer, TprmAssessment, TprmAssessmentItem } from '../../api'
import { ApiError } from '../../api'

export const MAX_EVIDENCE_FILES = 5

export const ANSWER_OPTIONS: { value: TprmAnswer | ''; label: string }[] = [
  { value: '', label: 'Select' },
  { value: 'YES', label: 'Yes' },
  { value: 'PARTIAL', label: 'Partial' },
  { value: 'NO', label: 'No' },
  { value: 'NA', label: 'N/A' },
]

export function portalStatusLabel(status: string): string {
  if (status === 'RETURNED') return 'Returned for update'
  if (status === 'IN_PROGRESS') return 'In progress'
  if (status === 'ISSUED') return 'Assigned'
  if (status === 'SUBMITTED') return 'Submitted'
  if (status === 'COMPLETE') return 'Closed'
  return status.replace(/_/g, ' ')
}

export function itemEvidenceFiles(it: TprmAssessmentItem) {
  if (it.evidenceFiles && it.evidenceFiles.length > 0) return it.evidenceFiles
  if (it.evidenceId) {
    return [{ id: it.evidenceId, filename: it.evidenceFilename?.trim() || `evidence-${it.evidenceId}` }]
  }
  return []
}

export function evidenceState(it: TprmAssessmentItem) {
  const files = itemEvidenceFiles(it)
  const ref = it.evidenceRef?.trim() || ''
  const waived = it.vendorAnswer === 'NA'
  const required = Boolean(it.evidenceMandatory && !waived)
  const fileCount = files.length
  return {
    required,
    missing: required && fileCount === 0,
    files,
    fileCount,
    ref,
  }
}

export function toEvidenceSummaries(it: TprmAssessmentItem): EvidenceSummary[] {
  return itemEvidenceFiles(it).map((f) => ({
    id: f.id,
    title: f.filename,
    classification: 'OPEN' as const,
    status: 'ACTIVE' as const,
    expiresOn: null,
    currentVersion: 1,
    latestFilename: f.filename,
    latestSizeBytes: 0,
    linkCount: 1,
    updatedAt: '',
  }))
}

export function groupByDomain(items: TprmAssessmentItem[]) {
  const groups: { domain: string; items: TprmAssessmentItem[] }[] = []
  for (const it of items) {
    const last = groups[groups.length - 1]
    if (last && last.domain === it.domain) last.items.push(it)
    else groups.push({ domain: it.domain, items: [it] })
  }
  return groups
}

export function domainAnchor(domain: string) {
  const slug = (domain || 'questions').trim().toLowerCase().replace(/[^a-z0-9]+/g, '-')
  return `vq-domain-${slug || 'questions'}`
}

export function patchItem(assessment: TprmAssessment, id: number, patch: Partial<TprmAssessmentItem>): TprmAssessment {
  return {
    ...assessment,
    items: assessment.items.map((row) => (row.id === id ? { ...row, ...patch } : row)),
  }
}

/** Keep local draft answers when a server response would otherwise wipe unsaved work. */
export function mergeAssessmentPreservingLocal(
  local: TprmAssessment,
  server: TprmAssessment,
  opts?: { preferServerAnswers?: boolean },
): TprmAssessment {
  const preferServer = opts?.preferServerAnswers === true
  const byId = new Map(server.items.map((it) => [it.id, it]))
  return {
    ...server,
    canSubmit: server.canSubmit ?? local.canSubmit,
    items: local.items.map((localItem) => {
      const remote = byId.get(localItem.id)
      if (!remote) return localItem
      if (preferServer) {
        return {
          ...localItem,
          ...remote,
          vendorAnswer: remote.vendorAnswer ?? localItem.vendorAnswer,
          justification: remote.justification ?? localItem.justification,
          evidenceRef: remote.evidenceRef ?? localItem.evidenceRef,
        }
      }
      return {
        ...localItem,
        evidenceId: remote.evidenceId,
        evidenceFilename: remote.evidenceFilename,
        evidenceFiles: remote.evidenceFiles,
        vendorAnswer: localItem.vendorAnswer ?? remote.vendorAnswer,
        justification: localItem.justification ?? remote.justification,
        evidenceRef: localItem.evidenceRef?.trim() ? localItem.evidenceRef : remote.evidenceRef,
        reviewVerdict: remote.reviewVerdict,
        reviewComment: remote.reviewComment,
        returned: remote.returned,
        unchangedSinceReturn: remote.unchangedSinceReturn,
      }
    }),
  }
}

export function answersSnapshot(items: TprmAssessmentItem[]): string {
  return JSON.stringify(
    items.map((it) => ({
      id: it.id,
      vendorAnswer: it.vendorAnswer,
      justification: it.justification ?? '',
      evidenceRef: it.evidenceRef ?? '',
      evidenceFiles: itemEvidenceFiles(it).map((f) => f.id),
    })),
  )
}

export function submitBlockers(
  items: TprmAssessmentItem[],
  returnBaselines?: Record<number, string>,
): { code: string; reason: string }[] {
  const blockers: { code: string; reason: string }[] = []
  for (const it of items) {
    if (!it.vendorAnswer) {
      blockers.push({ code: it.controlCode, reason: 'Answer is required' })
      continue
    }
    if (!(it.justification ?? '').trim()) {
      blockers.push({ code: it.controlCode, reason: 'Justification is required' })
      continue
    }
    if (evidenceState(it).missing) {
      blockers.push({ code: it.controlCode, reason: 'Evidence file is required' })
      continue
    }
    if (it.returned && returnedContentUnchanged(it, returnBaselines?.[it.id])) {
      blockers.push({
        code: it.controlCode,
        reason: 'Update this returned control (answer, justification, or evidence)',
      })
    }
  }
  return blockers
}

export function itemIncomplete(it: TprmAssessmentItem, returnBaseline?: string) {
  return (
    !it.vendorAnswer
    || !(it.justification ?? '').trim()
    || evidenceState(it).missing
    || (Boolean(it.returned) && returnedContentUnchanged(it, returnBaseline))
  )
}

/** Local + server view of whether a returned control still matches the hand-off snapshot. */
export function returnedContentUnchanged(it: TprmAssessmentItem, returnBaseline?: string | null): boolean {
  if (!it.returned) return false
  if (returnBaseline != null && returnBaseline !== '') {
    return itemContentKey(it) === returnBaseline
  }
  return it.unchangedSinceReturn === true
}

export function itemContentKey(it: TprmAssessmentItem): string {
  const answer = it.vendorAnswer ?? ''
  const justification = (it.justification ?? '').trim()
  const ref = (it.evidenceRef ?? '').trim()
  const ids = itemEvidenceFiles(it)
    .map((f) => f.id)
    .sort((a, b) => a - b)
    .join(',')
  return `${answer}\u001f${justification}\u001f${ref}\u001f${ids}`
}

export function formatPortalError(err: unknown): string {
  if (err instanceof ApiError) return err.message
  return String(err).replace(/^ApiError:\s*/i, '').replace(/^Error:\s*/i, '')
}

export function controlCodeFromMessage(msg: string): string | null {
  const m = msg.match(/\b([DCA]\d+-\d+)\b/i)
  return m ? m[1].toUpperCase() : null
}
