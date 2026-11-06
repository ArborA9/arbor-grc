import { TablePagination } from '@mui/material'
import { brand } from '../../theme'

/**
 * Register pager. Hidden unless a second page is needed (count > rowsPerPage).
 */
export default function RegisterPagination({
  count,
  page,
  rowsPerPage,
  onPageChange,
  onRowsPerPageChange,
  alwaysShow = false,
  dense = false,
  rowsPerPageOptions = [10, 25, 50],
}: {
  count: number
  page: number
  rowsPerPage: number
  onPageChange: (page: number) => void
  onRowsPerPageChange: (rowsPerPage: number) => void
  /** Force the footer on (rare). Prefer omitting so short lists stay clean. */
  alwaysShow?: boolean
  /** Shorter labels and no first/last buttons (working papers). */
  dense?: boolean
  rowsPerPageOptions?: number[]
  /** @deprecated Ignored. */
  showWhenEmpty?: boolean
}) {
  if (count === 0) return null
  if (!alwaysShow && count <= rowsPerPage) return null
  const bar = dense ? 28 : 36
  return (
    <TablePagination
      component="div"
      count={count}
      page={page}
      onPageChange={(_, next) => onPageChange(next)}
      rowsPerPage={rowsPerPage}
      onRowsPerPageChange={(e) => onRowsPerPageChange(parseInt(e.target.value, 10))}
      rowsPerPageOptions={rowsPerPageOptions}
      showFirstButton={!dense && alwaysShow}
      showLastButton={!dense && alwaysShow}
      labelRowsPerPage={dense ? 'Rows' : 'Rows per page'}
      labelDisplayedRows={({ from, to, count: total }) =>
        total === 0 ? 'No rows' : dense ? `${from}-${to} of ${total}` : `Showing ${from}-${to} of ${total}`
      }
      sx={{
        borderTop: `1px solid ${brand.border}`,
        minHeight: bar,
        '.MuiTablePagination-toolbar': {
          minHeight: bar,
          height: bar,
          px: dense ? 0.75 : 1,
          py: 0,
        },
        '.MuiTablePagination-spacer': { flex: '1 1 auto' },
        '.MuiTablePagination-selectLabel, .MuiTablePagination-displayedRows': {
          fontSize: dense ? '0.6875rem' : '0.75rem',
          m: 0,
          color: brand.muted,
        },
        '.MuiTablePagination-select': {
          fontSize: dense ? '0.6875rem' : '0.75rem',
          py: 0,
        },
        '.MuiTablePagination-actions': {
          ml: 0.25,
          '& .MuiIconButton-root': { p: dense ? 0.25 : 0.5 },
        },
      }}
    />
  )
}
