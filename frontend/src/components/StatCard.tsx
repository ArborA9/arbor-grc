import { type ReactNode } from 'react'
import { Box, Card, CardContent, Stack, Typography, alpha } from '@mui/material'
import { brand } from '../theme'

interface StatCardProps {
  label: string
  value: ReactNode
  icon: ReactNode
  color?: string
  hint?: string
}

/** Compact KPI strip cell: ops density, not marketing cards. */
export default function StatCard({ label, value, icon, color = brand.accent, hint }: StatCardProps) {
  return (
    <Card sx={{ flex: 1, minWidth: 0, height: '100%' }}>
      <CardContent sx={{ py: 1.15, px: 1.5, '&:last-child': { pb: 1.15 } }}>
        <Stack direction="row" spacing={1.1} alignItems="center">
          <Box
            sx={{
              width: 28,
              height: 28,
              flexShrink: 0,
              borderRadius: 0.75,
              display: 'grid',
              placeItems: 'center',
              color,
              bgcolor: alpha(color, 0.1),
              '& .MuiSvgIcon-root': { fontSize: 15 },
            }}
          >
            {icon}
          </Box>
          <Box sx={{ minWidth: 0, flex: 1 }}>
            <Typography variant="overline" sx={{ display: 'block', lineHeight: 1.2, mb: 0.25 }}>
              {label}
            </Typography>
            <Stack direction="row" spacing={0.6} alignItems="baseline">
              <Typography
                sx={{
                  fontWeight: 700,
                  fontSize: '1.2rem',
                  lineHeight: 1,
                  letterSpacing: '-0.03em',
                  fontVariantNumeric: 'tabular-nums',
                }}
              >
                {value}
              </Typography>
              {hint && (
                <Typography variant="caption" sx={{ color: 'text.secondary', fontSize: '0.68rem' }}>
                  {hint}
                </Typography>
              )}
            </Stack>
          </Box>
        </Stack>
      </CardContent>
    </Card>
  )
}
