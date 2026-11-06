import type { SoaApplicability, SoaItem, SoaSummary } from '../../api'

export type SoaApplicabilityFilter = 'ALL' | 'UNANSWERED' | 'DECIDED' | 'APPLICABLE' | 'NOT_APPLICABLE'

/** Doc 02 G-04: Clause (domain) vs Annex (control) vs Cloud sheet. */
export type SoaSectionTab = 'CLAUSE' | 'ANNEX' | 'CLOUD'

export function soaApplicabilityBadge(
  applicability: SoaApplicability | null,
): { variant: 'soa'; status: 'yes' | 'no' | 'na'; label: string } | { variant: 'gap'; status: 'notAssessed'; label: string } {
  if (!applicability) {
    return { variant: 'gap', status: 'notAssessed', label: '- Not decided' }
  }
  if (applicability === 'APPLICABLE') {
    return { variant: 'soa', status: 'yes', label: '✓ Yes' }
  }
  return { variant: 'soa', status: 'no', label: '✗ No' }
}

export function soaCompletionPct(soa: SoaSummary): number {
  if (soa.totalControls <= 0) return 0
  const decided = soa.totalControls - soa.unansweredCount
  return Math.round((decided / soa.totalControls) * 100)
}

export function classifySoaSection(item: SoaItem): SoaSectionTab {
  const rt = (item.recordType ?? '').toUpperCase()
  if (rt === 'CLOUD' || rt.includes('CLOUD')) return 'CLOUD'
  if (rt === 'ANNEX_CONTROL' || rt.includes('ANNEX')) return 'ANNEX'
  if (rt === 'CLAUSE') return 'CLAUSE'
  const code = (item.nodeCode ?? '').trim().toUpperCase()
  if (code === 'CLD' || code.startsWith('CLD.') || code === 'CLOUD' || code.startsWith('CLOUD.')) {
    return 'CLOUD'
  }
  if (code.startsWith('A.')) return 'ANNEX'
  return 'CLAUSE'
}

export function filterSoaItemsBySection(items: SoaItem[], tab: SoaSectionTab): SoaItem[] {
  return items.filter((item) => classifySoaSection(item) === tab)
}

export function soaSectionCounts(items: SoaItem[]) {
  let clause = 0
  let annex = 0
  let cloud = 0
  for (const item of items) {
    const s = classifySoaSection(item)
    if (s === 'CLAUSE') clause++
    else if (s === 'ANNEX') annex++
    else cloud++
  }
  return { clause, annex, cloud }
}

export function filterSoaItems(items: SoaItem[], filter: SoaApplicabilityFilter): SoaItem[] {
  return items.filter((item) => {
    if (filter === 'UNANSWERED') return !item.applicability
    if (filter === 'DECIDED') return !!item.applicability
    if (filter === 'APPLICABLE') return item.applicability === 'APPLICABLE'
    if (filter === 'NOT_APPLICABLE') return item.applicability === 'NOT_APPLICABLE'
    return true
  })
}

export function soaFilterCounts(items: SoaItem[]) {
  return {
    all: items.length,
    unanswered: items.filter((i) => !i.applicability).length,
    applicable: items.filter((i) => i.applicability === 'APPLICABLE').length,
    notApplicable: items.filter((i) => i.applicability === 'NOT_APPLICABLE').length,
  }
}
