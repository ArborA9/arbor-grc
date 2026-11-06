import { Box, Stack, Typography } from '@mui/material'
import type { LicenceTermView } from '../../licenseExpiry'
import StatusChip from '../StatusChip'
import { brand } from '../../theme'

const panel: Record<LicenceTermView['tone'], { bg: string; border: string; fg: string; chip: 'success' | 'warning' | 'danger' | 'neutral' }> = {
  danger: { bg: '#FEF2F2', border: '#FECACA', fg: '#9B2C2C', chip: 'danger' },
  warning: { bg: '#FFF7ED', border: '#FED7AA', fg: '#9A3412', chip: 'warning' },
  success: { bg: '#F0FDF4', border: '#BBF7D0', fg: '#166534', chip: 'success' },
  neutral: { bg: brand.hover, border: brand.border, fg: brand.muted, chip: 'neutral' },
}

function termLine(term: LicenceTermView): string {
  return term.detail ? `${term.headline} · ${term.detail}` : term.headline
}

/** Licence term. Compact = table cell. Default = one-line colored panel. */
export default function LicenceTermBadge({
  term,
  compact = false,
}: {
  term: LicenceTermView
  compact?: boolean
}) {
  const t = panel[term.tone]
  if (compact) {
    return (
      <Typography
        noWrap
        title={termLine(term)}
        sx={{
          fontSize: '0.75rem',
          fontWeight: 650,
          lineHeight: 1.3,
          color: t.fg,
        }}
      >
        {termLine(term)}
      </Typography>
    )
  }

  return (
    <Box
      sx={{
        flexShrink: 0,
        px: 1.15,
        py: 0.55,
        borderRadius: 1,
        bgcolor: t.bg,
        border: `1px solid ${t.border}`,
      }}
    >
      <Stack direction="row" spacing={0.75} alignItems="center" flexWrap="nowrap">
        <Typography
          noWrap
          sx={{
            fontSize: '0.8125rem',
            fontWeight: 700,
            lineHeight: 1.25,
            letterSpacing: '-0.01em',
            color: t.fg,
          }}
        >
          {term.headline}
        </Typography>
        {term.detail ? (
          <StatusChip label={term.detail} tone={t.chip} sx={{ height: 20, fontSize: '0.625rem' }} />
        ) : null}
      </Stack>
    </Box>
  )
}
