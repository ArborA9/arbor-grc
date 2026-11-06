import type { ColumnDefinition } from '../../hooks/columnVisibility'

export const GAP_REGISTER_COLUMNS: ColumnDefinition[] = [
  { id: 'control', label: 'Control', width: '90px', required: true },
  { id: 'title', label: 'Title', width: '1fr', required: true },
  { id: 'gapStatus', label: 'Gap status', width: '130px' },
  { id: 'evidence', label: 'Workflow', width: '168px' },
  { id: 'assigned', label: 'Assigned to', width: '130px' },
]
