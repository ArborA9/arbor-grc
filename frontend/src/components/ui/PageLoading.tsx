import type { ReactNode } from 'react'
import { Box, CircularProgress, Typography } from '@mui/material'
import { brand } from '../../theme'

export default function PageLoading({ label = 'Loading…' }: { label?: string }) {
  return (
    <Box
      role="status"
      aria-live="polite"
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 1,
        minHeight: 220,
        color: brand.muted,
      }}
    >
      <CircularProgress size={22} sx={{ color: brand.goldDark }} />
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
    </Box>
  )
}

export function EmptyRegister({
  message,
  action,
}: {
  message: string
  action?: ReactNode
}) {
  return (
    <Box sx={{ py: 4, px: 2, textAlign: 'center' }}>
      <Typography variant="body2" color="text.secondary">
        {message}
      </Typography>
      {action ? <Box sx={{ mt: 1.5 }}>{action}</Box> : null}
    </Box>
  )
}
