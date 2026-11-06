import { Chip, type ChipProps } from '@mui/material'
import { brand } from '../theme'

type Tone = 'success' | 'warning' | 'info' | 'neutral' | 'danger'

const tones: Record<Tone, { bg: string; fg: string; border: string }> = {
  success: { bg: 'rgba(13,148,136,0.12)', fg: '#0F766E', border: 'rgba(13,148,136,0.22)' },
  warning: { bg: 'rgba(183,121,31,0.12)', fg: '#8A5A12', border: 'rgba(183,121,31,0.28)' },
  info: { bg: brand.accentMuted, fg: brand.accentHover, border: 'rgba(27,159,224,0.28)' },
  neutral: { bg: 'rgba(10,31,51,0.05)', fg: brand.muted, border: brand.hairlineStrong },
  danger: { bg: 'rgba(197,48,48,0.1)', fg: '#9B2C2C', border: 'rgba(197,48,48,0.22)' },
}

export default function StatusChip({
  label,
  tone = 'neutral',
  ...rest
}: { label: string; tone?: Tone } & Omit<ChipProps, 'label' | 'color'>) {
  const t = tones[tone]
  return (
    <Chip
      size="small"
      label={label}
      {...rest}
      sx={{
        height: 22,
        fontWeight: 700,
        fontSize: '0.6875rem',
        letterSpacing: '0.02em',
        borderRadius: 1,
        bgcolor: t.bg,
        color: t.fg,
        border: `1px solid ${t.border}`,
        '& .MuiChip-label': { px: 1 },
        ...rest.sx,
      }}
    />
  )
}
