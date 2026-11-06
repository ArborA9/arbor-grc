/** SAMA CSF Gap evidence gates (workbook M–Q): shared labels + maturity derivation. */

import { brand, semantic } from '../../theme'

export const SAMA_GATE_COUNT = 5

export const SAMA_GATE_LABELS: string[] = [
  'Level 1: Is there ANYTHING at all? (documented awareness, activity, or named owner)',
  'Level 2: Is it done consistently across departments/owners (even if informal)?',
  'Level 3: Approved policy + standard + procedure, implemented, compliance monitored, KPIs reported?',
  'Level 4: KRIs with thresholds, effectiveness measured, trend reporting?',
  'Level 5: Continuous improvement, ERM integrated, real-time monitoring, peer benchmarking?',
]

export type GateAnswer = boolean | null

export function emptyGates(): GateAnswer[] {
  return [null, null, null, null, null]
}

export function normalizeGates(raw: (boolean | null)[] | null | undefined): GateAnswer[] {
  const out = emptyGates()
  if (!raw) return out
  for (let i = 0; i < SAMA_GATE_COUNT && i < raw.length; i++) {
    out[i] = raw[i] ?? null
  }
  return out
}

/** Consecutive Yes from gate 1; first No/blank stops. Gate1 No → 0; gate1 blank → null. */
export function maturityFromGates(gates: GateAnswer[]): number | null {
  if (gates[0] == null) return null
  if (gates[0] === false) return 0
  for (let i = 1; i < SAMA_GATE_COUNT; i++) {
    if (gates[i] !== true) return i
  }
  return 5
}

export function gatesComplete(gates: GateAnswer[]): boolean {
  return gates.length === SAMA_GATE_COUNT && gates.every((g) => g !== null)
}

export const SAMA_MATURITY_LABELS: Record<number, string> = {
  0: '0: Non-existent',
  1: '1: Initial',
  2: '2: Repeatable',
  3: '3: Defined (target)',
  4: '4: Managed',
  5: '5: Optimized',
}

/** Chip colors. Below the target (3) warms toward red. The target is gold. Above it is green. */
export function samaMaturityTone(level: number | null): { bg: string; text: string } {
  switch (level) {
    case 0:
      return semantic.gap.nonCompliant
    case 1:
      return semantic.riskChip.high
    case 2:
      return semantic.gap.partial
    case 3:
      return { bg: brand.goldLight, text: brand.goldDark }
    case 4:
      return semantic.gap.compliant
    case 5:
      return { bg: semantic.risk.low, text: '#FFFFFF' }
    default:
      return semantic.gap.notAssessed
  }
}
