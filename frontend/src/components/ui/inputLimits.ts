/** Matches typical API `@Size` bands. Override on CompactTextField when the DTO differs. */
export const INPUT_LIMIT = {
  code: 64,
  orgName: 200,
  name: 255,
  email: 255,
  title: 512,
  /** Risk register title: IS seed is "{control code} - {control title}". */
  riskTitle: 1024,
  website: 500,
  dataSubjects: 500,
  reason: 2000,
  customQuestion: 2000,
  description: 4000,
  notes: 8000,
  policyBody: 50000,
} as const

export function inferInputLimit(label: string, multiline?: boolean): number {
  const l = label.toLowerCase()
  if (l.includes('email')) return INPUT_LIMIT.email
  if (l.includes('website')) return INPUT_LIMIT.website
  if (l.includes('data subjects')) return INPUT_LIMIT.dataSubjects
  if (l.includes('storage location')) return INPUT_LIMIT.website
  if (l.includes('body text')) return INPUT_LIMIT.policyBody
  if (l.includes('custom question')) return INPUT_LIMIT.customQuestion
  if (l.includes('legal name')) return INPUT_LIMIT.orgName
  if (l.includes('code') && !l.includes('related')) return INPUT_LIMIT.code
  if (l.includes('title')) return INPUT_LIMIT.title
  if (l.includes('reason')) return INPUT_LIMIT.reason
  if (/^[cia] justification$/.test(l)) return INPUT_LIMIT.description
  if (l.includes('justification')) return INPUT_LIMIT.notes
  if (multiline || l.includes('comment') || l.includes('note') || l.includes('description')) {
    return INPUT_LIMIT.notes
  }
  if (l.includes('name')) return INPUT_LIMIT.name
  return INPUT_LIMIT.name
}
