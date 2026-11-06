import { Box } from '@mui/material'
import type { ReactNode } from 'react'
import { brand } from '../../theme'

/** Shared content column: every section aligns to the same left/right edges. */
export const contributorRailMaxWidth = { xs: '100%', lg: 1200, xl: 1360 }

/** Task detail forms use the full rail width (same edges as list + stats). */
export const CONTRIBUTOR_DETAIL_MAX = contributorRailMaxWidth

export const contributorPadX = { xs: 2, sm: 3, lg: 4 } as const

const railBoxSx = {
  width: '100%',
  maxWidth: contributorRailMaxWidth,
  mx: 'auto',
  px: contributorPadX,
  boxSizing: 'border-box',
} as const

/** Full-viewport app shell. */
export default function ContributorShell({
  children,
  lockViewport,
}: {
  children: ReactNode
  /** Pin the shell to the viewport so a child pane can scroll instead of the page. */
  lockViewport?: boolean
}) {
  return (
    <Box
      sx={{
        minHeight: '100vh',
        height: lockViewport ? '100vh' : undefined,
        overflow: lockViewport ? 'hidden' : undefined,
        width: '100%',
        bgcolor: brand.canvas,
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {children}
    </Box>
  )
}

/** Centered content rail: header, banner, tabs, lists share this width. */
export function ContributorRail({
  children,
  sx,
}: {
  children: ReactNode
  sx?: Record<string, unknown>
}) {
  return (
    <Box
      sx={{
        ...railBoxSx,
        ...sx,
      }}
    >
      {children}
    </Box>
  )
}

/** @deprecated use ContributorRail */
export const ContributorInset = ContributorRail

/** Flex-grow work area below tabs: fills remaining viewport with a subtle tint. */
export function ContributorWorkArea({
  children,
  sx,
}: {
  children: ReactNode
  sx?: Record<string, unknown>
}) {
  return (
    <Box
      sx={{
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        minHeight: 0,
        overflow: 'auto',
        bgcolor: brand.hover,
        borderTop: `1px solid ${brand.border}`,
        ...sx,
      }}
    >
      {children}
    </Box>
  )
}
