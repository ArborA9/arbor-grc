export type TenantLocale = {
  dateFormat: string
  timezone: string
  displayName: string | null
}

export const DEFAULT_TENANT_LOCALE: TenantLocale = {
  dateFormat: 'DD/MM/YYYY',
  timezone: 'Asia/Dubai',
  displayName: null,
}

let current: TenantLocale = { ...DEFAULT_TENANT_LOCALE }

export function setTenantLocale(next: Partial<TenantLocale>) {
  current = { ...current, ...next }
}

export function getTenantLocale(): TenantLocale {
  return current
}

function pad(n: number) {
  return String(n).padStart(2, '0')
}

function partsFromDateOnly(ymd: string): { y: number; m: number; d: number } | null {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(ymd.trim())
  if (!m) return null
  return { y: Number(m[1]), m: Number(m[2]), d: Number(m[3]) }
}

function partsInZone(iso: string, timeZone: string): { y: number; m: number; d: number; hh: number; mm: number } | null {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return null
  const fmt = new Intl.DateTimeFormat('en-GB', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  })
  const bag: Record<string, string> = {}
  for (const p of fmt.formatToParts(date)) {
    if (p.type !== 'literal') bag[p.type] = p.value
  }
  return {
    y: Number(bag.year),
    m: Number(bag.month),
    d: Number(bag.day),
    hh: Number(bag.hour),
    mm: Number(bag.minute),
  }
}

function formatYmd(y: number, m: number, d: number, dateFormat: string): string {
  const pattern = (dateFormat || 'DD/MM/YYYY').toUpperCase()
  if (pattern.startsWith('MM/DD')) return `${pad(m)}/${pad(d)}/${y}`
  if (pattern.startsWith('YYYY-MM') || pattern.startsWith('YYYY/MM')) return `${y}-${pad(m)}-${pad(d)}`
  return `${pad(d)}/${pad(m)}/${y}`
}

/** Date-only `YYYY-MM-DD` stays that calendar day. ISO datetimes use tenant timezone. */
export function formatTenantDate(value: string | null | undefined, locale: TenantLocale = current): string {
  if (!value) return '-'
  const trimmed = value.trim()
  if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) {
    const p = partsFromDateOnly(trimmed)
    if (p) return formatYmd(p.y, p.m, p.d, locale.dateFormat)
  }
  const z = partsInZone(value, locale.timezone || DEFAULT_TENANT_LOCALE.timezone)
  if (!z) return value
  return formatYmd(z.y, z.m, z.d, locale.dateFormat)
}

export function formatTenantDateTime(value: string | null | undefined, locale: TenantLocale = current): string {
  if (!value) return '-'
  if (/^\d{4}-\d{2}-\d{2}$/.test(value.trim())) return formatTenantDate(value, locale)
  const z = partsInZone(value, locale.timezone || DEFAULT_TENANT_LOCALE.timezone)
  if (!z) return value
  return `${formatYmd(z.y, z.m, z.d, locale.dateFormat)} ${pad(z.hh)}:${pad(z.mm)}`
}

export function formatAed(n: number | null | undefined): string {
  if (n == null || Number.isNaN(Number(n))) return '-'
  return new Intl.NumberFormat('en-AE', {
    style: 'currency',
    currency: 'AED',
    maximumFractionDigits: 0,
  }).format(Number(n))
}
