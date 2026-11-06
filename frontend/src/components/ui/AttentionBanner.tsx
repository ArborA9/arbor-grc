import { type ReactNode } from 'react'
import { Box, IconButton, Typography } from '@mui/material'
import CloseRoundedIcon from '@mui/icons-material/CloseRounded'
import WarningAmberRoundedIcon from '@mui/icons-material/WarningAmberRounded'
import { brand } from '../../theme'

export type AttentionBannerTone = 'warning' | 'info' | 'error' | 'success'

const tones: Record<
  AttentionBannerTone,
  { bg: string; border: string; color: string; emphasis: string }
> = {
  warning: {
    bg: '#FFFBEB',
    border: brand.goldMid,
    color: '#92400E',
    emphasis: '#78350F',
  },
  success: {
    bg: '#F0FDF4',
    border: '#BBF7D0',
    color: '#166534',
    emphasis: '#14532D',
  },
  info: {
    bg: brand.goldLight,
    border: brand.goldMid,
    color: brand.goldDark,
    emphasis: brand.inkSoft,
  },
  error: {
    bg: '#FEF2F2',
    border: '#FECACA',
    color: '#991B1B',
    emphasis: '#7F1D1D',
  },
}

export interface AttentionBannerProps {
  /** Bold lead-in (e.g. "5 items need attention"). */
  emphasis?: string
  message: ReactNode
  actionLabel?: string
  onAction?: () => void
  /** When set, shows a close control on the right. */
  onDismiss?: () => void
  dismissLabel?: string
  /** Tighter padding; no bottom margin (use in Stacks that already space children). */
  dense?: boolean
  tone?: AttentionBannerTone
  icon?: ReactNode
}

export default function AttentionBanner({
  emphasis,
  message,
  actionLabel,
  onAction,
  onDismiss,
  dismissLabel = 'Dismiss',
  tone = 'warning',
  icon,
  dense = false,
}: AttentionBannerProps) {
  const t = tones[tone]

  return (
    <Box
      role="status"
      sx={{
        display: 'flex',
        alignItems: 'center',
        gap: 1.25,
        px: dense ? 1.5 : 2,
        py: dense ? 0.75 : 1.25,
        mb: dense ? 0 : 2.5,
        bgcolor: t.bg,
        border: `1px solid ${t.border}`,
        borderRadius: 1.125,
        fontSize: '0.75rem',
        fontWeight: 500,
        color: t.color,
      }}
    >
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          color: t.emphasis,
          '& .MuiSvgIcon-root': { fontSize: 18 },
        }}
      >
        {icon ?? <WarningAmberRoundedIcon />}
      </Box>
      <Typography component="div" sx={{ flex: 1, fontSize: 'inherit', fontWeight: 'inherit', color: 'inherit', minWidth: 0 }}>
        {emphasis && (
          <Box component="span" sx={{ fontWeight: 700, color: t.emphasis }}>
            {emphasis}
          </Box>
        )}
        {emphasis && message ? ': ' : null}
        {message}
      </Typography>
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
            textDecoration: 'underline',
            '&:hover': { color: brand.gold },
          }}
        >
          {actionLabel}
        </Box>
      )}
      {onDismiss && (
        <IconButton
          size="small"
          aria-label={dismissLabel}
          onClick={onDismiss}
          sx={{
            flexShrink: 0,
            ml: actionLabel && onAction ? 0.25 : 0,
            color: t.emphasis,
            p: 0.35,
            '&:hover': { bgcolor: 'rgba(0,0,0,0.04)' },
          }}
        >
          <CloseRoundedIcon sx={{ fontSize: 16 }} />
        </IconButton>
      )}
    </Box>
  )
}
