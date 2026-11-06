import { TableCell, TableSortLabel } from '@mui/material'
import { registerHeadCellSx } from './registerTableLayout'

export default function RegisterSortHeader({
  label,
  active,
  dir,
  onClick,
  width,
}: {
  label: string
  active: boolean
  dir: 'asc' | 'desc'
  onClick: () => void
  width?: number | string
}) {
  return (
    <TableCell width={width} sortDirection={active ? dir : false} sx={registerHeadCellSx}>
      <TableSortLabel
        active={active}
        direction={active ? dir : 'asc'}
        onClick={onClick}
        sx={{
          color: 'inherit',
          '&.Mui-active': { color: 'inherit' },
          '& .MuiTableSortLabel-icon': { color: 'inherit !important' },
        }}
      >
        {label}
      </TableSortLabel>
    </TableCell>
  )
}
