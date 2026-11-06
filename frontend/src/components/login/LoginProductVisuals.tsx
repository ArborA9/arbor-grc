import { Box, Stack, Typography } from '@mui/material'
import FolderOpenRoundedIcon from '@mui/icons-material/FolderOpenRounded'
import FactCheckRoundedIcon from '@mui/icons-material/FactCheckRounded'
import { alpha } from '@mui/material/styles'
import { brand } from '../../theme'
import { loginDecorCardSx, loginDecorSizes, loginFloat } from './loginStyles'
import LoginHeatmapGrid from './LoginHeatmapGrid'

const decorLabelSx = {
  fontSize: '0.55rem',
  fontWeight: 700,
  letterSpacing: '0.08em',
  color: brand.muted,
} as const

/** Decorative cards in a dedicated side lane: never overlaps hero copy. */
export default function LoginProductVisuals() {
  const large = loginDecorSizes.large
  const medium = loginDecorSizes.medium

  // Vertical stack: gaps account for taller numbered heatmap grid + float animation (±5px)
  const heatmapTop = 136
  const heatmapHeight = 108
  const cardGap = 18
  const auditTop = heatmapTop + heatmapHeight + cardGap
  const auditHeight = 108
  const evidenceTop = auditTop + auditHeight + cardGap
  const laneHeight = evidenceTop + 108 + 12

  return (
    <Box
      aria-hidden
      sx={{
        position: 'relative',
        width: '100%',
        height: '100%',
        minHeight: laneHeight,
        pointerEvents: 'none',
      }}
    >
      <Box
        sx={{
          ...loginDecorCardSx,
          ...loginFloat('0s', '13s'),
          position: 'absolute',
          top: 0,
          left: 0,
          width: large.width,
          minHeight: large.minHeight,
          p: large.p,
        }}
      >
        <Typography sx={{ ...decorLabelSx, mb: 0.75 }}>COMPLIANCE POSTURE</Typography>
        <Stack direction="row" alignItems="baseline" spacing={0.5} sx={{ mb: 1 }}>
          <Typography sx={{ fontWeight: 700, fontSize: '1.75rem', color: brand.inkSoft, lineHeight: 1 }}>78</Typography>
          <Typography sx={{ fontSize: '0.75rem', fontWeight: 600, color: brand.goldDark, opacity: 0.9 }}>%</Typography>
        </Stack>
        <Box sx={{ height: 5, borderRadius: 1, bgcolor: brand.borderLight, overflow: 'hidden' }}>
          <Box sx={{ width: '78%', height: '100%', bgcolor: brand.gold, borderRadius: 1, opacity: 0.8 }} />
        </Box>
        <Typography sx={{ fontSize: '0.6rem', color: brand.muted, mt: 0.75 }}>Across activated frameworks</Typography>
      </Box>

      <Box
        sx={{
          ...loginDecorCardSx,
          ...loginFloat('2.8s', '11s'),
          position: 'absolute',
          top: heatmapTop,
          left: -12,
          width: medium.width,
          p: medium.p,
        }}
      >
        <Typography sx={{ ...decorLabelSx, mb: 0.65 }}>RISK HEATMAP</Typography>
        <LoginHeatmapGrid />
      </Box>

      <Box
        sx={{
          ...loginDecorCardSx,
          ...loginFloat('5.2s', '15s'),
          position: 'absolute',
          top: auditTop,
          left: 8,
          width: medium.width,
          minHeight: medium.minHeight,
          p: medium.p,
        }}
      >
        <Stack direction="row" spacing={0.75} alignItems="center" sx={{ mb: 0.75 }}>
          <FactCheckRoundedIcon sx={{ fontSize: 14, color: brand.gold, opacity: 0.85 }} />
          <Typography sx={decorLabelSx}>AUDIT ENGAGEMENT</Typography>
        </Stack>
        <Typography sx={{ fontSize: '0.68rem', fontWeight: 650, color: brand.inkSoft, mb: 0.65 }}>
          Q2 Internal Audit · In progress
        </Typography>
        <Box sx={{ height: 4, borderRadius: 1, bgcolor: brand.borderLight, overflow: 'hidden', mb: 0.35 }}>
          <Box sx={{ width: '54%', height: '100%', bgcolor: brand.goldDark, borderRadius: 1, opacity: 0.8 }} />
        </Box>
        <Typography sx={{ fontSize: '0.58rem', color: brand.muted }}>12 of 22 controls reviewed</Typography>
      </Box>

      <Box
        sx={{
          ...loginDecorCardSx,
          ...loginFloat('7.6s', '12s'),
          position: 'absolute',
          top: evidenceTop,
          left: -8,
          width: medium.width,
          minHeight: medium.minHeight,
          p: medium.p,
          bgcolor: alpha(brand.paper, 0.88),
        }}
      >
        <Stack direction="row" spacing={0.75} alignItems="center" sx={{ mb: 0.5 }}>
          <FolderOpenRoundedIcon sx={{ fontSize: 14, color: brand.gold, opacity: 0.85 }} />
          <Typography sx={decorLabelSx}>EVIDENCE REPOSITORY</Typography>
        </Stack>
        <Stack spacing={0.45}>
          {['Access control policy.pdf', 'BCP test results.xlsx'].map((f) => (
            <Box
              key={f}
              sx={{
                px: 0.75,
                py: 0.35,
                borderRadius: 0.75,
                bgcolor: alpha(brand.hover, 0.9),
                border: `1px solid ${alpha(brand.borderLight, 0.9)}`,
                fontSize: '0.58rem',
                color: brand.muted,
                whiteSpace: 'nowrap',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
              }}
            >
              {f}
            </Box>
          ))}
        </Stack>
      </Box>
    </Box>
  )
}
