import type { RiskLevel } from '../../api'

/**
 * FCA approved 5×5 Likelihood × Impact lookup (not product banding).
 * Rows: Almost Certain → Extremely Unlikely (VERY_HIGH → VERY_LOW).
 * Cols: Very Low → Very High.
 * Display: Low / Moderate / High / Significant / Extreme via ermRiskLabels.
 */
const ROW_INDEX: Record<RiskLevel, number> = {
  VERY_HIGH: 0,
  HIGH: 1,
  MEDIUM: 2,
  LOW: 3,
  VERY_LOW: 4,
}

const COL_INDEX: Record<RiskLevel, number> = {
  VERY_LOW: 0,
  LOW: 1,
  MEDIUM: 2,
  HIGH: 3,
  VERY_HIGH: 4,
}

const LOOKUP: RiskLevel[][] = [
  ['MEDIUM', 'MEDIUM', 'HIGH', 'VERY_HIGH', 'VERY_HIGH'],
  ['LOW', 'MEDIUM', 'MEDIUM', 'HIGH', 'VERY_HIGH'],
  ['LOW', 'LOW', 'MEDIUM', 'MEDIUM', 'HIGH'],
  ['VERY_LOW', 'LOW', 'LOW', 'MEDIUM', 'MEDIUM'],
  ['VERY_LOW', 'VERY_LOW', 'LOW', 'LOW', 'MEDIUM'],
]

/** Brief default appetite: Moderate (internal LOW). */
export const FCA_DEFAULT_APPETITE: RiskLevel = 'LOW'

export function fcaLookup(probability: RiskLevel, impact: RiskLevel): RiskLevel {
  return LOOKUP[ROW_INDEX[probability]][COL_INDEX[impact]]
}

export function fcaRatingKey(probability: RiskLevel, impact: RiskLevel): string {
  return fcaLookup(probability, impact)
}

export function fcaAction(probability: RiskLevel, impact: RiskLevel): string {
  switch (fcaLookup(probability, impact)) {
    case 'VERY_HIGH':
      return 'Immediate board escalation: cease activity if possible'
    case 'HIGH':
      return 'Urgent executive action: treatment mandatory'
    case 'MEDIUM':
      return 'Treatment required'
    case 'LOW':
      return 'Within appetite: monitor with review date'
    case 'VERY_LOW':
      return 'Auto-close: residual Low'
  }
}

export function fcaWithinAppetite(residualRating: RiskLevel, appetite: RiskLevel = FCA_DEFAULT_APPETITE): boolean {
  const order: RiskLevel[] = ['VERY_LOW', 'LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH']
  return order.indexOf(residualRating) <= order.indexOf(appetite)
}

export function fcaAutoClosable(residualRating: RiskLevel): boolean {
  return residualRating === 'VERY_LOW'
}

/** Design × Performance → 1–5 control score. */
export type FcaControlAxis = 'SATISFACTORY' | 'NEEDS_IMPROVEMENT' | 'UNSATISFACTORY'

export const FCA_CONTROL_AXIS_LABEL: Record<FcaControlAxis, string> = {
  SATISFACTORY: 'Satisfactory',
  NEEDS_IMPROVEMENT: 'Needs Improvement',
  UNSATISFACTORY: 'Unsatisfactory',
}

const CE_SCORE: number[][] = [
  [5, 4, 2],
  [3, 3, 2],
  [1, 1, 1],
]

const AXIS_INDEX: Record<FcaControlAxis, number> = {
  SATISFACTORY: 0,
  NEEDS_IMPROVEMENT: 1,
  UNSATISFACTORY: 2,
}

export function fcaControlScore(design: FcaControlAxis, performance: FcaControlAxis): number {
  return CE_SCORE[AXIS_INDEX[design]][AXIS_INDEX[performance]]
}

export function fcaControlWord(score: number): string {
  switch (score) {
    case 5:
      return 'Very strong'
    case 4:
      return 'Strong'
    case 3:
      return 'Moderate'
    case 2:
      return 'Weak'
    case 1:
      return 'Ineffective'
    default:
      return '-'
  }
}

export function fcaControlGapRequired(design: FcaControlAxis, performance: FcaControlAxis): boolean {
  return design !== 'SATISFACTORY' || performance !== 'SATISFACTORY'
}

/** Rounded average; cap at 3 if any performance is Unsatisfactory. */
export function fcaConsolidateControls(
  scores: number[],
  anyUnsatisfactoryPerformance: boolean,
): number | null {
  if (!scores.length) return null
  const avg = scores.reduce((a, b) => a + b, 0) / scores.length
  const rounded = Math.round(avg)
  return anyUnsatisfactoryPerformance ? Math.min(rounded, 3) : rounded
}
