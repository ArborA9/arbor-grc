import type { SxProps, Theme } from '@mui/material'
import { brand } from '../../theme'

/**
 * Register tables fill the card width. Use % tracks that sum ~100% so leftover
 * space is shared across columns: never a trailing empty spacer (looks like a bug)
 * and never `width: auto` on the name column under `table-layout: fixed` (steals all space).
 */
export const registerTableSx = {
  width: '100%',
  tableLayout: 'fixed',
} as const satisfies SxProps<Theme>

/** Same cream fill as theme head cells (ID, Type, and other non-sort columns). */
export const registerHeadCellSx = {
  fontSize: '0.6875rem',
  fontWeight: 700,
  letterSpacing: '0.08em',
  textTransform: 'uppercase' as const,
  color: brand.muted,
  background: brand.sidebar,
  backgroundColor: brand.sidebar,
  bgcolor: brand.sidebar,
  borderBottom: `1.5px solid ${brand.border}`,
  '&.MuiTableCell-stickyHeader': {
    background: brand.sidebar,
    backgroundColor: brand.sidebar,
  },
} as const satisfies SxProps<Theme>

/** Name / title: share of row, not leftover absorber. */
export const primaryTableCellSx = {
  width: '26%',
  verticalAlign: 'middle',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  pr: 1,
} as const satisfies SxProps<Theme>

/** Primary register identifier (project / asset name). Same size as table body. */
export const primaryRegisterNameSx = {
  fontWeight: 600,
  fontSize: '0.75rem',
  lineHeight: 1.3,
  color: brand.ink,
} as const satisfies SxProps<Theme>

/** Short metadata (year, status). */
export const compactTableCellSx = {
  width: '12%',
  whiteSpace: 'nowrap',
  verticalAlign: 'middle',
  pr: 1,
} as const satisfies SxProps<Theme>

/** Framework codes, types, etc. */
export const mediumTableCellSx = {
  width: '20%',
  whiteSpace: 'nowrap',
  verticalAlign: 'middle',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  pr: 1,
} as const satisfies SxProps<Theme>

/** Last data column: tight right edge against the card. */
export const lastTableCellSx = {
  pr: 1,
  verticalAlign: 'middle',
  whiteSpace: 'nowrap',
} as const satisfies SxProps<Theme>

/** @deprecated Prefer last-column width share; kept for gradual cleanup. */
export const spacerTableCellSx = {
  width: 0,
  p: 0,
  borderBottom: 'none',
} as const satisfies SxProps<Theme>

export function compactTableCellSxWidth(width: string | number): SxProps<Theme> {
  return {
    width,
    whiteSpace: 'nowrap',
    verticalAlign: 'middle',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    pr: 1,
  }
}

/** @deprecated Prefer compactTableCellSxWidth. */
export function compactTableCellSxMax(maxWidth: number): SxProps<Theme> {
  return compactTableCellSxWidth(Math.min(maxWidth, 180))
}
