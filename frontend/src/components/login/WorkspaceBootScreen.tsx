import { Box, LinearProgress, Typography } from '@mui/material'
import { LogoMark } from '../ui'
import { brand } from '../../theme'
import { loginFadeIn } from './loginStyles'

/** Full-viewport sign-in / first-load wait. Keep copy calm; stay until the next screen has data. */
export default function WorkspaceBootScreen({
  title,
  message,
}: {
  title: string
  message: string
}) {
  return (
    <Box
      role="status"
      aria-live="polite"
      aria-busy="true"
      sx={{
        position: 'fixed',
        inset: 0,
        zIndex: 40,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        px: 2,
        bgcolor: brand.canvas,
      }}
    >
      <Box
        sx={{
          ...loginFadeIn,
          width: '100%',
          maxWidth: 400,
          bgcolor: brand.paper,
          border: `1px solid ${brand.border}`,
          borderRadius: 1.5,
          boxShadow: '0 14px 36px rgba(28, 25, 23, 0.055), 0 2px 10px rgba(184, 134, 11, 0.035)',
          px: 3,
          py: 3.25,
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25, mb: 2.25 }}>
          <LogoMark size={36} />
          <Box sx={{ minWidth: 0 }}>
            <Typography sx={{ fontWeight: 700, fontSize: '0.9375rem', letterSpacing: '-0.02em', color: brand.ink, lineHeight: 1.2 }}>
              Arbor GRC
            </Typography>
            <Typography sx={{ fontSize: '0.6875rem', color: brand.mutedLight, mt: 0.2 }}>
              Secure workspace
            </Typography>
          </Box>
        </Box>
        <Typography sx={{ fontSize: '1.0625rem', fontWeight: 700, color: brand.ink, letterSpacing: '-0.03em', lineHeight: 1.3 }}>
          {title}
        </Typography>
        <Typography sx={{ fontSize: '0.8125rem', color: brand.muted, lineHeight: 1.55, mt: 0.75, mb: 2 }}>
          {message}
        </Typography>
        <LinearProgress
          aria-label={title}
          sx={{
            height: 3,
            borderRadius: 1,
            bgcolor: brand.goldLight,
            '& .MuiLinearProgress-bar': { bgcolor: brand.goldDark },
          }}
        />
      </Box>
    </Box>
  )
}
