import { useMemo } from 'react'
import ColumnPicker from '../components/ui/ColumnPicker'
import type { ColumnDefinition } from './columnVisibility'
import { useColumnVisibility } from './useColumnVisibility'

/** Session column visibility + ready-to-slot `ColumnPicker` for `RegisterToolbar`. */
export function useRegisterColumnPicker(columns: ColumnDefinition[]) {
  const visibility = useColumnVisibility(columns)
  const columnPicker = useMemo(
    () => (
      <ColumnPicker
        columns={visibility.columns}
        visibleIds={visibility.visibleIds}
        onToggle={visibility.toggle}
        onReset={visibility.reset}
      />
    ),
    [visibility.columns, visibility.visibleIds, visibility.toggle, visibility.reset],
  )
  return { ...visibility, columnPicker }
}
