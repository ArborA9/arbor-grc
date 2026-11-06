/**
 * Hidden from Risk Management left-nav and the Contributor dashboard
 * (module cards, Decide Today, Your Queue) until we pick them up again.
 * Detail handlers in ContributorHome stay wired for deep links.
 * Restore by removing the id from this set.
 */
export const RISK_NAV_HIDDEN_IDS = new Set([
  'kri', // KRI
  'incidents', // Incidents
])

export function isRiskNavHidden(id: string): boolean {
  return RISK_NAV_HIDDEN_IDS.has(id)
}
