import { type ReactNode } from 'react'
import { Box, Typography } from '@mui/material'
import { brand } from '../../theme'

export type KpiTrendTone = 'danger' | 'warning' | 'neutral' | 'success'

const trendTones: Record<KpiTrendTone, { bg: string; color: string }> = {
  danger: { bg: '#FEE2E2', color: '#991B1B' },
  warning: { bg: brand.goldLight, color: '#92400E' },
  neutral: { bg: brand.hover, color: brand.muted },
  success: { bg: '#DCFCE7', color: '#166534' },
}

export interface KpiCardProps {
  label: string
  value: ReactNode
  /** Left accent bar and default value color. */
  accentColor?: string
  /** Override value text color (accent bar still uses accentColor). */
  valueColor?: string
  sub?: string
  trend?: {
    label: string
    tone?: KpiTrendTone
    bg?: string
    color?: string
  }
  /** Compact strip variant (e.g. risk register mini KPIs). */
  compact?: boolean
  /**
   * Shrink value type when the string is long (department names in a narrow column).
   * Numeric KPIs should leave this off.
   */
  fitText?: boolean
  onClick?: () => void
}

/** Compact / default rem sizes for long string values (e.g. department name). */
function fittedValueSx(text: string, compact: boolean): { fontSize: string; lineHeight: number } {
  const len = text.trim().length
  if (compact) {
    if (len <= 14) return { fontSize: '1.375rem', lineHeight: 1.15 }
    if (len <= 22) return { fontSize: '1.125rem', lineHeight: 1.2 }
    if (len <= 36) return { fontSize: '0.9375rem', lineHeight: 1.25 }
    return { fontSize: '0.8125rem', lineHeight: 1.3 }
  }
  if (len <= 18) return { fontSize: '1.875rem', lineHeight: 1.15 }
  if (len <= 28) return { fontSize: '1.375rem', lineHeight: 1.2 }
  if (len <= 44) return { fontSize: '1.125rem', lineHeight: 1.25 }
  return { fontSize: '0.9375rem', lineHeight: 1.3 }
}

export default function KpiCard({
  label,
  value,
  accentColor = brand.gold,
  valueColor,
  sub,
  trend,
  compact = false,
  fitText = false,
  onClick,
}: KpiCardProps) {
  const trendStyle = trend
    ? trend.bg && trend.color
      ? { bg: trend.bg, color: trend.color }
      : trendTones[trend.tone ?? 'neutral']
    : null

  const clickable = Boolean(onClick)
  const valueInk = valueColor ?? accentColor
  const valueText = fitText && typeof value === 'string' ? value : null
  const fitted = valueText != null ? fittedValueSx(valueText, compact) : null

  return (
    <Box
      component={clickable ? 'button' : 'div'}
      type={clickable ? 'button' : undefined}
      onClick={onClick}
      sx={{
        position: 'relative',
        overflow: 'hidden',
        bgcolor: brand.paper,
        border: `1px solid ${brand.border}`,
        borderRadius: 1.125,
        boxShadow: brand.shadowCard,
        px: compact ? 1.75 : 2.25,
        py: compact ? 1.1 : 2,
        cursor: clickable ? 'pointer' : 'default',
        font: 'inherit',
        textAlign: 'left',
        width: '100%',
        height: '100%',
        boxSizing: 'border-box',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'stretch',
        justifyContent: 'flex-start',
        // Buttons otherwise vertically center content when the grid stretches the card.
        appearance: 'none',
        WebkitAppearance: 'none',
        transition: clickable ? 'box-shadow .12s ease, transform .12s ease' : undefined,
        '&:hover': clickable
          ? { boxShadow: '0 4px 14px rgba(28, 25, 23, 0.08)', transform: 'translateY(-1px)' }
          : undefined,
        '&:focus-visible': clickable
          ? { outline: `2px solid ${brand.gold}`, outlineOffset: 2 }
          : undefined,
      }}
    >
      <Box
        sx={{
          // Fixed label band so values share one baseline across the strip.
          minHeight: compact ? 28 : undefined,
          mb: compact ? 0.4 : 0.75,
          flexShrink: 0,
        }}
      >
        <Typography
          sx={{
            fontSize: compact ? '0.5625rem' : '0.59375rem',
            fontWeight: 600,
            letterSpacing: '0.07em',
            textTransform: 'uppercase',
            color: brand.ink,
            lineHeight: 1.25,
            display: compact ? '-webkit-box' : undefined,
            WebkitLineClamp: compact ? 2 : undefined,
            WebkitBoxOrient: compact ? 'vertical' : undefined,
            overflow: compact ? 'hidden' : undefined,
          }}
        >
          {label}
        </Typography>
      </Box>
      <Typography
        component="div"
        title={valueText && valueText.length > 36 ? valueText : undefined}
        sx={{
          fontSize: fitted?.fontSize ?? (compact ? '1.375rem' : '1.875rem'),
          fontWeight: 700,
          lineHeight: fitted?.lineHeight ?? 1,
          color: valueInk,
          mb: sub ? 0.35 : 0,
          fontVariantNumeric: fitted ? undefined : 'tabular-nums',
          flexShrink: 0,
          overflowWrap: fitted ? 'anywhere' : undefined,
          wordBreak: fitted ? 'break-word' : undefined,
        }}
      >
        {value}
      </Typography>
      {sub ? (
        <Typography sx={{ fontSize: '0.6875rem', color: valueColor ?? brand.muted, lineHeight: 1.35 }}>
          {sub}
        </Typography>
      ) : null}
      {trend && trendStyle && (
        <Box
          sx={{
            position: 'absolute',
            top: compact ? 12 : 16,
            right: compact ? 12 : 16,
            fontSize: '0.6875rem',
            fontWeight: 600,
            px: 1,
            py: 0.25,
            borderRadius: 20,
            bgcolor: trendStyle.bg,
            color: trendStyle.color,
          }}
        >
          {trend.label}
        </Box>
      )}
    </Box>
  )
}

export function KpiCardGrid({
  columns = 3,
  dense = false,
  children,
}: {
  columns?: number
  /** Tighter strip for register pages (less empty band under the KPIs). */
  dense?: boolean
  children: ReactNode
}) {
  return (
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: {
          xs: '1fr',
          sm: `repeat(${Math.min(columns, 2)}, 1fr)`,
          md: `repeat(${columns}, 1fr)`,
        },
        alignItems: 'stretch',
        gap: dense ? 1 : 1.5,
        mb: dense ? 1 : 2.75,
      }}
    >
      {children}
    </Box>
  )
}
