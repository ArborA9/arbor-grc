/** Hidden from Business Continuity left-nav until product-ready. Pages stay wired in Dashboard. */
export const BCMS_PENDING_NAV_IDS = new Set([
  'bcms-profiles', // Department Plan / Dept profiles
  'bcms-plans', // Recovery Plans (user, 2026-10-20)
  'bcms-exercises', // Exercises
  'bcms-ncema', // NCEMA
])

export function isBcmsNavPending(id: string): boolean {
  return BCMS_PENDING_NAV_IDS.has(id)
}
