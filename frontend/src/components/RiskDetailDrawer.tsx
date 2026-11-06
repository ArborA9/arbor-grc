import { type ReactNode, type RefObject } from 'react'
import { Box, Card } from '@mui/material'

export type RiskDetailSection = { id: string; label: string }

/** Scrollable content panel for risk detail (chrome lives in AppShell topbar). */
export default function RiskDetailDrawer({
  scrollRef,
  autoClosedMessage,
  children,
}: {
  scrollRef: RefObject<HTMLDivElement | null>
  autoClosedMessage?: ReactNode
  children: ReactNode
}) {
  return (
    <Card
      sx={{
        flex: 1,
        minHeight: 0,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
      }}
    >
      <Box ref={scrollRef} sx={{ flex: 1, minHeight: 0, overflow: 'auto', px: 2.5, py: 2 }}>
        {autoClosedMessage ? <Box sx={{ mb: 1.25 }}>{autoClosedMessage}</Box> : null}
        {children}
      </Box>
    </Card>
  )
}
