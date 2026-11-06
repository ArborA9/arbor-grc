import type { UserSummary } from './api'

/** Gap Assessor is a stored name. Only an active Admin or Owner can be chosen. */
export function isGapAssessor(user: UserSummary): boolean {
  return user.status === 'ACTIVE' && (user.roles.includes('ADMIN') || user.roles.includes('OWNER'))
}
