import { type ReactNode } from 'react'
import { Box, Stack, Typography } from '@mui/material'
import { brand } from '../../theme'

export default function WorkspaceEmptyPanel({
  title,
  message,
  steps,
  choicesLabel,
  choices,
  action,
}: {
  title: string
  message: string
  steps?: { title: string; detail: string }[]
  choicesLabel?: string
  choices?: { id: string | number; label: string; hint?: string; actionLabel?: string; onClick?: () => void }[]
  action?: ReactNode
}) {
  return (
    <Box
      sx={{
        flex: 1,
        alignSelf: 'stretch',
        height: '100%',
        minHeight: 0,
        display: 'flex',
        flexDirection: 'column',
        bgcolor: brand.canvas,
      }}
    >
      <Box
        sx={{
          px: 1.25,
          py: 1.25,
          bgcolor: brand.sidebarSelectedFill,
          borderBottom: `1px solid ${brand.border}`,
          position: 'relative',
          flexShrink: 0,
          '&::before': {
            content: '""',
            position: 'absolute',
            left: 0,
            top: 0,
            bottom: 0,
            width: 3,
            bgcolor: brand.gold,
          },
        }}
      >
        <Typography sx={{ fontSize: '1.0625rem', fontWeight: 650, color: brand.ink, letterSpacing: '-0.02em' }}>
          {title}
        </Typography>
        <Typography sx={{ fontSize: '0.8125rem', color: brand.muted, mt: 0.5, lineHeight: 1.45, maxWidth: 720 }}>
          {message}
        </Typography>
      </Box>

      <Box sx={{ flex: 1, minHeight: 0, overflow: 'auto', px: 1.25, py: 1.25 }}>
        <Box sx={{ maxWidth: 760 }}>
          {steps && steps.length > 0 ? (
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: { xs: '1fr', md: `repeat(${Math.min(steps.length, 3)}, minmax(0, 1fr))` },
                gap: 1.25,
                mb: 2.5,
              }}
            >
              {steps.map((step, i) => (
                <Box
                  key={step.title}
                  sx={{
                    border: `1px solid ${brand.border}`,
                    borderRadius: 1,
                    px: 1.5,
                    py: 1.25,
                    bgcolor: brand.paper,
                  }}
                >
                  <Typography sx={{ fontSize: '0.6875rem', fontWeight: 700, color: brand.goldDark, letterSpacing: '0.04em' }}>
                    {i + 1}. {step.title}
                  </Typography>
                  <Typography sx={{ fontSize: '0.75rem', color: brand.muted, mt: 0.5, lineHeight: 1.4 }}>
                    {step.detail}
                  </Typography>
                </Box>
              ))}
            </Box>
          ) : null}

          {choices && choices.length > 0 ? (
            <Box sx={{ mb: 2 }}>
              {choicesLabel ? (
                <Typography sx={{ fontSize: '0.6875rem', fontWeight: 650, color: brand.muted, mb: 1 }}>
                  {choicesLabel}
                </Typography>
              ) : null}
              <Box
                sx={{
                  border: `1px solid ${brand.border}`,
                  borderRadius: 1,
                  overflow: 'hidden',
                  bgcolor: brand.paper,
                }}
              >
                {choices.map((choice, i) => (
                  <Box
                    key={choice.id}
                    component={choice.onClick ? 'button' : 'div'}
                    type={choice.onClick ? 'button' : undefined}
                    onClick={choice.onClick}
                    sx={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      gap: 1.5,
                      width: '100%',
                      textAlign: 'left',
                      border: 0,
                      borderTop: i === 0 ? 0 : `1px solid ${brand.border}`,
                      bgcolor: 'transparent',
                      color: brand.ink,
                      px: 1.5,
                      py: 1.15,
                      fontFamily: 'inherit',
                      cursor: choice.onClick ? 'pointer' : 'default',
                      '&:hover': choice.onClick
                        ? { bgcolor: brand.goldLight }
                        : undefined,
                    }}
                  >
                    <Stack spacing={0.15} sx={{ minWidth: 0 }}>
                      <Typography sx={{ fontSize: '0.8125rem', fontWeight: 650, color: brand.ink }}>
                        {choice.label}
                      </Typography>
                      {choice.hint ? (
                        <Typography sx={{ fontSize: '0.75rem', color: brand.muted }}>
                          {choice.hint}
                        </Typography>
                      ) : null}
                    </Stack>
                    {choice.onClick ? (
                      <Typography sx={{ fontSize: '0.75rem', fontWeight: 650, color: brand.goldDark, flexShrink: 0 }}>
                        {choice.actionLabel ?? 'Open'}
                      </Typography>
                    ) : null}
                  </Box>
                ))}
              </Box>
            </Box>
          ) : null}

          {action ? <Box>{action}</Box> : null}
        </Box>
      </Box>
    </Box>
  )
}
