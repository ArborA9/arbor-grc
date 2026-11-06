import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  buildGridTemplateColumns,
  defaultVisibleColumnIds,
  type ColumnDefinition,
} from './columnVisibility'

function sameIds(a: readonly string[], b: readonly string[]): boolean {
  return a.length === b.length && a.every((id, i) => id === b[i])
}

function pruneVisibleIds(
  prev: readonly string[],
  columns: ColumnDefinition[],
  defaults: string[],
  maxVisible?: number,
): string[] {
  const allowed = new Set(columns.map((c) => c.id))
  const order = columns.map((c) => c.id)
  const required = columns.filter((c) => c.required).map((c) => c.id)
  let next = prev.filter((id) => allowed.has(id))
  for (const id of required) {
    if (!next.includes(id)) next.push(id)
  }
  if (maxVisible != null && next.length > maxVisible) {
    const optional = next.filter((id) => !required.includes(id))
    next = [...required, ...optional.slice(0, Math.max(0, maxVisible - required.length))]
  }
  next.sort((a, b) => order.indexOf(a) - order.indexOf(b))
  if (next.length === 0) return defaults
  return next
}

export function useColumnVisibility(
  columns: ColumnDefinition[],
  options?: { maxVisible?: number },
) {
  const maxVisible = options?.maxVisible
  const defaults = useMemo(() => defaultVisibleColumnIds(columns), [columns])
  const [visibleIds, setVisibleIds] = useState<string[]>(() =>
    pruneVisibleIds(defaults, columns, defaults, maxVisible),
  )

  useEffect(() => {
    setVisibleIds((prev) => {
      const next = pruneVisibleIds(prev, columns, defaults, maxVisible)
      return sameIds(prev, next) ? prev : next
    })
  }, [columns, defaults, maxVisible])

  const isVisible = useCallback((id: string) => visibleIds.includes(id), [visibleIds])

  const toggle = useCallback(
    (id: string) => {
      const col = columns.find((c) => c.id === id)
      if (!col || col.required) return
      setVisibleIds((prev) => {
        if (prev.includes(id)) {
          const next = prev.filter((x) => x !== id)
          return next.length > 0 ? next : prev
        }
        if (maxVisible != null && prev.length >= maxVisible) return prev
        const order = columns.map((c) => c.id)
        return [...prev, id].sort((a, b) => order.indexOf(a) - order.indexOf(b))
      })
    },
    [columns, maxVisible],
  )

  const reset = useCallback(() => {
    setVisibleIds(pruneVisibleIds(defaults, columns, defaults, maxVisible))
  }, [columns, defaults, maxVisible])

  const gridTemplateColumns = useMemo(
    () => buildGridTemplateColumns(columns, visibleIds),
    [columns, visibleIds],
  )

  const visibleCount = visibleIds.length

  return {
    columns,
    visibleIds,
    visibleCount,
    isVisible,
    toggle,
    reset,
    gridTemplateColumns,
  }
}
