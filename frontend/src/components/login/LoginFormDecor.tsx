import { Box } from '@mui/material'
import { brand } from '../../theme'
import { loginDecorCardSx, loginDecorLayerSx, loginDecorSizes, loginFloat } from './loginStyles'

/** Graphics near the sign-in card: behind the panel, with a clear safe zone. */
export default function LoginFormDecor() {
  const medium = loginDecorSizes.medium

  return (
    <Box
      aria-hidden
      sx={{
        ...loginDecorLayerSx,
        inset: -72,
      }}
    >
      {/* Gap assessment: medium, lower-left, clear of hero copy */}
      <Box
        sx={{
          display: { xs: 'none', xl: 'block' },
          position: 'absolute',
          bottom: -88,
          left: -112,
          width: medium.width,
          ...loginFloat('3.5s', '14s'),
        }}
      >
        <Box sx={{ ...loginDecorCardSx, width: '100%', minHeight: medium.minHeight, p: medium.p }}>
          <Box sx={{ fontSize: '0.55rem', fontWeight: 700, letterSpacing: '0.08em', color: brand.muted, mb: 0.5 }}>
            GAP ASSESSMENT
          </Box>
          <Box sx={{ fontSize: '0.68rem', fontWeight: 650, color: brand.inkSoft, mb: 0.75 }}>
            ISO 27001 · Domain A.5
          </Box>
          <Box sx={{ height: 4, borderRadius: 1, bgcolor: brand.borderLight, overflow: 'hidden', mb: 0.35 }}>
            <Box sx={{ width: '68%', height: '100%', bgcolor: brand.gold, borderRadius: 1, opacity: 0.85 }} />
          </Box>
          <Box sx={{ fontSize: '0.58rem', color: brand.goldDark, fontWeight: 600, opacity: 0.9 }}>68% assessed</Box>
        </Box>
      </Box>
    </Box>
  )
}
