import { MODULE_LABELS } from './api'
import { licenceTermView } from './licenseExpiry'
import { formatTenantDateTime } from './uiQuality/tenantLocale'

/** Display name before the colon in MODULE_LABELS (e.g. "Privacy" from "Privacy"). */
export function moduleShortLabel(code: string): string {
  const full = MODULE_LABELS[code]
  if (!full) return code
  const dash = full.indexOf(': ')
  return dash > 0 ? full.slice(0, dash) : full
}

/** Subtitle after the colon in MODULE_LABELS. */
export function moduleDescription(code: string): string {
  const full = MODULE_LABELS[code]
  if (!full) return ''
  const dash = full.indexOf(': ')
  return dash > 0 ? full.slice(dash + 2) : full
}

export function formatLicenseDateTime(iso: string | null | undefined): string {
  if (!iso) return '-'
  return formatTenantDateTime(iso)
}

export function formatLicenseDate(iso: string | null | undefined): string {
  if (!iso) return 'No expiry date'
  return licenceTermView({ expiresOn: iso, expiresAt: iso }).headline
}
