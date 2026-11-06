import { semantic } from '../../theme'

/** RAG / risk scale for chips, dots, and select options. */
export type RatingTone = 'critical' | 'high' | 'medium' | 'low' | 'veryLow'

export const RATING_TONE_COLOR: Record<RatingTone, string> = {
  critical: semantic.risk.critical,
  high: semantic.risk.high,
  medium: semantic.risk.medium,
  low: semantic.risk.low,
  veryLow: semantic.risk.veryLow,
}

export function apiLevelTone(level: string | null | undefined): RatingTone | undefined {
  if (!level) return undefined
  switch (level) {
    case 'VERY_HIGH':
    case 'CRITICAL':
    case 'P1':
    case 'MAJOR_NC':
    case 'RED':
      return 'critical'
    case 'HIGH':
    case 'P2':
    case 'MINOR_NC':
    case 'AMBER':
      return 'high'
    case 'MEDIUM':
    case 'P3':
    case 'OBSERVATION':
      return 'medium'
    case 'LOW':
    case 'P4':
    case 'OFI':
    case 'GREEN':
      return 'low'
    case 'VERY_LOW':
      return 'veryLow'
    default:
      return undefined
  }
}

/** CIA / 1–5 impact bands: 1–2 green, 3 amber, 4 high, 5 red. */
export function scale1to5Tone(n: number): RatingTone {
  if (n >= 5) return 'critical'
  if (n >= 4) return 'high'
  if (n >= 3) return 'medium'
  if (n >= 2) return 'low'
  return 'veryLow'
}

/** Control effectiveness is inverted: weak controls are red, strong are green. */
export function controlEffectivenessTone(level: string | null | undefined): RatingTone | undefined {
  switch (level) {
    case 'VERY_LOW':
      return 'critical'
    case 'LOW':
      return 'high'
    case 'MEDIUM':
      return 'medium'
    case 'HIGH':
      return 'low'
    case 'VERY_HIGH':
      return 'veryLow'
    default:
      return apiLevelTone(level)
  }
}
