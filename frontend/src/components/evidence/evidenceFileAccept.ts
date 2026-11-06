/** Matches `arbor.evidence.allowed-extensions` and 25MB cap. */
export const EVIDENCE_ACCEPT = '.pdf,.docx,.xlsx,.png,.jpg,.jpeg'
export const EVIDENCE_MAX_BYTES = 25 * 1024 * 1024

function extensionsFromAccept(accept: string): string[] {
  return accept
    .split(',')
    .map((s) => s.trim().toLowerCase())
    .filter((s) => s.startsWith('.'))
    .map((s) => s.slice(1))
}

function fileExtension(name: string): string {
  const i = name.lastIndexOf('.')
  return i >= 0 ? name.slice(i + 1).toLowerCase() : ''
}

/** Field-level message when the picker or drop includes a disallowed type or oversized file. */
export function fileAcceptIssue(
  file: File,
  accept = EVIDENCE_ACCEPT,
  maxBytes = EVIDENCE_MAX_BYTES,
): string | null {
  const allowed = extensionsFromAccept(accept)
  const ext = fileExtension(file.name)
  if (allowed.length > 0 && !allowed.includes(ext)) {
    const listed = allowed.map((e) => e.toUpperCase()).join(', ')
    return `This file type is not allowed. Use ${listed}.`
  }
  if (maxBytes > 0 && file.size > maxBytes) {
    const mb = Math.round(maxBytes / (1024 * 1024))
    return `File is too large. Maximum size is ${mb}MB.`
  }
  return null
}
