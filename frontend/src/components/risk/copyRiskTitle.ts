/** Next unused "Copy of {title}" for the current project list. API still 409s if another tab won. */
export function nextCopyTitle(sourceTitle: string, existingTitles: Iterable<string>): string {
  const raw = sourceTitle.trim() || 'Untitled risk'
  const alreadyCopy = /^copy of /i.test(raw)
  const base = alreadyCopy ? raw.replace(/\s+\(\d+\)$/, '') : `Copy of ${raw}`
  const taken = new Set(
    [...existingTitles].map((t) => t.trim().toLowerCase()).filter(Boolean),
  )
  if (!taken.has(base.toLowerCase())) return base
  let n = 2
  while (taken.has(`${base} (${n})`.toLowerCase())) n += 1
  return `${base} (${n})`
}
