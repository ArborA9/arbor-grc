/** Field-level errors parsed from API validation responses. */

export type FieldErrorMap = Record<string, string>

/** Parse `"field: message; field2: message2"` from RFC-7807 ProblemDetail.detail. */
export function parseFieldErrorsFromDetail(detail: string): FieldErrorMap {
  const map: FieldErrorMap = {}
  if (!detail?.trim()) return map
  for (const part of detail.split('; ')) {
    const colon = part.indexOf(': ')
    if (colon <= 0) continue
    const field = part.slice(0, colon).trim()
    const message = part.slice(colon + 2).trim()
    if (!field || !message) continue
    if (!/^[A-Za-z][A-Za-z0-9_.]*$/.test(field)) continue
    map[field] = message
  }
  return map
}

/** Prefer structured `errors` array; fall back to parsing detail text. */
export function parseFieldErrorsFromProblem(problem: unknown, detail: string): FieldErrorMap {
  const map: FieldErrorMap = {}
  if (problem && typeof problem === 'object' && 'errors' in problem) {
    const errors = (problem as { errors: unknown }).errors
    if (Array.isArray(errors)) {
      for (const err of errors) {
        if (err && typeof err === 'object' && 'field' in err && 'message' in err) {
          const field = String((err as { field: unknown }).field)
          const message = String((err as { message: unknown }).message)
          if (field && message) map[field] = message
        }
      }
    }
  }
  return Object.keys(map).length > 0 ? map : parseFieldErrorsFromDetail(detail)
}

/** Extract field errors from an ApiError or plain Error message. */
export function extractFieldErrors(error: unknown): FieldErrorMap {
  if (error && typeof error === 'object' && 'fieldErrors' in error) {
    const fieldErrors = (error as { fieldErrors: unknown }).fieldErrors
    if (fieldErrors && typeof fieldErrors === 'object' && !Array.isArray(fieldErrors)) {
      return fieldErrors as FieldErrorMap
    }
  }
  if (error instanceof Error) return parseFieldErrorsFromDetail(error.message)
  return {}
}

/** Show client error only after blur or submit; server errors always win. */
export function visibleFieldError(
  clientError: string,
  touched: boolean,
  submitAttempted: boolean,
  serverError?: string,
): string {
  if (serverError) return serverError
  if (!touched && !submitAttempted) return ''
  return clientError
}
