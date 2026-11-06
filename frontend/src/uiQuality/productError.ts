/** User-facing error copy. Never show host/port or `ApiError:` to testers. */

export function productErrorMessage(e: unknown): string {
  if (e && typeof e === 'object' && 'status' in e && (e as { status?: number }).status === 403) {
    return 'You don’t have permission to do that.'
  }
  if (e == null || e === '') return ''
  const raw =
    e instanceof Error ? e.message : typeof e === 'string' ? e : 'Something went wrong. Try again.'
  const cleaned = raw.replace(/^ApiError:\s*/i, '').trim()
  if (!cleaned) return ''
  if (
    /cannot reach|backend running|:8080|:18080|failed to fetch|networkerror|load failed|err_connection/i.test(
      cleaned,
    )
  ) {
    return 'Arbor can’t reach the server. Try again in a moment.'
  }
  if (/^(403|forbidden)$/i.test(cleaned) || /^403\s+forbidden/i.test(cleaned)) {
    return 'You don’t have permission to do that.'
  }
  return cleaned
}
