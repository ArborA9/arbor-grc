import type { GapItem } from '../../api'
import type { GapStatus } from '../ui/DomainBadge'

const ANNEX_THEME_NAMES: Record<string, string> = {
  'A.5': 'Annex A: Organisational Controls',
  'A.6': 'Annex A: People Controls',
  'A.7': 'Annex A: Physical Controls',
  'A.8': 'Annex A: Technological Controls',
}

/** ISO 27001 mandatory clause section titles (Doc 02 G-12 / G-13). */
const ISO_CLAUSE_SECTION_TITLES: Record<number, string> = {
  4: 'Context of the Organisation',
  5: 'Leadership',
  6: 'Planning',
  7: 'Support',
  8: 'Operation',
  9: 'Performance Evaluation',
  10: 'Improvement',
}

/**
 * UAE IA V2.1 Domain Objectives (GRC_UAE_IA_V2.1.xlsx): level-1 domains M1–M6 / T1–T9.
 * Gap leaves are sub-controls (e.g. M1.1.1.1); section = parent domain.
 */
const UAE_IA_DOMAIN_TITLES: Record<string, string> = {
  M1: 'Strategy and Planning',
  M2: 'IS Risk Management',
  M3: 'Awareness and Training',
  M4: 'Human Resource Security',
  M5: 'Compliance',
  M6: 'Performance Evaluation and Improvement',
  T1: 'Asset Management',
  T2: 'Physical and Environmental Security',
  T3: 'Operations Management',
  T4: 'Network Security',
  T5: 'Identity and Access Management',
  T6: 'Third Party Security',
  T7: 'IS Acquisition, Development and Maintenance',
  T8: 'IS Incident Management',
  T9: 'IS Continuity Management',
}

export type GapScoringProfile = 'ISO_PACK' | 'UAE_IA' | 'SAMA' | 'NCEMA' | 'OTHER'

/** Doc 02 G-12: primary Gap list tabs. */
export type GapSectionTab = 'CLAUSE' | 'ANNEX' | 'CLOUD' | 'ALL'

export interface GapDomainGroup {
  key: string
  label: string
  items: GapItem[]
}

export interface GapProgressStats {
  compliant: number
  partial: number
  planned: number
  nonCompliant: number
  notAssessed: number
  na: number
  completionPct: number
  assessedCount: number
  inScopeCount: number
}

export function resolveGapScoringProfile(
  frameworkCode: string | null | undefined,
  frameworkName?: string | null,
): GapScoringProfile {
  const c = (frameworkCode ?? '').toUpperCase().replace(/[\s-]+/g, '_')
  const n = (frameworkName ?? '').toUpperCase()
  if (c.includes('SAMA') || n.includes('SAMA')) return 'SAMA'
  if (c.includes('UAE_IA') || n.includes('UAE IA') || (c.includes('TDRA') && n.includes('IA'))) {
    return 'UAE_IA'
  }
  if (c.includes('NCEMA') || n.includes('NCEMA')) return 'NCEMA'
  if (
    c.includes('ISO')
    || c.includes('CIS')
    || c.includes('NIST')
    || c.includes('PCI')
    || c.includes('PDPL')
    || c.includes('GDPR')
  ) {
    return 'ISO_PACK'
  }
  return 'OTHER'
}

export function isSamaFramework(code: string | null | undefined, name?: string | null): boolean {
  return resolveGapScoringProfile(code, name) === 'SAMA'
}

export function gapStatusFromItem(item: GapItem): GapStatus {
  if (item.applicability === 'NOT_APPLICABLE') return 'na'
  // SAMA maturity path (implementation status unused).
  if (item.maturity != null || item.finalMaturity != null) {
    const m = item.finalMaturity ?? item.maturity
    if (m == null) return 'notAssessed'
    if (m >= 3) return 'compliant'
    if (m >= 1) return 'partial'
    return 'nonCompliant'
  }
  const effective = item.finalImplementationStatus ?? item.implementationStatus
  if (!effective) return 'notAssessed'
  switch (effective) {
    case 'FULLY_IMPLEMENTED':
      return 'compliant'
    case 'PARTIALLY_IMPLEMENTED':
      return 'partial'
    case 'PLANNED':
      return 'planned'
    case 'NOT_IMPLEMENTED':
      return 'nonCompliant'
    default:
      return 'notAssessed'
  }
}

/** Kit-style gap badge label with symbol prefix. */
export function gapStatusDisplayLabel(status: GapStatus): string {
  switch (status) {
    case 'compliant':
      return '✓ Compliant'
    case 'partial':
      return '◑ Partial'
    case 'planned':
      return '◷ Planned'
    case 'nonCompliant':
      return '✗ Non-compliant'
    case 'notAssessed':
      return '- Not assessed'
    case 'na':
      return 'N/A'
    default:
      return status
  }
}

/**
 * Cloud / extended controls (ISO/IEC 27017 sheet and similar packs).
 * Codes are typically CLD.*: not A.* annex or clause 4–10.
 */
export function isCloudControlCode(nodeCode: string | null | undefined): boolean {
  if (!nodeCode) return false
  const code = nodeCode.trim().toUpperCase()
  return (
    code === 'CLD'
    || code.startsWith('CLD.')
    || code === 'CLOUD'
    || code.startsWith('CLOUD.')
  )
}

/**
 * SAMA CSF leaves are subdomain-ctrl (e.g. 3.1.1-1). Domain = first two dotted segments (3.1).
 */
function deriveSamaDomain(nodeCode: string): { key: string; label: string } {
  const raw = nodeCode.trim()
  const beforeHyphen = raw.split('-')[0] ?? raw
  const parts = beforeHyphen.split('.').filter(Boolean)
  if (parts.length >= 2) {
    const domain = `${parts[0]}.${parts[1]}`
    return { key: `SAMA_${domain}`, label: `Domain ${domain}` }
  }
  if (parts.length === 1) {
    return { key: `SAMA_${parts[0]}`, label: `Domain ${parts[0]}` }
  }
  return { key: 'SAMA_OTHER', label: 'Other SAMA controls' }
}

/**
 * UAE IA: M1.1.1.1 → domain M1 (Excel Domain Objectives / catalog level-1).
 */
function deriveUaeIaDomain(nodeCode: string): { key: string; label: string } {
  const code = nodeCode.trim().toUpperCase()
  const match = code.match(/^([MT]\d+)/)
  if (match) {
    const domain = match[1]
    const title = UAE_IA_DOMAIN_TITLES[domain]
    return {
      key: `UAE_${domain}`,
      label: title ? `${domain}: ${title}` : domain,
    }
  }
  if (code.startsWith('M')) {
    return { key: 'UAE_M', label: 'Management controls' }
  }
  if (code.startsWith('T')) {
    return { key: 'UAE_T', label: 'Technical controls' }
  }
  return { key: 'UAE_OTHER', label: 'Other UAE IA controls' }
}

export function deriveGapDomain(
  nodeCode: string,
  profile: GapScoringProfile = 'ISO_PACK',
): { key: string; label: string } {
  const code = nodeCode.trim().toUpperCase()
  if (profile === 'SAMA') {
    return deriveSamaDomain(nodeCode)
  }
  if (profile === 'UAE_IA') {
    return deriveUaeIaDomain(nodeCode)
  }
  if (isCloudControlCode(code)) {
    return { key: 'CLOUD', label: 'Cloud (ISO 27017)' }
  }
  if (code.startsWith('A.')) {
    const theme = code.split('.').slice(0, 2).join('.')
    return { key: theme, label: ANNEX_THEME_NAMES[theme] ?? `Annex A: ${theme}` }
  }
  const major = parseInt(code.split('.')[0] ?? '', 10)
  if (!Number.isNaN(major) && major >= 4 && major <= 10) {
    const title = ISO_CLAUSE_SECTION_TITLES[major]
    return {
      key: `CLAUSE_${major}`,
      label: title ? `Clause ${major}: ${title}` : `Clause ${major}`,
    }
  }
  if (!Number.isNaN(major)) {
    return { key: `CLAUSE_${major}`, label: `Clause ${major}` }
  }
  return { key: 'OTHER', label: 'Other controls' }
}

/** Which primary Gap tab a domain group belongs to (ISO-style packs). */
export function gapDomainSectionTab(groupKey: string): GapSectionTab {
  if (groupKey === 'CLOUD') return 'CLOUD'
  if (groupKey.startsWith('A.')) return 'ANNEX'
  if (groupKey === 'CLAUSES' || groupKey.startsWith('CLAUSE_')) return 'CLAUSE'
  return 'ALL'
}

export function filterGroupsBySectionTab(
  groups: GapDomainGroup[],
  tab: GapSectionTab,
  profile: GapScoringProfile,
): GapDomainGroup[] {
  if (tab === 'ALL' || profile !== 'ISO_PACK') return groups
  return groups.filter((g) => gapDomainSectionTab(g.key) === tab)
}

export function gapSectionTabCounts(
  items: GapItem[],
  profile: GapScoringProfile,
): { clause: number; annex: number; cloud: number; all: number } {
  if (profile !== 'ISO_PACK') {
    return { clause: 0, annex: 0, cloud: 0, all: items.length }
  }
  let clause = 0
  let annex = 0
  let cloud = 0
  for (const item of items) {
    const { key } = deriveGapDomain(item.nodeCode, profile)
    const tab = gapDomainSectionTab(key)
    if (tab === 'CLAUSE') clause++
    else if (tab === 'ANNEX') annex++
    else if (tab === 'CLOUD') cloud++
  }
  return { clause, annex, cloud, all: items.length }
}

function domainSortKey(key: string, profile: GapScoringProfile): string {
  if (profile === 'SAMA' && key.startsWith('SAMA_')) return `0_${key}`
  if (profile === 'UAE_IA' && key.startsWith('UAE_')) {
    // M1…M6 then T1…T9
    const m = key.match(/^UAE_([MT])(\d+)$/)
    if (m) {
      const family = m[1] === 'M' ? '0' : '1'
      return `0_${family}_${m[2].padStart(2, '0')}`
    }
    return `1_${key}`
  }
  if (key === 'CLAUSES') return '0'
  if (key.startsWith('CLAUSE_')) {
    const n = key.replace('CLAUSE_', '').padStart(2, '0')
    return `0_${n}`
  }
  if (key.startsWith('A.')) return `2_${key}`
  if (key === 'CLOUD') return '3_CLOUD'
  return `9_${key}`
}

export interface UaeIaL3Group {
  key: string
  label: string
  pct: number | null
  items: GapItem[]
}

export interface UaeIaL2Group {
  key: string
  label: string
  groups: UaeIaL3Group[]
}

export interface UaeIaL1Group {
  key: string
  label: string
  groups: UaeIaL2Group[]
}

/** Parse M1 / M1.1 / M1.1.1 / M1.1.1.1 (and T*) into L1–L3 keys. */
export function parseUaeIaCode(
  nodeCode: string,
): { l1: string; l2: string | null; l3: string | null } | null {
  const m = nodeCode.trim().toUpperCase().match(/^([MT]\d+)(?:\.(\d+))?(?:\.(\d+))?/)
  if (!m) return null
  const l1 = m[1]
  const l2 = m[2] != null ? `${l1}.${m[2]}` : null
  const l3 = m[2] != null && m[3] != null ? `${l1}.${m[2]}.${m[3]}` : null
  return { l1, l2, l3 }
}

function averageItemPct(items: GapItem[]): number | null {
  const scores = items
    .filter((i) => i.applicability === 'APPLICABLE' && i.score != null)
    .map((i) => i.score as number)
  if (scores.length === 0) return null
  const avg = scores.reduce((a, b) => a + b, 0) / scores.length
  return Math.round(avg * 10) / 10
}

function uaeIaGroupLabel(code: string, items: GapItem[]): string {
  const exact = items.find((i) => i.nodeCode.trim().toUpperCase() === code)
  if (exact?.title?.trim()) return `${code} · ${exact.title.trim()}`
  return code
}

/** Nested L1 → L2 → L3 groups for UAE IA Gap lists (D3-11). */
export function groupUaeIaNested(items: GapItem[]): UaeIaL1Group[] {
  type L3Acc = { items: GapItem[] }
  type L2Acc = { l3: Map<string, L3Acc> }
  const l1Map = new Map<string, { l2: Map<string, L2Acc> }>()

  for (const item of items) {
    const parts = parseUaeIaCode(item.nodeCode)
    const { key: fallbackKey } = deriveUaeIaDomain(item.nodeCode)
    const l1 = parts?.l1 ?? fallbackKey.replace(/^UAE_/, '')
    const l2 = parts?.l2 ?? l1
    const l3 = parts?.l3 ?? l2
    const l1Entry = l1Map.get(l1) ?? { l2: new Map() }
    l1Map.set(l1, l1Entry)
    const l2Entry = l1Entry.l2.get(l2) ?? { l3: new Map() }
    l1Entry.l2.set(l2, l2Entry)
    const l3Entry = l2Entry.l3.get(l3) ?? { items: [] }
    l3Entry.items.push(item)
    l2Entry.l3.set(l3, l3Entry)
  }

  const l1Order = (code: string): string => {
    const m = code.toUpperCase().match(/^([MT])(\d+)$/)
    if (!m) return `9_${code}`
    return `${m[1] === 'M' ? '0' : '1'}_${m[2].padStart(2, '0')}`
  }

  return [...l1Map.entries()]
    .sort((a, b) => l1Order(a[0]).localeCompare(l1Order(b[0])))
    .map(([l1, l1Entry]) => {
      const title = UAE_IA_DOMAIN_TITLES[l1]
      return {
        key: `UAE_${l1}`,
        label: title ? `${l1}: ${title}` : l1,
        groups: [...l1Entry.l2.entries()]
          .sort((a, b) => a[0].localeCompare(b[0], undefined, { numeric: true }))
          .map(([l2, l2Entry]) => {
            const l2Items = [...l2Entry.l3.values()].flatMap((g) => g.items)
            return {
              key: l2,
              label: uaeIaGroupLabel(l2, l2Items),
              groups: [...l2Entry.l3.entries()]
                .sort((a, b) => a[0].localeCompare(b[0], undefined, { numeric: true }))
                .map(([l3, l3Entry]) => ({
                  key: l3,
                  label: uaeIaGroupLabel(l3, l3Entry.items),
                  pct: averageItemPct(l3Entry.items),
                  items: l3Entry.items,
                })),
            }
          }),
      }
    })
}

/** Catalog clause title when the framework has one. ISO 4-10 titles stay as the fallback. */
function clauseHeadingLabel(item: GapItem, fallback: string, profile: GapScoringProfile): string {
  if (profile === 'SAMA' || profile === 'UAE_IA') return fallback
  const title = (item.domainLabel ?? '').trim()
  if (!title) return fallback
  if (/^clause\b/i.test(title)) return title
  const code = (item.domainCode ?? '').trim()
  if (/^\d+$/.test(code)) return `Clause ${code}: ${title}`
  if (code) return `${code}: ${title}`
  return title
}

export function groupItemsByDomain(
  items: GapItem[],
  profile: GapScoringProfile = 'ISO_PACK',
): GapDomainGroup[] {
  const map = new Map<string, GapDomainGroup>()
  for (const item of items) {
    const derived = deriveGapDomain(item.nodeCode, profile)
    const label = clauseHeadingLabel(item, derived.label, profile)
    const group = map.get(derived.key) ?? { key: derived.key, label, items: [] }
    if (label !== derived.label) group.label = label
    group.items.push(item)
    map.set(derived.key, group)
  }
  return [...map.values()].sort((a, b) =>
    domainSortKey(a.key, profile).localeCompare(domainSortKey(b.key, profile)),
  )
}

export function computeGapProgressStats(
  items: GapItem[],
  overallScore: number | null | undefined,
): GapProgressStats {
  let compliant = 0
  let partial = 0
  let planned = 0
  let nonCompliant = 0
  let notAssessed = 0
  let na = 0

  for (const item of items) {
    switch (gapStatusFromItem(item)) {
      case 'compliant':
        compliant++
        break
      case 'partial':
        partial++
        break
      case 'planned':
        planned++
        break
      case 'nonCompliant':
        nonCompliant++
        break
      case 'notAssessed':
        notAssessed++
        break
      case 'na':
        na++
        break
    }
  }

  const inScope = items.length - na
  const assessed = inScope - notAssessed
  const completionPct =
    overallScore != null
      ? overallScore
      : inScope > 0
        ? Math.round((assessed / inScope) * 100)
        : 0

  return {
    compliant,
    partial,
    planned,
    nonCompliant,
    notAssessed,
    na,
    completionPct,
    assessedCount: assessed,
    inScopeCount: inScope,
  }
}

/** Assign and review lists show Q1, Q2, Q3. Stored codes such as 3.1-Q1 stay unchanged. */
export function gapQuestionDisplayCode(questionCode: string | null | undefined, index: number): string {
  const code = (questionCode ?? '').trim()
  const match = code.match(/(?:^|-)(Q\d+)$/i)
  if (match) return match[1].toUpperCase()
  return `Q${index + 1}`
}

export function formatGapScorePct(value: number | null | undefined): string {
  if (value == null || Number.isNaN(value)) return '-'
  const rounded = Math.round(value * 100) / 100
  return Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(2)
}

/**
 * Same bands as {@code GapScoring.ragFor}: ≤25 Critical, ≤50 Low, ≤75 Moderate, ≤80 Good, else Strong.
 * Used when the API has a score but no RAG yet (e.g. nothing assessed, average is 0).
 */
export function gapScoreRagBand(score: number | null | undefined, rag?: string | null): string | null {
  if (rag) return rag
  if (score == null || Number.isNaN(score)) return null
  if (score <= 25) return 'CRITICAL'
  if (score <= 50) return 'LOW'
  if (score <= 75) return 'MODERATE'
  if (score <= 80) return 'GOOD'
  return 'STRONG'
}

export function domainGroupStats(items: GapItem[]) {
  let compliant = 0
  let partial = 0
  let planned = 0
  let nonCompliant = 0
  let notAssessed = 0
  let na = 0
  for (const item of items) {
    switch (gapStatusFromItem(item)) {
      case 'compliant':
        compliant++
        break
      case 'partial':
        partial++
        break
      case 'planned':
        planned++
        break
      case 'nonCompliant':
        nonCompliant++
        break
      case 'na':
        na++
        break
      default:
        notAssessed++
    }
  }
  return {
    assessed: items.length - notAssessed - na,
    compliant,
    partial,
    planned,
    nonCompliant,
    notAssessed,
    na,
    total: items.length,
  }
}
