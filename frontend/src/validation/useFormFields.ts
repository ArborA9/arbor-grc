import { useCallback, useState } from 'react'
import { extractFieldErrors, visibleFieldError } from './apiErrors'

/**
 * Tracks touched/submitted state and maps API field errors to form fields.
 * Use with ValidatedTextField or visibleError() for blur-first validation UX.
 */
export function useFormFields<T extends string>() {
  const [touched, setTouched] = useState<Partial<Record<T, boolean>>>({})
  const [submitted, setSubmitted] = useState(false)
  const [serverErrors, setServerErrors] = useState<Partial<Record<T, string>>>({})

  const touch = useCallback((field: T) => {
    setTouched((prev) => ({ ...prev, [field]: true }))
  }, [])

  const touchAll = useCallback(() => {
    setSubmitted(true)
  }, [])

  const reset = useCallback(() => {
    setTouched({})
    setSubmitted(false)
    setServerErrors({})
  }, [])

  /** Returns true when field errors were applied (caller can skip generic toast). */
  const applyApiError = useCallback((error: unknown): boolean => {
    const parsed = extractFieldErrors(error) as Partial<Record<T, string>>
    if (Object.keys(parsed).length === 0) return false
    setServerErrors(parsed)
    setSubmitted(true)
    return true
  }, [])

  const fieldError = useCallback(
    (field: T, clientError: string): string =>
      visibleFieldError(clientError, !!touched[field], submitted, serverErrors[field]),
    [touched, submitted, serverErrors],
  )

  const hasError = useCallback(
    (field: T, clientError: string): boolean => !!fieldError(field, clientError),
    [fieldError],
  )

  return {
    touch,
    touchAll,
    reset,
    applyApiError,
    fieldError,
    hasError,
    submitted,
    setSubmitted,
    serverErrors,
  }
}
