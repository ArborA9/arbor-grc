import type { RiskLevel } from '../../api'

export type MatrixGridSize = 3 | 4 | 5

export type MatrixAxes = {
  rows: RiskLevel[]
  cols: RiskLevel[]
}

export const ALL_RISK_LEVELS: RiskLevel[] = ['VERY_LOW', 'LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH']

export type AxisPreset = {
  label: string
  rows: RiskLevel[]
  cols: RiskLevel[]
}

/** Quick axis bundles when configuring 3×3 / 4×4 matrices. */
export const AXIS_PRESETS: Record<MatrixGridSize, AxisPreset[]> = {
  3: [
    {
      label: 'Low · Medium · High',
      rows: ['HIGH', 'MEDIUM', 'LOW'],
      cols: ['LOW', 'MEDIUM', 'HIGH'],
    },
    {
      label: 'Medium · High · Very High',
      rows: ['VERY_HIGH', 'HIGH', 'MEDIUM'],
      cols: ['MEDIUM', 'HIGH', 'VERY_HIGH'],
    },
    {
      label: 'Very Low · Low · Medium',
      rows: ['MEDIUM', 'LOW', 'VERY_LOW'],
      cols: ['VERY_LOW', 'LOW', 'MEDIUM'],
    },
  ],
  4: [
    {
      label: 'Very Low · Low · Medium · High (Excel 4×4 ranks)',
      rows: ['HIGH', 'MEDIUM', 'LOW', 'VERY_LOW'],
      cols: ['VERY_LOW', 'LOW', 'MEDIUM', 'HIGH'],
    },
    {
      label: 'Low · Medium · High · Very High',
      rows: ['VERY_HIGH', 'HIGH', 'MEDIUM', 'LOW'],
      cols: ['LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH'],
    },
  ],
  5: [
    {
      label: 'Full scale (Very Low → Very High)',
      rows: ['VERY_HIGH', 'HIGH', 'MEDIUM', 'LOW', 'VERY_LOW'],
      cols: ['VERY_LOW', 'LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH'],
    },
  ],
}

export function parseGridSize(n: number | null | undefined): MatrixGridSize {
  if (n === 3 || n === 4) return n
  return 5
}

export function axisRows(gridSize: MatrixGridSize): RiskLevel[] {
  switch (gridSize) {
    case 3:
      return ['HIGH', 'MEDIUM', 'LOW']
    case 4:
      return ['HIGH', 'MEDIUM', 'LOW', 'VERY_LOW']
    default:
      return ['VERY_HIGH', 'HIGH', 'MEDIUM', 'LOW', 'VERY_LOW']
  }
}

export function axisCols(gridSize: MatrixGridSize): RiskLevel[] {
  switch (gridSize) {
    case 3:
      return ['LOW', 'MEDIUM', 'HIGH']
    case 4:
      return ['VERY_LOW', 'LOW', 'MEDIUM', 'HIGH']
    default:
      return ['VERY_LOW', 'LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH']
  }
}

export function defaultAxes(gridSize: MatrixGridSize): MatrixAxes {
  return { rows: axisRows(gridSize), cols: axisCols(gridSize) }
}

export function axesFromConfig(
  gridSize: MatrixGridSize,
  rowLevels?: RiskLevel[] | null,
  colLevels?: RiskLevel[] | null,
): MatrixAxes {
  if (
    rowLevels &&
    colLevels &&
    rowLevels.length === gridSize &&
    colLevels.length === gridSize
  ) {
    return { rows: rowLevels, cols: colLevels }
  }
  return defaultAxes(gridSize)
}

/** Residual bands shown on the register Priority filter and KPI tiles. */
export function priorityBandsForGrid(gridSize: MatrixGridSize): RiskLevel[] {
  switch (gridSize) {
    case 3:
      return ['HIGH', 'MEDIUM', 'LOW']
    case 4:
      return ['HIGH', 'MEDIUM', 'LOW', 'VERY_LOW']
    default:
      return ['VERY_HIGH', 'HIGH', 'MEDIUM', 'LOW', 'VERY_LOW']
  }
}

/** Map a stored residual band onto the names this grid shows. */
export function foldBandToGrid(level: RiskLevel, gridSize: MatrixGridSize): RiskLevel {
  if (gridSize === 3) {
    if (level === 'VERY_HIGH') return 'HIGH'
    if (level === 'VERY_LOW') return 'LOW'
  }
  if (gridSize === 4 && level === 'VERY_HIGH') return 'HIGH'
  return level
}

export function residualBandForGrid(
  residualRisk: RiskLevel,
  residualRating: string | null | undefined,
  gridSize: MatrixGridSize,
): RiskLevel {
  const raw = residualRating ? excelRatingToLevel(residualRating) : residualRisk
  return foldBandToGrid(raw, gridSize)
}

/** Levels valid for probability / impact pickers. */
export function activeScoreLevels(axes: MatrixAxes): RiskLevel[] {
  const seen = new Set<RiskLevel>()
  for (const l of axes.rows) seen.add(l)
  for (const l of axes.cols) seen.add(l)
  return ALL_RISK_LEVELS.filter((l) => seen.has(l))
}

/** Rank: top row = N, left col = 1 (Excel workbook). */
export function rowRank(axes: MatrixAxes, probability: RiskLevel): number {
  const idx = axes.rows.indexOf(probability)
  if (idx < 0) return 1
  return axes.rows.length - idx
}

export function colRank(axes: MatrixAxes, impact: RiskLevel): number {
  const idx = axes.cols.indexOf(impact)
  if (idx < 0) return 1
  return idx + 1
}

/** Numeric score = Probability rank × Impact rank. */
export function excelScore(axes: MatrixAxes, probability: RiskLevel, impact: RiskLevel): number {
  return rowRank(axes, probability) * colRank(axes, impact)
}

export function excelBandLabel(gridSize: MatrixGridSize, score: number): string {
  return excelRating(gridSize, score)
}

/** Score band on the active matrix (same five names on 3×3 / 4×4 / 5×5). */
export function excelRating(gridSize: MatrixGridSize, score: number): string {
  if (gridSize === 3) {
    if (score >= 9) return 'VERY_HIGH'
    if (score >= 6) return 'HIGH'
    if (score >= 3) return 'MEDIUM'
    if (score >= 2) return 'LOW'
    return 'VERY_LOW'
  }
  if (gridSize === 4) {
    if (score >= 16) return 'VERY_HIGH'
    if (score >= 9) return 'HIGH'
    if (score >= 6) return 'MEDIUM'
    if (score >= 3) return 'LOW'
    return 'VERY_LOW'
  }
  if (score >= 20) return 'VERY_HIGH'
  if (score >= 13) return 'HIGH'
  if (score >= 9) return 'MEDIUM'
  if (score >= 5) return 'LOW'
  return 'VERY_LOW'
}

/** Map an Excel rating label to the API risk level used for RAG chips. */
export function excelRatingToLevel(rating: string | null | undefined): RiskLevel {
  const r = (rating ?? '').toUpperCase().replace(/[\s-]+/g, '_')
  if (r === 'VERY_HIGH' || r === 'CRITICAL') return 'VERY_HIGH'
  if (r === 'HIGH') return 'HIGH'
  if (r === 'MEDIUM') return 'MEDIUM'
  if (r === 'LOW_MED' || r === 'LOWMED') return 'LOW'
  if (r === 'VERY_LOW') return 'VERY_LOW'
  if (r === 'LOW') return 'LOW'
  return 'MEDIUM'
}

/** Action text aligned to the active-matrix band. */
export function excelAction(gridSize: MatrixGridSize, score: number): string {
  if (gridSize === 3) {
    if (score >= 9) return 'Immediate action required: escalate to top management'
    if (score >= 6) return 'Urgent treatment required: assign risk owner'
    if (score >= 3) return 'Treatment plan required within 30 days'
    if (score >= 2) return 'Monitor and review: treatment may be required'
    return 'Accept and monitor: periodic review'
  }
  if (gridSize === 4) {
    if (score >= 16) return 'Immediate escalation: board/executive level'
    if (score >= 9) return 'Urgent treatment: senior management action'
    if (score >= 6) return 'Treatment plan required: management action'
    if (score >= 3) return 'Monitor and review: routine management'
    return 'Accept: periodic monitoring only'
  }
  if (score >= 20) return 'Immediate board escalation: cease activity if possible'
  if (score >= 13) return 'Urgent executive action: treatment mandatory'
  if (score >= 9) return 'Treatment required'
  if (score >= 5) return 'Within appetite: monitor with review date'
  return 'Auto-close: residual Very Low'
}

/** Auto-close: Very Low band only. */
export function excelAutoCloseMax(gridSize: MatrixGridSize): number {
  if (gridSize === 3) return 1
  if (gridSize === 4) return 2
  return 4
}

export function excelAutoClosable(gridSize: MatrixGridSize, score: number): boolean {
  return score <= excelAutoCloseMax(gridSize)
}

export function ceReductionPercent(gridSize: MatrixGridSize, ceRank: number): number {
  const r = Math.min(gridSize, Math.max(1, ceRank))
  if (gridSize === 3) return r === 1 ? 0 : r === 2 ? 0.4 : 0.8
  if (gridSize === 4) return r === 1 ? 0 : r === 2 ? 0.25 : r === 3 ? 0.5 : 0.75
  return r === 1 ? 0 : r === 2 ? 0.2 : r === 3 ? 0.4 : r === 4 ? 0.6 : 0.8
}

export function autoResidualScore(inherentScore: number, reduction: number): number {
  return Math.max(1, Math.round(inherentScore * (1 - reduction)))
}

export function appetiteMaxResidual(gridSize: MatrixGridSize, appetite: RiskLevel = 'LOW'): number {
  if (gridSize === 5) {
    if (appetite === 'VERY_LOW') return 4
    if (appetite === 'LOW') return 8
    if (appetite === 'MEDIUM') return 12
    if (appetite === 'HIGH') return 19
    return 25
  }
  if (gridSize === 4) {
    if (appetite === 'VERY_LOW') return 2
    if (appetite === 'LOW') return 4
    if (appetite === 'MEDIUM') return 8
    if (appetite === 'HIGH') return 12
    return 16
  }
  if (appetite === 'VERY_LOW') return 1
  if (appetite === 'LOW') return 2
  if (appetite === 'MEDIUM') return 4
  if (appetite === 'HIGH') return 6
  return 9
}

/** Highest Likelihood × Impact product for this grid (3×3 → 9, 4×4 → 16, 5×5 → 25). */
export function excelMaxProduct(gridSize: MatrixGridSize): number {
  return gridSize * gridSize
}

/** UI check for typed residual / treated-target scores. Empty is OK unless `required`. */
export function excelManualScoreError(
  raw: string,
  gridSize: MatrixGridSize,
  opts?: { required?: boolean; max?: number },
): string {
  const matrixMax = excelMaxProduct(gridSize)
  const max = Math.min(matrixMax, Math.max(1, opts?.max ?? matrixMax))
  const t = raw.trim()
  if (!t) return opts?.required ? `Enter a whole number from 1 to ${max}` : ''
  if (!/^\d+$/.test(t)) return `Enter a whole number from 1 to ${max}`
  const n = Number(t)
  if (n < 1 || n > max) return `Must be a whole number from 1 to ${max}`
  return ''
}

/** Excel band → workflow RiskLevel (legacy bridge only). */
export function excelLevelFromScore(gridSize: MatrixGridSize, score: number): RiskLevel {
  const band = excelRating(gridSize, score)
  if (band === 'CRITICAL' || band === 'VERY_HIGH') return 'VERY_HIGH'
  if (band === 'HIGH' || band === 'MED-HIGH') return 'HIGH'
  if (band === 'MEDIUM') return 'MEDIUM'
  if (band === 'VERY_LOW') return 'VERY_LOW'
  return 'LOW'
}

export function excelLevel(
  gridSize: MatrixGridSize,
  axes: MatrixAxes,
  probability: RiskLevel,
  impact: RiskLevel,
): RiskLevel {
  return excelLevelFromScore(gridSize, excelScore(axes, probability, impact))
}

export function levelGrid(gridSize: MatrixGridSize, axes: MatrixAxes): RiskLevel[][] {
  return axes.rows.map((row) => axes.cols.map((col) => excelLevel(gridSize, axes, row, col)))
}

export function scoreGrid(gridSize: MatrixGridSize, axes: MatrixAxes): number[][] {
  return axes.rows.map((row) => axes.cols.map((col) => excelScore(axes, row, col)))
}

const ROW_INDEX_5: Record<RiskLevel, number> = {
  VERY_HIGH: 0,
  HIGH: 1,
  MEDIUM: 2,
  LOW: 3,
  VERY_LOW: 4,
}

const COL_INDEX_5: Record<RiskLevel, number> = {
  VERY_LOW: 0,
  LOW: 1,
  MEDIUM: 2,
  HIGH: 3,
  VERY_HIGH: 4,
}

/** §13.3 CE × Inherent (auto residual). */
export const DEFAULT_RESIDUAL_CE_5: RiskLevel[][] = [
  ['VERY_LOW', 'VERY_LOW', 'LOW', 'LOW', 'LOW'],
  ['VERY_LOW', 'VERY_LOW', 'LOW', 'MEDIUM', 'MEDIUM'],
  ['VERY_LOW', 'VERY_LOW', 'LOW', 'MEDIUM', 'HIGH'],
  ['VERY_LOW', 'LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH'],
  ['VERY_LOW', 'LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH'],
]

export function projectGrid(full5x5: RiskLevel[][], axes: MatrixAxes): RiskLevel[][] {
  return axes.rows.map((rowLevel) =>
    axes.cols.map((colLevel) => full5x5[ROW_INDEX_5[rowLevel]][COL_INDEX_5[colLevel]]),
  )
}

export function defaultResidualCe(gridSize: MatrixGridSize, axes?: MatrixAxes): RiskLevel[][] {
  return projectGrid(DEFAULT_RESIDUAL_CE_5, axes ?? defaultAxes(gridSize))
}

/** @deprecated stored grids ignored for Excel P×I: formula used instead */
export function projectGridExcel(_full5x5: RiskLevel[][], axes: MatrixAxes): RiskLevel[][] {
  return levelGrid(axes.rows.length as MatrixGridSize, axes)
}

export function defaultInherent(gridSize: MatrixGridSize, axes?: MatrixAxes): RiskLevel[][] {
  return levelGrid(gridSize, axes ?? defaultAxes(gridSize))
}

export function defaultResidual(gridSize: MatrixGridSize, axes?: MatrixAxes): RiskLevel[][] {
  return levelGrid(gridSize, axes ?? defaultAxes(gridSize))
}

export function calcInherent(
  _grid: RiskLevel[][],
  axes: MatrixAxes,
  probability: RiskLevel,
  impact: RiskLevel,
): RiskLevel {
  const n = axes.rows.length as MatrixGridSize
  return excelLevel(n, axes, probability, impact)
}

/** Residual = residual P × residual I (Excel). */
export function calcResidualPi(
  axes: MatrixAxes,
  residualProbability: RiskLevel,
  residualImpact: RiskLevel,
): RiskLevel {
  const n = axes.rows.length as MatrixGridSize
  return excelLevel(n, axes, residualProbability, residualImpact)
}

/** Auto residual from CE% reduction of inherent product score. */
export function calcResidual(
  _grid: RiskLevel[][],
  axes: MatrixAxes,
  controlEffectiveness: RiskLevel,
  _inherent: RiskLevel,
  probability?: RiskLevel,
  impact?: RiskLevel,
): RiskLevel {
  const n = axes.rows.length as MatrixGridSize
  const inherentScore =
    probability && impact ? excelScore(axes, probability, impact) : excelScore(axes, controlEffectiveness, _inherent)
  const reduction = ceReductionPercent(n, colRank(axes, controlEffectiveness))
  return excelLevelFromScore(n, autoResidualScore(inherentScore, reduction))
}

export function matrixSizeLabel(gridSize: MatrixGridSize): string {
  return `${gridSize}×${gridSize}`
}

export function axesEqual(a: MatrixAxes, b: MatrixAxes): boolean {
  return a.rows.length === b.rows.length && a.rows.every((l, i) => l === b.rows[i])
    && a.cols.length === b.cols.length && a.cols.every((l, i) => l === b.cols[i])
}

export function hasDistinctLevels(levels: RiskLevel[]): boolean {
  return new Set(levels).size === levels.length
}
