/** Local calendar yyyy-MM-dd helpers for Super Admin licence expiry pickers. */

export function todayYmd(): string {
  const d = new Date()
  return toYmd(d)
}

export function ymdPlusYears(years: number): string {
  const d = new Date()
  d.setFullYear(d.getFullYear() + years)
  return toYmd(d)
}

export function toYmd(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

export function expiryInputValue(iso: string | null | undefined): string {
  if (!iso) return ymdPlusYears(1)
  return iso.slice(0, 10)
}

export function licenseStatusTone(status?: string): 'success' | 'warning' | 'danger' | 'neutral' {
  if (status === 'EXPIRED') return 'danger'
  if (status === 'EXPIRING') return 'warning'
  if (status === 'ACTIVE') return 'success'
  return 'neutral'
}

export function licenseStatusLabel(mod: {
  enabled?: boolean
  status?: string
  expired?: boolean
}): string {
  if (!mod.enabled) return 'Not licensed'
  if (mod.status === 'EXPIRED' || mod.expired) return 'Expired'
  if (mod.status === 'EXPIRING') return 'Expiring'
  return 'Licensed'
}

export function tenantLicenseStatusLabel(status?: string | null, writeLocked?: boolean): string {
  if (writeLocked || status === 'EXPIRED') return 'Expired'
  if (status === 'EXPIRING') return 'Expiring'
  if (!status || status === 'NONE') return 'None'
  return 'Licensed'
}

export function tenantExpiryRemainingLabel(
  daysRemaining: number | null | undefined,
  writeLocked?: boolean,
): string | null {
  if (writeLocked) return 'Has expired module'
  if (daysRemaining == null) return null
  if (daysRemaining < 0) return 'Term has ended'
  if (daysRemaining === 0) return 'Expires today'
  if (daysRemaining === 1) return '1 day left'
  return `${daysRemaining} days left`
}

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

/** Calendar yyyy-MM-dd for a licence term. Instant values use the UTC date the API stored. */
export function expiryCalendarYmd(iso: string | null | undefined): string | null {
  if (!iso) return null
  const match = /^(\d{4}-\d{2}-\d{2})/.exec(iso.trim())
  return match ? match[1] : null
}

/** True when the licence row has a stored calendar expiry. */
export function hasLicenceCalendarDate(mod: {
  expiresOn?: string | null
  expiresAt?: string | null
}): boolean {
  return Boolean(expiryCalendarYmd(mod.expiresOn) ?? expiryCalendarYmd(mod.expiresAt))
}

/** Unambiguous licence date: 16 Sep 2027 (year cannot hide in DD/MM/YYYY). */
export function formatLicenceCalendarDate(ymd: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(ymd)
  if (!match) return ymd
  return `${Number(match[3])} ${MONTHS[Number(match[2]) - 1]} ${match[1]}`
}

function calendarDaysBetween(fromYmd: string, toYmd: string): number {
  const from = Date.parse(`${fromYmd}T00:00:00Z`)
  const to = Date.parse(`${toYmd}T00:00:00Z`)
  if (Number.isNaN(from) || Number.isNaN(to)) return 0
  return Math.round((to - from) / 86_400_000)
}

export type LicenceTermKind = 'none' | 'expired' | 'today' | 'soon' | 'ok'

export type LicenceTermView = {
  kind: LicenceTermKind
  tone: 'danger' | 'warning' | 'success' | 'neutral'
  /** Expired on 16 Sep 2026 | Expires 16 Sep 2027 */
  headline: string
  detail: string | null
  ymd: string | null
}

export function licenceTermView(input: {
  expiresOn?: string | null
  expiresAt?: string | null
  expired?: boolean
  status?: string
  daysRemaining?: number | null
  /** workspace: Valid until DATE (tenant Frameworks). operator: Expires DATE (Super Admin). */
  phrase?: 'expires' | 'valid-until'
}): LicenceTermView {
  const ymd = expiryCalendarYmd(input.expiresOn) ?? expiryCalendarYmd(input.expiresAt)
  if (!ymd) {
    return {
      kind: 'none',
      tone: 'neutral',
      headline: 'No expiry date',
      detail: input.phrase === 'valid-until' ? null : 'Set a term',
      ymd: null,
    }
  }
  const remaining = calendarDaysBetween(todayYmd(), ymd)
  const dateLabel = formatLicenceCalendarDate(ymd)
  const past = remaining < 0 || input.expired === true || input.status === 'EXPIRED'
  if (past) {
    return { kind: 'expired', tone: 'danger', headline: `Expired on ${dateLabel}`, detail: 'Term has ended', ymd }
  }
  if (remaining === 0) {
    return { kind: 'today', tone: 'warning', headline: 'Expires today', detail: dateLabel, ymd }
  }
  const detail = remaining === 1 ? '1 day left' : `${remaining} days left`
  const liveHeadline = input.phrase === 'valid-until' ? `Valid until ${dateLabel}` : `Expires ${dateLabel}`
  if (remaining <= 90 || input.status === 'EXPIRING') {
    return { kind: 'soon', tone: 'warning', headline: liveHeadline, detail, ymd }
  }
  return { kind: 'ok', tone: 'success', headline: liveHeadline, detail, ymd }
}

/** Header lock copy when the open screen belongs to an expired module. */
export function moduleWriteLockReason(moduleCode: string | undefined | null, expiredCodes: string[]): string | undefined {
  if (!moduleCode || !expiredCodes.includes(moduleCode)) return undefined
  if (moduleCode === 'AUDIT') {
    return 'Audit is view-only because every licensed module has expired. Contact Arbor to extend a licence.'
  }
  return `${moduleCode} is view-only because its licence has expired. Other licensed modules stay fully usable. Contact Arbor to extend ${moduleCode}.`
}
