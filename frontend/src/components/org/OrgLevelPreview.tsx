import { Box, TextField, Typography } from '@mui/material'
import { brand } from '../../theme'

const LEVEL_HINTS = ['e.g. Department', 'e.g. Division', 'e.g. Section', 'e.g. Team'] as const

export default function OrgLevelPreview({
  drafts,
  onChange,
  disabled,
}: {
  drafts: Record<number, string>
  onChange: (level: number, label: string) => void
  disabled?: boolean
}) {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
      {[1, 2, 3, 4].map((level) => {
        const isRoot = level === 1
        const pad = (level - 1) * 1.5
        const badgeSize = 22 - (level - 1) * 2

        return (
          <Box
            key={level}
            sx={{
              display: 'flex',
              alignItems: 'center',
              gap: 1.25,
              pl: pad,
              py: 1,
              px: 1.5,
              bgcolor: isRoot ? 'rgba(184, 134, 11, 0.08)' : brand.hover,
              border: `1px solid ${isRoot ? brand.goldMid : brand.border}`,
              borderRadius: 0.75,
            }}
          >
            <Box
              sx={{
                width: badgeSize,
                height: badgeSize,
                borderRadius: 0.5,
                bgcolor: isRoot ? brand.gold : '#EDE8DF',
                color: isRoot ? '#fff' : brand.goldDark,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: badgeSize <= 14 ? '0.5rem' : '0.5625rem',
                fontWeight: 700,
                flexShrink: 0,
              }}
            >
              {level}
            </Box>
            {disabled ? (
              <Typography
                sx={{
                  fontSize: '0.6875rem',
                  fontWeight: 600,
                  color: isRoot ? brand.goldDark : brand.ink,
                  flex: 1,
                  minWidth: 0,
                }}
              >
                {drafts[level] || `Level ${level}`}
              </Typography>
            ) : (
              <TextField
                size="small"
                variant="standard"
                value={drafts[level] ?? ''}
                onChange={(e) => onChange(level, e.target.value)}
                placeholder={`Level ${level}`}
                InputProps={{ disableUnderline: true }}
                sx={{
                  flex: 1,
                  '& .MuiInputBase-input': {
                    fontSize: '0.6875rem',
                    fontWeight: 600,
                    color: isRoot ? brand.goldDark : brand.ink,
                    py: 0,
                  },
                }}
              />
            )}
            <Typography
              sx={{
                fontSize: '0.6875rem',
                color: brand.mutedLight,
                flexShrink: 0,
                display: { xs: 'none', sm: 'block' },
              }}
            >
              {LEVEL_HINTS[level - 1]}
            </Typography>
          </Box>
        )
      })}
    </Box>
  )
}
