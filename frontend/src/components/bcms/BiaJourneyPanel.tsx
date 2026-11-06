import { type ReactNode } from 'react'
import { Box, Stack, Typography } from '@mui/material'
import { brand } from '../../theme'
import SettingsCard from '../ui/SettingsCard'

export type BiaJourneyStepState = 'done' | 'active' | 'todo'

export type BiaJourneyStep = {
  id: string
  label: string
  /** Short progress under the label when active/done (e.g. "2 of 3 approved"). */
  hint?: string
  /** Person for this step, shown in gold under the label. */
  person?: string
  state: BiaJourneyStepState
}

/**
 * Persistent journey rail.
 * Default horizontal (BIA). `orientation="vertical"` for side rails (TPRM) so the form keeps width.
 */
export default function BiaJourneyPanel({
  title = 'Workflow',
  steps,
  actionTitle,
  children,
  orientation = 'horizontal',
}: {
  title?: string
  steps: BiaJourneyStep[]
  /** Headline for the current primary action. */
  actionTitle: string
  children: ReactNode
  orientation?: 'horizontal' | 'vertical'
}) {
  const vertical = orientation === 'vertical'

  return (
    <SettingsCard autoHeight title={title}>
      <Stack
        direction={vertical ? 'column' : 'row'}
        spacing={vertical ? 0 : 2.5}
        sx={{
          mb: 1.75,
          overflowX: vertical ? 'visible' : 'auto',
          pb: vertical ? 0 : 0.5,
          borderBottom: vertical ? 'none' : `1px solid ${brand.hairline}`,
          borderRight: vertical ? `1px solid ${brand.hairline}` : 'none',
          pr: vertical ? 1 : 0,
        }}
      >
        {steps.map((step, i) => {
          const isLast = i === steps.length - 1
          const done = step.state === 'done'
          const active = step.state === 'active'
          return (
            <Stack
              key={step.id}
              direction={vertical ? 'column' : 'row'}
              alignItems={vertical ? 'stretch' : 'flex-start'}
              sx={
                vertical
                  ? { width: '100%' }
                  : {
                      flex: '1 1 0',
                      minWidth: 148,
                      pr: isLast ? 0 : 0.5,
                    }
              }
            >
              <Stack direction="row" spacing={0.75} alignItems="flex-start" sx={{ minWidth: 0, width: '100%' }}>
                <Stack alignItems="center" sx={{ flexShrink: 0 }}>
                  <Box
                    aria-hidden
                    sx={{
                      width: 22,
                      height: 22,
                      borderRadius: '50%',
                      flexShrink: 0,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: '0.6875rem',
                      fontWeight: 700,
                      bgcolor: done || active ? brand.accent : brand.paper,
                      color: done || active ? '#fff' : brand.muted,
                      border: `1.5px solid ${done || active ? brand.accent : brand.hairlineStrong}`,
                    }}
                  >
                    {done ? '✓' : i + 1}
                  </Box>
                  {vertical && !isLast && (
                    <Box
                      aria-hidden
                      sx={{
                        width: 2,
                        flex: 1,
                        minHeight: 14,
                        my: 0.35,
                        bgcolor: done ? brand.accent : brand.hairlineStrong,
                      }}
                    />
                  )}
                </Stack>
                <Stack spacing={0.15} sx={{ flex: 1, minWidth: 0, pb: vertical ? (isLast ? 0 : 0.75) : 0 }}>
                  <Typography
                    sx={{
                      fontSize: '0.75rem',
                      fontWeight: active ? 700 : 600,
                      color: active ? brand.inkSoft : done ? brand.inkSoft : brand.muted,
                      lineHeight: 1.25,
                    }}
                  >
                    {step.label}
                  </Typography>
                  {step.person ? (
                    <Typography
                      sx={{
                        display: 'block',
                        fontSize: '0.6875rem',
                        fontWeight: 600,
                        lineHeight: 1.35,
                        color: brand.gold,
                        overflowWrap: 'anywhere',
                      }}
                    >
                      {step.person}
                    </Typography>
                  ) : null}
                  {step.hint ? (
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block', lineHeight: 1.3 }}>
                      {step.hint}
                    </Typography>
                  ) : null}
                </Stack>
              </Stack>
            </Stack>
          )
        })}
      </Stack>

      <Typography variant="subtitle2" sx={{ mb: 0.75, fontWeight: 700, color: brand.inkSoft }}>
        {actionTitle}
      </Typography>
      {children}
    </SettingsCard>
  )
}
