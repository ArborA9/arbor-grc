/** Full label for control effectiveness (client feedback: never abbreviate). */
export const CONTROL_EFFECTIVENESS_LABEL = 'Control effectiveness'

const TITLE: Record<string, string> = {
  VERY_LOW: 'Very Low',
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  VERY_HIGH: 'Very High',
}

export function fmtLevel(l: string | null | undefined): string {
  if (!l) return '-'
  return TITLE[l] ?? l.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase())
}
