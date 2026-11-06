import { Box, CircularProgress, LinearProgress, Typography } from '@mui/material'
import { brand } from '../../theme'

/** Blocking wait while an IS / ERM project is created (UAE IA default risks can take a minute). */
export default function ProjectCreateProgress({
  isErm,
  isPrivacy = false,
  projectName,
  frameworkLabel,
  seeded,
  total,
}: {
  isErm: boolean
  isPrivacy?: boolean
  projectName: string
  frameworkLabel: string | null
  seeded: number
  total: number
}) {
  const title = isErm
    ? 'Creating Enterprise Risk project'
    : isPrivacy
      ? 'Creating Privacy Risk project'
      : 'Creating Information Security Risk project'
  const name = projectName.trim()
  const fw = frameworkLabel ?? 'the selected framework'
  const showCount = !isErm && total > 0
  const body = isErm
    ? 'Saving the project. Stay on this page.'
    : showCount
      ? `Loading default risks for ${fw}. Stay on this page until the project opens.`
      : 'Saving the project. Stay on this page.'
  const pct = showCount ? Math.min(100, Math.round((100 * seeded) / total)) : undefined

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
          <Box sx={{ minWidth: 0 }}>
            <Typography sx={{ fontSize: '0.9375rem', fontWeight: 700, color: brand.ink, lineHeight: 1.35 }}>
              {title}
            </Typography>
            {name ? (
              <Typography
                sx={{
                  fontSize: '0.875rem',
                  fontWeight: 600,
                  color: brand.ink,
                  mt: 0.35,
                  lineHeight: 1.35,
                }}
                noWrap
              >
                {name}
              </Typography>
            ) : null}
          </Box>
        </Box>
        <Typography sx={{ fontSize: '0.8125rem', color: brand.muted, lineHeight: 1.5, mb: 1.5 }}>
          {body}
        </Typography>
        <LinearProgress
          variant={showCount ? 'determinate' : 'indeterminate'}
          value={pct}
          aria-label="Project create in progress"
        />
        {showCount ? (
          <Typography sx={{ fontSize: '0.6875rem', color: brand.mutedLight, mt: 1, fontVariantNumeric: 'tabular-nums' }}>
            {seeded} of {total} default risk{total === 1 ? '' : 's'}
          </Typography>
        ) : null}
      </Box>
    </Box>
  )
}
