/**
 * User-facing framework name. Underscores become spaces. ISO/IEC is shown as ISO.
 */
export function frameworkDisplayName(name?: string | null, code?: string | null): string {
  const rawName = (name ?? '').trim()
  const rawCode = (code ?? '').trim()
  if (rawCode === 'ISO_20000') return 'ISO 20000'
  const text = (rawName || rawCode).replace(/_/g, ' ').replace(/ISO\s*\/\s*IEC/gi, 'ISO')
  const cleaned = text.replace(/\s+/g, ' ').trim()
  if (/^ISO 20000-1$/i.test(cleaned)) return 'ISO 20000'
  return cleaned || '-'
}

/** Tenant-facing framework title: catalog name, no code and no version/year. */
export function frameworkLabel(f: {
  name?: string | null
  code: string
  version?: string | null
}): string {
  const name = (f.name || '').trim() || f.code
  const ver = (f.version || '').trim()
  if (!ver) return name
  const escaped = ver.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const stripped = name.replace(new RegExp(`\\s+${escaped}$`, 'i'), '').trim()
  return frameworkDisplayName(stripped || name, f.code)
}
