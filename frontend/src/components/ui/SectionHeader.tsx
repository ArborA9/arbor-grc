import { type ReactNode } from 'react'
import { Box, Typography } from '@mui/material'
import { brand } from '../../theme'

export interface SectionHeaderProps {
  title: string
  actionLabel?: string
  onAction?: () => void
  hint?: ReactNode
}

/** Kit `.section-hdr`: uppercase muted title with optional gold action link. */
export default function SectionHeader({ title, actionLabel, onAction, hint }: SectionHeaderProps) {
  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: 1,
        mb: 1.5,
      }}
    >
      <Box sx={{ minWidth: 0 }}>
        <Typography
          sx={{
            fontSize: '0.6875rem',
            fontWeight: 600,
            color: brand.muted,
            letterSpacing: '0.04em',
            textTransform: 'uppercase',
          }}
        >
          {title}
        </Typography>
        {hint && (
          <Typography variant="caption" sx={{ color: brand.mutedLight, mt: 0.25, display: 'block' }}>{hint}</Typography>
        )}
      </Box>
      {actionLabel && onAction && (
        <Box
          component="button"
          type="button"
          onClick={onAction}
          sx={{
            border: 0,
            p: 0,
            m: 0,
            flexShrink: 0,
            font: 'inherit',
            fontSize: '0.6875rem',
            fontWeight: 600,
            color: brand.goldDark,
            cursor: 'pointer',
            bgcolor: 'transparent',
            '&:hover': { color: brand.gold, textDecoration: 'underline' },
          }}
        >
          {actionLabel}
        </Box>
      )}
    </Box>
  )
}
