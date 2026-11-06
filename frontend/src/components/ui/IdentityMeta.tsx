import { type ReactNode } from 'react'
import { Box, Typography } from '@mui/material'
import StatusChip from '../StatusChip'
import { brand } from '../../theme'
import { narrativeReadScrollSx } from './formLayout'
import { readOnlyCaptionSx } from './FormFieldLabel'

export function MetaPair({
  label,
  value,
  scroll,
}: {
  label: string
  value: ReactNode
  scroll?: boolean
}) {
  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: scroll ? 'flex-start' : 'baseline',
        gap: 0.75,
        minWidth: 0,
        py: 0.15,
      }}
    >
      <Typography
        sx={{
          ...readOnlyCaptionSx,
          mb: 0,
          flexShrink: 0,
          lineHeight: 1.3,
          minWidth: 72,
        }}
      >
        {label}
      </Typography>
      <Typography
        sx={{
          fontSize: '0.75rem',
          fontWeight: 550,
          color: brand.ink,
          lineHeight: 1.3,
          minWidth: 0,
          flex: 1,
          wordBreak: 'break-word',
          ...(scroll ? narrativeReadScrollSx : {}),
        }}
      >
        {value === '' || value == null ? '-' : value}
      </Typography>
    </Box>
  )
}

export function MetaChip({
  label,
  chipLabel,
  tone,
}: {
  label: string
  chipLabel: string
  tone: 'success' | 'info' | 'warning' | 'danger' | 'neutral'
}) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.75, py: 0.15, minWidth: 0 }}>
      <Typography sx={{ ...readOnlyCaptionSx, mb: 0, flexShrink: 0, minWidth: 72, lineHeight: 1.3 }}>
        {label}
      </Typography>
      <StatusChip label={chipLabel} tone={tone} />
    </Box>
  )
}

export const identityTileGridSx = {
  display: 'grid',
  gridTemplateColumns: { xs: '1fr', md: 'repeat(3, minmax(0, 1fr))' },
  gap: 1,
  flexShrink: 0,
} as const
