/** Column metadata for register table visibility (session-only state). */
export type ColumnDefinition = {
  id: string
  label: string
  /**
   * CSS grid track, e.g. `7rem`, `max-content`, or `minmax(10rem, 1fr)`.
   * Prefer fixed/`max-content` for metadata; reserve `fr` for the primary text column only.
   */
  width?: string
  /** When true, column cannot be hidden. */
  required?: boolean
  /** Defaults to true when omitted. */
  defaultVisible?: boolean
}

export function defaultVisibleColumnIds(columns: ColumnDefinition[]): string[] {
  return columns.filter((c) => c.required || c.defaultVisible !== false).map((c) => c.id)
}

/** Long-text columns that should absorb leftover width (never a date or chip). */
const FLEX_COLUMN_IDS = new Set([
  'title',
  'description',
  'observedGap',
  'treatmentPlan',
  'treatmentDescription',
  'existingControls',
  'strategicObjective',
  'name',
  'justification',
  'remarks',
  'activity',
  'subject',
  'record',
  'reference',
  'project',
  'assessment',
])

function preferredMaxWidth(width: string | undefined): string {
  if (!width) return '8rem'
  const minmax = width.match(/minmax\(\s*([^,]+)\s*,\s*([^)]+)\)/i)
  if (minmax) {
    const min = minmax[1].trim()
    const max = minmax[2].trim()
    if (/\bfr\b/.test(max)) return min
    return max
  }
  return width
}

function flexColumnIndex(visible: ColumnDefinition[]): number {
  const namedFr = visible.findIndex((c) => c.width != null && /\bfr\b/.test(c.width))
  if (namedFr >= 0) return namedFr
  for (let i = 0; i < visible.length; i += 1) {
    if (FLEX_COLUMN_IDS.has(visible[i].id)) return i
  }
  const optional = visible.findIndex((c) => !c.required)
  return optional >= 0 ? optional : 0
}

/**
 * One text column grows (`1fr`). Every other track can shrink (`minmax(0, …)`)
 * so a full picker set does not force a horizontal scroll or leave a blank
 * strip on Date / Attachment / actions.
 */
export function buildGridTemplateColumns(
  columns: ColumnDefinition[],
  visibleIds: readonly string[],
): string {
  const visible = columns.filter((c) => visibleIds.includes(c.id))
  if (visible.length === 0) return ''

  const flexIndex = flexColumnIndex(visible)
  return visible
    .map((c, i) => (i === flexIndex ? 'minmax(0, 1fr)' : `minmax(0, ${preferredMaxWidth(c.width)})`))
    .join(' ')
}
