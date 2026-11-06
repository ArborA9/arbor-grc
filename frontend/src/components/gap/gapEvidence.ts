import { api, type EvidenceSummary } from '../../api'

/** Link identity: GAP / GAP_QUESTION / `{itemId}:{questionKey}` e.g. `501:c:12`. */
export function gapQuestionRecordId(gapItemId: number, questionKey: string): string {
  return `${gapItemId}:${questionKey}`
}

export async function loadGapQuestionEvidence(
  gapItemId: number,
  questionKeys: string[],
): Promise<Record<string, EvidenceSummary[]>> {
  const pairs = await Promise.all(
    questionKeys.map(async (key) => {
      try {
        const list = await api.listEvidenceLinked('GAP', 'GAP_QUESTION', gapQuestionRecordId(gapItemId, key))
        return [key, list] as const
      } catch {
        return [key, []] as const
      }
    }),
  )
  return Object.fromEntries(pairs)
}

export function evidencePreviewKind(
  filename: string | null | undefined,
  contentType?: string | null,
): 'image' | 'pdf' | 'other' {
  const type = (contentType ?? '').toLowerCase()
  const name = (filename ?? '').toLowerCase()
  if (type.startsWith('image/') || /\.(png|jpe?g|gif|webp)$/.test(name)) return 'image'
  if (type.includes('pdf') || name.endsWith('.pdf')) return 'pdf'
  return 'other'
}
