/**
 * Hidden from the Privacy left-nav, Licensed Module shortcuts and Overview widgets
 * until product-ready (user, 2026-10-20: show ROPA and DPIA only).
 * Pages stay wired in Dashboard for deep links. Restore by removing the id.
 */
export const PRIVACY_PENDING_NAV_IDS = new Set([
  'privacy-inventory', // Data Inventory
  'privacy-dsr', // DSR
  'privacy-consent', // Consent
  'privacy-breach', // Breaches
])

export function isPrivacyNavPending(id: string): boolean {
  return PRIVACY_PENDING_NAV_IDS.has(id)
}
