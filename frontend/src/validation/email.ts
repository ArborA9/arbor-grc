import { MSG_EMAIL_INVALID } from './messages'

/**
 * Practical email format check (not full RFC 5322).
 * Mirrors backend {@code com.a9.arbor.grc.common.validation.EmailRules}.
 */
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function isValidEmail(value: string): boolean {
  const trimmed = value.trim()
  if (!trimmed) return false
  return EMAIL_PATTERN.test(trimmed)
}

/** Returns an error message when invalid; empty string when valid or blank (optional fields). */
export function emailError(value: string | null | undefined, required = false): string {
  const trimmed = (value ?? '').trim()
  if (!trimmed) return required ? 'Email is required' : ''
  return isValidEmail(trimmed) ? '' : MSG_EMAIL_INVALID
}
