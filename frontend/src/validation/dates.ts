import { MSG_DATE_PAST, MSG_DATE_RANGE, MSG_DATE_WITHIN_YEAR } from './messages'

/**
 * Date rule categories (keep in sync with backend DateRules):
 * - A: action due dates: today or future when set
 * - B: scheduled review dates: today or future when newly set (unchanged past OK on edit)
 * - C: expiry / contract: any valid date (past allowed)
 * - D: ranges: end on or after start
 */

function formatIsoDate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** Today's date as YYYY-MM-DD in local timezone (for HTML date input min/max). */
export function todayIso(): string {
  return formatIsoDate(startOfToday())
}

/** Latest allowed tolerance review date (+1 year). */
export function maxToleranceReviewIso(): string {
  const d = startOfToday()
  d.setFullYear(d.getFullYear() + 1)
  return formatIsoDate(d)
}

/** Parse YYYY-MM-DD; returns null when empty or invalid. */
export function parseIsoDate(value: string | null | undefined): Date | null {
  if (!value?.trim()) return null
  const parts = value.trim().split('-').map(Number)
  if (parts.length !== 3 || parts.some((n) => Number.isNaN(n))) return null
  const [y, m, d] = parts
  const date = new Date(y, m - 1, d)
  if (date.getFullYear() !== y || date.getMonth() !== m - 1 || date.getDate() !== d) return null
  return date
}

function startOfToday(): Date {
  const d = new Date()
  d.setHours(0, 0, 0, 0)
  return d
}

/** Category A: action due dates: today or future. Null/empty is valid when optional. */
export function isOnOrAfterToday(value: string | null | undefined): boolean {
  const parsed = parseIsoDate(value)
  if (!parsed) return true
  return parsed.getTime() >= startOfToday().getTime()
}

/**
 * Action due date that must be today or later, and on or before `latest` when that date is set.
 * `latestLabel` is the name of the cap, for example "Gap Assessment target date".
 */
export function dueDateBoundError(
  value: string | null | undefined,
  latest: string | null | undefined,
  latestLabel: string,
): string {
  const base = actionDueDateError(value)
  if (base) return base
  const trimmed = (value ?? '').trim()
  const cap = (latest ?? '').trim()
  if (!trimmed || !cap) return ''
  const due = parseIsoDate(trimmed)
  const end = parseIsoDate(cap)
  if (!due || !end) return ''
  return due.getTime() <= end.getTime() ? '' : `Due date cannot be later than the ${latestLabel}`
}

export function actionDueDateError(value: string | null | undefined, required = false): string {
  const trimmed = (value ?? '').trim()
  if (!trimmed) return required ? 'Date is required' : ''
  if (!parseIsoDate(trimmed)) return 'Enter a valid date'
  return isOnOrAfterToday(trimmed) ? '' : MSG_DATE_PAST
}

/**
 * On edit: allow an unchanged past due date (overdue records); reject only when changed to a past date.
 */
export function actionDueDateErrorIfChanged(
  current: string | null | undefined,
  next: string | null | undefined,
  required = false,
): string {
  const trimmedNext = (next ?? '').trim()
  const trimmedCurrent = (current ?? '').trim()
  if (!trimmedNext) return required ? 'Date is required' : ''
  if (trimmedNext === trimmedCurrent) return ''
  return actionDueDateError(trimmedNext, required)
}

/** Inclusive upper bound: date must be on or before today + years. */
export function isWithinYearsFromToday(value: string | null | undefined, years: number): boolean {
  const parsed = parseIsoDate(value)
  if (!parsed) return true
  const max = startOfToday()
  max.setFullYear(max.getFullYear() + years)
  return parsed.getTime() <= max.getTime()
}

/** Tolerance review date: today → +1 year (matches RiskService). */
export function toleranceReviewDateError(value: string | null | undefined, required = false): string {
  const trimmed = (value ?? '').trim()
  if (!trimmed) return required ? 'Review date is required' : ''
  if (!parseIsoDate(trimmed)) return 'Enter a valid date'
  if (!isOnOrAfterToday(trimmed)) return MSG_DATE_PAST
  if (!isWithinYearsFromToday(trimmed, 1)) return MSG_DATE_WITHIN_YEAR
  return ''
}

/** Category D: end on or after start; either side may be empty. */
export function dateRangeError(
  start: string | null | undefined,
  end: string | null | undefined,
): string {
  const s = parseIsoDate(start)
  const e = parseIsoDate(end)
  if (!s || !e) return ''
  return e.getTime() >= s.getTime() ? '' : MSG_DATE_RANGE
}

/** Category C: optional date; validates format only (past dates allowed). */
export function optionalIsoDateError(value: string | null | undefined): string {
  const trimmed = (value ?? '').trim()
  if (!trimmed) return ''
  return parseIsoDate(trimmed) ? '' : 'Enter a valid date'
}
