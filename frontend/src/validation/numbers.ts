import type { KriDirection } from '../api'

/** Positive integer (e.g. questionnaire expiry days). */
export function positiveIntError(
  value: string | number | null | undefined,
  min = 1,
  required = false,
): string {
  const raw = value == null ? '' : String(value).trim()
  if (!raw) return required ? 'Value is required' : ''
  const n = Number(raw)
  if (!Number.isInteger(n) || n < min) {
    return min === 1 ? 'Enter a whole number of at least 1' : `Enter a whole number of at least ${min}`
  }
  return ''
}

/** KRI threshold ordering: mirrors backend KriScoring.validateThresholds. */
export function kriThresholdsError(
  direction: KriDirection,
  green: number,
  amber: number,
  red: number,
): string {
  if ([green, amber, red].some((n) => Number.isNaN(n))) return 'Enter valid threshold numbers'
  if (direction === 'HIGHER_IS_WORSE') {
    if (!(green < amber && amber < red)) {
      return 'For higher-is-worse: Green < Amber < Red'
    }
  } else if (!(red < amber && amber < green)) {
    return 'For lower-is-worse: Red < Amber < Green'
  }
  return ''
}
