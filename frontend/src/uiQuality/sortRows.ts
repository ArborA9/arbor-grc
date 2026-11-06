export type SortDir = 'asc' | 'desc'

export function toggleSort(currentKey: string, currentDir: SortDir, nextKey: string): { key: string; dir: SortDir } {
  if (currentKey === nextKey) return { key: nextKey, dir: currentDir === 'asc' ? 'desc' : 'asc' }
  return { key: nextKey, dir: 'asc' }
}

export function compareLoose(a: unknown, b: unknown, dir: SortDir): number {
  const av = a == null ? '' : String(a)
  const bv = b == null ? '' : String(b)
  const cmp = av.localeCompare(bv, undefined, { numeric: true, sensitivity: 'base' })
  return dir === 'asc' ? cmp : -cmp
}
