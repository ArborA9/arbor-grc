import type { ColumnDefinition } from '../../hooks/columnVisibility'

export const SOA_REGISTER_COLUMNS: ColumnDefinition[] = [
  { id: 'control', label: 'Control', width: '80px', required: true },
  { id: 'title', label: 'Title', width: '1fr', required: true },
  { id: 'applicable', label: 'Applicable?', width: '120px' },
  { id: 'justification', label: 'Justification', width: 'minmax(160px, 1.2fr)' },
  { id: 'remarks', label: 'Remarks', width: 'minmax(140px, 1fr)' },
  { id: 'priority', label: 'Priority', width: '90px', defaultVisible: false },
]
