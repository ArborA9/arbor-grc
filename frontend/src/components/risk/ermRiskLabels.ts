import type { RiskLevel } from '../../api'
import { fmtLevel } from '../../riskLevelLabels'

/** ERM Likelihood axis labels (FCA / Arbor P0). Internal ranks stay VERY_LOW…VERY_HIGH. */
export const ERM_LIKELIHOOD_LABEL: Record<RiskLevel, string> = {
  VERY_LOW: 'Extremely Unlikely',
  LOW: 'Unlikely',
  MEDIUM: 'Possible',
  HIGH: 'Likely',
  VERY_HIGH: 'Almost Certain',
}

/** ERM Impact axis labels - same words as the matrix Impact row. */
export const ERM_IMPACT_LABEL: Record<RiskLevel, string> = {
  VERY_LOW: 'Very Low',
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  VERY_HIGH: 'Very High',
}

/**
 * ERM score / residual rating chips (FCA matrix colour names).
 * Maps product bands: VL→Low, L→Moderate, M→High, H→Significant, VH→Extreme.
 */
export const ERM_RATING_LABEL: Record<RiskLevel, string> = {
  VERY_LOW: 'Low',
  LOW: 'Moderate',
  MEDIUM: 'High',
  HIGH: 'Significant',
  VERY_HIGH: 'Extreme',
}

export function fmtErmLikelihood(level: string | null | undefined): string {
  if (!level) return '-'
  return ERM_LIKELIHOOD_LABEL[level as RiskLevel] ?? fmtLevel(level)
}

export function fmtErmImpact(level: string | null | undefined): string {
  if (!level) return '-'
  return ERM_IMPACT_LABEL[level as RiskLevel] ?? fmtLevel(level)
}

export function fmtErmRating(levelOrBand: string | null | undefined): string {
  if (!levelOrBand) return '-'
  const key = levelOrBand.toUpperCase().replace(/[\s-]+/g, '_')
  return ERM_RATING_LABEL[key as RiskLevel] ?? fmtLevel(levelOrBand)
}
