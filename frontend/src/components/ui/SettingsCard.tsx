import { type ReactNode } from 'react'
import { Box, Typography } from '@mui/material'
import { brand } from '../../theme'

export interface SettingsCardProps {
  title: string
  subtitle?: ReactNode
  action?: ReactNode
  children: ReactNode
  id?: string
  /** Tighter padding for dense forms. */
  compact?: boolean
  /** Page-section heading (readable). Default caption size is for nested settings groups. */
  heading?: boolean
  /** Stacked page sections. Default fills a grid cell (`height: 100%`). */
  autoHeight?: boolean
  /** KPI-style left accent (Arbor gold unless a token is passed). */
  accentColor?: string
  /** Warm header band (kit cream / sand). */
  headerBg?: string
  /** Primary form section: all caps, slightly larger than field captions. */
  titleCaps?: boolean
}

/** Kit Screen 5: settings panel (`.kpi-card` with titled header rule). */
export default function SettingsCard({
  title,
  subtitle,
  action,
  children,
  id,
  compact,
  heading,
  autoHeight,
  accentColor,
  headerBg,
  titleCaps,
}: SettingsCardProps) {
  const pad = compact ? 1 : 2.5
  const accentInset = accentColor ? 0.375 : 0
  return (
    <Box
      id={id}
      sx={{
        position: 'relative',
        bgcolor: brand.paper,
        border: `1px solid ${brand.border}`,
        borderRadius: 1,
        boxShadow: brand.shadowCard,
        p: pad,
        height: autoHeight ? 'auto' : '100%',
        // autoHeight cards must not flex-shrink in a column stack: overflow:hidden
        // would clip the body to the title (BIA Admin Specialist review).
        flexShrink: autoHeight ? 0 : undefined,
        display: 'flex',
        flexDirection: 'column',
        minWidth: 0,
        overflow: 'hidden',
        minHeight: autoHeight ? undefined : 0,
      }}
    >
      {accentColor ? (
        <Box
          aria-hidden
          sx={{
            position: 'absolute',
            left: 0,
            top: 0,
            bottom: 0,
            width: 3,
            borderRadius: '8px 0 0 8px',
            bgcolor: accentColor,
          }}
        />
      ) : null}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 1,
          pb: compact ? 0.5 : 1.25,
          mb: compact ? 0.75 : 2,
          borderBottom: `1px solid ${brand.border}`,
          flexShrink: 0,
          ...(headerBg
            ? {
                bgcolor: headerBg,
                mx: -pad,
                mt: -pad,
                px: pad,
                pt: compact ? 0.85 : 1.5,
                pl: pad + accentInset,
              }
            : accentColor
              ? { pl: accentInset }
              : undefined),
        }}
      >
        <Box sx={{ minWidth: 0 }}>
          <Typography
            sx={{
              fontSize: titleCaps
                ? compact
                  ? '0.75rem'
                  : '0.9375rem'
                : heading
                  ? compact
                    ? '0.8125rem'
                    : '0.9375rem'
                  : compact
                    ? '0.6875rem'
                    : '0.75rem',
              fontWeight: titleCaps || !heading ? 700 : 600,
              color: brand.ink,
              lineHeight: 1.3,
              letterSpacing: titleCaps ? '0.04em' : heading ? '-0.01em' : undefined,
              textTransform: titleCaps ? 'uppercase' : undefined,
            }}
          >
            {title}
          </Typography>
          {subtitle ? (
            <Typography
              sx={{
                fontSize: heading ? (compact ? '0.6875rem' : '0.75rem') : '0.5625rem',
                color: heading ? brand.muted : brand.mutedLight,
                mt: 0.15,
                lineHeight: 1.3,
              }}
            >
              {subtitle}
            </Typography>
          ) : null}
        </Box>
        {action ? <Box sx={{ flexShrink: 0 }}>{action}</Box> : null}
      </Box>
      <Box
        sx={{
          ...(autoHeight ? {} : { flex: 1, minHeight: 0 }),
          ...(accentColor ? { pl: accentInset } : {}),
        }}
      >
        {children}
      </Box>
    </Box>
  )
}

export function SettingsCardGrid({
  children,
  columns = 2,
}: {
  children: ReactNode
  columns?: 2 | 3
}) {
  return (
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns:
          columns === 3
            ? { xs: '1fr', md: 'repeat(3, minmax(0, 1fr))' }
            : { xs: '1fr', md: '1fr 1fr' },
        gap: 1.25,
        alignItems: 'stretch',
      }}
    >
      {children}
    </Box>
  )
}

export function SettingsField({
  label,
  children,
  mono = false,
}: {
  label: string
  children: ReactNode
  mono?: boolean
}) {
  return (
    <Box sx={{ mb: 1, '&:last-child': { mb: 0 } }}>
      <Typography
        sx={{
          fontSize: '0.6875rem',
          fontWeight: 600,
          color: brand.muted,
          textTransform: 'uppercase',
          letterSpacing: '0.06em',
          mb: 0.4,
        }}
      >
        {label}
      </Typography>
      <Box
        sx={{
          fontSize: mono ? '0.6875rem' : '0.75rem',
          color: brand.ink,
          fontFamily: mono ? 'ui-monospace, Menlo, monospace' : 'inherit',
          lineHeight: 1.4,
        }}
      >
        {children}
      </Box>
    </Box>
  )
}

export function SettingsReadonlyValue({
  children,
  accent,
  inline,
}: {
  children: ReactNode
  accent?: boolean
  inline?: boolean
}) {
  return (
    <Box
      component="span"
      sx={{
        display: inline ? 'inline-flex' : 'flex',
        px: 1,
        py: inline ? 0.25 : 0.5,
        bgcolor: accent ? brand.goldLight : brand.hover,
        border: `1px solid ${accent ? brand.goldMid : brand.border}`,
        borderRadius: 0.5,
        fontSize: '0.6875rem',
        color: accent ? brand.goldDark : brand.ink,
        fontWeight: 600,
        alignItems: 'center',
        gap: 0.5,
      }}
    >
      {children}
    </Box>
  )
}
