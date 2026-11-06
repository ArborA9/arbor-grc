import { Box, CircularProgress, LinearProgress, Typography } from '@mui/material'
import { brand } from '../../theme'

/** Page-level wait while an ERM / IS risk is created or a Contributor submits (files stay off this chrome). */
export default function RiskRecordCreateProgress({
  isErm,
  isPrivacy = false,
  mode = 'create',
}: {
  isErm: boolean
  isPrivacy?: boolean
  mode?: 'create' | 'submit'
}) {
  const title =
    mode === 'submit'
      ? isErm
        ? 'Submitting ERM Risk'
        : isPrivacy
          ? 'Submitting Privacy Risk'
          : 'Submitting Information Security Risk'
      : isErm
        ? 'Creating ERM Risk'
        : isPrivacy
          ? 'Creating Privacy Risk'
          : 'Creating Information Security Risk'
  const copy =
    mode === 'submit' ? 'Saving your action. Stay on this page.' : 'Saving the risk. Stay on this page.'
  return (
    <Box
      role="status"
      aria-live="polite"
      aria-busy="true"
      sx={{
        position: 'absolute',
        inset: 0,
        zIndex: 2,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        px: 2,
        py: 3,
        bgcolor: 'rgba(250, 250, 248, 0.92)',
      }}
    >
      <Box
        sx={{
          width: '100%',
          maxWidth: 420,
          bgcolor: brand.paper,
          border: `1px solid ${brand.border}`,
          borderRadius: 1,
          boxShadow: brand.shadowCard,
          p: 2.5,
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1.25, mb: 1.25 }}>
          <CircularProgress size={22} sx={{ color: brand.goldDark, mt: 0.15, flexShrink: 0 }} />
          <Typography sx={{ fontSize: '0.9375rem', fontWeight: 700, color: brand.ink, lineHeight: 1.35 }}>
            {title}
          </Typography>
        </Box>
        <Typography sx={{ fontSize: '0.8125rem', color: brand.muted, lineHeight: 1.5, mb: 1.5 }}>
          {copy}
        </Typography>
        <LinearProgress variant="indeterminate" aria-label="Risk save in progress" />
      </Box>
    </Box>
  )
}
