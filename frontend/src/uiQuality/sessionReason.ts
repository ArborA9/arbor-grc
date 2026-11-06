const KEY = 'arbor_session_reason'

export type SessionEndReason = 'expired' | 'idle'

export function markSessionEnded(reason: SessionEndReason) {
  try {
    sessionStorage.setItem(KEY, reason)
  } catch {
    /* ignore quota */
  }
}

export function takeSessionEndMessage(): string {
  try {
    const reason = sessionStorage.getItem(KEY)
    sessionStorage.removeItem(KEY)
    if (reason === 'expired') return 'Your session expired. Please sign in again.'
    if (reason === 'idle') return 'You were signed out after inactivity. Please sign in again.'
  } catch {
    /* ignore */
  }
  return ''
}
