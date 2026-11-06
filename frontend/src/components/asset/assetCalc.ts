/** Client-side Asset Register calcs mirroring AssetCia (Excel Valuation rules). */
import type { AssetClassification, AssetTriState } from '../../api'

export function assetValue(c: number, i: number, a: number) {
  return Math.max(c, i, a)
}

export function aggregateScore(c: number, i: number, a: number) {
  return c + i + a
}

export function weightedScore(c: number, i: number, a: number) {
  return Math.round(((c + i + a) / 3) * 100) / 100
}

export function criticalityFromValue(v: number): { level: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'; label: string } {
  if (v >= 5) return { level: 'CRITICAL', label: 'Tier 1: Critical' }
  if (v === 4) return { level: 'HIGH', label: 'Tier 2: High' }
  if (v === 3) return { level: 'MEDIUM', label: 'Tier 3: Medium' }
  if (v === 2) return { level: 'LOW', label: 'Tier 4: Low' }
  return { level: 'LOW', label: 'Tier 5: Minimal' }
}

export function classificationFromC(c: number): AssetClassification {
  if (c >= 5) return 'SECRET'
  if (c === 4) return 'SENSITIVE'
  if (c >= 2) return 'CONFIDENTIAL'
  return 'OPEN'
}

export function classRank(c: AssetClassification): number {
  switch (c) {
    case 'SECRET':
      return 4
    case 'SENSITIVE':
      return 3
    case 'CONFIDENTIAL':
      return 2
    default:
      return 1
  }
}

export function recommendedRto(a: number) {
  return a === 5 ? 1 : a === 4 ? 4 : a === 3 ? 24 : a === 2 ? 72 : 168
}

export function recommendedRpo(a: number) {
  return a === 5 ? 0 : a === 4 ? 1 : a === 3 ? 4 : a === 2 ? 24 : 168
}

export function reviewCycleMonths(value: number) {
  return value === 5 ? 3 : value === 4 ? 6 : value === 3 ? 12 : value === 2 ? 24 : 36
}

export function triLabel(v: AssetTriState | '' | null | undefined) {
  if (v === 'YES') return 'Yes'
  if (v === 'NO') return 'No'
  if (v === 'NOT_APPLICABLE') return 'Not applicable'
  return '-'
}

export const TRI_OPTIONS: { value: AssetTriState | ''; label: string }[] = [
  { value: '', label: '-' },
  { value: 'YES', label: 'Yes' },
  { value: 'NO', label: 'No' },
  { value: 'NOT_APPLICABLE', label: 'Not applicable' },
]

export const CIA_SCALE = [
  { value: '1', label: '1: Negligible / Minimal', tone: 'veryLow' as const },
  { value: '2', label: '2: Low', tone: 'low' as const },
  { value: '3', label: '3: Medium', tone: 'medium' as const },
  { value: '4', label: '4: High', tone: 'high' as const },
  { value: '5', label: '5: Critical / Catastrophic', tone: 'critical' as const },
]

export const CLASS_OPTIONS: { value: AssetClassification | ''; label: string }[] = [
  { value: '', label: '- (use derived from C)' },
  { value: 'SECRET', label: 'Secret' },
  { value: 'SENSITIVE', label: 'Sensitive' },
  { value: 'CONFIDENTIAL', label: 'Confidential' },
  { value: 'OPEN', label: 'Open' },
]

export function derivedClassificationLabel(c: AssetClassification): string {
  switch (c) {
    case 'SECRET':
      return 'Secret'
    case 'SENSITIVE':
      return 'Sensitive'
    case 'CONFIDENTIAL':
      return 'Confidential'
    default:
      return 'Open'
  }
}

/** Client definitions for Derived Asset Classification (Confidentiality-driven). */
export const DERIVED_CLASSIFICATION_COPY: Record<AssetClassification, string> = {
  SECRET:
    'Unauthorised disclosure would endanger life, national security or public safety, trigger regulatory sanction, or cause irrecoverable loss of public trust.',
  SENSITIVE:
    'Disclosure would breach law or contract, expose sensitive personal data at scale, or cause significant reputational and financial damage.',
  CONFIDENTIAL:
    'Internal-only information whose disclosure would cause embarrassment, operational disadvantage, or limited personal-data exposure.',
  OPEN: 'Already public, or approved for public release.',
}

/** Value → criticality map from client feedback. Classification column is when C equals Asset Value. */
export const ASSET_VALUE_TIER_MAP = [
  { value: 5, tier: 'Tier 1 - Critical', classification: 'Secret' },
  { value: 4, tier: 'Tier 2 - High', classification: 'Sensitive' },
  { value: 3, tier: 'Tier 3 - Medium', classification: 'Confidential' },
  { value: 2, tier: 'Tier 4 - Low', classification: 'Confidential' },
  { value: 1, tier: 'Tier 5 - Minimal', classification: 'Open' },
] as const
